# CU-AUT-002 - Cerrar sesión

| Campo                        | Valor                                                                                                  |
| ---------------------------- | ------------------------------------------------------------------------------------------------------ |
| ID                           | CU-AUT-002                                                                                             |
| Caso de uso                  | Cerrar sesión                                                                                          |
| Área                         | Usuarios y permisos                                                                                    |
| Actor principal              | Usuario                                                                                                |
| Actores secundarios          | Sistema, Supabase Auth                                                                                 |
| Prioridad                    | P1 Alta                                                                                                |
| Alcance                      | MVP                                                                                                    |
| RF relacionados              | RF-AUT-001, RF-AUT-003, RF-AUT-005, RF-AUT-006, RF-WEB-001, RF-AND-002                                 |
| RNF relacionados             | RNF-SEG-001, RNF-SEG-002, RNF-SEG-003, RNF-SEG-005, RNF-AUT-001, RNF-AUT-003, RNF-COM-002, RNF-COM-003 |
| HU relacionadas              | HU-CLI-001, HU-EMP-001, HU-ADM-008                                                                     |
| Reglas críticas relacionadas | RNFC-001, RNFC-003, RNFC-005, RNFC-009                                                                 |

## 1. Caso de Uso

Cerrar sesión.

## 2. Actores

| Actor         | Participación                                                                                                                     |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| Usuario       | Solicita cerrar su sesión activa desde Web o Android                                                                              |
| Sistema       | Finaliza la sesión local, invalida o descarta credenciales activas según el mecanismo disponible y redirige a una pantalla segura |
| Supabase Auth | Gestiona la sesión autenticada y su cierre según el cliente utilizado                                                             |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un usuario cierra su sesión activa en **La Montaña**.

El cierre de sesión debe impedir que el usuario continúe accediendo a vistas, acciones o datos protegidos sin volver a autenticarse.

El sistema debe limpiar el estado de sesión del cliente Web o Android y evitar que queden datos sensibles visibles después del cierre.

Cerrar sesión no modifica pedidos, archivos, estados, cobros, comprobantes, auditoría ni trabajos de impresión.

Este caso aplica a los roles iniciales del sistema:

* cliente;
* empleado;
* administrador.

## 4. Precondición

* El usuario está autenticado.
* Existe una sesión activa en Web o Android.
* Supabase Auth está disponible o el cliente puede limpiar su sesión local de forma segura.
* El sistema puede redirigir al usuario a una pantalla pública, de login o de sesión finalizada.
* La interfaz no debe mantener datos sensibles visibles luego del cierre.

## 5. Datos de entrada

| Dato                   | Obligatorio | Descripción                                       |
| ---------------------- | ----------- | ------------------------------------------------- |
| Usuario autenticado    | Sí          | Usuario que solicita cerrar sesión                |
| Sesión activa          | Sí          | Sesión actual que debe finalizarse                |
| Canal de acceso        | No          | Web o Android, según desde dónde se cierra sesión |
| Confirmación de cierre | No          | Confirmación explícita si el flujo la requiere    |

## 6. Datos de salida

| Dato                     | Descripción                                                        |
| ------------------------ | ------------------------------------------------------------------ |
| Sesión finalizada        | Sesión cerrada o descartada correctamente                          |
| Estado de sesión         | Estado actualizado a no autenticado                                |
| Redirección segura       | Pantalla pública, login o pantalla de sesión cerrada               |
| Mensaje al usuario       | Confirmación opcional de cierre de sesión                          |
| Limpieza de estado local | Datos sensibles de sesión removidos del cliente cuando corresponda |

## 7. Permisos y seguridad

| Aspecto               | Regla                                                                                      |
| --------------------- | ------------------------------------------------------------------------------------------ |
| Autenticación         | Requiere una sesión activa para cerrar sesión                                              |
| Autorización          | Cualquier usuario autenticado puede cerrar su propia sesión                                |
| RLS / acceso a datos  | Luego del cierre, no debe conservarse acceso autenticado a datos protegidos                |
| Cliente final         | Al cerrar sesión no debe poder consultar pedidos ni archivos hasta autenticarse nuevamente |
| Usuario interno       | Al cerrar sesión no debe poder acceder a vistas internas hasta autenticarse nuevamente     |
| Tokens o credenciales | Deben descartarse o invalidarse según el mecanismo de Supabase Auth y cliente utilizado    |
| Datos sensibles en UI | Deben limpiarse o quedar inaccesibles después del cierre                                   |
| Validación backend    | Las acciones posteriores al cierre deben ser rechazadas por no tener sesión válida         |

