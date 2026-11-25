# Cliente: tres etapas, presupuesto global y entrega separada

Actualización implementada en `desarrollo` · 27/10/2026. Base: `18c834b4989b0aed1ddc83e1de17e81ddcfe7bc4`.

## Alcance y referencias

Se recupera la composición de MC-CLI-002 (Inicio), 003/004 (preparación), 005 (resumen) y 006 (pedido actual), manteniendo los PNG originales sin sobrescribirlos. Se reutilizan logo/montañas, shell, tarjetas, identidad azul/blanca y acciones naranjas. Las imágenes son referencias visuales, no contratos de precios, tipos de archivo, estados ni códigos de retiro.

Rigen [oferta global](alcance-global-imprenta-y-recorrido-cliente.md), [casa matriz versionada](casa-matriz-y-retiro-versionado.md) y [pagos/retención V42](pagos-archivos-y-retencion.md). Esta actualización reemplaza las descripciones anteriores que pedían sucursal, entrega y pago antes de presupuestar, o dividían la confirmación nueva en varias pantallas.

## Recorrido implementado

| Etapa | Vista y decisiones | Persistencia y límites |
|---|---|---|
| 1. Prepará tu trabajo | PDF, análisis, servicio, tamaño, papel/gramaje, impresión, simple/doble faz, copias inicialmente 1, adicionales compatibles separados. CTA Revisar presupuesto. | Una preparación abierta por cliente; hasta 20 PDF, 10 MiB cada uno. Antivirus e inspección PDFBox en servidor. Subtotal sin entrega/pago y sin sucursal productiva elegida por el cliente. |
| 2. Revisá y confirmá | Un único resumen con tarjetas del trabajo, entrega, medio, total, estimación, condiciones, revisión de páginas y aceptación final. | Recalcula mediante cotizaciones inmutables sucesoras. Revalida versión/huella, archivo, reglas y cupo al crear. Cierra preparación, acepta oferta y crea pedido en una misma transacción. |
| 3. Seguí tu pedido | Número real, timeline, detalles, PDF autorizados, importes aplicados, saldo, destino y acciones según estado. | Consulta el pedido ya persistido. Crear no equivale a aprobar ni producir. Se preservan cancelación, corrección, cobro, entrega, código de retiro temporal e historial. |

Los PDF recibidos y el presupuesto sobreviven a la recarga. Los selectores todavía no presupuestados no se presentan como guardados. Para reemplazar un PDF se quita el anterior y se carga otro; quitar/cargar invalida el presupuesto. Se exige incluir todos los archivos de la preparación al presupuestar o quitar los que no se usarán. Los originales no se copian por cada recálculo de entrega.

## Globalidad y logística

El navegador **no envía un origen productivo en el contrato de preparación**. El servidor obtiene la casa matriz activa, explícitamente confirmada. Si no existe una matriz válida, no elige la primera sucursal ni inventa un origen.

- `oferta.sucursal` conserva el origen interno para permisos y compatibilidad con contratos existentes.
- `oferta.origenGlobal=true` identifica su nuevo significado.
- `oferta.entrega.destinoSucursal.sucursal` identifica el retiro elegido.
- `reserva_entrega.origen_publico` guarda matriz; `destino_publico`, el destino real.
- Retiro en matriz: preparación y franja de retiro.
- Otra sucursal: preparación, traslado y franja de ese destino, aunque no tenga impresoras.
- Punto de entrega: disponibilidad temporal, relación con matriz, costo y franja del punto.
- Domicilio: cobertura, costo, tiempo y franja de zona; receptor/teléfono exigidos.
- Un cambio de matriz entre oferta y confirmación exige revisar una oferta nueva.
- La reserva y las reprogramaciones verifican el destino real, sin quitar controles horarios ni de cupo.

Los pedidos anteriores sin `origenGlobal` se interpretan como legados. No se modifican sus JSON, precios, destinos, matrix histórica ni estados. El timeline usa la configuración confirmada y los movimientos registrados, no la versión actual del motor ni el mero paso del tiempo. Producción/calidad continúan agrupadas; no se inventa un evento independiente.

## Contratos API

Todas las rutas de preparación exigen sesión CLIENTE; las escrituras conservan CSRF y autorización por propietario. Los UUID no otorgan acceso por sí mismos. El proxy mantiene rutas/métodos explícitos y límites de tamaño; los PDF se transmiten como binario, no JSON.

Base: `/api/cliente/preparaciones`.

