# Requerimientos no funcionales iniciales - La Montaña

| Campo | Valor |
|---|---|
| Versión | 1.0 |
| Estado | Borrador inicial |
| Fecha | 2026-05-24 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo del documento

Este documento define los requerimientos no funcionales iniciales del sistema **La Montaña**.

Los requerimientos no funcionales describen las cualidades que debe cumplir el sistema para operar de forma segura, mantenible, trazable, escalable y confiable.

Este documento complementa a los requerimientos funcionales definidos en `docs/analisis/espesificacion-de-requerimientos/requerimientos-funcionales.md`.

---

## 2. Criterio de clasificación

Cada requerimiento se identifica con un código único.

| Código | Área |
|---|---|
| RNF-SEG | Seguridad |
| RNF-AUT | Autenticación, autorización y permisos |
| RNF-RLS | Row Level Security en Supabase |
| RNF-ARC | Protección y acceso a archivos |
| RNF-AUD | Trazabilidad y auditoría |
| RNF-MAN | Mantenibilidad |
| RNF-MOD | Modularidad y extensibilidad |
| RNF-ESC | Escalabilidad |
| RNF-REN | Rendimiento |
| RNF-DIS | Disponibilidad y continuidad operativa |
| RNF-USA | Usabilidad |
| RNF-COM | Compatibilidad |
| RNF-IMP | Operación del subsistema de impresión |
| RNF-DOC | Documentación |

La prioridad se expresa como:

| Prioridad | Significado |
|---|---|
| P0 Crítica | Requerimiento imprescindible para seguridad, operación o coherencia del producto |
| P1 Alta | Requerimiento importante para completar el producto base |
| P2 Media | Requerimiento relevante pero no bloqueante |
| P3 Baja | Requerimiento deseable o evolutivo |

El alcance se expresa como:

| Alcance | Significado |
|---|---|
| MVP | Debe estar en la primera versión funcional validable |
| Producto base | Debe estar para considerar completo el producto principal |
| Post-MVP | Queda como evolución futura o módulo opcional |

---

## 3. Requerimientos no funcionales

### 3.1 Seguridad

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-SEG-001 | Seguridad por diseño | El sistema debe diseñarse considerando seguridad desde el inicio, no como agregado posterior. | P0 Crítica | MVP |
| RNF-SEG-002 | Protección de datos sensibles | El sistema debe proteger datos de usuarios, pedidos, archivos, estados, pagos, comprobantes y auditoría. | P0 Crítica | MVP |
| RNF-SEG-003 | Mínimo privilegio | Cada usuario, rol o componente debe acceder solo a la información y operaciones necesarias para cumplir su función. | P0 Crítica | MVP |
| RNF-SEG-004 | Validaciones del lado servidor | Las operaciones sensibles deben validarse en backend, RPC, Edge Functions o políticas de base de datos, evitando depender solo del frontend. | P0 Crítica | MVP |
| RNF-SEG-005 | Separación entre cliente y operación interna | El cliente final no debe acceder a información interna del negocio, de otros clientes ni de estados operativos no visibles. | P0 Crítica | MVP |
| RNF-SEG-006 | Protección contra acciones no autorizadas | El sistema debe impedir modificaciones no autorizadas sobre pedidos, archivos, estados, cobros, comprobantes, auditoría y trabajos de impresión. | P0 Crítica | MVP |

---

### 3.2 Autenticación, autorización y permisos

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-AUT-001 | Autenticación centralizada | La autenticación debe centralizarse mediante Supabase Auth. | P0 Crítica | MVP |
| RNF-AUT-002 | Autorización basada en roles y permisos | Las acciones del sistema deben autorizarse según roles y permisos definidos. | P0 Crítica | MVP |
| RNF-AUT-003 | Control de sesión | El sistema debe manejar sesiones de usuario de forma segura y coherente entre Web y Android. | P0 Crítica | MVP |
| RNF-AUT-004 | Usuarios internos diferenciados | Los usuarios internos deben diferenciarse de los clientes finales para evitar mezclas de permisos. | P0 Crítica | MVP |
| RNF-AUT-005 | Acciones administrativas restringidas | Las acciones administrativas deben estar restringidas a usuarios autorizados. | P0 Crítica | MVP |
| RNF-AUT-006 | Revisión de permisos antes de producción | Toda operación que avance un pedido hacia producción debe validar permisos y estado del pedido. | P0 Crítica | MVP |

