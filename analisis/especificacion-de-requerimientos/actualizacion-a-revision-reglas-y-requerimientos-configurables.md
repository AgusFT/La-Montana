# Actualización a revisión - Reglas y requerimientos configurables

| Campo | Valor |
|---|---|
| Versión | 2.5 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-10 |
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
| PROP-RN-I-014 | Si el modelo exige pago total o seña previa, el archivo no se almacena ni procesa en el servidor hasta acreditar la condición |
| PROP-RN-I-015 | Una condición económica previa solo puede considerarse satisfecha mediante un medio de pago acreditable por el sistema o por un operador autorizado |
| PROP-RN-I-016 | Una impresora configurada posee únicamente los estados Operativa y Deshabilitada |
| PROP-RN-I-017 | Una impresora Operativa puede recibir trabajos y no puede editarse ni eliminarse; una impresora Deshabilitada no recibe trabajos y puede editarse |
| PROP-RN-I-018 | La eliminación solo está disponible dentro de la edición de una impresora Deshabilitada |
| PROP-RN-I-019 | La identidad técnica de una impresora permanece estable aunque cambie su nombre visible, y su eliminación no debe romper referencias históricas de trabajos procesados |

### 2.2 Reglas configurables

| Código provisional | Regla |
|---|---|
| PROP-RN-C-001 | La imprenta selecciona un modelo de aprobación certificado |
| PROP-RN-C-002 | La imprenta configura medios y momentos de pago compatibles |
| PROP-RN-C-003 | La imprenta configura reglas de seña dentro de parámetros permitidos |
| PROP-RN-C-004 | La imprenta registra cada impresora con identificación visible, capacidades de formato, capacidad B/N o color, dúplex cuando corresponda y capacidad máxima de hojas |
| PROP-RN-C-005 | En V1 la asignación de impresora es manual y predeterminada; la asignación automática permanece visible como evolución futura deshabilitada hasta disponer de un modelo certificado |
| PROP-RN-C-006 | La futura gestión de módulos permitirá activar o desactivar capacidades opcionales mediante un estado activo/inactivo, sin impedir el flujo básico del negocio; el catálogo de módulos y su distribución entre planes quedan pendientes |
| PROP-RN-C-007 | La imprenta configura modalidades de entrega, puntos de entrega, horarios operativos y tiempos estimados aplicables |
| PROP-RN-C-008 | La imprenta activa cambios inmediatamente o los programa |
| PROP-RN-C-009 | La imprenta selecciona Control manual o Control condicional entre los modelos habilitados |
| PROP-RN-C-010 | El Control condicional utiliza únicamente pago previo, pago de seña o monto total del pedido como condiciones certificadas iniciales |
| PROP-RN-C-011 | En Control manual, la seña es una regla financiera opcional y no sustituye la decisión humana de aprobación |
| PROP-RN-C-012 | En aprobación por pago previo, el efectivo no puede utilizarse para satisfacer la condición previa y la seña no aplica |
| PROP-RN-C-013 | En aprobación por pago de seña, la condición viene fijada por Fase 2; Fase 3 configura tipo y valor de la seña, pero no puede desactivar esa condición |
| PROP-RN-C-014 | En aprobación por monto, el ADMIN_ADMIN configura un umbral monetario; hasta ese umbral el pedido puede aprobarse automáticamente y utilizar medios flexibles, incluido efectivo |
| PROP-RN-C-015 | En aprobación por monto, superar el umbral exige una seña previa configurable antes de continuar; la seña debe acreditarse mediante transferencia, pago digital u otro medio acreditable |
| PROP-RN-C-016 | En aprobación por monto, el saldo restante luego de acreditar la seña puede abonarse mediante cualquiera de los medios generales habilitados, incluido efectivo cuando corresponda |
| PROP-RN-C-017 | La capacidad máxima de hojas de cada impresora se carga manualmente al crearla o editarla mientras está Deshabilitada |
| PROP-RN-C-018 | El sistema puede recomendar una impresora entre las compatibles utilizando las características del trabajo y la disponibilidad estimada de papel, sin realizar asignación automática en V1 |
| PROP-RN-C-019 | La imprenta configura por separado cada día de la semana con estado habilitado, hora de apertura y hora de cierre |
| PROP-RN-C-020 | El tiempo estimado de preparación en Casa Central se configura en horas y representa el compromiso operativo para dejar un pedido listo |
| PROP-RN-C-021 | El tiempo estimado para preparar o liberar un pedido para envío se configura en horas y representa un compromiso estimado de la imprenta, no el tiempo garantizado de transporte externo |
| PROP-RN-C-022 | Cada punto de entrega puede registrar nombre, ubicación, días y franjas horarias de atención, además de los datos operativos necesarios para ofrecerlo al cliente |
| PROP-RN-C-023 | Los tiempos estimados se calculan únicamente dentro de los horarios operativos configurados |
| PROP-RN-C-024 | Un pedido recibido fuera del horario operativo puede aceptarse y permanecer en cola; el cómputo del tiempo estimado comienza en la próxima apertura comercial |
| PROP-RN-C-025 | Las franjas horarias de los puntos expresan ventanas estimadas de disponibilidad y no obligan a fijar una hora exacta de encuentro |

