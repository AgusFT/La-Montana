# Demo de La Montaña para Windows

La Demo permite probar el trabajo diario de una imprenta ficticia en la
aplicación web de La Montaña. Está prevista para Windows 11 x64 y podrá usarse
sin conexión después de instalarla.

**Estado de la entrega.** La aplicación ya arranca desde el código fuente y se
identifica como Demo. Puede ejecutarse junto a la edición de desarrollo con su
propia base, archivos y sesión. El selector de roles, la imprenta precargada y
el instalador para Windows corresponden a las próximas etapas.
[Backlog y pruebas](backlog.md) · [Guía de distribución](distribucion.md)

## Cómo se usará la Demo

1. Descargar el instalador desde **Descargar demo** en la web de YG Soluciones.
2. Instalar la aplicación y abrirla en el navegador desde el acceso directo.
3. Elegir un rol en el selector. Se podrá entrar sin registro ni contraseña.
4. Explorar los datos de la imprenta ficticia y recorrer un pedido hasta la
   simulación de impresión.

Cada rol abre una sesión con sus propios permisos.

| Rol | Qué permite explorar |
| --- | --- |
| Administrador | Gestión de la imprenta dentro de las funciones habilitadas para la Demo. |
| Empleado | Operación de pedidos y seguimiento de la producción conforme a sus permisos. |
| Cliente | Experiencia de cliente, con consulta y seguimiento de sus pedidos. |

## Cupos de la imprenta ficticia

Los cupos se comparten entre todos los roles de una instalación. Los datos
precargados ocupan parte de esos cupos.

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
(APK) quedan fuera de esta entrega. El panel de administración de usuarios y
los módulos de puntos de entrega y riders aparecen en gris. La barra lateral
identifica Analytics y Chat Bot como funciones prémium futuras. Los módulos de
presentación quedan fuera de las operaciones disponibles en el servidor de Demo.

## Vigencia

Los 15 días equivalen a 360 horas desde el primer arranque de la Demo en el
equipo. Todas las vistas, incluido el selector de roles, muestran un banner con
los días restantes. Reiniciar, reparar o reinstalar la Demo conserva el plazo
original.

Si se atrasa el reloj del equipo, las operaciones quedan bloqueadas hasta
corregirlo. Al corregirlo, la Demo vuelve a comprobar el vencimiento original.
Al cumplirse las 360 horas, se conservan los archivos y se muestra este mensaje.

> Fin de la Demo, para adquirir el producto comunicate con administracion@yg-soluciones.com
