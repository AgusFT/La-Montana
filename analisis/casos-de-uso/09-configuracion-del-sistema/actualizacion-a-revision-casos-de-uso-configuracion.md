# Actualización a revisión - Casos de uso de configuración

| Campo | Valor |
|---|---|
| Versión | 2.2 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-05 |
| Dominio candidato | Configuración del sistema |
| Código de área candidato | CU-CFG |
| Integración | Pendiente de validación |

> Este archivo propone un nuevo dominio. No modifica el catálogo actual ni asigna códigos oficiales. Los casos definitivos deberán documentarse individualmente con plantilla-caso-de-uso.md y trazabilidad completa.

## 1. Objetivo

Identificar las intenciones operativas necesarias para configurar el producto sin agrupar toda la lógica en un único caso genérico.

## 2. Actores

| Actor | Participación |
|---|---|
| ADMIN_ADMIN | Prepara, valida, activa, programa y consulta configuraciones |
| Sistema | Controla permisos, coherencia, vigencia, versionado y auditoría |
| Servicio de autenticación | Verifica identidad y factor adicional cuando corresponda |
| Superadministración de plataforma | Define módulos contratados y modelos disponibles, fuera del alcance operativo de la imprenta |

El nombre formal ADMIN_ADMIN debe validarse durante la integración.

## 3. Catálogo candidato

| Código candidato | Caso de uso | Prioridad | Estado |
|---|---|---:|---|
| CAND-CU-CFG-001 | Consultar configuración activa | P0 | Confirmado para revisión |
| CAND-CU-CFG-002 | Seleccionar modelo operativo certificado | P0 | Confirmado para revisión |
| CAND-CU-CFG-003 | Configurar parámetros permitidos | P0 | Confirmado para revisión |
| CAND-CU-CFG-004 | Validar coherencia de configuración | P0 | Confirmado para revisión |
| CAND-CU-CFG-005 | Previsualizar flujo resultante | P1 | Confirmado para diseño |
| CAND-CU-CFG-006 | Activar configuración inmediatamente | P0 | Confirmado para revisión |
| CAND-CU-CFG-007 | Programar activación | P1 | Confirmado para revisión |
| CAND-CU-CFG-008 | Cancelar activación programada | P1 | Confirmado para revisión |
| CAND-CU-CFG-009 | Consultar historial de versiones | P1 | Confirmado para revisión |
| CAND-CU-CFG-010 | Utilizar versión histórica como base | P2 | Propuesto |

## 4. CAND-CU-CFG-001 - Consultar configuración activa

### Intención

Permitir que el ADMIN_ADMIN conozca qué configuración está vigente y desde cuándo.

### Precondiciones

- usuario autenticado;
- rol autorizado;
- imprenta identificada;
- configuración activa disponible.

### Flujo resumido

1. El actor ingresa a Configuración.
2. El backend valida rol e imprenta.
3. El sistema muestra la única versión activa, su vigencia y resumen.
4. El sistema muestra el único cambio pendiente, si existe: borrador en preparación o versión programada.
5. Si existe un borrador, muestra base, responsable, fechas y permite continuarlo o cancelarlo.
6. Si existe una versión programada, muestra su vigencia futura y bloquea toda edición.
7. Solo cuando no existe cambio pendiente, el actor puede iniciar una nueva configuración desde la activa, una histórica o los valores predeterminados.

### Resultado

No cambia ninguna regla ni estado. Se presenta información autorizada y las acciones se habilitan según la exclusión entre borrador y programación.

## 5. CAND-CU-CFG-002 - Seleccionar modelo certificado

### Intención

Elegir una estructura operativa coherente como base.

### Flujo resumido

1. El sistema presenta Control manual y Control condicional en una única vista comparable.
2. Explica intervención humana, aprobación, restricciones y recorrido de cada modelo.
3. Muestra Automatización certificada como evolución futura deshabilitada.
4. El actor selecciona un modelo habilitado.
5. Si selecciona Control manual, el sistema informa que todos los pedidos requieren aprobación o rechazo humano y habilita la continuidad hacia pagos y señas.
6. Si selecciona Control condicional, el sistema habilita la elección de una condición certificada.
7. La selección queda guardada en la configuración en preparación y no modifica la versión activa.

