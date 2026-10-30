# Selección de impresora y operación diaria

Esta entrega retoma la distinción entre capacidades del motor y disponibilidad cotidiana. Complementa `correcciones-fases-4-y-5.md`, donde las recargas todavía figuraban pendientes, y `operacion-pedido-por-tareas.md`. Atiende el acceso rápido y la reducción de sobrecarga visual de RNF-USA-005, con hechos auditables según RF-AUD.

## Accesos

El menú prioriza Pedidos y tareas, **Impresoras y papel** y **Puntos de entrega**. El configurador conserva el alta, capacidades, formatos y condiciones versionadas.

| Pantalla | Administrador | Empleado |
| --- | --- | --- |
| Impresoras y papel | `/administracion/impresoras` | `/operacion/impresoras` |
| Disponibilidad de puntos | `/administracion/puntos-entrega` | `/operacion/puntos-entrega` |

`GESTIONAR_IMPRESORAS` permite los cambios cotidianos y el papel; `GESTIONAR_PRODUCCION` permite consultar los equipos e iniciar los trabajos autorizados. `GESTIONAR_PUNTOS_ENTREGA` permite consultar y cambiar la disponibilidad de puntos. Los permisos tienen alcance global: las impresoras pertenecen a la imprenta, y el destino del cliente no decide dónde producir.

El administrador dispone de los permisos activos. A empleados existentes se los asigna desde Empleados: la migración **no concede permisos nuevos automáticamente**. Una instalación nueva del escenario de pruebas los incluye en Producción. No hace falta reiniciar datos para actualizar una instalación existente.

## Selección por tarjetas

Preparar producción muestra el PDF, sus hojas requeridas incluyendo copias y doble faz, y las impresoras de la configuración efectiva del pedido. Cada tarjeta incluye ilustración genérica orientativa, formatos, color, doble faz, capacidad, estado diario y papel registrado. No utiliza especificaciones ficticias del mockup ni fotografías que afirmen un modelo diferente.

Los equipos incompatibles permanecen visibles con sus causas y sin selección. Se vuelve a comprobar la disponibilidad al abrir/actualizar y antes de iniciar. El servidor valida formato, color, doble faz, capacidad declarada, estado configurado, retiro y estado diario. Si se conoce el papel y no alcanza, exige recarga/conteo o elegir otro equipo. Si nunca se informó papel, lo dice expresamente y conserva el inicio manual: no interpreta ausencia como cero ni como bandeja llena.

Se recomienda un único equipo entre los utilizables: primero papel suficiente conocido; después la menor capacidad suficiente; desempate estable por nombre e identidad. Se explica el criterio, sin inventar velocidad, cola, estado Online ni detección física.

Las observaciones normales están plegadas y son opcionales. El botón Iniciar producción confirma el hecho manual; el servidor conserva su confirmación, permisos, versión, anticipo acreditado/aplicado y verificación del PDF. El recurso fuera del inventario queda como excepción secundaria con descripción obligatoria.

## Papel y estado diario

La migración V45 agrega estado y eventos por identidad de impresora, separados del snapshot del motor. Sin registro previo, la habilitación cotidiana no impone otro bloqueo sobre un equipo declarado operativo y el papel permanece **sin informar**.

- Informar papel guarda un conteo observado, incluido cero.
- Recargar papel suma únicamente las hojas agregadas y requiere un conteo anterior conocido.
- La ventana ofrece **Llenar por completo**, **1 resma (500 hojas)** y **Otra cantidad**. Llenar calcula sólo la diferencia hasta la capacidad declarada (1.988 → 2.000 agrega 12); queda deshabilitado si el saldo ya alcanza o supera esa capacidad. Resma y cantidad manual suman lo efectivamente agregado. Se muestra el saldo resultante antes de confirmar. La capacidad es la declarada en el motor, no una lectura ni una especificación física obtenida del equipo.
- Las opciones rápidas usan el mismo registro RECARGAR, versión e identificador estable: una recuperación incierta no vuelve a sumar. El primer conteo y las correcciones siguen siendo cantidades observadas, sin inventar el saldo anterior.
- Iniciar un trabajo asigna sus hojas previstas y las descuenta del saldo registrado si se conoce. Se guarda ASIGNAR_TRABAJO con vínculo al trabajo y su responsable, en la misma transacción del inicio.
- El saldo es una **estimación manual**, no una lectura física, un inventario por gramaje/formato ni una señal del equipo. El operador comprueba el papel apropiado para el trabajo.
- Error/cancelación no devuelven papel automáticamente: puede haberse utilizado o descartado. Corregir conteo permite registrar el remanente físico; reimpresión asigna nuevas hojas.
- Deshabilitar bloquea nuevos inicios sin cancelar los que ya están en curso ni reescribir configuraciones. Habilitar no convierte en operativo un recurso que el motor no admite.

Las escrituras comparten el orden de bloqueos de producción, comprueban versión y usan identificadores de operación. Reintentar no repite una recarga/asignación; reutilizar una operación con otro contenido genera conflicto. Un fallo de auditoría o de inicio revierte el cambio dentro de la misma transacción. La base también rechaza iniciar trabajos en una impresora deshabilitada.

## Puntos de entrega e historial

Se reutiliza la disponibilidad por identidad existente. Habilitar/deshabilitar es inmediato, sin editar el motor. Se verifica en cotización, confirmación y aceptación de reprogramaciones; no cancela ni cambia pedidos ya confirmados. Cada tarjeta ofrece historial paginado con actor y fecha. La migración protege también esos eventos frente a actualización/borrado.

Las impresoras tienen historial paginado de conteos, recargas, habilitaciones y asignaciones, con actor/rol capturados, fecha, cantidades y valores anteriores/nuevos. Los formularios bloquean navegación durante respuestas inciertas y recuperan la misma operación.

## Validación

- TypeScript y build web; pruebas de contratos de impresoras y tarea operativa.
- Resultado local: 20 pruebas backend (servicios y cadena de seguridad) y 16 web aprobadas. Navegador con componentes reales y API simulada: selección/inicio, recarga con respuesta incierta y recuperación, deshabilitación, historial y punto de entrega. A 320 px, el diálogo y los puntos no desbordan horizontalmente. La evidencia visual usa datos ficticios.
- Ocho pruebas de OperacionImpresoraServiceTest con JDBC simulado: ausencia de stock, conteos/recargas, auditoría, idempotencia, conflictos, asignación, deshabilitación, permisos y datos inválidos.
- Cuatro pruebas de SeleccionImpresoraServiceTest con colaboradores simulados: todos los equipos y motivos, criterio de recomendación, rechazo de selección deshabilitada/papel insuficiente y bloqueo económico.
- Cadena real de Spring Security en RecuperacionImpresorasSecurityTest: roles internos, cliente rechazado, métodos y CSRF, además de sus regresiones existentes.
- `DatosPruebaIntegrationTest#operacionDiariaDeImpresorasYPuntosNoReescribeElMotor` cubre V45, inicio y asignación idempotentes, rollback ante fallo de auditoría, permisos, historial de puntos y conservación de configuraciones. Compiló; su ejecución con PostgreSQL real queda pendiente en este entorno.

```bash
cd backend
mvn -Dtest=OperacionImpresoraServiceTest,SeleccionImpresoraServiceTest,RecuperacionImpresorasSecurityTest,DatosPruebaIntegrationTest#operacionDiariaDeImpresorasYPuntosNoReescribeElMotor test
```

No se implementa CUPS ni envío físico en esta entrega. Las tarjetas y las acciones diarias funcionan con los datos declarados y registrados por las personas.
