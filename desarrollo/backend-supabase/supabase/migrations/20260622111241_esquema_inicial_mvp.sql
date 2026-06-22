-- Migracion inicial del esquema MVP de La Montana.
-- Fuente: guia-estilo-bdd.md, modelo-tablas-relaciones-supabase.md y DER #121.
-- Issue relacionada: #95.

create table public.rol (
  id_rol bigint generated always as identity,
  codigo text not null,
  nombre text not null,
  descripcion text,
  activo boolean not null default true,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint rol_pkey primary key (id_rol),
  constraint rol_codigo_unico unique (codigo),
  constraint rol_codigo_no_vacio_check check (btrim(codigo) <> ''),
  constraint rol_nombre_no_vacio_check check (btrim(nombre) <> ''),
  constraint rol_version_fila_check check (version_fila >= 1)
);

create table public.permiso (
  id_permiso bigint generated always as identity,
  codigo text not null,
  nombre text not null,
  descripcion text,
  activo boolean not null default true,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint permiso_pkey primary key (id_permiso),
  constraint permiso_codigo_unico unique (codigo),
  constraint permiso_codigo_no_vacio_check check (btrim(codigo) <> ''),
  constraint permiso_nombre_no_vacio_check check (btrim(nombre) <> ''),
  constraint permiso_version_fila_check check (version_fila >= 1)
);

create table public.rol_permiso (
  id_rol_permiso bigint generated always as identity,
  id_rol bigint not null,
  id_permiso bigint not null,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint rol_permiso_pkey primary key (id_rol_permiso),
  constraint rol_permiso_id_rol_fk foreign key (id_rol) references public.rol (id_rol) on delete restrict,
  constraint rol_permiso_id_permiso_fk foreign key (id_permiso) references public.permiso (id_permiso) on delete restrict,
  constraint rol_permiso_version_fila_check check (version_fila >= 1)
);

create table public.usuario (
  id_usuario bigint generated always as identity,
  id_usuario_auth uuid not null,
  id_rol bigint not null,
  nombre text not null,
  apellido text,
  email text not null,
  estado text not null default 'activo',
  observacion text,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint usuario_pkey primary key (id_usuario),
  constraint usuario_id_usuario_auth_unico unique (id_usuario_auth),
  constraint usuario_email_unico unique (email),
  constraint usuario_id_usuario_auth_fk foreign key (id_usuario_auth) references auth.users (id) on delete restrict,
  constraint usuario_id_rol_fk foreign key (id_rol) references public.rol (id_rol) on delete restrict,
  constraint usuario_nombre_no_vacio_check check (btrim(nombre) <> ''),
  constraint usuario_email_no_vacio_check check (btrim(email) <> ''),
  constraint usuario_estado_no_vacio_check check (btrim(estado) <> ''),
  constraint usuario_version_fila_check check (version_fila >= 1)
);

create table public.contacto (
  id_contacto bigint generated always as identity,
  id_usuario bigint not null,
  tipo_contacto text not null,
  valor text not null,
  principal boolean not null default false,
  verificado boolean not null default false,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint contacto_pkey primary key (id_contacto),
  constraint contacto_id_usuario_fk foreign key (id_usuario) references public.usuario (id_usuario) on delete restrict,
  constraint contacto_tipo_contacto_no_vacio_check check (btrim(tipo_contacto) <> ''),
  constraint contacto_valor_no_vacio_check check (btrim(valor) <> ''),
  constraint contacto_version_fila_check check (version_fila >= 1)
);

create table public.direccion (
  id_direccion bigint generated always as identity,
  id_usuario bigint not null,
  calle text not null,
  numero text,
  piso text,
  departamento text,
  localidad text not null,
  provincia text not null,
  codigo_postal text,
  referencia text,
  principal boolean not null default false,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint direccion_pkey primary key (id_direccion),
  constraint direccion_id_usuario_fk foreign key (id_usuario) references public.usuario (id_usuario) on delete restrict,
  constraint direccion_calle_no_vacio_check check (btrim(calle) <> ''),
  constraint direccion_localidad_no_vacio_check check (btrim(localidad) <> ''),
  constraint direccion_provincia_no_vacio_check check (btrim(provincia) <> ''),
  constraint direccion_version_fila_check check (version_fila >= 1)
);

