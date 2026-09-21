# Fase 6 — Resumen, simulación, conflictos y referencia tarifaria

| Campo | Valor |
|---|---|
| Estado | Evidencia visual aprobada para revisión |
| Fecha | 2026-09-14 |
| Épica | E19 #197 |
| Issue | #222 |
| Vista complementaria | #227 |

## Objetivo

Cerrar el recorrido de configuración antes de la aplicación o programación. La Fase 6 no incorpora parámetros nuevos: resume, simula y valida las decisiones de las Fases 1–5.

## Vista 1 — Resumen de Fase 6

La pantalla resume:

1. modelo operativo y aprobación;
2. pagos y reglas de seña;
3. impresoras y producción;
4. horarios y tiempos;
5. entrega y puntos;
6. referencia vigente de servicios y precios.

Cada bloque del motor permite regresar a su fase. Servicios y precios abre la vista administrativa independiente.

Después del resumen, la pantalla presenta:

- una simulación narrativa punta a punta;
- Bloqueos que impiden continuar;
- Advertencias que permiten continuar con atención;
- Información sobre consecuencias y vigencia.

## Vista 2 — Servicios y precios

La vista se accede desde:

- Dashboard / Administración;
- Fase 6 mediante Ver detalle de servicios y precios.

Permite consultar y administrar tarifas de impresión, productos, servicios y terminaciones admitidos. También permite agregar nuevos elementos dentro de las reglas certificadas.

## Reglas confirmadas

- Para activar la configuración inicial v1 debe existir una referencia tarifaria válida.
- Crear una v2 o posterior del motor no obliga a modificar precios si la referencia comercial vigente sigue siendo válida.
- Servicios y precios poseen ciclo de vida y trazabilidad propios; no forman parte de la versión operativa.
- Una revisión comercial puede activarse inmediatamente o programarse para fecha y hora.
- La revisión actual continúa vigente hasta la entrada en vigor de la programada.
- Los cambios se aplican a nuevas cotizaciones.
- Cotizaciones emitidas, pedidos confirmados y trabajos en curso conservan los precios y servicios capturados.
- Si falta una tarifa necesaria, Fase 6 registra un Bloqueo y no permite avanzar a Fase 7.

## Evidencia visual

Las dos imágenes aprobadas deben conservarse sin reconstrucciones ni sustituciones:

- `MC-ADM-CFG-010-011-resumen-simulacion-conflictos.png`;
- `MC-ADM-COM-001-servicios-precios.png`.

## Trazabilidad

- E19: #197
- Fase 6: #222
- Servicios y precios: #227
- Revisión DER: #226
- Decisión funcional: `marco-del-proyecto/decision-catalogo-servicios-precios.md`
- Checkpoint previo: `marco-del-proyecto/checkpoint-funcional-fases-1-5.md`
- PR de revisión: #196