## 8. Flujo principal

| Paso | Flujo principal                                                                        | Flujo alternativo / excepciones                                                                                   |
| ---- | -------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| 1    | El usuario autenticado selecciona la opción de cerrar sesión.                          | Si no hay sesión activa, el sistema redirige a login o pantalla pública.                                          |
| 2    | El sistema solicita confirmación si el flujo lo requiere.                              | Si el usuario cancela, la sesión permanece activa.                                                                |
| 3    | El sistema ejecuta el cierre de sesión mediante Supabase Auth o mecanismo equivalente. | Si Supabase Auth no responde, el cliente debe limpiar sesión local o informar error controlado según corresponda. |
| 4    | El sistema elimina o descarta datos locales de sesión.                                 | Si no puede limpiar algún dato local, debe impedir acceso a vistas protegidas.                                    |
| 5    | El sistema actualiza el estado del usuario a no autenticado en la interfaz.            | Si la interfaz no puede actualizarse, debe forzar redirección segura.                                             |
| 6    | El sistema redirige al usuario a login, pantalla pública o pantalla de sesión cerrada. | Si falla la redirección, no debe permitir navegación protegida.                                                   |
| 7    | El usuario intenta acceder a una vista protegida luego del cierre.                     | El sistema debe solicitar autenticación nuevamente.                                                               |

## 9. Impacto en estados

| Estado                      | Impacto                                     |
| --------------------------- | ------------------------------------------- |
| Estado interno              | Sin cambios                                 |
| Estado visible al cliente   | Sin cambios                                 |
| Estado financiero           | Sin cambios                                 |
| Estado técnico de impresión | Sin cambios                                 |
| Estado de sesión            | Cambia a sesión finalizada o no autenticada |

## 10. Eventos de auditoría

| Evento                      | Cuándo se registra                                                             |
| --------------------------- | ------------------------------------------------------------------------------ |
| Cierre de sesión exitoso    | Cuando el usuario cierra sesión correctamente, si se decide auditar sesiones   |
| Error al cerrar sesión      | Cuando ocurre una falla técnica durante el cierre                              |
| Acceso posterior sin sesión | Cuando se intenta acceder a una acción o dato protegido luego de cerrar sesión |
| Limpieza de sesión local    | Puede registrarse si se decide auditar eventos técnicos de sesión              |

## 11. Observaciones

* Este caso de uso no modifica pedidos.
* Este caso de uso no modifica archivos.
* Este caso de uso no modifica estados.
* Este caso de uso no registra cobros ni comprobantes.
* Este caso de uso no ejecuta acciones de impresión.
* Cerrar sesión no elimina la cuenta del usuario.
* Cerrar sesión no cambia roles ni permisos.
* Web y Android deben manejar el cierre de sesión de forma consistente.
* Las vistas protegidas no deben quedar accesibles luego del cierre.
* La seguridad real debe sostenerse por sesión válida, RLS, permisos y validaciones backend.
* La interfaz debe evitar mostrar datos sensibles cacheados después del cierre.

## 12. Poscondición

Al finalizar correctamente:

* la sesión activa queda cerrada;
* el usuario queda en estado no autenticado;
* las vistas protegidas quedan inaccesibles hasta nuevo inicio de sesión;
* los datos sensibles de sesión se limpian o quedan inaccesibles;
* no se modifican pedidos, archivos, estados, cobros ni trabajos de impresión;
* el usuario puede iniciar sesión nuevamente si lo desea.

## 13. Criterios de aceptación

* El usuario autenticado puede cerrar su propia sesión.
* El sistema finaliza o descarta la sesión activa.
* El sistema impide acceder a vistas protegidas luego del cierre.
* El sistema redirige a una pantalla segura.
* El cierre de sesión funciona de forma coherente en Web y Android.
* El cierre de sesión no modifica datos de negocio.
* El cierre de sesión no cambia roles ni permisos.
* Las acciones protegidas posteriores requieren autenticación nuevamente.
* El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.
