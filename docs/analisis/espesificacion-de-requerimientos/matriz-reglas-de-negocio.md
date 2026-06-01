# Matriz de reglas de negocio - La Montaña

| Campo | Valor |
|---|---|
| Documento | Matriz de reglas de negocio |
| Proyecto | La Montaña |
| Épica relacionada | E02 - Alcance, requerimientos y planificación base |
| Estado | Borrador inicial alineado a trazabilidad |
| Alcance | MVP / Producto base |
| Fuente principal | Matriz de trazabilidad inicial |
| Última actualización | Pendiente de completar |
| Responsable | Equipo del proyecto |

---

## 1. Objetivo del documento

Este documento define la matriz de reglas de negocio del sistema **La Montaña**.

Su objetivo es consolidar, en un único documento, las reglas críticas del negocio ya identificadas en la matriz de trazabilidad inicial.

Esta matriz no agrega reglas nuevas, no expande el alcance funcional y no incorpora decisiones técnicas no validadas. Su función es ordenar las reglas críticas existentes y permitir que sean utilizadas como referencia para:

- diseño funcional;
- casos de uso;
- arquitectura;
- modelo de datos;
- seguridad;
- backlog;
- pruebas;
- validación del MVP.

---

## 2. Relación con la matriz de trazabilidad

La matriz de trazabilidad inicial relaciona objetivos, requerimientos funcionales, requerimientos no funcionales, historias de usuario y reglas críticas del negocio.

Esta matriz de reglas de negocio toma como fuente esa trazabilidad y se limita a documentar las reglas críticas ya registradas allí.

Por lo tanto, las reglas incluidas en este documento deben existir previamente en la matriz de trazabilidad bajo alguno de estos tipos:

- `RFC`: Regla funcional crítica.
- `RNFC`: Regla no funcional crítica.

---

## 3. Criterio de alcance

Esta versión de la matriz de reglas de negocio incluye únicamente reglas críticas ya trazadas.

No incluye todavía:

- reglas operativas nuevas;
- reglas derivadas no documentadas;
- reglas específicas de implementación;
- reglas técnicas no validadas;
- casos de uso no cruzados formalmente;
- reglas de pricing avanzadas;
- reglas de configuración futura;
- reglas post-MVP no trazadas.

Cualquier ampliación futura deberá justificarse primero mediante actualización de la matriz de trazabilidad.

---

## 4. Abreviaturas utilizadas

| Código | Significado |
|---|---|
| RFC | Regla funcional crítica |
| RNFC | Regla no funcional crítica |
| RF | Requerimiento funcional |
| RNF | Requerimiento no funcional |
| HU | Historia de usuario |
| MVP | Primera versión funcional validable |
| Producto base | Producto principal completo y coherente |
| Post-MVP | Evolución futura o módulo opcional |

---

## 5. Matriz de reglas funcionales críticas

| ID | Regla de negocio | Historias relacionadas | Requerimientos relacionados | Estado de cobertura | Alcance | Observación |
|---|---|---|---|---|---|---|
| RFC-001 | Ningún pedido creado por cliente pasa automáticamente a producción. | HU-CLI-002, HU-ADM-001, HU-SIS-001 | RF-PED-003, RF-REV-001 | Cubierta | MVP | Regla central del flujo de pedidos. Impide que un pedido creado por cliente salte la revisión administrativa. |
| RFC-002 | Todo pedido nuevo queda inicialmente pendiente de revisión. | HU-CLI-002, HU-ADM-001, HU-SIS-001 | RF-PED-003, RF-REV-001 | Cubierta | MVP | Define el estado inicial obligatorio para pedidos creados por clientes. |
| RFC-003 | Debe existir mediación administrativa humana antes de avanzar a producción. | HU-ADM-001, HU-ADM-002, HU-SIS-001 | RF-REV-001, RF-REV-002, RF-REV-005 | Cubierta | MVP | La revisión administrativa es una condición obligatoria antes de producción. |
| RFC-004 | El sistema distingue estado interno, estado visible al cliente y estado financiero. | HU-CLI-004, HU-ADM-004, HU-SIS-002 | RF-EST-001, RF-EST-002, RF-EST-003 | Cubierta | MVP | Evita mezclar información operativa interna, información visible al cliente y situación financiera. |
| RFC-005 | Si el pedido supera 200 carillas, requiere seña del 30%. | HU-CLI-006, HU-SIS-003 | RF-FIN-001, RF-FIN-002 | Cubierta | MVP | Regla financiera crítica para pedidos de mayor volumen. |
| RFC-006 | El cierre requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final. | HU-ADM-005, HU-ADM-006, HU-SIS-004 | RF-FIN-006, RF-AUD-004 | Cubierta | MVP / Producto base | El cierre no depende únicamente de imprimir el pedido. |
| RFC-007 | Los archivos del pedido son parte central del flujo. | HU-CLI-003, HU-EMP-002, HU-IMP-002 | RF-ARC-001, RF-ARC-002, RF-ARC-006 | Cubierta | MVP | Los archivos son necesarios para revisión, producción e impresión. |
| RFC-008 | No se usan rutas locales del cliente como mecanismo de impresión. | HU-CLI-003, HU-IMP-002 | RF-ARC-006, RNF-ARC-003 | Cubierta | MVP | El acceso a archivos debe resolverse mediante almacenamiento y autorización del backend. |
| RFC-009 | Web y Android consumen el mismo backend. | HU-CLI-007, HU-CLI-008 | RF-WEB-001, RF-AND-001, RF-AND-005 | Cubierta | MVP | Evita duplicación de lógica y garantiza consistencia entre canales. |
| RFC-010 | El subsistema de impresión solo ejecuta trabajos autorizados y no toma decisiones de negocio. | HU-IMP-001, HU-IMP-004 | RF-IMP-001, RF-IMP-005, RNF-IMP-001, RNF-IMP-002 | Cubierta | MVP / Producto base | Raspberry, CUPS y gateway no definen reglas comerciales ni administrativas. |

