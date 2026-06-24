-- Auditoria de eventos criticos del MVP.
-- Issue relacionada: #100.

create or replace function public.accion_auditoria_mvp_valida(accion_consulta text)
returns boolean
language sql
stable
set search_path = public
as $$
  select accion_consulta in (
    'usuario_registrado',
    'perfil_creado',
    'pedido_creado',
    'archivo_pedido_cargado',
    'pedido_confirmado',
    'revision_pedido_iniciada',
    'pedido_aprobado',
    'pedido_rechazado',
    'estado_pedido_actualizado',
    'validacion_financiera_realizada',
    'error_operacion'
  )
$$;

create or replace function public.metadata_auditoria_es_segura(metadata_consulta jsonb)
returns boolean
language plpgsql
stable
set search_path = public
as $$
declare
  clave text;
  valor jsonb;
begin
  if metadata_consulta is null then
    return true;
  end if;

  if pg_column_size(metadata_consulta) > 4096 then
    return false;
  end if;

  if jsonb_typeof(metadata_consulta) = 'object' then
    for clave, valor in
      select e.key, e.value
      from jsonb_each(metadata_consulta) as e(key, value)
    loop
      if clave ~* '(^|[_-])(authorization|cookie|password|passwd|secret|token|jwt|apikey|api_key|service_role|private_key|wrapped_key|clave|clave_aes|clave_privada|iv|hash_archivo|payload|binario|base64)([_-]|$)' then
        return false;
      end if;

      if not public.metadata_auditoria_es_segura(valor) then
        return false;
      end if;
    end loop;

    return true;
  end if;

  if jsonb_typeof(metadata_consulta) = 'array' then
    for valor in
      select a.value
      from jsonb_array_elements(metadata_consulta) as a(value)
    loop
      if not public.metadata_auditoria_es_segura(valor) then
        return false;
      end if;
    end loop;

    return true;
  end if;

  return true;
end;
$$;

create or replace function public.registrar_evento_auditoria(
  p_accion text,
  p_mensaje text,
  p_nivel text default 'info',
  p_codigo text default null,
  p_id_pedido bigint default null,
  p_tabla_afectada text default null,
  p_id_registro_afectado text default null,
  p_request_id text default null,
  p_metadata jsonb default '{}'::jsonb,
  p_id_usuario_actor bigint default null,
  p_id_usuario_auth_actor uuid default null
)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  id_usuario_actor_resuelto bigint;
  rol_actor_resuelto text;
  metadata_segura jsonb;
  id_auditoria_creada bigint;
begin
  if p_accion is null or not public.accion_auditoria_mvp_valida(p_accion) then
    raise exception 'Accion de auditoria no permitida: %', p_accion
      using errcode = '22023';
  end if;

  if p_nivel is null or p_nivel not in ('info', 'warning', 'error', 'critico') then
    raise exception 'Nivel de auditoria no permitido: %', p_nivel
      using errcode = '22023';
  end if;

  if p_mensaje is null or btrim(p_mensaje) = '' then
    raise exception 'El mensaje de auditoria es obligatorio.'
      using errcode = '23514';
  end if;

  metadata_segura = coalesce(p_metadata, '{}'::jsonb);

  if jsonb_typeof(metadata_segura) <> 'object'
    or not public.metadata_auditoria_es_segura(metadata_segura) then
    raise exception 'La metadata de auditoria contiene datos no permitidos o excede el tamano maximo.'
      using errcode = '22023';
  end if;

  id_usuario_actor_resuelto = p_id_usuario_actor;

  if id_usuario_actor_resuelto is null and p_id_usuario_auth_actor is not null then
    select u.id_usuario
    into id_usuario_actor_resuelto
    from public.usuario u
    where u.id_usuario_auth = p_id_usuario_auth_actor
      and u.eliminado = false
    limit 1;
  end if;

  id_usuario_actor_resuelto = coalesce(id_usuario_actor_resuelto, public.usuario_actual_id());

  if id_usuario_actor_resuelto is null then
    raise exception 'No se pudo resolver el usuario actor para auditoria.'
      using errcode = '23502';
  end if;

  select r.codigo
  into rol_actor_resuelto
  from public.usuario u
  join public.rol r on r.id_rol = u.id_rol
  where u.id_usuario = id_usuario_actor_resuelto
    and u.eliminado = false
    and r.eliminado = false
  limit 1;

  if rol_actor_resuelto is null then
    raise exception 'No se pudo resolver el rol del usuario actor para auditoria.'
      using errcode = '23503';
  end if;

  insert into public.auditoria (
    id_usuario_actor,
    id_pedido,
    tabla_afectada,
    id_registro_afectado,
    accion,
    nivel,
    codigo,
    mensaje,
    request_id,
    metadata
  )
  values (
    id_usuario_actor_resuelto,
    p_id_pedido,
    nullif(btrim(p_tabla_afectada), ''),
    nullif(btrim(p_id_registro_afectado), ''),
    p_accion,
    p_nivel,
    nullif(btrim(p_codigo), ''),
    p_mensaje,
    nullif(btrim(p_request_id), ''),
    metadata_segura || jsonb_build_object('rol_actor', rol_actor_resuelto)
  )
  returning id_auditoria into id_auditoria_creada;

  return id_auditoria_creada;
