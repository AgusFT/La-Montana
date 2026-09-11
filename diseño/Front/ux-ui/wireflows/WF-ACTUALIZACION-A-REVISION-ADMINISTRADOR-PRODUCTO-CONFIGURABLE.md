# WF - Actualización a revisión - Administrador del producto configurable

| Campo | Valor |
|---|---|
| Versión | 2.7 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-10 |
| Plataforma principal | Web administrativa |
| Actor principal | ADMIN_ADMIN |
| Actores complementarios | Empleado, Cliente, Sistema |
| Relación | Extensión del wireflow administrativo histórico |

> Este wireflow no reemplaza WF-ADMINISTRADOR-MVP.md. Propone las vistas necesarias para el producto configurable y sirve como base inmediata para mockups. Las vistas de cuenta corriente permanecen exploratorias.

## 1. Principios de experiencia

- configuración guiada;
- lenguaje operativo;
- consecuencias visibles;
- selección por modelos certificados;
- parámetros progresivos;
- resumen antes de activar;
- seguridad explícita;
- estado actual siempre identificable;
- errores accionables;
- accesibilidad y respuesta adaptable;
- backend como fuente de verdad.

## 2. Navegación propuesta

Configuración se incorpora como sección principal del panel del ADMIN_ADMIN.

Subsecciones:

- Resumen;
- Operación;
- Aprobación;
- Pagos y seña;
- Impresoras;
- Horarios y entrega;
- Versiones;
- Cuenta corriente, sujeta a revisión.

La gestión comercial de módulos queda fuera de la Fase 5 hasta definir el catálogo y la composición de los planes Gratis, Inicial y Avanzado.

La pausa operativa, la recarga de papel y la disponibilidad temporal de puntos deben estar accesibles desde el dashboard o áreas operativas, sin obligar a ingresar al asistente de configuración.

## 3. Inventario de vistas

| Código visual | Vista | Estado de definición |
|---|---|---|
| WF-CFG-01 | Inicio de configuración | Mockup V3 en revisión |
| WF-CFG-02 | Seleccionar modelo operativo | Mockup V3 en revisión |
| WF-CFG-03 | Configurar aprobación | Tres mockups V3 en revisión |
| WF-CFG-04 | Configurar pagos | Mockups V3 en definición |
| WF-CFG-05 | Configurar seña | Mockups V3 en definición |
| WF-CFG-06 | Impresoras y capacidades | Mockups V3 aprobados para revisión |
| WF-CFG-07 | Método de asignación | Mockups V3 aprobados para revisión |
| WF-CFG-08 | Horarios operativos y tiempos estimados | Mockup V3 aprobado para revisión |
| WF-CFG-09 | Entrega, puntos y envíos | Mockup V3 aprobado para revisión |
| WF-CFG-10 | Resumen y simulación | Lista para mockup |
| WF-CFG-11 | Conflictos de configuración | Lista para mockup |
| WF-CFG-12 | Activar o programar | Lista para mockup |
| WF-CFG-13 | Verificación de seguridad | Lista para mockup |
| WF-CFG-14 | Confirmación de activación | Lista para mockup |
| WF-CFG-15 | Historial de versiones | Lista para mockup |
| WF-CFG-16 | Detalle de versión | Lista para mockup |
| WF-OPE-01 | Confirmar pausa | Lista para mockup |
| WF-OPE-02 | Sistema pausado | Lista para mockup |
| WF-OPE-03 | Confirmar reanudación | Lista para mockup |
| WF-OPE-04 | Registrar recarga de papel | Mockup operativo V3 aprobado para revisión |
| WF-OPE-05 | Seleccionar impresora compatible | Mockup operativo V3 aprobado para revisión |
| WF-OPE-06 | Disponibilidad temporal de puntos | Definición lista para mockup operativo |
| WF-COT-01 | Aviso de inactividad | Lista para mockup cliente |
| WF-COT-02 | Cuenta regresiva | Lista para mockup cliente |
| WF-COT-03 | Bloqueo por pausa | Lista para mockup cliente |
| WF-COT-04 | Aceptar nueva fecha | Lista para mockup cliente |
| WF-CC-01 | Clientes con cuenta corriente | Exploratoria |
| WF-CC-02 | Detalle de deuda | Exploratoria |
| WF-CC-03 | Revisar pedido a cuenta | Exploratoria |
| WF-CC-04 | Definir fecha manual | Exploratoria |

