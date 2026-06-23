# La Montaña

Sistema integral de gestión administrativa, operativa y productiva para una imprenta.

## Equipo

Proyecto desarrollado por:

- Agustín Tejero
- Alejandro Herms

El proyecto se diseña con criterio de producción real: mantenibilidad, seguridad, trazabilidad, escalabilidad y documentación defendible.

## Arquitectura confirmada

- Supabase como fuente única de verdad.
- Supabase para PostgreSQL, Auth, Storage, Realtime, RPC y Edge Functions.
- Web para clientes, empleados y administradores.
- App Android desde el MVP.
- Subsistema de impresión con Raspberry Pi, CUPS, gateway/agente y print jobs.
- Sin módulo Desktop.
- El subsistema de impresión no toma decisiones de negocio; solo ejecuta trabajos autorizados.

## Documentación

La documentación y el desarrollo del proyecto se organizan desde las carpetas de raíz definidas por el WBS.

En `main` hoy se conserva la documentación vigente, diseño, análisis y marketing. Las ramas de desarrollo activas incorporan la carpeta `desarrollo/`, que se integrará a `main` cuando corresponda por merge.

Documento inicial recomendado:

`marco-del-proyecto/guia-uso-github-project.md`

## Mapa del repositorio

### Estructura actual en `main`

```text
La-Montana/
├── README.md
├── analisis/
│   ├── casos-de-uso/
│   │   ├── 01-pedidos/
│   │   ├── 02-archivos/
│   │   ├── 03-revision-administrativa/
│   │   ├── 04-estados-y-finanzas/
│   │   ├── 05-impresion/
│   │   ├── 06-usuarios-y-permisos/
│   │   ├── 07-trazabilidad-y-cierre/
│   │   └── 08-web-y-android/
│   ├── especificacion-de-requerimientos/
│   │   ├── matriz-reglas-de-negocio.md
│   │   ├── requerimientos-funcionales.md
│   │   └── requerimientos-no-funcionales.md
│   └── historias-de-usuarios/
│       └── historias-de-usuario.md
├── diseño/
│   ├── Back/
│   │   └── arquitectura-del-sistema/
│   │       └── diagramas/
│   └── Front/
│       └── ux-ui/
│           ├── documentacion/
│           │   ├── ejemplo-implementacion/
│           │   └── styles/
│           ├── vistas-android-mockups/
│           ├── vistas-web-mockups/
│           │   ├── administrador/
│           │   ├── cliente/
│           │   └── empleado/
│           └── wireflows/
│               ├── wireframes-administrador/
│               └── wireframes-cliente/
├── marco-del-proyecto/
│   ├── alcance-general.md
│   ├── control-de-versiones.md
│   ├── documento-alcance-parcial-2.docx
│   ├── gantt-roadmap-general.xlsx
│   ├── guia-uso-github-project.md
│   ├── matriz-trazabilidad.md
│   ├── objetivos-del-proyecto.md
│   ├── stakeholders-y-actores.md
│   ├── WBS V3.jpg
│   └── LINK MIRO - WBS
└── marketing/
    └── flyers-publicitarios/
```

### Estructura objetivo luego de integrar ramas activas

Este árbol muestra cómo debería leerse el repositorio cuando `main` incorpore el trabajo de las ramas actuales y las carpetas raíz previstas por el WBS. Dentro de cada carpeta se muestran solo subcarpetas que ya existen en `main` o en ramas activas.

```text
La-Montana/
├── README.md
├── analisis/
│   ├── casos-de-uso/
│   │   ├── 01-pedidos/
│   │   ├── 02-archivos/
│   │   ├── 03-revision-administrativa/
│   │   ├── 04-estados-y-finanzas/
│   │   ├── 05-impresion/
│   │   ├── 06-usuarios-y-permisos/
│   │   ├── 07-trazabilidad-y-cierre/
│   │   └── 08-web-y-android/
│   ├── especificacion-de-requerimientos/
│   └── historias-de-usuarios/
├── diseño/
│   ├── Back/
│   │   └── arquitectura-del-sistema/
│   │       └── diagramas/
│   └── Front/
│       └── ux-ui/
│           ├── documentacion/
│           ├── vistas-android-mockups/
│           ├── vistas-web-mockups/
│           │   ├── administrador/
│           │   ├── cliente/
│           │   └── empleado/
│           └── wireflows/
│               ├── wireframes-administrador/
│               └── wireframes-cliente/
├── marco-del-proyecto/
├── marketing/
│   └── flyers-publicitarios/
├── desarrollo/
│   ├── backend-supabase/
│   │   └── supabase/
│   │       ├── functions/
│   │       ├── migrations/
│   │       └── tests/
│   │           └── database/
│   ├── prototipo-figma/
│   │   ├── guidelines/
│   │   └── src/
│   └── web/
│       ├── public/
│       └── src/
│           ├── app/
│           ├── components/
│           ├── constants/
│           ├── features/
│           ├── layouts/
│           ├── lib/
│           ├── mocks/
│           ├── styles/
│           └── types/
├── validacion/
└── lanzamiento/
```

### Lectura por rama

- `main`: fuente principal para documentación vigente, análisis, diseño, marco del proyecto y marketing.
- `feat/web-mvp`: desarrollo del portal Web MVP y prototipo técnico asociado.
- `feat/backend-supabase`: configuración versionable del backend Supabase, migraciones, funciones y pruebas de base de datos.

## Estado del repositorio

Este repositorio fue limpiado para alinear su estructura con la arquitectura actual del proyecto.

Las carpetas antiguas asociadas a Desktop, Firebase, Cursor, logs locales y placeholders iniciales fueron eliminadas porque ya no forman parte de la solución definida.

## Gestión del proyecto

El seguimiento oficial se realiza mediante GitHub Projects.

Tablero público de seguimiento:

https://github.com/users/AgusFT/projects/4

El Project se organiza con:

- campos personalizados;
- vistas por área;
- milestones;
- backlog;
- Kanban general;
- roadmap general;
- vista de ruta crítica;
- vista de riesgos.
