# CU-AUT-001 - Iniciar sesión

| Campo                        | Valor                                                                                                  |
| ---------------------------- | ------------------------------------------------------------------------------------------------------ |
| ID                           | CU-AUT-001                                                                                             |
| Caso de uso                  | Iniciar sesión                                                                                         |
| Área                         | Usuarios y permisos                                                                                    |
| Actor principal              | Usuario                                                                                                |
| Actores secundarios          | Sistema, Supabase Auth                                                                                 |
| Prioridad                    | P0 Crítica                                                                                             |
| Alcance                      | MVP                                                                                                    |
| RF relacionados              | RF-AUT-001, RF-AUT-002, RF-AUT-003, RF-AUT-005, RF-AUT-006, RF-WEB-001, RF-AND-002                     |
| RNF relacionados             | RNF-SEG-001, RNF-SEG-002, RNF-SEG-003, RNF-SEG-005, RNF-AUT-001, RNF-AUT-002, RNF-AUT-003, RNF-RLS-001 |
| HU relacionadas              | HU-CLI-001, HU-EMP-001, HU-ADM-008                                                                     |
| Reglas críticas relacionadas | RNFC-001, RNFC-003, RNFC-005, RNFC-009                                                                 |

## 1. Caso de Uso

Iniciar sesión.

## 2. Actores

| Actor         | Participación                                                                             |
| ------------- | ----------------------------------------------------------------------------------------- |
| Usuario       | Ingresa sus credenciales para acceder al sistema según su rol y permisos                  |
| Sistema       | Valida credenciales, obtiene perfil, rol y permisos, y habilita el acceso correspondiente |
| Supabase Auth | Servicio responsable de autenticar al usuario                                             |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un usuario inicia sesión en **La Montaña**.

El inicio de sesión permite autenticar a clientes, empleados y administradores para acceder a las funcionalidades correspondientes según su rol y permisos.

El sistema debe usar Supabase Auth como mecanismo central de autenticación. Luego de autenticarse, el sistema debe obtener o consultar la información necesaria del perfil del usuario para determinar qué vistas, acciones y datos puede utilizar.

Iniciar sesión no debe otorgar permisos por sí solo más allá de la autenticación. La autorización debe resolverse mediante roles, permisos, RLS, políticas de acceso, RPC, Edge Functions o mecanismos equivalentes definidos en el backend.

El frontend puede adaptar la interfaz según el rol, pero la seguridad real no debe depender únicamente de ocultar botones, rutas o pantallas.

## 4. Precondición

* El usuario tiene una cuenta registrada en el sistema.
* Supabase Auth está disponible.
* El backend Supabase está disponible.
* El usuario accede desde Web o Android.
* El sistema tiene definidos roles iniciales: cliente, empleado y administrador.
* El sistema puede consultar o derivar el perfil, rol y permisos del usuario autenticado.
* Las políticas RLS deben proteger los datos sensibles luego de la autenticación.

## 5. Datos de entrada

| Dato                     | Obligatorio | Descripción                                                                 |
| ------------------------ | ----------- | --------------------------------------------------------------------------- |
| Identificador de usuario | Sí          | Email, usuario u otro identificador definido para iniciar sesión            |
| Credencial secreta       | Sí          | Contraseña u otro mecanismo de autenticación definido                       |
| Canal de acceso          | No          | Web o Android, según desde dónde se inicia sesión                           |
| Información de sesión    | No          | Datos técnicos de sesión, dispositivo o contexto si el sistema los registra |
| Factor adicional         | No          | Segundo factor u otro mecanismo adicional si se incorpora en el futuro      |

## 6. Datos de salida

| Dato                | Descripción                                                  |
| ------------------- | ------------------------------------------------------------ |
| Sesión autenticada  | Sesión válida generada por Supabase Auth                     |
| Usuario autenticado | Identificador del usuario que inició sesión                  |
| Perfil del usuario  | Datos mínimos necesarios para operar dentro del sistema      |
| Rol del usuario     | Rol asignado: cliente, empleado, administrador u otro futuro |
| Permisos aplicables | Permisos derivados del rol o configuración                   |
| Vista inicial       | Dashboard, pantalla o módulo inicial según rol               |
| Mensaje de error    | Mensaje controlado si la autenticación falla                 |

## 7. Permisos y seguridad

| Aspecto              | Regla                                                                              |
| -------------------- | ---------------------------------------------------------------------------------- |
| Autenticación        | Se realiza mediante Supabase Auth                                                  |
| Autorización         | Se determina luego de autenticar, usando rol y permisos                            |
| RLS / acceso a datos | La autenticación no reemplaza las políticas de acceso a datos                      |
| Cliente final        | Solo debe acceder a sus propios pedidos, archivos e información visible            |
| Usuario interno      | Solo debe acceder a funcionalidades internas según permisos                        |
| Administrador        | Puede acceder a funciones ampliadas según permisos definidos                       |
| Sesión               | La sesión debe manejarse de forma segura y consistente entre Web y Android         |
| Validación backend   | Las acciones sensibles deben validarse en backend, no solo en frontend             |
| Datos sensibles      | No deben exponerse perfiles, roles, pedidos o archivos fuera del alcance permitido |

