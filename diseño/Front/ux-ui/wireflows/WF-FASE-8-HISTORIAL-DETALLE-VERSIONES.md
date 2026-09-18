# WF - Fase 8: Historial y detalle de versiones

**Épica:** E19 - #197  
**Issue:** #218  
**Estado:** tres mockups PNG versionados; decisión funcional consolidada; validación final de Agustín pendiente.  
**Fecha:** 17/09/2026  
**Base visual:** Fases 6 y 7 - #222 y #224.

## 1. Propósito

La Fase 8 permite consultar, auditar y comparar configuraciones activas, programadas e históricas sin modificar sus registros. También permite seleccionar una configuración elegible como base de un nuevo borrador.

**Usar como base no reactiva, edita ni reemplaza la versión seleccionada.** Crea una copia editable nueva, conserva la trazabilidad de origen e inicia nuevamente el recorrido guiado desde la Fase 2.

## 2. Invariantes del ciclo de vida

- Existe exactamente una configuración activa por imprenta.
- Puede existir como máximo un único cambio pendiente: un borrador o una versión programada, nunca ambos.
- Las versiones Activa, Programada e Histórica son inmutables.
- Una versión histórica conserva autor, vigencia, motivo, auditoría y valores originales.
- Una configuración Programada consume el único cambio pendiente y bloquea Usar como base.
- Servicios y precios mantienen historial comercial independiente; la versión operativa conserva solamente la referencia informativa utilizada.
- Toda consulta y reutilización requiere rol `ADMIN_ADMIN`; activar, programar o cancelar una programación mantiene la seguridad reforzada definida en Fase 7.

## 3. MC-ADM-CFG-015 - Historial de versiones