| Método/ruta | Operación |
|---|---|
| POST base | Crear o recuperar la única preparación abierta del cliente. |
| GET /{id} | Estado, versión, archivos, ítems, presupuesto, preferencias, cotización y pedido asociado. |
| POST /{id}/archivos | Iniciar carga con operación idempotente y nombre seguro. |
| GET /{id}/archivos y /{id}/archivos/{file} | Metadatos privados e información para el visor reutilizado. |
| GET /{id}/archivos/{file}/carga | Autorizar recepción antes de transmitir bytes. |
| PUT /{id}/archivos/{file}/contenido | Recibir PDF, analizar seguridad y contar/inspeccionar páginas reales. |
| GET /{id}/archivos/{file}/original y /paginas/{n} | Contenido privado con no-store, nosniff y sandbox. |
| POST /{id}/archivos/{file}/aceptar | Aceptar explícitamente la huella del PDF inspeccionado. |
| POST /{id}/archivos/{file}/quitar | Retirar del trabajo, invalidar presupuesto y revocar acceso de preparación. |
| POST /{id}/presupuesto | Operación, versión, referencias vigentes e ítems {archivo, trabajo}; precio exclusivamente backend. |
| GET /{id}/resumen | Preparación, cotización, revisión actual y huella de consentimiento. |
| POST /{id}/cotizar | Operación, versión, preferencias {modalidad, destinoRetiro, punto, direccion, medioPago}. |
| POST /{id}/confirmar | Operación, versión, cotización, huella, aceptación explícita y contacto si corresponde. Devuelve el pedido existente/creado. |

`GET /api/cliente/cotizaciones/opciones` añade matriz, destinos de retiro, gramaje/terminación de papel. El contrato antiguo conserva sus campos para compatibilidad. El nuevo formulario deduplica sus combinaciones globales y no las filtra por destino.

`GET /api/cliente/cotizaciones/{id}` añade `preparacion` cuando pertenece a este flujo: Mis cotizaciones enlaza al resumen actual en lugar de ofrecer el confirmador antiguo. Las ofertas históricas y las correcciones continúan usando los componentes controlados correspondientes. El POST de confirmación legado rechaza ofertas globales de preparación; deben cerrarse mediante su endpoint atómico.

## Confirmación, pagos y seguridad

La huella incluye oferta inmutable, configuración/condiciones actuales, franja y archivos aceptados. Cambiar entrega o pago invalida el consentimiento visual. Cambios detectados en backend exigen actualizar/revisar; no se cambia silenciosamente el importe aceptado.

Se reutilizan los locks de configuración, pedidos y pagos. La operación recuperable tiene actor, versión, tipo, hash y resultado auditados. Un doble envío devuelve el pedido guardado; no genera otra reserva ni otro pedido. Un comando reutilizado con otro contenido se rechaza. Los archivos preparados sólo pueden vincularse a sucesoras del mismo trabajo, dueño y cadena; un original no puede terminar en dos pedidos.

El pago se informa después de crear desde el panel real existente. Informar transferencia no acredita dinero. Anticipo total/seña debe estar acreditado y aplicado antes de aprobar/producir, según el modelo; manual conserva revisión humana. Se mantienen plazos, recordatorios, retención 24/48/72 h, recuperación, suspensión de purga por informes/dinero/gestión y resolución administrativa.

La preparación cerrada devuelve 410 al pedir su contenido; el acceso posterior usa los endpoints del pedido/cotización, sujetos a conservación/revocación. La purga conserva metadatos, oferta, dinero e historia. No se agrega un vencimiento automático para preparaciones abiertas sin pedido: una política de abandono general requeriría definición específica.

## Componentes y migración

Reutilizados: `ClientShell`, `MountainMark`, `ClientJourney`, `PrivateFiles`, `GuidedTour`, `OrderProgress`, `OrderManagement`, correcciones, entrega/reprogramación, panel financiero, almacenamiento/antivirus/PDFBox, precio, cotización y confirmación existentes.

Nuevos/adaptados:
- `QuoteForm`: preparación, cargas persistidas, selectores por PDF y presupuesto.
- `LegacyQuoteForm`: edición controlada de ofertas anteriores/correcciones; no agrega otro motor de precios.
- `PreparedCheckout` y `/cliente/preparaciones/[id]`: resumen único y creación.
- `ClientContextTour`: guías independientes por etapa; omitir/cerrar/reabrir no modifica el negocio.
- `prepared-work.ts`: contratos y comprobaciones de respuestas.
- `guided-tour.css`: reutiliza los estilos del spotlight existente para que funcione también fuera de instalación.
- `ArchivoPreparadoService`, `PreparacionClienteStore/Service/Controller`: coordinación del flujo privado.
- Dashboard: pedido en curso más reciente, criterio visible, otros activos y siguiente acción económica/corrección.
- Pedido actual: resumen inicial, timeline, archivos privados y consulta de importes reales; sin sucursal productiva presentada como elección del cliente.

