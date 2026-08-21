# Actualización a revisión - Casos de uso de disponibilidad operativa

| Campo | Valor |
|---|---|
| Versión | 2.0 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-08-21 |
| Dominio candidato | Disponibilidad operativa |
| Código de área candidato | CU-OPE |

> Este dominio se propone para separar la pausa inmediata de las configuraciones versionadas. Los códigos son candidatos y deberán validarse antes de la integración definitiva.

## 1. Objetivo

Documentar las acciones que permiten detener y reanudar la recepción de nuevos trabajos ante emergencias o contingencias.

## 2. Diferencia con configuración

| Configuración versionada | Disponibilidad operativa |
|---|---|
| Modifica reglas para nuevas cotizaciones | Habilita o bloquea temporalmente la recepción |
| Solo ADMIN_ADMIN | ADMIN_ADMIN o empleado |
| Puede programarse | Acción inmediata |
| Genera una versión | Genera un evento operativo |
| Requiere revisión amplia | Requiere reconfirmación de contraseña |
| Permanece hasta nueva versión | Permanece hasta reanudación |

## 3. Catálogo candidato

| Código candidato | Caso de uso | Prioridad |
|---|---|---:|
| CAND-CU-OPE-001 | Consultar disponibilidad de la imprenta | P0 |
| CAND-CU-OPE-002 | Pausar recepción de pedidos | P0 |
| CAND-CU-OPE-003 | Reanudar recepción de pedidos | P0 |
| CAND-CU-OPE-004 | Revalidar cotización después de una pausa | P0 |

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

## 8. Mensajes propuestos

### Pausa al cotizar

La imprenta no está recibiendo nuevos pedidos en este momento. Volvé a intentarlo más tarde.

### Pausa al crear

La imprenta pausó temporalmente la recepción por una situación operativa. Tu pedido todavía no fue creado. Podrás volver a intentarlo mientras tu cotización permanezca vigente.

### Reanudación con nueva fecha

La recepción fue reanudada. Debido a la interrupción, la fecha estimada cambió. El precio y las demás condiciones se mantienen.

## 9. Seguridad

- reconfirmación de contraseña para pausar y reanudar;
- validación backend;
- control de rol;
- protección contra solicitudes repetidas;
- el frontend no puede simular disponibilidad;
- la respuesta pública no expone el motivo interno si no fue autorizado.

## 10. Auditoría

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
- errores de propagación.

## 11. Impacto en estados

| Estado | Impacto |
|---|---|
| Estados internos de pedidos existentes | Sin cambios |
| Estado visible de pedidos existentes | Sin cambios |
| Estado financiero | Sin cambios |
| Estado técnico de impresión | Sin cambios directo |
| Disponibilidad de la imprenta | Operativa o pausada |
| Cotización temporal | Puede quedar bloqueada hasta reanudación o expiración |

## 12. Criterios de aceptación propuestos

- ADMIN_ADMIN y empleado pueden pausar y reanudar.
- El sistema solicita contraseña.
- El backend rechaza credenciales inválidas.
- No se crean cotizaciones durante la pausa.
- No se confirman pedidos durante la pausa.
- Los pedidos existentes no se modifican.
- La reanudación queda auditada.
- La fecha estimada se revalida.
- Un cambio de fecha requiere aceptación.
- La pausa no extiende el temporizador de cotización.

## 13. Registro de cambios y justificación

| Cambio | Situación previa | Justificación vinculada al motor | Estado |
|---|---|---|---|
| Pausa como dominio operativo | Considerada capacidad futura | La imprenta necesita una respuesta inmediata ante emergencias | Confirmado |
| Empleado autorizado | Configuración concentrada en administrador | Pausar no modifica políticas y debe estar disponible en operación diaria | Confirmado |
| Reconfirmación de contraseña | Acción no especificada | Reduce pausas accidentales o maliciosas | Confirmado |
| Revalidación de fecha | Cotización asumida estable | La pausa puede alterar capacidad sin modificar precio | Confirmado |
| Temporizador continúa | No definido | Seguridad económica prevalece sobre la contingencia | Confirmado para revisión |
| Separación de CU-CFG | Pausa podía confundirse con configuración | Evita versionar un estado temporal | Confirmado |

## 14. Referencias para integración

Esta propuesta se origina por el motor de configuración, pero separa expresamente la disponibilidad temporal de las reglas versionadas.

Debe revisarse contra:

- marco-del-proyecto/actualizacion-a-revision-producto-configurable.md
- marco-del-proyecto/actualizacion-a-revision-motor-configuracion-versionada.md
- analisis/casos-de-uso/casos-de-uso.md
- analisis/casos-de-uso/plantilla-caso-de-uso.md
- analisis/especificacion-de-requerimientos/actualizacion-a-revision-reglas-y-requerimientos-configurables.md
