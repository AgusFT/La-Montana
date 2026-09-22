# La Montaña · v0.1

Piloto local en construcción con Spring Boot, Next.js y PostgreSQL. Esta rama `desarrollo` reemplaza la implementación deprecada; el código anterior sigue disponible en el historial Git.

## Estado actual · entrega 19

Implementado: base técnica de la entrega 1 más alta única del propietario, credenciales persistentes, login/logout y áreas administrativa, de cliente y de operación con sesiones reales, registro de particulares y redirección según rol. También funcionan la verificación por correo local, recuperación de acceso, cambio de contraseña y catálogo global con revisión comercial propia. Se puede crear y retomar un borrador operativo, guardar modelo y condición de aprobación, o cancelarlo con contraseña y código de correo conservando su historial. La fase3 guarda medios y parámetros financieros, y calcula ejemplos sin crear pedidos. La fase4 declara impresoras/capacidades y servicios por sucursal con asignación manual. La fase5 guarda calendarios, tiempos y modalidades, valida sus mínimos y calcula ejemplos temporales. También permite definir puntos de entrega con dirección, relaciones por sucursal, costos y franjas/cupos versionados. La disponibilidad temporal se declara por separado desde administración, con recordatorio de deshabilitados y sin cambiar el borrador. Las zonas domiciliarias globales se configuran por código postal/localidad/provincia, con costo y cupos compartidos. La simulación de puntos y domicilio integra origen y próxima franja del destino, con costo y cupo configurados. La fase6 reúne las validaciones operativas y comerciales, señala bloqueos/advertencias con enlaces correctivos y permite simular un ítem PDF declarado, sus terminaciones, precio, condiciones financieras y recorrido temporal. La fase7 permite activar inmediatamente con contraseña/código de correo, conserva la predecesora como histórica y muestra el resultado auditado. Compose incluye base, backend, frontend y buzón local Mailpit.

**Ya se puede crear al propietario, registrar particulares, iniciar/cerrar sesiones y gestionar sucursales y empleados desde administración.** El empleado ingresa a su espacio y consulta únicamente sus sucursales habilitadas. Las tarifas y los servicios globales ya se pueden configurar y programar. La configuración operativa ya admite activación inmediata. Oferta real de entregas, operación de recursos, programación operativa, historial completo, pedidos, movimientos de pagos, PDF y producción/entrega siguen **En construcción**. No hay cuentas ni datos comerciales precargados. V1 prepara el esquema; V2 agrega identidad, el rol técnico de administrador, el registro único de inicialización, eventos de acceso y tablas técnicas de sesiones. V3 incorpora el rol técnico CLIENTE para particulares, sin crear cuentas.

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

Mailpit recibe los códigos de verificación, recuperación, cancelación de borradores y activación de configuración enviados por el backend mediante SMTP `mailpit:1025`. Abrir el buzón local para leerlos; no se envían a proveedores de correo externos.

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

Para desarrollar sin contenedores, proporcionar `DB_URL`, `DB_USER`, `DB_PASSWORD` y opcionalmente `FILES_DIR` al backend, apuntando a un PostgreSQL propio. El frontend requiere `BACKEND_INTERNAL_URL` y se inicia con `pnpm dev`. Los directorios de archivos no se publican como recursos web. Son públicos el estado, el token CSRF y el recorrido inicial de alta/login; `/api/auth/me` exige sesión y `/api/admin/estado` exige rol administrativo. Las rutas administrativas de organización y catálogo exigen propietario; los módulos todavía no implementados permanecen cerrados. No existe usuario automático de Spring.

## Primer acceso y comprobación manual

1. Ejecutar el script de arranque. Además de la credencial de base, genera `SETUP_TOKEN` aleatorio en `.env` si falta. Abrir ese archivo **localmente** y copiar únicamente el token al formulario; no publicarlo ni adjuntarlo a Git.
2. Entrar a `/instalacion` e ingresar token, nombre, apellido, correo y una contraseña propia de 12 a 128 caracteres. No hay datos predeterminados. El token requiere al menos 32 caracteres al configurarlo manualmente; si falta o es insuficiente, el alta queda deshabilitada.
3. Tras crear al propietario, entrar a `/acceso` con el correo y contraseña elegidos. El alta no inicia sesión automáticamente. `/administracion` muestra la identidad real y las funciones todavía en construcción.
4. Recargar o reiniciar el backend y comprobar que la sesión continúa. Cerrar sesión y comprobar que volver a administración requiere autenticarse. El alta inicial permanece cerrada, incluso tras reiniciar o conservar el token antiguo.

Las contraseñas se guardan con PBKDF2 y sal aleatoria; el correo se compara sin distinguir mayúsculas. El correo comienza sin verificar y sólo cambia al confirmar un código recibido en el buzón local. Las sesiones se guardan mediante Spring Session JDBC, vencen tras 30 minutos de inactividad y usan cookies HttpOnly/SameSite=Lax. Cada POST requiere un token CSRF obtenido para esa sesión, renovado tras login/logout. El backend renueva el identificador al autenticar e invalida la sesión al salir.

El token de instalación habilita una única alta, bloqueada transaccionalmente ante concurrencia. No habilita un segundo propietario si luego se desactiva la cuenta. Se registran alta, login correcto/fallido y logout sin guardar contraseñas ni tokens en esos eventos. El piloto aplica un máximo global de 30 solicitudes de alta/login/registro/correo/cambio de contraseña/autorización de cancelación o activación por minuto y proceso; ese contador se reinicia con el backend. Los códigos tienen además límites persistentes de reenvío e intentos.

El proxy Next admite solo las rutas declaradas de identidad, organización, catálogo y configuración, conserva las cookies y no almacena credenciales en el navegador. `SETUP_TOKEN` es configuración privada del backend y no se envía al frontend automáticamente. En ejecución manual del backend, definir también esa variable para habilitar el primer acceso.

Como adaptación técnica del DER, esta etapa usa sesiones HTTP persistentes de Spring Session; no implementa un circuito paralelo de refresh tokens. El rol técnico de administrador conserva el código ADMIN_ADMIN utilizado desde la entrega 2; el empleado usa EMPLEADO. Las migraciones V3–V6 extienden usuarios, sucursales, permisos y credenciales temporales. La instalación inicia sin configuración ni datos comerciales.

## Decisiones que se mantienen para completar la demo

