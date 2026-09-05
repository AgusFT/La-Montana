# Actualización a revisión - Trazabilidad del producto configurable

| Campo | Valor |
|---|---|
| Versión | 2.1 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-01 |
| Propósito | Relacionar decisiones, requerimientos, historias, casos candidatos y vistas |

> Esta matriz es complementaria y provisional. No sustituye matriz-trazabilidad.md. Los identificadores PROP y CAND deben consolidarse después de la revisión de Agustín.

## 1. Criterio de estado

| Estado | Significado |
|---|---|
| Confirmado | Decisión acordada durante el análisis |
| Confirmado para revisión | Base aceptada, pendiente de integración documental |
| Requiere revisión de Agustín | No debe considerarse regla definitiva |
| Futuro | Fuera del alcance inmediato |
| Provisional | Código o estructura todavía no oficial |

## 2. Trazabilidad de decisiones principales

| Decisión | Regla propuesta | RF propuesto | Historia propuesta | Caso candidato | Vista | Estado |
|---|---|---|---|---|---|---|
| Estados internos fijos | PROP-RN-I-001 | PROP-RF-CFG-003 | PROP-HU-ADM-002 | CAND-CU-CFG-004 | WF-CFG-03, WF-CFG-10 | Confirmado |
| Avance configurable | PROP-RN-C-001 | PROP-RF-CFG-001 | PROP-HU-ADM-001 | CAND-CU-CFG-002 | WF-CFG-02 | Confirmado |
| Modelos certificados | PROP-RN-C-001 | PROP-RF-CFG-002 | PROP-HU-ADM-001 | CAND-CU-CFG-002 | WF-CFG-02 | Confirmado |
| Activación inmediata | PROP-RN-C-008 | PROP-RF-CFG-006 | PROP-HU-ADM-007 | CAND-CU-CFG-006 | WF-CFG-12 a 14 | Confirmado |
| Activación programada | PROP-RN-C-008 | PROP-RF-CFG-007 | PROP-HU-ADM-008 | CAND-CU-CFG-007 | WF-CFG-12 a 14 | Confirmado |
| Cancelación programada | PROP-RN-I-010 | PROP-RF-CFG-008 | PROP-HU-ADM-008 | CAND-CU-CFG-008 | WF-CFG-01, WF-CFG-13 | Confirmado |
| Configuración activa única | PROP-RN-I-011 | PROP-RF-CFG-014 | PROP-HU-ADM-001 | CAND-CU-CFG-001 | WF-CFG-01 | Confirmado |
| Un único cambio pendiente | PROP-RN-I-012 | PROP-RF-CFG-015 y 016 | PROP-HU-ADM-001 | CAND-CU-CFG-001 | WF-CFG-01 | Confirmado |
| Exclusión borrador-programada | PROP-RN-I-012 | PROP-RF-CFG-016 y 017 | PROP-HU-ADM-008 | CAND-CU-CFG-007 | WF-CFG-01, WF-CFG-12 a 14 | Confirmado |
| Programación inmutable | PROP-RN-I-013 | PROP-RF-CFG-017 y 018 | PROP-HU-ADM-008 | CAND-CU-CFG-007 y 008 | WF-CFG-01, WF-CFG-15 | Confirmado |
| Versiones inmutables | PROP-RN-I-007 | PROP-RF-CFG-009 | PROP-HU-ADM-009 | CAND-CU-CFG-009 | WF-CFG-15, WF-CFG-16 | Confirmado |
| No retroactividad | PROP-RN-I-008 | PROP-RF-CFG-011 | PROP-HU-CLI-001 | Caso de cotización por definir | WF-CFG-10 | Confirmado |
| Configuración al cotizar | PROP-RN-Q-001 | PROP-RF-COT-001 | PROP-HU-CLI-001 | Caso de pedido por definir | WF-COT-01 | Confirmado |
| Cinco minutos iniciales | PROP-RN-Q-003 | PROP-RF-COT-004 | PROP-HU-CLI-002 | Excepción del caso de cotización | WF-COT-01 | Confirmado |
| Sesenta segundos | PROP-RN-Q-004 | PROP-RF-COT-005 | PROP-HU-CLI-002 | Excepción del caso de cotización | WF-COT-01 | Confirmado |
| Diez minutos exactos | PROP-RN-Q-005 | PROP-RF-COT-006 | PROP-HU-CLI-002 | Excepción del caso de cotización | WF-COT-02 | Confirmado |
| Eliminación de temporales | PROP-RN-Q-007 | PROP-RF-COT-009 | PROP-HU-CLI-002 | Excepción del caso de cotización | WF-COT-02 | Confirmado |
| Pausa por administrador o empleado | PROP-RN-O-001 | PROP-RF-OPE-001 | PROP-HU-OPE-001 | CAND-CU-OPE-002 | WF-OPE-01 | Confirmado |
| Reconfirmación de contraseña | PROP-RN-O-002 | PROP-RF-OPE-002 | PROP-HU-OPE-001 | CAND-CU-OPE-002 | WF-OPE-01 | Confirmado |
| Pausa bloquea creación | PROP-RN-Q-008 | PROP-RF-COT-011 | PROP-HU-CLI-003 | CAND-CU-OPE-004 | WF-COT-03 | Confirmado |
| Cambio de fecha | PROP-RN-Q-009 | PROP-RF-COT-012 y 013 | PROP-HU-CLI-004 | CAND-CU-OPE-004 | WF-COT-04 | Confirmado |
| Módulos por plan | PROP-RN-C-006 | PROP-RF-CFG-012 | PROP-HU-ADM-005 | CAND-CU-CFG-003 | WF-CFG-08 | Confirmado para revisión |
| Cuenta corriente manual | PROP-RN-CC-001 | PROP-RF-CC-001 | PROP-HU-ADM-CC-001 | Caso financiero por definir | WF-CC-01 | Requiere revisión |
| Revisión de todo pedido a cuenta | PROP-RN-CC-003 | PROP-RF-CC-005 | PROP-HU-ADM-CC-002 | Caso financiero por definir | WF-CC-03 | Requiere revisión |
| Fecha manual de cuenta corriente | PROP-RN-CC-006 | PROP-RF-CC-007 | PROP-HU-ADM-CC-003 | Caso financiero por definir | WF-CC-04 | Requiere revisión |

