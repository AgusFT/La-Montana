# Actualización a revisión - Cuenta corriente

| Campo | Valor |
|---|---|
| Versión | 1.0 - Propuesta |
| Estado | Requiere revisión específica de Agustín |
| Fecha | 2026-08-21 |
| Dominio | Estados y finanzas |
| Integración | Pendiente |

> Este documento separa la propuesta de cuenta corriente del resto de las decisiones ya acordadas. No establece todavía reglas definitivas ni modifica los casos financieros existentes.

## 1. Objetivo

Analizar una modalidad comercial para clientes de confianza que pueden enviar trabajos sin pago previo, acumular pedidos y cancelar la deuda posteriormente.

La cuenta corriente busca representar prácticas reales de imprentas con clientes frecuentes, empresas o instituciones que envían grandes volúmenes de trabajo.

## 2. Principio central propuesto

Tener cuenta corriente permite enviar un pedido sin pagar, pero no autoriza automáticamente su producción.

Todo pedido realizado bajo esta modalidad debe ser revisado manualmente por ADMIN_ADMIN.

## 3. Habilitación

La cuenta corriente:

- se asigna manualmente;
- no se obtiene por reglas automáticas;
- solo puede administrarla ADMIN_ADMIN;
- puede estar activa, suspendida o deshabilitada;
- puede configurarse con límite o sin límite;
- debe conservar historial de cambios.

## 4. Flujo propuesto

1. El cliente habilitado prepara el pedido.
2. Genera la cotización.
3. Selecciona cuenta corriente si corresponde.
4. Confirma el pedido sin pago.
5. El pedido ingresa al flujo normal con bloqueo financiero.
6. El sistema informa que requiere revisión.
7. ADMIN_ADMIN consulta el pedido y la situación completa.
8. Evalúa deuda, capacidad y fechas comprometidas.
9. Define manualmente la fecha estimada de entrega.
10. Aprueba, rechaza o mantiene pendiente.
11. Solamente una aprobación habilita el avance hacia producción.
12. La decisión queda auditada.

## 5. Estados y bloqueos

No se propone agregar estados nuevos al recorrido principal.

La cuenta corriente debe representarse mediante una condición financiera o bloqueo asociado al pedido.

Valores conceptuales:

- requiere revisión de cuenta corriente;
- pendiente de aprobación;
- aprobado para continuar;
- rechazado;
- suspendido por situación financiera.

Los nombres técnicos deberán validarse.

## 6. Información para el administrador

La vista de revisión debería mostrar:

### Cliente y cuenta

- identificación;
- estado de cuenta;
- límite configurado;
- fecha de alta;
- observaciones;
- acuerdos comerciales registrados.

### Deuda

- deuda actual;
- monto del nuevo pedido;
- deuda proyectada;
- pagos registrados;
- último pago;
- cantidad de pedidos impagos;
- antigüedad de la deuda si se implementa.

### Producción

- pedidos pendientes;
- trabajos en producción;
- fechas comprometidas;
- recursos necesarios;
- capacidad disponible;
- impacto estimado del nuevo trabajo.

### Decisión

- aprobar;
- rechazar;
- mantener pendiente;
- observación obligatoria cuando corresponda;
- fecha estimada de entrega manual.

## 7. Fecha estimada de entrega

La fecha debe asignarse manualmente porque:

- los trabajos pueden ser grandes;
- el cliente puede tener varios pedidos;
- no existe cobro previo que priorice automáticamente el trabajo;
- deben contemplarse clientes que pagan en el momento;
- el administrador conoce la capacidad real de la imprenta;
- puede existir un acuerdo particular.

La fecha registrada debe quedar asociada a la decisión administrativa.

## 8. Límite de cuenta

Modalidades propuestas:

- sin límite;
- con límite monetario.

### Decisión pendiente

Cuando la deuda proyectada supera el límite, se deben comparar estas alternativas:

1. impedir la aprobación;
2. permitir autorización excepcional;
3. recibir el pedido y mantenerlo bloqueado hasta resolver deuda o límite.

Recomendación preliminar para revisión: recibir el pedido sin comprometer producción y mantenerlo bloqueado hasta una decisión expresa.