- Una instalación/base por imprenta, varias sucursales reales y empleados asignados. Políticas y catálogo globales, servicios/capacidades habilitados por sucursal y permisos financieros explícitos.
- Solo particulares y PDF. El primer propietario creará sus credenciales y configurará el sistema desde cero; sin precios, sucursales, reglas ni plantillas de fábrica. No incorporar la regla histórica de 200 carillas/30%.
- Seguir los mocks de Alejandro: asistente de configuración de fases 1–8, modelos manual/condicional y catálogo con revisión comercial independiente. Programación, historial, comparación y usar como base forman parte del objetivo.
- Una cotización vigente conserva el precio; al confirmar se revalidan las condiciones actuales. Registrar versión operativa cotizada, revisión comercial y versión de validación final.
- **D1:** en la variante por monto superior al umbral, revisar PDF/datos, pedir correcciones o aprobar antes de exigir la seña configurada para producir. Pago previo/seña puros conservan el bloqueo de upload hasta acreditar.
- **D2:** conservar un pago tardío y permitir aplicación autorizada a una nueva cotización aceptada, mostrando diferencias. Si no continúa, registrar devolución. No crear un pedido automáticamente ni cobrar de nuevo lo recibido.
- Producción manual, correcciones, calidad/reimpresión, cancelaciones y entrega/cobro completos. Retiro local: **Listo para entregar**. **En viaje** solo para traslado a punto o domicilio; llegada y entrega efectiva son hechos diferentes.
- Reclamos posteriores, empresas/cuenta corriente, conversión DOCX/ODT, CUPS/agentes, mobile, pasarelas y seriales/licencias: **En construcción**. D2 y las correcciones operativas no pertenecen a los reclamos diferidos.

Referencia funcional: DER y flujos de `main` en `3983058e`, más contratos/mocks de `docs/actualizacion-producto-configurable-revision` en `56f89d40`. Las decisiones del usuario prevalecen. El subconjunto del DER y sus adaptaciones se incorporan por etapas: identidad, organización, catálogo y preparación de configuración ya persisten; cotizaciones, pedidos y movimientos financieros siguen pendientes.