### Excepciones

- modelo no incluido;
- módulo requerido no contratado;
- modelo discontinuado;
- falta de permisos.
- intento de seleccionar Automatización certificada en esta versión.

## 6. CAND-CU-CFG-003 - Configurar parámetros

### Intención

Ajustar valores permitidos sin modificar invariantes.

### Flujo resumido para la aprobación condicional

1. El sistema muestra las condiciones certificadas pago previo, pago de seña y monto total del pedido.
2. El actor selecciona una única condición compatible.
3. Para pago previo, el sistema exige acreditar el total antes de habilitar la carga del archivo.
4. Para pago de seña, el sistema exige acreditar la seña configurada antes de habilitar la carga del archivo.
5. Para monto total, el sistema solicita un umbral: hasta ese monto puede aprobar automáticamente y, si se supera, deriva a revisión humana.
6. La interfaz simula el recorrido completo y muestra las consecuencias antes de continuar.
7. El backend valida la condición y su compatibilidad con la configuración financiera de la Fase 3.

### Excepciones

- condición no incluida en el catálogo certificado;
- pago o seña no acreditados: no se habilita la carga ni se almacena el archivo;
- umbral superado: se deriva a revisión humana sin rechazo automático;
- combinación incompatible con los medios de pago seleccionados;
- falta de permisos.

### Resultado

La configuración permanece en preparación y todavía no afecta cotizaciones. La Fase 2 define el criterio de aprobación; montos, porcentajes y medios concretos se completan en la Fase 3.

## 7. CAND-CU-CFG-004 - Validar coherencia

### Intención

Detectar incompatibilidades antes de activar.

### Validaciones propuestas

- módulos disponibles;
- combinación pago-aprobación;
- impresoras compatibles;
- reglas de seña completas;
- puntos de entrega activos;
- parámetros obligatorios;
- invariantes de seguridad.

### Resultado

La configuración queda apta o se devuelve una lista de correcciones.

## 8. CAND-CU-CFG-005 - Previsualizar flujo

### Intención

Mostrar las consecuencias operativas en lenguaje comprensible.

### Información

- comparación con versión activa;
- recorrido de pedido;
- puntos de intervención humana;
- momento de pago;
- ejemplo con seña;
- ejemplo de impresora;
- módulos afectados.

### Resultado

No activa cambios. Habilita continuar o volver a editar.

## 9. CAND-CU-CFG-006 - Activar inmediatamente

### Precondiciones

- configuración coherente;
- existe un único borrador en preparación;
- no existe otra versión programada;
- ADMIN_ADMIN autenticado;
- resumen revisado;
- protocolo de seguridad disponible.

### Flujo resumido

1. El actor elige Activar ahora.
2. El sistema muestra impacto.
3. Solicita reingreso de contraseña.
4. Solicita segundo factor cuando corresponda.
5. El backend revalida permisos y coherencia.
6. Crea versión inmutable.
7. Marca la nueva versión como activa.
8. Archiva la anterior.
9. Registra auditoría.
10. Informa fecha y hora de vigencia.

### Resultado

La siguiente cotización utiliza la nueva versión.

## 10. CAND-CU-CFG-007 - Programar activación

### Flujo resumido

1. El actor elige Programar.
2. Define fecha y hora.
3. Revisa resumen.
4. Completa protocolo de seguridad.
5. El sistema cierra el borrador editable.
6. Registra una versión programada inmutable con fecha y hora futura.
7. La versión actual continúa activa.
8. El sistema bloquea la creación o continuación de otro borrador.
9. Se muestra la programación en el inicio.

### Excepciones

- fecha pasada;
- superposición no resuelta;
- pérdida de permisos;
- configuración incompatible antes de activarse.

## 11. CAND-CU-CFG-008 - Cancelar programación

