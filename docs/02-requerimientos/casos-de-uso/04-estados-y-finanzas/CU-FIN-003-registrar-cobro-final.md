# CU-FIN-003 - Registrar cobro final

| Campo | Valor |
|---|---|
| ID | CU-FIN-003 |
| Caso de uso | Registrar cobro final |
| Área | Estados y finanzas |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema, Cliente |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-FIN-003, RF-FIN-004, RF-FIN-005, RF-FIN-006, RF-EST-003, RF-EST-004, RF-EST-005, RF-AUD-001, RF-AUD-002, RF-AUD-004 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-004, RNF-AUD-001, RNF-AUD-003, RNF-AUD-004, RNF-REN-004 |
| HU relacionadas | HU-ADM-005, HU-SIS-004 |
| Reglas críticas relacionadas | RFC-006, RNFC-001, RNFC-003, RNFC-007, RNFC-008 |

## 1. Caso de Uso

Registrar cobro final.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Registra el cobro final de un pedido si tiene permisos financieros |
| Administrador | Registra, valida o supervisa el cobro final con permisos ampliados |
| Sistema | Valida permisos, pedido, estado financiero, monto, comprobante si corresponde y registra auditoría |
| Cliente | Puede visualizar información financiera visible asociada al cobro, si corresponde |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador registra el cobro final de un pedido.

El cobro final representa el pago pendiente necesario para completar la situación financiera del pedido. Esta operación impacta en el estado financiero, pero no debe cerrar el pedido por sí sola si todavía faltan condiciones operativas, entrega, comprobante, auditoría o estado final consistente.

El cierre del pedido no depende solo de cobrar. Debe existir consistencia entre entrega, cobro, comprobante, auditoría y estado final.

Este caso de uso no genera trabajos de impresión, no ejecuta impresión y no cierra automáticamente el pedido.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos internos para registrar cobros.
- El pedido existe.
- El pedido pertenece al flujo operativo de la imprenta.
- El pedido no fue cerrado.
- El pedido no fue cancelado.
- El pedido tiene estado financiero definido.
- El sistema puede determinar si existe saldo pendiente.
- El backend Supabase está disponible.
- Las políticas de acceso protegen la información financiera del pedido.
- El sistema puede registrar auditoría de la operación.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido al que se registra el cobro final |
| Usuario interno autenticado | Sí | Empleado o administrador que registra el cobro |
| Monto cobrado | Sí | Monto abonado como cobro final o saldo restante |
| Medio de pago | No | Medio por el cual se recibió el pago |
| Fecha de cobro | Sí | Fecha en que se recibió o registró el cobro |
| Referencia de comprobante | No | Número, imagen, archivo o referencia del comprobante si corresponde |
| Observación interna | No | Comentario interno asociado al cobro |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Cobro final registrado | Registro financiero asociado al pedido |
| Estado financiero actualizado | Estado financiero actualizado, por ejemplo pagado, saldo cancelado o equivalente |
| Pedido asociado | Pedido al que corresponde el cobro final |
| Usuario que registra | Usuario interno que registró la operación |
| Fecha de registro | Momento en que se guardó el cobro |
| Referencia de comprobante | Comprobante o referencia asociada si corresponde |
| Información visible al cliente | Mensaje o estado financiero visible si corresponde |
| Evento de auditoría | Registro de trazabilidad asociado a la operación |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos financieros pueden registrar cobros finales |
| RLS / acceso a datos | El pedido y su información financiera deben estar protegidos por políticas de acceso |
| Cliente final | El cliente no puede registrar manualmente el cobro final por este flujo interno |
| Estado financiero | Solo puede actualizarse mediante operación autorizada |
| Comprobante | Si existe comprobante, debe almacenarse o referenciarse mediante mecanismo autorizado |
| Validación backend | La operación no debe depender únicamente del frontend |
| Auditoría | El cobro final debe quedar auditado con usuario, fecha, monto y contexto |
| Integridad | El sistema debe evitar duplicados, montos inválidos o estados financieros contradictorios |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede al pedido desde una vista interna. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona la opción de registrar cobro final. | Si no tiene permisos financieros, el sistema bloquea la acción. |
| 4 | El sistema valida que el pedido existe y está disponible. | Si el pedido no existe, fue cancelado o cerrado, rechaza la operación. |
| 5 | El sistema valida la situación financiera del pedido. | Si no existe saldo pendiente, advierte o bloquea según reglas financieras definidas. |
| 6 | El usuario ingresa monto, fecha, medio de pago y comprobante si corresponde. | Si falta un dato obligatorio, el sistema solicita completarlo. |
| 7 | El sistema valida el monto registrado. | Si el monto es inválido, negativo, cero o incompatible con el saldo pendiente, rechaza la operación. |
| 8 | El sistema registra el cobro final asociado al pedido. | Si no puede registrar el cobro, no debe modificar el estado financiero. |
| 9 | El sistema asocia comprobante o referencia si corresponde. | Si falla la asociación del comprobante, debe informar el error o dejar la operación pendiente de revisión según reglas. |
| 10 | El sistema actualiza el estado financiero del pedido. | Si no puede actualizarlo consistentemente, revierte o bloquea la operación. |
| 11 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente. |
| 12 | El sistema informa que el cobro final fue registrado correctamente. | Si ocurre un error final, informa la falla y evita duplicar registros ante reintentos. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios directo, salvo que el flujo defina una transición posterior condicionada por cobro |
| Estado visible al cliente | Puede actualizarse para informar que el cobro fue registrado o que no queda saldo pendiente |
| Estado financiero | Cambia a pagado, saldo cancelado, cobro final registrado o equivalente según reglas definidas |
| Estado técnico de impresión | Sin cambios. No se genera ni modifica ningún trabajo de impresión |
| Estado de cierre | No debe cerrarse el pedido solo por registrar el cobro final |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Cobro final registrado | Cuando un usuario autorizado registra el cobro final |
| Comprobante asociado a cobro | Cuando se asocia comprobante o referencia de pago |
| Registro de cobro rechazado | Cuando el sistema rechaza la operación por datos inválidos o permisos insuficientes |
| Intento no autorizado de registrar cobro | Cuando un usuario sin permisos intenta registrar un cobro |
| Estado financiero actualizado | Cuando cambia el estado financiero por registro de cobro |
| Error al registrar cobro | Cuando ocurre una falla técnica durante el registro |

