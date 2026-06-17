# Historias de usuario iniciales - La Montaña

| Campo | Valor |
|---|---|
| Versión | 1.0 |
| Estado | Borrador inicial |
| Fecha | 2026-05-24 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo del documento

Este documento define las historias de usuario iniciales del sistema **La Montaña**.

Las historias de usuario describen necesidades concretas desde el punto de vista de los actores del sistema. Sirven como puente entre el alcance, los requerimientos, el diseño funcional, el modelo de datos, la arquitectura, el desarrollo y las pruebas.

Este documento toma como base:

- `docs/marco-del-proyecto/alcance-general.md`
- `docs/marco-del-proyecto/objetivos-del-proyecto.md`
- `docs/marco-del-proyecto/stakeholders-y-actores.md`
- `docs/analisis/espesificacion-de-requerimientos/requerimientos-funcionales.md`
- `docs/analisis/espesificacion-de-requerimientos/requerimientos-no-funcionales.md`

---

## 2. Formato utilizado

Cada historia de usuario sigue este formato:

> Como [actor], quiero [acción], para [beneficio].

Cada historia incluye:

- identificador único;
- actor principal;
- historia;
- criterios de aceptación iniciales;
- prioridad;
- alcance;
- requerimientos relacionados.

---

## 3. Actores considerados

| Actor | Descripción |
|---|---|
| Cliente | Persona que solicita pedidos de impresión, carga archivos y consulta el avance visible |
| Empleado | Usuario interno que participa en revisión, producción, entrega u operación diaria |
| Administrador | Usuario interno con permisos ampliados para gestión, control y configuración |
| Agente de impresión | Componente técnico que consulta y ejecuta trabajos de impresión autorizados |
| Sistema | Comportamiento automático necesario para aplicar reglas, validar estados y proteger datos |

---

## 4. Historias de usuario

### 4.1 Historias de cliente

| ID | Actor | Historia | Criterios de aceptación iniciales | Prioridad | Alcance | Requerimientos relacionados |
|---|---|---|---|---|---|---|
| HU-CLI-001 | Cliente | Como cliente, quiero iniciar sesión en el sistema para acceder a mis pedidos y archivos. | El cliente puede autenticarse.<br>El cliente solo accede a su información.<br>El sistema no muestra información interna de otros clientes. | P0 Crítica | MVP | RF-AUT-001, RF-AUT-006, RNF-AUT-001, RNF-RLS-002 |
| HU-CLI-002 | Cliente | Como cliente, quiero crear un pedido de impresión para solicitar un trabajo a la imprenta. | El cliente puede registrar un pedido.<br>El pedido queda asociado al cliente autenticado.<br>El pedido queda inicialmente pendiente de revisión. | P0 Crítica | MVP | RF-PED-001, RF-PED-002, RF-PED-003, RF-REV-001 |
| HU-CLI-003 | Cliente | Como cliente, quiero cargar archivos en mi pedido para que la imprenta pueda revisarlos y usarlos en producción. | El cliente puede adjuntar archivos al pedido.<br>Los archivos quedan asociados al pedido correcto.<br>Los archivos se almacenan de forma centralizada.<br>No se usan rutas locales del cliente como mecanismo de impresión. | P0 Crítica | MVP | RF-ARC-001, RF-ARC-002, RF-ARC-003, RF-ARC-006 |
| HU-CLI-004 | Cliente | Como cliente, quiero consultar el estado visible de mis pedidos para conocer su avance sin acceder a información interna. | El cliente ve solo sus pedidos.<br>El cliente ve el estado visible correspondiente.<br>El sistema no expone estados internos no destinados al cliente. | P0 Crítica | MVP | RF-PED-004, RF-EST-002, RF-EST-006, RNF-SEG-005 |
| HU-CLI-005 | Cliente | Como cliente, quiero recibir indicaciones cuando mi pedido requiera correcciones para poder completar datos o archivos faltantes. | El sistema permite informar que un pedido requiere corrección.<br>El cliente puede identificar qué debe corregir.<br>El pedido no avanza a producción hasta ser revisado nuevamente. | P1 Alta | Producto base | RF-REV-003, RF-PED-006, RF-EST-002 |
| HU-CLI-006 | Cliente | Como cliente, quiero conocer si mi pedido requiere seña para poder continuar el proceso correctamente. | El sistema identifica pedidos que requieren seña.<br>Si el pedido supera 200 carillas, se informa la necesidad de seña del 30%.<br>El estado financiero visible es coherente con la situación del pedido. | P0 Crítica | MVP | RF-FIN-001, RF-FIN-002, RF-EST-003 |
| HU-CLI-007 | Cliente | Como cliente, quiero consultar desde la Web mis pedidos y su avance para hacer seguimiento sin depender de comunicación manual. | El cliente puede acceder a una vista Web.<br>Puede ver pedidos propios.<br>Puede ver estados visibles y datos relevantes del pedido. | P0 Crítica | MVP | RF-WEB-001, RF-WEB-002, RF-WEB-005 |
| HU-CLI-008 | Cliente | Como cliente, quiero consultar desde Android información básica de mis pedidos para hacer seguimiento desde el celular. | La app Android se autentica contra el mismo backend.<br>El cliente puede consultar pedidos propios.<br>Android respeta las mismas reglas que Web. | P0 Crítica | MVP | RF-AND-001, RF-AND-002, RF-AND-003, RF-AND-005 |
| HU-CLI-009 | Cliente | Como cliente, quiero ver un resumen de mi pedido antes de confirmarlo, para revisar los detalles, costo y tiempo estimado. | El sistema muestra un resumen del pedido en estado borrador. Se visualiza: cantidad de páginas, configuración del trabajo, nombre del archivo/pedido. Se muestra el precio final calculado. Se muestra el tiempo estimado de entrega. El cliente puede seleccionar método de pago. El cliente puede seleccionar punto de entrega. El pedido no se crea definitivamente hasta la confirmación explícita del usuario. | P0 Crítica | MVP | RF-PED-001, RF-PED-002, RF-PED-003, RF-REV-001 |