### 2.3 Reglas operativas

| Código provisional | Regla |
|---|---|
| PROP-RN-O-001 | ADMIN_ADMIN o empleado pueden pausar la recepción |
| PROP-RN-O-002 | Pausar y reanudar requieren reconfirmación de contraseña |
| PROP-RN-O-003 | La pausa bloquea cotizaciones y confirmaciones nuevas |
| PROP-RN-O-004 | La pausa no elimina ni reconfigura pedidos existentes |
| PROP-RN-O-005 | Toda pausa y reanudación queda auditada |
| PROP-RN-O-006 | La disponibilidad de papel es una estimación calculada desde la capacidad configurada, el consumo de trabajos y los eventos manuales de recarga |
| PROP-RN-O-007 | Registrar una recarga presupone que el operador completó físicamente la impresora hasta su capacidad máxima configurada; el evento restablece la disponibilidad estimada al 100 % |
| PROP-RN-O-008 | Una recarga no reinicia el contador histórico de hojas impresas |
| PROP-RN-O-009 | Un punto de entrega puede habilitarse o deshabilitarse de forma operativa e inmediata sin crear una nueva versión de configuración y sin solicitar motivo |
| PROP-RN-O-010 | Un punto deshabilitado deja de ofrecerse para nuevos pedidos hasta que vuelva a habilitarse |
| PROP-RN-O-011 | Deshabilitar un punto no modifica automáticamente las entregas ya pactadas; esos pedidos conservan el punto acordado y pueden continuar con demora si la contingencia lo exige |
| PROP-RN-O-012 | Mientras exista uno o más puntos deshabilitados, el dashboard debe mantener un aviso visible para evitar que la situación operativa quede olvidada |
| PROP-RN-O-013 | Si un pedido queda listo antes del tiempo estimado, el flujo debe avanzar inmediatamente y el cliente debe visualizar el nuevo estado sin esperar al horario estimado original |

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
- PROP-RF-CFG-012: [Futuro] La gestión de módulos deberá respetar el plan contratado y no podrá convertir una mejora opcional en requisito para completar el flujo básico del negocio.
- PROP-RF-CFG-013: El sistema debe exigir protocolo de seguridad para cambios sensibles.
- PROP-RF-CFG-014: El sistema debe garantizar que exista exactamente una configuración activa por imprenta.
- PROP-RF-CFG-015: El sistema debe limitar a una la configuración en preparación por imprenta.
- PROP-RF-CFG-016: El sistema debe impedir crear o continuar un borrador mientras exista una versión programada.
- PROP-RF-CFG-017: Al confirmar una activación futura, el sistema debe cerrar el borrador y registrar una versión programada inmutable.
- PROP-RF-CFG-018: Al cancelar una programación, el sistema debe liberar la creación de un nuevo borrador sin eliminar la trazabilidad de la versión cancelada.
- PROP-RF-CFG-019: El sistema debe presentar Control manual y Control condicional dentro de una única vista de selección del modelo operativo.
- PROP-RF-CFG-020: En Control manual, el sistema debe exigir revisión y aprobación o rechazo humano para todos los pedidos.
- PROP-RF-CFG-021: En Control condicional, el sistema debe permitir seleccionar únicamente una condición certificada compatible.
- PROP-RF-CFG-022: En aprobación por pago previo, el sistema debe impedir la carga, almacenamiento y procesamiento del archivo hasta acreditar el pago total.
- PROP-RF-CFG-023: En aprobación por seña, el sistema debe impedir la carga, almacenamiento y procesamiento del archivo hasta acreditar la seña configurada.
- PROP-RF-CFG-024: En aprobación por monto, el sistema debe permitir aprobación automática hasta el umbral monetario configurado y exigir una seña previa configurable cuando el pedido supere ese umbral.
- PROP-RF-CFG-025: En Fase 3, el sistema debe mostrar los medios de pago compatibles con el modelo heredado de Fase 2 y mantener visibles pero deshabilitadas las combinaciones incompatibles.
- PROP-RF-CFG-026: En aprobación por pago previo, el sistema debe deshabilitar efectivo como medio para satisfacer la condición y debe impedir configurar una seña adicional.
- PROP-RF-CFG-027: En aprobación por seña, el sistema debe bloquear la condición heredada y permitir configurar solamente el tipo y el valor de la seña dentro de parámetros permitidos.
- PROP-RF-CFG-028: En aprobación por monto, el sistema debe permitir configurar el valor del umbral y el tipo/valor de la seña exigida para pedidos superiores.
- PROP-RF-CFG-029: En aprobación por monto, la acreditación de la seña para pedidos superiores debe limitarse a transferencia, pago digital u otros medios acreditables; efectivo no debe utilizarse para acreditar la seña previa.
- PROP-RF-CFG-030: En aprobación por monto, el sistema debe permitir que el saldo restante después de la seña utilice los medios generales habilitados, incluido efectivo cuando corresponda.
- PROP-RF-CFG-031: La interfaz de Fase 3 debe mostrar una simulación del flujo resultante que conecte la configuración heredada de Fase 1 y Fase 2 con las decisiones financieras de Fase 3.
- PROP-RF-CFG-032: El sistema debe registrar una identidad técnica estable y un nombre visible editable para cada impresora.
- PROP-RF-CFG-033: El sistema debe permitir configurar formatos admitidos, capacidad B/N o color, dúplex cuando corresponda y capacidad máxima de hojas.
- PROP-RF-CFG-034: El sistema debe manejar solamente los estados Operativa y Deshabilitada para las impresoras de esta versión.
- PROP-RF-CFG-035: El sistema debe impedir la edición de una impresora Operativa y habilitarla únicamente cuando la impresora se encuentre Deshabilitada.
- PROP-RF-CFG-036: La opción Eliminar impresora debe aparecer únicamente dentro de la edición de una impresora Deshabilitada y no debe romper la trazabilidad histórica.
- PROP-RF-CFG-037: El sistema debe determinar automáticamente la compatibilidad de un trabajo con las impresoras a partir del formato de hoja y de sus necesidades de B/N o color, además de otras capacidades configuradas aplicables.
- PROP-RF-CFG-038: En V1, el sistema debe utilizar asignación manual como modalidad predeterminada y puede recomendar una impresora entre las compatibles; la asignación automática debe permanecer deshabilitada.
- PROP-RF-CFG-039: El sistema debe mantener por impresora un contador histórico acumulado de hojas impresas independiente de la disponibilidad actual de papel.
- PROP-RF-CFG-040: El sistema debe calcular y mostrar la cantidad estimada de hojas disponibles y su porcentaje respecto de la capacidad máxima configurada.
- PROP-RF-CFG-041: Al registrar la ejecución de un trabajo, el sistema debe descontar de la disponibilidad estimada la cantidad de hojas calculada para ese trabajo.
- PROP-RF-CFG-042: El sistema debe ofrecer una acción manual Registrar recarga de papel que, tras confirmación del operador de que completó físicamente la impresora, restablezca la disponibilidad estimada a la capacidad máxima configurada y al 100 %.
- PROP-RF-CFG-043: La recomendación de impresora puede considerar compatibilidad y disponibilidad estimada de papel, pero la decisión final de asignación permanece manual en V1.
- PROP-RF-CFG-044: La Fase 5 debe permitir configurar cada día de la semana por separado, indicando si es operativo y sus horas de apertura y cierre.
- PROP-RF-CFG-045: La Fase 5 debe permitir configurar en horas el tiempo estimado de preparación de un pedido para retiro en Casa Central.
- PROP-RF-CFG-046: La Fase 5 debe permitir configurar en horas el tiempo estimado para preparar o liberar un pedido para envío.
- PROP-RF-CFG-047: El cálculo de tiempos estimados debe consumir únicamente horas comprendidas dentro de las ventanas operativas configuradas.
- PROP-RF-CFG-048: Si un pedido se recibe fuera de horario, el sistema debe poder aceptarlo, mostrarlo en cola y comenzar el cómputo en la próxima apertura comercial.
- PROP-RF-CFG-049: La Fase 5 debe ofrecer una acción Administrar puntos que conduzca a un panel dedicado para consultar, agregar y editar puntos de entrega.
- PROP-RF-CFG-050: Cada punto debe permitir configurar nombre, ubicación y, cuando se requiera, días y franjas horarias propias de atención o entrega.
- PROP-RF-CFG-051: El cliente debe visualizar únicamente modalidades y puntos que estén realmente disponibles en el momento aplicable.
- PROP-RF-CFG-052: La Fase 5 debe incluir una simulación del recorrido que muestre pedido recibido, fuera de horario/en cola cuando corresponda, próxima apertura, preparación, disponibilidad en Casa Central y disponibilidad en punto según franja horaria.
- PROP-RF-CFG-053: Si un pedido finaliza antes del tiempo estimado, el sistema debe permitir avanzar inmediatamente al estado operativo correspondiente y actualizar el timeline del cliente.
- PROP-RF-CFG-054: Si un punto posee franjas horarias propias, el cálculo presentado al cliente debe respetar la siguiente ventana válida del punto y no ofrecer un retiro fuera de ella.

