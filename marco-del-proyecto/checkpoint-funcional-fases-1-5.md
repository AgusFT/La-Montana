# Checkpoint funcional — Motor de configuración Fases 1–5

| Campo | Valor |
|---|---|
| Estado | Cierre funcional previo a Fase 6 |
| Fecha | 2026-09-11 |
| Alcance | Validación transversal de Fases 1–5 |
| Relación | E19 #197 · PR #196 · Fase 6 #222 |

## 1. Objetivo

Consolidar las decisiones mínimas que deben quedar cerradas antes de diseñar la Fase 6 — Resumen, simulación y conflictos.

La Fase 6 no incorpora reglas nuevas de negocio: verifica, resume y simula la configuración definida en las Fases 1–5.

## 2. Regla general de avance del asistente

Cada fase posee condiciones mínimas de validez.

- El administrador puede guardar un borrador incompleto.
- No puede avanzar a la siguiente fase mientras la fase actual no cumpla sus mínimos obligatorios.
- La Fase 6 vuelve a validar el conjunto completo, pero no reemplaza las validaciones locales de cada fase.
- Para que una versión pueda activarse o programarse, todas las fases obligatorias deben ser válidas.

## 3. Fase 1 — Estado actual e inicio de configuración

Se mantiene:

- exactamente una configuración activa;
- como máximo un único cambio pendiente;
- el cambio pendiente puede ser borrador o versión programada, nunca ambos;
- solo el borrador es editable;
- una versión programada es inmutable hasta activarse o cancelarse;
- los cambios estructurales se realizan creando o continuando un borrador y recorriendo el motor completo.

Los horarios, capacidades, pagos y demás compromisos habituales no deben exponerse como atajos de edición directa desde el dashboard.

## 4. Fase 2 — Modelo operativo y aprobación

Para V1 se conserva una única condición certificada cuando se utiliza Control condicional.

No se habilita un constructor libre de combinaciones. Las condiciones certificadas vigentes siguen siendo:

- pago previo;
- pago de seña;
- monto del pedido.

La automatización avanzada permanece fuera de V1.

## 5. Fase 3 — Pagos y reglas de seña

Se mantiene la matriz ya definida:

- Control manual: medios flexibles y seña opcional;
- Pago previo: exige acreditación total mediante medio acreditable;
- Pago de seña: condición obligatoria heredada de Fase 2, con tipo y valor configurables;
- Monto del pedido: hasta el umbral puede existir autoaprobación sin seña; por encima se exige seña acreditable.

No se incorporan reglas adicionales en este checkpoint.

## 6. Fase 4 — Impresoras, capacidades y asignación

### 6.1 Condición mínima de validez

Para completar Fase 4 y continuar debe existir al menos una impresora en estado **Operativa**, correctamente configurada.

El administrador puede guardar un borrador aunque todavía no haya completado la fase, pero no puede avanzar mientras no exista una alternativa productiva operativa.

La Fase 6 vuelve a verificar esta condición como integridad global.

### 6.2 Deshabilitación y trabajos existentes

Deshabilitar una impresora impide nuevas asignaciones.

No modifica silenciosamente trabajos ya asignados o en curso. Esos trabajos conservan su trazabilidad y deben resolverse operativamente cuando corresponda.

La recarga de papel y la selección de impresora para un trabajo siguen siendo acciones operativas no versionadas.

## 7. Fase 5 — Horarios, puntos de entrega y envíos

### 7.1 Horarios como compromiso estructural

Los días y horarios operativos son parte de la configuración versionada y representan un compromiso habitual de la imprenta con sus clientes.

No deben modificarse mediante un acceso rápido de dashboard.

Un cambio de días, apertura o cierre requiere:

`crear/continuar borrador → recorrer el motor → revisar → activar o programar nueva versión`.

La disponibilidad temporal de un punto o una contingencia operativa continúan fuera del versionado.

### 7.2 Tiempos estimados

Se consolidan dos duraciones configurables:

1. **Tiempo estimado de preparación**: tiempo operativo estimado hasta que el trabajo queda terminado.
2. **Tiempo estimado de traslado/envío**: tiempo adicional estimado cuando el pedido debe salir de Casa Central hacia un punto o modalidad de envío.

Los tiempos son estimaciones y no bloqueos artificiales.

Para retiro en Casa Central:

`disponibilidad estimada = preparación`

Para punto de entrega o envío:

`disponibilidad estimada = preparación + traslado/envío`

Cuando existe una franja horaria de punto, el resultado se ajusta a la siguiente franja válida.

La imprenta configura estas estimaciones porque conoce su operación, distancias y compromisos reales.

No se define un tiempo distinto por cada punto en V1.

### 7.3 Estado real y timeline del cliente

La estimación nunca reemplaza el estado real.

- Si el pedido se adelanta, el timeline avanza antes y el cliente puede ser notificado.
- Si el pedido se demora, el timeline refleja el nuevo estado o estimación y alerta al cliente cuando corresponda.
- Las franjas horarias siguen siendo flexibles y la coordinación fina puede resolverse por WhatsApp u otro canal.

### 7.4 Cambios estructurales de puntos

Crear o editar la definición habitual de un punto —nombre, dirección, días o franjas— es un cambio estructural y forma parte de una nueva configuración versionada.

En cambio, habilitar o deshabilitar temporalmente un punto sigue siendo una acción operativa inmediata y no versionada.

### 7.5 Modalidad mínima de entrega

Para completar Fase 5 debe existir al menos una modalidad real de entrega entre:

- Retiro en local;
- Puntos de entrega;
- Envío.

Si la única modalidad seleccionada es **Puntos de entrega**, debe existir al menos un punto utilizable/habilitado para nuevos pedidos.

Sin una modalidad válida, la fase no permite continuar porque el sistema no podría completar el ciclo del pedido.

## 8. Cotizaciones y cambio de versión

Una cotización captura la configuración vigente al momento de ser generada y conserva esas condiciones mientras continúe vigente.

Si se activa una nueva versión:

- las cotizaciones nuevas utilizan la nueva versión;
- las cotizaciones anteriores todavía vigentes conservan la versión con la que fueron generadas hasta confirmar o vencer;
- un pedido confirmado conserva las condiciones capturadas;
- una activación no fuerza una recotización de una cotización todavía vigente.

La posibilidad de programar una activación permite al propietario elegir horarios de baja demanda y reducir la convivencia temporal de cotizaciones pertenecientes a versiones diferentes.

Esta decisión reemplaza la interpretación previa del DER v3.2 que invalidaba automáticamente cotizaciones no confirmadas al activarse una nueva versión y debe alinearse en la revisión v3.3.

## 9. Alcance diferido

Quedan fuera de V1 del motor actual:

- catálogo de módulos y composición de planes;
- asignación automática certificada;
- planificación dinámica por saturación o carga;
- tiempos logísticos específicos por cada punto;
- configuración avanzada de canales de notificación;
- cuenta corriente como funcionalidad comprometida, hasta cierre de su Spike.

## 10. Consecuencia para Fase 6

La Fase 6 debe trabajar únicamente sobre decisiones ya válidas y debe:

- resumir Fases 1–5;
- permitir volver a editar una fase;
- simular un pedido de punta a punta;
- distinguir Bloqueos, Advertencias e Información;
- detectar inconsistencias globales que no correspondan a una sola pantalla;
- no introducir parámetros nuevos.

Ejemplos de bloqueos globales:

- no existe una impresora Operativa;
- no existe ninguna modalidad real de entrega;
- pago previo o seña sin medio acreditable;
- horario o tiempo estimado inválido;
- modalidad Puntos de entrega seleccionada sin ningún punto utilizable.

## 11. Estado

Con estas decisiones, las Fases 1–5 quedan funcionalmente cerradas para avanzar al diseño de Fase 6, sin perjuicio de la revisión final del equipo y de la alineación técnica del DER v3.3.
