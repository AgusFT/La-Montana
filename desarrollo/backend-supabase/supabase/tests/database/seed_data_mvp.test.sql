create extension if not exists pgtap;

begin;

select plan(13);

select is(
  (
    select count(*)::integer
    from public.usuario u
    where u.email in (
      'admin@admin.com',
      'empleado@empleado.com',
      'cliente@cliente.com'
    )
      and u.estado = 'activo'
      and u.eliminado = false
  ),
  3,
  'seed crea usuarios locales admin, empleado y cliente'
);

select is(
  (
    select r.codigo
    from public.usuario u
    join public.rol r on r.id_rol = u.id_rol
    where u.email = 'admin@admin.com'
  ),
  'administrador',
  'admin local tiene rol administrador'
);

select is(
  (
    select r.codigo
    from public.usuario u
    join public.rol r on r.id_rol = u.id_rol
    where u.email = 'empleado@empleado.com'
  ),
  'empleado',
  'empleado local tiene rol empleado'
);

select is(
  (
    select r.codigo
    from public.usuario u
    join public.rol r on r.id_rol = u.id_rol
    where u.email = 'cliente@cliente.com'
  ),
  'cliente',
  'cliente local tiene rol cliente'
);

select is(
  (
    select count(*)::integer
    from public.punto_entrega pe
    where pe.descripcion in ('front_id:local', 'front_id:medicina', 'front_id:filosofia')
      and pe.activo = true
      and pe.eliminado = false
  ),
  3,
  'seed crea los tres puntos de entrega del mock frontend'
);

select is(
  (
    select pe.nombre
    from public.punto_entrega pe
    where pe.descripcion = 'front_id:local'
  ),
  'Retiro Local',
  'punto local coincide con mock frontend'
);

select is(
  (
    select pe.direccion_texto
    from public.punto_entrega pe
    where pe.descripcion = 'front_id:medicina'
  ),
  'Paraguay 2155 (C.A.B.A.)',
  'punto medicina coincide con mock frontend'
);

select is(
  (
    select pe.direccion_texto
    from public.punto_entrega pe
    where pe.descripcion = 'front_id:filosofia'
  ),
  'Púan 480 (C.A.B.A.)',
  'punto filosofia coincide con mock frontend'
);

select is(
  (
    select s.precio_base
    from public.servicio s
    where s.codigo = 'impresion_byn_pagina'
  ),
  20.00::numeric,
  'precio byn coincide con MOCK_PRICING.blackAndWhitePage'
);

select is(
  (
    select s.precio_base
    from public.servicio s
    where s.codigo = 'impresion_color_pagina'
  ),
  50.00::numeric,
  'precio color coincide con MOCK_PRICING.colorPage'
);

select is(
  (
    select s.precio_base
    from public.servicio s
    where s.codigo = 'encuadernado'
  ),
  500.00::numeric,
  'precio encuadernado coincide con MOCK_PRICING.bound'
);

select is(
  (
    select s.precio_base
    from public.servicio s
    where s.codigo = 'anillado'
  ),
  700.00::numeric,
  'precio anillado coincide con MOCK_PRICING.spiralBound'
);

select is(
  (
    select b.public
    from storage.buckets b
    where b.id = public.bucket_archivos_pedidos()
  ),
  false,
  'bucket archivos-pedidos no es publico'
);

select * from finish();

rollback;