---

### 4.2 Historias de empleado

| ID | Actor | Historia | Criterios de aceptación iniciales | Prioridad | Alcance | Requerimientos relacionados |
|---|---|---|---|---|---|---|
| HU-EMP-001 | Empleado | Como empleado, quiero consultar pedidos asignados o disponibles para operar el flujo diario de trabajo. | El empleado puede consultar pedidos según permisos.<br>El sistema no muestra información fuera de su alcance.<br>Los pedidos muestran datos operativos necesarios. | P0 Crítica | MVP | RF-PED-005, RF-AUT-003, RNF-AUT-002 |
| HU-EMP-002 | Empleado | Como empleado, quiero revisar los archivos de un pedido para validar si pueden usarse en producción. | El empleado puede acceder a archivos autorizados.<br>Puede verificar archivos asociados al pedido.<br>El acceso queda controlado por permisos. | P0 Crítica | MVP | RF-ARC-004, RF-ARC-005, RF-ARC-007 |
| HU-EMP-003 | Empleado | Como empleado, quiero registrar observaciones internas en un pedido para dejar trazabilidad operativa. | El empleado puede agregar observaciones internas según permisos.<br>Las observaciones quedan asociadas al pedido.<br>El cliente no ve información interna no destinada a él. | P1 Alta | Producto base | RF-PED-007, RF-AUD-001, RNF-AUD-001 |
| HU-EMP-004 | Empleado | Como empleado, quiero preparar un pedido aprobado para producción para avanzar con el trabajo operativo. | El pedido debe haber sido revisado previamente.<br>El empleado solo puede avanzar si tiene permisos.<br>El sistema impide avanzar pedidos no autorizados. | P0 Crítica | MVP | RF-REV-001, RF-REV-002, RF-EST-004 |
| HU-EMP-005 | Empleado | Como empleado, quiero registrar incidencias de producción o impresión para que el pedido conserve trazabilidad. | El empleado puede registrar problemas operativos.<br>La incidencia queda asociada al pedido o trabajo de impresión.<br>El estado puede reflejar que requiere atención. | P1 Alta | Producto base | RF-AUD-001, RF-IMP-007, RNF-DIS-005 |
| HU-EMP-006 | Empleado | Como empleado, quiero registrar la entrega de un pedido para dejar constancia de que el cliente recibió el trabajo. | El pedido puede marcarse como entregado por usuario autorizado.<br>La entrega queda registrada.<br>El cierre no depende únicamente de la entrega. | P1 Alta | Producto base | RF-WEB-008, RF-AUD-004, RNF-AUD-003 |

---

### 4.3 Historias de administrador