## 4. WF-CFG-01 - Inicio de configuración

### Objetivo

Mostrar el estado actual sin obligar al administrador a editar.

### Contenido

- configuración vigente: nombre, versión, estado, fecha de vigencia y modelo operativo;
- próxima configuración: ausencia de programación o versión programada con fecha, hora y zona horaria;
- estado de edición: sin borrador o borrador en preparación con base, fecha de creación y último guardado;
- resumen operativo, económico y de recursos dinámicos de la versión activa;
- acceso a historial y detalle;
- acciones habilitadas según el cambio pendiente existente.

### Variantes obligatorias

#### CFG-001 - Sin cambio pendiente

- existe una única versión activa;
- no existe borrador;
- no existe versión programada;
- se habilita Editar configuración, usar una versión histórica o restaurar valores predeterminados.

#### CFG-001B - Borrador en curso

- la versión activa continúa vigente;
- no existe versión programada;
- se muestran base, responsable, fecha de creación, último guardado y paso alcanzado;
- se habilita Ver cambios, Editar borrador y Cancelar borrador;
- se bloquea la creación de otro borrador desde cualquier base.

#### CFG-001C - Versión programada

- la versión activa continúa vigente hasta la fecha futura;
- se muestra la versión programada, fecha, hora, zona horaria y modelo;
- no existe borrador;
- se habilita Ver detalle y Cancelar programación;
- se bloquea toda edición hasta la activación o cancelación.

### Acciones

- editar a partir de la activa cuando no existe cambio pendiente;
- continuar o cancelar el único borrador existente;
- elegir otra base únicamente cuando no existe cambio pendiente;
- consultar detalle;
- cancelar programación;
- ir al historial.

La interfaz no debe denominar borrador a una versión programada. El borrador es editable y no tiene vigencia; la versión programada ya fue confirmada, es inmutable y ocupa el único cambio pendiente permitido.

## 5. WF-CFG-02 - Seleccionar modelo operativo

La vista utiliza tarjetas comparables y se representa mediante un único mockup `MC-ADM-CFG-002`.

Cada tarjeta muestra:

- nombre;
- descripción;
- aprobación;
- momento de pago;
- intervención humana;
- dependencias aplicables;
- recomendación de uso;
- etiqueta Disponible o No disponible cuando corresponda.

No debe mostrar combinaciones técnicas crudas.

Modelos de esta versión:

- Control manual;
- Control condicional;
- Automatización certificada, visible como evolución futura y deshabilitada.

Control manual exige revisión, aprobación o rechazo humano para todos los pedidos. Control condicional habilita la configuración de una condición certificada en `WF-CFG-03`.

## 6. WF-CFG-03 - Configurar aprobación

Esta vista se habilita únicamente al seleccionar Control condicional. Se representa mediante tres mockups:

- `MC-ADM-CFG-003A`: aprobación por pago previo;
- `MC-ADM-CFG-003B`: aprobación por pago de seña;
- `MC-ADM-CFG-003C`: aprobación por monto total del pedido.

Cada alternativa muestra la condición, el recorrido completo y los bloqueos aplicables.

Reglas visibles:

- pago previo exige acreditación total antes de habilitar la carga del archivo;
- pago de seña exige acreditar la seña configurada antes de habilitar la carga del archivo;
- mientras falte pago o seña previa, el archivo permanece del lado del cliente y no utiliza almacenamiento ni procesamiento del servidor;
- el criterio por monto permite aprobación automática hasta un umbral monetario que se parametriza en Fase 3;
- cuando el pedido supera el umbral, la Fase 3 exige una seña previa configurable antes de continuar;
- superar el umbral no deriva automáticamente a revisión humana ni implica rechazo automático;
- los montos, porcentajes y medios se completan en la Fase 3;
- los estados internos no se editan y el backend conserva la decisión final.

## 7. WF-CFG-04 - Configurar pagos

La vista debe comenzar mostrando el **modelo heredado de Fase 2** y explicar que las opciones incompatibles permanecen visibles pero deshabilitadas.

### Matriz de medios por modelo

#### Control manual

- transferencia: disponible;
- efectivo: disponible;
- pago digital: disponible;
- la aprobación continúa siendo humana;
- la seña, si se configura, funciona como regla financiera opcional.

#### Control condicional - Pago previo

- transferencia: disponible si puede acreditarse;
- efectivo: deshabilitado para cumplir la condición;
- pago digital: disponible;
- debe existir al menos un medio acreditable;
- el archivo permanece del lado del cliente hasta acreditar el total;
- la seña no aplica.

