-- Politicas RLS iniciales del MVP.
-- Issue relacionada: #98.

create or replace function public.pedido_pertenece_a_usuario(id_pedido_consulta bigint)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.pedido p
    where p.id_pedido = id_pedido_consulta
      and p.id_usuario = public.usuario_actual_id()
      and p.eliminado = false
  )
$$;

create or replace function public.archivo_pertenece_a_usuario(id_archivo_consulta bigint)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.archivo a
    join public.pedido p on p.id_pedido = a.id_pedido
    where a.id_archivo = id_archivo_consulta
      and a.eliminado = false
      and p.eliminado = false
      and (a.id_usuario = public.usuario_actual_id() or p.id_usuario = public.usuario_actual_id())
  )
$$;

revoke all on function public.pedido_pertenece_a_usuario(bigint) from public;
revoke all on function public.archivo_pertenece_a_usuario(bigint) from public;

grant execute on function public.pedido_pertenece_a_usuario(bigint) to authenticated, service_role;
grant execute on function public.archivo_pertenece_a_usuario(bigint) to authenticated, service_role;

alter table public.usuario enable row level security;
alter table public.rol enable row level security;
alter table public.permiso enable row level security;
alter table public.rol_permiso enable row level security;
alter table public.contacto enable row level security;
alter table public.direccion enable row level security;
alter table public.punto_entrega enable row level security;
alter table public.servicio enable row level security;
alter table public.pedido enable row level security;
alter table public.pedido_servicio enable row level security;
alter table public.archivo enable row level security;
alter table public.pago enable row level security;
alter table public.pago_pedido enable row level security;
alter table public.auditoria enable row level security;

revoke all on public.usuario from anon, authenticated;
revoke all on public.rol from anon, authenticated;
revoke all on public.permiso from anon, authenticated;
revoke all on public.rol_permiso from anon, authenticated;
revoke all on public.contacto from anon, authenticated;
revoke all on public.direccion from anon, authenticated;
revoke all on public.punto_entrega from anon, authenticated;
revoke all on public.servicio from anon, authenticated;
revoke all on public.pedido from anon, authenticated;
revoke all on public.pedido_servicio from anon, authenticated;
revoke all on public.archivo from anon, authenticated;
revoke all on public.pago from anon, authenticated;
revoke all on public.pago_pedido from anon, authenticated;
revoke all on public.auditoria from anon, authenticated;

grant select, insert, update, delete on public.usuario to authenticated;
grant select, insert, update, delete on public.rol to authenticated;
grant select, insert, update, delete on public.permiso to authenticated;
grant select, insert, update, delete on public.rol_permiso to authenticated;
grant select, insert, update, delete on public.contacto to authenticated;
grant select, insert, update, delete on public.direccion to authenticated;
grant select, insert, update, delete on public.punto_entrega to authenticated;
grant select, insert, update, delete on public.servicio to authenticated;
grant select, insert, update, delete on public.pedido to authenticated;
grant select, insert, update, delete on public.pedido_servicio to authenticated;
grant select, insert, update, delete on public.archivo to authenticated;
grant select, insert, update, delete on public.pago to authenticated;
grant select, insert, update, delete on public.pago_pedido to authenticated;
grant select, insert, update, delete on public.auditoria to authenticated;

grant usage, select on all sequences in schema public to authenticated;

drop policy if exists usuario_select_propio on public.usuario;
drop policy if exists usuario_select_autorizado on public.usuario;
drop policy if exists usuario_insert_admin on public.usuario;
drop policy if exists usuario_update_admin on public.usuario;
drop policy if exists usuario_delete_admin on public.usuario;

create policy usuario_select_autorizado
  on public.usuario
  for select
  to authenticated
  using (
    id_usuario_auth = auth.uid()
    or public.usuario_tiene_permiso('usuarios.gestionar')
    or public.usuario_tiene_permiso('pedidos.gestionar')
  );

create policy usuario_insert_admin
  on public.usuario
  for insert
  to authenticated
  with check (public.usuario_tiene_permiso('usuarios.gestionar'));

create policy usuario_update_admin
  on public.usuario
  for update
  to authenticated
  using (public.usuario_tiene_permiso('usuarios.gestionar'))
  with check (public.usuario_tiene_permiso('usuarios.gestionar'));

