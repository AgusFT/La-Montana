# CU-IMP-003 - Ejecutar trabajo de impresión

| Campo | Valor |
|---|---|
| ID | CU-IMP-003 |
| Caso de uso | Ejecutar trabajo de impresión |
| Área | Impresión |
| Actor principal | Agente de impresión |
| Actores secundarios | Sistema, CUPS |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-IMP-001, RF-IMP-002, RF-IMP-003, RF-IMP-004, RF-IMP-005, RF-IMP-006, RF-ARC-006, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-RLS-007, RNF-ARC-004, RNF-AUD-001, RNF-AUD-004, RNF-IMP-001, RNF-IMP-002, RNF-IMP-003, RNF-IMP-004, RNF-IMP-005 |
| HU relacionadas | HU-IMP-001, HU-IMP-002, HU-IMP-003, HU-IMP-004 |
| Reglas críticas relacionadas | RFC-007, RFC-008, RFC-010, RNFC-001, RNFC-003, RNFC-004, RNFC-006 |

## 1. Caso de Uso

Ejecutar trabajo de impresión.

## 2. Actores

| Actor | Participación |
|---|---|
| Agente de impresión | Toma un trabajo autorizado, accede al archivo autorizado y lo envía a CUPS |
| Sistema | Valida estado del trabajo, autorización, acceso al archivo y registra trazabilidad |
| CUPS | Recibe el trabajo técnico de impresión y lo envía a la impresora correspondiente |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual el agente/gateway de impresión ejecuta un trabajo de impresión previamente autorizado.

La ejecución se realiza desde el subsistema de impresión compuesto por Raspberry Pi, CUPS y agente/gateway. El agente no decide si un pedido puede imprimirse: solo ejecuta trabajos que ya fueron autorizados por el backend.

El agente debe acceder únicamente a archivos autorizados y vinculados al print job correspondiente. No debe usar rutas locales del cliente ni acceder a archivos fuera del flujo autorizado.

Este caso de uso ejecuta técnicamente el trabajo de impresión, pero no cierra el pedido, no registra cobros, no valida comprobantes y no modifica reglas de negocio. La actualización técnica posterior del estado de impresión se documenta en casos específicos del dominio `05-impresion`.

## 4. Precondición

