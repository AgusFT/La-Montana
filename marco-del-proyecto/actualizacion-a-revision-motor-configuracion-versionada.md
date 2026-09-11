# Actualización a revisión - Motor de configuración versionada

| Campo | Valor |
|---|---|
| Versión | 2.5 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-10 |
| Documento relacionado | motor-de-configuracion-del-sistema.md |
| Propósito | Proponer la evolución del motor sin modificar la definición vigente |

> Esta actualización debe ser revisada por Agustín antes de integrarse. Los términos, identificadores y estructuras descritos son conceptuales y no representan todavía contratos definitivos de implementación.

## 1. Objetivo del motor

El motor de configuración debe permitir que cada imprenta adapte su funcionamiento mediante modelos operativos certificados y parámetros controlados.

No se propone un editor libre de reglas. El motor debe impedir configuraciones contradictorias, inseguras o difíciles de sostener.

## 2. Conceptos principales

### 2.1 Versión de configuración operativa

Conjunto inmutable de decisiones que determina el comportamiento aplicable a nuevas cotizaciones.

Puede contener:

- modelo de aprobación;
- condiciones de avance;
- medios y momentos de pago;
- reglas de seña;
- impresoras y capacidades;
- método de asignación;
- horarios operativos;
- tiempos estimados;
- modalidades de entrega;
- definición de puntos de entrega;
- parámetros de notificación;
- servicios habilitados.

La disponibilidad temporal de recursos, como una recarga de papel o la habilitación/deshabilitación cotidiana de un punto, se modela fuera de la versión cuando corresponda.

### 2.2 Modelo operativo certificado

Estructura preconfigurada, validada por el producto y compatible con reglas de seguridad.

Ejemplos conceptuales:

- revisión manual y pago al entregar;
- revisión manual y pago digital previo;
- aprobación condicional y pago previo;
- aprobación automática bajo condiciones certificadas.

Los ejemplos no constituyen todavía el catálogo definitivo.

#### Modelos habilitados en la Fase 2

Para la primera versión del producto se distinguen dos modelos operativos seleccionables:

- **Control manual:** todos los pedidos requieren revisión y decisión humana. Las validaciones técnicas obligatorias continúan vigentes y ninguna regla financiera reemplaza la aprobación o el rechazo del operador.
- **Control condicional:** el sistema evalúa una condición certificada. Si la condición habilita el avance, puede aprobar el pedido automáticamente; cuando la condición económica previa no se cumple, se impide iniciar la carga del archivo.

La **automatización certificada** puede mostrarse como evolución futura, pero permanece deshabilitada y no forma parte de esta versión.

El control condicional admite inicialmente tres criterios certificados:

1. **Pago previo:** exige acreditar el total antes de habilitar la carga del archivo.
2. **Pago de seña:** exige acreditar la seña configurada antes de habilitar la carga del archivo.
3. **Monto total del pedido:** permite aprobación automática hasta un umbral configurable. Cuando el pedido supera ese umbral, se activa una seña previa configurable que debe acreditarse antes de continuar.

Pago previo, pago de seña y la seña exigida por exceso del umbral son condiciones económicas estrictas: mientras no estén acreditadas, el archivo permanece del lado del cliente y no se almacena ni procesa en el servidor.

#### Parametrización consolidada en la Fase 3

La Fase 3 hereda el modelo seleccionado y solamente muestra parámetros compatibles:

