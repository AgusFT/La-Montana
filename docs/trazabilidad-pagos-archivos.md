# Trazabilidad · pagos y conservación de archivos

Contrato rector: [Pagos, archivos privados y recuperación](pagos-archivos-y-retencion.md), 27/10/2026. Los identificadores `PYA` son nuevos identificadores de esta corrección, no renumeraciones de RF/RN/HU del repositorio documental anterior. Si una referencia histórica exige pagar antes de cargar el PDF o revisión humana automática por monto, prevalece este contrato para la nueva implementación. No se altera el historial de E03 ni las evidencias visuales previas.

## Reglas de negocio y requerimientos

| Regla | Requerimiento implementado | Evidencia técnica | Tarea |
|---|---|---|---|
| RN-PYA-01: existencia ≠ autorización | RF-PYA-01: recibir e inspeccionar PDF y crear pedido sin anticipo; impedir aprobar/producir sin cobertura | ArchivoService, PedidoService, RevisionPedidoService, ProduccionService, triggers V42 | #210 / #216 |
| RN-PYA-02: umbral no impone intervención humana | RF-PYA-02: total previo, seña previa y aprobación según monto; revisión humana sólo en modelo manual o revisión explícita | EvaluadorFinanciero, resumen financiero, timeline | #210 |
| RN-PYA-03: dos relojes | RF-PYA-03: plazo 1 min–30 días, recordatorio anterior opcional y retención 24/48/72 h desde vencimiento | PoliticaPagoArchivos, Fase 3, CHECK SQL | #216 |
| RN-PYA-04: protección obligatoria de evidencia | RF-PYA-04: informe, comprobante o intervención suspenden la purga en backend | PagoService, ComprobanteService, CobroPedidoService, funciones cobro_* | #216 |
| RN-PYA-05: recuperación sin recrear | RF-PYA-05: informar tarde dentro de retención y resolver con actor/motivo/fecha/resultado | Panel de pago, endpoint cobro/resolver, evento_cobro_pedido | #216 |
| RN-PYA-06: rechazo no reinicia plazo | RF-PYA-06: nuevo plazo exclusivamente por extensión administrativa explícita | Resolver + control de versión/idempotencia | #216 |
| RN-PYA-07: eliminar contenido, conservar trazabilidad | RF-PYA-07: revocar acceso y trabajos, limpiar originales/previews sin borrar historia ni finanzas | eliminacion_archivo, scheduler, ArchivoService, historial | #216 / #224 |
| RN-PYA-08: dinero cancelado requiere resolución | RF-PYA-08: mantener conservación hasta devolver/resolver y finalizar cancelación | cobro_dinero_por_resolver + gestión del pedido | #216 |
| RN-PYA-09: backend autoritativo | RF-PYA-09: rol/CSRF, locks compartidos, verificación antes de borrar, reintentos auditados | SeguridadConfiguration, proxy exacto, transacciones y outbox | #197 |
| RN-PYA-10: política visible antes de activar | RF-PYA-10: Fases 6/7 muestran política y rechazan valores incompletos/incompatibles | PaymentArchiveSummary + validación financiera backend | #222 / #224 |
| RN-PYA-11: no retroactividad | RF-PYA-11: snapshots por pedido; configuraciones legadas sin vencimiento inventado | V42 sin backfill, copia de política versionada | #197 / #224 |

## Historias y criterios de aceptación

- **HU-PYA-01 — Cliente:** quiero enviar mi PDF y completar el pedido sin esperar la acreditación. Dado un archivo válido, cuando acepto la vista previa y confirmo, se crea un pedido pendiente de condición económica; no se autoriza producción. Un rechazo técnico sigue bloqueando la confirmación.
- **HU-PYA-02 — Cliente:** quiero informar una transferencia tardía sin repetir el trabajo. Dentro de la ventana recuperable, informar registra fecha/referencia y conserva archivos hasta resolución; el mensaje dice pendiente de verificación, nunca pago acreditado.
- **HU-PYA-03 — Administrador:** quiero definir plazos entendibles y seguros. Un recordatorio igual/posterior al plazo, una retención distinta de 24/48/72 o un plazo fuera de rango se rechazan sin guardado parcial. Fases 6/7 muestran la misma política.
- **HU-PYA-04 — Administrador:** quiero resolver excepciones sin perder dinero ni evidencia. Cada acción exige motivo, versión y operación; aceptar no acredita fondos, rechazar no cambia fecha, extender queda auditado y finalizar cancelación exige resolver dinero/informes.
- **HU-PYA-05 — Operación:** quiero imprimir sólo lo autorizado. Aun manipulando la UI, no se crea trabajo con archivo revocado o condición económica incumplida. La revisión manual no se omite por cobrar.
- **HU-PYA-06 — Auditoría:** quiero reconstruir qué pasó tras eliminar un PDF. Permanecen metadatos, pedido, condiciones, importes, eventos financieros, actor/fecha/motivo y resultado de limpieza/reintentos.

## Casos de uso

### CU-PYA-01 — Crear pedido antes de pagar

