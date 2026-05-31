# CU-AUD-002 - Consultar historial del pedido

| Campo | Valor |
|---|---|
| ID | CU-AUD-002 |
| Caso de uso | Consultar historial del pedido |
| Área | Trazabilidad y cierre |
| Actor principal | Administrador |
| Actores secundarios | Empleado autorizado, Sistema |
| Prioridad | P1 Alta |
| Alcance | Producto base |
| RF relacionados | RF-AUD-001, RF-AUD-002, RF-AUD-003, RF-AUD-004, RF-AUD-005, RF-PED-008, RF-PED-005 |
| RNF relacionados | RNF-AUD-001, RNF-AUD-002, RNF-AUD-003, RNF-AUD-004, RNF-AUD-005, RNF-SEG-003, RNF-SEG-005, RNF-RLS-005, RNF-USA-002 |
| HU relacionadas | HU-ADM-006, HU-SIS-006 |
| Reglas críticas relacionadas | RFC-004, RFC-006, RNFC-001, RNFC-003, RNFC-005, RNFC-007, RNFC-008 |

## 1. Caso de Uso

Consultar historial del pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Administrador | Consulta el historial completo o ampliado del pedido para auditoría y control |
| Empleado autorizado | Consulta eventos del historial según permisos internos |
| Sistema | Valida permisos, pedido, visibilidad del historial y protege eventos sensibles |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un administrador o empleado autorizado consulta el historial de un pedido.

El historial del pedido permite reconstruir el recorrido del pedido desde su creación hasta su estado actual. Puede incluir eventos como creación, carga de archivos, revisión administrativa, solicitud de corrección, aprobación, rechazo, cambios de estado, validaciones financieras, registro de seña, cobros, comprobantes, impresión, entrega, cierre y errores técnicos.

La consulta del historial es fundamental para trazabilidad, auditoría, soporte operativo y control de consistencia.

El cliente final no debe acceder al historial interno completo. Si se necesita mostrar información al cliente, debe hacerse mediante estados visibles o comunicaciones específicas, no mediante exposición directa de auditoría interna.

Este caso de uso es de solo lectura. No modifica pedidos, estados, archivos, pagos, comprobantes ni trabajos de impresión.

## 4. Precondición

- El administrador o empleado autorizado está autenticado.
- El usuario tiene permisos para consultar historial del pedido.
- El pedido existe.
- El backend Supabase está disponible.
- Existen eventos registrados o el sistema puede devolver historial vacío.
- Las políticas de acceso protegen eventos de auditoría e historial.
- El sistema puede filtrar eventos según rol y permisos del usuario.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| ID del pedido | Sí | Identificador del pedido cuyo historial se consulta |
| Usuario interno autenticado | Sí | Administrador o empleado autorizado que consulta el historial |
| Filtro de tipo de evento | No | Permite filtrar por estado, archivo, finanzas, impresión, cierre, error u otro tipo |
| Rango de fechas | No | Permite limitar el historial por fecha de evento |
| Nivel de detalle | No | Vista resumida, completa o técnica según permisos |
| Canal de acceso | No | Web interna o Android si la funcionalidad se habilita para usuarios internos |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| Historial del pedido | Lista de eventos asociados al pedido |
| Evento de historial | Evento individual con tipo, fecha, origen y descripción |
| Usuario o componente origen | Usuario, sistema o agente que generó el evento |
| Estado anterior | Estado previo si el evento corresponde a transición |
| Estado nuevo | Estado resultante si el evento corresponde a transición |
| Metadata visible | Información adicional permitida según rol y permisos |
| Resultado de consulta | Lista de eventos, lista vacía o error controlado |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere administrador o empleado autorizado autenticado |
| Autorización | Solo usuarios internos con permisos pueden consultar historial |
| RLS / acceso a datos | Los eventos de historial deben protegerse mediante políticas de acceso |
| Cliente final | No puede consultar historial interno completo por este flujo |
| Información sensible | Debe ocultarse o filtrarse si el usuario no tiene permisos suficientes |
| Auditoría técnica | Eventos técnicos sensibles solo deben mostrarse a roles autorizados |
| Datos financieros | Eventos financieros solo deben mostrarse si el usuario tiene permisos |
| Validación backend | La visibilidad del historial no debe depender únicamente del frontend |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El administrador o empleado autorizado ingresa al sistema. | Si no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El usuario accede a un pedido desde una vista interna. | Si no tiene permisos internos, el sistema rechaza el acceso. |
| 3 | El usuario selecciona la opción de consultar historial. | Si no tiene permisos para historial, el sistema bloquea la acción. |
| 4 | El sistema valida que el pedido existe. | Si el pedido no existe, informa la situación. |
| 5 | El sistema valida permisos del usuario sobre el pedido y sus eventos. | Si no tiene permisos, rechaza la consulta. |
| 6 | El usuario aplica filtros si corresponde. | Si los filtros son inválidos, el sistema informa el error. |
| 7 | El sistema recupera eventos asociados al pedido. | Si falla la consulta, informa error controlado sin exponer datos internos. |
| 8 | El sistema filtra eventos según permisos del usuario. | Si algún evento no debe ser visible, no se incluye en la respuesta. |
| 9 | El sistema muestra el historial permitido. | Si no existen eventos visibles, muestra una lista vacía o mensaje correspondiente. |
| 10 | El usuario revisa la trazabilidad del pedido. | Si intenta acceder a datos no permitidos, backend/RLS debe rechazar la operación. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Sin cambios |
| Estado visible al cliente | Sin cambios |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios |
| Estado de cierre | Sin cambios. La consulta solo permite verificar trazabilidad |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Historial consultado | Cuando un usuario autorizado consulta el historial de un pedido |
| Filtro de historial aplicado | Cuando se consulta el historial con filtros relevantes |
| Intento no autorizado de consultar historial | Cuando un usuario sin permisos intenta consultar eventos |
| Evento oculto por permisos | Puede registrarse si se decide auditar filtrado de eventos sensibles |
| Error de consulta de historial | Cuando ocurre una falla técnica durante la consulta |

## 11. Observaciones

- Este caso de uso es de solo lectura.
- Este caso de uso no modifica el pedido.
- Este caso de uso no cambia estados.
- Este caso de uso no registra cobros ni comprobantes.
- Este caso de uso no genera trabajos de impresión.
- Este caso de uso no cierra pedidos.
- El historial interno no debe exponerse al cliente final.
- La consulta del historial ayuda a validar consistencia del flujo.
- El cierre del pedido requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.
- La auditoría debe protegerse contra acceso indebido.
- Los eventos deben filtrarse según permisos.
- La consulta puede ser base para soporte operativo, revisión administrativa y control interno.

## 12. Poscondición

Al finalizar correctamente:

- el usuario autorizado visualiza el historial permitido del pedido;
- los eventos sensibles quedan protegidos según permisos;
- el pedido no cambia de estado;
- no se modifican datos financieros;
- no se generan trabajos de impresión;
- no se ejecuta ningún cambio operativo;
- la trazabilidad del pedido queda disponible para análisis interno.

## 13. Criterios de aceptación

- El administrador o empleado autorizado puede consultar historial si tiene permisos.
- El sistema rechaza consultas de usuarios no autorizados.
- El sistema valida que el pedido existe.
- El sistema filtra eventos según rol y permisos.
- El cliente final no accede al historial interno completo.
- La consulta no modifica datos ni estados.
- La consulta permite reconstruir el recorrido del pedido.
- La operación se valida mediante backend, RLS, RPC, Edge Function o mecanismo equivalente.
- El resultado es coherente con los RF, RNF, HU y reglas críticas relacionadas.