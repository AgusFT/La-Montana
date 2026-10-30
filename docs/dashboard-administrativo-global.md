# Dashboard administrativo: una imprenta, una bandeja global

Corrección funcional · 27/10/2026. Continúa la separación entre [oferta global y destino](alcance-global-imprenta-y-recorrido-cliente.md), [casa matriz versionada](casa-matriz-y-retiro-versionado.md) y [recorrido Cliente](cliente-tres-etapas.md).

## Problema y decisión

El dashboard todavía pedía «Sucursal del resumen» y consultaba `/api/operacion/sucursales/{id}/pedidos`. Ese endpoint filtra por el origen interno guardado del pedido; no es una vista de toda la imprenta ni del destino de retiro. Por eso un pedido podía verse en Urquiza y desaparecer al seleccionar Medicina. Ocultar el selector sin cambiar la consulta no resolvía el alcance.

El propietario `ADMIN_ADMIN` ahora consulta **todos los pedidos de la imprenta**, independientemente de origen o destino. Se incluyen tanto pedidos nuevos como históricos, sin restringir la consulta a la matriz vigente ni a sucursales activas. El destino elegido por el cliente no divide la cola de revisión, producción ni gestión.

## Comportamiento

Actualización 30/10/2026: **Pedidos y tareas** muestra cuatro botones rápidos, con los mismos estados y nombres que los cuatro paneles del dashboard: Pendientes de revisión, Aprobados, En producción y Entregas pendientes. La definición se comparte en `operation-queues.ts`. Correcciones solicitadas conserva su métrica y acceso del dashboard; junto con entregados, cerrados, rechazados y cancelados se consulta desde **Filtros avanzados**, plegado en la vista habitual. Una entrada directa con un estado adicional abre el avanzado y conserva la selección. “Ver todos los pedidos” limpia el filtro y cambiar de estado reinicia la paginación. El selector del cliente conserva su presentación anterior.

Validación del ajuste: TypeScript/build y las dos pruebas del proxy administrativo aprobados. Componentes React reales con API simulada: cuatro botones, avanzado/correcciones, navegación del dashboard mediante router de ensayo, página 2 de producción y regreso a página 1 al cambiar de estado. A 320 px los botones se distribuyen en dos columnas sin desbordar la página. No se modifica la consulta del servidor, su alcance ni sus permisos; no hay migraciones.

- Dashboard sin selector de sucursal, con conteos globales por estado. Los errores de consulta se muestran como no disponibles, nunca como cero.
- Las filas muestran modalidad, destino persistido y situación logística. «Retiro con traslado» distingue la sucursal secundaria; no se presenta el origen productivo como destino.
- «Ver todos» abre `/administracion/pedidos?estado=...`, conservando el filtro de estado. El enlace general permite consultar también entregados, cerrados, rechazados y cancelados.
- La bandeja global tiene paginación de 25 y filtros por estado. No se suman bandejas de sucursales en el navegador, evitando duplicados u omisiones.
- Los enlaces «Ver pedido» siguen al detalle operativo existente: revisión, archivos, pagos, producción, correcciones, reprogramación y entrega, según sus permisos y condiciones.
- Desde el detalle del administrador, «Volver a pedidos» vuelve a la bandeja global. La navegación «Operación y cobros» también apunta allí; `/operacion` redirige al propietario para conservar enlaces antiguos.
- Los importes son valores de pedidos, **no ingresos cobrados**. No se acreditan pagos ni se alteran importes al cambiar esta presentación.

## API y seguridad

`GET /api/admin/pedidos?pagina=0&estado=PENDIENTE_REVISION`

Sólo `ADMIN_ADMIN` activo. La seguridad HTTP y el servicio rechazan clientes y empleados; no se admite escritura en esta ruta. El proxy conserva sesión, query, métodos explícitos y `no-store`. Se validan estado y número de página.

La consulta reutiliza `pedido_actual`, la cotización efectiva, el estado logístico y la última reserva persistida. El resumen añade `destino` (nombre guardado de la reserva) y `retiroConTraslado`; conserva los campos anteriores para compatibilidad. Los detalles son los ya existentes, no copias nuevas del pedido.

**No se amplían permisos de empleados.** Sus bandejas por sucursal autorizada y los controles por origen interno permanecen. Elegir un destino de retiro no otorga acceso al personal de ese destino. Un eventual reparto global de tareas para empleados requiere una decisión de permisos propia; este cambio no la inventa.

No hay migración nueva: se reutilizan datos existentes. No se modifica ninguna oferta, reserva, casa matriz histórica, precio, estado ni asignación de impresora. El cliente no obtiene un selector productivo ni cambia su recorrido aprobado.

## Verificación

Resultado de esta entrega: compilación backend y build/TypeScript de frontend aprobados; **10 escenarios HTTP** (2 nuevos de administración global + 8 de regresión Cliente) y **56 pruebas frontend/proxy**, sin fallos. Ensayo Chromium de componentes reales con API simulada aprobado.

Pruebas nuevas de backend HTTP: tres pedidos con dos orígenes internos y destinos distintos, incluyendo el nuevo flujo global; conteo y destino correctos, cancelado conservado, filtro por estado, alcance privado de cada cliente, permisos por sucursal de empleados, rechazo de acceso global ajeno y validación de filtros/paginación. Se ejecutan junto con las ocho pruebas del recorrido Cliente.

Pruebas del proxy: reenvío de estado/página/cookie, rechazo de escrituras y rutas vecinas. Regresión frontend completa.

Navegador con componentes React reales y **API simulada**: ausencia del selector, destinos Urquiza/Medicina juntos, conteos globales, enlaces, paginación, filtro, fallo parcial sin falso cero y ancho 390/768/1440 sin desborde ni errores JavaScript. Esto no se presenta como prueba integral de frontend/backend.

Los ensayos HTTP usan Spring, sesiones y CSRF reales, PDFBox y base efímera PGlite por JDBC, con antivirus de ensayo. No certifican concurrencia multiproceso ni Docker/PostgreSQL/ClamAV nativos. La aceptación local del equipo continúa siendo necesaria.

## Prueba local sin borrar datos

```bash
git switch desarrollo
git pull --ff-only
./infra/iniciar-local.sh
```

El script ya reconstruye y levanta Docker. No borrar volúmenes ni reinstalar la base. Entrar como administrador: deben verse juntos los pedidos de los distintos destinos, conservarse el filtro al usar «Ver todos» y mantenerse la navegación global al volver desde un pedido. La cuenta de empleado debe conservar su alcance anterior.
