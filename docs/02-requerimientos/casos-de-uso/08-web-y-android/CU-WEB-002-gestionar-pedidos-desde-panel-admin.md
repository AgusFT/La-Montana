# CU-WEB-002 - Gestionar pedidos desde panel administrativo

| Campo                        | Valor                                                                                                                                          |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| ID                           | CU-WEB-002                                                                                                                                     |
| Caso de uso                  | Gestionar pedidos desde panel administrativo                                                                                                   |
| Área                         | Web y Android                                                                                                                                  |
| Actor principal              | Administrador                                                                                                                                  |
| Actores secundarios          | Empleado autorizado, Sistema                                                                                                                   |
| Prioridad                    | P0 Crítica                                                                                                                                     |
| Alcance                      | MVP                                                                                                                                            |
| RF relacionados              | RF-WEB-006, RF-WEB-008, RF-PED-005, RF-PED-007, RF-PED-008, RF-AUT-003, RF-AUT-005, RF-EST-001, RF-EST-002, RF-EST-003, RF-AUD-001, RF-AUD-002 |
| RNF relacionados             | RNF-SEG-003, RNF-SEG-004, RNF-SEG-005, RNF-AUT-002, RNF-RLS-002, RNF-RLS-004, RNF-AUD-001, RNF-USA-001, RNF-USA-002, RNF-REN-001               |
| HU relacionadas              | HU-ADM-008, HU-ADM-004, HU-ADM-006, HU-EMP-001                                                                                                 |
| Reglas críticas relacionadas | RFC-001, RFC-002, RFC-003, RFC-004, RFC-006, RNFC-001, RNFC-003, RNFC-005, RNFC-007, RNFC-009                                                  |

## 1. Caso de Uso

Gestionar pedidos desde panel administrativo.

## 2. Actores

| Actor               | Participación                                                                                           |
| ------------------- | ------------------------------------------------------------------------------------------------------- |
| Administrador       | Consulta, filtra, supervisa y accede a acciones administrativas sobre pedidos                           |
| Empleado autorizado | Consulta y opera pedidos desde el panel si tiene permisos internos suficientes                          |
| Sistema             | Valida sesión, rol, permisos, visibilidad de datos, acciones disponibles y acceso a información interna |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un administrador o empleado autorizado gestiona pedidos desde el panel administrativo Web de **La Montaña**.

El panel administrativo funciona como una vista centralizada para consultar, filtrar, revisar y acceder a operaciones sobre pedidos. Desde este panel se pueden iniciar acciones como revisión administrativa, solicitud de corrección, aprobación, rechazo, consulta de archivos, registro de observaciones, gestión de estados, seguimiento financiero, impresión, entrega o cierre, siempre que el usuario tenga permisos y que el pedido esté en una etapa válida.

Este caso de uso no reemplaza los casos específicos de negocio. El panel administrativo actúa como punto de acceso y coordinación, pero cada acción sensible debe resolverse por su caso de uso correspondiente y validarse en backend.

El sistema debe evitar que el panel exponga información o acciones no autorizadas. La seguridad no debe depender únicamente de ocultar botones en la interfaz.

## 4. Precondición

* El administrador o empleado autorizado está autenticado.
* El usuario accede desde la aplicación Web.
* El perfil del usuario existe.
* El sistema puede determinar rol y permisos del usuario.
* El backend Supabase está disponible.
* Existen pedidos registrados o el sistema puede mostrar una vista vacía.
* Las políticas RLS protegen pedidos, archivos, estados, información financiera y auditoría.
* El sistema puede determinar qué datos y acciones están permitidos para el usuario.
* La Web consume el mismo backend definido para el sistema.

## 5. Datos de entrada

