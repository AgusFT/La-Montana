# Guia de estilo BDD Supabase

| Campo | Valor |
|---|---|
| Version | 1.0 |
| Estado | Inicial |
| Fecha | 2026-06-21 |
| Responsable | Agustin Tejero |

## 1. Objetivo

Este documento define las reglas de estilo para el diseno de la base de datos
Supabase/PostgreSQL del proyecto La Montana.

La guia debe usarse antes de documentar tablas, relaciones, DER, migraciones,
seeds, RLS y cualquier operacion backend asociada a la base de datos.

## 2. Alcance

Aplica a:

- tablas;
- columnas;
- claves primarias;
- claves foraneas;
- tablas intermedias;
- enums;
- constraints;
- indices;
- policies RLS;
- funciones SQL;
- migraciones;
- seeds;
- tablas de auditoria.

## 3. Principios generales

- Usar nombres en espanol.
- Usar nombres en singular.
- Usar `snake_case`.
- Usar minusculas.
- No usar espacios.
- No usar tildes.
- No usar caracteres especiales.
- No usar palabras reservadas de SQL/PostgreSQL.
- Priorizar nombres claros sobre abreviaturas.
- Mantener Supabase/PostgreSQL como fuente de verdad.
- Evitar que el frontend defina reglas criticas de negocio.

Ejemplos correctos:

```text
usuario
rol
permiso
rol_permiso
pedido
pedido_servicio
pago
pago_pedido
auditoria
```

Ejemplos a evitar:

```text
usuarios
Usuario
user
order
pedido-servicio
direccion_con_tilde
```

## 4. Normalizacion

El modelo debe tender a tercera forma normal, 3NF, como criterio general.

Reglas esperadas:

- Los atributos deben ser atomicos.
- No se deben guardar listas dentro de columnas de texto.
- No se deben duplicar datos que pertenezcan a otra entidad.
- No se deben guardar datos derivados salvo decision justificada.
- Las relaciones muchos a muchos deben resolverse con tablas intermedias.

Se permite desnormalizacion puntual solo cuando:

- congele una decision de negocio al momento de una operacion;
- preserve trazabilidad historica;
- evite recalcular informacion historica sensible;
- no duplique archivos, secretos, claves ni payloads grandes;
- quede explicada en la documentacion de tablas.

Ejemplo valido de desnormalizacion:

```text
pedido_servicio.precio_unitario_aplicado
```

Ese valor se guarda aunque `servicio.precio_base` exista, porque el precio
aplicado debe conservarse tal como fue usado en el pedido.

## 5. Tablas

Las tablas deben nombrarse en espanol, singular y `snake_case`.

Formato:

```text
nombre_tabla
```

Ejemplos:

```text
usuario
rol
permiso
contacto
direccion
punto_entrega
servicio
pedido
archivo
pago
auditoria
```

Las tablas intermedias deben usar los nombres de las entidades relacionadas,
tambien en singular.

Ejemplos:

```text
rol_permiso
pedido_servicio
pago_pedido
```

## 6. Claves primarias

Toda tabla debe tener una clave primaria con el formato:

```text
id_<nombre_de_tabla>
```

Ejemplos:

```text
usuario.id_usuario
rol.id_rol
pedido.id_pedido
pedido_servicio.id_pedido_servicio
auditoria.id_auditoria
```

Las claves primarias deben ser autoincrementales por defecto usando identidad
de PostgreSQL:

```sql
generated always as identity
```

Se puede usar otro tipo de identificador solo si existe una decision tecnica
justificada y documentada.

## 7. Claves foraneas

Las claves foraneas deben usar el formato:

```text
id_<tabla_referenciada>
```

Ejemplos:

```text
pedido.id_usuario
archivo.id_pedido
pedido_servicio.id_servicio
pago_pedido.id_pago
```

Cuando una tabla tenga mas de una relacion hacia la misma tabla, se debe sumar
un nombre semantico claro.

Ejemplos:

```text
auditoria.id_usuario_actor
usuario.id_rol
pedido.id_punto_entrega
```

## 8. Relacion con Supabase Auth

Supabase Auth mantiene sus usuarios en `auth.users`.

La tabla propia del sistema debe llamarse:

```text
usuario
```

Debe incluir un campo para vincular el usuario del negocio con Supabase Auth:

```text
id_usuario_auth
```

Este campo debe referenciar `auth.users(id)` y debe usarse para validar
ownership y reglas RLS junto con `auth.uid()`.

## 9. Roles y permisos

Para el MVP, cada usuario tiene un solo rol.

La tabla `usuario` debe tener:

```text
id_rol
```

Un rol puede tener muchos permisos y un permiso puede estar en muchos roles.
Esa relacion debe modelarse con:

```text
rol_permiso
```

Relaciones esperadas:

```text
rol 1 - N usuario
rol N - N permiso mediante rol_permiso
```

## 10. Auditoria base

Todas las tablas persistentes deben incluir auditoria base, salvo excepcion
justificada.

Campos requeridos:

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