### 3.2 Pausa y operación diaria

- PROP-RF-OPE-001: El sistema debe permitir pausar la recepción a ADMIN_ADMIN y empleados autenticados.
- PROP-RF-OPE-002: El sistema debe solicitar reconfirmación de contraseña.
- PROP-RF-OPE-003: El sistema debe solicitar un motivo.
- PROP-RF-OPE-004: El sistema debe bloquear nuevas cotizaciones durante la pausa.
- PROP-RF-OPE-005: El sistema debe bloquear confirmaciones de pedidos durante la pausa.
- PROP-RF-OPE-006: El sistema debe mostrar el estado de pausa en las vistas internas.
- PROP-RF-OPE-007: El sistema debe comunicar la pausa al cliente.
- PROP-RF-OPE-008: El sistema debe permitir reanudar mediante reconfirmación.
- PROP-RF-OPE-009: El sistema debe registrar pausa y reanudación.
- PROP-RF-OPE-010: El sistema debe permitir a un usuario interno autorizado registrar una recarga de papel de una impresora.
- PROP-RF-OPE-011: Antes de confirmar la recarga, la interfaz debe informar la cantidad estimada actual, la capacidad máxima y que la acción supone haber completado físicamente la bandeja hasta el máximo configurado.
- PROP-RF-OPE-012: La recarga debe restablecer únicamente el contador de disponibilidad actual y no el contador histórico de hojas impresas.
- PROP-RF-OPE-013: El sistema debe permitir habilitar o deshabilitar un punto de entrega de forma inmediata desde su gestión operativa, sin solicitar motivo y sin crear una nueva versión de configuración.
- PROP-RF-OPE-014: Un punto deshabilitado no debe ofrecerse como opción para nuevos pedidos hasta que vuelva a habilitarse.
- PROP-RF-OPE-015: El dashboard debe mostrar un recordatorio persistente cuando exista al menos un punto de entrega deshabilitado y permitir acceder a su gestión.
- PROP-RF-OPE-016: Deshabilitar un punto no debe reasignar ni cancelar automáticamente pedidos que ya tenían esa entrega pactada.
- PROP-RF-OPE-017: El sistema debe permitir volver a habilitar manualmente el punto cuando esté operativo nuevamente.

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
- PROP-RNF-AUD-005: Los eventos de recarga de papel deben registrar al menos impresora, usuario y momento para poder evaluar la vigencia de la estimación.
- PROP-RNF-AUD-006: Los cambios operativos de disponibilidad de puntos deben conservar al menos punto, usuario, estado anterior, estado nuevo y momento, aunque no requieran motivo.