- **Control manual:** permite medios de pago flexibles, incluido efectivo, transferencia y pago digital. La seña puede configurarse como regla financiera opcional y no reemplaza la aprobación humana.
- **Control condicional + pago previo:** efectivo queda deshabilitado para cumplir la condición inicial. Debe existir al menos un medio acreditable, como transferencia o pago digital. La seña no aplica porque la condición exige el pago total.
- **Control condicional + pago de seña:** la condición de seña ya viene impuesta por Fase 2 y no puede desactivarse ni cambiarse por otra condición. En Fase 3 se configura el tipo y el valor de la seña. La acreditación debe realizarse por transferencia, pago digital u otro medio certificable; efectivo no es válido para acreditar la seña previa.
- **Control condicional + monto del pedido:** la condición `monto total del pedido` permanece bloqueada como criterio heredado; el ADMIN_ADMIN configura el valor del umbral. Los pedidos que no superan el umbral pueden aprobarse automáticamente y utilizar medios flexibles, incluido efectivo. Los pedidos que superan el umbral requieren una seña previa configurable; esa seña debe acreditarse mediante transferencia, pago digital u otro medio acreditable antes de habilitar el avance. El saldo restante puede abonarse luego mediante cualquiera de los medios generales habilitados, incluido efectivo cuando corresponda.

La regla histórica de **30 % desde 200 carillas** deja de constituir una condición fija del modelo. Se conserva únicamente como antecedente o valor predeterminado configurable; la condición y los valores efectivos dependen del modelo seleccionado y de la parametrización de la Fase 3.

#### Parametrización consolidada en la Fase 4

La Fase 4 administra impresoras, sus capacidades y el método de asignación disponible.

Cada impresora se registra con una identidad técnica estable y un nombre visible. Como mínimo se configuran:

- formatos de hoja admitidos;
- capacidad B/N o color;
- dúplex cuando corresponda;
- capacidad máxima de hojas;
- estado de la impresora.

Los únicos estados de impresora de V1 son:

- **Operativa:** puede recibir trabajos y no puede editarse ni eliminarse;
- **Deshabilitada:** no recibe nuevos trabajos y puede editarse.

Para editar una impresora debe deshabilitarse primero. La acción **Eliminar impresora** aparece únicamente dentro de la edición de una impresora Deshabilitada. La eliminación debe retirar el equipo de la administración operativa sin romper la trazabilidad histórica de trabajos anteriores.

El sistema determina la compatibilidad de un trabajo con las impresoras a partir de las características ya conocidas del trabajo, especialmente formato de hoja, B/N o color y otras capacidades aplicables. Las impresoras incompatibles deben mostrar el motivo de la incompatibilidad.

En V1:

- la asignación es **manual y predeterminada**;
- el sistema puede recomendar una impresora compatible;
- la recomendación no realiza una asignación automática;
- la opción **Asignación automática** permanece visible como evolución futura deshabilitada.

La futura asignación automática requerirá un modelo certificado. Su algoritmo no se define todavía; podrá considerar compatibilidad, carga, disponibilidad de papel, trabajos pendientes y estado operativo.

La Fase 4 también establece la base para conocer la disponibilidad estimada de papel:

- la capacidad máxima se configura manualmente;
- el sistema mantiene un contador histórico acumulado de hojas impresas;
- mantiene por separado una disponibilidad actual estimada expresada en hojas y porcentaje;
- cada trabajo procesado descuenta las hojas calculadas para ese trabajo;
- la disponibilidad no se presenta como lectura física de sensor.

La recarga de papel no es una nueva versión de configuración. Es un evento operativo: el operador completa físicamente el faltante hasta la capacidad máxima y luego confirma **Registrar recarga de papel**. El sistema restablece la disponibilidad estimada al 100 % sin reiniciar el contador histórico.

#### Parametrización consolidada en la Fase 5

La Fase 5 se redefine como **Horarios, puntos de entrega y envíos**. La definición comercial de módulos se retira de esta fase y queda para una etapa futura.

La configuración de horarios permite definir cada día de la semana por separado con:

- día habilitado o cerrado;
- hora de apertura;
- hora de cierre.

Los compromisos de preparación se expresan en horas y se computan únicamente dentro de esas ventanas operativas.

Si un pedido se recibe fuera de horario, puede aceptarse y quedar en cola. El tiempo comienza a correr en la próxima apertura comercial. Si el cálculo cruza el cierre de la jornada, las horas restantes continúan en la próxima apertura.

La fase configura al menos:

- horas estimadas de preparación para retiro en Casa Central;
- horas estimadas para preparar o liberar un pedido para envío.