---

### 3.3 Row Level Security en Supabase

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-RLS-001 | RLS obligatorio | Las tablas sensibles de Supabase deben tener Row Level Security habilitado. | P0 Crítica | MVP |
| RNF-RLS-002 | RLS en pedidos | Los pedidos deben protegerse para que cada cliente acceda solo a sus propios pedidos y los usuarios internos accedan según permisos. | P0 Crítica | MVP |
| RNF-RLS-003 | RLS en perfiles y usuarios | La información de perfiles, roles y permisos debe protegerse mediante políticas adecuadas. | P0 Crítica | MVP |
| RNF-RLS-004 | RLS en estados | Los estados internos, visibles al cliente y financieros deben protegerse para evitar lectura o modificación indebida. | P0 Crítica | MVP |
| RNF-RLS-005 | RLS en auditoría | Los registros de auditoría deben estar protegidos contra modificación indebida y acceso no autorizado. | P0 Crítica | Producto base |
| RNF-RLS-006 | RLS y Storage | Las políticas de acceso a archivos en Supabase Storage deben ser coherentes con las políticas de datos del pedido. | P0 Crítica | MVP |
| RNF-RLS-007 | RLS para agente de impresión | El agente/gateway de impresión debe acceder solo a trabajos y archivos autorizados. | P0 Crítica | MVP |
| RNF-RLS-008 | No depender solo del frontend | La seguridad de datos no debe depender únicamente de ocultar botones, rutas o pantallas en la interfaz. | P0 Crítica | MVP |
| RNF-RLS-009 | Documentación de políticas | La estrategia RLS debe quedar documentada antes de implementar el backend real. | P0 Crítica | Producto base |

---

### 3.4 Protección y acceso a archivos

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-ARC-001 | Archivos centralizados | Los archivos del pedido deben almacenarse en una ubicación centralizada y autorizada. | P0 Crítica | MVP |
| RNF-ARC-002 | Acceso controlado a archivos | El acceso a archivos debe depender del pedido, el usuario, el rol y los permisos correspondientes. | P0 Crítica | MVP |
| RNF-ARC-003 | No usar rutas locales del cliente | El sistema no debe depender de rutas locales del dispositivo del cliente para imprimir o procesar archivos. | P0 Crítica | MVP |
| RNF-ARC-004 | Archivos disponibles para impresión autorizada | El subsistema de impresión debe poder acceder a los archivos necesarios solo cuando exista autorización válida. | P0 Crítica | MVP |
| RNF-ARC-005 | Trazabilidad de archivos | Deben registrarse eventos relevantes asociados a carga, revisión, aceptación, rechazo o uso de archivos. | P1 Alta | Producto base |
| RNF-ARC-006 | Control de tipos y tamaños | El sistema debe contemplar restricciones sobre tipos y tamaños de archivos aceptados. | P1 Alta | Producto base |

---

