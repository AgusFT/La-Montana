# Requerimientos funcionales iniciales - La Montaña

| Campo | Valor |
|---|---|
| Versión | 1.0 |
| Estado | Borrador inicial |
| Fecha | 2026-05-20 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo del documento

Este documento define los requerimientos funcionales iniciales del sistema **La Montaña**.

Los requerimientos funcionales describen qué debe hacer el sistema desde el punto de vista del producto, del negocio y de los usuarios.

Este documento toma como base:

- `marco-del-proyecto/alcance-general.md`
- `marco-del-proyecto/objetivos-del-proyecto.md`
- `marco-del-proyecto/stakeholders-y-actores.md`

---

## 2. Criterio de clasificación

Cada requerimiento se identifica con un código único.

| Código | Área |
|---|---|
| RF-AUT | Autenticación, usuarios, roles y permisos |
| RF-PED | Gestión de pedidos |
| RF-ARC | Gestión de archivos |
| RF-REV | Revisión administrativa |
| RF-EST | Estados del pedido |
| RF-FIN | Gestión financiera básica |
| RF-WEB | Aplicación Web |
| RF-AND | Aplicación Android |
| RF-IMP | Subsistema de impresión |
| RF-AUD | Trazabilidad y auditoría |
| RF-CFG | Configuración y adaptación del producto |

La prioridad se expresa como:

| Prioridad | Significado |
|---|---|
| P0 Crítica | Requerimiento imprescindible para el flujo principal |
| P1 Alta | Requerimiento importante para completar la operación |
| P2 Media | Requerimiento relevante pero no bloqueante |
| P3 Baja | Requerimiento deseable o evolutivo |

El alcance se expresa como:

| Alcance | Significado |
|---|---|
| MVP | Debe estar en la primera versión funcional validable |
| Entrega final | Debe estar para considerar completo el producto base |
| Post-MVP | Queda como evolución futura o módulo opcional |

---

## 3. Requerimientos funcionales

### 3.1 Autenticación, usuarios, roles y permisos

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-AUT-001 | Autenticación de usuarios | El sistema debe permitir que los usuarios inicien sesión de forma segura. | P0 Crítica | MVP |
| RF-AUT-002 | Roles iniciales | El sistema debe contemplar como mínimo los roles cliente, empleado y administrador. | P0 Crítica | MVP |
| RF-AUT-003 | Acceso según rol | El sistema debe mostrar funcionalidades, vistas y acciones según el rol y los permisos del usuario autenticado. | P0 Crítica | MVP |
| RF-AUT-004 | Gestión de usuarios internos | El administrador debe poder gestionar usuarios internos del sistema. | P1 Alta | Entrega final |
| RF-AUT-005 | Control de permisos | El sistema debe validar permisos antes de permitir acciones sensibles, como aprobar pedidos, modificar estados internos, autorizar impresión o registrar cobros. | P0 Crítica | MVP |
| RF-AUT-006 | Sesión de cliente final | El cliente final debe poder acceder a su información y pedidos sin ver información interna de otros usuarios o de la operación completa. | P0 Crítica | MVP |

---

### 3.2 Gestión de pedidos

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-PED-001 | Crear pedido | El cliente debe poder crear un pedido de impresión desde el sistema. | P0 Crítica | MVP |
| RF-PED-002 | Registrar datos básicos del pedido | El sistema debe permitir registrar información básica del pedido, como tipo de trabajo, cantidad estimada, observaciones y archivos asociados. | P0 Crítica | MVP |
| RF-PED-003 | Pedido pendiente de revisión | Todo pedido creado por un cliente debe quedar inicialmente en estado pendiente de revisión. | P0 Crítica | MVP |
| RF-PED-004 | Consultar pedidos propios | El cliente debe poder consultar sus pedidos y el estado visible correspondiente. | P0 Crítica | MVP |
| RF-PED-005 | Consultar pedidos internos | Los empleados y administradores deben poder consultar pedidos según sus permisos internos. | P0 Crítica | MVP |
| RF-PED-006 | Editar datos antes de producción | El sistema debe permitir modificar o completar datos del pedido antes de que avance a producción, según permisos. | P1 Alta | MVP |
| RF-PED-007 | Registrar observaciones internas | Los usuarios internos deben poder registrar observaciones operativas o administrativas asociadas a un pedido. | P1 Alta | Entrega final |
| RF-PED-008 | Historial del pedido | El sistema debe permitir consultar el recorrido general del pedido desde su creación hasta su cierre. | P1 Alta | Entrega final |
| RF-PED-009 | Mostrar resumen previo del pedido | El sistema debe mostrar al cliente un resumen de la información ingresada antes de confirmar la creación del pedido. | P0 Crítica | MVP |
| RF-PED-010 | Mostrar costo estimado | El sistema debe calcular y mostrar un costo estimado del pedido antes de su confirmación. | P0 Crítica | MVP |
| RF-PED-011 | Mostrar tiempo estimado de entrega | El sistema debe informar un tiempo estimado de entrega basado en las características del pedido. | P0 Crítica | MVP |


