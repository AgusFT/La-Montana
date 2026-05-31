# CU-ARC-003 - Validar archivo del pedido

| Campo | Valor |
|---|---|
| ID | CU-ARC-003 |
| Caso de uso | Validar archivo del pedido |
| Área | Archivos |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-ARC-004, RF-ARC-005, RF-ARC-007, RF-REV-001, RF-REV-002, RF-REV-005, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-006, RNF-ARC-002, RNF-ARC-005, RNF-AUD-001, RNF-AUD-004 |
| HU relacionadas | HU-EMP-002, HU-ADM-001, HU-ADM-002 |
| Reglas críticas relacionadas | RFC-001, RFC-002, RFC-003, RFC-007, RFC-008, RNFC-001, RNFC-003, RNFC-004 |

## 1. Caso de Uso

Validar archivo del pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Revisa un archivo asociado a un pedido y registra si está en condiciones de continuar el flujo operativo |
| Administrador | Valida archivos con permisos ampliados y puede usar esta validación como parte de la revisión administrativa |
| Sistema | Valida permisos, acceso al archivo, relación archivo-pedido, estado del pedido, estado del archivo y registra trazabilidad |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador valida un archivo asociado a un pedido.

La validación del archivo permite registrar que el archivo fue revisado y que, desde el punto de vista operativo, puede continuar dentro del flujo del pedido. Esto no implica por sí solo que el pedido completo quede aprobado para producción ni que se genere un trabajo de impresión.

El archivo validado queda marcado como aceptado, válido o equivalente según el modelo de estados de archivos que se defina posteriormente.

Si durante la revisión se detecta que el archivo no cumple condiciones mínimas, el flujo debe derivar al caso de uso correspondiente de rechazo u observación del archivo, sin habilitarlo para producción ni impresión.

Este caso de uso es importante porque los archivos del pedido son parte central del flujo de La Montaña y deben ser revisados antes de cualquier operación de producción o impresión.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos internos para revisar archivos.
- El pedido existe.
- El archivo existe.
- El archivo está asociado al pedido correspondiente.
- El usuario tiene permiso para consultar el pedido y el archivo.
- El archivo fue cargado mediante un mecanismo autorizado.
- El archivo no fue eliminado ni reemplazado por una versión posterior.
- El pedido todavía no fue cerrado.
- El backend Supabase está disponible.
- Supabase Storage o el mecanismo de almacenamiento definido está disponible.
- Las políticas de acceso deben impedir revisar archivos de pedidos no autorizados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido al que pertenece el archivo |
| ID del archivo | Sí | Identificador del archivo que se desea validar |
| Usuario interno autenticado | Sí | Empleado o administrador que realiza la validación |
| Resultado de validación | Sí | Resultado de la revisión: archivo válido, aceptado o equivalente |
| Observación de validación | No | Comentario interno asociado a la revisión del archivo |
| Tipo de validación | No | Clasificación opcional, por ejemplo formato, legibilidad, contenido, tamaño, páginas o condición técnica |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Archivo validado | Archivo marcado como válido, aceptado o equivalente |
| Estado del archivo | Estado actualizado del archivo dentro del flujo documental |
| Pedido asociado | Pedido al que pertenece el archivo validado |
| Usuario validador | Usuario interno que realizó la validación |
| Fecha de validación | Momento en que se registró la validación |
| Observación interna | Comentario de validación si fue registrado |
| Evento de auditoría | Registro de trazabilidad asociado a la validación |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos de revisión pueden validar archivos |
| RLS / acceso a datos | El acceso al pedido, archivo y relación archivo-pedido debe estar protegido por políticas de seguridad |
| Storage | El archivo debe consultarse mediante mecanismo autorizado |
| Cliente final | El cliente no puede validar archivos |
| Estado del archivo | Solo archivos en estado revisable pueden ser validados |
| Estado del pedido | No se deben validar archivos de pedidos cerrados o fuera del flujo operativo permitido |
| Validación backend | La validación no debe depender únicamente del frontend |
| Auditoría | La validación debe quedar registrada con usuario, fecha y contexto |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a la sección interna de pedidos o archivos pendientes de revisión. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona un pedido. | Si el pedido no existe o no está disponible, el sistema informa la situación. |
| 4 | El sistema valida que el usuario tenga permisos sobre el pedido. | Si no tiene permisos, el sistema rechaza la consulta y registra el intento si corresponde. |
| 5 | El usuario selecciona un archivo asociado al pedido. | Si el archivo no existe, fue reemplazado o fue eliminado, el sistema informa la situación. |
| 6 | El sistema valida que el archivo pertenece al pedido seleccionado. | Si el archivo no pertenece al pedido, el sistema bloquea la operación. |
| 7 | El sistema valida que el archivo está en estado revisable. | Si el archivo ya fue validado, rechazado o no está disponible, informa el estado actual. |
| 8 | El usuario revisa el archivo mediante acceso autorizado. | Si el archivo no puede abrirse o descargarse, el sistema informa el error sin exponer rutas internas. |
| 9 | El usuario confirma que el archivo es válido para continuar el flujo. | Si detecta un problema, debe derivar al flujo de rechazo u observación del archivo. |
| 10 | El sistema registra la validación del archivo. | Si la validación no puede guardarse, el sistema evita dejar estados parciales. |
| 11 | El sistema actualiza el estado del archivo a válido, aceptado o equivalente. | Si el estado de archivo no puede actualizarse, debe registrarse error técnico. |
| 12 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 13 | El sistema informa que el archivo fue validado correctamente. | Si ocurre un error final, informa la falla y evita duplicar eventos de validación. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Puede mantenerse sin cambios o reflejar que el pedido tiene archivo validado, según el modelo de estados |
| Estado visible al cliente | Sin cambios directo, salvo que luego se defina una comunicación visible específica |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios. No se genera trabajo de impresión |
| Estado del archivo | Cambia a validado, aceptado o equivalente |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Archivo validado | Cuando un usuario autorizado marca el archivo como válido |
| Intento de validación no autorizada | Cuando un usuario sin permisos intenta validar un archivo |
| Archivo no pertenece al pedido | Cuando se detecta una relación inválida entre pedido y archivo |
| Error de validación | Cuando ocurre una falla técnica durante la validación |
| Acceso al archivo para revisión | Puede registrarse si se decide auditar accesos a archivos durante revisión |
| Estado de archivo actualizado | Cuando el archivo cambia a validado, aceptado o equivalente |

