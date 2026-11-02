# La Montaña — Android Mobile: alcance y flujos de puntos de retiro y reparto

**Documento de producto v0.1 — 02/11/2026.**  
**Estado:** base documental para evolución Android; **no acredita que exista una app Android implementada**.  
**Rama de referencia:** `pruebas`, commit `4d9ebe0` (cuentas y operación de puntos).  
**Objetivo:** diseñar una experiencia móvil sencilla para los responsables de puntos de retiro (incluidos kioscos sin PC), repartidores y, de forma progresiva, clientes y personal interno. La app debe usar el backend común; no crea una segunda lógica financiera o logística.

## 1. Antecedentes y decisiones ya confirmadas

1. **Una cuenta y un único responsable por punto.** Administración crea y gestiona la cuenta; puede suspenderla, cambiar responsable y habilitar/deshabilitar permiso de cobros. El punto nunca ve pedidos de otro destino ni entra a producción/configuración.
2. **Punto de retiro en web ya operativo en `pruebas`.** La vista `/punto-retiro` dispone de las bandejas **Por llegar**, **Para retirar** y **Entregados**, además de **Finanzas del punto**. Recepción por lote, consulta por cliente/pedido/código, entrega con código, cobros autorizados e historial están implementados en el backend y la web.
3. **El entorno `pruebas` conserva aproximadamente seis meses de actividad simulada.** Incluye puntos Caballito y Palermo y movimientos históricos; la incorporación de cuentas no reescribe los actores de movimientos anteriores.
4. **El código de entrega se genera automáticamente** cuando el pedido sale al destino o, para retiro local sin viaje, al quedar listo. El cliente puede consultarlo; el punto o repartidor lo **ingresa para validarlo**, pero no puede obtener el secreto del servidor.
5. **Entrega a domicilio y ausencia ya tienen reglas de backend/web.** Si el cliente no está, se registra el intento sin entrega; un nuevo intento requiere una acción explícita. El código se conserva. No se registra entrega ni cobro por el solo hecho de marcar llegada.
6. **Trabajo en reparto pendiente de construir para Android.** El usuario aprobó incorporar un actor repartidor que retira pedidos en la imprenta, lleva varios a puntos de retiro y otros a domicilios, y actualiza su progreso con pocos pasos.
7. La oferta comercial, las impresoras y los servicios son globales a la imprenta. La **casa matriz** y los destinos se rigen por la configuración versionada del pedido. Un punto de retiro no necesita PC ni recursos productivos.

**Evidencia técnica vigente:** `docs/cuentas-puntos-retiro.md`, `docs/entorno-pruebas-funcionales.md`, `docs/calidad-y-entrega-simples.md`, `docs/entrega-domicilio-ausencia.md`, `docs/cobro-efectivo-al-entregar.md`; `frontend/src/components/point-desk.tsx`, `frontend/src/components/order-delivery.tsx`; `backend/src/main/java/ar/com/lamontana/puntos/`.

## 2. Perfiles móviles y alcance inicial

| Perfil | Experiencia | Estado |
| --- | --- | --- |
| Responsable de punto de retiro / kiosco | Recibir lotes, buscar pedidos, entregar con código, registrar cobros autorizados, consultar movimientos. | **Reglas y web implementadas**; UI Android pendiente. |
| Repartidor / delivery | Aceptar retiros, llevar pedidos a puntos y domicilios, registrar llegada/incidencia/entrega con pocos pasos. | **Requerimiento aprobado; diseño y backend dedicado pendientes.** |
| Cliente | Consultar pedido/timeline y código; informar pago/gestionar acciones autorizadas. | Web existente; Android cliente histórico como antecedente, requiere adaptación. |
| Personal interno / administrador | Preparar y despachar, asignar repartos, ver incidencias y trazabilidad. | Web parcialmente existente; alcance móvil por definir, sin replicar todo el panel administrativo. |

La aplicación Android podrá tener una entrada por perfil tras autenticar. No conceder permisos por mostrar una pestaña: el backend comprueba **identidad, rol, destino o reparto asignado, estado y versión** en cada operación.

