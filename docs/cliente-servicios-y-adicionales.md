# Servicio implícito y adicionales dinámicos

Actualización del 29/10/2026 en `pruebas`.

Cuando la oferta tiene un único servicio de impresión, la etapa 1 lo selecciona automáticamente y oculta el desplegable. El cliente empieza por tamaño de hoja, papel/gramaje y color; conserva copias, caras y adicionales. La misma selección implícita se utiliza para archivos nuevos, preparaciones recuperadas, indicadores de completitud y el envío del presupuesto. Si la oferta no tiene un servicio único, se conserva la selección explícita.

Los adicionales provienen de `GET /api/cliente/cotizaciones/opciones`, que cruza configuración operativa activa y catálogo comercial vigente mediante `OpcionesImpresion`. Se ofrecen todos los servicios de terminación habilitados compatibles con el formato y papel seleccionados. Los nombres visibles también proceden de la revisión comercial; no existe una lista fija de anillado/abrochado/plastificado en el formulario.

Al unificar combinaciones repetidas para distintos destinos, el frontend conserva todos sus identificadores de adicionales, sin duplicados ni mezcla con otros servicios, tamaños, papeles o colores. La pantalla muestra cuántos adicionales están disponibles. Cada selección se envía en `trabajo.terminaciones`; el servidor vuelve a validar habilitación y compatibilidad y calcula cada importe con su base de precio configurada.

No se cambian los precios, la configuración ni el backend de producción. Los adicionales deshabilitados, de revisiones futuras o incompatibles no deben ofrecerse como opciones válidas. La configuración vigente se recupera al cargar/actualizar los datos; una oferta desactualizada conserva el rechazo por versión del servidor.

## Verificación

- `pnpm typecheck` y `pnpm build` aprobados.
- `pnpm test:print-options`: seis casos, con selección automática, recuperación de campos, cambios de compatibilidad, ausencia/multiplicidad de servicios y conservación de 35 adicionales dinámicos sin contaminar otras combinaciones.
- `mvn -Dtest=AdicionalesClienteIntegrationTest test`: PostgreSQL real, 44 migraciones y configuración operativa de pruebas. Crea ocho servicios personalizados desde el catálogo, confirma una nueva revisión, consulta el endpoint HTTP con dos clientes y comprueba todos sus identificadores. El presupuesto incluye sus ocho líneas y suma exactamente sus precios configurados. Se rechazan adicionales deshabilitados/incompatibles; deshabilitar uno lo retira de las opciones y conserva la revisión histórica anterior.

Estas pruebas se ejecutan en datos aislados. No hace falta reiniciar los datos de la instalación local para aplicar el cambio.
