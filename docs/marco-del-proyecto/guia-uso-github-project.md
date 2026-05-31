# Guía de uso del GitHub Project - La Montaña

## 1. Objetivo

Este documento define cómo debe usarse el GitHub Project del proyecto La Montaña.

Sirve como guía común para:

- Agustín Tejero.
- Alejandro Herms.
- Asistentes de IA que colaboren con el proyecto.

El objetivo es mantener una estructura única para issues, épicas, historias de usuario, tareas técnicas, documentación, riesgos, milestones y vistas del Project.

## 2. Contexto resumido

La Montaña es un sistema integral para la gestión administrativa, operativa y productiva de una imprenta.

El proyecto se planifica con criterio de producto real: arquitectura mantenible, segura, trazable, escalable y defendible.

Decisiones cerradas:

- Supabase es la fuente única de verdad.
- Se usa Supabase para PostgreSQL, Auth, Storage, Realtime, RPC y Edge Functions.
- No existe módulo Desktop.
- La Web será usada por clientes, empleados y administradores.
- Habrá app Android desde el MVP.
- Web y Android consumirán el mismo backend.
- El subsistema de impresión usa Raspberry Pi, CUPS, gateway/agente y print jobs.
- El subsistema de impresión no toma decisiones de negocio.
- Ningún pedido creado por cliente pasa automáticamente a producción.
- Todo pedido nuevo queda pendiente de revisión.
- Debe existir revisión administrativa humana antes de producción.
- El sistema distingue estado interno, estado visible al cliente y estado financiero.
- Si un pedido supera 200 carillas, requiere seña del 30%.
- El cierre requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.
- No se usan rutas locales del cliente como mecanismo de impresión.

## 3. Metodología de trabajo

No se usará Scrum completo porque el equipo tiene dos integrantes y dedicación semanal reducida.

Se usará Kanban liviano + milestones + puntos de control + backlog estructurado.

Esto significa:

- No hay sprints rígidos por defecto.
- Las tareas avanzan por estados Kanban.
- Las entregas grandes se organizan con milestones.
- Las tareas se estructuran en épicas, historias de usuario y tareas técnicas.
- El avance debe poder ser explicado y defendido con evidencia documental, funcional y técnica.

## 4. Jerarquía de trabajo

La estructura recomendada es:

- Épica.
  - Historia de usuario.
    - Tarea técnica.
      - Sub-issue si hace falta.

Regla:

Si una tarea toca muchas áreas, se debe dividir en sub-issues.

Ejemplo incorrecto:

- Implementar pedidos en Web, Backend y Android.

Ejemplo correcto:

- Épica: Gestión integral de pedidos.
  - [BDD] Diseñar tablas de pedidos.
  - [API] Crear RPC para crear pedido.
  - [WEB] Crear formulario de pedido.
  - [ANDROID] Crear vista de pedido.

## 5. Campos del GitHub Project

Cada issue debe tener, como mínimo:

| Campo | Uso |
|---|---|
| Status | Estado operativo dentro del Kanban |
| Area | Área principal afectada |
| Tipo | Naturaleza del item |
| Prioridad | Importancia operativa |
| MoSCoW | Alcance comprometido |
| Puntos | Esfuerzo relativo |
| Horas | Estimación horaria aproximada |
| Dificultad | Complejidad técnica o funcional |
| Riesgo | Impacto potencial si falla o se retrasa |
| RutaCritica | Si pertenece al camino crítico manual |
| FechaInicio | Inicio planificado |
| FechaObjetivo | Fecha objetivo |
| Assignees | Responsable/s |
| Milestone | Hito asociado |

## 6. Status

| Status | Significado |
|---|---|
| Backlog | Tarea registrada pero todavía no refinada ni lista para ejecutar |
| Para hacer | Tarea refinada, estimada y lista para ser tomada |
| En desarrollo | Tarea tomada por un integrante y actualmente en ejecución |
| Bloqueado | Tarea detenida por dependencia, falta de definición o problema externo |
| Testeo | Tarea terminada preliminarmente y pendiente de prueba o validación funcional |
| En revision | Tarea terminada por quien la trabajó y pendiente de revisión cruzada por otro integrante |
| Terminado | Tarea completada, revisada y aceptada según la Definition of Done |
| Descartado | Tarea fuera del alcance actual, cancelada o reemplazada |

Importante: Status es el estado de avance del trabajo en GitHub Projects. No representa los estados internos de los pedidos del sistema.

## 7. Area

