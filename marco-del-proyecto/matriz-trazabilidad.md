# Matriz de trazabilidad - La Montaña

| Campo | Valor |
|---|---|
| Versión | 1.5 |
| Estado | Actualizada por replanteo arquitectónico M9: Supabase histórico y Spring Boot/PostgreSQL vigente |
| Fecha | 2026-08-12 |
| Responsables | Agustín Tejero y Alejandro Herms |

## 1. Objetivo del documento

Este documento define la matriz de trazabilidad del sistema **La Montaña**.

Su objetivo es relacionar objetivos, requerimientos funcionales, requerimientos no funcionales, historias de usuario y reglas críticas del negocio para verificar que el proyecto mantenga coherencia entre lo que se quiere lograr, lo que el sistema debe hacer, las restricciones que debe respetar y los escenarios de uso que se deberán diseñar, implementar y validar.

La matriz permite detectar:

- requerimientos sin historia de usuario asociada;
- historias sin respaldo en requerimientos;
- reglas críticas sin cobertura funcional;
- áreas del sistema con trazabilidad débil;
- decisiones que deberán profundizarse en arquitectura, modelo de datos, pruebas o implementación.

---

## 2. Documentos base

Esta matriz toma como base los siguientes documentos:

| Documento | Uso dentro de la matriz |
|---|---|
| `marco-del-proyecto/alcance-general.md` | Define el alcance general y las reglas críticas del producto |
| `marco-del-proyecto/objetivos-del-proyecto.md` | Define objetivo general y objetivos específicos |
| `marco-del-proyecto/stakeholders-y-actores.md` | Define actores, stakeholders y cliente piloto |
| `analisis/especificacion-de-requerimientos/requerimientos-funcionales.md` | Define qué debe hacer el sistema |
| `analisis/especificacion-de-requerimientos/requerimientos-no-funcionales.md` | Define cualidades, restricciones y condiciones técnicas del sistema |
| `analisis/especificacion-de-requerimientos/matriz-reglas-de-negocio.md` | Consolida reglas funcionales y no funcionales críticas |
| `analisis/historias-de-usuarios/historias-de-usuario.md` | Define necesidades concretas desde el punto de vista de los actores |
| `analisis/casos-de-uso/casos-de-uso.md` | Define flujos funcionales detallados por dominio |
| `marco-del-proyecto/guia-uso-github-project.md` | Define el uso del Project, épicas, milestones y criterios de seguimiento |
| `README.md` | Resume la arquitectura vigente Spring Boot/PostgreSQL |
| `deprecados/supabase/README.md` | Centraliza la documentación Supabase deprecada como trazabilidad histórica |

---

## 3. Criterio de trazabilidad

La trazabilidad se organiza en cinco niveles:

| Nivel | Descripción |
|---|---|
| Objetivo | Resultado general o específico que el producto debe alcanzar |
| Requerimiento | Condición funcional o no funcional necesaria para cumplir el objetivo |
| Historia de usuario | Necesidad concreta expresada desde un actor del sistema |
| Regla crítica | Condición de negocio que no debe romperse durante diseño, desarrollo ni operación |
| Épica de Product Backlog | Iniciativa grande del Project que agrupa trabajo por milestone y permite planificar la entrega |

### 3.1 Criterio de vigencia arquitectónica

La trazabilidad funcional de pedidos, archivos, usuarios, estados, auditoría, Web, Android e impresión sigue vigente.

La trazabilidad técnica específica de Supabase, RLS, RPC, Edge Functions, Auth y Storage queda como histórico. No debe leerse como arquitectura backend vigente.

La arquitectura backend vigente se planifica sobre Spring Boot y PostgreSQL. Las restricciones técnicas equivalentes se profundizan en M10 a M14, especialmente en #155, #164, #171, #179 y #188.

Este criterio responde al replanteo arquitectónico de M9, épica #140 e issue documental #148.

---

## 4. Abreviaturas utilizadas

| Código | Significado |
|---|---|
| OBJ | Objetivo del proyecto |
| RF | Requerimiento funcional |
| RNF | Requerimiento no funcional |
| HU | Historia de usuario |
| E | Épica del Product Backlog |
| RFC | Regla funcional crítica |
| RNFC | Regla no funcional crítica |
| MVP | Primera versión funcional validable |
| Producto base | Producto principal completo y coherente |
| Post-MVP | Evolución futura o módulo opcional |

