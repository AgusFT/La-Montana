# CU-PED-004 - Modificar pedido antes de producción

| Campo | Valor |
|---|---|
| ID | CU-PED-004 |
| Caso de uso | Modificar pedido antes de producción |
| Área | Pedidos |
| Actor principal | Cliente |
| Actores secundarios | Sistema |
| Prioridad | P1 Alta |
| Alcance | MVP |
| RF relacionados | RF-PED-006, RF-PED-004, RF-ARC-001, RF-ARC-002, RF-ARC-004, RF-EST-002, RF-EST-003, RF-EST-006, RF-FIN-001, RF-FIN-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-RLS-002, RNF-ARC-002, RNF-AUD-001, RNF-REN-004 |
| HU relacionadas | HU-CLI-003, HU-CLI-005 |
| Reglas críticas relacionadas | RFC-001, RFC-002, RFC-003, RFC-004, RFC-005, RFC-007, RFC-008, RNFC-001, RNFC-003, RNFC-004 |

## 1. Caso de Uso

Modificar pedido antes de producción.

## 2. Actores

| Actor | Participación |
|---|---|
| Cliente | Modifica datos permitidos de un pedido propio antes de que avance a producción |
| Sistema | Valida autenticación, propiedad, permisos, estado del pedido, reglas de negocio, archivos asociados y auditoría |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un cliente modifica datos permitidos de un pedido propio mientras el pedido todavía se encuentra en una etapa editable.

La modificación solo puede realizarse si el pedido todavía no fue habilitado para producción, no fue impreso, no fue entregado y no fue cerrado.

El cliente puede modificar únicamente datos no críticos del pedido, como:

- descripción o aclaraciones del pedido;
- cantidad estimada o datos declarados por el cliente;
- observaciones del cliente;
- punto de entrega preferido, si corresponde;
- datos complementarios de contacto, si el flujo los solicita;
- archivos asociados al pedido, cuando la operación sea permitida y se derive al flujo correspondiente de archivos.

El cliente no puede modificar:

- precios;
- estados internos;
- estados visibles al cliente;
- estados financieros;
- cantidades finales calculadas o validadas por el negocio;
- información administrativa;
- observaciones internas;
- datos de auditoría;
- autorizaciones de producción;
- trabajos de impresión.

Si la modificación afecta datos que pueden cambiar la condición financiera del pedido, por ejemplo la cantidad de carillas, el sistema debe marcar la necesidad de reevaluación financiera o aplicar la validación correspondiente.

## 4. Precondición