create table public.punto_entrega (
  id_punto_entrega bigint generated always as identity,
  nombre text not null,
  descripcion text,
  direccion_texto text not null,
  horario_atencion text,
  activo boolean not null default true,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint punto_entrega_pkey primary key (id_punto_entrega),
  constraint punto_entrega_nombre_no_vacio_check check (btrim(nombre) <> ''),
  constraint punto_entrega_direccion_texto_no_vacio_check check (btrim(direccion_texto) <> ''),
  constraint punto_entrega_version_fila_check check (version_fila >= 1)
);

create table public.servicio (
  id_servicio bigint generated always as identity,
  codigo text not null,
  nombre text not null,
  descripcion text,
  precio_base numeric(12, 2) not null default 0,
  tipo_moneda text not null default 'ARS',
  activo boolean not null default true,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint servicio_pkey primary key (id_servicio),
  constraint servicio_codigo_unico unique (codigo),
  constraint servicio_codigo_no_vacio_check check (btrim(codigo) <> ''),
  constraint servicio_nombre_no_vacio_check check (btrim(nombre) <> ''),
  constraint servicio_precio_base_check check (precio_base >= 0),
  constraint servicio_tipo_moneda_check check (tipo_moneda in ('ARS', 'USD')),
  constraint servicio_version_fila_check check (version_fila >= 1)
);

create table public.pedido (
  id_pedido bigint generated always as identity,
  id_usuario bigint not null,
  id_punto_entrega bigint,
  codigo text not null,
  descripcion text,
  observacion_cliente text,
  observacion_interna text,
  cantidad_estimada integer,
  cantidad_carillas integer,
  estado_interno text not null default 'pendiente_revision',
  estado_visible_cliente text not null default 'pendiente_revision',
  estado_financiero text not null default 'pendiente_evaluacion',
  requiere_senia boolean not null default false,
  porcentaje_senia numeric(5, 2) not null default 0,
  total_estimado numeric(12, 2),
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint pedido_pkey primary key (id_pedido),
  constraint pedido_codigo_unico unique (codigo),
  constraint pedido_id_usuario_fk foreign key (id_usuario) references public.usuario (id_usuario) on delete restrict,
  constraint pedido_id_punto_entrega_fk foreign key (id_punto_entrega) references public.punto_entrega (id_punto_entrega) on delete restrict,
  constraint pedido_codigo_no_vacio_check check (btrim(codigo) <> ''),
  constraint pedido_cantidad_estimada_check check (cantidad_estimada is null or cantidad_estimada >= 0),
  constraint pedido_cantidad_carillas_check check (cantidad_carillas is null or cantidad_carillas >= 0),
  constraint pedido_estado_interno_no_vacio_check check (btrim(estado_interno) <> ''),
  constraint pedido_estado_visible_cliente_no_vacio_check check (btrim(estado_visible_cliente) <> ''),
  constraint pedido_estado_financiero_no_vacio_check check (btrim(estado_financiero) <> ''),
  constraint pedido_porcentaje_senia_check check (porcentaje_senia >= 0 and porcentaje_senia <= 100),
  constraint pedido_total_estimado_check check (total_estimado is null or total_estimado >= 0),
  constraint pedido_version_fila_check check (version_fila >= 1)
);

create table public.pedido_servicio (
  id_pedido_servicio bigint generated always as identity,
  id_pedido bigint not null,
  id_servicio bigint not null,
  cantidad numeric(12, 2) not null default 1,
  precio_unitario_aplicado numeric(12, 2) not null,
  subtotal numeric(12, 2) not null,
  observacion text,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint pedido_servicio_pkey primary key (id_pedido_servicio),
  constraint pedido_servicio_id_pedido_fk foreign key (id_pedido) references public.pedido (id_pedido) on delete restrict,
  constraint pedido_servicio_id_servicio_fk foreign key (id_servicio) references public.servicio (id_servicio) on delete restrict,
  constraint pedido_servicio_cantidad_check check (cantidad > 0),
  constraint pedido_servicio_precio_unitario_aplicado_check check (precio_unitario_aplicado >= 0),
  constraint pedido_servicio_subtotal_check check (subtotal >= 0),
  constraint pedido_servicio_version_fila_check check (version_fila >= 1)
);

