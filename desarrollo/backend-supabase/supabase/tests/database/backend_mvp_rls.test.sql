create extension if not exists pgtap;

begin;

select plan(13);

create temporary table tmp_backend_mvp_context (
  id_pedido bigint not null,
  id_usuario_cliente bigint not null,
  id_usuario_otro_cliente bigint not null,
  id_usuario_empleado bigint not null,
  id_punto_entrega bigint not null
) on commit drop;

grant select on tmp_backend_mvp_context to authenticated;

do $$
declare
  cliente_auth uuid := '00000000-0000-4000-8000-000000000003'::uuid;
  otro_cliente_auth uuid := '00000000-0000-4000-8000-000000000004'::uuid;
  id_usuario_cliente bigint;
  id_usuario_otro_cliente bigint;
  id_usuario_empleado bigint;
  id_punto_entrega_local bigint;
  pedido jsonb;
  id_pedido_creado bigint;
begin
  insert into auth.users (
    instance_id,
    id,
    aud,
    role,
    email,
    encrypted_password,
    email_confirmed_at,
    last_sign_in_at,
    raw_app_meta_data,
    raw_user_meta_data,
    is_super_admin,
    created_at,
    updated_at,
    confirmation_token,
    recovery_token,
    email_change_token_new,
    email_change,
    is_sso_user,
    is_anonymous
  )
  values (
    '00000000-0000-0000-0000-000000000000'::uuid,
    otro_cliente_auth,
    'authenticated',
    'authenticated',
    'cliente2@cliente.com',
    crypt('cliente2', gen_salt('bf')),
    now(),
    now(),
    jsonb_build_object('provider', 'email', 'providers', jsonb_build_array('email')),
    jsonb_build_object('nombre', 'Cliente', 'apellido', 'Dos'),
    false,
    now(),
    now(),
    '',
    '',
    '',
    '',
    false,
    false
  );

  select u.id_usuario
  into id_usuario_cliente
  from public.usuario u
  where u.id_usuario_auth = cliente_auth;

  select u.id_usuario
  into id_usuario_otro_cliente
  from public.usuario u
  where u.id_usuario_auth = otro_cliente_auth;

  select u.id_usuario
  into id_usuario_empleado
  from public.usuario u
  where u.id_usuario_auth = '00000000-0000-4000-8000-000000000002'::uuid;

  select pe.id_punto_entrega
  into id_punto_entrega_local
  from public.punto_entrega pe
  where pe.descripcion = 'front_id:local'
    and pe.eliminado = false
  limit 1;

  perform set_config('request.jwt.claim.sub', cliente_auth::text, true);
  perform set_config('request.jwt.claim.role', 'authenticated', true);

  pedido := public.crear_pedido_cliente(
    p_cantidad_carillas := 18,
    p_cantidad_copias := 2,
    p_tamano_hoja := 'A4',
    p_tipo_impresion := 'byn',
    p_doble_faz := false,
    p_encuadernado := true,
    p_anillado := false,
    p_observacion_cliente := 'Pedido QA backend MVP.',
    p_descripcion := 'Trabajo de prueba QA backend MVP.',
    p_id_punto_entrega := id_punto_entrega_local,
    p_metodo_pago_preferido := 'efectivo',
    p_request_id := 'test-backend-mvp-crear-pedido'
  );
  id_pedido_creado := (pedido ->> 'id_pedido')::bigint;

  perform public.registrar_archivo_pedido_cliente(
    p_id_usuario_auth_actor := cliente_auth,
    p_id_pedido := id_pedido_creado,
    p_nombre_original := 'pedido-qa.pdf',
    p_mime_original := 'application/pdf',
    p_tamano_bytes := 256,
    p_tamano_original_bytes := 256,
    p_hash_archivo := repeat('b', 64),
    p_bucket := public.bucket_archivos_pedidos(),
    p_ruta_storage := public.generar_ruta_storage_archivo(
      id_usuario_cliente,
      id_pedido_creado,
      gen_random_uuid()
    ),
    p_clave_envuelta := 'wrapped-key-qa',
    p_iv := 'iv-qa',
    p_version_cifrado := 'v1',
    p_request_id := 'test-backend-mvp-registrar-archivo'
  );

  perform public.confirmar_pedido_cliente(
    id_pedido_creado,
    'test-backend-mvp-confirmar-pedido'
  );

  insert into tmp_backend_mvp_context (
    id_pedido,
    id_usuario_cliente,
    id_usuario_otro_cliente,
    id_usuario_empleado,
    id_punto_entrega
  )
  values (
    id_pedido_creado,
    id_usuario_cliente,
    id_usuario_otro_cliente,
    id_usuario_empleado,
    id_punto_entrega_local
  );