#### Control condicional - Pago de seña

- transferencia: disponible para acreditar la seña;
- efectivo: deshabilitado para acreditar la seña previa;
- pago digital: disponible para acreditar la seña;
- el archivo permanece del lado del cliente hasta acreditar la seña;
- la condición de seña viene fijada por Fase 2.

#### Control condicional - Monto del pedido

- transferencia: disponible;
- efectivo: disponible como medio general para pedidos dentro del umbral y para el saldo posterior cuando corresponda;
- pago digital: disponible;
- la aprobación hasta el umbral es automática;
- para pedidos superiores al umbral, la seña previa debe acreditarse mediante transferencia, pago digital u otro medio acreditable;
- efectivo no puede utilizarse para acreditar esa seña previa.

La interfaz debe diferenciar **medios generales habilitados** de **medios válidos para acreditar una condición previa**.

## 8. WF-CFG-05 - Configurar seña

Los controles dependen del modelo heredado.

### Control manual

- habilitar seña: opcional;
- tipo: porcentaje o monto fijo;
- valor: editable;
- condición de aplicación: configurable dentro de parámetros permitidos;
- la seña no aprueba el pedido y no reemplaza la revisión humana.

### Pago previo

- seña: no aplica;
- controles visibles pero deshabilitados;
- explicación: el modelo exige pago total previo.

### Pago de seña

- la condición `pago de seña` está definida en Fase 2 y no puede apagarse ni sustituirse;
- no debe existir un toggle para deshabilitar la seña;
- la condición se muestra bloqueada;
- tipo de seña: editable, por ejemplo porcentaje o monto fijo;
- valor de la seña: editable;
- hasta acreditarse la seña, el archivo permanece del lado del cliente.

La referencia histórica de 30 % desde 200 carillas no constituye una condición fija de este modelo. Puede utilizarse como ejemplo o valor predeterminado, pero no debe imponerse en la interfaz.

### Monto del pedido

- condición: `por monto del pedido`, heredada de Fase 2 y bloqueada;
- umbral de autoaprobación: input monetario editable;
- pedidos hasta el umbral: pueden aprobarse automáticamente y no requieren seña previa;
- pedidos superiores al umbral: se exige una seña previa configurable;
- tipo de seña: porcentaje o monto fijo;
- valor de seña: editable;
- medios para acreditar la seña: transferencia, pago digital u otros acreditables;
- efectivo puede mantenerse como medio general, pero no acredita la seña previa;
- una vez acreditada la seña, el saldo restante conserva los medios generales habilitados.

### Presentación común

Cada variante debe incluir:

- ejemplo operativo calculado;
- explicación de qué campos provienen de Fase 2 y cuáles son editables;
- simulación visual del recorrido desde Fase 1 → Fase 2 → Fase 3 → resultado.

## 9. WF-CFG-06 - Impresoras y capacidades

### Objetivo

Permitir que el ADMIN_ADMIN identifique cada impresora, declare qué trabajos puede realizar y configure la capacidad necesaria para que el sistema calcule compatibilidad y disponibilidad estimada de papel.

### Listado

Cada impresora debe mostrar como mínimo:

- nombre visible;
- estado: Operativa o Deshabilitada;
- formatos admitidos;
- B/N o color;
- dúplex cuando corresponda;
- capacidad máxima de hojas;
- hojas disponibles estimadas;
- porcentaje estimado disponible;
- contador histórico de hojas impresas;
- acción Deshabilitar cuando está Operativa;
- acción Editar únicamente cuando está Deshabilitada.

La identidad técnica es estable y no depende del nombre visible.

### Estados y acciones

#### Operativa

- puede recibir trabajos;
- no puede editarse;
- no puede eliminarse;
- puede Deshabilitarse.

#### Deshabilitada

- no recibe nuevos trabajos;
- puede editarse;
- puede volver a habilitarse posteriormente;
- dentro de Editar aparece al final la acción Eliminar impresora.

La opción Eliminar no debe aparecer en el listado general ni para una impresora Operativa.

### Alta y edición

La edición debe contemplar:

- nombre visible;
- formatos compatibles;
- capacidad B/N o color;
- dúplex cuando aplique;
- capacidad máxima de hojas;
- explicación de que la disponibilidad de papel es estimada;
- contador histórico visible pero no editable.