| ID | Actor | Historia | Criterios de aceptación iniciales | Prioridad | Alcance | Requerimientos relacionados |
|---|---|---|---|---|---|---|
| HU-ADM-001 | Administrador | Como administrador, quiero revisar pedidos nuevos antes de que pasen a producción para evitar errores operativos o trabajos no validados. | Todo pedido nuevo queda pendiente de revisión.<br>El administrador puede aprobar, pedir corrección o rechazar según corresponda.<br>La decisión queda registrada. | P0 Crítica | MVP | RF-REV-001, RF-REV-002, RF-REV-005 |
| HU-ADM-002 | Administrador | Como administrador, quiero aprobar un pedido revisado para habilitar su avance a producción. | Solo usuarios autorizados pueden aprobar.<br>La aprobación valida datos y archivos mínimos.<br>El pedido puede avanzar recién después de la aprobación. | P0 Crítica | MVP | RF-REV-002, RF-EST-004, RNF-AUT-006 |
| HU-ADM-003 | Administrador | Como administrador, quiero gestionar usuarios internos para controlar quién puede operar el sistema. | El administrador puede gestionar usuarios internos.<br>Los usuarios internos tienen roles o permisos.<br>El sistema diferencia clientes de usuarios internos. | P1 Alta | Producto base | RF-AUT-004, RF-AUT-005, RNF-AUT-004 |
| HU-ADM-004 | Administrador | Como administrador, quiero controlar los estados internos, visibles y financieros de un pedido para mantener coherencia operativa. | El sistema separa los tres tipos de estado.<br>Solo usuarios autorizados pueden modificarlos.<br>Se evitan combinaciones inconsistentes. | P0 Crítica | MVP | RF-EST-001, RF-EST-002, RF-EST-003, RF-EST-005 |
| HU-ADM-005 | Administrador | Como administrador, quiero registrar cobros y comprobantes para mantener consistente el estado financiero del pedido. | Se puede registrar cobro total o parcial.<br>Se puede asociar comprobante cuando corresponda.<br>El cierre valida inconsistencias financieras relevantes. | P1 Alta | Producto base | RF-FIN-003, RF-FIN-004, RF-FIN-005, RF-FIN-006 |
| HU-ADM-006 | Administrador | Como administrador, quiero consultar la trazabilidad de un pedido para auditar decisiones, estados, archivos y cierre. | El sistema registra eventos relevantes.<br>El administrador puede consultar historial según permisos.<br>Las decisiones críticas quedan asociadas al pedido. | P0 Crítica | Producto base | RF-AUD-001, RF-AUD-002, RF-AUD-004, RF-AUD-005 |
| HU-ADM-007 | Administrador | Como administrador, quiero configurar servicios y puntos de entrega para adaptar el sistema al negocio. | El sistema puede representar servicios ofrecidos.<br>El sistema puede representar puntos de entrega.<br>La configuración no debe acoplar el producto a un único cliente piloto. | P1 Alta | Producto base | RF-CFG-001, RF-CFG-002, RF-CFG-003 |
| HU-ADM-008 | Administrador | Como administrador, quiero usar un panel Web de gestión para revisar pedidos, archivos, estados y condiciones operativas. | Existe una vista administrativa Web.<br>La vista respeta permisos.<br>Permite operar el flujo principal del pedido. | P0 Crítica | MVP | RF-WEB-006, RF-WEB-008, RNF-USA-001 |

---

### 4.4 Historias del agente de impresión

| ID | Actor | Historia | Criterios de aceptación iniciales | Prioridad | Alcance | Requerimientos relacionados |
|---|---|---|---|---|---|---|
| HU-IMP-001 | Agente de impresión | Como agente de impresión, quiero consultar trabajos de impresión autorizados para ejecutarlos mediante CUPS. | El agente consulta solo trabajos autorizados.<br>El backend decide qué trabajos están habilitados.<br>El agente no toma decisiones de negocio. | P0 Crítica | MVP | RF-IMP-001, RF-IMP-002, RF-IMP-003, RF-IMP-005 |
| HU-IMP-002 | Agente de impresión | Como agente de impresión, quiero acceder solo a los archivos autorizados para imprimir el pedido correcto. | El agente accede únicamente a archivos autorizados.<br>El acceso respeta permisos y políticas de seguridad.<br>No se usan rutas locales del cliente. | P0 Crítica | MVP | RF-IMP-004, RF-ARC-006, RNF-RLS-007, RNF-ARC-004 |
| HU-IMP-003 | Agente de impresión | Como agente de impresión, quiero reportar el estado técnico de un trabajo para que el sistema refleje su avance o error. | El agente puede informar estados técnicos.<br>Los errores relevantes quedan registrados.<br>El estado técnico no reemplaza el estado de negocio del pedido. | P1 Alta | Producto base | RF-IMP-006, RF-IMP-007, RNF-IMP-003 |
| HU-IMP-004 | Agente de impresión | Como agente de impresión, quiero ejecutar trabajos sin modificar reglas comerciales para mantener separado el subsistema técnico del negocio. | El agente no aprueba pedidos.<br>El agente no registra cobros.<br>El agente no cierra pedidos.<br>El agente solo ejecuta trabajos autorizados. | P0 Crítica | MVP | RF-IMP-005, RNF-IMP-001, RNF-IMP-002 |

