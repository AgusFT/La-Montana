# Demo de La Montaña para Windows

La Demo permite probar el trabajo diario de una imprenta ficticia en la
aplicación web de La Montaña. Está prevista para Windows 11 x64 y podrá usarse
sin conexión después de instalarla.

**Estado de la entrega.** La web ya permite recorrer la imprenta Senderos desde
el código fuente. El instalador para Windows y su descarga pública se entregarán
en etapas posteriores. [Backlog y pruebas](backlog.md) · [Guía de distribución](distribucion.md)

## Entrar a la imprenta

Abrí la web local y elegí una de las tres cuentas. El selector está en la
portada y en `/acceso`. **Cambiar rol** cierra la sesión actual y permite volver a
elegir. Las tres cuentas comparten los datos de la misma imprenta.

| Rol | Cuenta de muestra | Qué podés probar |
| --- | --- | --- |
| Administrador | Elena Senderos | Consultar el panel general, la configuración y el historial de los pedidos. |
| Empleado | Mateo Tintas | Operar los pedidos de su sucursal y simular la impresión y el control de calidad. |
| Cliente | Lucía Papel | Cotizar un trabajo y consultar sus pedidos, su pago y su PDF. |

## Probar el pedido de muestra

En una instalación nueva hay una sucursal, una ficha de cliente, una impresora
simulada y un pedido aprobado. El pedido contiene el afiche **Encuentro de
caminantes**, de una página A4 en blanco y negro. Se pidieron tres copias.
El cobro de **$300 ARS** ya está aplicado.

1. Entrá como **Cliente**. En **Mis pedidos**, abrí el pedido y consultá **Archivos PDF** para ver o descargar el afiche.
2. Cambiá a **Empleado**, abrí **Ver sucursal** y elegí el mismo pedido. En **Impresión simulada**, iniciá el trabajo con la impresora de Senderos. Confirmá el motivo y después registrá la finalización.
3. Si querés completar el recorrido, realizá el control de calidad. La aprobación queda en el historial y el pedido pasa a **Listo para entrega**.

El reinicio conserva el estado que dejó el visitante y una sola copia de la imprenta.

## Cupos de la imprenta ficticia

Abrí **Administración → Cupos de la Demo** para ver cuántos lugares se usan y
qué registros los ocupan. Todos los roles comparten los límites de la instalación.
La imprenta recién instalada usa un lugar en cada recurso.

| Recurso | Cupo | Qué cuenta |
| --- | ---: | --- |
| Pedidos | 20 | Los pedidos cuentan en cualquier estado, incluso si fueron cancelados. Dar de baja un pedido en la Demo libera su lugar y conserva el historial. |
| Clientes | 10 | Lucía Papel ocupa un lugar reservado para el selector de Cliente. Quedan nueve lugares para otras fichas ficticias. El administrador y los empleados con permiso para registrar cobros pueden crearlas. |
| Caja | 30 | Cada cobro y cada devolución. Aplicar a un pedido un saldo ya recibido mantiene el contador. Dar de baja un movimiento conserva el asiento y sus vínculos. |
| Impresoras | 2 | Cada impresora simulada por su identidad. Las copias de la configuración de una impresora comparten un solo lugar. |

Para probar una baja, elegí un recurso, seleccioná el registro y confirmá.
El historial de bajas muestra quién hizo la operación y cuándo. La baja de un
cobro o una devolución conserva el asiento, el saldo y sus aplicaciones. Dar de
baja una impresora o un pedido impide iniciar nuevos trabajos con ese registro.
Las fichas de cliente que crees también admiten baja. Se conserva su historial
y se bloquea la creación de nuevas cotizaciones y pedidos a su nombre.

Al llegar al límite, el formulario explica qué cupo se agotó. Un administrador
puede liberar un lugar y después se puede volver a intentar el alta. Si dos
personas intentan ocupar el último lugar a la vez, una sola lo obtiene.

## Funciones y módulos de presentación

La producción usa impresoras simuladas. La impresión física y la app móvil
(APK) quedan fuera de esta entrega. En **Administración de usuarios** podés
ver los campos de la ficha, las sucursales asignadas y los permisos. Los
controles aparecen en gris. **Puntos de entrega** y **Riders** también tienen
vistas de muestra. El recorrido de Senderos usa retiro en sucursal. Las barras
laterales identifican **Analytics** y **Chat Bot** como funciones prémium futuras.

La operación comercial sigue disponible. Por ejemplo, podés crear una ficha
ficticia desde caja, consultar un pedido e iniciar su impresión simulada.

## Vigencia

Disponés de **360 horas desde el primer arranque** de la Demo en el equipo.
La franja superior muestra los días restantes en todas las vistas, incluido el
selector de roles. Una fracción del último día se muestra como un día restante.
El servidor local verifica la vigencia incluso cuando el equipo está sin Internet.

Si aparece **Revisá el reloj del equipo**, corregí la fecha y la hora y elegí
**Volver a comprobar**. La Demo retoma el plazo original al alcanzar la última
hora válida registrada. El reinicio conserva ese plazo. Al cumplirse las
360 horas, la aplicación cierra el acceso aun con una sesión abierta,
conserva los archivos y muestra este mensaje.

> Fin de la Demo, para adquirir el producto comunicate con administracion@yg-soluciones.com
