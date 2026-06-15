# CU-AND-002 - Consultar detalle de pedido desde Android

| Campo | Valor |
|---|---|
| ID | CU-AND-002 |
| Caso de uso | Consultar detalle de pedido desde Android |
| Área | Web y Android |
| Actor principal | Cliente |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-AND-001, RF-AND-002, RF-AND-003, RF-AND-005, RF-PED-004, RF-EST-002, RF-EST-006, RF-ARC-002, RF-AUT-001, RF-AUT-003 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-005, RNF-AUT-001, RNF-AUT-002, RNF-RLS-002, RNF-RLS-006, RNF-ARC-002, RNF-USA-002, RNF-REN-001, RNF-COM-002, RNF-COM-003 |
| HU relacionadas | HU-CLI-003, HU-CLI-004, HU-CLI-008, HU-SIS-003 |
| Reglas críticas relacionadas | RFC-004, RFC-007, RFC-008, RFC-009, RNFC-001, RNFC-003, RNFC-004, RNFC-005, RNFC-009 |

## 1. Caso de Uso

Consultar detalle de pedido desde Android.

## 2. Actores

| Actor | Participación |
|---|---|
| Cliente | Consulta desde Android el detalle visible de un pedido propio |
| Sistema | Valida autenticación, propiedad del pedido, permisos, estado visible, archivos visibles e información permitida |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un cliente consulta desde Android el detalle visible de un pedido propio.

El detalle del pedido puede incluir información general del pedido, estado visible al cliente, archivos asociados visibles, mensajes o indicaciones para el cliente, información financiera visible y próxima acción esperada.

La app Android consume el mismo backend que la Web. Por lo tanto, debe respetar las mismas reglas de autenticación, autorización, RLS, visibilidad de estados, acceso a archivos y protección de información sensible.

El cliente no debe acceder a estados internos, observaciones internas, auditoría administrativa, datos de otros clientes, información operativa interna ni archivos no autorizados.

Este caso de uso es de consulta. No modifica pedidos, no cambia estados, no carga archivos, no registra cobros, no autoriza producción, no genera trabajos de impresión y no ejecuta impresión.

## 4. Precondición

