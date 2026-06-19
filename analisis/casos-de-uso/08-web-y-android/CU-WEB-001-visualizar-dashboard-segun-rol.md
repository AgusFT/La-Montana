# CU-WEB-001 - Visualizar dashboard según rol

| Campo | Valor |
|---|---|
| ID | CU-WEB-001 |
| Caso de uso | Visualizar dashboard según rol |
| Área | Web y Android |
| Actor principal | Usuario |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-WEB-001, RF-WEB-002, RF-WEB-003, RF-WEB-004, RF-WEB-005, RF-WEB-006, RF-AUT-002, RF-AUT-003, RF-AUT-005 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-005, RNF-AUT-002, RNF-RLS-002, RNF-USA-001, RNF-USA-002, RNF-COM-002 |
| HU relacionadas | HU-CLI-007, HU-EMP-001, HU-ADM-008, HU-SIS-003 |
| Reglas críticas relacionadas | RFC-004, RFC-009, RNFC-001, RNFC-003, RNFC-005, RNFC-009 |

## 1. Caso de Uso

Visualizar dashboard según rol.

## 2. Actores

| Actor | Participación |
|---|---|
| Cliente | Accede a un dashboard orientado a sus pedidos, estados visibles, archivos y acciones permitidas |
| Empleado | Accede a un dashboard operativo interno según permisos |
| Administrador | Accede a un dashboard de gestión con permisos ampliados |
| Sistema | Valida sesión, rol, permisos y datos visibles antes de mostrar el dashboard |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un usuario autenticado visualiza el dashboard Web correspondiente a su rol y permisos.

La Web de **La Montaña** será utilizada por clientes, empleados y administradores. El sistema debe mostrar vistas, acciones, datos y accesos diferentes según el rol del usuario autenticado.

El dashboard no debe exponer información interna a clientes finales. Tampoco debe permitir que empleados sin permisos accedan a funciones administrativas o datos sensibles.

El frontend puede adaptar la interfaz visual según el rol, pero la seguridad real debe estar respaldada por backend, permisos, RLS, RPC, Edge Functions o mecanismos equivalentes.

Este caso de uso es principalmente de consulta y navegación inicial. No crea pedidos, no modifica estados, no registra cobros, no autoriza producción, no genera trabajos de impresión y no cierra pedidos.

## 4. Precondición

