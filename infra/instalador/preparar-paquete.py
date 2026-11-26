#!/usr/bin/env python3
"""Preparación de release (solo construcción; Python no se instala en el cliente)."""
import argparse
import hashlib
import io
import re
from pathlib import Path
import shutil
import tempfile
import zipfile
from comprobar_firmas_imagenes import comprobar_firmas_externas


def properties(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines()
                if '=' in line and not line.startswith('#'))


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('plataforma', choices=['ubuntu', 'windows'])
    parser.add_argument('app', type=Path)
    parser.add_argument('dependencias', type=Path)
    parser.add_argument('salida', type=Path)
    parser.add_argument('--version-instalador', help='Versión del asistente; por defecto usa la versión de la app.')
    parser.add_argument('--avisos-terceros', type=Path, help='ZIP de avisos de Windows; por defecto se busca AVISOS-TERCEROS.zip junto a las dependencias.')
    args = parser.parse_args()
    base = Path(__file__).resolve().parent
    manifest = properties(args.app / 'release.properties')
    if manifest.get('edition') != 'DEMO' or manifest.get('files') != 'images.tar.gz':
        parser.error('Se requiere el archivo de imágenes de la edición Demo.')
    bytecode_externo = manifest.get('antivirus.bytecode') == 'externo'
    if args.plataforma == 'windows' and not bytecode_externo:
        parser.error('Generá las imágenes con crear-paquete.sh --para-windows para obtener las firmas al instalar.')
    if args.plataforma == 'ubuntu' and bytecode_externo:
        parser.error('La entrega de Ubuntu requiere el paquete de imágenes con sus tres bases de firmas.')
    services = {'postgres', 'backend', 'frontend', 'antivirus', 'mailpit'}
    if {key for key in manifest if key.startswith('image.')} != {
            key for service in services for key in (f'image.{service}', f'image.{service}.id')}:
        parser.error('Las imágenes no corresponden a los cinco servicios de Demo.')
    if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', manifest['app.version']):
        parser.error('Versión de la aplicación inválida.')
    for service in services:
        if not re.fullmatch(r'sha256:[0-9a-f]{64}', manifest[f'image.{service}.id']):
            parser.error(f'Identidad de imagen inválida para {service}.')
        if service in {'backend', 'frontend', 'antivirus'} and manifest[f'image.{service}'] != f"lamontana-demo-{service}:{manifest['app.version']}":
            parser.error(f'El servicio {service} requiere una imagen de Demo.')
    installer_version = args.version_instalador or manifest['app.version']
    if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', installer_version):
        parser.error('La versión del instalador debe tener formato mayor.menor.parche.')
    lock = properties(base / 'dependencias/versiones.properties')
    required = ['docker', 'navegador'] + (['wsl', 'ubuntu'] if args.plataforma == 'windows' else [])
    # Comprobar antes de crear la salida; nunca empaquetar recursos parciales.
    files = [(args.app / name, manifest['sha256.' + name]) for name in manifest['files'].split(',')]
    for component in required:
        key = f'dep.{args.plataforma}.{component}'
        files.append((args.dependencias / lock[key + '.file'], lock[key + '.sha256']))
        manifest.update({k: v for k, v in lock.items() if k.startswith(key + '.')})
    for source, expected in files:
        if digest(source) != expected:
            raise SystemExit('SHA-256 incorrecto: ' + source.name)
    if args.plataforma == 'windows':
        comprobar_firmas_externas(args.app / 'images.tar.gz', manifest['image.antivirus'])
        avisos = args.avisos_terceros or args.dependencias / 'AVISOS-TERCEROS.zip'
        if not avisos.is_file():
            parser.error('Falta AVISOS-TERCEROS.zip para Windows.')
        avisos_bytes = avisos.read_bytes()
        with zipfile.ZipFile(io.BytesIO(avisos_bytes)) as archive:
            nombres = archive.namelist()
            required_notices = {'componentes.sha256', 'Firefox/origen.tsv', 'Firefox/toolkit-license.html',
                                'Firefox/browser-license.html', 'Originales/WSL-3.0.3-NOTICE.txt',
                                'Inventarios/paquetes-ubuntu-wsl.tsv', 'Inventarios/paquetes-docker.tsv',
                                'Inventarios/imagenes.tsv', 'Inventarios/origen-atribuciones.tsv'}
            expected = {source.name: sha for source, sha in files}
            if len(nombres) != len(set(nombres)) or not required_notices.issubset(nombres) or archive.testzip():
                parser.error('Los textos, inventarios o sumas del ZIP de avisos están incompletos o dañados.')
            manifest_data = archive.read('componentes.sha256').decode('ascii').splitlines()
            if sorted(manifest_data) != sorted(f'{sha}  {name}' for name, sha in expected.items()):
                parser.error('Los avisos de terceros corresponden a otra carga.')
        navegador = re.fullmatch(r'firefox-([0-9]+(?:\.[0-9]+){1,2})\.exe', lock['dep.windows.navegador.file'])
        if not navegador:
            parser.error('Nombre inesperado para Firefox de Windows.')
    elif args.avisos_terceros:
        parser.error('--avisos-terceros sólo se usa para Windows.')
    args.salida.parent.mkdir(parents=True, exist_ok=True)
    if args.salida.exists():
        parser.error('La salida ya existe.')
    with tempfile.TemporaryDirectory(dir=args.salida.parent) as tmp:
        output = Path(tmp) / 'paquete'
        output.mkdir()
        for source, expected in files:
            shutil.copyfile(source, output / source.name)
            manifest['sha256.' + source.name] = expected
            manifest['size.' + source.name] = str(source.stat().st_size)
        recursos = [base / 'dependencias/docker-descargas.properties']
        if args.plataforma == 'windows':
            texto = (base / 'AVISOS-TERCEROS.txt').read_text().replace('@FIREFOX@', navegador[1])
            (output / 'AVISOS-TERCEROS.txt').write_text(texto, encoding='utf-8')
            (output / 'AVISOS-TERCEROS.zip').write_bytes(avisos_bytes)
        else:
            recursos.append(base / 'empaquetado/ubuntu/AVISOS-TERCEROS.txt')
        for source in recursos:
            shutil.copyfile(source, output / source.name)
        avisos_y_catalogo = ['AVISOS-TERCEROS.txt', 'docker-descargas.properties']
        if args.plataforma == 'windows':
            avisos_y_catalogo.append('AVISOS-TERCEROS.zip')
        for nombre in avisos_y_catalogo:
            recurso = output / nombre
            manifest['sha256.' + nombre] = digest(recurso)
            manifest['size.' + nombre] = str(recurso.stat().st_size)
        manifest['files'] = ','.join(sorted(p.name for p in output.iterdir()))
        manifest['offline.complete'] = 'false' if bytecode_externo else 'true'
        manifest['platform'] = args.plataforma + '-x64'
        manifest['installer.version'] = installer_version
        (output / 'release.properties').write_text(''.join(f'{k}={v}\n' for k, v in sorted(manifest.items())))
        output.rename(args.salida)
    size = sum(p.stat().st_size for p in args.salida.iterdir())
    print(f'Carga de instalación preparada: {size / 1024**2:.1f} MiB. Falta medir el instalador nativo con su runtime.')


if __name__ == '__main__':
    main()
