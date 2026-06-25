-- Confirmacion de pedidos desde resumen cliente.
-- Issue relacionada: #112.

alter table public.pedido
  add column fecha_confirmacion_cliente timestamptz,
  add column id_usuario_confirmacion bigint;

alter table public.pedido
  add constraint pedido_id_usuario_confirmacion_fk
  foreign key (id_usuario_confirmacion) references public.usuario (id_usuario) on delete set null;

comment on column public.pedido.fecha_confirmacion_cliente
  is 'Momento en que el cliente confirma explicitamente el pedido desde el resumen.';

comment on column public.pedido.id_usuario_confirmacion
  is 'Usuario de negocio que confirmo explicitamente el pedido desde el flujo cliente.';

create index idx_pedido_fecha_confirmacion_cliente
  on public.pedido (fecha_confirmacion_cliente);

create unique index idx_auditoria_pedido_confirmado_unico
  on public.auditoria (id_pedido)
  where accion = 'pedido_confirmado';

create or replace function public.pedido_archivo_es_editable_mvp(
  estado_interno_consulta text,
  estado_visible_cliente_consulta text,
  fecha_confirmacion_cliente_consulta timestamptz
)
returns boolean
language sql
immutable
set search_path = public
as $$
  select estado_interno_consulta = 'pendiente_revision'
    and estado_visible_cliente_consulta = 'pendiente_revision'
    and fecha_confirmacion_cliente_consulta is null
$$;

create or replace function public.pedido_archivo_es_editable_mvp(
  estado_interno_consulta text,
  estado_visible_cliente_consulta text
)
returns boolean
language sql
immutable
set search_path = public
as $$
  select public.pedido_archivo_es_editable_mvp(
    estado_interno_consulta,
    estado_visible_cliente_consulta,
    null::timestamptz
  )
$$;