create policy usuario_delete_admin
  on public.usuario
  for delete
  to authenticated
  using (public.usuario_tiene_permiso('usuarios.gestionar'));

drop policy if exists rol_crud_admin on public.rol;
drop policy if exists permiso_crud_admin on public.permiso;
drop policy if exists rol_permiso_crud_admin on public.rol_permiso;

create policy rol_crud_admin
  on public.rol
  for all
  to authenticated
  using (public.usuario_tiene_permiso('roles.gestionar'))
  with check (public.usuario_tiene_permiso('roles.gestionar'));

create policy permiso_crud_admin
  on public.permiso
  for all
  to authenticated
  using (public.usuario_tiene_permiso('roles.gestionar'))
  with check (public.usuario_tiene_permiso('roles.gestionar'));

create policy rol_permiso_crud_admin
  on public.rol_permiso
  for all
  to authenticated
  using (public.usuario_tiene_permiso('roles.gestionar'))
  with check (public.usuario_tiene_permiso('roles.gestionar'));

drop policy if exists contacto_select_autorizado on public.contacto;
drop policy if exists contacto_insert_autorizado on public.contacto;
drop policy if exists contacto_update_autorizado on public.contacto;
drop policy if exists contacto_delete_autorizado on public.contacto;

create policy contacto_select_autorizado
  on public.contacto
  for select
  to authenticated
  using (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
    or public.usuario_tiene_permiso('pedidos.gestionar')
  );

create policy contacto_insert_autorizado
  on public.contacto
  for insert
  to authenticated
  with check (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
  );

create policy contacto_update_autorizado
  on public.contacto
  for update
  to authenticated
  using (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
  )
  with check (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
  );

create policy contacto_delete_autorizado
  on public.contacto
  for delete
  to authenticated
  using (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
  );

drop policy if exists direccion_select_autorizado on public.direccion;
drop policy if exists direccion_insert_autorizado on public.direccion;
drop policy if exists direccion_update_autorizado on public.direccion;
drop policy if exists direccion_delete_autorizado on public.direccion;

create policy direccion_select_autorizado
  on public.direccion
  for select
  to authenticated
  using (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
    or public.usuario_tiene_permiso('pedidos.gestionar')
  );

create policy direccion_insert_autorizado
  on public.direccion
  for insert
  to authenticated
  with check (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
  );

create policy direccion_update_autorizado
  on public.direccion
  for update
  to authenticated
  using (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
  )
  with check (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
  );

create policy direccion_delete_autorizado
  on public.direccion
  for delete
  to authenticated
  using (
    id_usuario = public.usuario_actual_id()
    or public.usuario_tiene_permiso('usuarios.gestionar')
  );

drop policy if exists punto_entrega_select_activos on public.punto_entrega;
drop policy if exists punto_entrega_crud_admin on public.punto_entrega;
drop policy if exists servicio_select_activos on public.servicio;
drop policy if exists servicio_crud_admin on public.servicio;

create policy punto_entrega_select_activos
  on public.punto_entrega
  for select
  to authenticated
  using (
    (activo = true and eliminado = false)
    or public.usuario_tiene_permiso('catalogos.gestionar')
  );

create policy punto_entrega_crud_admin
  on public.punto_entrega
  for all
  to authenticated
  using (public.usuario_tiene_permiso('catalogos.gestionar'))
  with check (public.usuario_tiene_permiso('catalogos.gestionar'));

create policy servicio_select_activos
  on public.servicio
  for select
  to authenticated
  using (
    (activo = true and eliminado = false)
    or public.usuario_tiene_permiso('catalogos.gestionar')
  );

create policy servicio_crud_admin
  on public.servicio
  for all
  to authenticated
  using (public.usuario_tiene_permiso('catalogos.gestionar'))
  with check (public.usuario_tiene_permiso('catalogos.gestionar'));

drop policy if exists pedido_select_interno on public.pedido;
drop policy if exists pedido_insert_interno on public.pedido;
drop policy if exists pedido_update_interno on public.pedido;
drop policy if exists pedido_delete_interno on public.pedido;

