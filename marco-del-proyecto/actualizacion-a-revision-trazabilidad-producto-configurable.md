# Actualización a revisión - Trazabilidad del producto configurable

| Campo | Valor |
|---|---|
| Versión | 2.4 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-08 |
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
| Control manual obligatorio | PROP-RN-C-009 | PROP-RF-CFG-020 | PROP-HU-ADM-001 | CAND-CU-CFG-002 | WF-CFG-02 | Confirmado |
| Control condicional certificado | PROP-RN-C-009 y 010 | PROP-RF-CFG-021 | PROP-HU-ADM-002 | CAND-CU-CFG-003 | WF-CFG-02 y 03 | Confirmado |
| Pago previo antes de carga | PROP-RN-I-014 y PROP-RN-C-012 | PROP-RF-CFG-022 y 026 | PROP-HU-ADM-002 y 003 | CAND-CU-CFG-003 | WF-CFG-03 a 05 | Confirmado |
| Seña antes de carga | PROP-RN-I-014 y PROP-RN-C-013 | PROP-RF-CFG-023 y 027 | PROP-HU-ADM-002 y 003 | CAND-CU-CFG-003 | WF-CFG-03 a 05 | Confirmado |
| Medio acreditable para condición previa | PROP-RN-I-015 | PROP-RF-CFG-025, 026, 029 | PROP-HU-ADM-003 | CAND-CU-CFG-003 y 004 | WF-CFG-04, WF-CFG-05, WF-CFG-11 | Confirmado para revisión |
| Umbral monetario autoaprobable | PROP-RN-C-014 | PROP-RF-CFG-024 y 028 | PROP-HU-ADM-002 y 003 | CAND-CU-CFG-003 | WF-CFG-03 a 05 | Confirmado para revisión |
| Umbral con seña escalonada | PROP-RN-C-015 y 016 | PROP-RF-CFG-024, 028, 029 y 030 | PROP-HU-ADM-003 | CAND-CU-CFG-003 | WF-CFG-04 y 05 | Confirmado para revisión |
| Matriz financiera heredada de Fase 2 | PROP-RN-C-011 a 016 | PROP-RF-CFG-025 a 031 | PROP-HU-ADM-003 | CAND-CU-CFG-003 y 004 | WF-CFG-04, WF-CFG-05, WF-CFG-11 | Confirmado para revisión |
| Estados Operativa/Deshabilitada de impresora | PROP-RN-I-016 a 018 | PROP-RF-CFG-034 a 036 | PROP-HU-ADM-004 | CAND-CU-CFG-012 | WF-CFG-06 | Confirmado |
| Identidad y capacidades de impresora | PROP-RN-I-019 y PROP-RN-C-004 | PROP-RF-CFG-032 y 033 | PROP-HU-ADM-004 | CAND-CU-CFG-011 | WF-CFG-06 | Confirmado |
| Edición solo deshabilitada y eliminación desde edición | PROP-RN-I-017 a 019 | PROP-RF-CFG-035 y 036 | PROP-HU-ADM-004 | CAND-CU-CFG-012 | WF-CFG-06 | Confirmado |
| Compatibilidad automática por trabajo | PROP-RN-C-004 y 018 | PROP-RF-CFG-037 y 043 | PROP-HU-OPE-003 | CAND-CU-OPE-006 | WF-CFG-07, WF-OPE-05 | Confirmado |
| Asignación manual predeterminada V1 | PROP-RN-C-005 y 018 | PROP-RF-CFG-038 y 043 | PROP-HU-ADM-010 y PROP-HU-OPE-003 | CAND-CU-CFG-013 y CAND-CU-OPE-006 | WF-CFG-07, WF-OPE-05 | Confirmado |
| Asignación automática certificada | PROP-RN-C-005 | PROP-RF-CFG-038 | PROP-HU-ADM-010 | CAND-CU-CFG-013 | WF-CFG-07 | Futuro |
| Capacidad máxima de papel configurada | PROP-RN-C-017 | PROP-RF-CFG-033, 039 y 040 | PROP-HU-ADM-004 | CAND-CU-CFG-011 | WF-CFG-06 | Confirmado |
| Disponibilidad estimada de papel | PROP-RN-O-006 | PROP-RF-CFG-039 a 041 | PROP-HU-ADM-004 y PROP-HU-OPE-003 | CAND-CU-CFG-011 y CAND-CU-OPE-006 | WF-CFG-06, WF-OPE-05 | Confirmado |
| Recarga manual al 100 % | PROP-RN-O-007 y 008 | PROP-RF-CFG-042 y PROP-RF-OPE-010 a 012 | PROP-HU-OPE-004 | CAND-CU-OPE-005 | WF-OPE-04 | Confirmado |
| Contador histórico independiente de recarga | PROP-RN-O-008 | PROP-RF-CFG-039 y PROP-RF-OPE-012 | PROP-HU-ADM-004 y PROP-HU-OPE-004 | CAND-CU-OPE-005 | WF-CFG-06, WF-OPE-04 | Confirmado |
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
| Auditoría de recarga | PROP-RNF-AUD-005 | Recarga de papel | WF-OPE-04 | Confirmado |