- El cliente tiene instalada o disponible la app Android.
- El cliente está autenticado.
- Existe una sesión válida contra Supabase Auth.
- El pedido existe.
- El pedido pertenece al cliente autenticado.
- El pedido tiene información visible disponible.
- El backend Supabase está disponible.
- La app Android consume el mismo backend que la Web.
- Las políticas RLS impiden que el cliente consulte pedidos ajenos.
- Las políticas de Storage o acceso a archivos impiden consultar archivos no autorizados.
- El sistema tiene definido un estado visible para el pedido.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| Usuario autenticado | Sí | Cliente que consulta el detalle desde Android |
| Sesión activa | Sí | Sesión válida que identifica al cliente |
| ID del pedido | Sí | Identificador del pedido cuyo detalle se desea consultar |
| Canal de acceso | Sí | Android |
| Parámetros de vista | No | Sección o pestaña del detalle, por ejemplo datos, archivos, estado o pagos visibles |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Detalle visible del pedido | Información permitida del pedido para el cliente |
| ID del pedido | Identificador del pedido consultado |
| Descripción del pedido | Información general visible |
| Estado visible al cliente | Estado comunicable al cliente |
| Información financiera visible | Información de seña, pago o pendiente financiero cuando corresponda |
| Archivos visibles | Archivos asociados que el cliente puede consultar |
| Mensajes o indicaciones | Corrección solicitada, seña requerida, pedido recibido u otra indicación visible |
| Próxima acción esperada | Acción requerida del cliente si corresponde |
| Mensaje de error | Mensaje controlado si la consulta falla |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere cliente autenticado mediante Supabase Auth |
| Autorización | El cliente solo puede consultar detalles de pedidos propios |
| RLS / acceso a datos | El pedido debe estar protegido para impedir acceso a pedidos ajenos |
| Storage / archivos | Los archivos visibles deben estar protegidos por políticas de acceso autorizadas |
| Estado interno | No debe mostrarse al cliente |
| Estado visible | Es el estado operativo comunicable al cliente |
| Estado financiero | Solo debe mostrarse información financiera visible y autorizada |
| Observaciones internas | No deben mostrarse al cliente |
| Auditoría | No debe mostrarse auditoría administrativa al cliente |
| Backend común | Android debe consumir el mismo backend y respetar las mismas reglas que Web |
| Validación backend | La seguridad no debe depender únicamente de controles de la app Android |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El cliente abre la app Android. | Si no hay sesión activa, la app solicita iniciar sesión. |
| 2 | La app valida o recupera la sesión del cliente. | Si la sesión expiró, solicita autenticación nuevamente. |
| 3 | El cliente accede a la sección de pedidos. | Si no tiene permisos o sesión válida, el sistema rechaza la consulta. |
| 4 | El cliente selecciona un pedido. | Si el pedido no existe o ya no está disponible, la app informa la situación. |
| 5 | La app solicita al backend el detalle del pedido seleccionado. | Si no hay conexión o falla el backend, muestra error controlado. |
| 6 | El backend valida que el pedido pertenece al cliente autenticado. | Si el pedido no pertenece al cliente, rechaza la operación. |
| 7 | El backend recupera información visible del pedido. | Si falta un dato visible crítico, devuelve mensaje controlado o estado seguro. |
| 8 | El backend recupera archivos visibles si corresponde. | Si un archivo no está disponible o no está autorizado, no se incluye en la respuesta. |
| 9 | El backend recupera información financiera visible si corresponde. | Si no corresponde mostrar información financiera, no expone datos internos. |
| 10 | La app muestra el detalle visible del pedido. | Si ocurre error de renderizado, muestra mensaje controlado sin exponer datos internos. |
| 11 | El cliente visualiza estado, datos, archivos visibles y próxima acción esperada. | Si intenta acceder a información no permitida, backend/RLS debe rechazar la operación. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios. No se expone al cliente |
| Estado visible al cliente | Sin cambios. Se consulta para mostrarlo en Android |
| Estado financiero | Sin cambios. Solo se muestra información visible si corresponde |
| Estado técnico de impresión | Sin cambios |
| Estado de sesión | Sin cambios, salvo que la sesión esté expirada y requiera autenticación |
| Estado de archivos | Sin cambios. Solo se consultan archivos visibles |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Consulta de detalle desde Android | Puede registrarse si se decide auditar consultas del cliente |
| Sesión inválida en Android | Cuando el cliente intenta consultar detalle sin sesión válida |
| Intento de acceso a pedido ajeno | Cuando se intenta consultar detalle de un pedido no asociado al cliente |
| Intento de acceso a archivo no autorizado | Cuando se intenta consultar archivo no permitido |
| Error de consulta de detalle Android | Cuando ocurre una falla técnica relevante en la consulta |

## 11. Observaciones

- Este caso de uso es de solo consulta.
- Este caso de uso no crea pedidos.
- Este caso de uso no modifica pedidos.
- Este caso de uso no carga archivos.
- Este caso de uso no cambia estados.
- Este caso de uso no registra cobros ni comprobantes.
- Este caso de uso no genera trabajos de impresión.
- Android debe consumir el mismo backend que Web.
- Android debe respetar las mismas reglas de permisos, RLS, Storage policies y visibilidad.
- El cliente solo debe ver estados visibles, no estados internos.
- El cliente no debe acceder a información interna del negocio.
- La app puede tener una presentación distinta a la Web, pero no reglas de negocio diferentes.
- La seguridad debe estar en backend, no solo en la app.
- No se deben exponer rutas locales del cliente ni rutas internas del sistema para archivos.

## 12. Poscondición

Al finalizar correctamente:

- el cliente visualiza el detalle visible de su pedido desde Android;
- el cliente solo ve información propia y permitida;
- el cliente ve estados visibles correspondientes;
- el cliente puede consultar archivos visibles autorizados;
- no se exponen estados internos ni auditoría;
- no se modifican datos de negocio;
- no se generan trabajos de impresión;
- la consulta queda protegida por backend, RLS, Storage policies y permisos.

## 13. Criterios de aceptación

- El cliente autenticado puede consultar el detalle de pedidos propios desde Android.
- El sistema rechaza consultas sin sesión válida.
- El sistema rechaza acceso a detalles de pedidos ajenos.
- La app muestra solo información visible para el cliente.
- La app muestra estado visible y datos permitidos.
- La app no muestra estados internos, observaciones internas ni auditoría.
- La app permite consultar solo archivos autorizados.
- Android consume el mismo backend que Web.
- Android respeta las mismas reglas de permisos, RLS y Storage policies.
- La consulta no modifica pedidos ni estados.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.
