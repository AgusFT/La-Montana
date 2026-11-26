# Finanzas de puntos de entrega

## Operación en el mostrador y Android

En **Para retirar**, abrí el pedido. Para recibir efectivo, usá **Cobrar y entregar** con el código del cliente. Para una transferencia, elegí la cuenta que indica el backend y registrá la referencia bancaria. La ficha identifica el origen por la cotización vigente del pedido. Un punto compartido puede ofrecer distintos destinatarios según el pedido.

En **Finanzas → Cobros**, elegí Hoy, Ayer o Últimos 7 días. Los períodos usan el calendario de Buenos Aires. El total gestionado incluye efectivo, transferencias al punto y transferencias directas a origen que ya se acreditaron. El pendiente de los pedidos disponibles es el saldo que todavía deben los clientes. Abrí el movimiento para ver su referencia y destinatario. En un aviso pendiente podés adjuntar el comprobante PDF. Quien tiene acceso a la cuenta verifica la acreditación y aplica el dinero. Después se puede entregar con código si el pedido reúne los requisitos.

En **Finanzas → Rendiciones** se ve la obligación acumulada, el importe informado pendiente de confirmación y el disponible para una nueva rendición. Usá **Informar rendición** después de entregar o transferir el dinero. Elegí un destinatario y medio habilitados, ingresá el importe y consigná la persona receptora del efectivo o la referencia bancaria. Podés incorporar una observación y un comprobante de hasta 2 MiB.

## Caja de la imprenta

En **Administración → Caja → Finanzas de puntos de entrega**, el resumen conserva los saldos acumulados al filtrar el historial. Se puede buscar por punto, sucursal, fechas, medio, destino, estado y responsable. El detalle de un cobro abre los pagos y comprobantes del pedido. Un cobro histórico de destinatario incierto admite una revisión fundamentada por Administración.

En **Autorizar cuentas y destinatarios**, seleccioná el punto, la operación, la sucursal cuando corresponda y el medio. Para una transferencia cargá instrucciones que permitan identificar su cuenta. Habilitá expresamente a quienes puedan verificarla. La versión de instrucciones presentada al informar se conserva con el aviso. Una transferencia a la sucursal de origen se acredita desde los pagos de esa sucursal.

En la caja de la sucursal destinataria, **Rendiciones pendientes de recepción** permite consultar el importe, referencia, persona receptora y adjunto. Confirmar registra un movimiento de caja denominado recepción de rendición de punto. Rechazar exige motivo y lo muestra al punto. Para pagar por servicios, usá **Registrar un pago → Pago por servicios de un punto**, seleccioná el beneficiario y registrá la transferencia realizada. Caja conserva el egreso, su referencia y su comprobante.

| Operación | Efecto sobre el pedido | Efecto sobre la obligación del punto |
|---|---|---|
| Cobro acreditado en el punto | Aplica el dinero permitido al pedido | Aumenta por el importe recibido |
| Transferencia acreditada en origen | Aplica el dinero permitido al pedido | Ninguno |
| Aviso pendiente | Pendiente de verificación | Ninguno |
| Devolución realizada con fondos del punto | Reutiliza las reglas de devolución de Pagos | Disminuye por el importe devuelto |
| Rendición informada | Ninguno | Reserva el importe |
| Rendición rechazada | Ninguno | Libera la reserva |
| Rendición confirmada | Ninguno | Reduce la deuda y libera la reserva |
| Transferencia por servicios al punto | Ninguno | Ninguno |

## Contratos y trazabilidad