---

## 6. Matriz de reglas no funcionales críticas

| ID | Regla de negocio / restricción crítica | Historias relacionadas | Documentación o etapa donde se profundiza | Estado de cobertura | Alcance | Observación |
|---|---|---|---|---|---|---|
| RNFC-001 | La seguridad no debe depender únicamente del frontend. | HU-SIS-005, HU-ADM-003 | Arquitectura, modelo de datos, RLS, RPC y Edge Functions | Cubierta como restricción | MVP | Las validaciones críticas deben existir en backend y base de datos. |
| RNFC-002 | Supabase debe mantenerse como fuente única de verdad. | HU-CLI-007, HU-CLI-008, HU-IMP-001 | Arquitectura general y modelo de datos | Cubierta como restricción | MVP | Web, Android e impresión deben operar sobre el mismo backend. |
| RNFC-003 | Las tablas sensibles deben contemplar Row Level Security. | HU-SIS-005, HU-CLI-001, HU-IMP-002 | Estrategia RLS en Supabase | Cubierta como restricción | MVP | Las políticas de acceso deben proteger datos por rol y ownership. |
| RNFC-004 | El acceso a archivos debe estar autorizado y asociado al pedido correspondiente. | HU-CLI-003, HU-EMP-002, HU-IMP-002 | Storage, RLS, políticas de acceso y agente de impresión | Cubierta | MVP | Los archivos no deben quedar expuestos fuera del flujo autorizado. |
| RNFC-005 | El cliente final no debe acceder a información interna del negocio. | HU-CLI-001, HU-CLI-004 | RLS, permisos y diseño de vistas | Cubierta | MVP | El cliente debe ver información adecuada a su rol. |
| RNFC-006 | El agente de impresión solo debe ejecutar trabajos autorizados. | HU-IMP-001, HU-IMP-002, HU-IMP-004 | Subsistema de impresión y seguridad backend | Cubierta | MVP / Producto base | El gateway de impresión no puede operar por fuera de autorizaciones del backend. |
| RNFC-007 | El sistema debe registrar eventos críticos del flujo de pedidos. | HU-ADM-006, HU-SIS-006 | Auditoría y modelo de datos | Cubierta | MVP / Producto base | Permite reconstruir acciones críticas y cambios relevantes. |
| RNFC-008 | El cierre del pedido no debe depender solo de la impresión. | HU-EMP-006, HU-ADM-005, HU-SIS-004 | Flujo de cierre y reglas de negocio | Cubierta | MVP / Producto base | Refuerza la regla funcional de cierre consistente. |
| RNFC-009 | Web y Android deben respetar las mismas reglas de backend. | HU-CLI-007, HU-CLI-008, HU-SIS-005 | Arquitectura y backend Supabase | Cubierta | MVP | Ambos canales deben consumir las mismas reglas y validaciones. |
| RNFC-010 | El producto debe evitar quedar rígidamente acoplado al cliente piloto. | HU-ADM-007 | Configuración, modularidad y arquitectura | Cubierta como criterio de diseño | Producto base | El sistema debe ser adaptable a otras imprentas en futuras etapas. |

---

## 7. Reglas agrupadas por área del negocio

