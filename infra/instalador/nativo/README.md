# Instalador de La Montaña Demo para Windows 11 x64

La versión 0.1.7 instala una aplicación web para el usuario de Windows que ejecuta el instalador. El navegador muestra la demo. En el propio equipo, Java procesa las operaciones, Node.js sirve la interfaz, PostgreSQL guarda los datos, ClamAV revisa los adjuntos y Mailpit recibe los correos de prueba. Los servicios escuchan en la interfaz local. El EXE incluye las firmas principal y diaria para analizar archivos desde el primer inicio. FreshClam intenta obtener firmas adicionales por Internet y verifica su firma y compatibilidad antes de activarlas. Si la descarga falla, se mantienen las firmas locales.

## Instalar y abrir la demo

1. Abrí `LaMontanaDemo-0.1.7.exe` y leé la presentación. Windows propone `%LOCALAPPDATA%\Programs\YG Soluciones\LaMontanaDemoNativa` para los componentes. **Examinar** permite elegir otra carpeta local escribible en una instalación nueva. Al actualizar una edición nativa anterior, el asistente muestra y conserva su carpeta.
2. En **Componentes**, elegí si querés un acceso en el escritorio. El menú **Inicio → YG Soluciones → La Montaña Demo** siempre incluye uno. Ambos muestran la montaña de la barra lateral.
3. Al finalizar, seleccioná **Abrir La Montaña Demo**. El panel indica qué servicio está preparando. Cuando Windows acepta abrir la dirección en el navegador, el panel se minimiza en la barra de tareas. Si el navegador no abre, el panel deja visible la dirección local. Usá **Copiar dirección** o **Abrir la demo** para reintentar.
4. Para volver, usá el mismo acceso. Para terminar, abrí el panel y elegí **Cerrar la demo → Apagar la demo**. La X permite **Seguir usando la demo** o confirmar el apagado. En el próximo inicio estarán disponibles tus datos mientras siga vigente la demo.

## Construcción

1. Compilá `backend/target/lamontana.jar` con Maven y JDK 17 (`mvn package` desde `backend`). Desde `frontend`, instalá lo especificado en `pnpm-lock.yaml` y ejecutá `pnpm build` para generar `.next/standalone`.
2. Prepará una carpeta de insumos con los archivos de `dependencias.sha256`. Los ZIP de Temurin y ClamAV corresponden a sus publicaciones oficiales, Node.js a la distribución win-x64 y Mailpit a la publicación v1.31.0. El JAR de PostgreSQL es `io.zonky.test.postgres:embedded-postgres-binaries-windows-amd64:18.6.0` de Maven Central. El paquete `@img/sharp-win32-x64` corresponde al registro npm. Las bases `main.cvd` y `daily.cld` corresponden a la carga auditada de la demo. En `avisos/` colocá `postgresql-REL_18_6-COPYRIGHT`, `main.cvd-COPYING`, `daily.cld-COPYING`, `bibliotecas-backend.tsv` y `modulos-node.tsv` del inventario de componentes.
3. Usá Python 3.12 o posterior, JDK 17 en `JAVA_HOME` y NSIS 3.10 con el destino `amd64-unicode` en el `PATH`. Desde `infra/instalador`, ejecutá `python nativo/crear.py <insumos> <salida>`. En PowerShell también podés usar `./empaquetar-windows.ps1 -Paquete <insumos> -Salida <salida>`. El proceso verifica los insumos y genera el EXE y su SHA-256.

## Datos y mantenimiento

El panel abre el selector de roles después de comprobar el backend, el frontend, el correo de prueba y el antivirus. Si otro programa ocupa un puerto, se elige uno libre en el mismo equipo. **Cerrar la demo** apaga sus procesos. Para reparar la instalación, cerrá el panel y ejecutá otra vez el instalador.

Si la carpeta del programa o del perfil de Windows contiene tildes, una ñ u otros caracteres fuera de ASCII, el panel reserva una letra de unidad libre para cada ruta que la necesite. El Explorador puede mostrarla mientras usás la demo. Apunta a la misma carpeta y conserva sus permisos. Al apagar la demo, el panel retira esas letras. Si Windows interrumpe el panel, al abrirlo de nuevo recupera las que todavía apuntan a sus carpetas.

Los archivos, la base y los registros están en `%LOCALAPPDATA%\YG-Soluciones\LaMontanaDemo\nativa`. La marca de vigencia del equipo se conserva en `%ProgramData%\YG-Soluciones\LaMontanaDemo\vigencia`. Al desinstalar se conservan estos datos. La 0.1.4 tiene su propio desinstalador y sus datos siguen en los volúmenes de esa edición. La edición nativa inicia su propia imprenta de muestra y respeta la fecha de vigencia anterior.

Al actualizar o desinstalar, el instalador coteja la ruta y el SHA-256 de cada componente con el inventario del EXE. Conserva archivos personales, incluso dentro de la carpeta del programa. Si el nombre de un archivo personal coincide con un componente nuevo y su contenido es diferente, el instalador identifica el conflicto para que lo muevas y reintentes. Una instalación interrumpida puede dejar una carpeta de recuperación junto a `app`.

Los inventarios de compatibilidad corresponden a los archivos dentro de `$PLUGINSDIR/contenido.zip` de las entregas 0.1.5 (`da44ae121dd148b7a793245e342d53cabfb6ebab7a199dc57c0350ed8dd11939`) y 0.1.6 (`c320be4bda284057cd6dc701efd41293e9eefcf71271b63732a88797962b4e8a`). Cada línea guarda SHA-256, dos espacios, ruta relativa con barras y salto de línea, en UTF-8 y comprimida con gzip. `crear.py` genera el inventario de la edición que se está empaquetando. El ícono usa la variante con sol de `MountainMark` en el panel lateral.

Desde la raíz de la rama Demo, ejecutá `node infra/instalador/src/test/sistema/archivos-nativos.test.mjs` para verificar conservación, colisiones y recuperación en carpetas temporales. La verificación de servicios en Windows se puede ejecutar con `verificar-demo-windows.ps1` después de iniciar el panel.

Para probar las carpetas Unicode y la recuperación de las letras de unidad en Windows, desde `infra/instalador` ejecutá con un JDK 17:

```powershell
$clases = New-Item -ItemType Directory -Path (Join-Path $env:TEMP ([guid]::NewGuid()))
javac --release 17 -encoding UTF-8 -d $clases.FullName nativo/RutasPostgres.java src/test/sistema/RutasPostgresTest.java
java -cp $clases.FullName RutasPostgresTest
```
