#!/usr/bin/env bash
set -euo pipefail
ruta=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../.." && pwd)
source "$ruta/empaquetado/ubuntu/apertura-ubuntu.sh"
tmp=$(mktemp -d)
trap 'rm -rf -- "$tmp"' EXIT
# APT exitoso, error, transacción anterior y ejecución de otro usuario.
cat > "$tmp/historial" <<'LOG'
Start-Date: 2026-10-14  13:18:22
Commandline: apt install /tmp/mi paquete.deb
Requested-By: prueba (1000)
Upgrade: lamontana-instalador:amd64 (0.2.2-1, 0.2.3-1)
End-Date: 2026-10-14  13:18:26
LOG
fecha=$(date -d '2026-10-14 13:18:22' +%s)
historial_correcto 'apt install /tmp/mi paquete.deb' 1000 "$fecha" "$fecha" "$tmp/historial"
if historial_correcto 'apt install /tmp/otro.deb' 1000 "$fecha" "$fecha" "$tmp/historial"; then exit 1; fi
if historial_correcto 'apt install /tmp/mi paquete.deb' 1001 "$fecha" "$fecha" "$tmp/historial"; then exit 1; fi
if historial_correcto 'apt install /tmp/mi paquete.deb' 1000 "$((fecha+10))" "$((fecha+10))" "$tmp/historial"; then exit 1; fi
cp "$tmp/historial" "$tmp/previo"
sed -i '/End-Date/i Error: Sub-process /usr/bin/dpkg returned an error code (1)' "$tmp/historial"
if historial_correcto 'apt install /tmp/mi paquete.deb' 1000 "$fecha" "$fecha" "$tmp/historial"; then exit 1; fi
cat "$tmp/previo" "$tmp/historial" > "$tmp/combinado"
if historial_correcto 'apt install /tmp/mi paquete.deb' 1000 "$fecha" "$fecha" "$tmp/combinado"; then exit 1; fi
sed -i '/Error:/d; /End-Date/d' "$tmp/historial"
if historial_correcto 'apt install /tmp/mi paquete.deb' 1000 "$fecha" "$fecha" "$tmp/historial"; then exit 1; fi
# Sesiones simuladas: una gráfica local, ninguna, ambigüedad y SSH.
caso=normal
loginctl(){
 if [[ $1 == list-sessions ]]; then
  printf '%s\n' '2 1000 prueba seat0' '3 1000 prueba -'
 else
  printf '%s\n' 'User=1000' 'Active=yes' 'Class=user'
  [[ $caso == remota ]] && echo Remote=yes || echo Remote=no
  if [[ $2 == 2 || $caso == ambigua ]]; then echo Type=wayland;else echo Type=tty;fi
 fi
}
[[ $(sesion_grafica 1000) == 2 ]]
if sesion_grafica 1001; then exit 1; fi
caso=ambigua;if sesion_grafica 1000; then exit 1; fi
caso=remota;if sesion_grafica 1000; then exit 1; fi
# No debe programar nada ni consultar sesiones en ejecución desatendida/SSH.
sesion_grafica(){ echo 'No debió consultarse la sesión' >&2;return 99; }
DEBIAN_FRONTEND=noninteractive programar
SSH_CONNECTION='prueba' programar
proceso $$
[[ ${proc[19]} =~ ^[0-9]+$ ]]
printf '%s\n' 'OK: apertura omite transacciones fallidas, incompletas, ajenas y sesiones remotas/ambiguas.'

# Una instancia de una actualización anterior no bloquea una transacción nueva.
# Reintentar el mismo evento conserva una sola apertura. No lanza nada real.
activas=(lamontana-asistente.service)
aperturas=()
runuser(){
 local argumento unidad= existente
 for argumento in "$@"; do
  case $argumento in
   lamontana-asistente-*.service)
    for existente in "${activas[@]}"; do [[ $existente == "$argumento" ]] && return 0; done
    return 3;;
   --unit=*) unidad=${argumento#--unit=};;
  esac
 done
 [[ -n $unidad ]] || return 99
 activas+=("$unidad.service");aperturas+=("$unidad")
}
lanzar_en_sesion prueba 1000 /home/prueba 101-200-1000
lanzar_en_sesion prueba 1000 /home/prueba 101-200-1000
lanzar_en_sesion prueba 1000 /home/prueba 102-300-1000
[[ ${#aperturas[@]} == 2 ]]
[[ ${aperturas[0]} == lamontana-asistente-101-200-1000 && ${aperturas[1]} == lamontana-asistente-102-300-1000 ]]
printf '%s\n' 'OK: una actualización abre su asistente aunque exista una instancia anterior; el mismo evento no se duplica.'
