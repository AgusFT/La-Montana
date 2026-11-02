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

El diálogo actual de salida conjunta agregará:

- selector obligatorio **Rider que retira**;
- sólo cuentas activas y habilitadas para reparto;
- manifiesto agrupado por destino;
- cantidad total de pedidos y paradas;
- advertencia de pedidos bloqueados antes de confirmar;
- acción final **Asignar e iniciar reparto de N pedidos**.

Si una validación falla, no se crea el recorrido ni se pone en viaje ningún pedido. Un reintento con el mismo UUID recupera el resultado original y no duplica recorrido, códigos ni movimientos.

## Experiencia Android del rider

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

## Contrato técnico pendiente

La implementación deberá agregar, mediante una nueva migración, entidades equivalentes a:

- cuenta/perfil de rider individual;
- recorrido con estado, sucursal de origen y rider actual;
- parada con orden, tipo y destino;
- relación inmutable entre recorrido y pedido;
- eventos de custodia, reasignación, incidencia y finalización.

El lote existente de `POST /api/operacion/entregas/lotes` podrá evolucionar para exigir `rider` cuando la acción sea `SALIR_REPARTO`. Las consultas Android deberán devolver sólo recorridos asignados a la identidad autenticada. La autorización se comprueba en backend; conocer un UUID no concede acceso.

## Criterios de aceptación

- No se puede poner un grupo en viaje sin rider.
- Un rider no ve pedidos o recorridos de otro.
- Todos los pedidos del recorrido superaron calidad y estaban preparados.
- Una misma operación no duplica asignación ni salida.
- Android agrupa pedidos por parada y muestra direcciones.
- El punto, no el rider, confirma la recepción que habilita el retiro.
- Cliente ausente no registra entrega, cobro ni devolución.
- Todo cambio conserva actor, fecha, hora, estado y custodia anterior/posterior.
