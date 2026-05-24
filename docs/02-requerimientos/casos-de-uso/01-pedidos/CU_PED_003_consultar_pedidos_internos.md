# CU-PED-003 - Consultar pedidos internos

| Campo | Valor |
|---|---|
| ID | CU-PED-003 |
| Caso de uso | Consultar pedidos internos |
| Área | Pedidos |
| Actor principal | Empleado interno (Administrativo / Producción / Comercial) |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-PED-005, RF-EST-003, RF-EST-006, RF-ADM-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-005, RNF-RLS-002, RNF-USA-002, RNF-REN-001 |
| HU relacionadas | HU-EMP-004, HU-ADM-002 |
| Reglas críticas relacionadas | RFC-004, RFC-006, RFC-009, RNFC-001, RNFC-003 |

## 1. Caso de Uso

Consultar pedidos internos.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado interno | Consulta pedidos internos según su rol (Administrativo, Producción, Comercial) |
| Sistema | Valida identidad, permisos, reglas de negocio y acceso a información interna |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado interno de La Montaña consulta pedidos internos para gestión administrativa, productiva o comercial.

El sistema debe mostrar información interna completa según rol: estados internos, datos administrativos, detalles operativos, archivos internos, observaciones, auditoría relevante, información financiera y cualquier información necesaria para la operación.

Cada tipo de empleado debe ver únicamente la información habilitada por su rol o permiso.

## 4. Precondición

- El empleado está autenticado.
- El empleado tiene permisos internos válidos (administrativo, comercial, producción u otro rol interno).
- El pedido existe.
- El pedido pertenece al ámbito de la empresa.
- El backend está disponible.
- Las políticas de acceso a datos deben garantizar visibilidad adecuada según rol.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | No | Si se consulta un pedido puntual |
| Filtros de búsqueda | No | Estado, cliente, fecha, tipo de pedido, responsable, etc. |
| Usuario interno autenticado | Sí | Determina permisos y nivel de visibilidad |
| Canal de acceso | No | Web interno o app interna si corresponde |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Lista de pedidos | Pedidos internos visibles según permisos |
| Detalle del pedido | Información completa según rol (estados internos, auditoría, operativa, financiera) |
| Historial interno | Auditoría relevante disponible para roles internos |
| Archivos internos | Archivos operativos y administrativos asociados |
| Observaciones internas | Notas internas accesibles según permisos |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere usuario interno autenticado |
| Autorización | Roles internos determinan la visibilidad (adm/com/prod) |
| RLS / acceso a datos | Debe aplicarse protección completa en backend, no solo en frontend |
| Estados internos | Visibles según permisos internos |
| Información financiera | Visible según rol administrativo/comercial |
| Observaciones internas | Solo para roles internos |
| Auditoría | Visible únicamente para roles autorizados |
| Validación backend | Toda validación de permisos debe realizarse del lado del backend |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado ingresa al sistema. | Si no está autenticado, se solicita inicio de sesión. |
| 2 | Accede a la sección de pedidos internos. | Si no tiene permisos, se rechaza el acceso. |
| 3 | El sistema recupera pedidos internos según el rol del empleado. | Si falla la consulta, se informa el error. |
| 4 | El sistema muestra la lista de pedidos internos visibles según permisos. | Si algún pedido no puede cargarse, se evita mostrar información inconsistente. |
| 5 | El empleado selecciona un pedido interno. | Si el pedido fue archivado/eliminado, se indica su indisponibilidad. |
| 6 | El sistema valida permisos avanzados (finanzas, operativa, producción). | Si el empleado no tiene permisos para ver información específica, no se muestra. |
| 7 | El sistema obtiene detalle completo del pedido. | Si faltan datos críticos, se registra y se muestra un mensaje controlado. |
| 8 | El sistema muestra el detalle completo permitido. | Si algún archivo no está disponible, se informa sin exponer rutas internas. |
| 9 | El empleado visualiza información interna completa según rol. | Si intenta acceder a información no autorizada, backend bloquea el acceso. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios. Solo lectura |
| Estado visible al cliente | Sin cambios |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Consulta de pedido interno | Opcional, según políticas de auditoría interna |
| Acceso a información sensible | Debe registrarse si el rol requiere trazabilidad |
| Intento de acceso no autorizado | Debe registrarse siempre |
| Falla técnica en consulta | Registrar si afecta disponibilidad u operación |

## 11. Observaciones

- Este caso de uso no modifica pedidos.
- No cambia estados internos ni visibles.
- No autoriza trabajos de impresión.
- No crea procesos productivos.
- Los permisos deben ser estrictamente backend-first.
- El empleado puede ver información interna pero solo lo permitido por su rol.
- La lógica de visibilidad debe ser consistente entre Web interno y API.
- La auditoría debe registrar accesos sensibles.

## 12. Poscondición

Al finalizar correctamente:

- el empleado visualiza información interna del pedido según su rol;
- no se modifica el pedido ni su estado;
- el sistema no expone información fuera del alcance permitido;
- la operación queda protegida por permisos, políticas y RLS.

## 13. Criterios de aceptación

- El empleado autenticado puede consultar únicamente pedidos autorizados según su rol.
- El sistema protege accesos mediante permisos internos y RLS.
- El sistema muestra información interna completa solo para roles habilitados.
- Auditoría registra accesos sensibles y denegados.
- La consulta no altera estados ni información del pedido.
- El resultado es consistente con RF, RNF y reglas críticas aplicables.