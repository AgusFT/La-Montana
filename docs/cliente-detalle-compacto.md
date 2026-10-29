# Cliente: pedido actual en una sola pantalla

Actualización del 29/10/2026 en `pruebas`. Retoma el concepto de MC-CLI-006: seguimiento, datos claros y una acción de cancelación visible.

## Presentación

- El seguimiento queda abierto. Las tres etapas de creación se mantienen en una línea discreta; las fechas provienen del historial real y las etapas pendientes no tienen fechas inventadas.
- Se elimina del cliente la fila de pestañas Resumen, Correcciones, Entrega y cierre, Reprogramación, Archivos PDF y Pagos.
- Las tarjetas muestran **Detalles del trabajo**, **Archivo**, **Información del pedido**, **Método de pago** y **Lugar de retiro / Punto de entrega / Dirección de entrega** según su modalidad.
- Si hay varios PDF, un selector permite consultar uno por vez. Las opciones de impresión, nombre, tamaño y descarga corresponden al archivo seleccionado. Veinte PDF no generan veinte tarjetas abiertas ni un scroll dentro de la ficha.
- La descarga usa el original confirmado y sólo se ofrece cuando los metadatos autorizados indican disponibilidad. El visor abre la primera página del archivo elegido en una ventana con cierre por botón/Escape y retorno del foco.
- El precio total, la franja guardada, el destino y el saldo siguen accesibles. Los movimientos y formularios de pago se despliegan dentro de su tarjeta y conservan su borrador al cerrar y volver a abrir.
- Las correcciones pendientes aparecen directamente en la página. Las anteriores quedan plegadas. Las propuestas de fecha aparecen junto al destino cuando existen; se conservan sus controles de aceptación, rechazo y cupo.
- El código de retiro utiliza el componente real de entrega dentro de Información del pedido. Se muestra cuando el estado permite generarlo, con su vencimiento y renovación; se informa la recepción cuando fue registrada.
- **Cancelar pedido** queda al pie, mientras su estado lo permite. Solicita una confirmación explícita y ofrece un motivo opcional. Si se omite, la auditoría registra «Cancelación solicitada por el cliente». No se realiza una devolución automática y se conserva la recuperación de operaciones inciertas.
- El historial completo y las condiciones permanecen en un único desplegable secundario. Se mantienen los enlaces contextuales `#pedido-*`.

## Alcance

Cambian la composición y la presentación del cliente. Se reutilizan los componentes y endpoints de pagos, archivos, cancelación, correcciones, reprogramación y entrega. No se modifica el backend, el esquema, los precios, permisos ni los estados. Las etapas 1 y 2 conservan su presentación. La vista interna mantiene sus pestañas, formularios y decisiones; las variantes compactas/contextuales son optativas.

## Verificación

- `pnpm typecheck`, build de producción y 41 pruebas existentes de seguimiento/recorrido cliente aprobadas.
- Navegador con los componentes React reales compilados en un banco aislado y **API simulada**: cinco tarjetas, ausencia de pestañas del cliente, selector entre veinte PDF, descarga no ofrecida para contenido revocado, error de saldo sin dinero inventado, apertura directa del visor y cierre por Escape con devolución del foco.
- Comprobados los anchos de celular/tableta de 320, 390 y 768 px, y el escritorio disponible, sin desborde horizontal del documento. El timeline conserva su desplazamiento propio.
- Comprobadas en ese banco la aparición de correcciones y propuestas de fecha, la presentación del código ficticio de retiro, la ausencia de cancelación cuando está listo para entregar, la conservación de borradores al plegar pagos y la recuperación de una cancelación con respuesta incierta.
- El banco de interfaz no certifica una integración contra Docker/PostgreSQL/ClamAV. Los originales, movimientos financieros, códigos y decisiones de una instalación siguen bajo los controles reales del servidor.

## Actualizar la instalación

```bash
git pull --no-rebase --no-edit origin pruebas && ./infra/pruebas/iniciar.sh
```

Se conservan los datos actuales. El merge permite mantener también los commits locales del equipo.
