# CU-FIN-002 - Registrar seña

| Campo                        | Valor                                                                                                                  |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| ID                           | CU-FIN-002                                                                                                             |
| Caso de uso                  | Registrar seña                                                                                                         |
| Área                         | Estados y finanzas                                                                                                     |
| Actor principal              | Empleado o administrador                                                                                               |
| Actores secundarios          | Sistema, Cliente                                                                                                       |
| Prioridad                    | P0 Crítica                                                                                                             |
| Alcance                      | Producto base                                                                                                          |
| RF relacionados              | RF-FIN-001, RF-FIN-002, RF-FIN-003, RF-FIN-004, RF-FIN-005, RF-EST-003, RF-EST-004, RF-EST-005, RF-AUD-001, RF-AUD-002 |
| RNF relacionados             | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-004, RNF-AUD-001, RNF-AUD-003, RNF-AUD-004, RNF-REN-004                 |
| HU relacionadas              | HU-CLI-006, HU-ADM-005, HU-SIS-003                                                                                     |
| Reglas críticas relacionadas | RFC-005, RFC-006, RNFC-001, RNFC-003, RNFC-007, RNFC-008                                                               |

## 1. Caso de Uso

Registrar seña.

## 2. Actores

| Actor         | Participación                                                                                         |
| ------------- | ----------------------------------------------------------------------------------------------------- |
| Empleado      | Registra una seña recibida para un pedido cuando tiene permisos internos                              |
| Administrador | Registra, valida o supervisa la seña con permisos ampliados                                           |
| Sistema       | Valida permisos, pedido, condición financiera, monto, comprobante si corresponde y registra auditoría |
| Cliente       | Puede visualizar información financiera visible asociada a la seña, si corresponde                    |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador registra una seña asociada a un pedido.

La regla de negocio definida indica que si un pedido supera 200 carillas, requiere una seña del 30%.

Registrar una seña implica dejar constancia de que el cliente realizó un pago parcial requerido para continuar el flujo operativo del pedido. Esta operación impacta en el estado financiero del pedido, pero no debe modificar por sí sola el estado interno ni autorizar automáticamente producción si faltan otras condiciones.

La seña puede estar asociada a un comprobante o referencia de pago cuando corresponda. La gestión detallada de comprobantes puede complementarse con casos específicos de comprobantes.

Este caso de uso no genera trabajos de impresión, no ejecuta impresión, no cierra el pedido y no reemplaza la validación administrativa del pedido.

## 4. Precondición

* El empleado o administrador está autenticado.
* El usuario tiene permisos internos para registrar pagos o señas.
* El pedido existe.
* El pedido pertenece al flujo operativo de la imprenta.
* El pedido no fue cerrado.
* El pedido no fue cancelado.
* El pedido tiene estado financiero definido.
* El sistema puede determinar si el pedido requiere seña.
* El backend Supabase está disponible.
* Las políticas de acceso protegen la información financiera del pedido.
* El sistema puede registrar auditoría de la operación.

## 5. Datos de entrada

| Dato                        | Obligatorio | Descripción                                                                  |
| --------------------------- | ----------- | ---------------------------------------------------------------------------- |
| ID del pedido               | Sí          | Identificador del pedido al que se registra la seña                          |
| Usuario interno autenticado | Sí          | Empleado o administrador que registra la seña                                |
| Monto de seña               | Sí          | Monto abonado como seña                                                      |
| Porcentaje de seña          | No          | Porcentaje aplicado, por ejemplo 30% cuando corresponde                      |
| Medio de pago               | No          | Medio por el cual se recibió la seña                                         |
| Fecha de pago               | Sí          | Fecha en que se recibió o registró la seña                                   |
| Referencia de comprobante   | No          | Número, imagen, archivo o referencia del comprobante si corresponde          |
| Observación interna         | No          | Comentario interno asociado al registro de seña                              |
| Canal de acceso             | No          | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato                           | Descripción                                                              |
| ------------------------------ | ------------------------------------------------------------------------ |
| Seña registrada                | Registro financiero asociado al pedido                                   |
| Estado financiero actualizado  | Estado financiero actualizado, por ejemplo seña registrada o equivalente |
| Pedido asociado                | Pedido al que corresponde la seña                                        |
| Usuario que registra           | Usuario interno que registró la operación                                |
| Fecha de registro              | Momento en que se guardó la seña                                         |
| Referencia de comprobante      | Comprobante o referencia asociada si corresponde                         |
| Información visible al cliente | Mensaje o estado financiero visible si corresponde                       |
| Evento de auditoría            | Registro de trazabilidad asociado a la operación                         |

## 7. Permisos y seguridad

| Aspecto              | Regla                                                                                     |
| -------------------- | ----------------------------------------------------------------------------------------- |
| Autenticación        | Requiere empleado o administrador autenticado                                             |
| Autorización         | Solo usuarios internos con permisos financieros pueden registrar señas                    |
| RLS / acceso a datos | El pedido y su información financiera deben estar protegidos por políticas de acceso      |
| Cliente final        | El cliente no puede registrar manualmente la seña por este flujo interno                  |
| Estado financiero    | Solo puede actualizarse mediante operación autorizada                                     |
| Comprobante          | Si existe comprobante, debe almacenarse o referenciarse mediante mecanismo autorizado     |
| Validación backend   | La operación no debe depender únicamente del frontend                                     |
| Auditoría            | El registro de seña debe quedar auditado con usuario, fecha, monto y contexto             |
| Integridad           | El sistema debe evitar duplicados, montos inválidos o estados financieros contradictorios |

## 8. Flujo principal

