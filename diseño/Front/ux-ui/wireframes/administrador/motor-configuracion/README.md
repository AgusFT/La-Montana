# Wireframes editables — E19 Motor de configuración

Lienzo Excalidraw basado en el prototipo navegable V2 del motor de configuración administrativo. Reúne las ocho fases visuales y conserva la trazabilidad completa de **MC-ADM-CFG-001 a MC-ADM-CFG-016**.

![Vista general de las ocho fases](WF-E19-MOTOR-CONFIGURACION-preview.png)

## Archivo principal

- `WF-E19-MOTOR-CONFIGURACION.excalidraw`
- `WF-E19-MOTOR-CONFIGURACION-preview.png` — vista general para revisar en GitHub sin abrir el editor.

Puede abrirse en [Excalidraw](https://excalidraw.com) mediante **Open** o directamente en VS Code con la extensión Excalidraw. Todos los textos, tarjetas, botones y bloques son elementos editables.

## Cobertura

| Fase | Referencias | Alcance |
|---|---|---|
| 1 | CFG-001 | Estado actual, borrador, historial y predeterminados |
| 2 | CFG-002–003 | Modelo operativo y aprobación |
| 3 | CFG-004–005 | Pagos y reglas de seña |
| 4 | CFG-006–007 | Impresoras, capacidades y asignación |
| 5 | CFG-008–009 | Módulos contratados y entrega |
| 6 | CFG-010–011 | Resumen, simulación y conflictos |
| 7 | CFG-012–014 | Aplicación, seguridad y confirmación |
| 8 | CFG-015–016 | Historial y detalle de versiones |

## Criterios ya incorporados

- La configuración activa es inmutable y una edición se realiza mediante borrador.
- El borrador permite guardar y salir; la confirmación final no.
- Restaurar predeterminados o usar una versión histórica crea una base nueva y ejecuta compatibilidad dinámica.
- Las combinaciones incompatibles siguen visibles, deshabilitadas y explicadas.
- `ADMIN_ADMIN` es el rol autorizado; activar, programar, cancelar y restaurar requieren seguridad reforzada.
- Activación inmediata y programada son alternativas excluyentes.
- La operación final se ejecuta únicamente con `CONFIRMAR`.
- Después de confirmar solo se ofrecen `Ir al inicio` y `Consultar historial`.
- Pedidos y cotizaciones anteriores conservan las condiciones originales.

## Forma de trabajo

1. Hacer pull de la rama `docs/actualizacion-producto-configurable-revision`.
2. Abrir el archivo y editar una fase por vez.
3. Guardar antes de cambiar de equipo o editor.
4. Evitar editar el mismo lienzo simultáneamente: el JSON puede producir conflictos de Git.
5. Al aprobar una fase, exportarla como PNG con la nomenclatura `MC-ADM-CFG-###`.

## Fuente

- Prototipo V2: `diseño/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v2/MC-ADM-CFG-prototipo-navegable-v2.html`
- Épica: [E19 — #197](https://github.com/AgusFT/La-Montana/issues/197)
- Milestone: M16 — Evolución UX/UI del producto

> Estado: primera versión editable para revisión conjunta. Los valores demostrativos continúan señalados como ejemplos.