La capacidad máxima de hojas se carga manualmente y actúa como base constante para el cálculo de disponibilidad.

### Disponibilidad estimada de papel

La interfaz debe separar:

- **contador histórico de hojas impresas**, acumulado y no reiniciable por recarga;
- **disponibilidad actual estimada**, expresada como `hojas disponibles / capacidad máxima` y porcentaje.

Ejemplo:

`420 / 500 hojas · 84 % estimado`

Cada trabajo efectivamente procesado descuenta la cantidad de hojas calculada para ese trabajo.

Debe evitarse presentar esta métrica como lectura física de sensor.

## 10. WF-CFG-07 - Método de asignación

### V1

La opción predeterminada y disponible es:

- **Asignación manual**.

El sistema determina compatibilidad automáticamente y puede recomendar una impresora, pero el usuario interno decide la asignación final.

La opción:

- **Asignación automática**

debe permanecer visible, explicada y deshabilitada como evolución futura fuera de V1.

### Compatibilidad

El sistema conoce las características del trabajo y compara al menos:

- formato de hoja requerido;
- necesidad de B/N o color;
- dúplex u otras capacidades configuradas cuando correspondan;
- estado Operativa de la impresora.

Una impresora incompatible permanece visible cuando sea útil para explicar la decisión, pero no puede seleccionarse y debe mostrar la causa.

Ejemplos:

- `No compatible · el trabajo requiere color y esta impresora solo imprime B/N`;
- `No compatible · el formato A3 no está admitido`;
- `No disponible · impresora deshabilitada`.

### Recomendación en V1

Entre las impresoras compatibles, el sistema puede marcar una como `Recomendada` considerando:

- compatibilidad técnica;
- disponibilidad estimada de papel;
- capacidad suficiente para el trabajo;
- estado operativo.

Si una impresora es técnicamente compatible pero no posee suficiente papel estimado para completar el trabajo, debe mostrar una advertencia y no debería ser la recomendada.

La recomendación no constituye asignación automática.

### Evolución futura

Una futura asignación automática certificada podrá considerar además:

- características del trabajo;
- capacidad;
- carga de trabajos;
- páginas u hojas pendientes;
- disponibilidad de papel;
- estado operativo;
- otros criterios certificados.

No se define todavía el algoritmo ni la ponderación. Esta opción permanece fuera de V1.

## 11. WF-CFG-08 - Horarios operativos y tiempos estimados

### Objetivo

Definir cuándo corre el reloj productivo y cuánto tiempo estimado se compromete la imprenta para preparar un pedido para retiro o envío.

### Días y horarios

Cada día de la semana debe aparecer por separado con:

- estado habilitado/cerrado;
- input de hora de apertura;
- input de hora de cierre.

No deben agruparse lunes a viernes en un único control, porque cada día puede requerir una ventana distinta.

Ejemplo:

- lunes a viernes: configurables individualmente, `09:00 a 18:00`;
- sábado: `10:00 a 12:00`;
- domingo: cerrado.

### Tiempos estimados

Inputs configurables expresados en horas:

- `Preparación en Casa Central`;
- `Preparación para envío`.

El primero representa el compromiso estimado para dejar el pedido listo para retiro en Casa Central.

El segundo representa el compromiso estimado para preparar o liberar el pedido para la etapa de envío; no garantiza por sí solo el tiempo de transporte de un tercero.

### Regla de cómputo

- los tiempos se consumen únicamente dentro de los horarios operativos configurados;
- si el pedido llega fuera de horario, puede aceptarse y queda En cola;
- el cómputo comienza en la próxima apertura comercial;
- si el tiempo cruza el cierre, el remanente continúa en la siguiente apertura;
- el estimado no obliga a esperar: si el pedido queda listo antes, el flujo avanza inmediatamente.

Ejemplo:

`Pedido recibido 02:00 → Próxima apertura 08:00 → Preparación 4 hs → Listo estimado 12:00`.

## 12. WF-CFG-09 - Entrega, puntos y envíos

### Modalidades

La vista presenta las modalidades utilizables:

- **Retiro en local**, según horario operativo;
- **Puntos de entrega**, según disponibilidad y franjas horarias;
- **Envío**, según el compromiso estimado configurado.

Solo se muestran al cliente opciones realmente disponibles.

### Administrar puntos

Debe existir la acción **Administrar puntos**.

Conduce a un panel dedicado para:

- consultar puntos existentes;
- agregar un punto;
- editar nombre, ubicación y datos habituales;
- configurar días y franjas horarias cuando corresponda;
- consultar su estado operativo.

La disponibilidad temporal del punto se mantiene separada del versionado.

### Franjas de los puntos

Cada punto puede definir ventanas de atención o entrega, por ejemplo:

`Facultad de Medicina · Lunes a viernes · 17:00 a 21:00`.

Las franjas son ventanas estimadas, no una cita exacta. La coordinación fina puede resolverse por WhatsApp u otro canal, mientras la aplicación mantiene estados como En camino o Listo para retirar.

La estimación ofrecida al cliente debe respetar la siguiente franja válida del punto y nunca prometer un retiro fuera de ella.

### Simulación de Fase 5

La vista debe conservar el patrón narrativo utilizado en las fases anteriores:

`Pedido recibido → Fuera de horario / En cola → Próxima apertura → En preparación → Listo en Casa Central → Disponible en punto según franja`.

Si el pedido queda listo antes del estimado, el timeline avanza inmediatamente y el cliente puede ser notificado.

La especificación detallada se encuentra en `WF-FASE-5-HORARIOS-PUNTOS-ENVIOS.md`.

## 13. WF-CFG-10 - Resumen y simulación

Debe ser una vista narrativa.

Ejemplo:

Un pedido compatible ingresa. Se aplican el modelo operativo y la configuración financiera capturados. Si existe una condición económica previa, el archivo no se habilita hasta que esa condición quede acreditada. Después puede continuar hacia las reglas de producción, la selección de una impresora compatible y el cálculo de disponibilidad según horarios y modalidad de entrega.

Secciones:

- cambios respecto de la versión activa;
- recorrido resultante;
- ejemplo estándar;
- ejemplo con pago previo;
- ejemplo con seña;
- ejemplo por monto dentro y fuera del umbral;
- ejemplo de trabajo con impresoras compatibles, incompatibles y una recomendada;
- disponibilidad estimada de papel de las impresoras compatibles;
- ejemplo de pedido recibido fuera de horario y en cola;
- siguiente apertura operativa;
- tiempo estimado para retiro o envío;
- disponibilidad en punto según franja horaria;
- ejemplo de finalización anticipada;
- ejemplo con error;
- advertencias.

La definición comercial de módulos no forma parte del resumen de Fase 5 hasta que exista un catálogo aprobado.

## 14. WF-CFG-11 - Conflictos

Formato recomendado:

| Conflicto | Por qué ocurre | Cómo resolver |
|---|---|---|
| Pago previo sin medio acreditable | No existe forma habilitada de completar la condición | Activar transferencia o pago digital |
| Pago previo con seña adicional | El modelo ya exige el 100 % previo | Deshabilitar la seña o elegir otro modelo |
| Pago de seña con efectivo como única opción | El efectivo no puede acreditar una seña previa | Activar transferencia o pago digital |
| Monto superior con seña sin medio acreditable | No existe forma de registrar la seña previa | Activar transferencia o pago digital |
| Impresora incompatible | El trabajo requiere un formato o capacidad que el equipo no posee | Seleccionar otra impresora compatible |
| Papel estimado insuficiente | La impresora no tendría hojas suficientes para completar el trabajo | Recargar papel o seleccionar otra impresora |
| Intento de editar impresora Operativa | Las capacidades no pueden cambiar mientras el equipo está habilitado para recibir trabajos | Deshabilitar la impresora antes de editar |
| Asignación automática en V1 | La automatización todavía no dispone de un modelo certificado | Mantener Asignación manual |
| Horario operativo inválido | La apertura no es anterior al cierre o falta una hora requerida | Corregir la ventana del día |
| Tiempo estimado inválido | El compromiso de preparación no posee un valor válido mayor que cero | Completar el tiempo en horas |
| Punto sin franja válida | La ventana configurada no permite ofrecer el retiro correctamente | Corregir días y franja del punto |
| Modalidad sin disponibilidad real | La opción no puede cumplirse con la configuración o estado operativo actual | Revisar horario, punto o modalidad |

No debe utilizar mensajes genéricos.

Los conflictos asociados a módulos se definirán cuando se cierre la estrategia comercial de planes.

## 15. WF-CFG-12 - Activar o programar

Dos alternativas principales:

### Activar ahora

Texto: Se aplicará a la siguiente cotización confirmada después de la activación.

### Programar

Controles:

