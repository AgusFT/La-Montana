# CU-AUT-005 - Asignar rol o permiso

| Campo | Valor |
|---|---|
| ID | CU-AUT-005 |
| Caso de uso | Asignar rol o permiso |
| Área | Usuarios y permisos |
| Actor principal | Administrador |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | Producto base |
| RF relacionados | RF-AUT-002, RF-AUT-003, RF-AUT-004, RF-AUT-005, RF-AUD-001, RF-AUD-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-SEG-006, RNF-AUT-002, RNF-AUT-004, RNF-AUT-005, RNF-RLS-003, RNF-AUD-001, RNF-AUD-004 |
| HU relacionadas | HU-ADM-003, HU-SIS-005 |
| Reglas críticas relacionadas | RNFC-001, RNFC-003, RNFC-005, RNFC-007 |

## 1. Caso de Uso

Asignar rol o permiso.

## 2. Actores

| Actor | Participación |
|---|---|
| Administrador | Asigna, modifica o revoca roles y permisos de usuarios internos según reglas del sistema |
| Sistema | Valida permisos administrativos, usuario objetivo, rol o permiso asignado, consistencia y auditoría |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un administrador asigna, modifica o revoca roles y permisos de un usuario del sistema **La Montaña**.

Los roles y permisos determinan qué acciones puede realizar cada usuario y qué información puede consultar o modificar.

Los roles iniciales confirmados del sistema son:

- cliente;
- empleado;
- administrador.

La asignación de roles o permisos debe realizarse con controles estrictos, porque afecta directamente la seguridad del sistema, el acceso a pedidos, archivos, estados, información financiera, auditoría y funciones internas.

Este caso de uso no crea usuarios internos. La creación, edición, activación o desactivación de usuarios internos se documenta en `CU-AUT-004-gestionar-usuario-interno.md`.

La autorización real no debe depender únicamente del frontend. Los permisos deben ser validados en backend, RLS, RPC, Edge Functions o mecanismos equivalentes.

## 4. Precondición

- El administrador está autenticado.
- El administrador tiene permisos para gestionar roles y permisos.
- El usuario objetivo existe.
- El usuario objetivo tiene un perfil operativo asociado.
- El backend Supabase está disponible.
- El sistema tiene definidos roles y permisos válidos.
- Las políticas RLS protegen perfiles, roles, permisos y datos sensibles.
- El sistema puede registrar auditoría de la operación.
- La operación no debe dejar al sistema sin administradores válidos ni generar permisos inconsistentes.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del usuario objetivo | Sí | Usuario al que se le asigna, modifica o revoca un rol o permiso |
| Administrador autenticado | Sí | Administrador que realiza la operación |
| Rol a asignar | No | Rol que se desea asignar, por ejemplo cliente, empleado o administrador |
| Permiso a asignar | No | Permiso específico que se desea otorgar |
| Rol o permiso a revocar | No | Rol o permiso que se desea quitar |
| Motivo del cambio | No | Justificación interna de la modificación |
| Observación administrativa | No | Comentario interno asociado al cambio |
| Canal de acceso | No | Web administrativa o canal interno habilitado |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Rol actualizado | Rol asignado, modificado o revocado |
| Permiso actualizado | Permiso asignado, modificado o revocado |
| Usuario objetivo actualizado | Usuario cuyo conjunto de roles o permisos fue modificado |
| Administrador responsable | Administrador que realizó el cambio |
| Fecha de actualización | Momento en que se registró el cambio |
| Evento de auditoría | Registro de trazabilidad asociado a la operación |
| Mensaje de resultado | Confirmación o error controlado |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere administrador autenticado |
| Autorización | Solo administradores con permisos específicos pueden asignar, modificar o revocar roles y permisos |
| RLS / acceso a datos | Roles, permisos y perfiles deben protegerse mediante políticas de acceso |
| Cliente final | Un cliente no puede asignar ni modificar roles o permisos |
| Empleado común | Un empleado sin permisos administrativos no puede asignar ni modificar roles o permisos |
| Escalamiento de privilegios | El sistema debe impedir asignaciones no autorizadas o inconsistentes |
| Validación backend | La operación no debe depender únicamente del frontend |
| Auditoría | Todo cambio de rol o permiso debe quedar registrado con usuario, fecha, rol/permiso anterior y nuevo |
| Integridad | El sistema debe evitar dejar usuarios con permisos incoherentes o sin perfil operativo válido |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El administrador accede a la sección de roles y permisos. | Si no tiene permisos administrativos suficientes, el sistema rechaza el acceso. |
| 3 | El administrador selecciona el usuario objetivo. | Si el usuario no existe o no está disponible, el sistema informa la situación. |
| 4 | El sistema valida que el administrador puede gestionar roles o permisos del usuario objetivo. | Si no tiene permisos suficientes, bloquea la operación. |
| 5 | El sistema muestra roles y permisos actuales del usuario objetivo. | Si no puede obtener la información actual, bloquea la modificación para evitar inconsistencias. |
| 6 | El administrador selecciona el rol o permiso que desea asignar, modificar o revocar. | Si el rol o permiso no existe, el sistema rechaza la selección. |
| 7 | El administrador confirma la operación. | Si cancela, no se aplican cambios. |
| 8 | El sistema valida reglas de seguridad e integridad. | Si el cambio genera escalamiento indebido o inconsistencia, rechaza la operación. |
| 9 | El sistema aplica la modificación de rol o permiso. | Si no puede aplicarla de forma consistente, revierte o bloquea la operación. |
| 10 | El sistema actualiza la información de autorización aplicable al usuario objetivo. | Si falla la actualización, debe impedir permisos parciales o contradictorios. |
| 11 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse una alerta técnica o evento equivalente. |
| 12 | El sistema informa al administrador que el cambio fue realizado correctamente. | Si ocurre un error final, informa la falla y evita duplicar cambios. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios en pedidos |
| Estado visible al cliente | Sin cambios |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios |
| Estado de usuario | Puede cambiar su rol, permisos o nivel de acceso |
| Estado de sesión | Puede requerir refrescar sesión, token o permisos efectivos del usuario afectado |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Rol asignado | Cuando se asigna un rol a un usuario |
| Rol revocado | Cuando se quita un rol a un usuario |
| Permiso asignado | Cuando se asigna un permiso específico |
| Permiso revocado | Cuando se quita un permiso específico |
| Intento no autorizado de asignación | Cuando un usuario sin permisos intenta modificar roles o permisos |
| Cambio bloqueado por inconsistencia | Cuando la operación se rechaza por romper reglas de seguridad |
| Error de asignación de rol o permiso | Cuando ocurre una falla técnica durante la operación |

