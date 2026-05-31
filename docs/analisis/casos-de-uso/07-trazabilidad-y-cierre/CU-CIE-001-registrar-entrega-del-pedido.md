# CU-CIE-001 - Registrar entrega del pedido

| Campo                        | Valor                                                                                                      |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------- |
| ID                           | CU-CIE-001                                                                                                 |
| Caso de uso                  | Registrar entrega del pedido                                                                               |
| Área                         | Trazabilidad y cierre                                                                                      |
| Actor principal              | Empleado o administrador                                                                                   |
| Actores secundarios          | Sistema, Cliente                                                                                           |
| Prioridad                    | P1 Alta                                                                                                    |
| Alcance                      | Producto base                                                                                              |
| RF relacionados              | RF-WEB-008, RF-AUD-001, RF-AUD-002, RF-AUD-004, RF-AUD-005, RF-EST-001, RF-EST-002, RF-EST-004, RF-FIN-006 |
| RNF relacionados             | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-004, RNF-AUD-001, RNF-AUD-003, RNF-AUD-004, RNF-USA-003     |
| HU relacionadas              | HU-EMP-006, HU-SIS-004                                                                                     |
| Reglas críticas relacionadas | RFC-006, RNFC-001, RNFC-003, RNFC-007, RNFC-008                                                            |

## 1. Caso de Uso

Registrar entrega del pedido.

## 2. Actores

| Actor         | Participación                                                                                                       |
| ------------- | ------------------------------------------------------------------------------------------------------------------- |
| Empleado      | Registra que el pedido fue entregado al cliente o dejado en el punto de entrega correspondiente                     |
| Administrador | Registra, corrige o supervisa entregas con permisos ampliados                                                       |
| Sistema       | Valida permisos, pedido, estado operativo, registra entrega, actualiza estados si corresponde y audita la operación |
| Cliente       | Puede visualizar información visible sobre la entrega si el flujo lo contempla                                      |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador registra la entrega de un pedido.

Registrar la entrega permite dejar constancia de que el trabajo físico fue entregado al cliente o dejado en el punto de entrega acordado.

La entrega es una condición importante del cierre del pedido, pero no debe cerrar el pedido por sí sola. El cierre requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.

Este caso de uso no registra cobro final, no genera comprobante, no cierra el pedido y no modifica información financiera de forma directa.

## 4. Precondición

* El empleado o administrador está autenticado.
* El usuario tiene permisos internos para registrar entregas.
* El pedido existe.
* El pedido pertenece al flujo operativo de la imprenta.
* El pedido no fue cerrado.
* El pedido no fue cancelado.
* El pedido se encuentra en una etapa compatible con entrega.
* El trabajo físico está listo para entregar o fue entregado efectivamente.
* El backend Supabase está disponible.
* El sistema puede registrar auditoría de la entrega.
* Las políticas de acceso impiden registrar entregas sobre pedidos no autorizados.

## 5. Datos de entrada

| Dato                        | Obligatorio | Descripción                                                                  |
| --------------------------- | ----------- | ---------------------------------------------------------------------------- |
| ID del pedido               | Sí          | Identificador del pedido entregado                                           |
| Usuario interno autenticado | Sí          | Empleado o administrador que registra la entrega                             |
| Fecha de entrega            | Sí          | Fecha en que se realizó o registró la entrega                                |
| Punto de entrega            | No          | Lugar donde se entregó el pedido, si corresponde                             |
| Persona que recibe          | No          | Nombre o referencia de quien recibió el pedido, si se registra               |
| Observación interna         | No          | Comentario interno asociado a la entrega                                     |
| Confirmación de entrega     | Sí          | Confirmación explícita de que el pedido fue entregado                        |
| Canal de acceso             | No          | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato                                  | Descripción                                    |
| ------------------------------------- | ---------------------------------------------- |
| Entrega registrada                    | Registro de entrega asociado al pedido         |
| Pedido asociado                       | Pedido sobre el que se registró la entrega     |
| Usuario que registra                  | Usuario interno que registró la entrega        |
| Fecha de registro                     | Momento en que se guardó la entrega            |
| Estado interno actualizado            | Estado interno actualizado si corresponde      |
| Estado visible al cliente actualizado | Estado visible actualizado si corresponde      |
| Evento de auditoría                   | Registro de trazabilidad asociado a la entrega |

## 7. Permisos y seguridad

| Aspecto              | Regla                                                                                                                         |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| Autenticación        | Requiere empleado o administrador autenticado                                                                                 |
| Autorización         | Solo usuarios internos con permisos pueden registrar entregas                                                                 |
| RLS / acceso a datos | El pedido y la entrega deben protegerse mediante políticas de acceso                                                          |
| Cliente final        | El cliente no puede registrar entrega por este flujo interno                                                                  |
| Estado del pedido    | Solo pedidos en etapa compatible pueden marcarse como entregados                                                              |
| Información interna  | Observaciones internas no deben exponerse al cliente                                                                          |
| Validación backend   | La entrega no debe depender únicamente del frontend                                                                           |
| Auditoría            | La entrega debe quedar registrada con usuario, fecha y contexto                                                               |
| Integridad           | Registrar entrega no debe cerrar automáticamente el pedido si faltan cobro, comprobante, auditoría o estado final consistente |

