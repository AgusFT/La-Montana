# CU-PED-002 - Consultar pedido propio

| Campo | Valor |
|---|---|
| ID | CU-PED-002 |
| Caso de uso | Consultar pedido propio |
| Área | Pedidos |
| Actor principal | Cliente |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-PED-004, RF-EST-002, RF-EST-006, RF-WEB-005, RF-AND-003 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-005, RNF-RLS-002, RNF-USA-002, RNF-REN-001 |
| HU relacionadas | HU-CLI-004, HU-CLI-007, HU-CLI-008 |
| Reglas críticas relacionadas | RFC-004, RFC-009, RNFC-001, RNFC-003, RNFC-005, RNFC-009 |

## 1. Caso de Uso

Consultar pedido propio.

## 2. Actores

| Actor | Participación |
|---|---|
| Cliente | Consulta el detalle o resumen de uno de sus pedidos |
| Sistema | Valida identidad, permisos y propiedad del pedido antes de mostrar información |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un cliente consulta un pedido propio desde Web o Android.

El sistema debe mostrar únicamente información permitida para el cliente, principalmente datos generales del pedido, estado visible, información financiera correspondiente y archivos asociados cuando aplique.

El cliente no debe acceder a estados internos, observaciones internas, auditoría administrativa, datos de otros clientes ni información operativa que no corresponda exponer.

## 4. Precondición

- El cliente está autenticado.
- El pedido existe.
- El pedido pertenece al cliente autenticado.
- El sistema tiene definido un estado visible para el pedido.
- El backend Supabase está disponible.
- Las políticas de acceso a datos deben impedir que el cliente consulte pedidos ajenos.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido que el cliente desea consultar |
| Usuario autenticado | Sí | Usuario cliente que realiza la consulta |
| Canal de acceso | No | Web o Android, según desde dónde se realiza la consulta |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| ID del pedido | Identificador del pedido consultado |
| Fecha de creación | Fecha en que el pedido fue creado |
| Descripción del pedido | Información general visible para el cliente |
| Estado visible al cliente | Estado que representa el avance del pedido para el cliente |
| Estado financiero visible | Información financiera que corresponda mostrar al cliente |
| Archivos visibles | Archivos asociados que el cliente puede consultar |
| Mensajes o indicaciones | Información relevante como pedido pendiente de revisión, corrección solicitada o seña requerida |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere cliente autenticado |
| Autorización | El cliente solo puede consultar pedidos propios |
| RLS / acceso a datos | La consulta debe estar protegida para impedir acceso a pedidos ajenos |
| Estados internos | No deben mostrarse estados internos no destinados al cliente |
| Información financiera | Solo debe mostrarse información financiera visible o relevante para el cliente |
| Observaciones internas | No deben exponerse observaciones administrativas u operativas internas |
| Auditoría | No debe exponerse auditoría interna al cliente |
| Validación backend | La propiedad del pedido debe validarse en backend, RLS, RPC o política equivalente, no solo en frontend |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El cliente ingresa al sistema desde Web o Android. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El cliente accede a la sección de sus pedidos. | Si no tiene pedidos, el sistema muestra una vista vacía o mensaje correspondiente. |
| 3 | El sistema consulta los pedidos asociados al cliente autenticado. | Si ocurre un error de conexión o backend, el sistema informa la falla. |
| 4 | El sistema muestra la lista de pedidos propios del cliente. | Si algún pedido no puede cargarse, el sistema debe evitar mostrar información parcial incorrecta. |
| 5 | El cliente selecciona un pedido para ver su detalle. | Si el pedido seleccionado ya no existe o fue eliminado lógicamente, el sistema informa que no está disponible. |
| 6 | El sistema valida que el pedido pertenezca al cliente autenticado. | Si el pedido no pertenece al cliente, el sistema rechaza la consulta. |
| 7 | El sistema recupera la información visible del pedido. | Si faltan datos visibles obligatorios, el sistema debe mostrar un mensaje controlado y registrar el problema si corresponde. |
| 8 | El sistema muestra el detalle permitido del pedido. | Si el pedido requiere corrección, el sistema muestra indicaciones visibles para el cliente. |
| 9 | El cliente visualiza estado, datos generales y archivos visibles. | Si algún archivo no está disponible, el sistema informa la situación sin exponer rutas internas. |
| 10 | El sistema mantiene oculta la información interna no visible para el cliente. | Si el frontend intenta solicitar datos internos, backend/RLS debe impedir el acceso. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios |
| Estado visible al cliente | Sin cambios. Se consulta para mostrarlo al cliente |
| Estado financiero | Sin cambios. Se consulta solo la información financiera visible correspondiente |
| Estado técnico de impresión | Sin cambios. No se ejecutan ni modifican trabajos de impresión |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Consulta de pedido propio | Puede registrarse si se decide auditar accesos relevantes del cliente |
| Intento de acceso no autorizado | Debe registrarse si un usuario intenta acceder a un pedido que no le pertenece |
| Error de consulta | Debe registrarse si ocurre una falla técnica relevante al consultar el pedido |

## 11. Observaciones

- Este caso de uso no modifica el pedido.
- Este caso de uso no cambia estados.
- Este caso de uso no autoriza producción.
- Este caso de uso no genera trabajos de impresión.
- El cliente solo debe ver información diseñada para comunicación externa.
- La separación entre estado interno y estado visible es obligatoria.
- Web y Android deben consumir el mismo backend y respetar las mismas reglas de acceso.
- La seguridad no debe depender de ocultar botones o rutas en la interfaz.
- RLS debe impedir acceso a pedidos ajenos incluso si se intenta consultar manualmente un ID inválido.
- La visualización de archivos asociados debe respetar las políticas de acceso definidas para Storage.

## 12. Poscondición

Al finalizar correctamente:

- el cliente visualiza información permitida de su pedido;
- el sistema no expone información interna;
- el pedido no cambia de estado;
- no se crea ningún trabajo de impresión;
- no se modifica información financiera;
- el acceso queda controlado por permisos y políticas de seguridad.

## 13. Criterios de aceptación

- El cliente autenticado puede consultar únicamente sus propios pedidos.
- El sistema rechaza consultas sobre pedidos ajenos.
- El cliente ve el estado visible del pedido.
- El cliente no ve estados internos no autorizados.
- El cliente no ve observaciones internas ni auditoría administrativa.
- El sistema muestra información financiera visible cuando corresponde.
- Web y Android respetan las mismas reglas de backend.
- La consulta no modifica datos del pedido.
- El sistema protege la consulta mediante permisos y RLS.
- El resultado del caso de uso es coherente con los requerimientos funcionales, no funcionales e historias relacionadas.
