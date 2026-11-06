# La Montaña Mobile

Base nativa Android de la rama `android`. No registra ni administra usuarios: autentica las cuentas creadas desde la Web y abre la experiencia permitida por el backend.

## Estado v0.1

- Login real contra `POST /api/auth/login` y consulta de `GET /api/auth/me`.
- Enrutamiento por perfil: mostrador del punto, recorridos del rider o cuenta sin acceso a Android.
- Mostrador mobile con cabecera, contadores y menú **Por llegar / Para retirar / Entregados / Finanzas**.
- Contexto real del punto mediante `GET /api/punto/contexto` y primeras bandejas reales mediante `POST /api/punto/pedidos/buscar` con sesión y CSRF.
- Cliente HTTP con sesión por cookie y CSRF para registrar movimientos.
- Las tarjetas del `@Preview` usan ejemplos exclusivamente visuales; al ejecutar la app, las bandejas y la búsqueda muestran la respuesta real del backend.
- El rider consulta sus recorridos y selecciona los pedidos que dejó en cada punto. La presentación deja el lote pendiente de recepción.
- En **Por llegar**, el punto confirma la recepción física de los pedidos presentados. Después aparecen en **Para retirar**.
- La ficha del punto muestra el estado, los trabajos, la franja y el saldo. Permite entregar con código y, cuando el servidor lo habilita, cobrar efectivo y entregar en una sola operación.
- Los movimientos de finanzas y el registro de transferencias se gestionan desde el mostrador web.

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