---

## 5. Matriz de objetivos a requerimientos e historias

| Objetivo | Descripción resumida | Requerimientos relacionados | Historias relacionadas | Área principal | Alcance |
|---|---|---|---|---|---|
| OBJ-001 | Centralizar la gestión del pedido | RF-PED-001, RF-PED-002, RF-PED-004, RF-PED-005, RF-PED-009, RF-PED-010, RF-PED-011, RNF-ESC-002 | HU-CLI-002, HU-CLI-004, HU-CLI-009, HU-EMP-001, HU-ADM-008 | Pedidos | MVP |
| OBJ-002 | Garantizar revisión administrativa antes de producción | RF-REV-001, RF-REV-002, RF-REV-005, RNF-AUT-006 | HU-ADM-001, HU-ADM-002, HU-SIS-001 | Revisión administrativa | MVP |
| OBJ-003 | Gestionar usuarios, roles y permisos | RF-AUT-001, RF-AUT-002, RF-AUT-003, RF-AUT-005, RNF-AUT-001, RNF-AUT-002 | HU-CLI-001, HU-ADM-003, HU-SIS-003 | Usuarios y seguridad | MVP |
| OBJ-004 | Centralizar archivos del pedido | RF-ARC-001, RF-ARC-002, RF-ARC-003, RF-ARC-004, RF-ARC-006, RNF-ARC-001, RNF-ARC-002 | HU-CLI-003, HU-EMP-002, HU-IMP-002 | Archivos | MVP |
| OBJ-005 | Separar estados del pedido | RF-EST-001, RF-EST-002, RF-EST-003, RF-EST-004, RNF-SEG-005 | HU-CLI-004, HU-ADM-004, HU-SIS-001 | Estados | MVP |
| OBJ-006 | Implementar reglas de negocio críticas | RF-REV-001, RF-FIN-002, RF-FIN-006, RF-IMP-005, RNF-SEG-004 | HU-SIS-001, HU-SIS-002, HU-SIS-003, HU-SIS-004, HU-IMP-004 | Reglas de negocio | MVP / Producto base |
| OBJ-007 | Desarrollar Web multirol | RF-WEB-001, RF-WEB-002, RF-WEB-003, RF-WEB-004, RF-WEB-005, RF-WEB-006 | HU-CLI-007, HU-ADM-008 | Web | MVP |
| OBJ-008 | Desarrollar Android conectado al mismo backend | RF-AND-001, RF-AND-002, RF-AND-003, RF-AND-005, RNF-COM-002, RNF-COM-003 | HU-CLI-008 | Android | MVP |
| OBJ-009 | Integrar subsistema de impresión autorizado | RF-IMP-001, RF-IMP-002, RF-IMP-003, RF-IMP-004, RF-IMP-005, RNF-IMP-001, RNF-IMP-002 | HU-IMP-001, HU-IMP-002, HU-IMP-003, HU-IMP-004 | Impresión | MVP / Producto base |
| OBJ-010 | Garantizar trazabilidad y auditoría | RF-AUD-001, RF-AUD-002, RF-AUD-004, RF-AUD-005, RNF-AUD-001, RNF-AUD-004 | HU-EMP-003, HU-ADM-006, HU-SIS-004 | Auditoría | MVP / Producto base |
| OBJ-011 | Mantener producto modular y adaptable | RF-CFG-001, RF-CFG-002, RF-CFG-003, RNF-MOD-001, RNF-MOD-004 | HU-ADM-007 | Configuración | Producto base |
| OBJ-012 | Mantener documentación coherente y versionada | RNF-DOC-001, RNF-DOC-002, RNF-DOC-003, RNF-DOC-004, RNF-DOC-006 | No aplica como historia operativa directa | Documentación | MVP / Producto base |

---

## 6. Matriz de requerimientos funcionales a historias de usuario

