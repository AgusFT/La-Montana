#!/usr/bin/env python3
"""Renueva los avisos al cambiar únicamente el instalador oficial de Firefox.

Para la primera entrega con avisos (instalador 0.1.1), partir de su EXE verificado.
En la carpeta de instalación, app/paquete/AVISOS-TERCEROS.zip debe coincidir con
sha256.AVISOS-TERCEROS.zip en release.properties. Su SHA-256 es
8e59513e7a86b0672145702bea1fdee0aae03f94194d2a3cca49d2556aec09cd.

Conservar ese ZIP en el archivo de entregas. Pasarlo como primer argumento
junto al Firefox oficial y a SHA256SUMS del mismo directorio de versión de
archive.mozilla.org. Con los mismos insumos se reconstruye el ZIP original.
Al renovar Firefox, usar el ZIP verificado de la entrega anterior. Si cambia
otra dependencia, regenerar sus atribuciones y el inventario antes de empaquetar.
"""
import argparse
import hashlib
import io
from pathlib import Path
import re
import struct
import subprocess
import tempfile
import zipfile


def licencia(exe, omni, ruta):
    datos = subprocess.check_output(['7z', 'e', '-so', exe, omni])
    # Los omni.ja optimizados de Mozilla sitúan el directorio ZIP antes de los
    # datos. Repetirlo al final permite a zipfile validar cada CRC al leerlo.
    if datos[4:8] == b'PK\x01\x02' and datos[-22:-18] == b'PK\x05\x06':
        largo, inicio = struct.unpack_from('<II', datos, len(datos) - 10)
        if inicio != 4 or datos[inicio + largo:inicio + largo + 4] != b'PK\x05\x06':
            raise ValueError('Directorio ZIP de Firefox inconsistente.')
        pie = bytearray(datos[-22:])
        struct.pack_into('<I', pie, 16, len(datos))
        datos += datos[inicio:inicio + largo] + pie
    with zipfile.ZipFile(io.BytesIO(datos)) as archive:
        return archive.read(ruta)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('anterior', type=Path, help='AVISOS-TERCEROS.zip de una entrega verificada')
    parser.add_argument('navegador', type=Path, help='firefox-VERSION.exe identificado por el lock de Windows')
    parser.add_argument('sumas_mozilla', type=Path, help='SHA256SUMS oficial de esa versión')
    parser.add_argument('salida', type=Path, help='Ruta nueva de AVISOS-TERCEROS.zip')
    args = parser.parse_args()
    version = re.fullmatch(r'firefox-([0-9]+(?:\.[0-9]+){1,2})\.exe', args.navegador.name)
    if not version or args.salida.exists():
        parser.error('Indicá un Firefox versionado y una salida nueva.')
    version = version[1]
    src = f'source/firefox-{version}.source.tar.xz'
    publica = f'win64/es-AR/Firefox Setup {version}.exe'
    sumas = {}
    for linea in args.sumas_mozilla.read_text().splitlines():
        sha, ruta = linea.split(None, 1)
        sumas[ruta] = sha
    with args.navegador.open('rb') as fp:
        sha = hashlib.file_digest(fp, 'sha256').hexdigest()
    if sumas.get(publica) != sha or not re.fullmatch('[0-9a-f]{64}', sumas.get(src, '')):
        parser.error('La edición es-AR x64 o la fuente de Firefox difiere del manifiesto de Mozilla.')
    args.salida.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.anterior) as anterior, tempfile.TemporaryDirectory(dir=args.salida.parent) as temp:
        cargas = anterior.read('componentes.sha256').decode('ascii').splitlines()
        sin_firefox = [c for c in cargas if not re.fullmatch(r'[0-9a-f]{64}  firefox-[0-9.]+\.exe', c)]
        if len(cargas) != 5 or len(sin_firefox) != 4:
            parser.error('El archivo de avisos anterior no corresponde al instalador de Windows.')
        nuevas = sorted([*sin_firefox, sha + '  ' + args.navegador.name])
        generado = Path(temp) / 'AVISOS-TERCEROS.zip'
        with zipfile.ZipFile(generado, 'w', zipfile.ZIP_DEFLATED) as nuevo:
            def agregar(nombre, datos):
                if isinstance(datos, str):
                    datos = datos.encode('utf-8')
                if nombre in anterior.namelist() and anterior.read(nombre) == datos:
                    # Conserva los metadatos del ZIP para reconstruir la misma entrega.
                    info = anterior.getinfo(nombre)
                else:
                    info = zipfile.ZipInfo(nombre)
                    info.compress_type = zipfile.ZIP_DEFLATED
                nuevo.writestr(info, datos)

            for item in anterior.infolist():
                if item.filename != 'componentes.sha256' and not item.filename.startswith('Firefox/'):
                    nuevo.writestr(item, anterior.read(item))
            agregar('componentes.sha256', '\n'.join(nuevas) + '\n')
            for nombre, omni, ruta in [('toolkit', 'core/omni.ja', 'chrome/toolkit/content/global/license.html'),
                                       ('browser', 'core/browser/omni.ja', 'chrome/browser/content/browser/license.html')]:
                agregar(f'Firefox/{nombre}-license.html', licencia(args.navegador, omni, ruta))
            url = f'https://archive.mozilla.org/pub/firefox/releases/{version}/'
            agregar('Firefox/origen.tsv', f'archivo\tsha256\torigen\n{args.navegador.name}\t{sha}\t{url}{publica.replace(" ", "%20")}\n'
                           f'{Path(src).name}\t{sumas[src]}\t{url}{src}\n')
        generado.rename(args.salida)


if __name__ == '__main__':
    main()
