# Actualización a revisión - Producto configurable

| Campo | Valor |
|---|---|
| Versión | 2.3 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-10 |
| Responsables de revisión | Agustín Tejero y Alejandro Herms |
| Propósito | Servir como insumo para la integración posterior en la documentación oficial |

> Este documento no reemplaza ni modifica la documentación vigente. Reúne decisiones y propuestas surgidas del análisis del producto configurable. Agustín deberá revisarlo, contrastarlo y definir su integración definitiva.

## 1. Objetivo

Definir la evolución de La Montaña desde el alcance histórico del MVP hacia un producto comercial modular y configurable para imprentas.

El producto debe adaptarse a distintas formas de trabajo sin imponer como universales las decisiones operativas tomadas durante el MVP. La configuración debe mantenerse dentro de estructuras certificadas y coherentes, evitando un constructor libre de reglas que permita combinaciones inseguras o contradictorias.

## 2. Visión del producto

La Montaña será una plataforma modular para imprentas que permitirá:

- seleccionar un modelo operativo certificado;
- ajustar parámetros permitidos dentro de ese modelo;
- configurar aprobación, cobro, seña, impresoras, horarios y entrega;
- activar los cambios inmediatamente o programarlos;
- mantener trazabilidad de cada modificación;
- aplicar cada nueva configuración solamente a trabajos nuevos;
- preservar los estados internos fijos del pedido;
- adaptar la experiencia sin comprometer seguridad, integridad ni auditoría;
- incorporar mejoras opcionales de forma progresiva sin impedir el flujo básico del negocio.

La definición comercial de módulos y su distribución entre planes queda pendiente y fuera del alcance actual de Fase 5.

## 3. Principios confirmados

### 3.1 Estados internos fijos

Cada imprenta utilizará el mismo conjunto de estados internos y el mismo recorrido general en tiempo real.

No se configura qué estados existen. Se configura cuándo y bajo qué condiciones un trabajo puede avanzar entre ellos.

### 3.2 Configuraciones certificadas

El administrador no construirá reglas arbitrarias combinando cualquier criterio.

El sistema ofrecerá modelos operativos previamente definidos y validados. Dentro de cada modelo se habilitarán parámetros acotados y compatibles.

### 3.3 Configuración global

La configuración activa se aplica a todos los clientes y trabajos nuevos de la imprenta.

Las excepciones deben estar expresamente modeladas y auditadas. No deben existir modificaciones informales por pedido que eviten las reglas de seguridad.

### 3.4 Versionado y no retroactividad

Cada activación genera una versión inmutable.

Los trabajos anteriores conservan las condiciones con las que fueron cotizados. Una configuración nueva no modifica pedidos ni cotizaciones anteriores.

### 3.5 Modularidad comercial - Pendiente

La estrategia de módulos permanece como evolución futura.

Principios aceptados:

- cada módulo podrá representarse mediante un estado activo/inactivo;
- la composición de los planes Gratis, Inicial y Avanzado todavía no está definida;
- una mejora opcional no debe impedir completar el flujo básico del negocio;
- Fase 5 no diseña ni decide el catálogo de módulos.

### 3.6 Experiencia visual

La configuración debe ser visual, guiada e intuitiva.

La interfaz debe explicar consecuencias y mostrar ejemplos. No debe reducirse a formularios extensos con términos técnicos o reglas aisladas.

### 3.7 Seguridad y auditoría

Toda configuración sensible se valida en backend y queda auditada.

La interfaz puede ocultar o deshabilitar acciones según el rol, pero la seguridad no puede depender del frontend.

## 4. Roles considerados en esta etapa

### 4.1 Administrador de máximo nivel de la imprenta

Nombre funcional utilizado durante el análisis: ADMIN_ADMIN.

Es la autoridad habilitada para:

- modificar configuraciones;
- activar cambios inmediatamente;
- programar cambios;
- cancelar cambios programados;
- configurar pagos y señas;
- configurar impresoras;
- configurar horarios operativos y tiempos estimados;
- administrar la definición de puntos de entrega;
- administrar clientes con cuenta corriente;
- consultar el historial completo de configuración.

El nombre formal del rol deberá validarse durante la integración definitiva para diferenciarlo del superadministrador de la plataforma.

### 4.2 Empleado de imprenta

En esta etapa no se definen todavía todos los permisos operativos finos para empleados.

