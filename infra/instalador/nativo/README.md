# Instalador de La Montaña Demo para Windows 11 x64

La versión 0.1.5 instala el panel de Inicio para el usuario actual. Java, PostgreSQL, Node.js, ClamAV y Mailpit se ejecutan localmente mientras el panel está abierto. La interfaz usa el navegador predeterminado. Las dependencias vienen en el EXE, salvo la base firmada `bytecode.cvd` que ClamAV obtiene del canal oficial o de una copia local elegida en el primer arranque.

## Construcción

1. Compilá `backend/target/lamontana.jar` con Maven y JDK 17 (`mvn package` desde `backend`). Desde `frontend`, instalá lo especificado en `pnpm-lock.yaml` y ejecutá `pnpm build` para generar `.next/standalone`.
2. Prepará una carpeta de insumos con los archivos de `dependencias.sha256`. Los ZIP de Temurin y ClamAV corresponden a sus publicaciones oficiales, Node.js a la distribución win-x64 y Mailpit a la publicación v1.31.0. El JAR de PostgreSQL es `io.zonky.test.postgres:embedded-postgres-binaries-windows-amd64:18.6.0` de Maven Central. El paquete `@img/sharp-win32-x64` corresponde al registro npm. Las bases `main.cvd` y `daily.cld` corresponden a la carga auditada de la demo. En `avisos/` colocá `postgresql-REL_18_6-COPYRIGHT`, `main.cvd-COPYING`, `daily.cld-COPYING`, `bibliotecas-backend.tsv` y `modulos-node.tsv` del inventario de componentes.
3. Usá Python 3.12 o posterior, JDK 17 en `JAVA_HOME` y NSIS 3.10 con el destino `amd64-unicode` en el `PATH`. Desde `infra/instalador`, ejecutá `python nativo/crear.py <insumos> <salida>`. En PowerShell también podés usar `./empaquetar-windows.ps1 -Paquete <insumos> -Salida <salida>`. El proceso verifica los insumos y genera el EXE y su SHA-256.

## Datos y mantenimiento

El panel abre el selector de roles después de comprobar el backend, el frontend, el correo de prueba y el antivirus. Si otro programa ocupa un puerto, se elige uno libre en el mismo equipo. **Cerrar la demo** apaga sus procesos. Para reparar la instalación, cerrá el panel y ejecutá otra vez el instalador.

Los archivos, la base y los registros están en `%LOCALAPPDATA%\YG-Soluciones\LaMontanaDemo\nativa`. La marca de vigencia del equipo se conserva en `%ProgramData%\YG-Soluciones\LaMontanaDemo\vigencia`. Al desinstalar se conservan estos datos. La 0.1.4 tiene su propio desinstalador y sus datos siguen en los volúmenes de esa edición. La edición nativa inicia su propia imprenta de muestra y respeta la fecha de vigencia anterior.
