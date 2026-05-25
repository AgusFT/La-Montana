# CU-PED-003 - Consultar pedidos internos

| Campo | Valor |
|---|---|
| ID | CU-PED-003 |
| Caso de uso | Consultar pedidos internos |
| Área | Pedidos |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-PED-005, RF-AUT-003, RF-AUT-005, RF-EST-001, RF-EST-003, RF-EST-006, RF-WEB-006 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-005, RNF-RLS-002, RNF-USA-002, RNF-REN-001 |
| HU relacionadas | HU-EMP-001, HU-ADM-008 |
| Reglas críticas relacionadas | RFC-004, RNFC-001, RNFC-003, RNFC-005, RNFC-009 |

## 1. Caso de Uso

Consultar pedidos internos.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Consulta pedidos internos según los permisos asociados a su rol o función operativa |
| Administrador | Consulta pedidos internos con permisos ampliados para supervisión y gestión |
| Sistema | Valida identidad, permisos, reglas de acceso y visibilidad de la información interna |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador consulta pedidos internos del sistema **La Montaña** para tareas administrativas, operativas, productivas o de seguimiento.

El sistema debe mostrar la información interna permitida según rol y permisos: estados internos, datos administrativos, detalles operativos, archivos autorizados, observaciones internas, auditoría relevante e información financiera cuando corresponda.

Cada usuario interno debe ver únicamente la información habilitada por su rol, permisos o función operativa. El sistema no debe exponer información interna por defecto ni depender únicamente del frontend para ocultar datos sensibles.

## 4. Precondición

- El empleado o administrador está autenticado.
- El empleado o administrador tiene permisos internos válidos para consultar pedidos.
- El sistema tiene registrados pedidos internos.
- El backend Supabase está disponible.
- Las políticas de acceso a datos deben garantizar visibilidad adecuada según rol y permisos.
- La consulta debe ejecutarse contra datos autorizados por backend, RLS, RPC o política equivalente.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| Usuario interno autenticado | Sí | Usuario que realiza la consulta y determina el alcance de permisos |
| ID del pedido | No | Identificador de un pedido puntual cuando se consulta un detalle específico |
| Filtros de búsqueda | No | Estado, cliente, fecha, tipo de pedido, responsable, prioridad u otros filtros operativos |
| Canal de acceso | No | Web multirol; Android solo si la funcionalidad se habilita para roles internos |
| Parámetros de paginación | No | Límite, página o cursor para evitar consultas excesivas |
| Ordenamiento | No | Criterio de orden, por ejemplo fecha de creación, estado o prioridad operativa |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Lista de pedidos | Pedidos internos visibles según permisos del usuario autenticado |
| Resumen del pedido | Datos generales necesarios para operar o revisar pedidos |
| Detalle del pedido | Información interna permitida según rol y permisos |
| Estado interno | Estado operativo interno visible solo para usuarios autorizados |
| Estado visible al cliente | Estado que el cliente puede ver, útil para comparar comunicación externa |
| Estado financiero | Información financiera visible solo para roles habilitados |
| Archivos autorizados | Archivos asociados al pedido que el usuario interno puede consultar |
| Observaciones internas | Notas internas visibles según permisos |
| Auditoría relevante | Eventos visibles solo para roles autorizados |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere usuario interno autenticado |
| Autorización | Los roles y permisos internos determinan la visibilidad de la información |
| RLS / acceso a datos | Debe aplicarse protección completa en backend y base de datos, no solo en frontend |
| Estados internos | Visibles únicamente para usuarios internos autorizados |
| Información financiera | Visible según permisos administrativos o financieros |
| Observaciones internas | Solo visibles para roles internos habilitados |
| Auditoría | Visible únicamente para roles autorizados |
| Archivos | Solo se muestran archivos autorizados y asociados al pedido correspondiente |
| Validación backend | Toda validación de permisos debe realizarse del lado del backend, RPC, RLS o política equivalente |
| Separación cliente/interno | La información interna consultada en este caso de uso no debe exponerse al cliente final |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a la sección interna de pedidos. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El sistema identifica el rol y permisos del usuario autenticado. | Si no puede determinar permisos, el sistema bloquea la consulta por seguridad. |
| 4 | El usuario consulta la lista de pedidos internos o aplica filtros de búsqueda. | Si los filtros son inválidos, el sistema informa el error y no ejecuta la consulta. |
| 5 | El sistema recupera pedidos visibles según rol y permisos. | Si falla la consulta, el sistema informa la falla sin exponer información sensible. |
| 6 | El sistema muestra la lista de pedidos internos permitidos. | Si no existen pedidos visibles, muestra una vista vacía o mensaje correspondiente. |
| 7 | El usuario selecciona un pedido para consultar su detalle. | Si el pedido no existe o no está disponible, el sistema informa la situación. |
| 8 | El sistema valida permisos específicos sobre el pedido seleccionado. | Si el usuario no tiene permiso sobre ese pedido, el sistema rechaza la consulta. |
| 9 | El sistema recupera la información interna permitida para ese usuario. | Si algún dato sensible no está autorizado, no se incluye en la respuesta. |
| 10 | El sistema muestra el detalle interno permitido del pedido. | Si algún archivo no está disponible, se informa sin exponer rutas internas ni datos técnicos sensibles. |
| 11 | El usuario visualiza la información necesaria para operar, revisar o dar seguimiento al pedido. | Si intenta acceder a información fuera de su permiso, backend/RLS bloquea el acceso. |
| 12 | El sistema registra eventos de auditoría cuando corresponda. | Si se detecta intento no autorizado, debe registrarse como evento de seguridad. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios. Solo se consulta si el usuario tiene permisos |
| Estado visible al cliente | Sin cambios. Puede mostrarse como referencia comparativa |
| Estado financiero | Sin cambios. Solo se consulta si el usuario tiene permisos |
| Estado técnico de impresión | Sin cambios. Este caso no ejecuta ni modifica trabajos de impresión |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Consulta de pedido interno | Puede registrarse cuando un usuario interno consulta un pedido |
| Consulta de información sensible | Debe registrarse si se accede a información financiera, auditoría, archivos sensibles u observaciones internas críticas |
| Intento de acceso no autorizado | Debe registrarse cuando un usuario intenta acceder a pedidos o datos fuera de su permiso |
| Error de consulta interna | Debe registrarse si una falla técnica afecta disponibilidad, consistencia o seguridad |
| Filtro o búsqueda interna ejecutada | Puede registrarse si se decide auditar búsquedas internas relevantes |

