# Entrega a domicilio e intento sin receptor

El panel diferencia envío a domicilio, traslado a un punto/otra sucursal y retiro local. Un envío a domicilio en viaje propone **Ingresar código y entregar**, no registrar la llegada a una sucursal. El destino y el contacto están visibles; el teléfono permite iniciar una llamada desde un dispositivo compatible. La confirmación conserva el circuito existente de código, calidad, saldo, entrega y cierre.

El ingreso del código tiene foco inicial, letras grandes y un solo campo. Acepta texto en minúsculas o separado por espacios/guiones y lo normaliza antes de comprobarlo. Nombre de quien recibe y observación comparten una sección opcional. Un código incorrecto mantiene el formulario abierto; no registra una entrega.

## Ausencia y reintento

**El cliente no está** registra un intento a domicilio sin receptor. La confirmación no requiere observaciones ni casillas adicionales. Guarda usuario, fecha y hora en el historial inmutable del pedido y en el movimiento logístico. El cliente ve que la entrega sigue pendiente y conserva su código.

| Hecho | Origen | Destino logístico |
|---|---|---|
| CLIENTE_AUSENTE | ENVIO_DOMICILIO / EN_VIAJE | CLIENTE_AUSENTE |
| REINTENTAR_ENTREGA | ENVIO_DOMICILIO / CLIENTE_AUSENTE | EN_VIAJE |

El estado general sigue siendo LISTO_PARA_ENTREGA. No se consume ni rota el código, no se cierra el pedido, no se modifica el dinero ni la reserva y no se presume un retorno físico a la imprenta. Después de coordinar con el cliente, **Iniciar nuevo intento** confirma una nueva salida hacia el mismo domicilio. No asigna por sí mismo otra fecha, dirección o costo: esos cambios comerciales quedan fuera de esta acción.

La bandeja muestra la ausencia en amarillo, con su fecha de estado y último movimiento. Ese pedido no se incluye en preparaciones/despachos conjuntos como si fuera un pedido sin despachar. La entrega con código queda bloqueada mientras figure CLIENTE_AUSENTE; vuelve a habilitarse al iniciar el nuevo intento y sigue exigiendo saldo cubierto. Puntos de retiro y sucursales conservan el registro de llegada antes de entregar.

## Implementación y validación

La migración V49 amplía las acciones auditadas y el movimiento logístico con validación de modalidad y recorrido en PostgreSQL. Conserva la historia existente. No se editan migraciones anteriores ni se reinician datos. El servicio mantiene permisos, versión de pedido, confirmación e idempotencia.

Se validaron TypeScript, build de Next.js, 54 pruebas de interfaz/lógica y 8 pruebas del servicio. La prueba integral `ausenciaDomiciliariaConservaReservaCodigoYPermiteReintentoAuditable` ejecutó las 49 migraciones sobre PostgreSQL 18.6 y comprobó ausencia, consulta del código por el cliente, recuperación sin duplicados, bloqueo de entrega durante la ausencia, último movimiento en la bandeja, reintento, entrega y cierre. En navegador se probó el componente real con API simulada, incluido el error de código y su corrección sin completar datos opcionales.

También pasó la prueba integral de bandeja y despacho conjunto. Se normalizó la fecha del resultado de lote a microsegundos, la precisión persistida por PostgreSQL, para que la recuperación idempotente devuelva exactamente la misma fecha.

La prueba heredada `ActivacionConfiguracionIntegrationTest.entregasTresModalidadesCodigoCierreYReinicio` falla con HTTP 400 al crear su cotización, antes de ejecutar entregas. Se reprodujo exactamente el mismo fallo sobre el código anterior al cambio (HEAD 588a358), usando en ambas corridas únicamente TCP para PostgreSQL embebido por la limitación de sockets Unix del entorno. Ese caso heredado queda pendiente de actualizar; no se cambiaron las cotizaciones ni se presenta como prueba aprobada.

## Proyección mobile

La futura experiencia del repartidor conserva este mismo contrato: cuenta individual, recorrido asignado, entrega domiciliaria con código y acción breve **Cliente ausente**, sin observación ni fotografía obligatorias. La recepción de lotes en puntos sin PC y la custodia durante el recorrido se detallan en [Android Mobile: alcance y flujos de puntos de retiro y reparto](mobile-android-alcance-y-flujos.md).
