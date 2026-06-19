# CU-IMP-004 - Reportar estado técnico de impresión

| Campo | Valor |
|---|---|
| ID | CU-IMP-004 |
| Caso de uso | Reportar estado técnico de impresión |
| Área | Impresión |
| Actor principal | Agente de impresión |
| Actores secundarios | Sistema, CUPS |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-IMP-006, RF-IMP-007, RF-IMP-001, RF-IMP-003, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-RLS-007, RNF-AUD-001, RNF-AUD-004, RNF-DIS-004, RNF-DIS-005, RNF-IMP-003, RNF-IMP-005 |
| HU relacionadas | HU-IMP-003, HU-IMP-004, HU-EMP-005 |
| Reglas críticas relacionadas | RFC-010, RNFC-001, RNFC-006, RNFC-007, RNFC-008 |

## 1. Caso de Uso

Reportar estado técnico de impresión.

## 2. Actores

| Actor | Participación |
|---|---|
| Agente de impresión | Reporta al backend el avance técnico de un trabajo de impresión ejecutado por Raspberry Pi y CUPS |
| Sistema | Valida identidad del agente, print job, estado reportado, transición técnica y registra auditoría |
| CUPS | Provee información técnica del trabajo de impresión cuando corresponde |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual el agente/gateway de impresión reporta al backend el estado técnico de un trabajo de impresión.

El estado técnico de impresión representa el avance o resultado operativo del print job dentro del subsistema de impresión. Puede indicar situaciones como trabajo en cola, enviado a CUPS, en ejecución, completado, cancelado, fallido o equivalente según el modelo técnico que se defina.

El reporte técnico no debe tomar decisiones de negocio. El agente no puede cerrar pedidos, registrar cobros, aprobar producción ni modificar estados financieros. Solo informa el resultado técnico de la ejecución.

El backend puede usar estos reportes para actualizar el estado técnico del print job, registrar auditoría y permitir que usuarios internos den seguimiento operativo.

## 4. Precondición