## 11. Observaciones

- Este caso de uso es de consulta y no modifica pedidos.
- Este caso de uso no cambia estados internos, visibles ni financieros.
- Este caso de uso no autoriza producción.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no registra cobros ni comprobantes.
- El usuario interno puede ver información interna solo si su rol o permisos lo habilitan.
- La lógica de visibilidad debe ser consistente entre Web y backend.
- La seguridad debe ser backend-first: RLS, RPC, políticas y validaciones de permisos.
- No deben tratarse funciones operativas como roles formales si todavía no fueron definidas en el modelo RBAC.
- Las funciones como administración, producción o atención comercial deben interpretarse como responsabilidades operativas o permisos específicos.
- La consulta debe soportar filtros y paginación para evitar problemas de rendimiento.
- Este caso de uso puede servir luego como base para vistas internas de tablero, listado de pedidos, panel administrativo y pantallas de seguimiento operativo.

## 12. Poscondición

Al finalizar correctamente:

- el empleado o administrador visualiza únicamente pedidos internos permitidos;
- el usuario accede solo a información interna autorizada;
- el pedido no cambia de estado;
- no se modifica información financiera;
- no se genera ni ejecuta ningún trabajo de impresión;
- no se expone información interna al cliente final;
- el acceso queda protegido por roles, permisos, RLS y validaciones de backend;
- los accesos sensibles o denegados quedan auditados cuando corresponda.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede consultar pedidos internos según permisos.
- El sistema rechaza accesos de usuarios no autenticados.
- El sistema rechaza accesos de usuarios sin permisos internos.
- El sistema muestra solo pedidos visibles para el usuario autenticado.
- El sistema muestra información interna permitida según rol y permisos.
- El sistema no expone información financiera, auditoría u observaciones internas a usuarios no habilitados.
- La consulta no altera estados ni datos del pedido.
- El sistema protege la consulta mediante backend, RLS, permisos o políticas equivalentes.
- Los intentos de acceso no autorizado quedan registrados.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.
