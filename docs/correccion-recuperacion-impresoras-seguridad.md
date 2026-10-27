# Recuperación de impresoras vigentes: autorización HTTP

Revisión del 27/10/2026. Complementa las correcciones de Fases 4 y 5 y el alcance global de las impresoras.

## Incidencia

En un borrador anterior sin impresoras, el botón **Recuperar impresoras vigentes** devolvía HTTP 403 con el mensaje «La operación no está autorizada». La ruta existía en el controlador y estaba permitida en el proxy web, pero faltaba declararla en la lista de autorización de Spring Security. La solicitud se rechazaba antes de ejecutar el servicio de recuperación.

## Corrección acotada

Se autoriza exclusivamente `POST /api/admin/configuracion/borradores/{borrador}/impresoras/recuperar-vigentes` para el rol `ADMIN_ADMIN`, igual que las demás modificaciones de configuración. No se agregan permisos genéricos ni se desactiva CSRF. La comprobación del propietario, sesión vigente, versión del borrador, estado editable, no sobrescritura e idempotencia sigue a cargo de los controles existentes.

La recuperación conserva identidad, capacidades, formatos y ubicación interna de los equipos vigentes, sin sustituir pagos ni horarios. No exige cancelar el borrador ni volver a crear impresoras. Esta corrección no requiere una nueva migración ni modifica los datos existentes.

## Regresión

`RecuperacionImpresorasSecurityTest` recorre la cadena real de Spring Security, con sesiones HTTP y tokens CSRF; no se limita a invocar directamente el servicio. Comprueba administrador autorizado, roles no permitidos, ausencia de sesión, CSRF ausente o inválido, métodos/rutas no autorizados y sesión revocada. Se sustituyen únicamente las consultas de vigencia de usuario por respuestas controladas; no se presenta como prueba con la base de datos del usuario.

Antes de la corrección, la prueba del administrador reproduce el HTTP 403 y el mensaje observado. Tras agregar el permiso específico, los seis casos pasan sin fallos, errores ni omisiones.
