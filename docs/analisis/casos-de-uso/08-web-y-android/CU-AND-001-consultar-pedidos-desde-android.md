# CU-AND-001 - Consultar pedidos desde Android

| Campo | Valor |
|---|---|
| ID | CU-AND-001 |
| Caso de uso | Consultar pedidos desde Android |
| Área | Web y Android |
| Actor principal | Cliente |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-AND-001, RF-AND-002, RF-AND-003, RF-AND-005, RF-PED-004, RF-EST-002, RF-EST-006, RF-AUT-001, RF-AUT-003 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-005, RNF-AUT-001, RNF-AUT-002, RNF-RLS-002, RNF-USA-002, RNF-REN-001, RNF-COM-002, RNF-COM-003 |
| HU relacionadas | HU-CLI-004, HU-CLI-008, HU-SIS-005 |
| Reglas críticas relacionadas | RFC-004, RFC-009, RNFC-001, RNFC-003, RNFC-005, RNFC-009 |

## 1. Caso de Uso

Consultar pedidos desde Android.

## 2. Actores

| Actor | Participación |
|---|---|
| Cliente | Consulta desde la app Android sus pedidos y estados visibles |
| Sistema | Valida autenticación, propiedad de los pedidos, permisos, estados visibles y datos permitidos |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un cliente consulta sus pedidos desde la aplicación Android de **La Montaña**.

La app Android consume el mismo backend que la Web. Por lo tanto, debe respetar las mismas reglas de autenticación, autorización, RLS, estados visibles, separación de datos internos y protección de información sensible.

El cliente solo puede consultar pedidos propios. No debe acceder a pedidos de otros clientes, estados internos, observaciones internas, auditoría administrativa, información operativa interna ni datos financieros no visibles.

Este caso de uso es de consulta. No crea pedidos, no modifica pedidos, no cambia estados, no registra cobros, no carga archivos, no autoriza producción y no genera trabajos de impresión.

## 4. Precondición