create policy pedido_select_interno
  on public.pedido
  for select
  to authenticated
  using (public.usuario_tiene_permiso('pedidos.gestionar'));

create policy pedido_insert_interno
  on public.pedido
  for insert
  to authenticated
  with check (public.usuario_tiene_permiso('pedidos.gestionar'));

create policy pedido_update_interno
  on public.pedido
  for update
  to authenticated
  using (public.usuario_tiene_permiso('pedidos.gestionar'))
  with check (public.usuario_tiene_permiso('pedidos.gestionar'));

create policy pedido_delete_interno
  on public.pedido
  for delete
  to authenticated
  using (public.usuario_tiene_permiso('pedidos.gestionar'));

drop policy if exists pedido_servicio_select_autorizado on public.pedido_servicio;
drop policy if exists pedido_servicio_crud_interno on public.pedido_servicio;

create policy pedido_servicio_select_autorizado
  on public.pedido_servicio
  for select
  to authenticated
  using (
    public.pedido_pertenece_a_usuario(id_pedido)
    or public.usuario_tiene_permiso('pedidos.gestionar')
  );

create policy pedido_servicio_crud_interno
  on public.pedido_servicio
  for all
  to authenticated
  using (public.usuario_tiene_permiso('pedidos.gestionar'))
  with check (public.usuario_tiene_permiso('pedidos.gestionar'));

drop policy if exists archivo_select_interno on public.archivo;
drop policy if exists archivo_insert_interno on public.archivo;
drop policy if exists archivo_update_interno on public.archivo;
drop policy if exists archivo_delete_interno on public.archivo;

create policy archivo_select_interno
  on public.archivo
  for select
  to authenticated
  using (public.usuario_tiene_permiso('archivos.gestionar_pedidos'));

create policy archivo_insert_interno
  on public.archivo
  for insert
  to authenticated
  with check (public.usuario_tiene_permiso('archivos.gestionar_pedidos'));

create policy archivo_update_interno
  on public.archivo
  for update
  to authenticated
  using (public.usuario_tiene_permiso('archivos.gestionar_pedidos'))
  with check (public.usuario_tiene_permiso('archivos.gestionar_pedidos'));

create policy archivo_delete_interno
  on public.archivo
  for delete
  to authenticated
  using (public.usuario_tiene_permiso('archivos.gestionar_pedidos'));

drop policy if exists pago_crud_interno on public.pago;
drop policy if exists pago_pedido_crud_interno on public.pago_pedido;

create policy pago_crud_interno
  on public.pago
  for all
  to authenticated
  using (public.usuario_tiene_permiso('pagos.gestionar'))
  with check (public.usuario_tiene_permiso('pagos.gestionar'));

create policy pago_pedido_crud_interno
  on public.pago_pedido
  for all
  to authenticated
  using (public.usuario_tiene_permiso('pagos.gestionar'))
  with check (public.usuario_tiene_permiso('pagos.gestionar'));

drop policy if exists auditoria_select_admin on public.auditoria;
drop policy if exists auditoria_crud_admin on public.auditoria;

create policy auditoria_select_admin
  on public.auditoria
  for select
  to authenticated
  using (public.usuario_tiene_permiso('auditoria.ver'));

create policy auditoria_crud_admin
  on public.auditoria
  for all
  to authenticated
  using (public.usuario_tiene_rol('administrador'))
  with check (public.usuario_tiene_rol('administrador'));

drop view if exists public.archivo_cliente;
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

create view public.archivo_cliente
with (security_barrier = true)
as
select
  a.id_archivo,
  a.id_pedido,
  a.nombre_original,
  a.mime_original,
  a.tamano_bytes,
  a.estado_archivo,
  a.fecha_creacion,
  a.fecha_modificacion,
  a.eliminado,
  a.version_fila
from public.archivo a
join public.pedido p on p.id_pedido = a.id_pedido
where (a.id_usuario = public.usuario_actual_id() or p.id_usuario = public.usuario_actual_id())
  and a.eliminado = false
  and p.eliminado = false;

revoke all on public.pedido_cliente from anon, authenticated;
revoke all on public.archivo_cliente from anon, authenticated;

grant select on public.pedido_cliente to authenticated;
grant select on public.archivo_cliente to authenticated;