### 4.3 Usabilidad

- PROP-RNF-USA-001: La configuración debe implementarse como flujo guiado.
- PROP-RNF-USA-002: Cada elección debe explicar consecuencias.
- PROP-RNF-USA-003: El resumen debe utilizar lenguaje operativo, no técnico.
- PROP-RNF-USA-004: Los errores deben indicar cómo resolver el conflicto.
- PROP-RNF-USA-005: El estado activo y la fecha de vigencia deben ser visibles.
- PROP-RNF-USA-006: La Fase 3 debe explicar qué decisiones provienen de Fase 2 y cuáles permanecen editables.
- PROP-RNF-USA-007: La disponibilidad de papel debe identificarse como estimada y diferenciarse del contador histórico de hojas impresas.
- PROP-RNF-USA-008: Una impresora incompatible debe explicar la causa, por ejemplo formato no admitido o ausencia de impresión color.
- PROP-RNF-USA-009: La Fase 5 debe mostrar cada día operativo de forma independiente y utilizar inputs claros para apertura, cierre y tiempos expresados en horas.
- PROP-RNF-USA-010: Las franjas de entrega deben comunicarse como ventanas estimadas y el timeline debe explicar visualmente cuándo un pedido está en cola, cuándo comienza su cómputo y cuándo queda disponible.

