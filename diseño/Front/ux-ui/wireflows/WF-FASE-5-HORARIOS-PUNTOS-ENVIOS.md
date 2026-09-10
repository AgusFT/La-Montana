# WF - Fase 5 - Horarios, puntos de entrega y envíos

| Campo | Valor |
|---|---|
| Versión | 1.1 - Propuesta |
| Estado | Actualización a revisión - evidencia visual aprobada |
| Fecha | 2026-09-10 |
| Fase | Fase 5 - Horarios, puntos de entrega y envíos |
| Vistas relacionadas | WF-CFG-08, WF-CFG-09 |
| Actor de configuración | ADMIN_ADMIN |
| Actores operativos | Usuario interno autorizado, Sistema, Cliente |

> Este documento consolida las decisiones funcionales y de navegación de Fase 5. La definición comercial de módulos queda fuera de esta fase hasta definir el catálogo y la composición de los planes Gratis, Inicial y Avanzado.

## 1. Objetivo

Permitir que la imprenta configure un calendario operativo realista y compromisos estimados de preparación y entrega, manteniendo separadas las decisiones estructurales de las contingencias operativas cotidianas.

La fase debe responder tres preguntas:

1. ¿Cuándo trabaja la imprenta y cuándo corre el reloj productivo?
2. ¿Cuánto tiempo estimado necesita para dejar un pedido listo para retiro o envío?
3. ¿Qué modalidades y puntos de entrega puede ofrecer al cliente y en qué franjas?

## 2. Estructura visual propuesta

Título de fase:

**Fase 5 - Horarios, puntos de entrega y envíos**

Bloques principales:

- `CFG-008 - Horarios operativos`;
- `Tiempos estimados`;
- `CFG-009 - Entrega y puntos habilitados`;
- `Simulación del recorrido`;
- `Regla operativa`.

La interfaz debe conservar el lenguaje visual de las fases anteriores: cards, inputs configurables, avisos informativos y timeline de ejemplo.

## 3. CFG-008 - Horarios operativos

### 3.1 Configuración por día

Cada día de la semana se representa de forma independiente.

Por día se muestra:

- checkbox o toggle Habilitado;
- día;
- input de Apertura;
- input de Cierre.

Ejemplo:

| Día | Estado | Apertura | Cierre |
|---|---|---:|---:|
| Lunes | Habilitado | 09:00 | 18:00 |
| Martes | Habilitado | 09:00 | 18:00 |
| Miércoles | Habilitado | 09:00 | 18:00 |
| Jueves | Habilitado | 09:00 | 18:00 |
| Viernes | Habilitado | 09:00 | 18:00 |
| Sábado | Habilitado | 10:00 | 12:00 |
| Domingo | Cerrado | - | - |

No se agrupan lunes a viernes como un único control: el administrador debe poder ajustar cada día por separado.

### 3.2 Regla de cómputo

Los tiempos estimados se consumen únicamente dentro de las ventanas operativas configuradas.

Si un pedido ingresa fuera de horario:

- puede ser recibido;
- queda En cola;
- el cómputo comienza en la próxima apertura comercial.

Si el tiempo necesario cruza el cierre de una jornada, el remanente continúa desde la siguiente apertura.

Ejemplo:

- pedido recibido: 02:00;
- próxima apertura: 08:00;
- preparación configurada: 4 h;
- disponibilidad estimada en Casa Central: 12:00.

## 4. Tiempos estimados

Los valores se presentan como inputs editables y se expresan en horas.

### 4.1 Preparación en Casa Central

Campo:

`Preparación en Casa Central: [ 4 ] hs`

Representa el compromiso estimado de la imprenta para dejar listo un pedido que se retirará en Casa Central.

### 4.2 Preparación para envío

Campo:

`Preparación para envío: [ 36 ] hs`

Representa el compromiso estimado de la imprenta para preparar o liberar el pedido para la etapa de envío.

No debe interpretarse automáticamente como tiempo garantizado de transporte de un tercero.

### 4.3 Finalización anticipada

Los tiempos configurados son estimaciones y no una espera obligatoria.

Si un pedido queda listo antes:

1. el operador o sistema avanza el estado correspondiente;
2. el timeline del cliente se actualiza inmediatamente;
3. el cliente puede recibir una notificación de que el pedido está disponible antes de lo previsto.

## 5. CFG-009 - Entrega y puntos habilitados

### 5.1 Modalidades

La vista debe mostrar las modalidades que la configuración permite ofrecer:

- Retiro en local, sujeto al horario operativo;
- Puntos de entrega, sujetos a su disponibilidad y franjas;
- Envío, sujeto al compromiso estimado configurado.

