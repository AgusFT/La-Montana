# Actualización a revisión - Motor de configuración versionada

| Campo | Valor |
|---|---|
| Versión | 2.1 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-01 |
| Documento relacionado | motor-de-configuracion-del-sistema.md |
| Propósito | Proponer la evolución del motor sin modificar la definición vigente |

> Esta actualización debe ser revisada por Agustín antes de integrarse. Los términos, identificadores y estructuras descritos son conceptuales y no representan todavía contratos definitivos de implementación.

## 1. Objetivo del motor

El motor de configuración debe permitir que cada imprenta adapte su funcionamiento mediante modelos operativos certificados y parámetros controlados.

No se propone un editor libre de reglas. El motor debe impedir configuraciones contradictorias, inseguras o difíciles de sostener.

## 2. Conceptos principales

### 2.1 Versión de configuración operativa

Conjunto inmutable de decisiones que determina el comportamiento aplicable a nuevas cotizaciones.

Puede contener:

- modelo de aprobación;
- condiciones de avance;
- medios y momentos de pago;
- reglas de seña;
- impresoras y capacidades;
- método de asignación;
- módulos activos;
- puntos de entrega;
- parámetros de notificación;
- horarios y servicios habilitados.

### 2.2 Modelo operativo certificado

Estructura preconfigurada, validada por el producto y compatible con reglas de seguridad.

Ejemplos conceptuales:

- revisión manual y pago al entregar;
- revisión manual y pago digital previo;
- aprobación condicional y pago previo;
- aprobación automática bajo condiciones certificadas.

Los ejemplos no constituyen todavía el catálogo definitivo.

### 2.3 Parámetro controlado

Valor que puede modificarse dentro de límites permitidos sin alterar invariantes del sistema.

### 2.4 Invariante

Regla que ninguna imprenta puede desactivar, como autenticación, autorización backend, auditoría, aislamiento de datos, protección de archivos y ejecución de impresión solamente autorizada.

## 3. Ciclo de vida

| Estado | Definición |
|---|---|
| En preparación | Configuración que el ADMIN_ADMIN está revisando y todavía no tiene vigencia |
| Programada | Configuración confirmada con fecha y hora futura |
| Activa | Versión utilizada para nuevas cotizaciones |
| Histórica | Versión que estuvo activa y permanece disponible para auditoría |
| Programada cancelada | Versión futura cancelada antes de entrar en vigencia |

Una versión activa o histórica no se edita.

Para cambiarla se crea una versión nueva.

### 3.1 Cardinalidad y exclusión del cambio pendiente

Por cada imprenta deben cumplirse simultáneamente estas reglas:

- existe exactamente una configuración activa;
- puede existir como máximo una configuración en preparación;
- puede existir como máximo una configuración programada;
- una configuración en preparación y una programada no pueden coexistir;
- por lo tanto, existe como máximo un cambio pendiente respecto de la configuración activa.

La configuración en preparación es el único estado editable. No tiene fecha de vigencia y puede guardarse, continuarse o cancelarse.

Cuando el ADMIN_ADMIN completa el protocolo de seguridad y confirma una fecha futura, la configuración deja de estar en preparación y pasa a Programada. La versión programada ya no es un borrador: queda cerrada, auditada e inmutable mientras espera su activación.

| Situación | Estado activo | Cambio pendiente permitido | Edición disponible |
|---|---|---|---|
| Sin cambios pendientes | Una versión activa | Ninguno | Puede iniciarse un borrador |
| Edición en curso | Una versión activa | Un borrador en preparación | Solo puede continuarse o cancelarse ese borrador |
| Activación futura confirmada | Una versión activa | Una versión programada | No; debe activarse o cancelarse la programación |

No puede iniciarse otro borrador desde la versión activa, una histórica o los valores predeterminados mientras exista un borrador o una programación pendiente.

## 4. Activación

### 4.1 Activación inmediata

La nueva versión entra en vigencia después de completar el protocolo de seguridad.

La primera cotización posterior debe utilizarla.

### 4.2 Activación programada

El ADMIN_ADMIN define fecha y hora.

Hasta ese momento continúa vigente la configuración actual. La configuración programada puede cancelarse antes de entrar en vigencia.

La programación consume el único cambio pendiente permitido. Mientras exista, no puede crearse ni continuarse un borrador. Si se requiere modificar sus decisiones, primero debe cancelarse la programación; la cancelación libera la edición sin eliminar el registro auditado.

### 4.3 Reutilización de una versión anterior

Si se desea recuperar una configuración histórica, debe copiarse como base de una nueva versión.

No se modifica el historial ni se reactiva silenciosamente un registro anterior.

## 5. Protocolo de seguridad

La activación o programación de cambios sensibles debe contemplar:

1. resumen de cambios;
2. explicación de consecuencias;
3. confirmación explícita;
4. reingreso de contraseña;
5. segundo factor o código por correo cuando corresponda;
6. validación backend;
7. creación de la versión;
8. registro de auditoría.

El mecanismo definitivo de segundo factor debe validarse técnicamente.

## 6. Auditoría

Cada acción debe registrar:

- usuario responsable;
- rol;
- imprenta;
- fecha y hora;
- versión anterior;
- nueva versión;
- diferencias;
- tipo de activación;
- fecha de vigencia;
- resultado de la verificación de seguridad;
- cancelación, si corresponde;
- motivo u observación;
- contexto técnico permitido.

## 7. No retroactividad

Las configuraciones nuevas no alteran:

- pedidos existentes;
- trabajos en producción;
- estados anteriores;
- cotizaciones ya generadas;
- decisiones financieras registradas;
- historial de auditoría.

La versión se captura en el momento de cotizar y queda asociada al resultado mientras la cotización permanezca vigente.

