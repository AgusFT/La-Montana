# Consulta opcional de PDF

En las etapas 1 y 2, **Archivos adjuntos** se presenta cerrado. Al desplegarlo, **Ver mis PDF** abre el visor en una ventana con navegación entre páginas, descarga y regreso al trabajo/resumen. No registra aceptación ni exige visitar las páginas.

La etapa 2 conserva visibles el total, el destino y la ventana estimada. Las aclaraciones del cálculo están cerradas en **Cómo se estima la entrega**, y las condiciones completas mantienen su propio desplegable. Las señas y anticipos aplicables permanecen visibles.

**Crear pedido** confirma el resumen actual en un solo paso, sin una casilla de aceptación de archivos. El servidor sigue verificando identidad, versión, huella del resumen, disponibilidad y condiciones económicas. La aceptación comercial de la cotización se registra al crear el pedido; consultar una vista previa no es un requisito.

## Original a imprimir e historial

La migración aditiva V44 conserva las aceptaciones anteriores como `VISTA_PREVIA`. Los nuevos registros técnicos se distinguen con `ENVIO_PEDIDO`: identifican el PDF enviado con el pedido o la respuesta de corrección, sin afirmar que el cliente abrió o aceptó el visor. En las respuestas de la API, `aceptadaEn` permanece nulo para estos registros.

Se conservan las referencias obligatorias del original, propietario, SHA-256, antivirus, inspección PDF y páginas reales. Un trigger diferido exige que cada registro de envío tenga su pedido o respuesta en la misma transacción. No se modifican las migraciones anteriores ni la auditoría histórica. La revisión humana de la imprenta y las condiciones de producción mantienen sus controles.

## Verificación

- `pnpm typecheck`, `pnpm test:journey` (13 pruebas) y `pnpm build`.
- `mvn -Dtest=VistaPreviaOpcionalIntegrationTest test`: carga real de PDF de dos páginas, envío sin consultar/aceptar el visor, rechazo de huella desactualizada y original alterado, repetición idempotente, corrección sin aceptación de vista previa, revisión/aprobación y producción. Verifica aislamiento del propietario, rechazo de PDF inválido/antivirus y conservación del historial anterior. El antivirus usa el fixture de protocolo de test; no sustituye una comprobación con ClamAV real.
- `mvn -Dtest=DatosPruebaIntegrationTest test` (3 pruebas): escenario histórico, consultas con siete cuentas, catálogo, caja, continuidad operativa, reinicio e aislamiento de perfiles.
- Revisión visual interactiva pendiente: `sites-preview` añade argumentos de Vite incompatibles con `next dev` y no llegó a iniciar la vista. No se modificó el comando de desarrollo para acomodar esta limitación del entorno.

## Actualizar la instalación de pruebas actual

```bash
git switch pruebas
git pull --ff-only origin pruebas
./infra/pruebas/iniciar.sh
```

V44 se aplica al arrancar. No hace falta reiniciar los datos ficticios ni borrar volúmenes para este cambio.
