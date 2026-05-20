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

La documentación del proyecto se organiza en:

```text
docs/