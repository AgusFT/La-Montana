# Estrategia RLS Supabase

| Campo | Valor |
|---|---|
| Version | 1.0 |
| Estado | Inicial |
| Fecha | 2026-06-22 |
| Responsable | Agustin Tejero |

## 1. Objetivo

Este documento define la estrategia inicial de Row Level Security, RLS, para la
base de datos Supabase/PostgreSQL del MVP de La Montana.

El documento sirve como entrada directa para:

- #92 - modelo de datos inicial y estrategia RLS;
- #96 - Auth, usuario, roles y permisos iniciales;
- #98 - politicas RLS iniciales del MVP;
- #102 - seed data, pruebas RLS y validacion tecnica backend.

## 2. Alcance

La estrategia cubre las tablas del modelo MVP:

- `usuario`;
- `rol`;
- `permiso`;
- `rol_permiso`;
- `contacto`;
- `direccion`;
- `punto_entrega`;
- `servicio`;
- `pedido`;
- `pedido_servicio`;
- `archivo`;
- `pago`;
- `pago_pedido`;
- `auditoria`.

Tambien cubre el criterio de acceso a Storage privado cuando el acceso depende
de `pedido`, `archivo` y `usuario`.

## 3. Principios de seguridad

- RLS debe estar habilitado en todas las tablas expuestas o sensibles.
- La seguridad no debe depender de ocultar botones en el frontend.
- El cliente solo debe acceder a datos propios o catalogos publicables.
- Las operaciones criticas deben pasar por Edge Functions, RPC controladas o
  procesos internos.
- `service_role` solo debe usarse desde backend controlado, nunca desde cliente.
- Las tablas internas no deben quedar modificables por usuarios finales.
- La auditoria no debe exponer payloads, archivos, claves, tokens ni secretos.
- Los archivos deben permanecer en Storage privado, sin public URLs.
- El borrado fisico no debe ser la operacion normal sobre tablas con
  trazabilidad; debe priorizarse borrado logico.

## 4. Actores

| Actor | Descripcion | Origen esperado |
|---|---|---|
| `anon` | Usuario no autenticado. | Rol anonimo de Supabase. |
| `cliente` | Cliente autenticado que realiza pedidos. | `usuario.id_rol -> rol.codigo = cliente`. |
| `empleado` | Usuario interno que opera pedidos. | `usuario.id_rol -> rol.codigo = empleado`. |
| `administrador` | Usuario interno con gestion ampliada. | `usuario.id_rol -> rol.codigo = administrador`. |
| `agente_impresion` | Actor autorizado para preparar o imprimir trabajos. | Rol o permiso especifico pendiente. |
| `service_role` | Rol tecnico de backend. | Edge Functions, RPC o tareas internas. |

Para el MVP cada `usuario` tiene un solo `rol`. Los permisos se resuelven con
`rol_permiso`.

## 5. Funciones auxiliares esperadas

#98 deberia implementar funciones auxiliares para evitar duplicar logica en
cada policy.

Funciones candidatas:

| Funcion | Proposito |
|---|---|
| `usuario_actual_id()` | Devuelve `usuario.id_usuario` asociado a `auth.uid()`. |
| `usuario_actual_rol()` | Devuelve `rol.codigo` del usuario autenticado. |
| `usuario_tiene_rol(codigo_rol text)` | Verifica si el usuario actual tiene un rol. |
| `usuario_tiene_permiso(codigo_permiso text)` | Verifica permisos por `rol_permiso`. |
| `pedido_pertenece_a_usuario(id_pedido bigint)` | Verifica ownership de un pedido. |
| `archivo_pertenece_a_usuario(id_archivo bigint)` | Verifica ownership indirecto de un archivo. |

Criterios tecnicos:

- usar nombres en espanol, singular cuando aplique y `snake_case`;
- usar `security definer` solo cuando sea necesario;
- fijar `search_path` dentro de funciones sensibles;
- no retornar datos sensibles desde funciones helper;
- validar que las funciones no permitan escalamiento de rol.

## 6. Criterio por tabla

