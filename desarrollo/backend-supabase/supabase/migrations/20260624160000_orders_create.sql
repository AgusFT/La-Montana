-- Pedido y RPC para crear pedidos desde Edge Function.
-- Issue relacionada: #110.

alter table public.pedido
  add column cantidad_copias integer not null default 1,
  add column tamano_hoja text not null default 'A4',
  add column tipo_impresion text not null default 'byn',
  add column doble_faz boolean not null default false,
  add column encuadernado boolean not null default false,
  add column anillado boolean not null default false,
  add column metodo_pago_preferido text;

alter table public.pedido
  drop constraint if exists pedido_cantidad_carillas_check;

alter table public.pedido
  add constraint pedido_cantidad_carillas_check check (
    cantidad_carillas is null
    or cantidad_carillas > 0
  ),
  add constraint pedido_cantidad_copias_check check (cantidad_copias > 0),
  add constraint pedido_tamano_hoja_check check (tamano_hoja in ('A4', 'A3', 'OFICIO')),
  add constraint pedido_tipo_impresion_check check (tipo_impresion in ('byn', 'color')),
  add constraint pedido_metodo_pago_preferido_check check (
    metodo_pago_preferido is null
    or metodo_pago_preferido in ('efectivo', 'transferencia', 'mercado_pago', 'tarjeta', 'otro')
  );

comment on column public.pedido.cantidad_copias
  is 'Cantidad de copias solicitadas por el cliente para el pedido.';

comment on column public.pedido.tamano_hoja
  is 'Tamanio de hoja solicitado para el pedido: A4, A3 u OFICIO.';

comment on column public.pedido.tipo_impresion
  is 'Tipo de impresion solicitado: byn o color.';

comment on column public.pedido.doble_faz
  is 'Indica si el pedido requiere impresion doble faz.';

comment on column public.pedido.encuadernado
  is 'Indica si el pedido requiere encuadernado.';

comment on column public.pedido.anillado
  is 'Indica si el pedido requiere anillado.';

comment on column public.pedido.metodo_pago_preferido
  is 'Metodo de pago preferido informado por el cliente, sin procesar pago real.';

create index idx_pedido_tamano_hoja on public.pedido (tamano_hoja);
create index idx_pedido_tipo_impresion on public.pedido (tipo_impresion);

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
  p.fecha_creacion,
  p.fecha_modificacion,
  p.eliminado,
  p.version_fila
from public.pedido p
where p.id_usuario = public.usuario_actual_id()
  and p.eliminado = false;

revoke all on public.pedido_cliente from anon, authenticated;
grant select on public.pedido_cliente to authenticated;

create or replace function public.normalizar_metodo_pago_preferido(
  metodo_pago_consulta text
)
returns text
language sql
immutable
set search_path = public
as $$
  select case lower(btrim(coalesce(metodo_pago_consulta, '')))
    when '' then null
    when 'cash' then 'efectivo'
    when 'efectivo' then 'efectivo'
    when 'transferencia' then 'transferencia'
    when 'mercado_pago' then 'mercado_pago'
    when 'mercadopago' then 'mercado_pago'
    when 'tarjeta' then 'tarjeta'
    when 'otro' then 'otro'
    else lower(btrim(metodo_pago_consulta))
  end
$$;

create or replace function public.generar_codigo_pedido()
returns text
language sql
volatile
set search_path = public
as $$
  select format(
    'PED-%s-%s',
    to_char(now(), 'YYYYMMDD'),
    upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))
  )
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

  codigo_pedido := public.generar_codigo_pedido();

  insert into public.pedido (
    id_usuario,
    id_punto_entrega,
    codigo,
    descripcion,
    observacion_cliente,
    cantidad_carillas,
    cantidad_copias,
    tamano_hoja,
    tipo_impresion,
    doble_faz,
    encuadernado,
    anillado,
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
    p_cantidad_carillas,
    p_cantidad_copias,
    upper(btrim(p_tamano_hoja)),
    lower(btrim(p_tipo_impresion)),
    coalesce(p_doble_faz, false),
    coalesce(p_encuadernado, false),
    coalesce(p_anillado, false),
    metodo_pago_normalizado,
    'pendiente_revision',
    'pendiente_revision',
    'pendiente_evaluacion',
    id_usuario_actor
  )
  returning id_pedido into id_pedido_creado;

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
      'tamano_hoja', upper(btrim(p_tamano_hoja)),
      'tipo_impresion', lower(btrim(p_tipo_impresion)),
      'doble_faz', coalesce(p_doble_faz, false),
      'encuadernado', coalesce(p_encuadernado, false),
      'anillado', coalesce(p_anillado, false),
      'metodo_pago_preferido', metodo_pago_normalizado
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
    'tamano_hoja', upper(btrim(p_tamano_hoja)),
    'tipo_impresion', lower(btrim(p_tipo_impresion)),
    'doble_faz', coalesce(p_doble_faz, false),
    'encuadernado', coalesce(p_encuadernado, false),
    'anillado', coalesce(p_anillado, false),
    'id_punto_entrega', p_id_punto_entrega,
    'metodo_pago_preferido', metodo_pago_normalizado
  );
end;
$$;

revoke all on function public.normalizar_metodo_pago_preferido(text) from public;
revoke all on function public.generar_codigo_pedido() from public;
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

grant execute on function public.normalizar_metodo_pago_preferido(text) to service_role;
grant execute on function public.generar_codigo_pedido() to service_role;
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
) is 'Crea un pedido de cliente autenticado en estado pendiente_revision con configuracion de impresion y auditoria pedido_creado.';
