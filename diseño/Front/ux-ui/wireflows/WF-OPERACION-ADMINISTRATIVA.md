# Operación administrativa — incumbencias, navegación y evidencia

| Campo | Valor |
|---|---|
| Fecha | 2026-09-18 |
| Estado | Seis mockups aprobados y publicados; integración funcional a revisión |
| Épica | E22 #228 — M16 Evolución UX/UI del producto |
| Fuente | Decisiones de revisión UX/UI del 18/09 y seis PNG originales |
| Actores | Usuarios internos autorizados; administración según permisos backend |

## 1. Alcance y precedencia

Estas superficies representan la operación diaria posterior al motor configurable. Complementan E19 #197; no son fases nuevas del asistente. El histórico MVP permanece en `flujo de negocio principal` y E03 #43. Sus invariantes útiles se conservan; las reglas que evolucionaron deben leerse con la documentación configurable a revisión del PR #196.

El motor define condiciones; las pantallas operan pedidos concretos. Recargar papel, asignar una impresora y revisar calidad no crean versiones de configuración. Los pedidos aceptados conservan sus condiciones; la configuración vigente no debe reinterpretarlos retroactivamente. La confirmación del pedido sigue requiriendo validación backend como se define en Fase 7.

## 2. Responsabilidades

| Área | Responsabilidad | Límite |
|---|---|---|
| Pedidos | Seguimiento transversal, Activos / Histórico, filtros y detalle | Consultar no cambia estados ni autoriza producción |
| Requieren revisión | Acceso a intervención humana cuando el modelo la contempla | Su presencia depende de la configuración, no de que el contador sea mayor que cero |
| Producción | Trabajos realmente habilitados, asignación e impresión, control de calidad | Una recomendación no asigna ni imprime automáticamente en V1 |
| Entregas | Trabajos con calidad aprobada; retiro, punto y envío | Entrega física y cobro son hechos distintos; cierre exige validar condiciones |
| Servicios y precios | Catálogo comercial independiente | No es obligatorio modificar tarifas para crear otra versión del motor |
| Impresoras | Disponibilidad y recarga cotidiana | Capacidades y método se configuran en Fase 4 |

## 3. Acciones por vista

### MC-ADM-PED-001 — Estado de pedidos

| Control | Resultado esperado |
|---|---|
| Activos / Histórico | Cambia el conjunto consultado; Histórico contempla finalizados, cancelados y rechazados |
| Buscar, filtros y paginación | Acota la consulta sin modificar el pedido |
| Ver detalle | Abre el pedido seleccionado con datos y acciones permitidas por rol y estado |
| Requieren revisión | Abre la bandeja de revisión humana cuando corresponde al modelo; no aprueba pedidos |

La bandeja separa estado operativo, entrega comprometida y estado financiero. El detalle y las variantes de revisión no forman parte de estos seis PNG y requieren contraste con las reglas del motor antes de implementación.

### MC-ADM-PRO-001 — Producción

| Control | Resultado esperado |
|---|---|
| Listos para producción / En producción / Control de calidad | Cambia la bandeja dentro de Producción |
| Buscar y filtros | Acota trabajos por criterios visibles |
| Asignar impresora | Abre PRO-002 para el trabajo concreto |
| Iniciar producción | Entra al recorrido de confirmación de PRO-002; el rótulo no autoriza saltar validaciones ni asignación |
| Archivo del trabajo | Acceso al documento sujeto a autorización backend |
| Alerta de impresora | Acceso operativo al estado de la impresora; la recarga usa OPE-004 |

Solo son candidatos a producción los pedidos que ya cumplieron las condiciones de aprobación y pago aplicables. El backend valida estado y permisos; la pantalla no decide reglas por sí sola.

### MC-ADM-PRO-002 — Asignación de impresora / iniciar producción