Compatibilidad consultada: [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [Next.js](https://nextjs.org/docs/app/getting-started/installation) y [PostgreSQL](https://www.postgresql.org/docs/current/release.html). Versiones exactas del frontend en el lockfile y del backend en Maven; las imágenes están declaradas en Dockerfiles/Compose.

## Continuación

Siguiente entrega: completar fase7 con programación, cancelación autorizada y ejecución automática; después fase8, historial, comparación, usar como base y reversión mediante nueva versión. Continuar luego PDF/pedidos/pagos y operación/entrega. La disponibilidad temporal incluye puntos de la activa y nuevas identidades del borrador; la oferta/confirmación futura revalidará estado y capacidad sin mezclarlos con snapshots. Docker sigue pendiente de certificación; no repetir el intento sin cambio de entorno.

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


## Servicios y precios · entrega 7

`/administracion/catalogo` permite crear formatos, papeles y servicios sin valores de muestra; editar tarifas y terminaciones; publicar una revisión comercial inmediata y consultar revisiones anteriores. El acceso se ofrece desde administración; la integración de retorno a Fase 6 se agregará con el configurador. Los precios son globales para la imprenta. Las capacidades y servicios por sucursal se configuran en el módulo operativo pendiente.

Los formatos guardan código, nombre y dimensiones; los papeles, código, nombre, gramaje y terminación/material. Estos datos estables no se reescriben: una variante se agrega con otro código. El administrador crea el servicio de impresión y las terminaciones que ofrecerá. Habilitación comercial, nombre visible, precio, base de cobro, preparación y compatibilidades quedan congelados en cada revisión. Deshabilitar o retirar una opción de una nueva revisión conserva las anteriores.

Cada tarifa se identifica por formato, papel y modo B/N o color; incluye precio por carilla y recargo de doble faz. Las terminaciones usan la base elegida: por copia, hoja, carilla o importe fijo por ítem. Se deben indicar expresamente las combinaciones de formato/papel admitidas. Al guardar debe haber al menos una tarifa habilitada y al menos un servicio de impresión habilitado. Cada ítem elegirá un único servicio de impresión cuando se implemente el cotizador. Una terminación habilitada exige tarifa para sus combinaciones. Importes negativos, precisión mayor a dos decimales, referencias inexistentes y duplicados se rechazan.

Decisiones técnicas para evitar ambigüedad en el futuro cotizador: el recargo doble faz se expresa **por carilla impresa**; el servicio IMPRESION toma el precio de la tarifa, con base POR_CARILLA y sin sumar otro precio base. Esta precisión no estaba definida en el DER y se explicita en la vista. Los cálculos usarán BigDecimal; el cotizador todavía no está implementado.

Guardar crea una revisión de contenido inmutable con motivo, actor, fecha y referencia a la anterior. Su estado de vigencia puede cambiar sin reescribir tarifas ni servicios. La actualización completa es una transacción: un fallo conserva la vigente y revierte todas las filas nuevas. Una confirmación repetida no duplica revisiones; una edición basada en una versión que cambió recibe conflicto para revisión del administrador. El historial y los precios previos se conservan. El alcance restante debe enlazar las cotizaciones a estas revisiones para comprobar la no retroactividad E2E.

La migración V7 separa `catalogo_revision` de la futura versión operativa y vincula a ella `tarifa_impresion`, `configuracion_servicio` y sus compatibilidades. Es la adaptación pedida por el contrato comercial posterior al DER. Las tablas estables no llevan habilitación comercial propia; esa condición pertenece a cada revisión. La entrega 8 agrega programación comercial y recuperación al reiniciar.

Prueba integrada nueva con PostgreSQL real: instalación vacía; altas y duplicados; validación decimal/referencial; CSRF y cliente sin acceso; historial inmutable; reintento idempotente; confirmaciones simultáneas y editores con base obsoleta; fallo inyectado de almacenamiento sin publicación parcial; estado y sesión conservados tras reiniciar. Resultado correcto.

Prueba de navegador del catálogo: altas desde formularios vacíos, edición/publicación, dos editores con conflicto y valores conservados, consulta de vigente/base, historial con precio original y cliente rechazado. El reintento tras cortar la respuesta después del guardado usa el mismo contenido y UUID y recupera una sola revisión. Se corrigió la validación para admitir varios servicios de impresión en el catálogo, respetando que la selección única corresponde al ítem.

Comparación de esta vista con `MC-ADM-COM-001-servicios-precios.png`: conserva navegación lateral azul oscura, cabecera de usuario, ruta de navegación, acciones azules, aviso comercial, paneles de tarifas/servicios e historial. Se agregaron formularios vacíos para la primera instalación y campos explícitos de recargo y compatibilidades; no se copiaron los precios/personas del mock. La marca aún usa el símbolo geométrico provisional de las entregas anteriores; la unificación visual de todas las áreas y la revisión final de la identidad gráfica siguen pendientes. En pantallas angostas los paneles se apilan y las tablas tienen desplazamiento interno.


## Programación comercial · entrega 8

En Servicios y precios, **Programar cambios** permite elegir una fecha futura y guardar la revisión completa. La fecha de ingreso y las fechas mostradas se expresan explícitamente en UTC. La vigente se conserva hasta la activación. También se puede programar la primera revisión, manteniendo el catálogo sin vigente hasta entonces; no se inventan precios de respaldo.

La migración V8 registra estados VIGENTE, HISTORICA, PROGRAMADA y CANCELADA, fecha prevista y fecha efectiva. Sólo se admite una programación pendiente: hay que cancelarla o esperar su activación antes de publicar otro cambio. Es un control técnico para evitar reemplazos ambiguos, independiente de la futura programación de versiones operativas. La activación comercial exige sesión de propietario y CSRF; no se agrega un OTP al contrato comercial. La activación operativa con contraseña y correo pertenece al configurador pendiente.

La tarjeta de programación permite actualizar el estado y cancelar con un motivo obligatorio. El servidor conserva responsable, motivo y fecha de cancelación; la revisión cancelada sigue disponible en el historial. Una cancelación que llega después de la fecha de vigencia es rechazada y no deshace la activación. Si se pierde una respuesta, las vistas conservan el comando para reintentar sin duplicar el guardado ni la cancelación.

Spring revisa las programaciones cada cinco segundos y también al consultar, publicar o cancelar revisiones. Usa la hora de PostgreSQL y una transacción con exclusión mutua: un reinicio recupera las pendientes y un fallo de almacenamiento revierte conjuntamente estado y auditoría. La fecha efectiva puede ser posterior a la prevista si el servicio estuvo apagado. Las futuras operaciones de cotización deberán consultar esta vigencia reconciliada.

Validación del backend: cinco pruebas de programación y la regresión del catálogo pasaron con PostgreSQL real. Cubren fecha futura, una sola pendiente, permisos y CSRF, idempotencia, concurrencia, reinicio, rechazo posterior al vencimiento y rollback de activación/cancelación ante fallos SQL. Build y comprobación de tipos del frontend correctos. La certificación de Compose y el circuito completo de pedidos siguen pendientes.

Recorrido real de navegador de programación: guardar un cambio futuro conserva el precio vigente; cancelar con respuesta perdida permite reintentar el mismo comando; corregir una fecha pasada conserva el formulario; esperar la fecha real activa la nueva revisión y actualizar el estado carga la base correcta. Historial y vistas de escritorio/móvil comprobados sin errores JavaScript, respuestas 5xx ni desbordes de página. Se corrigió la asociación explícita entre las etiquetas y los campos de motivo para mantener su identificación al editar después de un error.

Migración comprobada sobre V7 poblada: al aplicar V8 se conservaron revisión vigente, histórica, tarifas, terminaciones, compatibilidades, fechas y comprobantes; repetir solicitudes V7 sin el campo nuevo recuperó las mismas revisiones sin duplicar eventos. La prueba específica de actualización también pasó.


## Borrador del configurador · entrega 9

Desde Administración → Configurar la imprenta se puede crear un único borrador vacío, retomar su edición y elegir Control manual o Control condicional. El manual no lleva criterio automático; el condicional exige elegir Pago previo total, Pago de seña o Monto total del pedido. Al crear, modelo y criterio están sin seleccionar; no se asignan importes, porcentajes ni políticas predeterminadas.

Guardar esta selección **no activa reglas ni habilita pedidos**. La entrega 11 implementa parámetros financieros y ejemplos calculados de fase 3. La entrega12 agrega recursos/capacidades declarados y servicios por sucursal. La entrega13 agrega calendarios y ejemplos temporales, la18 revisión integral y la19 activación inmediata segura. Oferta real de entregas, programación e historial operativo completo siguen En construcción. La entrega 10 habilita cancelar el borrador con contraseña y código por correo, como exige el mock; también se puede seguir editando el existente.

V9 incorpora el subconjunto inicial de `configuracion_version`, separado de `catalogo_revision`: número de configuración, versión de edición, creador, fechas y selección del modelo. V9 admite EN_PREPARACION; V10 agrega CANCELADA; V17 agrega ACTIVA/HISTORICA y metadatos de aplicación. La programación se incorporará con su recorrido. Comprobantes y eventos conservan actor, destino, tipo y versión. El backend exige propietario activo, rol y CSRF. Un reintento devuelve el estado actual del mismo borrador y no vuelve a aplicar cambios antiguos.

Dos editores no se sobrescriben silenciosamente: la versión obsoleta recibe conflicto, el formulario conserva la selección y permite consultar/adoptar la versión actual antes de guardar. Creación, guardado, auditoría y comprobante son atómicos. El borrador y la sesión se conservaron al reiniciar en la prueba integrada.

Dos pruebas integradas con PostgreSQL real/HTTP comprobaron estado vacío, selección y validaciones, permisos, CSRF, concurrencia, idempotencia, fallos de almacenamiento con rollback y reinicio. Se corrigió el manejo de JSON/enum inválido para devolver un error 400 sanitizado en lugar de un error de autorización 403. La prueba de migración comercial V7→V8 sigue pasando con su destino fijado en V8.

El recorrido de navegador comprobó creación con respuesta perdida/reintento, guardado manual y recuperación al recargar, conflicto entre dos editores y recuperación conservando la selección, exclusión del cliente y distintivos de las funciones pendientes. Build/typecheck correctos. Las vistas se contrastaron con CFG-001B y CFG-002 de Alejandro: navegación por fases, resumen en tarjetas, modelos comparables, selección azul y automatización futura deshabilitada. Se adaptó el estado inicial vacío y se corrigió el tamaño/alineación de los controles de selección. No se copiaron valores de ejemplo del mock. La revisión visual final de toda la aplicación continúa pendiente.


## Cancelación segura de borradores · entrega 10

Desde Configuración actual, **Cancelar borrador** solicita motivo y contraseña del propietario. El código llega al buzón local Mailpit y debe confirmarse junto con la contraseña. Cancelar conserva selección, creador, número y fechas; agrega responsable, motivo y fecha de cancelación al historial. Después permite crear un nuevo borrador vacío. No activa reglas ni modifica revisiones comerciales.

V10 incorpora autorizaciones ligadas a propósito, propietario, correo, generación de acceso, operación, borrador, edición y motivo. El código aleatorio de 43 caracteres se guarda sólo como hash, vence a los 15 minutos y admite cinco intentos; se exige un minuto entre emisiones. Contraseña incorrecta al solicitar no envía correo. Cambiar el borrador, la contraseña o el correo invalida la autorización pendiente. Contraseñas y códigos no forman parte de comprobantes ni de representaciones de los DTO.

Las respuestas perdidas se recuperan reenviando el mismo comando: solicitar otra vez no duplica el correo y confirmar otra vez recupera la cancelación registrada sin repetirla ni afectar un borrador posterior. Confirmar y guardar estado, consumo, comprobante y auditoría forman una transacción. Un fallo SQL conserva el borrador; un fallo SMTP conserva también la autorización anterior y no consume el intervalo de reenvío. El historial muestra los últimos 50 borradores cancelados; activación, comparación y rollback de versiones operativas siguen En construcción.

Validación: cinco pruebas nuevas con PostgreSQL real/HTTP/SMTP y doce regresiones de configuración y correo pasaron, sin fallos ni omitidos. Cubren permisos/CSRF/actor, separación de propósitos, contraseña, límite de intentos, expiración, revocación, concurrencia, reinicio, reintentos y fallos SQL/SMTP. Build/typecheck Next correctos. El recorrido real Next → Spring → PostgreSQL → Mailpit verificó errores corregibles, respuestas perdidas al solicitar y confirmar, historial persistente, nuevo borrador vacío y cliente rechazado. Capturas de escritorio/móvil revisadas sin desbordes, errores JavaScript ni 5xx. El piloto E2E completo y el arranque de Compose permanecen pendientes.


## Pagos y reglas de seña · entrega 11

La fase 3 permite guardar medios generales, instrucciones de transferencia, vigencia de cotización en minutos y reglas financieras. Todos los datos comienzan vacíos; guardar el modelo no asigna medios ni parámetros comerciales. Las transferencias se acreditarán manualmente por un usuario autorizado en el módulo de pagos. Las pasarelas permanecen En construcción.

| Modelo | Regla financiera configurable | Medios del requisito |
|---|---|---|
| Manual | Elegir sin seña o seña fija/porcentual, siempre, desde carillas o desde importe. La revisión es humana y la seña aplicable se exige antes de producir. | Medios generales habilitados. |
| Pago previo total | Total acreditado antes de cargar PDF; no admite seña adicional. | Transferencia; efectivo deshabilitado. |
| Seña previa | Seña fija/porcentual para todos los pedidos, antes de cargar PDF. | Transferencia; efectivo puede habilitarse sólo para el saldo. |
| Por monto, D1 | Hasta el umbral inclusive, sin seña por esa regla. Al superarlo, revisar/corregir PDF y aprobar; después acreditar seña antes de producir. | Transferencia para la seña; medios generales para el saldo. |

Los campos heredados del modelo no pueden sustituirse. La API rechaza combinaciones incompatibles, importes no positivos, porcentajes superiores a 100 y fracciones en campos enteros. Cambiar realmente el modelo o criterio elimina sólo los parámetros financieros del borrador para que se configuren de nuevo; guardar la misma selección los conserva. Guardar pagos incrementa la versión, deja auditoría e invalida códigos de autorización pendientes. Los reintentos conservan operación/contenido y devuelven el estado actual sin reaplicar reglas viejas.

**Calcular ejemplo** recibe total y carillas introducidos por el administrador y evalúa la edición financiera guardada en el backend. Muestra pago previo, seña, saldo, necesidad de revisión, bloqueo de carga y momento de exigencia, sin crear pedido ni acreditar dinero. Cambiar campos exige guardar antes de simular. Dos editores reciben conflictos con recuperación explícita de versión y conservación del formulario. Esta simulación financiera todavía no valida disponibilidad, archivos, producción ni entregas.

Adaptaciones del DER: V11 implementa una guía financiera por versión en `configuracion_financiera`, con `configuracion_medio_pago` para medios generales; el evaluador deriva los medios del anticipo y su momento según el modelo. Sustituye para este piloto el editor libre de condiciones/acciones por las opciones certificadas. La regla se aplica al pedido y las carillas cuentan todas sus copias. La vigencia proviene de `politica_operativa` del DER; se incorpora junto a esta guía para completar el futuro cotizador. Las instrucciones de transferencia son texto configurable; no se precargan datos bancarios ni se supone un plazo bancario.

Los importes usan BigDecimal y se expresan como cadenas decimales en la API. Importes monetarios admiten hasta dos decimales y porcentajes hasta cuatro; el importe de seña se redondea a centavos con HALF_UP. La seña fija se limita al total del pedido para evitar exigir un sobrepago. Estas precisiones son comunes al evaluador que se reutilizará al cotizar y validar pedidos.

Si un porcentaje positivo aplicado a un importe pequeño redondea a0,00, no se inventa un cargo mínimo: el ejemplo explica que no hay anticipo monetario exigible. Ese borde no elimina la revisión humana de manual/D1 ni los controles técnicos.

Validación de entrega11: tres pruebas nuevas PG/HTTP y siete regresiones de configuración/cancelación pasaron (10 en total, sin fallos ni omitidos). Se comprobaron matriz completa, umbrales inclusivos/estrictos, decimales/redondeo, ejemplo sin escrituras, entradas inválidas, reintentos y concurrencia, rollback SQL, revocación de autorización pendiente, permisos/CSRF y reinicio. Build/typecheck Next correctos. En navegador se recorrieron las cuatro variantes, un guardado con respuesta perdida, conflicto entre editores y recuperación sin perder el formulario, cambio de modelo, persistencia al recargar y rechazo del cliente. Cinco capturas escritorio/móvil comparadas con CFG004005 A-D, sin desbordes de página, errores JS ni5xx. Todavía faltan la activación operativa y el recorrido E2E de pedidos y dinero.

La revisión detectó y corrigió que cambiar de fase podía desmontar un formulario con guardado o cancelación pendientes. La navegación interna y salida al dashboard ahora permanecen bloqueadas mientras se procesa o se recupera una respuesta incierta; se conserva el comando original hasta resolverlo.


## Recursos y servicios por sucursal · entrega 12

La fase4 permite declarar impresoras, capacidades y servicios de cada sucursal y confirmar expresamente la asignación manual. La instalación no selecciona un método ni crea recursos. Los formatos y servicios se crean en el catálogo global; la vista permite abrirlo, volver y actualizar las opciones sin inventar datos. Los precios siguen siendo comerciales y no se copian a la versión operativa.

Cada impresora tiene identidad estable y sucursal inmutable, nombre, formatos, capacidad máxima positiva, B/N con color opcional y doble faz declarado. El alta exige seleccionar sus capacidades y estado. Los estados mostrados son **previstos en el borrador**, sin afirmar que ya hay una configuración activa. Se debe deshabilitar antes de editar o retirar; el retiro sólo aparece dentro del editor, requiere motivo y conserva identidad y registro. Cambiar de modelo o pagos no borra los recursos.

Los servicios habilitados son independientes por sucursal e incluyen impresión y terminaciones del catálogo. Asignación automática, CUPS, lecturas de dispositivos, recargas, estimación de papel y selección/registro de trabajos siguen **En construcción**. Estas últimas operaciones manuales se completarán en los bloques de producción incluidos en v0.1. La capacidad máxima no se presenta como papel disponible y no se inventa un porcentaje ni un contador histórico.

Adaptación del DER para el piloto sin CUPS: identidad de impresora ligada directamente a la sucursal; capacidades y estado declarados se guardan por versión operativa, separados de los futuros eventos cotidianos de papel/producción. No se exige agente ni cola CUPS para configurar. La guía de Alejandro define B/N y color como capacidades; color incluye B/N. Retirar afecta el snapshot del borrador, sin borrar la identidad histórica.

La interfaz distingue listado, editor y cambio de estado con motivo, siguiendo CFG006/007. El historial de retiradas puede mostrarse en la lista. Ediciones concurrentes se rechazan con conflicto y se permite consultar/adoptar la versión actual conservando datos; una respuesta incierta bloquea navegación y mantiene el comando exacto para reintentarlo.

Validación: 13 pruebas de backend aprobadas con PostgreSQL real (3 de recursos y 10 regresiones de configuración, finanzas y cancelación SMTP), sin fallos ni omitidos. Cubren referencias, duplicados, valores obligatorios/enteros, estados, permisos/CSRF, sucursal desactivada, reintentos, concurrencia, rollback, revocación de autorización y reinicio. Los eventos de alta/cambio/retiro conservan estado anterior/nuevo, recurso, actor y motivo. Cancelar conserva los recursos en el historial; el nuevo borrador comienza vacío.

Build/typecheck del frontend y recorrido real Next→Spring→PostgreSQL correctos. Se verificaron dos sucursales con servicios distintos, altas vacías, formatos obligatorios, respuesta perdida recuperada sin duplicado, dos editores, deshabilitar/editar/habilitar/retirar, recarga y rechazo del cliente. Se corrigieron etiquetas de selectores y la posibilidad de deshacer una deselección antes de guardar en una sucursal desactivada. Cuatro capturas de lista/editor en escritorio/móvil sin desbordes de página, errores JS ni respuestas5xx.

Comparación con CFG006/007: se conservan fases laterales, fase activa azul, lista de capacidades con acciones según estado, editor con información técnica separada y retiro al pie. Se agregan sucursal, servicios y campos vacíos por el alcance configurable de la demo; los datos operativos inexistentes se muestran Sin registro, sin copiar ejemplos ni porcentajes del mock. Las consideraciones se ubican debajo para dar espacio a servicios por sucursal. En pantallas angostas el editor se apila y la tabla tiene desplazamiento interno. La simulación integral del flujo y la revisión visual final del conjunto todavía están pendientes.

## Calendario y estimación de entrega · entrega 13

La fase5 permite guardar horarios por sucursal, tiempos globales de preparación y traslado, y modalidades de retiro, punto o domicilio. Cada día se decide individualmente como abierto/cerrado o queda pendiente; no se copian días, horas ni tiempos de muestra. Guardar admite un borrador incompleto y la validación explica qué falta antes de avanzar. Las sucursales activas con servicios habilitados necesitan un calendario completo y al menos un día abierto; debe existir al menos una sucursal utilizable.

Los horarios habituales pertenecen a la versión operativa. La zona IANA de cada calendario se captura desde la sucursal al primer guardado y no cambia silenciosamente al editar sus datos; guardar su eliminación y después volver a incorporar explícitamente el calendario tomará la zona actual. Los siete días admiten una ventana cada uno, como el mock. Las franjas múltiples y cupos estructurales de puntos se agregan en la entrega14 y su disponibilidad temporal en la15. Las reservas y excepciones todavía están En construcción.

La semántica sigue el cierre funcional del 11/09 de `WF-FASE-5-HORARIOS-PUNTOS-ENVIOS`: preparación y traslado son tiempos diferentes y se suman cuando el trabajo sale hacia un punto o domicilio. Para resolver la redacción ambigua de la sección7, se conserva la regla explícita de la sección3.2: **ambas duraciones consumen ventanas operativas de la sucursal de origen**. La vista lo informa. No se interpreta el estimado como garantía del transportista ni como espera obligatoria.

El ejemplo temporal es calculado en backend sobre la edición guardada. Usa recepción expresada en UTC, presenta resultados en la zona del calendario y distingue cola, inicio, fin de preparación, llegada estimada y disponibilidad. El remanente cruza cierres/fines de semana hasta la próxima apertura. Si la preparación termina exactamente al cierre, el retiro se ofrece desde la próxima apertura. Domicilio resuelve la combinación territorial desde la entrega17 y ajusta la llegada a una franja utilizable de su zona global. No crea pedidos ni reservas. Los puntos integran sus condiciones de destino en el evaluador desde la entrega16, como se detalla más abajo.

V13 agrega `configuracion_entrega`, modalidades y calendario versionados por sucursal, conservando los módulos anteriores. Las horas viajan como cadenas decimales, con hasta dos decimales y máximo técnico10000; 0,01h equivale exactamente a36segundos. Preparación debe ser positiva cuando se completa y traslado puede ser cero explícito. El evaluador consume segundos reales entre aperturas/cierres locales y contempla cambios de horario estacional. Limita su búsqueda a cinco años desde la recepción para rechazar combinaciones imposibles de resolver dentro de ese horizonte sin quedar en un bucle.

Las zonas domiciliarias, costos y franjas/cupos se agregan en la entrega17. Las reservas y la oferta real de entrega **siguen dentro de v0.1 y pendientes**, marcadas En construcción. Habilitar un checkbox no certifica una modalidad: se exigen destinos utilizables cuando sólo se ofrecen puntos; con retiro local válido, la falta de puntos utilizables genera un aviso. El envío requiere cobertura utilizable; si otra modalidad puede operar, se advierte que no se ofrecerán envíos. Sin alternativa utilizable, la fase queda incompleta. La validación local de retiro no sustituye la futura revisión integral, la reserva atómica ni la activación segura.

Validación de entrega13: 17 pruebas PG/HTTP/SMTP aprobadas, sin fallos ni omitidos (4 de calendario y 13 regresiones). Cubren vacío/parcial, siete días/múltiples sucursales, intervalos y referencias, valores de horas, permisos/CSRF, versiones/replay/concurrencia, rollback, revocación y conservación al cancelar/reiniciar. El evaluador verifica fuera de horario, cierres/fines de semana, fracciones de36segundos, DST en ambos sentidos, ventanas inválidas tras salto y horizonte de cinco años; simular no escribe.

Build/typecheck y recorrido real de navegador correctos: guardado parcial con respuesta perdida y reintento exacto, error de intervalo conservando campos, calendario incompleto sin falso conflicto, cálculos de retiro/envío, dos editores y recuperación de zona del calendario, persistencia, precisión de segundos visible y cliente rechazado. La prueba inicial del navegador esperaba una cadena sin escala; se corrigió la expectativa al formato decimal de PostgreSQL (5,00), sin modificar el cálculo ni debilitar sus resultados. Se corrigieron en frontend la presentación de segundos y la sincronización de zonas al recuperar/volver a agregar calendarios.

Se compararon escritorio y móvil con CFG008009: fases laterales, calendario de siete días frente a tiempos, modalidades y timeline con panel verde. Se agregan selector de sucursales, estado Sin configurar y errores de validación por el arranque vacío/multisucursal. Las horas y nombres del mock no se precargan. Tablas con desplazamiento interno en pantallas angostas; sin desbordes de página, errores JS ni5xx. El panel dedicado de puntos CFG009 se agrega en la entrega14; la fidelidad visual final de toda la aplicación sigue pendiente.


## Puntos de entrega estructurales · entrega 14

Desde fase5, **Administrar puntos** permite crear y editar puntos con código estable, nombre, dirección, referencias opcionales y zona IANA explícita. El código se normaliza a mayúsculas y no puede reutilizarse ni cambiarse; el nombre y la dirección pertenecen a la versión. Todos los campos comienzan vacíos. Se puede guardar una dirección sin sucursales y completarla después, sin que eso habilite entregas.

Cada relación con una sucursal de origen tiene habilitación estructural, costo ARS y franjas por día con apertura, cierre, cupo entero e indicación habilitada/deshabilitada. Son datos explícitos: costo cero no se supone y cupo cero no significa ilimitado. Las franjas pueden ser contiguas, pero no superponerse para un mismo origen/punto/día, incluso si están deshabilitadas. No hay un segundo calendario global del punto que contradiga a sus relaciones. Un punto puede recibir trabajos de varios orígenes con costos y ventanas diferentes.

V14 separa identidad permanente (`punto_entrega`), definición versionada (`configuracion_definicion_punto`) y condiciones por origen (`configuracion_punto_entrega` y `franja_entrega`), siguiendo la relación de franjas del DER. Editar reemplaza sólo relaciones del borrador, todavía sin reservas; los snapshots cancelados permanecen inmutables. Quitar una relación o deshabilitarla no borra la identidad. No se requiere deshabilitar el punto antes de corregir su definición. Una sucursal desactivada no admite relaciones habilitadas nuevas, pero puede conservar/deshabilitar relaciones anteriores.

Alta y edición son transaccionales, con control de versión, auditoría, revocación de códigos de autorización y reintentos ligados a actor, operación, destino y contenido. Guardar modelo, pagos u horarios conserva los puntos. Un código duplicado permite corregir el formulario directamente; un conflicto de edición permite consultar y adoptar la versión actual conservando datos. Ante respuesta perdida se mantiene el comando exacto y se bloquea la navegación hasta resolverlo.

La entrega15 incorpora disponibilidad temporal fuera del snapshot. El panel estructural enlaza a su gestión separada y ya no devuelve un estado operativo ficticio dentro de la configuración. Todavía no se ofrecen reservas ni se simula una entrega confirmada. La validación de fase5 comprueba relaciones útiles y mantiene un bloqueo explícito hasta integrar oferta real y condiciones del destino.

Validación: 20 pruebas PG/HTTP/SMTP aprobadas (3 nuevas de puntos y17 de configuración, calendario, recursos, pagos y cancelación), sin fallos ni omitidos. Cubren vacío, dos orígenes, intervalos, costo/cupo0, entradas inválidas, sucursales desactivadas, permisos/CSRF, concurrencia/replay, rollback, revocación, reinicio e historial cancelado. Build/typecheck Next correctos.

Recorrido real Next→Spring→PostgreSQL aprobado: formulario vacío, alta con respuesta perdida y reintento idéntico, dos orígenes, rechazo de franjas superpuestas, código duplicado corregible, búsqueda, ediciones concurrentes con recuperación de datos, recarga, conservación al guardar horarios y rechazo del cliente. Sin errores JavaScript ni5xx. El script se corrigió para esperar el fin del guardado, localizar por identidad en lugar de posición y usar la etiqueta real del checkbox; no se modificó lógica de producto para acomodar esas expectativas.

Cuatro capturas de lista/editor en escritorio/móvil revisadas frente a CFG009: navegación por fases, tarjetas de ubicación, búsqueda, acciones y resumen lateral, adaptados a varios orígenes. En móvil el formulario se apila y las tablas se desplazan internamente, sin desborde de página. El resumen cuenta definiciones y relaciones; todavía no muestra cantidades operativas inexistentes. La revisión visual integral del conjunto sigue pendiente.


## Disponibilidad temporal de puntos · entrega 15

El propietario puede entrar desde el dashboard a **Disponibilidad de puntos** y habilitar o deshabilitar temporalmente una identidad de punto. La primera decisión es explícita: crear una definición no genera un estado habilitado ni deshabilitado. Se muestra **Sin definir** hasta que el propietario elige. No se exige motivo, fecha final, contraseña adicional ni crear otra versión. Se muestra responsable y fecha del último cambio.

El dashboard recuerda los puntos deshabilitados de la preparación actual y enlaza al panel; al rehabilitarlos desaparece el recordatorio. La lista permite buscar por nombre/código/dirección y filtrar estados. Se preserva la estructura visual de tarjetas, estados y resumen lateral de CFG009, separando acciones cotidianas de la definición de dirección/franjas.

Antes de la primera activación se presentan sólo las identidades del borrador, con aviso **Sin configuración activa**. El estado temporal persiste por identidad si se cancela ese borrador, pero las definiciones históricas no se presentan como puntos ofrecibles. Desde la entrega19 se incluyen puntos de la configuración activa y nuevas identidades del borrador; si ambos comparten identidad, se muestra primero la definición vigente. La oferta/confirmación de nuevos pedidos deberá revalidar disponibilidad y capacidad. La oferta, las reservas y la gestión de compromisos existentes siguen En construcción: esta entrega no crea, reasigna ni cancela pedidos. La operación por empleados se integrará con los permisos y bandejas correspondientes.

V15 agrega `disponibilidad_punto_entrega` y `evento_disponibilidad_punto`, separadas de las tablas versionadas. El evento conserva operación, actor, estado anterior/nuevo, versión operativa y fecha. La versión de disponibilidad sirve para rechazar cambios concurrentes; no es una versión de configuración. No se modifica ni se revoca una autorización ligada exclusivamente al contenido del borrador por cambiar disponibilidad. La activación inmediata revalida elegibilidad actual y una huella de la revisión, además de comprobar el contenido autorizado; un cambio relevante exige revisar nuevamente.

Los reintentos están ligados a actor, destino y contenido; devolver un comprobante anterior informa el estado actual sin reaplicar el cambio viejo. Una respuesta incierta mantiene el comando y bloquea otras acciones del panel. Un conflicto exige actualizar y volver a elegir; no sobrescribe automáticamente la decisión ajena. Guardado y auditoría son atómicos. Las rutas de API están limitadas al propietario y protegidas por sesión/CSRF; clientes, empleados y administradores sin propiedad no pueden usarlas.

Validación backend:15 pruebas PG/HTTP/SMTP aprobadas, sin fallos ni omitidos (3 nuevas de disponibilidad y12 de puntos, calendarios y cancelación). Incluyen ausencia de defaults, independencia de configuración/autorización, recordatorio, estados explícitos, permisos/CSRF, concurrencia/replay, destinos/contenido, entradas inválidas, rollback de estado/evento y reinicio. La conservación sin borrador se probó con un fixture de configuración cancelada; la cancelación segura por correo conserva sus pruebas propias. Build/typecheck Next correctos.

El navegador recorrió Next→Spring→PostgreSQL: panel vacío, elección explícita, respuesta perdida y reintento idéntico, recordatorio del dashboard, filtros, conflicto entre editores, recuperación, recarga y rechazo/redirección del cliente. La configuración se comparó antes/después sin diferencias. No hubo errores JS ni respuestas5xx. La primera pasada del script buscaba como enlace un control deshabilitado sin href; se corrigió su localizador sin modificar el comportamiento del producto. La revisión visual detectó y corrigió el selector de estado con estilo nativo, alineándolo con los filtros azules de la administración. Se revisaron escritorio/móvil sin desbordes.


## Simulación de puntos de entrega · entrega 16

El ejemplo temporal de fase5 permite elegir un punto para la sucursal de origen. La API ofrece sólo identidades del borrador con relación habilitada desde una sucursal activa con servicios, disponibilidad temporal **Habilitado** y alguna franja habilitada de cupo positivo. La selección comienza vacía; cambiar origen o modalidad la limpia. El backend vuelve a comprobar esos requisitos al calcular. Si otro administrador deshabilita el punto, la vista actualiza las opciones y conserva la fecha ingresada, sin confundir ese cambio cotidiano con una edición del borrador.

Preparación y traslado consumen ventanas del origen; luego se busca la primera ventana utilizable del punto en su zona IANA. La llegada y la disponibilidad se distinguen y se muestran junto al intervalo, costo, cupo configurado y edición de disponibilidad consultada. Apertura inclusiva y cierre exclusivo: llegar al cierre exige otra ventana. Franjas deshabilitadas o de cupo0 no sirven. **El cupo configurado no representa plazas libres y la simulación no reserva capacidad.** La oferta al cliente y las reservas concurrentes siguen En construcción.

La validación de fase5 usa la misma elegibilidad. Si sólo se ofrecen puntos, exige uno utilizable. Con retiro local configurado, la falta de puntos genera un aviso; los calendarios, tiempos y demás mínimos siguen siendo obligatorios. La simulación no sustituye la revisión integral de fase6 ni la futura activación.

El cálculo común de intervalos locales contempla saltos y repeticiones de horario estacional: no cuenta horas inexistentes ni períodos cerrados entre dos ocurrencias de una hora repetida. Las dos ocurrencias de una misma fecha/franja comparten su cupo configurado; no crean dos capacidades. Esta misma interpretación se deberá conservar al implementar reservas. El horizonte técnico sigue siendo cinco años.

Verificación:22 pruebas de backend aprobadas, con PostgreSQL real en integración y cuatro pruebas puras de calendario/franjas. Cubren límites de intervalos, cierres/fines de semana, zonas distintas, cambios DST, día civil omitido, elegibilidad por origen/disponibilidad/cupo, permisos/CSRF, versión, reinicio y ausencia de escrituras de simulación, además de regresiones de puntos y cancelación. Build/typecheck Next correctos. El recorrido en navegador comprobó dos orígenes con costos/franjas distintos, selección vacía, deshabilitación desde otra sesión, recuperación sin perder datos, retiro alternativo y rechazo de clientes. Capturas de escritorio/móvil revisadas frente a CFG008009, con timeline verde y separación origen/destino, sin erroresJS/5xx ni desbordes de página. La revisión visual integral final sigue pendiente.


## Zonas globales de envío · entrega 17

Desde fase5, **Administrar zonas y cupos** permite definir código estable, nombre, descripción opcional, zona IANA, costo ARS, habilitación, cobertura y franjas. Son decisiones estructurales del borrador, sin valores comerciales predeterminados. Una zona es **global para todas las sucursales**, según el DER: su costo y cupo por fecha/franja se comparten; no se multiplican por origen. Los tiempos de preparación y traslado sí usan el calendario de la sucursal elegida.

La cobertura exige coincidencia de la combinación completa **código postal + localidad + provincia**. Se normalizan Unicode, mayúsculas y espacios; se conservan acentos, y el código postal alfanumérico no distingue espacios. No hay mapas, rangos, comodines ni coincidencias parciales. Una misma combinación no puede pertenecer a dos zonas habilitadas de la misma versión. La base refuerza esta exclusividad incluso al habilitar una zona: primero debe deshabilitarse la otra o quitarse la combinación. Las zonas deshabilitadas conservan sus definiciones.

Se puede guardar una zona sin cobertura o sin franjas para completarla luego. Eso no la hace utilizable: la validación y la simulación exigen habilitación, territorio y una franja habilitada de cupo positivo. Puntos y zonas comparten validación de intervalos (sin superposición por destino/día, contiguos permitidos, cero sin capacidad) y cálculo de ventanas locales. Si una modalidad seleccionada no tiene destinos utilizables pero otra sí, se informa que no será ofrecida; cuando no existe alternativa, la fase queda incompleta. Todos los calendarios y tiempos requeridos siguen siendo obligatorios.

La simulación domiciliaria solicita los tres datos territoriales y muestra zona resuelta, llegada, próxima disponibilidad, ventana, costo y cupo configurados. Rechaza domicilios sin cobertura. El costo no es un cargo registrado y el cupo no representa plazas libres: pedidos y reservas siguen **En construcción**. Se usa horario de24horas y se distingue zona del origen/destino.

V16 agrega identidad global y definición/cobertura versionadas, y permite franjas de punto o zona con destino exclusivo. Las escrituras usan versión, bloqueo, reintento vinculado al contenido/destino/actor, auditoría y revocación de autorizaciones pendientes. Cancelar conserva las definiciones históricas; otro borrador comienza vacío. Los códigos históricos no se reutilizan. La cobertura normalizada y la versión/habilitación vinculada mediante FK permiten que PostgreSQL imponga exclusividad entre zonas habilitadas sin mezclar snapshots.

Verificación:26 pruebas distintas aprobadas (4 nuevas de zonas y22 de calendario, puntos, disponibilidad y cancelación), con PostgreSQL real y pruebas puras de intervalos. Se verifican cobertura/normalización, estados, importes/cupos, intervalos, dos orígenes compartiendo destino, permisos/CSRF, concurrencia/replay, rollback, revocación, historial/reinicio y simulación sin escrituras. Build/typecheck correctos. Navegador Next→Spring→PostgreSQL: formulario vacío, guardado con respuesta perdida, recuperación exacta, solapamiento corregible, editores concurrentes, recarga, dos orígenes, domicilio sin cobertura y cliente rechazado, sin erroresJS/5xx. La lista/editor se adapta al patrón CFG009 y el resultado a CFG008009; se corrigieron estilo del buscador, tarjetas/panel lateral y separación de campos, con capturas de escritorio/móvil sin desbordes. No existe un mock específico de zonas; no se lo presenta como evidencia de diseño aprobada independiente.


## Revisión integral y simulación · entrega 18

La fase6 reúne seis tarjetas del borrador: modelo, pagos, recursos, horarios, entregas y catálogo. Entrar exige mínimos válidos de fase5 y datos guardados; se consulta nuevamente el servidor. Los bloqueos impiden simular; las advertencias permiten revisar combinaciones no disponibles sin inventar oferta. Cada observación enlaza su fase o el catálogo, que abre en otra pestaña; al volver se debe actualizar la revisión. La activación inmediata segura se agregó en la entrega19; la programación y el historial completo siguen En construcción.

Se exige una revisión comercial vigente, una impresora declarada Operativa y combinaciones compatibles de servicio, formato, papel y color por sucursal activa. Cada servicio habilitado localmente debe tener oferta/precio vigente y al menos una combinación producible. Las tarifas globales sin origen compatible generan advertencias y no se ofrecen. El catálogo programado para el futuro no sustituye al vigente. Las programaciones comerciales vencidas se concilian en una transacción independiente antes de revisar: un ejemplo rechazado no deshace su activación comercial.

El ejemplo requiere elegir sucursal/combinación, páginas, copias, caras, modalidad y recepción; las terminaciones son opcionales. Para retiro en punto sólo se ofrecen destinos utilizables según disponibilidad temporal, relación y franjas/cupos, revalidados al calcular; domicilio exige territorio cubierto. Se muestra el desglose y el recorrido económico/operativo, sin subir archivos ni crear cotizaciones, pedidos, pagos o reservas. El precio es calculado en el backend con decimales exactos: tarifa más recargo doble faz por carilla, hojas redondeadas por copia y terminaciones según su base. Impresión no se cobra por segunda vez como servicio. El total incluye entrega antes de evaluar el umbral y la seña; una seña porcentual se redondea a dos decimales. Si el total es cero, no queda una acreditación inexistente bloqueando el upload en pago previo. Los límites técnicos del ejemplo son 10000 páginas y 10000 copias por ítem, hasta 100 terminaciones, con máximo monetario compatible con numeric(19,2); no son reglas comerciales precargadas.

La preparación usa el mayor entre el tiempo global y el mínimo del servicio más lento, sin sumarlos, dentro de las ventanas del origen. La capacidad de carga de papel declarada no limita el tamaño total del trabajo: producción manual puede requerir recargas. La simulación enumera impresoras compatibles, sin consultar consumibles ni conectarse a CUPS. Supone resueltos los requisitos económicos/documentales anteriores para calcular el reloj. Retiro local usa Listo para entregar; puntos y domicilio describen traslado y entrega efectiva por separado.

Ante cambio de edición o revisión comercial, el servidor rechaza el ejemplo con 409 y la vista pide actualizar; conserva campos de cantidad/recepción/domicilio y exige volver a elegir las opciones vigentes. Los reconocimientos de advertencias y resultados se limpian al actualizar. No se modifica el borrador por leer o simular. Esto prepara la futura cotización; todavía no certifica el recorrido E2E de pedidos.

Validación de entrega 18: 19 pruebas backend aprobadas (7 nuevas de precio/revisión y 12 regresiones de pagos, calendarios y programación comercial) con PostgreSQL real, permisos/CSRF, referencia vigente/programada, conflictos, ausencia de escrituras y reinicio. Build/typecheck Next y recorrido en Chrome aprobados: bloqueo de entrada, enlaces, tres modalidades, D1, cambios concurrentes y persistencia del formulario. Capturas de escritorio/móvil contrastadas con MC-ADM-CFG-010-011: seis tarjetas, navegación lateral, resultado verde y observaciones por severidad; formulario móvil en una columna y desglose con desplazamiento interno. El frontend y backend usan una base temporal sólo de verificación; no se agregan datos a la instalación.


## Activación inmediata segura · entrega 19

Después de revisar las advertencias y corregir los bloqueos en fase6, **Continuar a fase7** abre la decisión de aplicación. La selección comienza vacía. Elegir Activar ahora, ingresar un motivo y continuar a Verificación de seguridad; allí se reingresa la contraseña y se solicita el código del buzón Mailpit. La acción final vuelve a validar la revisión y activa la configuración. Programar activación y el historial completo siguen En construcción, dentro del alcance pendiente.

El desafío compartido con cancelación liga propietario, propósito, destino, edición, contenido y versión de acceso de la cuenta. Para activar también incluye la huella integral revisada, revisión comercial, reconocimiento de advertencias y motivo. Sólo persiste el hash del token aleatorio; vence a los 15 minutos, admite cinco intentos y exige un minuto entre emisiones. Errores de credencial consumen intentos persistentes y se registran sin guardar contraseña/código. Volver a aplicación, volver a fase6 o pedir otro código revoca la autorización existente; editar datos guardados también la revoca. Los cambios comerciales o de elegibilidad detectados en la comprobación final exigen revisar nuevamente.

Mientras una respuesta es incierta se conserva el mismo comando en memoria y se bloquean otras acciones. Reintentar una solicitud no vuelve a enviar correo; recuperar una confirmación consumida requiere sus mismos datos, contraseña y código y devuelve la versión aplicada sin autorizar otra transición. Al finalizar se borran las credenciales de la vista. Si falla la consulta posterior del estado, la activación confirmada sigue visible y las acciones de editar ese borrador ya están retiradas; se indica recargar para actualizar el resto del panel. El código no se guarda en localStorage ni se agrega a la configuración. Las sesiones y vencimientos siguen exigiendo nueva autenticación cuando corresponda.

V17 agrega ACTIVA/HISTORICA, índice de una única activa y `activacion_configuracion` con predecesora efectiva, revisión comercial informativa, actor, operación, motivo, fecha y fin de vigencia. Desde base vacía se admite no tener activa; después, la transición cambia ambas versiones en una misma transacción y cierra exactamente la vigencia anterior al comenzar la nueva. El snapshot anterior permanece intacto; las APIs de edición sólo admiten EN_PREPARACION. Si falla almacenamiento, se conservan la activa previa, el borrador y el código sin consumir. Configuración, organización y catálogo se bloquean en un orden común durante la comprobación final. El tarifario conserva su ciclo independiente.

La fase1 muestra la versión activa y permite crear otro borrador vacío. El panel de disponibilidad cotidiana mantiene puntos de la activa junto a nuevas identidades de la preparación, sin modificar snapshots. Todavía no hay cotizaciones/pedidos reales; por eso no se afirma haber verificado su no retroactividad. Esa verificación se completará al implementar confirmación de pedidos.

Verificación: 16 pruebas de backend aprobadas con PostgreSQL/HTTP/SMTP reales (4 nuevas de activación y 12 regresiones de cancelación, revisión y disponibilidad). Cubren primera activación, reemplazo, confirmaciones concurrentes/idempotentes, rollback con fallo inyectado, reinicio, inmutabilidad por API, expiración/intentos, revocación, cambios de edición/catálogo, fallo SMTP, reconocimiento de advertencias, propósito del código y permisos/CSRF. Build/typecheck y recorrido Chrome correctos: formulario sin selección, contraseña/código incorrectos, revocación al volver, respuesta perdida al enviar/confirmar, única activa al recargar, disponibilidad del punto y nuevo borrador independiente. Una comprobación adicional activó V2 con fallo simulado de la consulta posterior: conservó el resultado confirmado y retiró las acciones de edición de V2, preservando V1 como predecesora. Se contrastaron vistas de escritorio/móvil con CFG012/013/014A; no hubo errores JS, 5xx ni desbordes. Los datos de ese recorrido son sólo de una base temporal.