### Intención

Evitar que una configuración futura entre en vigencia.

### Reglas

- solo antes de activarse;
- requiere ADMIN_ADMIN;
- la configuración actual continúa;
- la cancelación queda auditada;
- no se elimina el registro programado.
- la versión cancelada no vuelve al estado editable;
- la cancelación libera la posibilidad de crear un nuevo borrador.

## 12. CAND-CU-CFG-009 - Consultar historial

### Información

- número de versión;
- autor;
- fecha;
- vigencia;
- diferencias;
- activación inmediata o programada;
- cancelaciones;
- estado actual.

Las versiones históricas no se editan.

## 13. Seguridad transversal

- autorización backend obligatoria;
- aislamiento por imprenta;
- historial inmutable;
- validación de plan;
- protección contra escalada de privilegios;
- factor adicional para activaciones sensibles;
- frontend no autoritativo.

## 14. Auditoría transversal

Registrar:

- consulta sensible cuando corresponda;
- inicio de preparación;
- validaciones fallidas;
- activación;
- programación;
- cancelación;
- intento no autorizado;
- error técnico;
- versión anterior y nueva.

## 15. Casos que no pertenecen a este dominio

No deben incorporarse como configuración permanente:

- pausar recepción;
- reanudar recepción;
- confirmar una cotización;
- cerrar sesión por inactividad;
- aprobar un pedido de cuenta corriente.

Esas intenciones pertenecen a disponibilidad, pedidos, seguridad o finanzas.

## 16. Requisitos para documentación definitiva

Cada caso aprobado deberá:

- usar un código oficial único;
- respetar las 13 secciones de la plantilla;
- relacionar RF, RNF, HU y reglas críticas;
- indicar impacto en estados;
- incluir flujo principal y excepciones;
- definir auditoría y criterios de aceptación;
- actualizar el catálogo oficial o su versión consolidada.

## 17. Registro de cambios y justificación

| Cambio | Documentación previa | Justificación por el motor de configuración | Estado |
|---|---|---|---|
| Nuevo dominio Configuración | No existe entre las áreas actuales | El motor introduce intenciones propias que no corresponden a pedidos o finanzas | Propuesto |
| Separación por intención | Configuración descrita de forma general | Respeta el criterio vigente de no agrupar demasiada lógica | Confirmado para revisión |
| Activación inmediata y programada | Vigencia no desarrollada como CU | El motor debe controlar cuándo entra en efecto una versión | Confirmado |
| Exclusión borrador-programada | No se limitaba el cambio pendiente | Evita ediciones concurrentes y bases futuras contradictorias | Confirmado |
| Programación inmutable | No se distinguía de una edición con fecha | La confirmación de seguridad debe cerrar la edición | Confirmado |
| Control manual y condicional | Modelos de aprobación sin recorrido cerrado | Define cuándo la decisión es humana y cuándo puede intervenir una condición certificada | Confirmado |
| Pago previo y seña antes de carga | Uso de almacenamiento no explicitado | Evita recibir archivos que todavía no pueden avanzar | Confirmado |
| Umbral de monto con derivación | Resultado al superar condición no explicitado | Elimina el rechazo automático y conserva revisión humana | Confirmado |
| Validación y previsualización | No documentadas como interacción | Evitan errores y sostienen UX remota | Confirmado |
| Códigos CAND | Catálogo oficial sin CU-CFG | Evita presentar identificadores no validados como definitivos | Provisional |

## 18. Referencias para integración

La propuesta se fundamenta en el motor de configuración y debe contrastarse con:

- analisis/casos-de-uso/casos-de-uso.md
- analisis/casos-de-uso/plantilla-caso-de-uso.md
- marco-del-proyecto/actualizacion-a-revision-motor-configuracion-versionada.md
- analisis/especificacion-de-requerimientos/actualizacion-a-revision-reglas-y-requerimientos-configurables.md
- analisis/historias-de-usuarios/actualizacion-a-revision-historias-producto-configurable.md
