FROM ubuntu:26.04
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl gnupg
RUN install -m 0755 -d /etc/apt/keyrings && curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc && chmod a+r /etc/apt/keyrings/docker.asc
RUN printf 'Types: deb\nURIs: https://download.docker.com/linux/ubuntu\nSuites: resolute\nComponents: stable\nSigned-By: /etc/apt/keyrings/docker.asc\n' > /etc/apt/sources.list.d/docker.sources && apt-get update
# Incluir también paquetes ya presentes en el constructor: el destino offline puede ser mínimo.
RUN mkdir /salida /uris && dpkg-query -W -f='${binary:Package}=${Version}\n' > /base.txt \
    && apt-get --download-only --reinstall --no-install-recommends -y -o Dir::Cache::archives=/salida install $(cat /base.txt) docker-ce docker-ce-cli containerd.io docker-compose-plugin docker-buildx-plugin systemd-sysv iptables libgtk-3-0t64 libasound2t64 libdbus-glib-1-2 libx11-xcb1 libxtst6 \
    && apt-get --print-uris --download-only --reinstall --no-install-recommends -y -o Dir::Cache::archives=/uris install $(cat /base.txt) docker-ce docker-ce-cli containerd.io docker-compose-plugin docker-buildx-plugin systemd-sysv iptables libgtk-3-0t64 libasound2t64 libdbus-glib-1-2 libx11-xcb1 libxtst6 > /salida/descargas.txt \
    && cd /salida && sha256sum *.deb > SHA256SUMS
