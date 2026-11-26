# Distribución de la Demo para Windows

La edición para **Windows 11 x64** se instala para el usuario actual. El paquete
de la versión 0.1.0 incluye la aplicación y los componentes para usarla sin
conexión. El ejecutable pasó las pruebas de instalación, reparación y
reinstalación en Windows 11 x64. La descarga pública está pendiente.

## Preparar el equipo

Necesitás virtualización habilitada, WSL2 y un navegador local. El asistente
reconoce Edge, Chrome y Firefox. Si necesitás un navegador, podés usar el Firefox
incluido. La Demo utiliza su propia distribución de WSL2, **LaMontanaDemo-Ubuntu**.
El asistente comprueba que Docker Engine 24 o posterior y Compose 2.20 o
posterior estén disponibles dentro de esa distribución. Incluye los componentes
para prepararla.

## Instalar y abrir

1. Ejecutá `LaMontanaDemo-0.1.0.exe`. El instalador de Windows muestra los
   avisos del paquete. Si los aceptás, pulsá **Install** y, al terminar, **Finish**.
   Desde el menú Inicio, en **YG Soluciones**, abrí **LaMontanaDemo**.
2. Elegí **Verificar el equipo**. Si queda un componente pendiente, abrí
   **Necesito resolver una dependencia**, elegí el componente y **Copia incluida,
   sin Internet**. Windows puede pedir permisos de administrador o un reinicio
   al habilitar WSL2. Después, volvé a abrir el asistente y repetí la verificación.
3. Elegí una carpeta vacía para la imprenta. La sugerida es
   `%USERPROFILE%\LaMontanaDemo`. Pulsá **Comprobar carpeta** y avanzá.
4. Dejá el puerto local **3200**, o indicá otro puerto libre. Revisá la carpeta
   y la dirección web, pulsá **Instalar la Demo** y esperá a que termine.
5. Pulsá **Abrir la Demo y elegir rol**. Podés entrar como administrador,
   empleado o cliente. En adelante, **LaMontanaDemo** inicia los servicios de
   la instalación guardada por el asistente y abre el selector en el navegador. Con el
   puerto sugerido, la dirección es `http://127.0.0.1:3200/acceso`.

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
  Usa el mismo procedimiento de respaldo.
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
(Get-Item -LiteralPath '.\LaMontanaDemo-0.1.0.exe').Length
(Get-FileHash -Algorithm SHA256 -LiteralPath '.\LaMontanaDemo-0.1.0.exe').Hash
```

| Dato de la versión | Registro |
| --- | --- |
| Versión y nombre del archivo | 0.1.0 · `LaMontanaDemo-0.1.0.exe` |
| Tamaño en bytes | 1.575.023.104 |
| SHA-256 | `70728a6645cd7d2f183ef910d719998ea61d72f990946137750762c23d0f7c8c` |
| Firma del instalador | Sin firma digital (Authenticode). |
| Validación en Windows 11 x64 | Instalación inicial sin La Montaña, operación con Ethernet desconectado, reparación con respaldo, desinstalación y reinstalación verificadas. Se conservaron el pedido, el PDF, la base y el vencimiento. |
| URL pública y fecha de publicación | Pendientes de la validación de la descarga. |

## Publicación y retiro

La descarga pública usará un depósito dedicado a la Demo con un archivo
versionado. **Descargar demo** enlazará a ese archivo desde la web de YG
Soluciones. Las entregas privadas de clientes conservan su canal y sus permisos
de acceso.

El ejecutable de prueba de esta ficha se identifica como distribución local.
Para la entrega pública, YG Soluciones debe revisar los avisos de los
componentes incluidos y reunir los textos y fuentes que indica el paquete.
Si cambia el ejecutable, se debe repetir la validación de Windows y registrar
el tamaño y la huella del archivo que se ofrecerá para descargar.

Antes de dar por publicada una versión, descargar el archivo en una sesión
anónima y cotejar el tamaño y SHA-256 con el instalador probado en Windows.
Comprobar que las entregas privadas siguen exigiendo sus permisos actuales.
Registrar el resultado, la URL y la fecha de publicación.

Para retirar una versión, quitar su enlace de la web y deshabilitar el acceso
público al archivo correspondiente. Comprobar en una sesión anónima que la web
ya no ofrece esa versión y que la URL directa dejó de servir el archivo.
Registrar la fecha del retiro junto al registro de la versión.
