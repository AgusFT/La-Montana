| Rol | Usuario |
|---|---|
| Administrador | `admin@prueba.local` |
| Producción | `produccion@prueba.local` |
| Caja | `caja@prueba.local` |
| Encargada Casa Matriz Floresta | `sucursal.floresta` |
| Encargada Sucursal Medicina | `sucursal.medicina` |
| Encargada Sucursal La Plata | `sucursal.la.plata` |
| Rider Oeste · Martina Ruta | `rider.oeste@prueba.local` |
| Rider Centro · Nicolás Reparto | `rider.centro@prueba.local` |
| Punto Caballito | `punto.caballito.retiro` |
| Punto Palermo | `punto.palermo.retiro` |
| Punto La Plata Centro | `punto.laplata-centro.retiro` |
| Punto Tolosa | `punto.tolosa.retiro` |
| Punto City Bell | `punto.city-bell.retiro` |
| Cliente 1 | `cliente1@prueba.local` |
| Cliente 2 | `cliente2@prueba.local` |
| Cliente 3 | `cliente3@prueba.local` |
| Cliente 4 | `cliente4@prueba.local` |
| Contraseña común | `MontanaPrueba2026!` |

Los riders sólo visualizan los recorridos y pedidos que les fueron asignados. Las cuentas de punto acceden únicamente al mostrador y las finanzas de su propio punto. Cada cuenta `sucursal.*` ve los pedidos, retiros e impresoras de su propia sucursal. El administrador conserva la vista global y puede derivar un pedido aprobado antes de iniciarlo o después de registrar un intento fallido, cancelado u observado para reimpresión.

## Actualizar e iniciar la última versión

**Requisitos:** Docker Desktop en ejecución, Docker Compose y Git. En Windows, ejecutar los comandos desde **Git Bash** (por ejemplo, desde la terminal de VS Code) y ubicarse en la **raíz del repositorio** `La-Montana`.

### Actualizar la rama `pruebas` sin borrar datos

```bash
git status
git switch pruebas
git pull --ff-only origin pruebas
bash ./infra/pruebas/iniciar.sh
```

- `git status` permite comprobar si existen cambios locales antes de actualizar. Si los hay y bloquean el cambio de rama o el pull, conservarlos y resolver el conflicto antes de continuar; **no ejecutar `git reset --hard` ni `git clean -fd` para forzar la actualización**.
- `git pull --ff-only origin pruebas` trae los cambios recientes sin crear un merge automático.
- `iniciar.sh` ejecuta Docker Compose con `--build --detach --wait`, reconstruye los contenedores y aplica las migraciones pendientes. **Conserva los volúmenes y los datos existentes**.
- Si la rama `pruebas` ya está seleccionada y actualizada, basta con repetir `bash ./infra/pruebas/iniciar.sh`.

### Reiniciar el escenario de pruebas desde cero

**⚠️ Operación destructiva — sólo cuando realmente se quiera descartar el estado actual del entorno de pruebas.**

```bash
bash ./infra/pruebas/reiniciar-datos.sh
```

Este script ejecuta `docker compose ... down --volumes` **únicamente sobre el proyecto Docker `lamontana-pruebas`** y, a continuación, vuelve a llamar a `iniciar.sh`. Elimina la base y los archivos persistidos de **ese entorno de pruebas**, reconstruye los datos ficticios y vuelve a levantar los servicios. **No usarlo para actualizar código**. No elimina expresamente los volúmenes de la instalación normal `desarrollo`, que usa otro proyecto Docker, pero los datos anteriores de `pruebas` se pierden.

### Direcciones locales de pruebas

| Servicio | Dirección local | Puerto |
|---|---|---|
| Aplicación y acceso a las cuentas de prueba | http://localhost:3100/pruebas | `3100` |
| Correos de prueba (Mailpit) | http://localhost:8125 | `8125` |
| Backend/API (servicio de pruebas) | http://localhost:8180 | `8180` |
| PostgreSQL (base de pruebas) | `localhost:55432` | `55432` |

Estas direcciones corresponden a los valores predeterminados que genera el script en `.env.pruebas` durante el primer inicio. Si ese archivo ya existe y sus puertos se modificaron, prevalecen sus valores. Los puertos de la instalación normal son distintos.

### Acceso de los usuarios de ejemplo

Usar los nombres de usuario/correos de la tabla de este documento. La contraseña de muestra es `MontanaPrueba2026!`, salvo que el entorno se haya inicializado con otro valor de `TEST_USER_PASSWORD` o se hayan cambiado las credenciales. **Son cuentas ficticias de un entorno local, no credenciales de producción.**