## 8. Flujo de cotización

1. El cliente prepara el pedido.
2. Presiona Cotizar pedido.
3. El backend verifica que la imprenta no esté pausada.
4. Consulta la versión activa.
5. Calcula precio y condiciones.
6. Genera un identificador temporal de cotización.
7. Registra la versión utilizada.
8. Devuelve resumen, medios de pago, entrega, seña y fecha estimada.
9. El cliente confirma dentro del tiempo permitido.
10. El backend valida nuevamente pausa, cotización y sesión.
11. Crea el pedido con la información capturada.
12. Elimina recursos temporales que ya no sean necesarios.

El frontend no debe ser la fuente de verdad de las reglas aplicadas.

## 9. Temporizadores de cotización

| Momento | Comportamiento |
|---|---|
| Generación | Comienzan cinco minutos iniciales |
| Fin de los cinco minutos | Se muestra consulta de actividad |
| Ventana de respuesta | Hasta sesenta segundos |
| Sin respuesta | Se anula cotización, se eliminan temporales y se redirige o cierra sesión |
| Respuesta afirmativa | Se otorgan diez minutos exactos |
| Durante la extensión | Se muestra cuenta regresiva visible |
| Fin de la extensión | Se anula cotización, se eliminan temporales y se cierra sesión |

La extensión no se reinicia por actividad posterior.

## 10. Pausa operativa

La pausa no forma parte de una versión de configuración. Es un estado operacional prioritario.

Puede ser ejecutada por ADMIN_ADMIN o empleado con reconfirmación de contraseña.

Debe bloquear:

- generación de nuevas cotizaciones;
- confirmación de pedidos cotizados;
- reintentos de confirmación mientras continúe activa.

No debe bloquear la consulta o administración de pedidos existentes, salvo que otra contingencia técnica lo impida.

## 11. Reanudación y fecha de entrega

Al reanudar:

1. el cliente conserva precio y condiciones si la cotización continúa vigente;
2. el backend recalcula la fecha estimada;
3. si no cambia, permite continuar;
4. si cambia, muestra fecha anterior y nueva;
5. solicita aceptación explícita;
6. crea el pedido solamente si el cliente acepta.

La pausa no detiene el temporizador de seguridad de la cotización.

## 12. Cuenta corriente - Propuesta a validar

La cuenta corriente se modela como una excepción financiera autorizada manualmente, no como una combinación libre de reglas.

Propuesta:

- asignación manual por ADMIN_ADMIN;
- opción con límite o sin límite;
- estado activa, suspendida o deshabilitada;
- todos los pedidos requieren revisión manual;
- el pedido puede enviarse sin pago;
- producción permanece bloqueada hasta aprobación;
- el administrador consulta deuda total y carga operativa;
- la fecha estimada de entrega se define manualmente.

La conducta al superar el límite queda pendiente de validación por Agustín.

## 13. Orden de precedencia propuesto

| Prioridad | Regla |
|---:|---|
| 1 | Seguridad, autorización, integridad y auditoría |
| 2 | Pausa operativa |
| 3 | Disponibilidad de módulos según plan |
| 4 | Versión activa al cotizar |
| 5 | Excepciones financieras autorizadas |
| 6 | Parámetros visuales y preferencias no críticas |

Esta precedencia debe validarse durante el diseño técnico.

## 14. Configuraciones futuras

Se preservan como evolución:

- cambios por demanda;
- cambios por horario;
- pausas automáticas;
- limitación por stock;
- reprogramación para otro día;
- asignación dinámica según carga;
- respuestas automáticas ante saturación.

No deben mezclarse con el alcance base de activación inmediata o programada.

## 15. Registro de cambios y justificación

| Cambio propuesto | Referencia anterior | Justificación por el motor de configuración | Estado |
|---|---|---|---|
| Sustituir política por versión de configuración | Terminología amplia de políticas | Reduce ambigüedad para negocio, UX e implementación | Confirmado para revisión |
| Modelos certificados | Combinación abierta no definida | Evita zonas grises e incompatibilidades | Confirmado |
| Estados del ciclo de vida | Activación sin modelo completo | Permite preparar, programar, cancelar y auditar | Confirmado |
| Un único cambio pendiente | Convivencia de borradores y programaciones no explicitada | Evita carreras, bases ambiguas y configuraciones futuras contradictorias | Confirmado |
| Programación inmutable | Programación tratada solo como fecha futura | Separa la edición de una versión ya confirmada y auditada | Confirmado |
| Captura al cotizar | Reglas aplicadas principalmente al crear pedido | Congela condiciones visibles y evita carreras de configuración | Confirmado |
| Pausa fuera de la versión | Pausa considerada evolución futura | Debe actuar inmediatamente sin reversionar toda la configuración | Confirmado |
| Temporizadores de cotización | Sin vigencia detallada | Protege información económica y libera recursos temporales | Confirmado |
| Cuenta corriente manual | Excepción genérica por lista | Requiere control financiero y operativo por pedido | Requiere revisión de Agustín |
| Precedencia de reglas | No explicitada | El motor necesita resolver conflictos de manera determinista | Propuesta técnica |

## 16. Referencias para integración

Esta actualización debe contrastarse con:

- marco-del-proyecto/motor-de-configuracion-del-sistema.md
- marco-del-proyecto/alcance-general.md
- analisis/especificacion-de-requerimientos/matriz-reglas-de-negocio.md
- analisis/especificacion-de-requerimientos/requerimientos-funcionales.md
- analisis/especificacion-de-requerimientos/requerimientos-no-funcionales.md
- marco-del-proyecto/matriz-trazabilidad.md

La justificación principal de este documento es la incorporación del motor de configuración como núcleo del producto definido.