El cliente solo debe visualizar opciones realmente disponibles.

### 5.2 Administrar puntos

La Fase 5 incluye el botón:

**Administrar puntos**

Ese botón abre un panel dedicado de puntos de entrega. El panel también puede ser accesible desde el dashboard para no obligar a recorrer el asistente cada vez que se necesita gestionar un punto.

El panel permite:

- consultar puntos;
- agregar punto;
- editar datos habituales;
- ver estado operativo;
- habilitar o deshabilitar de forma inmediata cuando el usuario posee permisos.

## 6. Definición de un punto de entrega

Cada punto puede contener:

- nombre;
- ubicación o dirección;
- días de atención o entrega;
- una o más franjas horarias habituales cuando sea necesario;
- estado operativo Habilitado/Deshabilitado.

Las franjas representan ventanas estimadas, similares a una ventana comercial de entrega, no una cita exacta.

Ejemplo:

**Facultad de Medicina**

- Lunes a viernes;
- franja habitual: 17:00 a 21:00.

La coordinación exacta puede cerrarse posteriormente por WhatsApp u otro canal. La aplicación conserva el estado operativo general, por ejemplo En camino o Listo para retirar.

## 7. Habilitar y deshabilitar puntos

La disponibilidad temporal de un punto es una acción operativa y no una nueva versión de configuración.

### Deshabilitar

- acción inmediata;
- no solicita motivo;
- no solicita una fecha de fin obligatoria;
- el punto deja de ofrecerse para nuevos pedidos;
- permanece deshabilitado hasta que un usuario lo vuelva a habilitar.

### Habilitar

- acción manual inmediata;
- el punto vuelve a ofrecerse a nuevos pedidos sujeto a sus días y franjas habituales.

### Pedidos ya pactados

Deshabilitar un punto no modifica automáticamente pedidos que ya tenían esa entrega acordada.

Esos pedidos:

- conservan el mismo punto;
- no se reasignan automáticamente;
- pueden demorarse si la contingencia lo exige;
- continúan mostrando su avance real en el timeline.

## 8. Recordatorio en dashboard

Mientras exista uno o más puntos deshabilitados, el dashboard debe mantener una advertencia visible.

Ejemplo:

`⚠ Hay 2 puntos de entrega deshabilitados`

Acciones sugeridas:

- Ver puntos;
- acceder al panel de administración;
- habilitar nuevamente el punto cuando corresponda.

El objetivo del aviso es evitar que una baja temporal quede olvidada.

No se requiere registrar un motivo para mostrar el recordatorio.

## 9. Franjas horarias y cálculo de disponibilidad

El cálculo debe distinguir:

- horario operativo de la imprenta: determina cuándo corre el reloj de preparación;
- franja del punto: determina cuándo puede presentarse una disponibilidad de retiro válida.

Ejemplo:

- Casa Central trabaja 08:00 a 18:00;
- pedido termina de prepararse a las 12:00;
- Punto Medicina admite entregas de 17:00 a 21:00;
- el sistema no promete retiro a las 12:00 en Medicina;
- presenta la siguiente ventana válida del punto.

La franja puede funcionar como margen estimado; la coordinación final puede realizarse externamente.

## 10. Simulación del recorrido

La vista de Fase 5 debe conservar el patrón de simulación usado en las fases anteriores.

Timeline base:

1. **Pedido recibido** - 02:00.
2. **Fuera de horario** - queda En cola.
3. **Próxima apertura** - 08:00.
4. **En preparación** - consume 4 horas operativas.
5. **Listo en Casa Central** - 12:00 estimado.
6. **Disponible en punto** - según la siguiente franja horaria válida.

Mensajes complementarios:

- `Si el pedido queda listo antes, el cliente es notificado y el flujo avanza inmediatamente.`
- `Los puntos de entrega ofrecen ventanas estimadas; la coordinación fina puede resolverse por WhatsApp.`

## 11. Impacto en timeline del cliente

El timeline debe explicar tanto la estimación como el estado real.

Ejemplo de estados visibles:

`Pedido recibido → En cola → En preparación → En camino → Listo para retirar → Entregado`

Si el pedido entra fuera del horario operativo, En cola debe explicar que el procesamiento comenzará en la próxima apertura.

Si el trabajo finaliza antes de la estimación, el nuevo estado reemplaza la espera prevista y se notifica al cliente.

## 12. Separación configuración / operación

