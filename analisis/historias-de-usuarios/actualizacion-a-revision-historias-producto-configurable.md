# Actualización a revisión - Historias del producto configurable

| Campo | Valor |
|---|---|
| Versión | 2.4 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-10 |
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
- en la condición por monto, hasta el umbral configurado el pedido puede aprobarse automáticamente;
- cuando el pedido supera el umbral, el flujo financiero se completa en Fase 3 mediante una seña previa configurable y no mediante una revisión humana automática;
- se muestran las condiciones de avance mediante una simulación de punta a punta;
- los estados internos no pueden modificarse;
- el backend valida la configuración.

### PROP-HU-ADM-003 - Configurar pagos y seña

Como ADMIN_ADMIN quiero configurar medios de pago, momento de cobro y reglas de seña para reflejar la operación comercial de la imprenta sin romper las decisiones tomadas en Fase 2.

Criterios propuestos:

- la interfaz muestra el modelo heredado de Fase 2 y solamente combinaciones compatibles;
- en Control manual pueden habilitarse efectivo, transferencia y pago digital, y la seña es opcional;
- en aprobación por pago previo, efectivo queda deshabilitado para satisfacer la condición, deben existir medios acreditables y la seña no aplica;
- en aprobación por pago de seña, la condición viene fijada por Fase 2, no puede apagarse ni cambiarse, y Fase 3 permite configurar tipo y valor;
- en aprobación por monto, el ADMIN_ADMIN configura el valor del umbral monetario;
- los pedidos dentro del umbral pueden aprobarse automáticamente y utilizar efectivo si está habilitado;
- para pedidos que superan el umbral se exige una seña previa configurable;
- la seña de pedidos superiores debe acreditarse por transferencia, pago digital u otro medio acreditable; efectivo no satisface la seña previa;
- el saldo restante puede abonarse mediante los medios generales habilitados, incluido efectivo cuando corresponda;
- la regla histórica del 30 % desde 200 carillas se conserva solo como antecedente o valor predeterminado configurable y no como condición fija;
- las reglas se muestran mediante ejemplos y una simulación del recorrido desde Fase 1, Fase 2 y Fase 3;
- ninguna modificación se activa sin confirmación.

### PROP-HU-ADM-004 - Configurar impresoras y capacidades

Como ADMIN_ADMIN quiero registrar y mantener impresoras con sus capacidades para que el sistema pueda determinar qué equipos son compatibles con cada tipo de trabajo y asistir la operación remota.

Criterios propuestos:

- cada impresora mantiene una identidad técnica estable y un nombre visible editable;
- se configuran formatos admitidos, capacidad B/N o color, dúplex cuando corresponda y capacidad máxima de hojas;
- los únicos estados de esta versión son Operativa y Deshabilitada;
- una impresora Operativa puede recibir trabajos y no puede editarse ni eliminarse;
- una impresora Deshabilitada no recibe nuevos trabajos y puede editarse;
- la opción Eliminar aparece únicamente dentro de la edición de una impresora Deshabilitada;
- eliminar una impresora no debe romper la trazabilidad histórica de trabajos anteriores;
- la capacidad máxima de papel se carga manualmente al crear o editar la impresora;
- se muestran por separado el contador histórico de hojas impresas y la disponibilidad actual estimada de papel;
- la disponibilidad estimada se expresa como cantidad de hojas y porcentaje respecto de la capacidad configurada;
- una impresora no operativa no debe ofrecerse para nuevos trabajos.

### PROP-HU-ADM-005 - Administrar módulos - Futuro

Como ADMIN_ADMIN quiero activar o desactivar módulos opcionales para adaptar el producto a las necesidades de la imprenta.

Estado actual:

- la definición comercial de módulos queda fuera de Fase 5;
- todavía no se definieron las prestaciones de los planes Gratis, Inicial y Avanzado;
- conceptualmente cada módulo podrá poseer un estado activo/inactivo;
- una mejora opcional no debe impedir completar el flujo básico del negocio;
- esta historia no debe utilizarse como base de los mockups actuales de Fase 5.

### PROP-HU-ADM-006 - Previsualizar configuración

Como ADMIN_ADMIN quiero visualizar un resumen del flujo resultante para comprender las consecuencias antes de activarlo.

Criterios propuestos:

- muestra diferencias contra la versión activa;
- presenta ejemplos de pedidos;
- identifica conflictos;
- explica impacto en aprobación, pago, producción, horarios y entrega.

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

### PROP-HU-ADM-010 - Configurar método de asignación

