# DER del modelo de datos Supabase

| Campo | Valor |
|---|---|
| Version | 1.1 |
| Estado | Actualizado |
| Fecha | 2026-06-25 |
| Responsable | Agustin Tejero |
| Issue relacionado | [#121 - BDD - Confeccionar DER del modelo de datos Supabase](https://github.com/AgusFT/La-Montana/issues/121) |

## 1. Objetivo

Este documento centraliza la referencia actual del Diagrama Entidad-Relacion
del MVP Supabase de La Montana y explica la estructura vigente de la base de
datos implementada para el flujo principal de pedidos.

El DER documenta las entidades, claves primarias, claves foraneas y relaciones
principales usadas por el backend Supabase/PostgreSQL. Sirve como complemento
del documento `modelo-tablas-relaciones-supabase.md` y como evidencia asociada
al issue [#121](https://github.com/AgusFT/La-Montana/issues/121).

## 2. Link al DER

Diagrama DER en Lucidchart:

<https://lucid.app/lucidchart/cfca862c-ff65-4b4d-aade-8e260835f0a1/edit?viewport_loc=41%2C-386%2C4893%2C2235%2Cpage1&invitationId=inv_fde19ddf-fa17-48b9-9130-3327712969f5>

Artefactos versionados relacionados:

| Artefacto | Ubicacion |
|---|---|
| DER SVG exportado desde Lucidchart | `diseño/diseño-de-datos/der-modelo-datos-supabase.svg` |
| Modelo de tablas | `diseño/diseño-de-datos/modelo-tablas-relaciones-supabase.md` |
| Flujo BDD | `diseño/diseño-de-datos/flujo-construccion-poblado-bdd.md` |

## 3. Alcance del modelo actual

El modelo actual cubre la base de datos necesaria para el MVP del sistema La
Montana, con foco en el portal cliente, gestion de pedidos, archivos, estados,
cotizacion, pagos basicos y auditoria.

La implementacion tecnica se encuentra en las migraciones de Supabase:

```text
desarrollo/backend-supabase/supabase/migrations/
```

Los datos iniciales de desarrollo se encuentran en:

```text
desarrollo/backend-supabase/supabase/seed.sql
```

## 4. Estructura general de la base de datos

| Bloque | Tablas principales | Responsabilidad |
|---|---|---|
| Seguridad y usuarios | `auth.users`, `usuario`, `rol`, `permiso`, `rol_permiso` | Vincular autenticacion Supabase Auth con usuarios de negocio, roles y permisos. |
| Contacto y entrega | `contacto`, `direccion`, `punto_entrega` | Guardar datos de contacto, direcciones y puntos disponibles de retiro o entrega. |
| Pedidos y servicios | `pedido`, `pedido_servicio`, `servicio` | Registrar solicitudes de impresion, servicios seleccionados, estados y cotizacion. |
| Archivos | `archivo` | Registrar metadata segura de archivos asociados a pedidos y Storage privado. |
| Pagos | `pago`, `pago_pedido` | Registrar pagos y aplicarlos a uno o mas pedidos. |
| Auditoria | `auditoria` | Registrar eventos criticos, errores controlados y trazabilidad operativa. |

## 5. Entidades principales

### 5.1 Seguridad y usuarios

Supabase Auth mantiene la identidad tecnica en `auth.users`. La tabla
`usuario` representa al usuario propio del negocio y se vincula con
`auth.users(id)` mediante `usuario.id_usuario_auth`.

Cada usuario tiene un rol principal mediante `usuario.id_rol`. Los roles se
definen en `rol`, los permisos funcionales en `permiso` y la relacion muchos a
muchos entre ambos se resuelve mediante `rol_permiso`.

Relaciones principales:

```text
auth.users 1 - 0..1 usuario
rol 1 - N usuario
rol N - N permiso mediante rol_permiso
```

### 5.2 Contacto y entrega

`contacto` y `direccion` dependen de `usuario` y permiten registrar informacion
asociada al cliente o usuario interno. `punto_entrega` funciona como catalogo
de lugares habilitados para retiro o entrega.

Relaciones principales:

```text
usuario 1 - N contacto
usuario 1 - N direccion
punto_entrega 1 - N pedido
```

### 5.3 Pedidos y servicios

`pedido` es la entidad central del modulo implementado. Representa la solicitud
del cliente, sus estados, datos de configuracion del trabajo, punto de entrega,
totales estimados y referencias de auditoria.

`servicio` contiene el catalogo de conceptos cobrables. `pedido_servicio`
relaciona pedidos con servicios y congela la cantidad, precio unitario aplicado
y subtotal calculado al momento de la cotizacion.

Relaciones principales:

```text
usuario 1 - N pedido
pedido N - N servicio mediante pedido_servicio
```

Decision de diseno importante:

- `pedido.estado_interno` representa el estado operativo usado por backend y
  usuarios internos.
- `pedido.estado_visible_cliente` representa lo que puede ver el cliente.
- `pedido.estado_financiero` separa la situacion de pago o evaluacion
  financiera del avance operativo.

### 5.4 Archivos

`archivo` registra metadata de archivos cargados por el cliente para un pedido.
No guarda el binario en PostgreSQL; el archivo se almacena en Supabase Storage
privado y la tabla conserva referencias controladas como bucket, ruta, hash,
estado del archivo y datos necesarios para el contrato de cifrado.

Relaciones principales:

```text
pedido 1 - N archivo
usuario 1 - N archivo
```

### 5.5 Pagos

`pago` representa un registro financiero. `pago_pedido` permite aplicar pagos a
pedidos, manteniendo separada la transaccion financiera de la solicitud de
impresion.

Relaciones principales:

```text
pago N - N pedido mediante pago_pedido
```

### 5.6 Auditoria

`auditoria` registra eventos criticos del backend, acciones relevantes sobre
pedidos y archivos, errores controlados y datos minimos de trazabilidad. Puede
referenciar al usuario actor y al pedido asociado.

Relaciones principales:

```text
usuario 1 - N auditoria
pedido 1 - N auditoria
```

La tabla de auditoria se trata como append-only en la practica: no forma parte
del flujo normal modificar o eliminar eventos ya registrados.

## 6. Migraciones relacionadas

| Archivo | Aporte principal |
|---|---|
| `20260622111241_esquema_inicial_mvp.sql` | Crea tablas base, claves primarias, foraneas, constraints, indices y triggers de auditoria base. |
| `20260623205800_auth_roles_permisos_iniciales.sql` | Carga roles/permisos iniciales y funciones auxiliares de usuario actual, rol y permisos. |
| `20260624010500_rls_inicial_mvp.sql` | Habilita RLS, define politicas iniciales y vistas cliente como `pedido_cliente` y `archivo_cliente`. |
| `20260624120000_auditoria_eventos_criticos.sql` | Define funciones de auditoria para eventos criticos del MVP. |
| `20260624143000_storage_archivos_pedidos.sql` | Configura bucket privado y validaciones de archivos para pedidos. |
| `20260624160000_orders_create.sql` | Agrega campos de creacion de pedido y RPC para crear pedidos de cliente. |
| `20260624170000_cargar_archivo.sql` | Agrega soporte para preparar y registrar carga de archivos. |
| `20260624183000_confirmar_pedido.sql` | Agrega confirmacion de pedido por parte del cliente y auditoria asociada. |
| `20260625030000_cotizacion_pedido.sql` | Implementa cotizacion real, reglas de senia y persistencia de `pedido_servicio`. |
| `20260625210000_cancelar_pedido.sql` | Implementa cancelacion de pedidos propios no confirmados en estado `pendiente_revision`. |

## 7. Seed de desarrollo

El archivo `desarrollo/backend-supabase/supabase/seed.sql` carga datos minimos
para demo y validacion local.

Incluye:

- usuarios locales de prueba: administrador, empleado y cliente;
- roles iniciales: `administrador`, `empleado`, `cliente`;
- puntos de entrega usados por el flujo web;
- servicios base de impresion y terminacion;
- precios iniciales alineados con la cotizacion del MVP.

El seed no debe incluir datos reales de clientes, claves privadas, tokens,
archivos binarios ni informacion sensible.

## 8. Reglas de integridad y seguridad reflejadas en el DER

El modelo contempla:

- claves primarias `id_<tabla>`;
- claves foraneas explicitas entre entidades relacionadas;
- constraints para evitar codigos vacios, montos negativos y estados invalidos;
- indices sobre campos de busqueda frecuentes y claves foraneas;
- borrado logico mediante `eliminado` y `fecha_eliminacion`;
- auditoria base mediante fechas, usuario de creacion/modificacion/eliminacion
  y `version_fila`;
- RLS para limitar acceso por rol y ownership;
- vistas/RPC/Edge Functions para evitar exponer tablas criticas directamente al
  cliente.

## 9. Trazabilidad

Este documento queda vinculado al issue
[#121 - BDD - Confeccionar DER del modelo de datos Supabase](https://github.com/AgusFT/La-Montana/issues/121).

Tambien se relaciona con:

- `#120` - documentar tablas y relaciones del modelo Supabase;
- `#122` - documentar flujo de construccion y poblado de la BDD;
- `#95` - crear migraciones iniciales del esquema MVP;
- `#102` - preparar seed data, pruebas RLS y validacion tecnica backend MVP.
