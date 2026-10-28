#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."

command -v docker >/dev/null || { echo 'Falta instalar Docker.' >&2; exit 1; }
docker compose version >/dev/null
docker info >/dev/null 2>&1 || { echo 'Docker no está accesible.' >&2; exit 1; }

env_file=.env.pruebas
if [[ ! -e "$env_file" ]]; then
  (
    umask 077
    db_secret=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')
    setup_secret=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')
    web_source=$(realpath -m -- "$PWD/datos-pruebas/imagenes-web/entrada")
    printf 'DB_PASSWORD=%s\nSETUP_TOKEN=%s\nTEST_USER_PASSWORD=MontanaPrueba2026!\nPOSTGRES_PORT=55432\nBACKEND_PORT=8180\nFRONTEND_PORT=3100\nMAILPIT_PORT=8125\nWEB_SOURCE_DIR=%s\n' "$db_secret" "$setup_secret" "$web_source" > "$env_file"
  )
  chmod 600 "$env_file"
  echo 'Se creó .env.pruebas con credenciales exclusivas del entorno de prueba.'
fi

web_source=$(sed -n 's/^WEB_SOURCE_DIR=//p' "$env_file")
[[ "$web_source" == /* ]] || { echo 'WEB_SOURCE_DIR de pruebas debe ser absoluto.' >&2; exit 1; }
mkdir -p -- "$web_source"

docker compose --project-name lamontana-pruebas --env-file "$env_file" -f compose.yaml -f compose.pruebas.yaml up --build --detach --wait
echo 'Entorno de pruebas listo: http://localhost:3100/pruebas'
echo 'Correo local de pruebas: http://localhost:8125'
