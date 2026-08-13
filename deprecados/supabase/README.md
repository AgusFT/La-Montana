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
- contrato de errores para Edge Functions;
- diagramas v2 de arquitectura, componentes y despliegue.

## Artefactos movidos

| Archivo historico | Ubicacion original |
|---|---|
| `diseño-de-datos/guia-estilo-bdd.md` | `diseño/diseño-de-datos/guia-estilo-bdd.md` |
| `diseño-de-datos/modelo-tablas-relaciones-supabase.md` | `diseño/diseño-de-datos/modelo-tablas-relaciones-supabase.md` |
| `diseño-de-datos/estrategia-rls-supabase.md` | `diseño/diseño-de-datos/estrategia-rls-supabase.md` |
| `diseño-de-datos/flujo-construccion-poblado-bdd.md` | `diseño/diseño-de-datos/flujo-construccion-poblado-bdd.md` |
| `edge-functions/functions-errors.md` | `diseño/Back/edge-functions/functions-errors.md` |
| `diagramas/3 Diagrama de Arquitectura v2.png` | `diseño/Back/arquitectura-del-sistema/diagramas/3 Diagrama de Arquitectura v2.png` |
| `diagramas/4.1 Diagrama de Componentes v2.png` | `diseño/Back/arquitectura-del-sistema/diagramas/4.1 Diagrama de Componentes v2.png` |
| `diagramas/4.2 Diagrama de Despliegue v2.png` | `diseño/Back/arquitectura-del-sistema/diagramas/4.2 Diagrama de Despliegue v2.png` |

## Diagramas v2

Los diagramas v2 Supabase fueron deprecados en el issue #151 y se conservan en
`deprecados/supabase/diagramas/`.

Los reemplazos vigentes se trabajaran como diagramas v3:

- Diagrama de Arquitectura v3: #152.
- Diagrama de Componentes v3: #153.
- Diagrama de Despliegue v3 preliminar: #154.

## Fuera de este movimiento

Tampoco se movio documentacion frontend, historias de usuario, casos de uso,
requerimientos ni trazabilidad general, porque este movimiento se limita a la
documentacion backend Supabase.