- `fecha_creacion` debe completarse al insertar.
- `fecha_modificacion` debe actualizarse al modificar.
- `fecha_eliminacion` debe completarse en borrado logico.
- `eliminado` debe indicar borrado logico.
- `version_fila` debe comenzar en `1` y aumentar ante cambios relevantes.
- Los campos `id_usuario_*` deben referenciar `usuario(id_usuario)` cuando
  exista usuario actor.
- En operaciones de sistema, migraciones o seeds puede admitirse valor nulo si
  queda justificado.

## 11. Borrado logico

El borrado logico debe usarse cuando preservar historial, trazabilidad o
relaciones sea importante.

Debe aplicarse especialmente en:

```text
usuario
contacto
direccion
punto_entrega
servicio
pedido
archivo
pago
rol
permiso
rol_permiso
pedido_servicio
pago_pedido
```

La tabla `auditoria` no debe eliminarse fisicamente ni modificarse libremente.

## 12. Tabla auditoria

Debe existir una tabla llamada:

```text
auditoria
```

Su objetivo es registrar eventos criticos, errores y datos utiles para
trazabilidad y depuracion.

Campos recomendados:

```text
id_auditoria
id_usuario_actor
tabla_afectada
id_registro_afectado
accion
nivel
codigo
mensaje
request_id
metadata
fecha_creacion
```

Criterios:

- No guardar archivos.
- No guardar claves.
- No guardar tokens.
- No guardar passwords.
- No guardar payloads completos.
- No guardar datos sensibles innecesarios.
- Usar `metadata` solo para informacion compacta y segura.
- Usar `request_id` para relacionar errores, Edge Functions y eventos.

## 13. Servicios y precios

La tabla `servicio` debe representar servicios ofrecidos y sus precios base.

Campos base esperados:

```text
id_servicio
codigo
nombre
descripcion
precio_base
tipo_moneda
activo
```

`tipo_moneda` debe aceptar valores controlados, inicialmente:

```text
ARS
USD
```

Los servicios aplicados a un pedido deben registrarse en:

```text
pedido_servicio
```

Esta tabla debe conservar el precio aplicado al momento del pedido.

## 14. Pagos

Los pagos deben modelarse con tablas separadas:

```text
pago
pago_pedido
```

La tabla `pago` representa el pago recibido o registrado.

La tabla `pago_pedido` vincula pagos con pedidos y permite registrar montos
aplicados, senas, saldos o futuros pagos parciales.

Estados iniciales sugeridos para `pago.estado`:

```text
pendiente
confirmado
rechazado
anulado
```

## 15. Estados de pedido

La tabla `pedido` debe contemplar al menos un estado operativo inicial.

Estados iniciales sugeridos:

```text
pendiente_autorizacion
confirmado
en_proceso
enviando
completado
```

Si se requiere separar estado visible, interno y financiero, esa decision debe
quedar documentada en el modelo de tablas y relaciones antes de implementar las
migraciones.

## 16. Enums y valores controlados

Los valores controlados pueden implementarse como enums o constraints segun el
caso.

Convencion de nombres para enums:

```text
enum_<tabla>_<campo>
```

Ejemplos:

```text
enum_pedido_estado
enum_pago_estado
enum_servicio_tipo_moneda
```

## 17. Constraints

Las constraints deben tener nombres claros.

Formato sugerido:

```text
<tabla>_<campo>_<tipo_constraint>
```

Ejemplos:

```text
usuario_email_unico
servicio_codigo_unico
pago_monto_check
```

## 18. Indices

Los indices deben nombrarse con el formato:

```text
idx_<tabla>_<campo_o_proposito>
```

Ejemplos:

```text
idx_pedido_id_usuario
idx_archivo_id_pedido
idx_auditoria_request_id
```

## 19. Policies RLS

Las policies RLS deben nombrarse en espanol y describir actor, accion y alcance.

Formato sugerido:

```text
<tabla>_<accion>_<actor>_<alcance>
```

Ejemplos:

```text
pedido_select_cliente_propios
archivo_insert_cliente_propios
auditoria_select_admin
```

## 20. Migraciones y seeds

Las migraciones deben implementar lo definido previamente en la documentacion
de diseno de datos.

Las seeds deben usarse para datos minimos reproducibles de desarrollo y
validacion local.

No se deben guardar secretos, claves privadas, tokens ni datos sensibles reales
en seeds.

## 21. Relacion con otros documentos

Esta guia debe ser usada como base para:

- #92 - modelo de datos inicial y estrategia RLS.
- #95 - migraciones iniciales del esquema MVP.
- #120 - documento de tablas y relaciones.
- #121 - DER del modelo de datos.
- #122 - flujo de construccion y poblado de la BDD.

## 22. Criterios de aceptacion

Este documento queda completo cuando:

- define convenciones de nombres para tablas y columnas;
- define convenciones para PK, FK, enums, constraints, indices y policies;
- define auditoria base y borrado logico;
- define el uso de la tabla `auditoria`;
- define el criterio de normalizacion;
- sirve como entrada directa para documentar tablas y relaciones.
