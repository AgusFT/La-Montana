# Backend Supabase - La Montana

Este directorio contiene la configuracion versionable del backend Supabase del proyecto.

## Alcance

Incluye la base para trabajar sobre:

- PostgreSQL administrado por Supabase.
- Supabase Auth.
- Supabase Storage.
- migraciones de base de datos.
- politicas RLS.
- RPC y Edge Functions.
- datos iniciales de desarrollo.
- pruebas tecnicas de base de datos y reglas de acceso.

## Estructura

```txt
desarrollo/backend-supabase/
├── README.md
├── .env.example
├── package.json
├── package-lock.json
└── supabase/
    ├── config.toml
    ├── seed.sql
    ├── functions/
    ├── migrations/
    └── tests/
        └── database/
```

## Carpetas de trabajo

| Ruta | Uso previsto |
|---|---|
| `supabase/config.toml` | Configuracion local del proyecto Supabase CLI |
| `supabase/seed.sql` | Datos iniciales para entornos reproducibles de desarrollo |
| `supabase/migrations/` | Migraciones SQL versionadas y aplicables al proyecto Supabase |
| `supabase/functions/` | Edge Functions desplegables |
| `supabase/tests/database/` | Pruebas de base de datos, RLS, RPC y reglas criticas |

## Relacion con el WBS

El bloque `Desarrollo > Backend - Supabase` del WBS V3 se implementa en este directorio asi:

| Elemento WBS | Representacion en el repo |
|---|---|
| README backend-supabase | `README.md` |
| Configuracion de variables de entorno | `.env.example` y `.env` local no versionado |
| Configuracion de Supabase CLI | `package.json`, `package-lock.json`, `supabase/config.toml` |
| Configuracion del proyecto Supabase | `supabase/config.toml` |
| Datos iniciales de desarrollo | `supabase/seed.sql` |
| Supabase Auth / perfiles / roles | migraciones en `supabase/migrations/` |
| Base de datos PostgreSQL | migraciones en `supabase/migrations/` |
| Storage y buckets | migraciones en `supabase/migrations/` |
| RLS y politicas aplicadas | migraciones en `supabase/migrations/` y pruebas en `supabase/tests/database/` |
| RPC definidas | migraciones en `supabase/migrations/` |
| Auditoria de eventos criticos | migraciones en `supabase/migrations/` |
| Acceso autorizado a archivos | migraciones en `supabase/migrations/` y pruebas en `supabase/tests/database/` |
| Flujo de revision administrativa | migraciones en `supabase/migrations/` y pruebas en `supabase/tests/database/` |
| Edge Functions implementadas | `supabase/functions/` |
| Pruebas bdd, rls, unitarias, funcionales, otras | `supabase/tests/database/` |

## Convencion de migraciones

Las categorias del WBS no se representan como subcarpetas SQL. Se representan como archivos de migracion ordenados dentro de `supabase/migrations/`.

Ejemplo de nombres previstos:

```txt
202606190001_auth_perfiles_roles.sql
202606190002_base_datos_postgresql.sql
202606190003_storage_buckets.sql
202606190004_rls_politicas.sql
202606190005_rpc_definidas.sql
202606190006_auditoria_eventos_criticos.sql
202606190007_acceso_autorizado_archivos.sql
202606190008_flujo_revision_administrativa.sql
```

## Uso local

Los comandos de Supabase se ejecutan desde este directorio:

```bash
cd desarrollo/backend-supabase
npm install
npm run supabase:version
npm run supabase:link
npm run supabase:start
```

El archivo `.env` es local y no debe versionarse. Para compartir la estructura de variables se usa `.env.example`.