Como capacidades confirmadas o autorizables para usuarios internos se contemplan:

- pausar o reanudar la recepción mediante reconfirmación de contraseña;
- registrar una recarga física de papel cuando el rol operativo lo permita;
- seleccionar manualmente una impresora compatible para un trabajo cuando el rol operativo lo permita;
- habilitar o deshabilitar temporalmente un punto de entrega cuando el rol operativo lo permita.

La autorización real de estas acciones debe validarse en backend.

### 4.3 Cliente

Puede cotizar, confirmar y consultar sus pedidos según la configuración vigente de la imprenta.

No puede modificar reglas, estados internos, configuraciones ni condiciones administrativas.

## 5. Capacidades configurables

La versión de configuración podrá contemplar:

- aprobación manual, automática o condicional;
- momento en que se habilita la producción;
- medios de pago disponibles;
- pago previo, pago al entregar u otros modelos certificados;
- reglas de seña;
- impresoras habilitadas;
- capacidades de las impresoras;
- asignación manual en V1;
- asignación automática certificada como evolución futura;
- horarios operativos por día;
- tiempos estimados expresados en horas;
- retiro en Casa Central;
- puntos y franjas habituales de entrega;
- preparación o liberación para envío;
- notificaciones;
- parámetros de servicios disponibles.

### 5.1 Matriz financiera consolidada para Fase 3

La configuración de pagos y seña depende del modelo elegido previamente:

- **Control manual:** admite medios flexibles, incluido efectivo, transferencia y pago digital. La seña puede configurarse como regla opcional y nunca reemplaza la aprobación humana.
- **Pago previo:** exige el pago total mediante un medio acreditable antes de habilitar la carga. Efectivo no satisface la condición previa y la seña no aplica.
- **Pago de seña:** la condición viene fijada desde Fase 2. En Fase 3 se configuran tipo y valor; la seña debe acreditarse por un medio compatible antes de habilitar la carga.
- **Monto del pedido:** funciona como modalidad flexible. Hasta un umbral monetario configurable puede existir autoaprobación y efectivo. Si el pedido supera el umbral, se exige una seña previa configurable y acreditable. El saldo restante puede conservar medios flexibles, incluido efectivo cuando corresponda.

La regla histórica de **30 % desde 200 carillas** no constituye una condición fija del producto configurable. Puede conservarse como antecedente o valor predeterminado, pero el modelo y los valores deben quedar parametrizados.

### 5.2 Impresoras, capacidades y asignación consolidada para Fase 4

La Fase 4 separa configuración de capacidades de operación cotidiana.

Cada impresora debe mantener una identidad técnica estable y un nombre visible editable. En la configuración se declaran como mínimo:

- formatos de hoja admitidos;
- capacidad de impresión B/N o color;
- dúplex cuando corresponda;
- capacidad máxima de hojas;
- estado operativo.

Los únicos estados de la impresora para V1 son:

- **Operativa:** puede recibir trabajos y no puede editarse ni eliminarse;
- **Deshabilitada:** no recibe nuevos trabajos y puede editarse.

Para modificar una impresora debe deshabilitarse primero. La acción **Eliminar impresora** aparece únicamente dentro de la edición de una impresora Deshabilitada. La eliminación debe retirar el equipo de la administración operativa sin romper la referencia histórica de trabajos ya procesados.

El sistema conoce las características del trabajo, incluido formato de hoja y necesidad de B/N o color, y las compara con las capacidades configuradas. Puede identificar equipos compatibles, explicar por qué otros no lo son y recomendar uno entre los compatibles.

En V1:

- la asignación es **manual y predeterminada**;
- la recomendación no asigna automáticamente;
- la opción de asignación automática queda visible como evolución futura deshabilitada;
- no se define todavía un algoritmo certificado de asignación automática.

Para sostener la operación remota, cada impresora mantiene dos métricas separadas:

1. **contador histórico de hojas impresas**, acumulado y no reiniciable por recarga;
2. **disponibilidad actual estimada de papel**, expresada como cantidad y porcentaje respecto de la capacidad máxima configurada.

La disponibilidad se calcula a partir de la capacidad configurada, el consumo de trabajos y eventos manuales de recarga. No constituye una lectura física de sensor.