| Grupo RF | Descripción | Historias relacionadas | Cobertura inicial |
|---|---|---|---|
| RF-AUT | Autenticación, usuarios, roles y permisos | HU-CLI-001, HU-ADM-003, HU-SIS-003 | Cubierto |
| RF-PED | Gestión de pedidos | HU-CLI-002, HU-CLI-004, HU-CLI-009, HU-EMP-001, HU-EMP-003, HU-ADM-008 | Cubierto |
| RF-ARC | Gestión de archivos | HU-CLI-003, HU-EMP-002, HU-IMP-002 | Cubierto |
| RF-REV | Revisión administrativa | HU-ADM-001, HU-ADM-002, HU-SIS-001, HU-CLI-005, HU-CLI-009, HU-EMP-004 | Cubierto |
| RF-EST | Estados del pedido | HU-CLI-004, HU-ADM-004, HU-SIS-001, HU-EMP-004 | Cubierto |
| RF-FIN | Gestión financiera básica | HU-CLI-006, HU-CLI-009, HU-ADM-005, HU-SIS-002 | Cubierto |
| RF-WEB | Aplicación Web | HU-CLI-007, HU-ADM-008, HU-EMP-006 | Cubierto |
| RF-AND | Aplicación Android | HU-CLI-008 | Cobertura inicial mínima |
| RF-IMP | Subsistema de impresión | HU-IMP-001, HU-IMP-002, HU-IMP-003, HU-IMP-004, HU-EMP-005 | Cubierto |
| RF-AUD | Trazabilidad y auditoría | HU-EMP-003, HU-EMP-005, HU-ADM-006, HU-SIS-004 | Cubierto |
| RF-CFG | Configuración y adaptación del producto | HU-ADM-007, HU-CLI-009 | Cobertura inicial mínima |

---

## 7. Matriz de requerimientos no funcionales a historias de usuario