| Área | Reglas relacionadas | Descripción |
|---|---|---|
| Pedidos | RFC-001, RFC-002, RFC-003 | Controlan la creación del pedido, el estado inicial y la revisión administrativa obligatoria. |
| Estados | RFC-004 | Define la separación entre estado interno, estado visible al cliente y estado financiero. |
| Finanzas | RFC-005, RFC-006, RNFC-008 | Controlan seña, cobro, comprobantes y cierre consistente. |
| Archivos | RFC-007, RFC-008, RNFC-004 | Definen que los archivos son parte central del flujo y deben ser accesibles de forma autorizada. |
| Impresión | RFC-010, RNFC-006 | Limitan al subsistema de impresión a ejecutar trabajos autorizados sin decidir reglas de negocio. |
| Seguridad | RNFC-001, RNFC-003, RNFC-005 | Definen restricciones de backend, RLS y visibilidad según rol. |
| Arquitectura | RNFC-002, RNFC-009 | Definen Supabase como fuente única de verdad y backend común para Web y Android. |
| Auditoría | RNFC-007 | Define la necesidad de registrar eventos críticos del flujo. |
| Producto | RNFC-010 | Define que el producto debe evitar acoplarse rígidamente al cliente piloto. |
| Canales | RFC-009, RNFC-009 | Definen coherencia entre Web, Android y backend. |

---

## 8. Cobertura por actor

| Actor | Reglas relacionadas | Observación |
|---|---|---|
| Cliente | RFC-001, RFC-002, RFC-004, RFC-005, RFC-007, RFC-008, RFC-009, RNFC-004, RNFC-005, RNFC-009 | El cliente puede crear pedidos, adjuntar archivos y consultar estados, pero no puede saltar revisión ni acceder a información interna. |
| Empleado | RFC-003, RFC-006, RFC-007, RFC-010, RNFC-004, RNFC-006, RNFC-008 | El empleado participa en revisión, operación, archivos, entrega e impresión dentro de reglas controladas. |
| Administrador | RFC-003, RFC-004, RFC-006, RNFC-001, RNFC-003, RNFC-005, RNFC-007, RNFC-010 | El administrador tiene responsabilidad sobre revisión, control interno, cobros, auditoría y configuración. |
| Agente de impresión | RFC-008, RFC-010, RNFC-004, RNFC-006 | El agente de impresión solo accede a archivos y trabajos autorizados. |
| Sistema | RFC-001, RFC-002, RFC-004, RFC-005, RFC-006, RFC-009, RNFC-001, RNFC-002, RNFC-003, RNFC-007, RNFC-009 | El sistema aplica validaciones, estados, seguridad, trazabilidad y consistencia entre canales. |

---

## 9. Reglas que impactan directamente el MVP

| Regla | Impacto en MVP |
|---|---|
| RFC-001 | El flujo de creación de pedidos debe impedir producción automática. |
| RFC-002 | Todo pedido nuevo debe iniciar pendiente de revisión. |
| RFC-003 | Debe existir revisión administrativa antes de producción. |
| RFC-004 | El modelo de estados debe separar estado interno, visible y financiero. |
| RFC-005 | El cálculo de seña debe existir para pedidos de más de 200 carillas. |
| RFC-007 | La carga y asociación de archivos debe estar disponible desde el MVP. |
| RFC-008 | El sistema no debe depender de rutas locales del cliente para imprimir. |
| RFC-009 | Web y Android deben consumir el mismo backend desde el MVP. |
| RFC-010 | El subsistema de impresión debe ejecutar solo trabajos autorizados. |
| RNFC-001 | Las reglas críticas no pueden depender solo del frontend. |
| RNFC-002 | Supabase debe operar como fuente única de verdad. |
| RNFC-003 | Las tablas sensibles deben contemplar RLS. |
| RNFC-004 | El acceso a archivos debe estar autorizado. |
| RNFC-005 | El cliente no debe ver información interna. |
| RNFC-006 | El agente de impresión no debe ejecutar trabajos no autorizados. |
| RNFC-009 | Web y Android deben respetar las mismas reglas de backend. |

---


## 11. Criterio de mantenimiento

Esta matriz debe actualizarse únicamente cuando cambie la matriz de trazabilidad o cuando se apruebe formalmente una nueva regla crítica.

No se deben agregar reglas nuevas directamente en este documento sin antes reflejarlas en la matriz de trazabilidad.

Toda regla agregada deberá indicar como mínimo:

- ID de regla crítica;
- descripción;
- historias relacionadas;
- requerimientos relacionados o documentación donde se profundiza;
- estado de cobertura;
- alcance.

---

## 12. Criterios de aceptación del documento

Este documento se considera válido cuando:

- usa como fuente principal la matriz de trazabilidad inicial;
- no agrega reglas no trazadas;
- conserva las reglas funcionales críticas `RFC-001` a `RFC-010`;
- conserva las reglas no funcionales críticas `RNFC-001` a `RNFC-010`;
- mantiene relación con historias de usuario;
- mantiene relación con requerimientos funcionales o no funcionales cuando corresponde;
- identifica impacto sobre el MVP;
- identifica reglas que requieren profundización posterior;
- queda versionado en Git.

---
