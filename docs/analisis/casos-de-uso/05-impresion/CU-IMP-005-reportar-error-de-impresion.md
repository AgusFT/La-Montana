# CU-IMP-005 - Reportar error de impresión

| Campo                        | Valor                                                                                                                            |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| ID                           | CU-IMP-005                                                                                                                       |
| Caso de uso                  | Reportar error de impresión                                                                                                      |
| Área                         | Impresión                                                                                                                        |
| Actor principal              | Agente de impresión                                                                                                              |
| Actores secundarios          | Sistema, CUPS, Empleado o administrador                                                                                          |
| Prioridad                    | P1 Alta                                                                                                                          |
| Alcance                      | Producto base                                                                                                                    |
| RF relacionados              | RF-IMP-006, RF-IMP-007, RF-IMP-001, RF-IMP-003, RF-AUD-001, RF-AUD-002                                                           |
| RNF relacionados             | RNF-SEG-003, RNF-SEG-004, RNF-RLS-007, RNF-AUD-001, RNF-AUD-004, RNF-DIS-002, RNF-DIS-004, RNF-DIS-005, RNF-IMP-003, RNF-IMP-005 |
| HU relacionadas              | HU-EMP-005, HU-IMP-003, HU-IMP-004                                                                                               |
| Reglas críticas relacionadas | RFC-010, RNFC-001, RNFC-006, RNFC-007, RNFC-008                                                                                  |

## 1. Caso de Uso

Reportar error de impresión.

## 2. Actores

| Actor                    | Participación                                                                                              |
| ------------------------ | ---------------------------------------------------------------------------------------------------------- |
| Agente de impresión      | Reporta al backend un error técnico ocurrido durante la ejecución o preparación de un trabajo de impresión |
| Sistema                  | Valida identidad del agente, print job, error reportado, estado técnico y registra trazabilidad            |
| CUPS                     | Puede proveer información técnica del error cuando corresponda                                             |
| Empleado o administrador | Consulta el error reportado para tomar una decisión operativa posterior                                    |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual el agente/gateway de impresión reporta un error técnico asociado a un trabajo de impresión.

El error puede originarse durante la consulta del trabajo, acceso al archivo, preparación del archivo, comunicación con CUPS, envío a la cola de impresión, ejecución de impresión, falta de impresora disponible, error de cola, archivo inválido para CUPS u otra falla técnica.

Reportar un error de impresión no debe tomar decisiones de negocio. El agente no puede rechazar pedidos, cerrar pedidos, registrar cobros, modificar estados financieros ni decidir si el pedido debe volver a producción.

El reporte de error permite dejar trazabilidad técnica para que un empleado o administrador pueda revisar el problema y tomar una acción operativa posterior.

## 4. Precondición

* El agente de impresión está identificado o autenticado mediante el mecanismo técnico definido.
* El agente tiene permisos para reportar errores técnicos.
* El print job existe o el agente tiene una referencia técnica válida del intento de ejecución.
* El print job fue previamente autorizado por el backend.
* El print job está asociado a un pedido válido.
* El error ocurrió durante preparación, acceso a archivo, envío a CUPS, ejecución técnica o actualización de estado.
* El backend Supabase está disponible o el agente puede almacenar temporalmente el error para reintento posterior.
* Las políticas de acceso impiden que agentes no autorizados reporten errores sobre trabajos ajenos o inexistentes.

## 5. Datos de entrada

