# CU-IMP-001 - Generar trabajo de impresión autorizado

| Campo | Valor |
|---|---|
| ID | CU-IMP-001 |
| Caso de uso | Generar trabajo de impresión autorizado |
| Área | Impresión |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema, Agente de impresión |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-IMP-001, RF-IMP-003, RF-IMP-004, RF-IMP-005, RF-PED-005, RF-ARC-006, RF-EST-001, RF-EST-004, RF-FIN-006, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-007, RNF-ARC-004, RNF-AUD-001, RNF-AUD-004, RNF-IMP-001, RNF-IMP-002, RNF-IMP-004 |
| HU relacionadas | HU-IMP-001, HU-IMP-002, HU-IMP-004, HU-ADM-002 |
| Reglas críticas relacionadas | RFC-003, RFC-006, RFC-007, RFC-008, RFC-010, RNFC-001, RNFC-003, RNFC-004, RNFC-006 |

## 1. Caso de Uso

Generar trabajo de impresión autorizado.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Solicita generar un trabajo de impresión cuando el pedido está habilitado y tiene permisos |
| Administrador | Genera o supervisa trabajos de impresión autorizados con permisos ampliados |
| Sistema | Valida permisos, pedido, estados, archivos, condiciones financieras y crea el print job |
| Agente de impresión | No decide la generación; posteriormente podrá consultar o recibir trabajos autorizados |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador genera un trabajo de impresión autorizado para un pedido.

Un trabajo de impresión autorizado representa una instrucción registrada en el backend para que el subsistema de impresión pueda ejecutarla posteriormente mediante Raspberry Pi, CUPS y el agente/gateway de impresión.

La generación del trabajo de impresión no debe ocurrir automáticamente por la sola creación del pedido. Debe existir revisión administrativa humana y deben cumplirse las condiciones necesarias del pedido.

Este caso de uso no ejecuta la impresión. Solo genera el print job autorizado en el sistema. La ejecución técnica se documenta en casos de uso posteriores del dominio `05-impresion`.

El subsistema de impresión no toma decisiones de negocio. Solo podrá ejecutar trabajos autorizados por el backend.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos para generar trabajos de impresión.
- El pedido existe.
- El pedido fue revisado administrativamente.
- El pedido se encuentra en una etapa compatible con producción o impresión.
- El pedido no fue cerrado.
- El pedido no fue cancelado.
- Los archivos necesarios están cargados, validados y habilitados para impresión.
- El pedido cumple las condiciones financieras mínimas definidas para avanzar.
- Si el pedido supera 200 carillas, debe estar resuelta la condición de seña según reglas del flujo.
- El backend Supabase está disponible.
- El sistema puede crear un registro de print job.
- El agente de impresión no debe recibir archivos sin autorización.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido para el cual se genera el trabajo de impresión |
| Usuario interno autenticado | Sí | Empleado o administrador que solicita la generación |
| ID del archivo habilitado | Sí | Archivo que será utilizado para el trabajo de impresión |
| Configuración de impresión | No | Parámetros como impresora, cantidad de copias, doble faz, color, tamaño u opciones disponibles |
| Observación interna | No | Comentario interno asociado al trabajo |
| Prioridad del trabajo | No | Prioridad operativa si el sistema la contempla |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Print job creado | Registro del trabajo de impresión autorizado |
| ID del print job | Identificador único del trabajo |
| Pedido asociado | Pedido al que pertenece el trabajo de impresión |
| Archivo asociado | Archivo autorizado que será utilizado |
| Estado técnico inicial | Estado inicial del trabajo, por ejemplo pendiente |
| Usuario que genera | Usuario interno que solicitó la generación |
| Fecha de generación | Momento en que se creó el trabajo |
| Evento de auditoría | Registro de trazabilidad asociado a la generación |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos pueden generar print jobs |
| RLS / acceso a datos | Pedido, archivo y print job deben protegerse mediante políticas de acceso |
| Archivos | Solo pueden usarse archivos autorizados y habilitados para impresión |
| Estado del pedido | El pedido debe estar en una etapa compatible con impresión |
| Estado financiero | Si existe seña o cobro requerido antes de producción, debe estar resuelto según reglas del flujo |
| Agente de impresión | No participa en la decisión de negocio; solo ejecutará trabajos autorizados |
| Validación backend | La generación no debe depender únicamente del frontend |
| Auditoría | La generación del print job debe quedar registrada con usuario, fecha y contexto |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a un pedido habilitado para producción o impresión. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona la opción de generar trabajo de impresión. | Si no tiene permisos para generar print jobs, el sistema bloquea la acción. |
| 4 | El sistema valida que el pedido existe y está disponible. | Si el pedido no existe, fue cancelado o cerrado, rechaza la operación. |
| 5 | El sistema valida que el pedido fue revisado y aprobado para continuar el flujo. | Si no fue revisado o aprobado, bloquea la generación. |
| 6 | El sistema valida que el archivo seleccionado está habilitado para impresión. | Si el archivo no está habilitado, fue rechazado o no pertenece al pedido, bloquea la operación. |
| 7 | El sistema valida condiciones financieras necesarias. | Si requiere seña y no está resuelta, bloquea la generación o deriva a estado financiero correspondiente. |
| 8 | El usuario confirma configuración de impresión si corresponde. | Si faltan parámetros obligatorios de impresión, el sistema solicita completarlos. |
| 9 | El sistema crea el print job autorizado en el backend. | Si no puede crearlo, no debe generar referencias parciales ni inconsistentes. |
| 10 | El sistema vincula el print job con pedido, archivo y usuario solicitante. | Si falla la vinculación, revierte o bloquea la operación. |
| 11 | El sistema asigna estado técnico inicial al print job. | Si no existe estado técnico inicial válido, bloquea la generación. |
| 12 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente. |
| 13 | El sistema informa que el trabajo de impresión fue generado correctamente. | Si ocurre un error final, informa la falla y evita duplicar trabajos ante reintentos. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Puede mantenerse o avanzar a preparado para impresión, en cola o equivalente según modelo de estados |
| Estado visible al cliente | Puede mantenerse o actualizarse si existe mapeo visible definido |
| Estado financiero | Sin cambios, salvo bloqueo si existe condición financiera pendiente |
| Estado técnico de impresión | Se crea un nuevo estado técnico inicial del print job, por ejemplo pendiente |
| Estado de archivo | Sin cambios. El archivo debe estar previamente habilitado para impresión |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Print job generado | Cuando se crea el trabajo autorizado |
| Intento no autorizado de generar print job | Cuando un usuario sin permisos intenta generar un trabajo |
| Generación bloqueada por pedido no aprobado | Cuando el pedido no cumple condiciones de revisión |
| Generación bloqueada por archivo no habilitado | Cuando el archivo no está apto para impresión |
| Generación bloqueada por condición financiera | Cuando existe seña o condición financiera pendiente |
| Print job vinculado a archivo | Cuando el trabajo queda asociado al archivo autorizado |
| Error al generar print job | Cuando ocurre una falla técnica durante la generación |