| Decisión | Contexto | Genera nueva versión |
|---|---|---:|
| Cambiar horarios operativos habituales | Motor de configuración - Fase 5 | Sí |
| Cambiar tiempo estimado en horas | Motor de configuración - Fase 5 | Sí |
| Agregar o editar definición de punto | Panel Administrar puntos / configuración estructural | Según integración definitiva del motor |
| Habilitar o deshabilitar temporalmente un punto | Operación / dashboard | No |
| Mostrar recordatorio de puntos deshabilitados | Dashboard | No |
| Avanzar un pedido porque quedó listo antes | Operación del pedido | No |

La integración definitiva deberá decidir cómo persiste una edición estructural de punto respecto del versionado, pero la disponibilidad temporal queda confirmada como operativa y no versionada.

## 13. Módulos - fuera de alcance de Fase 5

No se diseñan módulos en esta fase.

Queda para una etapa futura definir:

- catálogo de módulos;
- prestaciones de plan Gratis;
- prestaciones de plan Inicial;
- prestaciones de plan Avanzado;
- dependencias comerciales.

Principio preservado: una mejora opcional futura no debe impedir el flujo básico del negocio.

## 14. Criterios de aceptación

- cada día puede configurarse individualmente;
- apertura y cierre se presentan mediante inputs utilizables;
- tiempos estimados se expresan en horas;
- pedidos fuera de horario pueden quedar en cola y comenzar en la próxima apertura;
- el cálculo respeta cierres y siguientes aperturas;
- existe una acción Administrar puntos;
- cada punto puede definir días y franjas propias;
- las franjas se comunican como ventanas estimadas;
- un punto puede habilitarse o deshabilitarse sin motivo y sin nueva versión;
- el dashboard recuerda la existencia de puntos deshabilitados;
- pedidos pactados no se reasignan automáticamente por una baja posterior;
- si el pedido queda listo antes, el timeline avanza inmediatamente;
- la pantalla incluye simulación del recorrido;
- módulos no forman parte del diseño actual de Fase 5.

## 15. Evidencia visual aprobada

La revisión del 10/09/2026 confirmó exactamente dos mockups para presentar la Fase 5.

| Evidencia | Alcance | Decisión representada |
|---|---|---|
| [MC-ADM-CFG-008-009 - Horarios, puntos y envíos](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-008-009-horarios-puntos-envios.png) | Pantalla principal de Fase 5 | Días independientes, aperturas y cierres, tiempos estimados, modalidades y simulación completa |
| [MC-ADM-CFG-009 - Administrar puntos de entrega](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-009-administrar-puntos-entrega.png) | Panel dedicado de puntos | Alta, edición, estado, días de operación, franjas horarias y resumen de disponibilidad |

### 15.1 Horarios, modalidades y simulación

`MC-ADM-CFG-008-009` muestra:

- los siete días configurados por separado, sin agrupar lunes a viernes;
- apertura y cierre propios para cada día habilitado;
- preparación en Casa Central y preparación para envío expresadas en horas;
- retiro en local, puntos de entrega y envío como modalidades configurables;
- el acceso `Administrar puntos`;
- un recorrido desde pedido recibido fuera de horario hasta la siguiente franja válida del punto;
- la regla de que el reloj productivo solo consume horas operativas y puede avanzar antes del estimado.

### 15.2 Administración de puntos

`MC-ADM-CFG-009` muestra:

- búsqueda y filtro de puntos;
- alta y edición de su definición estructural;
- nombre, dirección, días de operación y franja horaria por punto;
- estados Operativo y Deshabilitado;
- acciones Habilitar y Deshabilitar;
- resumen de puntos registrados, operativos y deshabilitados;
- separación entre la estructura habitual del punto y las contingencias operativas cotidianas.

Estas dos imágenes son la evidencia visual vigente. Las pruebas y generaciones anteriores no deben utilizarse para presentación.

## 16. Referencias

- `analisis/especificacion-de-requerimientos/actualizacion-a-revision-reglas-y-requerimientos-configurables.md`
- `analisis/historias-de-usuarios/actualizacion-a-revision-historias-producto-configurable.md`
- `analisis/casos-de-uso/09-configuracion-del-sistema/actualizacion-a-revision-casos-de-uso-configuracion.md`
- `analisis/casos-de-uso/10-disponibilidad-operativa/actualizacion-a-revision-casos-de-uso-disponibilidad.md`
- `marco-del-proyecto/actualizacion-a-revision-producto-configurable.md`
- `marco-del-proyecto/actualizacion-a-revision-motor-configuracion-versionada.md`

## 17. Registro de la revisión visual

| Fecha | Cambio | Justificación | Estado |
|---|---|---|---|
| 2026-09-10 | Se vinculan los dos mockups finales confirmados | Cierra la evidencia de horarios, tiempos, modalidades y administración de puntos sin incorporar versiones intermedias | Aprobado para revisión del equipo |
