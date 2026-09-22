# La Montaña · v0.1

Piloto local en construcción con Spring Boot, Next.js y PostgreSQL. Esta rama `desarrollo` reemplaza la implementación deprecada; el código anterior sigue disponible en el historial Git.

## Estado actual · entrega 6

Implementado: base técnica de la entrega 1 más alta única del propietario, credenciales persistentes, login/logout y áreas administrativa, de cliente y de operación con sesiones reales, registro de particulares y redirección según rol. También funcionan la verificación por correo local, recuperación de acceso y cambio de contraseña. Compose incluye base, backend, frontend y buzón local Mailpit.

**Ya se puede crear al propietario, registrar particulares, iniciar/cerrar sesiones y gestionar sucursales y empleados desde administración.** El empleado ingresa a su espacio y consulta únicamente sus sucursales habilitadas. Horarios/capacidades, configuración comercial, pedidos, pagos, PDF y producción/entrega siguen **En construcción**. No hay cuentas ni datos comerciales precargados. V1 prepara el esquema; V2 agrega identidad, el rol técnico de administrador, el registro único de inicialización, eventos de acceso y tablas técnicas de sesiones. V3 incorpora el rol técnico CLIENTE para particulares, sin crear cuentas.

## Arranque local

Requisitos: Docker Engine y Docker Compose accesibles desde la terminal, acceso a Internet para descargar imágenes/dependencias y puertos disponibles.

Desde esta carpeta:

```bash
./infra/iniciar-local.sh
```

El script comprueba Docker, crea `.env` si no existe con una contraseña técnica aleatoria para PostgreSQL y ejecuta `docker compose up --build --detach --wait`. Esa contraseña no corresponde a un usuario del programa. El archivo es privado, se ignora en Git y se conserva entre ejecuciones.

Puertos predeterminados, publicados solo en la máquina local:

| Servicio | Acceso |
|---|---|
| Aplicación | http://localhost:3000 |
| Estado del backend | http://localhost:8080/api/sistema/estado |
| Disponibilidad con conexión a base | http://localhost:8080/actuator/health/readiness |
| Buzón de correo local | http://localhost:8025 |
| PostgreSQL | localhost:5432 · base y usuario técnicos `lamontana` |

Mailpit recibe los códigos de verificación y recuperación enviados por el backend mediante SMTP `mailpit:1025`. Abrir el buzón local para leerlos; no se envían a proveedores de correo externos. Los códigos de activación de configuración siguen pendientes.

Si hay puertos ocupados, agregar `FRONTEND_PORT`, `BACKEND_PORT`, `POSTGRES_PORT` o `MAILPIT_PORT` a `.env`; `.env.example` muestra las opciones. Los puertos internos no cambian. No reemplazar la contraseña si ya existe un volumen de PostgreSQL inicializado: editar el entorno no cambia la contraseña guardada en la base.

```bash
docker compose ps
docker compose logs --tail=100 backend frontend
docker compose stop
docker compose up --detach --wait
```

Base, archivos y correo utilizan volúmenes nombrados y persisten al reiniciar o detener. `docker compose down` conserva esos volúmenes; `docker compose down --volumes` elimina los datos y no es un paso normal de arranque.

Si el script informa que Docker no está accesible, comprobar el servicio y los permisos de la terminal. En la sesión de implementación el socket no fue accesible, incluso fuera del sandbox: por eso no se afirma que los contenedores estén levantados ni que su persistencia haya sido probada. No se cambiaron permisos del equipo para sortear ese problema.

## Estructura y comprobaciones

- `backend/`: Java 17, Spring Boot 4.1.1, Spring Security, JPA y Flyway. PostgreSQL 18.6. Se usa `ddl-auto: validate`; únicamente las migraciones cambian el esquema.
- `frontend/`: Next.js 16.3.5, React 19.3.0, TypeScript y pnpm. App Router, salida standalone y lectura del estado desde el servidor sin caché.
- `infra/` y `compose.yaml`: arranque, dependencias, salud y volúmenes locales. Backend/frontend ejecutan como usuarios sin privilegios dentro de sus imágenes.

