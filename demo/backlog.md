# Backlog de la Demo para Windows

La [épica E23](https://github.com/AgusFT/La-Montana/issues/229) agrupa diez tareas
con una estimación total de 16 horas. Los criterios de la tabla indican qué
comprobar antes de cerrar cada tarea. Las referencias de commit y los resultados
se completan a medida que se validan las etapas.

| Etapa | Tarea | Estimación | Dependencia | Criterio de aceptación | Commit y evidencia |
| --- | --- | ---: | --- | --- | --- |
| 1. Base y documentación | [#230](https://github.com/AgusFT/La-Montana/issues/230) Base e identidad de Demo | 1 h | Ninguna | Cotejar el árbol importado, reproducir la compilación y comprobar que Demo tiene identidad y recursos propios y rechaza una base de datos de otra edición. | `f46976ec` incorpora de `desarrollo` el árbol exacto de `e0cb6f8d`, sobre la raíz `b9f49860` de `Demo`. Árbol cotejado. `d5440d3f` separa los recursos locales y la web. `f2857510` y `c73aa223` comprueban el origen de la base desde su creación. Ambas ediciones compilan y arrancan en paralelo. Las pruebas con los dos programas conservan la identidad en el reinicio y las huellas del esquema y de los datos tras cada rechazo cruzado. |
| 1. Base y documentación | [#231](https://github.com/AgusFT/La-Montana/issues/231) Documentación de Demo | 1 h | Ninguna | Reunir el recorrido del visitante, el backlog de diez tareas y la guía de distribución. | [Recorrido](README.md), backlog y [guía de distribución](distribucion.md) redactados. Publicado en `844a6098` de `main`. |
| 2. Imprenta ficticia y roles | [#232](https://github.com/AgusFT/La-Montana/issues/232) Precarga e impresión simulada | 2 h | #230 | Inventariar la precarga, comprobar que deja cupos disponibles y que reiniciar conserva una sola copia de cada dato. Recorrer un pedido hasta la impresión simulada. | `b9e955d1` precarga un pedido, un cliente, un cobro y una impresora. En la prueba se abrió el PDF, se completó el trabajo y se aprobó el control de calidad. El reinicio conservó el historial y el inventario. |
| 2. Imprenta ficticia y roles | [#233](https://github.com/AgusFT/La-Montana/issues/233) Selector de roles | 1 h | #232 | Entrar sin registro ni contraseña y comprobar las sesiones y permisos de administrador, empleado y cliente. | `480d7497` implementa la sesión y `c21d7565` el selector. Se verificaron las tres entradas sin credenciales, la renovación de sesión, el cierre y los permisos de cada rol. |
| 3. Cupos y bajas | [#234](https://github.com/AgusFT/La-Montana/issues/234) Cupos transaccionales y baja lógica | 3 h | #232 | Por cada cupo, verificar el último lugar, el primer exceso, dos altas simultáneas por un lugar, el reintento, la reversión y el alta después de una baja. Ejecutar las mutaciones desde la interfaz y por API, verificar el cómputo de caja y la conservación de vínculos e historial. | `97babc27` reserva los lugares y conserva las bajas. `c8cbc25a` incorpora la operación por rol, `4de97b19` reserva la ficha del selector y `3c2f2a06` incorpora los controles en la web. La matriz de PostgreSQL y HTTP está en `362a85f4`. |
| 4. Exclusiones y vencimiento | [#235](https://github.com/AgusFT/La-Montana/issues/235) Módulos de presentación | 1 h | #230, #233 | Verificar los módulos en gris, las etiquetas prémium y la ausencia de las rutas excluidas mediante inventario y llamadas directas. Conservar capturas de la interfaz. | Pendiente. |
| 4. Exclusiones y vencimiento | [#236](https://github.com/AgusFT/La-Montana/issues/236) Vigencia sin conexión | 2 h | #230, #233 | Comprobar el banner en todas las vistas, que el servidor permite operar a las 359 h 59 min 59 s y que bloquea a las 360 h. Probar sesión abierta, reloj atrasado y corregido, reinicio y red desconectada. Verificar el mensaje de vencimiento y la integridad de los archivos. | Pendiente. |
| 5. Instalador y prueba Windows | [#237](https://github.com/AgusFT/La-Montana/issues/237) Paquete para Windows 11 x64 | 2 h | #234, #235, #236 | Construir el `.exe` con identidad de instalación y actualización, directorios y volúmenes propios de Demo. Registrar versión, tamaño y SHA-256. | Pendiente. |
| 5. Instalador y prueba Windows | [#238](https://github.com/AgusFT/La-Montana/issues/238) Validación del paquete | 2 h | #237 | En Windows 11 x64, instalar en una máquina sin La Montaña, operar sin conexión, reparar, desinstalar y reinstalar. Verificar el plazo original y el aislamiento de una instalación del producto completo. Registrar las pruebas de Windows y la regresión del servidor, la interfaz y el instalador. | Pendiente. |
| 6. Distribución | [#239](https://github.com/AgusFT/La-Montana/issues/239) Descarga pública | 1 h | #231, #238 | Enlazar el archivo versionado desde YG Soluciones. Descargarlo de forma anónima y comparar su tamaño y SHA-256 con el artefacto probado. Verificar los permisos de las entregas privadas y documentar URL, fecha y retiro. | Pendiente. |

## Verificación de los cupos

Cada fila se ejercitó con dos transacciones independientes que compitieron por
el último lugar. En cada caso se probó una operación revertida, el reintento
del alta aceptada, el rechazo por exceso y el alta posterior a una baja
repetida. [Tarea #234](https://github.com/AgusFT/La-Montana/issues/234)

| Recurso | Último lugar | Primer exceso | Comprobación adicional |
| --- | --- | --- | --- |
| Pedidos | 19 → 20 | Pedido 21 | Cancelar mantiene el lugar. La baja conserva el pedido y su historial. |
| Clientes | 9 → 10 | Ficha 11 | Lucía Papel sigue disponible en el selector. El empleado con permiso de cobro puede crear fichas. La baja de las demás conserva sus antecedentes. |
| Caja | 29 → 30 | Movimiento 31 | Cobros y devoluciones comparten el contador. Aplicar saldo lo mantiene. La baja conserva el saldo y los vínculos. |
| Impresoras | 1 → 2 | Impresora 3 | Copiar la configuración conserva la identidad. Un equipo dado de baja queda fuera de la selección para trabajos nuevos. |

En la prueba de actualización se conservaron las doce fichas de una instalación
anterior. Las primeras dos bajas la llevaron a su máximo de diez. La tercera
dejó disponible un lugar para un alta nueva.

En Chromium, los formularios de confirmación de pedido, alta de ficha, cobro
en efectivo y alta de impresora mostraron el motivo del rechazo. Después de
liberar un lugar desde el panel se pudo completar cada alta. La API rechazó
la baja de Lucía y el selector siguió abriendo su pedido de muestra.
