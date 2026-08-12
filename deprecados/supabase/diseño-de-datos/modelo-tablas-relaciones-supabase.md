# Modelo de tablas y relaciones Supabase

| Campo | Valor |
|---|---|
| Version | 1.0 |
| Estado | Inicial |
| Fecha | 2026-06-22 |
| Responsable | Agustin Tejero |

## 1. Objetivo

Este documento define el modelo inicial de tablas y relaciones de la base de
datos Supabase/PostgreSQL del MVP de La Montana.

El documento sirve como entrada directa para:

- #95 - migraciones iniciales del esquema MVP;
- #98 - politicas RLS iniciales;
- #121 - DER del modelo de datos;
- #122 - flujo de construccion y poblado de la BDD.

## 2. Criterio de diseno

El modelo respeta `guia-estilo-bdd.md`.

Criterios principales:

- nombres en espanol;
- nombres en singular;
- `snake_case`;
- claves primarias `id_<nombre_de_tabla>`;
- claves foraneas `id_<tabla_referenciada>`;
- tablas persistentes con auditoria base;
- normalizacion tendiente a 3NF;
- Supabase Auth como proveedor de identidad;
- `usuario` como tabla propia de negocio;
- Storage privado para archivos;
- RLS como defensa obligatoria.

## 3. Tablas del modelo MVP

| Tabla | Proposito |
|---|---|
| `usuario` | Representa al usuario de negocio vinculado a Supabase Auth. |
| `rol` | Define roles funcionales como cliente, empleado y administrador. |
| `permiso` | Define permisos atomicos o funcionales del sistema. |
| `rol_permiso` | Relaciona roles con permisos. |
| `contacto` | Guarda medios de contacto asociados a usuarios. |
| `direccion` | Guarda direcciones asociadas a usuarios. |
| `punto_entrega` | Define puntos de entrega o retiro disponibles. |
| `servicio` | Catalogo de servicios ofrecidos por la imprenta. |
| `pedido` | Representa la solicitud principal del cliente. |
| `pedido_servicio` | Relaciona pedidos con servicios y congela precio aplicado. |
| `archivo` | Guarda metadata segura de archivos asociados a pedidos. |
| `pago` | Registra pagos, senas, cobros o comprobantes financieros. |
| `pago_pedido` | Relaciona pagos con pedidos y montos aplicados. |
| `auditoria` | Registra eventos criticos, errores y trazabilidad. |

## 4. Relaciones generales

| Relacion | Cardinalidad | Descripcion |
|---|---:|---|
| `auth.users` -> `usuario` | 1 - 0..1 | Un usuario autenticado puede tener un usuario de negocio asociado. |
| `rol` -> `usuario` | 1 - N | Cada usuario tiene un rol principal en el MVP. |
| `rol` -> `rol_permiso` -> `permiso` | N - N | Un rol agrupa permisos y un permiso puede pertenecer a varios roles. |
| `usuario` -> `contacto` | 1 - N | Un usuario puede tener varios contactos. |
| `usuario` -> `direccion` | 1 - N | Un usuario puede tener varias direcciones. |
| `usuario` -> `pedido` | 1 - N | Un cliente puede crear varios pedidos. |
| `punto_entrega` -> `pedido` | 1 - N | Un pedido puede seleccionar un punto de entrega o retiro. |
| `pedido` -> `archivo` | 1 - N | Un pedido puede tener varios archivos. |
| `pedido` -> `pedido_servicio` -> `servicio` | N - N | Un pedido puede incluir varios servicios. |
| `pedido` -> `pago_pedido` -> `pago` | N - N | Un pago puede aplicarse a uno o mas pedidos. |
| `usuario` -> `auditoria` | 1 - N | Un usuario puede actuar como origen de eventos auditados. |
| `pedido` -> `auditoria` | 1 - N | Un pedido puede tener eventos de trazabilidad asociados. |

## 5. Auditoria base

Todas las tablas persistentes del modelo, salvo `auditoria`, deben contemplar
los siguientes campos:

```text
id_usuario_creacion
id_usuario_modificacion
id_usuario_eliminacion
fecha_creacion
fecha_modificacion
fecha_eliminacion
eliminado
version_fila
```

Criterios:

- `fecha_creacion` se completa al insertar.
- `fecha_modificacion` se actualiza al modificar.
- `fecha_eliminacion` se completa en borrado logico.
- `eliminado` indica borrado logico.
- `version_fila` permite control simple de cambios relevantes.
- Los campos `id_usuario_*` referencian `usuario(id_usuario)` cuando hay actor.
- En seeds, migraciones o procesos de sistema se permite nulo si queda
  justificado.

