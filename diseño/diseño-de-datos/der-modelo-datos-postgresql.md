# DER v3 del modelo de datos PostgreSQL

| Campo | Valor |
|---|---|
| Versión | 3.0 |
| Estado | Aprobado |
| Fecha | 2026-09-09 |
| Rama de elaboración | `docs/der-modelo-datos-postgresql` |
| Publicación | `main` |
| Base documental | `main@09db5ad799362088dafb4bc8f881f432e67dc390` |
| Issue relacionada | [#157 - Crear DER v3 PostgreSQL/Spring Boot](https://github.com/AgusFT/La-Montana/issues/157) |
| Épica padre | [#155 - Fundación backend Spring Boot y PostgreSQL](https://github.com/AgusFT/La-Montana/issues/155) |
| Milestone | [M10 - Fundación Spring Boot y PostgreSQL](https://github.com/AgusFT/La-Montana/milestone/11) |
| Fuente editable | [`der-modelo-datos-postgresql.excalidraw`](./der-modelo-datos-postgresql.excalidraw) |
| Vista SVG | [`DER-V3.svg`](./DER-V3.svg) |
| Revisión visual | Aprobada por el usuario el 2026-09-09 |

## 1. Estado y autoridad

Este documento y el archivo `der-modelo-datos-postgresql.excalidraw` forman el diseño vigente del modelo de datos objetivo de La Montaña para PostgreSQL y Spring Boot. Sustituyen como autoridad de diseño al DER de Supabase, que se conserva únicamente como referencia histórica.

El diseño representa **una instalación y una base de datos por imprenta**. Las sucursales pertenecen a esa única imprenta; por eso no existe una tabla tenant `imprenta` ni se repite un identificador de tenant en cada entidad. `instalacion` identifica el despliegue técnico para licencias e integraciones, no una empresa usuaria adicional.

La fuente gráfica editable es el archivo Excalidraw. Puede importarse directamente en [Excalidraw](https://excalidraw.com/) sin instalar una extensión de VSCodium. La [vista SVG](./DER-V3.svg) permite consultar el diagrama completo directamente desde el repositorio.

La versión 3.0 aplica el escenario conservador de la auditoría de normalización. El lienzo separa una vista general con entidades completas y relaciones agregadas entre dominios de una vista formada por un grafo de relaciones para cada entidad. Esta duplicación visual no duplica datos del modelo: ambos sectores se generan desde este mismo diccionario.

## 2. Fuentes y precedencia

1. Decisiones funcionales aprobadas durante el relevamiento específico de este DER.
2. [Motor de configuración](../../marco-del-proyecto/motor-de-configuracion-del-sistema.md).
3. [WBS de Spring Boot y PostgreSQL](../../marco-del-proyecto/wbs-v4-spring-boot-postgresql.md).
4. [Stakeholders y actores](../../marco-del-proyecto/stakeholders-y-actores.md).
5. [DER Supabase histórico](./der-modelo-datos-supabase.md), solo para trazabilidad del estado anterior.

Ante contradicciones, este DER incorpora las decisiones posteriores aprobadas: motor de configuración oficial, aprobación automática/condicional, cuenta corriente, producción manual o automatizada, archivos ofimáticos convertidos y licencias por módulos.

## 3. Convenciones PostgreSQL

- Tablas y columnas usan español en `snake_case`.
- Las PK internas se proyectan como `bigint generated always as identity`; UUID separados sirven como identificadores públicos opacos.
- `timestamptz` representa instantes; `date` fechas civiles; `time` horarios locales interpretados con la zona IANA correspondiente.
- Todo importe usa `numeric(19,2)` y moneda ARS. Los porcentajes usan `numeric(7,4)` o `numeric(19,4)` cuando comparten una columna con cantidades.
- `jsonb` aparece solo para metadatos variables y sanitizados. Relaciones, condiciones, precios, módulos y capacidades permanecen normalizados.
- `PK` significa clave primaria, `FK` clave foránea, `UK` unicidad individual y `UK*` participación en una unicidad compuesta. `NN` en el diagrama significa no nulo.
- Los valores cerrados se implementarán después mediante restricciones `CHECK`, enums de aplicación o catálogos técnicos según su ciclo de cambio; este documento no incluye DDL.
- Los historiales y hechos financieros son append-only. Las FK históricas se acompañan con snapshots cuando una entidad viva podría cambiar.

## 4. Mapa de dominios

| Dominio | Color | Entidades | Responsabilidad |
|---|---|---:|---|
| Instalación, licencias e integraciones | `#0b7285` | 7 | Identificar la instalación local, licencias, módulos y eventos de proveedores. |
| Identidad, acceso y clientes | `#1971c2` | 12 | Autenticación, autorización, clientes personales y empresas cliente. |
| Sucursales y operación | `#2b8a3e` | 7 | Sucursales, horarios, pausas, puntos y zonas comunes. |
| Configuración, catálogos y reglas | `#e67700` | 22 | Versionar catálogos, precios, reglas, medios, entregas e impresoras habilitadas. |
| Cotizaciones, pedidos y correcciones | `#d9480f` | 14 | Persistir cotizaciones, pedidos, snapshots y correcciones. |
| Archivos, conversión y validación | `#c2255c` | 4 | Referenciar storage privado, validar, convertir y conservar trazabilidad. |
| Agentes, impresoras y producción | `#6741d9` | 9 | Operar agentes, impresoras, trabajos y control humano de calidad. |
| Pagos, cuenta corriente y documentos | `#c92a2a` | 14 | Registrar dinero, aplicaciones, cuenta corriente y documentos internos. |
| Programación y entregas | `#087f5b` | 6 | Definir capacidad, reservar cupos y registrar entregas completas. |
| Reclamos y compensaciones | `#a61e4d` | 5 | Resolver reclamos sin modificar pedidos cerrados. |
| Notificaciones, alertas y auditoría | `#495057` | 6 | Mantener inbox personal, push, alertas operativas y auditoría. |

**Total:** 106 entidades, 946 atributos y 229 relaciones FK explícitas.


## 5. Diccionario de entidades

### 5.1. Instalación, licencias e integraciones

#### `instalacion`

Identifica técnicamente esta instancia local sin representar una imprenta tenant.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_instalacion` | `bigint` | No | `PK` | — | Identificador interno de la instalación. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco compartido con servicios de YG. |
| `modo_alojamiento` | `varchar(16)` | No | — | — | ON_PREMISE o YG_HOSTING. |
| `dominio_principal` | `varchar(253)` | Sí | `UK` | — | Dominio público configurado para la aplicación. |
| `zona_horaria` | `varchar(64)` | No | — | — | Zona IANA usada para presentar fechas globales. |
| `estado` | `varchar(24)` | No | — | — | Estado operativo de la instalación. |
| `fecha_alta` | `timestamptz` | No | — | — | Momento de creación. |
| `fecha_actualizacion` | `timestamptz` | No | — | — | Última actualización técnica. |

**Restricciones de entidad**

- Existe exactamente una fila por base de datos operativa.

#### `modulo`

Catálogo técnico de módulos licenciables conocidos por la aplicación.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_modulo` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo` | `varchar(64)` | No | `UK` | — | Código estable, por ejemplo PAGOS_DIGITALES. |
| `nombre` | `varchar(120)` | No | — | — | Nombre visible. |
| `descripcion` | `text` | Sí | — | — | Descripción funcional. |
| `es_base` | `boolean` | No | — | — | Indica si forma parte del producto base. |
| `activo` | `boolean` | No | — | — | Permite retirar un módulo sin borrar historia. |

#### `licencia_local`

Caché local verificable de una licencia emitida por YG; nunca guarda el serial plano.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_licencia_local` | `bigint` | No | `PK` | — | Identificador interno. |
| `identificador_externo` | `uuid` | No | `UK` | — | Identificador asignado por YG. |
| `huella_serial` | `char(64)` | No | `UK` | — | Hash SHA-256 del serial, no reversible. |
| `tipo_vigencia` | `varchar(16)` | No | — | — | PERPETUA o CON_VENCIMIENTO. |
| `estado` | `varchar(24)` | No | — | — | ACTIVA, VENCIDA, REVOCADA o NO_VALIDADA. |
| `fecha_activacion` | `timestamptz` | No | — | — | Activación confirmada por YG. |
| `fecha_vencimiento` | `timestamptz` | Sí | — | — | Vencimiento conocido. |
| `ultima_validacion_exitosa` | `timestamptz` | Sí | — | — | Inicio de referencia para tolerancia. |
| `tolerancia_hasta` | `timestamptz` | Sí | — | — | Máximo de 72 horas solo ante indisponibilidad. |
| `fecha_revocacion` | `timestamptz` | Sí | — | — | Revocación recibida, efectiva inmediatamente. |

**Restricciones de entidad**

- La licencia pertenece a la única instalación representada por esta base de datos.

#### `licencia_modulo`

Concesiones aditivas de módulos incluidas en cada licencia.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_licencia_modulo` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_licencia_local` | `bigint` | No | `FK UK*` | `licencia_local` | Licencia que concede el módulo. |
| `id_modulo` | `bigint` | No | `FK UK*` | `modulo` | Módulo concedido. |
| `fecha_concesion` | `timestamptz` | No | — | — | Momento informado por YG. |

**Restricciones de entidad**

- La pareja licencia-módulo es única.
- Un módulo es efectivo si al menos una licencia vigente lo concede.

#### `integracion_externa`

Configuración local cifrada de Mercado Pago, MODO, correo o push.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_integracion_externa` | `bigint` | No | `PK` | — | Identificador interno. |
| `tipo` | `varchar(32)` | No | `UK` | — | MERCADO_PAGO, MODO, CORREO o PUSH. |
| `estado` | `varchar(24)` | No | — | — | NO_CONFIGURADA, ACTIVA, ERROR o DESHABILITADA. |
| `credencial_cifrada` | `bytea` | Sí | — | — | Secreto cifrado; la llave vive fuera de PostgreSQL. |
| `version_credencial` | `integer` | No | — | — | Versión usada para rotación. |
| `fecha_ultima_validacion` | `timestamptz` | Sí | — | — | Última comprobación correcta. |
| `metadatos` | `jsonb` | Sí | — | — | Metadatos no secretos permitidos por proveedor. |
| `fecha_actualizacion` | `timestamptz` | No | — | — | Última modificación. |

**Restricciones de entidad**

- El tipo es único dentro de la única instalación representada por esta base de datos.

#### `evento_integracion`

Evento idempotente recibido de un proveedor y evidencia de su procesamiento.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_evento_integracion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_integracion_externa` | `bigint` | No | `FK UK*` | `integracion_externa` | Integración que recibió el evento. |
| `identificador_externo` | `varchar(190)` | No | `UK*` | — | ID idempotente del proveedor. |
| `tipo_evento` | `varchar(80)` | No | — | — | Tipo informado por el proveedor. |
| `firma_valida` | `boolean` | No | — | — | Resultado de validar autenticidad. |
| `payload_hash` | `char(64)` | No | — | — | SHA-256 del cuerpo recibido. |
| `payload_sanitizado` | `jsonb` | Sí | — | — | Datos permitidos; nunca secretos ni payload bruto en auditoría. |
| `estado_procesamiento` | `varchar(24)` | No | — | — | RECIBIDO, PROCESADO, IGNORADO o ERROR. |
| `fecha_recepcion` | `timestamptz` | No | — | — | Momento de ingreso. |
| `fecha_procesamiento` | `timestamptz` | Sí | — | — | Momento de resolución. |
| `codigo_error` | `varchar(80)` | Sí | — | — | Código técnico sanitizado. |

**Restricciones de entidad**

- La pareja integración-identificador externo es única.

#### `conciliacion_integracion`

Ejecución periódica de respaldo contra la API del proveedor.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_conciliacion_integracion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_integracion_externa` | `bigint` | No | `FK` | `integracion_externa` | Integración conciliada. |
| `estado` | `varchar(24)` | No | — | — | INICIADA, COMPLETADA o ERROR. |
| `fecha_inicio` | `timestamptz` | No | — | — | Inicio de la consulta. |
| `fecha_fin` | `timestamptz` | Sí | — | — | Fin de la consulta. |
| `cantidad_consultada` | `integer` | No | — | — | Operaciones revisadas. |
| `cantidad_corregida` | `integer` | No | — | — | Diferencias reconciliadas. |
| `codigo_error` | `varchar(80)` | Sí | — | — | Error sanitizado. |

### 5.2. Identidad, acceso y clientes

#### `rol`

Catálogo cerrado de roles principales del sistema.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_rol` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo` | `varchar(40)` | No | `UK` | — | Código estable del rol. |
| `nombre` | `varchar(80)` | No | — | — | Nombre visible. |
| `es_interno` | `boolean` | No | — | — | Distingue personal de clientes. |
| `activo` | `boolean` | No | — | — | Conservación sin borrado. |

#### `permiso`

Catálogo de acciones autorizables por backend.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_permiso` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo` | `varchar(80)` | No | `UK` | — | Código funcional estable. |
| `nombre` | `varchar(120)` | No | — | — | Nombre visible. |
| `descripcion` | `text` | No | — | — | Alcance de la autorización. |
| `alcance` | `varchar(24)` | No | — | — | GLOBAL, SUCURSAL o PROPIO. |
| `activo` | `boolean` | No | — | — | Conservación sin borrado. |

#### `rol_permiso`

Permisos predeterminados concedidos a cada rol.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_rol_permiso` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_rol` | `bigint` | No | `FK UK*` | `rol` | Rol beneficiado. |
| `id_permiso` | `bigint` | No | `FK UK*` | `permiso` | Permiso concedido. |

**Restricciones de entidad**

- La pareja rol-permiso es única.

#### `empresa_cliente`

Agrupa usuarios empresariales, pedidos y una posible cuenta corriente.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_empresa_cliente` | `bigint` | No | `PK` | — | Identificador interno. |
| `razon_social` | `varchar(180)` | No | — | — | Nombre legal o comercial. |
| `cuit` | `char(11)` | No | `UK` | — | CUIT normalizado sin separadores. |
| `correo` | `varchar(254)` | No | — | — | Contacto principal. |
| `telefono` | `varchar(40)` | No | — | — | Contacto principal. |
| `estado` | `varchar(20)` | No | — | — | ACTIVA, DESACTIVADA o ANONIMIZADA. |
| `fecha_alta` | `timestamptz` | No | — | — | Alta por registro público. |
| `fecha_desactivacion` | `timestamptz` | Sí | — | — | Baja lógica. |

**Restricciones de entidad**

- Una empresa activa debe conservar al menos una dirección activa y completa.

#### `usuario`

Identidad autenticable y actor humano del sistema.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_usuario` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_rol` | `bigint` | No | `FK` | `rol` | Único rol principal. |
| `id_empresa_cliente` | `bigint` | Sí | `FK` | `empresa_cliente` | Empresa para roles empresariales. |
| `correo` | `varchar(254)` | No | `UK` | — | Único sin distinguir mayúsculas. |
| `hash_contrasena` | `varchar(255)` | No | — | — | Hash adaptativo de contraseña. |
| `nombre` | `varchar(100)` | No | — | — | Nombre de la persona. |
| `apellido` | `varchar(100)` | No | — | — | Apellido de la persona. |
| `telefono` | `varchar(40)` | Sí | — | — | Teléfono personal opcional. |
| `estado` | `varchar(28)` | No | — | — | Estado de acceso. |
| `correo_verificado_en` | `timestamptz` | Sí | — | — | Momento de verificación. |
| `debe_cambiar_contrasena` | `boolean` | No | — | — | Obliga cambio en próximo ingreso. |
| `es_soporte` | `boolean` | No | — | — | Marca la cuenta reservada SOPORTE. |
| `es_administrador_propietario` | `boolean` | No | — | — | Único administrador propietario activo. |
| `fecha_alta` | `timestamptz` | No | — | — | Creación. |
| `fecha_desactivacion` | `timestamptz` | Sí | — | — | Baja lógica. |

**Restricciones de entidad**

- Los roles empresariales exigen empresa; los demás la prohíben.
- Existe como máximo un SOPORTE y un administrador propietario activo.

#### `usuario_permiso`

Permiso adicional individual, permitido solamente para empleados.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_usuario_permiso` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_usuario` | `bigint` | No | `FK UK*` | `usuario` | Empleado beneficiado. |
| `id_permiso` | `bigint` | No | `FK UK*` | `permiso` | Permiso adicional. |
| `id_usuario_otorgante` | `bigint` | No | `FK` | `usuario` | Administrador que lo otorgó. |
| `fecha_otorgamiento` | `timestamptz` | No | — | — | Inicio de vigencia. |
| `fecha_revocacion` | `timestamptz` | Sí | — | — | Fin de vigencia. |

**Restricciones de entidad**

- No representa denegaciones.
- Existe como máximo una concesión activa por usuario y permiso; ciclos históricos revocados pueden repetirse.

#### `direccion_cliente`

Dirección reutilizable de un usuario personal o una empresa cliente.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_direccion_cliente` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_usuario` | `bigint` | Sí | `FK` | `usuario` | Propietario personal. |
| `id_empresa_cliente` | `bigint` | Sí | `FK` | `empresa_cliente` | Propietario empresarial. |
| `etiqueta` | `varchar(80)` | No | — | — | Nombre visible, por ejemplo Oficina. |
| `calle` | `varchar(160)` | No | — | — | Calle. |
| `numero` | `varchar(20)` | No | — | — | Altura. |
| `piso_departamento` | `varchar(40)` | Sí | — | — | Complemento. |
| `localidad` | `varchar(120)` | No | — | — | Localidad. |
| `provincia` | `varchar(120)` | No | — | — | Provincia. |
| `codigo_postal` | `varchar(12)` | No | — | — | Código postal normalizado. |
| `referencias` | `text` | Sí | — | — | Indicaciones opcionales. |
| `es_predeterminada` | `boolean` | No | — | — | Preferencia del propietario. |
| `activa` | `boolean` | No | — | — | Baja lógica. |

**Restricciones de entidad**

- Exactamente uno entre usuario y empresa debe ser no nulo.

#### `usuario_sucursal`

Asignación mutable de empleados a una o varias sucursales.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_usuario_sucursal` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_usuario` | `bigint` | No | `FK UK*` | `usuario` | Empleado asignado. |
| `id_sucursal` | `bigint` | No | `FK UK*` | `sucursal` | Sucursal habilitada. |
| `fecha_desde` | `timestamptz` | No | — | — | Inicio. |
| `fecha_hasta` | `timestamptz` | Sí | — | — | Fin de la asignación. |
| `activa` | `boolean` | No | — | — | Vigencia actual. |

**Restricciones de entidad**

- Un empleado activo conserva al menos una asignación a sucursal activa.

#### `credencial_temporal`

Credencial de alta, restablecimiento o cambio de llave, de un solo uso.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_credencial_temporal` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_usuario` | `bigint` | No | `FK` | `usuario` | Usuario destinatario. |
| `id_usuario_autorizante` | `bigint` | Sí | `FK` | `usuario` | Actor que autorizó el restablecimiento. |
| `proposito` | `varchar(32)` | No | — | — | ALTA o RESTABLECIMIENTO de acceso local. |
| `hash_codigo` | `varchar(255)` | No | — | — | Código nunca almacenado en plano. |
| `fecha_emision` | `timestamptz` | No | — | — | Creación. |
| `fecha_vencimiento` | `timestamptz` | No | — | — | Expiración. |
| `fecha_consumo` | `timestamptz` | Sí | — | — | Uso único. |
| `fecha_revocacion` | `timestamptz` | Sí | — | — | Revocación anticipada. |

#### `token_verificacion_correo`

Desafío de un solo uso para validar el correo de altas públicas.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_token_verificacion_correo` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_usuario` | `bigint` | No | `FK` | `usuario` | Usuario a verificar. |
| `hash_token` | `varchar(255)` | No | `UK` | — | Token hasheado. |
| `fecha_emision` | `timestamptz` | No | — | — | Creación. |
| `fecha_vencimiento` | `timestamptz` | No | — | — | Expiración. |
| `fecha_consumo` | `timestamptz` | Sí | — | — | Uso correcto. |
| `fecha_revocacion` | `timestamptz` | Sí | — | — | Invalidación. |

#### `sesion_usuario`

Refresh token hasheado por sesión o dispositivo, revocable individualmente.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_sesion_usuario` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco de sesión. |
| `id_usuario` | `bigint` | No | `FK` | `usuario` | Usuario autenticado. |
| `id_dispositivo_usuario` | `bigint` | Sí | `FK` | `dispositivo_usuario` | Dispositivo asociado si corresponde. |
| `hash_refresh_token` | `varchar(255)` | No | `UK` | — | Refresh token hasheado. |
| `fecha_emision` | `timestamptz` | No | — | — | Creación. |
| `fecha_vencimiento` | `timestamptz` | No | — | — | Vencimiento. |
| `ultimo_uso` | `timestamptz` | Sí | — | — | Última rotación o uso. |
| `fecha_revocacion` | `timestamptz` | Sí | — | — | Revocación inmediata. |
| `motivo_revocacion` | `varchar(120)` | Sí | — | — | Motivo normalizado. |

#### `autorizacion_soporte`

Ventana temporal auditada durante la cual SOPORTE recupera acceso.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_autorizacion_soporte` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_usuario_soporte` | `bigint` | No | `FK` | `usuario` | Cuenta reservada habilitada. |
| `id_usuario_habilitante` | `bigint` | No | `FK` | `usuario` | Administrador que habilita. |
| `motivo` | `text` | No | — | — | Solicitud de soporte. |
| `estado` | `varchar(20)` | No | — | — | VIGENTE, VENCIDA o REVOCADA. |
| `fecha_inicio` | `timestamptz` | No | — | — | Inicio. |
| `fecha_fin` | `timestamptz` | No | — | — | Máximo 24 horas. |
| `id_usuario_revocante` | `bigint` | Sí | `FK` | `usuario` | Administrador que revoca. |
| `fecha_revocacion` | `timestamptz` | Sí | — | — | Cierre anticipado. |

### 5.3. Sucursales y operación

#### `sucursal`

Local operativo de la imprenta a la que pertenecen pedidos y recursos.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_sucursal` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo` | `varchar(40)` | No | `UK` | — | Código estable. |
| `nombre` | `varchar(140)` | No | — | — | Nombre visible. |
| `calle` | `varchar(160)` | No | — | — | Dirección. |
| `numero` | `varchar(20)` | No | — | — | Altura. |
| `localidad` | `varchar(120)` | No | — | — | Localidad. |
| `provincia` | `varchar(120)` | No | — | — | Provincia. |
| `codigo_postal` | `varchar(12)` | No | — | — | Código postal. |
| `correo` | `varchar(254)` | Sí | — | — | Contacto. |
| `telefono` | `varchar(40)` | Sí | — | — | Contacto. |
| `zona_horaria` | `varchar(64)` | No | — | — | Zona IANA local. |
| `estado` | `varchar(20)` | No | — | — | ACTIVA o DESACTIVADA. |
| `fecha_alta` | `timestamptz` | No | — | — | Creación. |
| `fecha_desactivacion` | `timestamptz` | Sí | — | — | Baja lógica. |

#### `horario_sucursal`

Intervalos semanales regulares de atención y retiro.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_horario_sucursal` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_sucursal` | `bigint` | No | `FK` | `sucursal` | Sucursal. |
| `dia_semana` | `smallint` | No | — | — | 1 a 7, lunes a domingo. |
| `hora_desde` | `time` | No | — | — | Apertura. |
| `hora_hasta` | `time` | No | — | — | Cierre. |
| `activo` | `boolean` | No | — | — | Vigencia. |

#### `excepcion_horario_sucursal`

Feriado, cierre o horario especial de una sucursal.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_excepcion_horario_sucursal` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_sucursal` | `bigint` | No | `FK UK*` | `sucursal` | Sucursal. |
| `fecha` | `date` | No | `UK*` | — | Fecha civil local. |
| `cerrada` | `boolean` | No | — | — | Cierre total. |
| `hora_desde` | `time` | Sí | — | — | Apertura especial. |
| `hora_hasta` | `time` | Sí | — | — | Cierre especial. |
| `motivo` | `varchar(180)` | No | — | — | Explicación. |

#### `pausa_operativa`

Pausa global o de una sucursal que bloquea nuevas cotizaciones y confirmaciones.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_pausa_operativa` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_sucursal` | `bigint` | Sí | `FK` | `sucursal` | Nulo indica pausa global. |
| `id_usuario_inicio` | `bigint` | No | `FK` | `usuario` | Actor que pausa. |
| `id_usuario_fin` | `bigint` | Sí | `FK` | `usuario` | Actor que reanuda. |
| `motivo` | `text` | No | — | — | Causa obligatoria. |
| `estado` | `varchar(16)` | No | — | — | ACTIVA o FINALIZADA. |
| `fecha_inicio` | `timestamptz` | No | — | — | Inicio. |
| `fecha_fin` | `timestamptz` | Sí | — | — | Fin. |

**Restricciones de entidad**

- Como máximo una pausa activa por alcance.

#### `punto_entrega`

Lugar externo donde un empleado entrega directamente el pedido al cliente.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_punto_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo` | `varchar(40)` | No | `UK` | — | Código estable. |
| `nombre` | `varchar(160)` | No | — | — | Nombre visible. |
| `calle` | `varchar(160)` | No | — | — | Dirección. |
| `numero` | `varchar(20)` | No | — | — | Altura. |
| `localidad` | `varchar(120)` | No | — | — | Localidad. |
| `provincia` | `varchar(120)` | No | — | — | Provincia. |
| `codigo_postal` | `varchar(12)` | No | — | — | Código postal. |
| `referencias` | `text` | Sí | — | — | Indicaciones. |
| `estado` | `varchar(20)` | No | — | — | ACTIVO o DESACTIVADO. |

#### `zona_entrega`

Zona global de envío a domicilio compartida por todas las sucursales.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_zona_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo` | `varchar(40)` | No | `UK` | — | Código estable. |
| `nombre` | `varchar(140)` | No | — | — | Nombre visible. |
| `descripcion` | `text` | Sí | — | — | Alcance legible. |
| `estado` | `varchar(20)` | No | — | — | ACTIVA o DESACTIVADA. |

#### `zona_entrega_codigo_postal`

Códigos postales y localidades que delimitan una zona sin mapas.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_zona_entrega_codigo_postal` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_zona_entrega` | `bigint` | No | `FK` | `zona_entrega` | Zona propietaria. |
| `codigo_postal` | `varchar(12)` | No | `UK*` | — | Código postal normalizado. |
| `localidad` | `varchar(120)` | No | `UK*` | — | Localidad. |
| `provincia` | `varchar(120)` | No | `UK*` | — | Provincia. |

**Restricciones de entidad**

- La combinación territorial no puede pertenecer a dos zonas activas.

### 5.4. Configuración, catálogos y reglas

#### `configuracion_version`

Versión completa e inmutable de las decisiones comerciales y operativas.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_configuracion_version` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_version_base` | `bigint` | Sí | `FK` | `configuracion_version` | Versión copiada como origen. |
| `numero_version` | `integer` | No | `UK` | — | Secuencia creciente local. |
| `estado` | `varchar(20)` | No | — | — | EN_PREPARACION, ACTIVA o HISTORICA. |
| `id_usuario_creador` | `bigint` | No | `FK` | `usuario` | Administrador creador. |
| `id_usuario_activador` | `bigint` | Sí | `FK` | `usuario` | Administrador que activa. |
| `fecha_creacion` | `timestamptz` | No | — | — | Inicio de edición. |
| `fecha_activacion` | `timestamptz` | Sí | — | — | Efecto manual inmediato. |
| `observacion` | `text` | Sí | — | — | Motivo o resumen. |

**Restricciones de entidad**

- Antes del alta inicial puede haber cero activas y una en preparación.
- Luego existe exactamente una activa y como máximo una en preparación.

#### `politica_operativa`

Parámetros globales explícitos de una versión, sin mapa clave-valor.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_politica_operativa` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK` | `configuracion_version` | Versión propietaria. |
| `modo_aprobacion` | `varchar(20)` | No | — | — | MANUAL, AUTOMATICA o CONDICIONAL. |
| `modo_asignacion_impresora` | `varchar(20)` | No | — | — | MANUAL o AUTOMATICA. |
| `vigencia_cotizacion_minutos` | `integer` | No | — | — | Duración de cotizaciones automáticas. |
| `retencion_archivos_dias` | `integer` | No | — | — | Plazo posterior al cierre. |
| `plazo_reclamo_dias` | `integer` | No | — | — | Plazo congelado en el pedido. |
| `requiere_revision_preview_ofimatica` | `boolean` | No | — | — | Control ante advertencias de conversión. |

#### `formato`

Catálogo estable de tamaños de impresión.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_formato` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo` | `varchar(40)` | No | `UK` | — | Código estable. |
| `nombre` | `varchar(100)` | No | — | — | Nombre visible. |
| `ancho_mm` | `numeric(8,2)` | No | — | — | Ancho en milímetros. |
| `alto_mm` | `numeric(8,2)` | No | — | — | Alto en milímetros. |
| `activo` | `boolean` | No | — | — | Baja lógica. |

**Restricciones de entidad**

- Las dimensiones no se reescriben tras uso; una variante crea otro formato.

#### `papel`

Catálogo estable de materiales de impresión sin inventario.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_papel` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo` | `varchar(40)` | No | `UK` | — | Código estable. |
| `nombre` | `varchar(120)` | No | — | — | Nombre visible. |
| `gramaje_g_m2` | `numeric(8,2)` | No | — | — | Gramaje nominal. |
| `terminacion_tipo` | `varchar(100)` | No | — | — | Tipo o terminación del material. |
| `activo` | `boolean` | No | — | — | Baja lógica. |

**Restricciones de entidad**

- No representa existencias, compras ni movimientos de stock.

#### `servicio`

Catálogo estable de impresión y terminaciones ofrecibles.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_servicio` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo` | `varchar(50)` | No | `UK` | — | Código estable. |
| `nombre` | `varchar(140)` | No | — | — | Nombre visible. |
| `tipo` | `varchar(20)` | No | — | — | IMPRESION o TERMINACION. |
| `descripcion` | `text` | Sí | — | — | Descripción comercial. |
| `activo` | `boolean` | No | — | — | Baja lógica. |

#### `configuracion_servicio`

Versión de disponibilidad, precio base y tiempo de un servicio.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_configuracion_servicio` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK*` | `configuracion_version` | Versión. |
| `id_servicio` | `bigint` | No | `FK UK*` | `servicio` | Servicio estable. |
| `nombre_visible` | `varchar(140)` | No | — | — | Nombre congelado para la versión. |
| `base_precio` | `varchar(24)` | No | — | — | POR_COPIA, POR_HOJA, POR_CARILLA o FIJO_POR_ITEM. |
| `precio_unitario` | `numeric(19,2)` | No | — | — | Precio ARS no negativo. |
| `preparacion_minutos` | `integer` | No | — | — | Tiempo mínimo para fecha de entrega. |
| `habilitado` | `boolean` | No | — | — | Disponibilidad global. |

**Restricciones de entidad**

- La pareja versión-servicio es única.

#### `compatibilidad_servicio`

Combinaciones de formato y papel admitidas por un servicio de terminación.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_compatibilidad_servicio` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_servicio` | `bigint` | No | `FK UK*` | `configuracion_servicio` | Servicio versionado. |
| `id_formato` | `bigint` | No | `FK UK*` | `formato` | Formato admitido. |
| `id_papel` | `bigint` | No | `FK UK*` | `papel` | Papel admitido. |

**Restricciones de entidad**

- La terna servicio-formato-papel es única.

#### `tarifa_impresion`

Precio versionado de impresión por combinación material.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_tarifa_impresion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK*` | `configuracion_version` | Versión. |
| `id_formato` | `bigint` | No | `FK UK*` | `formato` | Formato. |
| `id_papel` | `bigint` | No | `FK UK*` | `papel` | Papel. |
| `modo_color` | `varchar(16)` | No | `UK*` | — | BLANCO_NEGRO o COLOR. |
| `precio_por_carilla` | `numeric(19,2)` | No | — | — | Precio final ARS. |
| `recargo_doble_faz` | `numeric(19,2)` | No | — | — | Recargo opcional; cero si no aplica. |
| `habilitada` | `boolean` | No | — | — | Disponibilidad de la combinación. |

**Restricciones de entidad**

- La combinación versión-formato-papel-color es única.

#### `sucursal_servicio`

Habilitación de un servicio global según capacidad de una sucursal.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_sucursal_servicio` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK*` | `configuracion_version` | Versión. |
| `id_sucursal` | `bigint` | No | `FK UK*` | `sucursal` | Sucursal. |
| `id_servicio` | `bigint` | No | `FK UK*` | `servicio` | Servicio. |
| `habilitado` | `boolean` | No | — | — | Disponibilidad en la sucursal. |

**Restricciones de entidad**

- La terna versión-sucursal-servicio es única.

#### `configuracion_medio_pago`

Medio de pago habilitado y condición de avance en una versión.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_configuracion_medio_pago` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK*` | `configuracion_version` | Versión. |
| `medio_pago` | `varchar(24)` | No | `UK*` | — | Medio cerrado del sistema. |
| `habilitado` | `boolean` | No | — | — | Disponibilidad para nuevas cotizaciones. |
| `momento_exigencia` | `varchar(24)` | No | — | — | ANTES_PRODUCCION, ANTES_ENTREGA o CUENTA. |
| `id_modulo_requerido` | `bigint` | Sí | `FK` | `modulo` | Módulo necesario para habilitar. |

**Restricciones de entidad**

- La pareja versión-medio es única.

#### `configuracion_modalidad_entrega`

Modalidad de entrega habilitada por versión.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_configuracion_modalidad_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK*` | `configuracion_version` | Versión. |
| `modalidad` | `varchar(28)` | No | `UK*` | — | RETIRO_SUCURSAL, RETIRO_PUNTO_ENTREGA o ENVIO_DOMICILIO. |
| `habilitada` | `boolean` | No | — | — | Disponibilidad global. |

**Restricciones de entidad**

- La pareja versión-modalidad es única.

#### `configuracion_punto_entrega`

Punto externo habilitado para una sucursal, con costo propio.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_configuracion_punto_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK*` | `configuracion_version` | Versión. |
| `id_sucursal` | `bigint` | No | `FK UK*` | `sucursal` | Sucursal de origen. |
| `id_punto_entrega` | `bigint` | No | `FK UK*` | `punto_entrega` | Punto compartido. |
| `costo` | `numeric(19,2)` | No | — | — | Costo ARS; puede ser cero. |
| `habilitado` | `boolean` | No | — | — | Disponibilidad. |

**Restricciones de entidad**

- La terna versión-sucursal-punto es única.

#### `configuracion_zona_entrega`

Costo y disponibilidad global de una zona de envío en una versión.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_configuracion_zona_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK*` | `configuracion_version` | Versión. |
| `id_zona_entrega` | `bigint` | No | `FK UK*` | `zona_entrega` | Zona global. |
| `costo` | `numeric(19,2)` | No | — | — | Costo ARS; puede ser cero. |
| `habilitada` | `boolean` | No | — | — | Disponibilidad. |

**Restricciones de entidad**

- La pareja versión-zona es única.

#### `configuracion_impresora`

Impresora física habilitada y priorizada en una versión.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_configuracion_impresora` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK*` | `configuracion_version` | Versión. |
| `id_impresora` | `bigint` | No | `FK UK*` | `impresora` | Impresora física. |
| `prioridad` | `integer` | No | — | — | Orden preferido. |
| `habilitada` | `boolean` | No | — | — | Participación automática. |

**Restricciones de entidad**

- La pareja versión-impresora es única.

#### `regla_comercial`

Regla estructurada por item para descuento, seña o prepago.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_regla_comercial` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK` | `configuracion_version` | Versión propietaria. |
| `nombre` | `varchar(140)` | No | — | — | Nombre comprensible. |
| `prioridad` | `integer` | No | — | — | Orden estable de evaluación. |
| `habilitada` | `boolean` | No | — | — | Participación. |

#### `condicion_regla`

Condición AND certificada de una regla comercial.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_condicion_regla` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_regla_comercial` | `bigint` | No | `FK` | `regla_comercial` | Regla propietaria. |
| `campo` | `varchar(32)` | No | — | — | Campo permitido del item o cliente. |
| `operador` | `varchar(16)` | No | — | — | Operador certificado. |
| `valor_numero_desde` | `numeric(19,4)` | Sí | — | — | Valor o límite inferior. |
| `valor_numero_hasta` | `numeric(19,4)` | Sí | — | — | Límite superior de ENTRE. |
| `valor_texto` | `varchar(120)` | Sí | — | — | Valor categórico permitido. |
| `valor_booleano` | `boolean` | Sí | — | — | Valor booleano. |

**Restricciones de entidad**

- Exactamente la familia de valores compatible con campo y operador debe estar informada.

#### `accion_regla`

Única consecuencia certificada de una regla comercial.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_accion_regla` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_regla_comercial` | `bigint` | No | `FK UK` | `regla_comercial` | Regla propietaria. |
| `tipo_accion` | `varchar(28)` | No | — | — | SENA_PORCENTAJE, SENA_FIJA, PREPAGO_TOTAL o DESCUENTO_PORCENTAJE. |
| `valor` | `numeric(19,4)` | Sí | — | — | Porcentaje o importe según tipo. |

#### `regla_aprobacion`

Caso administrable que habilita aprobación automática dentro del modo CONDICIONAL.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_regla_aprobacion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK` | `configuracion_version` | Versión propietaria. |
| `nombre` | `varchar(140)` | No | — | — | Nombre comprensible. |
| `prioridad` | `integer` | No | — | — | Orden estable de evaluación. |
| `habilitada` | `boolean` | No | — | — | Participación. |

**Restricciones de entidad**

- Una regla coincidente solo habilita el intento automático; los controles técnicos todavía pueden derivar a revisión manual.

#### `condicion_aprobacion`

Condición AND certificada de una regla de aprobación condicional.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_condicion_aprobacion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_regla_aprobacion` | `bigint` | No | `FK` | `regla_aprobacion` | Regla propietaria. |
| `campo` | `varchar(32)` | No | — | — | Campo permitido de pedido, item, cliente o pago. |
| `operador` | `varchar(16)` | No | — | — | Operador certificado. |
| `valor_numero_desde` | `numeric(19,4)` | Sí | — | — | Valor o límite inferior. |
| `valor_numero_hasta` | `numeric(19,4)` | Sí | — | — | Límite superior de ENTRE. |
| `valor_texto` | `varchar(120)` | Sí | — | — | Valor categórico permitido. |
| `valor_booleano` | `boolean` | Sí | — | — | Valor booleano. |

**Restricciones de entidad**

- Exactamente una familia de valores compatible con campo y operador debe estar informada.
- Las condiciones de una regla se combinan con AND; reglas distintas expresan alternativas.

#### `regla_asignacion`

Regla ordenada para seleccionar una impresora compatible.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_regla_asignacion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK` | `configuracion_version` | Versión propietaria. |
| `nombre` | `varchar(140)` | No | — | — | Nombre comprensible. |
| `estrategia` | `varchar(28)` | No | — | — | LISTA_PRIORIDAD o MENOR_COLA. |
| `prioridad` | `integer` | No | — | — | Orden de evaluación. |
| `habilitada` | `boolean` | No | — | — | Participación. |

#### `condicion_asignacion`

Condición certificada por atributos del item para una regla de impresora.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_condicion_asignacion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_regla_asignacion` | `bigint` | No | `FK` | `regla_asignacion` | Regla propietaria. |
| `campo` | `varchar(32)` | No | — | — | Formato, papel, color, faz o servicio. |
| `operador` | `varchar(16)` | No | — | — | ES, NO_ES, PRESENTE o AUSENTE. |
| `valor_texto` | `varchar(120)` | Sí | — | — | Código o valor comparado. |
| `valor_booleano` | `boolean` | Sí | — | — | Valor booleano. |

#### `regla_asignacion_impresora`

Lista ordenada de impresoras candidatas para una regla.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_regla_asignacion_impresora` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_regla_asignacion` | `bigint` | No | `FK UK*` | `regla_asignacion` | Regla. |
| `id_impresora` | `bigint` | No | `FK UK*` | `impresora` | Impresora candidata. |
| `orden` | `integer` | No | `UK*` | — | Orden dentro de LISTA_PRIORIDAD. |

**Restricciones de entidad**

- La pareja regla-impresora es única.
- La pareja regla-orden es única.

### 5.5. Cotizaciones, pedidos y correcciones

#### `cotizacion`

Oferta persistida que congela configuración, importes, entrega y condiciones.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_cotizacion` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco para API. |
| `id_usuario_creador` | `bigint` | No | `FK` | `usuario` | Usuario que cotiza. |
| `id_empresa_cliente` | `bigint` | Sí | `FK` | `empresa_cliente` | Empresa propietaria si corresponde. |
| `id_sucursal` | `bigint` | No | `FK` | `sucursal` | Sucursal elegida. |
| `id_configuracion_version` | `bigint` | No | `FK` | `configuracion_version` | Versión capturada. |
| `id_cotizacion_reemplazada` | `bigint` | Sí | `FK` | `cotizacion` | Cotización anterior cancelada. |
| `estado` | `varchar(16)` | No | — | — | BORRADOR, VIGENTE, CONFIRMADA, EXPIRADA o CANCELADA. |
| `es_manual` | `boolean` | No | — | — | Creada por un usuario interno. |
| `motivo_manual` | `text` | Sí | — | — | Justificación de excepción. |
| `subtotal` | `numeric(19,2)` | No | — | — | Suma antes de entrega. |
| `descuento_total` | `numeric(19,2)` | No | — | — | Descuento congelado. |
| `costo_entrega` | `numeric(19,2)` | No | — | — | Puede ser cero. |
| `modalidad_entrega` | `varchar(28)` | No | — | — | Modalidad congelada. |
| `id_franja_entrega` | `bigint` | Sí | `FK` | `franja_entrega` | Franja ofrecida, todavía sin reservar. |
| `id_punto_entrega` | `bigint` | Sí | `FK` | `punto_entrega` | Punto elegido si corresponde. |
| `id_zona_entrega` | `bigint` | Sí | `FK` | `zona_entrega` | Zona elegida si corresponde. |
| `destino_entrega_snapshot` | `text` | Sí | — | — | Dirección o ubicación mostrada al cotizar. |
| `total` | `numeric(19,2)` | No | — | — | Total final ARS. |
| `sena_requerida` | `numeric(19,2)` | No | — | — | Importe calculado. |
| `medio_pago_preferido` | `varchar(24)` | No | — | — | Selección inicial. |
| `generada_en` | `timestamptz` | No | — | — | Creación. |
| `vigente_hasta` | `timestamptz` | No | — | — | Único vencimiento. |
| `confirmada_en` | `timestamptz` | Sí | — | — | Confirmación única. |

#### `cotizacion_item`

Item homogéneo cotizado a partir de un único archivo.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_cotizacion_item` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_cotizacion` | `bigint` | No | `FK UK*` | `cotizacion` | Cotización propietaria. |
| `orden` | `integer` | No | `UK*` | — | Posición dentro de la cotización. |
| `id_formato` | `bigint` | No | `FK` | `formato` | Formato seleccionado. |
| `id_papel` | `bigint` | No | `FK` | `papel` | Papel seleccionado. |
| `cantidad_paginas` | `integer` | No | — | — | Páginas detectadas. |
| `cantidad_carillas` | `integer` | No | — | — | Carillas cobradas. |
| `cantidad_hojas` | `integer` | No | — | — | Hojas físicas estimadas. |
| `cantidad_copias` | `integer` | No | — | — | Copias solicitadas. |
| `modo_color` | `varchar(16)` | No | — | — | BLANCO_NEGRO o COLOR. |
| `doble_faz` | `boolean` | No | — | — | Impresión dúplex. |
| `precio_base_configurado` | `numeric(19,2)` | No | — | — | Precio calculado por la configuración. |
| `precio_base_aplicado` | `numeric(19,2)` | No | — | — | Precio usado, igual salvo excepción autorizada. |
| `porcentaje_descuento_configurado` | `numeric(7,4)` | No | — | — | Mayor descuento de reglas. |
| `porcentaje_descuento_aplicado` | `numeric(7,4)` | No | — | — | Descuento usado, igual salvo excepción. |
| `id_usuario_ajuste` | `bigint` | Sí | `FK` | `usuario` | Interno que autoriza una excepción de precio. |
| `motivo_ajuste` | `text` | Sí | — | — | Obligatorio si valor configurado y aplicado difieren. |
| `subtotal` | `numeric(19,2)` | No | — | — | Subtotal redondeado del item. |
| `sena_requerida` | `numeric(19,2)` | No | — | — | Mayor requisito del item. |
| `preparacion_minutos` | `integer` | No | — | — | Tiempo congelado. |

**Restricciones de entidad**

- La pareja cotización-orden es única.

#### `cotizacion_item_servicio`

Servicio y precio congelados dentro de un item cotizado.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_cotizacion_item_servicio` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_cotizacion_item` | `bigint` | No | `FK UK*` | `cotizacion_item` | Item. |
| `id_servicio` | `bigint` | No | `FK UK*` | `servicio` | Servicio estable. |
| `tipo_servicio` | `varchar(20)` | No | — | — | IMPRESION o TERMINACION congelado. |
| `base_precio` | `varchar(24)` | No | — | — | Base usada. |
| `cantidad` | `numeric(19,4)` | No | — | — | Unidades cobradas. |
| `precio_unitario` | `numeric(19,2)` | No | — | — | Precio aplicado. |
| `subtotal` | `numeric(19,2)` | No | — | — | Importe redondeado. |

**Restricciones de entidad**

- Exactamente un servicio IMPRESION y cero o más TERMINACION por item.

#### `cotizacion_regla_aplicada`

Evidencia reproducible de la regla ganadora aplicada a un item.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_cotizacion_regla_aplicada` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_cotizacion_item` | `bigint` | No | `FK UK*` | `cotizacion_item` | Item afectado. |
| `id_regla_comercial` | `bigint` | No | `FK UK*` | `regla_comercial` | Regla evaluada. |
| `tipo_accion` | `varchar(28)` | No | — | — | Acción congelada. |
| `valor_accion` | `numeric(19,4)` | Sí | — | — | Valor congelado. |
| `importe_resultante` | `numeric(19,2)` | No | — | — | Efecto monetario. |
| `aplicada_en` | `timestamptz` | No | — | — | Evaluación. |

**Restricciones de entidad**

- La pareja item-regla comercial es única.

#### `pedido`

Compromiso comercial confirmado y centro del flujo operativo.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_pedido` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco para API. |
| `numero_pedido` | `bigint` | No | `UK` | — | Número humano secuencial. |
| `id_cotizacion` | `bigint` | No | `FK UK` | `cotizacion` | Cotización confirmada una vez. |
| `id_usuario_creador` | `bigint` | No | `FK` | `usuario` | Persona que confirmó. |
| `id_empresa_cliente` | `bigint` | Sí | `FK` | `empresa_cliente` | Propietario empresarial. |
| `id_sucursal` | `bigint` | No | `FK` | `sucursal` | Sucursal responsable. |
| `id_configuracion_version` | `bigint` | No | `FK` | `configuracion_version` | Versión capturada. |
| `estado_interno` | `varchar(28)` | No | — | — | Estado operativo fijo. |
| `modo_aprobacion_aplicado` | `varchar(20)` | No | — | — | MANUAL, AUTOMATICA o CONDICIONAL congelado. |
| `id_usuario_aprobador` | `bigint` | Sí | `FK` | `usuario` | Interno que decide; nulo en aprobación automática. |
| `aprobado_en` | `timestamptz` | Sí | — | — | Momento de aprobación. |
| `subtotal` | `numeric(19,2)` | No | — | — | Suma congelada. |
| `descuento_total` | `numeric(19,2)` | No | — | — | Descuento congelado. |
| `costo_entrega` | `numeric(19,2)` | No | — | — | Costo congelado. |
| `modalidad_entrega` | `varchar(28)` | No | — | — | Modalidad congelada. |
| `id_punto_entrega` | `bigint` | Sí | `FK` | `punto_entrega` | Punto externo elegido. |
| `id_zona_entrega` | `bigint` | Sí | `FK` | `zona_entrega` | Zona de envío elegida. |
| `destino_entrega_snapshot` | `text` | Sí | — | — | Destino reproducible al confirmar. |
| `total` | `numeric(19,2)` | No | — | — | Total ARS. |
| `medio_pago_preferido` | `varchar(24)` | No | — | — | Preferencia inicial. |
| `plazo_reclamo_dias` | `integer` | No | — | — | Plazo congelado. |
| `confirmado_en` | `timestamptz` | No | — | — | Nacimiento del pedido. |
| `produccion_iniciada_en` | `timestamptz` | Sí | — | — | Límite de edición directa. |
| `entregado_en` | `timestamptz` | Sí | — | — | Entrega completa. |
| `cerrado_en` | `timestamptz` | Sí | — | — | Cierre operativo. |

#### `pedido_item`

Snapshot operativo de un item confirmado.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_pedido_item` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido` | `bigint` | No | `FK UK*` | `pedido` | Pedido propietario. |
| `id_cotizacion_item` | `bigint` | No | `FK UK` | `cotizacion_item` | Item cotizado de origen. |
| `orden` | `integer` | No | `UK*` | — | Posición. |
| `formato_codigo` | `varchar(40)` | No | — | — | Snapshot de formato. |
| `formato_ancho_mm` | `numeric(8,2)` | No | — | — | Ancho congelado. |
| `formato_alto_mm` | `numeric(8,2)` | No | — | — | Alto congelado. |
| `papel_codigo` | `varchar(40)` | No | — | — | Snapshot de papel. |
| `papel_gramaje_g_m2` | `numeric(8,2)` | No | — | — | Gramaje congelado. |
| `cantidad_paginas` | `integer` | No | — | — | Páginas aceptadas. |
| `cantidad_carillas` | `integer` | No | — | — | Carillas cobradas. |
| `cantidad_hojas` | `integer` | No | — | — | Hojas físicas. |
| `cantidad_copias` | `integer` | No | — | — | Copias. |
| `modo_color` | `varchar(16)` | No | — | — | BLANCO_NEGRO o COLOR. |
| `doble_faz` | `boolean` | No | — | — | Dúplex. |
| `precio_base` | `numeric(19,2)` | No | — | — | Importe previo. |
| `porcentaje_descuento` | `numeric(7,4)` | No | — | — | Descuento. |
| `subtotal` | `numeric(19,2)` | No | — | — | Resultado del item. |

#### `pedido_item_servicio`

Snapshot de cada servicio incluido en un item confirmado.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_pedido_item_servicio` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido_item` | `bigint` | No | `FK UK*` | `pedido_item` | Item propietario. |
| `id_servicio` | `bigint` | No | `FK UK*` | `servicio` | Servicio estable para trazabilidad. |
| `servicio_codigo` | `varchar(50)` | No | — | — | Código congelado. |
| `servicio_nombre` | `varchar(140)` | No | — | — | Nombre congelado. |
| `tipo_servicio` | `varchar(20)` | No | — | — | Tipo congelado. |
| `base_precio` | `varchar(24)` | No | — | — | Base congelada. |
| `cantidad` | `numeric(19,4)` | No | — | — | Cantidad. |
| `precio_unitario` | `numeric(19,2)` | No | — | — | Precio aplicado. |
| `subtotal` | `numeric(19,2)` | No | — | — | Importe. |

**Restricciones de entidad**

- La pareja item-servicio es única.

#### `historial_estado_pedido`

Fuente de verdad append-only de las transiciones del pedido.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_historial_estado_pedido` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido` | `bigint` | No | `FK` | `pedido` | Pedido. |
| `id_usuario_actor` | `bigint` | Sí | `FK` | `usuario` | Actor humano; nulo para sistema. |
| `actor_tipo` | `varchar(24)` | No | — | — | USUARIO o SISTEMA. |
| `estado_anterior` | `varchar(28)` | Sí | — | — | Nulo en creación. |
| `estado_nuevo` | `varchar(28)` | No | — | — | Estado resultante. |
| `motivo` | `text` | Sí | — | — | Obligatorio para decisiones sensibles. |
| `fecha` | `timestamptz` | No | — | — | Momento de transición. |

#### `observacion_interna`

Nota append-only visible solamente para personal autorizado.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_observacion_interna` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido` | `bigint` | No | `FK` | `pedido` | Pedido comentado. |
| `id_usuario_autor` | `bigint` | No | `FK` | `usuario` | Empleado o administrador. |
| `tipo` | `varchar(32)` | Sí | — | — | Clasificación operativa. |
| `importancia` | `varchar(16)` | Sí | — | — | INFORMATIVA, RELEVANTE o CRITICA. |
| `texto` | `text` | No | — | — | Contenido interno. |
| `fecha` | `timestamptz` | No | — | — | Creación. |

#### `solicitud_correccion`

Pedido formal de corrección visible al cliente antes de producción.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_solicitud_correccion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido` | `bigint` | No | `FK` | `pedido` | Pedido detenido. |
| `id_usuario_solicitante` | `bigint` | No | `FK` | `usuario` | Usuario interno. |
| `tipo` | `varchar(32)` | No | — | — | ARCHIVO, DATOS, ENTREGA, CANTIDAD u OTRO. |
| `motivo` | `text` | No | — | — | Motivo interno completo. |
| `mensaje_cliente` | `text` | No | — | — | Indicación visible. |
| `observacion_interna` | `text` | Sí | — | — | Detalle no visible. |
| `estado` | `varchar(16)` | No | — | — | PENDIENTE, RESPONDIDA, CERRADA o CANCELADA. |
| `fecha_solicitud` | `timestamptz` | No | — | — | Inicio. |
| `fecha_cierre` | `timestamptz` | Sí | — | — | Resolución. |

**Restricciones de entidad**

- Existe como máximo una solicitud activa por pedido.

#### `solicitud_correccion_item`

Items alcanzados por una solicitud de corrección.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_solicitud_correccion_item` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_solicitud_correccion` | `bigint` | No | `FK UK*` | `solicitud_correccion` | Solicitud. |
| `id_pedido_item` | `bigint` | No | `FK UK*` | `pedido_item` | Item afectado. |

**Restricciones de entidad**

- La pareja solicitud-item es única.

#### `solicitud_correccion_archivo`

Archivos específicos observados por una solicitud.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_solicitud_correccion_archivo` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_solicitud_correccion` | `bigint` | No | `FK UK*` | `solicitud_correccion` | Solicitud. |
| `id_archivo_almacenado` | `bigint` | No | `FK UK*` | `archivo_almacenado` | Archivo observado. |

**Restricciones de entidad**

- La pareja solicitud-archivo es única.

#### `respuesta_correccion`

Respuesta inmutable del cliente a una solicitud.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_respuesta_correccion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_solicitud_correccion` | `bigint` | No | `FK` | `solicitud_correccion` | Solicitud respondida. |
| `id_usuario_autor` | `bigint` | No | `FK` | `usuario` | Cliente que responde. |
| `mensaje` | `text` | Sí | — | — | Aclaración opcional. |
| `fecha` | `timestamptz` | No | — | — | Momento de respuesta. |

#### `solicitud_cancelacion`

Solicitud del cliente cuando la producción ya impide cancelar directamente.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_solicitud_cancelacion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido` | `bigint` | No | `FK` | `pedido` | Pedido cuya cancelación se evalúa. |
| `id_usuario_solicitante` | `bigint` | No | `FK` | `usuario` | Cliente autorizado que solicita. |
| `id_usuario_resolvedor` | `bigint` | Sí | `FK` | `usuario` | Interno que decide. |
| `motivo_solicitud` | `text` | No | — | — | Fundamento del cliente. |
| `estado` | `varchar(16)` | No | — | — | PENDIENTE, APROBADA, RECHAZADA o CANCELADA. |
| `motivo_resolucion` | `text` | Sí | — | — | Obligatorio al resolver. |
| `fecha_solicitud` | `timestamptz` | No | — | — | Inicio. |
| `fecha_resolucion` | `timestamptz` | Sí | — | — | Decisión. |

**Restricciones de entidad**

- Existe como máximo una solicitud PENDIENTE por pedido.
- Antes de iniciar producción, una cancelación autorizada no necesita esta entidad.

### 5.6. Archivos, conversión y validación

#### `archivo_almacenado`

Metadatos persistentes de un objeto privado cuyo contenido vive fuera de PostgreSQL.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_archivo_almacenado` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco para accesos autorizados. |
| `id_usuario_cargador` | `bigint` | Sí | `FK` | `usuario` | Usuario que cargó el archivo; nulo si lo generó el sistema. |
| `finalidad` | `varchar(32)` | No | — | — | TRABAJO, DOCUMENTO_COBRO, COMPROBANTE, EVIDENCIA o SISTEMA. |
| `clave_storage` | `varchar(500)` | No | `UK` | — | Clave privada, nunca URL pública permanente. |
| `nombre_original` | `varchar(255)` | No | — | — | Nombre informado al cargar o generar. |
| `extension` | `varchar(12)` | No | — | — | Extensión normalizada. |
| `mime_declarado` | `varchar(120)` | Sí | — | — | MIME del cliente, no confiable. |
| `mime_detectado` | `varchar(120)` | No | — | — | MIME validado por contenido. |
| `cantidad_bytes` | `bigint` | No | — | — | Tamaño no negativo. |
| `sha256` | `char(64)` | No | — | — | Huella del contenido almacenado. |
| `estado` | `varchar(24)` | No | — | — | CARGADO, VALIDANDO, VALIDO, REQUIERE_REVISION, RECHAZADO o PURGADO. |
| `fecha_creacion` | `timestamptz` | No | — | — | Alta de metadatos. |
| `fecha_purga_programada` | `timestamptz` | Sí | — | — | Fecha calculada al finalizar el caso de negocio. |
| `fecha_purga` | `timestamptz` | Sí | — | — | Borrado efectivo del objeto. |
| `motivo_purga` | `varchar(180)` | Sí | — | — | Razón de eliminación del contenido. |

**Restricciones de entidad**

- PURGADO conserva metadatos y hash, pero la clave ya no debe resolver a un objeto.
- PDF, DOCX, ODT, TXT, JPEG y PNG conforman la lista global permitida.

#### `archivo_trabajo`

Asocia un original o derivado a exactamente un item de cotización o de pedido.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_archivo_trabajo` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_archivo_almacenado` | `bigint` | No | `FK UK` | `archivo_almacenado` | Objeto privado asociado una sola vez. |
| `id_cotizacion_item` | `bigint` | Sí | `FK` | `cotizacion_item` | Propietario temporal antes de confirmar. |
| `id_pedido_item` | `bigint` | Sí | `FK` | `pedido_item` | Propietario definitivo después de confirmar. |
| `id_archivo_trabajo_reemplazado` | `bigint` | Sí | `FK` | `archivo_trabajo` | Versión anterior que no se sobrescribe. |
| `rol_archivo` | `varchar(28)` | No | — | — | ORIGINAL, IMPRESION_CANONICO o VISTA_PREVIA. |
| `aceptado_por_cliente` | `boolean` | Sí | — | — | Nulo si no es una vista previa; decisión en otro caso. |
| `fecha_aceptacion` | `timestamptz` | Sí | — | — | Momento de aceptación. |
| `activo` | `boolean` | No | — | — | Versión vigente para el item. |
| `fecha_asociacion` | `timestamptz` | No | — | — | Alta o transferencia atómica. |

**Restricciones de entidad**

- Exactamente uno entre cotización_item y pedido_item debe ser no nulo.
- Cada item tiene un único ORIGINAL activo; DOCX y ODT además tienen un único IMPRESION_CANONICO PDF activo.

#### `validacion_archivo`

Resultado inmutable de inspeccionar tipo real, seguridad, legibilidad y renderizado.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_validacion_archivo` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_archivo_almacenado` | `bigint` | No | `FK` | `archivo_almacenado` | Archivo inspeccionado. |
| `tipo_validacion` | `varchar(28)` | No | — | — | TIPO_REAL, SEGURIDAD, ESTRUCTURA, RENDERIZADO o PREIMPRESION. |
| `resultado` | `varchar(24)` | No | — | — | ACEPTADO, ADVERTENCIA o RECHAZADO. |
| `motor` | `varchar(100)` | No | — | — | Herramienta ejecutada. |
| `version_motor` | `varchar(60)` | No | — | — | Versión reproducible. |
| `cantidad_paginas` | `integer` | Sí | — | — | Páginas detectadas cuando aplica. |
| `archivo_cifrado` | `boolean` | No | — | — | Detección de cifrado o contraseña. |
| `malware_detectado` | `boolean` | No | — | — | Resultado del escaneo. |
| `render_completo` | `boolean` | Sí | — | — | Todas las páginas pudieron renderizarse. |
| `codigo_resultado` | `varchar(80)` | Sí | — | — | Código técnico estable. |
| `detalle_sanitizado` | `jsonb` | Sí | — | — | Advertencias estructuradas sin contenido del archivo. |
| `fecha` | `timestamptz` | No | — | — | Ejecución. |

**Restricciones de entidad**

- Cifrado, malware, corrupción o contenido activo inseguro producen rechazo.

#### `conversion_archivo`

Conversión reproducible de DOCX u ODT a PDF canónico para cálculo, vista previa y CUPS.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_conversion_archivo` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_archivo_origen` | `bigint` | No | `FK` | `archivo_almacenado` | DOCX u ODT original. |
| `id_archivo_resultado` | `bigint` | Sí | `FK UK` | `archivo_almacenado` | PDF canónico si finalizó correctamente. |
| `motor` | `varchar(100)` | No | — | — | Conversor del servidor. |
| `version_motor` | `varchar(60)` | No | — | — | Versión reproducible. |
| `estado` | `varchar(20)` | No | — | — | PENDIENTE, COMPLETADA, ADVERTENCIA o FALLIDA. |
| `fuentes_sustituidas` | `boolean` | No | — | — | Exige revisión si es verdadero. |
| `objetos_incompatibles` | `boolean` | No | — | — | Exige revisión si es verdadero. |
| `detalle_sanitizado` | `jsonb` | Sí | — | — | Advertencias del motor. |
| `fecha_inicio` | `timestamptz` | No | — | — | Inicio. |
| `fecha_fin` | `timestamptz` | Sí | — | — | Fin. |

**Restricciones de entidad**

- El resultado exitoso es PDF y la conversión identifica de forma única su archivo de origen y resultado.

### 5.7. Agentes, impresoras y producción

#### `agente_impresion`

Proceso Linux o Raspberry Pi de una sucursal que controla una o varias impresoras.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_agente_impresion` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco del agente. |
| `id_sucursal` | `bigint` | No | `FK` | `sucursal` | Sucursal donde opera. |
| `nombre` | `varchar(120)` | No | — | — | Nombre visible. |
| `huella_equipo` | `char(64)` | No | `UK` | — | Huella estable del host autorizado. |
| `version_agente` | `varchar(60)` | Sí | — | — | Última versión reportada. |
| `estado` | `varchar(20)` | No | — | — | PENDIENTE, ACTIVO, BLOQUEADO o RETIRADO. |
| `ultima_conexion` | `timestamptz` | Sí | — | — | Último contacto, no cada heartbeat. |
| `fecha_alta` | `timestamptz` | No | — | — | Registro. |
| `fecha_baja` | `timestamptz` | Sí | — | — | Retiro lógico. |

#### `credencial_agente`

Credencial única hasheada, rotativa y revocable de un agente.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_credencial_agente` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_agente_impresion` | `bigint` | No | `FK` | `agente_impresion` | Agente autenticado. |
| `identificador` | `uuid` | No | `UK` | — | Parte pública de la credencial. |
| `hash_secreto` | `varchar(255)` | No | — | — | Secreto nunca almacenado en plano. |
| `fecha_emision` | `timestamptz` | No | — | — | Creación. |
| `fecha_vencimiento` | `timestamptz` | Sí | — | — | Expiración opcional. |
| `ultimo_uso` | `timestamptz` | Sí | — | — | Último uso correcto. |
| `fecha_revocacion` | `timestamptz` | Sí | — | — | Revocación inmediata. |

**Restricciones de entidad**

- Como máximo una credencial vigente por agente.

#### `impresora`

Equipo físico estable, independiente del versionado de configuración.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_impresora` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco. |
| `id_agente_impresion` | `bigint` | No | `FK UK*` | `agente_impresion` | Agente que la controla. |
| `nombre` | `varchar(140)` | No | — | — | Nombre visible. |
| `cola_cups` | `varchar(180)` | No | `UK*` | — | Nombre de cola dentro del agente. |
| `fabricante` | `varchar(120)` | Sí | — | — | Dato detectado o confirmado. |
| `modelo` | `varchar(120)` | Sí | — | — | Dato detectado o confirmado. |
| `numero_serie` | `varchar(160)` | Sí | `UK` | — | Serie física cuando existe. |
| `tecnologia` | `varchar(40)` | No | — | — | LÁSER, INYECCION u otra categoría controlada. |
| `admite_color` | `boolean` | No | — | — | Capacidad confirmada. |
| `admite_doble_faz` | `boolean` | No | — | — | Capacidad confirmada. |
| `estado_operativo` | `varchar(28)` | No | — | — | ACTIVA, MANTENIMIENTO o FUERA_DE_SERVICIO. |
| `ultima_observacion_online` | `timestamptz` | Sí | — | — | Base para conectividad derivada. |
| `fecha_alta` | `timestamptz` | No | — | — | Registro. |
| `fecha_baja` | `timestamptz` | Sí | — | — | Retiro lógico. |

**Restricciones de entidad**

- La cola CUPS es única dentro del agente de impresión.
- La sucursal se deriva del agente propietario.
- ONLINE u OFFLINE se deriva, no es estado operativo persistido.

#### `impresora_formato`

Formato confirmado como soportado por una impresora.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_impresora_formato` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_impresora` | `bigint` | No | `FK UK*` | `impresora` | Impresora. |
| `id_formato` | `bigint` | No | `FK UK*` | `formato` | Formato. |
| `origen` | `varchar(16)` | No | — | — | DETECTADO o MANUAL. |
| `confirmado` | `boolean` | No | — | — | Confirmación administrativa. |
| `fecha_confirmacion` | `timestamptz` | Sí | — | — | Momento de confirmación. |

**Restricciones de entidad**

- La pareja impresora-formato es única.

#### `impresora_servicio`

Servicio técnico confirmado como realizable por una impresora.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_impresora_servicio` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_impresora` | `bigint` | No | `FK UK*` | `impresora` | Impresora. |
| `id_servicio` | `bigint` | No | `FK UK*` | `servicio` | Servicio. |
| `origen` | `varchar(16)` | No | — | — | DETECTADO o MANUAL. |
| `confirmado` | `boolean` | No | — | — | Confirmación administrativa. |
| `fecha_confirmacion` | `timestamptz` | Sí | — | — | Momento de confirmación. |

**Restricciones de entidad**

- La pareja impresora-servicio es única.

#### `historial_estado_impresora`

Cambios auditables del estado operativo de una impresora.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_historial_estado_impresora` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_impresora` | `bigint` | No | `FK` | `impresora` | Impresora. |
| `id_usuario_actor` | `bigint` | Sí | `FK` | `usuario` | Actor humano; nulo para agente o sistema. |
| `actor_tipo` | `varchar(24)` | No | — | — | USUARIO, SISTEMA o AGENTE_IMPRESION. |
| `estado_anterior` | `varchar(28)` | Sí | — | — | Nulo en alta. |
| `estado_nuevo` | `varchar(28)` | No | — | — | Estado resultante. |
| `motivo` | `text` | No | — | — | Justificación. |
| `fecha` | `timestamptz` | No | — | — | Transición. |

#### `trabajo_impresion`

Intento técnico completo de producir un item sin dividir sus copias.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_trabajo_impresion` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco. |
| `id_pedido_item` | `bigint` | No | `FK` | `pedido_item` | Item completo a producir. |
| `id_trabajo_origen` | `bigint` | Sí | `FK` | `trabajo_impresion` | Trabajo previo que origina una reimpresión. |
| `id_impresora` | `bigint` | Sí | `FK` | `impresora` | Opcional en flujo manual. |
| `id_agente_impresion` | `bigint` | Sí | `FK` | `agente_impresion` | Opcional en flujo manual. |
| `id_usuario_asignador` | `bigint` | Sí | `FK` | `usuario` | Interno que asigna o exceptúa. |
| `modo` | `varchar(20)` | No | — | — | MANUAL o AUTOMATIZADO. |
| `tipo` | `varchar(32)` | No | — | — | NORMAL, REIMPRESION_CALIDAD o REIMPRESION_RECLAMO. |
| `estado` | `varchar(32)` | No | — | — | Estado técnico fijo. |
| `motivo_excepcion` | `text` | Sí | — | — | Obligatorio fuera de impresoras versionadas. |
| `identificador_cups` | `varchar(160)` | Sí | — | — | Job aceptado por CUPS. |
| `token_lease` | `uuid` | Sí | `UK` | — | Claim atómico temporal. |
| `lease_hasta` | `timestamptz` | Sí | — | — | Vencimiento del claim previo a CUPS. |
| `intentos` | `integer` | No | — | — | Cantidad de envíos técnicos. |
| `fecha_creacion` | `timestamptz` | No | — | — | Alta. |
| `fecha_asignacion` | `timestamptz` | Sí | — | — | Asignación. |
| `fecha_inicio` | `timestamptz` | Sí | — | — | Inicio. |
| `fecha_fin` | `timestamptz` | Sí | — | — | Finalización. |

**Restricciones de entidad**

- Existe como máximo un trabajo activo por item.
- Después de aceptación por CUPS, un reintento automático queda prohibido.

#### `historial_trabajo_impresion`

Transiciones append-only de un trabajo de impresión.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_historial_trabajo_impresion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_trabajo_impresion` | `bigint` | No | `FK` | `trabajo_impresion` | Trabajo. |
| `id_usuario_actor` | `bigint` | Sí | `FK` | `usuario` | Actor humano si existe. |
| `id_agente_actor` | `bigint` | Sí | `FK` | `agente_impresion` | Agente actor si existe. |
| `actor_tipo` | `varchar(24)` | No | — | — | USUARIO, SISTEMA, AGENTE_IMPRESION o CUPS. |
| `estado_anterior` | `varchar(32)` | Sí | — | — | Nulo en alta. |
| `estado_nuevo` | `varchar(32)` | No | — | — | Estado resultante. |
| `codigo_resultado` | `varchar(80)` | Sí | — | — | Resultado técnico sanitizado. |
| `motivo` | `text` | Sí | — | — | Justificación humana. |
| `fecha` | `timestamptz` | No | — | — | Transición. |

**Restricciones de entidad**

- A lo sumo uno entre usuario y agente actor es no nulo para cada tipo humano o técnico.

#### `control_calidad`

Revisión humana obligatoria posterior a cada trabajo completado.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_control_calidad` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_trabajo_impresion` | `bigint` | No | `FK UK` | `trabajo_impresion` | Trabajo revisado una vez. |
| `id_usuario_inspector` | `bigint` | No | `FK` | `usuario` | Empleado o administrador. |
| `resultado` | `varchar(28)` | No | — | — | APROBADO, REIMPRESION_REQUERIDA o INCIDENCIA. |
| `motivo` | `text` | Sí | — | — | Obligatorio salvo aprobación simple. |
| `fecha` | `timestamptz` | No | — | — | Control. |

**Restricciones de entidad**

- LISTO_PARA_ENTREGA exige que la última revisión de cada item esté aprobada.

### 5.8. Pagos, cuenta corriente y documentos

#### `intento_pago`

Operación solicitada que todavía no constituye dinero confirmado.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_intento_pago` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco para API y callbacks. |
| `id_usuario_solicitante` | `bigint` | No | `FK` | `usuario` | Usuario que inicia o informa. |
| `id_usuario_pagador` | `bigint` | Sí | `FK` | `usuario` | Pagador personal. |
| `id_empresa_pagadora` | `bigint` | Sí | `FK` | `empresa_cliente` | Pagador empresarial. |
| `id_sucursal_origen` | `bigint` | Sí | `FK` | `sucursal` | Sucursal de origen de la operación. |
| `id_integracion_externa` | `bigint` | Sí | `FK UK*` | `integracion_externa` | Proveedor digital cuando corresponde. |
| `destino` | `varchar(32)` | No | — | — | PAGO_PEDIDOS o PAGO_CUENTA_CORRIENTE. |
| `medio_pago` | `varchar(24)` | No | — | — | EFECTIVO, TRANSFERENCIA, MERCADO_PAGO o MODO. |
| `importe_solicitado` | `numeric(19,2)` | No | — | — | Importe ARS positivo. |
| `identificador_externo` | `varchar(190)` | Sí | `UK*` | — | ID idempotente del proveedor. |
| `estado` | `varchar(24)` | No | — | — | INICIADO, PENDIENTE, CONFIRMADO, CANCELADO, EXPIRADO o FALLIDO. |
| `fecha_creacion` | `timestamptz` | No | — | — | Inicio. |
| `fecha_vencimiento` | `timestamptz` | Sí | — | — | Expiración técnica. |
| `fecha_resolucion` | `timestamptz` | Sí | — | — | Confirmación o cierre. |
| `codigo_resultado` | `varchar(80)` | Sí | — | — | Resultado sanitizado. |

**Restricciones de entidad**

- Exactamente uno entre usuario y empresa pagadora debe ser no nulo.
- CUENTA_CORRIENTE no es un intento de pago: financia el pedido mediante un cargo.
- El identificador externo es único por integración cuando el proveedor lo informa.

#### `pago`

Hecho financiero inmutable nacido únicamente de un intento confirmado.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_pago` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco. |
| `id_intento_pago` | `bigint` | No | `FK UK` | `intento_pago` | Intento confirmado una vez. |
| `id_usuario_pagador` | `bigint` | Sí | `FK` | `usuario` | Pagador personal congelado. |
| `id_empresa_pagadora` | `bigint` | Sí | `FK` | `empresa_cliente` | Pagador empresarial congelado. |
| `id_sucursal_origen` | `bigint` | Sí | `FK` | `sucursal` | Sucursal de origen conservada. |
| `id_usuario_confirmador` | `bigint` | Sí | `FK` | `usuario` | Interno que verifica efectivo o transferencia. |
| `destino` | `varchar(32)` | No | — | — | PAGO_PEDIDOS o PAGO_CUENTA_CORRIENTE. |
| `medio_pago` | `varchar(24)` | No | — | — | Medio confirmado e inmutable. |
| `importe_bruto` | `numeric(19,2)` | No | — | — | Importe ARS bruto confirmado. |
| `identificador_externo` | `varchar(190)` | Sí | — | — | ID del proveedor cuando aplica. |
| `fecha_confirmacion` | `timestamptz` | No | — | — | Nacimiento del pago. |

**Restricciones de entidad**

- Exactamente uno entre usuario y empresa pagadora debe ser no nulo.
- Un pago no cruza pagadores ni destinos y nunca cambia de medio.

#### `aplicacion_pago_pedido`

Parte de un pago normal aplicada a un pedido y clasificada como seña o saldo.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_aplicacion_pago_pedido` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pago` | `bigint` | No | `FK UK*` | `pago` | Pago de destino PAGO_PEDIDOS. |
| `id_pedido` | `bigint` | No | `FK UK*` | `pedido` | Pedido del mismo pagador. |
| `clasificacion` | `varchar(12)` | No | `UK*` | — | SENA o SALDO. |
| `importe_aplicado` | `numeric(19,2)` | No | — | — | Importe positivo. |
| `fecha_aplicacion` | `timestamptz` | No | — | — | Aplicación inmutable. |

**Restricciones de entidad**

- La suma aplicada no supera el pago ni el saldo de los pedidos; no admite excedente.

#### `senia`

Requisito financiero único por pedido, separado para identificar su cobertura.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_senia` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido` | `bigint` | No | `FK UK` | `pedido` | Pedido con requisito de seña. |
| `id_regla_comercial` | `bigint` | Sí | `FK` | `regla_comercial` | Regla que originó el requisito. |
| `tipo_origen` | `varchar(24)` | No | — | — | REGLA, EXCEPCION_MANUAL o PREPAGO_TOTAL. |
| `importe_requerido` | `numeric(19,2)` | No | — | — | Importe congelado positivo. |
| `estado_derivado` | `varchar(16)` | No | — | — | PENDIENTE, PARCIAL, CUBIERTA o REVERTIDA; caché verificable. |
| `fecha_creacion` | `timestamptz` | No | — | — | Alta. |
| `fecha_cobertura` | `timestamptz` | Sí | — | — | Cobertura completa. |
| `fecha_reversion` | `timestamptz` | Sí | — | — | Reversión completa. |

**Restricciones de entidad**

- Un pedido enteramente a cuenta corriente no tiene seña.
- La cobertura se deriva de aplicaciones SENA menos reembolsos.

#### `comprobante_pago`

Evidencia privada informada o recibo generado para un intento o un pago.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_comprobante_pago` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_intento_pago` | `bigint` | Sí | `FK` | `intento_pago` | Evidencia previa a confirmar. |
| `id_pago` | `bigint` | Sí | `FK` | `pago` | Comprobante de un pago confirmado. |
| `id_archivo_almacenado` | `bigint` | No | `FK UK` | `archivo_almacenado` | Archivo privado sin purga automática. |
| `tipo` | `varchar(32)` | No | — | — | EVIDENCIA_TRANSFERENCIA, RECIBO_INTERNO o COMPROBANTE_PROVEEDOR. |
| `fecha_creacion` | `timestamptz` | No | — | — | Carga o generación. |

**Restricciones de entidad**

- Exactamente uno entre intento y pago debe ser no nulo.

#### `reembolso`

Devolución que reduce aplicaciones originales sin reescribir pagos ni pedidos.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_reembolso` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco. |
| `id_pago` | `bigint` | No | `FK` | `pago` | Pago confirmado de origen. |
| `id_aplicacion_pago_pedido` | `bigint` | Sí | `FK` | `aplicacion_pago_pedido` | Aplicación normal reducida. |
| `id_usuario_solicitante` | `bigint` | No | `FK` | `usuario` | Interno autorizado. |
| `id_integracion_externa` | `bigint` | Sí | `FK UK*` | `integracion_externa` | Proveedor que procesa la devolución. |
| `importe` | `numeric(19,2)` | No | — | — | Importe ARS positivo y reembolsable. |
| `estado` | `varchar(16)` | No | — | — | PENDIENTE, CONFIRMADO, FALLIDO o CANCELADO. |
| `motivo` | `text` | No | — | — | Justificación obligatoria. |
| `identificador_externo` | `varchar(190)` | Sí | `UK*` | — | ID del proveedor. |
| `fecha_solicitud` | `timestamptz` | No | — | — | Inicio. |
| `fecha_resolucion` | `timestamptz` | Sí | — | — | Resultado. |

**Restricciones de entidad**

- La suma confirmada no supera el saldo reembolsable de la aplicación o pago.
- El identificador externo es único por integración cuando el proveedor lo informa.

#### `historial_reembolso`

Transiciones append-only de una devolución.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_historial_reembolso` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_reembolso` | `bigint` | No | `FK` | `reembolso` | Reembolso. |
| `id_usuario_actor` | `bigint` | Sí | `FK` | `usuario` | Actor humano si existe. |
| `actor_tipo` | `varchar(24)` | No | — | — | USUARIO, SISTEMA o SISTEMA_EXTERNO. |
| `estado_anterior` | `varchar(16)` | Sí | — | — | Nulo al crear. |
| `estado_nuevo` | `varchar(16)` | No | — | — | Estado resultante. |
| `codigo_resultado` | `varchar(80)` | Sí | — | — | Resultado sanitizado. |
| `fecha` | `timestamptz` | No | — | — | Transición. |

#### `cuenta_corriente`

Cuenta financiera opcional y única de una empresa cliente.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_cuenta_corriente` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_empresa_cliente` | `bigint` | No | `FK UK` | `empresa_cliente` | Empresa titular. |
| `id_usuario_activador` | `bigint` | No | `FK` | `usuario` | Administrador que la crea. |
| `limite_credito` | `numeric(19,2)` | No | — | — | Límite ARS no negativo. |
| `estado` | `varchar(16)` | No | — | — | ACTIVA, BLOQUEADA o CERRADA. |
| `fecha_activacion` | `timestamptz` | No | — | — | Alta posterior al pedido externo. |
| `fecha_cierre` | `timestamptz` | Sí | — | — | Cierre con saldo cero. |

**Restricciones de entidad**

- El saldo y disponible son derivados del libro; alcanzar exactamente el límite está permitido.

#### `historial_cuenta_corriente`

Cambios inmutables de límite o estado de una cuenta corriente.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_historial_cuenta_corriente` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_cuenta_corriente` | `bigint` | No | `FK` | `cuenta_corriente` | Cuenta afectada. |
| `id_usuario_actor` | `bigint` | No | `FK` | `usuario` | Administrador o empleado financiero autorizado. |
| `tipo_cambio` | `varchar(24)` | No | — | — | ACTIVACION, LIMITE, BLOQUEO, DESBLOQUEO, CIERRE o REAPERTURA. |
| `estado_anterior` | `varchar(16)` | Sí | — | — | Estado previo. |
| `estado_nuevo` | `varchar(16)` | Sí | — | — | Estado posterior. |
| `limite_anterior` | `numeric(19,2)` | Sí | — | — | Límite previo. |
| `limite_nuevo` | `numeric(19,2)` | Sí | — | — | Límite posterior. |
| `motivo` | `text` | No | — | — | Justificación. |
| `fecha` | `timestamptz` | No | — | — | Cambio. |

#### `movimiento_cuenta_corriente`

Libro mayor append-only de débitos y créditos de la empresa.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_movimiento_cuenta_corriente` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_cuenta_corriente` | `bigint` | No | `FK` | `cuenta_corriente` | Cuenta propietaria. |
| `id_pedido` | `bigint` | Sí | `FK UK` | `pedido` | Pedido para un cargo. |
| `id_pago` | `bigint` | Sí | `FK UK` | `pago` | Pago que origina un crédito. |
| `id_reembolso` | `bigint` | Sí | `FK UK` | `reembolso` | Devolución que vuelve a debitar. |
| `id_movimiento_origen` | `bigint` | Sí | `FK` | `movimiento_cuenta_corriente` | Cargo o crédito que se revierte. |
| `id_resolucion_reclamo` | `bigint` | Sí | `FK UK` | `resolucion_reclamo` | Compensación originada por reclamo. |
| `tipo` | `varchar(32)` | No | — | — | CARGO_PEDIDO, REVERSION_CARGO, PAGO, CREDITO_COMPENSACION o REEMBOLSO. |
| `importe_debe` | `numeric(19,2)` | No | — | — | Débito ARS; cero si es crédito. |
| `importe_haber` | `numeric(19,2)` | No | — | — | Crédito ARS; cero si es débito. |
| `concepto` | `varchar(240)` | No | — | — | Descripción inmutable. |
| `fecha` | `timestamptz` | No | — | — | Momento contable. |

**Restricciones de entidad**

- Exactamente uno entre debe y haber es positivo.
- El tipo determina una única referencia de origen válida.
- Existe un único movimiento original CARGO_PEDIDO por pedido a cuenta corriente.

#### `aplicacion_movimiento_cuenta`

Distribución inmutable de un movimiento con haber disponible entre cargos de la misma cuenta.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_aplicacion_movimiento_cuenta` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_movimiento_origen` | `bigint` | No | `FK UK*` | `movimiento_cuenta_corriente` | Movimiento con haber disponible que se aplica. |
| `id_movimiento_cargo` | `bigint` | No | `FK UK*` | `movimiento_cuenta_corriente` | Cargo cubierto. |
| `importe_aplicado` | `numeric(19,2)` | No | — | — | Importe positivo. |
| `fecha_aplicacion` | `timestamptz` | No | — | — | Aplicación inmutable. |

**Restricciones de entidad**

- La pareja movimiento-origen y movimiento-cargo es única.
- Origen, cargo y aplicación pertenecen a la misma cuenta.
- El origen debe tener haber disponible, el destino debe ser un cargo y no se permiten autoaplicaciones.
- La suma aplicada no puede superar el haber disponible ni el saldo del cargo.
- La propuesta por defecto prioriza documentos vencidos y luego cargos antiguos; el usuario decide.

#### `plantilla_documento_cobro`

Plantilla normalizada e inmutable usada para reproducir documentos internos de cobro.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_plantilla_documento_cobro` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_version` | `bigint` | No | `FK UK` | `configuracion_version` | Versión que la define. |
| `id_logo_archivo` | `bigint` | Sí | `FK` | `archivo_almacenado` | Logo privado opcional. |
| `razon_social_emisor` | `varchar(180)` | No | — | — | Texto configurado por la imprenta. |
| `cuit_emisor` | `char(11)` | No | — | — | CUIT mostrado, sin integración fiscal. |
| `direccion_emisor` | `text` | No | — | — | Dirección mostrada. |
| `correo_emisor` | `varchar(254)` | Sí | — | — | Contacto mostrado. |
| `telefono_emisor` | `varchar(40)` | Sí | — | — | Contacto mostrado. |
| `pie` | `text` | Sí | — | — | Leyenda no fiscal. |
| `color_primario` | `char(7)` | Sí | — | — | Color hexadecimal opcional. |

**Restricciones de entidad**

- No existe plantilla comercial predeterminada de YG; la primera se completa antes de activar la configuración.

#### `documento_cobro`

Solicitud de pago interna, no fiscal, emitida sobre cargos ya existentes.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_documento_cobro` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco. |
| `numero_secuencial` | `bigint` | No | `UK` | — | Secuencia global usada como DC-000001. |
| `id_cuenta_corriente` | `bigint` | No | `FK` | `cuenta_corriente` | Cuenta destinataria. |
| `id_plantilla_documento_cobro` | `bigint` | No | `FK` | `plantilla_documento_cobro` | Plantilla histórica reproducible. |
| `id_usuario_emisor` | `bigint` | No | `FK` | `usuario` | Administrador o empleado. |
| `id_archivo_pdf` | `bigint` | No | `FK UK` | `archivo_almacenado` | PDF privado e inmutable. |
| `id_documento_reemplazo` | `bigint` | Sí | `FK` | `documento_cobro` | Nuevo documento tras anulación. |
| `numero_visible` | `varchar(24)` | No | `UK` | — | Número formateado congelado. |
| `razon_social_cliente` | `varchar(180)` | No | — | — | Snapshot del receptor. |
| `cuit_cliente` | `char(11)` | No | — | — | Snapshot del receptor. |
| `total` | `numeric(19,2)` | No | — | — | Suma de líneas reservadas. |
| `estado` | `varchar(12)` | No | — | — | EMITIDO o ANULADO. |
| `estado_pago_derivado` | `varchar(12)` | No | — | — | PENDIENTE, PARCIAL o PAGADO; caché verificable. |
| `fecha_emision` | `timestamptz` | No | — | — | Emisión atómica. |
| `fecha_vencimiento` | `timestamptz` | No | — | — | Plazo que genera alerta. |
| `fecha_pago` | `timestamptz` | Sí | — | — | Cobertura completa. |
| `fecha_anulacion` | `timestamptz` | Sí | — | — | Anulación. |
| `motivo_anulacion` | `text` | Sí | — | — | Obligatorio al anular. |

**Restricciones de entidad**

- No tiene borrador editable: se emite en una transacción o no existe.
- Vencido es derivado y no bloquea por sí mismo la cuenta.
- La integridad del PDF se obtiene de archivo_almacenado.sha256, que permanece inmutable.

#### `linea_documento_cobro`

Línea congelada que reserva parte de un cargo para evitar doble documentación.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_linea_documento_cobro` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_documento_cobro` | `bigint` | No | `FK UK*` | `documento_cobro` | Documento propietario. |
| `id_movimiento_cargo` | `bigint` | No | `FK UK*` | `movimiento_cuenta_corriente` | Cargo documentado. |
| `numero_pedido_snapshot` | `bigint` | No | — | — | Número congelado. |
| `concepto` | `varchar(240)` | No | — | — | Concepto congelado. |
| `importe` | `numeric(19,2)` | No | — | — | Parte positiva reservada. |
| `fecha_cargo_snapshot` | `timestamptz` | No | — | — | Fecha congelada. |

**Restricciones de entidad**

- Un cargo puede dividirse entre documentos vigentes sin superar su saldo documentable.
- El pedido se deriva del movimiento de cargo; los valores impresos permanecen congelados.

### 5.9. Programación y entregas

#### `franja_entrega`

Plantilla semanal versionada de horario y capacidad para una modalidad y destino.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_franja_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_configuracion_modalidad_entrega` | `bigint` | No | `FK` | `configuracion_modalidad_entrega` | Modalidad versionada. |
| `id_sucursal` | `bigint` | Sí | `FK` | `sucursal` | Destino para RETIRO_SUCURSAL. |
| `id_configuracion_punto_entrega` | `bigint` | Sí | `FK` | `configuracion_punto_entrega` | Relación sucursal-punto versionada. |
| `id_configuracion_zona_entrega` | `bigint` | Sí | `FK` | `configuracion_zona_entrega` | Zona global versionada. |
| `dia_semana` | `smallint` | No | — | — | 1 a 7, lunes a domingo. |
| `hora_desde` | `time` | No | — | — | Inicio local. |
| `hora_hasta` | `time` | No | — | — | Fin local. |
| `capacidad_pedidos` | `integer` | No | — | — | Cantidad de pedidos completos; puede ser cero. |
| `habilitada` | `boolean` | No | — | — | Disponibilidad. |

**Restricciones de entidad**

- Exactamente uno entre sucursal, punto versionado y zona versionada debe ser no nulo y coincidir con la modalidad.
- Para la misma versión, modalidad, destino efectivo y día no pueden existir intervalos horarios superpuestos.
- Una franja es un único turno y su capacidad puede admitir varios pedidos completos o ser cero.

#### `excepcion_franja_entrega`

Cierre o capacidad especial de una franja en una fecha civil.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_excepcion_franja_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_franja_entrega` | `bigint` | No | `FK UK*` | `franja_entrega` | Franja afectada. |
| `fecha` | `date` | No | `UK*` | — | Fecha local concreta. |
| `cerrada` | `boolean` | No | — | — | Cierre total. |
| `capacidad_pedidos` | `integer` | Sí | — | — | Reemplazo de capacidad si permanece abierta. |
| `motivo` | `varchar(180)` | No | — | — | Feriado, cierre o excepción. |

**Restricciones de entidad**

- La pareja franja-fecha es única.

#### `reserva_entrega`

Cupo único reservado atómicamente al confirmar un pedido.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_reserva_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido` | `bigint` | No | `FK UK` | `pedido` | Pedido completo reservado una vez. |
| `id_franja_entrega` | `bigint` | Sí | `FK` | `franja_entrega` | Franja regular; nula para excepción manual. |
| `id_sucursal_origen` | `bigint` | No | `FK` | `sucursal` | Sucursal operativa congelada. |
| `id_punto_entrega` | `bigint` | Sí | `FK` | `punto_entrega` | Punto externo congelado. |
| `id_zona_entrega` | `bigint` | Sí | `FK` | `zona_entrega` | Zona global congelada. |
| `id_usuario_autorizante` | `bigint` | Sí | `FK` | `usuario` | Interno que autoriza una fecha manual. |
| `modalidad` | `varchar(28)` | No | — | — | Modalidad congelada. |
| `destino_nombre` | `varchar(180)` | No | — | — | Nombre reproducible del destino. |
| `inicio_programado` | `timestamptz` | No | — | — | Inicio de la ventana concreta. |
| `fin_programado` | `timestamptz` | No | — | — | Fin de la ventana concreta. |
| `costo` | `numeric(19,2)` | No | — | — | Costo ARS congelado; puede ser cero. |
| `codigo_retiro_hash` | `varchar(255)` | Sí | — | — | Código de retiro hasheado. |
| `es_excepcion_manual` | `boolean` | No | — | — | Fecha autorizada fuera de propuesta automática. |
| `motivo_excepcion` | `text` | Sí | — | — | Obligatorio para excepción. |
| `estado` | `varchar(16)` | No | — | — | ACTIVA, LIBERADA o CUMPLIDA. |
| `fecha_reserva` | `timestamptz` | No | — | — | Confirmación. |
| `fecha_liberacion` | `timestamptz` | Sí | — | — | Cancelación o cambio. |

**Restricciones de entidad**

- Solo ACTIVA consume una unidad de capacidad.
- La cotización consulta cupo, pero nunca crea esta fila.

#### `historial_reserva_entrega`

Historial append-only de reserva, liberación y reprogramación aceptada.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_historial_reserva_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_reserva_entrega` | `bigint` | No | `FK` | `reserva_entrega` | Reserva actual. |
| `id_usuario_actor` | `bigint` | Sí | `FK` | `usuario` | Actor humano; nulo para sistema. |
| `accion` | `varchar(24)` | No | — | — | RESERVA, LIBERACION, PROPUESTA o REPROGRAMACION. |
| `inicio_anterior` | `timestamptz` | Sí | — | — | Ventana anterior. |
| `fin_anterior` | `timestamptz` | Sí | — | — | Ventana anterior. |
| `inicio_nuevo` | `timestamptz` | Sí | — | — | Ventana nueva. |
| `fin_nuevo` | `timestamptz` | Sí | — | — | Ventana nueva. |
| `aceptada_por_cliente` | `boolean` | Sí | — | — | Decisión requerida para reprogramar. |
| `motivo` | `text` | Sí | — | — | Justificación. |
| `fecha` | `timestamptz` | No | — | — | Evento. |

#### `direccion_entrega_snapshot`

Dirección y contacto inmutables usados por un pedido con envío a domicilio.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_direccion_entrega_snapshot` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido` | `bigint` | No | `FK UK` | `pedido` | Pedido enviado a domicilio. |
| `id_direccion_cliente_origen` | `bigint` | Sí | `FK` | `direccion_cliente` | Dirección reutilizable de origen. |
| `receptor_nombre` | `varchar(200)` | No | — | — | Nombre de contacto congelado. |
| `receptor_telefono` | `varchar(40)` | No | — | — | Teléfono congelado. |
| `calle` | `varchar(160)` | No | — | — | Calle. |
| `numero` | `varchar(20)` | No | — | — | Altura. |
| `piso_departamento` | `varchar(40)` | Sí | — | — | Complemento. |
| `localidad` | `varchar(120)` | No | — | — | Localidad. |
| `provincia` | `varchar(120)` | No | — | — | Provincia. |
| `codigo_postal` | `varchar(12)` | No | — | — | Código postal. |
| `referencias` | `text` | Sí | — | — | Indicaciones. |

#### `entrega`

Constancia inmutable de una entrega completa o su anulación correctiva.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_entrega` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_reserva_entrega` | `bigint` | No | `FK` | `reserva_entrega` | Programación cumplida. |
| `id_usuario_entregador` | `bigint` | No | `FK` | `usuario` | Empleado que valida y entrega. |
| `id_entrega_anulada` | `bigint` | Sí | `FK` | `entrega` | Constancia incorrecta sustituida. |
| `modalidad` | `varchar(28)` | No | — | — | Modalidad ejecutada. |
| `ubicacion_snapshot` | `text` | No | — | — | Sucursal, punto o domicilio reproducible. |
| `receptor_nombre` | `varchar(200)` | No | — | — | Persona que recibe. |
| `receptor_documento` | `varchar(40)` | Sí | — | — | Identificación opcional. |
| `codigo_retiro_validado` | `boolean` | No | — | — | Validación presencial cuando corresponde. |
| `nota` | `text` | Sí | — | — | Observación opcional. |
| `estado` | `varchar(12)` | No | — | — | REGISTRADA o ANULADA. |
| `fecha_entrega` | `timestamptz` | No | — | — | Momento real. |
| `fecha_anulacion` | `timestamptz` | Sí | — | — | Corrección. |
| `motivo_anulacion` | `text` | Sí | — | — | Obligatorio al anular. |

**Restricciones de entidad**

- Existe como máximo una entrega REGISTRADA por pedido derivado de la reserva.
- El pedido se deriva de la reserva obligatoria.
- No se representan entregas parciales.

### 5.10. Reclamos y compensaciones

#### `reclamo`

Caso posterior que no reabre ni modifica el pedido cerrado.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_reclamo` | `bigint` | No | `PK` | — | Identificador interno. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco. |
| `id_pedido` | `bigint` | No | `FK` | `pedido` | Pedido reclamado. |
| `id_usuario_solicitante` | `bigint` | No | `FK` | `usuario` | Cliente empresarial, personal o interno. |
| `tipo` | `varchar(32)` | No | — | — | Calidad, faltante, daño, entrega u otra categoría controlada. |
| `descripcion` | `text` | No | — | — | Detalle del caso. |
| `estado` | `varchar(20)` | No | — | — | ABIERTO, EN_REVISION, RESUELTO, RECHAZADO o CANCELADO. |
| `fecha_limite` | `timestamptz` | No | — | — | Plazo congelado desde el pedido. |
| `fecha_apertura` | `timestamptz` | No | — | — | Alta. |
| `fecha_cierre` | `timestamptz` | Sí | — | — | Resolución. |

#### `reclamo_item`

Items específicos incluidos en un reclamo.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_reclamo_item` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_reclamo` | `bigint` | No | `FK UK*` | `reclamo` | Reclamo. |
| `id_pedido_item` | `bigint` | No | `FK UK*` | `pedido_item` | Item afectado. |

**Restricciones de entidad**

- La pareja reclamo-item es única y ambos pertenecen al mismo pedido.

#### `evidencia_reclamo`

Archivo privado aportado como evidencia y retenido hasta resolver el caso.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_evidencia_reclamo` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_reclamo` | `bigint` | No | `FK` | `reclamo` | Reclamo documentado. |
| `id_archivo_almacenado` | `bigint` | No | `FK UK` | `archivo_almacenado` | Archivo de evidencia. |
| `id_usuario_cargador` | `bigint` | No | `FK` | `usuario` | Usuario que aporta. |
| `descripcion` | `varchar(240)` | Sí | — | — | Contexto opcional. |
| `fecha` | `timestamptz` | No | — | — | Carga. |

**Restricciones de entidad**

- Nunca se purga antes de la resolución y luego respeta la retención del pedido.

#### `resolucion_reclamo`

Acción compensatoria inmutable; varias filas permiten combinaciones compatibles.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_resolucion_reclamo` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_reclamo` | `bigint` | No | `FK` | `reclamo` | Reclamo resuelto. |
| `id_pedido_item` | `bigint` | Sí | `FK` | `pedido_item` | Item alcanzado cuando corresponde. |
| `id_usuario_resolvedor` | `bigint` | No | `FK` | `usuario` | Interno autorizado. |
| `id_trabajo_impresion` | `bigint` | Sí | `FK UK` | `trabajo_impresion` | Reimpresión sin cargo creada. |
| `id_reembolso` | `bigint` | Sí | `FK UK` | `reembolso` | Reembolso creado. |
| `tipo` | `varchar(32)` | No | — | — | REIMPRESION_SIN_CARGO, REEMBOLSO_TOTAL, REEMBOLSO_PARCIAL o SIN_COMPENSACION. |
| `importe` | `numeric(19,2)` | Sí | — | — | Importe para compensación monetaria. |
| `destino_material` | `varchar(16)` | Sí | — | — | DEVUELTO, DESCARTADO o CONSERVADO. |
| `motivo` | `text` | No | — | — | Fundamento obligatorio. |
| `fecha` | `timestamptz` | No | — | — | Decisión. |

**Restricciones de entidad**

- El tipo exige exactamente el trabajo, reembolso o ausencia de salida que corresponda.
- Un reclamo puede tener varias resoluciones del mismo tipo, incluso sobre el mismo item.
- Cada resolución es un hecho inmutable independiente; trabajo y reembolso no pueden reutilizarse en otra resolución.

#### `historial_reclamo`

Transiciones append-only del estado de un reclamo.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_historial_reclamo` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_reclamo` | `bigint` | No | `FK` | `reclamo` | Reclamo. |
| `id_usuario_actor` | `bigint` | Sí | `FK` | `usuario` | Actor humano; nulo para sistema. |
| `estado_anterior` | `varchar(20)` | Sí | — | — | Nulo al abrir. |
| `estado_nuevo` | `varchar(20)` | No | — | — | Estado resultante. |
| `motivo` | `text` | Sí | — | — | Obligatorio al rechazar o cancelar. |
| `fecha` | `timestamptz` | No | — | — | Transición. |

### 5.11. Notificaciones, alertas y auditoría

#### `notificacion`

Contenido persistente de un evento que puede tener uno o varios destinatarios.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_notificacion` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_pedido` | `bigint` | Sí | `FK` | `pedido` | Contexto de pedido. |
| `id_documento_cobro` | `bigint` | Sí | `FK` | `documento_cobro` | Contexto financiero. |
| `id_reclamo` | `bigint` | Sí | `FK` | `reclamo` | Contexto de reclamo. |
| `id_alerta` | `bigint` | Sí | `FK` | `alerta` | Alerta que también notificó. |
| `id_licencia_local` | `bigint` | Sí | `FK` | `licencia_local` | Contexto de licencia. |
| `tipo` | `varchar(60)` | No | — | — | Tipo funcional estable. |
| `titulo` | `varchar(180)` | No | — | — | Título del inbox. |
| `cuerpo` | `text` | No | — | — | Contenido visible. |
| `ruta_destino` | `varchar(500)` | Sí | — | — | Ruta interna autorizada, nunca URL secreta. |
| `fecha_creacion` | `timestamptz` | No | — | — | Generación. |

**Restricciones de entidad**

- Como máximo una referencia de contexto principal debe ser no nula.

#### `notificacion_usuario`

Inbox personal con lectura; al eliminar se borra solo esta relación.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_notificacion_usuario` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_notificacion` | `bigint` | No | `FK UK*` | `notificacion` | Contenido compartido. |
| `id_usuario` | `bigint` | No | `FK UK*` | `usuario` | Destinatario. |
| `fecha_recepcion` | `timestamptz` | No | — | — | Ingreso al inbox. |
| `fecha_lectura` | `timestamptz` | Sí | — | — | Lectura; nulo significa no leída. |

**Restricciones de entidad**

- La pareja notificación-usuario es única.
- El hard delete solicitado por el usuario se registra en auditoría antes de eliminar esta fila.

#### `dispositivo_usuario`

Dispositivo autorizado opcionalmente para recibir push además del inbox.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_dispositivo_usuario` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_usuario` | `bigint` | No | `FK` | `usuario` | Propietario. |
| `codigo_publico` | `uuid` | No | `UK` | — | Identificador opaco. |
| `plataforma` | `varchar(20)` | No | — | — | ANDROID u otra plataforma soportada. |
| `token_hash` | `char(64)` | No | `UK` | — | Huella para deduplicar. |
| `token_cifrado` | `bytea` | No | — | — | Token push cifrado; llave fuera de la base. |
| `permiso_push` | `boolean` | No | — | — | Consentimiento vigente. |
| `estado` | `varchar(20)` | No | — | — | ACTIVO, DESHABILITADO o REVOCADO. |
| `fecha_registro` | `timestamptz` | No | — | — | Alta. |
| `ultima_actividad` | `timestamptz` | Sí | — | — | Último uso. |
| `fecha_desactivacion` | `timestamptz` | Sí | — | — | Retiro. |

#### `envio_push`

Ciclo técnico agregado de entrega push para una notificación y un dispositivo.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_envio_push` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_notificacion_usuario` | `bigint` | No | `FK UK*` | `notificacion_usuario` | Notificación del inbox. |
| `id_dispositivo_usuario` | `bigint` | No | `FK UK*` | `dispositivo_usuario` | Dispositivo destino. |
| `identificador_externo` | `varchar(190)` | Sí | — | — | ID del proveedor push. |
| `estado` | `varchar(20)` | No | — | — | PENDIENTE, ENVIADO, ENTREGADO o FALLIDO. |
| `intentos` | `integer` | No | — | — | Cantidad de intentos. |
| `codigo_resultado` | `varchar(80)` | Sí | — | — | Resultado sanitizado. |
| `fecha_creacion` | `timestamptz` | No | — | — | Alta. |
| `fecha_ultimo_intento` | `timestamptz` | Sí | — | — | Último intento. |

**Restricciones de entidad**

- La pareja notificación-usuario y dispositivo es única.
- Intentos, estado, resultado externo y última fecha acumulan el ciclo técnico más reciente.

#### `alerta`

Situación operacional visible en contexto y en la vista interna de alertas.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_alerta` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_empresa_cliente` | `bigint` | Sí | `FK` | `empresa_cliente` | Empresa con riesgo financiero. |
| `id_cuenta_corriente` | `bigint` | Sí | `FK` | `cuenta_corriente` | Cuenta afectada. |
| `id_documento_cobro` | `bigint` | Sí | `FK` | `documento_cobro` | Documento vencido. |
| `id_impresora` | `bigint` | Sí | `FK` | `impresora` | Impresora afectada. |
| `id_trabajo_impresion` | `bigint` | Sí | `FK` | `trabajo_impresion` | Trabajo afectado. |
| `id_licencia_local` | `bigint` | Sí | `FK` | `licencia_local` | Licencia afectada. |
| `id_integracion_externa` | `bigint` | Sí | `FK` | `integracion_externa` | Integración afectada. |
| `tipo` | `varchar(60)` | No | — | — | Tipo funcional estable. |
| `causa` | `varchar(120)` | No | — | — | Clave estable de la causa. |
| `severidad` | `varchar(16)` | No | — | — | ADVERTENCIA o CRITICA. |
| `estado` | `varchar(12)` | No | — | — | ACTIVA o RESUELTA. |
| `contador_ocurrencias` | `integer` | No | — | — | Repeticiones de la misma causa. |
| `primera_ocurrencia` | `timestamptz` | No | — | — | Inicio. |
| `ultima_ocurrencia` | `timestamptz` | No | — | — | Última repetición. |
| `fecha_resolucion` | `timestamptz` | Sí | — | — | Resolución automática o manual. |

**Restricciones de entidad**

- Exactamente una referencia causante debe ser no nula.
- Existe como máximo una alerta ACTIVA por tipo, entidad y causa.
- Solo un administrador puede hacer hard delete de una alerta RESUELTA, previa confirmación y auditoría.

#### `auditoria`

Registro append-only de acciones sensibles sin duplicar historiales de dominio.

| Campo | Tipo PostgreSQL | Nulo | Clave | Referencia | Descripción |
|---|---|:---:|---|---|---|
| `id_auditoria` | `bigint` | No | `PK` | — | Identificador interno. |
| `id_usuario_actor` | `bigint` | Sí | `FK` | `usuario` | Actor humano. |
| `id_agente_actor` | `bigint` | Sí | `FK` | `agente_impresion` | Agente técnico. |
| `id_integracion_actor` | `bigint` | Sí | `FK` | `integracion_externa` | Sistema externo conocido. |
| `actor_tipo` | `varchar(24)` | No | — | — | USUARIO, SISTEMA, AGENTE_IMPRESION o SISTEMA_EXTERNO. |
| `accion` | `varchar(100)` | No | — | — | Acción funcional estable. |
| `tipo_entidad_afectada` | `varchar(80)` | No | — | — | Tipo lógico de destino. |
| `id_entidad_afectada` | `varchar(100)` | No | — | — | ID serializado del destino. |
| `request_id` | `uuid` | No | — | — | Correlación extremo a extremo. |
| `metadatos_sanitizados` | `jsonb` | Sí | — | — | Solo contexto permitido, sin secretos ni payloads brutos. |
| `direccion_ip` | `inet` | Sí | — | — | Origen si es pertinente. |
| `fecha` | `timestamptz` | No | — | — | Momento inmutable. |

**Restricciones de entidad**

- La aplicación no actualiza ni elimina filas de auditoría.
- Solo administradores y SOPORTE habilitado pueden consultar.


## 6. Relaciones y cardinalidades

La notación se lee desde la entidad padre hacia la hija. La cardinalidad izquierda indica cuántos padres puede referenciar cada fila hija; la derecha cuántas filas hijas puede tener una fila padre. Las unicidades parciales y reglas temporales se detallan como restricciones, porque el DER no contiene DDL.

| Dominio hijo | Relación | FK | Nombre funcional |
|---|---|---|---|
| Archivos, conversión y validación | `usuario` (0..1) → `archivo_almacenado` (0..N) | `archivo_almacenado.id_usuario_cargador` → `usuario.id_usuario` | usuario_cargador |
| Archivos, conversión y validación | `archivo_almacenado` (1) → `archivo_trabajo` (0..1) | `archivo_trabajo.id_archivo_almacenado` → `archivo_almacenado.id_archivo_almacenado` | archivo_almacenado |
| Archivos, conversión y validación | `archivo_trabajo` (0..1) → `archivo_trabajo` (0..N) | `archivo_trabajo.id_archivo_trabajo_reemplazado` → `archivo_trabajo.id_archivo_trabajo` | reemplaza |
| Archivos, conversión y validación | `cotizacion_item` (0..1) → `archivo_trabajo` (0..N) | `archivo_trabajo.id_cotizacion_item` → `cotizacion_item.id_cotizacion_item` | cotizacion_item |
| Archivos, conversión y validación | `pedido_item` (0..1) → `archivo_trabajo` (0..N) | `archivo_trabajo.id_pedido_item` → `pedido_item.id_pedido_item` | pedido_item |
| Archivos, conversión y validación | `archivo_almacenado` (1) → `conversion_archivo` (0..N) | `conversion_archivo.id_archivo_origen` → `archivo_almacenado.id_archivo_almacenado` | origen |
| Archivos, conversión y validación | `archivo_almacenado` (0..1) → `conversion_archivo` (0..1) | `conversion_archivo.id_archivo_resultado` → `archivo_almacenado.id_archivo_almacenado` | resultado |
| Archivos, conversión y validación | `archivo_almacenado` (1) → `validacion_archivo` (0..N) | `validacion_archivo.id_archivo_almacenado` → `archivo_almacenado.id_archivo_almacenado` | archivo_almacenado |
| Cotizaciones, pedidos y correcciones | `cotizacion_item` (1) → `cotizacion_item_servicio` (0..N) | `cotizacion_item_servicio.id_cotizacion_item` → `cotizacion_item.id_cotizacion_item` | cotizacion_item |
| Cotizaciones, pedidos y correcciones | `servicio` (1) → `cotizacion_item_servicio` (0..N) | `cotizacion_item_servicio.id_servicio` → `servicio.id_servicio` | servicio |
| Cotizaciones, pedidos y correcciones | `cotizacion` (1) → `cotizacion_item` (0..N) | `cotizacion_item.id_cotizacion` → `cotizacion.id_cotizacion` | cotizacion |
| Cotizaciones, pedidos y correcciones | `formato` (1) → `cotizacion_item` (0..N) | `cotizacion_item.id_formato` → `formato.id_formato` | formato |
| Cotizaciones, pedidos y correcciones | `papel` (1) → `cotizacion_item` (0..N) | `cotizacion_item.id_papel` → `papel.id_papel` | papel |
| Cotizaciones, pedidos y correcciones | `usuario` (0..1) → `cotizacion_item` (0..N) | `cotizacion_item.id_usuario_ajuste` → `usuario.id_usuario` | usuario_ajuste |
| Cotizaciones, pedidos y correcciones | `cotizacion_item` (1) → `cotizacion_regla_aplicada` (0..N) | `cotizacion_regla_aplicada.id_cotizacion_item` → `cotizacion_item.id_cotizacion_item` | cotizacion_item |
| Cotizaciones, pedidos y correcciones | `regla_comercial` (1) → `cotizacion_regla_aplicada` (0..N) | `cotizacion_regla_aplicada.id_regla_comercial` → `regla_comercial.id_regla_comercial` | regla_comercial |
| Cotizaciones, pedidos y correcciones | `configuracion_version` (1) → `cotizacion` (0..N) | `cotizacion.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Cotizaciones, pedidos y correcciones | `cotizacion` (0..1) → `cotizacion` (0..N) | `cotizacion.id_cotizacion_reemplazada` → `cotizacion.id_cotizacion` | reemplaza |
| Cotizaciones, pedidos y correcciones | `empresa_cliente` (0..1) → `cotizacion` (0..N) | `cotizacion.id_empresa_cliente` → `empresa_cliente.id_empresa_cliente` | empresa_cliente |
| Cotizaciones, pedidos y correcciones | `franja_entrega` (0..1) → `cotizacion` (0..N) | `cotizacion.id_franja_entrega` → `franja_entrega.id_franja_entrega` | franja_entrega |
| Cotizaciones, pedidos y correcciones | `punto_entrega` (0..1) → `cotizacion` (0..N) | `cotizacion.id_punto_entrega` → `punto_entrega.id_punto_entrega` | punto_entrega |
| Cotizaciones, pedidos y correcciones | `sucursal` (1) → `cotizacion` (0..N) | `cotizacion.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Cotizaciones, pedidos y correcciones | `usuario` (1) → `cotizacion` (0..N) | `cotizacion.id_usuario_creador` → `usuario.id_usuario` | usuario_creador |
| Cotizaciones, pedidos y correcciones | `zona_entrega` (0..1) → `cotizacion` (0..N) | `cotizacion.id_zona_entrega` → `zona_entrega.id_zona_entrega` | zona_entrega |
| Cotizaciones, pedidos y correcciones | `pedido` (1) → `historial_estado_pedido` (0..N) | `historial_estado_pedido.id_pedido` → `pedido.id_pedido` | pedido |
| Cotizaciones, pedidos y correcciones | `usuario` (0..1) → `historial_estado_pedido` (0..N) | `historial_estado_pedido.id_usuario_actor` → `usuario.id_usuario` | usuario_actor |
| Cotizaciones, pedidos y correcciones | `pedido` (1) → `observacion_interna` (0..N) | `observacion_interna.id_pedido` → `pedido.id_pedido` | pedido |
| Cotizaciones, pedidos y correcciones | `usuario` (1) → `observacion_interna` (0..N) | `observacion_interna.id_usuario_autor` → `usuario.id_usuario` | usuario_autor |
| Cotizaciones, pedidos y correcciones | `pedido_item` (1) → `pedido_item_servicio` (0..N) | `pedido_item_servicio.id_pedido_item` → `pedido_item.id_pedido_item` | pedido_item |
| Cotizaciones, pedidos y correcciones | `servicio` (1) → `pedido_item_servicio` (0..N) | `pedido_item_servicio.id_servicio` → `servicio.id_servicio` | servicio |
| Cotizaciones, pedidos y correcciones | `cotizacion_item` (1) → `pedido_item` (0..1) | `pedido_item.id_cotizacion_item` → `cotizacion_item.id_cotizacion_item` | cotizacion_item |
| Cotizaciones, pedidos y correcciones | `pedido` (1) → `pedido_item` (0..N) | `pedido_item.id_pedido` → `pedido.id_pedido` | pedido |
| Cotizaciones, pedidos y correcciones | `configuracion_version` (1) → `pedido` (0..N) | `pedido.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Cotizaciones, pedidos y correcciones | `cotizacion` (1) → `pedido` (0..1) | `pedido.id_cotizacion` → `cotizacion.id_cotizacion` | cotizacion |
| Cotizaciones, pedidos y correcciones | `empresa_cliente` (0..1) → `pedido` (0..N) | `pedido.id_empresa_cliente` → `empresa_cliente.id_empresa_cliente` | empresa_cliente |
| Cotizaciones, pedidos y correcciones | `punto_entrega` (0..1) → `pedido` (0..N) | `pedido.id_punto_entrega` → `punto_entrega.id_punto_entrega` | punto_entrega |
| Cotizaciones, pedidos y correcciones | `sucursal` (1) → `pedido` (0..N) | `pedido.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Cotizaciones, pedidos y correcciones | `usuario` (0..1) → `pedido` (0..N) | `pedido.id_usuario_aprobador` → `usuario.id_usuario` | aprueba |
| Cotizaciones, pedidos y correcciones | `usuario` (1) → `pedido` (0..N) | `pedido.id_usuario_creador` → `usuario.id_usuario` | usuario_creador |
| Cotizaciones, pedidos y correcciones | `zona_entrega` (0..1) → `pedido` (0..N) | `pedido.id_zona_entrega` → `zona_entrega.id_zona_entrega` | zona_entrega |
| Cotizaciones, pedidos y correcciones | `solicitud_correccion` (1) → `respuesta_correccion` (0..N) | `respuesta_correccion.id_solicitud_correccion` → `solicitud_correccion.id_solicitud_correccion` | solicitud_correccion |
| Cotizaciones, pedidos y correcciones | `usuario` (1) → `respuesta_correccion` (0..N) | `respuesta_correccion.id_usuario_autor` → `usuario.id_usuario` | usuario_autor |
| Cotizaciones, pedidos y correcciones | `pedido` (1) → `solicitud_cancelacion` (0..N) | `solicitud_cancelacion.id_pedido` → `pedido.id_pedido` | pedido |
| Cotizaciones, pedidos y correcciones | `usuario` (0..1) → `solicitud_cancelacion` (0..N) | `solicitud_cancelacion.id_usuario_resolvedor` → `usuario.id_usuario` | resuelve |
| Cotizaciones, pedidos y correcciones | `usuario` (1) → `solicitud_cancelacion` (0..N) | `solicitud_cancelacion.id_usuario_solicitante` → `usuario.id_usuario` | usuario_solicitante |
| Cotizaciones, pedidos y correcciones | `archivo_almacenado` (1) → `solicitud_correccion_archivo` (0..N) | `solicitud_correccion_archivo.id_archivo_almacenado` → `archivo_almacenado.id_archivo_almacenado` | archivo_almacenado |
| Cotizaciones, pedidos y correcciones | `solicitud_correccion` (1) → `solicitud_correccion_archivo` (0..N) | `solicitud_correccion_archivo.id_solicitud_correccion` → `solicitud_correccion.id_solicitud_correccion` | solicitud_correccion |
| Cotizaciones, pedidos y correcciones | `pedido_item` (1) → `solicitud_correccion_item` (0..N) | `solicitud_correccion_item.id_pedido_item` → `pedido_item.id_pedido_item` | pedido_item |
| Cotizaciones, pedidos y correcciones | `solicitud_correccion` (1) → `solicitud_correccion_item` (0..N) | `solicitud_correccion_item.id_solicitud_correccion` → `solicitud_correccion.id_solicitud_correccion` | solicitud_correccion |
| Cotizaciones, pedidos y correcciones | `pedido` (1) → `solicitud_correccion` (0..N) | `solicitud_correccion.id_pedido` → `pedido.id_pedido` | pedido |
| Cotizaciones, pedidos y correcciones | `usuario` (1) → `solicitud_correccion` (0..N) | `solicitud_correccion.id_usuario_solicitante` → `usuario.id_usuario` | usuario_solicitante |
| Notificaciones, alertas y auditoría | `cuenta_corriente` (0..1) → `alerta` (0..N) | `alerta.id_cuenta_corriente` → `cuenta_corriente.id_cuenta_corriente` | cuenta_corriente |
| Notificaciones, alertas y auditoría | `documento_cobro` (0..1) → `alerta` (0..N) | `alerta.id_documento_cobro` → `documento_cobro.id_documento_cobro` | documento_cobro |
| Notificaciones, alertas y auditoría | `empresa_cliente` (0..1) → `alerta` (0..N) | `alerta.id_empresa_cliente` → `empresa_cliente.id_empresa_cliente` | empresa_cliente |
| Notificaciones, alertas y auditoría | `impresora` (0..1) → `alerta` (0..N) | `alerta.id_impresora` → `impresora.id_impresora` | impresora |
| Notificaciones, alertas y auditoría | `integracion_externa` (0..1) → `alerta` (0..N) | `alerta.id_integracion_externa` → `integracion_externa.id_integracion_externa` | integracion_externa |
| Notificaciones, alertas y auditoría | `licencia_local` (0..1) → `alerta` (0..N) | `alerta.id_licencia_local` → `licencia_local.id_licencia_local` | licencia_local |
| Notificaciones, alertas y auditoría | `trabajo_impresion` (0..1) → `alerta` (0..N) | `alerta.id_trabajo_impresion` → `trabajo_impresion.id_trabajo_impresion` | trabajo_impresion |
| Notificaciones, alertas y auditoría | `agente_impresion` (0..1) → `auditoria` (0..N) | `auditoria.id_agente_actor` → `agente_impresion.id_agente_impresion` | agente_actor |
| Notificaciones, alertas y auditoría | `integracion_externa` (0..1) → `auditoria` (0..N) | `auditoria.id_integracion_actor` → `integracion_externa.id_integracion_externa` | integracion_actor |
| Notificaciones, alertas y auditoría | `usuario` (0..1) → `auditoria` (0..N) | `auditoria.id_usuario_actor` → `usuario.id_usuario` | usuario_actor |
| Notificaciones, alertas y auditoría | `usuario` (1) → `dispositivo_usuario` (0..N) | `dispositivo_usuario.id_usuario` → `usuario.id_usuario` | usuario |
| Notificaciones, alertas y auditoría | `dispositivo_usuario` (1) → `envio_push` (0..N) | `envio_push.id_dispositivo_usuario` → `dispositivo_usuario.id_dispositivo_usuario` | dispositivo_usuario |
| Notificaciones, alertas y auditoría | `notificacion_usuario` (1) → `envio_push` (0..N) | `envio_push.id_notificacion_usuario` → `notificacion_usuario.id_notificacion_usuario` | notificacion_usuario |
| Notificaciones, alertas y auditoría | `notificacion` (1) → `notificacion_usuario` (0..N) | `notificacion_usuario.id_notificacion` → `notificacion.id_notificacion` | notificacion |
| Notificaciones, alertas y auditoría | `usuario` (1) → `notificacion_usuario` (0..N) | `notificacion_usuario.id_usuario` → `usuario.id_usuario` | usuario |
| Notificaciones, alertas y auditoría | `alerta` (0..1) → `notificacion` (0..N) | `notificacion.id_alerta` → `alerta.id_alerta` | alerta |
| Notificaciones, alertas y auditoría | `documento_cobro` (0..1) → `notificacion` (0..N) | `notificacion.id_documento_cobro` → `documento_cobro.id_documento_cobro` | documento_cobro |
| Notificaciones, alertas y auditoría | `licencia_local` (0..1) → `notificacion` (0..N) | `notificacion.id_licencia_local` → `licencia_local.id_licencia_local` | licencia_local |
| Notificaciones, alertas y auditoría | `pedido` (0..1) → `notificacion` (0..N) | `notificacion.id_pedido` → `pedido.id_pedido` | pedido |
| Notificaciones, alertas y auditoría | `reclamo` (0..1) → `notificacion` (0..N) | `notificacion.id_reclamo` → `reclamo.id_reclamo` | reclamo |
| Configuración, catálogos y reglas | `regla_comercial` (1) → `accion_regla` (0..1) | `accion_regla.id_regla_comercial` → `regla_comercial.id_regla_comercial` | regla_comercial |
| Configuración, catálogos y reglas | `configuracion_servicio` (1) → `compatibilidad_servicio` (0..N) | `compatibilidad_servicio.id_configuracion_servicio` → `configuracion_servicio.id_configuracion_servicio` | configuracion_servicio |
| Configuración, catálogos y reglas | `formato` (1) → `compatibilidad_servicio` (0..N) | `compatibilidad_servicio.id_formato` → `formato.id_formato` | formato |
| Configuración, catálogos y reglas | `papel` (1) → `compatibilidad_servicio` (0..N) | `compatibilidad_servicio.id_papel` → `papel.id_papel` | papel |
| Configuración, catálogos y reglas | `regla_aprobacion` (1) → `condicion_aprobacion` (0..N) | `condicion_aprobacion.id_regla_aprobacion` → `regla_aprobacion.id_regla_aprobacion` | regla_aprobacion |
| Configuración, catálogos y reglas | `regla_asignacion` (1) → `condicion_asignacion` (0..N) | `condicion_asignacion.id_regla_asignacion` → `regla_asignacion.id_regla_asignacion` | regla_asignacion |
| Configuración, catálogos y reglas | `regla_comercial` (1) → `condicion_regla` (0..N) | `condicion_regla.id_regla_comercial` → `regla_comercial.id_regla_comercial` | regla_comercial |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `configuracion_impresora` (0..N) | `configuracion_impresora.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `impresora` (1) → `configuracion_impresora` (0..N) | `configuracion_impresora.id_impresora` → `impresora.id_impresora` | impresora |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `configuracion_medio_pago` (0..N) | `configuracion_medio_pago.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `modulo` (0..1) → `configuracion_medio_pago` (0..N) | `configuracion_medio_pago.id_modulo_requerido` → `modulo.id_modulo` | modulo_requerido |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `configuracion_modalidad_entrega` (0..N) | `configuracion_modalidad_entrega.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `configuracion_punto_entrega` (0..N) | `configuracion_punto_entrega.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `punto_entrega` (1) → `configuracion_punto_entrega` (0..N) | `configuracion_punto_entrega.id_punto_entrega` → `punto_entrega.id_punto_entrega` | punto_entrega |
| Configuración, catálogos y reglas | `sucursal` (1) → `configuracion_punto_entrega` (0..N) | `configuracion_punto_entrega.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `configuracion_servicio` (0..N) | `configuracion_servicio.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `servicio` (1) → `configuracion_servicio` (0..N) | `configuracion_servicio.id_servicio` → `servicio.id_servicio` | servicio |
| Configuración, catálogos y reglas | `usuario` (0..1) → `configuracion_version` (0..N) | `configuracion_version.id_usuario_activador` → `usuario.id_usuario` | activa |
| Configuración, catálogos y reglas | `usuario` (1) → `configuracion_version` (0..N) | `configuracion_version.id_usuario_creador` → `usuario.id_usuario` | usuario_creador |
| Configuración, catálogos y reglas | `configuracion_version` (0..1) → `configuracion_version` (0..N) | `configuracion_version.id_version_base` → `configuracion_version.id_configuracion_version` | copia |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `configuracion_zona_entrega` (0..N) | `configuracion_zona_entrega.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `zona_entrega` (1) → `configuracion_zona_entrega` (0..N) | `configuracion_zona_entrega.id_zona_entrega` → `zona_entrega.id_zona_entrega` | zona_entrega |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `politica_operativa` (0..1) | `politica_operativa.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `regla_aprobacion` (0..N) | `regla_aprobacion.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `impresora` (1) → `regla_asignacion_impresora` (0..N) | `regla_asignacion_impresora.id_impresora` → `impresora.id_impresora` | impresora |
| Configuración, catálogos y reglas | `regla_asignacion` (1) → `regla_asignacion_impresora` (0..N) | `regla_asignacion_impresora.id_regla_asignacion` → `regla_asignacion.id_regla_asignacion` | regla_asignacion |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `regla_asignacion` (0..N) | `regla_asignacion.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `regla_comercial` (0..N) | `regla_comercial.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `sucursal_servicio` (0..N) | `sucursal_servicio.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `servicio` (1) → `sucursal_servicio` (0..N) | `sucursal_servicio.id_servicio` → `servicio.id_servicio` | servicio |
| Configuración, catálogos y reglas | `sucursal` (1) → `sucursal_servicio` (0..N) | `sucursal_servicio.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Configuración, catálogos y reglas | `configuracion_version` (1) → `tarifa_impresion` (0..N) | `tarifa_impresion.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Configuración, catálogos y reglas | `formato` (1) → `tarifa_impresion` (0..N) | `tarifa_impresion.id_formato` → `formato.id_formato` | formato |
| Configuración, catálogos y reglas | `papel` (1) → `tarifa_impresion` (0..N) | `tarifa_impresion.id_papel` → `papel.id_papel` | papel |
| Programación y entregas | `direccion_cliente` (0..1) → `direccion_entrega_snapshot` (0..N) | `direccion_entrega_snapshot.id_direccion_cliente_origen` → `direccion_cliente.id_direccion_cliente` | direccion_cliente_origen |
| Programación y entregas | `pedido` (1) → `direccion_entrega_snapshot` (0..1) | `direccion_entrega_snapshot.id_pedido` → `pedido.id_pedido` | pedido |
| Programación y entregas | `entrega` (0..1) → `entrega` (0..N) | `entrega.id_entrega_anulada` → `entrega.id_entrega` | anula |
| Programación y entregas | `reserva_entrega` (1) → `entrega` (0..N) | `entrega.id_reserva_entrega` → `reserva_entrega.id_reserva_entrega` | reserva_entrega |
| Programación y entregas | `usuario` (1) → `entrega` (0..N) | `entrega.id_usuario_entregador` → `usuario.id_usuario` | usuario_entregador |
| Programación y entregas | `franja_entrega` (1) → `excepcion_franja_entrega` (0..N) | `excepcion_franja_entrega.id_franja_entrega` → `franja_entrega.id_franja_entrega` | franja_entrega |
| Programación y entregas | `configuracion_modalidad_entrega` (1) → `franja_entrega` (0..N) | `franja_entrega.id_configuracion_modalidad_entrega` → `configuracion_modalidad_entrega.id_configuracion_modalidad_entrega` | configuracion_modalidad_entrega |
| Programación y entregas | `configuracion_punto_entrega` (0..1) → `franja_entrega` (0..N) | `franja_entrega.id_configuracion_punto_entrega` → `configuracion_punto_entrega.id_configuracion_punto_entrega` | configuracion_punto_entrega |
| Programación y entregas | `configuracion_zona_entrega` (0..1) → `franja_entrega` (0..N) | `franja_entrega.id_configuracion_zona_entrega` → `configuracion_zona_entrega.id_configuracion_zona_entrega` | configuracion_zona_entrega |
| Programación y entregas | `sucursal` (0..1) → `franja_entrega` (0..N) | `franja_entrega.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Programación y entregas | `reserva_entrega` (1) → `historial_reserva_entrega` (0..N) | `historial_reserva_entrega.id_reserva_entrega` → `reserva_entrega.id_reserva_entrega` | reserva_entrega |
| Programación y entregas | `usuario` (0..1) → `historial_reserva_entrega` (0..N) | `historial_reserva_entrega.id_usuario_actor` → `usuario.id_usuario` | usuario_actor |
| Programación y entregas | `franja_entrega` (0..1) → `reserva_entrega` (0..N) | `reserva_entrega.id_franja_entrega` → `franja_entrega.id_franja_entrega` | franja_entrega |
| Programación y entregas | `pedido` (1) → `reserva_entrega` (0..1) | `reserva_entrega.id_pedido` → `pedido.id_pedido` | pedido |
| Programación y entregas | `punto_entrega` (0..1) → `reserva_entrega` (0..N) | `reserva_entrega.id_punto_entrega` → `punto_entrega.id_punto_entrega` | punto_entrega |
| Programación y entregas | `sucursal` (1) → `reserva_entrega` (0..N) | `reserva_entrega.id_sucursal_origen` → `sucursal.id_sucursal` | sucursal_origen |
| Programación y entregas | `usuario` (0..1) → `reserva_entrega` (0..N) | `reserva_entrega.id_usuario_autorizante` → `usuario.id_usuario` | usuario_autorizante |
| Programación y entregas | `zona_entrega` (0..1) → `reserva_entrega` (0..N) | `reserva_entrega.id_zona_entrega` → `zona_entrega.id_zona_entrega` | zona_entrega |
| Pagos, cuenta corriente y documentos | `movimiento_cuenta_corriente` (1) → `aplicacion_movimiento_cuenta` (0..N) | `aplicacion_movimiento_cuenta.id_movimiento_cargo` → `movimiento_cuenta_corriente.id_movimiento_cuenta_corriente` | cubre cargo |
| Pagos, cuenta corriente y documentos | `movimiento_cuenta_corriente` (1) → `aplicacion_movimiento_cuenta` (0..N) | `aplicacion_movimiento_cuenta.id_movimiento_origen` → `movimiento_cuenta_corriente.id_movimiento_cuenta_corriente` | usa haber |
| Pagos, cuenta corriente y documentos | `pago` (1) → `aplicacion_pago_pedido` (0..N) | `aplicacion_pago_pedido.id_pago` → `pago.id_pago` | pago |
| Pagos, cuenta corriente y documentos | `pedido` (1) → `aplicacion_pago_pedido` (0..N) | `aplicacion_pago_pedido.id_pedido` → `pedido.id_pedido` | pedido |
| Pagos, cuenta corriente y documentos | `archivo_almacenado` (1) → `comprobante_pago` (0..1) | `comprobante_pago.id_archivo_almacenado` → `archivo_almacenado.id_archivo_almacenado` | archivo_almacenado |
| Pagos, cuenta corriente y documentos | `intento_pago` (0..1) → `comprobante_pago` (0..N) | `comprobante_pago.id_intento_pago` → `intento_pago.id_intento_pago` | intento_pago |
| Pagos, cuenta corriente y documentos | `pago` (0..1) → `comprobante_pago` (0..N) | `comprobante_pago.id_pago` → `pago.id_pago` | pago |
| Pagos, cuenta corriente y documentos | `empresa_cliente` (1) → `cuenta_corriente` (0..1) | `cuenta_corriente.id_empresa_cliente` → `empresa_cliente.id_empresa_cliente` | empresa_cliente |
| Pagos, cuenta corriente y documentos | `usuario` (1) → `cuenta_corriente` (0..N) | `cuenta_corriente.id_usuario_activador` → `usuario.id_usuario` | usuario_activador |
| Pagos, cuenta corriente y documentos | `archivo_almacenado` (1) → `documento_cobro` (0..1) | `documento_cobro.id_archivo_pdf` → `archivo_almacenado.id_archivo_almacenado` | archivo_pdf |
| Pagos, cuenta corriente y documentos | `cuenta_corriente` (1) → `documento_cobro` (0..N) | `documento_cobro.id_cuenta_corriente` → `cuenta_corriente.id_cuenta_corriente` | cuenta_corriente |
| Pagos, cuenta corriente y documentos | `documento_cobro` (0..1) → `documento_cobro` (0..N) | `documento_cobro.id_documento_reemplazo` → `documento_cobro.id_documento_cobro` | reemplazado por |
| Pagos, cuenta corriente y documentos | `plantilla_documento_cobro` (1) → `documento_cobro` (0..N) | `documento_cobro.id_plantilla_documento_cobro` → `plantilla_documento_cobro.id_plantilla_documento_cobro` | plantilla_documento_cobro |
| Pagos, cuenta corriente y documentos | `usuario` (1) → `documento_cobro` (0..N) | `documento_cobro.id_usuario_emisor` → `usuario.id_usuario` | usuario_emisor |
| Pagos, cuenta corriente y documentos | `cuenta_corriente` (1) → `historial_cuenta_corriente` (0..N) | `historial_cuenta_corriente.id_cuenta_corriente` → `cuenta_corriente.id_cuenta_corriente` | cuenta_corriente |
| Pagos, cuenta corriente y documentos | `usuario` (1) → `historial_cuenta_corriente` (0..N) | `historial_cuenta_corriente.id_usuario_actor` → `usuario.id_usuario` | usuario_actor |
| Pagos, cuenta corriente y documentos | `reembolso` (1) → `historial_reembolso` (0..N) | `historial_reembolso.id_reembolso` → `reembolso.id_reembolso` | reembolso |
| Pagos, cuenta corriente y documentos | `usuario` (0..1) → `historial_reembolso` (0..N) | `historial_reembolso.id_usuario_actor` → `usuario.id_usuario` | usuario_actor |
| Pagos, cuenta corriente y documentos | `empresa_cliente` (0..1) → `intento_pago` (0..N) | `intento_pago.id_empresa_pagadora` → `empresa_cliente.id_empresa_cliente` | pagador empresa |
| Pagos, cuenta corriente y documentos | `integracion_externa` (0..1) → `intento_pago` (0..N) | `intento_pago.id_integracion_externa` → `integracion_externa.id_integracion_externa` | integracion_externa |
| Pagos, cuenta corriente y documentos | `sucursal` (0..1) → `intento_pago` (0..N) | `intento_pago.id_sucursal_origen` → `sucursal.id_sucursal` | sucursal_origen |
| Pagos, cuenta corriente y documentos | `usuario` (0..1) → `intento_pago` (0..N) | `intento_pago.id_usuario_pagador` → `usuario.id_usuario` | pagador personal |
| Pagos, cuenta corriente y documentos | `usuario` (1) → `intento_pago` (0..N) | `intento_pago.id_usuario_solicitante` → `usuario.id_usuario` | usuario_solicitante |
| Pagos, cuenta corriente y documentos | `documento_cobro` (1) → `linea_documento_cobro` (0..N) | `linea_documento_cobro.id_documento_cobro` → `documento_cobro.id_documento_cobro` | documento_cobro |
| Pagos, cuenta corriente y documentos | `movimiento_cuenta_corriente` (1) → `linea_documento_cobro` (0..N) | `linea_documento_cobro.id_movimiento_cargo` → `movimiento_cuenta_corriente.id_movimiento_cuenta_corriente` | movimiento_cargo |
| Pagos, cuenta corriente y documentos | `cuenta_corriente` (1) → `movimiento_cuenta_corriente` (0..N) | `movimiento_cuenta_corriente.id_cuenta_corriente` → `cuenta_corriente.id_cuenta_corriente` | cuenta_corriente |
| Pagos, cuenta corriente y documentos | `movimiento_cuenta_corriente` (0..1) → `movimiento_cuenta_corriente` (0..N) | `movimiento_cuenta_corriente.id_movimiento_origen` → `movimiento_cuenta_corriente.id_movimiento_cuenta_corriente` | revierte |
| Pagos, cuenta corriente y documentos | `pago` (0..1) → `movimiento_cuenta_corriente` (0..1) | `movimiento_cuenta_corriente.id_pago` → `pago.id_pago` | pago |
| Pagos, cuenta corriente y documentos | `pedido` (0..1) → `movimiento_cuenta_corriente` (0..1) | `movimiento_cuenta_corriente.id_pedido` → `pedido.id_pedido` | pedido |
| Pagos, cuenta corriente y documentos | `reembolso` (0..1) → `movimiento_cuenta_corriente` (0..1) | `movimiento_cuenta_corriente.id_reembolso` → `reembolso.id_reembolso` | reembolso |
| Pagos, cuenta corriente y documentos | `resolucion_reclamo` (0..1) → `movimiento_cuenta_corriente` (0..1) | `movimiento_cuenta_corriente.id_resolucion_reclamo` → `resolucion_reclamo.id_resolucion_reclamo` | resolucion_reclamo |
| Pagos, cuenta corriente y documentos | `empresa_cliente` (0..1) → `pago` (0..N) | `pago.id_empresa_pagadora` → `empresa_cliente.id_empresa_cliente` | pagador empresa |
| Pagos, cuenta corriente y documentos | `intento_pago` (1) → `pago` (0..1) | `pago.id_intento_pago` → `intento_pago.id_intento_pago` | intento_pago |
| Pagos, cuenta corriente y documentos | `sucursal` (0..1) → `pago` (0..N) | `pago.id_sucursal_origen` → `sucursal.id_sucursal` | sucursal_origen |
| Pagos, cuenta corriente y documentos | `usuario` (0..1) → `pago` (0..N) | `pago.id_usuario_confirmador` → `usuario.id_usuario` | confirma |
| Pagos, cuenta corriente y documentos | `usuario` (0..1) → `pago` (0..N) | `pago.id_usuario_pagador` → `usuario.id_usuario` | pagador personal |
| Pagos, cuenta corriente y documentos | `configuracion_version` (1) → `plantilla_documento_cobro` (0..1) | `plantilla_documento_cobro.id_configuracion_version` → `configuracion_version.id_configuracion_version` | configuracion_version |
| Pagos, cuenta corriente y documentos | `archivo_almacenado` (0..1) → `plantilla_documento_cobro` (0..N) | `plantilla_documento_cobro.id_logo_archivo` → `archivo_almacenado.id_archivo_almacenado` | logo_archivo |
| Pagos, cuenta corriente y documentos | `aplicacion_pago_pedido` (0..1) → `reembolso` (0..N) | `reembolso.id_aplicacion_pago_pedido` → `aplicacion_pago_pedido.id_aplicacion_pago_pedido` | aplicacion_pago_pedido |
| Pagos, cuenta corriente y documentos | `integracion_externa` (0..1) → `reembolso` (0..N) | `reembolso.id_integracion_externa` → `integracion_externa.id_integracion_externa` | integracion_externa |
| Pagos, cuenta corriente y documentos | `pago` (1) → `reembolso` (0..N) | `reembolso.id_pago` → `pago.id_pago` | pago |
| Pagos, cuenta corriente y documentos | `usuario` (1) → `reembolso` (0..N) | `reembolso.id_usuario_solicitante` → `usuario.id_usuario` | usuario_solicitante |
| Pagos, cuenta corriente y documentos | `pedido` (1) → `senia` (0..1) | `senia.id_pedido` → `pedido.id_pedido` | pedido |
| Pagos, cuenta corriente y documentos | `regla_comercial` (0..1) → `senia` (0..N) | `senia.id_regla_comercial` → `regla_comercial.id_regla_comercial` | regla_comercial |
| Identidad, acceso y clientes | `usuario` (1) → `autorizacion_soporte` (0..N) | `autorizacion_soporte.id_usuario_habilitante` → `usuario.id_usuario` | habilita |
| Identidad, acceso y clientes | `usuario` (0..1) → `autorizacion_soporte` (0..N) | `autorizacion_soporte.id_usuario_revocante` → `usuario.id_usuario` | revoca |
| Identidad, acceso y clientes | `usuario` (1) → `autorizacion_soporte` (0..N) | `autorizacion_soporte.id_usuario_soporte` → `usuario.id_usuario` | usuario_soporte |
| Identidad, acceso y clientes | `usuario` (1) → `credencial_temporal` (0..N) | `credencial_temporal.id_usuario` → `usuario.id_usuario` | usuario |
| Identidad, acceso y clientes | `usuario` (0..1) → `credencial_temporal` (0..N) | `credencial_temporal.id_usuario_autorizante` → `usuario.id_usuario` | autoriza |
| Identidad, acceso y clientes | `empresa_cliente` (0..1) → `direccion_cliente` (0..N) | `direccion_cliente.id_empresa_cliente` → `empresa_cliente.id_empresa_cliente` | empresa_cliente |
| Identidad, acceso y clientes | `usuario` (0..1) → `direccion_cliente` (0..N) | `direccion_cliente.id_usuario` → `usuario.id_usuario` | usuario |
| Identidad, acceso y clientes | `permiso` (1) → `rol_permiso` (0..N) | `rol_permiso.id_permiso` → `permiso.id_permiso` | permiso |
| Identidad, acceso y clientes | `rol` (1) → `rol_permiso` (0..N) | `rol_permiso.id_rol` → `rol.id_rol` | rol |
| Identidad, acceso y clientes | `dispositivo_usuario` (0..1) → `sesion_usuario` (0..N) | `sesion_usuario.id_dispositivo_usuario` → `dispositivo_usuario.id_dispositivo_usuario` | dispositivo_usuario |
| Identidad, acceso y clientes | `usuario` (1) → `sesion_usuario` (0..N) | `sesion_usuario.id_usuario` → `usuario.id_usuario` | usuario |
| Identidad, acceso y clientes | `usuario` (1) → `token_verificacion_correo` (0..N) | `token_verificacion_correo.id_usuario` → `usuario.id_usuario` | usuario |
| Identidad, acceso y clientes | `permiso` (1) → `usuario_permiso` (0..N) | `usuario_permiso.id_permiso` → `permiso.id_permiso` | permiso |
| Identidad, acceso y clientes | `usuario` (1) → `usuario_permiso` (0..N) | `usuario_permiso.id_usuario` → `usuario.id_usuario` | usuario |
| Identidad, acceso y clientes | `usuario` (1) → `usuario_permiso` (0..N) | `usuario_permiso.id_usuario_otorgante` → `usuario.id_usuario` | otorga |
| Identidad, acceso y clientes | `sucursal` (1) → `usuario_sucursal` (0..N) | `usuario_sucursal.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Identidad, acceso y clientes | `usuario` (1) → `usuario_sucursal` (0..N) | `usuario_sucursal.id_usuario` → `usuario.id_usuario` | usuario |
| Identidad, acceso y clientes | `empresa_cliente` (0..1) → `usuario` (0..N) | `usuario.id_empresa_cliente` → `empresa_cliente.id_empresa_cliente` | empresa_cliente |
| Identidad, acceso y clientes | `rol` (1) → `usuario` (0..N) | `usuario.id_rol` → `rol.id_rol` | rol |
| Sucursales y operación | `sucursal` (1) → `excepcion_horario_sucursal` (0..N) | `excepcion_horario_sucursal.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Sucursales y operación | `sucursal` (1) → `horario_sucursal` (0..N) | `horario_sucursal.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Sucursales y operación | `sucursal` (0..1) → `pausa_operativa` (0..N) | `pausa_operativa.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Sucursales y operación | `usuario` (0..1) → `pausa_operativa` (0..N) | `pausa_operativa.id_usuario_fin` → `usuario.id_usuario` | reanuda |
| Sucursales y operación | `usuario` (1) → `pausa_operativa` (0..N) | `pausa_operativa.id_usuario_inicio` → `usuario.id_usuario` | usuario_inicio |
| Sucursales y operación | `zona_entrega` (1) → `zona_entrega_codigo_postal` (0..N) | `zona_entrega_codigo_postal.id_zona_entrega` → `zona_entrega.id_zona_entrega` | zona_entrega |
| Agentes, impresoras y producción | `sucursal` (1) → `agente_impresion` (0..N) | `agente_impresion.id_sucursal` → `sucursal.id_sucursal` | sucursal |
| Agentes, impresoras y producción | `trabajo_impresion` (1) → `control_calidad` (0..1) | `control_calidad.id_trabajo_impresion` → `trabajo_impresion.id_trabajo_impresion` | trabajo_impresion |
| Agentes, impresoras y producción | `usuario` (1) → `control_calidad` (0..N) | `control_calidad.id_usuario_inspector` → `usuario.id_usuario` | usuario_inspector |
| Agentes, impresoras y producción | `agente_impresion` (1) → `credencial_agente` (0..N) | `credencial_agente.id_agente_impresion` → `agente_impresion.id_agente_impresion` | agente_impresion |
| Agentes, impresoras y producción | `impresora` (1) → `historial_estado_impresora` (0..N) | `historial_estado_impresora.id_impresora` → `impresora.id_impresora` | impresora |
| Agentes, impresoras y producción | `usuario` (0..1) → `historial_estado_impresora` (0..N) | `historial_estado_impresora.id_usuario_actor` → `usuario.id_usuario` | usuario_actor |
| Agentes, impresoras y producción | `agente_impresion` (0..1) → `historial_trabajo_impresion` (0..N) | `historial_trabajo_impresion.id_agente_actor` → `agente_impresion.id_agente_impresion` | agente_actor |
| Agentes, impresoras y producción | `trabajo_impresion` (1) → `historial_trabajo_impresion` (0..N) | `historial_trabajo_impresion.id_trabajo_impresion` → `trabajo_impresion.id_trabajo_impresion` | trabajo_impresion |
| Agentes, impresoras y producción | `usuario` (0..1) → `historial_trabajo_impresion` (0..N) | `historial_trabajo_impresion.id_usuario_actor` → `usuario.id_usuario` | usuario_actor |
| Agentes, impresoras y producción | `formato` (1) → `impresora_formato` (0..N) | `impresora_formato.id_formato` → `formato.id_formato` | formato |
| Agentes, impresoras y producción | `impresora` (1) → `impresora_formato` (0..N) | `impresora_formato.id_impresora` → `impresora.id_impresora` | impresora |
| Agentes, impresoras y producción | `impresora` (1) → `impresora_servicio` (0..N) | `impresora_servicio.id_impresora` → `impresora.id_impresora` | impresora |
| Agentes, impresoras y producción | `servicio` (1) → `impresora_servicio` (0..N) | `impresora_servicio.id_servicio` → `servicio.id_servicio` | servicio |
| Agentes, impresoras y producción | `agente_impresion` (1) → `impresora` (0..N) | `impresora.id_agente_impresion` → `agente_impresion.id_agente_impresion` | agente_impresion |
| Agentes, impresoras y producción | `agente_impresion` (0..1) → `trabajo_impresion` (0..N) | `trabajo_impresion.id_agente_impresion` → `agente_impresion.id_agente_impresion` | agente_impresion |
| Agentes, impresoras y producción | `impresora` (0..1) → `trabajo_impresion` (0..N) | `trabajo_impresion.id_impresora` → `impresora.id_impresora` | impresora |
| Agentes, impresoras y producción | `pedido_item` (1) → `trabajo_impresion` (0..N) | `trabajo_impresion.id_pedido_item` → `pedido_item.id_pedido_item` | pedido_item |
| Agentes, impresoras y producción | `trabajo_impresion` (0..1) → `trabajo_impresion` (0..N) | `trabajo_impresion.id_trabajo_origen` → `trabajo_impresion.id_trabajo_impresion` | reimprime |
| Agentes, impresoras y producción | `usuario` (0..1) → `trabajo_impresion` (0..N) | `trabajo_impresion.id_usuario_asignador` → `usuario.id_usuario` | usuario_asignador |
| Reclamos y compensaciones | `archivo_almacenado` (1) → `evidencia_reclamo` (0..1) | `evidencia_reclamo.id_archivo_almacenado` → `archivo_almacenado.id_archivo_almacenado` | archivo_almacenado |
| Reclamos y compensaciones | `reclamo` (1) → `evidencia_reclamo` (0..N) | `evidencia_reclamo.id_reclamo` → `reclamo.id_reclamo` | reclamo |
| Reclamos y compensaciones | `usuario` (1) → `evidencia_reclamo` (0..N) | `evidencia_reclamo.id_usuario_cargador` → `usuario.id_usuario` | usuario_cargador |
| Reclamos y compensaciones | `reclamo` (1) → `historial_reclamo` (0..N) | `historial_reclamo.id_reclamo` → `reclamo.id_reclamo` | reclamo |
| Reclamos y compensaciones | `usuario` (0..1) → `historial_reclamo` (0..N) | `historial_reclamo.id_usuario_actor` → `usuario.id_usuario` | usuario_actor |
| Reclamos y compensaciones | `pedido_item` (1) → `reclamo_item` (0..N) | `reclamo_item.id_pedido_item` → `pedido_item.id_pedido_item` | pedido_item |
| Reclamos y compensaciones | `reclamo` (1) → `reclamo_item` (0..N) | `reclamo_item.id_reclamo` → `reclamo.id_reclamo` | reclamo |
| Reclamos y compensaciones | `pedido` (1) → `reclamo` (0..N) | `reclamo.id_pedido` → `pedido.id_pedido` | pedido |
| Reclamos y compensaciones | `usuario` (1) → `reclamo` (0..N) | `reclamo.id_usuario_solicitante` → `usuario.id_usuario` | usuario_solicitante |
| Reclamos y compensaciones | `pedido_item` (0..1) → `resolucion_reclamo` (0..N) | `resolucion_reclamo.id_pedido_item` → `pedido_item.id_pedido_item` | pedido_item |
| Reclamos y compensaciones | `reclamo` (1) → `resolucion_reclamo` (0..N) | `resolucion_reclamo.id_reclamo` → `reclamo.id_reclamo` | reclamo |
| Reclamos y compensaciones | `reembolso` (0..1) → `resolucion_reclamo` (0..1) | `resolucion_reclamo.id_reembolso` → `reembolso.id_reembolso` | reembolso |
| Reclamos y compensaciones | `trabajo_impresion` (0..1) → `resolucion_reclamo` (0..1) | `resolucion_reclamo.id_trabajo_impresion` → `trabajo_impresion.id_trabajo_impresion` | trabajo_impresion |
| Reclamos y compensaciones | `usuario` (1) → `resolucion_reclamo` (0..N) | `resolucion_reclamo.id_usuario_resolvedor` → `usuario.id_usuario` | usuario_resolvedor |
| Instalación, licencias e integraciones | `integracion_externa` (1) → `conciliacion_integracion` (0..N) | `conciliacion_integracion.id_integracion_externa` → `integracion_externa.id_integracion_externa` | integracion_externa |
| Instalación, licencias e integraciones | `integracion_externa` (1) → `evento_integracion` (0..N) | `evento_integracion.id_integracion_externa` → `integracion_externa.id_integracion_externa` | integracion_externa |
| Instalación, licencias e integraciones | `licencia_local` (1) → `licencia_modulo` (0..N) | `licencia_modulo.id_licencia_local` → `licencia_local.id_licencia_local` | licencia_local |
| Instalación, licencias e integraciones | `modulo` (1) → `licencia_modulo` (0..N) | `licencia_modulo.id_modulo` → `modulo.id_modulo` | modulo |



## 7. Reglas transversales e invariantes

### 7.1 Identidad y alcance

- Una cuenta tiene un solo rol principal. Una cuenta personal y una empresarial no comparten identidad ni correo.
- Los roles empresariales exigen `empresa_cliente`; los restantes la prohíben. Todos los usuarios empresariales ven pedidos, documentos y cuenta de su empresa.
- Solo `CLIENTE_EMPRESA_ADMIN` administra usuarios comunes de su empresa; no puede crear otro administrador empresarial. Si pierde acceso, un administrador de la imprenta restablece sus credenciales.
- SOPORTE es una cuenta administrativa reservada, inicialmente bloqueada. Una autorización explícita dura como máximo 24 horas y todo acceso queda auditado.
- Existe un único administrador propietario activo. La llave maestra se verifica contra el servicio seguro de YG; la base local no guarda su texto plano ni un secreto recuperable.
- Un empleado activo tiene al menos una sucursal activa y puede tener varias. Los permisos financieros son globales para la imprenta; la operación corriente se filtra por sus sucursales.

### 7.2 Configuración

- El primer inicio crea catálogos técnicos fijos y una configuración `EN_PREPARACION` vacía. No hay precios, plantilla comercial ni reglas predeterminadas de YG.
- Antes de la primera activación puede haber cero configuraciones activas. Luego existe exactamente una `ACTIVA` y, como máximo, una `EN_PREPARACION`.
- Activar requiere un administrador o SOPORTE habilitado, reingreso de contraseña y efecto inmediato. No existe activación programada.
- Una versión activa o histórica es inmutable. La siguiente versión es una copia completa normalizada.
- La activación no exige impresoras. Sin ellas, cotización, pedido, pago, producción manual, calidad y entrega siguen funcionando.
- Catálogo y precios son globales; `sucursal_servicio` limita lo que cada sucursal puede realizar. Una impresora física solo participa en automatización si la versión activa la habilita.
- Las reglas comerciales son datos estructurados, nunca SQL ni código. Sus condiciones se combinan con AND; alternativas requieren reglas separadas.
- Cada regla tiene una sola acción. Por item se usa el mayor descuento porcentual y el mayor requisito de seña. El orden es precio base, descuento, subtotal y seña.
- En modo `MANUAL`, todo pedido espera decisión interna. En `AUTOMATICA`, todo pedido elegible intenta aprobación técnica. En `CONDICIONAL`, ese intento ocurre solamente si coincide al menos una `regla_aprobacion`; dentro de cada regla las condiciones son AND y las reglas son alternativas.

### 7.3 Cotización, pedido y archivos

- Una cotización se confirma una sola vez y produce exactamente un pedido. Cambiar archivo, impresión, servicio, entrega o pago cancela la cotización y crea otra enlazada.
- La cotización no reserva crédito ni capacidad de entrega. La confirmación valida ambos atómicamente.
- Un pedido personal pertenece al usuario; uno empresarial pertenece a la empresa y conserva el creador. Solo el creador, el administrador empresarial o personal interno autorizado puede modificarlo según etapa.
- Cada item tiene un único archivo original y una configuración homogénea. No hay rangos de páginas ni modo mixto; blanco y negro y color del mismo archivo requieren pedidos distintos.
- Una excepción manual de precio o descuento conserva en el item el valor configurado, el aplicado, el actor y el motivo. El permiso se valida fuera del cálculo.
- PDF se valida directamente. DOCX y ODT se convierten en el servidor a PDF canónico; cálculos, preview y CUPS usan ese PDF. El cliente acepta la vista previa antes de confirmar.
- El MIME real se detecta por contenido. Malware, cifrado, corrupción o contenido activo inseguro rechazan; diferencias de renderizado requieren revisión manual.
- La preparación ajusta proporcionalmente al área imprimible sin recortar. Escala real o recorte especial requieren revisión humana.
- Un archivo de trabajo pertenece a cotización o pedido, nunca a ambos. La confirmación transfiere sus relaciones en una transacción. Reemplazar crea un nuevo objeto y conserva el anterior.
- La aprobación automática solo elimina la decisión humana: jamás omite límites, bloqueo, licencia, pago, compatibilidad, sucursal o controles técnicos.

### 7.4 Producción

- Un agente pertenece a una sucursal y controla varias impresoras; cada impresora pertenece al mismo ámbito físico.
- `ACTIVA`, `MANTENIMIENTO` y `FUERA_DE_SERVICIO` son estados operativos. ONLINE/OFFLINE se calcula desde el último contacto.
- Un item tiene historial de trabajos y como máximo uno activo. Sus copias nunca se dividen entre impresoras.
- En modo manual, agente e impresora son opcionales y un usuario interno registra transiciones. En modo automatizado se filtra por versión, sucursal, capacidad, estado y conexión.
- Una excepción de asignación exige compatibilidad, permiso, motivo y auditoría. No recotiza silenciosamente.
- El claim del agente usa lease atómico. Antes de CUPS puede liberarse; después de que CUPS acepta el job, un reintento requiere revisión humana para evitar duplicados.
- `COMPLETADO` solo significa que CUPS terminó. Cada trabajo completado requiere control humano; el pedido queda listo cuando la última revisión de todos los items fue aprobada.

### 7.5 Pagos, seña y cuenta corriente

- `intento_pago` no mueve dinero. `pago` nace al confirmar efectivo, transferencia o proveedor y queda inmutable.
- Transferencia informada permanece pendiente hasta validación interna. Mercado Pago y MODO confirman por webhook autenticado, verificación contra API e idempotencia; el redirect no confirma.
- Un pago tiene un único pagador y un único destino. Los pagos normales admiten varios pedidos y medios, pero no sobrepago. Cuenta corriente admite saldo a favor.
- `CUENTA_CORRIENTE` financia todo el pedido al confirmar y no se mezcla allí con otro medio. Crea un cargo; una cancelación o rechazo crea su reversión.
- `senia` identifica el requisito, pero el dinero siempre proviene de pagos y aplicaciones. Producción exige seña o prepago satisfecho; entrega exige saldo, salvo cuenta corriente.
- El saldo de cuenta se deriva de `movimiento_cuenta_corriente`; ninguna fila previa se modifica. Todo movimiento con haber disponible se aplica mediante `aplicacion_movimiento_cuenta`, con sugerencia de documentos vencidos y luego cargos antiguos.
- Superar el límite deshabilita cuenta corriente para nuevos pedidos. Un vencimiento solo genera alerta; bloquear la cuenta es decisión de un administrador o empleado financiero.
- `documento_cobro` documenta deuda existente y reserva importes; no genera deuda. Se emite sin borrador, es inmutable y se anula completo si cambia un cargo.
- Un pago aplicado al cargo reduce todos los documentos cuyas líneas lo referencian. El PDF pagado se conserva sin regenerarlo.
- Un reembolso confirmado reduce la aplicación original y nunca modifica el total histórico del pedido, pago o cargo.

### 7.6 Entrega y reclamos

- Puntos externos pueden servir a varias sucursales mediante configuración versionada. Las zonas domiciliarias son globales y no se superponen.
- Para una misma versión, modalidad, destino y día las franjas horarias no se superponen. Una única franja puede admitir varios pedidos mediante su capacidad, sin duplicar turnos.
- La capacidad cuenta pedidos completos. Cotizar muestra disponibilidad; confirmar crea una reserva. Cancelar o cambiar modalidad libera el cupo.
- Reprogramar conserva historial y exige aceptación del cliente. Una fecha manual requiere horario válido, permiso, motivo y auditoría.
- La entrega es completa. Retiro usa un código hasheado validado presencialmente; en un punto externo el empleado entrega directamente al cliente.
- Una constancia incorrecta se anula y se crea otra. No se reescribe.
- Antes de producción el cliente puede cancelar directamente. Después crea `solicitud_cancelacion` y un usuario interno decide; pagos, cargos y trabajos se revierten o cancelan con hechos nuevos.
- Un reclamo no reabre el pedido. Puede abarcar varios items y combinar o repetir acciones del mismo tipo; cada resolución es un hecho inmutable independiente y una reimpresión no crea pedido ni precio nuevo.
- Una compensación sobre cuenta corriente crea un crédito inmutable. No se modela inventario de material devuelto.

### 7.7 Notificaciones, alertas y auditoría

- La notificación es personal y persistente. Lectura vive en `notificacion_usuario`; eliminar del inbox borra solo esa relación después de auditarla.
- Push es un canal adicional opcional. Existe un ciclo agregado por notificación y dispositivo; sus reintentos actualizan contador y último resultado. Revocar permiso o cerrar la sesión deshabilita el dispositivo, pero no elimina la notificación interna.
- La alerta es operacional, no pertenece al inbox del cliente. Se muestra junto a su causa y en la vista interna según permisos.
- Existe una alerta activa por tipo, causa y entidad. Repeticiones aumentan contador; al desaparecer la causa se resuelve sin borrar historial.
- Solo un administrador, con confirmación inmediata, puede eliminar físicamente una alerta ya resuelta. Una activa no se elimina.
- Auditoría es append-only, no contiene secretos, tokens, archivos ni cuerpos brutos de webhook. Los historiales de dominio siguen siendo la fuente de verdad de las transiciones.

### 7.8 Licencias e integraciones

- YG emite seriales aditivos. La base local conserva identificador, huella, módulos y validaciones, nunca el serial plano ni la base central de YG.
- Activar y validar requiere Internet. Solo una indisponibilidad desconocida admite hasta 72 horas desde la última validación correcta; vencimiento conocido o revocación recibida son inmediatos.
- Perder un módulo bloquea operaciones nuevas y conserva datos. Pedidos confirmados se terminan; webhooks, conciliación y devoluciones pendientes siguen procesándose.
- Mercado Pago y MODO usan la cuenta comercial de la imprenta. Los webhooks llegan directamente a cada instalación y una conciliación periódica repara eventos omitidos; no existe relay central de YG.


## 8. Valores cerrados principales

| Campo | Valores |
|---|---|
| `usuario.estado` | `PENDIENTE_VERIFICACION`, `ACTIVO`, `BLOQUEADO`, `DESACTIVADO` |
| `rol.codigo` | `CLIENTE`, `EMPLEADO`, `ADMINISTRADOR`, `CLIENTE_EMPRESA`, `CLIENTE_EMPRESA_ADMIN` |
| `configuracion_version.estado` | `EN_PREPARACION`, `ACTIVA`, `HISTORICA` |
| `politica_operativa.modo_aprobacion` | `MANUAL`, `AUTOMATICA`, `CONDICIONAL` |
| `politica_operativa.modo_asignacion_impresora` | `MANUAL`, `AUTOMATICA` |
| `servicio.tipo` | `IMPRESION`, `TERMINACION` |
| `tarifa_impresion.modo_color` | `BLANCO_NEGRO`, `COLOR` |
| `configuracion_medio_pago.medio_pago` | `EFECTIVO`, `TRANSFERENCIA`, `MERCADO_PAGO`, `MODO`, `CUENTA_CORRIENTE` |
| `configuracion_modalidad_entrega.modalidad` | `RETIRO_SUCURSAL`, `RETIRO_PUNTO_ENTREGA`, `ENVIO_DOMICILIO` |
| `cotizacion.estado` | `BORRADOR`, `VIGENTE`, `CONFIRMADA`, `EXPIRADA`, `CANCELADA` |
| `pedido.estado_interno` | `PENDIENTE_PAGO`, `PENDIENTE_REVISION`, `CORRECCION_SOLICITADA`, `APROBADO`, `EN_PRODUCCION`, `LISTO_PARA_ENTREGA`, `ENTREGADO`, `CERRADO`, `RECHAZADO`, `CANCELADO` |
| `solicitud_correccion.estado` | `PENDIENTE`, `RESPONDIDA`, `CERRADA`, `CANCELADA` |
| `solicitud_cancelacion.estado` | `PENDIENTE`, `APROBADA`, `RECHAZADA`, `CANCELADA` |
| `archivo_almacenado.estado` | `CARGADO`, `VALIDANDO`, `VALIDO`, `REQUIERE_REVISION`, `RECHAZADO`, `PURGADO` |
| `trabajo_impresion.estado` | `PENDIENTE`, `ASIGNADO`, `EN_COLA`, `IMPRIMIENDO`, `COMPLETADO`, `ERROR`, `CANCELACION_SOLICITADA`, `CANCELADO` |
| `control_calidad.resultado` | `APROBADO`, `REIMPRESION_REQUERIDA`, `INCIDENCIA` |
| `intento_pago.estado` | `INICIADO`, `PENDIENTE`, `CONFIRMADO`, `CANCELADO`, `EXPIRADO`, `FALLIDO` |
| `senia.estado_derivado` | `PENDIENTE`, `PARCIAL`, `CUBIERTA`, `REVERTIDA` |
| `reembolso.estado` | `PENDIENTE`, `CONFIRMADO`, `FALLIDO`, `CANCELADO` |
| `cuenta_corriente.estado` | `ACTIVA`, `BLOQUEADA`, `CERRADA` |
| `documento_cobro.estado` | `EMITIDO`, `ANULADO` |
| `documento_cobro.estado_pago_derivado` | `PENDIENTE`, `PARCIAL`, `PAGADO` |
| `reserva_entrega.estado` | `ACTIVA`, `LIBERADA`, `CUMPLIDA` |
| `reclamo.estado` | `ABIERTO`, `EN_REVISION`, `RESUELTO`, `RECHAZADO`, `CANCELADO` |
| `alerta.severidad` | `ADVERTENCIA`, `CRITICA` |
| `alerta.estado` | `ACTIVA`, `RESUELTA` |
| `licencia_local.estado` | `ACTIVA`, `VENCIDA`, `REVOCADA`, `NO_VALIDADA` |



## 9. Datos derivados y cachés verificables

| Dato | Fuente de cálculo |
|---|---|
| Conectividad de impresora | `ultima_observacion_online`, umbral técnico y estado del agente. |
| Situación financiera del pedido | Total confirmado menos aplicaciones y reembolsos válidos. |
| Estado de seña | `importe_requerido` frente a aplicaciones `SENA` menos devoluciones. |
| Saldo de cuenta corriente | Suma de debe menos haber del libro inmutable. |
| Crédito disponible | Límite menos deuda neta; el saldo a favor aumenta disponibilidad. |
| Pago y vencimiento de documento | Aplicaciones a sus cargos, total de líneas, fecha actual y estado fijo. |
| Cupo de franja | Capacidad efectiva menos reservas `ACTIVA` para fecha y ventana. |
| Estado visible del pedido | Proyección controlada de `estado_interno`, sin flujo independiente editable. |
| Pedido listo para entrega | Último control de cada item aprobado. |
| Módulo habilitado | Al menos una licencia vigente que lo conceda. |

Los campos terminados en `_derivado` son cachés para consulta y deben poder reconstruirse. No sustituyen la fuente indicada.

## 10. Conservación y borrado

| Información | Política |
|---|---|
| Usuarios, empresas, sucursales, catálogos e impresoras | Baja lógica o anonimización según obligación; no se rompen referencias históricas. |
| Configuraciones activas e históricas | Inmutables y sin borrado ordinario. |
| Movimientos, pagos, aplicaciones, documentos y reembolsos | Append-only; corregir mediante reversión, anulación o nuevo hecho. |
| Archivos de trabajo | Purga del objeto al vencer la retención; permanecen metadatos, hash y validaciones. |
| Cotización expirada | Conserva metadatos y snapshots; sus binarios temporales se purgan y no se reutilizan. |
| Documentos de cobro y comprobantes | Sin purga automática. |
| Evidencias de reclamo | Nunca antes de resolver; después respetan el plazo aplicable del pedido. |
| Notificación de inbox | El usuario puede borrar físicamente solo su relación; el acto se audita. |
| Alerta | Solo un administrador elimina una resuelta, con confirmación y auditoría. |
| Auditoría | Sin actualización, hard delete ni vencimiento automático desde la aplicación. |

## 11. Acceso y visibilidad

| Actor | Alcance principal |
|---|---|
| Cliente personal | Sus cotizaciones, pedidos, pagos, entregas, reclamos y notificaciones. |
| Usuario de empresa | Historial comercial y financiero compartido de su empresa; notificaciones propias. |
| Administrador empresarial | Además administra usuarios comunes de su empresa. |
| Empleado | Sucursales asignadas; finanzas solo con permiso explícito. |
| Administrador de imprenta | Configuración, usuarios, operación, finanzas, alertas y auditoría. |
| Administrador propietario | Puede solicitar eliminación de otros administradores tras verificación externa de llave maestra. |
| SOPORTE | Facultades administrativas solo durante una autorización vigente y auditada. |
| Agente de impresión | Claim y transiciones técnicas autorizadas de su sucursal; nunca funciones comerciales. |

## 12. Límites externos y fuera de alcance

- La base central de YG para emitir licencias, verificar identidad y restablecer la llave maestra pertenece a otro sistema.
- Storage de objetos, CUPS, conversor ofimático, antivirus, correo, push, Mercado Pago y MODO son dependencias externas; este DER guarda referencias y resultados mínimos.
- No se modelan inventario, compras, proveedores de insumos, ARCA, factura fiscal, IVA calculado, retenciones, comisiones de proveedor, mapas, distancias ni relay central de webhooks.
- No hay multi-tenant dentro de una base, sucursales del cliente empresarial, entregas parciales, configuración programada ni cliente instalado por sucursal.
- Este artefacto diseña datos. Migraciones SQL, entidades JPA, repositorios, servicios y el diagrama de arquitectura corresponden a tareas posteriores.


## 13. Controles para una implementación futura

Al traducir el diseño a migraciones deben materializarse las unicidades compuestas, índices parciales de “una activa”, checks XOR, importes no negativos, límites de suma y transacciones que abarcan confirmación de cotización, transferencia de archivos, crédito y cupo. Las operaciones sensibles deben bloquear las filas relevantes para evitar confirmaciones concurrentes.

Los nombres y tipos de este diccionario son la referencia inicial. Si una limitación real de PostgreSQL, Spring Boot o un proveedor exige cambiar alcance, comportamiento, seguridad, esquema o criterio de aceptación, corresponde revisar este diseño antes de implementar; no debe corregirse silenciosamente en código.