create or replace function public.preparar_carga_archivo_cliente(
  p_id_usuario_auth_actor uuid,
  p_id_pedido bigint,
  p_tamano_bytes bigint,
  p_tamano_original_bytes bigint,
  p_request_id text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  id_usuario_actor bigint;
  pedido_estado_interno text;
  pedido_estado_visible_cliente text;
  pedido_fecha_confirmacion_cliente timestamptz;
  id_objeto_storage uuid;
  bucket_archivo text;
  ruta_archivo text;
begin
  if p_id_usuario_auth_actor is null then
    raise exception 'Debes iniciar sesion para cargar archivos.'
      using errcode = '42501';
  end if;

  select u.id_usuario
  into id_usuario_actor
  from public.usuario u
  where u.id_usuario_auth = p_id_usuario_auth_actor
    and u.estado = 'activo'
    and u.eliminado = false
  limit 1;

  if id_usuario_actor is null then
    raise exception 'No existe usuario de negocio activo para el usuario autenticado.'
      using errcode = '42501';
  end if;

  select p.estado_interno, p.estado_visible_cliente, p.fecha_confirmacion_cliente
  into pedido_estado_interno, pedido_estado_visible_cliente, pedido_fecha_confirmacion_cliente
  from public.pedido p
  where p.id_pedido = p_id_pedido
    and p.id_usuario = id_usuario_actor
    and p.eliminado = false
  limit 1;

  if pedido_estado_interno is null then
    raise exception 'El pedido no existe o no pertenece al usuario autenticado.'
      using errcode = '42501';
  end if;

  if not public.pedido_archivo_es_editable_mvp(
    pedido_estado_interno,
    pedido_estado_visible_cliente,
    pedido_fecha_confirmacion_cliente
  ) then
    raise exception 'El pedido no esta editable para cargar archivos.'
      using errcode = 'P0001';
  end if;

  if p_tamano_bytes is null
    or p_tamano_bytes <= 0
    or p_tamano_bytes > public.limite_bytes_archivo_mvp() then
    raise exception 'El archivo cifrado supera el limite permitido.'
      using errcode = '23514';
  end if;

  if p_tamano_original_bytes is null
    or p_tamano_original_bytes <= 0
    or p_tamano_original_bytes > public.limite_bytes_archivo_mvp() then
    raise exception 'El archivo original supera el limite permitido.'
      using errcode = '23514';
  end if;

  if not public.usuario_puede_agregar_archivo(id_usuario_actor, p_tamano_bytes) then
    raise exception 'El archivo supera el limite disponible de 10 MiB por cliente.'
      using errcode = '23514';
  end if;

  id_objeto_storage = gen_random_uuid();
  bucket_archivo = public.bucket_archivos_pedidos();
  ruta_archivo = public.generar_ruta_storage_archivo(
    id_usuario_actor,
    p_id_pedido,
    id_objeto_storage
  );

  return jsonb_build_object(
    'id_usuario', id_usuario_actor,
    'id_pedido', p_id_pedido,
    'bucket', bucket_archivo,
    'ruta_storage', ruta_archivo,
    'limite_bytes', public.limite_bytes_archivo_mvp(),
    'request_id', p_request_id
  );
end;
$$;

create or replace function public.registrar_archivo_pedido_cliente(
  p_id_usuario_auth_actor uuid,
  p_id_pedido bigint,
  p_nombre_original text,
  p_mime_original text,
  p_tamano_bytes bigint,
  p_tamano_original_bytes bigint,
  p_hash_archivo text,
  p_bucket text,
  p_ruta_storage text,
  p_clave_envuelta text,
  p_iv text,
  p_version_cifrado text,
  p_request_id text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  id_usuario_actor bigint;
  pedido_estado_interno text;
  pedido_estado_visible_cliente text;
  pedido_fecha_confirmacion_cliente timestamptz;
  id_archivo_creado bigint;
  fecha_creacion_archivo timestamptz;
begin
  if p_id_usuario_auth_actor is null then
    raise exception 'Debes iniciar sesion para registrar archivos.'
      using errcode = '42501';
  end if;

  select u.id_usuario
  into id_usuario_actor
  from public.usuario u
  where u.id_usuario_auth = p_id_usuario_auth_actor
    and u.estado = 'activo'
    and u.eliminado = false
  limit 1;

  if id_usuario_actor is null then
    raise exception 'No existe usuario de negocio activo para el usuario autenticado.'
      using errcode = '42501';
  end if;

  select p.estado_interno, p.estado_visible_cliente, p.fecha_confirmacion_cliente
  into pedido_estado_interno, pedido_estado_visible_cliente, pedido_fecha_confirmacion_cliente
  from public.pedido p
  where p.id_pedido = p_id_pedido
    and p.id_usuario = id_usuario_actor
    and p.eliminado = false
  limit 1;

  if pedido_estado_interno is null then
    raise exception 'El pedido no existe o no pertenece al usuario autenticado.'
      using errcode = '42501';
  end if;

  if not public.pedido_archivo_es_editable_mvp(
    pedido_estado_interno,
    pedido_estado_visible_cliente,
    pedido_fecha_confirmacion_cliente
  ) then
    raise exception 'El pedido no esta editable para cargar archivos.'
      using errcode = 'P0001';
  end if;

  if p_bucket <> public.bucket_archivos_pedidos() then
    raise exception 'El bucket de archivo no esta permitido.'
      using errcode = '23514';
  end if;

  if not public.usuario_puede_agregar_archivo(id_usuario_actor, p_tamano_bytes) then
    raise exception 'El archivo supera el limite disponible de 10 MiB por cliente.'
      using errcode = '23514';
  end if;

  insert into public.archivo (
    id_pedido,
    id_usuario,
    nombre_original,
    mime_original,
    tamano_bytes,
    tamano_original_bytes,
    hash_archivo,
    bucket,
    ruta_storage,
    clave_envuelta,
    iv,
    version_cifrado,
    estado_archivo,
    id_usuario_creacion
  )
  values (
    p_id_pedido,
    id_usuario_actor,
    btrim(p_nombre_original),
    btrim(p_mime_original),
    p_tamano_bytes,
    p_tamano_original_bytes,
    btrim(p_hash_archivo),
    p_bucket,
    p_ruta_storage,
    btrim(p_clave_envuelta),
    btrim(p_iv),
    btrim(p_version_cifrado),
    'cargado',
    id_usuario_actor
  )
  returning id_archivo, fecha_creacion
  into id_archivo_creado, fecha_creacion_archivo;

  perform public.registrar_evento_auditoria(
    p_accion := 'archivo_pedido_cargado',
    p_mensaje := 'Archivo cifrado cargado por cliente y asociado al pedido.',
    p_nivel := 'info',
    p_codigo := 'ARCHIVO_PEDIDO_CARGADO',
    p_id_pedido := p_id_pedido,
    p_tabla_afectada := 'archivo',
    p_id_registro_afectado := id_archivo_creado::text,
    p_request_id := p_request_id,
    p_metadata := jsonb_build_object(
      'origen', 'edge_function_cargar_archivo',
      'mime_original', btrim(p_mime_original),
      'tamano_bytes', p_tamano_bytes,
      'tamano_original_bytes', p_tamano_original_bytes,
      'bucket', p_bucket,
      'estado_archivo', 'cargado'
    ),
    p_id_usuario_actor := id_usuario_actor,
    p_id_usuario_auth_actor := p_id_usuario_auth_actor
  );

  return jsonb_build_object(
    'id_archivo', id_archivo_creado,
    'id_pedido', p_id_pedido,
    'bucket', p_bucket,
    'ruta_storage', p_ruta_storage,
    'hash_archivo', btrim(p_hash_archivo),
    'estado_archivo', 'cargado',
    'tamano_bytes', p_tamano_bytes,
    'tamano_original_bytes', p_tamano_original_bytes,
    'fecha_creacion', fecha_creacion_archivo
  );
end;
$$;

create or replace function public.confirmar_pedido_cliente(
  p_id_pedido bigint,
  p_request_id text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  id_usuario_actor bigint;
  id_usuario_auth_actor uuid;
  pedido_registro public.pedido%rowtype;
  tiene_archivo_cargado boolean;
  cantidad_archivos_cargados integer;
  existe_auditoria_confirmacion boolean;
begin
  id_usuario_auth_actor := auth.uid();

  if id_usuario_auth_actor is null then
    raise exception 'Debes iniciar sesion para confirmar un pedido.'
      using errcode = '42501';
  end if;

  select u.id_usuario
  into id_usuario_actor
  from public.usuario u
  where u.id_usuario_auth = id_usuario_auth_actor
    and u.estado = 'activo'
    and u.eliminado = false
  limit 1;

  if id_usuario_actor is null then
    raise exception 'No existe usuario de negocio activo para el usuario autenticado.'
      using errcode = '42501';
  end if;

  if not public.usuario_tiene_permiso('pedidos.editar_propios') then
    raise exception 'El usuario no tiene permiso para confirmar pedidos propios.'
      using errcode = '42501';
  end if;

  if p_id_pedido is null or p_id_pedido <= 0 then
    raise exception 'El id del pedido debe ser un entero mayor a cero.'
      using errcode = '23514';
  end if;

  select p.*
  into pedido_registro
  from public.pedido p
  where p.id_pedido = p_id_pedido
    and p.eliminado = false
  for update;

  if pedido_registro.id_pedido is null
    or pedido_registro.id_usuario <> id_usuario_actor then
    raise exception 'El pedido no existe o no pertenece al usuario autenticado.'
      using errcode = 'P0002';
  end if;

  if pedido_registro.estado_interno <> 'pendiente_revision'
    or pedido_registro.estado_visible_cliente <> 'pendiente_revision' then
    raise exception 'El pedido no esta en un estado confirmable.'
      using errcode = 'P0001';
  end if;

  select exists (
    select 1
    from public.auditoria a
    where a.id_pedido = p_id_pedido
      and a.accion = 'pedido_confirmado'
  )
  into existe_auditoria_confirmacion;

  if pedido_registro.fecha_confirmacion_cliente is not null
    or existe_auditoria_confirmacion then
    return jsonb_build_object(
      'id_pedido', pedido_registro.id_pedido,
      'codigo', pedido_registro.codigo,
      'estado_visible_cliente', pedido_registro.estado_visible_cliente,
      'estado_interno', pedido_registro.estado_interno,
      'estado_financiero', pedido_registro.estado_financiero,
      'fecha_confirmacion_cliente', pedido_registro.fecha_confirmacion_cliente,
      'confirmado', true,
      'ya_confirmado', true
    );
  end if;

  if pedido_registro.cantidad_carillas is null
    or pedido_registro.cantidad_carillas <= 0 then
    raise exception 'La cantidad de carillas del pedido debe ser mayor a cero.'
      using errcode = '23514';
  end if;

  if pedido_registro.cantidad_copias is null
    or pedido_registro.cantidad_copias <= 0 then
    raise exception 'La cantidad de copias del pedido debe ser mayor a cero.'
      using errcode = '23514';
  end if;

  if pedido_registro.tamano_hoja is null
    or pedido_registro.tamano_hoja not in ('A4', 'A3', 'OFICIO') then
    raise exception 'El tamanio de hoja del pedido no esta permitido.'
      using errcode = '23514';
  end if;

  if pedido_registro.tipo_impresion is null
    or pedido_registro.tipo_impresion not in ('byn', 'color') then
    raise exception 'El tipo de impresion del pedido no esta permitido.'
      using errcode = '23514';
  end if;

  select count(*)::integer
  into cantidad_archivos_cargados
  from public.archivo a
  where a.id_pedido = p_id_pedido
    and a.id_usuario = id_usuario_actor
    and a.estado_archivo = 'cargado'
    and a.eliminado = false;

  tiene_archivo_cargado := cantidad_archivos_cargados > 0;

  if not tiene_archivo_cargado then
    raise exception 'El pedido debe tener al menos un archivo cargado antes de confirmarse.'
      using errcode = '23514';
  end if;

  update public.pedido
  set
    estado_interno = 'pendiente_revision',
    estado_visible_cliente = 'pendiente_revision',
    fecha_confirmacion_cliente = now(),
    id_usuario_confirmacion = id_usuario_actor,
    id_usuario_modificacion = id_usuario_actor
  where id_pedido = p_id_pedido
  returning *
  into pedido_registro;

  perform public.registrar_evento_auditoria(
    p_accion := 'pedido_confirmado',
    p_mensaje := 'Pedido confirmado por cliente desde resumen y pendiente de revision administrativa.',
    p_nivel := 'info',
    p_codigo := 'PEDIDO_CONFIRMADO',
    p_id_pedido := p_id_pedido,
    p_tabla_afectada := 'pedido',
    p_id_registro_afectado := p_id_pedido::text,
    p_request_id := p_request_id,
    p_metadata := jsonb_build_object(
      'origen', 'edge_function_confirmar_pedido',
      'estado_interno', pedido_registro.estado_interno,
      'estado_visible_cliente', pedido_registro.estado_visible_cliente,
      'estado_financiero', pedido_registro.estado_financiero,
      'cantidad_archivos_cargados', cantidad_archivos_cargados
    ),
    p_id_usuario_actor := id_usuario_actor,
    p_id_usuario_auth_actor := id_usuario_auth_actor
  );

  return jsonb_build_object(
    'id_pedido', pedido_registro.id_pedido,
    'codigo', pedido_registro.codigo,
    'estado_visible_cliente', pedido_registro.estado_visible_cliente,
    'estado_interno', pedido_registro.estado_interno,
    'estado_financiero', pedido_registro.estado_financiero,
    'fecha_confirmacion_cliente', pedido_registro.fecha_confirmacion_cliente,
    'confirmado', true,
    'ya_confirmado', false
  );
end;
$$;

drop view if exists public.pedido_cliente;

create view public.pedido_cliente
with (security_barrier = true)
as
select
  p.id_pedido,
  p.id_punto_entrega,
  p.codigo,
  p.descripcion,
  p.observacion_cliente,
  p.cantidad_estimada,
  p.cantidad_carillas,
  p.cantidad_copias,
  p.tamano_hoja,
  p.tipo_impresion,
  p.doble_faz,
  p.encuadernado,
  p.anillado,
  p.metodo_pago_preferido,
  p.estado_visible_cliente,
  p.requiere_senia,
  p.total_estimado,
  p.fecha_confirmacion_cliente,
  p.fecha_creacion,
  p.fecha_modificacion,
  p.eliminado,
  p.version_fila
from public.pedido p
where p.id_usuario = public.usuario_actual_id()
  and p.eliminado = false;

revoke all on function public.pedido_archivo_es_editable_mvp(text, text, timestamptz) from public;
revoke all on function public.confirmar_pedido_cliente(bigint, text) from public;
revoke all on public.pedido_cliente from anon, authenticated;

grant execute on function public.pedido_archivo_es_editable_mvp(text, text, timestamptz) to service_role;
grant execute on function public.confirmar_pedido_cliente(bigint, text) to authenticated, service_role;
grant select on public.pedido_cliente to authenticated;

comment on function public.confirmar_pedido_cliente(bigint, text)
  is 'Confirma un pedido propio del cliente, exige archivo cargado y audita pedido_confirmado sin aprobar produccion.';
