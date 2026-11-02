# Pedidos con varios PDF: producción visible por trabajo

El detalle del personal muestra todos los PDF del pedido en tarjetas visibles, en lugar de esconderlos dentro de un selector. Cada tarjeta conserva su número original, nombre, páginas, copias y situación propia: falta imprimir, impresión iniciada, calidad pendiente, incidencia/reimpresión o calidad aprobada.

El resumen indica cuántos trabajos tienen calidad aprobada y cuántos faltan. Un acceso directo lleva a producción. Seleccionar una tarjeta cambia los detalles y el documento consultado; al guardar producción se vuelve a destacar un trabajo pendiente.

En el área de producción, los pendientes aparecen antes que los aprobados. Los aprobados quedan en una fila verde plegada con archivo, inspector y fecha; su registro completo sigue disponible. Los pendientes muestran sus opciones y su acción individual. La numeración de los trabajos no cambia al ordenar la lista.

Los contadores usan el último intento del ítem y el PDF vigente. Una aprobación antigua, un intento posterior pendiente, un archivo reemplazado o una respuesta de otra versión no completan el avance visual. Se conserva la validación del servidor: la calidad de un PDF no aprueba los demás ni habilita el despacho del pedido incompleto.

Verificación: TypeScript, build de Next.js y 24 pruebas de tareas, guía de calidad y avance por PDF. En navegador se probó el workspace real con una API simulada: primer PDF aprobado/segundo pendiente, inicio del segundo, control de su calidad y avance a entrega al quedar ambos aprobados. Esta tarea sólo modifica presentación; no cambia API, cobros ni transiciones del servidor.
