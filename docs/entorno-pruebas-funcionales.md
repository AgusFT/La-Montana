# Entorno de pruebas funcionales

La actualización de calidad y entrega agrega V46/V47: guía con adicionales reales, confirmaciones breves y código automático al despachar (o al quedar listo para retiro en casa matriz). No requiere reiniciar los datos. Los pedidos que ya estaban en viaje/disponibles recuperan el código al consultar la entrega. Los avisos se capturan en Mailpit, `http://localhost:8125`. Detalle y validaciones pendientes: [Calidad y entrega simples](calidad-y-entrega-simples.md).

La rama `pruebas` agrega una instalación local aislada. No comparte contenedores, volúmenes, base de datos, archivos ni puertos con la ejecución normal de `desarrollo`.

## Iniciar

```bash
git switch pruebas
./infra/pruebas/iniciar.sh
```

Abrir `http://localhost:3100/pruebas`. La pantalla enumera las cuentas ficticias y su propósito. Todos los usuarios utilizan inicialmente `MontanaPrueba2026!`, salvo que se cambie `TEST_USER_PASSWORD` en `.env.pruebas` antes del primer arranque.

## Reiniciar el escenario

```bash
./infra/pruebas/reiniciar-datos.sh
```

El script elimina únicamente los volúmenes del proyecto Docker `lamontana-pruebas` y vuelve a crear el escenario. No ejecuta operaciones contra el proyecto normal `lamontana`.

## Datos incluidos

- Administrador propietario, dos empleados con permisos diferentes y cuatro clientes.
- Casa Matriz Floresta, sucursales Medicina y La Plata.
- Puntos de entrega Caballito y Palermo como entidades distintas de las sucursales.
- Siete formatos (A3/A4/A5/A6/Carta/Folio/Legal), papel común de 75/80/90 g/m² y papel ilustración de 130/170/250 g/m² en A3/A4: 27 combinaciones habilitadas.
- Cuatro revisiones comerciales: cada una tiene 54 tarifas con nombre, precio simple y regla de doble faz, BN/color, y cuatro servicios (impresión, anillado, abrochado y plastificado).
- Tres versiones históricas del motor y una activa. Todos los módulos aplicables están completos: modelo, parámetros de seña, transferencia/efectivo, vigencia, política de archivos, asignación, dos impresoras, formatos, servicios, horarios de siete días, casa matriz, franjas, puntos, zona, coberturas y costos.
- Más de 120 pedidos en seis meses relativos a la fecha del primer arranque. Los cuatro clientes tienen compras anteriores y actividad actual. Hay 16 pedidos recientes distribuidos en los nueve estados del sistema, con incidencias de impresión/calidad y distintos recorridos logísticos.
- PDF reales generados localmente, privados y con huella verificable; sus validaciones históricas se identifican expresamente como simuladas. Las cargas nuevas usan la validación normal de PDF y antivirus.
- Libro de pagos con efectivo, transferencias informadas/acreditadas, aplicaciones, anticipos, saldos, reintegros y auditoría con fechas históricas. El historial de caja se consulta desde los pedidos/cotizaciones y las páginas de la bandeja de pagos existente; no se simulan funciones de cierre de caja inexistentes.

Los pedidos históricos son fixtures ficticios coherentes, construidos con los evaluadores reales de precios, reglas financieras y calendario. Se conservan activas todas las restricciones y los triggers: cada confirmación, revisión, aprobación, impresión, calidad, entrega, cierre y movimiento financiero guarda sus referencias y auditoría. Se comprueban las restricciones diferidas después de cada hecho; no se alteran auditorías ni se deshabilitan controles. Los pedidos actuales pueden continuar desde la UI con las acciones normales.

## Versiones del motor

| Versión | Momento aproximado | Modelo y cambios |
|---|---|---|
| V1 histórica | Hace seis meses | Manual, seña 30 % desde 201 carillas, cotización 12 h, preparación 8 h, traslado 4 h y retención 24 h. |
| V2 histórica | Hace cuatro meses | Condicional por monto, umbral $10.000, seña 30 % al superarlo, nuevos precios, preparación 6 h y retención 48 h. |
| V3 histórica | Hace dos meses | Condicional con seña 40 % siempre, nuevas tarifas, preparación 4 h y retención 72 h. |
| V4 activa | Hace dos semanas | Manual, seña 30 % desde 201 carillas, cotización 24 h, preparación 2 h, traslado 2 h y retención 72 h. |

Las vigencias están encadenadas con predecesora y revisión comercial. Los campos que no corresponden a un modelo (por ejemplo, umbral de aprobación en modo manual) quedan vacíos por regla del sistema.

## Actualizar desde el escenario básico anterior

```bash
git switch pruebas
git pull --ff-only origin pruebas
./infra/pruebas/reiniciar-datos.sh
```

Este reinicio elimina los datos ficticios y los archivos del proyecto `lamontana-pruebas` y reconstruye la simulación. Si se intenta usar la nueva versión con la base del escenario anterior, el backend informa este comando y conserva la base; no la borra automáticamente. En una instalación nueva alcanza con `iniciar.sh`.

## Comprobar el escenario

```bash
cd backend
mvn -Dtest=DatosPruebaIntegrationTest test
```

La prueba utiliza PostgreSQL real y las migraciones del repositorio (incluida V45), comprueba todos los estados, gramajes y tarifas, consultas HTTP con las siete cuentas, aislamiento entre perfiles, históricos de configuración/caja, integridad y vista previa de PDF, continuación operativa e idempotencia tras reiniciar. También verifica la conservación de la base básica anterior. El ensayo agregado para operación diaria de impresoras/puntos compiló, pero su ejecución con V45 queda pendiente en este entorno.

Impresoras y papel y Puntos de entrega tienen accesos diarios fuera del configurador. En una base ya iniciada, el administrador puede asignar a empleados los permisos nuevos Gestionar impresoras y papel y Habilitar puntos de entrega; actualizar no los concede automáticamente. El escenario creado desde cero los incluye en Producción. No hay que borrar pedidos ni reiniciar datos para aplicar V45. Ver `operacion-diaria-impresoras-y-puntos.md`.

La consulta de los PDF es opcional al crear un pedido. Para actualizar una instalación que ya contiene el escenario de seis meses, alcanza con `git pull --ff-only origin pruebas` y `./infra/pruebas/iniciar.sh`; V44 conserva esos datos. Ver [consulta opcional de PDF](cliente-vista-previa-opcional.md).

## Puertos

| Servicio | Pruebas | Normal |
|---|---:|---:|
| Aplicación | 3100 | 3000 |
| Backend | 8180 | 8080 |
| PostgreSQL | 55432 | 5432 |
| Mailpit | 8125 | 8025 |
