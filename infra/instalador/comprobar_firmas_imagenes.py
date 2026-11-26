"""Comprueba las capas entregadas por docker save, incluidas las ocultas en ejecución."""
import json
import tarfile
from pathlib import PurePosixPath


def comprobar_firmas_externas(archivo, imagen_antivirus):
    with tarfile.open(archivo, "r|*") as exterior:
        for miembro in exterior:
            if miembro.isfile() and miembro.name.removeprefix("./") == "manifest.json":
                imagenes = json.load(exterior.extractfile(miembro))
                break
        else:
            raise ValueError("Falta el manifiesto de docker save.")

    capas = {ruta for imagen in imagenes for ruta in imagen["Layers"]}
    antivirus = [imagen for imagen in imagenes if imagen_antivirus in (imagen["RepoTags"] or [])]
    if len(antivirus) != 1 or not antivirus[0]["Layers"]:
        raise ValueError("Faltan las capas declaradas del antivirus.")
    capas_antivirus = set(antivirus[0]["Layers"])
    bases = set()
    with tarfile.open(archivo, "r|*") as exterior:
        for miembro in exterior:
            ruta = miembro.name.removeprefix("./")
            if ruta not in capas:
                continue
            if not miembro.isfile():
                raise ValueError(f"Capa inválida del archivo de imágenes: {ruta}")
            with tarfile.open(fileobj=exterior.extractfile(miembro), mode="r|*") as capa:
                for entrada in capa:
                    path = PurePosixPath(entrada.name)
                    if path.name.lower() in {"bytecode.cvd", "bytecode.cld"}:
                        raise ValueError("Una capa contiene la base de bytecode que se obtiene al instalar.")
                    if (ruta in capas_antivirus and entrada.isfile() and
                            path.parent == PurePosixPath("opt/lamontana-firmas") and
                            path.suffix in {".cvd", ".cld"}):
                        bases.add(path.stem)
            capas.remove(ruta)
    if capas or bases != {"main", "daily"}:
        raise ValueError("No se pudieron comprobar todas las capas y las bases main y daily del antivirus.")