end;
$$;

select ok(
  (select id_usuario_otro_cliente is not null from tmp_backend_mvp_context),
  'signup local crea perfil cliente mediante trigger'
);

select ok(
  (select id_pedido is not null from tmp_backend_mvp_context),
  'cliente crea pedido propio con datos minimos'
);

select is(
  (
    select p.estado_visible_cliente
    from public.pedido p
    join tmp_backend_mvp_context t on t.id_pedido = p.id_pedido
  ),
  'pendiente_revision',
  'pedido confirmado queda pendiente de revision visible'
);

select ok(
  (
    select p.fecha_confirmacion_cliente is not null
    from public.pedido p
    join tmp_backend_mvp_context t on t.id_pedido = p.id_pedido
  ),
  'pedido confirmado registra fecha de confirmacion cliente'
);

select set_config(
  'request.jwt.claim.sub',
  '00000000-0000-4000-8000-000000000003',
  true
);
select set_config('request.jwt.claim.role', 'authenticated', true);
set local role authenticated;

select is(
  (
    select count(*)::integer
    from public.pedido_cliente pc
    join tmp_backend_mvp_context t on t.id_pedido = pc.id_pedido
  ),
  1,
  'cliente ve su pedido mediante vista pedido_cliente'
);

select is(
  (
    select count(*)::integer
    from public.pedido p
    join tmp_backend_mvp_context t on t.id_pedido = p.id_pedido
  ),
  0,
  'cliente no accede directo a tabla pedido'
);

select is(
  (
    select count(*)::integer
    from public.archivo_cliente ac
    join tmp_backend_mvp_context t on t.id_pedido = ac.id_pedido
  ),
  1,
  'cliente ve su archivo mediante vista archivo_cliente'
);

select is(
  (
    select count(*)::integer
    from public.archivo a
    join tmp_backend_mvp_context t on t.id_pedido = a.id_pedido
  ),
  0,
  'cliente no accede directo a tabla archivo'
);

reset role;

select set_config(
  'request.jwt.claim.sub',
  '00000000-0000-4000-8000-000000000004',
  true
);
set local role authenticated;

select is(
  (
    select count(*)::integer
    from public.pedido_cliente pc
    join tmp_backend_mvp_context t on t.id_pedido = pc.id_pedido
  ),
  0,
  'cliente no ve pedido ajeno en vista pedido_cliente'
);

reset role;

select set_config(
  'request.jwt.claim.sub',
  '00000000-0000-4000-8000-000000000002',
  true
);
set local role authenticated;

select is(
  (
    select count(*)::integer
    from public.pedido p
    join tmp_backend_mvp_context t on t.id_pedido = p.id_pedido
  ),
  1,
  'empleado puede consultar pedidos internos por RLS'
);

reset role;

select set_config(
  'request.jwt.claim.sub',
  '00000000-0000-4000-8000-000000000003',
  true
);
select set_config(
  'app.test.id_rol_administrador',
  (
    select r.id_rol::text
    from public.rol r
    where r.codigo = 'administrador'
  ),
  true
);
set local role authenticated;

update public.usuario
set id_rol = current_setting('app.test.id_rol_administrador')::bigint
where id_usuario_auth = '00000000-0000-4000-8000-000000000003'::uuid;

reset role;

select is(
  (
    select r.codigo
    from public.usuario u
    join public.rol r on r.id_rol = u.id_rol
    where u.id_usuario_auth = '00000000-0000-4000-8000-000000000003'::uuid
  ),
  'cliente',
  'cliente no modifica su rol'
);

select is(
  (
    select count(*)::integer
    from public.auditoria a
    join tmp_backend_mvp_context t on t.id_pedido = a.id_pedido
    where a.accion = 'pedido_creado'
  ),
  1,
  'auditoria registra pedido_creado'
);

select is(
  (
    select count(*)::integer
    from public.auditoria a
    join tmp_backend_mvp_context t on t.id_pedido = a.id_pedido
    where a.accion in ('archivo_pedido_cargado', 'pedido_confirmado')
  ),
  2,
  'auditoria registra archivo_pedido_cargado y pedido_confirmado'
);

select * from finish();

rollback;
