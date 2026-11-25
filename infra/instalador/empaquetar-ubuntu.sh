#!/usr/bin/env bash
set -euo pipefail
# Acotar la compresión evita picos de RAM en equipos de construcción modestos.
export DPKG_DEB_THREADS_MAX=1
base=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
paquete=$(realpath -- "${1:?Carpeta de paquete completo}")
salida=$(realpath -m -- "${2:?Carpeta nueva de instaladores}")
[[ ! -e $salida ]] || { echo 'La salida ya existe; no se reemplaza.' >&2; exit 73; }
[[ $(sed -n 's/^platform=//p' "$paquete/release.properties") == ubuntu-x64 ]] || exit 65
[[ $(sed -n 's/^offline.complete=//p' "$paquete/release.properties") == true ]] || exit 65
version=$(sed -n 's/^installer.version=//p' "$paquete/release.properties")
[[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || exit 65
"$base/build.sh"
mkdir -p "$salida"
# /tmp puede ser tmpfs: el paquete offline completo debe prepararse en disco.
temporal=$(mktemp -d "$salida/.construccion-XXXXXX")
export TMPDIR="$temporal"
trap 'rm -rf -- "$temporal"' EXIT
mkdir -p "$temporal/entrada" "$salida"
cp "$base/target/instalador.jar" "$temporal/entrada/"
cp "$base/empaquetado/ubuntu/apertura-ubuntu.sh" "$temporal/entrada/"
cp -R "$paquete" "$temporal/entrada/paquete"
"${JAVA_HOME:?Indicar JDK 17}/bin/jpackage" -J-Xmx512m --type deb --name LaMontanaInstalador --app-version "$version" \
 --vendor 'YG Soluciones' --description 'Instalación y mantenimiento local de La Montaña' \
 --input "$temporal/entrada" --main-jar instalador.jar --main-class ar.com.lamontana.instalador.Main --java-options '-Xmx256m' --resource-dir "$base/empaquetado/ubuntu" \
 --add-modules java.desktop,java.net.http,jdk.httpserver,jdk.crypto.ec \
 --dest "$salida" --temp "$temporal/jpackage" --linux-package-name lamontana-instalador --linux-shortcut --linux-menu-group Utility \
 --license-file "$base/AVISOS-TERCEROS.txt"
for file in "$salida/"*.deb; do
 (( $(stat -c %s "$file") < 2147483648 )) || { echo 'Supera 2 GiB; revisar distribución antes de publicar.' >&2; exit 65; }
 (cd "$salida" && sha256sum -- "${file##*/}" > "${file##*/}.sha256")
done
