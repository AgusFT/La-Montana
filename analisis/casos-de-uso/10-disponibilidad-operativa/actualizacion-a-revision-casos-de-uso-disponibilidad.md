# Actualización a revisión - Casos de uso de disponibilidad operativa

| Campo | Valor |
|---|---|
| Versión | 2.1 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-08 |
| Dominio candidato | Disponibilidad operativa |
| Código de área candidato | CU-OPE |

> Este dominio se propone para separar la pausa inmediata y otras acciones operativas de las configuraciones versionadas. Los códigos son candidatos y deberán validarse antes de la integración definitiva.

## 1. Objetivo

Documentar las acciones que permiten detener y reanudar la recepción de nuevos trabajos ante emergencias o contingencias, y mantener información operativa necesaria para seleccionar impresoras y sostener la impresión remota.

## 2. Diferencia con configuración

| Configuración versionada | Disponibilidad operativa |
|---|---|
| Modifica reglas para nuevas cotizaciones | Habilita o bloquea temporalmente la recepción y registra eventos diarios |
| Solo ADMIN_ADMIN | ADMIN_ADMIN o empleado autorizado según la acción |
| Puede programarse | Acción inmediata |
| Genera una versión | Genera un evento operativo |
| Define capacidad máxima y capacidades de impresora | Registra consumo, recarga y disponibilidad estimada |
| Permanece hasta nueva versión | Cambia con la operación cotidiana |

## 3. Catálogo candidato

| Código candidato | Caso de uso | Prioridad |
|---|---|---:|
| CAND-CU-OPE-001 | Consultar disponibilidad de la imprenta | P0 |
| CAND-CU-OPE-002 | Pausar recepción de pedidos | P0 |
| CAND-CU-OPE-003 | Reanudar recepción de pedidos | P0 |
| CAND-CU-OPE-004 | Revalidar cotización después de una pausa | P0 |
| CAND-CU-OPE-005 | Registrar recarga de papel | P0 |
| CAND-CU-OPE-006 | Seleccionar impresora compatible para un trabajo | P0 |

## 4. CAND-CU-OPE-001 - Consultar disponibilidad

### Actores

- cliente;
- empleado;
- ADMIN_ADMIN;
- sistema.

### Resultado esperado

El sistema informa de manera coherente si la imprenta está recibiendo nuevos pedidos.

La información pública no debe exponer detalles internos innecesarios.

## 5. CAND-CU-OPE-002 - Pausar recepción

### Actor principal

ADMIN_ADMIN o empleado autenticado.

### Precondiciones

- sesión activa;
- rol interno válido;
- contraseña disponible para reconfirmación;
- imprenta actualmente operativa.

### Datos de entrada

| Dato | Obligatorio | Descripción |
|---|---|---|
| Usuario | Sí | Actor que solicita la pausa |
| Contraseña | Sí | Reconfirmación de identidad |
| Motivo | Sí | Corte de luz, falla, insumos, saturación u otro |
| Mensaje público | No | Comunicación visible al cliente si se habilita |

### Flujo principal

1. El usuario interno selecciona Pausar recepción.
2. El sistema explica el impacto.
3. Solicita motivo.
4. Solicita reconfirmación de contraseña.
5. El backend valida sesión, rol y credencial.
6. Registra la pausa.
7. Bloquea nuevas cotizaciones.
8. Bloquea la confirmación de cotizaciones activas.
9. Actualiza indicadores internos.
10. Comunica temporalmente la indisponibilidad a clientes.
11. Registra auditoría.

### Excepciones

- credencial incorrecta;
- sesión expirada;
- rol no autorizado;
- imprenta ya pausada;
- error al propagar el estado;
- intento duplicado.

### Poscondición

La imprenta permanece accesible para administrar trabajos existentes, pero no recibe nuevos pedidos.

## 6. CAND-CU-OPE-003 - Reanudar recepción

### Precondiciones

- imprenta pausada;
- usuario interno autenticado;
- reconfirmación disponible.

### Flujo principal

1. El usuario selecciona Reanudar.
2. El sistema muestra motivo y momento de pausa.
3. Solicita reconfirmación de contraseña.
4. El backend valida identidad.
5. Marca la imprenta como disponible.
6. Actualiza indicadores.
7. Habilita nuevas cotizaciones.
8. Permite reintentos de confirmación todavía vigentes.
9. Registra usuario, fecha y hora.

### Poscondición

La imprenta vuelve a recibir cotizaciones y pedidos.

