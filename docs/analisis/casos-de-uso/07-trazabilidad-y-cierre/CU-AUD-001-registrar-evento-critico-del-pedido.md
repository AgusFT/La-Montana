# CU-AUD-001 - Registrar evento crítico del pedido

| Campo | Valor |
|---|---|
| ID | CU-AUD-001 |
| Caso de uso | Registrar evento crítico del pedido |
| Área | Trazabilidad y cierre |
| Actor principal | Sistema |
| Actores secundarios | Usuario autenticado, Agente de impresión |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-AUD-001, RF-AUD-002, RF-AUD-003, RF-AUD-004, RF-AUD-005, RF-PED-008 |
| RNF relacionados | RNF-AUD-001, RNF-AUD-002, RNF-AUD-003, RNF-AUD-004, RNF-AUD-005, RNF-SEG-004, RNF-RLS-005, RNF-REN-004 |
| HU relacionadas | HU-SIS-006, HU-ADM-006, HU-EMP-003 |
| Reglas críticas relacionadas | RFC-006, RNFC-001, RNFC-003, RNFC-007, RNFC-008 |

## 1. Caso de Uso

Registrar evento crítico del pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Sistema | Registra eventos críticos generados durante el flujo del pedido |
| Usuario autenticado | Ejecuta una acción relevante que dispara un evento crítico |
| Agente de impresión | Puede generar eventos técnicos relevantes asociados a impresión |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual el sistema registra eventos críticos asociados a un pedido.

La trazabilidad es central para La Montaña porque permite reconstruir qué ocurrió con un pedido desde su creación hasta su cierre. Los eventos críticos permiten auditar decisiones, cambios de estado, validaciones, errores, acciones financieras, archivos, impresión, entrega y cierre.

Un evento crítico debe registrar información suficiente para responder:

- qué ocurrió;
- cuándo ocurrió;
- quién o qué componente lo generó;
- sobre qué pedido ocurrió;
- qué datos relevantes cambiaron;
- qué contexto técnico o funcional aplica.

Este caso de uso no representa una acción manual aislada del usuario. Es un mecanismo transversal que puede ser invocado por otros casos de uso cuando ocurre una acción relevante.

## 4. Precondición