- El agente de impresión está identificado o autenticado mediante el mecanismo técnico definido.
- El agente tiene permisos para ejecutar trabajos autorizados.
- El print job existe.
- El print job está autorizado.
- El print job está pendiente o en una etapa técnica compatible con ejecución.
- El print job está asociado a un pedido válido.
- El print job está asociado a un archivo autorizado.
- El archivo está disponible mediante un mecanismo seguro.
- CUPS está disponible en la Raspberry Pi o entorno de impresión.
- La impresora o cola de impresión definida está disponible o puede recibir trabajos.
- El backend Supabase está disponible para consultar o actualizar información técnica del trabajo.
- Las políticas de acceso impiden ejecutar trabajos no autorizados o acceder a archivos no vinculados al print job.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del print job | Sí | Identificador del trabajo de impresión a ejecutar |
| Identificador del agente | Sí | Identificador técnico del agente/gateway que ejecuta el trabajo |
| Credencial o token técnico | Sí | Mecanismo de autenticación/autorización del agente |
| Referencia segura del archivo | Sí | Acceso autorizado al archivo asociado al print job |
| Parámetros de impresión | No | Configuración de impresión, por ejemplo impresora, copias, doble faz, color o tamaño |
| Cola CUPS | No | Cola de impresión destino si el trabajo lo especifica |
| Momento de ejecución | No | Fecha y hora en que el agente inicia la ejecución |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Trabajo enviado a CUPS | Resultado de enviar el archivo a la cola de impresión |
| ID técnico de CUPS | Identificador del trabajo dentro de CUPS si está disponible |
| Estado técnico actualizado | Estado del print job, por ejemplo enviado, en ejecución o error |
| Archivo utilizado | Archivo autorizado usado para ejecutar el trabajo |
| Agente ejecutor | Agente/gateway que tomó el trabajo |
| Evento de auditoría | Registro de trazabilidad asociado a la ejecución |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación técnica | El agente debe identificarse mediante token, credencial o mecanismo técnico definido |
| Autorización | El agente solo puede ejecutar trabajos autorizados |
| RLS / acceso a datos | El acceso del agente a print jobs y archivos debe estar limitado por políticas específicas |
| Archivos | Solo puede accederse al archivo asociado al print job |
| Rutas locales | No se deben usar rutas locales del cliente como mecanismo de impresión |
| Estado del trabajo | Solo pueden ejecutarse trabajos en estado técnico compatible |
| CUPS | El envío a CUPS debe realizarse desde el subsistema de impresión autorizado |
| Validación backend | La ejecución debe validarse contra el backend antes de procesar el trabajo |
| Auditoría | La ejecución debe quedar registrada con agente, fecha, print job y resultado técnico |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El agente obtiene o recibe un print job autorizado. | Si no hay trabajos disponibles, no ejecuta impresión. |
| 2 | El agente valida su credencial técnica contra el backend. | Si la credencial es inválida, el backend rechaza la operación. |
| 3 | El sistema valida que el print job existe y está autorizado. | Si el trabajo no existe, fue cancelado o no está autorizado, se rechaza la ejecución. |
| 4 | El sistema valida que el print job está en estado técnico compatible con ejecución. | Si ya fue ejecutado, cancelado o está bloqueado, se rechaza la ejecución. |
| 5 | El sistema valida que el archivo asociado está autorizado y disponible. | Si el archivo no está disponible o no está autorizado, se bloquea la ejecución. |
| 6 | El agente obtiene acceso seguro al archivo. | Si el acceso expira o falla, el agente informa error y no imprime. |
| 7 | El agente descarga o lee el archivo mediante el mecanismo autorizado. | Si falla la descarga, registra error técnico y no continúa. |
| 8 | El agente prepara el envío a CUPS con los parámetros definidos. | Si faltan parámetros obligatorios, informa error técnico. |
| 9 | El agente envía el archivo a la cola CUPS correspondiente. | Si CUPS no está disponible, informa error técnico. |
| 10 | CUPS recibe el trabajo y devuelve estado o identificador técnico si corresponde. | Si CUPS rechaza el trabajo, el agente registra error. |
| 11 | El agente informa al backend el resultado inicial de la ejecución. | Si no puede informar al backend, debe registrar el evento localmente o reintentar según política técnica. |
| 12 | El sistema actualiza el estado técnico del print job. | Si falla la actualización, debe registrarse alerta técnica o evento equivalente. |
| 13 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios directo, salvo que el modelo defina transición por inicio de impresión |
| Estado visible al cliente | Sin cambios directo, salvo que exista mapeo visible definido |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Cambia a enviado a CUPS, en ejecución, error o equivalente según resultado |
| Estado del archivo | Sin cambios. El archivo solo se utiliza como entrada autorizada |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Print job tomado por agente | Cuando el agente inicia la ejecución de un trabajo autorizado |
| Acceso seguro a archivo utilizado | Cuando el agente accede al archivo autorizado |
| Trabajo enviado a CUPS | Cuando el archivo es enviado correctamente a CUPS |
| CUPS rechaza trabajo | Cuando CUPS no acepta el trabajo |
| Error de acceso a archivo | Cuando el agente no puede acceder o descargar el archivo |
| Error de ejecución de impresión | Cuando ocurre una falla durante la ejecución técnica |
| Estado técnico actualizado | Cuando el backend registra el resultado técnico inicial |

## 11. Observaciones

- Este caso de uso ejecuta técnicamente un print job ya autorizado.
- Este caso de uso no decide si un pedido puede imprimirse.
- Este caso de uso no genera el print job; eso corresponde a `CU-IMP-001-generar-trabajo-de-impresion-autorizado.md`.
- Este caso de uso no consulta la lista de trabajos; eso corresponde a `CU-IMP-002-consultar-trabajos-pendientes-por-agente.md`.
- El agente no modifica reglas de negocio.
- El agente no registra cobros.
- El agente no cierra pedidos.
- El agente no aprueba producción.
- La Raspberry Pi y CUPS solo ejecutan trabajos autorizados.
- El acceso a archivos debe realizarse mediante referencias seguras y autorizadas.
- No se deben usar rutas locales del cliente como mecanismo de impresión.
- Si la ejecución falla, debe derivarse al flujo de reporte de error de impresión.
- Si la ejecución avanza correctamente, los cambios técnicos posteriores se documentan en casos de reporte de estado técnico.

## 12. Poscondición

Al finalizar correctamente:

- el trabajo fue enviado a CUPS;
- el print job queda con estado técnico actualizado;
- el archivo utilizado fue accedido mediante mecanismo autorizado;
- el agente ejecutor queda registrado;
- no se modifica información financiera;
- no se cierra el pedido;
- no se aprueba ni rechaza el pedido;
- el evento queda auditado;
- el sistema queda preparado para recibir actualizaciones técnicas posteriores.

## 13. Criterios de aceptación

- El agente autenticado puede ejecutar solo trabajos autorizados.
- El sistema rechaza trabajos no autorizados, cancelados, bloqueados o ya ejecutados.
- El agente solo accede al archivo asociado al print job.
- El sistema no usa rutas locales del cliente para imprimir.
- El agente envía el archivo a CUPS mediante el subsistema autorizado.
- El resultado inicial de ejecución queda registrado.
- La ejecución no modifica reglas de negocio.
- La ejecución no registra cobros ni cierra pedidos.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.