## 7. CAND-CU-OPE-004 - Revalidar cotización

### Actor principal

Cliente con cotización temporal vigente.

### Precondiciones

- cotización generada antes o durante el evento;
- intento bloqueado por pausa;
- servicio reanudado;
- sesión y temporizador vigentes.

### Flujo principal

1. El sistema detecta la reanudación.
2. Recupera la cotización temporal.
3. Conserva precio, pagos y condiciones.
4. Recalcula fecha estimada de entrega.
5. Compara fecha anterior y nueva.
6. Si son iguales, permite continuar.
7. Si cambió, muestra ambas fechas.
8. El cliente acepta o cancela.
9. Si acepta, el backend valida nuevamente pausa y vigencia.
10. Crea el pedido.
11. Si cancela, no crea el pedido y elimina temporales cuando corresponda.

### Excepciones

- cotización expirada;
- sesión cerrada;
- nueva pausa;
- error al recalcular;
- cliente no acepta el nuevo plazo.

## 8. CAND-CU-OPE-005 - Registrar recarga de papel

### Actor principal

Usuario interno autorizado.

### Intención

Mantener sincronizada la disponibilidad estimada de papel con la situación física de la impresora sin requerir sensores ni ingreso manual de la cantidad agregada.

### Precondiciones

- impresora registrada;
- capacidad máxima de hojas configurada;
- usuario interno autenticado;
- impresora físicamente accesible para realizar la recarga.

### Flujo principal

1. El sistema muestra la disponibilidad estimada actual, por ejemplo 468 de 500 hojas.
2. El operador verifica físicamente la impresora.
3. Completa el faltante hasta alcanzar la capacidad máxima configurada; en el ejemplo agrega 32 hojas.
4. El operador selecciona Registrar recarga de papel.
5. El sistema muestra una confirmación indicando que la acción supone haber completado físicamente la impresora hasta el máximo configurado.
6. El operador confirma.
7. El sistema restablece la disponibilidad estimada a 500 de 500 hojas y 100 %.
8. El contador histórico de hojas impresas permanece sin cambios.
9. El sistema registra impresora, usuario, fecha y hora del evento.

### Excepciones

- capacidad máxima no configurada;
- usuario no autorizado;
- confirmación cancelada;
- impresora retirada o no disponible administrativamente;
- error al persistir el evento.

### Poscondición

La disponibilidad estimada queda sincronizada al 100 % de la capacidad configurada. La acción no afirma una lectura de sensor: depende de que el operador haya completado físicamente el faltante.

## 9. CAND-CU-OPE-006 - Seleccionar impresora compatible para un trabajo

### Actor principal

Usuario interno autorizado.

### Precondiciones

- trabajo listo para asignación;
- características del trabajo conocidas por el sistema;
- impresoras configuradas con capacidades y estado;
- modalidad manual vigente en V1.

### Flujo principal

1. El sistema obtiene formato de hoja, requisito B/N o color y demás características aplicables del trabajo.
2. Compara el trabajo con las capacidades de las impresoras Operativas.
3. Clasifica las impresoras como compatibles o incompatibles.
4. Para las incompatibles muestra la causa, por ejemplo formato no admitido o equipo solo B/N ante un trabajo color.
5. Para las compatibles muestra disponibilidad estimada de papel en cantidad y porcentaje.
6. Puede destacar una impresora como recomendada considerando compatibilidad y disponibilidad estimada.
7. El usuario elige manualmente una impresora compatible.
8. El backend valida nuevamente compatibilidad y registra la asignación.

### Excepciones

- ninguna impresora compatible;
- impresora seleccionada deja de estar Operativa;
- disponibilidad estimada insuficiente para el trabajo;
- cambio concurrente de capacidades o estado;
- falta de permisos.

### Poscondición

El trabajo queda asignado manualmente a una impresora compatible. La recomendación no equivale a asignación automática.

## 10. Mensajes propuestos

### Pausa al cotizar

La imprenta no está recibiendo nuevos pedidos en este momento. Volvé a intentarlo más tarde.

### Pausa al crear

La imprenta pausó temporalmente la recepción por una situación operativa. Tu pedido todavía no fue creado. Podrás volver a intentarlo mientras tu cotización permanezca vigente.

### Reanudación con nueva fecha

La recepción fue reanudada. Debido a la interrupción, la fecha estimada cambió. El precio y las demás condiciones se mantienen.

### Confirmación de recarga