### 3.5 Trazabilidad y auditoría

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-AUD-001 | Auditoría de eventos críticos | El sistema debe registrar eventos críticos del flujo de pedidos. | P0 Crítica | MVP |
| RNF-AUD-002 | Auditoría de decisiones administrativas | Las aprobaciones, rechazos, cambios de estado y autorizaciones deben quedar registradas. | P0 Crítica | MVP |
| RNF-AUD-003 | Auditoría de cierre | El cierre del pedido debe respaldarse con evidencia de entrega, cobro, comprobante, auditoría y estado final. | P0 Crítica | Producto base |
| RNF-AUD-004 | Integridad de auditoría | Los registros de auditoría no deben poder modificarse libremente por usuarios comunes. | P0 Crítica | Producto base |
| RNF-AUD-005 | Consulta de trazabilidad | Los usuarios autorizados deben poder consultar trazabilidad relevante de un pedido. | P1 Alta | Producto base |
| RNF-AUD-006 | Registro de errores operativos | El sistema debe registrar errores relevantes de impresión, archivos, validaciones y operaciones críticas. | P1 Alta | Producto base |

---

### 3.6 Mantenibilidad

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-MAN-001 | Código organizado por responsabilidades | El código debe organizarse separando responsabilidades de interfaz, lógica de negocio, acceso a datos e integración. | P0 Crítica | MVP |
| RNF-MAN-002 | Bajo acoplamiento | Los módulos deben evitar dependencias innecesarias entre sí para reducir costo de cambio. | P1 Alta | Producto base |
| RNF-MAN-003 | Alta cohesión | Cada módulo debe concentrar responsabilidades relacionadas y claramente delimitadas. | P1 Alta | Producto base |
| RNF-MAN-004 | Convenciones de nombres | El proyecto debe mantener convenciones consistentes para carpetas, archivos, componentes, funciones, tablas y documentos. | P1 Alta | MVP |
| RNF-MAN-005 | Documentación acompañando al código | Las decisiones relevantes deben quedar documentadas para facilitar mantenimiento y evolución. | P1 Alta | Producto base |
| RNF-MAN-006 | Control de cambios con Git | Los cambios deben versionarse mediante Git con commits claros y trazables. | P0 Crítica | MVP |

---

### 3.7 Modularidad y extensibilidad

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-MOD-001 | Producto adaptable | El sistema debe diseñarse como una base común adaptable a distintos negocios de impresión. | P1 Alta | Producto base |
| RNF-MOD-002 | Separación por dominios | El diseño debe separar dominios como pedidos, archivos, usuarios, estados, pagos, impresión y auditoría. | P1 Alta | Producto base |
| RNF-MOD-003 | Evolución por módulos | Funcionalidades como inventario, facturación legal y reportes avanzados deben poder incorporarse como evolución futura. | P2 Media | Post-MVP |
| RNF-MOD-004 | Evitar lógica rígida del cliente piloto | El sistema no debe quedar acoplado exclusivamente a la operación del cliente piloto. | P1 Alta | Producto base |
| RNF-MOD-005 | Configuración progresiva | El sistema debe permitir incorporar configuraciones por negocio sin reescribir la base principal del producto. | P2 Media | Post-MVP |

---

### 3.8 Escalabilidad

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-ESC-001 | Crecimiento de usuarios | El sistema debe poder evolucionar para soportar más clientes, empleados y administradores. | P1 Alta | Producto base |
| RNF-ESC-002 | Crecimiento de pedidos | El sistema debe contemplar crecimiento progresivo del volumen de pedidos y archivos. | P1 Alta | Producto base |
| RNF-ESC-003 | Crecimiento de impresoras | El subsistema de impresión debe poder adaptarse progresivamente a más impresoras o colas de impresión. | P2 Media | Post-MVP |
| RNF-ESC-004 | Base de datos relacional escalable | El modelo de datos debe diseñarse evitando estructuras difíciles de consultar, auditar o extender. | P1 Alta | Producto base |
| RNF-ESC-005 | Separación de responsabilidades técnicas | Web, Android, backend y agente de impresión deben poder evolucionar sin depender de despliegues completamente acoplados. | P1 Alta | Producto base |

---