### 4.4 Integridad y rendimiento

- PROP-RNF-INT-001: La creación del pedido debe ser idempotente.
- PROP-RNF-INT-002: Una cotización expirada no puede reutilizarse.
- PROP-RNF-INT-003: La activación y captura de versión deben ser consistentes.
- PROP-RNF-INT-004: La eliminación de temporales debe ejecutarse aun ante cierre inesperado.
- PROP-RNF-INT-005: La pausa debe propagarse a los puntos de entrada en tiempo adecuado.
- PROP-RNF-INT-006: La disponibilidad de papel debe actualizarse de forma consistente al registrar trabajos y recargas, sin confundirse con una lectura física de sensores.
- PROP-RNF-INT-007: El cálculo de tiempos de Fase 5 debe utilizar una única fuente de verdad para calendario operativo, tiempos configurados y disponibilidad del punto.

## 5. Criterios de aceptación transversales

- Ninguna configuración permite desactivar invariantes.
- Una nueva versión afecta solamente cotizaciones posteriores.
- Una cotización conserva su versión mientras permanece vigente.
- Una pausa bloquea la creación aunque la interfaz todavía muestre una cotización.
- Los temporizadores críticos no dependen exclusivamente del frontend.
- La definición comercial de módulos y planes permanece fuera del alcance actual de Fase 5.
- Las mejoras opcionales futuras no deben impedir completar el flujo básico del negocio.
- Toda acción sensible queda auditada.
- Existe exactamente una configuración activa por imprenta.
- Existe como máximo un cambio pendiente: un borrador o una versión programada.
- Una versión programada no puede editarse ni convivir con un borrador.
- Control manual siempre exige una decisión humana.
- Control condicional utiliza únicamente condiciones certificadas por el producto.
- El archivo no llega al servidor antes de acreditar un pago previo o una seña exigida.
- En pago previo, efectivo no satisface la condición y la seña no aplica.
- En pago de seña, la condición no puede desactivarse en Fase 3; solo se parametrizan tipo y valor.
- En aprobación por monto, los pedidos hasta el umbral pueden aprobarse automáticamente y utilizar efectivo si está habilitado.
- En aprobación por monto, superar el umbral activa una seña previa configurable; no implica revisión humana automática.
- La seña de un pedido superior al umbral debe acreditarse por un medio acreditable y el saldo puede conservar medios flexibles.
- Las impresoras solo tienen estados Operativa o Deshabilitada.
- Una impresora Operativa no puede editarse ni eliminarse.
- Una impresora Deshabilitada puede editarse; Eliminar solo aparece dentro de esa edición.
- El sistema identifica compatibilidad según características conocidas del trabajo y capacidades registradas.
- En V1 la asignación es manual y la automática permanece deshabilitada.
- La disponibilidad de papel se expresa como cantidad y porcentaje estimados.
- Registrar una recarga presupone completar físicamente la capacidad máxima y restablece la estimación a 100 % sin afectar el contador histórico.
- Los horarios operativos se configuran por día y determinan cuándo consume tiempo un compromiso estimado.
- Los tiempos de preparación y preparación para envío se expresan en horas.
- Un pedido recibido fuera del horario puede quedar en cola y comienza a consumir tiempo desde la próxima apertura comercial.
- Los puntos pueden definir franjas horarias propias y el cliente no debe recibir una promesa fuera de una ventana válida.
- Un punto puede deshabilitarse temporalmente sin motivo y sin crear una versión; el dashboard mantiene un recordatorio mientras continúe deshabilitado.
- Los pedidos ya comprometidos con un punto no se reasignan automáticamente por una deshabilitación posterior.
- Si un pedido queda listo antes del estimado, el flujo avanza inmediatamente y el cliente ve la actualización.
- Las decisiones de cuenta corriente no autorizan producción automáticamente.