end;
$$;

revoke all on function public.accion_auditoria_mvp_valida(text) from public;
revoke all on function public.metadata_auditoria_es_segura(jsonb) from public;
revoke all on function public.registrar_evento_auditoria(
  text,
  text,
  text,
  text,
  bigint,
  text,
  text,
  text,
  jsonb,
  bigint,
  uuid
) from public;

grant execute on function public.registrar_evento_auditoria(
  text,
  text,
  text,
  text,
  bigint,
  text,
  text,
  text,
  jsonb,
  bigint,
  uuid
) to service_role;

create or replace function public.crear_usuario_desde_auth()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  id_rol_cliente bigint;
  id_usuario_negocio bigint;
  perfil_fue_creado boolean;
  nombre_usuario text;
  apellido_usuario text;
begin
  if new.email is null or btrim(new.email) = '' then
    raise exception 'El usuario Auth debe tener email para crear usuario de negocio.'
      using errcode = '23514';
  end if;

  select r.id_rol
  into id_rol_cliente
  from public.rol r
  where r.codigo = 'cliente'
    and r.activo = true
    and r.eliminado = false
  limit 1;

  if id_rol_cliente is null then
    raise exception 'No existe rol cliente para crear usuario de negocio.'
      using errcode = '23503';
  end if;

  nombre_usuario = coalesce(
    nullif(btrim(new.raw_user_meta_data ->> 'nombre'), ''),
    nullif(btrim(new.raw_user_meta_data ->> 'name'), ''),
    nullif(btrim(split_part(new.email, '@', 1)), ''),
    'cliente'
  );

  apellido_usuario = nullif(btrim(new.raw_user_meta_data ->> 'apellido'), '');

  insert into public.usuario (
    id_usuario_auth,
    id_rol,
    nombre,
    apellido,
    email,
    estado
  )
  values (
    new.id,
    id_rol_cliente,
    nombre_usuario,
    apellido_usuario,
    lower(new.email),
    'activo'
  )
  on conflict (id_usuario_auth) do nothing
  returning id_usuario into id_usuario_negocio;

  perfil_fue_creado = id_usuario_negocio is not null;

  if id_usuario_negocio is null then
    select u.id_usuario
    into id_usuario_negocio
    from public.usuario u
    where u.id_usuario_auth = new.id
    limit 1;
  end if;

  perform public.registrar_evento_auditoria(
    p_accion := 'usuario_registrado',
    p_mensaje := 'Usuario registrado mediante Supabase Auth.',
    p_nivel := 'info',
    p_codigo := 'AUTH_USUARIO_REGISTRADO',
    p_tabla_afectada := 'auth.users',
    p_id_registro_afectado := new.id::text,
    p_metadata := jsonb_build_object(
      'origen', 'supabase_auth',
      'email_confirmado', new.email_confirmed_at is not null
    ),
    p_id_usuario_actor := id_usuario_negocio
  );

  if perfil_fue_creado then
    perform public.registrar_evento_auditoria(
      p_accion := 'perfil_creado',
      p_mensaje := 'Perfil de negocio creado para usuario autenticado.',
      p_nivel := 'info',
      p_codigo := 'AUTH_PERFIL_CREADO',
      p_tabla_afectada := 'usuario',
      p_id_registro_afectado := id_usuario_negocio::text,
      p_metadata := jsonb_build_object(
        'origen', 'trigger_auth',
        'rol_asignado', 'cliente'
      ),
      p_id_usuario_actor := id_usuario_negocio
    );
  end if;

  return new;
end;
$$;

comment on function public.registrar_evento_auditoria(
  text,
  text,
  text,
  text,
  bigint,
  text,
  text,
  text,
  jsonb,
  bigint,
  uuid
) is 'Registra eventos criticos de auditoria del MVP con acciones en espanol y metadata compacta sin secretos.';

create index if not exists idx_auditoria_accion on public.auditoria (accion);
create index if not exists idx_auditoria_codigo on public.auditoria (codigo);
