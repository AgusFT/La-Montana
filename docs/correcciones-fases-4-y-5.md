# Correcciones de impresoras y edición de horarios

Revisión con Alejandro del 26/10/2026. Se priorizan fases 4 y 5. La revisión funcional y visual de la fase 6 se hará después.

## Borradores e impresoras

El inicio habitual de un borrador copia la configuración vigente: modelo, pagos, recursos, horarios y entrega. Reutiliza el mecanismo de «Usar como base», registra origen y barrido inicial y exige revisar nuevamente las fases antes de activar. Sólo la primera instalación, sin versión activa, comienza vacía.

Las impresoras conservan sus identificadores, formatos, capacidades y estados, incluidas las retiradas en su historial. No se crean equipos nuevos al copiar parámetros. Modificar el borrador no modifica la versión activa. Una programación o un borrador existente impiden crear otro cambio pendiente; los reintentos recuperan el mismo resultado.

Para un borrador vacío generado por versiones anteriores, la fase 4 ofrece **Recuperar impresoras vigentes**. Esta acción:

- requiere propietario autenticado, protección CSRF y versión de edición vigente;
- recupera sólo impresoras y formatos, sin sustituir pagos, horarios ni otras decisiones;
- registra la versión de origen, conserva identidades y admite reintentos sin duplicación;
- rechaza la recuperación si el borrador ya tiene cualquier impresora, para no sobrescribir su edición;
- mantiene las condiciones de edición: deshabilitar antes de editar o retirar; la activación sigue siendo posterior.

El título de la fase vuelve a «Impresoras y capacidades». La tabla muestra los datos disponibles; disponibilidad de papel, recargas, sensores e histórico de hojas todavía no se implementan con esta corrección y no se presentan cifras simuladas como datos reales.

## Alcance global y ubicación interna

Las impresoras pertenecen a la imprenta completa, no a una sucursal productiva. Fase 4 permite agregarlas sin crear ni seleccionar una sucursal. Para cada equipo puede guardarse una ubicación interna opcional:

- se versiona junto con el resto de la configuración;
- puede quedar vacía;
- puede señalar una sucursal activa o desactivada;
- ayuda al administrador a localizar físicamente el equipo;
- no habilita ni deshabilita servicios, papeles, tarifas o cotizaciones;
- no limita la compatibilidad de la impresora;
- no decide automáticamente dónde se producirá un pedido.

Los servicios y precios provienen de la revisión comercial global. Fase 4 ya no guarda «servicios por sucursal». La asignación concreta de una impresora a un trabajo continúa siendo una decisión manual del personal interno.

El listado permite revisar equipos, habilitarlos o deshabilitarlos, agregar nuevos y editar o retirar únicamente los deshabilitados. Retirar conserva identidad e historia. Para ofrecer impresión debe existir al menos una impresora global Operativa compatible. Para ofrecer retiro en sucursal sí se necesita, en la fase de entrega, una sucursal activa con franjas válidas.

## Horarios con pocos clics

Producción y retiro incorporan una herramienta «Configurar varios días juntos» con dos propuestas de días: lunes a viernes y lunes a sábado. El horario propuesto de 09:00 a 18:00 es editable y sólo se aplica al pulsar el botón. Se pueden elegir días individuales y modificar cada día o franja después.

En producción, el usuario decide si los días no elegidos quedan cerrados o conservan sus valores anteriores. En retiro, indica un cupo positivo y se reemplazan únicamente las franjas de los días elegidos; las de otros días se conservan. Reemplazar valores existentes pide confirmación. Repetir la aplicación no duplica franjas.

El horario de retiro es independiente: puede haber entregas fuera del horario de producción. No suma horas de preparación. Aplicar la propuesta modifica la edición en pantalla; guardar conserva el borrador y activar lo pone en vigencia.

Los borradores copiados heredan sus horarios, por lo que no requieren volver a cargar la semana en cada versión.

## Pendientes separados

La oferta y las capacidades de la imprenta tienen alcance global, según la [definición funcional corregida](alcance-global-imprenta-y-recorrido-cliente.md). La fase 4 ya separa catálogo, capacidades y ubicación física. El recorrido cliente todavía conserva temporalmente un destino previo en su contrato; las opciones que recibe usan el mismo catálogo y las mismas capacidades globales, nunca servicios habilitados localmente. La eliminación completa de esa selección temprana del cliente continúa como trabajo separado. Esta entrega no rediseña la experiencia de la fase 6.

## Verificación

- Pruebas de inicio con/sin configuración vigente, autorización y recuperación idempotente.
- Pruebas de recuperación parcial de equipos y rechazo de sobrescritura.
- Prueba de integración añadida al circuito de activación: copia íntegra, mismas identidades, recuperación de un borrador vacío y conservación de la configuración activa.
- Pruebas de calendarios: excepciones, cierre explícito, horarios de retiro independientes, cupos válidos y repetición sin duplicados.
- Comprobación en navegador de los componentes con API controlada y tamaño móvil.

La prueba de integración PostgreSQL requiere un usuario de ejecución distinto de root. Debe ejecutarse en un entorno compatible antes de considerar certificada esta corrección en PostgreSQL. Las pruebas unitarias y de navegador no reemplazan esa comprobación.
