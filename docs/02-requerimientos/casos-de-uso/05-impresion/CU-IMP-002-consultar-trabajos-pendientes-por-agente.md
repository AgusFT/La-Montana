# CU-IMP-002 - Consultar trabajos pendientes por agente

| Campo | Valor |
|---|---|
| ID | CU-IMP-002 |
| Caso de uso | Consultar trabajos pendientes por agente |
| Área | Impresión |
| Actor principal | Agente de impresión |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-IMP-001, RF-IMP-003, RF-IMP-004, RF-IMP-005, RF-IMP-006, RF-ARC-006, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-RLS-007, RNF-ARC-004, RNF-IMP-001, RNF-IMP-002, RNF-IMP-003, RNF-IMP-004, RNF-AUD-001, RNF-AUD-004 |
| HU relacionadas | HU-IMP-001, HU-IMP-002, HU-IMP-004 |
| Reglas críticas relacionadas | RFC-007, RFC-008, RFC-010, RNFC-001, RNFC-003, RNFC-004, RNFC-006 |

## 1. Caso de Uso

Consultar trabajos pendientes por agente.

## 2. Actores

| Actor | Participación |
|---|---|
| Agente de impresión | Consulta trabajos de impresión pendientes y autorizados para ejecutar mediante Raspberry Pi y CUPS |
| Sistema | Valida identidad del agente, permisos, trabajos autorizados, archivos asociados y registra trazabilidad |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual el agente/gateway de impresión consulta los trabajos de impresión pendientes y autorizados en el backend.

El agente de impresión forma parte del subsistema técnico basado en Raspberry Pi, CUPS y gateway/agente. Su responsabilidad es ejecutar trabajos autorizados, no decidir qué pedidos pueden avanzar, producirse, cobrarse, cerrarse o imprimirse.

El sistema solo debe entregar al agente trabajos previamente autorizados por el backend y asociados a archivos permitidos. El agente no debe acceder a archivos de pedidos no autorizados ni a información administrativa, financiera o interna que no necesite para ejecutar la impresión.

Este caso de uso no genera print jobs. Los trabajos ya deben haber sido creados por un flujo autorizado, como `CU-IMP-001-generar-trabajo-de-impresion-autorizado.md`.

## 4. Precondición