**V43__preparacion_privada_y_destino_cliente.sql** crea preparación, asociación privada de archivos y eventos inmutables. Permite compartir el mismo original entre ítems de cotizaciones sucesoras controladas y conserva la unicidad del archivo en pedido. Amplía el guard de vista previa a archivos preparados y adapta los guards de confirmación/reserva/reprogramación al destino separado, manteniendo invariantes de V28/V33 y el significado legado. No modifica migraciones ya publicadas ni hace backfill de precios, fechas o matrices.

## Verificación realizada

- Build de frontend Next.js y TypeScript: aprobado.
- Compilación de producción backend: aprobada.
- **50 pruebas backend** de reglas, copia, recursos globales, matriz, instalación, precios, horarios y cadena de seguridad: aprobadas.
- **54 pruebas frontend/proxy**: aprobadas, incluidas 3 nuevas de rutas/métodos, PDF binario, CSRF/cookie y descarga privada.
- **8 escenarios HTTP nuevos**: aprobados. Spring real, sesiones/CSRF reales, PDFBox real, antivirus de ensayo y base efímera PGlite accesible por JDBC. Los 43 scripts V1–V43 se ejecutan en cada preparación de ensayo.
- Navegador Chromium con componentes React reales y **API simulada**: carga/configuración, selección global sin origen productivo, subtotal, resumen único, recálculo de destino/total, creación, dashboard/pedido actual, omitir/reabrir guía; sin errores JavaScript ni desborde a 320/390/768/1440 px.

Las pruebas HTTP no son simulaciones de respuestas del servicio. Sí usan PGlite y un antivirus de ensayo; **no equivalen a certificar PostgreSQL/Docker nativo, carreras concurrentes multiproceso, ClamAV real, SMTP externo ni CUPS/agente físico**. La prueba de navegador con API simulada no se presenta como prueba integral frontend/backend.

### Cobertura de los 22 escenarios solicitados

| Escenarios | Evidencia |
|---|---|
| 1–4 PDF/páginas, opciones, adicionales y subtotal antes de destino | HTTP nuevo + interfaz React; precios reutilizados y sus pruebas unitarias. |
| 5–8 matriz, secundaria sin impresoras propias, punto y domicilio | Cuatro variantes HTTP; destino y origen persistidos separados. |
| 9–12 recálculo, medio, aceptación y creación sin duplicados | HTTP de sucesoras/reintento/versión y navegador con API simulada. |
| 13–15 pedido actual, timeline y movimientos reales | Componentes React; regresión de order-tracking con estados persistidos representados. |
| 16–18 informe no acreditado, pedido sin pago y retención | HTTP nuevo: informe conserva sin aprobar; purga elimina bytes y conserva pedido. Reglas V42 y tests existentes. |
| 19–20 cancelación/auditoría y condiciones históricas | HTTP de cancelación sin pérdida de oferta; regresiones existentes y ausencia de backfill. Suite histórica completa pendiente. |
| 21–22 responsive y guía opcional | Navegador con API simulada: cuatro anchos y guía omitida/reabierta sin escrituras comerciales. |

## Cómo probar localmente sin borrar datos

Desde el repositorio, sin archivos propios pendientes que se vayan a pisar:

```bash
git switch desarrollo
git pull --ff-only
./infra/iniciar-local.sh
```

El script ya construye y levanta Docker Compose. No hace falta ejecutar otro `up` después. No usar `down -v`, `volume rm` ni borrar la base para ver esta actualización. Esperar a los servicios y la migración V43.

1. En administración, confirmar/activar una configuración con matriz, oferta comercial, impresoras globales y franjas/cupos de retiro. Habilitar temporalmente los puntos y cobertura que se quieran probar.
2. Entrar como CLIENTE y abrir Crear pedido. Cargar PDF real; ver páginas analizadas, configurar copias y adicionales y revisar presupuesto sin entrega/pago.
3. Recargar: recuperar archivos y presupuesto. Elegir retiro en matriz/secundaria, punto o domicilio y un medio habilitado. Revisar total/estimación actualizados.
4. Abrir todas las páginas y aceptar cada PDF; aceptar el resumen final y crear. Ver número real, condiciones y timeline. Recargar o reintentar no debe duplicarlo.
5. Con anticipo pendiente, comprobar que existe el pedido/PDF pero no se puede aprobar/producir. Informar transferencia y verificar que permanece pendiente de acreditación.
6. Acreditar/aplicar desde usuario autorizado, continuar revisión/producción/traslado/entrega. Ver cambios reales en el timeline.
7. Probar cancelación permitida y consultar historia; no simular devolución ni borrar evidencia.
8. Omitir y reabrir cada guía; cambiar ancho de ventana. Las selecciones del negocio no deben alterarse por la guía.

Pendientes de aceptación: recorrido completo en Docker/PostgreSQL/ClamAV reales por el equipo, suite histórica integral y certificación concurrente nativa, validación visual final con Alejandro/Agustín y política general de abandono de preparaciones si se decide incorporarla. No se implementa CUPS ni pasarela bancaria.
