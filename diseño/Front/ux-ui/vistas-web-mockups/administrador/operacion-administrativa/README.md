# Operación administrativa

## Propósito

Esta carpeta reúne la evidencia UX/UI vigente de la **operación cotidiana del panel administrativo** de La Montaña una vez incorporado el motor de configuración.

No representa un flujo lineal único ni una repetición del MVP original. El comportamiento visible depende de la configuración activa de la imprenta, mientras que las vistas de esta carpeta representan las superficies operativas utilizadas en el día a día.

La carpeta histórica `flujo de negocio principal` conserva la evidencia del MVP original y no debe utilizarse como referencia vigente para las nuevas vistas administrativas.

## Criterio de organización

Se utilizan prefijos por dominio para evitar colisiones con la numeración del motor de configuración:

- `MC-ADM-PED`: pedidos y seguimiento general;
- `MC-ADM-PRO`: producción y control de calidad;
- `MC-ADM-ENT`: entregas y logística.

## Evidencia visual actual

### Pedidos

- `MC-ADM-PED-001-estado-pedidos.png`
  - bandeja transversal de pedidos;
  - tabs Activos / Histórico;
  - filtros por estado, cliente, fecha, entrega y estado financiero;
  - acceso `Requieren revisión` visible únicamente cuando la configuración activa contempla revisión humana.

### Producción

- `MC-ADM-PRO-001-produccion.png`
  - tabs Listos para producción / En producción / Control de calidad;
  - información del trabajo, compromiso de entrega e impresoras compatibles;
  - acceso a asignación e inicio de producción.

- `MC-ADM-PRO-002-asignacion-impresora-inicio-produccion.png`
  - detalle del cliente y del trabajo;
  - archivo autorizado;
  - impresoras compatibles e incompatibles;
  - recomendación de impresora;
  - disponibilidad estimada de papel;
  - asignación manual e inicio de producción.

- `MC-ADM-PRO-003-control-calidad-bandeja.png`
  - trabajos impresos pendientes de revisión manual;
  - impresora utilizada;
  - hora de finalización y compromiso de entrega;
  - acceso a revisión.

- `MC-ADM-PRO-004-control-calidad-detalle.png`
  - revisión manual del trabajo impreso;
  - checklist de calidad;
  - observaciones;
  - reimpresión ante rechazo;
  - aprobación y derivación a Entregas.

### Entregas

- `MC-ADM-ENT-001-entregas.png`
  - trabajos con control de calidad aprobado;
  - modalidades Retiro en local / Punto de entrega / Envío;
  - estados logísticos y financieros;
  - filtros por modalidad, fecha, punto, estado y estado financiero.

> Estado actual: la bandeja principal de Entregas está definida. Las vistas operativas de detalle, validación y cierre de entrega todavía se encuentran en revisión y se incorporarán cuando queden cerradas.

## Principios funcionales consolidados hasta este punto

- **Pedidos** centraliza el seguimiento general y la consulta histórica. Los estados no se convierten en submenús permanentes.
- **Requieren revisión** depende de la configuración activa del motor, no del contador de pedidos.
- **Producción** comienza cuando un pedido está realmente habilitado para producir y termina cuando supera el control de calidad.
- La asignación de impresora es manual en V1; el sistema puede recomendar una alternativa compatible.
- El control de calidad es una revisión manual. Aprobar deriva el trabajo a Entregas; rechazarlo deriva a reimpresión.
- **Entregas** comienza luego de la aprobación de calidad y adapta su recorrido a la modalidad capturada en el pedido.
- El estado operativo y el estado financiero permanecen separados.
- El timeline del cliente refleja el avance real sin exponer detalles técnicos internos.

## Estado documental

Estas vistas forman parte de la alineación UX/UI posterior al motor de configuración. Las incumbencias y recorridos deberán sincronizarse con wireflows, issues, historias/casos de uso y documentación del administrador una vez finalizada la revisión de todas las áreas del panel.
