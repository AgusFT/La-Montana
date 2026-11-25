# Alcance global de la imprenta y recorrido del cliente

Decisión funcional confirmada por Alejandro el 26/10/2026. Describe el comportamiento objetivo; la implementación todavía requiere adaptación.

El motor configura a la **imprenta que contrata el software**. Cada imprenta tiene su propio catálogo, capacidades y reglas. Global significa toda esa imprenta, no un catálogo compartido entre distintos clientes del producto.

| Concepto | Alcance |
| --- | --- |
| Servicios y adicionales | Catálogo general de la imprenta. |
| Papeles, tamaños y tarifas | Oferta comercial global vigente. |
| Impresoras, formatos, color y doble faz | Capacidades productivas de la imprenta. |
| Modelo operativo, aprobación y pagos | Reglas de la imprenta aplicadas al trabajo. |
| Sucursal o punto de entrega | Destino elegido por el cliente, con horarios, cupos y condiciones logísticas. No necesita recursos productivos. |
| Ubicación y asignación de recursos | Operación interna; no crea un catálogo distinto que el cliente deba seleccionar. |

La ubicación física de una impresora es opcional, interna y versionada. Puede asociarse a una sucursal para que el administrador sepa dónde está el equipo, pero también puede quedar sin ubicación. La sucursal no modifica su compatibilidad ni la oferta global, y el administrador conserva la decisión manual sobre dónde producir cada trabajo.

El cliente cotiza primero su archivo, detalles de impresión y servicios adicionales. No elige sucursal de producción ni necesita destino o medio de pago para conocer el presupuesto inicial del trabajo. Doble faz es una opción de impresión.

Después completa retiro o envío, destino y pago. Se calculan entrega, total y requisitos económicos antes de aceptarlos. El presupuesto inicial no puede presentar un costo de envío o una fecha definitiva supuestos. Se mantienen los controles de anticipo cuando correspondan, inspección y aceptación de PDF y confirmación final del servidor.

Una vez creado el pedido se muestra su número y seguimiento en el dashboard. Los trabajos pendientes y cotizaciones vencidas también tienen contexto y próxima acción. Los pedidos confirmados mantienen las reglas de inmutabilidad, cancelación y corrección controlada.

Ejemplo: La Montaña ofrece impresión color A4 y anillado. El cliente cotiza ese trabajo y elige retirar en un punto sin impresoras ni anilladora. La imprenta produce y traslada internamente. La disponibilidad de entrega se comprueba sin quitar anillado del catálogo.

## Estado de implementación · 27/10/2026

- Fase 4 no usa `serviciosPorSucursal` ni `sucursal_servicio` como habilitación comercial. El nuevo recorrido cliente ya no pide sucursal productiva: la preparación y el presupuesto son globales.
- Las combinaciones y compatibilidades son globales; el backend deriva el origen de la casa matriz confirmada y guarda por separado el destino de retiro.
- V43 incorpora preparación privada y presupuesto previo a la oferta final. Entrega/pago y creación conviven en un único resumen. Ver [contrato y validación](cliente-tres-etapas.md).
- La preparación y los archivos se autorizan por propietario, rol y CSRF; la globalidad comercial no concede acceso a datos privados de otra cuenta.
- Conservar versiones y pedidos históricos. No alcanza con ocultar la sucursal o elegir una arbitrariamente.
- La asignación interna de centro, operador e impresora debe tener reglas explícitas. Esta decisión no introduce un algoritmo automático.

Se conservan los modelos operativos, reglas económicas, ciclo de borradores, seguridad, activación/programación e historial. Los ajustes de recursos, simulación, entrega y cliente deben respetar esta separación.
