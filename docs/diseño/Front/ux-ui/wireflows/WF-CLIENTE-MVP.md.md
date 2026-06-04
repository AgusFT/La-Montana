# Flujo de Negocio - Cliente (MVP)

## Objetivo

Documentar el flujo de negocio principal del cliente dentro de La Montaña y definir el orden de creación de los wireframes necesarios para cubrir el recorrido completo del usuario dentro del MVP.

---

## Resumen - Estructural
### Fase 1
- WF-CLI-001 — Login
- WF-CLI-002 — Dashboard 
- WF-CLI-003 — Crear pedido
- WF-CLI-007 — Detalle de pedido

### Fase 2
- WF-CLI-008 — Corrección requerida
- WF-CLI-009 — Pedido cotizado
- WF-CLI-011 — Pedido requiere seña

### Fase 4
- WF-CLI-013 — Producción
- WF-CLI-014 — Listo para retirar
- WF-CLI-015 — Cerrado

---

# Fase 1 - Flujo Principal MVP

## WF-CLI-001 — Login

### Objetivo

Permitir al cliente autenticarse e ingresar al sistema.

---

## WF-CLI-002 — Dashboard de Cliente

### Objetivo

Proporcionar una vista general de la actividad del cliente.

### Información principal

* Pedidos activos.
* Pedidos recientes.
* Acceso a creación de nuevos pedidos.

---

## WF-CLI-003 — Crear Pedido

### Historia asociada

* HU-CLI-002

### Funcionalidades principales

* Ingreso de datos básicos del pedido.
* Selección de tipo de trabajo.
* Observaciones adicionales.

---

## WF-CLI-004 — Cargar Archivos

### Historia asociada

* HU-CLI-003

### Funcionalidades principales

* Adjuntar archivos al pedido.
* Asociar correctamente los archivos al pedido creado.
* Confirmar la carga de los archivos.

---

## WF-CLI-005 — Confirmación de Creación

### Objetivo

Informar al cliente que el pedido fue creado correctamente.

### Estado inicial

* Pendiente de revisión.

### Regla de negocio relacionada

Todo pedido nuevo queda inicialmente pendiente de revisión.

---

## WF-CLI-006 — Listado de Pedidos

### Historias asociadas

* HU-CLI-004
* HU-CLI-007

### Funcionalidades principales

* Consultar pedidos existentes.
* Visualizar estados visibles.
* Acceder al detalle de cada pedido.

---

## WF-CLI-007 — Detalle de Pedido

### Objetivo

Permitir al cliente consultar toda la información visible del pedido.

### Información principal

* Datos generales del pedido.
* Archivos asociados.
* Estado visible.
* Observaciones.
* Historial visible del pedido.

---

# Fase 2 - Revisión Administrativa

## WF-CLI-008 — Pedido Requiere Corrección

### Historia asociada

* HU-CLI-005

### Funcionalidades principales

* Consultar observaciones realizadas por la imprenta.
* Identificar información faltante o incorrecta.
* Volver a cargar archivos o corregir información.

---

## WF-CLI-009 — Pedido Cotizado

### Objetivo

Presentar al cliente la cotización generada por la imprenta.

### Información principal

* Precio.
* Condiciones.
* Información relevante para la aceptación.

---

## WF-CLI-010 — Aceptar o Rechazar Cotización

### Objetivo

Permitir al cliente decidir si continúa o no con el pedido.

### Resultado esperado

* Aceptar cotización.
* Rechazar cotización.

---

# Fase 3 - Flujo Financiero

## WF-CLI-011 — Pedido Requiere Seña

### Historia asociada

* HU-CLI-006

### Información principal

* Monto requerido.
* Motivo de la seña.
* Estado financiero visible.

### Regla de negocio relacionada

Los pedidos que superen 200 carillas requieren una seña del 30%.

---

## WF-CLI-012 — Seña Registrada

### Objetivo

Informar al cliente que la seña fue registrada correctamente y que el pedido puede continuar su flujo.

---

# Fase 4 - Seguimiento del Pedido

## WF-CLI-013 — Pedido en Producción

### Objetivo

Permitir al cliente conocer que el pedido se encuentra siendo producido.

---

## WF-CLI-014 — Pedido Listo para Retirar

### Objetivo

Informar al cliente que el trabajo se encuentra disponible para entrega o retiro.

---

## WF-CLI-015 — Pedido Entregado / Cerrado

### Objetivo

Mostrar la finalización del flujo del pedido.

---

# Resumen del Flujo Principal del Negocio - Cliente (MVP)

```text
Login
    ↓
Dashboard
    ↓
Crear Pedido
    ↓
Cargar Archivos
    ↓
Pedido Pendiente de Revisión
    ↓
┌─────────────────────────┐
│ Requiere Corrección     │
└──────────┬──────────────┘
           ↓
Corregir Pedido
           ↓
Pendiente de Revisión
           ↓
Pedido Cotizado
           ↓
Aceptar Cotización
           ↓
┌─────────────────────────┐
│ Requiere Seña           │
└──────────┬──────────────┘
           ↓
Registrar Seña
           ↓
Pedido en Producción
           ↓
Listo para Retirar
           ↓
Entregado / Cerrado
```
