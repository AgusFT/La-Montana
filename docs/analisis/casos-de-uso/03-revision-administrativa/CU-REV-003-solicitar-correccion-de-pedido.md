# CU-REV-003 - Solicitar corrección de pedido

| Campo | Valor |
|---|---|
| ID | CU-REV-003 |
| Caso de uso | Solicitar corrección de pedido |
| Área | Revisión administrativa |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema, Cliente |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-REV-001, RF-REV-003, RF-REV-005, RF-PED-006, RF-ARC-005, RF-ARC-007, RF-EST-001, RF-EST-002, RF-EST-004, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-002, RNF-RLS-004, RNF-ARC-002, RNF-AUD-001, RNF-AUD-002, RNF-AUD-004, RNF-USA-003 |
| HU relacionadas | HU-CLI-005, HU-ADM-001, HU-SIS-001 |
| Reglas críticas relacionadas | RFC-001, RFC-002, RFC-003, RFC-004, RFC-007, RFC-008, RNFC-001, RNFC-003, RNFC-004, RNFC-005 |

## 1. Caso de Uso

Solicitar corrección de pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Solicita corrección del pedido cuando detecta datos, archivos o condiciones incompletas |
| Administrador | Solicita corrección con permisos ampliados dentro del flujo de revisión administrativa |
| Sistema | Valida permisos, estado del pedido, registra la solicitud, actualiza estados y deja trazabilidad |
| Cliente | Recibe la indicación de corrección y luego puede completar o modificar la información solicitada |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador solicita al cliente una corrección sobre un pedido.

La solicitud de corrección se utiliza cuando el pedido no puede avanzar todavía porque falta información, existen archivos inválidos, hay dudas operativas, el pedido tiene datos incompletos o se requiere que el cliente realice una aclaración.

Solicitar corrección no aprueba el pedido, no lo rechaza definitivamente, no genera producción y no crea trabajos de impresión. El pedido queda detenido hasta que el cliente realice la corrección correspondiente o hasta que un usuario interno tome una decisión posterior.

