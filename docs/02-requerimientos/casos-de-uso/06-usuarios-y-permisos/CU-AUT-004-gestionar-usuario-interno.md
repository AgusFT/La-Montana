# CU-AUT-004 - Gestionar usuario interno

| Campo | Valor |
|---|---|
| ID | CU-AUT-004 |
| Caso de uso | Gestionar usuario interno |
| Área | Usuarios y permisos |
| Actor principal | Administrador |
| Actores secundarios | Sistema, Supabase Auth |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-AUT-002, RF-AUT-003, RF-AUT-004, RF-AUT-005, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-SEG-006, RNF-AUT-002, RNF-AUT-004, RNF-AUT-005, RNF-RLS-003, RNF-AUD-001, RNF-AUD-004 |
| HU relacionadas | HU-ADM-003, HU-SIS-005 |
| Reglas críticas relacionadas | RNFC-001, RNFC-003, RNFC-005, RNFC-007 |

## 1. Caso de Uso

Gestionar usuario interno.

## 2. Actores

| Actor | Participación |
|---|---|
| Administrador | Crea, consulta, edita, activa o desactiva usuarios internos según permisos |
| Sistema | Valida permisos administrativos, datos del usuario, relación con Supabase Auth, perfil interno y auditoría |
| Supabase Auth | Gestiona la identidad autenticable del usuario cuando corresponde |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un administrador gestiona usuarios internos del sistema **La Montaña**.

Un usuario interno representa una persona que participa en la operación del negocio, por ejemplo tareas administrativas, revisión, producción, atención, entrega, cobro o supervisión.

La gestión de usuarios internos puede incluir:

- crear un usuario interno;
- consultar usuarios internos existentes;
- editar datos básicos del usuario interno;
- activar o desactivar acceso;
- asociar el usuario interno con un perfil operativo;
- registrar cambios administrativos relevantes.

Este caso de uso no debe confundirse con la asignación detallada de roles o permisos. La asignación o modificación específica de roles y permisos se documenta en `CU-AUT-005-asignar-rol-o-permiso.md`.

La gestión de usuarios internos debe realizarse con controles fuertes de seguridad, porque afecta el acceso a información sensible del sistema.

## 4. Precondición

