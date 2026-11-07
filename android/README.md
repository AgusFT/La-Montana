# La Montaña Mobile

Base nativa Android de la rama `android`. No registra ni administra usuarios: autentica las cuentas creadas desde la Web y abre la experiencia permitida por el backend.

## Estado v0.1

- Login real contra `POST /api/auth/login` y consulta de `GET /api/auth/me`.
- Enrutamiento por perfil: mostrador del punto, recorridos del rider o mostrador de sucursal para administradores y empleados autorizados.
- Mostrador mobile con cabecera, contadores y menú **Por llegar / Para retirar / Entregados / Finanzas**.
- Contexto real del punto mediante `GET /api/punto/contexto` y primeras bandejas reales mediante `POST /api/punto/pedidos/buscar` con sesión y CSRF.
- Cliente HTTP con sesión por cookie y CSRF para registrar movimientos.
- Las tarjetas del `@Preview` usan ejemplos exclusivamente visuales; al ejecutar la app, las bandejas y la búsqueda muestran la respuesta real del backend.
- El rider consulta sus recorridos y selecciona los pedidos que dejó en cada punto. La presentación deja el lote pendiente de recepción.
- Reparto tiene **Actuales / Puntos / A domicilio / Entregados** y búsqueda por cliente, destino o número. Las tres primeras vistas muestran sólo pendientes, incluso si vienen de días anteriores. Los domicilios aparecen primero, ordenados por el fin de su franja de entrega.
- **Entregados** muestra las finalizaciones del día de la sucursal. En puntos y sucursales cuenta la recepción física; en domicilios, la entrega al cliente. Una presentación todavía sin aceptar permanece pendiente. Las paradas mixtas separan los pedidos recibidos de los que faltan.
- La jornada se consulta con `GET /api/rider/recorridos?jornada=true`: conserva la asignación y los permisos del rider, deja fuera los completados de otros días y usa la hora de la sucursal en lugar del reloj del teléfono. Las listas se renderizan a medida que se recorren.
- En **Por llegar**, el punto confirma la recepción física de los pedidos presentados. Después aparecen en **Para retirar**.
- La ficha del punto muestra el estado, los trabajos, la franja y el saldo. Permite entregar con código y, cuando el servidor lo habilita, cobrar efectivo y entregar en una sola operación.
- El mostrador de sucursal tiene **Para retirar / En camino / Entregados**, búsqueda por cliente o número y selector limitado a las sucursales autorizadas. Se habilita con `GESTIONAR_ENTREGAS`.
- Desde la ficha permite confirmar la llegada, registrar el medio real del saldo (efectivo o transferencia), cobrar efectivo y entregar juntos cuando el servidor lo habilita, y entregar pedidos ya abonados. Todo usa los mismos servicios, permisos y auditoría que la web.
- Las transferencias se confirman después de verificar la acreditación e indicar la referencia. Si coincide con un aviso pendiente del cliente por referencia e importe, se acredita ese aviso. No vuelve a cobrar dinero que ya está recibido sin aplicar.
- El retiro de un invitado en la misma sucursal verifica nombre y número; un invitado con traslado y los clientes registrados presentan su código. Las confirmaciones de llegada y entrega conservan la versión del pedido.
- **Caja** muestra el efectivo acumulado y los movimientos del día para el responsable de esa sucursal o el administrador. Los egresos, arqueos y comprobantes se administran desde la web.
- Si se pierde una respuesta al cobrar o entregar, **Recuperar registro** conserva exactamente la misma operación y bloquea cambios hasta conocer el resultado.
- El administrador abre **Sucursales · Mostrador**, incluso si también es rider. El menú de la cabecera alterna entre esa vista, **Mis repartos**, **Impresoras** y **Pausas de atención**. Mis repartos conserva únicamente sus asignaciones y requiere que su usuario esté habilitado como rider.
- En **Impresoras** puede buscar por equipo o sucursal, filtrar por ubicación y habilitar o deshabilitar equipos para trabajos nuevos. Se respetan el estado configurado, los equipos retirados, el papel registrado y las versiones del servidor.
- En **Pausas de atención**, las pestañas **Sucursales / Puntos de entrega** impiden nuevas cotizaciones y confirmaciones en esas ubicaciones. Reanudar recupera su disponibilidad comercial; la pausa no desactiva la sucursal ni bloquea la producción, cobro o entrega de pedidos confirmados. Los cambios guardan responsable y fecha.
- Las pausas de puntos y los controles de impresoras comparten los servicios de la web. La pausa temporal de sucursales requiere la migración `V62` del backend de `pruebas`; no reemplaza la baja lógica de una sucursal. Una respuesta perdida conserva el mismo comando y ofrece **Recuperar el mismo cambio**.

## Abrir en Android Studio

Abrir la carpeta `android/`. El proyecto usa Java 17, Android Gradle Plugin 9.5, Kotlin 2.4.10, `compileSdk 37` y Compose BOM `2026.09.00`.

El desarrollo Android usa de forma predeterminada el entorno `pruebas`: la Web se abre en `http://localhost:3100` y su backend se publica en `http://localhost:8180`. Desde el emulador, la PC anfitriona se alcanza como `10.0.2.2`, por lo que la app utiliza `http://10.0.2.2:8180`.

Para probar la recepción de lotes, levantá primero la rama `pruebas` actualizada desde la copia web.

La URL puede reemplazarse en `~/.gradle/gradle.properties` cuando sea necesario apuntar a otro entorno:

```properties
LAMONTANA_API_URL=http://10.0.2.2:8180
```

El tráfico HTTP sin cifrar sólo está habilitado en `debug`; `release` exige HTTPS. La primera sincronización desde Android Studio descargará Gradle y las dependencias. El repositorio incorporará el wrapper cuando se genere desde una instalación local de Android Studio/Gradle compatible.

## Límites deliberados

- No hay registro, recuperación ni alta de empleados desde Android.
- El cambio obligatorio de contraseña inicial se completa por Web.
- No se concede el perfil rider por asumir que un empleado lo es.
- No se muestran como reales pedidos ficticios del `@Preview`.
- Entregas, cobros y cambios de estado nunca mostrarán éxito sin confirmación del servidor.

Referencia funcional: [`docs/mobile-android-alcance-y-flujos.md`](../docs/mobile-android-alcance-y-flujos.md).
