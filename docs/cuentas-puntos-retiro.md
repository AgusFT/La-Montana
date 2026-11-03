# Cuentas y operación de puntos de retiro

Cada punto tiene una única cuenta, vinculada permanentemente a ese punto y con un solo responsable. El administrador la crea desde **Puntos de entrega → Cuentas de los puntos de retiro**, indicando usuario, correo del responsable, contraseña inicial y si puede registrar cobros. Puede cambiar el responsable, suspender/reactivar la cuenta o deshabilitar sus cobros; estos cambios invalidan sus sesiones anteriores. Las altas normales exigen cambiar la contraseña inicial. No se crea la cuenta mediante registro público de clientes.

La cuenta ingresa por **Usuario o correo** y abre `/punto-retiro`. Tiene tres bandejas: **Por llegar**, **Para retirar** y **Entregados**, más **Finanzas del punto**. Sólo aparecen pedidos cuyo destino es ese punto; no accede a producción, configuración, clientes de otros destinos ni descarga de PDF. El servidor comprueba el alcance en cada consulta y operación, además de las restricciones de pantalla.

La misma cuenta se utilizará en PC, tablet o teléfono. Para kioscos sin computadora, la vista responsive es la primera solución operativa y la futura aplicación Android reutilizará exactamente el mismo alcance: no se crea otra cuenta mobile ni se replica la lógica de negocio. El flujo conjunto con el repartidor se define en [Android Mobile: alcance y flujos de puntos de retiro y reparto](mobile-android-alcance-y-flujos.md).

## Recepción y entrega

- **Por llegar** permite marcar hasta 25 pedidos de la página y registrar juntos su recepción física. La operación es atómica: un pedido inválido revierte el lote completo. Sólo recibe pedidos enviados a ese punto y en viaje.
- **Para retirar** busca por nombre, correo, número de pedido o código de entrega. El código se envía en el cuerpo de la consulta, no en la URL, y no se revela en las respuestas. El detalle muestra al cliente, todos los trabajos del pedido, el saldo y el historial.
- Si el saldo está pagado, se ingresa el código y se confirma la entrega. No se exige teléfono, nombre del receptor ni observación. Si corresponde efectivo, **Cobrar [saldo] y entregar** usa el importe completo y el código en una sola acción; un código incorrecto no cobra nada.
- **Registrar efectivo o transferencia** trae el saldo autocompletado, admite un importe parcial positivo y una referencia opcional. Transferencia significa acreditación ya verificada por el responsable con permiso de cobro. No equivale al aviso de pago del cliente.
- Se reutilizan las reglas vigentes de pagos, códigos, disponibilidad, calidad y conciliación. El pedido sólo se entrega si los controles habilitan la entrega y se cierra cuando la conciliación está completa. Los reintentos conservan la operación original para evitar duplicar dinero o recepciones.

## Finanzas

El período inicial comprende los últimos seis meses; permite fechas y paginación. Muestra el pendiente de los pedidos actualmente disponibles, el efectivo recibido por esa cuenta y las transferencias verificadas por esa cuenta en el período. El historial incluye recepción, aplicación y devolución de pagos de pedidos de ese punto, incluso anticipos de una cotización anterior vinculada. Distingue **Cuenta del punto** de **Administración / caja**: una seña recibida antes en la imprenta no se suma al dinero recibido por el punto.

Cada movimiento conserva pedido, medio, importe, referencia, usuario responsable y fecha/hora. Los totales de recepción son importes brutos registrados; no representan una rendición de efectivo a la imprenta, una comisión del punto ni un cierre de caja. Las devoluciones se muestran como movimientos separados. No se agregaron integraciones bancarias ni telemetría.

## PRUEBAS

| Punto | Usuario de acceso | Correo |
|---|---|---|
| Caballito | `punto.caballito.retiro` | `punto.caballito.retiro@prueba.local` |
| Palermo | `punto.palermo.retiro` | `punto.palermo.retiro@prueba.local` |

Clave ficticia por defecto: `MontanaPrueba2026!`. Si se configuró `TEST_USER_PASSWORD`, las cuentas nuevas usan ese valor. Las cuentas de muestra entran directamente a la bandeja; la obligación de cambiar contraseña se mantiene para las creadas manualmente desde administración.

| Rider | Correo | Escenario |
|---|---|---|
| Rider Oeste · Martina Ruta | `rider.oeste@prueba.local` | Recorrido activo e historial de custodia |
| Rider Centro · Nicolás Reparto | `rider.centro@prueba.local` | Segundo recorrido aislado y destinos propios |

Los riders usan la misma clave ficticia. No acceden a la bandeja global: cada cuenta sólo consulta el recorrido asignado. La llegada al kiosco no reemplaza la confirmación de recepción realizada por la cuenta del punto.

Una instalación nueva crea once cuentas: administrador, producción, caja, cuatro clientes, dos puntos y dos riders. Conserva seis meses de recepciones, cobros, entregas y recorridos; agrega tres ejemplos actuales por punto y viajes activos visibles para los riders. Son datos ficticios.

Actualizar una base de escenario 2 o 3 incorpora puntos, riders y recorridos una sola vez, sin borrar pedidos ni reatribuir sus movimientos originales. El escenario pasa a versión 4; los siguientes reinicios no duplican identidades, recorridos ni lotes.

```bash
git switch pruebas
git pull --ff-only origin pruebas
./infra/pruebas/iniciar.sh
```

No ejecutar `reiniciar-datos.sh` para esta actualización del escenario de seis meses.

## Implementación y validación

V51 agrega alias de acceso, vínculo único punto/usuario, auditoría de administración de cuentas y recepción de lotes. Mantiene los roles existentes: una identidad `EMPLEADO` marcada como cuenta de punto tiene acceso exclusivo a `/api/punto/**` y a su propia seguridad de cuenta. Se excluye de la gestión general de empleados. Los servicios comprueban también el destino para reutilizar entrega/pagos sin ampliar el alcance a otra sucursal o punto.

Se comprobaron con PostgreSQL 18.6 y las 51 migraciones los accesos por alias, aislamiento HTTP y de servicios, códigos, recepción conjunta/reintento, cobro en efectivo con entrega/cierre, transferencia y su reintento, histórico de seis meses, suspensión de sesión, alta administrativa única, cambio inicial de contraseña y actualización/reinicio sin reescritura del historial. Pasaron las diez pruebas de `DatosPruebaIntegrationTest`. Las 109 pruebas unitarias restantes también aprobaron; la prueba de migración de horarios anterior a este cambio no pudo iniciarse en este entorno por la restricción de sockets Unix. La migración V51 sí se ejecutó en las pruebas integrales mediante TCP.

Frontend: build de producción y tipado correctos, 118 casos existentes y cuatro casos nuevos de proxy/acceso (período y página de finanzas, sesión/CSRF/operación, rutas/métodos cerrados y destino de ingreso). Revisión visual de los componentes reales con API simulada: bandeja, resumen financiero, entrega/cobro y administración de cuentas. La revisión visual no sustituye una prueba de navegador contra la instalación Docker del usuario.