## 8. Flujo principal

| Paso | Flujo principal                                                          | Flujo alternativo / excepciones                                                                      |
| ---- | ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------- |
| 1    | El usuario accede a la pantalla de inicio de sesión desde Web o Android. | Si el sistema no está disponible, informa una falla controlada.                                      |
| 2    | El usuario ingresa sus credenciales.                                     | Si faltan datos obligatorios, el sistema solicita completarlos.                                      |
| 3    | El usuario confirma el inicio de sesión.                                 | Si cancela, no se inicia sesión.                                                                     |
| 4    | El sistema envía las credenciales a Supabase Auth.                       | Si Supabase Auth no responde, informa error controlado.                                              |
| 5    | Supabase Auth valida las credenciales.                                   | Si las credenciales son inválidas, rechaza el inicio de sesión.                                      |
| 6    | El sistema recibe una sesión autenticada.                                | Si no se puede crear sesión válida, bloquea el acceso.                                               |
| 7    | El sistema obtiene o consulta el perfil del usuario.                     | Si el perfil no existe o está incompleto, bloquea o deriva a resolución administrativa según reglas. |
| 8    | El sistema determina rol y permisos aplicables.                          | Si el rol no está definido, bloquea el acceso a funcionalidades sensibles.                           |
| 9    | El sistema habilita la vista inicial correspondiente al rol.             | Si no puede determinar vista inicial, muestra una pantalla segura o mensaje controlado.              |
| 10   | El usuario accede al sistema con permisos aplicables.                    | Si intenta acceder a una función no permitida, el backend debe rechazar la operación.                |

## 9. Impacto en estados

| Estado                      | Impacto                            |
| --------------------------- | ---------------------------------- |
| Estado interno              | Sin cambios                        |
| Estado visible al cliente   | Sin cambios                        |
| Estado financiero           | Sin cambios                        |
| Estado técnico de impresión | Sin cambios                        |
| Estado de sesión            | Cambia a sesión autenticada activa |

## 10. Eventos de auditoría

| Evento                          | Cuándo se registra                                                             |
| ------------------------------- | ------------------------------------------------------------------------------ |
| Inicio de sesión exitoso        | Cuando un usuario se autentica correctamente                                   |
| Inicio de sesión fallido        | Cuando las credenciales son inválidas o la autenticación falla                 |
| Perfil no encontrado            | Cuando existe sesión autenticada pero no perfil operativo asociado             |
| Rol no definido                 | Cuando no puede determinarse rol o permisos del usuario                        |
| Intento de acceso no autorizado | Cuando un usuario autenticado intenta acceder a una acción o dato no permitido |
| Error de autenticación          | Cuando ocurre una falla técnica durante el inicio de sesión                    |

## 11. Observaciones

* Este caso de uso autentica al usuario, pero no reemplaza la autorización.
* La autorización debe validarse en backend, RLS, RPC, Edge Functions o mecanismo equivalente.
* Supabase Auth es el mecanismo central de autenticación.
* Web y Android deben consumir el mismo backend y respetar las mismas reglas de sesión y permisos.
* El cliente final no debe acceder a información interna del negocio.
* El empleado no debe acceder a funciones administrativas si no tiene permisos.
* El administrador debe tener permisos ampliados, pero igualmente controlados.
* La interfaz puede adaptarse por rol, pero ocultar botones no es una medida de seguridad suficiente.
* Las sesiones deben manejarse de forma segura y evitar exposición innecesaria de tokens o datos sensibles.
* Este caso de uso no crea pedidos, no modifica pedidos, no cambia estados y no opera impresión.

## 12. Poscondición

Al finalizar correctamente:

* el usuario queda autenticado;
* se crea una sesión válida;
* el sistema conoce el usuario autenticado;
* el sistema puede determinar rol y permisos;
* el usuario accede a la vista inicial correspondiente;
* las operaciones posteriores quedan sujetas a permisos, RLS y validaciones backend;
* no se modifican pedidos, archivos, estados, cobros ni trabajos de impresión.

## 13. Criterios de aceptación

* El usuario puede iniciar sesión con credenciales válidas.
* El sistema rechaza credenciales inválidas.
* El sistema obtiene perfil, rol y permisos luego de autenticar.
* El cliente accede solo a funcionalidades y datos permitidos para cliente.
* El empleado accede solo a funcionalidades y datos permitidos para empleado.
* El administrador accede solo a funcionalidades y datos permitidos para administrador.
* Web y Android usan el mismo backend de autenticación.
* La autenticación no reemplaza las políticas RLS ni las validaciones de autorización.
* Las acciones sensibles siguen protegidas en backend.
* El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.