| Control | Resultado esperado |
|---|---|
| Volver a Producción / Cancelar | Regresa sin iniciar ni confirmar asignación |
| Ver perfil / Ver detalle del pedido | Consulta información autorizada del cliente o pedido |
| Ver documento / Descargar archivo | Consulta o descarga autorizada del archivo asociado |
| Ver todas las impresoras | Amplía consulta de recursos; no modifica capacidades |
| Selección de impresora | Selección manual entre operativas y compatibles; la incompatible muestra el motivo y no puede seleccionarse |
| Asignar impresora e iniciar producción | Revalida estado, condiciones, permisos y compatibilidad en backend; si son válidos registra la asignación y autorización/inicio. Si falla, informa el motivo sin avanzar como si hubiera sido exitoso |

La disponibilidad de papel es estimada. Recomendada no significa asignada. La autorización de impresión se integra con CU-IMP-001 y el contrato #185; el agente/CUPS ejecuta trabajos autorizados, sin decidir reglas comerciales. Debe evitarse duplicar la asignación o impresión ante reintentos; el contrato técnico queda por verificar en implementación.

PRO-002 amplía el contexto de trabajo de OPE-005 (Fase 4); no introduce otro método de asignación.

### MC-ADM-PRO-003 — Bandeja de control de calidad

| Control | Resultado esperado |
|---|---|
| Control de calidad | Lista trabajos impresos pendientes de revisión humana |
| Buscar, cliente, impresora, fecha y paginación | Acota trabajos sin aprobarlos |
| Revisar | Abre PRO-004 sobre el trabajo seleccionado |
| Menú de tres puntos | Acciones adicionales pendientes de especificar; no habilitar transiciones inventadas |

No confundir impresión terminada con calidad aprobada. La bandeja conserva impresora utilizada, finalización y compromiso de entrega.

### MC-ADM-PRO-004 — Detalle de control de calidad

| Control | Resultado esperado |
|---|---|
| Ver perfil, documento, descargar, detalles de impresora | Consulta autorizada del contexto de la revisión |
| Checklist / Ver checklist completo | Permite verificar impresión, alineación, orden y acabados; catálogo definitivo a validar |
| Observaciones | Registra comentarios de revisión vinculados al trabajo |
| Cancelar / Volver | Sale sin aprobar ni enviar a reimpresión |
| Enviar a reimpresión | Registra revisión no aprobada y retorna al recorrido productivo; no libera a Entregas |
| Aprobar y pasar a entregas | Registra revisión humana favorable, usuario/fecha y habilita el trabajo en Entregas |

Quedan por precisar el tratamiento técnico de reimpresión parcial/total, consumo de papel, autorización y eventuales costos. El PNG no autoriza a cobrar automáticamente una reimpresión ni a duplicar el pedido.

### MC-ADM-ENT-001 — Entregas

| Control | Resultado esperado / límite |
|---|---|
| Listos para entregar / En curso | Cambia la bandeja logística |
| Buscar, modalidad, fecha, punto, estado y financiero | Filtra sin alterar la modalidad capturada en el pedido |
| Confirmar retiro | Abre el recorrido de confirmación; detalle, código y validaciones pendientes de cerrar |
| Iniciar traslado | Entrada al recorrido hacia punto; detalle y confirmaciones pendientes |
| Iniciar envío | Entrada al recorrido de envío; detalle y confirmaciones pendientes |
| Ver detalle | Consulta el pedido y su logística; pantalla complementaria pendiente |

Las modalidades son Retiro en local / Punto de entrega / Envío. El estado financiero se muestra por separado. Los pedidos entregados se consultan desde Histórico; aparecer allí no acredita saldo cancelado ni cierre administrativo. No inferir del botón visible que se permite entregar con saldo sin la validación correspondiente. No hay nuevas excepciones económicas aprobadas por este mockup.

## 4. Lectura de los originales y diferencias detectadas

Los PNG se preservan tal cual. Fechas, personas, impresoras, importes, conteos, tamaños de archivos y capacidades son datos ilustrativos, no parámetros certificados ni ampliaciones de alcance.

