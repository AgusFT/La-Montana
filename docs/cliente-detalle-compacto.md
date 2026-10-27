# Cliente: detalle compacto del pedido

Mejora UX/UI del paso 3 · 27/10/2026. Continúa el [recorrido cliente](cliente-tres-etapas.md) y los [paneles de las etapas 1 y 2](cliente-paneles-compactos.md).

## Presentación

- Número, estado actual, fecha de confirmación y controles de actualización reunidos en la cabecera.
- Recorrido completo desplegable. Los avisos de estado desconocido, cancelación, producción, corrección y cobro permanecen visibles sin abrirlo, cuando corresponden al pedido.
- Resumen en tres paneles de escritorio: trabajo/archivos, importe/saldo y destino/franja. En tabletas y móviles se reordena según el ancho disponible.
- Lista acotada de trabajos con desplazamiento propio. Adicionales y metadatos del PDF se consultan por archivo; veinte PDF no generan veinte tarjetas extensas abiertas sobre toda la página.
- Historial y opciones de gestión/cancelación desplegables al final del resumen.
- Se conservan las secciones y sus enlaces `#pedido-*`. Los formularios siguen montados al alternar secciones para preservar borradores e intentos recuperables.
- El visor confirmado utiliza el modo compacto existente, con lista y páginas en paralelo cuando hay espacio. No pide aceptar nuevamente el archivo ni cambia los endpoints privados.

## Datos y alcance

El estado y las fechas proceden del pedido y del evaluador de seguimiento existente. El destino y la franja son los guardados en su reserva; no se derivan de la matriz vigente. El importe confirmado no se presenta como dinero cobrado: aplicado y pendiente se consultan en la API de pagos. Si falla esa consulta, se muestra el error sin inventar saldo cero.

Se mantienen permisos, versiones, huellas, recuperación de operaciones, cancelación controlada, correcciones, pagos, reprogramación, código de recepción, archivos confirmados, revocación e historial. El contenido revocado no recibe enlaces de descarga. No se modifica backend, esquema, configuración, precios, etapas 1/2, inicio del cliente ni la presentación interna del administrador/empleado.

## Verificación

Build de producción y TypeScript del frontend aprobados; 56 pruebas frontend/proxy existentes aprobadas. Chromium con componentes React reales y **API simulada**: resumen y lista de veinte PDF acotada; despliegue del recorrido; acceso a pagos y PDF confirmados; borrador de cancelación conservado al cambiar de sección; error financiero sin saldo inventado; aviso de pedido cancelado visible con recorrido cerrado; contenido revocado sin descarga.

Revisión a 320, 390, 768 y 1440 px sin desborde horizontal de página. La guía del paso 3 mantiene sus identificadores y se adapta a la nueva composición. No equivale a una prueba integral contra Docker/PostgreSQL/ClamAV: la aceptación final del equipo se realiza en su instalación habitual.