## 9. Permisos

| Acción | ADMIN_ADMIN | Empleado | Cliente |
|---|---:|---:|---:|
| Habilitar cuenta | Sí | No | No |
| Modificar límite | Sí | No | No |
| Suspender cuenta | Sí | No | No |
| Consultar deuda completa | Sí | Según permiso futuro | Solo información visible propia |
| Enviar pedido a cuenta | No aplica | No aplica | Sí, si está habilitado |
| Aprobar producción | Sí | No | No |
| Definir fecha estimada | Sí | No | No |

Los permisos futuros de empleados quedan fuera de esta propuesta.

## 10. Auditoría

Registrar:

- alta de cuenta;
- cambio de límite;
- suspensión;
- reactivación;
- pedido enviado a cuenta;
- deuda antes y después;
- revisión;
- decisión;
- fecha estimada;
- observaciones;
- actor y momento;
- autorización excepcional, si finalmente se permite.

## 11. Riesgos identificados

- acumulación excesiva de deuda;
- consumo de insumos sin cobro;
- ocupación de máquinas;
- desplazamiento de trabajos pagados;
- acuerdos informales no registrados;
- falta de fecha de pago;
- pérdida financiera;
- acceso indebido al beneficio;
- aprobación automática por error.

## 12. Controles propuestos

- asignación manual;
- revisión de todos los pedidos;
- resumen consolidado de deuda;
- bloqueo antes de producción;
- fecha de entrega manual;
- límite configurable;
- suspensión inmediata;
- auditoría;
- validación backend;
- observaciones administrativas.

## 13. Vistas exploratorias

Pueden diseñarse para facilitar la discusión:

- listado de clientes con cuenta corriente;
- detalle financiero del cliente;
- habilitar o suspender cuenta;
- configurar límite;
- revisar pedido a cuenta;
- resumen de deuda;
- asignar fecha estimada;
- confirmar aprobación;
- historial de decisiones.

Estas vistas deben rotularse como exploratorias hasta la validación de Agustín.

## 14. Preguntas para validación

- ¿El límite es absoluto o admite excepción?
- ¿La deuda debe tener vencimientos?
- ¿El cliente visualiza el total adeudado?
- ¿Cómo se registran pagos parciales?
- ¿Se admite agrupar varios pedidos en un pago?
- ¿La suspensión bloquea pedidos ya enviados?
- ¿Debe existir una prioridad inferior para trabajos a cuenta?
- ¿Qué información puede consultar un empleado?

## 15. Registro de cambios y justificación

| Propuesta | Situación previa | Justificación por el motor de configuración | Estado |
|---|---|---|---|
| Cuenta corriente manual | Lista especial genérica | Formaliza una excepción comercial sin liberar reglas arbitrarias | Requiere revisión |
| Revisión de todos los pedidos | Umbral por monto considerado inicialmente | Reduce exposición financiera y productiva | Propuesta de Alejandro |
| Fecha de entrega manual | Fecha calculada por flujo general | Permite contemplar deuda, tamaño y carga de trabajo | Propuesta de Alejandro |
| Bloqueo financiero sin estado nuevo | Estados del sistema fijos | Respeta el timeline y separa dimensiones financieras | Confirmado como criterio |
| Resumen consolidado | Revisión aislada del pedido | La decisión necesita conocer deuda y compromisos completos | Requiere revisión |
| Límite o sin límite | Excepción sin parámetro financiero | Da control comercial por cliente | Requiere revisión |
| Conducta al superar límite | No definida | Debe resolverse antes de la implementación | Pendiente de Agustín |

## 16. Referencias para integración

Esta propuesta surge de la necesidad de incorporar excepciones controladas al motor de configuración.

Debe contrastarse con:

- analisis/casos-de-uso/04-estados-y-finanzas
- analisis/especificacion-de-requerimientos/matriz-reglas-de-negocio.md
- analisis/especificacion-de-requerimientos/requerimientos-funcionales.md
- analisis/historias-de-usuarios/historias-de-usuario.md
- marco-del-proyecto/actualizacion-a-revision-producto-configurable.md

Hasta su validación, la cuenta corriente no debe considerarse una regla definitiva.