## 3. Módulo móvil «Mostrador del punto»

### Identidad y acceso

- Se usa **la única cuenta existente del punto**, creada por Administración. No ofrecer registro público desde Android.
- Mostrar nombre del punto y responsable. Si el administrador suspende la cuenta o modifica accesos, deben revocarse sesiones anteriores.
- Preservar la posibilidad de bloquear la función de cobros sin bloquear recepción/entrega.
- No mostrar PDFs de clientes, reportes de otras ubicaciones ni datos de producción.

### Pantalla principal

Tres pestañas con cantidades: **Por llegar**, **Para retirar**, **Entregados**. Acceso visible a **Finanzas del punto**, búsqueda y actualización.

### Recibir pedidos trasladados

1. Abrir **Por llegar**.
2. Ver pedidos efectivamente destinados a ese punto y que figuran **En viaje**.
3. Seleccionar pedidos o un grupo recibido físicamente.
4. Pulsar **Confirmar recepción**.
5. El backend valida la totalidad del lote y registra responsable, fecha/hora y llegada. Los pedidos válidos pasan a **Para retirar**.

**Contratos actuales:** hasta 25 pedidos por lote/página, operación atómica e idempotente. Un pedido inválido hace fallar el lote; no simular recepciones parciales silenciosas.

### Entregar al cliente

1. Buscar por número de pedido, nombre, correo o código.
2. Abrir detalle con trabajos, saldo y estado.
3. Ingresar el código mostrado por el cliente.
4. Si el saldo está cubierto, confirmar **Entregar pedido**.
5. Si existe cobro en efectivo autorizado, ofrecer **Cobrar [saldo exacto] y entregar**, en una acción atómica.

Nombre del receptor y observaciones normales deben seguir siendo opcionales. Código erróneo = ninguna entrega ni cobro. No consumir el código al consultar el pedido.

### Finanzas del punto

Conservar lo implementado: pendientes de los pedidos disponibles, efectivo y transferencias efectivamente registrados por la cuenta, paginación y seis meses iniciales; movimientos con importe, medio, pedido, responsable, fecha/hora y referencias.

**Distinción funcional importante:** hoy este resumen **no es una rendición liquidada al propietario**, no calcula comisión ni determina qué efectivo ya se transfirió a la imprenta. La necesidad de «cuánto nos debe el punto, en efectivo o transferencia» fue planteada por el usuario: **agregarla como submódulo futuro pendiente de modelo contable**, no representar los totales brutos actuales como deuda neta.

El futuro módulo de rendiciones deberá definir por separado movimientos recibidos, dinero rendido/transferido, ajustes, saldo a rendir, responsable, conciliación y auditoría; sin inventar comisiones automáticas ni movimientos bancarios.

## 4. Módulo móvil «Reparto» — nuevo actor

### Objetivo UX

El repartidor debe operar con **muy pocos toques**. Mostrar siempre: tarea principal, dirección o punto de destino, cantidad de bultos/pedidos, contacto necesario, estado físico y siguiente acción. Evitar formularios largos y observaciones obligatorias en operaciones normales.

### Acceso y autorización — por desarrollar

Proponer una **identidad de repartidor propia**, gestionada por Administración, distinta de la cuenta única del punto. El acceso debe limitarse a entregas/repartos que tenga asignados y a la información mínima necesaria. La asignación y aceptación de rutas/lotes es un contrato por diseñar; no asumir que la bandeja global de pedidos equivale a un reparto asignado.

### Flujo A: retiro del repartidor en casa matriz

1. Administración prepara uno o varios pedidos, con calidad terminada y destino fijado.
2. Administración los agrupa/asigna a un reparto o recorrido.
3. El repartidor abre **Mis entregas / Por retirar**.
4. Visualiza bultos, destinos, cantidades y pedidos identificados.
5. Confirma **Retiré los pedidos** o **Iniciar reparto** cuando efectivamente los recibe.
6. El servidor registra traspaso de custodia, responsable y fecha/hora; sólo los pedidos preparados/autorizados pueden salir.

