# Cobro de efectivo al entregar y botón de regreso

En un pedido disponible para recibir, con medio elegido EFECTIVO y saldo pendiente, la tarea propone **Cobrar efectivo y entregar**. El botón muestra el saldo exacto, por ejemplo **Cobrar $155 y entregar**. La ventana muestra el importe completo, pide el código y deja receptor/observación como datos opcionales. Para pagos parciales, cambios de medio u otros movimientos se conserva Caja.

Una confirmación registra efectivo recibido, aplicación al pedido y entrega física. Si el usuario tiene CERRAR_PEDIDOS y no quedan informes o movimientos por resolver, también registra el cierre. Si no corresponde cerrar, se informa que el cierre administrativo continúa pendiente. No se concede un permiso por acceder a esta pantalla.

## Registro interno

El panel conserva un **Comprobante interno de cobro** con importe, medio efectivo, referencia, responsable y fecha/hora. Se obtiene del pago y del evento financiero persistidos, vinculados a la cotización del pedido. El historial de caja conserva RECIBIR y APLICAR; el pedido conserva ENTREGAR y, cuando corresponde, CERRAR. La pantalla permanece en el comprobante después de confirmar.

## Validaciones y atomicidad

POST `/api/operacion/pedidos/{pedido}/entrega/cobrar-efectivo` recibe operación, versión, código, importe esperado y confirmación. Revalida permisos de entrega/cobro y de cierre si se solicita, disponibilidad física, saldo exacto y medios de las condiciones aceptadas/históricas. Si hay dinero recibido sin aplicar, remite a Caja para evitar volver a cobrarlo.

Primero comprueba el código. Un error registra un único intento y no cobra. Una confirmación válida reutiliza el servicio financiero existente dentro de la misma transacción; si falla aplicación, entrega o cierre se revierte el conjunto. Recuperar la misma operación no vuelve a cobrar. Reutilizar su identificador con otros datos se rechaza.

V50 permite validar una entrega cuyo estado final ya sea CERRADO en esa misma transacción. Conserva las comprobaciones de calidad, saldo, recorrido, reserva y código. Vincula la versión validada con el evento de entrega y comprueba la versión final correspondiente al cierre posterior. No desactiva restricciones ni modifica migraciones anteriores.

## Regreso a pedidos

`BackToOrders` unifica el enlace de regreso en las vistas de personal, administración, cliente y estados de carga/error. Presenta flecha, borde, fondo, área de al menos 44 px y foco de teclado. Conserva el destino y los bloqueos de navegación existentes.

## Verificación

TypeScript, build de Next.js y 61 pruebas de lógica de tareas, entrega, seguimiento, estados y sugerencia de cobro. Ocho pruebas del servicio de entrega y tres integrales sobre PostgreSQL 18.6 con las 50 migraciones: cobro de $155 hasta cierre, ausencia/reintento y despacho conjunto. El caso de efectivo verifica código incorrecto, recuperación del mismo intento, saldo cambiado, permisos, rollback de cobro ante una falla de entrega, conciliación, comprobante persistente y rechazo de reutilización con otros datos. En navegador se verificó el workspace real con API simulada: importe completo, error/corrección del código, saldo cero, cierre y comprobante visible, además del botón de regreso.
