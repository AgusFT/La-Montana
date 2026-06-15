# Flujo de Negocio - Administrador (MVP)

## Objetivo

Documentar el flujo principal de operación administrativa dentro de La Montaña.

El administrador es responsable de:

- Revisar pedidos nuevos.
- Validar archivos y datos.
- Aprobar pedidos para producción.
- Solicitar correcciones al cliente.
- Rechazar pedidos cuando corresponda.
- Enviar pedidos aprobados a producción.
- Supervisar el avance operativo.
- Registrar el avance hasta la entrega.

---

# Resumen Estructural

## Fase 1 - Gestión Inicial


- WF-ADM-001 — Dashboard Administrativo
- WF-ADM-001 — Listado de Pedidos Pendientes
- WF-ADM-002 — Revisar Pedido

## Fase 2 - Decisión Administrativa

- WF-ADM-002 — Pedido Rechazado / Aprobar Pedido
- WF-ADM-003 — Solicitar Corrección

## Fase 3 - Pedidos Aprobados

- WF-ADM-004 — Pedidos Aprobados
- WF-ADM-005 — Detalle Pedido Aprobado


## Fase 4 - Producción

- WF-ADM-006 — Producción
- WF-ADM-007 — Control de Calidad

## Fase 4 - Listo Para Entregar

- WF-ADM-008 — Lista Entregas Pendientes
- WF-ADM-009 — Ver Detalle Pedido A Entregar

## Fase 5 - En Viaje 

- WF-ADM-010 — Entregas del día
- WF-ADM-011 — Ingresar Código de Pedido
- WF-ADM-012 — Código Validado

## Fase 6 - Entregado

- WF-ADM-012 — Finalizar Entrega
- WF-ADM-013 — Registrar Cobro

---

# Fase 1 - Gestión Inicial

## WF-ADM-001 — Dashboard Administrativo

### Objetivo

Proporcionar una vista consolidada de la operación diaria.

### Información principal

#### Pendientes de revisión

Pedidos que requieren validación administrativa.

#### En revisión

Pedidos actualmente analizados por administración.

#### Esperando seña

Pedidos que requieren pago parcial para continuar.

#### Entregas pendientes

Pedidos listos para ser programados para entrega.

#### Facturación

Resumen financiero del período.

#### Alertas

Incidencias operativas que requieren atención.

### Acciones disponibles

- Revisar pedido.
- Gestionar correcciones.
- Aprobar pedidos.
- Gestionar producción.
- Gestionar entregas.

---

## WF-ADM-002 — Listado de Pedidos Pendientes

### Objetivo

Mostrar los pedidos que requieren intervención administrativa.

### Información principal

- Número de pedido.
- Cliente.
- Fecha de recepción.
- Cantidad de páginas.
- Cantidad de copias.
- Estado actual.

### Acciones disponibles

- Revisar pedido.

---

## WF-ADM-002 — Revisar Pedido

### Objetivo

Analizar el pedido antes de tomar una decisión administrativa.

### Información visible

#### Cliente

- Nombre.
- Teléfono.
- Email.

#### Archivo

- Nombre.
- Tamaño.
- Vista previa.
- Descarga.

#### Trabajo

- Tamaño de hoja.
- Tipo de impresión.
- Cantidad de páginas.
- Cantidad de copias.
- Opcionales.

#### Pedido

- Fecha.
- Estado.
- Requiere seña.
- Monto estimado.

### Acciones disponibles

- Aprobar pedido.
- Solicitar corrección.
- Rechazar pedido.

---

# Fase 2 - Decisión Administrativa

## WF-ADM-002 — Pedido Rechazado / Aprobar Pedido

### Objetivo

Registrar la decisión administrativa sobre el pedido.

### Escenario A — Rechazar

#### Información principal

- Motivo de rechazo.
- Fecha.
- Usuario responsable.

### Resultado esperado

Estado interno:

- Rechazado.

Estado visible:

- Pedido rechazado.

### Escenario B — Aprobar

#### Validaciones mínimas

- Archivo válido.
- Datos completos.
- Configuración correcta.
- Reglas de negocio cumplidas.

### Resultado esperado

Estado interno:

- Aprobado.

---

## WF-ADM-003 — Solicitar Corrección

### Objetivo

Solicitar modificaciones o correcciones al cliente.

### Información principal

- Pedido.
- Observaciones.
- Archivos observados.

### Acciones disponibles

- Enviar corrección.
- Cancelar.

### Resultado esperado

Estado interno:

- Corrección solicitada.

Estado visible:

- Requiere corrección.

---

# Fase 3 - Pedidos Aprobados

## WF-ADM-004 — Pedidos Aprobados

### Objetivo

Mostrar los pedidos aprobados que todavía no fueron enviados a producción.

### Información principal

- Pedido.
- Cliente.
- Lugar de entrega.
- Fecha de solicitud.
- Fecha de entrega.
- Franja horaria.

