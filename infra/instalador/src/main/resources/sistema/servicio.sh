#!/usr/bin/env bash
set -euo pipefail
accion=${1:?}; raiz=${2:?}; proyecto=${3:?}; bind=${4:?}; puerto=${5:?}
[[ $EUID == 0 && $proyecto =~ ^lamontana-[a-f0-9]{12}$ && $puerto =~ ^[0-9]+$ ]] || exit 64
[[ $raiz == /* && -f $raiz/instalacion.properties && $raiz != *$'\n'* ]] || exit 65
[[ $bind == 127.0.0.1 || $bind == 0.0.0.0 || $bind =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || exit 65
case "$accion" in
 arranque)
  # Standard exige el motor de sistema; no se transforma Docker Desktop.
  [[ -S /var/run/docker.sock ]] || { echo 'Falta Docker Engine de sistema.' >&2; exit 69; }
  uid=${PKEXEC_UID:?No se pudo identificar el usuario solicitante}
  [[ $uid != 0 ]] || exit 77
  usuario=$(id -un "$uid")
  base="/var/lib/lamontana-servicios/$proyecto"
  install -d -m 755 "$base"
  {
   printf '#!/bin/bash\nset -euo pipefail\n'
   printf 'raiz=%q\nproyecto=%q\n' "$raiz" "$proyecto"
   cat <<'START'
estado=$(sed -n 's/^estado=//p' "$raiz/instalacion.properties")
case "$estado" in LISTA|PUBLICACION_PENDIENTE|VERIFICACION_PUBLICA_PENDIENTE) ;; *) exit 0 ;; esac
/usr/bin/docker --host unix:///var/run/docker.sock compose --project-name "$proyecto" --project-directory "$raiz" --env-file "$raiz/.env" -f "$raiz/compose.yaml" --profile standard up -d --wait --wait-timeout 600 --pull never
umask 077
printf 'boot.id=%s\n' "$(cat /proc/sys/kernel/random/boot_id)" > "$raiz/arranque.properties"
START
  } > "$base/arrancar.sh"
  chmod 755 "$base/arrancar.sh"
  cat > "/etc/systemd/system/$proyecto.service" <<UNIT
[Unit]
Description=La Montana ($proyecto)
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target
[Service]
Type=oneshot
RemainAfterExit=yes
User=$usuario
SupplementaryGroups=docker
ExecStart=$base/arrancar.sh
TimeoutStartSec=720
[Install]
WantedBy=multi-user.target
UNIT
  systemctl enable docker.service
  systemctl daemon-reload
  systemctl enable "$proyecto.service"
  ;;
 red)
  # UFW es la interfaz soportada; no desactivar ni sustituir otro firewall.
  command -v ufw >/dev/null || { echo 'Configurar manualmente el firewall del equipo.' >&2; exit 69; }
  if [[ $bind == 0.0.0.0 ]]; then ufw allow 80/tcp comment "$proyecto"; ufw allow 443/tcp comment "$proyecto"
  else
   [[ $bind != 127.0.0.1 ]] || exit 64
   ruta=$(ip -o -4 route show scope link | awk -v address="$bind" '$0 ~ "src "address"( |$)" {print $1;exit}')
   [[ -n $ruta ]] || exit 65
   ufw allow from "$ruta" to "$bind" port "$puerto" proto tcp comment "$proyecto"
  fi
  ;;
 *) exit 64 ;;
esac
