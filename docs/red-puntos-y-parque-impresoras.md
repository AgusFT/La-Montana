# Puntos de entrega e inventario de impresoras

## Administrar puntos después de la instalación

Desde **Administración → Puntos de entrega** se pueden crear y editar puntos sin abrir ni activar otra configuración general.

1. Crear primero las sucursales desde **Sucursales**.
2. Elegir **Agregar punto** y completar código, nombre, dirección y zona horaria.
3. Marcar una o varias sucursales de origen. Cada origen tiene una prioridad única, un costo explícito de traslado y franjas de retiro con cupo. Prioridad 1 se intenta primero entre las sucursales disponibles que pueden fabricar el trabajo.
4. Guardar. El punto queda habilitado para pedidos nuevos cuando existe una configuración operativa activa y al menos una ruta utilizable.
5. Crear su cuenta responsable desde el mismo apartado para gestionar recepción, cobros autorizados y entrega.

Cada ficha muestra las sucursales asignadas y la disponibilidad efectiva. También se puede filtrar por sucursal. En **Sucursales**, cada ficha muestra sus puntos y enlaza a la edición de esa red.

La pausa de una sucursal deja sus puntos exclusivos **Momentáneamente fuera de servicio**: siguen visibles y no se pueden seleccionar para pedidos nuevos. Un punto compartido puede seguir atendiendo por otra sucursal disponible con ruta habilitada. La pausa propia del punto se conserva al reabrir una sucursal.

Las ediciones guardan revisiones inmutables. Las cotizaciones pendientes deben actualizar sus condiciones si cambió el punto. Los pedidos confirmados conservan la dirección, el origen, el costo y la revisión de la red que reservaron. Reasignar un punto no impide completar pedidos que ya estaban confirmados. El rider presenta los pedidos y el responsable del punto confirma su llegada antes del retiro.

La Fase 5 del configurador consulta esta red actual y ofrece un enlace a su administración. Los puntos pueden agregarse después de terminar la configuración inicial. Los calendarios productivos y tiempos generales continúan gestionándose en el configurador.

## Comparar impresoras vigentes y borrador

La Fase 4 muestra todas las sucursales y compara el inventario vigente con el borrador. Los equipos vigentes que faltan en una copia parcial también se muestran, marcados como pendientes de incorporar.

**Incorporar N impresoras faltantes** agrega únicamente las identidades que faltan, con sus formatos y sucursal productiva. Conserva los nombres, capacidades, estados, asignaciones y retiros explícitos que ya se editaron en el borrador. No duplica equipos. La revisión integral impide activar una copia que omite impresoras vigentes sin haberlas incorporado y retirado expresamente.

Los conteos y movimientos de papel se consultan desde **Impresoras**. El estado previsto en el borrador y la habilitación operativa diaria son decisiones distintas. Los equipos operan en su sucursal productiva asignada; esa ubicación es obligatoria.

## Actualización

La migración V63 importa las definiciones y rutas de la configuración activa a la red independiente, conservando las identidades de puntos y sus cuentas. No reemplaza borradores, pedidos ni configuraciones anteriores. Las reservas anteriores siguen usando sus datos históricos. No requiere reiniciar los datos de pruebas.
