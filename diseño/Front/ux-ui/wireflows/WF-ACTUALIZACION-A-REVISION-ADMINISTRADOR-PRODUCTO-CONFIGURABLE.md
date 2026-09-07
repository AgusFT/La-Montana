# WF - Actualización a revisión - Administrador del producto configurable

| Campo | Valor |
|---|---|
| Versión | 2.3 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-07 |
| Plataforma principal | Web administrativa |
| Actor principal | ADMIN_ADMIN |
| Actores complementarios | Empleado, Cliente, Sistema |
| Relación | Extensión del wireflow administrativo histórico |

> Este wireflow no reemplaza WF-ADMINISTRADOR-MVP.md. Propone las vistas necesarias para el producto configurable y sirve como base inmediata para mockups. Las vistas de cuenta corriente permanecen exploratorias.

## 1. Principios de experiencia

- configuración guiada;
- lenguaje operativo;
- consecuencias visibles;
- selección por modelos certificados;
- parámetros progresivos;
- resumen antes de activar;
- seguridad explícita;
- estado actual siempre identificable;
- errores accionables;
- accesibilidad y respuesta adaptable;
- backend como fuente de verdad.

## 2. Navegación propuesta

Configuración se incorpora como sección principal del panel del ADMIN_ADMIN.

Subsecciones:

- Resumen;
- Operación;
- Aprobación;
- Pagos y seña;
- Impresoras;
- Entrega;
- Módulos;
- Versiones;
- Cuenta corriente, sujeta a revisión.

La pausa operativa debe estar disponible desde el encabezado o dashboard, sin obligar a ingresar al asistente de configuración.

## 3. Inventario de vistas

| Código visual | Vista | Estado de definición |
|---|---|---|
| WF-CFG-01 | Inicio de configuración | Mockup V3 en revisión |
| WF-CFG-02 | Seleccionar modelo operativo | Mockup V3 en revisión |
| WF-CFG-03 | Configurar aprobación | Tres mockups V3 en revisión |
| WF-CFG-04 | Configurar pagos | Mockups V3 en definición |
| WF-CFG-05 | Configurar seña | Mockups V3 en definición |
| WF-CFG-06 | Impresoras y capacidades | Lista para mockup |
| WF-CFG-07 | Método de asignación | Lista para mockup |
| WF-CFG-08 | Módulos contratados y activos | Lista para mockup |
| WF-CFG-09 | Entrega y puntos habilitados | Lista para mockup |
| WF-CFG-10 | Resumen y simulación | Lista para mockup |
| WF-CFG-11 | Conflictos de configuración | Lista para mockup |
| WF-CFG-12 | Activar o programar | Lista para mockup |
| WF-CFG-13 | Verificación de seguridad | Lista para mockup |
| WF-CFG-14 | Confirmación de activación | Lista para mockup |
| WF-CFG-15 | Historial de versiones | Lista para mockup |
| WF-CFG-16 | Detalle de versión | Lista para mockup |
| WF-OPE-01 | Confirmar pausa | Lista para mockup |
| WF-OPE-02 | Sistema pausado | Lista para mockup |
| WF-OPE-03 | Confirmar reanudación | Lista para mockup |
| WF-COT-01 | Aviso de inactividad | Lista para mockup cliente |
| WF-COT-02 | Cuenta regresiva | Lista para mockup cliente |
| WF-COT-03 | Bloqueo por pausa | Lista para mockup cliente |
| WF-COT-04 | Aceptar nueva fecha | Lista para mockup cliente |
| WF-CC-01 | Clientes con cuenta corriente | Exploratoria |
| WF-CC-02 | Detalle de deuda | Exploratoria |
| WF-CC-03 | Revisar pedido a cuenta | Exploratoria |
| WF-CC-04 | Definir fecha manual | Exploratoria |

## 4. WF-CFG-01 - Inicio de configuración

### Objetivo

Mostrar el estado actual sin obligar al administrador a editar.

### Contenido

