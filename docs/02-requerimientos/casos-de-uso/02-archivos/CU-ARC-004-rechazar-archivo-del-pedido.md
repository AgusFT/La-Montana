# CU-ARC-004 - Rechazar archivo del pedido

| Campo | Valor |
|---|---|
| ID | CU-ARC-004 |
| Caso de uso | Rechazar archivo del pedido |
| Área | Archivos |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-ARC-004, RF-ARC-005, RF-ARC-007, RF-REV-003, RF-REV-005, RF-AUD-001, RF-AUD-002, RF-AUD-003 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-006, RNF-ARC-002, RNF-ARC-005, RNF-AUD-001, RNF-AUD-004 |
| HU relacionadas | HU-EMP-002, HU-CLI-005, HU-ADM-001 |
| Reglas críticas relacionadas | RFC-001, RFC-002, RFC-003, RFC-007, RFC-008, RNFC-001, RNFC-003, RNFC-004 |

## 1. Caso de Uso

Rechazar archivo del pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Revisa un archivo asociado a un pedido y registra que no cumple las condiciones necesarias |
| Administrador | Rechaza archivos con permisos ampliados y puede usar esta decisión como parte de la revisión administrativa |
| Sistema | Valida permisos, acceso al archivo, relación archivo-pedido, estado del pedido, estado del archivo y registra trazabilidad |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador rechaza un archivo asociado a un pedido.

El rechazo de un archivo permite dejar registrado que el archivo cargado por el cliente no cumple las condiciones necesarias para continuar el flujo operativo, productivo o de impresión.

Un archivo puede rechazarse por motivos como:

- archivo ilegible;
- archivo corrupto;
- formato no permitido;
- archivo incompleto;
- archivo equivocado;
- baja calidad;
- cantidad de páginas/carillas inconsistente;
- contenido no correspondiente al pedido;
- imposibilidad técnica de impresión;
- cualquier otra condición definida por la operación.

Rechazar un archivo no debe aprobar ni rechazar automáticamente el pedido completo, pero puede dejar el pedido pendiente de corrección o requerir intervención administrativa.

Este caso de uso no genera trabajos de impresión, no autoriza producción y no modifica información financiera de forma directa.

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
- Las políticas de acceso deben impedir rechazar archivos de pedidos no autorizados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido al que pertenece el archivo |
| ID del archivo | Sí | Identificador del archivo que se desea rechazar |
| Usuario interno autenticado | Sí | Empleado o administrador que realiza el rechazo |
| Motivo de rechazo | Sí | Motivo por el cual el archivo no puede aceptarse |
| Observación interna | No | Comentario operativo visible solo para usuarios internos autorizados |
| Mensaje visible al cliente | No | Indicación opcional para que el cliente entienda qué debe corregir |
| Tipo de rechazo | No | Clasificación opcional, por ejemplo formato, legibilidad, contenido, tamaño o archivo incorrecto |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Archivo rechazado | Archivo marcado como rechazado o equivalente |
| Estado del archivo | Estado actualizado del archivo dentro del flujo documental |
| Pedido asociado | Pedido al que pertenece el archivo rechazado |
| Usuario que rechaza | Usuario interno que registró el rechazo |
| Fecha de rechazo | Momento en que se registró el rechazo |
| Motivo registrado | Motivo de rechazo asociado al archivo |
| Mensaje visible al cliente | Mensaje de corrección si corresponde |
| Evento de auditoría | Registro de trazabilidad asociado al rechazo |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos de revisión pueden rechazar archivos |
| RLS / acceso a datos | El acceso al pedido, archivo y relación archivo-pedido debe estar protegido por políticas de seguridad |
| Storage | El archivo debe consultarse mediante mecanismo autorizado |
| Cliente final | El cliente no puede rechazar archivos |
| Estado del archivo | Solo archivos en estado revisable pueden ser rechazados |
| Estado del pedido | No se deben rechazar archivos de pedidos cerrados o fuera del flujo operativo permitido |
| Validación backend | El rechazo no debe depender únicamente del frontend |
| Auditoría | El rechazo debe quedar registrado con usuario, fecha, motivo y contexto |
| Mensaje al cliente | El mensaje visible no debe exponer información interna del negocio |

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
| 9 | El usuario selecciona la acción de rechazar archivo. | Si no tiene permisos para rechazar, el sistema bloquea la acción. |
| 10 | El usuario ingresa el motivo del rechazo y, si corresponde, un mensaje visible para el cliente. | Si falta motivo obligatorio, el sistema solicita completarlo. |
| 11 | El sistema valida el motivo, permisos y estado del archivo. | Si los datos son inválidos, el sistema rechaza la operación e informa el motivo. |
| 12 | El sistema actualiza el estado del archivo a rechazado o equivalente. | Si no puede actualizar el estado, evita dejar datos inconsistentes. |
| 13 | El sistema registra el rechazo del archivo y su motivo. | Si no puede registrar el motivo, revierte o bloquea la operación según corresponda. |
| 14 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 15 | El sistema informa que el archivo fue rechazado correctamente. | Si ocurre un error final, informa la falla y evita duplicar eventos de rechazo. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Puede mantenerse sin cambios o reflejar que el pedido requiere corrección, según el modelo de estados |
| Estado visible al cliente | Puede mantenerse o mostrar una indicación visible de corrección requerida si se define en el flujo |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios. No se genera trabajo de impresión |
| Estado del archivo | Cambia a rechazado, requiere corrección o equivalente |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Archivo rechazado | Cuando un usuario autorizado marca el archivo como rechazado |
| Motivo de rechazo registrado | Cuando se guarda el motivo del rechazo |
| Mensaje visible al cliente registrado | Cuando se define una indicación de corrección visible para el cliente |
| Intento de rechazo no autorizado | Cuando un usuario sin permisos intenta rechazar un archivo |
| Archivo no pertenece al pedido | Cuando se detecta una relación inválida entre pedido y archivo |
| Error de rechazo | Cuando ocurre una falla técnica durante el rechazo |
| Estado de archivo actualizado | Cuando el archivo cambia a rechazado, requiere corrección o equivalente |

