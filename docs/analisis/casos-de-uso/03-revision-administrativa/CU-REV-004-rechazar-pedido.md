# CU-REV-004 - Rechazar pedido

| Campo | Valor |
|---|---|
| ID | CU-REV-004 |
| Caso de uso | Rechazar pedido |
| Área | Revisión administrativa |
| Actor principal | Administrador |
| Actores secundarios | Empleado autorizado, Sistema, Cliente |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-REV-001, RF-REV-004, RF-REV-005, RF-PED-005, RF-EST-001, RF-EST-002, RF-EST-004, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-002, RNF-RLS-004, RNF-AUD-001, RNF-AUD-002, RNF-AUD-004, RNF-USA-003 |
| HU relacionadas | HU-ADM-001, HU-SIS-001 |
| Reglas críticas relacionadas | RFC-001, RFC-002, RFC-003, RFC-004, RFC-007, RNFC-001, RNFC-003, RNFC-005 |

## 1. Caso de Uso

Rechazar pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Administrador | Rechaza un pedido cuando no puede ser procesado o no corresponde continuar el flujo |
| Empleado autorizado | Puede rechazar el pedido solo si cuenta con permisos explícitos para esta acción |
| Sistema | Valida permisos, estado del pedido, reglas de negocio, registra el rechazo y actualiza estados |
| Cliente | Recibe un estado o mensaje visible indicando que el pedido fue rechazado, si corresponde |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un administrador o empleado autorizado rechaza un pedido.

El rechazo del pedido se utiliza cuando, luego de la revisión administrativa, se determina que el pedido no puede continuar el flujo operativo. Puede ocurrir por información inválida, imposibilidad operativa, archivos inadecuados, solicitud fuera del alcance del negocio, incumplimiento de condiciones mínimas o cualquier causa definida por la operación.

Rechazar un pedido es una decisión administrativa interna. No puede realizarla el cliente y no debe ocurrir automáticamente sin mediación humana.

El rechazo debe quedar registrado con motivo, usuario responsable, fecha, estado resultante y trazabilidad suficiente.

Este caso de uso no elimina el pedido, no elimina archivos, no genera trabajos de impresión, no ejecuta impresión y no registra cobros.

## 4. Precondición

