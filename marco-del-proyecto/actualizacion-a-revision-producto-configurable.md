# Actualización a revisión - Producto configurable

| Campo | Valor |
|---|---|
| Versión | 2.0 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-08-21 |
| Responsables de revisión | Agustín Tejero y Alejandro Herms |
| Propósito | Servir como insumo para la integración posterior en la documentación oficial |

> Este documento no reemplaza ni modifica la documentación vigente. Reúne decisiones y propuestas surgidas del análisis del producto configurable. Agustín deberá revisarlo, contrastarlo y definir su integración definitiva.

## 1. Objetivo

Definir la evolución de La Montaña desde el alcance histórico del MVP hacia un producto comercial modular y configurable para imprentas.

El producto debe adaptarse a distintas formas de trabajo sin imponer como universales las decisiones operativas tomadas durante el MVP. La configuración debe mantenerse dentro de estructuras certificadas y coherentes, evitando un constructor libre de reglas que permita combinaciones inseguras o contradictorias.

## 2. Visión del producto

La Montaña será una plataforma modular para imprentas que permitirá:

- contratar y activar módulos según el plan adquirido;
- seleccionar un modelo operativo certificado;
- ajustar parámetros permitidos dentro de ese modelo;
- configurar aprobación, cobro, seña, impresoras y notificaciones;
- activar los cambios inmediatamente o programarlos;
- mantener trazabilidad de cada modificación;
- aplicar cada nueva configuración solamente a trabajos nuevos;
- preservar los estados internos fijos del pedido;
- adaptar la experiencia sin comprometer seguridad, integridad ni auditoría.

## 3. Principios confirmados

### 3.1 Estados internos fijos

Cada imprenta utilizará el mismo conjunto de estados internos y el mismo recorrido general en tiempo real.

No se configura qué estados existen. Se configura cuándo y bajo qué condiciones un trabajo puede avanzar entre ellos.

### 3.2 Configuraciones certificadas

El administrador no construirá reglas arbitrarias combinando cualquier criterio.

El sistema ofrecerá modelos operativos previamente definidos y validados. Dentro de cada modelo se habilitarán parámetros acotados y compatibles.

### 3.3 Configuración global

La configuración activa se aplica a todos los clientes y trabajos nuevos de la imprenta.

Las excepciones deben estar expresamente modeladas y auditadas. No deben existir modificaciones informales por pedido que eviten las reglas de seguridad.

### 3.4 Versionado y no retroactividad

Cada activación genera una versión inmutable.

Los trabajos anteriores conservan las condiciones con las que fueron cotizados. Una configuración nueva no modifica pedidos ni cotizaciones anteriores.

### 3.5 Modularidad comercial

La imprenta podrá activar o desactivar módulos incluidos en su plan.

Se distingue entre:

- módulo disponible por contratación;
- módulo activado por la imprenta.

Una imprenta no puede activar un módulo que no forma parte de su plan.

### 3.6 Experiencia visual

La configuración debe ser visual, guiada e intuitiva.

La interfaz debe explicar consecuencias y mostrar ejemplos. No debe reducirse a formularios extensos con términos técnicos o reglas aisladas.

### 3.7 Seguridad y auditoría

Toda configuración sensible se valida en backend y queda auditada.

La interfaz puede ocultar o deshabilitar acciones según el rol, pero la seguridad no puede depender del frontend.

## 4. Roles considerados en esta etapa

### 4.1 Administrador de máximo nivel de la imprenta

Nombre funcional utilizado durante el análisis: ADMIN_ADMIN.

Es la autoridad habilitada para:

- modificar configuraciones;
- activar cambios inmediatamente;
- programar cambios;
- cancelar cambios programados;
- configurar pagos y señas;
- configurar impresoras;
- activar módulos contratados;
- administrar clientes con cuenta corriente;
- consultar el historial completo de configuración.

El nombre formal del rol deberá validarse durante la integración definitiva para diferenciarlo del superadministrador de la plataforma.

### 4.2 Empleado de imprenta

En esta etapa no se definen todavía permisos operativos finos para empleados.

Como excepción confirmada, un empleado autenticado puede pausar o reanudar la recepción de pedidos mediante reconfirmación de contraseña.

### 4.3 Cliente

Puede cotizar, confirmar y consultar sus pedidos según la configuración vigente de la imprenta.

No puede modificar reglas, estados internos, configuraciones ni condiciones administrativas.

## 5. Capacidades configurables

La versión de configuración podrá contemplar:

- aprobación manual, automática o condicional;
- momento en que se habilita la producción;
- medios de pago disponibles;
- pago previo, pago al entregar u otros modelos certificados;
- reglas de seña;
- impresoras habilitadas;
- capacidades de las impresoras;
- asignación manual o automática;
- puntos y modalidades de entrega;
- notificaciones;
- horarios operativos;
- módulos activos;
- parámetros de servicios disponibles.

## 6. Pausa operativa

La pausa de recepción es una capacidad central del producto, no una mejora futura.

Puede utilizarse ante corte de luz, falla de impresoras, falta de insumos, saturación, emergencia o imposibilidad temporal de recibir trabajos.

La pausa:

- bloquea nuevas cotizaciones;
- bloquea la confirmación de nuevos pedidos;
- no elimina pedidos existentes;
- no modifica versiones de configuración;
- puede ser ejecutada por ADMIN_ADMIN o empleado;
- requiere reconfirmación de contraseña;
- registra usuario, rol, fecha, hora y motivo;
- puede revertirse mediante reanudación auditada.

## 7. Captura de configuración en la cotización

La versión aplicable se captura cuando el cliente presiona Cotizar pedido.

