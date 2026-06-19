# CU-REV-002 - Aprobar pedido para producción

| Campo | Valor |
|---|---|
| ID | CU-REV-002 |
| Caso de uso | Aprobar pedido para producción |
| Área | Revisión administrativa |
| Actor principal | Administrador |
| Actores secundarios | Empleado autorizado, Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-REV-001, RF-REV-002, RF-REV-005, RF-PED-003, RF-PED-005, RF-ARC-004, RF-ARC-005, RF-ARC-007, RF-EST-001, RF-EST-002, RF-EST-003, RF-EST-004, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-AUT-006, RNF-RLS-002, RNF-RLS-004, RNF-ARC-002, RNF-AUD-001, RNF-AUD-002, RNF-AUD-004, RNF-REN-004 |
| HU relacionadas | HU-ADM-001, HU-ADM-002, HU-SIS-001 |
| Reglas críticas relacionadas | RFC-001, RFC-002, RFC-003, RFC-004, RFC-007, RFC-008, RNFC-001, RNFC-003, RNFC-004 |

## 1. Caso de Uso

Aprobar pedido para producción.

## 2. Actores

| Actor | Participación |
|---|---|
| Administrador | Aprueba el avance del pedido hacia producción luego de la revisión administrativa |
| Empleado autorizado | Puede aprobar el pedido solo si cuenta con permisos explícitos para esta acción |
| Sistema | Valida permisos, estado del pedido, archivos, reglas de negocio, estados asociados y registra auditoría |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un administrador o empleado autorizado aprueba un pedido para que pueda avanzar hacia producción.

La aprobación para producción representa una decisión administrativa interna. Esta decisión confirma que el pedido fue revisado y que cumple las condiciones mínimas para continuar el flujo operativo.

Aprobar un pedido para producción no debe confundirse con generar un trabajo de impresión ni con ejecutar impresión. La generación de print jobs se documenta en casos de uso del dominio `05-impresion`.

Antes de aprobar el pedido, el sistema debe verificar que:

- el pedido exista;
- el pedido esté en una etapa que permita aprobación;
- el usuario tenga permisos suficientes;
- los datos mínimos del pedido estén completos;
- los archivos requeridos estén cargados y revisados según corresponda;
- no existan bloqueos administrativos evidentes;
- se respeten reglas críticas como revisión humana previa y separación de estados.

Si el pedido supera 200 carillas o puede requerir seña, la aprobación debe contemplar que el flujo financiero correspondiente se evalúe antes de permitir producción efectiva.

## 4. Precondición

