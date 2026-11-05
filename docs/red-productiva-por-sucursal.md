# Red productiva por sucursal · V57

Implementación del 5/11/2026. Reemplaza la regla operativa que trataba las impresoras como un parque global concentrado en casa matriz. Los precios y servicios continúan siendo globales; la capacidad física, la operación y la responsabilidad pasan a estar separadas por sucursal.

## Reglas vigentes

- Cada impresora operativa debe tener una sucursal física activa.
- Cada sucursal tiene una única cuenta encargada y una cuenta encargada no puede representar dos sucursales.
- El encargado sólo consulta y opera pedidos, impresoras, papel y retiros de su sucursal.
- El administrador general conserva la vista completa y puede operar cualquiera de las plantas.
- Un retiro en sucursal se produce inicialmente en esa misma sucursal.
- Un punto de entrega puede vincularse con varias plantas mediante prioridades únicas `1..999`. El servidor elige la de menor número que tenga capacidad compatible para todo el pedido.
- Los envíos a domicilio usan casa matriz como origen de respaldo.
- El destino elegido por el cliente nunca se modifica por una derivación productiva.

## Excepción administrada por falla

El administrador puede derivar un pedido aprobado a otra sucursal compatible. La acción está disponible antes del primer intento y también cuando el último intento de cada ítem afectado quedó en error, cancelado o con control de calidad que exige reimpresión. Se bloquea mientras exista una impresión en curso, un trabajo terminado pendiente de calidad o un trabajo aprobado.

La derivación actualiza solamente la sucursal productiva y el origen logístico activo. Conserva el cliente, la cotización, los importes, los PDF, el medio de pago, la fecha y el destino. La auditoría registra una operación idempotente, planta anterior, planta nueva, administrador, motivo y fecha. La base de datos rechaza impresoras que no pertenezcan a la planta vigente del pedido.

## Escenario de pruebas

| Sucursal | Cuenta encargada | Flota inicial |
|---|---|---|
| Casa Matriz Floresta | `sucursal.floresta` | Brother MFC-J3940DW, Xerox VersaLink C405, HP LaserJet Pro M404dn |
| Sucursal Medicina | `sucursal.medicina` | Epson EcoTank L8180, Brother HL-L6400DW |
| Sucursal La Plata | `sucursal.la.plata` | Canon MAXIFY GX7010, Brother MFC-L6900DW, Epson WorkForce Pro WF-C5890 |

La contraseña predeterminada es la común del entorno de pruebas. Los puntos Palermo, La Plata Centro, Tolosa y City Bell tienen más de una planta posible: la sucursal cercana posee prioridad 1 y Floresta actúa como respaldo. Caballito conserva Floresta como origen principal.

## Recorrido operativo

1. El cliente elige dónde retirar o recibir el pedido; no elige impresora.
2. El servidor determina la planta inicial y congela la reserva de entrega.
3. El encargado ve el pedido en su bandeja y sólo recibe las impresoras de su planta.
4. Si la planta no puede continuar, registra la falla o cancelación.
5. El administrador abre Producción, elige otra sucursal compatible y escribe el motivo.
6. El nuevo encargado recibe el trabajo; el destino final y el flujo de entrega continúan sin cambios.

Las cuentas de sucursal también conservan `CREAR_PEDIDOS`: pueden preparar pedidos de mostrador para invitados identificados sólo por nombre o buscar un cliente registrado. Para un cliente registrado deben elegir un retiro o punto que su sucursal esté autorizada a producir; la autorización se comprueba en el servidor.