---

### 3.3 Gestión de archivos

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-ARC-001 | Cargar archivos al pedido | El cliente debe poder cargar archivos asociados a un pedido. | P0 Crítica | MVP |
| RF-ARC-002 | Asociar archivos a pedidos | El sistema debe vincular cada archivo cargado con el pedido correspondiente. | P0 Crítica | MVP |
| RF-ARC-003 | Almacenamiento centralizado | Los archivos del pedido deben almacenarse de forma centralizada en Supabase Storage. | P0 Crítica | MVP |
| RF-ARC-004 | Acceso autorizado a archivos | El sistema debe permitir el acceso a archivos solo a usuarios o componentes autorizados. | P0 Crítica | MVP |
| RF-ARC-005 | Consulta de archivos por usuarios internos | Empleados y administradores deben poder consultar archivos asociados a pedidos según permisos. | P0 Crítica | MVP |
| RF-ARC-006 | Archivos disponibles para impresión | Los archivos autorizados deben estar disponibles para el subsistema de impresión sin depender de rutas locales del cliente. | P0 Crítica | MVP |
| RF-ARC-007 | Validación administrativa de archivos | El sistema debe permitir registrar si los archivos de un pedido fueron revisados, aceptados o requieren corrección. | P1 Alta | Entrega final |

---

### 3.4 Revisión administrativa

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-REV-001 | Revisión obligatoria | El sistema debe impedir que un pedido creado por un cliente avance automáticamente a producción. | P0 Crítica | MVP |
| RF-REV-002 | Aprobar pedido para avanzar | Un administrador o usuario autorizado debe poder aprobar el avance de un pedido luego de revisar datos y archivos. | P0 Crítica | MVP |
| RF-REV-003 | Solicitar correcciones | El sistema debe permitir marcar un pedido como pendiente de corrección cuando falten datos, archivos o condiciones necesarias. | P1 Alta | Entrega final |
| RF-REV-004 | Rechazar pedido | El sistema debe permitir rechazar un pedido cuando no pueda ser procesado. | P1 Alta | Entrega final |
| RF-REV-005 | Registrar decisión administrativa | Toda decisión administrativa relevante debe quedar asociada al pedido. | P0 Crítica | MVP |

---

### 3.5 Estados del pedido

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-EST-001 | Estado interno | El sistema debe mantener un estado interno para representar el avance operativo real del pedido. | P0 Crítica | MVP |
| RF-EST-002 | Estado visible al cliente | El sistema debe mantener un estado visible al cliente, separado del estado interno. | P0 Crítica | MVP |
| RF-EST-003 | Estado financiero | El sistema debe mantener un estado financiero separado del estado operativo. | P0 Crítica | MVP |
| RF-EST-004 | Cambio de estados por permisos | Solo usuarios autorizados deben poder modificar estados internos, visibles o financieros. | P0 Crítica | MVP |
| RF-EST-005 | Coherencia entre estados | El sistema debe evitar combinaciones inconsistentes entre estado interno, estado visible al cliente y estado financiero. | P0 Crítica | Entrega final |
| RF-EST-006 | Consulta de estados por cliente | El cliente debe poder ver únicamente el estado visible de su pedido y la información financiera que corresponda. | P0 Crítica | MVP |

---

