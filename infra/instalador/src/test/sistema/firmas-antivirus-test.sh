#!/usr/bin/env bash
set -euo pipefail
# El ensayo usa únicamente archivos oficiales del operador, sin incorporarlos al repositorio.
imagen=${1:?imagen local de ClamAV de la Demo}
oficial=$(realpath "${2:?ruta al bytecode.cvd oficial}")
otra=$(realpath "${3:?ruta a una base main.cvd oficial}")
base=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)
tmp=$(mktemp -d "${PWD}/.prueba-antivirus-XXXXXXXX")
vol="lamontana-firmas-prueba-$(basename "$tmp" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9')"
trap 'docker volume rm "$vol" >/dev/null; rm -rf -- "$tmp"' EXIT
docker volume create "$vol" >/dev/null
args=(run --rm --pull never --network none -e LAMONTANA_BYTECODE_EXTERNO=1 --entrypoint /bin/sh
      -v "$vol:/var/lib/clamav" -v "$tmp:/intercambio" -v "$base/antivirus/arrancar.sh:/probar.sh:ro")
ejecutar(){ docker "${args[@]}" "$imagen" /probar.sh preparar-firmas "$1"; }
rechazar(){
    local rc=0
    ejecutar "$1" || rc=$?
    [[ $rc == 21 ]] || { echo "Se esperaba rechazo de firma/categoría; salida $rc." >&2; exit 1; }
}
rechazar guardada
cp "$oficial" "$tmp/candidato.cvd"
ejecutar archivo
cmp "$oficial" "$tmp/validado.cvd"
cp "$otra" "$tmp/candidato.cvd"
rechazar archivo
head -c 250 "$oficial" > "$tmp/candidato.cvd"
rechazar archivo
ejecutar guardada
cmp "$oficial" "$tmp/validado.cvd"
echo 'OK: CVD oficial, categoría ajena, truncado y reintento offline preservando la copia válida.'
