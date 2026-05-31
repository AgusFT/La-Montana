# CU-EST-002 - Actualizar estado interno del pedido

| Campo | Valor |
|---|---|
| ID | CU-EST-002 |
| Caso de uso | Actualizar estado interno del pedido |
| Área | Estados y finanzas |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | Producto base |
| RF relacionados | RF-EST-001, RF-EST-002, RF-EST-003, RF-EST-004, RF-EST-005, RF-PED-005, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-004, RNF-AUD-001, RNF-AUD-002, RNF-AUD-004, RNF-REN-004 |
| HU relacionadas | HU-ADM-004, HU-SIS-002 |
| Reglas críticas relacionadas | RFC-001, RFC-003, RFC-004, RFC-006, RNFC-001, RNFC-003, RNFC-007, RNFC-008 |

## 1. Caso de Uso

Actualizar estado interno del pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Actualiza el estado interno del pedido si tiene permisos para la transición correspondiente |
| Administrador | Actualiza estados internos con permisos ampliados y supervisa consistencia del flujo |
| Sistema | Valida permisos, estado actual, transición permitida, reglas de negocio, consistencia y auditoría |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador actualiza el estado interno de un pedido.

El estado interno representa el avance operativo real del pedido dentro de la imprenta. No necesariamente coincide con el estado visible al cliente ni con el estado financiero.

La actualización del estado interno debe respetar reglas de negocio, permisos y transiciones válidas. El sistema no debe permitir combinaciones inconsistentes entre:

- estado interno;
- estado visible al cliente;
- estado financiero;
- estado técnico de impresión;
- cierre del pedido.

Actualizar el estado interno no debe permitir saltar mediaciones críticas, como la revisión administrativa humana antes de producción. Tampoco debe cerrar un pedido si no existe consistencia entre entrega, cobro, comprobante, auditoría y estado final.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos para actualizar el estado interno.
- El pedido existe.
- El pedido pertenece al flujo operativo de la imprenta.
- El pedido no se encuentra en un estado final que impida cambios por este flujo.
- El backend Supabase está disponible.
- El sistema tiene definido el estado interno actual.
- El sistema tiene definido el estado interno destino.
- El sistema puede validar si la transición entre estados está permitida.
- Las políticas de acceso impiden modificar estados de pedidos no autorizados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido cuyo estado interno se desea actualizar |
| Usuario interno autenticado | Sí | Empleado o administrador que solicita el cambio |
| Estado interno destino | Sí | Nuevo estado interno solicitado |
| Motivo del cambio | No | Justificación o contexto del cambio de estado |
| Observación interna | No | Comentario interno asociado a la transición |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Estado interno actualizado | Nuevo estado interno asignado al pedido |
| Estado visible resultante | Estado visible actualizado o conservado según reglas de mapeo |
| Estado financiero resultante | Estado financiero conservado o actualizado si corresponde |
| Pedido actualizado | Pedido con transición registrada |
| Usuario que actualizó | Usuario interno que solicitó el cambio |
| Fecha de actualización | Momento en que se registró la transición |
| Evento de auditoría | Registro de trazabilidad asociado al cambio de estado |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos pueden modificar estados internos |
| RLS / acceso a datos | El pedido y sus estados deben protegerse mediante políticas de acceso |
| Cliente final | El cliente no puede modificar estados internos |
| Estado visible | El cambio interno solo debe actualizar el estado visible si existe regla explícita de mapeo |
| Estado financiero | No debe modificarse salvo que exista regla explícita y validada |
| Transición | Solo se permiten transiciones definidas como válidas |
| Validación backend | La transición no debe depender únicamente del frontend |
| Auditoría | Todo cambio de estado interno debe quedar registrado con usuario, fecha, estado anterior, estado nuevo y contexto |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede al pedido desde una vista interna. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El sistema muestra el estado interno actual del pedido. | Si no puede obtener el estado actual, bloquea la actualización. |
| 4 | El usuario selecciona un nuevo estado interno permitido. | Si intenta seleccionar un estado no disponible para su rol, el sistema lo bloquea. |
| 5 | El usuario confirma el cambio de estado. | Si cancela, no se modifica el pedido. |
| 6 | El sistema valida permisos del usuario sobre el pedido. | Si no tiene permisos, rechaza la operación y registra el intento si corresponde. |
| 7 | El sistema valida que la transición entre estado actual y estado destino sea válida. | Si la transición no está permitida, rechaza el cambio. |
| 8 | El sistema valida reglas críticas asociadas a la transición. | Si la transición viola revisión administrativa, cierre, cobro, comprobante o auditoría, la bloquea. |
| 9 | El sistema determina si debe actualizar estado visible o financiero según reglas definidas. | Si no existe regla de mapeo, conserva los estados relacionados. |
| 10 | El sistema actualiza el estado interno del pedido. | Si no puede actualizarlo consistentemente, revierte o bloquea la operación. |
| 11 | El sistema actualiza estados relacionados solo si corresponde. | Si una actualización relacionada falla, evita dejar estados contradictorios. |
| 12 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente. |
| 13 | El sistema informa que el estado interno fue actualizado. | Si ocurre un error final, informa la falla y evita duplicar transiciones. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Cambia al estado interno destino si la transición es válida |
| Estado visible al cliente | Puede mantenerse o actualizarse si existe regla explícita de mapeo |
| Estado financiero | Puede mantenerse o actualizarse solo si existe regla financiera explícita |
| Estado técnico de impresión | Sin cambios directo, salvo integración posterior con eventos técnicos de impresión |
| Estado de cierre | No debe quedar cerrado si no se cumplen entrega, cobro, comprobante, auditoría y estado final |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Estado interno actualizado | Cuando el estado interno cambia correctamente |
| Cambio de estado rechazado | Cuando la transición no está permitida |
| Intento no autorizado de cambio de estado | Cuando un usuario sin permisos intenta modificar estados |
| Estado visible actualizado por mapeo | Cuando el cambio interno actualiza también el estado visible |
| Estado financiero actualizado por regla | Cuando el cambio interno afecta estado financiero de forma permitida |
| Error de actualización de estado | Cuando ocurre una falla técnica durante la transición |