- El agente de impresión está identificado o autenticado mediante el mecanismo técnico definido.
- El agente tiene permisos para consultar trabajos de impresión autorizados.
- El backend Supabase está disponible.
- Existen o pueden existir print jobs pendientes.
- Cada print job pendiente debe estar asociado a un pedido autorizado.
- Cada print job pendiente debe estar asociado a un archivo autorizado.
- El agente no debe recibir trabajos cancelados, cerrados, rechazados o no autorizados.
- Las políticas de acceso deben impedir que el agente consulte pedidos, archivos o datos fuera de su alcance técnico.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| Identificador del agente | Sí | Identificador técnico del agente/gateway de impresión |
| Credencial o token técnico | Sí | Mecanismo de autenticación/autorización del agente |
| Estado solicitado | No | Estado de trabajos a consultar, por ejemplo pendientes o en cola |
| Capacidad o cola disponible | No | Información opcional de impresora, cola CUPS o capacidades disponibles |
| Límite de trabajos | No | Cantidad máxima de trabajos a devolver por consulta |
| Fecha de última consulta | No | Marca temporal para consultar trabajos nuevos o actualizados |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Lista de print jobs autorizados | Trabajos pendientes disponibles para el agente |
| ID del print job | Identificador único del trabajo de impresión |
| ID del pedido | Pedido asociado al trabajo |
| ID del archivo | Archivo autorizado asociado al trabajo |
| Referencia segura de archivo | Mecanismo autorizado para que el agente acceda al archivo |
| Parámetros de impresión | Configuración necesaria para ejecutar el trabajo si corresponde |
| Estado técnico del trabajo | Estado actual, por ejemplo pendiente o en cola |
| Prioridad del trabajo | Prioridad operativa si existe |
| Información mínima necesaria | Datos estrictamente necesarios para ejecutar la impresión |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación técnica | El agente debe identificarse mediante token, credencial o mecanismo técnico definido |
| Autorización | El agente solo puede consultar trabajos autorizados para su ejecución |
| RLS / acceso a datos | El acceso del agente a print jobs y archivos debe estar limitado por políticas específicas |
| Archivos | El agente solo puede acceder a archivos vinculados a trabajos autorizados |
| Datos de negocio | El agente no debe acceder a información financiera, administrativa o de cliente que no necesite |
| Estado del pedido | Solo se exponen trabajos de pedidos autorizados para impresión |
| Validación backend | La consulta no debe depender de filtros locales del agente |
| Auditoría | Las consultas o entregas de trabajos al agente deben quedar registradas si corresponde |
| Acceso a archivos | Debe usarse un mecanismo seguro, como signed URLs, tokens de corta duración o equivalente |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El agente de impresión inicia una consulta al backend. | Si no puede conectarse, registra error local o reintenta según política técnica. |
| 2 | El sistema valida la identidad técnica del agente. | Si la credencial es inválida, rechaza la consulta. |
| 3 | El sistema valida permisos del agente. | Si el agente no tiene permisos para consultar trabajos, rechaza la operación. |
| 4 | El sistema busca print jobs pendientes y autorizados. | Si no hay trabajos pendientes, devuelve lista vacía. |
| 5 | El sistema filtra trabajos según estado, autorización y disponibilidad. | Si un trabajo no está autorizado, cancelado o bloqueado, no se devuelve. |
| 6 | El sistema valida que cada trabajo tenga archivo autorizado asociado. | Si falta archivo o no está habilitado, excluye el trabajo y registra inconsistencia si corresponde. |
| 7 | El sistema genera o recupera referencias seguras de acceso a archivos. | Si no puede generar acceso seguro, no entrega ese trabajo al agente. |
| 8 | El sistema devuelve al agente la lista de trabajos autorizados. | Si ocurre error técnico, informa falla controlada. |
| 9 | El sistema registra evento de consulta o entrega de trabajos si corresponde. | Si falla auditoría, registra alerta técnica o evento equivalente. |
| 10 | El agente queda en condiciones de tomar un trabajo para ejecución posterior. | Si el trabajo expira o cambia de estado antes de ejecutarse, deberá consultar nuevamente. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios directo, salvo que el modelo registre que el trabajo fue puesto a disposición del agente |
| Estado visible al cliente | Sin cambios |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Puede mantenerse pendiente o pasar a disponible/en cola según reglas técnicas definidas |
| Estado del archivo | Sin cambios. El archivo ya debe estar autorizado |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Consulta de trabajos por agente | Cuando el agente solicita trabajos pendientes |
| Trabajos autorizados entregados | Cuando el sistema devuelve trabajos al agente |
| Intento no autorizado de agente | Cuando el agente no está autorizado o usa credencial inválida |
| Trabajo excluido por falta de autorización | Cuando un print job no se entrega por no cumplir condiciones |
| Archivo no disponible para agente | Cuando no se puede generar acceso seguro al archivo |
| Error de consulta de trabajos | Cuando ocurre una falla técnica durante la consulta |

## 11. Observaciones

- Este caso de uso no genera print jobs.
- Este caso de uso no ejecuta impresión.
- Este caso de uso no decide si un pedido puede imprimirse.
- Este caso de uso no modifica estados financieros.
- Este caso de uso no cierra pedidos.
- El agente de impresión solo recibe trabajos autorizados por el backend.
- El agente no debe acceder a archivos mediante rutas locales del cliente.
- El agente debe acceder a archivos mediante referencias seguras y autorizadas.
- El backend es responsable de decidir qué trabajos están disponibles.
- La Raspberry Pi, CUPS y el gateway/agente solo ejecutan trabajos autorizados.
- Si un trabajo cambia de estado antes de ser ejecutado, el agente debe respetar el estado actualizado.
- La ejecución del trabajo se documenta en `CU-IMP-003-ejecutar-trabajo-de-impresion.md`.

## 12. Poscondición

Al finalizar correctamente:

- el agente recibe una lista de trabajos pendientes y autorizados;
- cada trabajo recibido está asociado a un archivo autorizado;
- el agente no recibe trabajos no autorizados;
- el agente no accede a datos financieros o administrativos innecesarios;
- el agente no ejecuta impresión todavía por este caso de uso;
- el acceso a archivos queda protegido mediante mecanismo seguro;
- la consulta queda registrada si corresponde.

## 13. Criterios de aceptación

- El agente autenticado puede consultar trabajos pendientes autorizados.
- El sistema rechaza consultas de agentes no autorizados.
- El sistema no devuelve trabajos cancelados, rechazados, bloqueados o no autorizados.
- El sistema solo devuelve trabajos asociados a archivos autorizados.
- El sistema no expone rutas locales del cliente.
- El sistema no expone información financiera o administrativa innecesaria.
- La consulta no genera trabajos de impresión nuevos.
- La consulta no ejecuta impresión.
- La consulta puede devolver lista vacía si no hay trabajos pendientes.
- El acceso a archivos se realiza mediante mecanismo seguro y autorizado.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.