### 3.6 Gestión financiera básica

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-FIN-001 | Identificar pedidos con seña | El sistema debe identificar pedidos que requieren seña según reglas de negocio. | P0 Crítica | MVP |
| RF-FIN-002 | Regla de 200 carillas | Si un pedido supera 200 carillas, el sistema debe requerir una seña del 30%. | P0 Crítica | MVP |
| RF-FIN-003 | Registrar estado financiero | El sistema debe permitir registrar el estado financiero del pedido. | P0 Crítica | MVP |
| RF-FIN-004 | Registrar cobro | El sistema debe permitir registrar el cobro total o parcial de un pedido. | P1 Alta | Entrega final |
| RF-FIN-005 | Asociar comprobante | El sistema debe permitir asociar un comprobante de pago cuando corresponda. | P1 Alta | Entrega final |
| RF-FIN-006 | Validar cierre financiero | El sistema debe validar que el pedido no se cierre si existen inconsistencias financieras relevantes. | P0 Crítica | Entrega final |
| RF-FIN-007 | Facturación legal | El sistema podrá incorporar facturación legal como módulo opcional. | P3 Baja | Post-MVP |
| RF-FIN-008 | El cliente debe poder indicar el método de pago previsto durante la creación del pedido. | P1 Alta | MVP |

---

### 3.7 Aplicación Web

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-WEB-001 | Acceso Web multirol | La Web debe ser utilizada por clientes, empleados y administradores. | P0 Crítica | MVP |
| RF-WEB-002 | Dashboard por rol | La Web debe mostrar un dashboard o vista principal adaptada al rol del usuario. | P0 Crítica | MVP |
| RF-WEB-003 | Creación de pedidos desde Web | El cliente debe poder crear pedidos desde la Web. | P0 Crítica | MVP |
| RF-WEB-004 | Carga de archivos desde Web | El cliente debe poder cargar archivos asociados a pedidos desde la Web. | P0 Crítica | MVP |
| RF-WEB-005 | Seguimiento de pedidos desde Web | El cliente debe poder consultar sus pedidos y estados visibles desde la Web. | P0 Crítica | MVP |
| RF-WEB-006 | Panel administrativo | Los administradores deben contar con una vista para revisar pedidos, archivos, estados y condiciones operativas. | P0 Crítica | MVP |
| RF-WEB-007 | Operación interna desde Web | Los empleados deben poder operar tareas internas según permisos. | P1 Alta | Entrega final |
| RF-WEB-008 | Gestión de cierre desde Web | Los usuarios autorizados deben poder registrar entrega, cobro, comprobante y cierre del pedido desde la Web. | P1 Alta | Entrega final |

---

### 3.8 Aplicación Android

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-AND-001 | Acceso Android | La app Android debe permitir acceso al sistema consumiendo el mismo backend que la Web. | P0 Crítica | MVP |
| RF-AND-002 | Autenticación Android | La app Android debe permitir iniciar sesión mediante el sistema de autenticación definido. | P0 Crítica | MVP |
| RF-AND-003 | Consulta de pedidos Android | La app Android debe permitir consultar pedidos y estados según rol y permisos. | P0 Crítica | MVP |
| RF-AND-004 | Carga de archivos Android | La app Android debe permitir cargar archivos asociados a pedidos si el alcance operativo lo requiere. | P1 Alta | Entrega final |
| RF-AND-005 | Coherencia con Web | La app Android debe respetar las mismas reglas de negocio, permisos y estados que la Web. | P0 Crítica | MVP |
| RF-AND-006 | Operación móvil básica | La app Android debe permitir al menos una operación funcional relevante sobre el flujo de pedidos. | P1 Alta | MVP |

---

### 3.9 Subsistema de impresión

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-IMP-001 | Print jobs autorizados | El sistema debe permitir generar o consultar trabajos de impresión autorizados. | P0 Crítica | MVP |
| RF-IMP-002 | Ejecución mediante Raspberry Pi y CUPS | El subsistema de impresión debe ejecutar trabajos mediante Raspberry Pi y CUPS. | P0 Crítica | MVP |
| RF-IMP-003 | Agente/gateway de impresión | El subsistema debe incluir un agente o gateway encargado de comunicarse con el backend y CUPS. | P0 Crítica | MVP |
| RF-IMP-004 | Acceso seguro a archivos | El agente de impresión debe acceder únicamente a archivos autorizados. | P0 Crítica | MVP |
| RF-IMP-005 | Sin decisiones de negocio | El subsistema de impresión no debe decidir si un pedido puede avanzar, cobrarse, cerrarse o producirse. | P0 Crítica | MVP |
| RF-IMP-006 | Reporte de estado técnico | El subsistema debe reportar estados técnicos relevantes de los trabajos de impresión. | P1 Alta | Entrega final |
| RF-IMP-007 | Gestión de errores de impresión | El sistema debe registrar errores o cancelaciones de trabajos de impresión cuando corresponda. | P1 Alta | Entrega final |