- El administrador o empleado autorizado está autenticado.
- El usuario tiene permisos explícitos para rechazar pedidos.
- El pedido existe.
- El pedido pertenece al flujo operativo de la imprenta.
- El pedido se encuentra en una etapa compatible con rechazo.
- El pedido no fue cerrado.
- El pedido no fue entregado.
- El pedido no tiene trabajos de impresión ejecutados.
- El pedido no debe quedar con estados contradictorios al ser rechazado.
- El backend Supabase está disponible.
- El sistema puede registrar motivo, estado y auditoría del rechazo.
- Las políticas de acceso impiden rechazar pedidos no autorizados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido que se desea rechazar |
| Usuario interno autenticado | Sí | Administrador o empleado autorizado que realiza el rechazo |
| Motivo de rechazo | Sí | Motivo por el cual el pedido no puede continuar |
| Mensaje visible al cliente | No | Mensaje controlado que puede mostrarse al cliente |
| Observación interna | No | Comentario interno no visible al cliente |
| Archivos relacionados | No | Archivos específicos que influyen en la decisión de rechazo |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Pedido rechazado | Pedido marcado como rechazado o equivalente |
| Estado interno actualizado | Estado interno que refleja el rechazo |
| Estado visible al cliente actualizado | Estado visible coherente con la decisión de rechazo |
| Motivo registrado | Motivo de rechazo asociado al pedido |
| Mensaje visible al cliente | Información controlada disponible para el cliente, si corresponde |
| Observación interna | Comentario interno asociado al rechazo, si corresponde |
| Usuario que rechaza | Usuario que tomó la decisión |
| Fecha de rechazo | Momento en que se registró la decisión |
| Evento de auditoría | Registro de trazabilidad asociado al rechazo |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere administrador o empleado autorizado autenticado |
| Autorización | Solo usuarios internos con permiso explícito pueden rechazar pedidos |
| RLS / acceso a datos | El pedido, archivos, estados, observaciones y auditoría deben protegerse mediante políticas de acceso |
| Cliente final | El cliente no puede rechazar pedidos |
| Estados | Solo pedidos en etapa compatible pueden ser rechazados |
| Información interna | El mensaje visible al cliente no debe exponer observaciones internas ni información sensible |
| Validación backend | El rechazo no debe depender únicamente del frontend |
| Auditoría | El rechazo debe quedar registrado con usuario, fecha, motivo y contexto |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El administrador o empleado autorizado ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a pedidos en revisión o pendientes de decisión administrativa. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona el pedido que desea rechazar. | Si el pedido no existe o no está disponible, el sistema informa la situación. |
| 4 | El sistema valida permisos sobre el pedido seleccionado. | Si no tiene permisos para rechazar, rechaza la operación y registra el intento si corresponde. |
| 5 | El sistema valida que el pedido esté en una etapa compatible con rechazo. | Si el pedido ya fue cerrado, entregado o impreso, bloquea el rechazo por este flujo. |
| 6 | El usuario selecciona la opción de rechazar pedido. | Si no tiene permiso para esa acción, el sistema la bloquea. |
| 7 | El usuario ingresa el motivo del rechazo. | Si falta el motivo obligatorio, el sistema solicita completarlo. |
| 8 | El usuario ingresa un mensaje visible al cliente y/o una observación interna, si corresponde. | Si el mensaje visible contiene información no permitida, el sistema debe impedir o solicitar corrección. |
| 9 | El sistema valida motivo, permisos, estado y consistencia del rechazo. | Si alguna validación falla, informa el motivo y no registra el rechazo. |
| 10 | El sistema registra la decisión de rechazo. | Si no puede registrar la decisión, no debe cambiar estados. |
| 11 | El sistema actualiza estado interno y estado visible del pedido según corresponda. | Si no puede actualizar estados consistentemente, revierte o bloquea la operación. |
| 12 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 13 | El sistema informa que el pedido fue rechazado correctamente. | Si ocurre un error final, informa la falla y evita duplicar decisiones de rechazo. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Cambia a rechazado, cancelado administrativamente o equivalente según modelo de estados |
| Estado visible al cliente | Cambia a rechazado o equivalente con mensaje controlado si corresponde |
| Estado financiero | Sin cambios directo, salvo que deban anularse condiciones pendientes según reglas futuras |
| Estado técnico de impresión | Sin cambios. No se genera ni modifica ningún trabajo de impresión |
| Estado de archivos | Sin cambios directo. Los archivos permanecen asociados al pedido salvo política posterior |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Pedido rechazado | Cuando un usuario autorizado rechaza el pedido |
| Motivo de rechazo registrado | Cuando se guarda el motivo del rechazo |
| Mensaje visible al cliente registrado | Cuando se define una comunicación visible para el cliente |
| Observación interna registrada | Cuando se agrega una observación interna al rechazo |
| Intento de rechazo no autorizado | Cuando un usuario sin permisos intenta rechazar |
| Rechazo bloqueado por estado inválido | Cuando el pedido no está en una etapa compatible |
| Estado actualizado por rechazo | Cuando se actualizan estados internos o visibles |
| Error al rechazar pedido | Cuando ocurre una falla técnica durante el proceso |

## 11. Observaciones

- Este caso de uso no elimina el pedido.
- Este caso de uso no elimina archivos.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no ejecuta impresión.
- Este caso de uso no registra cobros ni comprobantes.
- El rechazo debe ser una decisión administrativa humana.
- El cliente final no puede rechazar pedidos por este flujo.
- El mensaje visible al cliente debe ser controlado y no exponer información interna sensible.
- La observación interna puede contener más detalle operativo, pero no debe mostrarse al cliente.
- El rechazo debe quedar auditado con motivo, usuario, fecha y contexto.
- Si el pedido ya tiene trabajos de impresión ejecutados, entrega o cierre, el rechazo debe resolverse mediante otro flujo específico.
- La seguridad debe ser backend-first: no alcanza con ocultar botones en la interfaz.

## 12. Poscondición

Al finalizar correctamente:

- el pedido queda marcado como rechazado o equivalente;
- el motivo de rechazo queda registrado;
- el usuario que tomó la decisión queda registrado;
- el estado interno queda actualizado de forma consistente;
- el estado visible al cliente queda actualizado de forma controlada si corresponde;
- no se genera ningún trabajo de impresión;
- no se ejecuta impresión;
- no se modifica información financiera de forma directa;
- el evento queda auditado;
- el pedido no continúa hacia producción por este flujo.

## 13. Criterios de aceptación

- El administrador o empleado autorizado puede rechazar pedidos si tiene permisos.
- El sistema rechaza intentos de rechazo de usuarios no autorizados.
- El sistema valida que el pedido esté en una etapa compatible con rechazo.
- El rechazo requiere motivo obligatorio.
- El mensaje visible al cliente no expone información interna sensible.
- El pedido queda marcado como rechazado o equivalente.
- El rechazo queda registrado para trazabilidad.
- El rechazo no elimina el pedido ni sus archivos.
- El rechazo no genera trabajos de impresión.
- El rechazo no ejecuta impresión.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.