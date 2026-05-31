# CU-CIE-002 - Validar condiciones de cierre

| Campo | Valor |
|---|---|
| ID | CU-CIE-002 |
| Caso de uso | Validar condiciones de cierre |
| Área | Trazabilidad y cierre |
| Actor principal | Sistema |
| Actores secundarios | Empleado o administrador |
| Prioridad | P0 Crítica |
| Alcance | Producto base |
| RF relacionados | RF-FIN-006, RF-AUD-001, RF-AUD-002, RF-AUD-004, RF-AUD-005, RF-EST-001, RF-EST-002, RF-EST-003, RF-EST-004 |
| RNF relacionados | RNF-SEG-004, RNF-AUT-002, RNF-RLS-004, RNF-AUD-001, RNF-AUD-003, RNF-AUD-004, RNF-REN-004 |
| HU relacionadas | HU-SIS-004, HU-ADM-005, HU-ADM-006 |
| Reglas críticas relacionadas | RFC-006, RNFC-001, RNFC-003, RNFC-007, RNFC-008 |

## 1. Caso de Uso

Validar condiciones de cierre.

## 2. Actores

| Actor | Participación |
|---|---|
| Sistema | Verifica si el pedido cumple las condiciones necesarias para poder cerrarse |
| Empleado | Puede solicitar o consultar la validación de cierre según permisos |
| Administrador | Puede solicitar, revisar o supervisar la validación de cierre con permisos ampliados |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual el sistema valida si un pedido cumple las condiciones necesarias para poder cerrarse.

El cierre del pedido no depende solo de imprimir. Para cerrar un pedido debe existir consistencia entre:

- entrega;
- cobro;
- comprobante;
- auditoría;
- estado final.

Este caso de uso no cierra el pedido por sí solo. Solo verifica si están dadas las condiciones para que el cierre pueda ejecutarse en un caso de uso posterior.

La validación de cierre permite evitar que un pedido se marque como finalizado cuando todavía existen inconsistencias operativas, financieras, documentales o de trazabilidad.

## 4. Precondición