| Dato                     | Obligatorio | Descripción                                                                        |
| ------------------------ | ----------- | ---------------------------------------------------------------------------------- |
| Usuario autenticado      | Sí          | Administrador o empleado autorizado que accede al panel                            |
| Sesión activa            | Sí          | Sesión válida que identifica al usuario                                            |
| Rol y permisos           | Sí          | Permisos que determinan datos y acciones disponibles                               |
| Filtros de búsqueda      | No          | Estado, cliente, fecha, tipo de pedido, responsable, prioridad u otros criterios   |
| ID del pedido            | No          | Identificador de un pedido puntual cuando se abre un detalle                       |
| Parámetros de paginación | No          | Página, límite, cursor o criterio equivalente                                      |
| Ordenamiento             | No          | Criterio de orden, por ejemplo fecha, estado o prioridad                           |
| Vista seleccionada       | No          | Listado, detalle, revisión, producción, finanzas, impresión u otra vista permitida |

## 6. Datos de salida

| Dato                   | Descripción                                                                                       |
| ---------------------- | ------------------------------------------------------------------------------------------------- |
| Listado de pedidos     | Pedidos visibles para el usuario según rol y permisos                                             |
| Detalle de pedido      | Información interna permitida del pedido seleccionado                                             |
| Acciones disponibles   | Acciones habilitadas según permisos y estado del pedido                                           |
| Estados del pedido     | Estado interno, visible y financiero según permisos                                               |
| Archivos asociados     | Archivos visibles o accesibles según permisos                                                     |
| Información financiera | Datos financieros visibles solo para roles autorizados                                            |
| Observaciones internas | Observaciones visibles solo para usuarios internos autorizados                                    |
| Alertas operativas     | Indicadores de corrección, revisión, seña pendiente, error de impresión, cierre pendiente u otros |
| Mensaje de error       | Error controlado si no puede cargar datos o si el acceso es inválido                              |

## 7. Permisos y seguridad

| Aspecto                | Regla                                                                                                                   |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Autenticación          | Requiere usuario interno autenticado                                                                                    |
| Autorización           | Solo administradores o empleados autorizados pueden acceder al panel administrativo                                     |
| RLS / acceso a datos   | Los pedidos, archivos, estados, finanzas y auditoría deben protegerse mediante políticas de acceso                      |
| Acciones sensibles     | Aprobar, rechazar, cambiar estados, registrar cobros, autorizar impresión o cerrar pedidos requieren validación backend |
| Cliente final          | No puede acceder al panel administrativo                                                                                |
| Información financiera | Solo debe mostrarse a usuarios con permisos financieros                                                                 |
| Auditoría              | Solo debe mostrarse a usuarios con permisos de consulta de historial                                                    |
| Estados internos       | Solo deben mostrarse a usuarios internos autorizados                                                                    |
| Validación backend     | La autorización de datos y acciones no debe depender únicamente del frontend                                            |
| Visibilidad            | La interfaz debe ocultar acciones no permitidas, pero backend debe rechazarlas igualmente                               |

## 8. Flujo principal

| Paso | Flujo principal                                                                   | Flujo alternativo / excepciones                                                |
| ---- | --------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| 1    | El administrador o empleado autorizado ingresa a la Web.                          | Si no está autenticado, el sistema redirige a inicio de sesión.                |
| 2    | El usuario accede al panel administrativo de pedidos.                             | Si no tiene permisos internos, el sistema rechaza el acceso.                   |
| 3    | El sistema valida sesión, rol y permisos del usuario.                             | Si el rol no está definido o no tiene permisos suficientes, bloquea la vista.  |
| 4    | El sistema consulta pedidos visibles para el usuario.                             | Si no hay pedidos visibles, muestra una vista vacía o mensaje correspondiente. |
| 5    | El sistema muestra listado, filtros y datos resumidos permitidos.                 | Si falla la consulta, muestra error controlado sin exponer datos internos.     |
| 6    | El usuario aplica filtros, búsqueda u ordenamiento.                               | Si los filtros son inválidos, el sistema informa el error.                     |
| 7    | El usuario selecciona un pedido para ver detalle.                                 | Si el pedido no existe o no está disponible, informa la situación.             |
| 8    | El sistema valida permisos específicos sobre el pedido seleccionado.              | Si el usuario no tiene permiso sobre ese pedido, rechaza la consulta.          |
| 9    | El sistema muestra detalle permitido del pedido y acciones disponibles.           | Si alguna acción no está permitida, no se muestra o queda deshabilitada.       |
| 10   | El usuario selecciona una acción administrativa permitida.                        | Si intenta una acción no permitida, backend debe rechazarla.                   |
| 11   | El sistema deriva al caso de uso específico correspondiente.                      | Si el pedido no está en etapa válida para esa acción, se informa el motivo.    |
| 12   | El sistema mantiene trazabilidad de acciones sensibles realizadas desde el panel. | Si falla auditoría, debe registrarse alerta técnica o evento equivalente.      |