## 3. Trazabilidad de seguridad

| Necesidad | RNF propuesto | Flujo afectado | Vista | Estado |
|---|---|---|---|---|
| Validación backend | PROP-RNF-SEG-001 | Todos | Todas | Confirmado |
| Autenticación reforzada | PROP-RNF-SEG-002 | Activar y programar | WF-CFG-13 | Confirmado para revisión |
| Reconfirmación de pausa | PROP-RNF-SEG-003 | Pausar y reanudar | WF-OPE-01, WF-OPE-03 | Confirmado |
| Limpiar información al cerrar | PROP-RNF-SEG-004 | Cotización | WF-COT-01, WF-COT-02 | Confirmado |
| Temporizador no reiniciable | PROP-RNF-SEG-005 | Extensión | WF-COT-02 | Confirmado |
| Servidor como fuente de tiempo | PROP-RNF-SEG-006 | Cotización | Sin dependencia visual exclusiva | Confirmado para revisión |
| Historial inmutable | PROP-RNF-AUD-004 | Configuración | WF-CFG-15, WF-CFG-16 | Confirmado |

## 4. Trazabilidad de usabilidad

| Necesidad | RNF propuesto | Vista | Resultado esperado |
|---|---|---|---|
| Flujo guiado | PROP-RNF-USA-001 | WF-CFG-01 a 14 | Configuración progresiva |
| Explicar consecuencias | PROP-RNF-USA-002 | WF-CFG-02 a 10 | Decisiones comprensibles |
| Lenguaje operativo | PROP-RNF-USA-003 | Todas | Menor dependencia de soporte |
| Errores accionables | PROP-RNF-USA-004 | WF-CFG-11 | Resolución clara |
| Vigencia visible | PROP-RNF-USA-005 | WF-CFG-01, 14, 15 | Comprensión del estado actual |

## 5. Impacto sobre documentación vigente