Precondición: oferta vigente aceptada, PDF autorizado para ese cliente. Cargar privado, inspeccionar antivirus/estructura/renderizado, aceptar vista previa y confirmar. El servidor revalida configuración, compatibilidad y cupo; captura política. Con anticipo no cubierto, conserva `PENDIENTE_REVISION` como estado operativo existente y publica el motivo financiero, sin fingir revisión humana. Alternativa: PDF inválido, oferta vencida o condiciones incompatibles → no confirmar; conservar respuesta explicativa.

### CU-PYA-02 — Informar pago normal o tardío

Precondición: pedido recuperable y cliente autorizado. Informar referencia/importe/motivo o adjuntar comprobante al informe existente. Los mismos locks que limpieza protegen la escritura. Resultado: conservación suspendida, sin acreditación ni autorización. Tardío → intervención explícita. Alternativa: retención agotada sin obligaciones o pedido definitivamente finalizado → rechazar nueva recuperación y orientar a la imprenta; nunca fabricar dinero.

### CU-PYA-03 — Verificar, aplicar y resolver

Interno con permiso verifica recepción real y aplica importe a la oferta; conserva informes/resoluciones. Administrador resuelve excepción con motivo. Aceptar exige anticipo cubierto e informes resueltos; manual sigue esperando su revisión. Extender recalcula vencimiento+retención. Solicitar comprobante/mantener conserva bloqueo. Rechazar informe conserva la fecha y abre intervención. Repetición de la misma operación → sin duplicar; versión desactualizada → conflicto y nueva consulta.

### CU-PYA-04 — Vencimiento y limpieza

Vence el pago → recuperable, sin borrar. Después de retención, si no hay informes, dinero, intervenciones, validaciones ni trabajos/gestiones que conservar: cierre auditable, liberación de reserva, revocación durable y borrado físico de originales/previews. Descargas posteriores → 410. Falla storage → mantener revocado y reintentar. Si se informa pago primero, la comprobación bloquea la limpieza; si la limpieza definitiva se confirmó primero, no se acepta una recuperación ficticia.

### CU-PYA-05 — Cancelar con o sin dinero

Usar cancelación preproducción y permisos existentes; registrar actor/motivo. Sin obligaciones, limpiar contenido conservando historia. Con dinero/informes, revocar acceso pero conservar contenido y mostrar resolución financiera pendiente. Resolver informes, devolver dinero cuando corresponda y finalizar resolución; entonces habilitar limpieza. Nunca borrar pagos para cerrar un pedido.

## Wireflows y efectos de botones

| Vista existente | Cambios funcionales / navegación |
|---|---|
| WF-CFG-02/03 · Fase 2 | Seleccionar condición certificada con nuevos nombres; ejemplo siempre permite cargar privado. Pago/seña se exigen para aprobar/producir, no subir. |
| WF-CFG-04/05 · Fase 3 | Guardar política con plazo/unidad, aviso opcional y selector 24/48/72. Guardar no activa. Simular no crea dinero, pedidos ni archivos. |
| WF-CFG-10/11 · Fase 6 | Resumen «Pagos y archivos», misma política guardada y bloqueos de completitud. Editar vuelve a Fase 3. |
| WF-CFG-12/13/14 · Fase 7 | Resumen sólo lectura, misma seguridad de activación/programación. Sin campo libre de retención. No cambia pedidos existentes. |
| Cliente · pedido/pagos | «Informar pago» registra aviso; después muestra verificación pendiente y conservación suspendida. PDF revocado no muestra acciones de descarga/vista previa. |
| Operación · pedido/pagos | Recibir/aplicar/rechazar se mantienen; «Resolver excepción» agrega decisiones administrativas auditadas. Cancelar sigue en gestión del pedido. |

## Pruebas y pendientes de certificación

- `PagosArchivosTest`: total/seña antes de aprobación, carga independiente, umbral por debajo/igual/encima, manual, límites certificados y cálculo de retención desde vencimiento.
- Once casos HTTP dirigidos, incluidos controles manuales con/sin anticipo, variantes total/seña y corrección privada tras reinicio. `ActivacionConfiguracionIntegrationTest#pagosArchivosV42CargaPreviaRecuperacionYPurga`: flujo HTTP completo, PDF real validado, aprobación diferida por dinero aplicado, pago tardío, rechazo sin purga, permisos, resolución idempotente, cierre, revocación 410, eliminación idempotente, finanzas pendientes y recordatorio único.
- Frontend: `test:payment-archives`, `test:journey`, `test:tracking`; incluye unidades, límites, rechazo de política inválida y timeline sin revisión/cobro inventados.
- Migración V42: política versionada, conservación, auditoría append-only, permisos de transición, autorizaciones y revocación. Los datos anteriores no se borran ni se les asignan relojes.
- Certificar en integración del equipo con PostgreSQL/Docker nativo: concurrencia real entre informes/acreditación y worker; caída/reinicio durante filesystem y envío SMTP, más integración de agente/cola cuando exista. No declarar esas pruebas nativas completas por haber pasado un ensayo serial con PGlite.
