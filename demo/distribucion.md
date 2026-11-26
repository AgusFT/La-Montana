# Distribución de la Demo para Windows

La edición para **Windows 11 x64** se instala para el usuario actual. El
instalador 0.1.2 prepara la imprenta y comprueba las firmas de ClamAV antes
de abrirla. Después de instalar, la Demo funciona sin conexión. La descarga
pública está pendiente.

## Preparar el equipo

Necesitás virtualización habilitada, WSL2 y un navegador local. El asistente
reconoce Edge, Chrome y Firefox. Si necesitás un navegador, podés usar el Firefox
incluido. La Demo utiliza su propia distribución de WSL2, **LaMontanaDemo-Ubuntu**.
El asistente comprueba que Docker Engine 24 o posterior y Compose 2.20 o
posterior estén disponibles dentro de esa distribución. Incluye los componentes
para prepararla.

## Instalar y abrir

1. Ejecutá `LaMontanaDemo-0.1.2.exe`. El instalador de Windows muestra los
   avisos del paquete. Si los aceptás, pulsá **Install** y, al terminar, **Finish**.
   Desde el menú Inicio, en **YG Soluciones**, abrí **LaMontanaDemo**.
2. Elegí **Verificar el equipo**. Si queda un componente pendiente, abrí
   **Necesito resolver una dependencia**, elegí el componente y **Copia incluida,
   sin Internet**. Windows puede pedir permisos de administrador o un reinicio
   al habilitar WSL2. Después, volvé a abrir el asistente y repetí la verificación.
3. Elegí una carpeta vacía para la imprenta. La sugerida es
   `%USERPROFILE%\LaMontanaDemo`. Pulsá **Comprobar carpeta** y avanzá.
