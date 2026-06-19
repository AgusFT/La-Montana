# CU-AUT-003 - Consultar perfil propio

| Campo | Valor |
|---|---|
| ID | CU-AUT-003 |
| Caso de uso | Consultar perfil propio |
| Área | Usuarios y permisos |
| Actor principal | Usuario |
| Actores secundarios | Sistema |
| Prioridad | P1 Alta |
| Alcance | MVP |
| RF relacionados | RF-AUT-001, RF-AUT-002, RF-AUT-003, RF-AUT-005, RF-AUT-006 |
| RNF relacionados | RNF-SEG-002, RNF-SEG-003, RNF-SEG-005, RNF-AUT-001, RNF-AUT-002, RNF-AUT-003, RNF-RLS-003 |
| HU relacionadas | HU-CLI-001, HU-EMP-001, HU-ADM-003 |
| Reglas críticas relacionadas | RNFC-001, RNFC-003, RNFC-005 |

## 1. Caso de Uso

Consultar perfil propio.

## 2. Actores

| Actor | Participación |
|---|---|
| Usuario | Consulta la información de su propio perfil dentro del sistema |
| Sistema | Valida sesión, identidad, permisos y devuelve solo la información permitida del perfil autenticado |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un usuario autenticado consulta su propio perfil en **La Montaña**.

El perfil propio puede incluir datos básicos de cuenta, rol asignado, permisos aplicables, información visible del usuario y configuración mínima necesaria para operar dentro del sistema.

Este caso aplica a los roles iniciales:

- cliente;
- empleado;
- administrador.

El usuario solo puede consultar su propio perfil. No debe poder consultar perfiles de otros usuarios por este flujo.

Consultar el perfil propio no modifica datos, no cambia roles, no cambia permisos, no crea pedidos, no modifica pedidos y no ejecuta acciones operativas.

## 4. Precondición

- El usuario está autenticado.
- Existe una sesión válida.
- El perfil del usuario existe.
- El backend Supabase está disponible.
- Las políticas RLS permiten consultar únicamente el perfil propio.
- El sistema puede determinar rol y permisos asociados al usuario autenticado.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| Usuario autenticado | Sí | Usuario que solicita consultar su perfil |
| Sesión activa | Sí | Sesión válida que identifica al usuario |
| Canal de acceso | No | Web o Android, según desde dónde se consulta |
| ID de perfil | No | Solo debe coincidir con el usuario autenticado si se envía explícitamente |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| ID del usuario | Identificador del usuario autenticado |
| Datos básicos del perfil | Información mínima visible del usuario |
| Rol asignado | Rol principal del usuario, por ejemplo cliente, empleado o administrador |
| Permisos aplicables | Permisos derivados del rol o configuración |
| Estado de cuenta | Información operativa del perfil si corresponde |
| Preferencias básicas | Configuración visible si el sistema la contempla |
| Mensaje de error | Mensaje controlado si el perfil no puede consultarse |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere usuario autenticado |
| Autorización | Todo usuario autenticado puede consultar su propio perfil |
| RLS / acceso a datos | El usuario solo puede acceder al perfil asociado a su identidad autenticada |
| Perfiles ajenos | No deben exponerse por este caso de uso |
| Roles | El usuario puede ver su rol, pero no modificarlo |
| Permisos | El usuario puede ver permisos aplicables si se decide exponerlos, pero no modificarlos |
| Información sensible | No deben exponerse datos internos innecesarios |
| Validación backend | La propiedad del perfil debe validarse en backend/RLS, no solo en frontend |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El usuario autenticado accede a la sección de perfil. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El sistema identifica al usuario desde la sesión activa. | Si la sesión no es válida, rechaza la consulta. |
| 3 | El sistema consulta el perfil asociado al usuario autenticado. | Si el perfil no existe, muestra error controlado o deriva a resolución administrativa. |
| 4 | El sistema valida que el perfil consultado corresponde al usuario autenticado. | Si intenta consultar un perfil ajeno, rechaza la operación y registra el intento si corresponde. |
| 5 | El sistema obtiene datos básicos, rol y permisos aplicables. | Si no puede obtener rol o permisos, muestra información limitada o bloquea funciones sensibles. |
| 6 | El sistema muestra la información permitida del perfil. | Si ocurre un error técnico, informa la falla sin exponer datos internos. |
| 7 | El usuario visualiza su perfil. | Si intenta modificar rol o permisos, el sistema rechaza la acción por este flujo. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios |
| Estado visible al cliente | Sin cambios |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios |
| Estado de sesión | Sin cambios. La sesión continúa activa |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Consulta de perfil propio | Puede registrarse si se decide auditar consultas de perfil |
| Perfil no encontrado | Cuando no existe perfil operativo asociado al usuario autenticado |
| Intento de acceso a perfil ajeno | Cuando un usuario intenta consultar un perfil que no le pertenece |
| Error de consulta de perfil | Cuando ocurre una falla técnica durante la consulta |

## 11. Observaciones

- Este caso de uso no modifica el perfil.
- Este caso de uso no modifica roles.
- Este caso de uso no modifica permisos.
- Este caso de uso no crea usuarios.
- Este caso de uso no gestiona usuarios internos.
- Este caso de uso no modifica pedidos, archivos, estados, cobros ni impresión.
- La consulta de perfil propio debe estar protegida por RLS o mecanismo equivalente.
- El frontend puede mostrar u ocultar secciones según rol, pero la autorización real debe validarse en backend.
- El usuario no debe poder consultar perfiles de otros usuarios mediante manipulación de IDs.
- Si el perfil no existe o está incompleto, el sistema debe manejarlo de forma segura.

## 12. Poscondición

Al finalizar correctamente:

- el usuario visualiza la información permitida de su perfil;
- el usuario no accede a perfiles ajenos;
- no se modifican roles ni permisos;
- no se modifican datos de negocio;
- la sesión continúa activa;
- el acceso queda protegido por RLS, permisos y validaciones backend.

## 13. Criterios de aceptación

- El usuario autenticado puede consultar su propio perfil.
- El sistema rechaza consultas de usuarios no autenticados.
- El sistema rechaza intentos de consultar perfiles ajenos.
- El sistema muestra rol y datos básicos permitidos.
- El sistema no permite modificar roles ni permisos por este flujo.
- La consulta no modifica datos de negocio.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.