**Pendiente:** definir el modelo de asignación de recorrido, bultos y custodia. Reutilizar en lo posible el despacho por lote que ya existe; no crear estados duplicados o hacer que el repartidor despache trabajos todavía no preparados.

### Flujo B: traslado hacia un punto de retiro

1. El repartidor ve el destino, dirección, pedidos que transporta y próxima acción.
2. Al llegar pulsa **Llegué al punto** / **Entregué los bultos al responsable**.
3. El responsable del punto, usando su propio móvil, abre **Por llegar**, comprueba físicamente y pulsa **Confirmar recepción** (individual o por lote).
4. **Sólo la recepción validada por el sistema habilita «Disponible para retirar» al cliente.**
5. El repartidor ve la entrega al punto registrada y continúa la ruta.

**Regla de diseño:** distinguir «repartidor llegó o entregó bultos» de «punto confirmó recepción». Si se reutiliza la transición actual `LLEGAR_PUNTO`, definir exactamente qué actor la registra para no duplicar ni adelantar el estado. No marcar entregado al cliente cuando los bultos llegan al kiosco.

### Flujo C: envío a domicilio

1. El repartidor abre **En reparto → Domicilios**.
2. Ve dirección, contacto mínimo y pedido, con acción principal **Entregar**.
3. Solicita el código que tiene el cliente o quien recibe.
4. Ingresa el código; el backend verifica saldo, autorizaciones, calidad y recorrido.
5. Confirma la entrega física. El código se consume según las reglas existentes.

Si no hay receptor: botón **El cliente no está**; registrar intento con fecha/hora, sin inventar entrega, cobrar, cambiar dirección ni consumir código. **Iniciar nuevo intento** tras coordinación, conservando el mismo código y los demás datos históricos.

Cobro en puerta: **no otorgarlo por defecto al repartidor**. Sólo si el producto y la administración lo habilitan explícitamente y se reutilizan las garantías transaccionales de «cobrar efectivo y entregar». Transferencia informada por cliente no equivale a acreditación.

### Flujo D: incidencias

- Dirección inaccesible / problema de contacto / bulto dañado / no se encontró el paquete: ofrecer reporte simple con categoría y comentario opcional donde sea seguro.
- Una incidencia **no debe marcar exitosamente entrega ni mover el pedido a un estado no aprobado**. Determinar desde backend si se bloquea o se requiere resolución de personal autorizado.
- No inventar un regreso físico a la casa matriz al registrar ausencia.

## 5. Estados y hechos — reutilizar el dominio real

No crear un «estado mobile» paralelo. La app consume los mismos estados y eventos persistidos que web/backend, entre ellos:

- Pedido listo tras producción/calidad.
- `PREPARADO` / `EN_VIAJE` para traslado o envío.
- `DISPONIBLE_PUNTO` al registrarse recepción física válida por el punto/destino.
- `CLIENTE_AUSENTE` para domicilio sin receptor y `REINTENTAR_ENTREGA` para nuevo intento.
- `ENTREGADO` tras validación y entrega física.
- `CERRADO` como cierre financiero-administrativo separado.

La representación visual dependerá del destino (casa matriz, sucursal secundaria, punto, domicilio) y de la **versión del motor asociada al pedido**. No mostrar viaje en retiros locales sin traslado, ni marcar llegada por fechas estimadas.

## 6. Reglas transversales Android

