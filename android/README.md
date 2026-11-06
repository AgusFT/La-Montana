# La Montaña Mobile

Base nativa Android de la rama `android`. No registra ni administra usuarios: autentica las cuentas creadas desde la Web y abre la experiencia permitida por el backend.

## Estado v0.1

- Login real contra `POST /api/auth/login` y consulta de `GET /api/auth/me`.
- Enrutamiento por perfil: cuenta de punto, empleado pendiente de contrato rider o cuenta no habilitada.
- Mostrador mobile con cabecera, contadores y menú **Por llegar / Para retirar / Entregados / Finanzas**.
- Contexto real del punto mediante `GET /api/punto/contexto` y primeras bandejas reales mediante `POST /api/punto/pedidos/buscar` con sesión y CSRF.
- Cliente HTTP con sesión por cookie y preparación CSRF para las próximas mutaciones.
- Las tarjetas del `@Preview` usan ejemplos exclusivamente visuales; al ejecutar la app, las bandejas y la búsqueda muestran la respuesta real del backend.
- Recepción, detalle, entrega/cobro y finanzas se conectarán en el siguiente incremento.
- Rider no se simula: la pantalla informa que falta la asignación individual de recorridos en el backend.
- La composición `RiderRouteScreen` y su `@Preview` muestran el recorrido asignado, pedidos agrupados por parada y direcciones. La referencia exportada está en `docs/mockups/android-v0-rider-recorrido.svg`; todavía no consume recorridos reales.

## Abrir en Android Studio

Abrir la carpeta `android/`. El proyecto usa Java 17, Android Gradle Plugin 9.5, Kotlin 2.4.10, `compileSdk 37` y Compose BOM `2026.09.00`.

El desarrollo Android usa de forma predeterminada el entorno `pruebas`: la Web se abre en `http://localhost:3100` y su backend se publica en `http://localhost:8180`. Desde el emulador, la PC anfitriona se alcanza como `10.0.2.2`, por lo que la app utiliza `http://10.0.2.2:8180`.

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
