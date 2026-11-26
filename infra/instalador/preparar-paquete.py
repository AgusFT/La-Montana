#!/usr/bin/env python3
"""Preparación de release (solo construcción; Python no se instala en el cliente)."""
import argparse
import hashlib
import re
from pathlib import Path
import shutil
import tempfile


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
    args = parser.parse_args()
    base = Path(__file__).resolve().parent
    manifest = properties(args.app / 'release.properties')
    if manifest.get('edition') != 'DEMO' or manifest.get('files') != 'images.tar.gz':
        parser.error('Se requiere el archivo de imágenes de la edición Demo.')
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
        for source in [base / 'dependencias/docker-descargas.properties', base / 'AVISOS-TERCEROS.txt']:
            shutil.copyfile(source, output / source.name)
            manifest['sha256.' + source.name] = digest(source)
            manifest['size.' + source.name] = str(source.stat().st_size)
        manifest['files'] = ','.join(sorted(p.name for p in output.iterdir()))
        manifest['offline.complete'] = 'true'
        manifest['platform'] = args.plataforma + '-x64'
        manifest['installer.version'] = installer_version
        (output / 'release.properties').write_text(''.join(f'{k}={v}\n' for k, v in sorted(manifest.items())))
        output.rename(args.salida)
    size = sum(p.stat().st_size for p in args.salida.iterdir())
    print(f'Paquete completo preparado: {size / 1024**2:.1f} MiB. Falta medir el instalador nativo con su runtime.')


if __name__ == '__main__':
    main()