### 3.9 Rendimiento

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-REN-001 | Respuesta aceptable en operaciones comunes | Las operaciones comunes, como consultar pedidos, estados y archivos, deben responder en tiempos adecuados para uso operativo. | P1 Alta | MVP |
| RNF-REN-002 | Consultas eficientes | Las consultas a base de datos deben diseñarse evitando lecturas innecesarias o excesivas. | P1 Alta | Producto base |
| RNF-REN-003 | Carga de archivos controlada | La carga de archivos debe contemplar límites, validaciones y feedback adecuado al usuario. | P1 Alta | Producto base |
| RNF-REN-004 | Operaciones transaccionales consistentes | Las operaciones críticas deben ejecutarse de forma consistente, evitando estados parciales o contradictorios. | P0 Crítica | MVP |
| RNF-REN-005 | Uso responsable de Realtime | Supabase Realtime debe utilizarse solo cuando aporte valor operativo real. | P2 Media | Producto base |

---

### 3.10 Disponibilidad y continuidad operativa

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-DIS-001 | Backend central disponible | Supabase debe ser tratado como servicio central necesario para Web, Android y operación principal. | P0 Crítica | MVP |
| RNF-DIS-002 | Manejo de errores | El sistema debe informar errores de forma clara cuando una operación no pueda completarse. | P1 Alta | MVP |
| RNF-DIS-003 | Evitar pérdida de información crítica | Las operaciones críticas deben reducir el riesgo de pérdida de pedidos, archivos, estados o registros de auditoría. | P0 Crítica | MVP |
| RNF-DIS-004 | Recuperación ante fallas operativas | El sistema debe contemplar recuperación o reintento ante errores de impresión, carga de archivos u operaciones transaccionales. | P1 Alta | Producto base |
| RNF-DIS-005 | Visibilidad de estado del sistema | Los usuarios internos deben poder identificar si un pedido, archivo o trabajo de impresión requiere atención. | P1 Alta | Producto base |

---

### 3.11 Usabilidad

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-USA-001 | Interfaz clara por rol | La interfaz debe mostrar acciones e información relevantes según rol. | P0 Crítica | MVP |
| RNF-USA-002 | Estados comprensibles | Los estados visibles al cliente deben ser claros y no exponer complejidad interna innecesaria. | P0 Crítica | MVP |
| RNF-USA-003 | Feedback de operaciones | El sistema debe informar al usuario cuando una operación fue exitosa, falló o requiere corrección. | P1 Alta | MVP |
| RNF-USA-004 | Flujo de pedido entendible | El flujo de creación, revisión, producción, entrega, cobro y cierre debe ser comprensible para usuarios internos. | P1 Alta | Producto base |
| RNF-USA-005 | Evitar sobrecarga visual | Las pantallas deben evitar mostrar información innecesaria para el rol activo. | P2 Media | Producto base |

---

### 3.12 Compatibilidad

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-COM-001 | Web moderna | La aplicación Web debe funcionar en navegadores modernos. | P0 Crítica | MVP |
| RNF-COM-002 | Android conectado al mismo backend | Android debe consumir el mismo backend y respetar las mismas reglas de negocio que la Web. | P0 Crítica | MVP |
| RNF-COM-003 | Consistencia entre clientes | Web y Android no deben implementar reglas de negocio contradictorias. | P0 Crítica | MVP |
| RNF-COM-004 | Compatibilidad con CUPS | El subsistema de impresión debe integrarse con CUPS para ejecutar trabajos de impresión. | P0 Crítica | MVP |
| RNF-COM-005 | Separación entre interfaz y backend | Las interfaces cliente no deben contener lógica crítica que deba pertenecer al backend. | P0 Crítica | MVP |

---

