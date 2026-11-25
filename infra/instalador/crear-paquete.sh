#!/usr/bin/env bash
set -euo pipefail
base=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
version=${1:?Indicar version mayor.menor.parche}
firmas=${2:?Indicar carpeta con firmas ClamAV verificadas}
destino=${3:?Indicar carpeta nueva de salida}
[[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || exit 64
[[ ! -e $destino ]] || { echo 'La salida ya existe; no se sobrescribe.' >&2; exit 73; }
mkdir -p -- "$destino"
destino=$(cd -- "$destino" && pwd)
docker build -t "lamontana-backend:$version" "$base/backend"
docker build -t "lamontana-frontend:$version" "$base/frontend"
contexto=$(mktemp -d)
trap 'rm -rf -- "$contexto"' EXIT
cp "$base/infra/instalador/antivirus/"* "$contexto/"
cp "$base/infra/antivirus/clamd.conf" "$contexto/clamd.conf"
mkdir "$contexto/firmas"
# Solo firmas oficiales; no copiar registros ni otros datos del volumen.
find "$firmas" -maxdepth 1 -type f \( -name '*.cvd' -o -name '*.cld' \) -exec cp -- '{}' "$contexto/firmas/" \;
[[ -n $(find "$contexto/firmas" -type f -print -quit) ]] || { echo 'Faltan firmas ClamAV.' >&2; exit 66; }
docker build -t "lamontana-antivirus:$version" "$contexto"
postgres=postgres:18.6-alpine
mailpit=axllent/mailpit:v1.31.0
proxy=caddy:2.10.2-alpine
for image in "$postgres" "$mailpit" "$proxy"; do docker image inspect "$image" >/dev/null 2>&1 || docker pull "$image"; done
images=("$postgres" "lamontana-backend:$version" "lamontana-frontend:$version" "lamontana-antivirus:$version" "$mailpit" "$proxy")
services=(postgres backend frontend antivirus mailpit proxy)
docker save "${images[@]}" | gzip -1 > "$destino/images.tar.gz"
{
  printf 'app.version=%s\ninstaller.version=0.1.0\nplatform=ubuntu-x64\noffline.complete=false\nfiles=images.tar.gz\n' "$version"
  printf 'sha256.images.tar.gz=%s\n' "$(sha256sum "$destino/images.tar.gz" | cut -d' ' -f1)"
  for i in "${!images[@]}"; do
    printf 'image.%s=%s\n' "${services[$i]}" "${images[$i]}"
    printf 'image.%s.id=%s\n' "${services[$i]}" "$(docker image inspect --format '{{.Id}}' "${images[$i]}")"
  done
} > "$destino/release.properties"
printf 'Paquete de aplicación creado: %s\nFalta incorporar dependencias de plataforma antes de empaquetar el instalador offline.\n' "$destino"