- PED/PRO-001/ENT usan logo con sol naranja; PRO-002/003/004 muestran logo monocromo y variantes de menú. Para implementación, centralizar Sidebar/Topbar y contrastar con MC-ADM-001-Dashboard y la identidad aprobada. No copiar seis layouts diferentes ni crear permisos por lo que aparece en una imagen.
- PRO-003 muestra un badge superior «Listo para producción» pese a tener seleccionada la bandeja de calidad. Es una inconsistencia gráfica: las filas pertenecen a calidad pendiente. No mapear ese badge a una transición del sistema.
- PRO-001 incluye tamaños, formatos y ejemplos de cálculo de papel que no constituyen aprobación de nuevas capacidades ni límites de upload. Aplicar catálogo, cálculo y validaciones del backend/documentación vigente.
- Los checks ilustrados en PRO-004 no equivalen a una revisión realizada: el estado persiste pendiente hasta confirmación humana válida.

Estas observaciones acompañan la evidencia sin retocarla. Cualquier corrección visual futura deberá versionarse y aprobarse por Alejandro.

## 5. Trazabilidad

| Vistas / dominio | Referencias funcionales existentes | Issues relacionados |
|---|---|---|
| PED-001 / revisión | HU-EMP-001, HU-ADM-001/004/006/008; CU-REV-001 a 004; CU-AUD-002 | #180, #181, #182, #183; motor #210 |
| PRO-001/002 | HU-EMP-002/004/005; CU-IMP-001/003/004/005; CAND-CU-OPE-006 y WF Fase 4 | #214, #185, #49 |
| PRO-003/004 | HU-ADM-004/006; HU-EMP-005; auditoría CU-AUD-001; ampliación de calidad descrita aquí | #183, #186 |
| ENT-001 | HU-EMP-006; HU-ADM-004/005; CU-CIE-001/002/003 | #212, #183, #184, #186 |
| Catálogo y seguridad | Configuración versionada, WF Fase 6/7/8; catálogo independiente | #227, #222, #224, #218, #164 |

Los códigos referidos son trazabilidad existente, no equivalencia automática con todos los detalles nuevos. Los CU/HU históricos que exigen revisión humana universal deben contrastarse con los modelos certificados de E18/E19. La consolidación oficial permanece a revisión del equipo en PR #196.

## 6. Verificación de escritorio para implementación

| Caso | Resultado a comprobar |
|---|---|
| Modelo con revisión humana, contador cero | Acceso Requieren revisión presente; bandeja vacía |
| Trabajo sin condiciones para producción | Backend rechaza el avance aunque se invoque directamente la acción |
| Impresora compatible recomendada | Operador selecciona/confirma; recomendación no inicia impresión |
| Impresora deshabilitada entre consulta y confirmación | Revalidación impide asignar/iniciar y explica el motivo |
| Calidad rechazada | Registra observación/evento y vuelve a reimpresión; no aparece liberado en Entregas |
| Calidad aprobada | Pasa a Entregas con la modalidad del pedido, sin alterar saldo |
| Pedido entregado con saldo | Entrega no equivale a cierre financiero; validar CU-CIE-002/003 |
| Activación de nueva configuración | No reescribe condiciones de pedidos ya aceptados |

Son verificaciones propuestas para el equipo; no se reportan como pruebas ejecutadas ni software validado.

## 7. Pendientes antes de considerar completo el panel

1. Diseñar detalle y confirmación/código de Entregas, traslado/punto, envío, cobro y cierre.
2. Cerrar variantes de Requieren revisión y detalle de pedido compatibles con el motor.
3. Unificar sidebar, logo y navegación durante implementación respetando las referencias aprobadas.
4. Validar contrato de inicio/reintento/reimpresión, permisos, auditoría y consumo; no inventar consecuencias económicas.
5. Relevar código reutilizable y pendientes de E06/E07; registrar commits y pruebas del nuevo alcance en una épica de implementación diferenciada cuando se refine.

## 8. Evidencia

[Galería de los seis PNG originales](../vistas-web-mockups/administrador/operacion-administrativa/README.md).

Complementos existentes: [Fase 4](WF-FASE-4-IMPRESORAS-ACCESOS-OPERATIVOS.md), [Fase 7](WF-FASE-7-ACTIVACION-SEGURIDAD-CONFIRMACION.md), [Fase 8](WF-FASE-8-HISTORIAL-DETALLE-VERSIONES.md).