---

### 3.10 Trazabilidad y auditoría

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-AUD-001 | Registrar eventos relevantes | El sistema debe registrar eventos relevantes del flujo de pedidos. | P0 Crítica | MVP |
| RF-AUD-002 | Auditar decisiones administrativas | Las aprobaciones, rechazos, cambios de estado y autorizaciones deben quedar registradas. | P0 Crítica | MVP |
| RF-AUD-003 | Auditar archivos | El sistema debe registrar eventos relevantes asociados a carga, revisión y uso de archivos. | P1 Alta | Entrega final |
| RF-AUD-004 | Auditar cierre del pedido | El cierre del pedido debe quedar respaldado por registros de entrega, cobro, comprobante y estado final. | P0 Crítica | Entrega final |
| RF-AUD-005 | Consultar historial | Usuarios autorizados deben poder consultar historial o trazabilidad del pedido. | P1 Alta | Entrega final |

---

### 3.11 Configuración y adaptación del producto

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RF-CFG-001 | Base común adaptable | El sistema debe diseñarse como una base común adaptable a distintos negocios de impresión. | P1 Alta | Entrega final |
| RF-CFG-002 | Puntos de entrega | El sistema debe permitir representar puntos de entrega acordados para pedidos. | P1 Alta | Entrega final |
| RF-CFG-003 | Servicios de impresión | El sistema debe poder representar servicios ofrecidos por la imprenta. | P1 Alta | Entrega final |
| RF-CFG-004 | Módulo de inventario | El sistema podrá incorporar inventario como módulo opcional. | P3 Baja | Post-MVP |
| RF-CFG-005 | Reportes avanzados | El sistema podrá incorporar reportes avanzados como módulo opcional. | P3 Baja | Post-MVP |
| RF-CFG-006 | Seleccionar punto de entrega | El cliente debe poder seleccionar un punto de entrega o retiro al momento de crear el pedido. | P1 Alta | MVP |

---

## 4. Reglas funcionales críticas

Las siguientes reglas deben ser respetadas por el diseño funcional del sistema:

| Regla | Descripción |
|---|---|
| RFC-001 | Ningún pedido creado por un cliente pasa automáticamente a producción. |
| RFC-002 | Todo pedido nuevo queda inicialmente pendiente de revisión. |
| RFC-003 | Debe existir mediación administrativa humana antes de avanzar a producción. |
| RFC-004 | El sistema distingue estado interno, estado visible al cliente y estado financiero. |
| RFC-005 | Si el pedido supera 200 carillas, requiere seña del 30%. |
| RFC-006 | El cierre del pedido requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final. |
| RFC-007 | Los archivos del pedido son parte central del flujo. |
| RFC-008 | No se usan rutas locales del cliente como mecanismo de impresión. |
| RFC-009 | Web y Android consumen el mismo backend. |
| RFC-010 | El subsistema de impresión solo ejecuta trabajos autorizados y no toma decisiones de negocio. |

---

## 5. Trazabilidad documental inicial

| Documento base | Relación con este documento |
|---|---|
| `alcance-general.md` | Define el alcance que estos requerimientos detallan funcionalmente |
| `objetivos-del-proyecto.md` | Define los objetivos que estos requerimientos ayudan a cumplir |
| `stakeholders-y-actores.md` | Define actores y stakeholders sobre los que se apoyan estos requerimientos |

---

## 6. Criterios de aceptación del documento

Este documento se considera completo cuando:

- los requerimientos funcionales están identificados con códigos únicos;
- cada requerimiento tiene descripción, prioridad y alcance;
- los requerimientos cubren el flujo principal del sistema;
- las reglas críticas del negocio están contempladas;
- Web, Android, Supabase y subsistema de impresión están representados;
- existe coherencia con alcance, objetivos, stakeholders y actores;
- el documento puede usarse como base para historias de usuario, modelo de datos, arquitectura y pruebas.
