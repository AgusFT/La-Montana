#!/usr/bin/env python3
"""Comprueba el Firefox empaquetado antes de ofrecer la Demo para Windows."""
import argparse
import hashlib
import json
import re
from pathlib import Path
from urllib.request import urlopen


def verificar(paquete):
    manifiesto = dict(linea.split("=", 1) for linea in
                      (paquete / "release.properties").read_text().splitlines()
                      if linea and not linea.startswith("#"))
    if (manifiesto.get("edition"), manifiesto.get("platform")) != ("DEMO", "windows-x64"):
        raise ValueError("Se requiere la carga de la Demo para Windows x64.")
    with urlopen("https://product-details.mozilla.org/1.0/firefox_versions.json", timeout=25) as respuesta:
        version = json.load(respuesta)["LATEST_FIREFOX_VERSION"]
    if not re.fullmatch(r"[0-9]+(?:\.[0-9]+){1,2}", version):
        raise ValueError("Mozilla devolvió una versión estable inesperada.")
    nombre = f"firefox-{version}.exe"
    if manifiesto.get("dep.windows.navegador.file") != nombre:
        raise ValueError(f"La versión estable es Firefox {version}. Esta carga requiere renovación.")
    origen = f"https://archive.mozilla.org/pub/firefox/releases/{version}/"
    ruta = f"win64/es-AR/Firefox Setup {version}.exe"
    sumas = []
    with urlopen(origen + "SHA256SUMS", timeout=25) as respuesta:
        for linea in respuesta:
            partes = linea.decode().rstrip().split(maxsplit=1)
            if len(partes) == 2 and partes[1] == ruta:
                sumas.append(partes[0])
    if len(sumas) != 1 or not re.fullmatch(r"[0-9a-f]{64}", sumas[0]):
        raise ValueError("No se pudo identificar la suma oficial del instalador es-AR.")
    with (paquete / nombre).open("rb") as archivo:
        suma = hashlib.file_digest(archivo, "sha256").hexdigest()
    if suma != sumas[0] or suma != manifiesto.get("sha256." + nombre):
        raise ValueError("El archivo, el manifiesto y la suma de Mozilla difieren.")
    print(f"Firefox estable {version}, Windows x64 es-AR, SHA-256 {suma}.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paquete", type=Path, help="Carpeta con release.properties y la carga Windows.")
    args = parser.parse_args()
    try:
        verificar(args.paquete)
    except (OSError, ValueError, KeyError) as error:
        parser.exit(1, f"No se pudo validar la publicación: {error}\n")
