# Confirmación breve de dinero recibido

En el panel de pagos, **Registrar transferencia recibida** y **Registrar efectivo** abren una ventana con el importe editable. Se propone la seña o el pago previo que falte, y después el saldo pendiente. Si el medio no admite la seña, se propone el saldo posterior. Al verificar un aviso del cliente se propone su importe declarado, sin confundirlo con dinero ya recibido.

Referencia bancaria/recibo, fecha del movimiento y observaciones quedan en un desplegable opcional. **Confirmar recepción** es la declaración del responsable de haber comprobado el dinero en banco, billetera o caja. El servidor conserva el permiso financiero y exige esa confirmación. Si no se indicó una fecha, utiliza su reloj; el evento siempre conserva además la fecha de registro del servidor y el usuario autenticado.

Sin referencia externa se genera `Registro interno <operacion>`: identifica un asiento propio, nunca una operación bancaria inventada. Si se aporta una referencia externa se conserva su control de duplicados. La operación estable evita duplicar el mismo envío o su recuperación; dos registros nuevos sin referencia externa no pueden conciliarse automáticamente con un movimiento bancario. No hay conexión automática a bancos o billeteras.

## Recepción y descuento del saldo

La pantalla envía `aplicarAlPedido: true` a la recepción existente. En la misma transacción y bajo sus bloqueos se guardan dos hechos distintos: RECIBIR y, si corresponde, APLICAR. La segunda operación tiene un UUID determinista derivado de la primera. Una recuperación retorna el resultado guardado sin repetir ninguno de los hechos.

La aplicación utiliza el máximo permitido por el servidor: dinero disponible, saldo pendiente, acceso, vigencia y medios admitidos para las condiciones ofrecidas y las vigentes/confirmadas. No supera el saldo. El excedente queda disponible. Un cobro tardío o de un pedido terminado se conserva aunque no admita aplicación. Las excepciones comerciales del plazo siguen requiriendo la resolución correspondiente. No se inicia producción con este botón.

Las llamadas anteriores sin `aplicarAlPedido: true` conservan el comportamiento de registrar recepción solamente. Aplicaciones manuales a sucesoras, devoluciones y descartes conservan sus controles y motivos obligatorios.

## Historial

El historial tiene acceso propio desde el panel, separado del desplegable de comprobantes. Muestra cada aviso, recepción, aplicación, descarte y devolución con nombres legibles, usuario/rol, fecha de registro e importe/medio/referencia cuando corresponden. Para recepción y devolución también muestra la fecha del movimiento. Consulta los hechos existentes, sin reescribir la historia ni agregar migraciones. El aviso del cliente sigue identificado como informado; no acredita dinero por sí mismo.

## Validación

- `pnpm typecheck`, `pnpm build`, `pnpm test:payment-receipt` y `pnpm test:operation-task`.
- Ocho pruebas de `PagoSimpleServiceTest`: recepción/aplicación, responsable, referencia y fecha automática, recuperación, contenido diferente con la misma operación, excedente, medio no admitido para seña, vencimiento, contrato anterior, referencia duplicada y confirmación/permisos. Usa un doble de JDBC; no sustituye la comprobación de transacciones reales.
- Se agregó `DatosPruebaIntegrationTest#cobroSimpleConservaAuditoriaYNoDuplicaUnaConfirmacion` para probar consultas SQL, auditoría, recuperación y rollback conjunto ante un fallo de aplicación con PostgreSQL y datos precargados. Compiló; su ejecución queda pendiente en un entorno con PostgreSQL embebido disponible.
- Navegador: componentes reales con API simulada para comprobar importe editable con coma, actualización del saldo, historial legible, monto de aviso informado y recuperación incierta. Esta prueba visual no reemplaza la integración con PostgreSQL.

```bash
cd backend
mvn -Dtest=PagoSimpleServiceTest,DatosPruebaIntegrationTest#cobroSimpleConservaAuditoriaYNoDuplicaUnaConfirmacion test
```
