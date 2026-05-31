# CU-CIE-003 - Cerrar pedido

| Campo                        | Valor                                                                                                                  |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| ID                           | CU-CIE-003                                                                                                             |
| Caso de uso                  | Cerrar pedido                                                                                                          |
| Área                         | Trazabilidad y cierre                                                                                                  |
| Actor principal              | Administrador                                                                                                          |
| Actores secundarios          | Empleado autorizado, Sistema                                                                                           |
| Prioridad                    | P0 Crítica                                                                                                             |
| Alcance                      | Producto base                                                                                                          |
| RF relacionados              | RF-FIN-006, RF-AUD-001, RF-AUD-002, RF-AUD-004, RF-AUD-005, RF-EST-001, RF-EST-002, RF-EST-003, RF-EST-004, RF-PED-008 |
| RNF relacionados             | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-004, RNF-AUD-001, RNF-AUD-003, RNF-AUD-004, RNF-REN-004                 |
| HU relacionadas              | HU-SIS-004, HU-ADM-005, HU-ADM-006, HU-EMP-006                                                                         |
| Reglas críticas relacionadas | RFC-006, RNFC-001, RNFC-003, RNFC-007, RNFC-008                                                                        |

## 1. Caso de Uso

Cerrar pedido.

## 2. Actores

| Actor               | Participación                                                                                                 |
| ------------------- | ------------------------------------------------------------------------------------------------------------- |
| Administrador       | Cierra el pedido cuando se cumplen todas las condiciones operativas, financieras, documentales y de auditoría |
| Empleado autorizado | Puede cerrar el pedido solo si cuenta con permisos explícitos para esta acción                                |
| Sistema             | Valida condiciones de cierre, permisos, consistencia de estados, entrega, cobro, comprobante y auditoría      |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un administrador o empleado autorizado cierra un pedido.

Cerrar un pedido significa marcarlo como finalizado de forma consistente dentro del sistema. El cierre no depende únicamente de que el pedido haya sido impreso. Para cerrar un pedido deben cumplirse condiciones operativas, financieras, documentales y de trazabilidad.

Antes de cerrar el pedido, el sistema debe validar que exista consistencia entre:

* entrega;
* cobro;
* comprobante;
* auditoría;
* estado final.

El cierre del pedido es una acción crítica porque representa el final del ciclo operativo del pedido. Una vez cerrado, el pedido no debería poder modificarse por flujos ordinarios, salvo procesos específicos de corrección administrativa, auditoría o soporte definidos posteriormente.

Este caso de uso utiliza como precondición lógica la validación definida en `CU-CIE-002-validar-condiciones-de-cierre.md`.

## 4. Precondición

* El administrador o empleado autorizado está autenticado.
* El usuario tiene permisos explícitos para cerrar pedidos.
* El pedido existe.
* El pedido pertenece al flujo operativo de la imprenta.
* El pedido no fue cerrado previamente.
* El pedido no fue cancelado.
* La entrega fue registrada cuando corresponde.
* El cobro final fue registrado cuando corresponde.
* El comprobante fue registrado o asociado cuando corresponde.
* Existen eventos de auditoría suficientes para respaldar el cierre.
* El estado interno, estado visible y estado financiero son consistentes.
* La validación de condiciones de cierre fue ejecutada o puede ejecutarse antes del cierre.
* El backend Supabase está disponible.
* Las políticas de acceso protegen información financiera, auditoría y cierre.

## 5. Datos de entrada

