# Configurabilidad del Sistema

## Objetivo

El sistema permitirá que determinados comportamientos de negocio sean configurables por administradores autorizados, evitando modificaciones de código para adaptar el flujo operativo de cada imprenta.

Estas configuraciones representan políticas de operación y no cambios en la arquitectura del sistema.

---

# Principios Generales

Las configuraciones del sistema deberán cumplir los siguientes principios:

- Permitir adaptar el comportamiento operativo del sistema sin modificar el código fuente.
- Mantener la integridad de los datos y la consistencia del modelo de negocio.
- No comprometer la seguridad, la auditoría ni la trazabilidad de las operaciones.
- Ser administrables únicamente por usuarios con los permisos correspondientes.
- Ser compartidas por todos los clientes de las aplicaciones (Web, Android y subsistema de impresión), ya que la fuente única de verdad continúa siendo Supabase.

---

# Alcance

Las configuraciones podrán modificar reglas operativas del negocio, siempre que dichas modificaciones no alteren los principios fundamentales definidos para el sistema.

Las configuraciones **no podrán**:

- modificar la arquitectura del sistema;
- reemplazar reglas críticas de seguridad;
- eliminar registros de auditoría;
- comprometer la integridad de los datos;
- permitir operaciones que contradigan restricciones definidas como obligatorias por el negocio.

---

# Ejemplos de Configuraciones

## Flujo de aprobación de pedidos

Permitir definir cómo se gestionan los nuevos pedidos.

Ejemplos:

- Siempre requieren aprobación administrativa.
- Aprobar automáticamente pedidos previamente abonados.
- Aprobar automáticamente clientes autorizados.
- Aprobar automáticamente pedidos menores a un determinado importe.

---

## Reglas de pago para producción

Permitir definir cuándo un pedido puede ingresar al proceso productivo.

Ejemplos:

- Requerir una seña.
- Requerir el pago completo.
- Permitir producción sin pago previo.

---

## Estrategia de asignación de impresoras

Permitir definir cómo se asignan los trabajos a las impresoras disponibles.

Ejemplos:

- Asignación manual.
- Primera impresora disponible.
- Según tipo de trabajo.
- Según prioridad.
- Balanceo de carga.

---

## Notificaciones

Permitir configurar los mecanismos de comunicación con los usuarios.

Ejemplos:

- Correo electrónico.
- WhatsApp.
- Notificaciones Push.
- Combinación de varios canales.
- Sin notificaciones automáticas.

---

## Parámetros operativos

Permitir definir valores utilizados por distintas reglas del negocio.

Ejemplos:

- Cantidad mínima de hojas para solicitar una seña.
- Porcentaje de anticipo requerido.
- Tiempo máximo de reserva de un pedido.
- Horarios habilitados para impresión automática.
- Prioridades de procesamiento.

---

# Principio General

Toda configuración representa una **decisión de negocio** y no una modificación del diseño arquitectónico del sistema.

La existencia de una configuración implica únicamente que el sistema consulta un valor antes de ejecutar determinada regla de negocio.

---

# Evolución del Sistema

El conjunto de configuraciones disponibles podrá ampliarse durante la evolución del proyecto, siempre respetando los principios definidos en este documento.

La incorporación de nuevas configuraciones deberá:

- mantener la coherencia con las reglas de negocio existentes;
- preservar la trazabilidad de las operaciones;
- garantizar la compatibilidad con los distintos clientes del sistema;
- evitar duplicación de lógica de negocio.

---

# Relación con el resto de la documentación

Este documento establece el marco general de configurabilidad del sistema.

Las configuraciones concretas que afecten funcionalidades específicas deberán documentarse además en:

- **Especificaciones de Requerimientos**, indicando qué requisitos admiten configuración.
- **Casos de Uso**, describiendo cómo las configuraciones modifican el flujo correspondiente.
- **Historias de Usuario**, cuando la implementación de una nueva configuración forme parte del alcance de una iteración del proyecto.

---

# Estado del Documento

**Estado:** Activo.

Este documento define el enfoque general de configurabilidad del sistema y servirá como referencia para futuras ampliaciones del proyecto.