create table public.archivo (
  id_archivo bigint generated always as identity,
  id_pedido bigint not null,
  id_usuario bigint not null,
  nombre_original text not null,
  mime_original text not null,
  tamano_bytes bigint not null,
  hash_archivo text,
  bucket text not null,
  ruta_storage text not null,
  clave_envuelta text,
  iv text,
  version_cifrado text,
  estado_archivo text not null default 'registrado',
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint archivo_pkey primary key (id_archivo),
  constraint archivo_bucket_ruta_storage_unico unique (bucket, ruta_storage),
  constraint archivo_id_pedido_fk foreign key (id_pedido) references public.pedido (id_pedido) on delete restrict,
  constraint archivo_id_usuario_fk foreign key (id_usuario) references public.usuario (id_usuario) on delete restrict,
  constraint archivo_nombre_original_no_vacio_check check (btrim(nombre_original) <> ''),
  constraint archivo_mime_original_no_vacio_check check (btrim(mime_original) <> ''),
  constraint archivo_tamano_bytes_check check (tamano_bytes >= 0),
  constraint archivo_bucket_no_vacio_check check (btrim(bucket) <> ''),
  constraint archivo_ruta_storage_no_vacio_check check (btrim(ruta_storage) <> ''),
  constraint archivo_estado_archivo_no_vacio_check check (btrim(estado_archivo) <> ''),
  constraint archivo_version_fila_check check (version_fila >= 1)
);

create table public.pago (
  id_pago bigint generated always as identity,
  id_usuario_registro bigint,
  monto numeric(12, 2) not null,
  tipo_moneda text not null default 'ARS',
  medio_pago text not null,
  estado text not null default 'pendiente',
  fecha_pago timestamptz,
  referencia text,
  observacion text,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint pago_pkey primary key (id_pago),
  constraint pago_id_usuario_registro_fk foreign key (id_usuario_registro) references public.usuario (id_usuario) on delete restrict,
  constraint pago_monto_check check (monto >= 0),
  constraint pago_tipo_moneda_check check (tipo_moneda in ('ARS', 'USD')),
  constraint pago_medio_pago_no_vacio_check check (btrim(medio_pago) <> ''),
  constraint pago_estado_check check (estado in ('pendiente', 'confirmado', 'rechazado', 'anulado')),
  constraint pago_version_fila_check check (version_fila >= 1)
);

create table public.pago_pedido (
  id_pago_pedido bigint generated always as identity,
  id_pago bigint not null,
  id_pedido bigint not null,
  monto_aplicado numeric(12, 2) not null,
  tipo_aplicacion text not null,
  observacion text,
  id_usuario_creacion bigint,
  id_usuario_modificacion bigint,
  id_usuario_eliminacion bigint,
  fecha_creacion timestamptz not null default now(),
  fecha_modificacion timestamptz,
  fecha_eliminacion timestamptz,
  eliminado boolean not null default false,
  version_fila integer not null default 1,
  constraint pago_pedido_pkey primary key (id_pago_pedido),
  constraint pago_pedido_id_pago_fk foreign key (id_pago) references public.pago (id_pago) on delete restrict,
  constraint pago_pedido_id_pedido_fk foreign key (id_pedido) references public.pedido (id_pedido) on delete restrict,
  constraint pago_pedido_monto_aplicado_check check (monto_aplicado >= 0),
  constraint pago_pedido_tipo_aplicacion_no_vacio_check check (btrim(tipo_aplicacion) <> ''),
  constraint pago_pedido_version_fila_check check (version_fila >= 1)
);

create table public.auditoria (
  id_auditoria bigint generated always as identity,
  id_usuario_actor bigint,
  id_pedido bigint,
  tabla_afectada text,
  id_registro_afectado text,
  accion text not null,
  nivel text not null default 'info',
  codigo text,
  mensaje text not null,
  request_id text,
  metadata jsonb not null default '{}'::jsonb,
  fecha_creacion timestamptz not null default now(),
  constraint auditoria_pkey primary key (id_auditoria),
  constraint auditoria_id_usuario_actor_fk foreign key (id_usuario_actor) references public.usuario (id_usuario) on delete restrict,
  constraint auditoria_id_pedido_fk foreign key (id_pedido) references public.pedido (id_pedido) on delete restrict,
  constraint auditoria_accion_no_vacio_check check (btrim(accion) <> ''),
  constraint auditoria_nivel_check check (nivel in ('info', 'warning', 'error', 'critico')),
  constraint auditoria_mensaje_no_vacio_check check (btrim(mensaje) <> '')
);

