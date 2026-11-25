#!/usr/bin/env bash
set -euo pipefail
# Entrada de compatibilidad. El paquete nativo contiene Java y los recursos offline.
lanzador=/opt/lamontana-instalador/bin/LaMontanaInstalador
if [[ -x "$lanzador" ]]; then
  exec "$lanzador" "$@"
fi
cat >&2 <<'AVISO'
El arranque principal ahora se realiza con el instalador de La Montaña.
Instalá el paquete .deb de esta versión y abrí «LaMontanaInstalador» desde
el menú de aplicaciones. El asistente verificará Docker y las dependencias.

El código del asistente y sus herramientas de construcción están en infra/instalador.
Para trabajar expresamente con código fuente y el Compose anterior, consultá
README.md e infra/iniciar-desde-fuentes.sh. Esa vía no instala dependencias
ni realiza mantenimiento mediante respaldos.
AVISO
exit 69
