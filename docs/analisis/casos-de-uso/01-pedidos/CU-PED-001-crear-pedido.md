# CU-PED-001 - Crear pedido

| Campo | Valor |
|---|---|
| ID | CU-PED-001 |
| Caso de uso | Crear pedido |
| Área | Pedidos |
| Actor principal | Cliente |
| Actores secundarios | Sistema |
| Prioridad | P0 Crítica |
| Alcance | MVP |
| RF relacionados | RF-PED-001, RF-PED-002, RF-PED-003, RF-ARC-001, RF-EST-001, RF-EST-002 |
| RNF relacionados | RNF-SEG-003, RNF-SEG-004, RNF-RLS-002, RNF-AUD-001, RNF-REN-004 |
| HU relacionadas | HU-CLI-002, HU-CLI-003, HU-SIS-001 |
| Reglas críticas relacionadas | RFC-001, RFC-002, RFC-003, RFC-007, RNFC-001, RNFC-002, RNFC-003 |

## 1. Caso de Uso

Crear pedido.

## 2. Actores

| Actor | Participación |
|---|---|
| Cliente | Inicia la creación del pedido, completa los datos requeridos y confirma la solicitud |
| Sistema | Valida datos, crea el pedido, asigna estados iniciales, asocia el pedido al cliente autenticado y registra el evento correspondiente |

## 3. Descripción

Este caso de uso describe el flujo mediante el cual un cliente crea un nuevo pedido de impresión en La Montaña.

El pedido creado por el cliente no pasa automáticamente a producción. Al finalizar el flujo, el pedido queda registrado en el sistema, asociado al cliente autenticado y en estado pendiente de revisión administrativa.

La carga de archivos puede formar parte del flujo inicial de solicitud, pero el detalle específico de carga, validación y uso de archivos se documenta en casos de uso separados del dominio `02-archivos`.

## 4. Precondición

- El cliente está autenticado.
- El cliente tiene permisos para crear pedidos.
- El sistema tiene disponible el backend Supabase.
- El cliente accede desde Web o Android a una pantalla o flujo de creación de pedido.
- El sistema puede asociar el pedido al usuario autenticado.
- El sistema tiene definidos los estados iniciales necesarios para el pedido.

## 5. Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| Tipo de trabajo | Sí | Tipo general del pedido solicitado, por ejemplo impresión, fotocopia, anillado u otro servicio disponible |
| Descripción del pedido | Sí | Descripción breve de lo que el cliente solicita |
| Cantidad estimada | Sí | Cantidad estimada de unidades, copias, carillas o páginas según corresponda |
| Observaciones del cliente | No | Comentarios adicionales del cliente sobre el pedido |
| Archivo asociado | No en este caso de uso | El archivo puede cargarse en el flujo inicial, pero se detalla en `CU-ARC-001` |
| Punto de entrega preferido | No | Punto de entrega deseado si el flujo lo solicita en esta etapa |
| Medio de contacto complementario | No | Información complementaria si el negocio la requiere |

## 6. Datos de salida

| Dato | Descripción |
|---|---|
| ID del pedido | Identificador único del pedido creado |
| Cliente asociado | Usuario cliente autenticado que creó el pedido |
| Estado interno inicial | Estado operativo interno asignado al pedido luego de crearse |
| Estado visible inicial | Estado que verá el cliente luego de crear el pedido |
| Estado financiero inicial | Estado financiero inicial del pedido |
| Fecha de creación | Momento en que el pedido fue registrado |
| Evento de auditoría | Registro del evento de creación del pedido |

## 7. Permisos y seguridad

| Aspecto | Regla |
|---|---|
| Autenticación | Requiere cliente autenticado |
| Autorización | Solo usuarios con rol o permiso de cliente pueden crear pedidos como cliente |
| RLS / acceso a datos | El pedido debe quedar asociado al cliente autenticado y solo debe ser visible para ese cliente y usuarios internos autorizados |
| Archivos | Si se adjuntan archivos durante el flujo, deben almacenarse mediante mecanismo autorizado y asociarse al pedido correspondiente |
| Validación backend | La creación no debe depender únicamente de validaciones del frontend |
| Producción | El pedido no puede pasar automáticamente a producción al ser creado |

## 8. Flujo principal