## 6. Reglas anteriores que requieren reinterpretación

| Regla anterior | Reinterpretación propuesta |
|---|---|
| Revisión administrativa siempre obligatoria | Se conserva en Control manual; Control condicional solo puede exceptuarla mediante una condición certificada |
| Nunca existe aprobación automática | Puede existir dentro de condiciones certificadas |
| Seña fija de 30 % desde 200 carillas | Regla histórica o predeterminada configurable; ya no define por sí sola el modelo de seña |
| Umbral de monto con derivación humana | El umbral separa trabajos pequeños autoaprobables de trabajos superiores que requieren seña previa configurable |
| Flujo único de avance | Estados fijos con condiciones de avance configurables |
| Configuración como mejora posterior | Motor central del producto definido |
| Asignación manual de impresora | Se confirma como modalidad predeterminada de V1; la automatización se mantiene visible pero deshabilitada hasta contar con un modelo certificado |
| Disponibilidad de papel no modelada | Se incorpora una estimación basada en capacidad configurada, consumo de trabajos y recarga manual a capacidad máxima |
| Módulos dentro de Fase 5 | Se retiran de esta fase; catálogo, planes y activación comercial quedan como definición futura separada |
| Tiempo de entrega como duración continua | Se redefine como horas operativas consumidas únicamente dentro de las ventanas configuradas |
| Habilitación de puntos como cambio versionado | La disponibilidad temporal del punto pasa a ser un estado operativo inmediato y no una nueva versión |

