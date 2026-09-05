# Actualización a revisión - Reglas y requerimientos configurables

| Campo | Valor |
|---|---|
| Versión | 2.1 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-01 |
| Alcance | Producto definido |
| Identificadores | Provisionales, no oficiales |

> Los códigos PROP utilizados en este documento sirven únicamente para facilitar la revisión. No deben incorporarse como identificadores oficiales hasta validar duplicados, dependencias y trazabilidad.

## 1. Objetivo

Proponer reglas de negocio, requerimientos funcionales y requerimientos no funcionales derivados de la incorporación del motor de configuración.

## 2. Clasificación de reglas

### 2.1 Reglas invariables

| Código provisional | Regla |
|---|---|
| PROP-RN-I-001 | Los estados internos y el recorrido general del pedido son fijos |
| PROP-RN-I-002 | El avance entre estados siempre se valida en backend |
| PROP-RN-I-003 | Ningún cliente puede ejecutar impresión directamente |
| PROP-RN-I-004 | Solo se imprimen trabajos autorizados |
| PROP-RN-I-005 | Toda acción sensible requiere autenticación y autorización |
| PROP-RN-I-006 | Los datos de cada imprenta permanecen aislados |
| PROP-RN-I-007 | Las configuraciones activadas son inmutables |
| PROP-RN-I-008 | Los cambios no son retroactivos |
| PROP-RN-I-009 | La versión aplicable se captura al cotizar |
| PROP-RN-I-010 | Los eventos críticos quedan auditados |
| PROP-RN-I-011 | Cada imprenta mantiene exactamente una configuración activa |
| PROP-RN-I-012 | Cada imprenta mantiene como máximo un cambio pendiente: un borrador en preparación o una versión programada, nunca ambos |
| PROP-RN-I-013 | Una versión programada es inmutable hasta activarse o cancelarse |

### 2.2 Reglas configurables

| Código provisional | Regla |
|---|---|
| PROP-RN-C-001 | La imprenta selecciona un modelo de aprobación certificado |
| PROP-RN-C-002 | La imprenta configura medios y momentos de pago compatibles |
| PROP-RN-C-003 | La imprenta configura reglas de seña dentro de parámetros permitidos |
| PROP-RN-C-004 | La imprenta registra impresoras y capacidades |
| PROP-RN-C-005 | La imprenta selecciona asignación manual o automática cuando esté disponible |
| PROP-RN-C-006 | La imprenta activa módulos incluidos en su plan |
| PROP-RN-C-007 | La imprenta configura puntos y modalidades de entrega |
| PROP-RN-C-008 | La imprenta activa cambios inmediatamente o los programa |

### 2.3 Reglas operativas

| Código provisional | Regla |
|---|---|
| PROP-RN-O-001 | ADMIN_ADMIN o empleado pueden pausar la recepción |
| PROP-RN-O-002 | Pausar y reanudar requieren reconfirmación de contraseña |
| PROP-RN-O-003 | La pausa bloquea cotizaciones y confirmaciones nuevas |
| PROP-RN-O-004 | La pausa no elimina ni reconfigura pedidos existentes |
| PROP-RN-O-005 | Toda pausa y reanudación queda auditada |

### 2.4 Reglas de cotización

| Código provisional | Regla |
|---|---|
| PROP-RN-Q-001 | La configuración se captura al presionar Cotizar pedido |
| PROP-RN-Q-002 | Una configuración posterior no altera la cotización vigente |
| PROP-RN-Q-003 | La cotización tiene cinco minutos iniciales |
| PROP-RN-Q-004 | Después se concede hasta un minuto para confirmar actividad |
| PROP-RN-Q-005 | La respuesta afirmativa concede diez minutos exactos adicionales |
| PROP-RN-Q-006 | La extensión no se reinicia |
| PROP-RN-Q-007 | La expiración anula la cotización y elimina temporales |
| PROP-RN-Q-008 | Una pausa impide crear el pedido aunque la cotización sea válida |
| PROP-RN-Q-009 | Al reanudar se informa y acepta cualquier cambio de fecha de entrega |

### 2.5 Cuenta corriente - Requiere revisión

| Código provisional | Regla propuesta |
|---|---|
| PROP-RN-CC-001 | Solo ADMIN_ADMIN habilita, suspende o deshabilita una cuenta corriente |
| PROP-RN-CC-002 | La cuenta puede configurarse con límite o sin límite |
| PROP-RN-CC-003 | Todo pedido a cuenta requiere revisión manual |
| PROP-RN-CC-004 | Enviar el pedido sin pagar no autoriza producción |
| PROP-RN-CC-005 | El administrador visualiza deuda actual y proyectada |
| PROP-RN-CC-006 | El administrador define manualmente la fecha estimada de entrega |
| PROP-RN-CC-007 | La conducta al superar el límite queda pendiente de validación |

## 3. Requerimientos funcionales propuestos

### 3.1 Configuración