## 11. Observaciones

- Este caso de uso no puede ser ejecutado por el cliente final.
- Este caso de uso debe respetar la separación entre estado interno, visible y financiero.
- No toda actualización interna debe impactar el estado visible al cliente.
- No toda actualización interna debe impactar el estado financiero.
- El sistema debe impedir transiciones que salteen revisión administrativa humana antes de producción.
- El sistema debe impedir cierres inconsistentes.
- Si el cambio implica avance hacia producción, deben cumplirse los casos de revisión, archivos, seña y autorización correspondientes.
- Si el cambio depende de impresión técnica, debe integrarse con casos del dominio `05-impresion`.
- La seguridad debe ser backend-first: no alcanza con ocultar opciones en la interfaz.
- Las transiciones válidas deberán formalizarse en el modelo de estados y en el diseño de base de datos.

## 12. Poscondición

Al finalizar correctamente:

- el estado interno queda actualizado;
- el estado anterior y el nuevo estado quedan registrados;
- el usuario responsable queda registrado;
- los estados visibles y financieros se mantienen o actualizan según reglas explícitas;
- el pedido no queda en una combinación de estados inconsistente;
- el evento queda auditado;
- el cliente no obtiene acceso a información interna;
- la operación queda protegida por permisos, RLS y validaciones de backend.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede actualizar estados internos si tiene permisos.
- El sistema rechaza cambios solicitados por usuarios no autorizados.
- El sistema rechaza transiciones no permitidas.
- El sistema impide saltar revisión administrativa antes de producción.
- El sistema mantiene separación entre estado interno, visible y financiero.
- El sistema evita combinaciones inconsistentes entre estados.
- El sistema registra estado anterior, estado nuevo, usuario y fecha.
- El sistema no permite cerrar pedidos sin condiciones de cierre consistentes.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.