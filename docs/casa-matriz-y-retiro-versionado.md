# Casa matriz y retiro versionado · 27/10/2026

## Decisión y alcance

La imprenta es una sola. Sus servicios, papeles, tarifas e impresoras son globales. La ubicación de una impresora es información interna opcional y no determina qué puede cotizar un cliente. Esta entrega agrega una casa matriz explícita por versión del motor para distinguir el retiro local del retiro que necesita traslado.

La regla se aplica a la primera configuración y a todos los borradores posteriores. No se selecciona automáticamente la primera sucursal ni se convierte una ubicación de impresora en matriz.

## Sucursales y Fase 5

- Al crear o editar una sucursal se puede marcar **Proponer esta sucursal como casa matriz**. Solo hay una propuesta y debe estar activa. Es una preferencia para preparar la configuración, no una modificación de la vigente.
- **Fase 5 → Casa matriz de esta configuración** exige elegir una sucursal activa y confirmar expresamente la elección. Es una sola referencia por versión, independiente de la propuesta de la ficha.
- Un borrador desde la vigente o desde una histórica hereda la selección, pero no su confirmación. Se revisan recursos dinámicos y se vuelve a confirmar. Cambiar el selector desmarca la confirmación.
- Se pueden guardar avances incompletos. La revisión integral, la activación inmediata y la programación no admiten una matriz ausente, desactivada o sin confirmar.
- La matriz necesita su calendario de preparación completo. Una secundaria puede tener únicamente franjas de retiro, incluso con producción cerrada. Las franjas necesitan habilitación y cupo positivo; no deben quedar obligatoriamente dentro del horario productivo.
- No se puede desactivar ni dar de baja lógica a la matriz vigente o programada. Primero se activa otra configuración con el reemplazo, o se cancela la programación correspondiente. Se mantiene la baja lógica de las sucursales.
- La Fase 6 y el historial muestran la matriz y su confirmación. No se agregan permisos ni se omite la seguridad reforzada de publicación.

## Cálculo y seguimiento

| Destino | Estimación |
|---|---|
| Retiro en casa matriz | Preparación y próxima franja de retiro utilizable |
| Retiro en otra sucursal | Preparación + traslado y próxima franja del destino |
| Punto de entrega | Preparación + traslado y franja habilitada del punto desde la matriz |
| Domicilio | Preparación + envío y franja/cobertura de la zona |

Se conserva el evaluador de horas operativas: preparación y traslado estimados usan el calendario de referencia de la matriz. Los mínimos de servicios siguen aplicándose. Un traslado de cero horas debe indicarse explícitamente y no elimina la obligación de registrar la llegada a otra sucursal.

Ejemplo de escritorio: matriz lunes 09:00–17:00, preparación 2 horas, traslado 1 hora; recepción lunes 09:00. El trabajo termina estimativamente a las 11:00. Retiro en matriz: desde las 11:00 si su franja lo permite. Otra sucursal con retiro 19:00–20:00: llegada estimada 12:00 y retiro estimado desde las 19:00, aunque allí no se produzca.

La fecha estimada no adelanta estados ni obliga a esperar. El personal registra **preparado → en viaje → llegada al destino de retiro** después de calidad. El cliente ve los hechos y sus fechas reales en el timeline, incluso si suceden antes de lo estimado. Llegada no significa entrega física; se informa la franja de retiro. Código personal, saldo, entrega efectiva y cierre siguen teniendo sus verificaciones independientes.

## Punto de corte y no retroactividad

Al confirmar el pedido, el servidor vuelve a comprobar reglas, disponibilidad, archivos y condiciones. La clasificación del retiro se obtiene de la **versión efectivamente confirmada en el pedido**, no de la matriz vigente hoy ni de la estimación inicial de una cotización anterior. Esa versión y el destino preservados determinan si se necesita traslado. Activar otra matriz posteriormente no cambia pedidos anteriores.

La migración **V41** agrega columnas sin asignar una matriz arbitraria a las versiones anteriores. Las activas legadas mantienen su cálculo anterior; sus próximos borradores deben elegir y confirmar la matriz. Una programación anterior sin matriz explícita no se publica saltando este requisito: debe cancelarse y prepararse nuevamente.

Usar una histórica como base siempre abre un borrador con confirmación pendiente. La reversión protegida existente sigue siendo una operación distinta: muestra y exige reconocer expresamente la matriz de la predecesora, revalida compatibilidad y conserva contraseña/código. Si la predecesora no tiene matriz explícita, se bloquea la reversión directa y se indica usarla como base pasando por Fase 5.

## Alcance pendiente del cliente

Este cambio no reemplaza todavía el formulario inicial de cotización por el nuevo recorrido global en tres partes. Sigue pendiente retirar de esa pantalla el concepto de «sucursal que realizará el trabajo» y resolver automáticamente el origen interno. Mientras se conserva el contrato heredado, el retiro en otra sucursal calcula desde la matriz; los puntos se cotizan con origen en la matriz y rechazan un origen incompatible. Esto es una limitación transitoria de integración, no una oferta de servicios por sucursal.

## Verificación de esta entrega

- Backend: 32 pruebas dirigidas aprobadas, incluyendo matriz, copia/reconfirmación, retiro fuera de producción, traslados, baja de matriz e impresoras/seguridad. Compilación de las fuentes de integración y empaquetado de producción.
- Frontend: tipado, build y suites de timeline, recorrido y horarios. El timeline incluye casos de llegada anticipada, estimación sin movimiento real y cotización con matriz distinta de la confirmada.
- Navegador: componente real de Fase 5 con API simulada; selección, confirmación, cambio que desmarca, payload guardado, selección heredada sin confirmar y avance bloqueado. No es una prueba HTTP integrada contra PostgreSQL.
- SQL: ejecución de las 41 migraciones y comprobación de restricciones en PostgreSQL embebido WebAssembly aislado (PGlite): sin matriz arbitraria, confirmación con selección, FK y propuesta única que no cambia la versión.
- No se repitió la suite HTTP completa de integración con PostgreSQL nativo/Docker en este entorno. No se alteraron datos de una instalación real ni se realizó un despliegue productivo.

## Cómo probar desde una instalación existente

```bash
git pull --ff-only origin desarrollo
./infra/iniciar-local.sh
```

El script reconstruye e inicia los contenedores y Flyway aplica V41; no hace falta otro `docker compose up`. No borrar volúmenes ni reiniciar la base. Crear un borrador, revisar Fase 5, seleccionar/confirmar la matriz, guardar y comprobar Fase 6 antes de autorizar una activación.