| Documento vigente | Punto afectado | Nueva propuesta |
|---|---|---|
| alcance-general.md | Reglas del MVP tratadas como generales | Reinterpretarlas como configuración inicial |
| motor-de-configuracion-del-sistema.md | Motor sin ciclo completo | Incorporar modelos, versiones y vigencia |
| requerimientos-funcionales.md | CFG limitado y reglas fijas | Integrar RF propuestos validados |
| requerimientos-no-funcionales.md | Configuración progresiva posterior | Tratarla como núcleo del producto |
| matriz-reglas-de-negocio.md | No contempla reglas futuras | Separar invariantes y configurables |
| historias-de-usuario.md | Administrador con configuración limitada | Incorporar historias de configuración |
| casos-de-uso.md | No existen dominios CFG u OPE | Evaluar nuevos dominios y códigos |
| WF-ADMINISTRADOR-MVP.md | Trece vistas fijas | Extender con configuración y disponibilidad |
| matriz-trazabilidad.md | Brecha de configuración | Consolidar relaciones aprobadas |

Los archivos vigentes no fueron modificados en esta rama.

## 6. Documentos creados en esta actualización

| Área | Archivo | Propósito |
|---|---|---|
| Marco | actualizacion-a-revision-producto-configurable.md | Visión y decisiones |
| Marco | actualizacion-a-revision-motor-configuracion-versionada.md | Funcionamiento del motor |
| Requerimientos | actualizacion-a-revision-reglas-y-requerimientos-configurables.md | Reglas, RF y RNF |
| Historias | actualizacion-a-revision-historias-producto-configurable.md | Necesidades de actores |
| Casos de uso | 09-configuracion-del-sistema/actualizacion-a-revision-casos-de-uso-configuracion.md | Dominio candidato CFG |
| Casos de uso | 10-disponibilidad-operativa/actualizacion-a-revision-casos-de-uso-disponibilidad.md | Dominio candidato OPE |
| Finanzas | 04-estados-y-finanzas/actualizacion-a-revision-cuenta-corriente.md | Propuesta CC |
| Wireflows | WF-ACTUALIZACION-A-REVISION-ADMINISTRADOR-PRODUCTO-CONFIGURABLE.md | Base de mockups |
| Trazabilidad | Este documento | Relaciones e impacto |

## 7. Orden recomendado de integración

1. Validar visión del producto.
2. Validar terminología de roles.
3. Validar invariantes y configurables.
4. Aprobar ciclo de versiones.
5. Aprobar pausa operativa.
6. Aprobar cotización temporal.
7. Revisar cuenta corriente por separado.
8. Asignar códigos oficiales a RF, RNF e historias.
9. Crear casos de uso individuales con plantilla.
10. Actualizar wireflows definitivos.
11. Integrar matriz oficial.
12. Replanificar implementación.

## 8. Criterio para mockups

Puede diseñarse inmediatamente:

- configuración;
- modelos certificados;
- aprobación;
- pagos;
- seña;
- impresoras;
- módulos;
- resumen;
- vigencia;
- seguridad;
- historial;
- pausa;
- reanudación;
- caducidad de cotización;
- cambio de fecha por pausa.

Cuenta corriente debe diseñarse como exploración identificada y revisarse antes de consolidarse.

## 9. Registro de cambios y justificación

| Cambio de trazabilidad | Motivo | Relación con el motor de configuración | Estado |
|---|---|---|---|
| Nuevos IDs provisionales PROP | Evitar colisión con documentación vigente | Permiten discutir el motor sin declarar integración | Provisional |
| Casos candidatos CAND | No existen dominios oficiales | Facilitan revisar responsabilidades y límites | Provisional |
| Estado explícito por decisión | No todas las propuestas tienen igual madurez | Separa base de mockups de temas pendientes | Confirmado |
| Cuenta corriente aislada | Mayor riesgo financiero | Evita que una hipótesis se mezcle con reglas aceptadas | Requiere revisión |
| Mapeo hacia wireflows | El frontend necesita iniciar diseño | Permite avanzar sin esperar toda la consolidación | Confirmado |
| Variantes CFG-001 V3 | El estado inicial no diferenciaba preparación y programación | Vincula ciclo de vida, bloqueo de edición y evidencia visual | Confirmado |
| Referencias a documentos afectados | La integración será realizada después | Facilita revisión de Agustín y su IA | Confirmado |

## 10. Justificación general

La presente matriz existe porque el motor de configuración transforma reglas que durante el MVP se consideraban fijas en decisiones operativas versionadas.

Su objetivo es conservar el origen de cada cambio y facilitar que la integración final no pierda relaciones entre negocio, seguridad, requerimientos, interacción y diseño.
