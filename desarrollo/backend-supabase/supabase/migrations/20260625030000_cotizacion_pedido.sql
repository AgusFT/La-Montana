-- Cotizacion real de pedidos desde catalogo de servicios.
-- Issue relacionada: #109.

create or replace function public.calcular_cotizacion_pedido_cliente(
  p_cantidad_carillas integer,
  p_cantidad_copias integer,
  p_tamano_hoja text,
  p_tipo_impresion text,
  p_doble_faz boolean default false,
  p_encuadernado boolean default false,
  p_anillado boolean default false
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  id_usuario_actor bigint;
  id_usuario_auth_actor uuid;
  cantidad_estimada_num numeric;
  cantidad_estimada_calculada integer;
  requiere_senia_calculada boolean;
  porcentaje_senia_calculado numeric(5, 2);
  tipo_impresion_normalizado text;
  tamano_hoja_normalizado text;
  codigo_impresion text;
  servicio_impresion public.servicio%rowtype;
  servicio_adicional public.servicio%rowtype;
  lineas_cotizacion jsonb := '[]'::jsonb;
  subtotal_linea numeric(12, 2);
  total_estimado_calculado numeric(12, 2) := 0;
  tipo_moneda_cotizacion text := 'ARS';
begin
  id_usuario_auth_actor := auth.uid();

  if id_usuario_auth_actor is null then
    raise exception 'Debes iniciar sesion para cotizar un pedido.'
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

  if not public.usuario_tiene_permiso('pedidos.crear') then
    raise exception 'El usuario no tiene permiso para cotizar pedidos.'
      using errcode = '42501';
  end if;

  if p_cantidad_carillas is null or p_cantidad_carillas <= 0 then
    raise exception 'La cantidad de carillas debe ser mayor a cero.'
      using errcode = '23514';
  end if;

  if p_cantidad_copias is null or p_cantidad_copias <= 0 then
    raise exception 'La cantidad de copias debe ser mayor a cero.'
      using errcode = '23514';
  end if;

  tamano_hoja_normalizado := upper(btrim(coalesce(p_tamano_hoja, '')));

  if tamano_hoja_normalizado not in ('A4', 'A3', 'OFICIO') then
    raise exception 'El tamanio de hoja no esta permitido.'
      using errcode = '23514';
  end if;

  tipo_impresion_normalizado := lower(btrim(coalesce(p_tipo_impresion, '')));

  if tipo_impresion_normalizado not in ('byn', 'color') then
    raise exception 'El tipo de impresion no esta permitido.'
      using errcode = '23514';
  end if;

  cantidad_estimada_num := p_cantidad_carillas::numeric * p_cantidad_copias::numeric;

  if cantidad_estimada_num > 2147483647 then
    raise exception 'La cantidad estimada supera el maximo permitido.'
      using errcode = '23514';
  end if;

  cantidad_estimada_calculada := cantidad_estimada_num::integer;
  requiere_senia_calculada := cantidad_estimada_calculada > 200;
  porcentaje_senia_calculado := case
    when requiere_senia_calculada then 30.00
    else 0.00
  end;
  codigo_impresion := case
    when tipo_impresion_normalizado = 'color' then 'impresion_color_pagina'
    else 'impresion_byn_pagina'
  end;

  select s.*
  into servicio_impresion
  from public.servicio s
  where s.codigo = codigo_impresion
    and s.activo = true
    and s.eliminado = false
  limit 1;

  if not found then
    raise exception 'No se encontro servicio activo para calcular la impresion (%).', codigo_impresion
      using errcode = 'P0001';
  end if;

  tipo_moneda_cotizacion := servicio_impresion.tipo_moneda;
  subtotal_linea := round(cantidad_estimada_calculada::numeric * servicio_impresion.precio_base, 2);
  total_estimado_calculado := total_estimado_calculado + subtotal_linea;
  lineas_cotizacion := lineas_cotizacion || jsonb_build_array(
    jsonb_build_object(
      'id_servicio', servicio_impresion.id_servicio,
      'codigo_servicio', servicio_impresion.codigo,
      'nombre', servicio_impresion.nombre,
      'tipo', 'impresion',
      'cantidad', cantidad_estimada_calculada,
      'precio_unitario', servicio_impresion.precio_base,
      'subtotal', subtotal_linea,
      'tipo_moneda', servicio_impresion.tipo_moneda
    )
  );

  if coalesce(p_encuadernado, false) then
    select s.*
    into servicio_adicional
    from public.servicio s
    where s.codigo = 'encuadernado'
      and s.activo = true
      and s.eliminado = false
    limit 1;

    if not found then
      raise exception 'No se encontro servicio activo para calcular encuadernado.'
        using errcode = 'P0001';
    end if;

    if servicio_adicional.tipo_moneda <> tipo_moneda_cotizacion then
      raise exception 'La moneda del servicio encuadernado no coincide con la cotizacion.'
        using errcode = 'P0001';
    end if;

    subtotal_linea := round(servicio_adicional.precio_base, 2);
    total_estimado_calculado := total_estimado_calculado + subtotal_linea;
    lineas_cotizacion := lineas_cotizacion || jsonb_build_array(
      jsonb_build_object(
        'id_servicio', servicio_adicional.id_servicio,
        'codigo_servicio', servicio_adicional.codigo,
        'nombre', servicio_adicional.nombre,
        'tipo', 'adicional',
        'cantidad', 1,
        'precio_unitario', servicio_adicional.precio_base,
        'subtotal', subtotal_linea,
        'tipo_moneda', servicio_adicional.tipo_moneda
      )
    );
  end if;

  if coalesce(p_anillado, false) then
    select s.*
    into servicio_adicional
    from public.servicio s
    where s.codigo = 'anillado'
      and s.activo = true
      and s.eliminado = false
    limit 1;

    if not found then
      raise exception 'No se encontro servicio activo para calcular anillado.'
        using errcode = 'P0001';
    end if;

    if servicio_adicional.tipo_moneda <> tipo_moneda_cotizacion then
      raise exception 'La moneda del servicio anillado no coincide con la cotizacion.'
        using errcode = 'P0001';
    end if;

    subtotal_linea := round(servicio_adicional.precio_base, 2);
    total_estimado_calculado := total_estimado_calculado + subtotal_linea;
    lineas_cotizacion := lineas_cotizacion || jsonb_build_array(
      jsonb_build_object(
        'id_servicio', servicio_adicional.id_servicio,
        'codigo_servicio', servicio_adicional.codigo,
        'nombre', servicio_adicional.nombre,
        'tipo', 'adicional',
        'cantidad', 1,
        'precio_unitario', servicio_adicional.precio_base,
        'subtotal', subtotal_linea,
        'tipo_moneda', servicio_adicional.tipo_moneda
      )
    );
  end if;

  return jsonb_build_object(
    'cantidad_estimada', cantidad_estimada_calculada,
    'requiere_senia', requiere_senia_calculada,
    'porcentaje_senia', porcentaje_senia_calculado,
    'total_estimado', total_estimado_calculado,
    'tipo_moneda', tipo_moneda_cotizacion,
    'lineas', lineas_cotizacion
  );
end;
$$;

create or replace function public.crear_pedido_cliente(
  p_cantidad_carillas integer,
  p_cantidad_copias integer,
  p_tamano_hoja text,
  p_tipo_impresion text,
  p_doble_faz boolean default false,
  p_encuadernado boolean default false,
  p_anillado boolean default false,
  p_observacion_cliente text default null,
  p_descripcion text default null,
  p_id_punto_entrega bigint default null,
  p_metodo_pago_preferido text default null,
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
  id_pedido_creado bigint;
  codigo_pedido text;
  metodo_pago_normalizado text;
  cotizacion_pedido jsonb;
  linea_cotizacion jsonb;
  cantidad_estimada_calculada integer;
  requiere_senia_calculada boolean;
  porcentaje_senia_calculado numeric(5, 2);
  total_estimado_calculado numeric(12, 2);
begin
  id_usuario_auth_actor := auth.uid();

  if id_usuario_auth_actor is null then
    raise exception 'Debes iniciar sesion para crear un pedido.'
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

  if not public.usuario_tiene_permiso('pedidos.crear') then
    raise exception 'El usuario no tiene permiso para crear pedidos.'
      using errcode = '42501';
  end if;

  if p_cantidad_carillas is null or p_cantidad_carillas <= 0 then
    raise exception 'La cantidad de carillas debe ser mayor a cero.'
      using errcode = '23514';
  end if;

  if p_cantidad_copias is null or p_cantidad_copias <= 0 then
    raise exception 'La cantidad de copias debe ser mayor a cero.'
      using errcode = '23514';
  end if;

  if p_tamano_hoja is null or upper(btrim(p_tamano_hoja)) not in ('A4', 'A3', 'OFICIO') then
    raise exception 'El tamanio de hoja no esta permitido.'
      using errcode = '23514';
  end if;

  if p_tipo_impresion is null or lower(btrim(p_tipo_impresion)) not in ('byn', 'color') then
    raise exception 'El tipo de impresion no esta permitido.'
      using errcode = '23514';
  end if;

  metodo_pago_normalizado := public.normalizar_metodo_pago_preferido(p_metodo_pago_preferido);

  if metodo_pago_normalizado is not null
    and metodo_pago_normalizado not in ('efectivo', 'transferencia', 'mercado_pago', 'tarjeta', 'otro') then
    raise exception 'El metodo de pago preferido no esta permitido.'
      using errcode = '23514';
  end if;

  if p_id_punto_entrega is not null
    and not exists (
      select 1
      from public.punto_entrega pe
      where pe.id_punto_entrega = p_id_punto_entrega
        and pe.activo = true
        and pe.eliminado = false
    ) then
    raise exception 'El punto de entrega indicado no existe o no esta activo.'
      using errcode = '23503';
  end if;

  cotizacion_pedido := public.calcular_cotizacion_pedido_cliente(
    p_cantidad_carillas := p_cantidad_carillas,
    p_cantidad_copias := p_cantidad_copias,
    p_tamano_hoja := p_tamano_hoja,
    p_tipo_impresion := p_tipo_impresion,
    p_doble_faz := coalesce(p_doble_faz, false),
    p_encuadernado := coalesce(p_encuadernado, false),
    p_anillado := coalesce(p_anillado, false)
  );
  cantidad_estimada_calculada := (cotizacion_pedido ->> 'cantidad_estimada')::integer;
  requiere_senia_calculada := (cotizacion_pedido ->> 'requiere_senia')::boolean;
  porcentaje_senia_calculado := (cotizacion_pedido ->> 'porcentaje_senia')::numeric(5, 2);
  total_estimado_calculado := (cotizacion_pedido ->> 'total_estimado')::numeric(12, 2);
  codigo_pedido := public.generar_codigo_pedido();

  insert into public.pedido (
    id_usuario,
    id_punto_entrega,
    codigo,
    descripcion,
    observacion_cliente,
    cantidad_estimada,
    cantidad_carillas,
    cantidad_copias,
    tamano_hoja,
    tipo_impresion,
    doble_faz,
    encuadernado,
    anillado,
    requiere_senia,
    porcentaje_senia,
    total_estimado,
    metodo_pago_preferido,
    estado_interno,
    estado_visible_cliente,
    estado_financiero,
    id_usuario_creacion
  )
  values (
    id_usuario_actor,
    p_id_punto_entrega,
    codigo_pedido,
    nullif(btrim(p_descripcion), ''),
    nullif(btrim(p_observacion_cliente), ''),
    cantidad_estimada_calculada,
    p_cantidad_carillas,
    p_cantidad_copias,
    upper(btrim(p_tamano_hoja)),
    lower(btrim(p_tipo_impresion)),
    coalesce(p_doble_faz, false),
    coalesce(p_encuadernado, false),
    coalesce(p_anillado, false),
    requiere_senia_calculada,
    porcentaje_senia_calculado,
    total_estimado_calculado,
    metodo_pago_normalizado,
    'pendiente_revision',
    'pendiente_revision',
    'pendiente_evaluacion',
    id_usuario_actor
  )
  returning id_pedido into id_pedido_creado;

  for linea_cotizacion in
    select value
    from jsonb_array_elements(cotizacion_pedido -> 'lineas')
  loop
    insert into public.pedido_servicio (
      id_pedido,
      id_servicio,
      cantidad,
      precio_unitario_aplicado,
      subtotal,
      observacion,
      id_usuario_creacion
    )
    values (
      id_pedido_creado,
      (linea_cotizacion ->> 'id_servicio')::bigint,
      (linea_cotizacion ->> 'cantidad')::numeric(12, 2),
      (linea_cotizacion ->> 'precio_unitario')::numeric(12, 2),
      (linea_cotizacion ->> 'subtotal')::numeric(12, 2),
      linea_cotizacion ->> 'nombre',
      id_usuario_actor
    );
  end loop;

  perform public.registrar_evento_auditoria(
    p_accion := 'pedido_creado',
    p_mensaje := 'Pedido creado por cliente y pendiente de revision administrativa.',
    p_nivel := 'info',
    p_codigo := 'PEDIDO_CREADO',
    p_id_pedido := id_pedido_creado,
    p_tabla_afectada := 'pedido',
    p_id_registro_afectado := id_pedido_creado::text,
    p_request_id := p_request_id,
    p_metadata := jsonb_build_object(
      'estado_interno', 'pendiente_revision',
      'estado_visible_cliente', 'pendiente_revision',
      'cantidad_carillas', p_cantidad_carillas,
      'cantidad_copias', p_cantidad_copias,
      'cantidad_estimada', cantidad_estimada_calculada,
      'tamano_hoja', upper(btrim(p_tamano_hoja)),
      'tipo_impresion', lower(btrim(p_tipo_impresion)),
      'doble_faz', coalesce(p_doble_faz, false),
      'encuadernado', coalesce(p_encuadernado, false),
      'anillado', coalesce(p_anillado, false),
      'metodo_pago_preferido', metodo_pago_normalizado,
      'requiere_senia', requiere_senia_calculada,
      'porcentaje_senia', porcentaje_senia_calculado,
      'total_estimado', total_estimado_calculado,
      'tipo_moneda', cotizacion_pedido ->> 'tipo_moneda',
      'lineas_cotizacion', cotizacion_pedido -> 'lineas'
    ),
    p_id_usuario_actor := id_usuario_actor,
    p_id_usuario_auth_actor := id_usuario_auth_actor
  );

  return jsonb_build_object(
    'id_pedido', id_pedido_creado,
    'codigo', codigo_pedido,
    'estado_visible_cliente', 'pendiente_revision',
    'estado_interno', 'pendiente_revision',
    'estado_financiero', 'pendiente_evaluacion',
    'cantidad_carillas', p_cantidad_carillas,
    'cantidad_copias', p_cantidad_copias,
    'cantidad_estimada', cantidad_estimada_calculada,
    'tamano_hoja', upper(btrim(p_tamano_hoja)),
    'tipo_impresion', lower(btrim(p_tipo_impresion)),
    'doble_faz', coalesce(p_doble_faz, false),
    'encuadernado', coalesce(p_encuadernado, false),
    'anillado', coalesce(p_anillado, false),
    'id_punto_entrega', p_id_punto_entrega,
    'metodo_pago_preferido', metodo_pago_normalizado,
    'requiere_senia', requiere_senia_calculada,
    'porcentaje_senia', porcentaje_senia_calculado,
    'total_estimado', total_estimado_calculado,
    'tipo_moneda', cotizacion_pedido ->> 'tipo_moneda',
    'cotizacion', cotizacion_pedido
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
  p.porcentaje_senia,
  p.total_estimado,
  p.fecha_confirmacion_cliente,
  p.fecha_creacion,
  p.fecha_modificacion,
  p.eliminado,
  p.version_fila
from public.pedido p
where p.id_usuario = public.usuario_actual_id()
  and p.eliminado = false;

revoke all on public.pedido_cliente from anon, authenticated;
grant select on public.pedido_cliente to authenticated;

revoke all on function public.calcular_cotizacion_pedido_cliente(
  integer,
  integer,
  text,
  text,
  boolean,
  boolean,
  boolean
) from public;

revoke all on function public.crear_pedido_cliente(
  integer,
  integer,
  text,
  text,
  boolean,
  boolean,
  boolean,
  text,
  text,
  bigint,
  text,
  text
) from public;

grant execute on function public.calcular_cotizacion_pedido_cliente(
  integer,
  integer,
  text,
  text,
  boolean,
  boolean,
  boolean
) to authenticated, service_role;

grant execute on function public.crear_pedido_cliente(
  integer,
  integer,
  text,
  text,
  boolean,
  boolean,
  boolean,
  text,
  text,
  bigint,
  text,
  text
) to authenticated, service_role;

comment on function public.calcular_cotizacion_pedido_cliente(
  integer,
  integer,
  text,
  text,
  boolean,
  boolean,
  boolean
) is 'Calcula cotizacion de pedido para cliente autenticado usando precios activos de public.servicio y regla de senia MVP.';

comment on function public.crear_pedido_cliente(
  integer,
  integer,
  text,
  text,
  boolean,
  boolean,
  boolean,
  text,
  text,
  bigint,
  text,
  text
) is 'Crea un pedido de cliente autenticado, persiste cotizacion real en pedido_servicio y deja auditoria pedido_creado.';