| Area | Uso |
|---|---|
| Backend/Supabase | Backend general en Supabase |
| BDD | Modelo de datos, tablas, relaciones, estados y persistencia |
| API/RPC/Edge | RPC, Edge Functions y operaciones server-side |
| Web/Frontend | Interfaz Web |
| Android | App Android |
| Impresion | Raspberry Pi, CUPS, gateway y print jobs |
| Arquitectura | Diagramas, decisiones técnicas y estructura general |
| Documentacion | Documentación de producto, técnica y funcional |
| Testing | Pruebas, validación y QA |
| Gestion | Organización, planificación, milestones y seguimiento |

Cada issue debe tener una sola Area principal.

## 8. Tipo

| Tipo | Uso |
|---|---|
| Epica | Iniciativa grande que agrupa historias o tareas |
| Historia de usuario | Necesidad funcional desde el punto de vista de un usuario |
| Tarea tecnica | Trabajo técnico concreto |
| Spike | Investigación para reducir incertidumbre |
| Bug | Error detectado |
| Riesgo | Evento potencial que puede afectar el proyecto |
| Documentacion | Trabajo documental |
| Mantenimiento | Ajuste menor, limpieza o mejora interna |

## 9. Prioridad

| Prioridad | Significado |
|---|---|
| P0 Critica | Imprescindible o bloqueante |
| P1 Alta | Muy importante |
| P2 Media | Relevante pero replanificable |
| P3 Baja | Deseable o ajuste menor |

## 10. MoSCoW

| MoSCoW | Significado |
|---|---|
| Debe estar | Imprescindible para MVP, producto base o validación del sistema |
| Deberia estar | Importante, pero ajustable si el tiempo no alcanza |
| Podria estar | Deseable si hay margen |
| No entra ahora | Fuera del alcance actual o post-MVP |

## 11. Estimación

### Puntos

| Puntos | Interpretación |
|---|---|
| 0 | Sin estimar |
| 1 | Muy simple |
| 2 | Pequeña |
| 3 | Normal |
| 5 | Mediana |
| 8 | Grande |
| 13 | Demasiado grande; conviene dividir |

### Horas

Las horas representan tiempo estimado real aproximado.

Como el equipo dedica entre 2 y 4 horas semanales por integrante, una tarea de 8 horas puede representar una o dos semanas reales de trabajo.

## 12. Dificultad y Riesgo

| Dificultad | Significado |
|---|---|
| Baja | Tarea simple, conocida o repetitiva |
| Media | Tarea normal con cierta complejidad |
| Alta | Tarea compleja o con incertidumbre técnica relevante |

| Riesgo | Significado |
|---|---|
| Bajo | Impacto menor si se retrasa o falla |
| Medio | Puede afectar planificación o calidad |
| Alto | Puede afectar una entrega, integración o regla central |
| Critico | Puede comprometer MVP, seguridad, datos, continuidad o validación del sistema |

## 13. RutaCritica

| RutaCritica | Significado |
|---|---|
| Si | Forma parte del camino crítico manual |
| No | No condiciona directamente la fecha final |
| Pendiente | Todavía no se decidió |

La vista Ruta Critica es un roadmap manual. GitHub Projects no calcula automáticamente el camino crítico.

## 14. Views configuradas

| View | Layout | Uso |
|---|---|---|
| General | Table | Vista completa de control |
| Backlog | Table | Items en estado Backlog |
| Kanban General | Board | Vista completa por Status |
| Backend | Board | Items con Area Backend/Supabase |
| BDD | Board | Items con Area BDD |
| API | Board | Items con Area API/RPC/Edge |
| Web Frontend | Board | Items con Area Web/Frontend |
| Android | Board | Items con Area Android |
| Documentacion | Board | Items con Area Documentacion |
| Arquitectura | Board | Items con Area Arquitectura |
| Testing | Board | Items con Area Testing |
| Impresion | Board | Items con Area Impresion |
| Gestion | Board | Items con Area Gestion |
| Roadmap General | Roadmap | Cronograma general |
| Ruta Critica | Roadmap | Camino crítico manual |
| Riesgos | Table | Issues de tipo Riesgo |

## 15. Milestones

| Milestone | Uso |
|---|---|
| M0 - Setup del repo y Project | Configuración inicial |
| M1 - Documentacion base de alcance y planificacion | Documentación inicial |
| M2 - Arquitectura y modelo de datos | Arquitectura, PlantUML y modelo de datos |
| M3 - MVP backend Supabase | Backend base |
| M4 - MVP Web | Web funcional |
| M5 - MVP Android | App Android funcional |
| M6 - Subsistema de impresion | Raspberry Pi, CUPS, gateway y print jobs |
| M7 - Integracion end-to-end | Flujo completo punta a punta |
| M8 - Documentacion final y validacion | Documentación final, demo y validación del sistema |

