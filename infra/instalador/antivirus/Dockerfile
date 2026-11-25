FROM clamav/clamav:1.4.6_base@sha256:76cff2172141d860ad6485c86dc6d9bdc4a6b6e1128ed9d86aea5434ae55bf5f
COPY clamd.conf /etc/clamav/clamd.conf
COPY firmas/ /opt/lamontana-firmas/
COPY arrancar.sh /usr/local/bin/lamontana-antivirus
ENTRYPOINT ["/bin/sh", "/usr/local/bin/lamontana-antivirus"]
HEALTHCHECK --interval=10s --timeout=5s --start-period=360s --retries=6 CMD clamdscan --config-file=/etc/clamav/clamd.conf --ping=1