La tabla `auditoria` no debe tener borrado logico operativo ni actualizaciones
libres. Sus registros deben ser append-only en la practica.

## 6. Tabla `usuario`

Representa al usuario de negocio. No reemplaza a Supabase Auth; lo complementa.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_usuario` | bigint identity | PK. |
| `id_usuario_auth` | uuid | FK a `auth.users(id)`. |
| `id_rol` | bigint | FK a `rol(id_rol)`. |
| `nombre` | text | Nombre visible o legal del usuario. |
| `apellido` | text | Apellido del usuario cuando corresponda. |
| `email` | text | Email principal normalizado. |
| `estado` | text/enum | Estado operativo del usuario. |
| `observacion` | text | Observacion interna opcional. |

PK:

```text
usuario.id_usuario
```

FK:

```text
usuario.id_usuario_auth -> auth.users(id)
usuario.id_rol -> rol.id_rol
```

Restricciones sugeridas:

- `usuario.id_usuario_auth` unico.
- `usuario.email` unico cuando se use como identificador de negocio.
- `usuario.estado` con valores controlados.

RLS/ownership:

- un cliente puede leer su propio `usuario`;
- un cliente no puede cambiar `id_rol`;
- empleados y administradores acceden segun permisos;
- operaciones sensibles deben pasar por backend/RPC/Edge Function.

## 7. Tabla `rol`

Define roles funcionales.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_rol` | bigint identity | PK. |
| `codigo` | text | Codigo estable del rol. |
| `nombre` | text | Nombre visible del rol. |
| `descripcion` | text | Descripcion funcional. |
| `activo` | boolean | Indica si el rol puede usarse. |

PK:

```text
rol.id_rol
```

Valores iniciales sugeridos:

```text
cliente
empleado
administrador
```

RLS:

- lectura controlada para determinar permisos;
- escritura solo por administradores o procesos autorizados.

## 8. Tabla `permiso`

Define permisos funcionales o atomicos.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_permiso` | bigint identity | PK. |
| `codigo` | text | Codigo estable del permiso. |
| `nombre` | text | Nombre visible del permiso. |
| `descripcion` | text | Descripcion funcional. |
| `activo` | boolean | Indica si el permiso puede usarse. |

PK:

```text
permiso.id_permiso
```

Restricciones sugeridas:

- `permiso.codigo` unico.

RLS:

- lectura controlada;
- escritura solo por administradores o migraciones.

## 9. Tabla `rol_permiso`

Relaciona roles con permisos.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_rol_permiso` | bigint identity | PK. |
| `id_rol` | bigint | FK a `rol`. |
| `id_permiso` | bigint | FK a `permiso`. |

PK:

```text
rol_permiso.id_rol_permiso
```

FK:

```text
rol_permiso.id_rol -> rol.id_rol
rol_permiso.id_permiso -> permiso.id_permiso
```

Restricciones sugeridas:

- combinacion unica `id_rol`, `id_permiso`.

Cardinalidad:

```text
rol N - N permiso mediante rol_permiso
```

RLS:

- cliente no modifica esta tabla;
- empleados no modifican esta tabla salvo permiso explicito;
- administradores gestionan segun politica definida.

## 10. Tabla `contacto`

Guarda medios de contacto asociados a un usuario.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_contacto` | bigint identity | PK. |
| `id_usuario` | bigint | FK a `usuario`. |
| `tipo_contacto` | text/enum | Email, telefono, whatsapp u otro. |
| `valor` | text | Valor de contacto. |
| `principal` | boolean | Indica contacto principal. |
| `verificado` | boolean | Indica si fue verificado. |

PK:

```text
contacto.id_contacto
```

FK:

```text
contacto.id_usuario -> usuario.id_usuario
```

Cardinalidad:

```text
usuario 1 - N contacto
```

RLS:

- cliente gestiona sus contactos permitidos;
- administradores pueden consultar/gestionar segun permisos;
- no se expone contacto de otros clientes.

## 11. Tabla `direccion`

Guarda direcciones asociadas a usuarios.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_direccion` | bigint identity | PK. |
| `id_usuario` | bigint | FK a `usuario`. |
| `calle` | text | Calle o descripcion principal. |
| `numero` | text | Numero o altura. |
| `piso` | text | Piso opcional. |
| `departamento` | text | Departamento opcional. |
| `localidad` | text | Localidad. |
| `provincia` | text | Provincia. |
| `codigo_postal` | text | Codigo postal. |
| `referencia` | text | Referencia opcional. |
| `principal` | boolean | Indica direccion principal. |