- fecha;
- hora;
- zona horaria visible;
- resumen de vigencia;
- versión actual hasta ese momento.

## 16. WF-CFG-13 - Verificación de seguridad

Paso separado con:

- resumen final;
- impacto;
- reingreso de contraseña;
- código adicional cuando corresponda;
- cancelar;
- confirmar.

No debe permitir volver a editar después del código sin reiniciar la validación.

## 17. WF-CFG-14 - Confirmación

Muestra:

- versión creada;
- fecha y hora;
- activación inmediata o programada;
- usuario;
- acceso al detalle;
- volver al inicio.

## 18. WF-CFG-15 y 16 - Historial

Listado:

- versión;
- estado;
- vigencia;
- autor;
- modelo;
- resumen;
- ver detalle.

Detalle:

- comparación;
- auditoría;
- impresoras y asignación;
- horarios y tiempos estimados;
- definición de puntos y modalidades;
- parámetros;
- opción Usar como base.

No existe Editar versión.

## 19. WF-OPE-01 - Pausar recepción

Disponible para ADMIN_ADMIN y empleado.

Modal:

- título de advertencia;
- impacto;
- selector de motivo;
- observación;
- contraseña;
- cancelar;
- Pausar recepción.

El botón debe expresar la acción completa.

## 20. WF-OPE-02 - Estado pausado

Elementos:

- banner persistente;
- motivo interno;
- usuario que pausó;
- hora;
- pedidos existentes sin cambios;
- botón Reanudar;
- vista previa del mensaje público.

## 21. WF-OPE-03 - Reanudar

Modal:

- resumen de pausa;
- contraseña;
- advertencia sobre cotizaciones todavía vigentes;
- botón Reanudar recepción.

## 22. WF-OPE-04 - Registrar recarga de papel

Debe estar disponible desde la gestión operativa de la impresora sin obligar a editar su configuración.

Contenido:

- impresora;
- disponibilidad actual estimada;
- capacidad máxima configurada;
- cantidad faltante calculada;
- texto que indique que la recarga debe completar físicamente el faltante hasta el máximo;
- cancelar;
- Confirmar recarga.

Ejemplo:

`Disponibilidad estimada: 468 / 500 hojas`

`Para sincronizar el sistema, completá físicamente las 32 hojas faltantes antes de confirmar.`

Al confirmar:

- disponibilidad estimada → `500 / 500`;
- porcentaje → `100 %`;
- contador histórico de hojas impresas → sin cambios;
- registrar usuario y momento.

La acción no admite una recarga parcial en V1.

## 23. WF-OPE-05 - Seleccionar impresora compatible

Para un trabajo concreto debe mostrar:

- resumen del trabajo: formato, B/N o color, dúplex cuando corresponda y hojas calculadas;
- impresoras compatibles seleccionables;
- impresoras incompatibles con causa visible;
- disponibilidad estimada en hojas y porcentaje;
- advertencia de papel insuficiente cuando corresponda;
- etiqueta Recomendada en una impresora compatible cuando el sistema pueda sugerirla;
- confirmación de la elección manual.

En V1 no existe botón ni acción de asignación automática.

## 24. WF-OPE-06 - Disponibilidad temporal de puntos

Debe estar accesible desde el dashboard o el panel de administración de puntos sin obligar a crear una nueva versión de configuración.

Cada punto muestra como mínimo:

- nombre;
- estado Habilitado o Deshabilitado;
- días y franja habitual;
- acción Habilitar o Deshabilitar según estado.

Reglas:

- no se solicita motivo;
- no se exige fecha de fin;
- un punto deshabilitado deja de ofrecerse a nuevos pedidos;
- los pedidos ya pactados conservan el punto y no se reasignan automáticamente;
- el punto se habilita nuevamente de forma manual;
- mientras exista al menos un punto deshabilitado, el dashboard muestra un recordatorio persistente con acceso a la gestión.

Ejemplo de aviso:

`⚠ Hay 2 puntos de entrega deshabilitados · Ver puntos`.

## 25. Impacto en cliente

### WF-COT-01 - Aviso de actividad

- título: ¿Todavía estás ahí?;
- explicación de seguridad;
- contador de sesenta segundos;
- botón Continuar;
- cierre automático si no responde.

### WF-COT-02 - Cuenta regresiva final

- contador visible de diez minutos;
- no se reinicia;
- advertencia de pérdida de cotización;
- botón Crear pedido.

