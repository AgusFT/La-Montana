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

La etapa 3 implementará estos límites, compartidos por todos los roles. La
precarga ocupará un lugar en cada cupo.

| Recurso | Cupo | Qué cuenta |
| --- | ---: | --- |
| Pedidos | 20 | Los pedidos cuentan en cualquier estado, incluso si fueron cancelados. Dar de baja un pedido en la Demo libera su lugar y conserva el historial. |
| Clientes | 10 | Cada ficha de cliente, incluidas las precargadas. Se podrán crear fichas ficticias mediante el flujo comercial. Las cuentas de administrador y empleado quedan fuera del cupo. |
| Caja | 30 | Cada cobro y cada devolución. Aplicar a un pedido un saldo ya recibido mantiene el contador. Dar de baja un movimiento conserva el asiento y sus vínculos. |
| Impresoras | 2 | Cada impresora simulada por su identidad. Las copias de la configuración de una impresora comparten un solo lugar. |

Cada baja lógica permitida libera un lugar una sola vez. El historial y sus
vínculos siguen disponibles según los permisos del rol.

## Funciones y módulos de presentación

La producción usa impresoras simuladas. La impresión física y la app móvil
(APK) quedan fuera de esta entrega. En la etapa 4, el panel de administración
de usuarios y los módulos de puntos de entrega y riders aparecerán en gris.
Sus operaciones quedarán excluidas del servidor de Demo. La barra lateral
identificará Analytics y Chat Bot como funciones prémium futuras.

## Vigencia

La etapa 4 incorporará el control de vigencia. Los 15 días se contarán como
360 horas desde el primer arranque de la Demo en el equipo. Todas las vistas,
incluido el selector de roles, mostrarán los días restantes. Reiniciar, reparar
o reinstalar la Demo conservará el plazo original.

Si se atrasa el reloj del equipo, las operaciones quedarán bloqueadas hasta
corregirlo. Al corregirlo, la Demo volverá a comprobar el vencimiento original.
Al cumplirse las 360 horas, conservará los archivos y mostrará este mensaje.

> Fin de la Demo, para adquirir el producto comunicate con administracion@yg-soluciones.com
