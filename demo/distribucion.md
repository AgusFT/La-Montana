# Distribución de la Demo para Windows

El destino previsto es un instalador `.exe` para **Windows 11 x64**. La
validación del paquete, su firma y el destino público están pendientes.

## Requisitos e instalación previstos

La Demo se abrirá en el navegador del equipo. La instalación se basa en el
asistente nativo de La Montaña y prevé el uso de WSL2 con virtualización
habilitada. Windows puede pedir permisos de administrador o un reinicio al
habilitar esos componentes. Los requisitos del paquete final se confirmarán en
Windows 11 x64.

1. En la web de YG Soluciones, pulsar **Descargar demo** cuando esté disponible.
2. Comparar el tamaño y el SHA-256 del archivo con los datos de la versión publicada.
3. Ejecutar el `.exe` y completar el asistente. Si Windows pide reiniciar,
   volver a abrir el asistente después del reinicio.
4. Abrir el acceso directo y elegir un rol en el navegador. El uso local de la
   Demo después de la instalación se validará con la red desconectada.

La Demo tendrá sus propios identificadores de instalación y actualización,
directorios y volúmenes. La aceptación incluye reparar, desinstalar y volver a
instalar en el mismo equipo, comprobar que el plazo conserva la fecha de inicio
original y verificar el aislamiento respecto de otras ediciones de La Montaña.

## Verificar el archivo

En PowerShell, sustituir `ruta-al-instalador.exe` por la ruta del archivo
descargado y comparar `Hash` con el SHA-256 de la versión publicada.

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath '.\ruta-al-instalador.exe'
```

| Dato de la versión | Estado |
| --- | --- |
| Versión y nombre del archivo | Pendientes del empaquetado. |
| Tamaño y SHA-256 | Pendientes de la validación en Windows. |
| Firma del instalador | Pendiente de verificación. |
| URL pública y fecha de publicación | Pendientes de la validación de la descarga. |

## Publicación y retiro

La descarga pública usará un depósito dedicado a la Demo con un archivo
versionado. **Descargar demo** enlazará a ese archivo desde la web de YG
Soluciones. Las entregas privadas de clientes conservan su canal y sus permisos
de acceso.

Antes de dar por publicada una versión, descargar el archivo en una sesión
anónima y cotejar el tamaño y SHA-256 con el instalador probado en Windows.
Comprobar que las entregas privadas siguen exigiendo sus permisos actuales.
Registrar el resultado, la URL y la fecha de publicación.

Para retirar una versión, quitar su enlace de la web y deshabilitar el acceso
público al archivo correspondiente. Comprobar en una sesión anónima que la web
ya no ofrece esa versión y que la URL directa dejó de servir el archivo.
Registrar la fecha del retiro junto al registro de la versión.
