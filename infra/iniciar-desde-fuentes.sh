#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

command -v docker >/dev/null || { echo 'Falta instalar Docker.' >&2; exit 1; }
docker compose version >/dev/null
if ! docker info >/dev/null 2>&1; then
  echo 'Docker no está accesible. Verificá que el servicio esté iniciado y que esta terminal tenga permisos; no se modificó la instalación.' >&2
  exit 1
fi

compose=(docker compose -p lamontana-demo -f compose.yaml --env-file .env)

(
  umask 077
  exec 9>>.env
  flock -x 9
  chmod 600 .env
  if ! grep -q '^DEMO_DB_PASSWORD=.' .env; then
    db_secret=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')
    printf '\nDEMO_DB_PASSWORD=%s\n' "$db_secret" >&9
    echo 'Se creó una credencial técnica aleatoria para el PostgreSQL de Demo en .env.'
  fi
)

# Compose interpreta .env; no se ejecuta su contenido como código de shell.
web_source_path=$("${compose[@]}" config --environment | sed -n 's/^DEMO_WEB_SOURCE_DIR=//p')
web_source_path=${web_source_path:-"$PWD/datos/demo/imagenes-web/entrada"}
if [[ "$web_source_path" != /* ]]; then
  echo 'DEMO_WEB_SOURCE_DIR debe ser una ruta absoluta. Corregí .env antes de iniciar.' >&2
  exit 1
fi
web_source_path=$(realpath -m -- "$web_source_path")
mkdir -p -- "$web_source_path"
export DEMO_WEB_SOURCE_DIR="$web_source_path"
echo "Carpeta de entrada de imágenes web: $web_source_path"

"${compose[@]}" config --quiet
"${compose[@]}" up --build --detach --wait
echo 'Demo iniciada. Consultá los puertos publicados con: docker compose -p lamontana-demo ps'
echo 'Puertos predeterminados de Demo: aplicación http://localhost:3200 · correo local http://localhost:8225'
