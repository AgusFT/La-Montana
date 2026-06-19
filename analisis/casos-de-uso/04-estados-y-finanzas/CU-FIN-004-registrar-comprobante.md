# CU-FIN-004 - Registrar comprobante

| Campo | Valor |
|---|---|
| ID | CU-FIN-004 |
| Caso de uso | Registrar comprobante |
| Área | Estados y finanzas |
| Actor principal | Empleado o administrador |
| Actores secundarios | Sistema, Cliente |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-FIN-003, RF-FIN-005, RF-FIN-006, RF-ARC-002, RF-ARC-003, RF-ARC-004, RF-AUD-001, RF-AUD-002, RF-AUD-004 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-AUT-002, RNF-RLS-004, RNF-RLS-006, RNF-ARC-001, RNF-ARC-002, RNF-ARC-005, RNF-AUD-001, RNF-AUD-003, RNF-AUD-004 |
| HU relacionadas | HU-ADM-005, HU-SIS-004 |
| Reglas críticas relacionadas | RFC-006, RFC-007, RNFC-001, RNFC-003, RNFC-004, RNFC-007, RNFC-008 |

## 1. Caso de Uso

Registrar comprobante.

## 2. Actores

| Actor | Participación |
|---|---|
| Empleado | Registra o asocia un comprobante vinculado a una operación financiera del pedido |
| Administrador | Registra, valida o supervisa comprobantes con permisos ampliados |
| Sistema | Valida permisos, pedido, relación con operación financiera, archivo o referencia del comprobante y registra auditoría |
| Cliente | Puede visualizar información visible del comprobante si corresponde |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un empleado o administrador registra un comprobante asociado a un pedido.

El comprobante puede corresponder a una seña, cobro final, pago parcial u otra operación financiera definida por el flujo del negocio.

Registrar un comprobante no debe cerrar automáticamente el pedido. El cierre requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.

El comprobante puede registrarse como archivo, referencia externa, número, imagen, documento o metadata según la implementación definida. Si se almacena como archivo, debe utilizar un mecanismo autorizado de almacenamiento y acceso, como Supabase Storage o equivalente.

Este caso de uso no registra por sí solo una seña ni un cobro final. Es un flujo complementario para asociar evidencia documental a una operación financiera existente o en proceso.

## 4. Precondición

