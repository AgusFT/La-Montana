# Cliente: preparación y confirmación en paneles

Mejora UX/UI · 27/10/2026. Continúa el contrato de [tres etapas](cliente-tres-etapas.md), usando MC-CLI-003, 004 y 005 como referencias de composición.

## Decisión

Las etapas 1 y 2 aprovechan el ancho disponible en lugar de desplegar todos los formularios y el visor uno debajo de otro. Se conservan la identidad azul/blanca, las acciones naranjas y el recorrido de tres etapas.

- **Preparación:** lista de PDF, editor del archivo seleccionado y adicionales en paneles paralelos. Sólo hay un editor abierto. Cambiar de archivo conserva sus opciones locales; Revisar presupuesto sigue guardando el trabajo completo y calcula exclusivamente en el servidor.
- La carga se vuelve compacta al haber archivos. La lista tiene desplazamiento propio y mantiene accesibles hasta veinte PDF sin crear veinte formularios. Se muestran análisis, páginas, archivo seleccionado y cantidad configurada.
- **Confirmación:** fichas compactas del trabajo, entrega y pago en módulos; total y confirmación al costado en escritorio. El resumen lateral permanece visible mientras se desplaza la página en los anchos que admiten dos columnas.
- Los detalles secundarios de servicios y condiciones pueden desplegarse. Total, destino, estimación, revisión humana y anticipos exigibles siguen visibles cuando hay una oferta actualizada.
- **Revisión de PDF:** ventana modal con lista de archivos y visor en paralelo. En pantallas pequeñas se reordena. Se reutiliza `PrivateFiles`, con un modo compacto optativo; los demás usos conservan su presentación.
- La ventana usa el comportamiento modal nativo, nombre accesible, cierre por botón/Escape y retorno del foco al control que la abrió. Una carga/aceptación pendiente o incierta conserva el bloqueo y su recuperación.
- En tabletas y móviles los paneles se reordenan; se permite el desplazamiento vertical necesario para leer y operar sin reducir los controles ni provocar desborde horizontal.

## Reglas conservadas

No cambian los endpoints ni el modelo persistido. El presupuesto sigue incluyendo todos los archivos de la preparación; cada PDF requiere análisis y aceptación explícita de todas sus páginas. Aceptar un subconjunto no habilita la confirmación. Cambiar entrega o pago invalida el consentimiento del resumen anterior. La confirmación conserva versión, huella y operación idempotente, así como los requisitos económicos y operativos del backend.

No hay migración ni cambios de precios, permisos, casa matriz o destinos. Tampoco se modifica el seguimiento del pedido ni el flujo legado de cotización/correcciones.

## Implementación

- `frontend/src/components/quote-form.tsx`: selección de archivo y editor único.
- `frontend/src/components/prepared-checkout.tsx`: módulos y resumen lateral.
- `frontend/src/components/prepared-file-review.tsx`: apertura, bloqueo, cierre y foco del visor.
- `frontend/src/components/private-files.tsx`: modo compacto opcional, manteniendo el flujo de aceptación existente.
- `frontend/src/app/client-workspace.css`: estilos limitados a estas dos etapas y al visor modal.
- `frontend/src/components/client-context-tour.tsx`: ayudas adaptadas a los controles actuales.

## Verificación

Build de producción y TypeScript del frontend, junto con las 56 pruebas frontend/proxy existentes. Revisión Chromium de los componentes React reales con **API simulada**, separada de los datos de una instalación.

Escenarios de interfaz comprobados: opciones independientes al alternar dos archivos; veinte PDF con un solo editor y lista acotada; total recalculado según destino; aceptación bloqueada antes de abrir todas las páginas; un PDF aceptado no habilita un pedido de dos PDF; habilitación al aceptar ambos y el resumen; nuevo bloqueo al cambiar destino; cierre del visor y recuperación del foco. Ambas etapas sin desborde horizontal a 320, 390, 768 y 1440 px; domicilio y visor también revisados a 390 px.

Estas pruebas visuales no son una certificación integral contra Docker/PostgreSQL/ClamAV. La aceptación final se hace en la instalación local con los datos del equipo.