Estos valores son estimaciones máximas de operación y no una espera obligatoria. Si el pedido queda listo antes, el flujo avanza inmediatamente y el timeline del cliente se actualiza.

Los puntos de entrega se administran mediante un panel dedicado accesible desde **Administrar puntos**. Cada punto puede registrar nombre, ubicación y, cuando sea necesario, días y franjas horarias propias.

Las franjas horarias son ventanas estimadas de atención o entrega. La coordinación exacta puede resolverse por WhatsApp u otro canal sin alterar la trazabilidad de estados de la aplicación.

La disponibilidad temporal de un punto no crea una nueva versión:

- habilitar o deshabilitar es una acción operativa inmediata;
- no se solicita motivo;
- un punto deshabilitado deja de ofrecerse a nuevos pedidos;
- el dashboard mantiene un recordatorio mientras exista uno o más puntos deshabilitados;
- las entregas ya pactadas conservan el mismo punto y no se reasignan automáticamente;
- el punto vuelve a habilitarse manualmente cuando esté operativo.

La estimación presentada al cliente debe respetar la combinación de horario operativo de la imprenta, tiempo configurado y siguiente franja válida del punto.

La simulación de Fase 5 debe mostrar al menos:

`Pedido recibido → En cola si está fuera de horario → Próxima apertura → En preparación → Listo en Casa Central → Disponible en punto según franja`

### 2.3 Parámetro controlado

Valor que puede modificarse dentro de límites permitidos sin alterar invariantes del sistema.

### 2.4 Invariante

Regla que ninguna imprenta puede desactivar, como autenticación, autorización backend, auditoría, aislamiento de datos, protección de archivos y ejecución de impresión solamente autorizada.

### 2.5 Modularidad comercial futura

La gestión de módulos queda fuera del alcance actual de Fase 5.

Se conserva únicamente el principio conceptual de que una capacidad opcional podrá representarse mediante un estado activo/inactivo, pero:

- todavía no se define el catálogo de módulos;
- todavía no se define qué incluyen los planes Gratis, Inicial y Avanzado;
- ninguna mejora opcional futura debe impedir completar el flujo básico de cotización, pedido, producción y entrega.

## 3. Ciclo de vida

| Estado | Definición |
|---|---|
| En preparación | Configuración que el ADMIN_ADMIN está revisando y todavía no tiene vigencia |
| Programada | Configuración confirmada con fecha y hora futura |
| Activa | Versión utilizada para nuevas cotizaciones |
| Histórica | Versión que estuvo activa y permanece disponible para auditoría |
| Programada cancelada | Versión futura cancelada antes de entrar en vigencia |

Una versión activa o histórica no se edita.

Para cambiarla se crea una versión nueva.

### 3.1 Cardinalidad y exclusión del cambio pendiente

Por cada imprenta deben cumplirse simultáneamente estas reglas:

- existe exactamente una configuración activa;
- puede existir como máximo una configuración en preparación;
- puede existir como máximo una configuración programada;
- una configuración en preparación y una programada no pueden coexistir;
- por lo tanto, existe como máximo un cambio pendiente respecto de la configuración activa.

La configuración en preparación es el único estado editable. No tiene fecha de vigencia y puede guardarse, continuarse o cancelarse.

Cuando el ADMIN_ADMIN completa el protocolo de seguridad y confirma una fecha futura, la configuración deja de estar en preparación y pasa a Programada. La versión programada ya no es un borrador: queda cerrada, auditada e inmutable mientras espera su activación.

| Situación | Estado activo | Cambio pendiente permitido | Edición disponible |
|---|---|---|---|
| Sin cambios pendientes | Una versión activa | Ninguno | Puede iniciarse un borrador |
| Edición en curso | Una versión activa | Un borrador en preparación | Solo puede continuarse o cancelarse ese borrador |
| Activación futura confirmada | Una versión activa | Una versión programada | No; debe activarse o cancelarse la programación |

No puede iniciarse otro borrador desde la versión activa, una histórica o los valores predeterminados mientras exista un borrador o una programación pendiente.

## 4. Activación