- PROP-RF-CFG-001: El sistema debe permitir al ADMIN_ADMIN seleccionar un modelo operativo certificado.
- PROP-RF-CFG-002: El sistema debe mostrar solamente parámetros compatibles con el modelo seleccionado.
- PROP-RF-CFG-003: El sistema debe validar coherencia antes de permitir una activación.
- PROP-RF-CFG-004: El sistema debe permitir guardar una configuración en preparación.
- PROP-RF-CFG-005: El sistema debe mostrar un resumen de diferencias.
- PROP-RF-CFG-006: El sistema debe permitir activación inmediata.
- PROP-RF-CFG-007: El sistema debe permitir programación por fecha y hora.
- PROP-RF-CFG-008: El sistema debe permitir cancelar una programación no activada.
- PROP-RF-CFG-009: El sistema debe crear una versión inmutable por cada activación.
- PROP-RF-CFG-010: El sistema debe conservar historial de versiones.
- PROP-RF-CFG-011: El sistema debe asociar cada cotización con la versión utilizada.
- PROP-RF-CFG-012: El sistema debe limitar módulos activables a los incluidos en el plan.
- PROP-RF-CFG-013: El sistema debe exigir protocolo de seguridad para cambios sensibles.
- PROP-RF-CFG-014: El sistema debe garantizar que exista exactamente una configuración activa por imprenta.
- PROP-RF-CFG-015: El sistema debe limitar a una la configuración en preparación por imprenta.
- PROP-RF-CFG-016: El sistema debe impedir crear o continuar un borrador mientras exista una versión programada.
- PROP-RF-CFG-017: Al confirmar una activación futura, el sistema debe cerrar el borrador y registrar una versión programada inmutable.
- PROP-RF-CFG-018: Al cancelar una programación, el sistema debe liberar la creación de un nuevo borrador sin eliminar la trazabilidad de la versión cancelada.

### 3.2 Pausa operativa

- PROP-RF-OPE-001: El sistema debe permitir pausar la recepción a ADMIN_ADMIN y empleados autenticados.
- PROP-RF-OPE-002: El sistema debe solicitar reconfirmación de contraseña.
- PROP-RF-OPE-003: El sistema debe solicitar un motivo.
- PROP-RF-OPE-004: El sistema debe bloquear nuevas cotizaciones durante la pausa.
- PROP-RF-OPE-005: El sistema debe bloquear confirmaciones de pedidos durante la pausa.
- PROP-RF-OPE-006: El sistema debe mostrar el estado de pausa en las vistas internas.
- PROP-RF-OPE-007: El sistema debe comunicar la pausa al cliente.
- PROP-RF-OPE-008: El sistema debe permitir reanudar mediante reconfirmación.
- PROP-RF-OPE-009: El sistema debe registrar pausa y reanudación.

### 3.3 Cotización y sesión

- PROP-RF-COT-001: El backend debe consultar la configuración activa al cotizar.
- PROP-RF-COT-002: El sistema debe generar una cotización temporal identificable.
- PROP-RF-COT-003: El sistema debe mostrar medios de pago y entrega capturados.
- PROP-RF-COT-004: El sistema debe controlar cinco minutos iniciales.
- PROP-RF-COT-005: El sistema debe mostrar una consulta de actividad con sesenta segundos.
- PROP-RF-COT-006: El sistema debe otorgar diez minutos exactos al confirmar actividad.
- PROP-RF-COT-007: El sistema debe mostrar una cuenta regresiva.
- PROP-RF-COT-008: El sistema debe anular la cotización al expirar.
- PROP-RF-COT-009: El sistema debe eliminar archivos y datos temporales asociados.
- PROP-RF-COT-010: El sistema debe cerrar la sesión al finalizar la extensión.
- PROP-RF-COT-011: El backend debe volver a validar la pausa al crear el pedido.
- PROP-RF-COT-012: Al reanudar, el sistema debe recalcular la fecha estimada.
- PROP-RF-COT-013: Si la fecha cambia, debe solicitar aceptación expresa.

### 3.4 Cuenta corriente - Requiere revisión

- PROP-RF-CC-001: El sistema debe permitir al ADMIN_ADMIN habilitar una cuenta corriente.
- PROP-RF-CC-002: El sistema debe permitir límite monetario o modalidad sin límite.
- PROP-RF-CC-003: El sistema debe mostrar deuda actual y deuda proyectada.
- PROP-RF-CC-004: El sistema debe permitir enviar un pedido sin pago previo.
- PROP-RF-CC-005: El sistema debe bloquear producción hasta revisión manual.
- PROP-RF-CC-006: El sistema debe mostrar carga operativa y compromisos existentes.
- PROP-RF-CC-007: El sistema debe permitir asignar manualmente la fecha estimada.
- PROP-RF-CC-008: El sistema debe registrar decisión y observaciones.
- PROP-RF-CC-009: El sistema debe permitir suspender o deshabilitar la cuenta.

## 4. Requerimientos no funcionales propuestos

### 4.1 Seguridad