alter table public.rol
  add constraint rol_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint rol_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint rol_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.permiso
  add constraint permiso_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint permiso_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint permiso_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.rol_permiso
  add constraint rol_permiso_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint rol_permiso_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint rol_permiso_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.usuario
  add constraint usuario_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint usuario_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint usuario_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.contacto
  add constraint contacto_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint contacto_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint contacto_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.direccion
  add constraint direccion_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint direccion_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint direccion_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.punto_entrega
  add constraint punto_entrega_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint punto_entrega_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint punto_entrega_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.servicio
  add constraint servicio_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint servicio_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint servicio_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.pedido
  add constraint pedido_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint pedido_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint pedido_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.pedido_servicio
  add constraint pedido_servicio_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint pedido_servicio_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint pedido_servicio_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.archivo
  add constraint archivo_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint archivo_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint archivo_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.pago
  add constraint pago_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint pago_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint pago_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

alter table public.pago_pedido
  add constraint pago_pedido_id_usuario_creacion_fk foreign key (id_usuario_creacion) references public.usuario (id_usuario) on delete set null,
  add constraint pago_pedido_id_usuario_modificacion_fk foreign key (id_usuario_modificacion) references public.usuario (id_usuario) on delete set null,
  add constraint pago_pedido_id_usuario_eliminacion_fk foreign key (id_usuario_eliminacion) references public.usuario (id_usuario) on delete set null;

create unique index idx_rol_permiso_id_rol_id_permiso_activo
  on public.rol_permiso (id_rol, id_permiso)
  where eliminado = false;

create unique index idx_contacto_id_usuario_principal_activo
  on public.contacto (id_usuario)
  where principal = true and eliminado = false;

create unique index idx_direccion_id_usuario_principal_activo
  on public.direccion (id_usuario)
  where principal = true and eliminado = false;

create index idx_usuario_id_usuario_auth on public.usuario (id_usuario_auth);
create index idx_usuario_id_rol on public.usuario (id_rol);
create index idx_contacto_id_usuario on public.contacto (id_usuario);
create index idx_direccion_id_usuario on public.direccion (id_usuario);
create index idx_pedido_id_usuario on public.pedido (id_usuario);
create index idx_pedido_id_punto_entrega on public.pedido (id_punto_entrega);
create index idx_pedido_estado_interno on public.pedido (estado_interno);
create index idx_pedido_estado_visible_cliente on public.pedido (estado_visible_cliente);
create index idx_pedido_estado_financiero on public.pedido (estado_financiero);
create index idx_pedido_servicio_id_pedido on public.pedido_servicio (id_pedido);
create index idx_pedido_servicio_id_servicio on public.pedido_servicio (id_servicio);
create index idx_archivo_id_pedido on public.archivo (id_pedido);
create index idx_archivo_id_usuario on public.archivo (id_usuario);
create index idx_pago_id_usuario_registro on public.pago (id_usuario_registro);
create index idx_pago_pedido_id_pago on public.pago_pedido (id_pago);
create index idx_pago_pedido_id_pedido on public.pago_pedido (id_pedido);
create index idx_auditoria_id_usuario_actor on public.auditoria (id_usuario_actor);
create index idx_auditoria_id_pedido on public.auditoria (id_pedido);
create index idx_auditoria_request_id on public.auditoria (request_id);
create index idx_auditoria_fecha_creacion on public.auditoria (fecha_creacion);

create or replace function public.actualizar_auditoria_base()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.fecha_modificacion = now();
  new.version_fila = old.version_fila + 1;
  return new;
end;
$$;

create trigger trg_rol_actualizar_auditoria_base
  before update on public.rol
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_permiso_actualizar_auditoria_base
  before update on public.permiso
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_rol_permiso_actualizar_auditoria_base
  before update on public.rol_permiso
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_usuario_actualizar_auditoria_base
  before update on public.usuario
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_contacto_actualizar_auditoria_base
  before update on public.contacto
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_direccion_actualizar_auditoria_base
  before update on public.direccion
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_punto_entrega_actualizar_auditoria_base
  before update on public.punto_entrega
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_servicio_actualizar_auditoria_base
  before update on public.servicio
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_pedido_actualizar_auditoria_base
  before update on public.pedido
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_pedido_servicio_actualizar_auditoria_base
  before update on public.pedido_servicio
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_archivo_actualizar_auditoria_base
  before update on public.archivo
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_pago_actualizar_auditoria_base
  before update on public.pago
  for each row execute function public.actualizar_auditoria_base();

create trigger trg_pago_pedido_actualizar_auditoria_base
  before update on public.pago_pedido
  for each row execute function public.actualizar_auditoria_base();
