create extension if not exists pgtap;

begin;

select plan(12);

select set_config(
  'request.jwt.claim.sub',
  '00000000-0000-4000-8000-000000000003',
  true
);
select set_config('request.jwt.claim.role', 'authenticated', true);

create temporary table tmp_cotizacion_basica (
  respuesta jsonb not null
) on commit drop;

insert into tmp_cotizacion_basica (respuesta)
select public.calcular_cotizacion_pedido_cliente(
  p_cantidad_carillas := 18,
  p_cantidad_copias := 2,
  p_tamano_hoja := 'A4',
  p_tipo_impresion := 'byn',
  p_doble_faz := false,
  p_encuadernado := true,
  p_anillado := false
);

select is(
  (select (respuesta ->> 'cantidad_estimada')::integer from tmp_cotizacion_basica),
  36,
  'cotizacion calcula cantidad_estimada como carillas por copias'
);

select is(
  (select (respuesta ->> 'total_estimado')::numeric from tmp_cotizacion_basica),
  1220.00::numeric,
  'cotizacion suma impresion byn y encuadernado'
);

select is(
  (select (respuesta ->> 'requiere_senia')::boolean from tmp_cotizacion_basica),
  false,
  'cotizacion menor o igual a 200 no requiere senia'
);

select is(
  (select jsonb_array_length(respuesta -> 'lineas') from tmp_cotizacion_basica),
  2,
  'cotizacion devuelve linea de impresion y adicional'
);

create temporary table tmp_cotizacion_senia (
  respuesta jsonb not null
) on commit drop;

insert into tmp_cotizacion_senia (respuesta)
select public.calcular_cotizacion_pedido_cliente(
  p_cantidad_carillas := 101,
  p_cantidad_copias := 2,
  p_tamano_hoja := 'A4',
  p_tipo_impresion := 'color',
  p_doble_faz := false,
  p_encuadernado := false,
  p_anillado := true
);

select is(
  (select (respuesta ->> 'cantidad_estimada')::integer from tmp_cotizacion_senia),
  202,
  'cotizacion detecta cantidad_estimada superior a 200'
);

select is(
  (select (respuesta ->> 'requiere_senia')::boolean from tmp_cotizacion_senia),
  true,
  'cotizacion superior a 200 requiere senia'
);

select is(
  (select (respuesta ->> 'porcentaje_senia')::numeric from tmp_cotizacion_senia),
  50.00::numeric,
  'cotizacion aplica senia del 50 por ciento'
);

select is(
  (select (respuesta ->> 'total_estimado')::numeric from tmp_cotizacion_senia),
  10800.00::numeric,
  'cotizacion suma impresion color y anillado'
);

create temporary table tmp_pedido_cotizado (
  id_pedido bigint not null,
  respuesta jsonb not null
) on commit drop;

do $$
declare
  pedido jsonb;
begin
  pedido := public.crear_pedido_cliente(
    p_cantidad_carillas := 101,
    p_cantidad_copias := 2,
    p_tamano_hoja := 'A4',
    p_tipo_impresion := 'color',
    p_doble_faz := false,
    p_encuadernado := false,
    p_anillado := true,
    p_observacion_cliente := 'Pedido con senia para test.',
    p_descripcion := 'Cotizacion persistida test.',
    p_metodo_pago_preferido := 'transferencia',
    p_request_id := 'test-cotizacion-persistida'
  );

  insert into tmp_pedido_cotizado (id_pedido, respuesta)
  values ((pedido ->> 'id_pedido')::bigint, pedido);
end;
$$;

select is(
  (
    select p.total_estimado
    from public.pedido p
    join tmp_pedido_cotizado t on t.id_pedido = p.id_pedido
  ),
  10800.00::numeric,
  'crear_pedido persiste total_estimado calculado'
);

select ok(
  (
    select p.requiere_senia
      and p.porcentaje_senia = 50.00::numeric
      and p.cantidad_estimada = 202
    from public.pedido p
    join tmp_pedido_cotizado t on t.id_pedido = p.id_pedido
  ),
  'crear_pedido persiste cantidad estimada y senia'
);

select is(
  (
    select count(*)::integer
    from public.pedido_servicio ps
    join tmp_pedido_cotizado t on t.id_pedido = ps.id_pedido
  ),
  2,
  'crear_pedido persiste detalle en pedido_servicio'
);

select is(
  (
    select ps.subtotal
    from public.pedido_servicio ps
    join public.servicio s on s.id_servicio = ps.id_servicio
    join tmp_pedido_cotizado t on t.id_pedido = ps.id_pedido
    where s.codigo = 'anillado'
  ),
  700.00::numeric,
  'pedido_servicio registra subtotal de anillado'
);

select * from finish();

rollback;
