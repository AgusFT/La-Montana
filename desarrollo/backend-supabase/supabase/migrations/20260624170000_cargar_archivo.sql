-- Edge Function cargar-archivo y metadata transaccional.
-- Issue relacionada: #111.

alter table public.archivo
  add column if not exists tamano_original_bytes bigint;

update public.archivo
set tamano_original_bytes = tamano_bytes
where tamano_original_bytes is null;

alter table public.archivo
  alter column tamano_original_bytes set not null;

alter table public.archivo
  add constraint archivo_tamano_original_bytes_mvp_check check (
    tamano_original_bytes > 0
    and tamano_original_bytes <= public.limite_bytes_archivo_mvp()
  );

comment on column public.archivo.tamano_bytes
  is 'Tamanio en bytes del ciphertext almacenado en Storage privado.';

comment on column public.archivo.tamano_original_bytes
  is 'Tamanio en bytes del PDF original antes del cifrado cliente.';

create or replace function public.pedido_archivo_es_editable_mvp(
  estado_interno_consulta text,
  estado_visible_cliente_consulta text
)
returns boolean
language sql
immutable
set search_path = public
as $$
  select estado_interno_consulta = 'pendiente_revision'
    and estado_visible_cliente_consulta = 'pendiente_revision'
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

  select p.estado_interno, p.estado_visible_cliente
  into pedido_estado_interno, pedido_estado_visible_cliente
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
    pedido_estado_visible_cliente
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

  select p.estado_interno, p.estado_visible_cliente
  into pedido_estado_interno, pedido_estado_visible_cliente
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
    pedido_estado_visible_cliente
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

drop view if exists public.archivo_cliente;

create view public.archivo_cliente
with (security_barrier = true)
as
select
  a.id_archivo,
  a.id_pedido,
  a.nombre_original,
  a.mime_original,
  a.tamano_bytes,
  a.tamano_original_bytes,
  a.estado_archivo,
  a.fecha_creacion,
  a.fecha_modificacion,
  a.eliminado,
  a.version_fila
from public.archivo a
join public.pedido p on p.id_pedido = a.id_pedido
where (a.id_usuario = public.usuario_actual_id() or p.id_usuario = public.usuario_actual_id())
  and a.eliminado = false
  and p.eliminado = false;

revoke all on function public.pedido_archivo_es_editable_mvp(text, text) from public;
revoke all on function public.preparar_carga_archivo_cliente(uuid, bigint, bigint, bigint, text) from public;
revoke all on function public.registrar_archivo_pedido_cliente(
  uuid,
  bigint,
  text,
  text,
  bigint,
  bigint,
  text,
  text,
  text,
  text,
  text,
  text,
  text
) from public;

revoke all on public.archivo_cliente from anon, authenticated;

grant execute on function public.pedido_archivo_es_editable_mvp(text, text) to service_role;
grant execute on function public.preparar_carga_archivo_cliente(uuid, bigint, bigint, bigint, text) to service_role;
grant execute on function public.registrar_archivo_pedido_cliente(
  uuid,
  bigint,
  text,
  text,
  bigint,
  bigint,
  text,
  text,
  text,
  text,
  text,
  text,
  text
) to service_role;

grant select on public.archivo_cliente to authenticated;

comment on function public.preparar_carga_archivo_cliente(uuid, bigint, bigint, bigint, text)
  is 'Valida usuario, pedido editable y limites antes de subir ciphertext al bucket privado.';

comment on function public.registrar_archivo_pedido_cliente(
  uuid,
  bigint,
  text,
  text,
  bigint,
  bigint,
  text,
  text,
  text,
  text,
  text,
  text,
  text
) is 'Registra metadata segura de un archivo cifrado ya subido a Storage y audita archivo_pedido_cargado.';
