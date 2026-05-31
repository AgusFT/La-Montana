# CU-ARC-005 - Habilitar archivo para impresión

| Campo | Valor |
|---|---|
| ID | CU-ARC-005 |
| Caso de uso | Habilitar archivo para impresión |
| Área | Archivos |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-ARC-004, RF-ARC-006, RF-ARC-007, RF-REV-002, RF-IMP-001, RF-IMP-004, RF-AUD-001, RF-AUD-002, RF-AUD-003 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-006, RNF-RLS-007, RNF-ARC-002, RNF-ARC-004, RNF-ARC-005, RNF-IMP-001, RNF-IMP-004 |
| HU relacionadas | HU-EMP-002, HU-ADM-002, HU-IMP-002 |
| Reglas críticas relacionadas | RFC-003, RFC-007, RFC-008, RFC-010, RNFC-001, RNFC-003, RNFC-004, RNFC-006 |

## 1. Caso de Uso

Habilitar archivo para impresión.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Habilita un archivo validado para que pueda ser utilizado en el flujo de impresión |
| Administrador | Habilita archivos para impresión con permisos ampliados y dentro de una decisión operativa autorizada |
| Sistema | Valida permisos, estado del pedido, estado del archivo, relación archivo-pedido, acceso seguro y trazabilidad |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador habilita un archivo asociado a un pedido para que pueda ser utilizado posteriormente por el subsistema de impresión.

Habilitar un archivo para impresión significa marcarlo como apto para ser utilizado en un trabajo de impresión autorizado. Esta acción no debe confundirse con ejecutar la impresión ni con generar automáticamente un print job.

El archivo solo puede habilitarse si fue previamente cargado mediante un mecanismo autorizado, está asociado al pedido correspondiente, fue revisado o validado por un usuario interno autorizado y el pedido se encuentra en una etapa compatible con preparación para producción o impresión.

Este caso de uso no genera trabajos de impresión, no ejecuta CUPS, no envía archivos directamente a la Raspberry Pi y no permite que el subsistema de impresión tome decisiones de negocio.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos internos para habilitar archivos para impresión.
- El pedido existe.
- El archivo existe.
- El archivo está asociado al pedido correspondiente.
- El archivo fue cargado mediante un mecanismo autorizado.
- El archivo fue validado, aceptado o se encuentra en un estado equivalente.
- El pedido fue revisado administrativamente o se encuentra en una etapa que permite preparar impresión.
- El pedido no fue cerrado.
- El pedido no fue cancelado.
- El backend Supabase está disponible.
- Supabase Storage o el mecanismo de almacenamiento definido está disponible.
- Las políticas de acceso deben impedir habilitar archivos de pedidos no autorizados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido al que pertenece el archivo |
| ID del archivo | Sí | Identificador del archivo que se desea habilitar para impresión |
| Usuario interno autenticado | Sí | Empleado o administrador que realiza la habilitación |
| Confirmación de habilitación | Sí | Confirmación explícita de que el archivo queda apto para impresión |
| Observación interna | No | Comentario interno sobre la habilitación del archivo |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Archivo habilitado | Archivo marcado como apto para ser usado por el flujo de impresión |
| Estado del archivo | Estado actualizado a habilitado para impresión o equivalente |
| Pedido asociado | Pedido al que pertenece el archivo habilitado |
| Usuario que habilita | Usuario interno que realizó la habilitación |
| Fecha de habilitación | Momento en que se registró la habilitación |
| Referencia segura del archivo | Referencia interna para posterior acceso autorizado |
| Evento de auditoría | Registro de trazabilidad asociado a la habilitación |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos específicos pueden habilitar archivos para impresión |
| RLS / acceso a datos | El acceso al pedido, archivo y relación archivo-pedido debe estar protegido por políticas de seguridad |
| Storage | El archivo debe permanecer accesible solo mediante mecanismo autorizado |
| Cliente final | El cliente no puede habilitar archivos para impresión |
| Estado del archivo | Solo archivos validados o aceptados pueden habilitarse para impresión |
| Estado del pedido | El pedido debe estar en una etapa compatible con preparación para impresión |
| Validación backend | La habilitación no debe depender únicamente del frontend |
| Auditoría | La habilitación debe quedar registrada con usuario, fecha y contexto |
| Agente de impresión | El agente de impresión no debe acceder al archivo hasta que exista autorización o print job correspondiente |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a la sección interna de pedidos o archivos. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona un pedido. | Si el pedido no existe o no está disponible, el sistema informa la situación. |
| 4 | El sistema valida que el usuario tenga permisos sobre el pedido. | Si no tiene permisos, el sistema rechaza la operación y registra el intento si corresponde. |
| 5 | El usuario selecciona un archivo asociado al pedido. | Si el archivo no existe, fue eliminado o fue reemplazado, el sistema informa la situación. |
| 6 | El sistema valida que el archivo pertenece al pedido seleccionado. | Si el archivo no pertenece al pedido, el sistema bloquea la operación. |
| 7 | El sistema valida que el archivo fue revisado y aceptado. | Si el archivo no fue validado o fue rechazado, no permite habilitarlo para impresión. |
| 8 | El sistema valida que el pedido está en una etapa compatible con preparación para impresión. | Si el pedido todavía no fue revisado o no está habilitado para producción, rechaza la operación. |
| 9 | El usuario confirma la habilitación del archivo para impresión. | Si cancela la acción, no se realizan cambios. |
| 10 | El sistema actualiza el estado del archivo a habilitado para impresión o equivalente. | Si no puede actualizar el estado, evita dejar datos inconsistentes. |
| 11 | El sistema registra la referencia segura necesaria para acceso posterior. | Si no puede generar o registrar referencia segura, la operación se rechaza. |
| 12 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 13 | El sistema informa que el archivo quedó habilitado para impresión. | Si ocurre un error final, informa la falla y evita duplicar eventos de habilitación. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Puede mantenerse sin cambios o reflejar que el pedido tiene archivo habilitado para impresión, según el modelo de estados |
| Estado visible al cliente | Sin cambios directo, salvo que luego se defina una comunicación visible específica |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios. No se genera ni ejecuta trabajo de impresión |
| Estado del archivo | Cambia a habilitado para impresión o equivalente |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Archivo habilitado para impresión | Cuando un usuario autorizado habilita el archivo |
| Intento de habilitación no autorizada | Cuando un usuario sin permisos intenta habilitar un archivo |
| Archivo no validado | Cuando se intenta habilitar un archivo no validado o rechazado |
| Pedido no habilitado para producción | Cuando se intenta habilitar un archivo de un pedido que todavía no corresponde preparar para impresión |
| Archivo no pertenece al pedido | Cuando se detecta una relación inválida entre pedido y archivo |
| Referencia segura generada | Cuando se registra una referencia interna para acceso posterior autorizado |
| Error de habilitación | Cuando ocurre una falla técnica durante la habilitación |