### 3.13 Operación del subsistema de impresión

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-IMP-001 | Ejecución autorizada | El subsistema de impresión debe ejecutar únicamente trabajos autorizados por el backend. | P0 Crítica | MVP |
| RNF-IMP-002 | Sin decisiones de negocio | El agente de impresión no debe decidir aprobación, cobro, cierre ni avance productivo del pedido. | P0 Crítica | MVP |
| RNF-IMP-003 | Reporte técnico | El subsistema debe poder reportar estado técnico, errores o cancelaciones relevantes. | P1 Alta | Producto base |
| RNF-IMP-004 | Acceso seguro a archivos | El agente debe acceder a archivos mediante mecanismos autorizados, no mediante rutas locales del cliente. | P0 Crítica | MVP |
| RNF-IMP-005 | Aislamiento operativo | Una falla del subsistema de impresión no debe corromper pedidos, estados financieros ni auditoría del sistema. | P0 Crítica | Producto base |

---

### 3.14 Documentación

| ID | Requerimiento | Descripción | Prioridad | Alcance |
|---|---|---|---|---|
| RNF-DOC-001 | Documentación de alcance | El sistema debe contar con documentación clara de alcance, objetivos, stakeholders y requerimientos. | P0 Crítica | MVP |
| RNF-DOC-002 | Documentación de arquitectura | Las decisiones arquitectónicas principales deben quedar documentadas. | P0 Crítica | Producto base |
| RNF-DOC-003 | Documentación de modelo de datos | El modelo de datos, tablas, relaciones, estados y reglas relevantes deben quedar documentados. | P0 Crítica | Producto base |
| RNF-DOC-004 | Documentación de seguridad | La estrategia de seguridad, roles, permisos, RLS y acceso a archivos debe quedar documentada. | P0 Crítica | Producto base |
| RNF-DOC-005 | Documentación del subsistema de impresión | La operación del agente/gateway, CUPS, print jobs y acceso a archivos debe quedar documentada. | P1 Alta | Producto base |
| RNF-DOC-006 | Coherencia documental | La documentación debe mantenerse coherente con el GitHub Project, los issues y el código. | P1 Alta | Producto base |

---

## 4. Reglas no funcionales críticas

Las siguientes reglas deben respetarse durante el diseño, implementación y evolución del sistema:

| Regla | Descripción |
|---|---|
| RNFC-001 | La seguridad no debe depender únicamente del frontend. |
| RNFC-002 | Supabase debe mantenerse como fuente única de verdad. |
| RNFC-003 | Las tablas sensibles deben contemplar Row Level Security. |
| RNFC-004 | El acceso a archivos debe estar autorizado y asociado al pedido correspondiente. |
| RNFC-005 | El cliente final no debe acceder a información interna del negocio. |
| RNFC-006 | El agente de impresión solo debe ejecutar trabajos autorizados. |
| RNFC-007 | El sistema debe registrar eventos críticos del flujo de pedidos. |
| RNFC-008 | El cierre del pedido no debe depender solo de la impresión. |
| RNFC-009 | Web y Android deben respetar las mismas reglas de backend. |
| RNFC-010 | El producto debe evitar quedar rígidamente acoplado al cliente piloto. |

---

## 5. Trazabilidad documental inicial

| Documento base | Relación con este documento |
|---|---|
| `alcance-general.md` | Define el alcance que estos requerimientos deben respetar |
| `objetivos-del-proyecto.md` | Define los objetivos que estos requerimientos ayudan a sostener |
| `stakeholders-y-actores.md` | Define actores, stakeholders y cliente piloto considerados |
| `requerimientos-funcionales.md` | Define las funciones que estos requerimientos acompañan y condicionan |

---

## 6. Criterios de aceptación del documento

Este documento se considera completo cuando:

- los requerimientos no funcionales están identificados con códigos únicos;
- cada requerimiento tiene descripción, prioridad y alcance;
- contempla seguridad, autorización y Row Level Security;
- contempla protección de archivos;
- contempla trazabilidad y auditoría;
- contempla mantenibilidad, modularidad y escalabilidad;
- contempla Web, Android, Supabase y subsistema de impresión;
- mantiene coherencia con los requerimientos funcionales;
- sirve como base para arquitectura, modelo de datos, implementación y pruebas.
