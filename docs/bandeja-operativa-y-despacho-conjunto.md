# Bandeja operativa y despacho conjunto

La bandeja del personal muestra la situación concreta de los trabajos, la fecha del hecho que la sostiene y el último movimiento del pedido. La confirmación original se conserva como dato separado. El orden puede cambiar entre movimientos recientes/antiguos y confirmaciones recientes/antiguas; se aplica en servidor antes de paginar.

## Situaciones visibles

| Situación | Color | Hecho que la determina |
| --- | --- | --- |
| Requiere reimpresión | Rojo | Un último intento de un PDF vigente tiene calidad `REIMPRESION_REQUERIDA`. |
| Incidencia de impresión | Rojo | Un último intento tiene incidencia de calidad, error o cancelación. |
| Falta preparar trabajos | Amarillo | Algún PDF vigente todavía no tiene trabajo iniciado. |
| Confirmar impresión y calidad | Amarillo | Todos los PDF están iniciados; queda impresión o inspección sin confirmar. No supone que la impresora haya terminado automáticamente. |
| Calidad aprobada · preparar despacho | Amarillo | Pedido completo aprobado en calidad y logística pendiente de preparación. |
| Preparado para despachar | Verde | Preparación física registrada. |
| En viaje | Azul | Salida registrada. |
| Disponible para retirar | Verde | Retiro local o llegada registrada en destino. |

Se evalúa el último trabajo de cada ítem de `pedido_archivo_actual`, no intentos antiguos ni archivos reemplazados por una corrección. Las incidencias y reimpresiones tienen prioridad sobre otras tareas pendientes. La fecha mostrada junto a la situación corresponde al hecho vigente de producción/calidad o a la transición logística; el último movimiento tiene su propia fecha y responsable. Una nota posterior no convierte una calidad rechazada en aprobada.

Los mismos nombres y colores se usan en las listas del dashboard. El detalle de producción reconoce la reimpresión requerida y utiliza la misma guía visual al recibir la versión actual de sus trabajos.

## Preparar y despachar juntos

La gestión conjunta está en **Entregas pendientes**, sólo para personal con `GESTIONAR_ENTREGAS`:

1. Elegir **Preparar para despacho** y seleccionar pedidos completos con calidad aprobada.
2. Confirmar el grupo de pedidos y sus destinos.
3. Cuando salgan físicamente juntos, elegir **Poner en viaje**, seleccionar los pedidos preparados y confirmar su salida.

La selección abarca hasta 25 pedidos de la página actual. Cambiar tarea, filtro, orden o página limpia la selección. Los retiros locales y pedidos ya en viaje o disponibles no se seleccionan para una nueva salida. No se implementan vehículos, choferes ni optimización de rutas: el grupo registra un hecho operativo conjunto, que puede reunir destinos distintos.

`POST /api/operacion/entregas/lotes` acepta un UUID de operación, la acción y los identificadores/versiones de los pedidos. Comprueba permisos, alcance de cada pedido, versiones, calidad de todos sus ítems y paso logístico antes de escribir. Todo el grupo se guarda en una transacción; un fallo revierte movimientos, códigos y auditoría.

Cada pedido conserva su evento con usuario, fecha, hora e identificador del grupo. El despacho reutiliza la generación automática de su código y aviso al cliente. La migración V48 agrega la constancia inmutable del grupo; un reintento idéntico devuelve el resultado guardado y no repite movimientos. Reutilizar su UUID con otros pedidos o acción se rechaza. La interfaz conserva el mismo intento ante una respuesta incierta.

## Verificación

- Frontend: TypeScript, build y 22 pruebas de estados operativos, tareas y proxy.
- Backend: compilación de todas las fuentes con Java 17 y dependencias del proyecto. Comprobación ejecutada del servicio de grupos con dobles JDBC: calidad, versión, permisos, estado, duplicados, auditoría e idempotencia.
- Navegador con componentes reales y API simulada: colores/fechas, selección restringida, preparación de dos pedidos, recuperación de respuesta incierta y salida conjunta a `EN_VIAJE`.
- Prueba PostgreSQL agregada: `DatosPruebaIntegrationTest#bandejaOperativaYDespachoConjuntoConservanCalidadVersionYAuditoria`. Incluye orden global, versiones, rechazo de producción/calidad pendiente, rollback por fallo de auditoría y códigos. **No ejecutada en este entorno**, que carece de Maven y PostgreSQL de pruebas; la descarga de herramientas no estuvo disponible.

Para ejecutar la prueba integral en un entorno con Maven:

```sh
cd backend
mvn -Dtest=DatosPruebaIntegrationTest#bandejaOperativaYDespachoConjuntoConservanCalidadVersionYAuditoria test
```