### 4.1 Activación inmediata

La nueva versión entra en vigencia después de completar el protocolo de seguridad.

La primera cotización posterior debe utilizarla.

### 4.2 Activación programada

El ADMIN_ADMIN define fecha y hora.

Hasta ese momento continúa vigente la configuración actual. La configuración programada puede cancelarse antes de entrar en vigencia.

La programación consume el único cambio pendiente permitido. Mientras exista, no puede crearse ni continuarse un borrador. Si se requiere modificar sus decisiones, primero debe cancelarse la programación; la cancelación libera la edición sin eliminar el registro auditado.

### 4.3 Reutilización de una versión anterior

Si se desea recuperar una configuración histórica, debe copiarse como base de una nueva versión.

No se modifica el historial ni se reactiva silenciosamente un registro anterior.

## 5. Protocolo de seguridad

La activación o programación de cambios sensibles debe contemplar:

1. resumen de cambios;
2. explicación de consecuencias;
3. confirmación explícita;
4. reingreso de contraseña;
5. segundo factor o código por correo cuando corresponda;
6. validación backend;
7. creación de la versión;
8. registro de auditoría.

El mecanismo definitivo de segundo factor debe validarse técnicamente.

## 6. Auditoría

Cada acción debe registrar:

- usuario responsable;
- rol;
- imprenta;
- fecha y hora;
- versión anterior;
- nueva versión;
- diferencias;
- tipo de activación;
- fecha de vigencia;
- resultado de la verificación de seguridad;
- cancelación, si corresponde;
- motivo u observación cuando la acción lo requiera;
- contexto técnico permitido.

Los eventos operativos que no crean una versión, como una recarga de papel o el cambio temporal de disponibilidad de un punto, deben conservar su propia auditoría cuando corresponda. La habilitación/deshabilitación de un punto no requiere motivo.

## 7. No retroactividad

Las configuraciones nuevas no alteran:

- pedidos existentes;
- trabajos en producción;
- estados anteriores;
- cotizaciones ya generadas;
- decisiones financieras registradas;
- historial de auditoría.

La versión se captura en el momento de cotizar y queda asociada al resultado mientras la cotización permanezca vigente.

La deshabilitación operativa posterior de un punto tampoco reasigna automáticamente pedidos que ya lo tenían pactado.

## 8. Flujo de cotización

1. El cliente prepara el pedido.
2. Presiona Cotizar pedido.
3. El backend verifica que la imprenta no esté pausada.
4. Consulta la versión activa.
5. Calcula precio, condiciones y alternativas de entrega disponibles.
6. Aplica horarios operativos, tiempos estimados y disponibilidad de puntos cuando corresponda.
7. Genera un identificador temporal de cotización.
8. Registra la versión utilizada.
9. Devuelve resumen, medios de pago, entrega, seña y estimación aplicable.
10. El cliente confirma dentro del tiempo permitido.
11. El backend valida nuevamente pausa, cotización, sesión y disponibilidad operativa relevante.
12. Crea el pedido con la información capturada.
13. Elimina recursos temporales que ya no sean necesarios.

El frontend no debe ser la fuente de verdad de las reglas aplicadas.

## 9. Temporizadores de cotización

| Momento | Comportamiento |
|---|---|
| Generación | Comienzan cinco minutos iniciales |
| Fin de los cinco minutos | Se muestra consulta de actividad |
| Ventana de respuesta | Hasta sesenta segundos |
| Sin respuesta | Se anula cotización, se eliminan temporales y se redirige o cierra sesión |
| Respuesta afirmativa | Se otorgan diez minutos exactos |
| Durante la extensión | Se muestra cuenta regresiva visible |
| Fin de la extensión | Se anula cotización, se eliminan temporales y se cierra sesión |

La extensión no se reinicia por actividad posterior.

## 10. Pausa operativa y operación diaria

La pausa no forma parte de una versión de configuración. Es un estado operacional prioritario.

Puede ser ejecutada por ADMIN_ADMIN o empleado con reconfirmación de contraseña.