## 11. Observaciones

- Este caso de uso no genera un print job.
- Este caso de uso no ejecuta impresión.
- Este caso de uso no envía archivos a CUPS.
- Este caso de uso no permite que Raspberry Pi, CUPS o el agente decidan si un pedido puede imprimirse.
- El archivo habilitado solo queda disponible para ser usado posteriormente por un flujo autorizado de impresión.
- El agente de impresión debe acceder únicamente a archivos vinculados con trabajos autorizados.
- El cliente final no puede habilitar archivos para impresión.
- El archivo debe haber sido revisado y aceptado antes de ser habilitado.
- El pedido debe encontrarse en una etapa compatible con preparación para impresión.
- La habilitación debe estar protegida por permisos, RLS, Storage policies, signed URLs o mecanismo equivalente.
- La generación del print job se documenta en casos de uso del dominio `05-impresion`.
- La ejecución del trabajo de impresión se documenta en casos de uso del dominio `05-impresion`.
- Este caso puede ser una condición previa para generar un trabajo de impresión autorizado.

## 12. Poscondición

Al finalizar correctamente:

- el archivo queda marcado como habilitado para impresión o equivalente;
- el archivo sigue asociado al pedido correspondiente;
- el usuario que habilitó el archivo queda registrado;
- el evento de habilitación queda auditado;
- no se genera ningún trabajo de impresión;
- no se ejecuta impresión;
- no se modifica información financiera;
- el acceso posterior al archivo queda condicionado a autorización y políticas de seguridad;
- el archivo puede ser usado posteriormente en un flujo de impresión autorizado.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede habilitar archivos si tiene permisos.
- El sistema rechaza habilitaciones realizadas por usuarios no autorizados.
- El sistema valida que el archivo pertenece al pedido correspondiente.
- El sistema impide habilitar archivos rechazados, eliminados, reemplazados o no disponibles.
- El sistema impide habilitar archivos que no fueron previamente validados o aceptados.
- El sistema valida que el pedido se encuentra en una etapa compatible con preparación para impresión.
- El archivo queda marcado como habilitado para impresión o equivalente.
- La habilitación queda registrada para trazabilidad.
- La habilitación no genera trabajos de impresión.
- La habilitación no ejecuta impresión.
- El agente de impresión no accede al archivo sin autorización posterior.
- La habilitación se realiza mediante backend, RLS, Storage policies, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.