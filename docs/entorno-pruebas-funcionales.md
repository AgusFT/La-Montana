# Entorno de pruebas funcionales

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
- Catálogo comercial, papeles, tarifas, servicios y configuración activa.
- Impresora, horarios, franjas de retiro y medios de pago ficticios.

Los pedidos se crean desde las cuentas cliente mediante el flujo real. Esta decisión permite comprobar carga privada, cotización, aceptación, destino, confirmación y seguimiento sin depender de pedidos artificiales que omitan validaciones.

## Puertos

| Servicio | Pruebas | Normal |
|---|---:|---:|
| Aplicación | 3100 | 3000 |
| Backend | 8180 | 8080 |
| PostgreSQL | 55432 | 5432 |
| Mailpit | 8125 | 8025 |