| Dato                              | Obligatorio | Descripción                                                                  |
| --------------------------------- | ----------- | ---------------------------------------------------------------------------- |
| ID del pedido                     | Sí          | Identificador del pedido que se desea cerrar                                 |
| Usuario interno autenticado       | Sí          | Administrador o empleado autorizado que solicita el cierre                   |
| Confirmación de cierre            | Sí          | Confirmación explícita de que se desea cerrar el pedido                      |
| Resultado de validación de cierre | Sí          | Resultado de validación de condiciones de cierre                             |
| Observación interna               | No          | Comentario interno asociado al cierre                                        |
| Fecha de cierre                   | No          | Fecha efectiva de cierre si se registra explícitamente                       |
| Canal de acceso                   | No          | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato                                  | Descripción                                       |
| ------------------------------------- | ------------------------------------------------- |
| Pedido cerrado                        | Pedido marcado como cerrado o finalizado          |
| Estado interno actualizado            | Estado interno final del pedido                   |
| Estado visible al cliente actualizado | Estado visible final o equivalente                |
| Estado financiero final               | Estado financiero consistente con el cierre       |
| Usuario que cierra                    | Usuario interno que ejecutó el cierre             |
| Fecha de cierre registrada            | Momento en que se registró el cierre              |
| Evento de auditoría                   | Registro de trazabilidad asociado al cierre       |
| Resultado de cierre                   | Confirmación de cierre exitoso o error controlado |

## 7. Permisos y seguridad

| Aspecto                | Regla                                                                                       |
| ---------------------- | ------------------------------------------------------------------------------------------- |
| Autenticación          | Requiere administrador o empleado autorizado autenticado                                    |
| Autorización           | Solo usuarios internos con permiso explícito pueden cerrar pedidos                          |
| RLS / acceso a datos   | Pedido, estados, cobros, comprobantes, entrega y auditoría deben estar protegidos           |
| Cliente final          | El cliente no puede cerrar pedidos por este flujo                                           |
| Validación previa      | El cierre debe validar condiciones de entrega, cobro, comprobante, auditoría y estado final |
| Estados                | El cierre debe actualizar estados de forma consistente                                      |
| Información financiera | Debe existir consistencia financiera antes de cerrar                                        |
| Validación backend     | El cierre no debe depender únicamente del frontend                                          |
| Auditoría              | El cierre debe quedar registrado con usuario, fecha, estado previo, estado final y contexto |
| Integridad             | El sistema debe impedir cierres incompletos o contradictorios                               |

## 8. Flujo principal

| Paso | Flujo principal                                                              | Flujo alternativo / excepciones                                                               |
| ---- | ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| 1    | El administrador o empleado autorizado ingresa al sistema.                   | Si no está autenticado, el sistema solicita iniciar sesión.                                   |
| 2    | El usuario accede al pedido desde una vista interna.                         | Si no tiene permisos internos, el sistema rechaza el acceso.                                  |
| 3    | El usuario selecciona la opción de cerrar pedido.                            | Si no tiene permiso para cerrar pedidos, el sistema bloquea la acción.                        |
| 4    | El sistema valida que el pedido existe y está disponible.                    | Si el pedido no existe, fue cancelado o ya está cerrado, rechaza la operación.                |
| 5    | El sistema ejecuta o consulta la validación de condiciones de cierre.        | Si la validación no puede ejecutarse, bloquea el cierre.                                      |
| 6    | El sistema verifica entrega registrada.                                      | Si falta entrega requerida, bloquea el cierre e informa condición pendiente.                  |
| 7    | El sistema verifica cobro y estado financiero.                               | Si existe saldo pendiente o inconsistencia financiera, bloquea el cierre.                     |
| 8    | El sistema verifica comprobante asociado cuando corresponde.                 | Si falta comprobante requerido, bloquea el cierre.                                            |
| 9    | El sistema verifica eventos de auditoría relevantes.                         | Si falta trazabilidad mínima, bloquea o deriva a revisión según reglas.                       |
| 10   | El sistema verifica consistencia entre estado interno, visible y financiero. | Si existe combinación inconsistente, bloquea el cierre.                                       |
| 11   | El usuario confirma el cierre del pedido.                                    | Si cancela, no se modifica el pedido.                                                         |
| 12   | El sistema actualiza el estado interno a cerrado o equivalente.              | Si no puede actualizarlo, revierte o bloquea la operación.                                    |
| 13   | El sistema actualiza el estado visible al cliente si corresponde.            | Si no puede actualizarlo consistentemente, evita dejar estados contradictorios.               |
| 14   | El sistema confirma el estado financiero final.                              | Si no puede confirmarlo, bloquea el cierre.                                                   |
| 15   | El sistema registra evento de auditoría de cierre.                           | Si la auditoría falla, debe registrarse alerta técnica o bloquear el cierre según criticidad. |
| 16   | El sistema informa que el pedido fue cerrado correctamente.                  | Si ocurre un error final, informa la falla y evita duplicar cierres.                          |

