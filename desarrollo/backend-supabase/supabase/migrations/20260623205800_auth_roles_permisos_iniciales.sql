-- Auth, roles, permisos iniciales y helpers de autorizacion.
-- Issue relacionada: #96.

insert into public.rol (codigo, nombre, descripcion)
values
  ('cliente', 'Cliente', 'Usuario cliente que crea y consulta pedidos propios.'),
  ('empleado', 'Empleado', 'Usuario interno que gestiona el ciclo operativo de pedidos.'),
  ('administrador', 'Administrador', 'Usuario interno con privilegios completos del sistema.')
on conflict (codigo) do update
set
  nombre = excluded.nombre,
  descripcion = excluded.descripcion,
  activo = true,
  eliminado = false,
  fecha_eliminacion = null;

insert into public.permiso (codigo, nombre, descripcion)
values
  ('pedidos.crear', 'Crear pedidos propios', 'Permite crear pedidos propios desde el portal cliente.'),
  ('pedidos.ver_propios', 'Ver pedidos propios', 'Permite consultar pedidos propios.'),
  ('pedidos.editar_propios', 'Editar pedidos propios', 'Permite editar datos permitidos de pedidos propios mientras esten en estado editable.'),
  ('pedidos.cancelar_propios', 'Cancelar pedidos propios', 'Permite cancelar pedidos propios cuando el estado lo permite.'),
  ('pedidos.gestionar', 'Gestionar pedidos', 'Permite operar pedidos durante el ciclo administrativo y productivo.'),
  ('archivos.gestionar_pedidos', 'Gestionar archivos de pedidos', 'Permite operar archivos asociados a pedidos autorizados.'),
  ('pagos.gestionar', 'Gestionar pagos', 'Permite registrar y administrar pagos y cobros.'),
  ('usuarios.gestionar', 'Gestionar usuarios', 'Permite administrar usuarios de negocio.'),
  ('roles.gestionar', 'Gestionar roles y permisos', 'Permite administrar roles, permisos y asignaciones.'),
  ('catalogos.gestionar', 'Gestionar catalogos', 'Permite administrar catalogos operativos del sistema.'),
  ('auditoria.ver', 'Ver auditoria', 'Permite consultar eventos de auditoria.')
on conflict (codigo) do update
set
  nombre = excluded.nombre,
  descripcion = excluded.descripcion,
  activo = true,
  eliminado = false,
  fecha_eliminacion = null;

with asignacion(codigo_rol, codigo_permiso) as (
  values
    ('cliente', 'pedidos.crear'),
    ('cliente', 'pedidos.ver_propios'),
    ('cliente', 'pedidos.editar_propios'),
    ('cliente', 'pedidos.cancelar_propios'),
    ('empleado', 'pedidos.gestionar'),
    ('empleado', 'archivos.gestionar_pedidos'),
    ('empleado', 'pagos.gestionar'),
    ('administrador', 'pedidos.crear'),
    ('administrador', 'pedidos.ver_propios'),
    ('administrador', 'pedidos.editar_propios'),
    ('administrador', 'pedidos.cancelar_propios'),
    ('administrador', 'pedidos.gestionar'),
    ('administrador', 'archivos.gestionar_pedidos'),
    ('administrador', 'pagos.gestionar'),
    ('administrador', 'usuarios.gestionar'),
    ('administrador', 'roles.gestionar'),
    ('administrador', 'catalogos.gestionar'),
    ('administrador', 'auditoria.ver')
)
insert into public.rol_permiso (id_rol, id_permiso)
select r.id_rol, p.id_permiso
from asignacion a
join public.rol r on r.codigo = a.codigo_rol
join public.permiso p on p.codigo = a.codigo_permiso
where not exists (
  select 1
  from public.rol_permiso rp
  where rp.id_rol = r.id_rol
    and rp.id_permiso = p.id_permiso
    and rp.eliminado = false
);

create or replace function public.usuario_actual_id()
returns bigint
language sql
stable
security definer
set search_path = public
as $$
  select u.id_usuario
  from public.usuario u
  where u.id_usuario_auth = auth.uid()
    and u.estado = 'activo'
    and u.eliminado = false
  limit 1
$$;

create or replace function public.usuario_actual_rol()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select r.codigo
  from public.usuario u
  join public.rol r on r.id_rol = u.id_rol
  where u.id_usuario_auth = auth.uid()
    and u.estado = 'activo'
    and u.eliminado = false
    and r.activo = true
    and r.eliminado = false
  limit 1
$$;

create or replace function public.usuario_tiene_rol(codigo_rol text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.usuario u
    join public.rol r on r.id_rol = u.id_rol
    where u.id_usuario_auth = auth.uid()
      and u.estado = 'activo'
      and u.eliminado = false
      and r.activo = true
      and r.eliminado = false
      and r.codigo = codigo_rol
  )
$$;

create or replace function public.usuario_tiene_permiso(codigo_permiso text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.usuario u
    join public.rol r on r.id_rol = u.id_rol
    left join public.rol_permiso rp on rp.id_rol = r.id_rol and rp.eliminado = false
    left join public.permiso p on p.id_permiso = rp.id_permiso and p.activo = true and p.eliminado = false
    where u.id_usuario_auth = auth.uid()
      and u.estado = 'activo'
      and u.eliminado = false
      and r.activo = true
      and r.eliminado = false
      and (r.codigo = 'administrador' or p.codigo = codigo_permiso)
  )
$$;

revoke all on function public.usuario_actual_id() from public;
revoke all on function public.usuario_actual_rol() from public;
revoke all on function public.usuario_tiene_rol(text) from public;
revoke all on function public.usuario_tiene_permiso(text) from public;

grant execute on function public.usuario_actual_id() to authenticated, service_role;
grant execute on function public.usuario_actual_rol() to authenticated, service_role;
grant execute on function public.usuario_tiene_rol(text) to authenticated, service_role;
grant execute on function public.usuario_tiene_permiso(text) to authenticated, service_role;

create or replace function public.crear_usuario_desde_auth()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  id_rol_cliente bigint;
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
  on conflict (id_usuario_auth) do nothing;

  return new;
end;
$$;

revoke all on function public.crear_usuario_desde_auth() from public;

drop trigger if exists trg_auth_users_crear_usuario on auth.users;

create trigger trg_auth_users_crear_usuario
  after insert on auth.users
  for each row execute function public.crear_usuario_desde_auth();

create or replace function public.bloquear_escalamiento_rol_usuario()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  if new.id_rol is distinct from old.id_rol
    and current_user not in ('postgres', 'service_role')
    and not public.usuario_tiene_rol('administrador') then
    raise exception 'No esta permitido modificar el rol del usuario.'
      using errcode = '42501';
  end if;

  return new;
end;
$$;

drop trigger if exists trg_usuario_bloquear_escalamiento_rol on public.usuario;

create trigger trg_usuario_bloquear_escalamiento_rol
  before update of id_rol on public.usuario
  for each row execute function public.bloquear_escalamiento_rol_usuario();

alter table public.usuario enable row level security;
alter table public.rol enable row level security;
alter table public.permiso enable row level security;
alter table public.rol_permiso enable row level security;

grant select on public.usuario to authenticated;

create policy usuario_select_propio
  on public.usuario
  for select
  to authenticated
  using (
    id_usuario_auth = auth.uid()
    and estado = 'activo'
    and eliminado = false
  );
