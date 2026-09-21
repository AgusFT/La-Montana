# WF - Fase 7: Activación, seguridad y confirmación

**Épica:** E19 - #197  
**Issue:** #224  
**Estado:** cuatro mockups PNG versionados; validación final del equipo pendiente.  
**Fecha:** 15/09/2026  
**Base visual:** Fase 6 - #222.

## 1. Propósito

La Fase 7 no incorpora nuevas reglas de negocio. Recibe el borrador integralmente validado por la Fase 6 y permite decidir cuándo aplicarlo, verificar la identidad del ADMIN_ADMIN y comunicar el resultado de la operación.

El flujo conserva exactamente el shell visual utilizado en la Fase 6: encabezado, franja de trazabilidad, sidebar de ocho fases, tarjetas, estados y navegación inferior.

## 2. Entrada obligatoria desde Fase 6

Para ingresar deben cumplirse estas condiciones:

- borrador completo y guardado;
- cero Bloqueos;
- advertencias revisadas;
- compatibilidad integral de Fases 1–5;
- al menos una impresora Operativa;
- al menos una modalidad de entrega utilizable;
- referencia comercial válida para cotizar.

Servicios y precios continúan siendo un dominio independiente. La Fase 7 no permite modificarlos.

## 3. MC-ADM-CFG-012 - Activar o programar

![MC-ADM-CFG-012 — Activar o programar](https://raw.githubusercontent.com/AgusFT/La-Montana/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-012-activar-o-programar.png)

La pantalla presenta:

- V2 como borrador candidato;
- V1 como versión vigente;
- resultado de la validación de Fase 6;
- cantidad de Bloqueos y Advertencias revisadas;
- elección entre Activar ahora y Programar activación;
- aclaración sobre la independencia de Servicios y precios.

La evidencia muestra **Activar ahora** seleccionado. **Programar activación** es un estado alternativo de la misma pantalla: al seleccionarlo se habilitan fecha, hora y zona horaria, y la acción primaria cambia a Continuar con programación.

Mientras la operación final todavía no fue ejecutada, se mantienen:

- Volver a Fase 6;
- Guardar y salir;
- Continuar hacia Verificación de seguridad.

## 4. MC-ADM-CFG-013 - Verificación de seguridad

![MC-ADM-CFG-013 — Verificación de seguridad](https://raw.githubusercontent.com/AgusFT/La-Montana/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-013-verificacion-seguridad.png)

La pantalla es de solo lectura respecto de la configuración y muestra:

- acción elegida;
- versión vigente;
- versión candidata;
- vigencia inmediata o programada;
- reingreso de contraseña;
- envío e ingreso de código de un solo uso;
- advertencia de auditoría;
- invalidación del desafío si se vuelve a editar.

La acción final debe expresar la decisión concreta: **Activar configuración** o **Programar activación**.

## 5. MC-ADM-CFG-014A - Activación inmediata confirmada

![MC-ADM-CFG-014A — Activación inmediata confirmada](https://raw.githubusercontent.com/AgusFT/La-Montana/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-014A-activacion-inmediata-confirmada.png)

Resultado:

- V2 pasa a Activa;
- V1 pasa a Histórica;
- el borrador deja de existir como pendiente;
- la operación queda auditada;
- nuevas cotizaciones e intentos de pedido utilizan la configuración activa;
- pedidos ya creados conservan sus condiciones.

La pantalla no ofrece Guardar y salir ni volver a editar. Permite Ver detalle, Volver al inicio y Consultar historial.

## 6. MC-ADM-CFG-014B - Activación programada confirmada

![MC-ADM-CFG-014B — Activación programada confirmada](https://raw.githubusercontent.com/AgusFT/La-Montana/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-014B-activacion-programada-confirmada.png)

Resultado:

- V2 pasa a Programada e inmutable;
- V1 continúa Activa hasta la fecha indicada;
- no puede iniciarse otro borrador;
- la programación queda auditada;
- para modificarla debe cancelarse primero mediante el flujo autorizado.

La pantalla permite Ver detalle, Volver al inicio, Consultar historial y Ver programación. La cancelación no se presenta como acción primaria en la pantalla de éxito.

## 7. Validación final del backend

Antes de activar o programar se revalidan:

- permisos y sesión;
- contraseña y código;
- integridad del borrador;
- ausencia de cambios concurrentes;
- exclusión entre borrador y programación;
- invariantes de impresoras y entrega;
- existencia de una referencia comercial utilizable.

Si algo falla, no existe transición parcial: V1 continúa activa y V2 permanece recuperable como borrador.

En una ejecución programada, estas condiciones se verifican nuevamente en el instante efectivo. Si no se cumplen, la versión anterior continúa activa y el incidente queda auditado.

## 8. No retroactividad y confirmación del pedido

La activación afecta nuevas cotizaciones e intentos de creación de pedido. No modifica pedidos existentes.

Una cotización conserva sus datos mientras continúe vigente, pero el pedido solamente se crea cuando el backend revalida las reglas activas al presionar Confirmar/Crear pedido. Si existe una incompatibilidad, no se crea el pedido y se solicita corrección o recotización.

## 9. Estados complementarios pendientes de detalle

Los cuatro PNG cubren el recorrido principal. Durante implementación deben contemplarse como estados de las mismas vistas:

- procesamiento sin doble envío;
- código incorrecto o vencido;
- conflicto concurrente;
- fallo de activación;
- fallo al ejecutar una programación;
- retorno seguro a Fase 6.

## 10. Evidencia versionada

- [Carpeta de mockups V3](https://github.com/AgusFT/La-Montana/tree/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3)
- [Commit visual `b43604a`](https://github.com/AgusFT/La-Montana/commit/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed)
- [Contrato funcional de Fase 7](../../../../marco-del-proyecto/contrato-funcional-fase-7-activacion-confirmacion.md)
- [Fase 6 - #222](https://github.com/AgusFT/La-Montana/issues/222)
- [Fase 7 - #224](https://github.com/AgusFT/La-Montana/issues/224)
- [E19 - #197](https://github.com/AgusFT/La-Montana/issues/197)
- [PR #196](https://github.com/AgusFT/La-Montana/pull/196)
