# La Montaña

Sistema integral de gestión administrativa, operativa y productiva para una imprenta.

## Equipo

Proyecto universitario desarrollado por:

- Agustín Tejero
- Alejandro Herms

El proyecto tiene finalidad académica, pero se diseña con criterio de producción real: mantenibilidad, seguridad, trazabilidad, escalabilidad y documentación defendible.

## Arquitectura confirmada

- Supabase como fuente única de verdad.
- Supabase para PostgreSQL, Auth, Storage, Realtime, RPC y Edge Functions.
- Web para clientes, empleados y administradores.
- App Android desde el MVP.
- Subsistema de impresión con Raspberry Pi, CUPS, gateway/agente y print jobs.
- Sin módulo Desktop.
- El subsistema de impresión no toma decisiones de negocio; solo ejecuta trabajos autorizados.

## Documentación

La documentación del proyecto se organiza en las carpetas de raíz definidas por el WBS:

- `marco-del-proyecto/`
- `analisis/`
- `diseño/`
- `marketing/`

Documento inicial recomendado:

`marco-del-proyecto/guia-uso-github-project.md`

## Estado del repositorio

Este repositorio fue limpiado para alinear su estructura con la arquitectura actual del proyecto.

Las carpetas antiguas asociadas a Desktop, Firebase, Cursor, logs locales y placeholders iniciales fueron eliminadas porque ya no forman parte de la solución definida.

## Gestión del proyecto

El seguimiento oficial se realiza mediante GitHub Projects.

El Project se organiza con:

- campos personalizados;
- vistas por área;
- milestones;
- backlog;
- Kanban general;
- roadmap general;
- vista de ruta crítica;
- vista de riesgos.