| Ruta | Uso |
|---|---|
| GET /api/punto/finanzas?periodo=HOY&pagina=0 | Cobros propios. También admite AYER y SEMANA |
| POST /api/punto/pedidos/{pedido}/avisos | Operación UUID, versión de pedido, UUID de cuenta autorizada, importe y referencia |
| POST /api/punto/avisos/{aviso}/comprobante | PDF privado ligado al intento de pago. Reutiliza el servicio de comprobantes |
| POST /api/punto/finanzas/verificar | Operación UUID, cotización y aviso. Verificación de cuenta propia autorizada |
| GET, POST /api/punto/rendiciones | Consulta del saldo global propio, últimos 25 informes y destinos habilitados. Alta con UUID de operación, cuenta e importe |
| GET /api/admin/finanzas-puntos | Saldos acumulados por punto |
| GET /api/admin/finanzas-puntos/historial | Historial filtrable, paginado de a 50 |
| GET, POST /api/admin/finanzas-puntos/configuracion | Autorizaciones versionadas. Para reemplazar se envía la versión anterior |
| POST /api/admin/finanzas-puntos/revisiones | Revisión documentada del destinatario de un pago histórico |
| GET /api/operacion/sucursales/{sucursal}/rendiciones-puntos | Bandeja de la sucursal receptora |
| POST /api/operacion/sucursales/{sucursal}/rendiciones-puntos/{rendicion} | Confirma o rechaza una sola vez |
| POST /api/operacion/sucursales/{sucursal}/rendiciones-puntos/informes/{punto} | Informa por un punto con acuerdo vigente en esa sucursal |
| POST /api/operacion/sucursales/{sucursal}/caja/egresos | Tipo PAGO_PUNTO_ENTREGA, medio TRANSFERENCIA, UUID de punto, beneficiario, importe, concepto y referencia |

Flyway aplica **V64__finanzas_puntos.sql** después de V63. La migración agrega instrucciones versionadas, vínculos con avisos existentes, rendiciones, imputaciones, resoluciones y revisión histórica. Conserva los registros originales. Sólo los cobros con destino explícito generan obligación. Si falta el destino, se necesita evidencia y una revisión administrativa. La cuenta histórica del operador identifica quién gestionó el pago.

Los fondos se agrupan por punto en pesos. El backend vincula una rendición a los cobros disponibles por antigüedad y, ante igualdad de fecha, por evento. La sucursal que recibe el dinero es un dato de la rendición. No se atribuye la recaudación de un punto compartido a una única sucursal productiva. El saldo descuenta devoluciones con fondos del punto y rendiciones confirmadas. Las rendiciones pendientes reducen el disponible. La validación, reserva e imputación comparten el bloqueo transaccional del libro de pagos. Los reintentos conservan el UUID y los datos originales.

La cuenta restringida se identifica por sesión. Las fichas, avisos y comprobantes comprueban el alcance en backend. Para empleados se agregan los permisos INFORMAR_RENDICION_PUNTO, CONFIRMAR_RENDICION_PUNTO y PAGAR_PUNTO. Requieren asignación explícita y alcance de sucursal. Para pagar también se requiere acceso a la Caja de la sucursal. El autor de una rendición no puede resolverla.

## Preparar la prueba manual

En la copia de la rama pruebas, ejecutá `git pull --ff-only origin pruebas` y `bash infra/pruebas/iniciar.sh`. El script actualiza los servicios del proyecto de pruebas y conserva sus volúmenes. Para agregar ejemplos de una sola vez, usá `LAMONTANA_PRUEBAS_FINANZAS=true bash infra/pruebas/iniciar.sh` en ese entorno. Caballito recibe tres pedidos de ejemplo nuevos, un egreso por servicios y rendiciones pendiente, confirmada y rechazada. El marcador del escenario se guarda en la misma transacción que sus datos.

En la copia Android, ejecutá `git pull --ff-only origin android`. Abrí el directorio android en Android Studio y usá la variante debug con el emulador. Requiere el backend actualizado en el puerto 8180, accesible desde el emulador en 10.0.2.2. Para verificar la compilación local con Gradle 9.6.1, ejecutá `gradle :app:assembleDebug :app:testDebugUnitTest` desde android.

1. Abrí el mismo punto en Mobile y Web. Registrá efectivo y un aviso por cada cuenta habilitada. Antes de acreditar, compará el pendiente de los pedidos y la obligación del punto.
2. Acreditá cada transferencia con acceso a la cuenta correcta. Verificá que sólo la propia aumente la obligación y que el pedido se pueda entregar con código.
3. Informá una rendición parcial desde Mobile. Rechazala desde Caja y revisá el motivo y la reserva. Informá otra, confirmala en Web y actualizá Rendiciones en el teléfono.
4. Registrá un pago por servicios desde Caja. Compará el saldo acumulado del punto antes y después. Repetí una operación con respuesta perdida mediante **Recuperar registro**.
