# WF - Fase 5 - Horarios, puntos de entrega y envíos

| Campo | Valor |
|---|---|
| Versión | 1.2 - Propuesta |
| Estado | Cierre funcional previo a Fase 6 |
| Fecha | 2026-09-11 |
| Fase | Fase 5 - Horarios, puntos de entrega y envíos |
| Vistas relacionadas | WF-CFG-08, WF-CFG-09 |
| Actor de configuración | ADMIN_ADMIN |
| Actores operativos | Usuario interno autorizado, Sistema, Cliente |

> Este documento consolida las decisiones funcionales y de navegación de Fase 5. La definición comercial de módulos continúa fuera de esta fase.

## 1. Objetivo

Permitir que la imprenta configure un calendario operativo estable, tiempos estimados de preparación y traslado, y modalidades reales de entrega, manteniendo separadas las decisiones estructurales de las contingencias operativas cotidianas.

La fase responde:

1. ¿Cuándo trabaja la imprenta y cuándo corre el reloj productivo?
2. ¿Cuánto tiempo estimado necesita para terminar el trabajo?
3. ¿Cuánto tiempo adicional estima para trasladarlo o enviarlo cuando sale de Casa Central?
4. ¿Qué modalidades de entrega ofrece y en qué franjas pueden utilizarse?

## 2. Regla de avance

Fase 5 posee mínimos obligatorios.

- El borrador puede guardarse aunque esté incompleto.
- No puede avanzarse a Fase 6 si la fase no es válida.
- Debe existir al menos una modalidad real de entrega entre Retiro en local, Puntos de entrega o Envío.
- Si la única modalidad habilitada es Puntos de entrega, debe existir al menos un punto utilizable para nuevos pedidos.

Fase 6 volverá a verificar estas condiciones como integridad global, pero no reemplaza la validación local.

## 3. CFG-008 - Horarios operativos

### 3.1 Configuración por día

Cada día se configura de forma independiente con:

- estado Habilitado/Cerrado;
- hora de apertura;
- hora de cierre.

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

Los horarios habituales son un **compromiso estructural** de la imprenta. No se modifican mediante un atajo de dashboard.

Cambiar días, apertura o cierre requiere:

`crear/continuar borrador → recorrer el motor → revisar → activar o programar nueva versión`.

### 3.2 Regla de cómputo operativo

Los tiempos estimados se consumen únicamente dentro de las ventanas operativas configuradas.

Si un pedido ingresa fuera de horario:

- puede ser recibido;
- queda En cola;
- el cómputo comienza en la próxima apertura comercial.

Si la preparación cruza el cierre, el remanente continúa desde la siguiente apertura.

Ejemplo:

- pedido recibido: 02:00;
- próxima apertura: 08:00;
- preparación configurada: 4 h;
- listo estimado en Casa Central: 12:00.

## 4. Tiempos estimados

Los valores se expresan en horas y son configurados por el propietario de la imprenta según su operación real.

### 4.1 Tiempo estimado de preparación

Campo conceptual:

`Tiempo estimado de preparación: [ 4 ] hs`

Representa el tiempo operativo estimado hasta que el trabajo queda terminado.

Para retiro en Casa Central:

`disponibilidad estimada = preparación`

### 4.2 Tiempo estimado de traslado/envío

Campo conceptual:

`Tiempo estimado de traslado/envío: [ N ] hs`

Representa el tiempo adicional estimado cuando el pedido debe salir de Casa Central hacia un punto de entrega o modalidad de envío.

Para punto o envío:

`disponibilidad estimada = preparación + traslado/envío`

No se define un tiempo distinto por cada punto en V1. La imprenta establece una estimación general razonable según su operatoria.

### 4.3 Estimación vs estado real

Los tiempos son estimaciones, no una espera obligatoria ni una garantía rígida.

- Si el pedido termina antes, el flujo avanza inmediatamente.
- Si el pedido se demora, el timeline refleja el estado o nueva estimación real.
- El cliente puede ser alertado ante adelantos o demoras.
- El estado real siempre prevalece sobre la estimación inicial.

## 5. CFG-009 - Modalidades de entrega

La configuración puede habilitar:

- **Retiro en local**, sujeto al horario operativo;
- **Puntos de entrega**, sujetos a disponibilidad y franjas;
- **Envío**, sujeto a preparación + traslado/envío estimado.

El cliente solo visualiza opciones realmente disponibles.

Debe existir al menos una modalidad utilizable para considerar válida la fase.

## 6. Administrar puntos

La acción **Administrar puntos** conduce a un panel dedicado.

Cada punto puede definir:

- nombre;
- ubicación o dirección;
- días habituales;
- una o más franjas horarias;
- estado operativo Habilitado/Deshabilitado.

Las franjas son ventanas estimadas y flexibles, no una cita exacta. La coordinación fina puede resolverse posteriormente por WhatsApp u otro canal.

### 6.1 Estructura versionada

Crear o editar:

- nombre;
- dirección;
- días habituales;
- franjas horarias habituales;

es un cambio estructural y forma parte de una nueva configuración versionada.

No se permite tratar estas modificaciones habituales como un atajo operativo.

### 6.2 Disponibilidad temporal no versionada

Habilitar o deshabilitar temporalmente un punto:

- es una acción operativa inmediata;
- no solicita motivo obligatorio;
- no requiere fecha de fin;
- no crea una nueva versión;
- afecta la oferta a nuevos pedidos;
- no reasigna automáticamente pedidos ya pactados.

Mientras exista algún punto deshabilitado, el dashboard mantiene un recordatorio visible.

## 7. Franjas y disponibilidad

El cálculo distingue:

- horario operativo de la imprenta: determina cuándo corre preparación;
- tiempo de traslado/envío: agrega demora estimada cuando corresponde;
- franja del punto: determina la siguiente ventana válida en la que puede ofrecerse disponibilidad.

Ejemplo:

- preparación estimada termina a las 12:00;
- traslado estimado: 3 h;
- llegada estimada: 15:00;
- punto disponible de 17:00 a 21:00;
- el cliente ve disponibilidad dentro de la siguiente franja válida, no a las 15:00.

## 8. Timeline del cliente

El timeline debe mostrar tanto estimación como avance real.

Ejemplo:

`Pedido recibido → En cola → En preparación → Listo → En camino → Listo para retirar → Entregado`

Reglas:

- si el pedido entra fuera de horario, En cola explica la próxima apertura;
- si se adelanta, el timeline avanza antes y puede notificar al cliente;
- si se demora, el timeline refleja la nueva situación y puede alertar al cliente;
- la coordinación fina del punto puede resolverse externamente sin perder trazabilidad general.

## 9. Separación configuración / operación

| Decisión | Contexto | Genera nueva versión |
|---|---|---:|
| Cambiar horarios operativos habituales | Motor de configuración - Fase 5 | Sí |
| Cambiar tiempo de preparación | Motor de configuración - Fase 5 | Sí |
| Cambiar tiempo de traslado/envío | Motor de configuración - Fase 5 | Sí |
| Habilitar/deshabilitar una modalidad habitual | Motor de configuración - Fase 5 | Sí |
| Agregar o editar definición estructural de punto | Motor / Administrar puntos estructural | Sí |
| Habilitar o deshabilitar temporalmente un punto | Operación / dashboard | No |
| Mostrar recordatorio de puntos deshabilitados | Dashboard | No |
| Adelantar o actualizar ETA de un pedido | Operación del pedido | No |

## 10. Simulación de Fase 5

La vista debe conservar el patrón narrativo de las fases anteriores.

Ejemplo base:

1. Pedido recibido fuera de horario.
2. Queda En cola.
3. Comienza en la próxima apertura.
4. Consume el tiempo de preparación.
5. Queda listo en Casa Central.
6. Si corresponde, suma traslado/envío.
7. Si corresponde, se ajusta a la siguiente franja válida del punto.
8. El estado real puede adelantar o retrasar el recorrido estimado.

## 11. Módulos fuera de alcance

No se diseñan módulos en esta fase.

Queda para una etapa futura definir catálogo, planes y dependencias comerciales.

Principio preservado: una mejora opcional futura no debe impedir completar el flujo básico del negocio.

## 12. Criterios de aceptación

- cada día se configura individualmente;
- los horarios habituales son estructurales y versionados;
- los pedidos fuera de horario quedan en cola hasta la próxima apertura;
- preparación y traslado/envío son dos estimaciones diferenciadas;
- preparación + traslado/envío se suman cuando corresponde;
- las franjas de los puntos condicionan la ventana ofrecida;
- el estado real puede adelantar o retrasar el timeline;
- crear/editar un punto es versionado;
- habilitar/deshabilitar temporalmente un punto no es versionado;
- debe existir al menos una modalidad real de entrega;
- si solo se utilizan puntos, debe existir al menos uno utilizable;
- el dashboard recuerda puntos temporalmente deshabilitados;
- módulos no forman parte de Fase 5.

## 13. Evidencia visual aprobada

La revisión del 10/09/2026 confirmó exactamente dos mockups vigentes:

| Evidencia | Alcance |
|---|---|
| [MC-ADM-CFG-008-009 - Horarios, puntos y envíos](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-008-009-horarios-puntos-envios.png) | Pantalla principal de Fase 5 |
| [MC-ADM-CFG-009 - Administrar puntos de entrega](../vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-009-administrar-puntos-entrega.png) | Panel dedicado de puntos |

Los valores numéricos visibles en los mockups son ejemplos configurables. La actualización funcional del 11/09/2026 aclara su semántica sin reconstruir ni sustituir las imágenes aprobadas.

## 14. Referencias

- `marco-del-proyecto/checkpoint-funcional-fases-1-5.md`
- `analisis/especificacion-de-requerimientos/actualizacion-a-revision-reglas-y-requerimientos-configurables.md`
- `analisis/historias-de-usuarios/actualizacion-a-revision-historias-producto-configurable.md`
- `analisis/casos-de-uso/09-configuracion-del-sistema/actualizacion-a-revision-casos-de-uso-configuracion.md`
- `marco-del-proyecto/actualizacion-a-revision-producto-configurable.md`
- `marco-del-proyecto/actualizacion-a-revision-motor-configuracion-versionada.md`

## 15. Registro de revisión

| Fecha | Cambio | Estado |
|---|---|---|
| 2026-09-10 | Se vinculan los dos mockups finales confirmados | Aprobado para revisión |
| 2026-09-11 | Se cierran preparación + traslado/envío, versionado estructural de puntos y modalidad mínima obligatoria | Confirmado para Fase 6 |