Una recarga significa completar físicamente el faltante hasta alcanzar la capacidad máxima. Por ejemplo, si una impresora registra `468 / 500`, el operador agrega las 32 hojas faltantes y luego utiliza **Registrar recarga de papel**. El sistema restablece la disponibilidad estimada a `500 / 500 · 100 %` y conserva sin cambios el contador histórico.

Se recomienda operativamente verificar y completar las impresoras al comienzo de la jornada para mantener sincronizada la estimación. Si se realiza una recarga durante el día, también debe completarse siempre hasta el máximo configurado antes de registrar el evento.

### 5.3 Horarios, puntos de entrega y envíos consolidados para Fase 5

La Fase 5 deja de ocuparse de módulos y pasa a concentrarse en **Horarios, puntos de entrega y envíos**.

#### Horarios operativos

La imprenta configura cada día de la semana por separado:

- día habilitado o cerrado;
- hora de apertura;
- hora de cierre.

Los tiempos comprometidos se expresan en **horas operativas**. El reloj de preparación solo consume tiempo dentro de las ventanas configuradas.

Ejemplo: si la imprenta abre a las 08:00, un pedido recibido a las 02:00 puede aceptarse y quedar en cola. Si la preparación configurada es de 4 horas, el cálculo comienza a las 08:00 y la disponibilidad estimada en Casa Central será a las 12:00.

Si el pedido entra cerca del cierre y el tiempo restante excede la jornada, el cómputo continúa en la siguiente apertura comercial.

#### Tiempos estimados

La Fase 5 permite configurar al menos:

- tiempo estimado de preparación para retiro en Casa Central, en horas;
- tiempo estimado para preparar o liberar un pedido para envío, en horas.

Estos valores son compromisos estimados y no una espera obligatoria. Si un trabajo queda listo antes, el sistema puede avanzar inmediatamente al estado correspondiente y actualizar el timeline del cliente.

Para envíos, el tiempo configurado corresponde al compromiso de la imprenta para preparar o liberar el pedido. No constituye por sí solo una garantía del tiempo de transporte de un tercero.

#### Puntos de entrega

La Fase 5 incluye una acción **Administrar puntos** que conduce a un panel dedicado.

Cada punto puede definir:

- nombre;
- ubicación;
- días habituales;
- franjas horarias de atención o entrega cuando se requieran.

Las franjas se tratan como ventanas estimadas, no como una cita exacta. La coordinación fina puede resolverse posteriormente por WhatsApp u otro canal, mientras la aplicación mantiene estados como En camino o Listo para retirar.

La disponibilidad temporal de un punto es operativa y no genera una nueva versión de configuración:

- puede habilitarse o deshabilitarse sin solicitar motivo;
- un punto deshabilitado deja de ofrecerse para nuevos pedidos;
- el dashboard mantiene un recordatorio visible mientras exista uno o más puntos deshabilitados;
- el punto vuelve a habilitarse manualmente cuando esté operativo;
- los pedidos que ya tenían ese punto pactado conservan su esquema de entrega y no se reasignan automáticamente, aunque puedan sufrir una demora.

La estimación presentada al cliente debe respetar tanto el horario operativo de la imprenta como la siguiente franja válida del punto seleccionado.

#### Simulación y timeline

La Fase 5 debe mostrar una simulación coherente con las fases anteriores. El ejemplo debe poder representar:

`Pedido recibido → Fuera de horario / En cola → Próxima apertura → En preparación → Listo en Casa Central → Disponible en punto según franja`

Si el pedido finaliza antes del estimado, el timeline avanza inmediatamente y el cliente puede ser notificado de que ya está disponible antes de lo previsto.

## 6. Pausa operativa

La pausa de recepción es una capacidad central del producto, no una mejora futura.

Puede utilizarse ante corte de luz, falla de impresoras, falta de insumos, saturación, emergencia o imposibilidad temporal de recibir trabajos.

La pausa:

- bloquea nuevas cotizaciones;
- bloquea la confirmación de nuevos pedidos;
- no elimina pedidos existentes;
- no modifica versiones de configuración;
- puede ser ejecutada por ADMIN_ADMIN o empleado;
- requiere reconfirmación de contraseña;
- registra usuario, rol, fecha, hora y motivo;
- puede revertirse mediante reanudación auditada.

## 7. Captura de configuración en la cotización

La versión aplicable se captura cuando el cliente presiona Cotizar pedido.

