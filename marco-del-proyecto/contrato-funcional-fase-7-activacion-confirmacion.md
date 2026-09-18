# Contrato funcional de Fase 7 - Activación, programación y confirmación

**Estado:** decisiones funcionales confirmadas; cuatro mockups PNG versionados; validación final del equipo pendiente.  
**Fecha de consolidación:** 14/09/2026  
**Alcance:** E19 / Fase 7 / WF-CFG-12, WF-CFG-13 y WF-CFG-14.

## 1. Propósito

Esta fase define cómo una configuración preparada deja de ser un borrador y entra en vigencia, cómo se programa para una fecha futura y qué controles de seguridad, consistencia y auditoría deben cumplirse.

También fija el punto autoritativo del negocio frente a cambios de configuración: la cotización es una propuesta temporal; el pedido solamente existe después de que el backend valida el intento de confirmación y lo registra de forma atómica.

## 2. Invariantes

En todo momento el sistema debe respetar:

- exactamente una configuración operativa `Activa`;
- como máximo un único cambio pendiente;
- el cambio pendiente puede ser un `Borrador` o una versión `Programada`, nunca ambos;
- una versión `Programada` es inmutable;
- una versión `Histórica` es inmutable;
- activar o programar nunca reescribe pedidos ya creados;
- la creación del pedido es el punto de corte que congela sus condiciones.

## 3. Activación inmediata

Al confirmar **Activar ahora**, el backend debe ejecutar una única operación transaccional:

1. validar permisos, desafío de seguridad, integridad y compatibilidad;
2. comprobar que el borrador continúa siendo el mismo que fue revisado;
3. convertir el borrador validado en la nueva versión `Activa`;
4. convertir la versión anteriormente activa en `Histórica`;
5. retirar el estado y las acciones de borrador de la interfaz;
6. registrar usuario, fecha, hora, versión anterior, versión nueva y resultado;
7. aplicar la nueva configuración a nuevas cotizaciones e intentos de creación de pedido.

Si una parte falla, no se realiza ninguna transición parcial: la versión anterior continúa activa y el borrador permanece recuperable.

La activación no es retroactiva. Los pedidos ya creados conservan las condiciones que quedaron congeladas al crearse.

## 4. Activación programada

Al confirmar **Programar activación**:

- el borrador pasa a `Programada`;
- queda registrada la fecha, hora y zona horaria;
- la configuración actualmente activa continúa vigente hasta ese instante;
- la versión programada no puede editarse;
- no puede iniciarse ni mantenerse otro borrador mientras exista la programación;
- la programación puede consultarse o cancelarse mediante el flujo autorizado.

Para modificar una configuración programada se debe:

1. cancelar la programación;
2. mantener sin cambios la versión actualmente activa;
3. iniciar un nuevo borrador utilizando la versión cancelada como base, cuando corresponda;
4. volver a recorrer validación, seguridad y confirmación.

Al llegar la fecha programada, el cambio se ejecuta de forma atómica: la activa pasa a histórica y la programada pasa a activa. Si la validación técnica final falla, la versión anterior permanece activa y el incidente queda auditado.

## 5. Seguridad de la decisión final

La edición ordinaria requiere sesión autenticada con rol `ADMIN_ADMIN`. Activar ahora, programar, cancelar una programación y las demás decisiones finales de alto impacto requieren:

- reingreso de contraseña;
- código de un solo uso enviado por correo;
- vencimiento del código;
- nueva autenticación cuando exista inactividad relevante;
- auditoría completa del intento y su resultado.

El desafío autoriza una operación final concreta. Si el administrador vuelve a editar después de solicitar o validar el código, el desafío se invalida y debe iniciarse nuevamente.

## 6. Cotización y punto autoritativo de creación del pedido

### 6.1 Cotización

La cotización:

- captura la versión operativa y la referencia comercial utilizadas para calcularla;
- conserva el precio y las condiciones capturadas mientras siga vigente;
- no reserva por sí sola capacidad, medios de pago, disponibilidad de puntos ni aceptación del pedido;
- todavía no produce un pedido.

Un cambio posterior de tarifas, por sí solo, no reescribe el importe de una cotización todavía válida.

### 6.2 Confirmar / Crear pedido

El **breakpoint funcional** ocurre cuando el cliente presiona **Confirmar** o **Crear pedido**. En ese momento el backend debe volver a validar, como mínimo:

- vigencia e integridad de la cotización;
- estado global de recepción del sistema;
- sesión y autorización del cliente;
- disponibilidad y habilitación actual del servicio;
- disponibilidad del punto o modalidad de entrega elegida;
- compatibilidad operativa y capacidad requerida;
- medios de pago todavía habilitados;
- cumplimiento de señas o pagos previos;
- cualquier regla activa que determine si ese pedido puede aceptarse.

La interfaz no puede sustituir esta validación. El backend tiene la última palabra.

### 6.3 Resultado compatible

Si todas las verificaciones pasan, el backend crea el pedido de manera atómica y congela:

- importe y detalle cotizados;
- productos y servicios;
- modalidad y punto de entrega;
- medio y estado de pago o seña;
- versión operativa con la que se cotizó;
- versión operativa activa utilizada en la validación final;
- referencia o revisión comercial;
- fecha, hora y resultado de la auditoría.

Desde ese instante, cambios posteriores del motor, tarifas, puntos, impresoras o medios de pago no alteran automáticamente el pedido.

### 6.4 Resultado incompatible

Si alguna regla dejó de cumplirse, el pedido no se crea. El sistema debe:

- evitar cualquier alta parcial;
- explicar el motivo de forma concreta y no como error técnico genérico;
- conservar las selecciones que continúen siendo válidas;
- indicar qué dato debe corregirse;
- solicitar actualización o nueva cotización cuando cambien condiciones materiales;
- permitir reintentar una vez resuelta la incompatibilidad.

Ejemplos: punto deshabilitado, medio de pago ya no aceptado, servicio no disponible, capacidad incompatible, cotización vencida, condición económica incumplida o recepción pausada.

## 7. Relación con la pausa operativa

La pausa es una facultad operativa independiente del versionado:

- bloquea nuevas cotizaciones;
- bloquea confirmaciones pendientes aunque la cotización siga temporalmente vigente;
- no modifica pedidos ya creados;
- puede utilizarse como ventana de mantenimiento antes de activar cambios relevantes;
- al reanudar, el cliente debe reintentar la confirmación y el backend vuelve a validar.

## 8. Estados y transiciones visibles

| Estado encontrado | Acción disponible | Resultado |
|---|---|---|
| Activa sin pendiente | Editar configuración | Crea un único borrador |
| Borrador | Continuar o cancelar edición | Conserva o elimina el pendiente |
| Borrador validado | Activar ahora | Nueva activa; activa anterior a histórica |
| Borrador validado | Programar | Nueva programada; activa actual continúa |
| Programada | Consultar o cancelar | No se permite editar ni crear borrador |
| Histórica | Ver detalle o usar como base | Nunca se modifica la versión histórica |

## 9. Evidencia visual vigente

Los cuatro mockups conservan los componentes visuales y el sidebar de la Fase 6.

### MC-ADM-CFG-012 - Activar o programar

![MC-ADM-CFG-012 — Activar o programar](https://raw.githubusercontent.com/AgusFT/La-Montana/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-012-activar-o-programar.png)

La evidencia muestra Activar ahora seleccionado. Programar activación es un estado alternativo de la misma pantalla.

### MC-ADM-CFG-013 - Verificación de seguridad

![MC-ADM-CFG-013 — Verificación de seguridad](https://raw.githubusercontent.com/AgusFT/La-Montana/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-013-verificacion-seguridad.png)

### MC-ADM-CFG-014A - Activación inmediata confirmada

![MC-ADM-CFG-014A — Activación inmediata confirmada](https://raw.githubusercontent.com/AgusFT/La-Montana/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-014A-activacion-inmediata-confirmada.png)

### MC-ADM-CFG-014B - Activación programada confirmada

![MC-ADM-CFG-014B — Activación programada confirmada](https://raw.githubusercontent.com/AgusFT/La-Montana/b43604a7e0d917a4c6d13a495c2eef21d48fe6ed/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-014B-activacion-programada-confirmada.png)

La confirmación es una salida: no muestra Guardar y salir. Mantiene Volver al inicio, Consultar historial y acceso al detalle o programación según corresponda.

Documento visual específico: [WF-FASE-7-ACTIVACION-SEGURIDAD-CONFIRMACION.md](../diseño/Front/ux-ui/wireflows/WF-FASE-7-ACTIVACION-SEGURIDAD-CONFIRMACION.md).

## 10. Escenarios mínimos de prueba

1. Activación inmediata exitosa.
2. Fallo de activación sin transición parcial.
3. Programación exitosa manteniendo la versión activa.
4. Intento de editar o crear borrador con una versión programada.
5. Cancelación autorizada de la programación.
6. Activación automática en fecha y hora previstas.
7. Código vencido, inválido o reutilizado.
8. Edición posterior al desafío de seguridad.
9. Confirmación de cotización compatible después de un cambio de versión.
10. Rechazo por punto, medio de pago, servicio o capacidad incompatibles.
11. Rechazo por sistema pausado.
12. Pedido creado que permanece inmutable frente a cambios posteriores.
13. Cambio exclusivo de tarifas que no reescribe una cotización todavía válida.
14. Dos confirmaciones concurrentes sin creación duplicada.

## 11. Trazabilidad

- Épica UX/UI: [E19 - #197](https://github.com/AgusFT/La-Montana/issues/197)
- Fase 7: [#224](https://github.com/AgusFT/La-Montana/issues/224)
- Documentación a revisión: [PR #196](https://github.com/AgusFT/La-Montana/pull/196)
- Fase 6 y referencia de precios: [#226](https://github.com/AgusFT/La-Montana/issues/226)
- Administración de servicios y precios: [#227](https://github.com/AgusFT/La-Montana/issues/227)