La cotización conserva:

- precio;
- medios de pago;
- punto o modalidad de entrega;
- seña;
- condiciones aplicables;
- versión de configuración;
- fecha estimada de entrega;
- fecha y hora de generación.

Una modificación posterior de la configuración no altera esa cotización.

## 8. Vigencia de la cotización

La cotización existe únicamente durante el flujo activo de confirmación.

Reglas acordadas:

1. Se muestran cinco minutos iniciales.
2. Al finalizar, aparece una consulta de actividad.
3. El cliente dispone de hasta sesenta segundos para responder.
4. Si no responde, la cotización se anula.
5. Si responde, recibe exactamente diez minutos adicionales.
6. El plazo adicional no se reinicia.
7. Al agotarse, se anula la cotización y se cierra la sesión.
8. Los datos y archivos temporales deben eliminarse.
9. La cotización no se guarda para utilizarla en una sesión posterior.

## 9. Pausa durante una cotización

Si la imprenta se pausa después de generar una cotización:

- el cliente no puede crear el pedido;
- el precio y las condiciones se conservan mientras la cotización continúe vigente;
- el temporizador de seguridad continúa;
- al reanudarse, el sistema revisa la fecha estimada de entrega;
- si la fecha cambia, debe mostrarse la anterior y la nueva;
- el cliente debe aceptar expresamente el nuevo plazo;
- si no lo acepta, no se crea el pedido.

## 10. Cuenta corriente - Requiere revisión

La cuenta corriente se plantea para clientes de confianza que pueden enviar trabajos sin pago previo y acumular deuda.

Decisiones propuestas:

- habilitación manual por ADMIN_ADMIN;
- cuenta con límite o sin límite;
- posibilidad de suspenderla;
- todos los pedidos a cuenta requieren revisión manual antes de producción;
- el administrador consulta deuda y carga operativa;
- el administrador define manualmente la fecha estimada de entrega;
- el cliente puede enviar el pedido sin pagar;
- enviar el pedido no implica autorización para producir.

Antes de aprobar, deberían mostrarse monto del nuevo pedido, deuda actual, deuda proyectada, pedidos impagos, trabajos en producción, fechas comprometidas, último pago, historial financiero y observaciones.

### Punto pendiente

Debe definirse qué ocurre al superar el límite:

- bloqueo absoluto;
- autorización excepcional del ADMIN_ADMIN;
- recepción del pedido con bloqueo hasta resolver la situación.

Esta sección debe ser validada expresamente por Agustín antes de integrarse como regla definitiva.

## 11. Capacidades futuras

Quedan fuera de la definición inmediata, pero se preservan como línea de evolución:

- cambios automáticos por demanda;
- reglas por franja horaria;
- pausa automática;
- programación para días posteriores;
- disponibilidad por stock;
- saturación de producción;
- reasignación dinámica de impresoras.

## 12. Base disponible para mockups

Puede avanzarse desde ahora con:

- inicio de configuración;
- selección de modelo operativo;
- configuración de aprobación;
- configuración de pagos;
- configuración de seña;
- impresoras y capacidades;
- módulos disponibles y activos;
- resumen y previsualización;
- activación inmediata;
- activación programada;
- protocolo de seguridad;
- historial de versiones;
- pausa y reanudación;
- mensajes de cotización expirada;
- tratamiento de una pausa durante la confirmación.

Las vistas de cuenta corriente pueden explorarse, pero deben quedar marcadas como sujetas a revisión.

## 13. Registro de cambios y justificación

| Cambio propuesto | Situación documentada anteriormente | Justificación vinculada al motor de configuración | Estado |
|---|---|---|---|
| Producto configurable posterior al MVP | Alcance centrado en una operación fija | El motor requiere que las decisiones operativas dejen de tratarse como universales | Confirmado para revisión |
| Estados fijos y transiciones configurables | Flujo único con avance predeterminado | Se preserva la trazabilidad y se permite adaptar el momento de avance | Confirmado |
| Configuraciones certificadas | Parámetros aislados o mejoras futuras | Evita combinaciones incoherentes y facilita instalación remota | Confirmado |
| Configuración versionada | No se definía vigencia completa | Garantiza no retroactividad y auditoría | Confirmado |
| Pausa operativa central | Considerada posibilidad futura | Es necesaria ante emergencias reales de la imprenta | Confirmado |
| Captura al cotizar | Creación directa con reglas generales | La cotización debe congelar precio y condiciones vigentes | Confirmado |
| Cuenta corriente | Excepción comercial no formalizada | El motor debe contemplar clientes autorizados sin debilitar el control financiero | Requiere revisión de Agustín |
| Módulos según plan | Modularidad principalmente técnica | Permite ofrecer paquetes comerciales escalables | Confirmado para revisión |

## 14. Referencias para integración

Esta actualización se origina en los cambios introducidos por el motor de configuración y deberá contrastarse con:

- marco-del-proyecto/alcance-general.md
- marco-del-proyecto/motor-de-configuracion-del-sistema.md
- analisis/especificacion-de-requerimientos/requerimientos-funcionales.md
- analisis/especificacion-de-requerimientos/requerimientos-no-funcionales.md
- analisis/especificacion-de-requerimientos/matriz-reglas-de-negocio.md
- analisis/historias-de-usuarios/historias-de-usuario.md
- analisis/casos-de-uso/casos-de-uso.md
- diseño/Front/ux-ui/wireflows/WF-ADMINISTRADOR-MVP.md
- marco-del-proyecto/matriz-trazabilidad.md

Hasta su integración, el presente archivo funciona únicamente como actualización a revisión.