## 4. Trazabilidad de usabilidad

| Necesidad | RNF propuesto | Vista | Resultado esperado |
|---|---|---|---|
| Flujo guiado | PROP-RNF-USA-001 | WF-CFG-01 a 14 | Configuración progresiva |
| Explicar consecuencias | PROP-RNF-USA-002 | WF-CFG-02 a 10 | Decisiones comprensibles |
| Lenguaje operativo | PROP-RNF-USA-003 | Todas | Menor dependencia de soporte |
| Errores accionables | PROP-RNF-USA-004 | WF-CFG-11 | Resolución clara |
| Vigencia visible | PROP-RNF-USA-005 | WF-CFG-01, 14, 15 | Comprensión del estado actual |
| Herencia visible de Fase 2 | PROP-RNF-USA-006 | WF-CFG-04 y 05 | Diferenciar parámetros bloqueados de valores editables |
| Papel identificado como estimado | PROP-RNF-USA-007 | WF-CFG-06, WF-OPE-04, WF-OPE-05 | Evitar confundir cálculo del sistema con lectura de sensor |
| Causa de incompatibilidad visible | PROP-RNF-USA-008 | WF-CFG-07, WF-OPE-05 | Explicar por qué una impresora no puede seleccionarse |

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
- recarga de papel;
- selección manual de impresora compatible;
- caducidad de cotización;
- cambio de fecha por pausa.

Para Fase 3, los mockups deben representar explícitamente la herencia de Fase 2, los medios acreditables y la regla escalonada por monto.

Para Fase 4, los mockups deben representar como mínimo:

- identificación y capacidades de impresora;
- estados Operativa y Deshabilitada;
- edición disponible solamente en Deshabilitada;
- eliminación únicamente dentro de Editar;
- formatos y capacidad B/N/color;
- contador histórico de hojas impresas;
- disponibilidad estimada en hojas y porcentaje;
- recarga manual que completa físicamente el faltante y restablece el estimado a 100 %;
- compatibilidad automática del trabajo con equipos;
- recomendación sin asignación automática;
- Asignación manual como opción predeterminada de V1;
- Asignación automática visible pero deshabilitada.

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
| Matriz financiera Fase 3 | Faltaba vincular Fase 2 con medios y seña | Evita combinaciones incompatibles y mantiene trazabilidad de la decisión | Confirmado para revisión |
| Umbral con seña escalonada | La definición previa derivaba a revisión humana | Alinea reglas, RF, HU, CU y wireflow con la modalidad flexible acordada | Confirmado para revisión |
| Ciclo de impresoras Fase 4 | Los estados y acciones estaban implícitos | Alinea RN, RF, HU, CU y WF con Operativa/Deshabilitada, edición y eliminación | Confirmado |
| Compatibilidad y asignación manual Fase 4 | No se definía el criterio operativo | Permite recomendar equipos compatibles sin automatizar la decisión en V1 | Confirmado |
| Papel estimado y recarga | No existía trazabilidad del insumo para impresión remota | Separa capacidad configurada, consumo, recarga y contador histórico | Confirmado |
| Automática fuera de V1 | La opción futura podía interpretarse como disponible | Queda visible pero bloqueada hasta contar con un modelo certificado | Futuro |
| Referencias a documentos afectados | La integración será realizada después | Facilita revisión de Agustín y su IA | Confirmado |

## 10. Justificación general

La presente matriz existe porque el motor de configuración transforma reglas que durante el MVP se consideraban fijas en decisiones operativas versionadas.

Su objetivo es conservar el origen de cada cambio y facilitar que la integración final no pierda relaciones entre negocio, seguridad, requerimientos, interacción y diseño.