- PROP-RNF-SEG-001: Las acciones sensibles deben validarse en backend.
- PROP-RNF-SEG-002: La activación de configuración debe requerir autenticación reforzada.
- PROP-RNF-SEG-003: Pausar y reanudar deben requerir reconfirmación.
- PROP-RNF-SEG-004: La información económica no debe permanecer visible tras cerrar sesión.
- PROP-RNF-SEG-005: La extensión de cotización no debe reiniciarse desde el cliente.
- PROP-RNF-SEG-006: El servidor debe ser la fuente de verdad de los temporizadores críticos.

### 4.2 Auditoría

- PROP-RNF-AUD-001: Cada versión debe registrar autor, momento y diferencias.
- PROP-RNF-AUD-002: Pausas, reanudaciones y cancelaciones programadas deben auditarse.
- PROP-RNF-AUD-003: Las decisiones sobre cuentas corrientes deben conservar trazabilidad.
- PROP-RNF-AUD-004: Los registros históricos no deben eliminarse por cambios posteriores.

### 4.3 Usabilidad

- PROP-RNF-USA-001: La configuración debe implementarse como flujo guiado.
- PROP-RNF-USA-002: Cada elección debe explicar consecuencias.
- PROP-RNF-USA-003: El resumen debe utilizar lenguaje operativo, no técnico.
- PROP-RNF-USA-004: Los errores deben indicar cómo resolver el conflicto.
- PROP-RNF-USA-005: El estado activo y la fecha de vigencia deben ser visibles.

### 4.4 Integridad y rendimiento

- PROP-RNF-INT-001: La creación del pedido debe ser idempotente.
- PROP-RNF-INT-002: Una cotización expirada no puede reutilizarse.
- PROP-RNF-INT-003: La activación y captura de versión deben ser consistentes.
- PROP-RNF-INT-004: La eliminación de temporales debe ejecutarse aun ante cierre inesperado.
- PROP-RNF-INT-005: La pausa debe propagarse a los puntos de entrada en tiempo adecuado.

## 5. Criterios de aceptación transversales

- Ninguna configuración permite desactivar invariantes.
- Una nueva versión afecta solamente cotizaciones posteriores.
- Una cotización conserva su versión mientras permanece vigente.
- Una pausa bloquea la creación aunque la interfaz todavía muestre una cotización.
- Los temporizadores críticos no dependen exclusivamente del frontend.
- Los módulos activos pertenecen al plan contratado.
- Toda acción sensible queda auditada.
- Existe exactamente una configuración activa por imprenta.
- Existe como máximo un cambio pendiente: un borrador o una versión programada.
- Una versión programada no puede editarse ni convivir con un borrador.
- Las decisiones de cuenta corriente no autorizan producción automáticamente.

## 6. Reglas anteriores que requieren reinterpretación

| Regla anterior | Reinterpretación propuesta |
|---|---|
| Revisión administrativa siempre obligatoria | Política inicial o perfil certificado, no única posibilidad |
| Nunca existe aprobación automática | Puede existir dentro de condiciones certificadas |
| Seña fija de 30 % desde 200 carillas | Regla histórica o predeterminada configurable |
| Flujo único de avance | Estados fijos con condiciones de avance configurables |
| Configuración como mejora posterior | Motor central del producto definido |
| Asignación manual de impresora | Modo inicial, con automatización controlada posterior |

## 7. Registro de cambios y justificación

| Incorporación | Motivo | Relación con el motor | Estado |
|---|---|---|---|
| Reglas invariables y configurables separadas | Evitar que seguridad y operación se mezclen | Define límites del motor | Confirmado |
| RF de versionado y programación | Aplicación temporal consistente | Permite vigencia inmediata o futura | Confirmado |
| Cardinalidad del cambio pendiente | Evitar edición y programación simultáneas | Garantiza una única base futura y elimina ambigüedades | Confirmado |
| Inmutabilidad de la programación | Separar preparación de una decisión confirmada | Bloquea cambios posteriores al protocolo de seguridad | Confirmado |
| RF de cotización temporal | Proteger condiciones y recursos | Vincula la cotización con una versión | Confirmado |
| RF de pausa | Resolver emergencias operativas | Actúa por encima de la configuración | Confirmado |
| RNF de UX guiada | Facilitar instalación remota | El motor debe ser comprensible sin asistencia presencial | Confirmado |
| Cuenta corriente manual | Limitar riesgo de deuda y recursos | Excepción financiera controlada | Requiere revisión de Agustín |
| Identificadores PROP | Evitar colisiones con códigos existentes | Facilita revisión antes de integración | Provisional |

## 8. Referencias para integración

La propuesta se justifica por la evolución del motor de configuración y debe revisarse contra:

- requerimientos-funcionales.md
- requerimientos-no-funcionales.md
- matriz-reglas-de-negocio.md
- motor-de-configuracion-del-sistema.md
- matriz-trazabilidad.md

Los códigos definitivos deberán asignarse únicamente durante la integración oficial.