| Paso | Flujo principal | Flujo alternativo / excepciones |
|---|---|---|
| 1 | El cliente ingresa al sistema desde Web o Android. | Si el cliente no está autenticado, el sistema solicita iniciar sesión. |
| 2 | El cliente accede a la opción de crear un nuevo pedido. | Si el usuario no tiene permisos para crear pedidos, el sistema rechaza la operación. |
| 3 | El sistema muestra el formulario de creación de pedido. | Si el formulario no puede cargarse por error técnico, el sistema informa la falla. |
| 4 | El cliente completa los datos mínimos del pedido. | Si faltan datos obligatorios, el sistema indica qué campos deben completarse. |
| 5 | El cliente confirma la creación del pedido. | Si el cliente cancela la operación, no se crea ningún pedido. |
| 6 | El sistema valida los datos recibidos. | Si los datos son inválidos o inconsistentes, el sistema rechaza la creación e informa el motivo. |
| 7 | El sistema crea el pedido asociado al cliente autenticado. | Si no puede asociarse el pedido al cliente, la operación se cancela para evitar pedidos huérfanos. |
| 8 | El sistema asigna estado interno inicial pendiente de revisión. | Si no existe un estado inicial válido, la operación se detiene para evitar estados inconsistentes. |
| 9 | El sistema asigna estado visible inicial para el cliente. | Si no existe estado visible inicial, el pedido no debe quedar en un estado ambiguo para el cliente. |
| 10 | El sistema asigna estado financiero inicial según corresponda. | Si la información financiera mínima no puede determinarse, el pedido queda pendiente de revisión financiera o equivalente. |
| 11 | El sistema registra un evento de auditoría por creación del pedido. | Si la auditoría falla, debe evaluarse si la operación se revierte o si se registra una alerta técnica. |
| 12 | El sistema informa al cliente que el pedido fue creado correctamente y queda pendiente de revisión. | Si ocurre un error técnico luego de confirmar, el sistema debe evitar duplicar pedidos ante reintentos. |

## 9. Impacto en estados

| Estado | Impacto |
|---|---|
| Estado interno | Se asigna un estado inicial equivalente a pendiente de revisión |
| Estado visible al cliente | Se asigna un estado visible equivalente a solicitud recibida o pendiente de revisión |
| Estado financiero | Se asigna un estado inicial pendiente de evaluación, pendiente de seña o sin definir según reglas posteriores |
| Estado técnico de impresión | Sin cambios. No se crea trabajo de impresión en este caso de uso |

## 10. Eventos de auditoría

| Evento | Cuándo se registra |
|---|---|
| Pedido creado | Cuando el sistema crea correctamente el pedido |
| Pedido asociado a cliente | Cuando el pedido queda vinculado al usuario autenticado |
| Estado inicial asignado | Cuando se asignan los estados iniciales del pedido |
| Error de creación de pedido | Cuando la creación falla por validación, permisos o problema técnico |

## 11. Observaciones

- Este caso de uso no autoriza producción.
- Este caso de uso no genera un trabajo de impresión.
- Todo pedido creado por un cliente debe quedar pendiente de revisión administrativa.
- La seguridad debe validarse en backend, base de datos, RPC o políticas correspondientes, no solo ocultando opciones en el frontend.
- El acceso al pedido debe respetar RLS y permisos.
- La carga de archivos se detalla en `CU-ARC-001-cargar-archivo-al-pedido.md`.
- La revisión administrativa se detalla en `CU-REV-001-revisar-pedido-nuevo.md`.
- La regla de seña del 30% para pedidos de más de 200 carillas se detalla en `CU-FIN-001-detectar-pedido-con-senia-obligatoria.md`.
- La creación del pedido debe evitar duplicaciones ante reintentos del cliente o problemas de conectividad.

## 12. Poscondición

Al finalizar correctamente:

- el pedido queda creado;
- el pedido queda asociado al cliente autenticado;
- el pedido queda pendiente de revisión administrativa;
- el pedido tiene estado visible inicial para el cliente;
- el pedido tiene estado financiero inicial;
- el pedido no queda autorizado para producción;
- el pedido no genera automáticamente un trabajo de impresión;
- el evento de creación queda registrado para trazabilidad.

## 13. Criterios de aceptación

- El cliente autenticado puede crear un pedido con los datos mínimos requeridos.
- El sistema rechaza la creación si faltan datos obligatorios.
- El sistema asocia el pedido al cliente autenticado.
- El pedido creado queda inicialmente pendiente de revisión.
- El pedido no pasa automáticamente a producción.
- El cliente puede identificar que su solicitud fue recibida.
- El sistema no crea trabajos de impresión durante este flujo.
- La operación respeta permisos y políticas de acceso.
- El evento de creación queda registrado.
- El resultado del caso de uso es coherente con los requerimientos funcionales, no funcionales e historias relacionadas.
