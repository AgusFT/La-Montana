#!/usr/bin/env bash
# Invocado exclusivamente por el postinst o por una tarea transitoria root.
# No inicia interfaces ni instala componentes dentro de la transacción de APT.
set -euo pipefail
export PATH=/usr/sbin:/usr/bin:/sbin:/bin
base=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
estado=/run/lamontana-instalador-apertura
paquete=lamontana-instalador
lanzador=/opt/lamontana-instalador/bin/LaMontanaInstalador
aviso(){ printf '%s\n' "La Montaña: $*. Podés abrir LaMontanaInstalador desde el menú."; }

proceso(){
 local linea
 [[ -r /proc/$1/stat ]] || return 1
 linea=$(cat "/proc/$1/stat")
 read -r -a proc <<< "${linea##*) }"
 [[ ${#proc[@]} -ge 20 ]]
}
# Una sola sesión gráfica local activa; no se elige un escritorio ajeno/ambiguo.
sesion_grafica(){
 local id resto info matches=()
 while read -r id resto; do
  [[ $id =~ ^[a-zA-Z0-9_-]+$ ]] || continue
  info=$(loginctl show-session "$id" -p User -p Type -p Remote -p Active -p Class --no-pager 2>/dev/null) || continue
  if grep -qx "User=$1" <<< "$info" && grep -qx 'Remote=no' <<< "$info" && grep -qx 'Active=yes' <<< "$info" && grep -qx 'Class=user' <<< "$info" && grep -Eq '^Type=(x11|wayland)$' <<< "$info"; then matches+=("$id"); fi
 done < <(loginctl list-sessions --no-legend --no-pager 2>/dev/null)
 [[ ${#matches[@]} -eq 1 ]] || return 1
 printf '%s\n' "${matches[0]}"
}
# El registro de APT debe corresponder a este proceso, terminar y no registrar error.
historial_correcto(){
 local bloque fecha inicio archivo=${5:-/var/log/apt/history.log}
 bloque=$(LM_APT_COMMAND="$1" LM_APT_UID="$2" tail -c 1048576 "$archivo" | LM_APT_COMMAND="$1" LM_APT_UID="$2" awk '
  BEGIN{RS="";FS="\n"} {cmd=0;usr=0;fin=0;err=0;pkg=0;
   for(i=1;i<=NF;i++){
    if($i=="Commandline: " ENVIRON["LM_APT_COMMAND"])cmd=1;
    if($i ~ /^Requested-By:/ && index($i,"(" ENVIRON["LM_APT_UID"] ")"))usr=1;
    if($i ~ /^End-Date:/)fin=1;
    if($i ~ /^Error:/)err=1;
    if($i ~ /^(Install|Upgrade|Reinstall):/ && index($i,"lamontana-instalador:"))pkg=1;
   }
   if(cmd&&usr&&pkg){ultimo=$0;valido=fin&&!err;}
  } END{if(valido)print ultimo}') || return 1
 [[ -n $bloque ]] || return 1
 fecha=$(sed -n 's/^Start-Date: //p' <<< "$bloque")
 inicio=$(date -d "$fecha" +%s) || return 1
 (( inicio >= $3 - 3 && inicio <= $4 + 3 ))
}
programar(){
 [[ ${DEBIAN_FRONTEND:-} != noninteractive && -z ${SSH_CONNECTION:-} && -z ${SSH_TTY:-} ]] || return 0
 local p=$PPID aptpid='' aptinicio='' tty='' uid='' exe sudo_visto=false command sid arranque epoca ahora evento
 for ((n=0;n<64 && p>1;n++)); do
  proceso "$p" || break
  exe=$(readlink -f "/proc/$p/exe" 2>/dev/null || true)
  case ${exe##*/} in sshd|sshd-session|sshd-auth) return 0;; sudo|sudo-rs) sudo_visto=true;; esac
  if [[ $exe == /usr/bin/apt || $exe == /usr/bin/apt-get ]]; then
   aptpid=$p;aptinicio=${proc[19]};tty=${proc[4]};uid=$(cat "/proc/$p/loginuid")
   command=$(tr '\0' ' ' < "/proc/$p/cmdline");command=${command% }
  fi
  p=${proc[1]}
 done
 [[ $sudo_visto == true && -n $aptpid && $tty != 0 && $uid =~ ^[0-9]+$ && $uid -ge 1000 && $uid -lt 4294967295 ]] || return 0
 sid=$(sesion_grafica "$uid") || return 0
 arranque=$(awk '$1=="btime"{print $2}' /proc/stat)
 epoca=$((arranque + aptinicio / $(getconf CLK_TCK)));ahora=$(date +%s)
 [[ ! -L $estado ]] || return 0
 install -d -m 700 -o root -g root "$estado"
 evento=$aptpid-$aptinicio-$uid
 # mkdir es atómico: un reintento del mismo evento no programa otra apertura.
 mkdir -m 700 "$estado/$evento" 2>/dev/null || return 0
 printf '%s\n' "$command" > "$estado/$evento/comando"
 printf '%s\n' "$sid" > "$estado/$evento/sesion"
 printf '%s\n' "$epoca" "$ahora" > "$estado/$evento/fechas"
 if systemd-run --quiet --collect --unit="lamontana-apertura-$evento" --property=Type=exec --property=RuntimeMaxSec=360 --property=MemoryMax=64M --property=NoNewPrivileges=yes -- /bin/bash "$base/apertura-ubuntu.sh" esperar "$evento"; then
  printf '%s\n' 'La Montaña: el asistente se abrirá en tu escritorio cuando APT termine correctamente.'
 else aviso 'no se pudo programar la apertura automática'; fi
}
esperar(){
 local evento=$1 pid inicio uid sid actual command fechas nombre passwd gid home resto terminada=false
 [[ $evento =~ ^([0-9]+)-([0-9]+)-([0-9]+)$ ]] || return 1
 pid=${BASH_REMATCH[1]};inicio=${BASH_REMATCH[2]};uid=${BASH_REMATCH[3]}
 [[ -d $estado/$evento && ! -L $estado/$evento ]] || return 1
 sid=$(cat "$estado/$evento/sesion");command=$(cat "$estado/$evento/comando");mapfile -t fechas < "$estado/$evento/fechas"
 for ((n=0;n<300;n++)); do
  if ! proceso "$pid" || [[ ${proc[19]} != "$inicio" || ${proc[0]} == Z ]]; then
   if ! lslocks --noheadings --output PATH | grep -E '^ */var/(lib/dpkg/lock(-frontend)?|cache/apt/archives/lock)$' >/dev/null; then terminada=true;break;fi
  fi
  sleep 1
 done
 [[ $terminada == true ]] || { aviso 'se agotó la espera de APT';return 0; }
 [[ $(dpkg-query -W -f='${db:Status-Status}' "$paquete" 2>/dev/null) == installed ]] || { aviso 'el paquete no quedó configurado';return 0; }
 historial_correcto "$command" "$uid" "${fechas[0]}" "${fechas[1]}" || { aviso 'no se pudo acreditar el cierre correcto de esta instalación de APT';return 0; }
 actual=$(sesion_grafica "$uid") || { aviso 'ya no hay una sesión gráfica local inequívoca';return 0; }
 [[ $actual == "$sid" ]] || { aviso 'cambió la sesión gráfica';return 0; }
 [[ $(stat -c %u "/run/user/$uid") == "$uid" && -S /run/user/$uid/bus && -x $lanzador ]] || { aviso 'el escritorio no está disponible';return 0; }
 IFS=: read -r nombre passwd actual gid resto home resto < <(getent passwd "$uid")
 [[ -n $nombre && $actual == "$uid" && $home == /* ]] || return 0
 lanzar_en_sesion "$nombre" "$uid" "$home" "$evento"
}
lanzar_en_sesion(){
 local nombre=$1 uid=$2 home=$3 evento=$4 unidad="lamontana-asistente-$4"
 # La deduplicación corresponde a esta transacción, no a actualizaciones previas.
 # Todo lo siguiente se ejecuta como el usuario. El gestor de su sesión aporta
 # DISPLAY/WAYLAND_DISPLAY y demás entorno gráfico; no se abre un navegador root.
 local entorno=(runuser -u "$nombre" -- env -i "HOME=$home" "USER=$nombre" "LOGNAME=$nombre" PATH=/usr/bin:/bin "XDG_RUNTIME_DIR=/run/user/$uid" "DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/$uid/bus")
 if "${entorno[@]}" systemctl --user is-active --quiet "$unidad.service"; then return 0; fi
 "${entorno[@]}" systemd-run --user --quiet --collect --unit="$unidad" --property=MemoryMax=512M -- "$lanzador" || aviso 'no se pudo abrir el asistente'
}
if [[ ${BASH_SOURCE[0]} == "$0" ]]; then
 [[ $EUID == 0 ]] || { aviso 'la apertura posterior a APT requiere el gestor de paquetes';exit 0; }
 case ${1:-} in programar) programar;; esperar) esperar "${2:-}";; *) exit 64;; esac
fi