PK:

```text
direccion.id_direccion
```

FK:

```text
direccion.id_usuario -> usuario.id_usuario
```

Cardinalidad:

```text
usuario 1 - N direccion
```

RLS:

- cliente solo ve y gestiona sus direcciones permitidas;
- usuarios internos acceden segun permisos.

## 12. Tabla `punto_entrega`

Define puntos de retiro o entrega disponibles para pedidos.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_punto_entrega` | bigint identity | PK. |
| `nombre` | text | Nombre del punto. |
| `descripcion` | text | Descripcion operativa. |
| `direccion_texto` | text | Direccion visible del punto. |
| `horario_atencion` | text | Horario informado al cliente. |
| `activo` | boolean | Indica si puede seleccionarse. |

PK:

```text
punto_entrega.id_punto_entrega
```

Cardinalidad:

```text
punto_entrega 1 - N pedido
```

RLS:

- lectura permitida para clientes si el punto esta activo;
- escritura solo por administradores o procesos autorizados.

## 13. Tabla `servicio`

Catalogo de servicios ofrecidos por la imprenta.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_servicio` | bigint identity | PK. |
| `codigo` | text | Codigo estable del servicio. |
| `nombre` | text | Nombre visible. |
| `descripcion` | text | Descripcion funcional. |
| `precio_base` | numeric | Precio base vigente. |
| `tipo_moneda` | text/enum | Moneda: ARS o USD inicialmente. |
| `activo` | boolean | Indica si puede venderse. |

PK:

```text
servicio.id_servicio
```

Restricciones sugeridas:

- `servicio.codigo` unico.
- `precio_base >= 0`.

RLS:

- clientes pueden leer servicios activos si el flujo lo requiere;
- usuarios internos gestionan catalogo segun permisos.

## 14. Tabla `pedido`

Representa la solicitud principal del cliente.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_pedido` | bigint identity | PK. |
| `id_usuario` | bigint | FK al cliente propietario. |
| `id_punto_entrega` | bigint | FK opcional a `punto_entrega`. |
| `codigo` | text | Codigo operativo visible o interno. |
| `descripcion` | text | Descripcion breve del pedido. |
| `observacion_cliente` | text | Observaciones ingresadas por cliente. |
| `observacion_interna` | text | Observaciones solo internas. |
| `cantidad_estimada` | integer | Cantidad inicial estimada. |
| `cantidad_carillas` | integer | Carillas si aplica regla de sena. |
| `estado_interno` | text/enum | Estado operativo interno. |
| `estado_visible_cliente` | text/enum | Estado visible para cliente. |
| `estado_financiero` | text/enum | Estado financiero. |
| `requiere_senia` | boolean | Indica si aplica sena obligatoria. |
| `porcentaje_senia` | numeric | Porcentaje aplicado si corresponde. |
| `total_estimado` | numeric | Total estimado o cotizado. |

PK:

```text
pedido.id_pedido
```

FK:

```text
pedido.id_usuario -> usuario.id_usuario
pedido.id_punto_entrega -> punto_entrega.id_punto_entrega
```

Cardinalidad:

```text
usuario 1 - N pedido
punto_entrega 1 - N pedido
```

Estados iniciales sugeridos:

```text
estado_interno: pendiente_revision
estado_visible_cliente: pendiente_revision
estado_financiero: pendiente_evaluacion
```

Criterio:

- ningun pedido creado por cliente pasa automaticamente a produccion;
- la separacion de estados evita exponer informacion interna al cliente;
- el estado financiero se mantiene separado del estado operativo.

RLS:

- cliente solo ve sus pedidos y campos visibles;
- estados internos/financieros se protegen segun rol;
- empleado/admin acceden segun permisos;
- cambios criticos deben pasar por backend/RPC/Edge Function.

## 15. Tabla `pedido_servicio`

Relaciona pedidos con servicios y conserva el precio aplicado.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_pedido_servicio` | bigint identity | PK. |
| `id_pedido` | bigint | FK a `pedido`. |
| `id_servicio` | bigint | FK a `servicio`. |
| `cantidad` | numeric | Cantidad del servicio. |
| `precio_unitario_aplicado` | numeric | Precio usado en el pedido. |
| `subtotal` | numeric | Subtotal aplicado. |
| `observacion` | text | Observacion opcional. |

PK:

```text
pedido_servicio.id_pedido_servicio
```