Como ADMIN_ADMIN quiero definir el método disponible para asignar trabajos a impresoras para mantener una operación controlada y predecible.

Criterios propuestos:

- en V1 la asignación manual es la modalidad predeterminada;
- la asignación automática permanece visible como evolución futura, pero deshabilitada;
- el sistema puede recomendar una impresora compatible sin asignarla automáticamente;
- una futura asignación automática deberá basarse en un modelo certificado y considerar compatibilidad, disponibilidad, carga y estado operativo;
- configurar el método no asigna ningún trabajo por sí mismo.

### PROP-HU-ADM-011 - Configurar horarios y tiempos estimados

Como ADMIN_ADMIN quiero configurar los horarios operativos y los tiempos estimados de preparación y envío para que el sistema calcule compromisos de entrega compatibles con la jornada real de la imprenta.

Criterios propuestos:

- cada día de la semana se configura por separado;
- cada día puede habilitarse o cerrarse;
- los días habilitados definen hora de apertura y hora de cierre;
- el tiempo estimado de preparación en Casa Central se expresa en horas;
- el tiempo estimado para preparar o liberar un pedido para envío se expresa en horas;
- los tiempos se consumen únicamente dentro de los horarios operativos;
- un pedido recibido fuera de horario puede quedar en cola y comienza a consumir tiempo en la próxima apertura comercial;
- la vista incluye una simulación del recorrido resultante;
- si un pedido se completa antes del estimado, el flujo puede avanzar inmediatamente.

### PROP-HU-ADM-012 - Administrar puntos de entrega

Como ADMIN_ADMIN quiero administrar los puntos de entrega y sus franjas horarias para ofrecer al cliente ubicaciones y ventanas de retiro realmente utilizables.

Criterios propuestos:

- la Fase 5 presenta una acción Administrar puntos;
- el panel dedicado permite consultar, agregar y editar puntos;
- cada punto puede definir nombre y ubicación;
- cada punto puede definir, cuando sea necesario, días y franjas horarias propias;
- las franjas expresan ventanas estimadas y no una hora exacta obligatoria;
- la disponibilidad operativa temporal del punto se gestiona sin crear una versión nueva;
- el cliente solo visualiza puntos actualmente disponibles.

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

### PROP-HU-OPE-003 - Seleccionar impresora compatible

Como usuario interno autorizado quiero ver qué impresoras pueden realizar un trabajo y recibir una recomendación para asignarlo manualmente sin enviar trabajos a equipos incompatibles o con baja disponibilidad estimada de papel.

Criterios propuestos:

- el sistema conoce las características del trabajo, incluido formato de hoja y necesidad de B/N o color;
- compara esas características con las capacidades registradas de las impresoras Operativas;
- las impresoras compatibles pueden seleccionarse manualmente;
- las incompatibles se identifican con la causa, por ejemplo formato no admitido o ausencia de impresión color;
- el sistema puede recomendar una impresora compatible utilizando también la disponibilidad estimada de papel;
- la recomendación no reemplaza la decisión humana en V1;
- la asignación automática no está disponible en V1.

### PROP-HU-OPE-004 - Registrar recarga de papel

Como usuario interno autorizado quiero informar al sistema cuando una impresora fue completada físicamente hasta su capacidad máxima para mantener sincronizada la disponibilidad estimada y poder operar de forma remota con información útil.

Criterios propuestos:

- la capacidad máxima proviene de la configuración de la impresora;
- antes de confirmar, el sistema muestra las hojas estimadas actuales y la capacidad máxima;
- una recarga significa completar físicamente el faltante hasta alcanzar el máximo configurado, aunque el nivel previo sea alto;
- al confirmar la recarga, la disponibilidad estimada vuelve al 100 % y a la cantidad máxima configurada;
- el contador histórico de hojas impresas no se reinicia;
- el evento registra la impresora, el usuario y el momento;
- la interfaz identifica el dato de disponibilidad como estimado y no como lectura física de un sensor.

### PROP-HU-OPE-005 - Gestionar disponibilidad temporal de un punto

Como usuario interno autorizado quiero habilitar o deshabilitar rápidamente un punto de entrega para reflejar contingencias operativas sin modificar la configuración versionada.

Criterios propuestos:

- habilitar y deshabilitar no solicitan motivo;
- la acción es inmediata y no crea una versión nueva;
- un punto deshabilitado deja de ofrecerse a nuevos pedidos;
- el dashboard mantiene un recordatorio visible mientras exista uno o más puntos deshabilitados;
- el punto se vuelve a habilitar manualmente cuando esté operativo;
- las entregas ya pactadas conservan el punto acordado y no se reasignan automáticamente;
- una contingencia puede demorar esas entregas sin alterar automáticamente su modalidad.

