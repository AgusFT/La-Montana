# CU-PED-005 - Registrar observación interna

| Campo | Valor |
|---|---|
| ID | CU-PED-005 |
| Caso de uso | Registrar observación interna |
| Área | Pedidos |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-PED-007, RF-PED-005, RF-AUD-001, RF-AUD-002, RF-AUD-005 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-005, RNF-AUT-002, RNF-RLS-005, RNF-AUD-001, RNF-AUD-004 |
| HU relacionadas | HU-EMP-003, HU-ADM-006 |
| Reglas críticas relacionadas | RFC-004, RNFC-001, RNFC-003, RNFC-005, RNFC-007 |

## 1. Caso de Uso

Registrar observación interna.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Registra una observación interna asociada a un pedido para dejar trazabilidad operativa |
| Administrador | Registra o consulta observaciones internas con permisos ampliados |
| Sistema | Valida autenticación, permisos, existencia del pedido, visibilidad interna y registro de auditoría |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador registra una observación interna asociada a un pedido.

Las observaciones internas permiten dejar constancia de información operativa, administrativa o de seguimiento que puede ser útil para revisión, producción, entrega, cobro, incidencias o coordinación interna.

La observación interna no debe ser visible para el cliente final, salvo que exista un flujo específico que convierta parte de esa información en una comunicación visible al cliente.

Este caso de uso no modifica estados por sí solo, no autoriza producción, no registra cobros, no genera comprobantes, no crea trabajos de impresión y no cierra pedidos.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos internos para consultar el pedido.
- El usuario tiene permisos para registrar observaciones internas.
- El pedido existe.
- El pedido pertenece al ámbito operativo de la imprenta.
- El backend Supabase está disponible.
- Las políticas de acceso deben impedir que usuarios no autorizados registren observaciones internas.
- La observación debe asociarse a un pedido existente.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido sobre el cual se registra la observación |
| Usuario interno autenticado | Sí | Empleado o administrador que registra la observación |
| Texto de observación | Sí | Contenido de la observación interna |
| Tipo de observación | No | Clasificación opcional, por ejemplo administrativa, producción, entrega, archivo, cobro, incidencia u otra |
| Nivel de importancia | No | Indica si la observación es informativa, relevante o crítica |
| Referencia contextual | No | Información opcional vinculada a archivo, estado, impresión, entrega o cobro |
| Canal de acceso | No | Web interna o Android si el flujo interno está habilitado |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Observación registrada | Observación interna asociada al pedido |
| Usuario autor | Usuario interno que registró la observación |
| Fecha de registro | Momento en que se registró la observación |
| Pedido asociado | Pedido al que pertenece la observación |
| Evento de auditoría | Registro de trazabilidad por creación de observación interna |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere usuario interno autenticado |
| Autorización | Solo empleados o administradores con permisos internos pueden registrar observaciones |
| RLS / acceso a datos | La observación debe estar protegida para que solo sea visible a usuarios internos autorizados |
| Visibilidad cliente | La observación interna no debe ser visible para el cliente final |
| Auditoría | La creación de la observación debe quedar registrada |
| Validación backend | La operación debe validarse en backend, RLS, RPC o mecanismo equivalente |
| Integridad | La observación debe quedar asociada a un pedido existente |
| Datos sensibles | El sistema debe evitar exponer datos internos a usuarios sin permisos |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a la sección interna de pedidos. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona un pedido. | Si el pedido no existe o no está disponible, el sistema informa la situación. |
| 4 | El sistema valida que el usuario tenga permiso para consultar el pedido. | Si no tiene permiso sobre el pedido, se rechaza la operación. |
| 5 | El usuario selecciona la opción de registrar observación interna. | Si no tiene permiso para registrar observaciones, el sistema bloquea la acción. |
| 6 | El sistema muestra el formulario de observación interna. | Si el formulario no puede cargarse, el sistema informa el error. |
| 7 | El usuario ingresa el texto de la observación y datos opcionales. | Si falta el texto obligatorio, el sistema solicita completarlo. |
| 8 | El usuario confirma el registro de la observación. | Si cancela, no se registra la observación. |
| 9 | El sistema valida contenido, pedido asociado y permisos. | Si los datos son inválidos, el sistema rechaza la operación e informa el motivo. |
| 10 | El sistema registra la observación interna asociada al pedido. | Si ocurre un error técnico, evita crear registros incompletos o inconsistentes. |
| 11 | El sistema registra evento de auditoría. | Si falla la auditoría, debe registrarse una alerta técnica o evento equivalente. |
| 12 | El sistema informa que la observación fue registrada correctamente. | Si no puede confirmar el resultado, informa la falla de forma controlada. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios. La observación no modifica el estado del pedido por sí sola |
| Estado visible al cliente | Sin cambios. La observación interna no se expone al cliente final |
| Estado financiero | Sin cambios. La observación no registra cobros ni modifica situación financiera |
| Estado técnico de impresión | Sin cambios. La observación no ejecuta ni modifica trabajos de impresión |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Observación interna creada | Cuando se registra correctamente una observación interna |
| Intento no autorizado de observación | Cuando un usuario intenta registrar una observación sin permisos |
| Observación rechazada | Cuando la observación no cumple validaciones mínimas |
| Error técnico al registrar observación | Cuando ocurre una falla durante el registro |
| Consulta posterior de observación sensible | Puede registrarse si se consulta información interna sensible según políticas de auditoría |

## 11. Observaciones

- Este caso de uso no modifica estados del pedido.
- Este caso de uso no autoriza producción.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no registra cobros ni comprobantes.
- Este caso de uso no cierra pedidos.
- La observación interna no debe mostrarse al cliente final.
- Si se necesita enviar una comunicación visible al cliente, debe definirse otro flujo o caso de uso.
- El texto de la observación debe validarse para evitar contenido vacío o inválido.
- La observación debe asociarse siempre a un pedido existente.
- La seguridad debe ser backend-first: no alcanza con ocultar la opción en la interfaz.
- Las políticas RLS deben proteger tanto el pedido como las observaciones internas asociadas.
- Este caso puede ser usado luego como base para historial interno, auditoría operativa o seguimiento administrativo.

## 12. Poscondición

Al finalizar correctamente:

- la observación queda registrada;
- la observación queda asociada al pedido correspondiente;
- el usuario autor queda registrado;
- el pedido no cambia de estado por esta operación;
- el cliente final no ve la observación interna;
- no se modifica información financiera;
- no se generan trabajos de impresión;
- el evento queda disponible para trazabilidad o auditoría interna según permisos.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede registrar observaciones internas en pedidos autorizados.
- El sistema rechaza observaciones de usuarios no autenticados.
- El sistema rechaza observaciones de usuarios sin permisos internos.
- El sistema rechaza observaciones sin texto obligatorio.
- La observación queda asociada al pedido correcto.
- La observación no modifica estados por sí sola.
- La observación no es visible para el cliente final.
- La operación queda registrada para trazabilidad.
- El acceso a observaciones internas queda protegido por permisos, RLS o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.