## 9. Impacto en estados

| Estado                      | Impacto                                                                                               |
| --------------------------- | ----------------------------------------------------------------------------------------------------- |
| Estado interno              | Sin cambios directo por visualizar el panel; puede cambiar solo al ejecutar un caso de uso específico |
| Estado visible al cliente   | Sin cambios directo                                                                                   |
| Estado financiero           | Sin cambios directo                                                                                   |
| Estado técnico de impresión | Sin cambios directo                                                                                   |
| Estado de sesión            | Sin cambios. La sesión continúa activa                                                                |

## 10. Eventos de auditoría

| Evento                          | Cuándo se registra                                                    |
| ------------------------------- | --------------------------------------------------------------------- |
| Acceso al panel administrativo  | Puede registrarse cuando un usuario interno accede al panel           |
| Consulta de listado de pedidos  | Puede registrarse si se decide auditar consultas administrativas      |
| Consulta de detalle interno     | Puede registrarse cuando se abre un pedido con información sensible   |
| Intento de acceso no autorizado | Cuando un usuario sin permisos intenta acceder al panel o a un pedido |
| Acción administrativa iniciada  | Cuando el usuario inicia una acción sensible desde el panel           |
| Error de carga del panel        | Cuando ocurre una falla técnica al consultar datos                    |

## 11. Observaciones

* Este caso de uso representa el panel administrativo como punto de acceso a operaciones.
* Este caso de uso no reemplaza casos específicos como revisar, aprobar, rechazar, registrar cobro, registrar entrega o cerrar pedido.
* Este caso de uso no modifica pedidos por sí solo.
* Este caso de uso no cambia estados por sí solo.
* Este caso de uso no registra cobros ni comprobantes por sí solo.
* Este caso de uso no genera trabajos de impresión por sí solo.
* Este caso de uso no cierra pedidos por sí solo.
* El cliente final no debe acceder al panel administrativo.
* La Web debe mostrar acciones según rol, permisos y estado del pedido.
* Las acciones sensibles deben validarse en backend, RLS, RPC, Edge Function o mecanismo equivalente.
* La separación entre estado interno, estado visible y estado financiero debe respetarse en la visualización.
* El panel debe facilitar la operación, pero no debe concentrar reglas de negocio exclusivamente en frontend.

## 12. Poscondición

Al finalizar correctamente:

* el usuario visualiza el panel administrativo según sus permisos;
* el usuario puede consultar pedidos internos autorizados;
* el usuario puede acceder al detalle permitido de un pedido;
* el usuario ve solo acciones habilitadas para su rol, permisos y estado del pedido;
* no se modifican datos de negocio por visualizar el panel;
* las acciones posteriores quedan sujetas a casos de uso específicos y validación backend;
* no se expone información interna a usuarios no autorizados.

## 13. Criterios de aceptación

* El administrador autenticado puede acceder al panel administrativo.
* El empleado autorizado puede acceder al panel según permisos.
* El sistema rechaza acceso de clientes al panel administrativo.
* El sistema muestra solo pedidos visibles para el usuario.
* El sistema muestra solo acciones permitidas.
* El sistema no expone información financiera o auditoría a usuarios sin permisos.
* El sistema no permite ejecutar acciones sensibles solo por validación frontend.
* La visualización del panel no modifica datos ni estados por sí sola.
* Las acciones del panel derivan a casos de uso específicos.
* La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
* El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.

````
