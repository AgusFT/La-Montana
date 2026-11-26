"""Ensambla el instalador sin ejecutar la aplicación ni descargar dependencias."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import tarfile
import tempfile
import zipfile

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
VERSION = '0.1.5'


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def unzip(path, dest, include, skip_root=True):
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            parts = PurePosixPath(name).parts[int(skip_root):]
            if not parts or name.endswith('/') or not include('/'.join(parts)):
                continue
            target = dest.joinpath(*parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(name) as src, target.open('wb') as dst:
                shutil.copyfileobj(src, dst)


def frontend(app, deps):
    project = REPO / 'frontend'
    built = project / '.next/standalone'
    web = app / 'web'
    shutil.copytree(built, web, ignore=shutil.ignore_patterns('node_modules'))
    for relative in ('public', '.next/static'):
        shutil.copytree(project / relative, web / relative, dirs_exist_ok=True)
    # Next incluye la ruta absoluta de construcción en los metadatos standalone.
    manifest = web / '.next/required-server-files.json'
    metadata = json.loads(manifest.read_text(encoding='utf-8'))
    original_root = metadata['appDir']
    metadata['appDir'] = '.'
    metadata['config']['turbopack']['root'] = '.'
    metadata['config']['outputFileTracingRoot'] = '.'
    manifest.write_text(json.dumps(metadata), encoding='utf-8')
    server = web / 'server.js'
    source = server.read_text(encoding='utf-8')
    server.write_text(source.replace(json.dumps(original_root, ensure_ascii=False), 'dir'), encoding='utf-8')
    # Materializar los paquetes trazados por Next sin enlaces ni las rutas largas del almacén pnpm.
    modules = web / 'node_modules'
    for folder in (built / 'node_modules/.pnpm').glob('*/node_modules'):
        for package in folder.iterdir():
            candidates = package.iterdir() if package.name.startswith('@') else [package]
            for p in candidates:
                if p.is_symlink() or not p.is_dir() or 'linux-' in p.name:
                    continue
                target = modules / p.relative_to(folder)
                shutil.copytree(p, target)
                source = project / p.relative_to(built)
                for directory, children, files in os.walk(source):
                    children[:] = [d for d in children if d != 'node_modules']
                    for name in files:
                        if name.lower().startswith(('license', 'licence', 'notice', 'copying', 'copyright')):
                            notice = Path(directory, name)
                            installed = target / notice.relative_to(source)
                            installed.parent.mkdir(parents=True, exist_ok=True)
                            shutil.copyfile(notice, installed)
    with tarfile.open(deps / 'sharp-win32-x64-0.35.4.tgz') as archive:
        archive.extractall(modules / '@img/sharp-win32-x64', filter='data')
    sharp = modules / '@img/sharp-win32-x64/package'
    for p in sharp.iterdir():
        p.rename(sharp.parent / p.name)
    sharp.rmdir()


def build(deps, dest, tmp):
    if dest.exists():
        raise FileExistsError('La carpeta de salida ya existe: ' + str(dest))
    app = tmp / 'app'; app.mkdir()
    for line in (HERE / 'dependencias.sha256').read_text().splitlines():
        expected, name = line.split()
        if digest(deps / name) != expected:
            raise ValueError('SHA-256 incorrecto: ' + name)
    jdk = Path(os.environ['JAVA_HOME']) / 'bin'
    unzip(next(deps.glob('OpenJDK17U-*.zip')), tmp, lambda s: s.startswith('jmods/'))
    subprocess.run([jdk/'jlink', '--module-path', tmp/'jmods', '--add-modules',
                    'java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.charsets,jdk.management,jdk.localedata,jdk.naming.dns',
                    '--no-man-pages','--no-header-files','--output',app/'java'], check=True)
    classes=tmp/'classes'; classes.mkdir()
    subprocess.run([jdk/'javac','--release','17','-encoding','UTF-8','-d',classes,*HERE.glob('*.java')],check=True)
    subprocess.run([jdk/'jar','cfe',app/'panel.jar','DemoWindows','-C',classes,'.'],check=True)
    shutil.copyfile(REPO/'backend/target/lamontana.jar',app/'backend.jar')
    frontend(app,deps)
    unzip(deps/'node-v22.23.5-win-x64.zip',app/'node',lambda s:s in {'node.exe','LICENSE'})
    unzip(deps/'clamav-1.4.6.win.x64.zip',app/'clamav',lambda s: s.endswith('.dll') or s.startswith('COPYING') or s in {'clamd.exe','freshclam.exe','sigtool.exe','certs.pem'})
    unzip(deps/'mailpit-windows-amd64.zip',app/'correo',lambda s:True, False)
    with zipfile.ZipFile(deps/'embedded-postgres-binaries-windows-amd64-18.6.0.jar') as jar:
        with jar.open('postgres-windows-x86_64.txz') as txz, tarfile.open(fileobj=txz,mode='r|xz') as pg:
            pg.extractall(app/'pg',filter='data')
    for pattern in ('vcruntime*.dll','msvcp140*.dll'):
        for dll in (app/'clamav').glob(pattern):
            shutil.copyfile(dll,app/'pg/bin'/dll.name)
    signatures=app/'firmas';signatures.mkdir()
    for name in ('main.cvd','daily.cld'):
        shutil.copyfile(deps/name,signatures/name)
    notices=app/'avisos';notices.mkdir()
    for name in ('postgresql-REL_18_6-COPYRIGHT','main.cvd-COPYING','daily.cld-COPYING','bibliotecas-backend.tsv','modulos-node.tsv'):
        shutil.copyfile(deps/'avisos'/name,notices/name)
    shutil.copyfile(HERE/'dependencias.sha256',app/'componentes.sha256')
    (app/'version.txt').write_text(VERSION+'\n',encoding='ascii')
    content=tmp/'contenido.zip'
    with zipfile.ZipFile(content,'w',zipfile.ZIP_DEFLATED,compresslevel=2) as archive:
        for file in sorted(app.rglob('*')):
            if file.is_file():archive.write(file,file.relative_to(app))
    exe=dest/f'LaMontanaDemo-{VERSION}.exe'
    prefix='/' if os.name=='nt' else '-'
    subprocess.run(['makensis',prefix+'V2',prefix+'DVERSION='+VERSION,prefix+'DSALIDA='+str(tmp/exe.name),prefix+'DCONTENIDO='+str(content),HERE/'demo.nsi'],check=True)
    # Publicar el binario y la suma juntos, también si dos compilaciones terminan a la vez.
    ready = tmp / 'entrega'
    ready.mkdir()
    binary = (tmp / exe.name).rename(ready / exe.name)
    binary.with_suffix('.exe.sha256').write_text(digest(binary)+'  '+exe.name+'\n',encoding='ascii')
    ready.rename(dest)
    print(exe,flush=True)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('dependencias',type=Path);parser.add_argument('salida',type=Path)
    args=parser.parse_args();args.salida.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(dir=args.salida.resolve().parent) as temp:
        build(args.dependencias.resolve(),args.salida.resolve(),Path(temp))