- **Backend compartido actual:** Spring/API y PostgreSQL del proyecto vigente. La documentación Android histórica de `main` (`CU-AND-001/002`) habla de Supabase; usarla como antecedente conceptual, **no copiar su arquitectura desactualizada**.
- Todas las mutaciones deben tener identificador de operación, comprobación de versión e idempotencia. Recuperar una respuesta incierta no debe duplicar cobros, recepciones, entregas o salidas.
- Comunicación cifrada y sesión protegida; no guardar códigos de retiro ni PDF en almacenamiento persistente de Android.
- El código de entrega se **ingresa**, jamás se expone al punto/repartidor mediante API. No enviarlo en URL ni logs.
- Sólo datos del destino o reparto propio; permisos comprobados por backend. Revocar acceso al suspender cuenta o asignación.
- **Conectividad en V1:** exigir confirmación del servidor para cualquier entrega, cobro, recepción o movimiento de estado. Si no hay conexión, mostrar «Sin conexión; no se registró la operación»; no simular éxito ni permitir cobro offline sin conciliación.
- Botones grandes, contraste, estados inequívocos, búsqueda rápida, confirmaciones cortas; navegación en pocos toques y uso en teléfono de gama media.
- No exigir GPS continuo, fotografía, firma o lectura QR para completar la operación básica. Evaluar esas funciones sólo como evoluciones explícitas, con privacidad y permisos.
- La app no es autoridad para recalcular precios, cambiar versiones operativas, autorizar impresión ni declarar acreditaciones bancarias inexistentes.

## 7. Pantallas Android propuestas

| Prioridad | Pantalla | Perfil | Acción principal |
| --- | --- | --- | --- |
| P0 | Inicio y selección de sección según rol | Punto / repartidor | Ver próxima acción |
| P0 | Mostrador: Por llegar | Punto | Recibir lote |
| P0 | Mostrador: Para retirar | Punto | Buscar y entregar con código |
| P0 | Detalle de pedido del punto | Punto | Ver trabajos, saldo y código ingresado |
| P0 | Finanzas del punto | Punto | Consultar movimientos / registrar cobro permitido |
| P0 | Mis repartos | Repartidor | Ver encargos asignados |
| P0 | Retirar y salir | Repartidor | Confirmar toma de bultos y salida |
| P0 | Entrega en punto | Repartidor | Reportar entrega de bultos al punto |
| P0 | Entrega a domicilio | Repartidor | Validar código y entregar / cliente ausente |
| P1 | Histórico e incidencias | Punto / repartidor | Consultar hechos y comunicar excepción |
| P1 | Rendiciones y saldo a entregar a la imprenta | Punto / admin | Conciliar movimientos (requiere contrato) |
| P1 | Cliente: pedido y timeline | Cliente | Consultar y seguir |

Los nombres de secciones son etiquetas de trabajo; se validarán en UX sin cambiar los hechos de negocio.

## 8. Casos de uso Android a incorporar

Los históricos `CU-AND-001` y `CU-AND-002` describen **consulta de pedidos y detalle por cliente** en la rama `main`; requieren revisión de stack y permisos antes de reutilizarse.

Propuesta de nuevos IDs documentales, **reservados aquí pero todavía no creados como archivos individuales**:

- `CU-AND-003`: acceder al mostrador móvil del punto.
- `CU-AND-004`: consultar y recibir lote de pedidos en el punto.
- `CU-AND-005`: buscar pedido y entregar mediante código.
- `CU-AND-006`: registrar cobro permitido y consultar finanzas.
- `CU-AND-007`: consultar los repartos asignados.
- `CU-AND-008`: retirar pedidos e iniciar reparto.
- `CU-AND-009`: entregar bultos en punto y obtener recepción confirmada.
- `CU-AND-010`: entregar a domicilio con código.
- `CU-AND-011`: registrar cliente ausente e iniciar nuevo intento.
- `CU-AND-012`: registrar y consultar incidencias del reparto.
- `CU-AND-013` (fase futura): rendir/cotejar movimientos económicos del punto.

Cada caso deberá detallar precondición, actores, datos, flujo, alternativas, permisos, eventos, postcondición, errores e idempotencia.

## 9. Escenarios para pruebas y futura demo Android

Usar el entorno aislado `pruebas`, con datos históricos de seis meses, puntos Caballito/Palermo y sus cuentas reales de prueba:

1. Acceso a Caballito y aislamiento frente a Palermo.
2. Dos o más pedidos en viaje al mismo punto, recibidos en lote.
3. Código válido para retiro; código incorrecto sin entregar/cobrar.
4. Pedido con saldo pagado; pedido con efectivo permitido y cobro/entrega atómicos.
5. Transferencia informada, **no** acreditada: no equivale a saldo cubierto.
6. Finanzas e historial de seis meses, con quién/fecha/hora y sin atribuir al punto dinero cobrado por caja.
7. Repartidor con varios destinos: retiro de bultos, punto Caballito, punto Palermo y un domicilio.
8. Entrega a domicilio válida y cliente ausente/reintento.
9. Intento de ver/entregar pedido fuera del punto o reparto asignado: rechazo backend.
10. Corte de conexión/reintento de mutación: no duplicar dinero, entrega ni recepción.
11. Cambio de responsable/suspensión de cuenta durante sesión: invalidación de acceso.
12. Registro de llegada del repartidor distinto de la recepción física del punto; no adelantar disponibilidad sin confirmación.

Los puntos 1–6 parten de funcionalidades web/backend existentes; los recorridos dedicados del repartidor y la app nativa aún requieren implementación.

## 10. Contratos y decisiones pendientes antes de codificar reparto

**A desarrollar y validar:**

- Identidad, permisos, alta/baja y asignación de repartidores.
- Qué objeto representa recorrido, paradas, bultos y traspaso de custodia.
- Quién puede asignar/reasignar un reparto y si puede incluir varios puntos y domicilios.
- Acción exacta de salida desde casa matriz y relación con el despacho por lote existente.
- Protocolo de confirmación de llegada por repartidor y recepción por punto, sin doble transición.
- Cobro a domicilio por repartidor y su posible liquidación: **no habilitado por omisión**.
- Rendiciones del punto y definición de «monto que debe a la imprenta»: separar efectivo recibido, pagos digitales, transferencias rendidas, ajustes y conciliación, sin asumir comisiones.
- Estrategia de notificaciones móviles, prioridad y permisos; no prometer ubicación GPS en tiempo real.
- Política operativa para repartos sin conexión, paquetes perdidos y devoluciones físicas.

No convertir estos temas pendientes en comportamiento «implementado» ni inventar estados del backend. Su diseño debe conservar historia, auditoría, autorizaciones, compatibilidad con pedidos existentes y las reglas de casa matriz.

## 11. Orden sugerido de documentación e implementación futura

1. Convertir `CU-AND-003` a `CU-AND-006` en casos de uso y maquetas mobile del Mostrador, reutilizando el contrato real de `/punto-retiro`.
2. Definir dominio «Reparto» y permisos con diagrama de estados, eventos y trazabilidad; crear `CU-AND-007` a `011`.
3. Especificar pantallas mobile del repartidor y su interacción con el mostrador del punto y el seguimiento del cliente.
4. Diseñar rendiciones/conciliación como módulo financiero propio sin confundirlo con totales brutos.
5. Actualizar requisitos, historias de usuario, matriz de trazabilidad y pruebas reales de API.
6. Probar en `pruebas` escenarios de seis meses y teléfonos Android; mantener separados los resultados simulados y las integraciones reales.

**Criterio rector:** móvil simplifica la interacción humana; no duplica la lógica de pedidos, cobros, seguridad, estados ni auditoría del backend.

## 12. Primer incremento en la rama `android`

La rama `android` inicia la aplicación nativa con Jetpack Compose, sin registro ni administración de identidades. Incluye login contra el backend, lectura del perfil, enrutamiento seguro y la base responsive del mostrador del punto. Una cuenta marcada como punto abre **Por llegar / Para retirar / Entregados / Finanzas**; un empleado común no se convierte automáticamente en repartidor mientras no exista el contrato de asignación de recorridos.

La primera referencia visual está en [`mockups/android-v0-login-punto.svg`](mockups/android-v0-login-punto.svg). Las tarjetas de pedidos que aparecen en el `@Preview` son únicamente demostrativas; no se presentan como datos recibidos del backend. El contexto y los contadores del punto sí utilizan el contrato real. Las listas, recepción, búsqueda, entrega/cobro y finanzas se conectarán de forma incremental sin crear reglas paralelas.