4. Dejá el puerto local **3200**, o indicá otro puerto libre. En **Prepará la
   protección de archivos**, elegí **Descargar desde el servicio oficial de
   ClamAV** o **Usar un archivo oficial de este equipo o de un USB**. La
   descarga con FreshClam es gratuita y funciona sin cuenta. Para instalar
   con la red desconectada, llevá una copia de `bytecode.cvd` obtenida con
   FreshClam y seleccioná la carpeta que la contiene. El asistente comprueba
   su firma, que sea la base de bytecode y que sea compatible con el motor.
   [Firmas oficiales de ClamAV](https://docs.clamav.net/manual/Signatures.html)
5. Revisá la carpeta, la dirección web y el origen de las firmas. Pulsá
   **Instalar la Demo**. Si se interrumpe la descarga, podés reintentar o
   elegir el archivo local. La imprenta se inicia al completar la verificación.
6. Pulsá **Abrir la Demo y elegir rol**. Podés entrar como administrador,
   empleado o cliente. En adelante, **LaMontanaDemo** inicia los servicios de
   la instalación guardada por el asistente y abre el selector en el navegador. Con el
   puerto sugerido, la dirección es `http://127.0.0.1:3200/acceso`.

Para preparar el USB en otro equipo con ClamAV configurado y conexión,
creá una carpeta vacía en la que FreshClam pueda escribir. Ejecutá
`freshclam --update-db=bytecode --datadir="<carpeta vacía>"`, sustituyendo
la ruta entre comillas por la de esa carpeta. Copiá el archivo
`bytecode.cvd` al USB. Podés retirar el USB cuando el asistente confirme
la instalación. La Demo guarda su propia copia verificada.

## Reparar, retirar o reinstalar

Abrí **LaMontanaDemo-Mantenimiento**, verificá el equipo y seleccioná la
carpeta de la imprenta. Si olvidaste la ruta, el asistente permite
buscar instalaciones y comprobar la carpeta encontrada.

- **Reparar la misma versión** repone los componentes de la versión instalada.
  Elegí una carpeta privada de respaldos en un disco local, fuera de la carpeta
  de la imprenta. Al confirmar, el asistente detiene los servicios y verifica
  una copia de la base, archivos, configuración e imágenes antes de repararlos.
  La copia contiene credenciales de la instalación. Si el respaldo falla, el
  asistente informa el error y los servicios pueden quedar detenidos.
- **Actualizar la aplicación** requiere una versión posterior del paquete.
  Usa el mismo procedimiento de respaldo. La instalación guarda en su carpeta
  una copia verificada de las firmas para los arranques y el mantenimiento
  sin conexión. Si falta, el asistente permite volver a obtenerla.
- **Desinstalar la Demo y conservar sus datos** retira los servicios de la
  carpeta seleccionada. Para retirar todas las instalaciones de la Demo de tu
  usuario, abrí **Configuración → Aplicaciones → Aplicaciones instaladas** y
  desinstalá **LaMontanaDemo**. Windows quita también el asistente y sus accesos.
  En ambos casos se conservan los pedidos, los archivos y la fecha de vencimiento.
  Si Windows informa un error durante el retiro, podés consultar el detalle en
  `%LOCALAPPDATA%\YG-Soluciones\LaMontanaDemo\desinstalacion.log`.
- Para reinstalar, ejecutá el paquete de la misma versión, abrí el asistente y
  seleccioná la carpeta original. La reparación y la reinstalación en ese equipo
  mantienen las **360 horas contadas desde el primer arranque**. [Vigencia de la Demo](README.md#vigencia)

## Verificar el archivo

Desde la carpeta de la descarga, podés consultar el tamaño del instalador en
bytes y calcular su SHA-256 con PowerShell. Compará ambos con la ficha de la
versión que publique YG Soluciones.

```powershell
(Get-Item -LiteralPath '.\LaMontanaDemo-0.1.2.exe').Length
(Get-FileHash -Algorithm SHA256 -LiteralPath '.\LaMontanaDemo-0.1.2.exe').Hash
```

### Instalador 0.1.2

| Dato de la versión | Registro |
| --- | --- |
| Archivo | `LaMontanaDemo-0.1.2.exe` · Windows 11 x64 |
| Tamaño en bytes | 1.581.318.656 |
| SHA-256 | `a0021d06372a261e061dc57683aa7f6b820b84c26010395d62ea0de8993f9509` |
| Firma del instalador | Sin firma digital (Authenticode). |
| Firefox incluido | 161.0, Windows x64, español de Argentina. |
| Antivirus | ClamAV 1.4.6. Las bases principal y diaria se incluyen. El asistente obtiene o importa la base oficial de bytecode. |
| Verificación en Windows 11 x64 | Se probaron la descarga desde ClamAV y la instalación desde USB con la red desconectada. La reparación y la reinstalación conservaron el pedido, el PDF y el vencimiento. [Resultados de la 0.1.2](backlog.md#instalador-012) |
| URL pública y fecha de publicación | Pendientes. |

### Instalador de laboratorio 0.1.0

| Dato de la versión | Registro |
| --- | --- |
| Versión y nombre del archivo | 0.1.0 · `LaMontanaDemo-0.1.0.exe` |
| Tamaño en bytes | 1.575.023.104 |
| SHA-256 | `70728a6645cd7d2f183ef910d719998ea61d72f990946137750762c23d0f7c8c` |
| Firma del instalador | Sin firma digital (Authenticode). |
| Validación en Windows 11 x64 | Instalación inicial sin La Montaña, operación con Ethernet desconectado, reparación con respaldo, desinstalación y reinstalación verificadas. Se conservaron el pedido, el PDF, la base y el vencimiento. |
| Distribución | Versión de laboratorio para pruebas locales. |

## Publicación y retiro

Se creó el bucket dedicado `yg-soluciones-demo-lamontana` en el proyecto de
Google Cloud `yg-soluciones`, con almacenamiento Standard en `us-east1`.
El 26 de noviembre de 2026 se verificó que estaba vacío, con acceso uniforme
y bloqueo de acceso público. La portada de YG Soluciones
ofrecerá **Descargar demo** cuando esté disponible una versión pública.
Los instaladores destinados a clientes se entregan por su canal privado.

La versión 0.1.0 de esta ficha se validó para uso local. Incluye Firefox
157.0.1. Mozilla exige la versión estable vigente para redistribuir Firefox
con su marca bajo sus condiciones generales. La preparación de una entrega
pública incluye revisar los avisos y las fuentes correspondientes a los
componentes que acompañen a la aplicación. [Política de distribución de Mozilla](https://www.mozilla.org/en-US/foundation/trademarks/distribution-policy/)

1. Antes de publicar, comprobar el Firefox incluido en la carga de Windows con
   `python3 infra/instalador/verificar-firefox-windows.py <carpeta-del-paquete>`
   desde la rama Demo. El control consulta el canal estable y el SHA-256
   oficial del instalador para Windows x64 en español de Argentina. Validar en
   Windows el ejecutable definitivo y registrar la versión, el
   nombre, el tamaño en bytes y el SHA-256. Si cambian sus bytes, asignar una
   versión nueva y repetir la prueba con ese archivo.
2. Con la publicación autorizada, subir el archivo a
   `la-montana/<versión>/LaMontanaDemo-<versión>.exe` como archivo adjunto.
   Usar la precondición de generación cero para que una ruta ya ocupada se
   conserve. Registrar la generación devuelta por Cloud Storage. Preparar
   los archivos de terceros, el índice `fuentes/la-montana/<versión>/index.txt`
   y sus sumas SHA-256 en una carpeta administrada propia. Así las fuentes
   siguen disponibles cuando se retire el ejecutable.
3. Habilitar la lectura pública de la carpeta administrada de la versión
   aprobada en el bucket de la Demo. En una sesión sin
   credenciales, descargar el archivo completo y comparar tamaño y SHA-256
   con el registro de Windows. Abrir el índice público de fuentes y comprobar
   los archivos contra sus sumas. Comprobar también que el canal de clientes
   sigue exigiendo su autorización para descargar una entrega privada.
   [Acceso público a una carpeta en Cloud Storage](https://docs.cloud.google.com/storage/docs/access-control/making-data-public)
4. Configurar `YG_DEMO_BUCKET` y `YG_DEMO_VERSION` en la compilación de la
   landing autorizada para producción. Abrir **Descargar demo** desde la
   portada publicada en una sesión nueva y verificar el destino. Anotar la
   URL directa y la fecha de publicación en la ficha de esa versión.

Para retirar la entrega, quitar ambas variables, volver a desplegar la portada
y revocar la lectura pública del archivo. Comprobar en una sesión anónima que
el botón desapareció y que la URL directa dejó de servir el ejecutable.
Registrar la fecha del retiro.

La distribución mantiene Firefox incluido. Cuando Mozilla cambie la versión
estable, retirar la descarga anterior, generar una nueva versión del
instalador con el navegador oficial intacto y repetir las verificaciones
anteriores antes de ofrecerla. El instalador nuevo recibe su propia URL.
La vigencia de 15 días de una Demo ya instalada sigue contando desde el primer
arranque. [Canal estable de Mozilla](https://product-details.mozilla.org/1.0/firefox_versions.json)