La cotización conserva:

- precio;
- medios de pago;
- punto o modalidad de entrega;
- seña;
- condiciones aplicables;
- versión de configuración;
- fecha y hora estimadas de disponibilidad cuando correspondan;
- fecha y hora de generación.

Una modificación posterior de la configuración no altera esa cotización.

## 8. Vigencia de la cotización

La cotización existe únicamente durante el flujo activo de confirmación.

Reglas acordadas:

1. Se muestran cinco minutos iniciales.
2. Al finalizar, aparece una consulta de actividad.
3. El cliente dispone de hasta sesenta segundos para responder.
4. Si no responde, la cotización se anula.
5. Si responde, recibe exactamente diez minutos adicionales.
6. El plazo adicional no se reinicia.
7. Al agotarse, se anula la cotización y se cierra la sesión.
8. Los datos y archivos temporales deben eliminarse.
9. La cotización no se guarda para utilizarla en una sesión posterior.

## 9. Pausa durante una cotización

Si la imprenta se pausa después de generar una cotización:

- el cliente no puede crear el pedido;
- el precio y las condiciones se conservan mientras la cotización continúe vigente;
- el temporizador de seguridad continúa;
- al reanudarse, el sistema revisa la fecha estimada de entrega;
- si la fecha cambia, debe mostrarse la anterior y la nueva;
- el cliente debe aceptar expresamente el nuevo plazo;
- si no lo acepta, no se crea el pedido.

## 10. Cuenta corriente - Requiere revisión

La cuenta corriente se plantea para clientes de confianza que pueden enviar trabajos sin pago previo y acumular deuda.

Decisiones propuestas:

- habilitación manual por ADMIN_ADMIN;
- cuenta con límite o sin límite;
- posibilidad de suspenderla;
- todos los pedidos a cuenta requieren revisión manual antes de producción;
- el administrador consulta deuda y carga operativa;
- el administrador define manualmente la fecha estimada de entrega;
- el cliente puede enviar el pedido sin pagar;
- enviar el pedido no implica autorización para producir.

Antes de aprobar, deberían mostrarse monto del nuevo pedido, deuda actual, deuda proyectada, pedidos impagos, trabajos en producción, fechas comprometidas, último pago, historial financiero y observaciones.

### Punto pendiente

Debe definirse qué ocurre al superar el límite:

- bloqueo absoluto;
- autorización excepcional del ADMIN_ADMIN;
- recepción del pedido con bloqueo hasta resolver la situación.

Esta sección debe ser validada expresamente por Agustín antes de integrarse como regla definitiva.

## 11. Capacidades futuras

Quedan fuera de la definición inmediata, pero se preservan como línea de evolución:

- catálogo de módulos y definición de prestaciones de los planes Gratis, Inicial y Avanzado;
- activación/desactivación de módulos opcionales por plan;
- cambios automáticos por demanda;
- reglas automáticas adicionales por franja horaria;
- pausa automática;
- disponibilidad por stock;
- saturación de producción;
- asignación automática o reasignación dinámica de impresoras mediante un modelo certificado;
- generación o expansión de una red más amplia de puntos de entrega y logística asociada.

## 12. Base disponible para mockups

Puede avanzarse desde ahora con:

- inicio de configuración;
- selección de modelo operativo;
- configuración de aprobación;
- configuración de pagos;
- configuración de seña;
- impresoras y capacidades;
- estados Operativa/Deshabilitada y edición segura;
- método de asignación manual V1;
- compatibilidad y recomendación de impresora;
- disponibilidad estimada de papel;
- confirmación de recarga de papel;
- selección manual de impresora para un trabajo;
- horarios operativos por día;
- tiempos estimados en horas;
- retiro en Casa Central;
- puntos de entrega con franjas horarias;
- preparación para envío;
- simulación de pedido fuera de horario y próxima apertura;
- timeline con finalización anticipada;
- disponibilidad operativa de puntos y recordatorio en dashboard;
- resumen y previsualización;
- activación inmediata;
- activación programada;
- protocolo de seguridad;
- historial de versiones;
- pausa y reanudación;
- mensajes de cotización expirada;
- tratamiento de una pausa durante la confirmación.

Los módulos no forman parte de los mockups de Fase 5 hasta definir la estrategia comercial de planes.

