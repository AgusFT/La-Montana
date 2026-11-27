"""Actualiza contenidos y capturas desde una copia local del sitio de YG."""
from argparse import ArgumentParser
from pathlib import Path
from shutil import copyfile
import re

parser = ArgumentParser(description=__doc__)
parser.add_argument("sitio_yg", type=Path)
args = parser.parse_args()
frontend = Path(__file__).resolve().parents[1]
ruta = Path("src/app/manual-de-usuario")
imagenes = Path("public/manual/la-montana")
origen = args.sitio_yg / ruta
datos = ("tipos", "imagenes", "orientacion", "cliente", "operacion", "administracion")
archivos = set(re.findall(r"""['"]([\w.-]+\.webp)['"]""", (origen / "imagenes.ts").read_text()))
if not archivos or not all((args.sitio_yg / imagenes / nombre).is_file() for nombre in archivos):
    parser.error("El inventario de capturas de YG está incompleto.")
for modulo in datos:
    copyfile(origen / (modulo + ".ts"), frontend / ruta / (modulo + ".ts"))
for nombre in sorted(archivos):
    copyfile(args.sitio_yg / imagenes / nombre, frontend / imagenes / nombre)
print(f"Se actualizaron {len(archivos)} capturas y {len(datos)} módulos de contenido.")