```bash
# Java 17 y Maven 3.9.x; inicia un PostgreSQL 18.6 temporal exclusivo de las pruebas.
mvn -f backend/pom.xml verify

# Node 22 o 24 y pnpm 11.19.0
cd frontend
pnpm install --frozen-lockfile
pnpm typecheck
pnpm build
```

Las pruebas del backend usan binarios de PostgreSQL reales mediante una dependencia de test, sin requerir Docker. No usan H2 ni la base de la demo. Ejecutarlas como usuario normal en un sistema compatible con esos binarios. Las primeras descargas requieren Internet.

Validación histórica de la entrega 1: Maven `verify` pasó 2 pruebas integradas contra PostgreSQL 18.6; `pnpm typecheck` y `pnpm build` pasaron con Node 24.19; el servidor standalone respondió HTTP 200 en `/health` y mostró el aviso correcto en `/` sin backend. Compose pasó `config --quiet` y el script pasó validación de sintaxis. En esa entrega no se verificaron las imágenes Docker, el arranque conjunto, la persistencia de volúmenes ni las vistas en navegador; los checkpoints siguientes registran lo comprobado después. El build usa la API de TypeScript 5 mediante una opción soportada de Next; no se omitió la comprobación de tipos.

Para desarrollar sin contenedores, proporcionar `DB_URL`, `DB_USER`, `DB_PASSWORD` y opcionalmente `FILES_DIR` al backend, apuntando a un PostgreSQL propio. El frontend requiere `BACKEND_INTERNAL_URL` y se inicia con `pnpm dev`. Los directorios de archivos no se publican como recursos web. Son públicos el estado, el token CSRF y el recorrido inicial de alta/login; `/api/auth/me` exige sesión y `/api/admin/estado` exige rol administrativo. Las demás rutas de negocio permanecen cerradas. No existe usuario automático de Spring.

## Primer acceso y comprobación manual

1. Ejecutar el script de arranque. Además de la credencial de base, genera `SETUP_TOKEN` aleatorio en `.env` si falta. Abrir ese archivo **localmente** y copiar únicamente el token al formulario; no publicarlo ni adjuntarlo a Git.
2. Entrar a `/instalacion` e ingresar token, nombre, apellido, correo y una contraseña propia de 12 a 128 caracteres. No hay datos predeterminados. El token requiere al menos 32 caracteres al configurarlo manualmente; si falta o es insuficiente, el alta queda deshabilitada.
3. Tras crear al propietario, entrar a `/acceso` con el correo y contraseña elegidos. El alta no inicia sesión automáticamente. `/administracion` muestra la identidad real y las funciones todavía en construcción.
4. Recargar o reiniciar el backend y comprobar que la sesión continúa. Cerrar sesión y comprobar que volver a administración requiere autenticarse. El alta inicial permanece cerrada, incluso tras reiniciar o conservar el token antiguo.

Las contraseñas se guardan con PBKDF2 y sal aleatoria; el correo se compara sin distinguir mayúsculas. El correo comienza sin verificar y sólo cambia al confirmar un código recibido en el buzón local. Las sesiones se guardan mediante Spring Session JDBC, vencen tras 30 minutos de inactividad y usan cookies HttpOnly/SameSite=Lax. Cada POST requiere un token CSRF obtenido para esa sesión, renovado tras login/logout. El backend renueva el identificador al autenticar e invalida la sesión al salir.

El token de instalación habilita una única alta, bloqueada transaccionalmente ante concurrencia. No habilita un segundo propietario si luego se desactiva la cuenta. Se registran alta, login correcto/fallido y logout sin guardar contraseñas ni tokens en esos eventos. El piloto aplica un máximo global de 30 solicitudes de alta/login/registro/correo/cambio de contraseña por minuto y proceso; ese contador se reinicia con el backend. Los códigos tienen además límites persistentes de reenvío e intentos.

El proxy Next admite solo las rutas declaradas de identidad y organización, conserva las cookies y no almacena credenciales en el navegador. `SETUP_TOKEN` es configuración privada del backend y no se envía al frontend automáticamente. En ejecución manual del backend, definir también esa variable para habilitar el primer acceso.

Como adaptación técnica del DER, esta etapa usa sesiones HTTP persistentes de Spring Session; no implementa un circuito paralelo de refresh tokens. El rol técnico de administrador conserva el código ADMIN_ADMIN utilizado desde la entrega 2; el empleado usa EMPLEADO. Las migraciones V3–V6 extienden usuarios, sucursales, permisos y credenciales temporales. Configuración y datos comerciales permanecen vacíos.