No usar milestones para áreas técnicas. Para eso existe Area.

## 16. Flujo de uso: crear una épica

1. Ir a Issues.
2. Crear New issue.
3. Usar título como: E01 - Gestion del proyecto y documentacion base.
4. Completar objetivo, alcance, fuera de alcance y criterios de aceptación.
5. Asociar al Project La-Montana.
6. Completar Status, Area, Tipo, Prioridad, MoSCoW, Puntos, Horas, Dificultad, Riesgo, RutaCritica, Milestone y Assignees.

## 17. Flujo de uso: crear una historia de usuario

Formato recomendado:

HU - Cliente crea pedido con archivos

Ejemplo:

Como cliente quiero crear un pedido y adjuntar archivos para solicitar un trabajo de impresion.

Debe incluir criterios de aceptación y estar vinculada a una épica cuando corresponda.

## 18. Flujo de uso: crear una tarea técnica

Formato recomendado:

- [BDD] Crear tabla orders.
- [API] Crear RPC para crear pedido.
- [WEB] Crear formulario de pedido.
- [ANDROID] Crear pantalla de detalle de pedido.
- [IMPRESION] Implementar consulta de print jobs autorizados.
- [DOC] Documentar flujo principal del pedido.

Una tarea técnica debe tener una sola Area.

## 19. Flujo Kanban

Flujo normal:

Backlog → Para hacer → En desarrollo → Testeo → En revision → Terminado.

Si aparece un impedimento:

En desarrollo → Bloqueado.

Si se descarta:

Backlog / Para hacer → Descartado.

## 20. Definition of Ready

Una issue está lista para pasar a Para hacer cuando tiene:

- Título claro.
- Descripción suficiente.
- Area definida.
- Tipo definido.
- Prioridad definida.
- MoSCoW definido.
- Milestone asignado.
- Responsable inicial.
- Estimación en puntos y horas.
- Criterios de aceptación si corresponde.
- Dependencias identificadas si existen.

## 21. Definition of Done

Una issue puede pasar a Terminado cuando fue completada, revisada y aceptada:

- Cumple sus criterios de aceptación.
- El código fue implementado si corresponde.
- La documentación fue actualizada si corresponde.
- No rompe reglas críticas del negocio.
- Fue validada por el otro integrante o probada manualmente.
- Está vinculada a PR o commit si corresponde.
- El Project refleja correctamente su estado final.

## 22. Prompt base para usar con ChatGPT o Codex

Estoy trabajando en el proyecto La Montaña junto con Agustín Tejero y Alejandro Herms.

La Montaña es un sistema integral para gestión administrativa, operativa y productiva de una imprenta.

Respetá estas decisiones cerradas:

- Supabase es la fuente única de verdad.
- Se usa Supabase para PostgreSQL, Auth, Storage, Realtime, RPC y Edge Functions.
- No existe módulo Desktop.
- La Web será usada por clientes, empleados y administradores según rol/permisos.
- Habrá app Android desde el MVP consumiendo el mismo backend.
- Se mantiene un subsistema de impresión con Raspberry Pi + CUPS + gateway/agente + print jobs.
- El subsistema de impresión no toma decisiones de negocio, solo ejecuta trabajos autorizados.
- Ningún pedido creado por cliente pasa automáticamente a producción.
- Todo pedido nuevo queda en pendiente de revisión.
- Debe existir mediación administrativa humana antes de producción.
- El sistema distingue estado interno, estado visible al cliente y estado financiero.
- Si el pedido supera 200 carillas, requiere seña del 30%.
- El cierre del pedido requiere consistencia entre entrega, cobro, comprobante, auditoría y estado final.
- No se deben usar rutas locales del cliente como mecanismo de impresión.

El GitHub Project se usa como dashboard oficial.

Cuando propongas issues, respetá esta estructura:

- Status.
- Area.
- Tipo.
- Prioridad.
- MoSCoW.
- Puntos.
- Horas.
- Dificultad.
- Riesgo.
- RutaCritica.
- FechaInicio.
- FechaObjetivo.
- Assignees.
- Milestone.

No inventes campos nuevos, no cambies la arquitectura y no propongas Desktop.

Si una tarea toca varias áreas, dividila en sub-issues.