## 3. Cliente y cotización

### PROP-HU-CLI-001 - Cotizar con configuración vigente

Como cliente quiero recibir una cotización basada en las condiciones actuales de la imprenta para saber precio, pago, entrega y fecha estimada antes de crear el pedido.

Criterios propuestos:

- captura la versión al presionar Cotizar pedido;
- muestra solamente opciones vigentes y disponibles;
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

### PROP-HU-CLI-005 - Visualizar estimación y avance de entrega

Como cliente quiero visualizar en el timeline cuándo comenzará a trabajarse mi pedido y cuándo se estima que estará disponible para comprender demoras fuera de horario y recibir avisos si finaliza antes.

Criterios propuestos:

- si el pedido ingresa fuera de horario se muestra En cola;
- se informa la próxima apertura operativa cuando corresponda;
- el timeline muestra el avance real, por ejemplo En preparación, En camino y Listo para retirar;
- la estimación respeta horas operativas y franjas válidas del punto;
- si el pedido queda listo antes, el estado se actualiza inmediatamente y el cliente puede recibir la notificación correspondiente;
- la coordinación fina de un punto puede completarse por canales externos como WhatsApp sin alterar el estado operativo mostrado en la aplicación.

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

- PROP-HU-ADM-001 a PROP-HU-ADM-004;
- PROP-HU-ADM-006 a PROP-HU-ADM-012;
- PROP-HU-OPE-001 a PROP-HU-OPE-005;
- PROP-HU-CLI-001 a PROP-HU-CLI-005.

PROP-HU-ADM-005 permanece como evolución futura y no forma parte de la Fase 5 actual.

Las historias de cuenta corriente pueden representarse como exploración visual, pero deben mantenerse en estado de revisión.

## 6. Registro de cambios y justificación

| Historia incorporada | Necesidad detectada | Justificación por el motor de configuración | Estado |
|---|---|---|---|
| Seleccionar modelo certificado | Evitar combinaciones libres | El motor debe ofrecer estructuras coherentes | Confirmado |
| Control manual y condicional | Cerrar el alcance operativo de la Fase 2 | Separa decisión humana de condiciones certificadas | Confirmado |
| Tres condiciones certificadas | Evitar reglas libres de aprobación | Mantiene recorridos predecibles y validables | Confirmado |
| Archivo retenido en cliente | No se explicitaba el uso de recursos antes del pago | Evita almacenamiento y procesamiento antes de acreditar pago o seña | Confirmado |
| Matriz financiera heredada | Fase 3 no explicitaba dependencias de Fase 2 | Evita medios y reglas incompatibles con el modelo seleccionado | Confirmado para revisión |
| Umbral con seña escalonada | El modelo por monto no definía el cobro para trabajos superiores | Permite trabajos chicos sin seña y protege pedidos mayores con acreditación previa | Confirmado para revisión |
| Administración segura de impresoras | La historia de impresoras era genérica | Define estados, edición, eliminación y capacidades sin modificar equipos en operación | Confirmado |
| Compatibilidad y recomendación | No estaba definido cómo elegir un equipo | Evita asignaciones inválidas y asiste la operación manual de V1 | Confirmado |
| Disponibilidad estimada de papel | No existía una métrica para operación remota | Permite conocer cantidad y porcentaje estimados por impresora | Confirmado |
| Recarga manual a capacidad máxima | La estimación podía quedar desincronizada | Sincroniza el contador cuando el operador completa físicamente el faltante | Confirmado |
| Horarios y tiempos estimados | La entrega no contemplaba el calendario real de trabajo | Permite promesas calculadas dentro de horas operativas | Confirmado |
| Pedidos fuera de horario | Una duración continua podía producir horarios imposibles | El pedido queda en cola y el reloj comienza en la próxima apertura | Confirmado |
| Administración de puntos | Los puntos estaban tratados solo como opciones visibles | Define datos, franjas y acceso dedicado de gestión | Confirmado |
| Baja operativa de punto | Una contingencia temporal podía confundirse con una nueva configuración | Permite deshabilitar y reactivar sin versionar | Confirmado |
| Timeline de entrega | La estimación no explicaba el recorrido al cliente | Hace visible cola, inicio, preparación y finalización anticipada | Confirmado |
| Módulos fuera de Fase 5 | Los planes comerciales todavía no están definidos | Evita convertir hipótesis comerciales en requisitos actuales | Futuro |
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