## 11. Observaciones

- Este caso de uso no ejecuta impresión.
- Este caso de uso no envía directamente archivos a CUPS.
- Este caso de uso no permite que Raspberry Pi, CUPS o el agente decidan si el pedido puede imprimirse.
- El agente de impresión solo debe consultar o recibir trabajos ya autorizados.
- El archivo usado debe estar cargado, validado y habilitado para impresión.
- El pedido debe haber pasado por mediación administrativa humana.
- Si el pedido supera 200 carillas, debe respetarse la condición de seña.
- La generación del print job debe estar protegida por backend, RLS, permisos y auditoría.
- Si el trabajo requiere acceso posterior al archivo, ese acceso debe ser seguro y autorizado.
- La ejecución del trabajo se documenta en `CU-IMP-003-ejecutar-trabajo-de-impresion.md`.
- La consulta de trabajos por el agente se documenta en `CU-IMP-002-consultar-trabajos-pendientes-por-agente.md`.

## 12. Poscondición

Al finalizar correctamente:

- el print job queda creado;
- el print job queda asociado al pedido correcto;
- el print job queda asociado a un archivo autorizado;
- el usuario que generó el trabajo queda registrado;
- el estado técnico inicial queda definido;
- el evento queda auditado;
- no se ejecuta impresión todavía;
- no se envían archivos directamente a CUPS por este caso de uso;
- el agente de impresión podrá acceder al trabajo solo mediante flujo autorizado posterior.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede generar print jobs si tiene permisos.
- El sistema rechaza generación de trabajos por usuarios no autorizados.
- El sistema valida que el pedido fue revisado y aprobado según reglas del flujo.
- El sistema valida que el archivo pertenece al pedido y está habilitado para impresión.
- El sistema valida condiciones financieras necesarias antes de generar el trabajo.
- El print job queda asociado al pedido, archivo y usuario solicitante.
- El print job queda con estado técnico inicial.
- La generación queda auditada.
- La generación no ejecuta impresión.
- La generación no permite que el agente decida reglas de negocio.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.