FK:

```text
pedido_servicio.id_pedido -> pedido.id_pedido
pedido_servicio.id_servicio -> servicio.id_servicio
```

Cardinalidad:

```text
pedido N - N servicio mediante pedido_servicio
```

Criterio:

- `precio_unitario_aplicado` se guarda para preservar trazabilidad historica;
- no debe depender de recalcular contra el precio actual de `servicio`.

RLS:

- cliente puede ver servicios asociados a sus pedidos;
- modificaciones dependen del estado del pedido y permisos.

## 16. Tabla `archivo`

Guarda metadata segura de archivos asociados a pedidos.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_archivo` | bigint identity | PK. |
| `id_pedido` | bigint | FK a `pedido`. |
| `id_usuario` | bigint | FK al usuario que cargo el archivo. |
| `nombre_original` | text | Nombre original informado. |
| `mime_original` | text | MIME original informado. |
| `tamano_bytes` | bigint | Tamano original. |
| `hash_archivo` | text | Hash/checksum del ciphertext o del archivo segun decision. |
| `bucket` | text | Bucket privado de Storage. |
| `ruta_storage` | text | Ruta interna en Storage. |
| `clave_envuelta` | text | Clave AES envuelta, nunca clave plana. |
| `iv` | text | IV usado para cifrado. |
| `version_cifrado` | text | Version de estrategia de cifrado. |
| `estado_archivo` | text/enum | Estado operativo del archivo. |

PK:

```text
archivo.id_archivo
```

FK:

```text
archivo.id_pedido -> pedido.id_pedido
archivo.id_usuario -> usuario.id_usuario
```

Cardinalidad:

```text
pedido 1 - N archivo
usuario 1 - N archivo
```

Criterios de Storage:

- Storage debe ser privado;
- no se usan public URLs;
- Storage guarda ciphertext;
- la base guarda metadata segura;
- no se guarda archivo original;
- no se guarda clave AES plana;
- no se guardan secretos en auditoria.

RLS:

- cliente solo ve archivos asociados a sus pedidos;
- usuario no puede leer archivos de otros clientes;
- acceso al binario debe pasar por mecanismo autorizado;
- empleados/admin acceden segun permisos y estado del pedido.

## 17. Tabla `pago`

Registra pagos, senas o cobros.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_pago` | bigint identity | PK. |
| `id_usuario_registro` | bigint | Usuario que registro el pago. |
| `monto` | numeric | Monto total del pago. |
| `tipo_moneda` | text/enum | Moneda. |
| `medio_pago` | text/enum | Medio de pago. |
| `estado` | text/enum | Estado del pago. |
| `fecha_pago` | timestamptz | Fecha informada del pago. |
| `referencia` | text | Referencia externa o comprobante. |
| `observacion` | text | Observacion interna. |

PK:

```text
pago.id_pago
```

FK:

```text
pago.id_usuario_registro -> usuario.id_usuario
```

Estados iniciales sugeridos:

```text
pendiente
confirmado
rechazado
anulado
```

RLS:

- cliente no registra pagos internos;
- cliente solo ve informacion financiera visible;
- empleado/admin gestionan segun permisos financieros.

## 18. Tabla `pago_pedido`

