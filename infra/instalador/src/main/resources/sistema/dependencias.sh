#!/usr/bin/env bash
set -euo pipefail
accion=${1:?}; recurso=${2:-}
[[ $EUID == 0 ]] || exit 77
. /etc/os-release
[[ $ID == ubuntu && $VERSION_ID == 26.04 && $(uname -m) == x86_64 ]] || exit 78
case "$accion" in
  docker)
    [[ -f $recurso || -d $recurso ]] || exit 66
    destino=$(mktemp -d)
    trap 'rm -rf -- "$destino"' EXIT
    # Archivo del release comprobado antes de elevar; no se admiten rutas que escapen.
    if [[ -d $recurso ]]; then
      cp -- "$recurso/"*.deb "$destino/"
    else
    if tar -tzf "$recurso" | awk '/^\// || /(^|\/)\.\.(\/|$)/ {bad=1} END {exit !bad}'; then exit 65; fi
    tar --no-same-owner -xzf "$recurso" -C "$destino"
    fi
    mapfile -d '' paquetes < <(find "$destino" -type f -name '*.deb' -print0)
    (( ${#paquetes[@]} > 0 )) || exit 65
    # El inventario completo permite destinos mínimos, pero no debe forzar la
    # actualización de toda la base: WSL puede tener paquetes ligados a ella.
    for paquete in "${paquetes[@]}"; do
      dpkg-deb -f "$paquete"
      printf 'Filename: ./%s\nSize: %s\nSHA256: %s\n\n' "$(basename -- "$paquete")" "$(stat -c %s "$paquete")" "$(sha256sum "$paquete" | cut -d ' ' -f 1)"
    done > "$destino/Packages"
    printf 'deb [trusted=yes] file:%s ./\n' "$destino" > "$destino/origen.list"
    mkdir "$destino/listas"
    opciones=(-o "Dir::Etc::sourcelist=$destino/origen.list" -o Dir::Etc::sourceparts=- -o "Dir::State::lists=$destino/listas" -o "Dir::Cache::archives=$destino")
    apt-get "${opciones[@]}" update
    apt-get "${opciones[@]}" --no-download --no-remove --no-install-recommends -y install docker-ce docker-ce-cli containerd.io docker-compose-plugin docker-buildx-plugin systemd-sysv iptables
    systemctl enable --now docker
    # El usuario reingresa a su sesión para activar el grupo; nunca chmod 666 al socket.
    if [[ -n ${PKEXEC_UID:-} && $PKEXEC_UID != 0 ]]; then usermod -aG docker "$(id -un "$PKEXEC_UID")"; fi
    ;;
  iniciar) systemctl start docker ;;
  navegador)
    [[ -f $recurso ]] || exit 66
    destino=/opt/lamontana-demo-firefox
    [[ ! -e $destino ]] || exit 73
    mkdir -m 755 "$destino"
    tar --no-same-owner -xf "$recurso" -C "$destino" --strip-components=1
    chmod -R a+rX "$destino"
    ;;
  *) exit 64 ;;
esac
