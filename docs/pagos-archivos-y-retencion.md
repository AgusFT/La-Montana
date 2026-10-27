# Pagos, archivos privados y recuperación · 27/10/2026

## Contrato vigente y reemplazo de la regla anterior

Esta decisión reemplaza las leyendas que condicionaban la **carga del PDF** al pago total o la seña. También reemplaza la derivación automática a revisión humana por superar un umbral. Aplica a la implementación del motor en `desarrollo` y a las definiciones de E19 #197, Fase 2 #210, Fase 3 #216, Fase 6 #222 y Fase 7 #224. Las imágenes y comentarios anteriores se conservan como evidencia histórica; sus leyendas incompatibles no son especificación vigente.

Recorrido: **carga → almacenamiento privado → validación técnica/cálculo → condición económica → aprobación/autorización → producción**. La implementación actual conserva la cotización/aceptación explícita y la inspección/aceptación de la vista previa antes de confirmar el pedido. No redefine precios usando un archivo no inspeccionado ni elimina esos controles.

El PDF puede recibirse y el pedido crearse antes de cobrar. Existencia, inspección y acceso autorizado para revisión **no equivalen a autorización para imprimir**. El servidor mantiene los controles de contenido, integridad, permisos y cobertura económica. Una transferencia informada o un comprobante no crean dinero; se reutilizan recepción, aplicación y devolución del libro financiero existente.

## Fase 2: condiciones certificadas

| Modelo/condición | Archivo y creación | Aprobación y producción |
|---|---|---|
| Control manual | PDF privado, validación y pedido sin exigir anticipo para cargar | Revisión y aprobación humanas; antes de aprobar debe estar acreditada/aplicada la seña que corresponda. El saldo se exige antes de entregar. |
| Pago total previo | PDF privado y pedido antes de pagar | Total acreditado y aplicado por transferencia, controles técnicos y demás condiciones válidas. |
| Seña previa | PDF privado y pedido antes de pagar | Seña acreditada y aplicada por transferencia antes de aprobar/producir. Efectivo sólo para el saldo si está habilitado. |
| Aprobación según monto | El total se compara con el umbral de Fase 3 | Superarlo aplica la seña configurada cuando esté habilitada; **no inventa revisión humana**. Sin anticipo exigible se conserva el saldo antes de entregar. |

Se mantienen decimales exactos, redondeo HALF_UP, seña fija limitada al total y los medios admitidos por cada condición. El backend comprueba las condiciones cotizadas y las revalidadas al confirmar; no basta editar el navegador.

## Fase 3: dos relojes independientes

1. **Plazo para acreditar el pago:** decisión comercial explícita, expresable en minutos, horas o días. Se normaliza a minutos enteros, entre 1 y 43.200 (30 días, límite operativo de esta implementación). Comienza al confirmar/crear el pedido, no al cargar un PDF ni al aceptar una oferta.
2. **Recordatorio opcional:** minutos enteros positivos, estrictamente anteriores al vencimiento. No acredita, extiende, elimina ni autoriza. Se presenta en el pedido y se envía por el correo configurado; sin SMTP operativo no debe considerarse probado el envío real.
3. **Conservación posterior al vencimiento:** exclusivamente **24 / 48 / 72 horas**; 72 es el valor inicial recomendado. No es un campo libre ni un plazo bancario.

Ejemplo: pedido creado a las 17:45, plazo de 15 min, vence a las 18:00. Con retención de 72 h, el contenido sólo resulta elegible para limpieza a las 18:00 de tres días después, si no existe ningún bloqueo de conservación.

Vencer el plazo comercial **no elimina inmediatamente** ni requiere recrear el pedido. Las fases 6 y 7 muestran esta política guardada. La validación de Fase 6 y la activación/programación rechazan políticas faltantes o incompatibles en nuevas versiones.

## Estados financieros y autorización

Se reutilizan los estados operativos existentes. `cobro_pedido` agrega el seguimiento temporal y las excepciones; no duplica el libro de dinero.

| Concepto mostrado | Comportamiento |
|---|---|
| Pendiente de pago | Pedido/PDF existen; falta el anticipo exigible. No aprobar ni producir. |
| Pago informado · pendiente de verificación | Suspensión obligatoria de purga. No acredita ni autoriza producción. |
| Pago acreditado | Debe estar aplicado y cubrir las condiciones exigibles; no sustituye revisión humana, técnica o logística. |
| Condición sin anticipo | No simula dinero cobrado. Se mantienen revisión/aprobación cuando correspondan y saldo antes de entregar. |
| Pago vencido · recuperable | Puede informarse una transferencia dentro de la retención; no se exige otro pedido. |
| Pago informado fuera de término | Conservación suspendida; requiere verificación y resolución administrativa explícita. |
| Resolución administrativa pendiente | No se purga hasta resolver. Rechazar un informe no genera un plazo nuevo. |
| Retención técnica agotada | Sin pagos/informes/gestiones pendientes, el proceso puede cerrar y revocar/eliminar. |
| Cancelado con finanzas pendientes | Acceso al PDF de trabajo revocado, contenido conservado hasta resolver informes y devolver dinero. No reabre producción. |
| Vencido definitivamente / cerrado | Sin autorización de impresión; historia conservada y contenido elegible eliminado de forma auditada. |

Un informe dentro del plazo se verifica con el flujo financiero existente. Un informe tardío, rechazado o un comprobante presentado sobre un informe abre una intervención administrativa explícita. La inspección técnica del comprobante no determina autenticidad bancaria. Mientras se valida ese comprobante tampoco puede levantarse la conservación mediante una resolución final.