Relaciona pagos con pedidos y permite aplicar montos parciales.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_pago_pedido` | bigint identity | PK. |
| `id_pago` | bigint | FK a `pago`. |
| `id_pedido` | bigint | FK a `pedido`. |
| `monto_aplicado` | numeric | Monto del pago aplicado al pedido. |
| `tipo_aplicacion` | text/enum | Sena, saldo, pago parcial o cobro final. |
| `observacion` | text | Observacion opcional. |

PK:

```text
pago_pedido.id_pago_pedido
```

FK:

```text
pago_pedido.id_pago -> pago.id_pago
pago_pedido.id_pedido -> pedido.id_pedido
```

Cardinalidad:

```text
pago N - N pedido mediante pago_pedido
```

Criterio:

- permite registrar senas, saldos y futuros pagos parciales;
- evita mezclar la existencia del pago con su aplicacion a pedidos.

## 19. Tabla `auditoria`

Registra eventos criticos, errores y trazabilidad.

| Campo | Tipo sugerido | Descripcion |
|---|---|---|
| `id_auditoria` | bigint identity | PK. |
| `id_usuario_actor` | bigint | Usuario que dispara el evento, si aplica. |
| `id_pedido` | bigint | Pedido asociado, si aplica. |
| `tabla_afectada` | text | Tabla principal afectada. |
| `id_registro_afectado` | text | Identificador del registro afectado. |
| `accion` | text | Accion o evento ocurrido. |
| `nivel` | text/enum | Info, warning, error o critico. |
| `codigo` | text | Codigo estable para soporte o frontend. |
| `mensaje` | text | Mensaje seguro y compacto. |
| `request_id` | uuid/text | Identificador de request/operacion. |
| `metadata` | jsonb | Metadata compacta y segura. |
| `fecha_creacion` | timestamptz | Fecha de registro. |

PK:

```text
auditoria.id_auditoria
```

FK:

```text
auditoria.id_usuario_actor -> usuario.id_usuario
auditoria.id_pedido -> pedido.id_pedido
```

Criterios:

- no guardar archivos;
- no guardar claves;
- no guardar tokens;
- no guardar passwords;
- no guardar payloads completos;
- no guardar datos sensibles innecesarios;
- usar `metadata` solo para informacion compacta y segura;
- usar `request_id` para relacionar Edge Functions, errores y eventos.

RLS:

- cliente no modifica auditoria;
- cliente no accede a auditoria interna;
- lectura interna solo por roles/permisos autorizados;
- escritura solo mediante backend, RPC, Edge Functions o procesos internos.

## 20. Ownership y RLS esperada

| Tabla | Ownership principal | Regla RLS esperada |
|---|---|---|
| `usuario` | `id_usuario_auth` / `id_usuario` | Cliente lee su propio usuario; rol no editable por cliente. |
| `contacto` | `id_usuario` | Cliente gestiona sus contactos permitidos. |
| `direccion` | `id_usuario` | Cliente gestiona sus direcciones permitidas. |
| `pedido` | `id_usuario` | Cliente ve sus pedidos; cambios segun estado. |
| `archivo` | `id_pedido`, `id_usuario` | Cliente ve archivos de sus pedidos; binario por acceso autorizado. |
| `pedido_servicio` | `id_pedido` | Cliente ve servicios aplicados a sus pedidos. |
| `pago` | interno | Cliente solo ve informacion financiera visible si se expone. |
| `pago_pedido` | `id_pedido` | Cliente solo ve aplicaciones visibles de sus pedidos. |
| `auditoria` | interno | Cliente no accede a auditoria interna. |
| `rol`, `permiso`, `rol_permiso` | interno | Cliente no modifica autorizacion. |
| `servicio` | catalogo | Cliente lee servicios activos si el flujo lo requiere. |
| `punto_entrega` | catalogo | Cliente lee puntos activos. |

## 21. Relacion con seeds

Seeds de desarrollo esperados:

- roles iniciales: `cliente`, `empleado`, `administrador`;
- permisos iniciales necesarios para pruebas;
- relaciones `rol_permiso`;
- servicios base de ejemplo;
- puntos de entrega activos;
- usuarios de prueba solo si no contienen credenciales reales;
- pedidos, archivos y pagos de ejemplo solo como datos ficticios.

No deben guardarse en seeds:

- claves privadas;
- tokens;
- contrasenas reales;
- archivos reales;
- datos personales reales;
- claves AES planas;
- payloads binarios.

## 22. Relacion con DER y migraciones

El DER de #121 debe representar las tablas y relaciones de este documento.

Las migraciones de #95 deben implementar este modelo en el siguiente orden
sugerido:

1. `rol`.
2. `permiso`.
3. `rol_permiso`.
4. `usuario`.
5. `contacto`.
6. `direccion`.
7. `punto_entrega`.
8. `servicio`.
9. `pedido`.
10. `pedido_servicio`.
11. `archivo`.
12. `pago`.
13. `pago_pedido`.
14. `auditoria`.

## 23. Decisiones pendientes

Antes de implementar migraciones, deben validarse estas decisiones:

- valores definitivos de enums de estados de `pedido`;
- valores definitivos de `estado_financiero`;
- catalogo inicial de permisos;
- servicios reales del MVP;
- puntos de entrega reales o ficticios para demo;
- si el comprobante de pago se modela como metadata en `pago` o como archivo
  protegido adicional;
- si `punto_entrega` cubre solo retiro en local o tambien entrega a domicilio.

## 24. Criterios de aceptacion del documento

- Todas las tablas propuestas para #95 estan documentadas.
- Cada tabla tiene proposito, PK, relaciones y columnas principales.
- Las relaciones quedan claras para construir el DER.
- El documento respeta `guia-estilo-bdd.md`.
- El documento diferencia datos de negocio, Storage privado, auditoria y seeds.