## Decisiones que se mantienen para completar la demo

- Una instalación/base por imprenta, varias sucursales reales y empleados asignados. Políticas y catálogo globales, servicios/capacidades habilitados por sucursal y permisos financieros explícitos.
- Solo particulares y PDF. El primer propietario creará sus credenciales y configurará el sistema desde cero; sin precios, sucursales, reglas ni plantillas de fábrica. No incorporar la regla histórica de 200 carillas/30%.
- Seguir los mocks de Alejandro: asistente de configuración de fases 1–8, modelos manual/condicional y catálogo con revisión comercial independiente. Programación, historial, comparación y usar como base forman parte del objetivo.
- Una cotización vigente conserva el precio; al confirmar se revalidan las condiciones actuales. Registrar versión operativa cotizada, revisión comercial y versión de validación final.
- **D1:** en la variante por monto superior al umbral, revisar PDF/datos, pedir correcciones o aprobar antes de exigir la seña configurada para producir. Pago previo/seña puros conservan el bloqueo de upload hasta acreditar.
- **D2:** conservar un pago tardío y permitir aplicación autorizada a una nueva cotización aceptada, mostrando diferencias. Si no continúa, registrar devolución. No crear un pedido automáticamente ni cobrar de nuevo lo recibido.
- Producción manual, correcciones, calidad/reimpresión, cancelaciones y entrega/cobro completos. Retiro local: **Listo para entregar**. **En viaje** solo para traslado a punto o domicilio; llegada y entrega efectiva son hechos diferentes.
- Reclamos posteriores, empresas/cuenta corriente, conversión DOCX/ODT, CUPS/agentes, mobile, pasarelas y seriales/licencias: **En construcción**. D2 y las correcciones operativas no pertenecen a los reclamos diferidos.

Referencia funcional: DER y flujos de `main` en `3983058e`, más contratos/mocks de `docs/actualizacion-producto-configurable-revision` en `56f89d40`. Las decisiones del usuario prevalecen. El subconjunto del DER y sus adaptaciones se incorporarán por etapas; aún no hay entidades comerciales implementadas.

