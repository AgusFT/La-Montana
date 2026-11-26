#!/bin/sh
set -eu
db=/var/lib/clamav

# sigtool valida la firma del CVD; el nombre de archivo por sí solo no identifica la base.
validar_bytecode() {
    [ -f "$1" ] && [ ! -L "$1" ] || return 1
    sigtool --info="$1" >/dev/null 2>&1 || return 1
    info=$(dd if="$1" bs=512 skip=1 2>/dev/null | tar -xzO bytecode.info 2>/dev/null) || return 1
    nivel=$(printf '%s\n' "$info" | awk -F: 'NR==1 && $1=="ClamAV-VDB" && $3~/^[0-9]+$/ && $4~/^[0-9]+$/ && $5~/^[0-9]+$/ {print $5}')
    motor=$(sigtool --help | awk '/--flevel=FLEVEL/{getline; if ($1=="Default:" && $2~/^[0-9]+$/) print $2}')
    [ -n "$nivel" ] && [ -n "$motor" ] && [ "$nivel" -le "$motor" ]
}

preparar_bytecode() {
    temporal=$(mktemp -d)
    trap 'rm -rf "$temporal"; [ -z "${provisional:-}" ] || rm -f "$provisional"' EXIT
    candidato=$temporal/bytecode.cvd
    case "$1" in
        archivo) cp /intercambio/candidato.cvd "$candidato" || exit 21 ;;
        internet|guardada)
            if validar_bytecode "$db/bytecode.cvd"; then
                cp "$db/bytecode.cvd" "$candidato"
            elif [ "$1" = internet ]; then
                chown clamav:clamav "$temporal"
                freshclam --update-db=bytecode --datadir="$temporal" --config-file=/etc/clamav/freshclam.conf || exit 20
            else
                exit 21
            fi ;;
        *) exit 21 ;;
    esac
    validar_bytecode "$candidato" || exit 21
    # Una reparación puede encontrar una CLD local de una edición previa. Conservarla
    # fuera de las extensiones que carga clamd recién después de autenticar su reemplazo.
    if ! cmp -s "$candidato" "$db/bytecode.cvd"; then
        provisional=$(mktemp "$db/.bytecode-XXXXXX")
        cp "$candidato" "$provisional" && chown clamav:clamav "$provisional" && chmod 640 "$provisional" &&
            mv -f "$provisional" "$db/bytecode.cvd" || exit 22
    fi
    if [ -e "$db/bytecode.cld" ]; then mv -f "$db/bytecode.cld" "$db/bytecode.cld.anterior" || exit 22; fi
    cp "$candidato" /intercambio/validado.cvd || exit 22
}

mkdir -p "$db"
if [ "${1:-}" = preparar-firmas ]; then
    [ "${LAMONTANA_BYTECODE_EXTERNO:-}" = 1 ] && [ "$#" = 2 ] || exit 21
    preparar_bytecode "$2"
    exit 0
fi
for file in /opt/lamontana-firmas/*; do
    [ -f "$file" ] || continue
    name=$(basename "$file")
    [ -e "$db/$name" ] || cp "$file" "$db/$name"
done
if [ "${LAMONTANA_BYTECODE_EXTERNO:-}" = 1 ]; then
    [ ! -e "$db/bytecode.cld" ] && validar_bytecode "$db/bytecode.cvd" || {
        echo 'Falta una base bytecode oficial válida. Abrí el asistente de la Demo para prepararla.' >&2
        exit 21
    }
fi
chown -R clamav:clamav "$db"
if [ "${LAMONTANA_MODO:-DESARROLLO}" = STANDARD ]; then freshclam --daemon --checks=4 & fi
exec clamd --config-file=/etc/clamav/clamd.conf --foreground=true