## 9. Impacto en estados

| Estado                      | Impacto                                                                               |
| --------------------------- | ------------------------------------------------------------------------------------- |
| Estado interno              | Cambia a cerrado, finalizado o equivalente                                            |
| Estado visible al cliente   | Cambia a entregado/cerrado/finalizado o equivalente según comunicación definida       |
| Estado financiero           | Debe quedar en estado final consistente, por ejemplo pagado, saldado o equivalente    |
| Estado técnico de impresión | Sin cambios directo. La impresión completada no es suficiente por sí sola para cerrar |
| Estado de cierre            | Cambia a cerrado o equivalente                                                        |

## 10. Eventos de auditoría

| Evento                                   | Cuándo se registra                                                               |
| ---------------------------------------- | -------------------------------------------------------------------------------- |
| Cierre solicitado                        | Cuando un usuario autorizado intenta cerrar el pedido                            |
| Condiciones de cierre validadas          | Cuando el sistema verifica entrega, cobro, comprobante, auditoría y estado final |
| Cierre bloqueado por condición pendiente | Cuando falta entrega, cobro, comprobante, auditoría o consistencia de estados    |
| Pedido cerrado                           | Cuando el cierre se registra correctamente                                       |
| Estado actualizado por cierre            | Cuando se actualizan estados internos, visibles o financieros                    |
| Intento no autorizado de cierre          | Cuando un usuario sin permisos intenta cerrar un pedido                          |
| Error al cerrar pedido                   | Cuando ocurre una falla técnica durante el cierre                                |

## 11. Observaciones

* Este caso de uso cierra el pedido solo si todas las condiciones críticas están cumplidas.
* Este caso de uso no registra entrega; eso corresponde a `CU-CIE-001-registrar-entrega-del-pedido.md`.
* Este caso de uso no valida condiciones de cierre desde cero como objetivo principal; eso corresponde a `CU-CIE-002-validar-condiciones-de-cierre.md`.
* Este caso de uso no registra cobro final; eso corresponde a `CU-FIN-003-registrar-cobro-final.md`.
* Este caso de uso no registra comprobante; eso corresponde a `CU-FIN-004-registrar-comprobante.md`.
* Este caso de uso no ejecuta impresión.
* Este caso de uso no genera trabajos de impresión.
* La impresión completada no alcanza para cerrar el pedido.
* El cierre debe estar respaldado por auditoría suficiente.
* El cierre debe impedir modificaciones ordinarias posteriores del pedido.
* Si luego se requiere corrección posterior al cierre, deberá existir un flujo administrativo específico.
* La operación debe validarse en backend, RLS, RPC, Edge Function o mecanismo equivalente.

## 12. Poscondición

Al finalizar correctamente:

* el pedido queda cerrado;
* el estado interno queda en estado final;
* el estado visible al cliente queda actualizado si corresponde;
* el estado financiero queda consistente;
* el usuario que cerró el pedido queda registrado;
* el evento de cierre queda auditado;
* el pedido no puede continuar por flujos ordinarios de producción, impresión, cobro o edición;
* la trazabilidad permite explicar por qué el pedido pudo cerrarse.

## 13. Criterios de aceptación

* El administrador o empleado autorizado puede cerrar pedidos si tiene permisos.
* El sistema rechaza cierres de usuarios no autorizados.
* El sistema valida entrega registrada antes de cerrar.
* El sistema valida cobro y estado financiero antes de cerrar.
* El sistema valida comprobante cuando corresponde.
* El sistema valida auditoría y consistencia de estados.
* El sistema bloquea el cierre si falta una condición crítica.
* El sistema actualiza estados finales de forma consistente.
* El cierre queda registrado para trazabilidad.
* El cierre no depende solo de imprimir.
* La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
* El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.

```
```
