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

## Casos de uso y evidencia de prueba

| Caso | Actor y condición inicial | Resultado comprobable |
|---|---|---|
| Cobrar efectivo al retirar | Responsable con cobros habilitados, pedido disponible y código válido | El pago se aplica al pedido y aumenta los fondos del punto. El reintento conserva un solo pago. |
| Informar y acreditar una transferencia | Responsable del punto elige una cuenta autorizada para el pedido. Un verificador autorizado comprueba el ingreso en esa cuenta. | El aviso conserva la referencia. La acreditación en origen cubre el pedido y la acreditación propia también aumenta la obligación del punto. |
| Rendir una parte o la totalidad | El punto o un responsable autorizado dispone de fondos y un destinatario habilitado | La operación reserva el importe. Si dos informes distintos compiten por todo el disponible, sólo uno se acepta. |
| Resolver una rendición | Un usuario autorizado del destinatario comprueba la recepción | La confirmación reduce la obligación una vez. El rechazo libera la reserva y conserva el motivo. |
| Devolver fondos o pagar servicios | Un usuario autorizado documenta qué fondos entregó o desde qué sucursal hizo el pago | La devolución con fondos del punto disminuye su obligación. El pago de servicios registra un egreso bancario independiente. |

Las siguientes pruebas de integración usan PostgreSQL real, sesiones HTTP y el perfil de pruebas con una base temporal. Se pueden repetir desde `backend` con Maven:

```sh
mvn '-Dtest=DatosPruebaIntegrationTest#puntoCobraConMedioRealYSeparaElDestinoDeLaTransferencia+escenarioFinancieroOpcionalConservaFondosAlReiniciar+devolucionesYReservasConcurrentesRespetanAlPagadorReal+cajaComparteSaldoPorSucursalYAislaMovimientosComprobantesYArqueos' test
```

La prueba de devolución compara dos reembolsos del mismo pedido, uno pagado por la imprenta y otro por el punto. La prueba de concurrencia envía dos informes distintos por el total disponible y comprueba una respuesta 204, otra 409 y una sola reserva. La prueba de reinicio conserva las cantidades de pedidos y rendiciones y comprueba que Palermo continúa sin deuda en el escenario de ejemplo.

También se ejecutó el cliente de datos Android en JVM contra el backend y PostgreSQL, con la administración Web abierta en Chromium. En ese recorrido el cliente informó $1,25 y reenvió el mismo comando. Caja mostró una rendición, la confirmó y el cliente volvió a consultar la deuda reducida en $1,25. Otro informe de $2,50 realizado desde el mostrador Web siguió reservado. La validación de navegación, teclado, adjuntos y cortes de conexión desde el emulador forma parte de la prueba manual.

## Medios del retiro y acuerdos del punto

El medio elegido al hacer el pedido no impide usar otro medio admitido por la oferta para cubrir el saldo. En el punto, las transferencias requieren además una cuenta de cobro activa, autorizada por Administración para ese punto o para la sucursal de origen de la cotización. El operador del punto no puede elegir un destinatario arbitrario ni acreditar un ingreso a una cuenta ajena. En **Administración → Caja → Finanzas de puntos de entrega → Autorizar cuentas y destinatarios**, configurá los usos **COBRO_PUNTO** y/o **COBRO_ORIGEN** para habilitar la transferencia. Usá instrucciones bancarias verificables del destinatario real. Mientras no exista esa autorización, Mobile muestra por qué sólo puede ofrecer efectivo.

Si ya hay una transferencia informada y pendiente, el punto espera a que el destinatario verifique o descarte ese aviso antes de registrar otro cobro. El estado se muestra en la ficha del pedido. Para rendir, Administración debe habilitar un acuerdo **RENDICION** para el punto, la sucursal receptora y el medio. El punto informa importe, destinatario y referencia o persona receptora. La sucursal confirma o rechaza posteriormente. Un punto que trabaja con varias sucursales puede rendir fondos agrupados al destinatario acordado, sin asignar artificialmente cada cobro a una sucursal de producción.

En Mobile, **Caja** de sucursal admite Hoy, Ayer y últimos 7 días dentro de la misma sucursal autorizada. Muestra el efectivo al cierre de ayer cuando se elige Ayer y el efectivo actual en los demás períodos. Entradas, salidas, transferencias y movimientos corresponden al período seleccionado. La vista Web conserva el historial y los arqueos más amplios.