| Tabla | Ownership | Select cliente | Insert cliente | Update cliente | Escritura interna |
|---|---|---|---|---|---|
| `usuario` | `id_usuario_auth` | Solo propio. | No directo; automatico por #96. | Solo campos permitidos, nunca `id_rol`. | Admin/backend. |
| `rol` | Interno. | No directo o lectura minima controlada. | No. | No. | Admin/backend. |
| `permiso` | Interno. | No directo o lectura minima controlada. | No. | No. | Admin/backend. |
| `rol_permiso` | Interno. | No directo. | No. | No. | Admin/backend. |
| `contacto` | `id_usuario` | Solo propios. | Propios. | Propios. | Admin/backend. |
| `direccion` | `id_usuario` | Solo propias. | Propias. | Propias. | Admin/backend. |
| `punto_entrega` | Catalogo. | Activos. | No. | No. | Admin/backend. |
| `servicio` | Catalogo. | Activos. | No. | No. | Admin/backend. |
| `pedido` | `id_usuario` | Solo propios y campos visibles. | Preferentemente Edge Function. | Muy limitado o Edge Function. | Empleado/admin/backend. |
| `pedido_servicio` | `id_pedido` | Si el pedido es propio. | No directo. | No directo. | Empleado/admin/backend. |
| `archivo` | `id_usuario` e `id_pedido` | Metadata propia permitida. | Preferentemente Edge Function. | No directo o muy limitado. | Empleado/admin/backend. |
| `pago` | Interno. | No directo. | No. | No. | Empleado/admin/backend. |
| `pago_pedido` | `id_pedido` | Preferentemente via vista/RPC visible. | No. | No. | Empleado/admin/backend. |
| `auditoria` | Interno. | No. | No directo. | No. | Backend/RPC/Edge Function. |

## 7. Politicas esperadas por tabla

### 7.1 `usuario`

Reglas:

- cliente puede leer solo su propio registro:

```text
usuario.id_usuario_auth = auth.uid()
```

- cliente no puede modificar `id_rol`, `estado`, campos de auditoria ni flags
  de borrado logico;
- creacion automatica de `usuario` debe resolverse en #96 mediante trigger,
  funcion controlada o backend;
- administradores pueden gestionar usuarios segun permisos;
- empleados pueden consultar usuarios solo cuando una operacion lo justifique.

### 7.2 `rol`, `permiso` y `rol_permiso`

Reglas:

- cliente no puede insertar, actualizar ni eliminar roles o permisos;
- empleado no puede modificar autorizacion salvo permiso explicito;
- administrador o backend controlado gestionan roles y permisos;
- las funciones helper pueden consultar estas tablas para resolver permisos;
- si se permite lectura directa, debe limitarse a informacion no sensible y
  activa.

### 7.3 `contacto` y `direccion`

Reglas:

- cliente puede consultar registros propios;
- cliente puede crear y actualizar registros propios;
- borrado fisico no debe exponerse como operacion normal;
- administrador puede gestionar segun permisos;
- no se exponen contactos ni direcciones de otros clientes.

### 7.4 `punto_entrega` y `servicio`

Reglas:

- cliente puede leer registros activos si el flujo lo requiere;
- `anon` solo deberia leer catalogos si se decide que el catalogo sea publico;
- escritura queda reservada a administrador o backend;
- registros inactivos o eliminados no deben aparecer en consultas de cliente.

### 7.5 `pedido`

Reglas:

- cliente puede ver solo pedidos propios;
- cliente no debe ver campos internos si se exponen datos por tabla directa;
- `estado_interno`, `estado_financiero` y `observacion_interna` requieren
  proteccion adicional;
- crear pedido deberia pasar por Edge Function/RPC para validar negocio,
  servicios, archivos, senia, auditoria y estados iniciales;
- modificaciones criticas de estado deben pasar por backend controlado;
- empleado y administrador acceden segun rol/permisos.

Nota importante:

RLS protege filas, no columnas. Para ocultar campos internos al cliente, #98
debe combinar RLS con vistas, RPC, Edge Functions o privilegios por columna.

### 7.6 `pedido_servicio`

Reglas:

- cliente puede consultar servicios aplicados solo si el pedido asociado es
  propio;
- cliente no deberia modificar directamente `precio_unitario_aplicado` ni
  `subtotal`;
- altas y cambios deben pasar por backend controlado para preservar precios
  historicos y auditoria.

### 7.7 `archivo`

Reglas:

- cliente puede consultar metadata permitida solo para archivos propios o de
  pedidos propios;
- cliente no puede consultar metadata de archivos de otros pedidos;
- carga de metadata debe pasar preferentemente por Edge Function;
- `clave_envuelta`, `iv`, `hash_archivo` y `ruta_storage` no deben exponerse si
  no son necesarios para el actor;
- empleados y administradores acceden segun permisos y estado del pedido.

Nota importante:

La metadata en `archivo` no reemplaza las policies de Storage. Ambas capas deben
ser coherentes.

### 7.8 `pago` y `pago_pedido`

Reglas:

- cliente no registra pagos internos directamente;
- cliente no actualiza estados de pago;
- informacion financiera visible debe exponerse mediante vista, RPC o Edge
  Function cuando corresponda;
