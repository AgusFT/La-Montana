# Mockups del motor de configuración administrativo

Primera versión navegable basada en la documentación de actualización a revisión del producto configurable y en el wireflow `WF-ACTUALIZACION-A-REVISION-ADMINISTRADOR-PRODUCTO-CONFIGURABLE.md`.

## Archivo principal

- `MC-ADM-CFG-prototipo-navegable.html`

El archivo es autocontenido: incluye estructura, estilos, datos demostrativos y navegación entre las 16 vistas. Puede abrirse directamente en un navegador sin instalar dependencias.

## Ubicación propuesta

```text
diseño/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion/
```

## Inventario

| Mockup | Wireflow | Vista |
|---|---|---|
| MC-ADM-CFG-001 | WF-CFG-01 | Inicio de configuración |
| MC-ADM-CFG-002 | WF-CFG-02 | Seleccionar modelo operativo |
| MC-ADM-CFG-003 | WF-CFG-03 | Configurar aprobación |
| MC-ADM-CFG-004 | WF-CFG-04 | Configurar pagos |
| MC-ADM-CFG-005 | WF-CFG-05 | Configurar seña |
| MC-ADM-CFG-006 | WF-CFG-06 | Impresoras y capacidades |
| MC-ADM-CFG-007 | WF-CFG-07 | Método de asignación |
| MC-ADM-CFG-008 | WF-CFG-08 | Módulos contratados y activos |
| MC-ADM-CFG-009 | WF-CFG-09 | Entrega y puntos habilitados |
| MC-ADM-CFG-010 | WF-CFG-10 | Resumen y simulación |
| MC-ADM-CFG-011 | WF-CFG-11 | Conflictos de configuración |
| MC-ADM-CFG-012 | WF-CFG-12 | Activar o programar |
| MC-ADM-CFG-013 | WF-CFG-13 | Verificación de seguridad |
| MC-ADM-CFG-014 | WF-CFG-14 | Confirmación de activación |
| MC-ADM-CFG-015 | WF-CFG-15 | Historial de versiones |
| MC-ADM-CFG-016 | WF-CFG-16 | Detalle de versión |

## Criterios de esta versión

- Mantiene la navegación y el lenguaje visual de los mockups `MC-ADM` existentes.
- Representa al actor `ADMIN_ADMIN` como único administrador de configuraciones finas.
- Separa la pausa operativa del motor versionado.
- Conserva la no retroactividad de pedidos y cotizaciones.
- Impide editar versiones activas o históricas.
- Marca como **Datos de ejemplo** las cantidades, nombres de equipos, fechas y valores todavía no confirmados.
- Conserva como regla documentada la seña del 30% desde 200 carillas.
- No incorpora cuenta corriente porque continúa como propuesta exploratoria pendiente de validación.

## Exportación futura

Cuando el equipo apruebe esta navegación, cada paso puede exportarse como imagen PNG de `1536 × 1024` y conservar la nomenclatura `MC-ADM-CFG-001` a `MC-ADM-CFG-016`.