## 11. Observaciones

- Este caso de uso no aprueba el pedido completo para producción.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no modifica estados financieros.
- Este caso de uso no cierra pedidos.
- Este caso de uso no reemplaza la revisión administrativa completa del pedido.
- El cliente final no puede validar archivos.
- La validación del archivo debe ser realizada por un usuario interno autorizado.
- Si el archivo no cumple condiciones mínimas, debe usarse un caso de rechazo u observación del archivo.
- La validación debe registrarse con usuario, fecha y contexto.
- El acceso al archivo debe estar protegido por permisos, RLS, Storage policies, signed URLs o mecanismo equivalente.
- La validación del archivo puede ser una condición necesaria para aprobar el pedido, pero no debe ser la única condición.
- El archivo validado no debe quedar automáticamente habilitado para impresión si el pedido todavía no fue aprobado.

## 12. Poscondición

Al finalizar correctamente:

- el archivo queda marcado como validado, aceptado o equivalente;
- el archivo sigue asociado al pedido correspondiente;
- el usuario validador queda registrado;
- el evento de validación queda auditado;
- el pedido no queda aprobado automáticamente para producción;
- no se genera ningún trabajo de impresión;
- no se modifica información financiera;
- el acceso al archivo queda protegido por permisos y políticas de seguridad.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede validar archivos si tiene permisos.
- El sistema rechaza validaciones realizadas por usuarios no autorizados.
- El sistema valida que el archivo pertenece al pedido correspondiente.
- El sistema impide validar archivos eliminados, reemplazados o no disponibles.
- El archivo queda marcado como validado, aceptado o equivalente.
- La validación queda registrada para trazabilidad.
- La validación del archivo no aprueba automáticamente el pedido.
- La validación del archivo no genera trabajos de impresión.
- La validación se realiza mediante backend, RLS, Storage policies, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.