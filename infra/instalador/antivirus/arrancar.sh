#!/bin/sh
set -eu
mkdir -p /var/lib/clamav
for file in /opt/lamontana-firmas/*; do
  [ -f "$file" ] || continue
  name=$(basename "$file")
  [ -e "/var/lib/clamav/$name" ] || cp "$file" "/var/lib/clamav/$name"
done
chown -R clamav:clamav /var/lib/clamav
# Desarrollo conserva las firmas del release y no intenta salir a Internet.
if [ "${LAMONTANA_MODO:-DESARROLLO}" = STANDARD ]; then freshclam --daemon --checks=4 & fi
exec clamd --config-file=/etc/clamav/clamd.conf --foreground=true