## 11. Observaciones

- Este caso de uso no registra seña inicial; eso corresponde a `CU-FIN-002-registrar-senia.md`.
- Este caso de uso no genera comprobante final por sí solo si el comprobante requiere un flujo específico.
- Este caso de uso no cierra el pedido.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no ejecuta impresión.
- Registrar cobro final puede ser condición necesaria para cerrar el pedido, pero no debe ser la única condición.
- El cierre del pedido requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.
- La información financiera debe mantenerse separada del estado interno y del estado visible.
- El cliente solo debe visualizar información financiera autorizada y comprensible.
- La operación debe validarse en backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El sistema debe evitar duplicidad de registros ante reintentos o errores de conectividad.
- Si se adjunta comprobante como archivo, debe respetar las reglas de Storage y acceso autorizado.

## 12. Poscondición

Al finalizar correctamente:

- el cobro final queda registrado;
- el cobro queda asociado al pedido correcto;
- el usuario que registró la operación queda identificado;
- el estado financiero queda actualizado de forma consistente;
- el comprobante o referencia queda asociado si corresponde;
- el evento queda auditado;
- no se genera ningún trabajo de impresión;
- no se ejecuta impresión;
- el pedido no queda cerrado por esta operación;
- el cliente puede visualizar información financiera permitida si corresponde.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede registrar cobros finales si tiene permisos financieros.
- El sistema rechaza registros de usuarios no autorizados.
- El sistema valida que el pedido existe y está disponible.
- El sistema valida monto, fecha y datos obligatorios.
- El sistema permite asociar comprobante o referencia cuando corresponda.
- El estado financiero se actualiza de forma consistente.
- La operación queda auditada.
- Registrar cobro final no genera trabajos de impresión.
- Registrar cobro final no cierra el pedido.
- El cliente solo accede a información financiera visible y autorizada.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.