La impresora registra actualmente una disponibilidad estimada menor al máximo. Confirmá que completaste físicamente el papel hasta la capacidad configurada antes de registrar la recarga.

## 11. Seguridad

- reconfirmación de contraseña para pausar y reanudar;
- validación backend;
- control de rol;
- protección contra solicitudes repetidas;
- el frontend no puede simular disponibilidad;
- la respuesta pública no expone el motivo interno si no fue autorizado;
- la recarga y asignación de impresora requieren un usuario interno autorizado.

## 12. Auditoría

Registrar:

- usuario;
- rol;
- fecha y hora;
- motivo;
- estado anterior y nuevo;
- resultado de reconfirmación;
- reanudación;
- intentos fallidos;
- cotizaciones bloqueadas cuando corresponda;
- errores de propagación;
- eventos de recarga de papel;
- impresora afectada por la recarga;
- asignaciones manuales de trabajos cuando corresponda.

## 13. Impacto en estados

| Estado | Impacto |
|---|---|
| Estados internos de pedidos existentes | Sin cambios por pausa o recarga |
| Estado visible de pedidos existentes | Sin cambios por pausa o recarga |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Puede incorporar asignación manual a una impresora compatible |
| Disponibilidad de la imprenta | Operativa o pausada |
| Estado de impresora | Operativa o Deshabilitada, definido por configuración |
| Disponibilidad estimada de papel | Disminuye con consumo y vuelve al 100 % al registrar recarga completa |
| Cotización temporal | Puede quedar bloqueada hasta reanudación o expiración |

## 14. Criterios de aceptación propuestos

- ADMIN_ADMIN y empleado pueden pausar y reanudar.
- El sistema solicita contraseña.
- El backend rechaza credenciales inválidas.
- No se crean cotizaciones durante la pausa.
- No se confirman pedidos durante la pausa.
- Los pedidos existentes no se modifican por la pausa.
- La reanudación queda auditada.
- La fecha estimada se revalida.
- Un cambio de fecha requiere aceptación.
- La pausa no extiende el temporizador de cotización.
- La recarga solo restablece la disponibilidad estimada cuando el operador confirma haber completado físicamente la capacidad máxima.
- El contador histórico de hojas impresas no se reinicia con la recarga.
- El sistema muestra cantidad y porcentaje estimados de papel.
- El sistema determina compatibilidad por características del trabajo y capacidades de impresora.
- En V1 la asignación final es manual aunque exista una recomendación.
- Las impresoras incompatibles muestran una causa comprensible.

## 15. Registro de cambios y justificación

| Cambio | Situación previa | Justificación vinculada al motor | Estado |
|---|---|---|---|
| Pausa como dominio operativo | Considerada capacidad futura | La imprenta necesita una respuesta inmediata ante emergencias | Confirmado |
| Empleado autorizado | Configuración concentrada en administrador | Pausar no modifica políticas y debe estar disponible en operación diaria | Confirmado |
| Reconfirmación de contraseña | Acción no especificada | Reduce pausas accidentales o maliciosas | Confirmado |
| Revalidación de fecha | Cotización asumida estable | La pausa puede alterar capacidad sin modificar precio | Confirmado |
| Temporizador continúa | No definido | Seguridad económica prevalece sobre la contingencia | Confirmado para revisión |
| Separación de CU-CFG | Pausa podía confundirse con configuración | Evita versionar un estado temporal | Confirmado |
| Recarga como evento operativo | El papel solo se trataba como capacidad | La capacidad máxima se configura, pero la reposición física ocurre durante la jornada | Confirmado |
| Recarga siempre al máximo | Podía interpretarse como carga parcial | El sistema solo reinicia el estimado cuando el operador completa el faltante hasta 100 % | Confirmado |
| Selección asistida de impresora | No existía flujo operativo detallado | Permite trabajo remoto sin automatizar la decisión en V1 | Confirmado |

## 16. Referencias para integración

Esta propuesta se origina por el motor de configuración, pero separa expresamente la disponibilidad temporal y los eventos diarios de las reglas versionadas.

Debe revisarse contra:

- marco-del-proyecto/actualizacion-a-revision-producto-configurable.md
- marco-del-proyecto/actualizacion-a-revision-motor-configuracion-versionada.md
- analisis/casos-de-uso/casos-de-uso.md
- analisis/casos-de-uso/plantilla-caso-de-uso.md
- analisis/especificacion-de-requerimientos/actualizacion-a-revision-reglas-y-requerimientos-configurables.md