- El administrador está autenticado.
- El administrador tiene permisos para gestionar usuarios internos.
- Supabase Auth está disponible cuando la operación requiere crear o modificar identidad autenticable.
- El backend Supabase está disponible.
- El sistema tiene definido el modelo de perfiles internos.
- El sistema tiene definidos los roles iniciales: cliente, empleado y administrador.
- Las políticas RLS protegen perfiles, usuarios internos, roles y permisos.
- La operación debe ejecutarse desde un contexto administrativo autorizado.
- El sistema puede registrar auditoría de las operaciones realizadas.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del usuario interno | No | Identificador del usuario interno cuando se consulta, edita, activa o desactiva |
| Administrador autenticado | Sí | Usuario administrador que realiza la operación |
| Email o identificador de acceso | Sí para creación | Identificador usado para la cuenta del usuario interno |
| Nombre o datos básicos | Sí para creación | Datos mínimos del perfil interno |
| Estado del usuario | No | Activo, inactivo, bloqueado o equivalente según modelo definido |
| Tipo de usuario interno | No | Clasificación operativa si se define, por ejemplo empleado o administrador |
| Observación administrativa | No | Comentario interno asociado a la gestión del usuario |
| Canal de acceso | No | Web administrativa o canal interno habilitado |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Usuario interno gestionado | Usuario creado, consultado, actualizado, activado o desactivado |
| Perfil interno asociado | Perfil operativo vinculado al usuario |
| Estado actualizado | Estado operativo del usuario interno luego de la acción |
| Identidad Auth asociada | Relación con Supabase Auth si corresponde |
| Usuario administrador responsable | Administrador que realizó la operación |
| Evento de auditoría | Registro de trazabilidad asociado a la operación |
| Mensaje de resultado | Confirmación o error controlado |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere administrador autenticado |
| Autorización | Solo administradores con permisos específicos pueden gestionar usuarios internos |
| RLS / acceso a datos | Perfiles, usuarios internos, roles y permisos deben protegerse mediante políticas de acceso |
| Supabase Auth | Las operaciones sobre identidad autenticable deben ejecutarse mediante mecanismo seguro y autorizado |
| Cliente final | Un cliente no puede gestionar usuarios internos |
| Empleado común | Un empleado sin permisos administrativos no puede gestionar usuarios internos |
| Roles y permisos | La asignación detallada se controla en `CU-AUT-005` |
| Validación backend | La operación no debe depender únicamente del frontend |
| Auditoría | Toda creación, edición, activación o desactivación debe quedar registrada |
| Integridad | No deben quedar usuarios internos sin perfil operativo coherente |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El administrador accede a la sección de gestión de usuarios internos. | Si no tiene permisos administrativos, el sistema rechaza el acceso. |
| 3 | El sistema muestra usuarios internos existentes o una opción de creación. | Si no puede consultar usuarios, informa un error controlado. |
| 4 | El administrador selecciona crear, consultar, editar, activar o desactivar un usuario interno. | Si la acción no está permitida para su perfil, el sistema la bloquea. |
| 5 | El sistema solicita o muestra los datos necesarios para la acción elegida. | Si faltan datos obligatorios, el sistema solicita completarlos. |
| 6 | El administrador confirma la operación. | Si cancela, no se aplican cambios. |
| 7 | El sistema valida datos, permisos y consistencia de la operación. | Si los datos son inválidos o el usuario ya existe, informa el motivo. |
| 8 | Si corresponde, el sistema crea o actualiza la identidad en Supabase Auth mediante mecanismo autorizado. | Si falla la operación de Auth, no debe crearse un perfil interno inconsistente. |
| 9 | El sistema crea o actualiza el perfil interno asociado. | Si falla el perfil, debe revertir o marcar la operación para revisión técnica según corresponda. |
| 10 | El sistema actualiza el estado del usuario interno si corresponde. | Si la transición de estado no es válida, rechaza la operación. |
| 11 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 12 | El sistema informa al administrador que la operación fue realizada correctamente. | Si ocurre un error final, informa la falla y evita duplicar usuarios o estados inconsistentes. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios en pedidos |
| Estado visible al cliente | Sin cambios |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios |
| Estado de usuario interno | Puede cambiar a activo, inactivo, bloqueado o equivalente según operación |
| Estado de sesión | Sin cambios para el administrador; el usuario gestionado puede requerir iniciar sesión posteriormente |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Usuario interno creado | Cuando se crea un nuevo usuario interno |
| Usuario interno actualizado | Cuando se modifican datos básicos del usuario |
| Usuario interno activado | Cuando se habilita el acceso u operación del usuario |
| Usuario interno desactivado | Cuando se deshabilita el acceso u operación del usuario |
| Intento no autorizado de gestión | Cuando un usuario sin permisos intenta gestionar usuarios internos |
| Error de gestión de usuario | Cuando ocurre una falla técnica durante la operación |
| Inconsistencia Auth/perfil | Cuando se detecta inconsistencia entre Supabase Auth y perfil interno |

## 11. Observaciones

- Este caso de uso no permite al cliente gestionar usuarios internos.
- Este caso de uso no debe ejecutarse desde interfaces públicas.
- Este caso de uso no modifica pedidos.
- Este caso de uso no modifica archivos.
- Este caso de uso no modifica estados de pedidos.
- Este caso de uso no registra cobros ni comprobantes.
- Este caso de uso no ejecuta acciones de impresión.
- La asignación detallada de roles y permisos se documenta en `CU-AUT-005-asignar-rol-o-permiso.md`.
- La operación debe protegerse con validaciones backend, RLS, políticas y auditoría.
- Si se crea un usuario en Supabase Auth, debe existir un perfil interno coherente.
- Si se desactiva un usuario interno, debe impedirse su uso operativo sin eliminar necesariamente su trazabilidad histórica.
- No deben eliminarse registros históricos necesarios para auditoría.

## 12. Poscondición

Al finalizar correctamente:

- el usuario interno queda creado, actualizado, activado o desactivado según la operación;
- el perfil interno queda asociado y coherente;
- la identidad en Supabase Auth queda vinculada si corresponde;
- el administrador responsable queda registrado;
- la operación queda auditada;
- no se modifican pedidos, archivos, cobros, comprobantes ni trabajos de impresión;
- el acceso posterior del usuario interno queda sujeto a roles, permisos, RLS y validaciones backend.

## 13. Criterios de aceptación

- El administrador autenticado puede gestionar usuarios internos si tiene permisos.
- El sistema rechaza operaciones de usuarios no autorizados.
- El sistema valida datos obligatorios antes de crear o actualizar usuarios.
- El sistema evita usuarios internos sin perfil operativo coherente.
- El sistema registra creación, edición, activación o desactivación para trazabilidad.
- La operación no permite modificar roles o permisos detallados fuera del flujo correspondiente.
- La operación no modifica datos de negocio.
- La operación respeta Supabase Auth como mecanismo de autenticación.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.