- El agente de impresión está identificado o autenticado mediante el mecanismo técnico definido.
- El agente tiene permisos para reportar estados técnicos.
- El print job existe.
- El print job fue previamente autorizado por el backend.
- El print job está asociado a un pedido válido.
- El print job fue tomado, enviado o procesado por el subsistema de impresión.
- El backend Supabase está disponible.
- El sistema tiene definido el estado técnico actual del print job.
- El sistema tiene definido el estado técnico reportado.
- El sistema puede validar si la transición técnica es permitida.
- Las políticas de acceso impiden que agentes no autorizados reporten estados sobre trabajos ajenos o inexistentes.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del print job | Sí | Identificador del trabajo de impresión cuyo estado se reporta |
| Identificador del agente | Sí | Identificador técnico del agente/gateway que reporta el estado |
| Credencial o token técnico | Sí | Mecanismo de autenticación/autorización del agente |
| Estado técnico reportado | Sí | Estado técnico informado por el agente |
| ID técnico de CUPS | No | Identificador del trabajo dentro de CUPS, si existe |
| Mensaje técnico | No | Descripción técnica asociada al estado reportado |
| Timestamp técnico | No | Momento en que el agente o CUPS registró el evento |
| Metadata técnica | No | Información adicional como impresora, cola, páginas procesadas, error code u otros datos técnicos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Estado técnico actualizado | Estado técnico actualizado del print job |
| Print job asociado | Trabajo de impresión afectado por el reporte |
| Pedido asociado | Pedido vinculado al print job |
| Evento técnico registrado | Registro del estado técnico informado |
| Evento de auditoría | Registro de trazabilidad asociado al reporte |
| Respuesta al agente | Confirmación o rechazo del reporte técnico |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación técnica | El agente debe identificarse mediante token, credencial o mecanismo técnico definido |
| Autorización | El agente solo puede reportar estados de trabajos autorizados para ese agente o subsistema |
| RLS / acceso a datos | El acceso del agente a print jobs y reportes técnicos debe estar limitado por políticas específicas |
| Estado técnico | Solo se permiten transiciones técnicas válidas |
| Estados de negocio | El agente no puede modificar estado financiero, cobros, cierre ni decisiones administrativas |
| Validación backend | El reporte no debe depender únicamente de información enviada por el agente |
| Auditoría | Todo reporte técnico relevante debe quedar registrado con agente, fecha, print job y estado |
| Integridad | El sistema debe evitar reportes duplicados, fuera de orden o contradictorios cuando corresponda |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El agente obtiene un cambio de estado técnico desde CUPS o desde su propia ejecución. | Si no hay cambios, no reporta nada o mantiene polling según política técnica. |
| 2 | El agente envía el reporte técnico al backend. | Si no puede conectarse, registra localmente o reintenta según política técnica. |
| 3 | El sistema valida la identidad técnica del agente. | Si la credencial es inválida, rechaza el reporte. |
| 4 | El sistema valida que el print job existe. | Si el print job no existe, rechaza el reporte y registra inconsistencia si corresponde. |
| 5 | El sistema valida que el agente puede reportar sobre ese print job. | Si no está autorizado, rechaza el reporte. |
| 6 | El sistema valida el estado técnico reportado. | Si el estado no existe o no es válido, rechaza el reporte. |
| 7 | El sistema valida la transición entre estado técnico actual y estado reportado. | Si la transición es inválida o contradictoria, rechaza o marca para revisión técnica según reglas. |
| 8 | El sistema registra el evento técnico informado. | Si no puede registrar el evento, no debe actualizar estado técnico principal. |
| 9 | El sistema actualiza el estado técnico del print job. | Si no puede actualizarlo consistentemente, registra error técnico. |
| 10 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente. |
| 11 | El sistema responde al agente confirmando recepción y resultado. | Si ocurre un error final, informa rechazo o reintento según corresponda. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios directo, salvo que el modelo defina una transición operativa basada en estado técnico |
| Estado visible al cliente | Sin cambios directo, salvo que exista mapeo visible definido |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Cambia según el estado técnico reportado y validado |
| Estado de cierre | Sin cambios. El cierre no depende solo de imprimir |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Estado técnico reportado | Cuando el agente informa un nuevo estado técnico |
| Estado técnico actualizado | Cuando el backend actualiza el estado principal del print job |
| Reporte técnico rechazado | Cuando el reporte no es válido o no está autorizado |
| Transición técnica inválida | Cuando el estado reportado no corresponde al flujo permitido |
| Agente no autorizado | Cuando un agente inválido intenta reportar estado |
| Error al registrar estado técnico | Cuando ocurre una falla técnica durante el registro |

## 11. Observaciones

- Este caso de uso no ejecuta impresión; solo reporta estado técnico.
- Este caso de uso no genera print jobs.
- Este caso de uso no decide si un pedido puede avanzar.
- Este caso de uso no registra cobros.
- Este caso de uso no cierra pedidos.
- El agente de impresión no toma decisiones de negocio.
- El cierre del pedido no depende solo de imprimir.
- Un estado técnico completado puede ser insumo para pasos posteriores, pero no debe cerrar el pedido automáticamente.
- Los reportes técnicos deben estar protegidos por autenticación, autorización y auditoría.
- Si el reporte indica error, puede derivar al caso `CU-IMP-005-reportar-error-de-impresion.md`.
- La relación entre estado técnico e impacto operativo debe definirse en el modelo de estados.

## 12. Poscondición

Al finalizar correctamente:

- el reporte técnico queda registrado;
- el estado técnico del print job queda actualizado si la transición es válida;
- el agente que reportó queda identificado;
- el evento queda auditado;
- no se modifica información financiera;
- no se cierra el pedido;
- no se toman decisiones administrativas por parte del agente;
- el sistema queda preparado para que usuarios internos consulten o gestionen el resultado técnico.

## 13. Criterios de aceptación

- El agente autenticado puede reportar estados técnicos de trabajos autorizados.
- El sistema rechaza reportes de agentes no autorizados.
- El sistema valida que el print job existe.
- El sistema valida que el estado técnico reportado existe y es permitido.
- El sistema registra el evento técnico.
- El sistema actualiza el estado técnico si la transición es válida.
- El reporte técnico no modifica estado financiero ni cierra pedidos.
- El agente no toma decisiones de negocio.
- El cierre del pedido no depende solo de imprimir.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.