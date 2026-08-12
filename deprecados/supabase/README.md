# Supabase deprecado

| Campo | Valor |
|---|---|
| Estado | Deprecado |
| Fecha de deprecacion | 2026-08-12 |
| Epica | #140 - Replanteo arquitectonico: Supabase deprecado y Spring Boot vigente |
| Issue | #144 - Crear carpeta deprecados/supabase |
| Milestone | M9 - Replanteo arquitectonico y deprecacion Supabase |

## Objetivo

Esta carpeta conserva la documentacion historica de la etapa en la que
Supabase era considerado backend y fuente de verdad del proyecto La Montana.

El material queda archivado como evidencia y trazabilidad. No representa la
arquitectura vigente.

## Motivo del cambio

El proyecto cambio el criterio arquitectonico: Supabase queda deprecado como
backend/fuente de verdad y la direccion vigente pasa a ser Spring Boot con
PostgreSQL.

La documentacion se mueve fuera de las carpetas vigentes para evitar mezclar
artefactos historicos con la nueva base documental.

## Alcance conservado

Se conservan documentos backend Supabase relacionados con:

- diseno de base de datos;
- modelo de tablas y relaciones;
- estrategia RLS;
- flujo de construccion y poblado de BDD;
- contrato de errores para Edge Functions.

## Artefactos movidos

| Archivo historico | Ubicacion original |
|---|---|
| `diseño-de-datos/guia-estilo-bdd.md` | `diseño/diseño-de-datos/guia-estilo-bdd.md` |
| `diseño-de-datos/modelo-tablas-relaciones-supabase.md` | `diseño/diseño-de-datos/modelo-tablas-relaciones-supabase.md` |
| `diseño-de-datos/estrategia-rls-supabase.md` | `diseño/diseño-de-datos/estrategia-rls-supabase.md` |
| `diseño-de-datos/flujo-construccion-poblado-bdd.md` | `diseño/diseño-de-datos/flujo-construccion-poblado-bdd.md` |
| `edge-functions/functions-errors.md` | `diseño/Back/edge-functions/functions-errors.md` |

## Fuera de este movimiento

No se movieron diagramas Supabase en este issue. Los diagramas v2 se trataran
en issues especificos de deprecacion y reemplazo por diagramas v3.

Tampoco se movio documentacion frontend, historias de usuario, casos de uso,
requerimientos ni trazabilidad general, porque este movimiento se limita a la
documentacion backend Supabase.
