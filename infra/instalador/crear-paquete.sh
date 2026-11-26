#!/usr/bin/env bash
set -euo pipefail
base=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
version=${1:?Indicar version mayor.menor.parche}
firmas=${2:?Indicar carpeta con firmas ClamAV verificadas}
destino=${3:?Indicar carpeta nueva de salida}
[[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || exit 64
[[ $# -eq 3 || ( $# -eq 4 && $4 == --desde-demo-en-ejecucion ) ]] || exit 64
[[ ! -e $destino ]] || { echo 'La salida ya existe; no se sobrescribe.' >&2; exit 73; }
mkdir -p -- "$(dirname -- "$destino")"
contexto=$(mktemp -d)
salida=$(mktemp -d -- "${destino}.tmp.XXXXXXXX")
trap 'rm -rf -- "$contexto" "$salida"' EXIT

for servicio in backend frontend; do
  etiqueta="lamontana-demo-$servicio:$version"
  if [[ ${4:-} == --desde-demo-en-ejecucion ]]; then
    # Copiar la imagen exacta que sirve la Demo, sin volúmenes, datos ni entorno del contenedor.
    id=$(docker ps -q --filter label=com.docker.compose.project=lamontana-demo --filter "label=com.docker.compose.service=$servicio" --filter health=healthy)
    [[ -n $id && $id != *$'\n'* ]] || { echo "Se necesita un único $servicio sano de Demo." >&2; exit 1; }
    docker tag "$(docker inspect --format '{{.Image}}' "$id")" "$etiqueta"
  else
    docker build --platform linux/amd64 -t "$etiqueta" "$base/$servicio"
  fi
done
cp "$base/infra/instalador/antivirus/Dockerfile" "$base/infra/instalador/antivirus/arrancar.sh" "$contexto/"
cp "$base/infra/antivirus/clamd.conf" "$contexto/clamd.conf"
mkdir "$contexto/firmas"
for nombre in main daily bytecode; do
  if [[ -f "$firmas/$nombre.cld" ]]; then extension=cld; else extension=cvd; fi
  cp -- "$firmas/$nombre.$extension" "$contexto/firmas/"
done
docker build --platform linux/amd64 -t "lamontana-demo-antivirus:$version" "$contexto"
services=(postgres backend frontend antivirus mailpit)
images=(postgres:18.6-alpine "lamontana-demo-backend:$version" "lamontana-demo-frontend:$version" "lamontana-demo-antivirus:$version" axllent/mailpit:v1.31.0)
for image in "${images[@]}"; do
  [[ $(docker image inspect --format '{{.Os}}/{{.Architecture}}' "$image") == linux/amd64 ]] || { echo "Falta la imagen linux/amd64 de $image." >&2; exit 1; }
done
docker save "${images[@]}" | gzip -1 > "$salida/images.tar.gz"
{
  printf 'edition=DEMO\napp.version=%s\ninstaller.version=%s\noffline.complete=false\nfiles=images.tar.gz\n' "$version" "$version"
  printf 'sha256.images.tar.gz=%s\n' "$(sha256sum "$salida/images.tar.gz" | cut -d' ' -f1)"
  for i in "${!images[@]}"; do
    printf 'image.%s=%s\n' "${services[$i]}" "${images[$i]}"
    printf 'image.%s.id=%s\n' "${services[$i]}" "$(docker image inspect --format '{{.Id}}' "${images[$i]}")"
  done
} > "$salida/release.properties"
mv -T -- "$salida" "$destino"
printf 'Paquete de aplicación Demo creado en %s.\n' "$destino"