## 7. Registro de cambios y justificación

| Incorporación | Motivo | Relación con el motor | Estado |
|---|---|---|---|
| Reglas invariables y configurables separadas | Evitar que seguridad y operación se mezclen | Define límites del motor | Confirmado |
| RF de versionado y programación | Aplicación temporal consistente | Permite vigencia inmediata o futura | Confirmado |
| Cardinalidad del cambio pendiente | Evitar edición y programación simultáneas | Garantiza una única base futura y elimina ambigüedades | Confirmado |
| Inmutabilidad de la programación | Separar preparación de una decisión confirmada | Bloquea cambios posteriores al protocolo de seguridad | Confirmado |
| Modelos manual y condicional | Evitar opciones abiertas o ambiguas | Define recorridos operativos certificados para la Fase 2 | Confirmado |
| Condiciones económicas previas | Evitar almacenar trabajos que todavía no pueden avanzar | Protege almacenamiento y procesamiento hasta acreditar pago o seña | Confirmado |
| Matriz de pagos de Fase 3 | Evitar contradicciones entre aprobación y cobro | Habilita o bloquea medios según el modelo heredado | Confirmado para revisión |
| Umbral por monto con seña escalonada | Dar flexibilidad a trabajos chicos sin perder resguardo en montos mayores | Permite efectivo bajo umbral y exige seña acreditable por encima | Confirmado para revisión |
| Estados y edición de impresoras | Evitar modificaciones durante operación y controlar bajas | Separa operación, mantenimiento y eliminación de equipos | Confirmado |
| Compatibilidad y recomendación de impresora | Evitar asignaciones técnicamente inválidas y mejorar operación remota | Usa formato, color y disponibilidad para asistir la selección manual | Confirmado |
| Disponibilidad estimada de papel | Permitir decidir remotamente si una impresora puede seguir trabajando | Vincula capacidad configurada, consumo y recarga física | Confirmado |
| Recarga manual a capacidad máxima | Mantener sincronizado el estimado sin depender de sensores | El operador completa físicamente el faltante y confirma el reinicio a 100 % | Confirmado |
| Horarios operativos por día | Evitar estimaciones imposibles fuera del horario de trabajo | El calendario define cuándo corre el reloj productivo | Confirmado |
| Tiempos estimados en horas | Dar un compromiso comprensible y parametrizable | Permite calcular retiro en local y preparación para envío | Confirmado |
| Pedidos fuera de horario en cola | Permitir recepción continua sin prometer producción nocturna | El tiempo comienza en la próxima apertura comercial | Confirmado |
| Franjas horarias por punto | Evitar retiros prometidos fuera del horario real de cada ubicación | La disponibilidad se ajusta a ventanas configuradas | Confirmado |
| Disponibilidad operativa de puntos | Evitar versionar contingencias temporales | Habilitar/deshabilitar es inmediato y no requiere motivo | Confirmado |
| Recordatorio de puntos deshabilitados | Evitar olvidar una baja temporal | El dashboard mantiene visible la contingencia hasta reactivación | Confirmado |
| Continuidad de entregas pactadas | Proteger compromisos existentes | Una baja posterior no reasigna automáticamente pedidos ya acordados | Confirmado |
| Finalización anticipada | No convertir el estimado en una espera artificial | El timeline avanza y notifica apenas el pedido está listo | Confirmado |
| Módulos fuera de Fase 5 | Los planes Gratis/Inicial/Avanzado todavía no están definidos | Evita inventar alcance comercial prematuro | Futuro |
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