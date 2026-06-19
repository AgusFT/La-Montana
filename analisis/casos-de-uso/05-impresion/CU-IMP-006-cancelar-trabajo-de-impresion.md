# CU-IMP-006 - Cancelar trabajo de impresión

| Campo | Valor |
|---|---|
| ID | CU-IMP-006 |
| Caso de uso | Cancelar trabajo de impresión |
| Área | Impresión |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema, Agente de impresión, CUPS |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-IMP-001, RF-IMP-005, RF-IMP-006, RF-IMP-007, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-007, RNF-AUD-001, RNF-AUD-004, RNF-DIS-004, RNF-DIS-005, RNF-IMP-003, RNF-IMP-005 |
| HU relacionadas | HU-EMP-005, HU-IMP-003, HU-IMP-004 |
| Reglas críticas relacionadas | RFC-010, RNFC-001, RNFC-006, RNFC-007, RNFC-008 |

## 1. Caso de Uso

Cancelar trabajo de impresión.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Solicita cancelar un trabajo de impresión por incidencia operativa o decisión interna permitida |
| Administrador | Cancela trabajos de impresión con permisos ampliados |
| Sistema | Valida permisos, estado técnico, reglas de cancelación y registra trazabilidad |
| Agente de impresión | Recibe o consulta la cancelación y detiene el trabajo si técnicamente es posible |
| CUPS | Cancela el trabajo en cola o en ejecución si el subsistema técnico lo permite |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador cancela un trabajo de impresión previamente generado.

La cancelación de un trabajo de impresión es una acción técnica-operativa sobre un print job. No debe confundirse con cancelar el pedido completo, rechazar el pedido, cerrar el pedido o modificar su estado financiero.

La cancelación puede ser necesaria por errores de archivo, impresora incorrecta, parámetros de impresión incorrectos, falla técnica, decisión operativa interna, duplicación de trabajo, cambio solicitado antes de ejecución o cualquier situación que requiera detener la impresión.

El sistema debe validar si el trabajo está en una etapa técnicamente cancelable. Si el trabajo ya fue completado, la cancelación no debe aplicarse por este flujo y deberá registrarse la incidencia correspondiente.

