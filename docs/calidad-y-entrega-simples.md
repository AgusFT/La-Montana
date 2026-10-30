# Calidad y entrega con confirmaciones simples

Decisión operativa del 30/10/2026. Complementa y reemplaza el recorrido de casillas individuales, código generado por el cliente y validación separada descrito en documentos anteriores.

## Verificar el trabajo físico

La guía muestra lo contratado: páginas y hojas por ejemplar, cantidad de copias, total de hojas impresas, formato, papel, color, caras y **todos** los adicionales solicitados. Los nombres, cantidades y unidades de los adicionales salen de las líneas de la oferta aceptada, no de una lista fija de anillado/abrochado. Un adicional sin identificar no se oculta: se impide aprobar hasta recuperar sus datos.

Al lado aparecen los criterios de calidad: impresión nítida y uniforme, sin manchas ni cortes, hojas completas/alineadas/en orden y terminaciones prolijas. El inspector compara el trabajo físico con esa guía y pulsa **Está correcto · Aprobar**. No marca casillas ni escribe una nota para la acción normal. **Hay un problema** permite indicar una categoría y devolver a producción o pedir reimpresión; observación libre opcional.

POST `/api/operacion/pedidos/{pedido}/produccion/trabajos/{trabajo}/verificar` exige permiso de calidad, confirmación explícita al pulsar el botón, versión vigente, pertenencia del trabajo y ausencia de inspección anterior. Si la impresión seguía en curso exige también permiso de producción y guarda fin e inspección en una transacción. No aprueba automáticamente otros PDF ni entrega el pedido.

V46 conserva los controles anteriores como CHECKLIST. La confirmación nueva se identifica como CONFORMIDAD_GLOBAL y guarda una copia inmutable de los requisitos, construida por el servidor desde el pedido efectivo. Las columnas históricas de controles resumen esa conformidad, sin presentar cinco casillas como si el operador las hubiese marcado individualmente. La historia muestra modalidad, responsable, fecha y guía conservada.

## Movimientos y notas

Preparar, salir, llegar, entregar y cerrar tienen un botón de confirmación con su nombre. El destino aparece antes de guardar. No requieren otra casilla ni un motivo escrito. Las observaciones están plegadas y son opcionales. Los errores/cancelaciones de impresión también admiten una descripción automática del hecho cuando no hay nota libre.

Las observaciones internas duplicadas en solicitudes de corrección y propuestas/retiros de reprogramación son opcionales. Se conserva el mensaje destinado al cliente cuando necesita saber qué corregir o qué nueva fecha se propone. Una nota interna redactada intencionalmente sigue necesitando contenido.

El servidor registra usuario/rol, fecha/hora, acción, versiones y estados. Las descripciones predeterminadas son textos del sistema; no se presentan como una observación redactada por el empleado.

## Código automático y recepción

| Hecho | Código y próximo paso |
| --- | --- |
| Calidad completa, retiro en casa matriz sin traslado | Código automático y retiro habilitado. |
| Pedido preparado para trasladar | Todavía no se emite el código. |
| Salida de la imprenta al destino | Se emite en la misma transacción y se agenda el aviso por correo. |
| Pedido en viaje | El cliente ya ve el código en su cuenta y puede anotarlo. |
| Llegada a otra sucursal/punto | Se conserva el mismo código; se habilita el retiro. |
| Presentación del código | Una sola acción confirma código y entrega física, con saldo cubierto. |
| Entrega registrada | Código consumido y paso Entrega completado; falta el cierre administrativo. |

Que el pedido esté **Disponible para retirar** mantiene activa Entrega: todavía no lo recibió una persona. Sólo la entrega efectiva avanza a Cierre. El historial distingue preparado, en viaje y disponible, en vez de repetir el estado general.

POST `/api/operacion/pedidos/{pedido}/entrega/recibir` recibe código y confirmación. Nombre del receptor y nota son opcionales; si no se informa nombre registra “Persona que presentó el código de entrega”, sin inventar su identidad. No se exige celular ni sesión del cliente en el mostrador. El servidor comprueba el código dentro de esa misma operación y conserva la validación técnica auditada; no hay una pantalla previa para validarlo.

Se mantienen permisos por pedido, versión, calidad, recorrido físico, saldo/anticipos y CSRF. Código incorrecto no entrega y descuenta un intento; recuperar el mismo intento no vuelve a descontarlo. Cinco errores bloquean el código y requieren asistencia de la imprenta. El código no paga ni entrega por sí solo. El cierre sigue comprobando la conciliación.

## Persistencia, correo y compatibilidad

V47 guarda el código automático cifrado con AES-GCM, asociado a su identidad, además del hash de comprobación. Sólo la vista autorizada del cliente devuelve su texto; la vista interna no lo devuelve. No se guarda en localStorage. Ya no tiene vencimiento operativo de treinta minutos: se conserva hasta la entrega o revocación al cambiar la reserva. El endpoint anterior de emisión devuelve el código automático vigente, sin rotarlo por cada consulta.

La clave `codigo-retiro.key` queda en el directorio privado configurado para archivos, dentro del volumen persistente existente. **Los respaldos deben conservar ese archivo junto con la base y los PDF.** No se incluye ninguna clave en el repositorio. Las respuestas del proxy conservan `Cache-Control: no-store`.

El aviso al correo registrado se agenda en la base en la transacción que emite el código. Un trabajador lo envía por el SMTP configurado y reintenta a los cinco minutos si falla. Un fallo de correo no revierte un despacho ya confirmado ni genera otro código. Un reinicio entre aceptación SMTP y confirmación en base puede repetir el aviso con el mismo código; no se promete entrega de correo exactamente una vez. En pruebas el correo se captura en Mailpit, `http://localhost:8125`.

Los pedidos que ya estaban en viaje, disponibles o listos en casa matriz recuperan el nuevo código al consultar su entrega, usando el responsable conservado de despacho/calidad. No se reinicia el escenario ni se reescriben pedidos históricos entregados/cerrados.

## Validación

- TypeScript y build Next.js aprobados; 24 pruebas web de guía de calidad, tarea operativa e impresoras.
- 42 pruebas backend aprobadas: calidad global/conjunta, código persistente y aviso, recepción directa con código, papel/selección y cadena de seguridad. Los servicios usan dobles JDBC; no prueban por sí solos los triggers ni el rollback real de PostgreSQL.
- Se actualizaron y compilaron `VistaPreviaOpcionalIntegrationTest` y `ActivacionConfiguracionIntegrationTest` para el nuevo contrato. Ejecución con PostgreSQL real y entrega SMTP real pendiente en este entorno. No puede iniciarse PostgreSQL embebido como root y el entorno no permite cambiar a un UID no privilegiado.
- Navegador con componentes React reales, datos ficticios y API simulada: guía con anillado/tapas, aprobación sin casillas/notas, salida/llegada sin texto, código visible en viaje y recepción con sólo código pasando a Cierre.

```bash
cd frontend
pnpm build
node --test tests/quality-guide.test.cjs tests/operation-task.test.cjs tests/printer-types.test.cjs
cd ../backend
mvn -Dtest=FinalizarCalidadServiceTest,CodigoRetiroServiceTest,EntregaConCodigoServiceTest,OperacionImpresoraServiceTest,SeleccionImpresoraServiceTest,RecuperacionImpresorasSecurityTest,VistaPreviaOpcionalIntegrationTest,ActivacionConfiguracionIntegrationTest test
```
