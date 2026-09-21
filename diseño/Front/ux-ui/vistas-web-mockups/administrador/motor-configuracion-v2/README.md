# Mockups del motor de configuración administrativo

Segunda versión navegable basada en las decisiones acordadas sobre ciclo de vida, compatibilidad dinámica, seguridad e interacción del motor de configuración.

## Archivo principal

- `MC-ADM-CFG-prototipo-navegable-v2.html`

El archivo es autocontenido: incluye estructura, estilos, datos demostrativos y navegación entre las 16 vistas. Puede abrirse directamente en un navegador sin instalar dependencias.

## Ubicación propuesta

```text
diseño/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v2/
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

## Cambios principales de la V2

- Mantiene la navegación y el lenguaje visual de los mockups `MC-ADM` existentes.
- Presenta `CFG-001` con y sin borrador mediante una demostración navegable.
- Reemplaza “Crear nueva configuración” por `Editar configuración`.
- Permite continuar, guardar y salir o cancelar una edición.
- Incorpora restauración de valores predeterminados con seguridad reforzada.
- Permite utilizar una versión histórica como base de una versión nueva.
- Ejecuta un barrido de compatibilidad para bases históricas o predeterminadas.
- Explica recursos dinámicos faltantes, como impresoras que requieren reasignación.
- Vincula modelo operativo y aprobación mediante opciones certificadas.
- Actualiza el ejemplo de recorrido según control manual o condicional.
- Mantiene visibles y explica las opciones incompatibles.
- Agrega guardado progresivo en todos los pasos editables.
- Incorpora barrido final, impacto económico y no retroactividad antes de activar.
- Representa contraseña, código por correo e inactividad administrativa.
- Mantiene a `ADMIN_ADMIN` como único actor autorizado para configuración y operación crítica.
- Conserva la no retroactividad de pedidos y cotizaciones.
- Impide editar versiones activas o históricas.
- Marca como **Datos de ejemplo** las cantidades, nombres de equipos, fechas y valores todavía no confirmados.
- Conserva como regla documentada la seña del 30% desde 200 carillas.
- No incorpora cuenta corriente porque continúa como propuesta exploratoria pendiente de validación.

## Interacciones demostrables

- En `CFG-001`, alternar entre vista con y sin borrador.
- Iniciar edición desde la configuración activa.
- Restaurar valores predeterminados y observar el barrido de compatibilidad.
- Elegir `Control manual` o `Control condicional` en `CFG-002`.
- Observar cómo cambia `CFG-003` según el modelo elegido.
- Simular inactividad desde el encabezado del borrador.
- Guardar y salir desde cualquier paso.
- Utilizar una versión histórica como base desde `CFG-015/016`.
- Alternar entre `Activar cambios ahora` y `Programar activación` en `CFG-012`.
- Revisar la operación en `CFG-014` y ejecutarla mediante el botón explícito `CONFIRMAR`.

## Ajustes de revisión

- `CFG-012` permite seleccionar de forma excluyente la activación inmediata o programada.
- `CFG-013` refleja la alternativa elegida en el resumen de seguridad.
- `CFG-014` ya no ofrece `Guardar y salir`: funciona como confirmación final y solo registra la operación al presionar `CONFIRMAR`.
- Después de confirmar, `CFG-014` presenta únicamente las acciones finales `Ir al inicio` y `Consultar historial`; no permite guardar ni regresar al borrador.
- El resultado final diferencia visualmente una versión activa de una programada.

## Exportación futura

Cuando el equipo apruebe esta navegación, cada paso puede exportarse como imagen PNG de `1536 × 1024` y conservar la nomenclatura `MC-ADM-CFG-001` a `MC-ADM-CFG-016`.