Este caso de uso no decide reglas de negocio, no registra cobros, no cierra pedidos y no elimina archivos.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos para cancelar trabajos de impresión.
- El print job existe.
- El print job está asociado a un pedido válido.
- El print job fue generado previamente por un flujo autorizado.
- El print job se encuentra en estado técnico cancelable.
- El pedido no fue cerrado.
- El backend Supabase está disponible.
- El sistema puede registrar auditoría de la cancelación.
- Si el trabajo ya fue enviado a CUPS, el agente debe poder intentar cancelar técnicamente el trabajo.
- Las políticas de acceso impiden cancelar trabajos no autorizados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del print job | Sí | Identificador del trabajo de impresión a cancelar |
| Usuario interno autenticado | Sí | Empleado o administrador que solicita la cancelación |
| Motivo de cancelación | Sí | Motivo por el cual se cancela el trabajo |
| Observación interna | No | Comentario interno asociado a la cancelación |
| Acción sobre CUPS | No | Indica si debe intentarse cancelar también en CUPS cuando corresponda |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Print job cancelado | Trabajo de impresión marcado como cancelado o equivalente |
| Estado técnico actualizado | Estado técnico del trabajo luego de la cancelación |
| Pedido asociado | Pedido vinculado al trabajo cancelado |
| Usuario que cancela | Usuario interno que solicitó la cancelación |
| Motivo registrado | Motivo de cancelación asociado al trabajo |
| Resultado técnico CUPS | Resultado del intento de cancelación en CUPS si corresponde |
| Evento de auditoría | Registro de trazabilidad asociado a la cancelación |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos específicos pueden cancelar trabajos de impresión |
| RLS / acceso a datos | El acceso al print job, pedido y archivo asociado debe estar protegido por políticas de acceso |
| Estado técnico | Solo pueden cancelarse trabajos en estados técnicamente cancelables |
| Cliente final | El cliente no puede cancelar trabajos de impresión por este flujo interno |
| Agente de impresión | El agente no decide la cancelación de negocio; solo ejecuta la instrucción autorizada si corresponde |
| CUPS | La cancelación en CUPS debe realizarse mediante el subsistema técnico autorizado |
| Validación backend | La cancelación no debe depender únicamente del frontend |
| Auditoría | La cancelación debe quedar registrada con usuario, fecha, motivo y resultado técnico |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a la sección interna de trabajos de impresión. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona el print job que desea cancelar. | Si el trabajo no existe, el sistema informa la situación. |
| 4 | El sistema valida permisos del usuario sobre el print job. | Si el usuario no tiene permisos, rechaza la operación y registra el intento si corresponde. |
| 5 | El sistema valida que el print job está en estado cancelable. | Si el trabajo ya fue completado, cerrado o no es cancelable, rechaza la cancelación. |
| 6 | El usuario ingresa el motivo de cancelación. | Si falta motivo obligatorio, el sistema solicita completarlo. |
| 7 | El usuario confirma la cancelación. | Si cancela la acción, no se modifica el trabajo. |
| 8 | El sistema registra la solicitud de cancelación. | Si no puede registrarla, no debe modificar el estado técnico. |
| 9 | El sistema actualiza el estado técnico del print job a cancelado, cancelación solicitada o equivalente. | Si no puede actualizarlo consistentemente, bloquea o revierte la operación. |
| 10 | Si el trabajo fue enviado a CUPS, el sistema notifica al agente o deja la instrucción de cancelación disponible. | Si el agente no está disponible, el trabajo queda marcado para cancelación pendiente o revisión técnica. |
| 11 | El agente intenta cancelar el trabajo en CUPS si corresponde. | Si CUPS no permite cancelar el trabajo, el agente reporta error o estado técnico correspondiente. |
| 12 | El sistema registra el resultado técnico de la cancelación. | Si el resultado no puede registrarse, debe quedar alerta técnica o evento equivalente. |
| 13 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente. |
| 14 | El sistema informa que la cancelación fue registrada. | Si ocurre un error final, informa la falla y evita duplicar solicitudes de cancelación. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Puede mantenerse o marcar el pedido como requiere atención operativa según reglas del modelo |
| Estado visible al cliente | Sin cambios directo, salvo que exista mapeo visible definido para incidencias |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Cambia a cancelado, cancelación solicitada, cancelación fallida o equivalente |
| Estado de cierre | Sin cambios. El pedido no se cierra por cancelar un trabajo de impresión |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Cancelación de print job solicitada | Cuando un usuario autorizado solicita cancelar un trabajo |
| Print job cancelado | Cuando el sistema marca el trabajo como cancelado |
| Cancelación enviada al agente | Cuando se notifica o deja disponible la instrucción para el agente |
| Cancelación enviada a CUPS | Cuando el agente intenta cancelar el trabajo en CUPS |
| Cancelación rechazada por estado inválido | Cuando el trabajo no se encuentra en estado cancelable |
| Intento no autorizado de cancelación | Cuando un usuario sin permisos intenta cancelar un trabajo |
| Error de cancelación | Cuando ocurre una falla técnica durante la cancelación |

## 11. Observaciones

- Este caso de uso cancela un trabajo de impresión, no el pedido completo.
- Este caso de uso no elimina archivos.
- Este caso de uso no registra cobros.
- Este caso de uso no cierra pedidos.
- Este caso de uso no modifica estado financiero.
- El cliente final no puede ejecutar este flujo interno.
- La cancelación debe estar respaldada por permisos internos y auditoría.
- El agente de impresión no toma decisiones de negocio; solo ejecuta una instrucción autorizada.
- Si el trabajo ya fue completado, no debe cancelarse por este flujo.
- Si la cancelación falla técnicamente, debe reportarse como incidencia o error técnico.
- La cancelación puede requerir generar un nuevo print job posterior si el pedido debe imprimirse nuevamente.
- El cierre del pedido no depende solo del estado técnico de impresión.
- La relación entre cancelación técnica e impacto operativo debe definirse en el modelo de estados.

## 12. Poscondición

Al finalizar correctamente:

- el print job queda cancelado o con cancelación solicitada según el estado técnico real;
- el motivo de cancelación queda registrado;
- el usuario que solicitó la cancelación queda identificado;
- el resultado técnico de CUPS queda registrado si corresponde;
- el evento queda auditado;
- no se modifica información financiera;
- no se cierra el pedido;
- no se elimina el archivo asociado;
- el pedido puede quedar disponible para revisión operativa o generación de un nuevo trabajo si corresponde.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede cancelar trabajos de impresión si tiene permisos.
- El sistema rechaza cancelaciones de usuarios no autorizados.
- El sistema valida que el print job existe.
- El sistema valida que el print job está en estado cancelable.
- La cancelación requiere motivo obligatorio.
- El sistema registra la cancelación para trazabilidad.
- El agente solo ejecuta cancelaciones autorizadas por el backend.
- La cancelación no elimina el pedido ni sus archivos.
- La cancelación no modifica estado financiero ni cierra pedidos.
- Si la cancelación en CUPS falla, el error queda registrado.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.