![MC-ADM-CFG-015 — Historial de versiones](https://raw.githubusercontent.com/AgusFT/La-Montana/421606090a859c5b105b74d490245deced071e68/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-015-historial-versiones.png)

La pantalla presenta:

- total y estado de versiones Activas, Programadas e Históricas;
- búsqueda por versión;
- filtro por estado;
- orden temporal;
- versión, estado, vigencia, autor, modelo y resumen;
- acceso al detalle;
- acceso al inicio y a la configuración actual.

### Comportamiento de controles

| Control | Comportamiento |
|---|---|
| Buscar versión | Filtra el listado sin cambiar estados ni datos. |
| Estado | Filtra por Activa, Programada o Histórica. |
| Orden | Reordena los resultados, inicialmente por más recientes. |
| Ver detalle | Abre `CFG-016` para la versión de la fila. |
| Volver al inicio | Regresa al inicio administrativo sin modificar la navegación del historial. |
| Ver configuración actual | Regresa a la Fase 1 y muestra la configuración activa y el único cambio pendiente, si existe. |

## 4. MC-ADM-CFG-016 - Detalle de versión

![MC-ADM-CFG-016 — Detalle de versión](https://raw.githubusercontent.com/AgusFT/La-Montana/421606090a859c5b105b74d490245deced071e68/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-016-detalle-version.png)

La pantalla es de solo lectura y muestra:

- versión y estado;
- inicio y fin de vigencia;
- autor y rol;
- fecha de activación o programación;
- motivo;
- resumen por módulo;
- referencia comercial informativa;
- línea de auditoría completa.

### Comportamiento de controles

| Control | Comportamiento |
|---|---|
| Tarjeta o flecha de un módulo | Abre el detalle de ese módulo en modo de solo lectura. |
| Volver al historial | Regresa a `CFG-015` conservando, cuando sea posible, filtros y orden. |
| Comparar con V2 | Abre `CFG-016B` usando la versión consultada como origen y la activa como destino. |
| Usar como base | Solicita crear un nuevo borrador desde la versión seleccionada. No modifica la versión consultada. |

## 5. MC-ADM-CFG-016B - Comparar versiones

![MC-ADM-CFG-016B — Comparar versiones](https://raw.githubusercontent.com/AgusFT/La-Montana/421606090a859c5b105b74d490245deced071e68/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3/MC-ADM-CFG-016B-comparar-versiones.png)

La comparación identifica secciones modificadas y sin cambios. Es informativa y nunca altera las versiones.

| Control | Comportamiento |
|---|---|
| Origen | Selecciona la versión base de la comparación. |
| Destino | Selecciona la versión contra la cual se compara. |
| Cambiar comparación | Recalcula la comparación con las selecciones actuales. |
| Volver al detalle | Regresa al detalle de la versión de origen. |
| Usar V1 como base | Ejecuta la misma operación de Usar como base sobre la versión indicada por el botón. |

Servicios y precios se comparan desde su historial comercial independiente.

## 6. Flujo confirmado de Usar como base

1. El ADMIN_ADMIN selecciona una versión elegible desde el detalle o la comparación.
2. El backend valida sesión, rol, pertenencia a la imprenta y que no exista otro cambio pendiente.
3. Si ya existe un borrador, la operación no crea otro: se informa que debe continuarse o cancelarse el existente.
4. Si existe una versión programada, la operación se bloquea: para editar debe cancelarse primero la programación mediante el flujo autorizado.
5. Si no existe cambio pendiente, el sistema copia los parámetros versionados en un **nuevo borrador** y registra la versión de origen.
6. La versión seleccionada permanece inmutable y su historial no cambia.
7. El sistema ejecuta un primer barrido de recursos dinámicos. Los datos físicos u operativos actuales no se dan por válidos solamente porque existían en la versión de origen.
8. El administrador ingresa a la **Fase 2 - Modelo operativo y aprobación**.
9. Debe recorrer y confirmar nuevamente las Fases 2 a 6:
   - Fase 2: modelo operativo y aprobación;
   - Fase 3: pagos y reglas de seña;
   - Fase 4: impresoras, capacidades, compatibilidad y asignación;
   - Fase 5: horarios, puntos y envíos;
   - Fase 6: resumen, simulación, referencia comercial y conflictos.
10. La Fase 4 vuelve a detectar y validar impresoras y demás recursos dinámicos; no se presume que continúen disponibles.
11. En la Fase 7 el administrador decide si conserva el borrador para continuar luego, lo activa inmediatamente o programa su activación.
12. Activar o programar exige la verificación de seguridad y las revalidaciones finales de Fase 7.
13. La aplicación no es retroactiva: pedidos ya creados conservan sus condiciones.

## 7. Resultado y trazabilidad

Al crear el borrador deben registrarse, como mínimo:

- identificador de la nueva versión en preparación;
- `version_origen_id`;
- usuario y rol;
- fecha y hora;
- estado inicial Borrador;
- resultado del barrido de recursos dinámicos;
- motivo `USO_COMO_BASE`.

La relación con la versión de origen debe permanecer visible en auditoría. Copiar una configuración no equivale a aprobarla: cada fase debe quedar nuevamente confirmada antes de aplicar los cambios.

## 8. Errores y bloqueos

- usuario sin permiso;
- sesión inválida;
- borrador existente;
- programación existente;
- versión inexistente o no elegible;
- cambio concurrente;
- recurso dinámico ausente o incompatible;
- fallo al crear la copia.

Ante cualquier error no se modifica la versión seleccionada y no puede quedar un borrador parcial.

## 9. Criterios de aceptación

- [x] Se distinguen versiones activa, programada e históricas.
- [x] El detalle presenta autor, rol, fechas, motivo, módulos y auditoría.
- [x] La comparación no modifica ninguna versión.
- [x] Las versiones consultadas permanecen inmutables.
- [x] Usar como base crea un único borrador nuevo.
- [x] El flujo continúa desde Fase 2 y obliga a reconfirmar los módulos.
- [x] Se revalidan recursos dinámicos y conflictos antes de Fase 7.
- [x] Se bloquea la creación si existe borrador o programación.
- [x] Servicios y precios conservan historial independiente.
- [ ] Validación final del equipo y de Agustín.

## 10. Evidencia versionada

- [Carpeta de mockups V3](https://github.com/AgusFT/La-Montana/tree/421606090a859c5b105b74d490245deced071e68/dise%C3%B1o/Front/ux-ui/vistas-web-mockups/administrador/motor-configuracion-v3)
- [Commit visual `4216060`](https://github.com/AgusFT/La-Montana/commit/421606090a859c5b105b74d490245deced071e68)
- [Fase 8 - #218](https://github.com/AgusFT/La-Montana/issues/218)
- [E19 - #197](https://github.com/AgusFT/La-Montana/issues/197)
- [PR #196](https://github.com/AgusFT/La-Montana/pull/196)
