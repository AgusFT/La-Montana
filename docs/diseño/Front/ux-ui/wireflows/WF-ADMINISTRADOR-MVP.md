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

- WF-ADM-001 — Login
- WF-ADM-002 — Dashboard Administrativo
- WF-ADM-003 — Listado de Pedidos Pendientes
- WF-ADM-004 — Revisar Pedido

## Fase 2 - Decisión Administrativa

- WF-ADM-005 — Solicitar Corrección
- WF-ADM-006 — Pedido Rechazado
- WF-ADM-007 — Aprobar Pedido

## Fase 3 - Producción

- WF-ADM-008 — Enviar a Producción
- WF-ADM-009 — Pedido en Producción
- WF-ADM-010 — Control de Calidad

## Fase 4 - Entrega

- WF-ADM-011 — Listo para Entregar
- WF-ADM-012 — Registrar Entrega
- WF-ADM-013 — Pedido Entregado

---

# Fase 1 - Gestión Inicial

## WF-ADM-001 — Login

### Objetivo

Permitir al administrador autenticarse en el sistema.

---

## WF-ADM-002 — Dashboard Administrativo

### Objetivo

Proporcionar una vista consolidada de la operación diaria.

### Información principal

- Pedidos pendientes de revisión.
- Pedidos aprobados.
- Pedidos esperando seña.
- Pedidos en producción.
- Entregas pendientes.
- Alertas operativas.

### Acciones disponibles

- Ver detalle de pedido.
- Revisar pedido.
- Gestionar producción.
- Gestionar entregas.

---

## WF-ADM-003 — Listado de Pedidos Pendientes

### Objetivo

Mostrar todos los pedidos que requieren intervención administrativa.

### Información principal

- Número de pedido.
- Cliente.
- Fecha de creación.
- Cantidad de páginas.
- Estado actual.

---

## WF-ADM-004 — Revisar Pedido

### Objetivo

Analizar el pedido antes de habilitar cualquier avance.

### Información visible

#### Cliente

- Nombre.
- Contacto.
- Email.

#### Archivo

- Nombre del archivo.
- Tamaño.
- Descarga.

#### Trabajo

- Tamaño de hoja.
- Tipo de impresión.
- Cantidad de páginas.
- Cantidad de copias.
- Opcionales.

#### Pedido

- Fecha.
- Requiere seña.
- Estado.

### Acciones disponibles

- Aprobar pedido.
- Solicitar corrección.
- Rechazar pedido.

---

# Fase 2 - Decisión Administrativa

## WF-ADM-005 — Solicitar Corrección

### Objetivo

Enviar observaciones al cliente para corregir información o archivos.

### Información principal

- Datos del pedido.
- Observaciones administrativas.

### Acciones disponibles

- Enviar corrección.
- Cancelar.

### Resultado esperado

Pedido pasa a:

- Corrección solicitada.

---

## WF-ADM-006 — Pedido Rechazado

### Objetivo

Registrar que el pedido fue rechazado.

### Información principal

- Motivo de rechazo.
- Fecha.
- Usuario responsable.

### Resultado esperado

Pedido finaliza sin avanzar a producción.

---

## WF-ADM-007 — Aprobar Pedido

### Objetivo

Validar formalmente el pedido.

### Validaciones mínimas

- Archivo presente.
- Datos completos.
- Configuración válida.
- Reglas de negocio cumplidas.

### Resultado esperado

Pedido pasa a:

- Aprobado.

---

# Fase 3 - Producción

## WF-ADM-008 — Enviar a Producción

### Objetivo

Generar la orden de impresión.

### Información principal

#### Cliente

- Datos básicos.

#### Archivo

- Archivo aprobado.
- Tamaño.

#### Trabajo

- Configuración seleccionada.

#### Producción

- Impresora seleccionada.

### Acciones disponibles

- Seleccionar impresora.
- Enviar a producción.
- Cancelar.

### Resultado esperado

Se genera un Print Job autorizado.

---

## WF-ADM-009 — Pedido en Producción

### Objetivo

Monitorear el avance del trabajo.

### Información principal

- Estado técnico.
- Impresora utilizada.
- Progreso.

---

## WF-ADM-010 — Control de Calidad

### Objetivo

Validar el resultado de impresión antes de habilitar la entrega.

### Información principal

- Archivo impreso.
- Impresora utilizada.
- Configuración aplicada.

### Acciones disponibles

- Reimprimir.
- Reportar incidencia.
- Marcar listo para entregar.

### Resultado esperado

Pedido pasa a:

- Listo para entregar.

---

# Fase 4 - Entrega

## WF-ADM-011 — Listo para Entregar

### Objetivo

Administrar pedidos preparados para entrega.

### Información principal

- Cliente.
- Punto de entrega.
- Código de retiro.

---

## WF-ADM-012 — Registrar Entrega

### Objetivo

Confirmar la entrega física del pedido.

### Información principal

- Código de retiro.
- Palabra clave.
- Fecha y hora.

### Validaciones

- Código correcto.
- Pedido listo para entregar.

---

## WF-ADM-013 — Pedido Entregado

### Objetivo

Cerrar operativamente el pedido.

### Información principal

- Entrega registrada.
- Fecha.
- Responsable.

### Resultado esperado

Estado visible:

- Entregado.

Estado interno:

- Cerrado.

---

# Flujo Principal del Negocio - Administrador

```text
Login
    ↓
Dashboard Administrativo
    ↓
Pedidos Pendientes
    ↓
Revisar Pedido
    ↓
 ┌─────────────────────┐
 │ Solicitar Corrección│
 └──────────┬──────────┘
            ↓
 Esperar Corrección
            ↓
 Revisar Pedido
            ↓

 ┌─────────────────────┐
 │ Rechazar Pedido     │
 └─────────────────────┘

            ó

 Aprobar Pedido
            ↓
 Enviar a Producción
            ↓
 Pedido en Producción
            ↓
 Control de Calidad
            ↓
 Listo para Entregar
            ↓
 Registrar Entrega
            ↓
 Pedido Entregado
```
