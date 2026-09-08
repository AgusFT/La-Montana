# WF - Fase 4 - Impresoras: accesos operativos, recarga y asignación

| Campo | Valor |
|---|---|
| Versión | 1.0 - Propuesta |
| Estado | Actualización a revisión |
| Fecha | 2026-09-08 |
| Fase relacionada | Fase 4 - Impresoras, capacidades y asignación |
| Wireflows relacionados | WF-CFG-06, WF-CFG-07 |
| Casos relacionados | CAND-CU-OPE-005, CAND-CU-OPE-006 |
| Actor de configuración | ADMIN_ADMIN |
| Actor operativo | Usuario interno autorizado |

> Este documento complementa el wireflow general del motor de configuración. Su objetivo es separar con claridad las acciones de **configuración** de las acciones de **operación diaria**, especialmente la recarga de papel y la asignación de trabajos a impresoras.

## 1. Principio de navegación

La Fase 4 expone información que pertenece a dos momentos distintos del negocio:

1. **Configuración:** definir qué impresoras existen, qué capacidades poseen y qué método de asignación utiliza la imprenta.
2. **Operación diaria:** consultar disponibilidad estimada, registrar recargas de papel y seleccionar una impresora compatible para un trabajo concreto.

Las acciones operativas no deben obligar a ingresar al asistente de configuración.

En particular, **Registrar recarga de papel no pertenece al flujo de edición de una impresora**. Es un evento cotidiano y debe tener acceso directo desde el dashboard operativo.

## 2. Mapa general de accesos

| Necesidad | Punto de entrada | Destino | Frecuencia esperada |
|---|---|---|---|
| Configurar impresoras | Dashboard → Configuración | Motor de configuración → Fase 4 → Impresoras y capacidades | Baja |
| Editar una impresora | Configuración → Fase 4 → Deshabilitar → Editar | Panel de edición de impresora | Baja |
| Registrar recarga de papel | Dashboard → Estado de impresoras | Confirmación de recarga | Alta / diaria |
| Consultar disponibilidad | Dashboard → Estado de impresoras | Resumen operativo de impresoras | Alta |
| Asignar trabajo a impresora | Trabajo/Pedido listo para producción → Asignar impresora | Panel de compatibilidad y recomendación | Por trabajo |
| Cambiar método de asignación | Dashboard → Configuración | Motor de configuración → Fase 4 → Método de asignación | Muy baja |

## 3. Acceso al panel de configuración de impresoras

### Ruta principal

`Dashboard → Configuración → Motor de configuración → Fase 4 → Impresoras y capacidades`

### Propósito

Este panel se utiliza para decisiones relativamente estables:

- agregar una impresora;
- definir nombre visible;
- registrar formatos compatibles;
- definir B/N o color;
- configurar dúplex cuando corresponda;
- definir capacidad máxima de hojas;
- deshabilitar una impresora;
- editar una impresora previamente deshabilitada;
- eliminar una impresora desde su edición cuando corresponda.

No debe utilizarse como punto de entrada para tareas rutinarias como la recarga de papel.

## 4. Acceso a la edición de una impresora

### Ruta

`Configuración → Fase 4 → Impresoras y capacidades → Deshabilitar impresora → Editar`

### Regla visible

Una impresora Operativa no puede editarse.

Después de deshabilitarla:

- deja de recibir nuevos trabajos;
- se habilita Editar;
- dentro de Editar se pueden modificar sus parámetros permitidos;
- al final del panel aparece Eliminar impresora;
- posteriormente puede volver a habilitarse.

La edición pertenece al dominio de configuración y no debe confundirse con el panel operativo de estado.

## 5. Acceso operativo a la recarga de papel

### Decisión de UX

La acción **Registrar recarga de papel** debe estar disponible desde el **dashboard**, sin necesidad de entrar a Configuración ni de esperar a que exista un trabajo para asignar.

### Componente recomendado en dashboard

Bloque o tarjeta:

**Estado de impresoras**

Cada impresora Operativa muestra, como mínimo:

- nombre;
- estado;
- `hojas disponibles estimadas / capacidad máxima`;
- porcentaje estimado;
- última recarga registrada;
- advertencia si la disponibilidad es baja;
- acción `Registrar recarga`.

Ejemplo:

`HP LaserJet 4200`

`Operativa · 468 / 500 hojas · 94 % estimado`

`Última recarga: hoy 08:05`

`[ Registrar recarga ]`

El bloque puede incluir `Ver todas las impresoras` para abrir un panel operativo más amplio sin entrar al motor de configuración.

### Ruta rápida

`Dashboard → Estado de impresoras → Registrar recarga`

### Ruta secundaria

`Dashboard → Estado de impresoras → Ver todas las impresoras → Seleccionar impresora → Registrar recarga`

La ruta secundaria sirve cuando existen muchas impresoras y no resulta conveniente mostrar todas directamente en el dashboard.

## 6. Flujo de Registrar recarga