- El pedido existe.
- El pedido pertenece al flujo operativo de la imprenta.
- El pedido no fue cerrado previamente.
- El sistema puede consultar estado interno, estado visible y estado financiero.
- El sistema puede consultar entrega registrada.
- El sistema puede consultar cobros registrados.
- El sistema puede consultar comprobantes asociados.
- El sistema puede consultar eventos de auditoría relevantes.
- El backend Supabase está disponible.
- Las políticas de acceso protegen información financiera, auditoría y cierre.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido a validar para cierre |
| Usuario solicitante | No | Empleado, administrador o proceso del sistema que solicita la validación |
| Estado interno actual | No | Estado interno vigente del pedido |
| Estado financiero actual | No | Estado financiero vigente del pedido |
| Entrega registrada | No | Información de entrega asociada al pedido |
| Cobros registrados | No | Información de pagos, seña o cobro final asociados |
| Comprobantes asociados | No | Comprobantes vinculados al pedido |
| Eventos de auditoría | No | Eventos relevantes del recorrido del pedido |
| Canal de origen | No | Web interna, proceso del sistema, RPC, Edge Function u otro mecanismo |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Resultado de validación | Indica si el pedido está listo o no para cierre |
| Condiciones cumplidas | Lista de condiciones verificadas correctamente |
| Condiciones pendientes | Lista de condiciones faltantes o inconsistentes |
| Estado sugerido | Siguiente estado recomendado, por ejemplo listo para cerrar o requiere revisión |
| Evento de auditoría | Registro de la validación realizada |
| Mensaje interno | Detalle visible para usuarios internos autorizados |
| Mensaje visible al cliente | Información visible si el flujo decide comunicar algo al cliente |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Ejecución del sistema | El sistema puede ejecutar la validación como parte de un flujo autorizado |
| Usuario interno | Empleados o administradores pueden solicitar validación si tienen permisos |
| Cliente final | El cliente no puede forzar validaciones internas de cierre |
| RLS / acceso a datos | Datos financieros, auditoría, comprobantes y cierre deben estar protegidos |
| Información visible | El cliente no debe ver detalles internos de inconsistencias salvo comunicación controlada |
| Validación backend | La validación debe ejecutarse en backend, RPC, Edge Function o mecanismo equivalente |
| Auditoría | La validación y su resultado deben quedar registrados si corresponde |
| Integridad | El sistema no debe devolver resultado positivo si existen condiciones críticas incumplidas |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | Se solicita validar las condiciones de cierre del pedido. | Si el pedido no existe, el sistema rechaza la validación. |
| 2 | El sistema recupera los datos principales del pedido. | Si no puede recuperar datos básicos, informa error controlado. |
| 3 | El sistema verifica que el pedido no esté cerrado previamente. | Si ya está cerrado, informa que no corresponde nueva validación por este flujo. |
| 4 | El sistema valida el estado interno del pedido. | Si el estado interno no es compatible con cierre, marca condición pendiente. |
| 5 | El sistema valida la entrega registrada. | Si no existe entrega cuando es requerida, marca condición pendiente. |
| 6 | El sistema valida la situación financiera. | Si existen pagos pendientes, seña pendiente o saldo no registrado, marca condición pendiente. |
| 7 | El sistema valida comprobantes asociados cuando corresponda. | Si falta comprobante requerido, marca condición pendiente. |
| 8 | El sistema valida existencia de auditoría mínima relevante. | Si faltan eventos críticos, marca condición pendiente o requiere revisión interna. |
| 9 | El sistema verifica consistencia entre estado interno, visible y financiero. | Si encuentra combinación inconsistente, marca condición pendiente. |
| 10 | El sistema genera resultado de validación. | Si no puede determinar resultado por datos incompletos, marca requiere revisión. |
| 11 | El sistema registra evento de auditoría de la validación si corresponde. | Si falla auditoría, debe registrarse alerta técnica o evento equivalente. |
| 12 | El sistema devuelve condiciones cumplidas y pendientes. | Si ocurre un error final, informa la falla sin cerrar el pedido. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios directo. Puede sugerir listo para cerrar o requiere revisión |
| Estado visible al cliente | Sin cambios directo |
| Estado financiero | Sin cambios. Solo se consulta para validar cierre |
| Estado técnico de impresión | Sin cambios. Solo puede consultarse como insumo si corresponde |
| Estado de cierre | No cambia. Este caso solo valida condiciones |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Validación de cierre ejecutada | Cuando se evalúan condiciones de cierre |
| Condición de cierre incumplida | Cuando se detecta entrega, cobro, comprobante, auditoría o estado inconsistente |
| Pedido listo para cierre | Cuando todas las condiciones requeridas están cumplidas |
| Pedido requiere revisión antes de cierre | Cuando existen datos incompletos o inconsistentes |
| Error de validación de cierre | Cuando ocurre una falla técnica durante la validación |

## 11. Observaciones

- Este caso de uso no cierra el pedido.
- Este caso de uso no registra entrega.
- Este caso de uso no registra cobro.
- Este caso de uso no registra comprobante.
- Este caso de uso no modifica estados por sí solo.
- El cierre del pedido requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.
- La impresión completada no es condición suficiente para cerrar el pedido.
- Si faltan datos o existen inconsistencias, el cierre debe bloquearse.
- La validación debe ejecutarse en backend, RPC, Edge Function o mecanismo equivalente.
- Este caso de uso sirve como precondición para `CU-CIE-003-cerrar-pedido.md`.

## 12. Poscondición

Al finalizar correctamente:

- el sistema determina si el pedido está listo para cierre;
- el sistema identifica condiciones cumplidas;
- el sistema identifica condiciones pendientes o inconsistentes;
- el pedido no se cierra por este caso de uso;
- no se modifican pagos, comprobantes ni entrega;
- el resultado queda disponible para el flujo de cierre;
- el evento queda auditado si corresponde.

## 13. Criterios de aceptación

- El sistema valida entrega, cobro, comprobante, auditoría y estado final antes del cierre.
- El sistema bloquea resultado positivo si falta una condición crítica.
- El sistema no cierra el pedido por este caso de uso.
- El sistema informa condiciones pendientes o inconsistentes.
- El sistema mantiene separación entre estado interno, visible y financiero.
- La validación no modifica datos financieros.
- La validación no genera trabajos de impresión.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.