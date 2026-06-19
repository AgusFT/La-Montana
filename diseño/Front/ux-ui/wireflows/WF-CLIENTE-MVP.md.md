# Flujo de Negocio - Cliente (MVP)

## Objetivo

Documentar el flujo de negocio principal del cliente dentro de La Montaña y definir el orden de creación de los wireframes necesarios para cubrir el recorrido completo del usuario dentro del MVP.

---

## Resumen - Estructural
### Fase 1
- WF-CLI-001 — Login
- WF-CLI-002 — Dashboard 
- WF-CLI-003 — Crear pedido
- WF-CLI-003 — Cargar Archivos y Configurar Pedido (Detalles, opcionales)
- WF-CLI-004 — Confirmación de Archivo Cargado y Muestra conteo de número de Páginas

### Fase 2
- WF-CLI-005 — Pedido cotizado y resumen. 
- WF-CLI-005 — Selección de método de pago y seleccionar punto de entrega. 
- WF-CLI-005 — Confirmar pedido.

### Fase 3
- WF-CLI-006 — Pedido ya recibido a la espera de Revisión Administrativa
- WF-CLI-006 — Detalles del trabajo, Archivo cargado e información del Pedido, tiempo de entrega, precio y código de retiro 
- WF-CLI-006 — método de pago y punto de entrega
- WF-CLI-006 — Mientras se encuentre en Revisión se puede Cancelar pedido

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
* Acceso al detalle de cada pedido.

---

## WF-CLI-003 — Crear Pedido

### Historia asociada

* HU-CLI-002

### Objetivo

Permitir al cliente cargar el archivo del trabajo y completar los parámetros básicos necesarios para solicitar una cotización.

### Funcionalidades principales

#### Archivo del trabajo

* Seleccionar archivo.
* Validar formatos permitidos.
* Validar tamaño máximo de 10 MB.
* Detectar automáticamente la cantidad de páginas.

#### Detalles del trabajo

* Tamaño de hoja.
* Cantidad de copias.
* Tipo de impresión.
  * Blanco y negro.
  * Color.

#### Opcionales

* Anillado.
* Encuadernado.
* Doble faz.

### Regla de negocio relacionada

El cliente no define el precio final del pedido.

---

## WF-CLI-004 — Archivo Cargado y Previsualización de Cotización

### Historia asociada

* HU-CLI-003

### Objetivo

Mostrar al cliente la información obtenida del archivo cargado y una estimación preliminar antes de continuar.

### Información principal

#### Archivo cargado

* Nombre del archivo.
* Tamaño del archivo.
* Estado de carga correcta.
* Cantidad de páginas detectadas.

#### Información estimada

* Tiempo estimado de entrega.
* Precio estimado.

### Regla de negocio relacionada

La cantidad de páginas se obtiene automáticamente a partir del archivo cargado.

---

# Fase 2 - Confirmación del Pedido

## WF-CLI-005 — Resumen y Confirmación del Pedido

### Objetivo

Permitir al cliente revisar toda la información antes de generar el pedido.

### Información principal

#### Resumen del trabajo

* Cantidad de páginas.
* Cantidad de copias.
* Tamaño de hoja.
* Tipo de impresión.
* Opcionales seleccionados.

#### Archivo asociado

* Nombre del archivo.
* Tamaño del archivo.

#### Información económica

* Precio cotizado.
* Tiempo estimado de entrega.

#### Método de pago

* Efectivo.
* Débito.
* Transferencia.

#### Punto de entrega

* Selección de punto de entrega.
* Cambio de punto de entrega.

### Acciones disponibles

* Volver.
* Confirmar pedido.

### Resultado esperado

* Pedido creado.
* Estado inicial: Pendiente de revisión.

### Regla de negocio relacionada

Todo pedido nuevo queda inicialmente pendiente de revisión administrativa.

---

# Fase 3 - Pedido Pendiente de Revisión

## WF-CLI-006 — Pedido Recibido y En Revisión

### Objetivo

Permitir al cliente consultar el estado del pedido mientras es revisado por la imprenta.

### Información principal

#### Línea de tiempo del pedido

* Pedido recibido.
* En revisión.
* Producción.
* Control de calidad.
* Listo para entregar.
* En viaje.
* Entregado.

#### Detalles del trabajo

* Cantidad de páginas.
* Cantidad de copias.
* Tamaño de hoja.
* Tipo de impresión.
* Opcionales seleccionados.

#### Archivo asociado

* Nombre del archivo.
* Tamaño del archivo.
* Descarga del archivo.

#### Información del pedido

* Tiempo estimado de entrega.
* Precio.
* Código de retiro.

#### Método de pago

* Método seleccionado.

#### Punto de entrega

* Punto seleccionado.

### Acciones disponibles

* Cancelar pedido.

### Regla de negocio relacionada

El cliente puede cancelar el pedido mientras continúe en revisión administrativa.

---

# Fase 4 - Revisión Administrativa

## WF-CLI-007 — Pedido Requiere Corrección

### Historia asociada

* HU-CLI-005

### Objetivo

Permitir al cliente corregir información solicitada por la imprenta.

### Funcionalidades principales

* Consultar observaciones.
* Reemplazar archivos.
* Modificar información requerida.
* Reenviar para revisión.

---

## WF-CLI-008 — Pedido Cotizado

### Objetivo

Mostrar la cotización definitiva generada por la imprenta.

### Información principal

* Precio definitivo.
* Observaciones.
* Condiciones comerciales.

---

## WF-CLI-009 — Aceptar o Rechazar Cotización

### Objetivo

Permitir al cliente decidir si continúa o no con el pedido.

### Resultado esperado

* Aceptar cotización.
* Rechazar cotización.

---

# Fase 5 - Flujo Financiero

## WF-CLI-010 — Pedido Requiere Seña

### Historia asociada

* HU-CLI-006

### Información principal

* Monto de seña requerido.
* Porcentaje aplicado.
* Estado financiero.

### Regla de negocio relacionada

Los pedidos superiores a 200 carillas requieren una seña del 30%.

---

## WF-CLI-011 — Seña Registrada

### Objetivo

Informar al cliente que la seña fue registrada correctamente.

### Resultado esperado

* Pedido habilitado para producción.

---

# Fase 6 - Seguimiento y Entrega

## WF-CLI-012 — Pedido en Producción

### Objetivo

Informar que el trabajo se encuentra en proceso de impresión.

---

## WF-CLI-013 — Control de Calidad

### Objetivo

Informar que el trabajo se encuentra en control de calidad.

---

## WF-CLI-014 — Pedido Listo para Entregar

### Objetivo

Informar que el pedido se encuentra disponible para entrega.

---

## WF-CLI-015 — Pedido En Viaje

### Objetivo

Permitir al cliente conocer que el pedido se encuentra en distribución.

### Información principal

* Estado de entrega.
* Código de retiro.

---

## WF-CLI-016 — Pedido Entregado / Cerrado

### Objetivo

Mostrar la finalización completa del pedido.

### Información principal

* Entrega realizada.
* Cobro registrado.
* Pedido cerrado.