La aprobación condicional diferida se registra cuando el dinero queda aplicado y todos los controles se cumplen. El actor financiero queda en auditoría; esto **no simula revisión humana del contenido**. El control manual nunca se aprueba sólo por cobrar.

## Resolución administrativa

Sólo `ADMIN_ADMIN`, con sesión y CSRF válidos, puede usar `POST /api/operacion/pedidos/{pedido}/cobro/resolver`. La gestión normal conserva los permisos financieros y de cancelación existentes. Cada resolución exige versión vigente, operación idempotente y motivo visible al cliente.

| Acción | Efecto |
|---|---|
| Aceptar el caso y continuar | Primero acreditar y aplicar el anticipo; resolver informes pendientes. Levanta la excepción temporal, no crea dinero ni omite revisión humana. |
| Mantener pendiente | Conserva el bloqueo administrativo y suspende purga. |
| Extender plazo | Fecha futura, posterior a la anterior y como máximo 30 días desde la resolución. Recalcula retención desde esa fecha y permite nuevo recordatorio. No elimina informes pendientes. |
| Solicitar nuevo comprobante | Conservación suspendida e indicación al cliente. |
| Rechazar informe | Acción existente del movimiento de pago. Audita el motivo, mantiene intervención pendiente y **no reinicia el reloj**. |
| Cancelar pedido | Gestión existente con motivo/permisos, antes de producción. Revoca contenido y autorización. Si hay dinero o informes, la resolución financiera continúa pendiente. |
| Finalizar resolución financiera de cancelación | Sólo para pedidos ya cancelados/rechazados, sin informes ni dinero por devolver. Permite la limpieza; no reabre el pedido. |

Toda decisión conserva actor, fecha, operación, motivo y resultado. Reintentar la misma operación no duplica efectos; reutilizarla con otros datos se rechaza. No se cancela un pago real como si fuera un simple adjunto.

## Limpieza, privacidad y concurrencia

- La tarea toma los mismos bloqueos transaccionales que pedidos, pagos y comprobantes. Informar/creditar y purgar se serializan; se revalida justo antes de borrar.
- Se suspende si hay informe sin resolver, dinero real sin devolver, intervención administrativa, corrección/reprogramación pendiente, comprobante en validación o trabajo imprimiéndose.
- Primero se confirma la revocación durable y el estado final. Después se elimina el directorio del PDF de trabajo, incluyendo originales y vistas previas. Las descargas verifican revocación antes y después de leer/renderizar; responden 410 cuando corresponde.
- El contenido físico se elimina; **no se borran** pedido, cliente, importe, condiciones, pagos, reservas históricas, metadatos ni auditoría. Los comprobantes financieros no son objetivos de esta purga.
- Una cancelación sin obligaciones puede limpiarse sin esperar el vencimiento comercial. Con dinero pendiente, requiere devolución y finalización administrativa explícita.
- Los fallos de storage dejan acceso revocado, motivo de reintento y registro durable. El borrado es idempotente; no depende de una bandera opcional del frontend.
- Intervalo inicial de limpieza: 60 s, inicio 15 s después del arranque; propiedades `lamontana.archivos.conservacion-intervalo-ms` y `lamontana.archivos.conservacion-inicio-ms`. Elegibilidad no garantiza borrado en el segundo exacto.
- Los recordatorios usan una salida persistida única por pedido/vencimiento. Un reintento SMTP puede repetir correo si el proceso cae después de enviarlo pero antes de registrar el éxito; no repite efectos financieros.

**Límite de integración:** el piloto registra producción manual; CUPS/agente y cola externa no están implementados. Se bloquean nuevos trabajos en servicio y SQL. Cuando exista un agente deberá consumir exclusivamente trabajos autorizados, volver a comprobar su vigencia y cancelar los aún no ejecutados al revocar. No se publica un endpoint anónimo para PDFs ni se afirma que esa integración futura ya esté certificada.

## Versiones anteriores y no retroactividad

La migración V42 no crea vencimientos para pedidos anteriores ni reescribe sus condiciones, pagos o historia. Los nuevos pedidos capturan la política de la versión validada al confirmar. Una configuración activa/histórica legada sin política mantiene su lectura; no se le inventa un plazo comercial. Los nuevos borradores deben definirla expresamente antes de activarse. Cambiar el motor no modifica los relojes ya capturados: una extensión sólo corresponde a una resolución auditada del pedido concreto.

## Verificación y prueba manual

Consultar [trazabilidad y casos de uso](trazabilidad-pagos-archivos.md). Verificación dirigida de esta entrega: **11 pruebas HTTP de integración, 47 backend unitarias/seguridad, 51 frontend**, build de frontend y compilación del backend, 42 migraciones en base aislada y prueba de navegador con componentes reales/API simulada. No representa la suite histórica completa. La prueba `pagosArchivosV42CargaPreviaRecuperacionYPurga` recorre APIs reales, PDF inspeccionado, contenido privado, pagos, resolución, revocación, limpieza y recordatorio; también prueba rechazos de políticas inválidas y producción con un equipo global sin ubicación. En este entorno se usa PostgreSQL embebido PGlite por el protocolo JDBC y migraciones ejecutadas previamente; **no sustituye certificación concurrente con PostgreSQL/Docker nativo**. El test conserva por defecto el PostgreSQL nativo embebido para CI; el modo alternativo se limita a una base local aislada suministrada explícitamente.

Para revisar en una instalación existente: `git pull --ff-only` en `desarrollo` y `./infra/iniciar-local.sh`. El script reconstruye/levanta los servicios y ejecuta la migración. **No borrar volúmenes.** Crear un borrador nuevo, configurar Fases 2/3, revisar Fases 6/7 y activar; después crear pedidos nuevos. No acortar relojes de pedidos reales para probar: usar instalación/base de ensayo.