| Dato                       | Obligatorio | Descripción                                                                                                     |
| -------------------------- | ----------- | --------------------------------------------------------------------------------------------------------------- |
| ID del print job           | Sí          | Identificador del trabajo de impresión afectado                                                                 |
| Identificador del agente   | Sí          | Identificador técnico del agente/gateway que reporta el error                                                   |
| Credencial o token técnico | Sí          | Mecanismo de autenticación/autorización del agente                                                              |
| Tipo de error              | Sí          | Clasificación del error, por ejemplo archivo, CUPS, impresora, red, permisos, cola o ejecución                  |
| Mensaje técnico            | Sí          | Descripción técnica del error ocurrido                                                                          |
| Código de error            | No          | Código devuelto por CUPS, sistema operativo, backend o agente                                                   |
| ID técnico de CUPS         | No          | Identificador del trabajo dentro de CUPS si existe                                                              |
| Timestamp técnico          | No          | Momento en que el agente detectó el error                                                                       |
| Metadata técnica           | No          | Información adicional como impresora, cola, archivo, intento, páginas, respuesta de CUPS u otros datos técnicos |

## 6. Datos de salida

| Dato                       | Descripción                                                                        |
| -------------------------- | ---------------------------------------------------------------------------------- |
| Error registrado           | Registro técnico del error asociado al print job                                   |
| Estado técnico actualizado | Estado del print job actualizado a error, fallido, requiere atención o equivalente |
| Print job asociado         | Trabajo de impresión afectado por el error                                         |
| Pedido asociado            | Pedido vinculado al print job                                                      |
| Agente que reporta         | Agente/gateway que informó el error                                                |
| Evento de auditoría        | Registro de trazabilidad asociado al error                                         |
| Respuesta al agente        | Confirmación o rechazo del reporte de error                                        |

## 7. Permisos y seguridad

| Aspecto               | Regla                                                                                               |
| --------------------- | --------------------------------------------------------------------------------------------------- |
| Autenticación técnica | El agente debe identificarse mediante token, credencial o mecanismo técnico definido                |
| Autorización          | El agente solo puede reportar errores sobre trabajos autorizados o asignados                        |
| RLS / acceso a datos  | El acceso del agente a print jobs y reportes técnicos debe estar limitado por políticas específicas |
| Estados de negocio    | El agente no puede modificar estado financiero, cobros, cierre ni decisiones administrativas        |
| Estado técnico        | Solo puede actualizarse el estado técnico del print job según reglas permitidas                     |
| Validación backend    | El reporte no debe depender únicamente de información enviada por el agente                         |
| Auditoría             | Todo error relevante debe quedar registrado con agente, fecha, print job y contexto                 |
| Información sensible  | El mensaje técnico no debe exponer datos sensibles innecesarios a usuarios no autorizados           |

## 8. Flujo principal

| Paso | Flujo principal                                                                                         | Flujo alternativo / excepciones                                                                                            |
| ---- | ------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| 1    | El agente detecta un error durante preparación, acceso a archivo, envío a CUPS o ejecución técnica.     | Si el error ocurre antes de identificar un print job, el agente registra un error técnico general según política definida. |
| 2    | El agente prepara el reporte de error con datos técnicos disponibles.                                   | Si faltan datos opcionales, reporta al menos tipo y mensaje de error.                                                      |
| 3    | El agente envía el reporte al backend.                                                                  | Si no puede conectarse, almacena temporalmente o reintenta según política técnica.                                         |
| 4    | El sistema valida la identidad técnica del agente.                                                      | Si la credencial es inválida, rechaza el reporte.                                                                          |
| 5    | El sistema valida que el print job existe y está autorizado.                                            | Si no existe o no corresponde, rechaza o registra inconsistencia técnica.                                                  |
| 6    | El sistema valida que el agente puede reportar sobre ese print job.                                     | Si no está autorizado, rechaza el reporte.                                                                                 |
| 7    | El sistema valida el tipo de error reportado.                                                           | Si el tipo de error no es reconocido, lo registra como error técnico genérico o solicita clasificación válida.             |
| 8    | El sistema registra el error técnico asociado al print job.                                             | Si no puede registrar el error, no debe actualizar el estado técnico principal.                                            |
| 9    | El sistema actualiza el estado técnico del print job a error, fallido, requiere atención o equivalente. | Si la transición técnica no es válida, marca el reporte para revisión técnica.                                             |
| 10   | El sistema registra evento de auditoría.                                                                | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente.                                               |
| 11   | El sistema responde al agente confirmando recepción y resultado.                                        | Si ocurre un error final, informa rechazo o reintento según corresponda.                                                   |
| 12   | El error queda disponible para consulta operativa interna.                                              | Si no puede mostrarse en vistas internas, debe quedar igualmente registrado para diagnóstico.                              |

