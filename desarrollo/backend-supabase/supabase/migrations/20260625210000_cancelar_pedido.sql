-- Cancelacion de pedidos propios por cliente.
-- Issue relacionada: #136.

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
    'pedido_cancelado',
    'revision_pedido_iniciada',
    'pedido_aprobado',
    'pedido_rechazado',
    'estado_pedido_actualizado',
    'validacion_financiera_realizada',
    'error_operacion'
  )
$$;

create or replace function public.cancelar_pedido_cliente(
  p_id_pedido bigint,
  p_motivo text default null,
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
  estado_interno_anterior text;
  estado_visible_anterior text;
  estado_financiero_anterior text;
  motivo_normalizado text;
begin
  id_usuario_auth_actor := auth.uid();

  if id_usuario_auth_actor is null then
    raise exception 'Debes iniciar sesion para cancelar un pedido.'
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

  if not public.usuario_tiene_permiso('pedidos.cancelar_propios') then
    raise exception 'El usuario no tiene permiso para cancelar pedidos propios.'
      using errcode = '42501';
  end if;

  if p_id_pedido is null or p_id_pedido <= 0 then
    raise exception 'El id del pedido debe ser un entero mayor a cero.'
      using errcode = '23514';
  end if;

  motivo_normalizado := nullif(left(btrim(coalesce(p_motivo, '')), 300), '');

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
    raise exception 'El pedido no esta en un estado cancelable.'
      using errcode = 'P0001';
  end if;

  if pedido_registro.fecha_confirmacion_cliente is not null
    or exists (
      select 1
      from public.auditoria a
      where a.id_pedido = p_id_pedido
        and a.accion = 'pedido_confirmado'
    ) then
    raise exception 'El pedido ya fue confirmado y no puede cancelarse desde el portal cliente.'
      using errcode = 'P0001';
  end if;

  estado_interno_anterior := pedido_registro.estado_interno;
  estado_visible_anterior := pedido_registro.estado_visible_cliente;
  estado_financiero_anterior := pedido_registro.estado_financiero;

  update public.pedido
  set
    estado_interno = 'cancelado',
    estado_visible_cliente = 'cancelado',
    estado_financiero = 'cancelado',
    id_usuario_modificacion = id_usuario_actor
  where id_pedido = p_id_pedido
  returning *
  into pedido_registro;

  perform public.registrar_evento_auditoria(
    p_accion := 'pedido_cancelado',
    p_mensaje := 'Pedido cancelado por cliente antes de la confirmacion.',
    p_nivel := 'info',
    p_codigo := 'PEDIDO_CANCELADO',
    p_id_pedido := p_id_pedido,
    p_tabla_afectada := 'pedido',
    p_id_registro_afectado := p_id_pedido::text,
    p_request_id := p_request_id,
    p_metadata := jsonb_build_object(
      'origen', 'edge_function_cancelar_pedido',
      'estado_interno_anterior', estado_interno_anterior,
      'estado_visible_cliente_anterior', estado_visible_anterior,
      'estado_financiero_anterior', estado_financiero_anterior,
      'estado_interno_nuevo', pedido_registro.estado_interno,
      'estado_visible_cliente_nuevo', pedido_registro.estado_visible_cliente,
      'estado_financiero_nuevo', pedido_registro.estado_financiero,
      'motivo', motivo_normalizado
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
    'fecha_cancelacion', pedido_registro.fecha_modificacion,
    'cancelado', true
  );
end;
$$;

revoke all on function public.cancelar_pedido_cliente(bigint, text, text) from public;

grant execute on function public.cancelar_pedido_cliente(bigint, text, text) to authenticated, service_role;

comment on function public.cancelar_pedido_cliente(bigint, text, text)
  is 'Cancela un pedido propio del cliente cuando sigue pendiente de revision y aun no fue confirmado.';