- configuración vigente: nombre, versión, estado, fecha de vigencia y modelo operativo;
- próxima configuración: ausencia de programación o versión programada con fecha, hora y zona horaria;
- estado de edición: sin borrador o borrador en preparación con base, fecha de creación y último guardado;
- resumen operativo, económico y de recursos dinámicos de la versión activa;
- acceso a historial y detalle;
- acciones habilitadas según el cambio pendiente existente.

### Variantes obligatorias

#### CFG-001 - Sin cambio pendiente

- existe una única versión activa;
- no existe borrador;
- no existe versión programada;
- se habilita Editar configuración, usar una versión histórica o restaurar valores predeterminados.

#### CFG-001B - Borrador en curso

- la versión activa continúa vigente;
- no existe versión programada;
- se muestran base, responsable, fecha de creación, último guardado y paso alcanzado;
- se habilita Ver cambios, Editar borrador y Cancelar borrador;
- se bloquea la creación de otro borrador desde cualquier base.

#### CFG-001C - Versión programada

- la versión activa continúa vigente hasta la fecha futura;
- se muestra la versión programada, fecha, hora, zona horaria y modelo;
- no existe borrador;
- se habilita Ver detalle y Cancelar programación;
- se bloquea toda edición hasta la activación o cancelación.

### Acciones

- editar a partir de la activa cuando no existe cambio pendiente;
- continuar o cancelar el único borrador existente;
- elegir otra base únicamente cuando no existe cambio pendiente;
- consultar detalle;
- cancelar programación;
- ir al historial.

La interfaz no debe denominar borrador a una versión programada. El borrador es editable y no tiene vigencia; la versión programada ya fue confirmada, es inmutable y ocupa el único cambio pendiente permitido.

## 5. WF-CFG-02 - Seleccionar modelo operativo

La vista utiliza tarjetas comparables y se representa mediante un único mockup `MC-ADM-CFG-002`.

Cada tarjeta muestra:

- nombre;
- descripción;
- aprobación;
- momento de pago;
- intervención humana;
- módulos requeridos;
- recomendación de uso;
- etiqueta Disponible o No incluido.

No debe mostrar combinaciones técnicas crudas.

Modelos de esta versión:

- Control manual;
- Control condicional;
- Automatización certificada, visible como evolución futura y deshabilitada.

Control manual exige revisión, aprobación o rechazo humano para todos los pedidos. Control condicional habilita la configuración de una condición certificada en `WF-CFG-03`.

## 6. WF-CFG-03 - Configurar aprobación

Esta vista se habilita únicamente al seleccionar Control condicional. Se representa mediante tres mockups:

- `MC-ADM-CFG-003A`: aprobación por pago previo;
- `MC-ADM-CFG-003B`: aprobación por pago de seña;
- `MC-ADM-CFG-003C`: aprobación por monto total del pedido.

Cada alternativa muestra la condición, el recorrido completo y los bloqueos aplicables.

Reglas visibles:

- pago previo exige acreditación total antes de habilitar la carga del archivo;
- pago de seña exige acreditar la seña configurada antes de habilitar la carga del archivo;
- mientras falte pago o seña previa, el archivo permanece del lado del cliente y no utiliza almacenamiento ni procesamiento del servidor;
- el criterio por monto permite aprobación automática hasta un umbral monetario que se parametriza en Fase 3;
- cuando el pedido supera el umbral, la Fase 3 exige una seña previa configurable antes de continuar;
- superar el umbral no deriva automáticamente a revisión humana ni implica rechazo automático;
- los montos, porcentajes y medios se completan en la Fase 3;
- los estados internos no se editan y el backend conserva la decisión final.

## 7. WF-CFG-04 - Configurar pagos

La vista debe comenzar mostrando el **modelo heredado de Fase 2** y explicar que las opciones incompatibles permanecen visibles pero deshabilitadas.

### Matriz de medios por modelo

#### Control manual

- transferencia: disponible;
- efectivo: disponible;
- pago digital: disponible;
- la aprobación continúa siendo humana;
- la seña, si se configura, funciona como regla financiera opcional.

#### Control condicional - Pago previo