Debe bloquear:

- generación de nuevas cotizaciones;
- confirmación de pedidos cotizados;
- reintentos de confirmación mientras continúe activa.

No debe bloquear la consulta o administración de pedidos existentes, salvo que otra contingencia técnica lo impida.

### 10.1 Recarga de papel

La capacidad máxima de papel pertenece a la configuración de la impresora; la recarga física pertenece a la operación cotidiana.

Flujo conceptual:

1. el sistema muestra disponibilidad estimada actual y capacidad máxima;
2. el operador completa físicamente el faltante hasta el máximo configurado;
3. confirma Registrar recarga de papel;
4. el sistema restablece la disponibilidad estimada al 100 %;
5. conserva el contador histórico de hojas impresas;
6. registra impresora, usuario y momento.

En V1 no se modela una recarga parcial. El evento de recarga siempre significa volver físicamente a la capacidad máxima configurada.

### 10.2 Selección de impresora para un trabajo

La configuración define capacidades y la modalidad manual. Durante la operación:

1. el sistema toma las características del trabajo;
2. identifica impresoras Operativas compatibles;
3. explica las incompatibilidades;
4. muestra disponibilidad estimada de papel;
5. puede recomendar un equipo;
6. un usuario interno autorizado realiza la selección final manualmente.

### 10.3 Disponibilidad temporal de puntos

La definición habitual del punto pertenece a Fase 5; su disponibilidad cotidiana se administra operativamente.

Flujo conceptual:

1. un usuario autorizado deshabilita un punto desde el dashboard o panel de puntos;
2. no se solicita motivo;
3. el punto deja de ofrecerse para nuevos pedidos;
4. los pedidos ya pactados conservan ese punto y pueden continuar con demora;
5. el dashboard mantiene un recordatorio mientras exista uno o más puntos deshabilitados;
6. cuando el punto vuelve a estar operativo, el usuario lo habilita manualmente;
7. el aviso desaparece cuando ya no quedan puntos deshabilitados.

## 11. Reanudación y fecha de entrega

Al reanudar:

1. el cliente conserva precio y condiciones si la cotización continúa vigente;
2. el backend recalcula la fecha estimada;
3. si no cambia, permite continuar;
4. si cambia, muestra fecha anterior y nueva;
5. solicita aceptación explícita;
6. crea el pedido solamente si el cliente acepta.

La pausa no detiene el temporizador de seguridad de la cotización.

## 12. Cuenta corriente - Propuesta a validar

La cuenta corriente se modela como una excepción financiera autorizada manualmente, no como una combinación libre de reglas.

Propuesta:

- asignación manual por ADMIN_ADMIN;
- opción con límite o sin límite;
- estado activa, suspendida o deshabilitada;
- todos los pedidos requieren revisión manual;
- el pedido puede enviarse sin pago;
- producción permanece bloqueada hasta aprobación;
- el administrador consulta deuda total y carga operativa;
- la fecha estimada de entrega se define manualmente.

La conducta al superar el límite queda pendiente de validación por Agustín.

## 13. Orden de precedencia propuesto

| Prioridad | Regla |
|---:|---|
| 1 | Seguridad, autorización, integridad y auditoría |
| 2 | Pausa operativa |
| 3 | Disponibilidad operativa de recursos como puntos e impresoras |
| 4 | Versión activa al cotizar |
| 5 | Horarios y tiempos configurados |
| 6 | Excepciones financieras autorizadas |
| 7 | Parámetros visuales y preferencias no críticas |

Esta precedencia debe validarse durante el diseño técnico.

## 14. Configuraciones futuras

Se preservan como evolución:

- definición del catálogo de módulos;
- composición de los planes Gratis, Inicial y Avanzado;
- cambios automáticos por demanda;
- automatizaciones adicionales por horario;
- pausas automáticas;
- limitación por stock;
- reprogramación automática por saturación;
- asignación automática o reasignación dinámica según carga mediante un modelo certificado;
- expansión de la red de puntos y lógica logística avanzada.

No deben mezclarse con el alcance base actual.

