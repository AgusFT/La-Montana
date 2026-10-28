#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."

[[ -f .env.pruebas ]] || { echo 'Primero ejecutá ./infra/pruebas/iniciar.sh' >&2; exit 1; }
project=lamontana-pruebas
[[ "$project" == 'lamontana-pruebas' ]] || exit 1
docker compose --project-name "$project" --env-file .env.pruebas -f compose.yaml -f compose.pruebas.yaml down --volumes
exec ./infra/pruebas/iniciar.sh