- Existe un pedido asociado al evento o una referencia operativa válida.
- Ocurre una acción o situación considerada crítica para trazabilidad.
- El backend Supabase está disponible o existe una política de reintento/registro técnico si corresponde.
- El sistema puede identificar el origen del evento: usuario, sistema, agente de impresión o proceso interno.
- El sistema tiene definido un catálogo o tipo de eventos críticos.
- Las políticas de acceso protegen los registros de auditoría.
- El evento no debe exponer datos sensibles a usuarios no autorizados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Pedido asociado al evento crítico |
| Tipo de evento | Sí | Clasificación del evento registrado |
| Origen del evento | Sí | Usuario, sistema, agente de impresión o proceso interno |
| Usuario asociado | No | Usuario que ejecutó la acción, si corresponde |
| Componente asociado | No | Componente técnico que generó el evento, por ejemplo agente de impresión |
| Estado anterior | No | Estado previo si el evento corresponde a transición |
| Estado nuevo | No | Estado resultante si el evento corresponde a transición |
| Descripción del evento | Sí | Descripción breve del evento |
| Metadata adicional | No | Datos estructurados adicionales necesarios para auditoría |
| Fecha del evento | Sí | Momento en que ocurrió o se registró el evento |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Evento registrado | Registro de auditoría creado correctamente |
| ID del evento | Identificador único del evento |
| Pedido asociado | Pedido al que pertenece el evento |
| Fecha de registro | Momento en que el evento quedó registrado |
| Origen registrado | Usuario, sistema, agente o proceso asociado |
| Metadata registrada | Información adicional relevante |
| Resultado de registro | Confirmación o error controlado |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Registro de eventos | Solo el sistema o flujos autorizados pueden crear eventos críticos |
| RLS / acceso a datos | Los eventos de auditoría deben protegerse mediante políticas de acceso |
| Lectura de auditoría | Solo usuarios autorizados deben consultar eventos críticos |
| Modificación | Los eventos críticos no deben poder modificarse libremente por usuarios comunes |
| Cliente final | El cliente no debe ver auditoría interna salvo información visible diseñada específicamente |
| Agente de impresión | Solo puede generar eventos técnicos dentro de su alcance |
| Validación backend | El registro debe realizarse en backend, RPC, Edge Function o mecanismo equivalente |
| Integridad | El evento debe quedar asociado al pedido correcto y no romper trazabilidad |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | Ocurre una acción relevante dentro de un caso de uso del sistema. | Si la acción no es crítica, puede no generarse evento de auditoría. |
| 2 | El sistema identifica que la acción requiere registro de evento crítico. | Si no puede clasificar el evento, usa tipo genérico o registra alerta técnica según política. |
| 3 | El sistema recopila datos mínimos del evento. | Si faltan datos obligatorios, rechaza el registro o lo marca como incompleto según criticidad. |
| 4 | El sistema valida el pedido asociado. | Si el pedido no existe, registra inconsistencia o rechaza el evento según reglas. |
| 5 | El sistema identifica el origen del evento. | Si no puede identificar origen, registra origen sistema/desconocido según política de auditoría. |
| 6 | El sistema registra el evento crítico. | Si falla el registro, debe generar alerta técnica o reintento si corresponde. |
| 7 | El sistema protege el evento mediante políticas de acceso. | Si no puede aplicar protección, no debe exponer el evento a usuarios no autorizados. |
| 8 | El evento queda disponible para consulta autorizada posterior. | Si la consulta falla luego, debe manejarse en casos de uso de historial o auditoría. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios directo, salvo que el evento documente una transición ocurrida por otro caso de uso |
| Estado visible al cliente | Sin cambios directo |
| Estado financiero | Sin cambios directo, salvo que el evento documente una operación financiera ocurrida por otro caso de uso |
| Estado técnico de impresión | Sin cambios directo, salvo que el evento documente un reporte técnico ocurrido por otro caso de uso |
| Estado de cierre | Sin cambios directo, pero puede aportar evidencia necesaria para cierre consistente |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Evento crítico registrado | Cuando se crea un registro de auditoría |
| Error al registrar evento crítico | Cuando falla el registro de auditoría |
| Evento crítico incompleto | Cuando se registra un evento con información mínima incompleta |
| Intento no autorizado de escribir auditoría | Cuando un usuario o componente no autorizado intenta registrar eventos |
| Acceso no autorizado a auditoría | Cuando se intenta consultar eventos sin permisos |

## 11. Observaciones

- Este caso de uso es transversal a todo el sistema.
- Este caso de uso no modifica el pedido por sí solo.
- Este caso de uso no cambia estados por sí solo.
- Este caso de uso no registra cobros por sí solo.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no cierra pedidos.
- La auditoría debe ser resistente a modificaciones indebidas.
- La auditoría debe permitir reconstruir el recorrido del pedido.
- El cierre del pedido requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.
- Los eventos críticos deben diseñarse para ser útiles en soporte, operación y revisión posterior.
- La información sensible debe protegerse según permisos.
- La estrategia final de auditoría debe alinearse con el modelo de datos y RLS.

## 12. Poscondición

Al finalizar correctamente:

- el evento crítico queda registrado;
- el evento queda asociado al pedido correspondiente;
- el origen del evento queda registrado;
- la metadata relevante queda almacenada;
- el evento queda protegido por permisos y RLS;
- el evento queda disponible para consulta autorizada;
- la trazabilidad del pedido se fortalece;
- no se modifican estados ni datos de negocio por este caso de uso de forma directa.

## 13. Criterios de aceptación

- El sistema registra eventos críticos asociados al pedido.
- El evento registra tipo, origen, fecha, pedido asociado y descripción.
- El evento puede registrar usuario, componente, estado anterior, estado nuevo y metadata adicional.
- Los eventos quedan protegidos contra acceso no autorizado.
- Los eventos no pueden ser modificados libremente por usuarios comunes.
- El registro de evento no modifica estados por sí solo.
- El evento queda disponible para consulta autorizada posterior.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.