- transferencia: disponible si puede acreditarse;
- efectivo: deshabilitado para cumplir la condición;
- pago digital: disponible;
- debe existir al menos un medio acreditable;
- el archivo permanece del lado del cliente hasta acreditar el total;
- la seña no aplica.

#### Control condicional - Pago de seña

- transferencia: disponible para acreditar la seña;
- efectivo: deshabilitado para acreditar la seña previa;
- pago digital: disponible para acreditar la seña;
- el archivo permanece del lado del cliente hasta acreditar la seña;
- la condición de seña viene fijada por Fase 2.

#### Control condicional - Monto del pedido

- transferencia: disponible;
- efectivo: disponible como medio general para pedidos dentro del umbral y para el saldo posterior cuando corresponda;
- pago digital: disponible;
- la aprobación hasta el umbral es automática;
- para pedidos superiores al umbral, la seña previa debe acreditarse mediante transferencia, pago digital u otro medio acreditable;
- efectivo no puede utilizarse para acreditar esa seña previa.

La interfaz debe diferenciar **medios generales habilitados** de **medios válidos para acreditar una condición previa**.

## 8. WF-CFG-05 - Configurar seña

Los controles dependen del modelo heredado.

### Control manual

- habilitar seña: opcional;
- tipo: porcentaje o monto fijo;
- valor: editable;
- condición de aplicación: configurable dentro de parámetros permitidos;
- la seña no aprueba el pedido y no reemplaza la revisión humana.

### Pago previo

- seña: no aplica;
- controles visibles pero deshabilitados;
- explicación: el modelo exige pago total previo.

### Pago de seña

- la condición `pago de seña` está definida en Fase 2 y no puede apagarse ni sustituirse;
- no debe existir un toggle para deshabilitar la seña;
- la condición se muestra bloqueada;
- tipo de seña: editable, por ejemplo porcentaje o monto fijo;
- valor de la seña: editable;
- hasta acreditarse la seña, el archivo permanece del lado del cliente.

La referencia histórica de 30 % desde 200 carillas no constituye una condición fija de este modelo. Puede utilizarse como ejemplo o valor predeterminado, pero no debe imponerse en la interfaz.

### Monto del pedido

- condición: `por monto del pedido`, heredada de Fase 2 y bloqueada;
- umbral de autoaprobación: input monetario editable;
- pedidos hasta el umbral: pueden aprobarse automáticamente y no requieren seña previa;
- pedidos superiores al umbral: se exige una seña previa configurable;
- tipo de seña: porcentaje o monto fijo;
- valor de seña: editable;
- medios para acreditar la seña: transferencia, pago digital u otros acreditables;
- efectivo puede mantenerse como medio general, pero no acredita la seña previa;
- una vez acreditada la seña, el saldo restante conserva los medios generales habilitados.

### Presentación común

Cada variante debe incluir:

- ejemplo operativo calculado;
- explicación de qué campos provienen de Fase 2 y cuáles son editables;
- simulación visual del recorrido desde Fase 1 → Fase 2 → Fase 3 → resultado.

## 9. WF-CFG-06 - Impresoras y capacidades

Listado con:

- nombre;
- estado;
- tecnología;
- formatos;
- color;
- dúplex;
- capacidad;
- disponibilidad;
- editar;
- deshabilitar.

Una impresora no operativa debe identificarse claramente.

## 10. WF-CFG-07 - Método de asignación

Opciones:

- selección manual entre equipos compatibles;
- asignación automática, cuando esté certificada.

La opción automática explica que considera:

- características del trabajo;
- capacidad;
- carga;
- estado operativo.

## 11. WF-CFG-08 - Módulos

Tres grupos:

- Activos;
- Incluidos pero desactivados;
- No incluidos en el plan.

Cada módulo muestra descripción, dependencias e impacto.

Un módulo no contratado puede ofrecer información comercial, pero no permitir activación.

## 12. WF-CFG-09 - Entrega

Permite administrar:

- puntos activos;
- retiro;
- envío;
- horarios;
- capacidades o restricciones;
- opciones visibles al cliente.