| Paso | Flujo principal                                                              | Flujo alternativo / excepciones                                                                                         |
| ---- | ---------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| 1    | El empleado o administrador ingresa al sistema.                              | Si no está autenticado, el sistema solicita iniciar sesión.                                                             |
| 2    | El usuario accede al pedido desde una vista interna.                         | Si no tiene permisos internos, el sistema rechaza el acceso.                                                            |
| 3    | El usuario selecciona la opción de registrar seña.                           | Si no tiene permisos financieros, el sistema bloquea la acción.                                                         |
| 4    | El sistema valida que el pedido existe y está disponible.                    | Si el pedido no existe, fue cancelado o cerrado, rechaza la operación.                                                  |
| 5    | El sistema valida la condición financiera del pedido.                        | Si el pedido no requiere seña, puede permitir registro manual autorizado o advertir que no corresponde según reglas.    |
| 6    | El usuario ingresa monto, fecha, medio de pago y comprobante si corresponde. | Si falta un dato obligatorio, el sistema solicita completarlo.                                                          |
| 7    | El sistema valida el monto registrado.                                       | Si el monto es inválido, negativo, cero o incompatible con la regla definida, rechaza la operación.                     |
| 8    | El sistema valida la relación entre seña requerida y monto registrado.       | Si el monto no alcanza la seña requerida, registra pago parcial o mantiene seña pendiente según reglas financieras.     |
| 9    | El sistema registra la seña asociada al pedido.                              | Si no puede registrar la seña, no debe modificar el estado financiero.                                                  |
| 10   | El sistema asocia comprobante o referencia si corresponde.                   | Si falla la asociación del comprobante, debe informar el error o dejar la operación pendiente de revisión según reglas. |
| 11   | El sistema actualiza el estado financiero del pedido.                        | Si no puede actualizarlo consistentemente, revierte o bloquea la operación.                                             |
| 12   | El sistema registra evento de auditoría.                                     | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente.                                            |
| 13   | El sistema informa que la seña fue registrada correctamente.                 | Si ocurre un error final, informa la falla y evita duplicar registros ante reintentos.                                  |

## 9. Impacto en estados

| Estado                      | Impacto                                                                                       |
| --------------------------- | --------------------------------------------------------------------------------------------- |
| Estado interno              | Sin cambios directo, salvo que el flujo defina una transición posterior condicionada por seña |
| Estado visible al cliente   | Puede actualizarse para informar que la seña fue registrada o que resta saldo pendiente       |
| Estado financiero           | Cambia a seña registrada, seña parcial, seña confirmada o equivalente según reglas definidas  |
| Estado técnico de impresión | Sin cambios. No se genera ni modifica ningún trabajo de impresión                             |
| Estado de cierre            | No puede cerrarse el pedido solo por registrar la seña                                        |

## 10. Eventos de auditoría

| Evento                                  | Cuándo se registra                                                                  |
| --------------------------------------- | ----------------------------------------------------------------------------------- |
| Seña registrada                         | Cuando un usuario autorizado registra una seña                                      |
| Seña parcial registrada                 | Cuando el monto registrado no cubre la seña requerida completa                      |
| Comprobante asociado a seña             | Cuando se asocia comprobante o referencia de pago                                   |
| Registro de seña rechazado              | Cuando el sistema rechaza la operación por datos inválidos o permisos insuficientes |
| Intento no autorizado de registrar seña | Cuando un usuario sin permisos intenta registrar una seña                           |
| Estado financiero actualizado           | Cuando cambia el estado financiero por registro de seña                             |
| Error al registrar seña                 | Cuando ocurre una falla técnica durante el registro                                 |

## 11. Observaciones

* Este caso de uso no detecta por sí solo si el pedido requiere seña; esa evaluación se documenta en `CU-FIN-001-detectar-pedido-con-senia-obligatoria.md`.
* Este caso de uso no registra el cobro final del pedido.
* Este caso de uso no cierra el pedido.
* Este caso de uso no genera trabajos de impresión.
* Este caso de uso no ejecuta impresión.
* Registrar seña puede ser condición necesaria para continuar hacia producción efectiva, pero no debe ser la única condición.
* Si el pedido supera 200 carillas, debe respetarse la regla de seña del 30%.
* La información financiera debe mantenerse separada del estado interno y del estado visible.
* El cliente solo debe visualizar información financiera autorizada y comprensible.
* La operación debe validarse en backend, RLS, RPC, Edge Function o mecanismo equivalente.
* El sistema debe evitar duplicidad de registros ante reintentos o errores de conectividad.
* Si se adjunta comprobante como archivo, debe respetar las reglas de Storage y acceso autorizado.

## 12. Poscondición

Al finalizar correctamente:

* la seña queda registrada;
* la seña queda asociada al pedido correcto;
* el usuario que registró la operación queda identificado;
* el estado financiero queda actualizado de forma consistente;
* el comprobante o referencia queda asociado si corresponde;
* el evento queda auditado;
* no se genera ningún trabajo de impresión;
* no se ejecuta impresión;
* el pedido no queda cerrado por esta operación;
* el cliente puede visualizar información financiera permitida si corresponde.

## 13. Criterios de aceptación

* El empleado o administrador autenticado puede registrar señas si tiene permisos financieros.
* El sistema rechaza registros de usuarios no autorizados.
* El sistema valida que el pedido existe y está disponible.
* El sistema valida monto, fecha y datos obligatorios.
* El sistema permite asociar comprobante o referencia cuando corresponda.
* El estado financiero se actualiza de forma consistente.
* La operación queda auditada.
* Registrar seña no genera trabajos de impresión.
* Registrar seña no cierra el pedido.
* El cliente solo accede a información financiera visible y autorizada.
* La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
* El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.