| Grupo RNF | Descripción | Historias relacionadas | Cobertura inicial |
|---|---|---|---|
| RNF-SEG | Seguridad | HU-CLI-001, HU-CLI-004, HU-ADM-003, HU-SIS-003 | Cubierto |
| RNF-AUT | Autenticación, autorización y permisos | HU-CLI-001, HU-EMP-001, HU-EMP-004, HU-ADM-002, HU-ADM-003, HU-SIS-003 | Cubierto |
| RNF-RLS | Seguridad de acceso a datos: RLS Supabase histórico, autorización backend Spring Boot vigente | HU-CLI-001, HU-CLI-004, HU-IMP-002, HU-SIS-003 | HUs reutilizables; reemplazo técnico a profundizar en M11 (#167) |
| RNF-ARC | Protección y acceso a archivos | HU-CLI-003, HU-EMP-002, HU-IMP-002 | Cubierto |
| RNF-AUD | Trazabilidad y auditoría | HU-EMP-003, HU-ADM-006, HU-SIS-004 | Cubierto |
| RNF-MAN | Mantenibilidad | No aplica como historia operativa directa | Debe tratarse en arquitectura y desarrollo |
| RNF-MOD | Modularidad y extensibilidad | HU-ADM-007 | Cobertura inicial |
| RNF-ESC | Escalabilidad | HU-ADM-007, HU-IMP-001 | Debe tratarse en arquitectura |
| RNF-REN | Rendimiento | HU-CLI-004, HU-CLI-007, HU-CLI-008, HU-EMP-001 | Debe tratarse en arquitectura y pruebas |
| RNF-DIS | Disponibilidad y continuidad operativa | HU-EMP-005, HU-SIS-004 | Cobertura inicial |
| RNF-USA | Usabilidad | HU-CLI-004, HU-CLI-007, HU-CLI-008, HU-ADM-008 | Cubierto |
| RNF-COM | Compatibilidad | HU-CLI-007, HU-CLI-008, HU-IMP-001 | Cubierto |
| RNF-IMP | Operación del subsistema de impresión | HU-IMP-001, HU-IMP-002, HU-IMP-003, HU-IMP-004 | Cubierto |
| RNF-DOC | Documentación | No aplica como historia operativa directa | Debe tratarse como criterio de gestión y calidad |

---

## 8. Matriz de reglas críticas del negocio

| Regla crítica | Descripción | Historias relacionadas | Requerimientos relacionados | Estado de cobertura |
|---|---|---|---|---|
| RFC-001 | Ningún pedido creado por cliente pasa automáticamente a producción | HU-CLI-002, HU-ADM-001, HU-SIS-001 | RF-PED-003, RF-REV-001 | Cubierta |
| RFC-002 | Todo pedido nuevo queda inicialmente pendiente de revisión | HU-CLI-002, HU-ADM-001, HU-SIS-001 | RF-PED-003, RF-REV-001 | Cubierta |
| RFC-003 | Debe existir mediación administrativa humana antes de avanzar a producción | HU-ADM-001, HU-ADM-002, HU-SIS-001 | RF-REV-001, RF-REV-002, RF-REV-005 | Cubierta |
| RFC-004 | El sistema distingue estado interno, estado visible al cliente y estado financiero | HU-CLI-004, HU-ADM-004, HU-SIS-001 | RF-EST-001, RF-EST-002, RF-EST-003 | Cubierta |
| RFC-005 | Si el pedido supera 200 carillas, requiere seña del 30% | HU-CLI-006, HU-SIS-002 | RF-FIN-001, RF-FIN-002 | Cubierta |
| RFC-006 | El cierre requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final | HU-ADM-005, HU-ADM-006, HU-SIS-004 | RF-FIN-006, RF-AUD-004 | Cubierta |
| RFC-007 | Los archivos del pedido son parte central del flujo | HU-CLI-003, HU-EMP-002, HU-IMP-002 | RF-ARC-001, RF-ARC-002, RF-ARC-006 | Cubierta |
| RFC-008 | No se usan rutas locales del cliente como mecanismo de impresión | HU-CLI-003, HU-IMP-002 | RF-ARC-006, RNF-ARC-003 | Cubierta |
| RFC-009 | Web y Android consumen el mismo backend | HU-CLI-007, HU-CLI-008 | RF-WEB-001, RF-AND-001, RF-AND-005 | Cubierta |
| RFC-010 | El subsistema de impresión solo ejecuta trabajos autorizados y no toma decisiones de negocio | HU-IMP-001, HU-IMP-004 | RF-IMP-001, RF-IMP-005, RNF-IMP-001, RNF-IMP-002 | Cubierta |

---

## 9. Matriz de reglas no funcionales críticas

| Regla crítica | Descripción | Historias relacionadas | Documentación o etapa donde se profundiza | Estado de cobertura |
|---|---|---|---|---|
| RNFC-001 | La seguridad no debe depender únicamente del frontend | HU-SIS-003, HU-ADM-003 | Arquitectura backend, autorización Spring Boot, servicios y persistencia PostgreSQL | Cubierta como restricción |
| RNFC-002 | Spring Boot/PostgreSQL debe operar como backend y persistencia vigentes | HU-CLI-007, HU-CLI-008, HU-IMP-001 | Arquitectura v3, M10 y M11 | Actualizada por M9 |
| RNFC-003 | Las operaciones y datos sensibles deben validar permisos del lado backend | HU-SIS-003, HU-CLI-001, HU-IMP-002 | Seguridad backend Spring Boot, roles, permisos y PostgreSQL | Reemplaza RLS Supabase |
| RNFC-004 | El acceso a archivos debe estar autorizado y asociado al pedido correspondiente | HU-CLI-003, HU-EMP-002, HU-IMP-002 | Almacenamiento protegido, autorización backend y agente de impresión | Cubierta |
| RNFC-005 | El cliente final no debe acceder a información interna del negocio | HU-CLI-001, HU-CLI-004 | Permisos backend, consultas autorizadas y diseño de vistas | Cubierta |
| RNFC-006 | El agente de impresión solo debe ejecutar trabajos autorizados | HU-IMP-001, HU-IMP-002, HU-IMP-004 | Subsistema de impresión y seguridad backend | Cubierta |
| RNFC-007 | El sistema debe registrar eventos críticos del flujo de pedidos | HU-ADM-006, HU-SIS-004 | Auditoría y modelo de datos | Cubierta |
| RNFC-008 | El cierre del pedido no debe depender solo de la impresión | HU-EMP-006, HU-ADM-005, HU-SIS-004 | Flujo de cierre y reglas de negocio | Cubierta |
| RNFC-009 | Web y Android deben respetar las mismas reglas de backend | HU-CLI-007, HU-CLI-008, HU-SIS-003 | Arquitectura y backend Spring Boot/PostgreSQL | Cubierta |
| RNFC-010 | El producto debe evitar quedar rígidamente acoplado al cliente piloto | HU-ADM-007 | Configuración, modularidad y arquitectura | Cubierta como criterio de diseño |

---

## 10. Cobertura por actor

| Actor | Historias asociadas | Áreas cubiertas | Observación |
|---|---|---|---|
| Cliente | HU-CLI-001 a HU-CLI-009 | Autenticación, pedidos, resumen previo, archivos, estados, seña, Web, Android | Cobertura suficiente para MVP inicial |
| Empleado | HU-EMP-001 a HU-EMP-006 | Consulta operativa, archivos, observaciones, producción, incidencias, entrega | Cobertura operativa inicial |
| Administrador | HU-ADM-001 a HU-ADM-008 | Revisión, aprobación, usuarios, estados, cobros, auditoría, configuración, panel Web | Cobertura fuerte para control interno |
| Agente de impresión | HU-IMP-001 a HU-IMP-004 | Print jobs, archivos autorizados, reporte técnico, separación de negocio | Cobertura suficiente para definir arquitectura del subsistema |
| Sistema | HU-SIS-001 a HU-SIS-004 | Reglas automáticas, estados, seña, cierre, seguridad, auditoría | Cobertura fuerte de reglas críticas |

---

## 11. Trazabilidad con Product Backlog y milestones

El GitHub Project se organiza como Product Backlog de alto nivel mediante épicas asociadas a milestones. Las historias de usuario, casos de uso, requerimientos y reglas críticas quedan como documentación de soporte y trazabilidad funcional.

| Épica | Issue | Milestone | Objetivos relacionados | Evidencia o issues relacionados |
|---|---:|---|---|---|
| E01 - Gestion del proyecto y documentacion | #14 | M0 - Setup del repo y Project | OBJ-012 | #15, #16, #17, #18 |
| E02 - Alcance, requerimientos y planificacion base | #19 | M1 - Documentacion base de alcance y planificacion | OBJ-001 a OBJ-012 | #20, #21, #22, #23, #25, #26, #27, #28, #35 |
| E03 - Diseño UX/UI y prototipo MVP | #43 | M1 - Documentacion base de alcance y planificacion | OBJ-007, OBJ-012 | #32, #33, #36, #38, #39, #40, #41, #82, #83, #84, #85, #87, #88, #89 |
| E04 - Arquitectura, modelo de datos y seguridad Supabase | #44 | M2 - Arquitectura y modelo de datos | OBJ-003, OBJ-004, OBJ-005, OBJ-006, OBJ-010 | Histórico Supabase: #92, #119, #120, #121, #122, diagramas de arquitectura |
| E05 - Backend MVP en Supabase | #45 | M3 - MVP backend Supabase | OBJ-001, OBJ-003, OBJ-004, OBJ-005, OBJ-006, OBJ-010 | Histórico Supabase: HU-CLI-006 (#57), HU-SIS-001 a HU-SIS-004 (#78 a #81), #93, #95, #96, #97, #98, #99, #100, #101, #102, #110, #111, #112, #113, #114, #115 |
| E06 - Portal Web cliente MVP | #46 | M4 - MVP Web | OBJ-001, OBJ-004, OBJ-007 | HU-CLI-001 a HU-CLI-005 (#52 a #56), HU-CLI-007 (#58), HU-CLI-009 (#91), #36, #38, #39, #40, #41, #94, #103, #104, #105, #106, #107, #108, #109, #116, #117 |
| E07 - Panel Web administrativo MVP | #47 | M4 - MVP Web | OBJ-002, OBJ-005, OBJ-006, OBJ-007, OBJ-010 | HU-EMP-001 a HU-EMP-006 (#60 a #65), HU-ADM-001 a HU-ADM-008 (#66 a #73), wireflows administrador, CU-REV, CU-CIE |
| E08 - Aplicación Android MVP | #48 | M5 - MVP Android | OBJ-008 | HU-CLI-008 (#59), CU-AND-001, CU-AND-002 |
| E09 - Subsistema de impresión | #49 | M6 - Subsistema de impresion | OBJ-009 | HU-IMP-001 a HU-IMP-004 (#74 a #77), CU-IMP-001 a CU-IMP-006 |
| E10 - Integración end-to-end del flujo de pedidos | #50 | M7 - Integracion end-to-end | OBJ-001 a OBJ-010 | Flujo completo de pedido, cierre y auditoría |
| E11 - Validación, pruebas y documentación final | #51 | M8 - Documentacion final y validacion | OBJ-012 | #35, #90, README, MVP y evidencias finales |
| E12 - Replanteo arquitectonico: Supabase deprecado y Spring Boot vigente | #140 | M9 - Replanteo arquitectonico y deprecacion Supabase | OBJ-003, OBJ-004, OBJ-006, OBJ-010, OBJ-012 | #144, #145, #146, #147, #148 |
| E13 - Fundacion backend Spring Boot y PostgreSQL | #155 | M10 - Fundacion Spring Boot y PostgreSQL | OBJ-001, OBJ-003, OBJ-004, OBJ-005, OBJ-006, OBJ-010 | #156, #157, #158, #159, #160, #161, #162, #163 |
| E14 - Seguridad, usuarios y permisos backend Spring Boot | #164 | M11 - Seguridad, usuarios y permisos backend | OBJ-003, OBJ-004, OBJ-006, OBJ-010 | #165, #166, #167, #168, #169, #170 |
| E15 - Flujo clientes backend Spring Boot | #171 | M12 - Flujo clientes backend | OBJ-001, OBJ-003, OBJ-004, OBJ-005, OBJ-006 | #172, #173, #174, #175, #176, #177, #178 |
| E16 - Flujo administracion backend Spring Boot | #179 | M13 - Flujo administracion backend | OBJ-002, OBJ-005, OBJ-006, OBJ-009, OBJ-010 | #180, #181, #182, #183, #184, #185, #186, #187 |
| E17 - Deploy backend Spring Boot | #188 | M14 - Deploy | OBJ-003, OBJ-004, OBJ-010, OBJ-012 | #189, #190, #191, #192, #193, #194, #195 |

---

## 12. Brechas y puntos a profundizar

La matriz no detecta reglas críticas sin cobertura inicial.

Sin embargo, existen puntos que deberán profundizarse en documentos posteriores:

| Punto a profundizar | Motivo | Documento o etapa futura |
|---|---|---|
| Modelo de estados del pedido | La separación entre estado interno, visible y financiero requiere diseño preciso | Histórico: #92. Vigente: #158 y #183 |
| Seguridad y autorización backend | La seguridad debe reemplazar RLS Supabase por validaciones backend, roles, permisos y acceso autorizado a datos | #164 a #170 |
| Modelo de datos PostgreSQL/Spring Boot | El diseño de tablas debe adaptarse a PostgreSQL y migraciones del backend vigente | #157, #158, #159, #162 |
| Modelo de archivos | Los archivos son centrales para revisión, producción e impresión | Histórico: #97 y #113. Vigente: #169, #174 y #191 |
| API backend del flujo mínimo | El backend debe centralizar creación, carga de archivos, confirmación, consultas y errores sin acceso directo del frontend a datos críticos | #160, #172 a #178, #180 a #187 |
| Configuración local backend | El entorno local debe resolver variables, PostgreSQL, migraciones, seeds y ejecución Spring Boot | #156, #161, #162 |
| Integración Web con backend | El Portal Cliente debe pasar de vistas funcionales a autenticación real, rutas protegidas y consumo de API Spring Boot | #160, #172 a #178 |
| Flujo de cierre del pedido | El cierre depende de entrega, cobro, comprobante, auditoría y estado final | Casos de uso, modelo de datos y pruebas |
| Agente de impresión | Debe definirse comunicación, autorización, errores y acceso a archivos | Arquitectura del subsistema de impresión |
| Android MVP | La cobertura inicial es mínima y deberá bajarse a funcionalidades concretas | Historias refinadas y diseño mobile |
| Configuración por cliente | El producto debe ser adaptable a distintas imprentas | Arquitectura modular y configuración |

---

## 13. Uso de esta matriz

Esta matriz debe usarse como referencia para:

- crear casos de uso;
- priorizar el Product Backlog por épicas;
- validar que cada historia tenga respaldo;
- detectar requerimientos sin implementación planificada;
- diseñar el modelo de datos;
- diseñar estrategia de seguridad y autorización backend;
- definir pruebas funcionales;
- validar la coherencia entre documentación, GitHub Project y desarrollo.

Cuando se agreguen nuevos requerimientos, historias, épicas o milestones, esta matriz debe actualizarse para evitar pérdida de trazabilidad.

---

## 14. Criterios de aceptación del documento

Este documento se considera completo cuando:

- relaciona objetivos, requerimientos, historias y reglas críticas;
- relaciona épicas del Product Backlog con milestones y evidencia existente;
- permite verificar cobertura de reglas de negocio;
- identifica brechas o puntos a profundizar;
- mantiene coherencia con los documentos base de requerimientos;
- puede ser usado como referencia para casos de uso, arquitectura, modelo de datos, seguridad, pruebas y planificación;
- queda versionado en Git.