El sistema debe registrar la solicitud, indicar claramente qué debe corregirse y mantener separación entre información interna y mensaje visible para el cliente.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos internos para revisar pedidos y solicitar correcciones.
- El pedido existe.
- El pedido pertenece al flujo operativo de la imprenta.
- El pedido se encuentra en una etapa compatible con revisión o corrección.
- El pedido no fue aprobado para producción.
- El pedido no tiene trabajos de impresión autorizados o ejecutados.
- El pedido no fue entregado.
- El pedido no fue cerrado.
- El backend Supabase está disponible.
- El sistema puede registrar observación interna y mensaje visible al cliente si corresponde.
- Las políticas de acceso impiden solicitar correcciones sobre pedidos no autorizados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido sobre el cual se solicita corrección |
| Usuario interno autenticado | Sí | Empleado o administrador que solicita la corrección |
| Motivo de corrección | Sí | Motivo por el cual el pedido requiere corrección |
| Mensaje visible al cliente | Sí | Indicación clara para que el cliente sepa qué debe corregir |
| Observación interna | No | Comentario interno no visible al cliente |
| Archivos relacionados | No | Archivos específicos que requieren corrección, reemplazo o aclaración |
| Tipo de corrección | No | Clasificación opcional: datos incompletos, archivo inválido, cantidad dudosa, punto de entrega, contacto u otro |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Solicitud de corrección registrada | Registro formal de la corrección requerida |
| Estado interno actualizado | Estado interno que indica que el pedido requiere corrección o equivalente |
| Estado visible al cliente actualizado | Estado o mensaje visible que informa al cliente que debe corregir información |
| Mensaje visible al cliente | Indicación concreta de lo que debe corregirse |
| Observación interna | Comentario interno asociado a la revisión, si corresponde |
| Usuario solicitante | Usuario que registró la corrección |
| Fecha de solicitud | Momento en que se registró la solicitud |
| Evento de auditoría | Registro de trazabilidad asociado a la solicitud |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos de revisión pueden solicitar correcciones |
| RLS / acceso a datos | El pedido, archivos, observaciones y eventos deben protegerse mediante políticas de acceso |
| Cliente final | El cliente solo debe ver el mensaje visible de corrección, no observaciones internas |
| Estados | Solo pedidos en etapa compatible pueden pasar a corrección requerida |
| Archivos | Si la corrección involucra archivos, el acceso debe respetar permisos y Storage policies |
| Validación backend | La solicitud de corrección no debe depender únicamente del frontend |
| Auditoría | La solicitud debe quedar registrada con usuario, fecha, motivo y contexto |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a pedidos en revisión o pendientes de decisión. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona el pedido que requiere corrección. | Si el pedido no existe o no está disponible, el sistema informa la situación. |
| 4 | El sistema valida permisos sobre el pedido seleccionado. | Si no tiene permisos, rechaza la operación y registra el intento si corresponde. |
| 5 | El sistema valida que el pedido esté en una etapa compatible con solicitud de corrección. | Si el pedido ya fue aprobado para producción, impreso, entregado o cerrado, bloquea la operación. |
| 6 | El usuario selecciona la opción de solicitar corrección. | Si no tiene permiso para esa acción, el sistema la bloquea. |
| 7 | El usuario ingresa el motivo y el mensaje visible para el cliente. | Si falta información obligatoria, el sistema solicita completarla. |
| 8 | El usuario asocia archivos o datos específicos que requieren corrección, si corresponde. | Si un archivo indicado no pertenece al pedido, el sistema rechaza esa asociación. |
| 9 | El sistema valida motivo, mensaje, permisos y estado del pedido. | Si alguna validación falla, informa el motivo y no registra la solicitud. |
| 10 | El sistema registra la solicitud de corrección. | Si no puede registrar la solicitud, evita actualizar estados para no dejar inconsistencias. |
| 11 | El sistema actualiza el estado interno y visible del pedido según corresponda. | Si no puede actualizar estados consistentemente, revierte o bloquea la operación. |
| 12 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 13 | El sistema informa que la corrección fue solicitada correctamente. | Si ocurre un error final, informa la falla y evita duplicar solicitudes. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Cambia a requiere corrección, pendiente de corrección o equivalente |
| Estado visible al cliente | Cambia a corrección solicitada o equivalente, mostrando mensaje claro para el cliente |
| Estado financiero | Sin cambios directo, salvo que la corrección afecte cantidad/carillas y requiera reevaluación posterior |
| Estado técnico de impresión | Sin cambios. No se genera ni modifica ningún trabajo de impresión |
| Estado de archivos | Puede mantenerse o marcar archivos específicos como observados/requieren corrección según el flujo definido |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Corrección solicitada | Cuando un usuario autorizado solicita corrección del pedido |
| Motivo de corrección registrado | Cuando se guarda el motivo de la corrección |
| Mensaje visible al cliente registrado | Cuando se define la comunicación visible para el cliente |
| Observación interna registrada | Cuando se agrega una observación interna durante la solicitud |
| Archivo asociado a corrección | Cuando se vincula un archivo específico a la corrección solicitada |
| Intento de corrección no autorizado | Cuando un usuario sin permisos intenta solicitar corrección |
| Estado actualizado por corrección | Cuando el pedido cambia a estado de corrección requerida |
| Error al solicitar corrección | Cuando ocurre una falla técnica durante el proceso |

## 11. Observaciones

- Este caso de uso no aprueba el pedido.
- Este caso de uso no rechaza definitivamente el pedido.
- Este caso de uso no autoriza producción.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no ejecuta impresión.
- Este caso de uso no registra cobros ni comprobantes.
- El mensaje visible al cliente debe ser claro y no exponer información interna sensible.
- La observación interna puede tener más detalle operativo, pero no debe mostrarse al cliente.
- Si la corrección involucra archivos, el cliente deberá usar casos del dominio `02-archivos` para cargar o reemplazar archivos.
- Si la corrección involucra datos del pedido, el cliente deberá usar `CU-PED-004-modificar-pedido-antes-de-produccion.md` mientras el pedido siga siendo editable.
- La seguridad debe ser backend-first: no alcanza con ocultar botones en la interfaz.
- La solicitud de corrección debe dejar trazabilidad suficiente para entender quién la generó y por qué.

## 12. Poscondición

Al finalizar correctamente:

- la solicitud de corrección queda registrada;
- el motivo queda asociado al pedido;
- el mensaje visible queda disponible para el cliente;
- el pedido queda en estado de corrección requerida o equivalente;
- el pedido no queda aprobado para producción;
- no se genera ningún trabajo de impresión;
- no se modifica información financiera de forma directa;
- el evento queda auditado;
- el cliente puede completar o corregir la información solicitada en un flujo posterior.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede solicitar corrección si tiene permisos.
- El sistema rechaza solicitudes de usuarios no autorizados.
- El sistema valida que el pedido esté en una etapa compatible.
- La solicitud requiere motivo y mensaje visible para el cliente.
- El mensaje visible no expone información interna sensible.
- El pedido queda marcado como requiere corrección o equivalente.
- La solicitud queda registrada para trazabilidad.
- La solicitud no aprueba ni rechaza definitivamente el pedido.
- La solicitud no genera trabajos de impresión.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.