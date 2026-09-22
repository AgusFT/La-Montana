#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

command -v docker >/dev/null || { echo 'Falta instalar Docker.' >&2; exit 1; }
docker compose version >/dev/null
if ! docker info >/dev/null 2>&1; then
  echo 'Docker no está accesible. Verificá que el servicio esté iniciado y que esta terminal tenga permisos; no se modificó la instalación.' >&2
  exit 1
fi

if [[ ! -e .env ]]; then
  (
    umask 077
    set -o noclobber
    db_secret=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')
    printf 'DB_PASSWORD=%s\n' "$db_secret" > .env
  )
  echo 'Se creó .env con una credencial técnica aleatoria para PostgreSQL.'
fi

if grep -q '^SETUP_TOKEN=$' .env; then
  sed -i '/^SETUP_TOKEN=$/d' .env
fi
if ! grep -q '^SETUP_TOKEN=' .env; then
  (
    umask 077
    setup_secret=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')
    printf 'SETUP_TOKEN=%s\n' "$setup_secret" >> .env
  )
  chmod 600 .env
  echo 'Token de alta inicial creado en .env. Abrí ese archivo localmente para usarlo en /instalacion.'
fi

docker compose config --quiet
docker compose up --build --detach --wait
echo 'Base técnica iniciada. Consultá los puertos publicados con: docker compose ps'
echo 'Puertos predeterminados: aplicación http://localhost:3000 · correo local http://localhost:8025'
