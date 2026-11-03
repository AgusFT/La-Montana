# Asignación de riders y recorridos

## Decisión de negocio

El rider no consulta ni selecciona pedidos globales. Administración crea su cuenta individual desde la Web y le asigna un conjunto concreto cuando está físicamente en la sucursal para retirarlo.

La acción administrativa **Asignar e iniciar reparto** reúne en una sola transacción:

1. seleccionar pedidos ya preparados;
2. elegir un rider activo;
3. crear el recorrido y sus paradas;
4. asociar los pedidos seleccionados;
5. registrar el traspaso de custodia al rider;
6. aplicar `SALIR_REPARTO` y generar los códigos que correspondan;
7. hacer visible el recorrido en Android.

No se asigna rider durante **Preparar para despacho**. Se exige al poner en viaje, después de que el personal comprueba qué pedidos se entregan físicamente. Un pedido con reimpresión, calidad pendiente, incidencia, versión desactualizada o asignación activa no puede incorporarse.

## Experiencia administrativa

Disponible en la rama `pruebas`: **Administración → Empleados** permite crear o editar una cuenta y marcar **Habilitar como rider**. Las fichas identifican a los riders de reparto. En **Pedidos → Entregas pendientes**, elegí **Poner en viaje**, seleccioná los pedidos preparados y abrí **Despachar seleccionados**. El detalle de un pedido preparado también incluye **Elegir rider e iniciar reparto**, que lleva a esa bandeja.

El diálogo de salida conjunta incluye:

- selector obligatorio **Rider que retira**;
- sólo cuentas activas y habilitadas para reparto;
- manifiesto agrupado por destino;
- cantidad total de pedidos y paradas;
- advertencia de pedidos bloqueados antes de confirmar;
- acción final **Asignar e iniciar reparto de N pedidos**.

Si una validación falla, no se crea el recorrido ni se pone en viaje ningún pedido. Un reintento con el mismo UUID recupera el resultado original y no duplica recorrido, códigos ni movimientos.

## Experiencia del rider${added}### Android

Al iniciar sesión, el rider ve exclusivamente:

- recorrido activo;
- cantidad de pedidos y paradas;
- próxima parada;
- destinos y direcciones;
- pedidos agrupados por destino;
- incidencias propias;
- recorridos finalizados por esa identidad.

La referencia visual está en [Rider · recorrido y parada](mockups/android-v0-rider-recorrido.svg).

Una parada en punto agrupa todos los pedidos destinados al mismo kiosco. El rider presenta el lote y la cuenta del punto confirma la recepción. En domicilio, el rider ingresa el código del cliente; si corresponde efectivo, sólo podrá cobrar cuando tenga permiso explícito y el backend ofrezca la acción atómica de cobro y entrega.

## Custodia y reasignación

| Momento | Custodio | Confirmación |
|---|---|---|
| Pedido preparado | Imprenta | Personal de la sucursal |
| Asignar e iniciar reparto | Rider elegido | Administración entrega físicamente el grupo |
| Llegada a un punto | Continúa el rider | El rider presenta el lote |
| Recepción en el punto | Responsable del punto | Confirma los pedidos recibidos |
| Entrega a domicilio | Cliente/receptor | Código válido y reglas financieras cumplidas |
| Cliente ausente | Rider | Registra el intento; no supone devolución |

Cambiar el rider después de iniciar el viaje requiere **Transferir recorrido** o confirmar su devolución a la imprenta. Debe guardar rider anterior, rider nuevo o receptor interno, motivo, usuario autorizante, fecha y hora. Nunca se sobrescribe el responsable original.

## Contrato técnico implementado

La migración V52 agrega:

- cuenta/perfil de rider individual;
- recorrido con estado, sucursal de origen y rider actual;
- orden de pedidos para construir las paradas por destino;
- relación única entre recorrido y pedido;
- eventos de custodia, reasignación, incidencia y finalización.

`POST /api/operacion/entregas/lotes` exige `repartidor` cuando la acción es `SALIR_REPARTO`; `GET /api/rider/recorridos/activo` devuelve sólo el manifiesto del rider autenticado. El escenario 4 de pruebas aporta dos cuentas, recorridos finalizados y viajes activos con custodia auditable.

La salida individual por `/movimientos` tampoco permite omitir al rider: debe utilizarse la asignación conjunta, incluso para un solo pedido. El backend y la Web se publican en `pruebas`; la aplicación Android continúa en su rama `android`.

## Actualizar una instalación de pruebas

```sh
git switch pruebas
git pull --no-rebase --no-edit origin pruebas
./infra/pruebas/iniciar.sh
```

La migración V52 y la actualización del escenario 3 al 4 se aplican al iniciar, sin borrar pedidos ni pagos. Se incorporan `rider.oeste@prueba.local` y `rider.centro@prueba.local`, con la contraseña de pruebas configurada. Los accesos también figuran en `CuentasPruebas.md` y `/pruebas`.

## Criterios de aceptación

- No se puede poner un grupo en viaje sin rider.
- Un rider no ve pedidos o recorridos de otro.
- Todos los pedidos del recorrido superaron calidad y estaban preparados.
- Una misma operación no duplica asignación ni salida.
- Android agrupa pedidos por parada y muestra direcciones.
- El punto, no el rider, confirma la recepción que habilita el retiro.
- Cliente ausente no registra entrega, cobro ni devolución.
- Todo cambio conserva actor, fecha, hora, estado y custodia anterior/posterior.

## Verificación de la publicación en pruebas

- Backend compilado con Java 17 y las dependencias del proyecto; frontend con TypeScript y build de producción.
- PostgreSQL 18.6, con las 52 migraciones reales: aprobadas las pruebas de cuentas/aislamiento de riders, despacho conjunto transaccional y actualización idempotente del escenario existente conservando pedidos, pagos e historial.
- El administrador consulta los dos riders de prueba, crea uno nuevo y lo desactiva por HTTP; el selector deja de ofrecer la cuenta desactivada. Clientes, puntos y riders sin permiso de despacho no pueden consultar la lista de repartidores.
- Se rechaza salir sin rider por la operación conjunta y por la ruta individual. Se comprueban calidad, versiones, asignación del recorrido, código automático y recuperación del mismo intento sin duplicar movimientos.
- Aprobadas 22 pruebas de tareas operativas y situaciones del pedido en el frontend.