- El cliente está autenticado.
- El pedido existe.
- El pedido pertenece al cliente autenticado.
- El pedido se encuentra en un estado editable.
- El pedido no fue habilitado para producción.
- El pedido no tiene trabajos de impresión autorizados o ejecutados.
- El pedido no fue entregado.
- El pedido no fue cerrado.
- El backend Supabase está disponible.
- Las políticas RLS permiten que el cliente acceda únicamente a pedidos propios.
- El sistema puede validar propiedad, permisos y estado antes de permitir cualquier modificación.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido que el cliente desea modificar |
| Usuario autenticado | Sí | Cliente que solicita la modificación |
| Descripción o aclaraciones | No | Texto editable por el cliente para aclarar el pedido |
| Cantidad estimada | No | Cantidad declarada por el cliente, por ejemplo páginas, copias o carillas estimadas |
| Punto de entrega preferido | No | Punto de entrega elegido o solicitado por el cliente, si el flujo lo contempla |
| Observaciones del cliente | No | Comentarios visibles como información aportada por el cliente |
| Archivos asociados | No | Archivos que el cliente desea agregar, reemplazar o eliminar, derivados al flujo de archivos cuando corresponda |
| Canal de acceso | No | Web o Android, según desde dónde se realiza la operación |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Pedido actualizado | Pedido con los datos permitidos modificados |
| Estado interno | Estado operativo conservado o marcado para nueva revisión si corresponde |
| Estado visible al cliente | Estado visible coherente con la modificación realizada |
| Estado financiero | Estado financiero conservado o marcado para reevaluación si la modificación afecta carillas/seña |
| Archivos asociados | Archivos agregados, reemplazados o eliminados cuando el flujo de archivos lo permita |
| Evento de auditoría | Registro de la modificación realizada o del intento rechazado |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere cliente autenticado |
| Autorización | Solo el cliente propietario del pedido puede solicitar la modificación |
| RLS / acceso a datos | El pedido debe estar protegido para que el cliente acceda únicamente a pedidos propios |
| Estado del pedido | Solo se permite modificar pedidos que todavía no avanzaron a producción |
| Datos editables | El cliente solo puede modificar campos permitidos explícitamente |
| Datos protegidos | El cliente no puede modificar precios, estados, datos internos, datos financieros ni auditoría |
| Archivos | Los archivos deben gestionarse mediante mecanismos autorizados y asociados al pedido correspondiente |
| Validación backend | La validación debe realizarse en backend, RLS, RPC, Edge Function o mecanismo equivalente |
| Seguridad | Los intentos de modificación no autorizada deben ser rechazados y auditados cuando corresponda |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El cliente ingresa al sistema desde Web o Android. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El cliente accede a la sección de sus pedidos. | Si no tiene pedidos, el sistema muestra una vista vacía o mensaje correspondiente. |
| 3 | El cliente selecciona un pedido propio. | Si el pedido no existe, el sistema informa que no está disponible. |
| 4 | El sistema valida que el pedido pertenezca al cliente autenticado. | Si el pedido no pertenece al cliente, el sistema bloquea el acceso y registra el intento si corresponde. |
| 5 | El sistema valida que el pedido se encuentre en un estado editable. | Si el pedido ya fue habilitado para producción, impreso, entregado o cerrado, el sistema rechaza la modificación. |
| 6 | El sistema muestra los campos que el cliente puede modificar. | Si el sistema no puede determinar campos editables, bloquea la operación para evitar cambios indebidos. |
| 7 | El cliente modifica los datos permitidos. | Si modifica datos no permitidos, el sistema ignora o rechaza esos cambios. |
| 8 | El cliente confirma la modificación. | Si cancela, no se aplican cambios. |
| 9 | El sistema valida los datos modificados. | Si los datos son inválidos, el sistema informa qué debe corregirse. |
| 10 | El sistema evalúa si la modificación afecta reglas financieras, como cantidad de carillas o seña. | Si la modificación puede afectar la seña, el pedido queda pendiente de reevaluación financiera o se dispara la validación correspondiente. |
| 11 | El sistema actualiza el pedido con los cambios permitidos. | Si ocurre un error técnico, el sistema evita dejar el pedido en estado inconsistente. |
| 12 | El sistema registra evento de auditoría por modificación del pedido. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 13 | El sistema informa al cliente que los cambios fueron guardados. | Si la operación falla, el sistema informa el error y permite reintentar cuando corresponda. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Se mantiene en estado editable o vuelve a quedar pendiente de revisión según la modificación realizada |
| Estado visible al cliente | Se mantiene coherente con la etapa del pedido, por ejemplo pendiente de revisión o corrección enviada |
| Estado financiero | Sin cambios si la modificación no afecta la regla de seña. Si cambia cantidad/carillas, debe reevaluarse la condición financiera |
| Estado técnico de impresión | Sin cambios. No se genera ni modifica ningún trabajo de impresión |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Modificación de pedido | Cuando el cliente modifica datos permitidos del pedido |
| Modificación de datos sensibles rechazada | Cuando el cliente intenta modificar campos no permitidos |
| Modificación no autorizada | Cuando un usuario intenta modificar un pedido ajeno o no editable |
| Revisión financiera requerida | Cuando una modificación puede afectar la regla de seña |
| Archivo asociado modificado | Cuando se agregan, reemplazan o eliminan archivos mediante flujo autorizado |
| Error de modificación | Cuando ocurre una falla técnica durante la actualización |

## 11. Observaciones

- Este caso de uso no autoriza producción.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no permite modificar precios.
- Este caso de uso no permite modificar estados internos, visibles o financieros de forma directa.
- Este caso de uso no permite modificar información administrativa ni observaciones internas.
- La modificación de archivos debe respetar los casos de uso del dominio `02-archivos`.
- La carga de archivos se relaciona con `CU-ARC-001-cargar-archivo-al-pedido.md`.
- La consulta de archivos se relaciona con `CU-ARC-002-consultar-archivos-del-pedido.md`.
- La validación de archivos queda a cargo de casos de revisión o validación específicos.
- Si la modificación afecta cantidad de carillas, debe revisarse la regla de seña del 30%.
- La seguridad debe ser backend-first: no alcanza con ocultar campos en Web o Android.
- Web y Android deben respetar las mismas reglas de backend.

## 12. Poscondición

Al finalizar correctamente:

- el pedido queda actualizado solo con datos permitidos;
- el pedido sigue sin estar autorizado para producción;
- el pedido no genera trabajos de impresión;
- no se modifican datos internos ni administrativos;
- no se modifican estados de forma directa por acción del cliente;
- el estado financiero se mantiene o queda pendiente de reevaluación si corresponde;
- los eventos relevantes quedan registrados para trazabilidad;
- el acceso queda protegido por permisos, RLS y validaciones de backend.

## 13. Criterios de aceptación

- El cliente autenticado puede modificar únicamente pedidos propios.
- El sistema rechaza modificaciones sobre pedidos ajenos.
- El sistema rechaza modificaciones sobre pedidos no editables.
- El sistema solo permite modificar campos definidos como editables para el cliente.
- El cliente no puede modificar precios, estados, datos administrativos, datos financieros ni auditoría.
- El pedido no pasa a producción por ser modificado.
- El sistema no genera trabajos de impresión durante este flujo.
- Si cambia la cantidad de carillas, el sistema marca o dispara la reevaluación de la regla de seña.
- La operación se valida en backend, RLS, RPC, Edge Function o mecanismo equivalente.
- La modificación queda registrada para trazabilidad.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.