### WF-COT-03 - Pausa al confirmar

- comunica que el pedido no fue creado;
- conserva resumen visible;
- impide confirmar;
- informa que el tiempo continúa.

### WF-COT-04 - Nueva fecha

- fecha anterior;
- fecha actualizada;
- precio sin cambios;
- condiciones sin cambios;
- Aceptar y continuar;
- Cancelar.

### Timeline de horarios y entrega

Para Fase 5, el timeline del cliente debe poder mostrar:

- Pedido recibido;
- En cola cuando está fuera del horario operativo;
- próxima apertura o inicio estimado cuando aporte claridad;
- En preparación;
- En camino cuando corresponda;
- Listo para retirar;
- Entregado.

Si el pedido queda listo antes del estimado, el nuevo estado se refleja inmediatamente y puede generar una notificación al cliente.

## 26. Cuenta corriente - Vistas exploratorias

### WF-CC-01 - Listado

- cliente;
- estado;
- límite;
- deuda;
- pedidos pendientes;
- revisar.

### WF-CC-02 - Detalle

- deuda actual y proyectada;
- historial;
- pagos;
- pedidos;
- carga productiva;
- suspender cuenta.

### WF-CC-03 - Revisar pedido

- resumen del pedido;
- recursos;
- deuda;
- fechas comprometidas;
- aprobar, rechazar o dejar pendiente.

### WF-CC-04 - Fecha manual

- calendario;
- carga diaria;
- fecha propuesta;
- observaciones;
- confirmar decisión.

Estas vistas deben incluir una marca visible de Propuesta a revisión.

## 27. Flujo principal del administrador

Inicio de configuración
→ seleccionar modelo
→ completar aprobación y pagos
→ configurar impresoras y asignación
→ configurar horarios, tiempos, puntos y envíos
→ validar
→ resolver conflictos
→ previsualizar
→ elegir vigencia
→ verificar identidad
→ activar o programar
→ consultar confirmación e historial.

## 28. Permisos visuales

| Función | ADMIN_ADMIN | Empleado |
|---|---:|---:|
| Acceder a Configuración | Sí | No |
| Crear versión | Sí | No |
| Configurar impresoras y capacidades | Sí | No |
| Configurar horarios y tiempos | Sí | No |
| Agregar o editar definición de puntos | Sí | No, salvo permiso futuro específico |
| Activar o programar | Sí | No |
| Consultar historial completo | Sí | No |
| Pausar | Sí | Sí |
| Reanudar | Sí | Sí |
| Registrar recarga de papel | Sí | Sí, si está autorizado |
| Seleccionar impresora para un trabajo | Sí | Sí, si está autorizado |
| Habilitar o deshabilitar punto | Sí | Sí, si está autorizado |
| Administrar cuenta corriente | Sí | No |

La autorización real debe validarse en backend.

## 29. Registro de cambios y justificación

