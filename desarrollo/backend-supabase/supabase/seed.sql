-- Seed data local de desarrollo.
-- Issue relacionada: #96.

with usuarios_seed (
  id_usuario_auth,
  email,
  contrasena,
  nombre,
  apellido,
  codigo_rol
) as (
  values
    ('00000000-0000-4000-8000-000000000001'::uuid, 'admin@admin.com', 'admin123', 'Admin', 'Local', 'administrador'),
    ('00000000-0000-4000-8000-000000000002'::uuid, 'empleado@empleado.com', 'empleado', 'Empleado', 'Local', 'empleado'),
    ('00000000-0000-4000-8000-000000000003'::uuid, 'cliente@cliente.com', 'cliente', 'Cliente', 'Local', 'cliente')
)
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
select
  '00000000-0000-0000-0000-000000000000'::uuid,
  us.id_usuario_auth,
  'authenticated',
  'authenticated',
  us.email,
  crypt(us.contrasena, gen_salt('bf')),
  now(),
  now(),
  jsonb_build_object('provider', 'email', 'providers', jsonb_build_array('email')),
  jsonb_build_object('nombre', us.nombre, 'apellido', us.apellido),
  false,
  now(),
  now(),
  '',
  '',
  '',
  '',
  false,
  false
from usuarios_seed us
on conflict (id) do update
set
  email = excluded.email,
  encrypted_password = excluded.encrypted_password,
  email_confirmed_at = excluded.email_confirmed_at,
  raw_app_meta_data = excluded.raw_app_meta_data,
  raw_user_meta_data = excluded.raw_user_meta_data,
  updated_at = now(),
  is_sso_user = false,
  is_anonymous = false;

with usuarios_seed (
  id_usuario_auth,
  email,
  nombre,
  apellido
) as (
  values
    ('00000000-0000-4000-8000-000000000001'::uuid, 'admin@admin.com', 'Admin', 'Local'),
    ('00000000-0000-4000-8000-000000000002'::uuid, 'empleado@empleado.com', 'Empleado', 'Local'),
    ('00000000-0000-4000-8000-000000000003'::uuid, 'cliente@cliente.com', 'Cliente', 'Local')
)
insert into auth.identities (
  id,
  provider_id,
  user_id,
  identity_data,
  provider,
  last_sign_in_at,
  created_at,
  updated_at
)
select
  us.id_usuario_auth,
  us.id_usuario_auth::text,
  us.id_usuario_auth,
  jsonb_build_object(
    'sub', us.id_usuario_auth::text,
    'email', us.email,
    'email_verified', true,
    'phone_verified', false
  ),
  'email',
  now(),
  now(),
  now()
from usuarios_seed us
on conflict (provider_id, provider) do update
set
  identity_data = excluded.identity_data,
  updated_at = now();

with usuarios_seed (
  id_usuario_auth,
  email,
  nombre,
  apellido,
  codigo_rol
) as (
  values
    ('00000000-0000-4000-8000-000000000001'::uuid, 'admin@admin.com', 'Admin', 'Local', 'administrador'),
    ('00000000-0000-4000-8000-000000000002'::uuid, 'empleado@empleado.com', 'Empleado', 'Local', 'empleado'),
    ('00000000-0000-4000-8000-000000000003'::uuid, 'cliente@cliente.com', 'Cliente', 'Local', 'cliente')
)
insert into public.usuario (
  id_usuario_auth,
  id_rol,
  nombre,
  apellido,
  email,
  estado
)
select
  us.id_usuario_auth,
  r.id_rol,
  us.nombre,
  us.apellido,
  us.email,
  'activo'
from usuarios_seed us
join public.rol r on r.codigo = us.codigo_rol
on conflict (id_usuario_auth) do update
set
  id_rol = excluded.id_rol,
  nombre = excluded.nombre,
  apellido = excluded.apellido,
  email = excluded.email,
  estado = 'activo',
  eliminado = false,
  fecha_eliminacion = null;

with puntos_front (
  codigo_front,
  nombre,
  direccion_texto
) as (
  values
    ('local', 'Retiro Local', 'Lope de Vega 2150 (C.A.B.A.)'),
    ('medicina', 'Facultad de Medicina', 'Paraguay 2155 (C.A.B.A.)'),
    ('filosofia', 'Facultad de Filosofía', 'Púan 480 (C.A.B.A.)')
)
update public.punto_entrega pe
set
  nombre = pf.nombre,
  descripcion = 'front_id:' || pf.codigo_front,
  direccion_texto = pf.direccion_texto,
  horario_atencion = 'A coordinar',
  activo = true,
  eliminado = false,
  fecha_eliminacion = null
from puntos_front pf
where pe.descripcion = 'front_id:' || pf.codigo_front
  or (
    pe.nombre = pf.nombre
    and pe.direccion_texto = pf.direccion_texto
  );

with puntos_front (
  codigo_front,
  nombre,
  direccion_texto
) as (
  values
    ('local', 'Retiro Local', 'Lope de Vega 2150 (C.A.B.A.)'),
    ('medicina', 'Facultad de Medicina', 'Paraguay 2155 (C.A.B.A.)'),
    ('filosofia', 'Facultad de Filosofía', 'Púan 480 (C.A.B.A.)')
)
insert into public.punto_entrega (
  nombre,
  descripcion,
  direccion_texto,
  horario_atencion,
  activo
)
select
  pf.nombre,
  'front_id:' || pf.codigo_front,
  pf.direccion_texto,
  'A coordinar',
  true
from puntos_front pf
where not exists (
  select 1
  from public.punto_entrega pe
  where pe.descripcion = 'front_id:' || pf.codigo_front
    and pe.eliminado = false
);

insert into public.servicio (
  codigo,
  nombre,
  descripcion,
  precio_base,
  tipo_moneda,
  activo
)
values
  (
    'impresion_byn_pagina',
    'Impresion blanco y negro por pagina',
    'Alineado a MOCK_PRICING.blackAndWhitePage del front.',
    20,
    'ARS',
    true
  ),
  (
    'impresion_color_pagina',
    'Impresion color por pagina',
    'Alineado a MOCK_PRICING.colorPage del front.',
    50,
    'ARS',
    true
  ),
  (
    'encuadernado',
    'Encuadernado',
    'Alineado a MOCK_PRICING.bound del front.',
    500,
    'ARS',
    true
  ),
  (
    'anillado',
    'Anillado',
    'Alineado a MOCK_PRICING.spiralBound del front.',
    700,
    'ARS',
    true
  )
on conflict (codigo) do update
set
  nombre = excluded.nombre,
  descripcion = excluded.descripcion,
  precio_base = excluded.precio_base,
  tipo_moneda = excluded.tipo_moneda,
  activo = true,
  eliminado = false,
  fecha_eliminacion = null;