- empleado y administrador gestionan pagos segun permisos financieros;
- `pago_pedido` permite ownership indirecto por `pedido`, pero no implica que
  toda la informacion financiera sea publicable al cliente.

### 7.9 `auditoria`

Reglas:

- cliente no lee auditoria interna;
- cliente no inserta, actualiza ni elimina auditoria directamente;
- escritura debe hacerse desde backend, RPC, Edge Function o proceso interno;
- lectura queda restringida a administradores o soporte autorizado;
- no se guardan secretos, tokens, claves, archivos ni payloads completos.

## 8. Storage privado

Storage debe permanecer privado.

Criterios:

- no usar public URLs;
- no usar rutas locales del cliente como fuente de verdad;
- el binario original no se guarda en la base;
- si se aplica cifrado, Storage guarda ciphertext;
- la base guarda metadata segura en `archivo`;
- la policy de Storage debe validar ownership contra `archivo`, `pedido` y
  `usuario`;
- el acceso a binarios debe pasar por mecanismo autorizado, por ejemplo Edge
  Function, signed URL controlada o descarga mediada.

La ruta de Storage deberia tener una estructura predecible y validable, por
ejemplo:

```text
pedido/<id_pedido>/<id_archivo_o_uuid>
```

La decision final de rutas queda para #97 y #113.

## 9. Relacion con vistas, RPC y Edge Functions

Como RLS no filtra columnas por si sola, las consultas de cliente sobre tablas
con campos sensibles deben exponerse con una de estas estrategias:

- vistas con columnas visibles;
- RPC que retornen solo payloads seguros;
- Edge Functions que validen ownership y devuelvan DTOs controlados;
- privilegios por columna cuando sea apropiado.

Tablas donde esta precaucion es especialmente importante:

- `pedido`;
- `archivo`;
- `pago`;
- `pago_pedido`;
- `auditoria`.

## 10. Validacion esperada

#98 y #102 deben validar como minimo:

- cliente no ve `pedido` ajeno;
- cliente no ve `archivo` ajeno;
- cliente no puede modificar `id_rol`;
- cliente no puede modificar `rol`, `permiso` ni `rol_permiso`;
- cliente no accede a `auditoria`;
- cliente no modifica estados internos ni financieros;
- empleado accede solo segun rol/permisos;
- administrador puede operar tablas internas segun permisos;
- Storage no entrega archivos de pedidos ajenos;
- las operaciones con `service_role` no quedan expuestas al frontend.

## 11. Dependencias

| Issue | Relacion |
|---|---|
| #92 | Define esta estrategia documental. |
| #95 | Crea las tablas sobre las que se aplicara RLS. |
| #96 | Define Auth, usuario, roles, permisos y helpers necesarios. |
| #97 | Define Storage privado y policies asociadas a archivos. |
| #98 | Implementa policies RLS iniciales. |
| #99 | Implementa Edge Functions del flujo minimo. |
| #100 | Implementa auditoria de eventos criticos. |
| #102 | Agrega seeds y pruebas tecnicas/RLS. |

## 12. Preguntas pendientes antes de implementar #98

Estas preguntas no bloquean este documento inicial, pero deben resolverse antes
de cerrar la implementacion RLS:

1. `agente_impresion` sera un rol propio o un permiso dentro de `empleado`?
2. El cliente podra editar `usuario.nombre`, `usuario.apellido` y `usuario.email`
   directamente, o todo cambio pasara por Auth/backend?
3. `contacto` y `direccion` tendran CRUD directo del cliente o se gestionaran
   mediante RPC/Edge Function?
4. La creacion de `pedido` sera exclusivamente por Edge Function/RPC, o se
   permitira insert directo con RLS?
5. Que columnas exactas de `pedido` vera el cliente en Web/Android?
6. La informacion de `pago` visible al cliente saldra de una vista/RPC o de la
   tabla `pago_pedido` con RLS?
7. El catalogo `servicio` sera visible para `anon`, solo `authenticated` o solo
   mediante backend?
8. Los puntos de entrega seran solo retiro en local o tambien entrega a
   domicilio?
9. Cual sera el catalogo inicial de permisos funcionales?
10. La descarga/subida de archivos usara signed URLs, Edge Function mediadora o
    acceso directo a Storage con policies?

## 13. Criterios de aceptacion del documento

- Define actores y ownership principal por tabla.
- Distingue tablas de cliente, catalogos, tablas internas y auditoria.
- Identifica que RLS protege filas y no columnas.
- Define la necesidad de vistas, RPC o Edge Functions para datos sensibles.
- Alinea RLS con Storage privado y auditoria.
- Deja preguntas pendientes visibles antes de implementar #98.
