-- Storage privado para archivos de pedidos.
-- Issue relacionada: #97.

insert into storage.buckets (
  id,
  name,
  public,
  file_size_limit,
  allowed_mime_types
)
values (
  'archivos-pedidos',
  'archivos-pedidos',
  false,
  10485760,
  array['application/octet-stream']::text[]
)
on conflict (id) do update
set
  name = excluded.name,
  public = false,
  file_size_limit = 10485760,
  allowed_mime_types = array['application/octet-stream']::text[],
  updated_at = now();

drop policy if exists storage_archivos_pedidos_service_role on storage.objects;

create policy storage_archivos_pedidos_service_role
  on storage.objects
  for all
  to service_role
  using (bucket_id = 'archivos-pedidos')
  with check (bucket_id = 'archivos-pedidos');

drop policy if exists storage_buckets_archivos_pedidos_service_role on storage.buckets;

create policy storage_buckets_archivos_pedidos_service_role
  on storage.buckets
  for all
  to service_role
  using (id = 'archivos-pedidos')
  with check (id = 'archivos-pedidos');

grant select on public.usuario to service_role;
grant select on public.pedido to service_role;
grant select, insert, update, delete on public.archivo to service_role;
grant usage, select on all sequences in schema public to service_role;

create or replace function public.limite_bytes_archivo_mvp()
returns bigint
language sql
immutable
set search_path = public
as $$
  select 10485760::bigint
$$;

create or replace function public.bucket_archivos_pedidos()
returns text
language sql
immutable
set search_path = public
as $$
  select 'archivos-pedidos'::text
$$;

create or replace function public.generar_ruta_storage_archivo(
  id_usuario_consulta bigint,
  id_pedido_consulta bigint,
  id_objeto_storage uuid
)
returns text
language sql
immutable
set search_path = public
as $$
  select format(
    'usuarios/%s/pedidos/%s/archivos/%s.bin',
    id_usuario_consulta,
    id_pedido_consulta,
    id_objeto_storage
  )
$$;

create or replace function public.ruta_storage_archivo_valida(
  id_usuario_consulta bigint,
  id_pedido_consulta bigint,
  ruta_storage_consulta text
)
returns boolean
language sql
immutable
set search_path = public
as $$
  select ruta_storage_consulta ~* format(
    '^usuarios/%s/pedidos/%s/archivos/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\.bin$',
    id_usuario_consulta,
    id_pedido_consulta
  )
$$;

create or replace function public.tamano_archivos_activos_usuario(
  id_usuario_consulta bigint,
  id_archivo_excluido bigint default null
)
returns bigint
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(sum(a.tamano_bytes), 0)::bigint
  from public.archivo a
  where a.id_usuario = id_usuario_consulta
    and a.eliminado = false
    and a.fecha_eliminacion is null
    and a.estado_archivo <> 'eliminado'
    and (id_archivo_excluido is null or a.id_archivo <> id_archivo_excluido)
$$;

create or replace function public.usuario_puede_agregar_archivo(
  id_usuario_consulta bigint,
  tamano_nuevo_bytes bigint,
  id_archivo_excluido bigint default null
)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select tamano_nuevo_bytes > 0
    and tamano_nuevo_bytes <= public.limite_bytes_archivo_mvp()
    and public.tamano_archivos_activos_usuario(id_usuario_consulta, id_archivo_excluido)
      + tamano_nuevo_bytes <= public.limite_bytes_archivo_mvp()
$$;

alter table public.archivo
  add constraint archivo_bucket_archivos_pedidos_check check (bucket = public.bucket_archivos_pedidos()),
  add constraint archivo_mime_pdf_mvp_check check (mime_original = 'application/pdf'),
  add constraint archivo_tamano_bytes_mvp_check check (
    tamano_bytes > 0
    and tamano_bytes <= public.limite_bytes_archivo_mvp()
  ),
  add constraint archivo_hash_archivo_no_vacio_check check (
    hash_archivo is not null
    and btrim(hash_archivo) <> ''
  ),
  add constraint archivo_clave_envuelta_no_vacio_check check (
    clave_envuelta is not null
    and btrim(clave_envuelta) <> ''
  ),
  add constraint archivo_iv_no_vacio_check check (
    iv is not null
    and btrim(iv) <> ''
  ),
  add constraint archivo_version_cifrado_no_vacio_check check (
    version_cifrado is not null
    and btrim(version_cifrado) <> ''
  ),
  add constraint archivo_estado_archivo_mvp_check check (
    estado_archivo in ('registrado', 'cargado', 'procesado', 'rechazado', 'eliminado')
  );

create or replace function public.validar_archivo_storage_mvp()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  if not exists (
    select 1
    from public.pedido p
    where p.id_pedido = new.id_pedido
      and p.id_usuario = new.id_usuario
      and p.eliminado = false
  ) then
    raise exception 'El archivo debe asociarse a un pedido activo del mismo usuario.'
      using errcode = '23503';
  end if;

  if not public.ruta_storage_archivo_valida(new.id_usuario, new.id_pedido, new.ruta_storage) then
    raise exception 'La ruta de Storage del archivo no respeta el formato esperado.'
      using errcode = '23514';
  end if;

  if new.eliminado = false and new.estado_archivo <> 'eliminado'
    and not public.usuario_puede_agregar_archivo(
      new.id_usuario,
      new.tamano_bytes,
      case when tg_op = 'UPDATE' then old.id_archivo else null end
    ) then
    raise exception 'El archivo supera el limite disponible de 10 MiB por cliente.'
      using errcode = '23514';
  end if;

  return new;
end;
$$;

drop trigger if exists trg_archivo_validar_storage_mvp on public.archivo;

create trigger trg_archivo_validar_storage_mvp
  before insert or update of id_pedido, id_usuario, tamano_bytes, bucket, ruta_storage, eliminado, estado_archivo
  on public.archivo
  for each row execute function public.validar_archivo_storage_mvp();

revoke all on function public.limite_bytes_archivo_mvp() from public;
revoke all on function public.bucket_archivos_pedidos() from public;
revoke all on function public.generar_ruta_storage_archivo(bigint, bigint, uuid) from public;
revoke all on function public.ruta_storage_archivo_valida(bigint, bigint, text) from public;
revoke all on function public.tamano_archivos_activos_usuario(bigint, bigint) from public;
revoke all on function public.usuario_puede_agregar_archivo(bigint, bigint, bigint) from public;

grant execute on function public.limite_bytes_archivo_mvp() to service_role;
grant execute on function public.bucket_archivos_pedidos() to service_role;
grant execute on function public.generar_ruta_storage_archivo(bigint, bigint, uuid) to service_role;
grant execute on function public.ruta_storage_archivo_valida(bigint, bigint, text) to service_role;
grant execute on function public.tamano_archivos_activos_usuario(bigint, bigint) to service_role;
grant execute on function public.usuario_puede_agregar_archivo(bigint, bigint, bigint) to service_role;

comment on function public.usuario_puede_agregar_archivo(bigint, bigint, bigint)
  is 'Valida limite MVP de 10 MiB por archivo y 10 MiB acumulados por cliente para archivos activos.';
