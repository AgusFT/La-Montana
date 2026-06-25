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
./node_modules/.bin/supabase functions serve crear-pedido cargar-archivo confirmar-pedido --no-verify-jwt
```

Endpoints locales:

| Funcion | URL |
|---|---|
| `crear-pedido` | `http://127.0.0.1:54321/functions/v1/crear-pedido` |
| `cargar-archivo` | `http://127.0.0.1:54321/functions/v1/cargar-archivo` |
| `confirmar-pedido` | `http://127.0.0.1:54321/functions/v1/confirmar-pedido` |

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

## Pendientes para integracion

- #113: el front debe cifrar el archivo antes de llamar `cargar-archivo`.
- #116: el front debe obtener JWT real con Supabase Auth.
- #117: el front debe reemplazar `localStorage` por Edge Functions y resolver el
  mapeo de `deliveryPointId` del mock hacia `id_punto_entrega`.