## 13. WF-CFG-10 - Resumen y simulación

Debe ser una vista narrativa.

Ejemplo:

Un pedido compatible ingresa. Se aplican el modelo operativo y la configuración financiera capturados. Si existe una condición económica previa, el archivo no se habilita hasta que esa condición quede acreditada. Después puede continuar hacia las reglas de producción.

Secciones:

- cambios respecto de la versión activa;
- recorrido resultante;
- ejemplo estándar;
- ejemplo con pago previo;
- ejemplo con seña;
- ejemplo por monto dentro y fuera del umbral;
- ejemplo con error;
- módulos afectados;
- advertencias.

## 14. WF-CFG-11 - Conflictos

Formato recomendado:

| Conflicto | Por qué ocurre | Cómo resolver |
|---|---|---|
| Pago previo sin medio acreditable | No existe forma habilitada de completar la condición | Activar transferencia o pago digital |
| Pago previo con seña adicional | El modelo ya exige el 100 % previo | Deshabilitar la seña o elegir otro modelo |
| Pago de seña con efectivo como única opción | El efectivo no puede acreditar una seña previa | Activar transferencia o pago digital |
| Monto superior con seña sin medio acreditable | No existe forma de registrar la seña previa | Activar transferencia o pago digital |
| Asignación automática sin capacidades | El sistema no puede comparar impresoras | Completar capacidades |
| Módulo requerido no incluido | El perfil depende de una función no contratada | Elegir otro perfil o consultar plan |

No debe utilizar mensajes genéricos.

## 15. WF-CFG-12 - Activar o programar

Dos alternativas principales:

### Activar ahora

Texto: Se aplicará a la siguiente cotización confirmada después de la activación.

### Programar

Controles:

- fecha;
- hora;
- zona horaria visible;
- resumen de vigencia;
- versión actual hasta ese momento.

## 16. WF-CFG-13 - Verificación de seguridad

Paso separado con:

- resumen final;
- impacto;
- reingreso de contraseña;
- código adicional cuando corresponda;
- cancelar;
- confirmar.

No debe permitir volver a editar después del código sin reiniciar la validación.

## 17. WF-CFG-14 - Confirmación

Muestra:

- versión creada;
- fecha y hora;
- activación inmediata o programada;
- usuario;
- acceso al detalle;
- volver al inicio.

## 18. WF-CFG-15 y 16 - Historial

Listado:

- versión;
- estado;
- vigencia;
- autor;
- modelo;
- resumen;
- ver detalle.

Detalle:

- comparación;
- auditoría;
- módulos;
- parámetros;
- opción Usar como base.

No existe Editar versión.

## 19. WF-OPE-01 - Pausar recepción

Disponible para ADMIN_ADMIN y empleado.

Modal:

- título de advertencia;
- impacto;
- selector de motivo;
- observación;
- contraseña;
- cancelar;
- Pausar recepción.

El botón debe expresar la acción completa.

## 20. WF-OPE-02 - Estado pausado

Elementos:

- banner persistente;
- motivo interno;
- usuario que pausó;
- hora;
- pedidos existentes sin cambios;
- botón Reanudar;
- vista previa del mensaje público.

## 21. WF-OPE-03 - Reanudar

Modal:

- resumen de pausa;
- contraseña;
- advertencia sobre cotizaciones todavía vigentes;
- botón Reanudar recepción.

## 22. Impacto en cliente

### WF-COT-01 - Aviso de actividad

- título: ¿Todavía estás ahí?;
- explicación de seguridad;
- contador de sesenta segundos;
- botón Continuar;
- cierre automático si no responde.

### WF-COT-02 - Cuenta regresiva final

- contador visible de diez minutos;
- no se reinicia;
- advertencia de pérdida de cotización;
- botón Crear pedido.

### WF-COT-03 - Pausa al confirmar

- comunica que el pedido no fue creado;
- conserva resumen visible;
- impide confirmar;
- informa que el tiempo continúa.

### WF-COT-04 - Nueva fecha