## 11. Observaciones

- Este caso de uso no rechaza automáticamente el pedido completo.
- Este caso de uso no aprueba ni autoriza producción.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no modifica estados financieros.
- Este caso de uso no cierra pedidos.
- El cliente final no puede rechazar archivos.
- El rechazo del archivo debe ser realizado por un usuario interno autorizado.
- Si el rechazo requiere acción del cliente, debe existir un mensaje claro y controlado.
- El mensaje visible al cliente no debe exponer observaciones internas ni información sensible.
- El acceso al archivo debe estar protegido por permisos, RLS, Storage policies, signed URLs o mecanismo equivalente.
- El rechazo del archivo puede ser condición para solicitar corrección del pedido.
- La solicitud formal de corrección del pedido se documenta en un caso de uso de revisión administrativa.
- El archivo rechazado no debe quedar habilitado para impresión.
- Si el cliente carga una nueva versión del archivo, esa operación debe seguir el caso de uso correspondiente de carga o reemplazo de archivo.

## 12. Poscondición

Al finalizar correctamente:

- el archivo queda marcado como rechazado, requiere corrección o equivalente;
- el archivo sigue asociado al pedido correspondiente;
- el usuario que rechazó el archivo queda registrado;
- el motivo del rechazo queda registrado;
- el evento de rechazo queda auditado;
- el pedido no queda aprobado automáticamente para producción;
- no se genera ningún trabajo de impresión;
- no se modifica información financiera;
- el cliente puede recibir una indicación de corrección si el flujo lo define;
- el acceso al archivo queda protegido por permisos y políticas de seguridad.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede rechazar archivos si tiene permisos.
- El sistema rechaza intentos de rechazo realizados por usuarios no autorizados.
- El sistema valida que el archivo pertenece al pedido correspondiente.
- El sistema impide rechazar archivos eliminados, reemplazados o no disponibles.
- El rechazo requiere un motivo.
- El archivo queda marcado como rechazado, requiere corrección o equivalente.
- El rechazo queda registrado para trazabilidad.
- El rechazo del archivo no aprueba ni rechaza automáticamente el pedido completo.
- El rechazo del archivo no genera trabajos de impresión.
- El cliente no recibe información interna sensible.
- El rechazo se realiza mediante backend, RLS, Storage policies, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.