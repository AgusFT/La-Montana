# CU-EST-001 - Consultar estado visible del pedido

| Campo | Valor |
|---|---|
| ID | CU-EST-001 |
| Caso de uso | Consultar estado visible del pedido |
| Área | Estados y finanzas |
| Actor principal | Cliente |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-PED-004, RF-EST-002, RF-EST-006, RF-WEB-005, RF-AND-003 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-005, RNF-RLS-002, RNF-USA-002, RNF-COM-002, RNF-COM-003 |
| HU relacionadas | HU-CLI-004, HU-CLI-007, HU-CLI-008 |
| Reglas críticas relacionadas | RFC-004, RFC-009, RNFC-001, RNFC-003, RNFC-005, RNFC-009 |

## 1. Caso de Uso

Consultar estado visible del pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Cliente | Consulta el estado visible de un pedido propio |
| Sistema | Valida autenticación, propiedad del pedido, permisos y muestra únicamente información visible al cliente |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un cliente consulta el estado visible de un pedido propio.

La Montaña distingue entre:

- estado interno;
- estado visible al cliente;
- estado financiero.

El cliente no debe acceder al estado interno operativo del pedido ni a información administrativa interna. El sistema debe mostrar únicamente el estado visible diseñado para comunicación con el cliente y, cuando corresponda, información financiera visible.

Este caso de uso es de solo lectura. No modifica el pedido, no cambia estados, no autoriza producción, no genera trabajos de impresión y no ejecuta impresión.

## 4. Precondición

- El cliente está autenticado.
- El pedido existe.
- El pedido pertenece al cliente autenticado.
- El pedido tiene un estado visible definido.
- El backend Supabase está disponible.
- Las políticas RLS impiden que el cliente consulte pedidos ajenos.
- El sistema tiene una correspondencia válida entre estado interno y estado visible al cliente.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido cuyo estado visible se desea consultar |
| Usuario autenticado | Sí | Cliente que realiza la consulta |
| Canal de acceso | No | Web o Android, según desde dónde se consulta |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| ID del pedido | Identificador del pedido consultado |
| Estado visible al cliente | Estado que representa el avance del pedido para el cliente |
| Mensaje asociado al estado | Texto o indicación visible para el cliente, si corresponde |
| Fecha de última actualización visible | Momento de última actualización comunicable al cliente |
| Información financiera visible | Información de seña, pago o pendiente financiero cuando corresponda |
| Próxima acción esperada del cliente | Acción requerida, por ejemplo corregir datos, cargar archivo, abonar seña o esperar revisión |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere cliente autenticado |
| Autorización | El cliente solo puede consultar estados visibles de pedidos propios |
| RLS / acceso a datos | El pedido debe estar protegido para impedir acceso a pedidos ajenos |
| Estado interno | No debe mostrarse al cliente |
| Estado visible | Es el único estado operativo que el cliente puede consultar |
| Estado financiero | Solo debe mostrarse información financiera visible para el cliente |
| Observaciones internas | No deben mostrarse al cliente |
| Auditoría interna | No debe mostrarse al cliente |
| Validación backend | La propiedad del pedido y visibilidad del estado no deben depender únicamente del frontend |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El cliente ingresa al sistema desde Web o Android. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El cliente accede a la sección de sus pedidos. | Si no tiene pedidos, el sistema muestra una vista vacía o mensaje correspondiente. |
| 3 | El cliente selecciona un pedido propio. | Si el pedido no existe o no está disponible, el sistema informa la situación. |
| 4 | El sistema valida que el pedido pertenece al cliente autenticado. | Si el pedido no pertenece al cliente, el sistema rechaza la consulta. |
| 5 | El sistema recupera el estado visible del pedido. | Si no existe estado visible definido, el sistema muestra un mensaje controlado y registra el problema si corresponde. |
| 6 | El sistema recupera información financiera visible si corresponde. | Si no existe información financiera visible, no se muestra información interna ni datos ambiguos. |
| 7 | El sistema muestra el estado visible y mensajes asociados. | Si ocurre un error de consulta, informa la falla sin exponer información interna. |
| 8 | El cliente visualiza el avance del pedido. | Si el estado requiere acción del cliente, el sistema muestra la próxima acción esperada. |
| 9 | El sistema mantiene ocultos estados internos, observaciones internas y auditoría. | Si el frontend intenta solicitarlos, backend/RLS debe impedir el acceso. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios. No se expone al cliente |
| Estado visible al cliente | Sin cambios. Se consulta para mostrarlo |
| Estado financiero | Sin cambios. Se consulta solo información visible al cliente |
| Estado técnico de impresión | Sin cambios. No se consulta ni modifica por este flujo |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Consulta de estado visible | Puede registrarse si se decide auditar consultas relevantes del cliente |
| Intento de acceso a pedido ajeno | Debe registrarse cuando un usuario intenta consultar un pedido no propio |
| Falta de estado visible definido | Debe registrarse si un pedido no tiene estado visible válido |
| Error de consulta de estado | Debe registrarse si ocurre una falla técnica relevante |

## 11. Observaciones

- Este caso de uso no modifica pedidos.
- Este caso de uso no cambia estados.
- Este caso de uso no autoriza producción.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no ejecuta impresión.
- El cliente solo debe ver el estado visible del pedido.
- El estado interno debe quedar reservado para usuarios internos autorizados.
- La información financiera visible debe estar separada del estado financiero interno.
- Web y Android deben mostrar estados consistentes porque consumen el mismo backend.
- La seguridad debe ser backend-first: no alcanza con ocultar campos en la interfaz.
- El estado visible debe estar diseñado para ser comprensible para el cliente.

## 12. Poscondición

Al finalizar correctamente:

- el cliente visualiza el estado visible de su pedido;
- el cliente no accede al estado interno;
- el cliente no accede a observaciones internas;
- el cliente no accede a auditoría administrativa;
- el pedido no cambia de estado;
- no se modifica información financiera;
- no se genera ningún trabajo de impresión;
- el acceso queda protegido por permisos y RLS.

## 13. Criterios de aceptación

- El cliente autenticado puede consultar el estado visible de sus pedidos.
- El sistema rechaza consultas sobre pedidos ajenos.
- El sistema no muestra el estado interno al cliente.
- El sistema no muestra observaciones internas ni auditoría al cliente.
- El sistema muestra información financiera visible solo cuando corresponde.
- Web y Android muestran estados coherentes desde el mismo backend.
- La consulta no modifica datos ni estados.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.