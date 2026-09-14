# Decisión funcional — Catálogo de servicios y precios

| Campo | Valor |
|---|---|
| Estado | Decisión confirmada |
| Fecha | 2026-09-14 |
| Alcance | Catálogo comercial, Fase 6 y activación del motor |
| Relación | E19 #197 · Fase 6 #222 · DER revisión #226 · PR #196 |

## 1. Objetivo

Definir la relación entre el **motor de configuración operativa** y la administración de **servicios y precios** de la imprenta.

La decisión separa ambos dominios para evitar que una modificación comercial cotidiana obligue a crear una nueva versión completa del motor, sin perder la validación necesaria para que el sistema pueda cotizar y operar.

## 2. Separación de responsabilidades

### Motor de configuración

Administra reglas y compromisos operativos versionados, entre ellos:

- modelo operativo y aprobación;
- pagos y reglas de seña;
- impresoras y capacidades;
- horarios y tiempos estimados;
- modalidades y puntos de entrega;
- activación, programación, seguridad e historial.

### Catálogo de servicios y precios

Administra de forma independiente la oferta comercial utilizada por el cotizador, por ejemplo:

- tarifas de impresión por formato, papel y modo de color;
- terminaciones y servicios adicionales;
- anillado;
- encuadernado;
- otros servicios habilitados;
- tipo o base de cobro;
- precio aplicable;
- habilitación comercial del servicio.

**Modificar servicios o precios no obliga por sí mismo a modificar ni crear una nueva versión del motor de configuración.**

## 3. Accesos UX

La misma vista administrativa de **Servicios y precios** debe ser accesible desde dos contextos:

1. **Dashboard / Administración → Servicios y precios**: acceso habitual, independiente del asistente del motor.
2. **Fase 6 → Ver detalle de servicios y precios**: acceso contextual durante la revisión previa a activar/programar una configuración.

Ambos accesos representan el mismo dominio comercial y deben evitar configuraciones duplicadas.

Desde Fase 6, el administrador puede consultar o modificar servicios y precios y luego utilizar **Guardar y volver a Fase 6**.

## 4. Regla obligatoria para avanzar de Fase 6 a Fase 7

La Fase 6 debe comprobar que existe una **referencia comercial válida de servicios y precios** antes de permitir avanzar a Fase 7.

No se exige que los precios hayan sido modificados durante la creación de la versión del motor.

Una nueva versión operativa puede reutilizar sin cambios el catálogo comercial válido existente.

La condición es:

> Para activar o programar una versión del motor debe existir un catálogo comercial utilizable por el cotizador, con los servicios ofrecidos correctamente configurados y sus precios requeridos definidos.

Si no existe una referencia comercial válida:

- Fase 6 muestra un **Bloqueo**;
- el botón para continuar a Fase 7 permanece deshabilitado;
- se ofrece acceso directo a **Servicios y precios** para completar la información;
- al guardar, el administrador vuelve a Fase 6 y se revalida la condición.

## 5. No obligatoriedad de modificación por versión

Servicios y precios son una dependencia necesaria para operar, pero **no constituyen una fase obligatoria de edición en cada nueva versión del motor**.

Ejemplo:

- v3 del motor utiliza el catálogo comercial vigente;
- el administrador crea v4 únicamente para modificar horarios;
- Fase 6 confirma que existe una referencia válida de servicios y precios;
- si el catálogo sigue siendo válido, no es necesario editarlo;
- v4 puede continuar a Fase 7 sin alterar precios.

## 6. Modificación independiente desde Dashboard

El propietario puede ingresar a **Servicios y precios** fuera del motor y cambiar la oferta comercial sin modificar parámetros operativos del motor.

Ejemplos:

- actualizar el precio B/N A4;
- actualizar el precio color A3;
- modificar el precio de anillado;
- habilitar o deshabilitar una terminación;
- agregar una terminación admitida por el producto.

Estos cambios no deben generar artificialmente una nueva versión del motor operativo.

Cuando una modificación comercial entra en vigencia, se utiliza para **nuevas cotizaciones generadas desde ese momento**. No modifica hacia atrás cotizaciones ya emitidas, pedidos aceptados ni trabajos en curso.

## 7. No retroactividad y trazabilidad comercial

La independencia del motor no debe eliminar la trazabilidad de precios.

Para que una cotización conserve el importe que se le ofreció al cliente, el catálogo comercial debe disponer de una **revisión o versión comercial propia**, independiente de `configuracion_version`.

Al cotizar deben poder identificarse por separado:

- la versión operativa del motor utilizada;
- la revisión comercial de servicios/precios utilizada.

### Regla de no retroactividad

Un cambio de servicios o precios **no es retroactivo**.

- una cotización ya generada conserva los servicios, precios y condiciones económicas capturados mientras permanezca vigente;
- un pedido ya aceptado conserva los precios y servicios con los que fue confirmado;
- un trabajo ya en producción o en curso no se recalcula por una actualización posterior del catálogo;
- solamente las nuevas cotizaciones utilizan la revisión comercial vigente después del cambio.

Por lo tanto, actualizar el catálogo desde Dashboard puede ser una operación habitual y directa sin romper la lógica histórica ni económica del sistema.

Una modificación posterior de precios nunca debe reescribir el precio congelado de una cotización ya generada ni de un pedido confirmado.

Esta separación permite modificar precios desde Dashboard sin crear una versión nueva del motor y, a la vez, mantener auditoría y reproducibilidad económica.

## 8. Configuración inicial

Al contratar/instalar el servicio se crea la configuración operativa inicial **v1**.

Antes de que esa configuración pueda activarse como sistema funcional deben cumplirse todos los mínimos de las fases del motor y, además, debe existir una referencia comercial válida de servicios y precios.

La instalación puede incluir valores iniciales o una plantilla comercial, pero la activación solo es válida cuando la información necesaria para cotizar está completa.

## 9. Representación en Fase 6

El resumen de Fase 6 incorpora una tarjeta adicional:

**Servicios y precios**

Debe mostrar, de forma resumida:

- estado `Correcto` o `Bloqueado`;
- cantidad de tarifas configuradas;
- cantidad de servicios/terminaciones habilitados;
- indicación de que existe una referencia comercial válida;
- acción **Ver detalle de servicios y precios**.

La tarjeta no convierte el catálogo comercial en una sexta fase de configuración. Es una **dependencia externa validada desde Fase 6**.

## 10. Impacto sobre el DER vigente

El DER v3.2 actualmente relaciona `configuracion_servicio` y `tarifa_impresion` directamente con `configuracion_version`.

Esa relación no representa completamente esta decisión posterior porque impediría modificar precios de forma independiente del motor.

La revisión de datos debe evaluar separar la revisión comercial de la versión operativa, manteniendo como mínimo:

- catálogo estable de servicios;
- revisión/versionado propio de servicios y tarifas comerciales;
- una referencia comercial vigente para cotizar;
- captura de la revisión comercial en la cotización;
- snapshots de precios en cotización/pedido;
- validación en Fase 6 de que existe una referencia comercial válida.

La implementación concreta de tablas y claves corresponde a la revisión DER #226.

## 11. Consecuencia para Fase 6

Fase 6 resume y valida:

1. Modelo operativo y aprobación.
2. Pagos y reglas de seña.
3. Impresoras y producción.
4. Horarios y tiempos.
5. Entrega y puntos.
6. Referencia externa de **Servicios y precios**.

Luego presenta simulación integral y conflictos.

Un catálogo comercial faltante o inválido es un **Bloqueo** y no permite avanzar a Fase 7.