## 9. Impacto en estados

| Estado                      | Impacto                                                                                           |
| --------------------------- | ------------------------------------------------------------------------------------------------- |
| Estado interno              | Sin cambios directo, salvo que el modelo defina marcar el pedido como requiere atención operativa |
| Estado visible al cliente   | Sin cambios directo, salvo que exista mapeo visible definido para errores operativos              |
| Estado financiero           | Sin cambios                                                                                       |
| Estado técnico de impresión | Cambia a error, fallido, requiere atención o equivalente                                          |
| Estado de cierre            | Sin cambios. El pedido no se cierra por este reporte                                              |

## 10. Eventos de auditoría

| Evento                             | Cuándo se registra                                                                          |
| ---------------------------------- | ------------------------------------------------------------------------------------------- |
| Error de impresión reportado       | Cuando el agente informa un error técnico                                                   |
| Estado técnico actualizado a error | Cuando el backend actualiza el estado técnico del print job                                 |
| Reporte de error rechazado         | Cuando el reporte no es válido o no está autorizado                                         |
| Agente no autorizado               | Cuando un agente inválido intenta reportar error                                            |
| Error de archivo                   | Cuando el error está relacionado con acceso, descarga, formato o disponibilidad del archivo |
| Error de CUPS                      | Cuando CUPS rechaza o falla al procesar el trabajo                                          |
| Error de impresora o cola          | Cuando la cola o impresora no está disponible                                               |
| Error al registrar reporte         | Cuando ocurre una falla técnica al guardar el error                                         |

## 11. Observaciones

* Este caso de uso reporta errores técnicos; no toma decisiones de negocio.
* Este caso de uso no genera print jobs.
* Este caso de uso no ejecuta impresión.
* Este caso de uso no registra cobros.
* Este caso de uso no cierra pedidos.
* Este caso de uso no rechaza automáticamente el pedido.
* El agente de impresión no decide si un pedido debe volver a producción, cancelarse o cerrarse.
* El error reportado puede requerir intervención de un empleado o administrador.
* Un error técnico puede bloquear temporalmente la ejecución del print job.
* El cierre del pedido no depende solo de imprimir ni de fallar al imprimir.
* Si el error se resuelve, deberá existir un flujo posterior para reintento, cancelación o nueva ejecución según corresponda.
* La información técnica debe ser útil para diagnóstico, pero no debe exponer datos sensibles innecesarios.
* La relación entre errores técnicos e impacto operativo debe definirse en el modelo de estados.

## 12. Poscondición

Al finalizar correctamente:

* el error técnico queda registrado;
* el print job queda asociado al error reportado;
* el estado técnico del print job queda actualizado si corresponde;
* el agente que reportó queda identificado;
* el evento queda auditado;
* no se modifica información financiera;
* no se cierra el pedido;
* no se rechaza el pedido automáticamente;
* el error queda disponible para revisión interna.

## 13. Criterios de aceptación

* El agente autenticado puede reportar errores técnicos sobre trabajos autorizados.
* El sistema rechaza reportes de agentes no autorizados.
* El sistema valida que el print job existe o registra inconsistencia controlada.
* El sistema registra tipo, mensaje y contexto del error.
* El sistema actualiza el estado técnico si corresponde.
* El reporte de error no modifica estado financiero ni cierra pedidos.
* El reporte de error no toma decisiones de negocio.
* El error queda disponible para seguimiento interno.
* La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
* El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.

````