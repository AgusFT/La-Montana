# WF - Actualización a revisión - Administrador del producto configurable

| Campo | Valor |
|---|---|
| Versión | 2.1 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-01 |
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
| WF-CFG-02 | Seleccionar modelo operativo | Lista para mockup |
| WF-CFG-03 | Configurar aprobación | Lista para mockup |
| WF-CFG-04 | Configurar pagos | Lista para mockup |
| WF-CFG-05 | Configurar seña | Lista para mockup |
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

La vista utiliza tarjetas comparables.

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

## 6. WF-CFG-03 - Configurar aprobación

Opciones visuales:

- Manual;
- Automática;
- Condicional.

Cada opción explica:

- quién interviene;
- cuándo avanza;
- qué bloqueos permanecen;
- ejemplo de pedido;
- impacto en producción.

Los estados internos no se editan.

## 7. WF-CFG-04 - Configurar pagos

Debe mostrar primero decisiones de negocio:

- pago previo;
- pago al entregar;
- modelo compatible definido por perfil.

Después muestra medios:

- efectivo;
- transferencia;
- pago digital;
- otros módulos disponibles.

Las combinaciones incompatibles quedan deshabilitadas con explicación.

## 8. WF-CFG-05 - Configurar seña

Controles propuestos:

- requiere seña o no;
- monto fijo;
- porcentaje;
- condición de aplicación;
- ejemplo calculado;
- momento necesario para avanzar.

Los criterios disponibles dependen del modelo certificado.

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

Un pedido compatible ingresa. Se revisa manualmente. Si requiere seña, queda bloqueado hasta registrar el pago. Después puede avanzar hacia la impresora seleccionada.

Secciones:

- cambios respecto de la versión activa;
- recorrido resultante;
- ejemplo estándar;
- ejemplo con seña;
- ejemplo con error;
- módulos afectados;
- advertencias.

## 14. WF-CFG-11 - Conflictos

Formato recomendado:

| Conflicto | Por qué ocurre | Cómo resolver |
|---|---|---|
| Pago previo sin medio digital | No existe forma habilitada de completar el pago | Activar transferencia o elegir pago al entregar |
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
| Selección por modelos | Configuración limitada | Evita construir reglas libres | Lista para mockup |
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
