# Instructivo local - rama desarrollo

Guia rapida para levantar backend Supabase y frontend Web en local.

## Requisitos

- Node.js y npm instalados.
- Docker Desktop o Docker Engine corriendo.
- Git con acceso al repo `AgusFT/La-Montana`.

## 1. Clonar rama desarrollo

```bash
git clone --branch desarrollo git@github.com-agusft:AgusFT/La-Montana.git
cd La-Montana
```

## 2. Levantar Supabase local

Abrir una terminal desde la raiz del repo:

```bash
cd desarrollo/backend-supabase
npm install
npm run supabase:start
npm run supabase:db:reset
npm run supabase:status
```

Del comando `npm run supabase:status`, copiar el valor de `PUBLISHABLE_KEY`.
Tambien deberia verse:

```txt
API_URL: http://127.0.0.1:54321
```

No hace falta ejecutar `npm run supabase:link` para trabajar localmente.

## 3. Configurar variables del frontend

Abrir otra terminal desde la raiz del repo:

```bash
cd desarrollo/web
npm install
```

Crear el archivo local `desarrollo/web/.env.local` con este contenido:

```bash
NEXT_PUBLIC_SUPABASE_URL=http://127.0.0.1:54321
NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY=<PEGAR_PUBLISHABLE_KEY_DE_SUPABASE_STATUS>
```

El archivo `.env.local` no se versiona.

## 4. Levantar frontend

Desde `desarrollo/web`:

```bash
npm run dev
```

Abrir en el navegador:

```txt
http://localhost:3000
```

## 5. Probar signup y login

Para crear un usuario nuevo:

1. Entrar a `http://localhost:3000/signup`.
2. Completar email y contrasena.
3. Nombre, apellido y telefono son optativos.
4. Crear la cuenta.
5. El backend asigna rol `cliente` automaticamente.

Para iniciar sesion:

1. Entrar a `http://localhost:3000/login`.
2. Usar el usuario creado o uno de los usuarios seed.
3. Al autenticar correctamente, la app redirige al dashboard.

## Usuarios seed locales

Estos usuarios se cargan con `npm run supabase:db:reset`.

| Rol | Email | Password |
|---|---|---|
| Administrador | `admin@admin.com` | `admin123` |
| Empleado | `empleado@empleado.com` | `empleado` |
| Cliente | `cliente@cliente.com` | `cliente` |

Para el Portal Cliente usar preferentemente:

```txt
cliente@cliente.com
cliente
```

Las cuentas `admin` y `empleado` existen para pruebas de backend/roles, pero el
Portal Cliente solo permite acceso con rol `cliente`.

## Checks utiles

Backend:

```bash
cd desarrollo/backend-supabase
npm run supabase:test:db
```

Frontend:

```bash
cd desarrollo/web
npm run lint
npm run build
```

Si `npm run build` falla por variables faltantes, revisar que exista
`desarrollo/web/.env.local` con la URL y publishable key de Supabase local.

## Puertos locales principales

| Servicio | URL |
|---|---|
| Web | `http://localhost:3000` |
| Supabase API | `http://127.0.0.1:54321` |
| Supabase Studio | `http://127.0.0.1:54323` |
| Mailpit/Inbucket | `http://127.0.0.1:54324` |

