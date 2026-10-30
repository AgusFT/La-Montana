# Pedido operativo: revisión, producción y calidad

El detalle /operacion/pedidos/[id] utiliza seis cuadros para cliente, trabajo, documentos, entrega, importes e historial. Debajo se muestran la tarea y las acciones disponibles según el estado y los permisos. El recorrido superior señala la etapa actual, sin inventar nuevos estados persistidos ni fechas.

## Revisión y aprobación

El responsable consulta el PDF desde Documentos y pulsa **Aprobar pedido**. Una ventana breve pide confirmar la revisión de archivos, opciones y entrega. Las observaciones son opcionales. Solicitar corrección es una alternativa visible; rechazar y cancelar quedan en Otras acciones y conservan motivos y confirmaciones obligatorias.

La operación APROBAR en el servidor:

1. Comprueba acceso al pedido, permiso GESTIONAR_PEDIDOS, versión, estado, sucursal y correcciones.
2. Verifica las condiciones económicas con CobroPedidoService.exigirAprobable: dinero acreditado y aplicado, medios admitidos, seña/total y bloqueos del plazo.
3. Exige la confirmación humana y verifica los originales privados de todos los PDF.
4. Si falta la revisión, guarda REVISAR con su responsable y fecha, y después APROBAR. Ambos hechos se guardan en la misma transacción, con versiones consecutivas.
5. Conserva una revisión anterior cuando ya existe. No inicia producción ni registra dinero.

La revisión derivada utiliza un identificador determinista ligado a la operación principal. Recuperar APROBAR con la misma operación y contenido no duplica la revisión ni la aprobación; cambiar el contenido del mismo intento genera conflicto. El contrato anterior REVISAR sigue disponible para otros consumidores.

La consulta de acciones APROBAR ya no exige una operación REVISAR previa: la revisión explícitamente confirmada puede registrarse al aprobar. Los demás bloqueos del servidor se conservan. Si el motor impide aprobar, la pantalla muestra la causa y el acceso a cobros para perfiles financieros. Abrir la vista previa no registra una revisión ni una aprobación.

## Producción y calidad

- Preparar producción abre tarjetas de impresoras con capacidades, estado diario, papel estimado y motivos de compatibilidad. La recomendación respeta los datos del motor y los registros actuales. **Iniciar producción** registra el comienzo manual y cambia el pedido a EN_PRODUCCION.
- Una impresora declarada admite observaciones opcionales. Un recurso fuera del inventario exige describir la excepción.
- Registrar trabajo terminado admite observaciones opcionales y mantiene la confirmación del hecho físico.
- Controlar calidad abre datos del trabajo, checklist y resultado en columnas. La aprobación exige todos los controles y la confirmación del inspector; observaciones opcionales.
- Reimpresión, incidencia, error y cancelación de un trabajo exigen motivo.
- Sólo cuando todos los ítems superan calidad se avanza a LISTO_PARA_ENTREGA. Los ítems conservan sus intentos, recursos y controles anteriores.

Los textos de auditoría de acciones normales sin observaciones los genera el servidor como descripción del hecho. No se inventa una nota redactada por el empleado. Los permisos, restricciones de pago, integridad de archivos y guardas de producción se comprueban en el servidor.

Esta versión registra trabajo manual. No envía instrucciones a CUPS ni muestra impresoras como Online.

La operación diaria de impresoras, recargas y puntos se describe en [Operación diaria de impresoras y puntos](operacion-diaria-impresoras-y-puntos.md). Usa la migración V45 y no modifica capacidades ni configuraciones históricas.

## Consulta y navegación

Los paneles permanecen montados para conservar borradores al consultar herramientas. Las operaciones pendientes o de respuesta incierta bloquean el cambio de tarea; sus recuperaciones quedan disponibles dentro de las ventanas. Historia, notas, correcciones, cambio de fecha, PDF y caja siguen accesibles desde sus cuadros o Historial, notas y otras gestiones.

La entrega física, validación de código, conciliación y cierre conservan sus controles. El selector del resumen permite consultar todos los PDF y sus adicionales guardados. Los enlaces del menú del empleado mantienen altura normal.

## Verificación

- Web: pnpm typecheck, pnpm build y 59 pruebas de tarea operativa, seguimiento, recorrido del cliente y adicionales.
- Servidor: compilación Java 17 de las cuatro clases modificadas, con las dependencias del backend ya empaquetado, y ocho pruebas JUnit/Mockito de OperacionSimpleServiceTest. Cubren aprobación conjunta, bloqueos económicos, confirmación humana, PDF no verificable, permiso, versión, idempotencia, revisión anterior y motivo de rechazo.
- Se amplió VistaPreviaOpcionalIntegrationTest para probar la aprobación conjunta, versiones, recuperación y producción/calidad sin observaciones obligatorias. El test compiló; su ejecución con PostgreSQL real quedó pendiente.
- Maven no pudo descargar sus dependencias por las restricciones de red de este entorno; tampoco está disponible el usuario sin privilegios para arrancar PostgreSQL embebido. Por eso las pruebas de servicio se compilaron y ejecutaron directamente con Java/JUnit. Esta comprobación no sustituye el ensayo de transacciones y restricciones con PostgreSQL.
- Navegador: componentes React reales con API simulada y datos ficticios. Se comprobó el recorrido aprobación → producción → trabajo terminado → calidad → entrega, bloqueo por seña y por total previo, recuperación incierta y ventana de aprobación a 320 px sin desbordamiento horizontal. También se comprobó el detalle a 390 px.

Para ejecutar las pruebas del servidor en un entorno con Maven y PostgreSQL embebido disponibles:

```bash
cd backend
mvn -Dtest=OperacionSimpleServiceTest,VistaPreviaOpcionalIntegrationTest test
```

No hay migraciones nuevas. La actualización del entorno de pruebas conserva los pedidos y pagos existentes.
