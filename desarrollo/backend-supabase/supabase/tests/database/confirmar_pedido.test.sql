create extension if not exists pgtap;

begin;

select plan(8);

create temporary table tmp_confirmar_pedido (
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
    p_cantidad_carillas := 12,
    p_cantidad_copias := 1,
    p_tamano_hoja := 'A4',
    p_tipo_impresion := 'byn',
    p_request_id := 'test-confirmar-pedido-creacion'
  );

  insert into tmp_confirmar_pedido (id_pedido, id_usuario)
  values ((pedido ->> 'id_pedido')::bigint, id_usuario_cliente);
end;
$$;

select ok(
  (select count(*) from tmp_confirmar_pedido) = 1,
  'prepara un pedido propio de cliente'
);

select throws_ok(
  format(
    'select public.confirmar_pedido_cliente(%s, %L)',
    (select id_pedido from tmp_confirmar_pedido),
    'test-confirmar-pedido-sin-archivo'
  ),
  '23514',
  'El pedido debe tener al menos un archivo cargado antes de confirmarse.',
  'rechaza confirmar un pedido sin archivo cargado'
);

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
select
  t.id_pedido,
  t.id_usuario,
  'pedido-test.pdf',
  'application/pdf',
  128,
  128,
  repeat('a', 64),
  public.bucket_archivos_pedidos(),
  public.generar_ruta_storage_archivo(t.id_usuario, t.id_pedido, gen_random_uuid()),
  'wrapped-key-test',
  'iv-test',
  'v1',
  'cargado',
  t.id_usuario
from tmp_confirmar_pedido t;

create temporary table tmp_confirmacion (
  respuesta jsonb not null
) on commit drop;

insert into tmp_confirmacion (respuesta)
select public.confirmar_pedido_cliente(
  t.id_pedido,
  'test-confirmar-pedido-ok'
)
from tmp_confirmar_pedido t;

select is(
  (select (respuesta ->> 'confirmado')::boolean from tmp_confirmacion),
  true,
  'confirma el pedido con archivo cargado'
);

select ok(
  (select respuesta ->> 'fecha_confirmacion_cliente' from tmp_confirmacion) is not null,
  'devuelve fecha de confirmacion cliente'
);

create temporary table tmp_reintento_confirmacion (
  respuesta jsonb not null
) on commit drop;

insert into tmp_reintento_confirmacion (respuesta)
select public.confirmar_pedido_cliente(
  t.id_pedido,
  'test-confirmar-pedido-reintento'
)
from tmp_confirmar_pedido t;

select is(
  (select (respuesta ->> 'ya_confirmado')::boolean from tmp_reintento_confirmacion),
  true,
  'el reintento de confirmacion es idempotente'
);

select is(
  (
    select count(*)::integer
    from public.auditoria a
    join tmp_confirmar_pedido t on t.id_pedido = a.id_pedido
    where a.accion = 'pedido_confirmado'
  ),
  1,
  'registra una sola auditoria pedido_confirmado'
);

select throws_ok(
  format(
    'select public.preparar_carga_archivo_cliente(%L::uuid, %s, 128, 128, %L)',
    '00000000-0000-4000-8000-000000000003',
    (select id_pedido from tmp_confirmar_pedido),
    'test-confirmar-pedido-carga-posterior'
  ),
  'P0001',
  'El pedido no esta editable para cargar archivos.',
  'bloquea cargar archivos despues de confirmar el pedido'
);

create temporary table tmp_pedido_ajeno (
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
    p_cantidad_carillas := 4,
    p_cantidad_copias := 1,
    p_tamano_hoja := 'A4',
    p_tipo_impresion := 'byn',
    p_request_id := 'test-confirmar-pedido-ajeno-creacion'
  );

  insert into tmp_pedido_ajeno (id_pedido)
  values ((pedido_admin ->> 'id_pedido')::bigint);

  perform set_config('request.jwt.claim.sub', cliente_auth::text, true);
end;
$$;

select throws_ok(
  format(
    'select public.confirmar_pedido_cliente(%s, %L)',
    (select id_pedido from tmp_pedido_ajeno),
    'test-confirmar-pedido-ajeno'
  ),
  'P0002',
  'El pedido no existe o no pertenece al usuario autenticado.',
  'rechaza confirmar pedidos ajenos'
);

select * from finish();

rollback;
