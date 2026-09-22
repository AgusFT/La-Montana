# La Montaña · v0.1

Piloto local en construcción con Spring Boot, Next.js y PostgreSQL. Esta rama `desarrollo` reemplaza la implementación deprecada; el código anterior sigue disponible en el historial Git.

## Estado actual · entrega 1

Implementado: estructura nueva, compilación del backend/frontend, esquema inicial con Flyway, conexión real a PostgreSQL, endpoints de estado/salud, almacenamiento privado preparado y pantalla inicial con estados reales. Compose incluye base, backend, frontend y buzón local Mailpit.

**Todavía no se puede crear el administrador ni iniciar sesión.** Configuración, usuarios, sucursales, pedidos, pagos, PDF y operación se implementan en las siguientes entregas. Las vistas indican **En construcción**. No hay credenciales de aplicación ni datos comerciales precargados. La migración V1 únicamente prepara/versiona el esquema; el modelo de negocio llegará en migraciones posteriores.

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

Mailpit está preparado como infraestructura; el envío de códigos se integrará con identidad/configuración. Su SMTP dentro de Compose será `mailpit:1025`.

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

Validación del 22/09/2026: Maven `verify` pasó 2 pruebas integradas contra PostgreSQL 18.6; `pnpm typecheck` y `pnpm build` pasaron con Node 24.19; el servidor standalone respondió HTTP 200 en `/health` y mostró el aviso correcto en `/` sin backend. Compose pasó `config --quiet` y el script pasó validación de sintaxis. Las imágenes Docker, el arranque conjunto, la persistencia de volúmenes y la inspección visual en navegador siguen sin verificar por las limitaciones del entorno. El build usa la API de TypeScript 5 mediante una opción soportada de Next; no se omitió la comprobación de tipos.

Para desarrollar sin contenedores, proporcionar `DB_URL`, `DB_USER`, `DB_PASSWORD` y opcionalmente `FILES_DIR` al backend, apuntando a un PostgreSQL propio. El frontend requiere `BACKEND_INTERNAL_URL` y se inicia con `pnpm dev`. Los directorios de archivos no se publican como recursos web. Solo los GET de estado/salud son públicos; el resto de rutas queda cerrado y no existe usuario automático de Spring.

Comprobación manual de esta entrega: iniciar, abrir la aplicación y verificar el estado conectado; detener solo el backend y volver a comprobar para ver el aviso de conexión; reiniciarlo y comprobar la recuperación. Login y acciones comerciales deben continuar señalados como pendientes.

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

Siguiente entrega: alta única del propietario protegida por token local; usuarios y sesiones persistentes; registro/login/recuperación; administración de sucursales y empleados con autorización por sucursal. Luego configurador/catálogo, recorrido PDF/pagos y operación/entrega. Resolver el acceso local a Docker antes de certificar el arranque conjunto y la persistencia de sus volúmenes.

Este README es el único documento manual de producto. Se actualiza con lo efectivamente terminado, pruebas y decisiones relevantes en cada entrega.