## 11. Observaciones

- Este caso de uso no crea usuarios internos.
- Este caso de uso no desactiva usuarios internos.
- Este caso de uso no elimina usuarios.
- Este caso de uso no modifica pedidos.
- Este caso de uso no modifica archivos.
- Este caso de uso no modifica estados de pedidos.
- Este caso de uso no registra cobros ni comprobantes.
- Este caso de uso no ejecuta acciones de impresión.
- La gestión general del usuario interno se documenta en `CU-AUT-004-gestionar-usuario-interno.md`.
- La autorización real debe validarse en backend, RLS, RPC, Edge Functions o mecanismo equivalente.
- El frontend puede adaptar vistas según rol, pero ocultar botones no es suficiente como mecanismo de seguridad.
- Si un usuario pierde permisos, sus accesos posteriores deben reflejar el cambio.
- Si un usuario gana permisos, el sistema debe asegurar que el cambio sea intencional, autorizado y auditado.
- Deben evitarse escaladas de privilegios no autorizadas.
- No deben eliminarse registros históricos necesarios para auditoría.

## 12. Poscondición

Al finalizar correctamente:

- el rol o permiso queda asignado, modificado o revocado;
- el usuario objetivo queda con permisos coherentes;
- el administrador responsable queda registrado;
- la operación queda auditada;
- el sistema mantiene protegidos datos y acciones sensibles;
- no se modifican pedidos, archivos, cobros, comprobantes ni trabajos de impresión;
- las operaciones posteriores del usuario quedan sujetas a los roles, permisos, RLS y validaciones backend actualizadas.

## 13. Criterios de aceptación

- El administrador autenticado puede asignar, modificar o revocar roles y permisos si tiene autorización.
- El sistema rechaza operaciones de usuarios no autorizados.
- El sistema valida que el usuario objetivo existe.
- El sistema valida que el rol o permiso existe.
- El sistema evita escaladas de privilegios no autorizadas.
- El sistema evita permisos incoherentes o contradictorios.
- La operación queda registrada para trazabilidad.
- Los cambios de permisos se reflejan en acciones posteriores del usuario.
- La operación no modifica datos de negocio.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.