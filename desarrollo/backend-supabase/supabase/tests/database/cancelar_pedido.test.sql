create extension if not exists pgtap;

begin;

select plan(8);

create temporary table tmp_cancelar_pedido (
  id_pedido bigint not null,
  id_usuario bigint not null
) on commit drop;

do $$
declare
  cliente_auth uuid := '00000000-0000-4000-8000-000000000003'::uuid;
  id_usuario_cliente bigint;
  pedido jsonb;
begin
  perform set_config('request.jwt.claim.sub', cliente_auth::text, true);
  perform set_config('request.jwt.claim.role', 'authenticated', true);

  select u.id_usuario
  into id_usuario_cliente
  from public.usuario u
  where u.id_usuario_auth = cliente_auth;

  pedido := public.crear_pedido_cliente(
    p_cantidad_carillas := 8,
    p_cantidad_copias := 1,
    p_tamano_hoja := 'A4',
    p_tipo_impresion := 'byn',
    p_request_id := 'test-cancelar-pedido-creacion'
  );

  insert into tmp_cancelar_pedido (id_pedido, id_usuario)
  values ((pedido ->> 'id_pedido')::bigint, id_usuario_cliente);
end;
$$;

select ok(
  (select count(*) from tmp_cancelar_pedido) = 1,
  'prepara un pedido propio cancelable'
);

create temporary table tmp_cancelacion (
  respuesta jsonb not null
) on commit drop;

insert into tmp_cancelacion (respuesta)
select public.cancelar_pedido_cliente(
  t.id_pedido,
  'El cliente decidio no continuar.',
  'test-cancelar-pedido-ok'
)
from tmp_cancelar_pedido t;

select is(
  (select respuesta ->> 'estado_visible_cliente' from tmp_cancelacion),
  'cancelado',
  'cancelar_pedido devuelve estado visible cancelado'
);

select is(
  (
    select p.estado_interno
    from public.pedido p
    join tmp_cancelar_pedido t on t.id_pedido = p.id_pedido
  ),
  'cancelado',
  'pedido queda cancelado internamente'
);

select is(
  (
    select count(*)::integer
    from public.auditoria a
    join tmp_cancelar_pedido t on t.id_pedido = a.id_pedido
    where a.accion = 'pedido_cancelado'
  ),
  1,
  'registra auditoria pedido_cancelado'
);

select throws_ok(
  format(
    'select public.cancelar_pedido_cliente(%s, %L, %L)',
    (select id_pedido from tmp_cancelar_pedido),
    'Reintento',
    'test-cancelar-pedido-reintento'
  ),
  'P0001',
  'El pedido no esta en un estado cancelable.',
  'rechaza cancelar nuevamente un pedido ya cancelado'
);

create temporary table tmp_pedido_confirmado (
  id_pedido bigint not null,
  id_usuario bigint not null
) on commit drop;

do $$
declare
  cliente_auth uuid := '00000000-0000-4000-8000-000000000003'::uuid;
  id_usuario_cliente bigint;
  pedido jsonb;
  id_pedido_creado bigint;
begin
  perform set_config('request.jwt.claim.sub', cliente_auth::text, true);
  perform set_config('request.jwt.claim.role', 'authenticated', true);

  select u.id_usuario
  into id_usuario_cliente
  from public.usuario u
  where u.id_usuario_auth = cliente_auth;

  pedido := public.crear_pedido_cliente(
    p_cantidad_carillas := 5,
    p_cantidad_copias := 1,
    p_tamano_hoja := 'A4',
    p_tipo_impresion := 'byn',
    p_request_id := 'test-cancelar-pedido-confirmado-creacion'
  );
  id_pedido_creado := (pedido ->> 'id_pedido')::bigint;

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
    id_pedido_creado,
    id_usuario_cliente,
    'pedido-confirmado.pdf',
    'application/pdf',
    128,
    128,
    repeat('c', 64),
    public.bucket_archivos_pedidos(),
    public.generar_ruta_storage_archivo(id_usuario_cliente, id_pedido_creado, gen_random_uuid()),
    'wrapped-key-confirmado',
    'iv-confirmado',
    'v1',
    'cargado',
    id_usuario_cliente
  );

  perform public.confirmar_pedido_cliente(
    id_pedido_creado,
    'test-cancelar-pedido-confirmado-confirmacion'
  );

  insert into tmp_pedido_confirmado (id_pedido, id_usuario)
  values (id_pedido_creado, id_usuario_cliente);
end;
$$;

select throws_ok(
  format(
    'select public.cancelar_pedido_cliente(%s, %L, %L)',
    (select id_pedido from tmp_pedido_confirmado),
    null,
    'test-cancelar-pedido-confirmado'
  ),
  'P0001',
  'El pedido ya fue confirmado y no puede cancelarse desde el portal cliente.',
  'rechaza cancelar un pedido confirmado'
);

create temporary table tmp_pedido_ajeno_cancelacion (
  id_pedido bigint not null
) on commit drop;

do $$
declare
  admin_auth uuid := '00000000-0000-4000-8000-000000000001'::uuid;
  cliente_auth uuid := '00000000-0000-4000-8000-000000000003'::uuid;
  pedido_admin jsonb;
begin
  perform set_config('request.jwt.claim.sub', admin_auth::text, true);
  perform set_config('request.jwt.claim.role', 'authenticated', true);

  pedido_admin := public.crear_pedido_cliente(
    p_cantidad_carillas := 3,
    p_cantidad_copias := 1,
    p_tamano_hoja := 'A4',
    p_tipo_impresion := 'byn',
    p_request_id := 'test-cancelar-pedido-ajeno-creacion'
  );

  insert into tmp_pedido_ajeno_cancelacion (id_pedido)
  values ((pedido_admin ->> 'id_pedido')::bigint);

  perform set_config('request.jwt.claim.sub', cliente_auth::text, true);
end;
$$;

select throws_ok(
  format(
    'select public.cancelar_pedido_cliente(%s, %L, %L)',
    (select id_pedido from tmp_pedido_ajeno_cancelacion),
    null,
    'test-cancelar-pedido-ajeno'
  ),
  'P0002',
  'El pedido no existe o no pertenece al usuario autenticado.',
  'rechaza cancelar pedidos ajenos'
);

select throws_ok(
  'select public.cancelar_pedido_cliente(0, null, ''test-cancelar-pedido-id-invalido'')',
  '23514',
  'El id del pedido debe ser un entero mayor a cero.',
  'rechaza id de pedido invalido'
);

select * from finish();

rollback;
