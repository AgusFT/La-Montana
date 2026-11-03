# ClamAV: configuración al construir desde Windows

Si el log muestra `Incorrect argument format for option TCPSocket` y `Can't open/parse the config file`, el daemon no llegó a iniciar. Las firmas pueden estar actualizadas y el contenedor puede seguir ejecutándose mientras el entrypoint espera el socket.

La línea `TCPSocket 3310` es válida. En ClamAV 1.4.6, el parser elimina espacios, tabulaciones y `LF` al final del argumento, pero no `CR`. Un checkout de Windows con CRLF deja el argumento numérico como `3310\r`, que no supera su validación. La ausencia de una regla de finales de línea en Git permitía esa conversión.

La corrección fija LF para `infra/antivirus/clamd.conf` mediante `.gitattributes` y elimina los CR finales dentro de la imagen después de copiar el archivo. No modifica puertos, firmas, controles de escaneo ni volúmenes.

## Actualizar desde PowerShell

Desde la raíz del repositorio, en la rama `pruebas`:

```powershell
git pull --no-rebase --no-edit origin pruebas
docker compose --project-name lamontana-pruebas --env-file .env.pruebas -f compose.yaml -f compose.pruebas.yaml up --build --detach --wait antivirus
docker inspect --format '{{.State.Health.Status}}' lamontana-pruebas-antivirus-1
```

El resultado esperado es `healthy`. La nueva instrucción del Dockerfile invalida la capa previa de la configuración, sin requerir `--no-cache` ni borrar datos.

Para comprobar los caracteres del archivo copiado y el ping real:

```powershell
docker exec lamontana-pruebas-antivirus-1 sh -c 'sed -n "1,8l" /etc/clamav/clamd.conf'
docker exec lamontana-pruebas-antivirus-1 clamdscan --config-file=/etc/clamav/clamd.conf --ping=1
```

Las líneas deben terminar en `$`, sin `\r$`. Si el daemon sigue sin responder, consultar `docker logs --tail 100 lamontana-pruebas-antivirus-1` para distinguir un nuevo error del problema de formato.

## Comprobación de la corrección

Se verifica la regla de Git y que el comando de normalización transforma una copia con CRLF en un archivo idéntico al original LF. La confirmación de `healthy` y del ping debe realizarse en la instalación Docker; este entorno de edición no dispone del daemon Docker.