### Acciones disponibles

- Ver detalle.
- Enviar a producción.

### Resultado esperado

El administrador selecciona qué pedido enviar a producción.

---

## WF-ADM-005 — Detalle Pedido Aprobado

### Objetivo

Visualizar toda la información del pedido aprobado antes de enviarlo a producción.

### Información visible

#### Cliente

- Nombre.
- Contacto.
- Email.

#### Trabajo

- Tamaño de hoja.
- Tipo de impresión.
- Cantidad de hojas.
- Cantidad de copias.
- Opcionales.

#### Entrega

- Dirección.
- Franja horaria.
- Fecha programada.

#### Archivo

- Nombre.
- Tamaño.
- Visualización.

### Acciones disponibles

- Ver documento.
- Enviar a producción.

---

# Fase 4 - Producción

## WF-ADM-006 — Producción

### Objetivo

Configurar el trabajo para impresión.

### Información visible

#### Cliente

- Datos básicos.

#### Trabajo

- Configuración aprobada.

#### Archivo

- Archivo seleccionado para impresión.

#### Impresora

- Impresora seleccionada.

### Acciones disponibles

- Seleccionar archivo.
- Seleccionar impresora.
- Imprimir.
- Cancelar.

### Resultado esperado

Se genera un Print Job autorizado.

---

## WF-ADM-007 — Control de Calidad

### Objetivo

Validar que la impresión sea correcta antes de habilitar la entrega.

### Información principal

- Pedido.
- Archivo impreso.
- Impresora utilizada.
- Configuración aplicada.

### Acciones disponibles

- Reimprimir.
- Reportar incidencia.
- Marcar listo para entregar.

### Resultado esperado

Estado interno:

- Listo para entregar.

---

# Fase 5 - Listo Para Entregar

## WF-ADM-008 — Lista Entregas Pendientes

### Objetivo

Administrar pedidos preparados para entrega.

### Información principal

- Pedido.
- Cliente.
- Lugar de entrega.
- Fecha programada.
- Franja horaria.
- Código de retiro.

### Acciones disponibles

- Ver detalle.

---

## WF-ADM-009 — Ver Detalle Pedido A Entregar

### Objetivo

Consultar toda la información del pedido antes de iniciar la entrega.

### Información visible

#### Cliente

- Nombre.
- Contacto.
- Email.

#### Trabajo

- Configuración solicitada.

#### Entrega

- Dirección.
- Fecha.
- Franja horaria.
- Código de retiro.

#### Archivo

- Nombre.
- Tamaño.
- Vista previa.

### Acciones disponibles

- Ver documento.
- Reprogramar entrega.

---

# Fase 6 - En Viaje

## WF-ADM-010 — Entregas del Día

### Objetivo

Gestionar los pedidos asignados para entregar.

### Información principal

- Pedido.
- Cliente.
- Lugar de entrega.
- Franja horaria.
- Estado de entrega.

### Acciones disponibles

- Enviar mensaje.
- Ver detalle.
- Ir.
- Ingresar código.

### Resultado esperado

El repartidor llega al destino e inicia la validación del cliente.

---

## WF-ADM-011 — Ingresar Código de Pedido

### Objetivo

Validar la identidad del cliente antes de entregar el pedido.

### Información visible

- Número de pedido.
- Cliente.
- Código ingresado.

### Validaciones

- Código correcto.
- Pedido asignado al cliente.

### Acciones disponibles

- Validar código.
- Cancelar.

### Resultado esperado

Estado de entrega:

- Código validado.

---

## WF-ADM-012 — Código Validado

### Objetivo

Confirmar que la identidad del cliente fue verificada.

### Información visible

- Pedido.
- Cliente.
- Estado validado.

### Acciones disponibles

- Finalizar entrega.

### Resultado esperado

Se habilita el cierre operativo y financiero.

---

# Fase 7 - Entregado

## WF-ADM-012 — Finalizar Entrega

### Objetivo

Registrar la entrega física del pedido.

### Información visible

#### Pedido

- Número de pedido.

#### Cliente

- Datos de contacto.

#### Entrega

- Fecha.
- Hora.
- Responsable.

#### Estado financiero

- Total.
- Seña.
- Saldo pendiente.

### Acciones disponibles

- Registrar cobro.
- Confirmar entrega.

---

## WF-ADM-013 — Registrar Cobro

### Objetivo

Registrar el pago final del pedido y cerrar el flujo operativo.

### Información principal

- Total del pedido.
- Seña registrada.
- Saldo pendiente.
- Método de pago.

### Acciones disponibles

- Confirmar cobro.
- Confirmar entrega sin cobro (si ya fue abonado).

### Resultado esperado

Estado visible:

- Entregado.

Estado financiero:

- Pagado.

Estado interno:

- Cerrado.