Las vistas de cuenta corriente pueden explorarse, pero deben quedar marcadas como sujetas a revisión.

## 13. Registro de cambios y justificación

| Cambio propuesto | Situación documentada anteriormente | Justificación vinculada al motor de configuración | Estado |
|---|---|---|---|
| Producto configurable posterior al MVP | Alcance centrado en una operación fija | El motor requiere que las decisiones operativas dejen de tratarse como universales | Confirmado para revisión |
| Estados fijos y transiciones configurables | Flujo único con avance predeterminado | Se preserva la trazabilidad y se permite adaptar el momento de avance | Confirmado |
| Configuraciones certificadas | Parámetros aislados o mejoras futuras | Evita combinaciones incoherentes y facilita instalación remota | Confirmado |
| Configuración versionada | No se definía vigencia completa | Garantiza no retroactividad y auditoría | Confirmado |
| Matriz financiera de Fase 3 | Pagos y seña definidos de forma general | Alinea medios, acreditación y seña con el modelo heredado de Fase 2 | Confirmado para revisión |
| Modalidad por monto escalonada | El umbral no definía con precisión el cobro de trabajos superiores | Mantiene flexibilidad para trabajos chicos y exige seña acreditable en montos mayores | Confirmado para revisión |
| Impresoras y capacidades Fase 4 | La administración de equipos estaba definida de forma general | Cierra estados, capacidades, edición, eliminación y trazabilidad | Confirmado |
| Asignación manual V1 | Manual y automática figuraban sin alcance temporal preciso | Mantiene control humano mientras la automática no posee modelo certificado | Confirmado |
| Disponibilidad estimada de papel | No existía sincronización explícita del papel | Habilita operación remota mediante capacidad, consumo y recarga manual a 100 % | Confirmado |
| Fase 5 enfocada en horarios y entrega | Incluía módulos sin definición comercial cerrada | Evita inventar planes y concentra la fase en compromisos operativos reales | Confirmado |
| Horarios operativos por día | La entrega no definía cuándo corría el tiempo | Permite estimaciones coherentes con apertura y cierre | Confirmado |
| Pedido fuera de horario en cola | Un tiempo fijo podía prometer horarios imposibles | El reloj comienza en la próxima apertura comercial | Confirmado |
| Puntos con franjas horarias | Los puntos no distinguían ventanas de atención | Evita ofrecer retiros fuera del horario real de cada ubicación | Confirmado |
| Disponibilidad temporal de puntos fuera del versionado | Una contingencia breve podía exigir una nueva versión | Permite habilitar/deshabilitar sin motivo y sin reconfigurar el motor | Confirmado |
| Recordatorio de puntos deshabilitados | Una baja temporal podía olvidarse | Mantiene la situación visible en dashboard hasta su reactivación | Confirmado |
| Finalización anticipada | El estimado podía interpretarse como espera obligatoria | El pedido avanza y notifica al cliente apenas está listo | Confirmado |
| Pausa operativa central | Considerada posibilidad futura | Es necesaria ante emergencias reales de la imprenta | Confirmado |
| Captura al cotizar | Creación directa con reglas generales | La cotización debe congelar precio y condiciones vigentes | Confirmado |
| Cuenta corriente | Excepción comercial no formalizada | El motor debe contemplar clientes autorizados sin debilitar el control financiero | Requiere revisión de Agustín |
| Módulos según plan | Antes aparecían como parte inmediata del diseño | Catálogo y prestaciones de planes todavía no están definidos | Futuro |

## 14. Referencias para integración

Esta actualización se origina en los cambios introducidos por el motor de configuración y deberá contrastarse con:

- marco-del-proyecto/alcance-general.md
- marco-del-proyecto/motor-de-configuracion-del-sistema.md
- analisis/especificacion-de-requerimientos/requerimientos-funcionales.md
- analisis/especificacion-de-requerimientos/requerimientos-no-funcionales.md
- analisis/especificacion-de-requerimientos/matriz-reglas-de-negocio.md
- analisis/historias-de-usuarios/historias-de-usuario.md
- analisis/casos-de-uso/casos-de-uso.md
- diseño/Front/ux-ui/wireflows/WF-ADMINISTRADOR-MVP.md
- marco-del-proyecto/matriz-trazabilidad.md

Hasta su integración, el presente archivo funciona únicamente como actualización a revisión.