---

### 4.5 Historias del sistema

| ID | Actor | Historia | Criterios de aceptación iniciales | Prioridad | Alcance | Requerimientos relacionados |
|---|---|---|---|---|---|---|
| HU-SIS-001 | Sistema | Como sistema, quiero separar estado interno, estado visible al cliente y estado financiero para evitar inconsistencias operativas. | Existen estados diferenciados.<br>El cliente no ve estados internos no destinados a él.<br>Los cambios requieren permisos. | P0 Crítica | MVP | RF-EST-001, RF-EST-002, RF-EST-003, RFC-004 |
| HU-SIS-002 | Sistema | Como sistema, quiero aplicar la regla de seña del 30% cuando un pedido supere 200 carillas para respetar la regla financiera definida. | El sistema identifica pedidos mayores a 200 carillas.<br>El sistema marca o informa requerimiento de seña.<br>El estado financiero refleja la condición correspondiente. | P0 Crítica | MVP | RF-FIN-001, RF-FIN-002, RFC-005 |
| HU-SIS-003 | Sistema | Como sistema, quiero aplicar seguridad en backend y base de datos para que la protección no dependa solo del frontend. | Las operaciones sensibles validan permisos.<br>Las tablas sensibles contemplan RLS.<br>Las políticas de archivos son coherentes con pedidos y roles. | P0 Crítica | MVP | RNF-SEG-004, RNF-RLS-001, RNF-RLS-008 |
| HU-SIS-004 | Sistema | Como sistema, quiero registrar eventos críticos del flujo de pedidos para permitir trazabilidad y auditoría. | Se registran eventos críticos.<br>Las decisiones administrativas quedan asociadas al pedido.<br>Los usuarios no autorizados no pueden modificar auditoría libremente. | P0 Crítica | MVP | RF-AUD-001, RF-AUD-002, RNF-AUD-001, RNF-AUD-004 |

---

## 5. Reglas críticas cubiertas

| Regla | Historias relacionadas |
|---|---|
| Ningún pedido creado por cliente pasa automáticamente a producción | HU-CLI-002, HU-ADM-001 |
| Todo pedido nuevo queda pendiente de revisión | HU-CLI-002, HU-ADM-001 |
| Debe existir revisión administrativa humana antes de producción | HU-ADM-001, HU-ADM-002 |
| El sistema distingue estado interno, visible y financiero | HU-CLI-004, HU-ADM-004, HU-SIS-001 |
| Si el pedido supera 200 carillas, requiere seña del 30% | HU-CLI-006, HU-SIS-002 |
| El cierre requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final | HU-ADM-005, HU-ADM-006 |
| Los archivos del pedido son parte central del flujo | HU-CLI-003, HU-EMP-002, HU-IMP-002 |
| No se usan rutas locales del cliente como mecanismo de impresión | HU-CLI-003, HU-IMP-002 |
| Web y Android consumen el mismo backend | HU-CLI-007, HU-CLI-008 |
| El subsistema de impresión solo ejecuta trabajos autorizados | HU-IMP-001, HU-IMP-004 |

---

## 6. Trazabilidad documental inicial

| Documento base | Relación con este documento |
|---|---|
| `alcance-general.md` | Define el alcance que las historias concretan |
| `objetivos-del-proyecto.md` | Define los objetivos que las historias ayudan a cumplir |
| `stakeholders-y-actores.md` | Define actores principales y contexto del cliente piloto |
| `requerimientos-funcionales.md` | Define funciones que las historias expresan en lenguaje de usuario |
| `requerimientos-no-funcionales.md` | Define cualidades y restricciones que las historias deben respetar |

---

## 7. Criterios de aceptación del documento

Este documento se considera completo cuando:

- las historias están identificadas con códigos únicos;
- cada historia indica actor, necesidad y beneficio;
- cada historia tiene criterios de aceptación iniciales;
- las historias cubren clientes, empleados, administradores, sistema y agente de impresión;
- las historias contemplan Web, Android, Supabase y subsistema de impresión;
- las historias respetan las reglas críticas del negocio;
- las historias pueden usarse como base para backlog, diseño, arquitectura, modelo de datos, pruebas y demo.
