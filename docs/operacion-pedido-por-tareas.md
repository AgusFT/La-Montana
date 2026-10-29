# Detalle operativo orientado al trabajo

El detalle `/operacion/pedidos/[id]` abre la tarea que necesita el pedido y que corresponde al perfil conectado. Reemplaza la fila de ocho pestañas por una tarea principal y un resumen lateral del PDF, sus opciones, el cliente, el destino, la reserva y el importe.

## Criterio de entrada

| Situación | Qué aparece primero |
| --- | --- |
| Pendiente de revisión, con permiso de pedidos | Registrar revisión; después, aprobar cuando el servidor lo habilita |
| Pendiente de revisión, sin ese permiso | Esperando al responsable de revisión, con consulta del trabajo |
| Anticipo pendiente, perfil financiero | Caja y comprobantes |
| Aprobado o en producción, operador habilitado | Iniciar, continuar o repetir la impresión según los trabajos reales |
| Impresión terminada, inspector habilitado | Control de calidad |
| Corrección solicitada | Solicitud y respuesta del cliente |
| Listo para entregar, personal de entregas | Siguiente movimiento físico, código y entrega |
| Entregado, responsable del cierre | Requisitos de conciliación y cierre |
| Cerrado, cancelado o rechazado | Historial conservado |

Los permisos provienen del contexto de operación autenticado. Las acciones efectivas siguen tomando sus autorizaciones y bloqueos del servidor; un perfil o un estado por sí solos no habilitan revisar, imprimir, cobrar ni entregar. Las respuestas de otra versión del pedido no se usan para recomendar una acción disponible.

## Acciones y consulta

- Revisión muestra una decisión principal. Rechazar y cancelar quedan en “Otras decisiones sobre este pedido”, con sus motivos y confirmaciones existentes.
- El PDF confirmado puede consultarse en una ventana, sin salir del trabajo. El selector permite consultar todos los ítems y sus adicionales configurados.
- Producción conserva inicio manual, recurso, incidencias, reimpresión y checklist de calidad. Los problemas del trabajo se agrupan en un desplegable.
- Entrega muestra el estado logístico y el saldo antes de las acciones disponibles; conserva la validación del código, la recepción física y el cierre separados.
- Historia, notas, correcciones y cambio de fecha siguen accesibles en “Otras tareas de este pedido”. Caja está enlazada desde el importe para perfiles financieros.
- Los paneles permanecen montados al cambiar de herramienta para conservar borradores. Una operación en curso o de respuesta incierta impide cambiar de tarea hasta recuperarla; se conserva el mismo intento idempotente. Esto incluye las cargas de PDF y comprobantes.
- “Actualizar tareas” vuelve a consultar el pedido y sus vistas operativas. No reinicia los datos de prueba.

El menú del empleado utiliza enlaces de altura normal; ya no distribuye sus dos opciones ocupando toda la altura disponible. La navegación completa del propietario conserva su distribución.

## Verificación

- `pnpm typecheck` y `pnpm build`.
- `pnpm test:operation-task`: perfiles, revisión antes de aprobación, anticipo, producción, calidad, entrega, cierre, versiones antiguas y estados finales/desconocidos.
- Regresiones de seguimiento, recorrido del cliente y adicionales: `test:tracking`, `test:journey`, `test:print-options`.
- Comprobación visual sobre los componentes React reales con API simulada y datos ficticios: revisión seguida de aprobación, empleado sin permiso, producción, checklist de calidad, conservación de borrador al consultar PDF, caja y recuperación de respuesta incierta. La revisión visual final de entrega, cerrado y anchos móviles quedó pendiente porque la sesión del navegador dejó de responder. La lógica de entrega y cierre sí está cubierta por las pruebas automáticas.

No hay cambios de esquema ni de reglas de negocio. Esta mejora afecta la presentación del detalle operativo y la navegación; las pruebas visuales simuladas no sustituyen una prueba completa con los usuarios y la base del entorno Docker.