## 8. Flujo principal

| Paso | Flujo principal                                                                              | Flujo alternativo / excepciones                                                            |
| ---- | -------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| 1    | El empleado o administrador ingresa al sistema.                                              | Si no está autenticado, el sistema solicita iniciar sesión.                                |
| 2    | El usuario accede al pedido desde una vista interna.                                         | Si no tiene permisos internos, el sistema rechaza el acceso.                               |
| 3    | El usuario selecciona la opción de registrar entrega.                                        | Si no tiene permisos para registrar entregas, el sistema bloquea la acción.                |
| 4    | El sistema valida que el pedido existe y está disponible.                                    | Si el pedido no existe, fue cancelado o fue cerrado, rechaza la operación.                 |
| 5    | El sistema valida que el pedido esté en una etapa compatible con entrega.                    | Si el pedido no está listo para entrega o no corresponde entregarlo, bloquea la operación. |
| 6    | El usuario ingresa fecha, punto de entrega, persona que recibe y observación si corresponde. | Si falta un dato obligatorio, el sistema solicita completarlo.                             |
| 7    | El usuario confirma la entrega.                                                              | Si cancela, no se registra entrega.                                                        |
| 8    | El sistema registra la entrega asociada al pedido.                                           | Si no puede registrar la entrega, no debe actualizar estados.                              |
| 9    | El sistema actualiza estado interno o visible si existe regla definida.                      | Si no puede actualizar estados consistentemente, revierte o bloquea la operación.          |
| 10   | El sistema registra evento de auditoría.                                                     | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente.               |
| 11   | El sistema informa que la entrega fue registrada correctamente.                              | Si ocurre un error final, informa la falla y evita duplicar registros ante reintentos.     |

## 9. Impacto en estados

| Estado                      | Impacto                                                                             |
| --------------------------- | ----------------------------------------------------------------------------------- |
| Estado interno              | Puede cambiar a entregado, entrega registrada o equivalente según modelo de estados |
| Estado visible al cliente   | Puede cambiar a entregado o equivalente si se define comunicación visible           |
| Estado financiero           | Sin cambios directo                                                                 |
| Estado técnico de impresión | Sin cambios                                                                         |
| Estado de cierre            | No se cierra automáticamente; la entrega es solo una condición del cierre           |

## 10. Eventos de auditoría

| Evento                                     | Cuándo se registra                                       |
| ------------------------------------------ | -------------------------------------------------------- |
| Entrega registrada                         | Cuando un usuario autorizado registra la entrega         |
| Intento no autorizado de registrar entrega | Cuando un usuario sin permisos intenta registrar entrega |
| Entrega bloqueada por estado inválido      | Cuando el pedido no está en etapa compatible con entrega |
| Estado actualizado por entrega             | Cuando la entrega actualiza estado interno o visible     |
| Error al registrar entrega                 | Cuando ocurre una falla técnica durante el registro      |

## 11. Observaciones

* Este caso de uso no registra cobro final.
* Este caso de uso no registra comprobante.
* Este caso de uso no cierra el pedido.
* Este caso de uso no genera trabajos de impresión.
* Este caso de uso no ejecuta impresión.
* Registrar entrega puede ser condición necesaria para el cierre, pero no es la única condición.
* El cierre requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.
* La información visible al cliente debe estar separada de las observaciones internas.
* Si la entrega se realiza en puntos acordados, el punto de entrega debe registrarse si el flujo lo requiere.
* La operación debe validarse en backend, RLS, RPC, Edge Function o mecanismo equivalente.
* El sistema debe evitar duplicar entregas ante reintentos o errores de conectividad.

## 12. Poscondición

Al finalizar correctamente:

* la entrega queda registrada;
* la entrega queda asociada al pedido correcto;
* el usuario que registró la entrega queda identificado;
* el estado interno o visible se actualiza si corresponde;
* el evento queda auditado;
* no se modifica información financiera de forma directa;
* no se genera ningún trabajo de impresión;
* no se ejecuta impresión;
* el pedido no queda cerrado por esta operación;
* el pedido queda disponible para validación posterior de cierre.

## 13. Criterios de aceptación

* El empleado o administrador autenticado puede registrar entregas si tiene permisos.
* El sistema rechaza registros de usuarios no autorizados.
* El sistema valida que el pedido existe y está disponible.
* El sistema valida que el pedido esté en etapa compatible con entrega.
* El sistema registra fecha y contexto de entrega.
* La entrega queda asociada al pedido correcto.
* La operación queda auditada.
* Registrar entrega no registra cobro final.
* Registrar entrega no registra comprobante.
* Registrar entrega no cierra el pedido.
* El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.

```
```