## 15. Registro de cambios y justificación

| Cambio propuesto | Referencia anterior | Justificación por el motor de configuración | Estado |
|---|---|---|---|
| Sustituir política por versión de configuración | Terminología amplia de políticas | Reduce ambigüedad para negocio, UX e implementación | Confirmado para revisión |
| Modelos certificados | Combinación abierta no definida | Evita zonas grises e incompatibilidades | Confirmado |
| Estados del ciclo de vida | Activación sin modelo completo | Permite preparar, programar, cancelar y auditar | Confirmado |
| Un único cambio pendiente | Convivencia de borradores y programaciones no explicitada | Evita carreras, bases ambiguas y configuraciones futuras contradictorias | Confirmado |
| Programación inmutable | Programación tratada solo como fecha futura | Separa la edición de una versión ya confirmada y auditada | Confirmado |
| Captura al cotizar | Reglas aplicadas principalmente al crear pedido | Congela condiciones visibles y evita carreras de configuración | Confirmado |
| Pausa fuera de la versión | Pausa considerada evolución futura | Debe actuar inmediatamente sin reversionar toda la configuración | Confirmado |
| Temporizadores de cotización | Sin vigencia detallada | Protege información económica y libera recursos temporales | Confirmado |
| Matriz de pagos de Fase 3 | Medios de pago tratados en forma general | Impide combinaciones incompatibles y hereda restricciones de Fase 2 | Confirmado para revisión |
| Umbral por monto con seña escalonada | Superación del umbral derivaba a revisión humana | Mantiene agilidad para trabajos chicos y protege financieramente pedidos mayores mediante seña previa | Confirmado para revisión |
| Ciclo de impresoras Fase 4 | Impresoras y capacidades descritas de forma general | Define estados, edición, eliminación y trazabilidad antes de diseñar las vistas | Confirmado |
| Compatibilidad y asignación manual V1 | Método de asignación sin alcance temporal preciso | Permite recomendaciones automáticas sin delegar la decisión final | Confirmado |
| Disponibilidad estimada y recarga | Papel no modelado como recurso operativo | Permite operar remotamente sin depender de sensores físicos | Confirmado |
| Fase 5 sin módulos | Módulos figuraban dentro de la fase sin planes definidos | Evita inventar una política comercial aún pendiente | Confirmado |
| Horarios por día y horas operativas | Entrega no definía el calendario de cómputo | Evita estimaciones fuera de la jornada real | Confirmado |
| Pedido fuera de horario en cola | El plazo podía correr durante el cierre | El cómputo comienza en la próxima apertura comercial | Confirmado |
| Puntos con franjas horarias | La disponibilidad de puntos era genérica | Permite ventanas de retiro coherentes con cada ubicación | Confirmado |
| Estado temporal de punto fuera de versión | Una contingencia breve podía convertirse en configuración | Habilitar/deshabilitar es inmediato y no requiere motivo | Confirmado |
| Recordatorio en dashboard | Una baja temporal podía olvidarse | Mantiene visible la contingencia operativa | Confirmado |
| Finalización anticipada | El estimado podía interpretarse como espera obligatoria | El flujo avanza apenas el pedido está listo | Confirmado |
| Cuenta corriente manual | Excepción genérica por lista | Requiere control financiero y operativo por pedido | Requiere revisión de Agustín |
| Precedencia de reglas | No explicitada | El motor necesita resolver conflictos de manera determinista | Propuesta técnica |

## 16. Referencias para integración

Esta actualización debe contrastarse con:

- marco-del-proyecto/motor-de-configuracion-del-sistema.md
- marco-del-proyecto/alcance-general.md
- analisis/especificacion-de-requerimientos/matriz-reglas-de-negocio.md
- analisis/especificacion-de-requerimientos/requerimientos-funcionales.md
- analisis/especificacion-de-requerimientos/requerimientos-no-funcionales.md
- marco-del-proyecto/matriz-trazabilidad.md

La justificación principal de este documento es la incorporación del motor de configuración como núcleo del producto definido.