| Vista incorporada | Wireflow anterior | Justificación por el motor de configuración | Estado |
|---|---|---|---|
| Inicio de configuración | No existía como área central | El motor necesita mostrar versión y vigencia | Lista para mockup |
| Variantes de CFG-001 | No se distinguía borrador de programación | Hace visible la exclusión entre edición y activación futura | Mockup V3 en revisión |
| Selección por modelos | Configuración limitada | Evita construir reglas libres | Mockup V3 en revisión |
| Pagos y seña heredados de Fase 2 | Las restricciones financieras se trataban de forma general | La Fase 3 debe mostrar únicamente combinaciones compatibles con el modelo elegido | Mockups V3 en definición |
| Umbral monetario con seña escalonada | Superar el umbral implicaba revisión humana | Se redefine como flujo flexible: autoaprobación bajo umbral y seña acreditable por encima | Mockup V3 en definición |
| Impresoras con ciclo Operativa/Deshabilitada | La vista solo enumeraba datos generales | Evita editar equipos habilitados y concentra eliminación en edición segura | Mockups V3 aprobados para revisión |
| Compatibilidad y recomendación | No se explicaba cómo se elegía una impresora | El sistema filtra por formato/color y asiste la selección manual | Mockup operativo V3 aprobado para revisión |
| Disponibilidad estimada de papel | Capacidad y disponibilidad no tenían modelo operativo | Permite estimar continuidad de trabajo desde fuera de la oficina | Mockups V3 aprobados para revisión |
| Recarga manual de papel | No existía evento de sincronización | Permite restablecer el estimado al 100 % después de completar físicamente el faltante | Mockup operativo V3 aprobado para revisión |
| Asignación automática futura | Se mencionaba como opción cuando estuviera certificada | En V1 queda explícitamente visible pero deshabilitada | Representada como opción futura |
| Fase 5 - horarios por día | Se agrupaban horarios y módulos sin un criterio temporal completo | Define cuándo corre el reloj productivo y permite días diferentes | Mockup V3 aprobado para revisión |
| Fase 5 - tiempos en horas | Entrega no tenía compromiso temporal configurable | Permite calcular retiro y preparación para envío | Mockup V3 aprobado para revisión |
| Fase 5 - Administrar puntos | Los puntos estaban resumidos sin gestión dedicada | Separa la definición estructural de la vista principal | Mockup V3 aprobado para revisión |
| Puntos con franjas horarias | No existían ventanas propias por ubicación | Evita prometer retiro fuera de la disponibilidad habitual | Definición confirmada |
| Disponibilidad temporal de puntos | Una contingencia podía confundirse con una nueva configuración | Habilitar/deshabilitar es operativo, inmediato y sin motivo | Lista para mockup operativo |
| Recordatorio de puntos deshabilitados | No existía prevención contra bajas olvidadas | El dashboard mantiene visible la contingencia | Lista para mockup operativo |
| Timeline de Fase 5 | La estimación no mostraba el efecto del horario | Visualiza cola, próxima apertura, preparación y disponibilidad | Mockup V3 aprobado para revisión |
| Módulos fuera de Fase 5 | El wireflow los trataba como contenido inmediato | Los planes y catálogo todavía no están definidos | Futuro |
| Resumen y simulación | Sin previsualización integral | Permite comprender consecuencias | Lista para mockup |
| Activar o programar | Sin flujo temporal | Define cuándo aplican los cambios | Lista para mockup |
| Verificación reforzada | Acciones administrativas generales | Protege cambios operativos y financieros | Lista para mockup |
| Historial de versiones | Auditoría centrada en pedidos | El motor requiere trazabilidad propia | Lista para mockup |
| Pausa operativa | No priorizada | Atiende emergencias reales | Lista para mockup |
| Temporizadores cliente | Cotización sin caducidad detallada | Protege sesión económica y temporales | Lista para mockup |
| Cuenta corriente | No formalizada | Explora excepción financiera controlada | Requiere revisión |

## 30. Referencias para integración

Este wireflow se justifica por los cambios del motor de configuración y debe contrastarse con:

- diseño/Front/ux-ui/wireflows/WF-ADMINISTRADOR-MVP.md
- marco-del-proyecto/actualizacion-a-revision-producto-configurable.md
- marco-del-proyecto/actualizacion-a-revision-motor-configuracion-versionada.md
- analisis/historias-de-usuarios/actualizacion-a-revision-historias-producto-configurable.md
- analisis/casos-de-uso/09-configuracion-del-sistema/actualizacion-a-revision-casos-de-uso-configuracion.md
- analisis/casos-de-uso/10-disponibilidad-operativa/actualizacion-a-revision-casos-de-uso-disponibilidad.md
- diseño/Front/ux-ui/wireflows/WF-FASE-4-IMPRESORAS-ACCESOS-OPERATIVOS.md
- diseño/Front/ux-ui/wireflows/WF-FASE-5-HORARIOS-PUNTOS-ENVIOS.md

### Evidencia V3 de Fase 4

- [MC-ADM-CFG-006 - Impresoras y capacidades](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-006-impresoras-capacidades.png)
- [MC-ADM-CFG-007 - Editar impresora](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-007-editar-impresora.png)
- [MC-ADM-OPE-004 - Registrar recarga de papel](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-OPE-004-registrar-recarga-papel.png)
- [MC-ADM-OPE-005 - Asignar impresora](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-OPE-005-asignar-impresora.png)

### Evidencia de Fase 5

- [MC-ADM-CFG-008-009 - Horarios, puntos y envíos](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-008-009-horarios-puntos-envios.png)
- [MC-ADM-CFG-009 - Administrar puntos de entrega](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-009-administrar-puntos-entrega.png)

Ambas imágenes corresponden a la evidencia final confirmada el 10/09/2026. La primera representa la configuración temporal y la simulación de punta a punta; la segunda centraliza la definición y consulta de los puntos de entrega. No deben sustituirse por pruebas visuales anteriores.