- El cliente tiene instalada o disponible la app Android.
- El cliente está autenticado.
- Existe una sesión válida contra Supabase Auth.
- El cliente tiene uno o más pedidos, o el sistema puede devolver una lista vacía.
- El backend Supabase está disponible.
- La app Android consume el mismo backend que la Web.
- Las políticas RLS impiden que el cliente consulte pedidos ajenos.
- El sistema tiene definidos estados visibles para los pedidos consultados.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| Usuario autenticado | Sí | Cliente que consulta sus pedidos desde Android |
| Sesión activa | Sí | Sesión válida que identifica al cliente |
| Canal de acceso | Sí | Android |
| Filtros de consulta | No | Filtros opcionales como estado visible, fecha o búsqueda |
| Parámetros de paginación | No | Límite, página, cursor o criterio equivalente si existen muchos pedidos |
| Ordenamiento | No | Criterio de orden, por ejemplo fecha de creación o actualización |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Lista de pedidos propios | Pedidos asociados al cliente autenticado |
| ID del pedido | Identificador de cada pedido |
| Descripción resumida | Información visible y resumida del pedido |
| Estado visible al cliente | Estado comunicable al cliente |
| Información financiera visible | Información financiera permitida, por ejemplo seña requerida o pago pendiente si corresponde |
| Fecha de creación | Fecha en que se registró el pedido |
| Próxima acción esperada | Acción requerida del cliente, por ejemplo corregir datos, cargar archivo o abonar seña |
| Mensaje de error | Mensaje controlado si la consulta falla |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere cliente autenticado mediante Supabase Auth |
| Autorización | El cliente solo puede consultar pedidos propios |
| RLS / acceso a datos | Los pedidos deben estar protegidos para impedir acceso a pedidos ajenos |
| Estado interno | No debe mostrarse en Android al cliente final |
| Estado visible | Es el estado operativo comunicable al cliente |
| Estado financiero | Solo debe mostrarse información financiera visible y autorizada |
| Observaciones internas | No deben mostrarse al cliente |
| Auditoría | No debe mostrarse auditoría administrativa al cliente |
| Backend común | Android debe consumir el mismo backend y respetar las mismas reglas que Web |
| Validación backend | La seguridad no debe depender únicamente de controles de la app Android |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El cliente abre la app Android. | Si no hay sesión activa, la app solicita iniciar sesión. |
| 2 | La app valida o recupera la sesión del cliente. | Si la sesión expiró, solicita autenticación nuevamente. |
| 3 | El cliente accede a la sección de pedidos. | Si no tiene permisos o sesión válida, el sistema rechaza la consulta. |
| 4 | La app solicita al backend los pedidos del cliente autenticado. | Si no hay conexión o falla el backend, muestra error controlado. |
| 5 | El backend valida identidad, permisos y RLS sobre los pedidos. | Si se intenta consultar pedidos ajenos, rechaza la operación. |
| 6 | El backend devuelve solo pedidos propios y datos visibles. | Si no hay pedidos, devuelve lista vacía. |
| 7 | La app muestra la lista de pedidos propios. | Si ocurre error de renderizado, muestra mensaje controlado sin exponer datos internos. |
| 8 | El cliente visualiza estados visibles y datos permitidos. | Si un pedido requiere acción del cliente, la app muestra indicación correspondiente. |
| 9 | El cliente puede seleccionar un pedido para ver más detalle en otro caso de uso. | Si el pedido ya no está disponible, la app informa la situación. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios. No se expone al cliente |
| Estado visible al cliente | Sin cambios. Se consulta para mostrarlo en Android |
| Estado financiero | Sin cambios. Solo se muestra información visible si corresponde |
| Estado técnico de impresión | Sin cambios |
| Estado de sesión | Sin cambios, salvo que la sesión esté expirada y requiera autenticación |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Consulta de pedidos desde Android | Puede registrarse si se decide auditar consultas del cliente |
| Sesión inválida en Android | Cuando el cliente intenta consultar sin sesión válida |
| Intento de acceso a pedido ajeno | Cuando se intenta consultar información no asociada al cliente |
| Error de consulta Android | Cuando ocurre una falla técnica relevante en la consulta |
| Lista de pedidos vacía | Puede registrarse si se decide auditar experiencia o soporte |

## 11. Observaciones

- Este caso de uso es de solo consulta.
- Este caso de uso no crea pedidos.
- Este caso de uso no modifica pedidos.
- Este caso de uso no carga archivos.
- Este caso de uso no cambia estados.
- Este caso de uso no registra cobros ni comprobantes.
- Este caso de uso no genera trabajos de impresión.
- Android debe consumir el mismo backend que Web.
- Android debe respetar las mismas reglas de permisos, RLS y visibilidad.
- El cliente solo debe ver estados visibles, no estados internos.
- El cliente no debe acceder a información interna del negocio.
- La app puede tener una presentación distinta a la Web, pero no reglas de negocio diferentes.
- La seguridad debe estar en backend, no solo en la app.

## 12. Poscondición

Al finalizar correctamente:

- el cliente visualiza sus pedidos desde Android;
- el cliente solo ve información propia y permitida;
- el cliente ve estados visibles correspondientes;
- no se exponen estados internos ni auditoría;
- no se modifican datos de negocio;
- no se generan trabajos de impresión;
- la consulta queda protegida por backend, RLS y permisos.

## 13. Criterios de aceptación

- El cliente autenticado puede consultar sus pedidos desde Android.
- El sistema rechaza consultas sin sesión válida.
- El sistema rechaza acceso a pedidos ajenos.
- La app muestra solo pedidos propios del cliente.
- La app muestra estados visibles y datos permitidos.
- La app no muestra estados internos, observaciones internas ni auditoría.
- Android consume el mismo backend que Web.
- Android respeta las mismas reglas de permisos y RLS.
- La consulta no modifica pedidos ni estados.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.