# Actualización a revisión - Historias del producto configurable

| Campo | Valor |
|---|---|
| Versión | 2.1 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-05 |
| Identificadores | Provisionales |
| Propósito | Facilitar validación e integración posterior |

> Estas historias no sustituyen historias-de-usuario.md. Los códigos PROP-HU son referencias temporales y deberán consolidarse después de la revisión.

## 1. Administrador de configuración

### PROP-HU-ADM-001 - Seleccionar modelo operativo

Como ADMIN_ADMIN quiero seleccionar un modelo operativo certificado para adaptar el funcionamiento de la imprenta sin crear combinaciones inseguras.

Criterios propuestos:

- se muestran modelos disponibles;
- cada modelo explica sus consecuencias;
- Control manual y Control condicional se presentan en una única vista comparable;
- la automatización certificada permanece visible como evolución futura deshabilitada;
- solo se habilitan parámetros compatibles;
- seleccionar un modelo no lo activa automáticamente.

### PROP-HU-ADM-002 - Configurar aprobación

Como ADMIN_ADMIN quiero configurar una condición certificada cuando selecciono Control condicional para adaptar el avance sin crear reglas ambiguas.

Criterios propuestos:

- en Control manual todos los pedidos requieren decisión humana;
- en Control condicional se muestran pago previo, pago de seña y monto total del pedido;
- pago previo y seña bloquean la carga del archivo hasta su acreditación;
- superar el umbral de monto deriva a revisión humana y no rechaza automáticamente;
- se muestran las condiciones de avance mediante una simulación de punta a punta;
- los estados internos no pueden modificarse;
- el backend valida la configuración.

### PROP-HU-ADM-003 - Configurar pagos y seña

Como ADMIN_ADMIN quiero configurar medios de pago, momento de cobro y reglas de seña para reflejar la operación comercial de la imprenta.

Criterios propuestos:

- se muestran solamente combinaciones compatibles;
- efectivo puede vincularse con pago al entregar;
- pago previo puede vincularse con medios digitales;
- las reglas se muestran mediante ejemplos;
- ninguna modificación se activa sin confirmación.

### PROP-HU-ADM-004 - Configurar impresoras

Como ADMIN_ADMIN quiero registrar impresoras y capacidades para determinar qué equipos pueden realizar cada tipo de trabajo.

Criterios propuestos:

- cada impresora tiene disponibilidad y capacidades;
- el modo inicial permite asignación manual;
- la asignación automática se habilita solamente si existe un modelo certificado;
- una impresora no operativa no debe ofrecerse.

### PROP-HU-ADM-005 - Administrar módulos

Como ADMIN_ADMIN quiero activar o desactivar módulos contratados para adaptar el producto a las necesidades de la imprenta.

Criterios propuestos:

- se diferencian módulos incluidos y no incluidos;
- no puede activarse un módulo fuera del plan;
- desactivar un módulo no elimina historial;
- se explican dependencias.

### PROP-HU-ADM-006 - Previsualizar configuración

Como ADMIN_ADMIN quiero visualizar un resumen del flujo resultante para comprender las consecuencias antes de activarlo.

Criterios propuestos:

- muestra diferencias contra la versión activa;
- presenta ejemplos de pedidos;
- identifica conflictos;
- explica impacto en aprobación, pago y producción.

### PROP-HU-ADM-007 - Activar inmediatamente

Como ADMIN_ADMIN quiero activar una configuración inmediatamente para aplicarla a la siguiente cotización.

Criterios propuestos:

- requiere protocolo de seguridad;
- genera una versión inmutable;
- registra fecha y hora;
- no modifica cotizaciones anteriores.

### PROP-HU-ADM-008 - Programar activación

Como ADMIN_ADMIN quiero programar fecha y hora para aplicar una configuración cuando resulte conveniente.

Criterios propuestos:

- la versión actual continúa vigente;
- puede cancelarse antes de la activación;
- la fecha se muestra claramente;
- activación y cancelación quedan auditadas.

### PROP-HU-ADM-009 - Consultar historial

Como ADMIN_ADMIN quiero consultar versiones anteriores para conocer qué configuración estuvo vigente y quién la activó.

Criterios propuestos:

- muestra autor, fecha, diferencias y vigencia;
- las versiones históricas no se editan;
- puede utilizarse una versión como base de otra nueva;
- no se altera el historial.

## 2. Disponibilidad operativa

### PROP-HU-OPE-001 - Pausar recepción

Como usuario interno autorizado quiero pausar la recepción de pedidos para evitar comprometer trabajos durante una emergencia.

Actores propuestos:

- ADMIN_ADMIN;
- empleado autenticado.

Criterios propuestos:

- requiere reconfirmación de contraseña;
- solicita motivo;
- bloquea cotizaciones nuevas;
- bloquea confirmaciones pendientes;
- mantiene pedidos existentes;
- registra auditoría.

### PROP-HU-OPE-002 - Reanudar recepción

Como usuario interno autorizado quiero reanudar la recepción cuando la imprenta vuelva a estar operativa.

Criterios propuestos:

- requiere reconfirmación;
- registra usuario y momento;
- actualiza la comunicación al cliente;
- permite nuevos intentos de cotización y confirmación.

## 3. Cliente y cotización

### PROP-HU-CLI-001 - Cotizar con configuración vigente

Como cliente quiero recibir una cotización basada en las condiciones actuales de la imprenta para saber precio, pago, entrega y fecha estimada antes de crear el pedido.

Criterios propuestos:

- captura la versión al presionar Cotizar pedido;
- muestra solamente opciones vigentes;
- conserva las condiciones mientras la cotización esté activa;
- no permite cotizar si la imprenta está pausada.

### PROP-HU-CLI-002 - Confirmar dentro del tiempo seguro

Como cliente quiero conocer cuánto tiempo tengo para confirmar para evitar perder información sin advertencia.

Criterios propuestos:

- cinco minutos iniciales;
- aviso de actividad;
- hasta sesenta segundos para responder;
- diez minutos exactos adicionales si responde;
- cuenta regresiva visible;
- anulación y cierre al expirar.

### PROP-HU-CLI-003 - Recibir aviso por pausa

Como cliente con una cotización activa quiero ser informado si la imprenta se pausa para comprender por qué todavía no puedo crear el pedido.

Criterios propuestos:

- el mensaje no se presenta como error técnico;
- conserva precio y condiciones mientras siga vigente;
- informa que el pedido no fue creado;
- permite reintentar después de la reanudación.

### PROP-HU-CLI-004 - Aceptar nueva fecha

Como cliente quiero conocer cualquier cambio en la fecha estimada causado por una pausa para decidir si continúo.

Criterios propuestos:

- muestra fecha anterior y nueva;
- conserva precio y demás condiciones;
- exige aceptación explícita;
- no crea el pedido si no acepta.

## 4. Cuenta corriente - Requiere revisión

### PROP-HU-ADM-CC-001 - Administrar cuenta corriente

Como ADMIN_ADMIN quiero habilitar, suspender o deshabilitar una cuenta corriente para controlar qué clientes pueden enviar pedidos sin pago previo.

Criterios propuestos:

- asignación manual;
- modalidad con límite o sin límite;
- acciones auditadas;
- no implica aprobación automática.

### PROP-HU-ADM-CC-002 - Revisar pedido a cuenta

Como ADMIN_ADMIN quiero revisar manualmente cada pedido de cuenta corriente para proteger recursos y capacidad productiva.

Criterios propuestos:

- muestra deuda actual y proyectada;
- muestra pedidos y trabajos existentes;
- permite aprobar, rechazar o dejar pendiente;
- requiere decisión explícita antes de producción.

### PROP-HU-ADM-CC-003 - Definir fecha de entrega

Como ADMIN_ADMIN quiero establecer manualmente la fecha estimada de un pedido a cuenta para contemplar trabajos pagados, carga productiva y acuerdos con el cliente.

Criterios propuestos:

- la fecha no se calcula automáticamente;
- puede acompañarse con observaciones;
- queda registrada con la decisión;
- se comunica al cliente según el flujo que se defina.

### PROP-HU-CLI-CC-001 - Enviar pedido a cuenta

Como cliente habilitado quiero enviar un pedido sin pago previo para utilizar el acuerdo comercial con la imprenta.

Criterios propuestos:

- el sistema informa que requiere revisión manual;
- enviar no significa aprobar producción;
- el cliente puede consultar el resultado;
- las condiciones definitivas dependen de la revisión.

## 5. Historias habilitadas para diseño

Pueden utilizarse como base de mockups:

- PROP-HU-ADM-001 a PROP-HU-ADM-009;
- PROP-HU-OPE-001 y PROP-HU-OPE-002;
- PROP-HU-CLI-001 a PROP-HU-CLI-004.

Las historias de cuenta corriente pueden representarse como exploración visual, pero deben mantenerse en estado de revisión.

## 6. Registro de cambios y justificación

| Historia incorporada | Necesidad detectada | Justificación por el motor de configuración | Estado |
|---|---|---|---|
| Seleccionar modelo certificado | Evitar combinaciones libres | El motor debe ofrecer estructuras coherentes | Confirmado |
| Control manual y condicional | Cerrar el alcance operativo de la Fase 2 | Separa decisión humana de condiciones certificadas | Confirmado |
| Tres condiciones certificadas | Evitar reglas libres de aprobación | Mantiene recorridos predecibles y validables | Confirmado |
| Archivo retenido en cliente | No se explicitaba el uso de recursos antes del pago | Evita almacenamiento y procesamiento antes de acreditar pago o seña | Confirmado |
| Previsualizar consecuencias | Hacer configuración comprensible | Facilita instalación remota y reduce errores | Confirmado |
| Activar o programar | Definir vigencia | Permite aplicar cambios sin retroactividad | Confirmado |
| Historial de versiones | Auditar cambios | Cada activación debe ser trazable | Confirmado |
| Pausar y reanudar | Responder a emergencias | Disponibilidad operativa prioritaria | Confirmado |
| Cotización con temporizador | Proteger datos económicos | Vincula sesión, versión y recursos temporales | Confirmado |
| Aceptar nueva fecha | Tratar interrupciones excepcionales | Conserva condiciones y comunica impacto operativo | Confirmado |
| Revisar cuenta corriente | Proteger capital y producción | Excepción financiera controlada por pedido | Requiere revisión de Agustín |

## 7. Referencias para integración

Las historias surgen de los cambios introducidos por el motor de configuración y deberán contrastarse con:

- analisis/historias-de-usuarios/historias-de-usuario.md
- analisis/especificacion-de-requerimientos/actualizacion-a-revision-reglas-y-requerimientos-configurables.md
- marco-del-proyecto/actualizacion-a-revision-motor-configuracion-versionada.md
- diseño/Front/ux-ui/wireflows/WF-ADMINISTRADOR-MVP.md

La numeración definitiva deberá resolverse al integrar este documento.