Compatibilidad consultada: [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [Next.js](https://nextjs.org/docs/app/getting-started/installation) y [PostgreSQL](https://www.postgresql.org/docs/current/release.html). Versiones exactas del frontend en el lockfile y del backend en Maven; las imágenes están declaradas en Dockerfiles/Compose.

## Continuación

Siguiente entrega: catálogo y tarifas, seguidos del configurador con horarios/capacidades. Luego recorrido PDF/pagos y operación/entrega. Resolver el acceso local a Docker antes de certificar el arranque conjunto y la persistencia de sus volúmenes.

Validación de la entrega 2 (22/09/2026): 3 pruebas integradas pasaron con PostgreSQL real. El recorrido de identidad verifica CSRF, contraseña hasheada, alta concurrente única, renovación del identificador al ingresar, sesión conservada al reiniciar Spring, logout/rechazo de cookie anterior y límite de intentos. Build y typecheck de Next pasaron. Se comprobó además el recorrido HTTP real a través de Next → Spring → PostgreSQL temporal: alta, login, perfil, administración y logout; y se inspeccionaron vistas de escritorio/móvil sin desbordes. Esos usuarios fueron exclusivamente de prueba en una base temporal, no datos precargados de la demo. Docker y sus volúmenes siguen pendientes de comprobación.

## Acceso de clientes particulares

Después de crear al propietario, `/registro` permite crear cuentas de particulares con nombre, apellido, correo y contraseña propia. El registro no inicia sesión automáticamente. `/acceso` dirige al administrador a `/administracion` y al cliente a `/cliente`; cada uno ve su propio perfil. El servidor asigna siempre el rol CLIENTE al registro público. Un cliente no puede consultar la API administrativa ni utilizar sus pantallas. Los duplicados de correo, incluso concurrentes, se rechazan sin crear cuentas adicionales.

El cliente puede ingresar mientras la imprenta se configura, pero crear pedidos continúa en construcción. La seguridad de cuenta permite verificar correo y cambiar contraseña; `/recuperar` permite recuperar el acceso. Ningún correo se marca como verificado automáticamente. No hay registro de empresas.

Este README es el único documento manual de producto. Se actualiza con lo efectivamente terminado, pruebas y decisiones relevantes en cada entrega.

Validación de entrega 3: Maven verify, build/typecheck y circuito HTTP real de registro/login de cliente pasaron. Se comprobaron duplicados concurrentes, perfil propio y rechazo de API administrativa con 403; redirecciones por rol y logout correctos. Datos usados solo en PostgreSQL temporal.

## Sucursales: alta y listado

En `/administracion/sucursales`, el administrador puede crear varias sucursales con código, nombre, dirección completa y zona horaria IANA. Correo y teléfono son opcionales. El formulario comienza vacío; no hay sucursales precargadas. Los códigos se normalizan a mayúsculas, son únicos y las zonas horarias se validan en el servidor. La migración V4 registra también al usuario que dio el alta.

Crear una sucursal conserva sus datos en PostgreSQL; todavía no habilita recepción de pedidos. La entrega 5 agrega edición/desactivación y empleados/asignaciones. Horarios/capacidades y operación siguen en construcción. Clientes y usuarios anónimos no pueden acceder a esta API administrativa.

Pruebas backend de entrega 4: se crean dos sucursales, se rechaza código duplicado y zona inválida, se controla CSRF, se niega GET/POST al cliente y se conserva el listado al reiniciar. Build y typecheck del frontend también pasaron.

Comprobación integrada entrega 4: Next → Spring → PostgreSQL real permitió crear y ver ambas sucursales. El duplicado devolvió 409; un cliente recibió 403 tanto al listar como al crear y su pantalla redirigió a su propio espacio. Servidores temporales detenidos al finalizar.


## Organización y acceso de empleados · entrega 5

En `/administracion/sucursales` se pueden editar los datos y desactivar/reactivar sucursales. El código permanece fijo; cada edición exige la versión consultada para evitar sobrescribir cambios simultáneos. La baja es lógica y no borra datos. Se rechaza si deja a algún empleado activo sin una sucursal activa: primero reasignarlo o desactivarlo.

En `/administracion/empleados` el administrador crea cuentas de empleados con nombre, apellido, correo, contraseña inicial y una o varias sucursales activas elegidas expresamente. Puede editar datos, asignaciones, permisos y estado; no convierte clientes ni al propietario en empleados. La contraseña inicial la define el administrador: no se genera una cuenta ni clave de muestra. Desde la entrega 6, el empleado debe cambiar esa contraseña inicial antes de acceder a su operación.

Los permisos adicionales son **Registrar cobros**, **Acreditar pagos** y **Registrar devoluciones**, inicialmente sin seleccionar. La migración V5 crea estas constantes técnicas y el rol EMPLEADO; no concede permisos ni crea personas. Las asignaciones y concesiones conservan fechas de alta/revocación; los cambios registran actor, objeto y versión. La edición de una organización se serializa en transacciones para impedir bajas concurrentes que dejen empleados sin sucursal activa.

Un empleado ingresa por `/acceso`: su primer ingreso lo dirige a `/cuenta/seguridad` para cambiar la contraseña inicial; después puede acceder a `/operacion`. Consulta sólo sucursales asignadas y activas y sus permisos actuales; puede abrir el detalle autorizado. Administración no está disponible para él, tampoco por API. Cambiar asignaciones o revocar permisos afecta a la sesión ya iniciada. Desactivar la cuenta o cambiar su correo revoca sus sesiones. No se muestran bandejas de pedidos ni acciones financieras como terminadas: la operación comercial sigue En construcción.

La baja de una sucursal no borra la asignación existente; ésta puede conservarse si el empleado tiene otra sucursal activa, y no otorga acceso mientras la sucursal siga desactivada. Las nuevas asignaciones exigen sucursales activas. Reactivar un empleado también exige al menos una activa.

Validación backend: `mvn verify` pasó cuatro pruebas con PostgreSQL real, incluyendo la nueva integración de organización. Se verificaron dos empleados aislados, permisos explícitos y revocación con la misma sesión, bloqueo de API administrativa, datos inválidos, bajas/reasignaciones, preservación de históricos, control de versiones y bajas simultáneas (una aceptada y la otra rechazada para preservar una sucursal activa). Se mantuvieron correctas las pruebas previas de identidad y persistencia. Build/typecheck de Next correctos. Comprobación integrada en navegador: alta del propietario, dos sucursales, creación/edición de empleado y permisos, acceso de empleado a una sola sucursal, rechazo de otra sucursal y API administrativa, reasignación efectiva sin volver a ingresar, y registro/acceso de cliente correctos. Vistas de escritorio/móvil y acceso denegado inspeccionadas sin desbordes, errores de JavaScript ni fallos de hidratación. Las capturas y los datos usados corresponden exclusivamente a la base temporal de pruebas.

La vista de sucursal distingue el acceso denegado (403) de un problema de conexión; el mensaje se comprobó contra el backend real. Los servidores temporales se detienen al cerrar la entrega.


## Correo y seguridad de cuenta · entrega 6

En `/cuenta/seguridad`, administrador, cliente y empleado pueden solicitar un código para verificar su correo y cambiar su contraseña con la actual. `/recuperar` permite solicitar otro código sin iniciar sesión y elegir una contraseña nueva. Las pantallas validan la repetición de contraseña y conservan los datos al corregir errores. Cambiar o restablecer la contraseña cierra las sesiones de esa cuenta; se debe ingresar nuevamente.

Cada código aleatorio tiene 43 caracteres, vence a los 15 minutos y sirve una sola vez para su propósito. La base guarda únicamente su hash. Hay hasta cinco intentos por código y un minuto entre emisiones; al emitir otro se revoca el anterior. Verificación y recuperación usan credenciales separadas. La respuesta pública de solicitud de recuperación es uniforme para cuentas inexistentes, inactivas o con envío no disponible. Ante falla SMTP no se consume el intervalo de reenvío ni se revoca el código anterior. Cambiar el correo de un empleado, desactivarlo o cambiar su contraseña revoca los códigos vigentes.

La migración V6 incorpora también una versión de acceso: los recursos protegidos rechazan una sesión creada con una versión anterior, incluso si un login se solapó con el cambio de contraseña. Las sesiones previas a esta actualización requieren ingresar nuevamente. No se precargan identidades ni se verifica un correo por restablecer la contraseña.

Recorrido local: abrir Seguridad de la cuenta, solicitar código, copiarlo desde Mailpit en `http://localhost:8025` y confirmar. Para recuperar acceso, salir y abrir `/recuperar`; repetir la lectura del buzón y elegir una contraseña diferente. El empleado con contraseña inicial sólo puede consultar su perfil, verificar correo, cambiar su contraseña o salir; API y navegación bloquean la operación hasta completar el cambio.

Fuera de Compose se configuran `SMTP_HOST`, `SMTP_PORT` y `SMTP_FROM`; los valores técnicos predeterminados apuntan a un capturador local en `127.0.0.1:1025`. El remitente técnico se puede cambiar mediante `.env`; no es una cuenta ni una política comercial. La conexión SMTP tiene tiempos máximos de espera. Referencia técnica: [correo de Spring Boot](https://docs.spring.io/spring-boot/reference/io/email.html).

Comprobación integrada de correo: recorrido real Next → Spring → PostgreSQL y SMTP Mailpit con verificación, errores corregibles, cambio de contraseña, recuperación uniforme, cierre de sesiones anteriores y cambio inicial obligatorio del empleado. Cinco capturas de escritorio/móvil revisadas sin desbordes, errores JavaScript/hidratación ni respuestas 5xx. Los datos y el buzón fueron temporales y no se incorporan a la instalación.

Pruebas nuevas: diez ejecuciones de integración con PostgreSQL real y SMTP de captura comprobaron entrega y hash privado, separación de propósitos, cinco intentos persistentes, vencimiento/reenvío, uso único concurrente, respuesta pública uniforme, rollback ante fallo SMTP, revocación por cambio de correo/baja, rechazo de principal anterior y configuración de cookies.

Regresión posterior: las cuatro pruebas previas de base, identidad y organización también pasaron con el principal de sesión actualizado, incluida persistencia tras reinicio. Build/typecheck del frontend y validación sintáctica de Compose correctos; arranque de contenedores aún pendiente por permisos del servicio Docker del equipo.
