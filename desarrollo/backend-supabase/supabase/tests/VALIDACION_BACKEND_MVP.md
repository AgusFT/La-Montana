# Validacion tecnica backend MVP

Guia de datos y comandos para validar el backend Supabase antes de conectar el
portal web y Android.

## Usuarios locales

Estos usuarios se cargan con `supabase/seed.sql` despues de `supabase db reset`.

| Rol | Email | Password |
|---|---|---|
| Administrador | `admin@admin.com` | `admin123` |
| Empleado | `empleado@empleado.com` | `empleado` |
| Cliente | `cliente@cliente.com` | `cliente` |

## Datos alineados con front

Los puntos de entrega del seed reproducen el mock
`desarrollo/web/src/mocks/delivery-points.ts` de la rama `feat/web-mvp`.

| Front id | Nombre | Direccion |
|---|---|---|
| `local` | Retiro Local | Lope de Vega 2150 (C.A.B.A.) |
| `medicina` | Facultad de Medicina | Paraguay 2155 (C.A.B.A.) |
| `filosofia` | Facultad de Filosofía | Púan 480 (C.A.B.A.) |

Como el modelo backend actual usa `id_punto_entrega` numerico, el `front id`
queda guardado temporalmente en `punto_entrega.descripcion` con formato
`front_id:<id>`. En la integracion #117 el front debe resolver el
`id_punto_entrega` real consultando puntos activos o el backend debe exponer un
contrato especifico para ese catalogo.

Los servicios del seed reproducen `MOCK_PRICING`:

| Codigo backend | Precio | Equivalente front |
|---|---:|---|
| `impresion_byn_pagina` | 20 ARS | `blackAndWhitePage` |
| `impresion_color_pagina` | 50 ARS | `colorPage` |
| `encuadernado` | 500 ARS | `bound` |
| `anillado` | 700 ARS | `spiralBound` |

## Comandos de validacion

Desde `desarrollo/backend-supabase`:

```bash
npm install
npm run supabase:start
npm run supabase:db:reset
npm run supabase:test:db
./node_modules/.bin/supabase db diff --local --schema public
```

Para validar Edge Functions localmente:

```bash
./node_modules/.bin/supabase functions serve clave-publica-cifrado crear-pedido cargar-archivo confirmar-pedido resumen-dashboard-cliente --no-verify-jwt
```

Endpoints locales:

| Funcion | URL |
|---|---|
| `clave-publica-cifrado` | `http://127.0.0.1:54321/functions/v1/clave-publica-cifrado` |
| `crear-pedido` | `http://127.0.0.1:54321/functions/v1/crear-pedido` |
| `cargar-archivo` | `http://127.0.0.1:54321/functions/v1/cargar-archivo` |
| `confirmar-pedido` | `http://127.0.0.1:54321/functions/v1/confirmar-pedido` |
| `resumen-dashboard-cliente` | `http://127.0.0.1:54321/functions/v1/resumen-dashboard-cliente` |

`resumen-dashboard-cliente` se invoca con metodo `GET` y JWT de cliente. Devuelve
totales, pedido actual, pedidos recientes y punto de entrega principal para
reemplazar los mocks del Dashboard Cliente (#105).

## Contrato de cifrado para archivos

Antes de invocar `cargar-archivo`, el frontend debe consultar
`clave-publica-cifrado` para obtener:

- `algoritmo_archivo`: `AES-GCM`;
- `longitud_clave_archivo_bits`: `256`;
- `algoritmo_envoltura_clave`: `RSA-OAEP`;
- `hash_envoltura_clave`: `SHA-256`;
- `version_cifrado`;
- `clave_publica_spki_pem`.

Con esos datos el frontend debe cifrar el PDF en cliente, calcular SHA-256 del
ciphertext y enviar a `cargar-archivo` un `multipart/form-data` con:

- `archivo`: ciphertext con `Content-Type: application/octet-stream`;
- `hash_archivo`: SHA-256 hexadecimal del ciphertext;
- `clave_envuelta`: clave AES envuelta con la clave publica backend;
- `iv`: IV usado por AES-GCM;
- `version_cifrado`: version devuelta por `clave-publica-cifrado`;
- metadata funcional: `id_pedido`, `nombre_original`, `mime_original` y
  `tamano_original_bytes`.

Variables necesarias:

| Variable | Uso |
|---|---|
| `CLAVE_PUBLICA_CIFRADO_ARCHIVOS` | Clave publica SPKI/PEM que consume `clave-publica-cifrado`. Puede cargarse con saltos `\n` escapados. |
| `VERSION_CIFRADO_ARCHIVOS` | Version del contrato de cifrado. Si no se configura, usa `aes-256-gcm+rsa-oaep-sha256:v1`. |

## Cobertura de tests

Los tests de `supabase/tests/database` validan:

- seed minimo de usuarios, roles, puntos de entrega y servicios;
- bucket privado `archivos-pedidos`;
- creacion de pedido propio;
- visibilidad mediante vistas cliente;
- bloqueo de acceso directo a tablas criticas para cliente;
- bloqueo de pedido ajeno;
- consulta interna para empleado;
- bloqueo de escalamiento de rol cliente;
- auditoria de `pedido_creado`, `archivo_pedido_cargado` y
  `pedido_confirmado`;
- confirmacion idempotente de pedido.
- contrato agregado del Dashboard Cliente mediante `resumen-dashboard-cliente`.

## Pendientes para integracion

- #113: el front debe consumir `clave-publica-cifrado`, cifrar el archivo antes
  de llamar `cargar-archivo` y enviar solo ciphertext + metadata segura.
- #116: el front debe obtener JWT real con Supabase Auth.
- #117: el front debe reemplazar `localStorage` por Edge Functions y resolver el
  mapeo de `deliveryPointId` del mock hacia `id_punto_entrega`.