- El administrador o empleado autorizado está autenticado.
- El usuario tiene permisos para aprobar pedidos para producción.
- El pedido existe.
- El pedido fue creado por un cliente.
- El pedido fue revisado administrativamente.
- El pedido se encuentra en una etapa compatible con aprobación para producción.
- El pedido no fue rechazado.
- El pedido no fue cerrado.
- El pedido no tiene inconsistencias críticas conocidas.
- Los archivos requeridos fueron revisados, validados o se encuentran en una condición aceptable.
- El backend Supabase está disponible.
- Las políticas de acceso deben impedir aprobaciones no autorizadas.
- El sistema puede registrar auditoría de la decisión administrativa.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido que se desea aprobar |
| Usuario interno autenticado | Sí | Administrador o empleado autorizado que realiza la aprobación |
| Confirmación de aprobación | Sí | Confirmación explícita de aprobación para avanzar a producción |
| Observación interna | No | Comentario administrativo asociado a la aprobación |
| Resultado de revisión de archivos | No | Información disponible sobre estado de archivos revisados |
| Evaluación financiera preliminar | No | Información sobre si el pedido requiere seña o validación financiera previa |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Pedido aprobado | Pedido marcado como aprobado para avanzar en el flujo productivo |
| Estado interno actualizado | Estado operativo actualizado según modelo definido |
| Estado visible al cliente | Estado visible coherente con la aprobación, si corresponde |
| Estado financiero | Estado conservado o derivado a evaluación de seña si corresponde |
| Usuario aprobador | Usuario que realizó la aprobación |
| Fecha de aprobación | Momento en que se registró la aprobación |
| Evento de auditoría | Registro de trazabilidad asociado a la decisión administrativa |
| Próximo paso del flujo | Producción, seña pendiente, cotización, preparación o etapa equivalente según reglas definidas |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere administrador o empleado autorizado autenticado |
| Autorización | Solo usuarios internos con permiso explícito pueden aprobar pedidos para producción |
| RLS / acceso a datos | El acceso al pedido, archivos, estados y auditoría debe estar protegido por políticas de seguridad |
| Estado del pedido | Solo pedidos en etapa aprobable pueden avanzar |
| Archivos | Los archivos necesarios deben estar revisados, validados o aceptados según reglas del flujo |
| Estado financiero | Si corresponde seña, el pedido no debe quedar habilitado para producción efectiva sin resolver esa condición |
| Cliente final | El cliente no puede aprobar pedidos para producción |
| Validación backend | La aprobación no debe depender únicamente del frontend |
| Auditoría | La aprobación debe quedar registrada con usuario, fecha, contexto y resultado |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El administrador o empleado autorizado ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a pedidos revisados o pendientes de decisión administrativa. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona el pedido a aprobar. | Si el pedido no existe o no está disponible, el sistema informa la situación. |
| 4 | El sistema valida permisos sobre el pedido seleccionado. | Si el usuario no tiene permiso para aprobar, el sistema rechaza la operación y registra el intento si corresponde. |
| 5 | El sistema valida que el pedido esté en una etapa aprobable. | Si el pedido no está en una etapa compatible, se bloquea la aprobación. |
| 6 | El sistema muestra datos relevantes del pedido, archivos, estados y observaciones internas. | Si faltan datos críticos, se indica que no puede aprobarse hasta completar información. |
| 7 | El usuario revisa la información y confirma la aprobación para producción. | Si el usuario cancela, no se aplica ningún cambio. |
| 8 | El sistema valida que los archivos requeridos estén revisados o aceptados según corresponda. | Si hay archivos rechazados, faltantes o no revisados, se bloquea la aprobación o se deriva a corrección. |
| 9 | El sistema evalúa reglas financieras aplicables, incluyendo seña por más de 200 carillas si corresponde. | Si requiere seña pendiente, el sistema deriva a estado financiero correspondiente y no habilita producción efectiva. |
| 10 | El sistema registra la decisión administrativa de aprobación. | Si no puede registrar la decisión, evita avanzar el pedido. |
| 11 | El sistema actualiza estados correspondientes según el modelo definido. | Si no puede actualizar estados de forma consistente, revierte o bloquea la operación. |
| 12 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 13 | El sistema informa que el pedido fue aprobado para continuar el flujo. | Si ocurre un error final, informa la falla y evita duplicar decisiones administrativas. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Cambia a aprobado para producción, pendiente de seña, pendiente de preparación o equivalente según reglas definidas |
| Estado visible al cliente | Puede actualizarse a un estado visible coherente, por ejemplo en revisión completada, aprobado, pendiente de pago/seña o equivalente |
| Estado financiero | Sin cambios si no aplica condición financiera. Si supera 200 carillas, debe requerir seña del 30% antes de producción efectiva |
| Estado técnico de impresión | Sin cambios. No se genera ni ejecuta trabajo de impresión en este caso de uso |
| Estado de archivos | Sin cambios directo, pero deben estar validados o aceptados según corresponda |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Pedido aprobado para producción | Cuando un usuario autorizado registra la aprobación |
| Aprobación bloqueada por permisos | Cuando un usuario no autorizado intenta aprobar |
| Aprobación bloqueada por estado inválido | Cuando el pedido no está en etapa aprobable |
| Aprobación bloqueada por archivos | Cuando faltan archivos o existen archivos rechazados/no validados |
| Evaluación financiera requerida | Cuando se detecta que el pedido puede requerir seña |
| Estado actualizado por aprobación | Cuando se actualizan estados internos, visibles o financieros |
| Error de aprobación | Cuando ocurre una falla técnica durante la aprobación |

## 11. Observaciones

- Este caso de uso requiere mediación administrativa humana.
- Este caso de uso no puede ser ejecutado por el cliente final.
- Este caso de uso no genera automáticamente un print job.
- Este caso de uso no ejecuta impresión.
- Este caso de uso no envía archivos a CUPS ni a Raspberry Pi.
- El subsistema de impresión no participa en la decisión de aprobación.
- La aprobación del pedido debe estar respaldada por permisos, estado válido, revisión previa y auditoría.
- Si el pedido supera 200 carillas, debe respetarse la regla de seña del 30%.
- La producción efectiva puede depender de pasos posteriores, como seña, preparación, generación de print job o habilitación de archivo.
- La generación de trabajos de impresión se documenta en casos del dominio `05-impresion`.
- La seguridad debe ser backend-first: no alcanza con ocultar botones en la interfaz.

## 12. Poscondición

Al finalizar correctamente:

- el pedido queda registrado como aprobado para continuar el flujo productivo o derivado a la etapa que corresponda;
- la decisión administrativa queda auditada;
- el usuario que aprobó queda registrado;
- los estados se actualizan de forma consistente;
- no se genera automáticamente ningún trabajo de impresión;
- no se ejecuta impresión;
- si corresponde seña, el pedido no queda habilitado para producción efectiva hasta resolver esa condición;
- el pedido queda disponible para el siguiente paso del flujo.

## 13. Criterios de aceptación

- El administrador o empleado autorizado puede aprobar pedidos si tiene permisos.
- El sistema rechaza aprobaciones de usuarios no autorizados.
- El sistema rechaza aprobaciones de pedidos que no están en etapa aprobable.
- El sistema valida archivos requeridos antes de permitir la aprobación.
- El sistema contempla la regla de seña del 30% si corresponde.
- La aprobación queda registrada para trazabilidad.
- La aprobación no genera automáticamente trabajos de impresión.
- La aprobación no ejecuta impresión.
- La aprobación actualiza estados de forma consistente.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.