1. El usuario interno accede al dashboard.
2. Visualiza el bloque Estado de impresoras.
3. Identifica la impresora recargada físicamente.
4. El sistema muestra disponibilidad estimada actual y faltante hasta el máximo.
5. El usuario completa físicamente el papel hasta la capacidad máxima configurada.
6. Presiona Registrar recarga.
7. Se abre una confirmación con capacidad máxima, disponibilidad anterior y cantidad faltante calculada.
8. El usuario confirma que completó físicamente la impresora hasta el máximo.
9. El sistema restablece la disponibilidad estimada al 100 %.
10. El contador histórico de hojas impresas no se modifica.
11. Se registra usuario, fecha, hora e impresora.
12. El dashboard refleja inmediatamente el nuevo valor.

### Ejemplo

Antes:

`468 / 500 hojas · 94 % estimado`

Faltante calculado:

`32 hojas`

Después de la confirmación:

`500 / 500 hojas · 100 % estimado`

## 7. Recomendación de inicio de jornada

Como práctica operativa recomendada:

1. al comenzar la jornada, el personal verifica físicamente las impresoras;
2. completa cada bandeja hasta la capacidad máxima configurada cuando sea necesario;
3. registra la recarga correspondiente en el sistema;
4. el dashboard queda sincronizado antes de comenzar a recibir nuevos trabajos.

Si una recarga ocurre durante la jornada, se aplica la misma regla: **la reposición debe completarse hasta el 100 % antes de registrar el evento**.

No se registran recargas parciales en V1.

## 8. Acceso al panel de asignación de impresora

El panel de recomendación no se abre desde Configuración.

Se invoca cuando existe un trabajo concreto listo para ser asignado a producción.

### Ruta

`Pedido/Trabajo → Listo para producción → Asignar impresora`

### Comportamiento

1. el sistema conoce las características del trabajo;
2. evalúa formato, B/N o color y demás capacidades aplicables;
3. consulta solamente impresoras Operativas como candidatas seleccionables;
4. muestra incompatibles cuando resulte útil para explicar la decisión;
5. muestra disponibilidad estimada de papel;
6. puede destacar una impresora como Recomendada;
7. el usuario selecciona manualmente la impresora final;
8. el backend revalida compatibilidad antes de registrar la asignación.

En V1 la recomendación no asigna automáticamente.

## 9. Acceso al método de asignación

### Ruta

`Dashboard → Configuración → Motor de configuración → Fase 4 → Método de asignación`

### V1

- Asignación manual: seleccionada por defecto y disponible.
- Asignación automática: visible pero deshabilitada, identificada como futura.

Cambiar este criterio es una decisión de configuración y por eso permanece dentro del motor versionado.

Asignar una impresora a un trabajo concreto, en cambio, es una acción operativa y ocurre fuera del asistente.

## 10. Separación conceptual de los paneles

### A. Paneles de configuración

Acceso restringido al motor de configuración.

Incluyen:

- listado/configuración de impresoras;
- alta;
- deshabilitación;
- edición;
- eliminación;
- capacidad máxima;
- método de asignación.

### B. Paneles operativos

Accesibles durante la jornada sin editar una versión de configuración.

Incluyen:

- estado de impresoras;
- disponibilidad estimada;
- última recarga;
- registrar recarga;
- compatibilidad para un trabajo;
- recomendación;
- asignación manual.

Una recarga o una asignación no crea una nueva versión de configuración.

## 11. Criterios de aceptación de navegación

- Registrar recarga es accesible sin entrar a Configuración.
- El dashboard muestra una entrada directa al estado operativo de impresoras.
- El usuario no necesita esperar a asignar un trabajo para registrar una recarga.
- El panel operativo no permite modificar capacidades de la impresora.
- Editar capacidades exige ingresar a Configuración y que la impresora esté Deshabilitada.
- El panel de asignación se abre desde el trabajo concreto, no desde el editor de impresoras.
- El método de asignación se configura en Fase 4, pero la asignación concreta ocurre en operación.
- Recarga y asignación se registran como eventos operativos y no generan una versión de configuración.

## 12. Evidencia visual asociada

Los mockups de Fase 4 representan cuatro conceptos complementarios:

1. listado de impresoras y capacidades;
2. edición de una impresora Deshabilitada;
3. selección manual con compatibilidad y recomendación;
4. registro y confirmación de recarga de papel.

Para la integración visual definitiva, el mockup de recarga debe entenderse como **panel operativo accesible desde el dashboard**, aunque forme parte del análisis funcional realizado durante la Fase 4.

## 13. Referencias

- `diseño/Front/ux-ui/wireflows/WF-ACTUALIZACION-A-REVISION-ADMINISTRADOR-PRODUCTO-CONFIGURABLE.md`
- `analisis/casos-de-uso/10-disponibilidad-operativa/actualizacion-a-revision-casos-de-uso-disponibilidad.md`
- `analisis/especificacion-de-requerimientos/actualizacion-a-revision-reglas-y-requerimientos-configurables.md`
- `marco-del-proyecto/actualizacion-a-revision-trazabilidad-producto-configurable.md`
- Issue #214 - Fase 4 - Impresoras, capacidades y asignación
