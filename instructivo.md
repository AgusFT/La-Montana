# Instructivo local - rama desarrollo

Guia para clonar la rama `desarrollo` y levantar en local el backend Supabase,
las Edge Functions y el frontend Web.

El objetivo es que funcione el flujo completo del Portal Cliente: login,
dashboard, cotizacion, creacion de pedido con PDF, confirmacion y cancelacion
del pedido mientras siga pendiente de confirmacion.

## Requisitos

- Git.
- Node.js 20 LTS o superior y npm.
- Docker Desktop, Docker Engine o Docker con WSL2 en Windows.
- Acceso al repositorio `AgusFT/La-Montana`.

No hace falta ejecutar `supabase link` para trabajar localmente.

## 1. Clonar la rama desarrollo

Usar HTTPS, portable para cualquier maquina:

```bash
git clone --branch desarrollo https://github.com/AgusFT/La-Montana.git
cd La-Montana
```

Alternativa SSH estandar:

```bash
git clone --branch desarrollo git@github.com:AgusFT/La-Montana.git
cd La-Montana
```

## 2. Levantar Supabase local

Abrir una primera terminal desde la raiz del repo.

Linux/macOS/Git Bash:

```bash
cd desarrollo/backend-supabase
npm install
npm run supabase:start
npm run supabase:db:reset
npm run supabase:status
```

Windows PowerShell:

```powershell
cd desarrollo\backend-supabase
npm install
npm run supabase:start
npm run supabase:db:reset
npm run supabase:status
```

Del comando `npm run supabase:status`, guardar estos valores:

```txt
API_URL
PUBLISHABLE_KEY
ANON_KEY
SERVICE_ROLE_KEY
```

En local normalmente `API_URL` debe ser:

```txt
http://127.0.0.1:54321
```

Si la CLI no muestra `PUBLISHABLE_KEY`, usar el valor de `ANON_KEY` tambien
como publishable key local.

## 3. Generar clave RSA local para cifrado de archivos

Desde `desarrollo/backend-supabase`, ejecutar este comando. Funciona igual en
Bash y en PowerShell porque usa Node.js.

```bash
node -e "const { generateKeyPairSync } = require('crypto'); const fs = require('fs'); fs.mkdirSync('.dev-keys', { recursive: true }); const { publicKey, privateKey } = generateKeyPairSync('rsa', { modulusLength: 2048, publicKeyEncoding: { type: 'spki', format: 'pem' }, privateKeyEncoding: { type: 'pkcs8', format: 'pem' } }); fs.writeFileSync('.dev-keys/cifrado_archivos_private.pem', privateKey); fs.writeFileSync('.dev-keys/cifrado_archivos_public.pem', publicKey); console.log(publicKey.trim().replace(/\r?\n/g, String.fromCharCode(92, 110)));"
```

El comando crea:

```txt
desarrollo/backend-supabase/.dev-keys/cifrado_archivos_private.pem
desarrollo/backend-supabase/.dev-keys/cifrado_archivos_public.pem
```

Ademas imprime por consola la clave publica en una sola linea, con `\n`
escapados. Copiar esa salida para usarla en `CLAVE_PUBLICA_CIFRADO_ARCHIVOS`.

La clave privada queda solo en la maquina local. No commitear `.dev-keys`.

## 4. Configurar variables del backend

Crear el archivo local `desarrollo/backend-supabase/.env`.

Linux/macOS/Git Bash:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Editar `desarrollo/backend-supabase/.env` y dejarlo con valores locales:

```env
SUPABASE_PROJECT_REF=local
SUPABASE_URL=http://127.0.0.1:54321

SUPABASE_PUBLISHABLE_KEY=<PEGAR_PUBLISHABLE_KEY_O_ANON_KEY>
SUPABASE_ANON_KEY=<PEGAR_ANON_KEY_O_PUBLISHABLE_KEY>
SUPABASE_SECRET_KEY=<PEGAR_SERVICE_ROLE_KEY>
SUPABASE_SERVICE_ROLE_KEY=<PEGAR_SERVICE_ROLE_KEY>
SUPABASE_DB_PASSWORD=postgres

CLAVE_PUBLICA_CIFRADO_ARCHIVOS=<PEGAR_CLAVE_PUBLICA_RSA_EN_UNA_LINEA_CON_BARRA_N>
VERSION_CIFRADO_ARCHIVOS=aes-256-gcm+rsa-oaep-sha256:v1
```

Ejemplo del formato esperado para la clave publica:

```txt
CLAVE_PUBLICA_CIFRADO_ARCHIVOS=-----BEGIN PUBLIC KEY-----\nMIIB...\n-----END PUBLIC KEY-----
```

El archivo `.env` es local y no se versiona.

## 5. Levantar Edge Functions

Abrir una segunda terminal desde la raiz del repo. Esta terminal debe quedar
abierta mientras se prueba la aplicacion.

Linux/macOS/Git Bash:

```bash
cd desarrollo/backend-supabase
./node_modules/.bin/supabase functions serve --env-file .env --no-verify-jwt clave-publica-cifrado crear-pedido cotizar-pedido cargar-archivo confirmar-pedido cancelar-pedido resumen-dashboard-cliente
```

Windows PowerShell:

```powershell
cd desarrollo\backend-supabase
.\node_modules\.bin\supabase functions serve --env-file .env --no-verify-jwt clave-publica-cifrado crear-pedido cotizar-pedido cargar-archivo confirmar-pedido cancelar-pedido resumen-dashboard-cliente
```

Sin este paso, el frontend puede abrir, pero no van a funcionar correctamente
las operaciones que llaman a `/functions/v1/...`.

## 6. Configurar variables del frontend

Abrir una tercera terminal desde la raiz del repo.

Linux/macOS/Git Bash:

```bash
cd desarrollo/web
npm install
```

Windows PowerShell:

```powershell
cd desarrollo\web
npm install
```

Crear el archivo local `desarrollo/web/.env.local`:

```env
NEXT_PUBLIC_SUPABASE_URL=http://127.0.0.1:54321
NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY=<PEGAR_PUBLISHABLE_KEY_O_ANON_KEY>
```

Si la CLI local solo muestra `ANON_KEY`, se puede usar ese mismo valor en
`NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`.

El archivo `.env.local` es local y no se versiona.

## 7. Levantar frontend

Desde `desarrollo/web`, en la tercera terminal:

```bash
npm run dev
```

Abrir en el navegador:

```txt
http://localhost:3000
```

Para que todo funcione deben quedar activas:

| Terminal | Servicio |
|---|---|
| 1 | Supabase local iniciado con `npm run supabase:start` |
| 2 | Edge Functions iniciadas con `supabase functions serve` |
| 3 | Frontend iniciado con `npm run dev` |

## 8. Usuarios seed locales

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

## 9. Prueba rapida del flujo cliente

1. Entrar a `http://localhost:3000/login`.
2. Iniciar sesion con `cliente@cliente.com` y password `cliente`.
3. Ir al dashboard del cliente.
4. Crear un pedido nuevo.
5. Seleccionar producto, terminacion, color y punto de entrega.
6. Cargar un archivo PDF menor o igual a 10 MiB.
7. Confirmar el pedido.
8. Desde el pedido actual, verificar que la opcion de cancelar solo aparezca
   mientras el pedido siga pendiente de confirmacion.

## 10. Checks utiles

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

## 11. Puertos locales principales

| Servicio | URL |
|---|---|
| Web | `http://localhost:3000` |
| Supabase API | `http://127.0.0.1:54321` |
| Supabase Studio | `http://127.0.0.1:54323` |
| Mailpit/Inbucket | `http://127.0.0.1:54324` |

## 12. Problemas frecuentes

### `clave-publica-cifrado` devuelve 500

Revisar que `desarrollo/backend-supabase/.env` exista y tenga:

```txt
CLAVE_PUBLICA_CIFRADO_ARCHIVOS
VERSION_CIFRADO_ARCHIVOS
```

Tambien revisar que las Edge Functions se hayan iniciado con:

```bash
--env-file .env
```

### `cargar-archivo` devuelve `METHOD_NOT_ALLOWED`

Ese error aparece cuando la funcion se llama con un metodo distinto de `POST`,
por ejemplo si se abre la URL directamente en el navegador.

Desde la aplicacion, la carga correcta se hace con `POST multipart/form-data`.
Si falla desde la app, revisar en DevTools el request rojo de `cargar-archivo`
y confirmar que diga `Request Method: POST`.

### El frontend abre pero no crea pedidos

Verificar:

- Supabase local esta iniciado.
- Las Edge Functions estan corriendo en la segunda terminal.
- `desarrollo/backend-supabase/.env` tiene las claves locales.
- `desarrollo/web/.env.local` tiene la URL y publishable key locales.
- Despues de cambiar envs, reiniciar `supabase functions serve` y `npm run dev`.