- El empleado o administrador está autenticado.
- El usuario tiene permisos internos para registrar comprobantes.
- El pedido existe.
- El pedido pertenece al flujo operativo de la imprenta.
- El pedido no fue cerrado, salvo que exista un flujo específico de corrección documental posterior.
- Existe una operación financiera relacionada o una necesidad de asociar comprobante.
- El backend Supabase está disponible.
- Supabase Storage o el mecanismo de almacenamiento definido está disponible si el comprobante se carga como archivo.
- Las políticas de acceso protegen la información financiera y documental del pedido.
- El sistema puede registrar auditoría de la operación.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido al que se registra el comprobante |
| Usuario interno autenticado | Sí | Empleado o administrador que registra el comprobante |
| Tipo de comprobante | Sí | Seña, cobro final, pago parcial u otro tipo definido |
| Archivo de comprobante | No | Archivo, imagen o documento del comprobante |
| Referencia de comprobante | No | Número, enlace, código, descripción o referencia externa |
| Operación financiera asociada | No | Seña, cobro final u otra operación a la que se vincula el comprobante |
| Fecha del comprobante | No | Fecha del comprobante o del pago asociado |
| Observación interna | No | Comentario interno asociado al comprobante |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Comprobante registrado | Registro documental asociado al pedido |
| Pedido asociado | Pedido al que pertenece el comprobante |
| Operación financiera asociada | Seña, cobro final, pago parcial u operación relacionada |
| Archivo o referencia del comprobante | Archivo almacenado o referencia registrada |
| Usuario que registra | Usuario interno que registró el comprobante |
| Fecha de registro | Momento en que se registró el comprobante |
| Información visible al cliente | Información documental visible si corresponde |
| Evento de auditoría | Registro de trazabilidad asociado al comprobante |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere empleado o administrador autenticado |
| Autorización | Solo usuarios internos con permisos financieros o administrativos pueden registrar comprobantes |
| RLS / acceso a datos | El pedido, comprobante, archivos y datos financieros deben protegerse mediante políticas de acceso |
| Storage | Si el comprobante es archivo, debe almacenarse mediante mecanismo autorizado |
| Cliente final | El cliente no puede registrar comprobantes por este flujo interno |
| Visibilidad cliente | El cliente solo puede ver información del comprobante autorizada y no interna |
| Validación backend | La operación no debe depender únicamente del frontend |
| Auditoría | El registro del comprobante debe quedar auditado con usuario, fecha y contexto |
| Integridad | El comprobante debe asociarse a un pedido existente y, si corresponde, a una operación financiera válida |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El empleado o administrador ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede al pedido desde una vista interna. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona la opción de registrar comprobante. | Si no tiene permisos financieros o administrativos, el sistema bloquea la acción. |
| 4 | El sistema valida que el pedido existe y está disponible. | Si el pedido no existe o no está disponible, rechaza la operación. |
| 5 | El sistema valida si existe operación financiera relacionada. | Si no existe, puede permitir registrar comprobante pendiente de asociación o bloquear según reglas definidas. |
| 6 | El usuario ingresa tipo de comprobante, referencia y/o archivo. | Si faltan datos mínimos, el sistema solicita completarlos. |
| 7 | Si se adjunta archivo, el sistema valida tipo, tamaño y condiciones permitidas. | Si el archivo no cumple restricciones, rechaza la carga. |
| 8 | El sistema almacena el archivo del comprobante si corresponde. | Si falla Storage o almacenamiento, no debe registrar una referencia inválida. |
| 9 | El sistema registra la metadata del comprobante. | Si no puede registrar metadata, debe evitar inconsistencias. |
| 10 | El sistema asocia el comprobante al pedido y a la operación financiera si corresponde. | Si la asociación falla, revierte o deja la operación pendiente de revisión según reglas. |
| 11 | El sistema actualiza información documental o financiera relacionada, si corresponde. | Si la actualización genera inconsistencia, bloquea o revierte la operación. |
| 12 | El sistema registra evento de auditoría. | Si la auditoría falla, debe registrarse alerta técnica o evento equivalente. |
| 13 | El sistema informa que el comprobante fue registrado correctamente. | Si ocurre un error final, informa la falla y evita duplicar registros ante reintentos. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios directo, salvo que el flujo defina una transición posterior condicionada por comprobante |
| Estado visible al cliente | Puede actualizarse para indicar que existe comprobante registrado, si corresponde |
| Estado financiero | Puede mantenerse o actualizarse si el comprobante completa una condición financiera pendiente |
| Estado técnico de impresión | Sin cambios. No se genera ni modifica ningún trabajo de impresión |
| Estado de cierre | No debe cerrarse el pedido solo por registrar un comprobante |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Comprobante registrado | Cuando un usuario autorizado registra un comprobante |
| Archivo de comprobante cargado | Cuando se carga un archivo asociado al comprobante |
| Comprobante asociado a operación financiera | Cuando se vincula el comprobante con seña, cobro final u otra operación |
| Registro de comprobante rechazado | Cuando el sistema rechaza la operación por datos inválidos o permisos insuficientes |
| Intento no autorizado de registrar comprobante | Cuando un usuario sin permisos intenta registrar un comprobante |
| Error al registrar comprobante | Cuando ocurre una falla técnica durante el registro |

## 11. Observaciones

- Este caso de uso no registra el pago de una seña por sí solo.
- Este caso de uso no registra el cobro final por sí solo.
- Este caso de uso no cierra el pedido.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no ejecuta impresión.
- El comprobante puede ser condición necesaria para cierre, pero no es la única condición.
- El cierre del pedido requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.
- Si el comprobante se carga como archivo, debe respetar las reglas de Storage y acceso autorizado.
- El cliente solo debe visualizar información documental autorizada y comprensible.
- Las observaciones internas no deben exponerse al cliente.
- La operación debe validarse en backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El sistema debe evitar duplicidad de comprobantes ante reintentos o errores de conectividad.

## 12. Poscondición

Al finalizar correctamente:

- el comprobante queda registrado;
- el comprobante queda asociado al pedido correcto;
- el comprobante queda asociado a una operación financiera si corresponde;
- el usuario que registró la operación queda identificado;
- el archivo o referencia queda almacenado o registrado mediante mecanismo autorizado;
- el evento queda auditado;
- no se genera ningún trabajo de impresión;
- no se ejecuta impresión;
- el pedido no queda cerrado por esta operación;
- el cliente puede visualizar información documental permitida si corresponde.

## 13. Criterios de aceptación

- El empleado o administrador autenticado puede registrar comprobantes si tiene permisos.
- El sistema rechaza registros de usuarios no autorizados.
- El sistema valida que el pedido existe y está disponible.
- El sistema valida datos mínimos del comprobante.
- Si existe archivo, el sistema valida tipo, tamaño y condiciones permitidas.
- El comprobante queda asociado al pedido correcto.
- El comprobante puede asociarse a una operación financiera si corresponde.
- La operación queda auditada.
- Registrar comprobante no genera trabajos de impresión.
- Registrar comprobante no cierra el pedido.
- El cliente solo accede a información documental visible y autorizada.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.