- El usuario está autenticado.
- Existe una sesión válida.
- El perfil del usuario existe.
- El sistema puede determinar rol y permisos del usuario.
- El backend Supabase está disponible.
- Las políticas RLS protegen datos de pedidos, archivos, estados, información financiera y auditoría.
- La Web puede consultar la información mínima necesaria para construir el dashboard.
- El usuario accede desde la aplicación Web.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| Usuario autenticado | Sí | Usuario que accede al dashboard |
| Sesión activa | Sí | Sesión válida que identifica al usuario |
| Rol del usuario | Sí | Rol principal: cliente, empleado o administrador |
| Permisos aplicables | Sí | Permisos derivados del rol o configuración |
| Canal de acceso | Sí | Web |
| Parámetros de vista | No | Filtros, pestañas, estado inicial o preferencias de visualización |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Dashboard visible | Vista inicial correspondiente al rol y permisos del usuario |
| Acciones disponibles | Acciones habilitadas para el usuario autenticado |
| Datos visibles | Información que el usuario puede consultar según permisos |
| Resumen de pedidos | Datos resumidos de pedidos propios o internos según rol |
| Estados visibles | Estados permitidos para el usuario |
| Alertas o pendientes | Indicadores de acciones requeridas, si corresponde |
| Mensaje de error | Mensaje controlado si no puede cargarse el dashboard |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere usuario autenticado |
| Autorización | El dashboard se define según rol y permisos |
| RLS / acceso a datos | Los datos mostrados deben estar protegidos por políticas de acceso |
| Cliente final | Solo puede ver pedidos propios, archivos propios e información visible |
| Empleado | Solo puede ver datos internos y acciones habilitadas por permisos |
| Administrador | Puede acceder a funciones ampliadas según permisos definidos |
| Estados internos | No deben mostrarse al cliente final |
| Información financiera | Solo debe mostrarse si el rol o permiso lo habilita |
| Auditoría interna | Solo debe mostrarse a usuarios internos autorizados |
| Validación backend | Ocultar elementos en la UI no reemplaza validaciones backend |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El usuario autenticado accede a la Web. | Si no está autenticado, el sistema redirige a inicio de sesión. |
| 2 | El sistema valida la sesión activa. | Si la sesión expiró o es inválida, solicita iniciar sesión nuevamente. |
| 3 | El sistema consulta perfil, rol y permisos del usuario. | Si no existe perfil o rol válido, bloquea el acceso a vistas protegidas. |
| 4 | El sistema determina el tipo de dashboard correspondiente. | Si no puede determinar dashboard seguro, muestra una pantalla controlada sin datos sensibles. |
| 5 | El sistema consulta los datos necesarios para el dashboard. | Si ocurre un error de backend, muestra mensaje controlado. |
| 6 | El sistema filtra datos y acciones según permisos. | Si algún dato no está autorizado, no se incluye en la respuesta. |
| 7 | La Web renderiza el dashboard correspondiente al usuario. | Si falla la carga de componentes, muestra error controlado sin exponer datos internos. |
| 8 | El usuario visualiza sus accesos, datos y acciones permitidas. | Si intenta acceder a una acción no permitida, backend debe rechazar la operación. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios |
| Estado visible al cliente | Sin cambios. Puede consultarse para mostrar información permitida |
| Estado financiero | Sin cambios. Solo se muestra información permitida |
| Estado técnico de impresión | Sin cambios |
| Estado de sesión | Sin cambios. La sesión continúa activa |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Dashboard consultado | Puede registrarse cuando un usuario accede al dashboard |
| Acceso no autorizado a dashboard | Cuando un usuario intenta acceder a una vista que no corresponde a su rol |
| Perfil sin rol válido | Cuando no puede determinarse rol o permisos del usuario |
| Error de carga de dashboard | Cuando ocurre una falla técnica al cargar datos o vista |
| Intento de acceso a acción no permitida | Cuando el usuario intenta ejecutar una acción fuera de sus permisos |

## 11. Observaciones

- Este caso de uso no crea pedidos.
- Este caso de uso no modifica pedidos.
- Este caso de uso no modifica archivos.
- Este caso de uso no cambia estados.
- Este caso de uso no registra cobros ni comprobantes.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no cierra pedidos.
- La Web debe ser multirol desde el MVP.
- El cliente final no debe ver información interna del negocio.
- La separación entre estado interno, estado visible y estado financiero debe respetarse también en el dashboard.
- La seguridad no debe depender únicamente del frontend.
- Web y Android deben consumir el mismo backend y respetar las mismas reglas.
- Las diferencias entre dashboards deben surgir de rol, permisos y políticas de acceso.

## 12. Poscondición

Al finalizar correctamente:

- el usuario visualiza el dashboard correspondiente a su rol;
- el usuario solo ve datos y acciones permitidas;
- el cliente no accede a información interna;
- el empleado no accede a funciones administrativas sin permisos;
- el administrador accede a funciones ampliadas según permisos;
- no se modifican datos de negocio;
- la sesión continúa activa;
- las acciones posteriores quedan sujetas a validación backend, RLS y permisos.

## 13. Criterios de aceptación

- El usuario autenticado puede visualizar un dashboard según su rol.
- El sistema diferencia dashboard de cliente, empleado y administrador.
- El cliente solo ve información propia y visible.
- Los usuarios internos ven información según permisos.
- El sistema no expone estados internos al cliente final.
- El sistema no expone auditoría interna al cliente final.
- El dashboard no habilita acciones no permitidas.
- Las acciones sensibles siguen protegidas por backend y RLS.
- La Web consume el mismo backend definido para el sistema.
- La visualización no modifica datos ni estados.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.