- fecha anterior;
- fecha actualizada;
- precio sin cambios;
- condiciones sin cambios;
- Aceptar y continuar;
- Cancelar.

## 23. Cuenta corriente - Vistas exploratorias

### WF-CC-01 - Listado

- cliente;
- estado;
- límite;
- deuda;
- pedidos pendientes;
- revisar.

### WF-CC-02 - Detalle

- deuda actual y proyectada;
- historial;
- pagos;
- pedidos;
- carga productiva;
- suspender cuenta.

### WF-CC-03 - Revisar pedido

- resumen del pedido;
- recursos;
- deuda;
- fechas comprometidas;
- aprobar, rechazar o dejar pendiente.

### WF-CC-04 - Fecha manual

- calendario;
- carga diaria;
- fecha propuesta;
- observaciones;
- confirmar decisión.

Estas vistas deben incluir una marca visible de Propuesta a revisión.

## 24. Flujo principal del administrador

Inicio de configuración
→ seleccionar modelo
→ completar parámetros
→ validar
→ resolver conflictos
→ previsualizar
→ elegir vigencia
→ verificar identidad
→ activar o programar
→ consultar confirmación e historial.

## 25. Permisos visuales

| Función | ADMIN_ADMIN | Empleado |
|---|---:|---:|
| Acceder a Configuración | Sí | No |
| Crear versión | Sí | No |
| Activar o programar | Sí | No |
| Consultar historial completo | Sí | No |
| Pausar | Sí | Sí |
| Reanudar | Sí | Sí |
| Administrar cuenta corriente | Sí | No |

La autorización real debe validarse en backend.

## 26. Registro de cambios y justificación

| Vista incorporada | Wireflow anterior | Justificación por el motor de configuración | Estado |
|---|---|---|---|
| Inicio de configuración | No existía como área central | El motor necesita mostrar versión y vigencia | Lista para mockup |
| Variantes de CFG-001 | No se distinguía borrador de programación | Hace visible la exclusión entre edición y activación futura | Mockup V3 en revisión |
| Selección por modelos | Configuración limitada | Evita construir reglas libres | Mockup V3 en revisión |
| Pagos y seña heredados de Fase 2 | Las restricciones financieras se trataban de forma general | La Fase 3 debe mostrar únicamente combinaciones compatibles con el modelo elegido | Mockups V3 en definición |
| Umbral monetario con seña escalonada | Superar el umbral implicaba revisión humana | Se redefine como flujo flexible: autoaprobación bajo umbral y seña acreditable por encima | Mockup V3 en definición |
| Resumen y simulación | Sin previsualización integral | Permite comprender consecuencias | Lista para mockup |
| Activar o programar | Sin flujo temporal | Define cuándo aplican los cambios | Lista para mockup |
| Verificación reforzada | Acciones administrativas generales | Protege cambios operativos y financieros | Lista para mockup |
| Historial de versiones | Auditoría centrada en pedidos | El motor requiere trazabilidad propia | Lista para mockup |
| Pausa operativa | No priorizada | Atiende emergencias reales | Lista para mockup |
| Temporizadores cliente | Cotización sin caducidad detallada | Protege sesión económica y temporales | Lista para mockup |
| Cuenta corriente | No formalizada | Explora excepción financiera controlada | Requiere revisión |

## 27. Referencias para integración

Este wireflow se justifica por los cambios del motor de configuración y debe contrastarse con:

- diseño/Front/ux-ui/wireflows/WF-ADMINISTRADOR-MVP.md
- marco-del-proyecto/actualizacion-a-revision-producto-configurable.md
- marco-del-proyecto/actualizacion-a-revision-motor-configuracion-versionada.md
- analisis/historias-de-usuarios/actualizacion-a-revision-historias-producto-configurable.md
- analisis/casos-de-uso/09-configuracion-del-sistema/actualizacion-a-revision-casos-de-uso-configuracion.md
- analisis/casos-de-uso/10-disponibilidad-operativa/actualizacion-a-revision-casos-de-uso-disponibilidad.md
