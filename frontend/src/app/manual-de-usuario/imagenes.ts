type Captura = { archivo: string; ancho: number; alto: number; pie: string };

export const imagenes: Record<string, Captura[]> = {
  "cli-ingresar": [
    { archivo: "cli-ingresar.webp", ancho: 490, alto: 677, pie: "Ingresá con la cuenta que te asignaron. Debajo del botón están el registro de clientes y la recuperación de la contraseña." },
  ],
  "cli-registro": [
    { archivo: "cli-registro.webp", ancho: 490, alto: 867, pie: "Usá tu nombre, apellido y correo para crear la cuenta particular. La comprobación de la contraseña aparece en el mismo formulario." },
  ],
  "cli-recuperar": [
    { archivo: "cli-recuperar.webp", ancho: 490, alto: 1236, pie: "Primero solicitá el código en el formulario superior. Luego completá el código y la nueva contraseña en el segundo formulario." },
  ],
  "cli-seguridad": [
    { archivo: "cli-seguridad.webp", ancho: 700, alto: 837, pie: "Desde Seguridad podés comprobar el correo de tu cuenta y cambiar tu contraseña." },
  ],
  "cli-inicio": [
    { archivo: "cli-inicio.webp", ancho: 1380, alto: 900, pie: "En Inicio, el pedido actual muestra su próximo paso. A la derecha podés abrir los trabajos recientes o pasar al historial." },
  ],
  "cli-buscar": [
    { archivo: "cli-buscar.webp", ancho: 1380, alto: 900, pie: "Elegí la bandeja que corresponde al trabajo. El botón con el número PED abre su detalle." },
  ],
  "cli-cotizaciones": [
    { archivo: "cli-cotizaciones.webp", ancho: 1088, alto: 615, pie: "En cotizaciones guardadas podés comparar el vencimiento y el total de cada oferta antes de abrir su número COT." },
  ],
  "cli-archivos": [
    { archivo: "cli-archivos.webp", ancho: 320, alto: 362, pie: "La lista de Tus archivos muestra las páginas contadas y qué PDF falta configurar. Podés seleccionar otro archivo sin salir de la preparación." },
  ],
  "cli-impresion": [
    { archivo: "cli-impresion.webp", ancho: 456, alto: 386, pie: "Para este PDF se pidieron dos ejemplares. Papel, impresión y caras pertenecen al archivo seleccionado y se pueden elegir por separado para los demás PDF." },
  ],
  "cli-entrega": [
    { archivo: "cli-entrega.webp", ancho: 732, alto: 308, pie: "Seleccioná una modalidad, el destino y el medio de pago. El presupuesto se recalcula con esa combinación." },
  ],
  "cli-confirmar": [
    { archivo: "cli-confirmar.webp", ancho: 340, alto: 684, pie: "Comprobá el total final, la estimación y las condiciones que corresponden a este trabajo. Crear pedido guarda la compra." },
  ],
  "cli-saldo": [
    { archivo: "cli-saldo.webp", ancho: 536, alto: 225, pie: "La tarjeta Método de pago distingue el importe acreditado y aplicado del saldo pendiente. El desplegable muestra los movimientos y sus comprobantes." },
  ],
  "cli-transferencia": [
    { archivo: "cli-transferencia.webp", ancho: 540, alto: 564, pie: "Modificá el importe si transferiste otra cantidad. En esta ventana el comprobante PDF es opcional y se puede adjuntar más tarde." },
  ],
  "cli-cancelar": [
    { archivo: "cli-cancelar.webp", ancho: 600, alto: 301, pie: "La segunda pantalla permite conservar el pedido o confirmar la cancelación; antes de elegir, verificá el número del pedido." },
  ],
  "cli-seguimiento": [
    { archivo: "cli-seguimiento.webp", ancho: 1088, alto: 346, pie: "El hito resaltado muestra dónde está este pedido en viaje a un punto. Esperá la llegada al destino antes de ir a retirarlo." },
  ],
  "cli-retiro": [
    { archivo: "cli-retiro.webp", ancho: 352, alto: 645, pie: "El código aparece en Información del pedido cuando se habilita la recepción. En la imagen está oculto por privacidad." },
  ],
  "cli-corregir": [
    { archivo: "cli-corregir.webp", ancho: 1046, alto: 243, pie: "Leé la solicitud de la imprenta e identificá los PDF marcados. Después de corregirlos, completá la respuesta desde el mismo pedido." },
  ],
  "cli-reprogramar": [
    { archivo: "cli-reprogramar.webp", ancho: 486, alto: 980, pie: "Compará la reserva anterior con la fecha propuesta. La reserva actual sigue vigente hasta que aceptes y se vuelva a comprobar el cupo." },
  ],
  "op-bandejas": [
    { archivo: "op-bandejas.webp", ancho: 1084, alto: 750, pie: "La bandeja de producción distingue una reimpresión, una incidencia y un trabajo pendiente de control." },
  ],
  "op-mostrador": [
    { archivo: "op-mostrador.webp", ancho: 740, alto: 392, pie: "El personal elige entre identificar a un invitado por nombre o buscar la cuenta de un cliente registrado." },
  ],
  "op-revision": [
    { archivo: "op-revision.webp", ancho: 1084, alto: 675, pie: "Un pedido pendiente de revisión muestra la etapa y las opciones para resolver el pago que bloquea la aprobación." },
  ],
  "op-correcciones": [
    { archivo: "op-correcciones.webp", ancho: 988, alto: 577, pie: "La solicitud indica qué PDF necesita cambios y separa el mensaje para el cliente de las observaciones internas." },
  ],
  "op-reprogramar": [
    { archivo: "op-reprogramar.webp", ancho: 618, alto: 440, pie: "La propuesta pide consultar franjas disponibles para una nueva fecha antes de enviarla al cliente." },
  ],
  "op-historial": [
    { archivo: "op-historial.webp", ancho: 1084, alto: 588, pie: "Cada cambio del pedido queda asociado a una fecha, un estado y la persona que lo registró." },
  ],
  "op-cobros": [
    { archivo: "op-cobros.webp", ancho: 540, alto: 595, pie: "La confirmación de cobro propone un importe. Comprobá el medio y el dinero efectivamente recibido antes de guardar." },
  ],
  "op-vencimientos": [
    { archivo: "op-vencimientos.webp", ancho: 992, alto: 348, pie: "El pedido conserva su plazo comercial y la ventana posterior de conservación de los archivos." },
  ],
  "op-producir": [
    { archivo: "op-producir.webp", ancho: 1140, alto: 810, pie: "La preparación identifica el PDF e indica cuáles de las impresoras de la planta pueden realizar ese trabajo." },
  ],
  "op-calidad": [
    { archivo: "op-calidad.webp", ancho: 980, alto: 618, pie: "El inspector compara el trabajo físico con las copias, el papel, la impresión y los adicionales indicados en la guía." },
  ],
  "op-derivar": [
    { archivo: "op-derivar.webp", ancho: 1038, alto: 180, pie: "La derivación solicita la sucursal de destino para producción y el motivo del cambio." },
  ],
  "op-impresoras": [
    { archivo: "op-impresoras.webp", ancho: 350, alto: 497, pie: "La tarjeta de equipo permite informar el papel disponible y suspender nuevas asignaciones durante el mantenimiento." },
  ],
  "op-disponibilidad-puntos": [
    { archivo: "op-disponibilidad-puntos.webp", ancho: 1084, alto: 589, pie: "La vista operativa identifica los puntos y su disponibilidad actual para nuevos pedidos." },
  ],
  "op-despacho": [
    { archivo: "op-despacho.webp", ancho: 700, alto: 393, pie: "Antes de la salida, el personal revisa los pedidos seleccionados y elige el rider que recibe físicamente el grupo." },
  ],
  "op-reparto": [
    { archivo: "op-reparto.webp", ancho: 1121, alto: 688, pie: "El rider ve las paradas del recorrido que le asignaron y los pedidos que debe presentar en cada destino." },
  ],
  "op-domicilio": [
    { archivo: "op-domicilio.webp", ancho: 1077, alto: 173, pie: "Desde el domicilio, el rider puede confirmar la entrega con código o dejar constancia de que no había receptor." },
  ],
  "op-retiro-sucursal": [
    { archivo: "op-retiro-sucursal.webp", ancho: 1084, alto: 349, pie: "El mostrador de la sucursal permite buscar por nombre o pedido y separar los retiros de los trabajos en traslado." },
  ],
  "op-punto": [
    { archivo: "op-punto.webp", ancho: 1280, alto: 755, pie: "El mostrador del punto muestra los pedidos para retirar, su situación y el saldo de cada uno." },
  ],
  "op-cierre": [
    { archivo: "op-cierre.webp", ancho: 1084, alto: 675, pie: "El detalle de un pedido cerrado conserva las etapas completadas y la constancia del cierre." },
  ],
  "op-caja": [
    { archivo: "op-caja.webp", ancho: 1084, alto: 675, pie: "El efectivo acumulado de la sucursal se consulta junto con los movimientos del período y el acceso al arqueo." },
  ],
  "op-egresos": [
    { archivo: "op-egresos.webp", ancho: 1084, alto: 288, pie: "Al registrar un pago, Caja pide el destinatario, el medio y el concepto del dinero ya entregado." },
  ],
  "op-acuerdos": [
    { archivo: "op-acuerdos.webp", ancho: 1084, alto: 138, pie: "Administración puede autorizar una cuenta por punto, uso y medio, y elegir quién verifica la acreditación." },
  ],
  "op-finanzas-punto": [
    { archivo: "op-finanzas-punto.webp", ancho: 1224, alto: 320, pie: "Rendiciones distingue la obligación acumulada, el importe ya informado y el que todavía se puede rendir." },
  ],
  "op-conciliar-puntos": [
    { archivo: "op-conciliar-puntos.webp", ancho: 1084, alto: 135, pie: "La sucursal receptora puede ver el importe informado y confirmar la recepción o rechazarla con motivo." },
  ],
  "op-comprobantes": [
    { archivo: "op-comprobantes.webp", ancho: 992, alto: 416, pie: "El registro del dinero recibido detalla cuánto se aplicó, cuánto se devolvió y qué importe sigue disponible." },
  ],
  "admin-dashboard": [
    { archivo: "admin-dashboard.webp", ancho: 1380, alto: 900, pie: "Cada contador abre la bandeja correspondiente. Los pedidos se agrupan por estado con todos los destinos de la imprenta." },
  ],
  "admin-bandeja": [
    { archivo: "admin-bandeja.webp", ancho: 1380, alto: 900, pie: "Primero elegí el estado. La fila identifica el pedido, su destino y la tarea que corresponde abrir." },
  ],
  "admin-sucursal": [
    { archivo: "admin-sucursal.webp", ancho: 534, alto: 760, pie: "La ficha de Floresta reúne su dirección y horarios. Desde la misma ficha podés editarla o consultar los puntos que abastece." },
  ],
  "admin-empleado": [
    { archivo: "admin-empleado.webp", ancho: 534, alto: 215, pie: "La ficha del empleado permite comprobar en qué sucursales trabaja y qué tareas tiene habilitadas." },
  ],
  "admin-punto": [
    { archivo: "admin-punto.webp", ancho: 1380, alto: 900, pie: "Las prioridades de origen aparecen en la ficha de cada punto. Usá «Editar datos y sucursales» para cambiar sus rutas." },
  ],
  "admin-pausar-punto": [
    { archivo: "admin-pausar-punto.webp", ancho: 1084, alto: 158, pie: "En esta ficha el punto recibe pedidos. «Pausar el punto» suspende nuevas reservas y conserva los compromisos existentes." },
  ],
  "admin-cuenta-punto": [
    { archivo: "admin-cuenta-punto.webp", ancho: 620, alto: 406, pie: "La cuenta se administra por punto. El permiso de cobro se controla aparte de la habilitación de acceso." },
  ],
  "admin-papel-diario": [
    { archivo: "admin-papel-diario.webp", ancho: 1380, alto: 900, pie: "La tarjeta de cada equipo muestra su planta, sus capacidades y el registro de papel. El primer conteo se informa desde la propia tarjeta." },
  ],
  "admin-papeles": [
    { archivo: "admin-papeles.webp", ancho: 1380, alto: 900, pie: "Cada tarjeta agrupa un tamaño. Los gramajes se habilitan por separado según el material que usa la imprenta." },
  ],
  "admin-servicios-base": [
    { archivo: "admin-servicios-base.webp", ancho: 1380, alto: 900, pie: "La lista del catálogo identifica los trabajos que podés configurar después en Tarifas. Usá «Agregar servicio» para preparar otro." },
  ],
  "admin-borrador": [
    { archivo: "admin-borrador.webp", ancho: 1380, alto: 900, pie: "El comienzo del configurador distingue la versión vigente, el próximo cambio y el borrador que podés preparar." },
  ],
  "admin-historial": [
    { archivo: "admin-historial.webp", ancho: 1380, alto: 900, pie: "El historial operativo indica qué versión está activa. Los filtros y «Ver detalle» permiten investigar cambios anteriores." },
  ],
  "admin-revertir": [
    { archivo: "admin-revertir.webp", ancho: 1380, alto: 900, pie: "La revisión de la reversión identifica la versión activa y comprueba si hay una predecesora que se pueda recuperar." },
  ],
  "admin-web-identidad": [
    { archivo: "admin-web-identidad.webp", ancho: 1380, alto: 900, pie: "El nombre de la imprenta y la portada se editan en un borrador. El indicador de la izquierda permite ubicar el paso actual." },
  ],
  "admin-tarifas": [
    { archivo: "admin-tarifas.webp", ancho: 1380, alto: 900, pie: "Al editar una tarifa, revisá qué modos de impresión y hojas incluye. Cada papel y modo puede pertenecer a una sola tarifa." },
  ],
  "admin-precio-servicio": [
    { archivo: "admin-precio-servicio.webp", ancho: 1380, alto: 900, pie: "Elegí el adicional que vas a ofrecer y revisá su unidad de cobro y preparación. Los servicios que figuran en el catálogo también deben habilitarse." },
  ],
  "admin-publicar-precios": [
    { archivo: "admin-publicar-precios.webp", ancho: 1084, alto: 344, pie: "El historial comercial identifica la configuración vigente y su motivo. «Ver configuración» abre sus precios y servicios." },
  ],
  "admin-modelo": [
    { archivo: "admin-modelo.webp", ancho: 1380, alto: 900, pie: "La selección del modelo separa los pedidos que se revisarán manualmente de los sujetos a una condición económica." },
  ],
  "admin-reglas-pago": [
    { archivo: "admin-reglas-pago.webp", ancho: 1380, alto: 900, pie: "El borrador combina los medios de pago con el modelo guardado. Más abajo se definen la seña y los plazos que se aplicarán." },
  ],
  "admin-recursos": [
    { archivo: "admin-recursos.webp", ancho: 1380, alto: 900, pie: "En la fase 4, compará las impresoras vigentes con el borrador y revisá la flota de cada sucursal." },
  ],
  "admin-calendario": [
    { archivo: "admin-calendario.webp", ancho: 1380, alto: 900, pie: "El calendario de producción determina cuándo se preparan trabajos en esa sucursal. Las franjas de retiro tienen sus propios días, horas y cupos." },
  ],
  "admin-zonas": [
    { archivo: "admin-zonas.webp", ancho: 1380, alto: 900, pie: "Cada zona reúne un costo, una cobertura territorial y franjas compartidas. Desde la lista se consulta la existente o se agrega otra al borrador." },
  ],
  "admin-simular": [
    { archivo: "admin-simular.webp", ancho: 1380, alto: 900, pie: "La simulación opcional usa un ejemplo de documento, entrega y pago. Podés comprobar el cálculo sin reservar lugar ni generar un pedido." },
  ],
  "admin-web-imagenes": [
    { archivo: "admin-web-imagenes.webp", ancho: 838, alto: 264, pie: "En los selectores Logotipo e Imagen de portada podés asignar una imagen que ya importaste. Al elegirla se habilita su texto alternativo." },
  ],
  "admin-web-fichas": [
    { archivo: "admin-web-fichas.webp", ancho: 1380, alto: 900, pie: "Cada ficha admite tipo, nombre, descripción, orden y fotos. Marcá «Mostrar ficha» para incluirla en el borrador que vas a revisar." },
  ],
  "admin-web-publicar": [
    { archivo: "admin-web-publicar.webp", ancho: 1380, alto: 900, pie: "Desde la vista previa se puede comprobar el borrador en escritorio y móvil. La publicación se autoriza por separado en el paso siguiente." },
  ],
  "admin-activar": [
    { archivo: "admin-activar.webp", ancho: 1380, alto: 900, pie: "En la fase 7 elegí cuándo aplicar la versión revisada. Hasta que el propietario complete la confirmación de seguridad, la versión en uso sigue vigente." },
  ],
  "admin-primera-configuracion": [
    { archivo: "admin-primera-configuracion.webp", ancho: 1380, alto: 900, pie: "La guía de la primera instalación indica qué ya está guardado y cuál es el siguiente requisito. Entrá al paso pendiente para completarlo." },
  ],
  "admin-asistente": [
    { archivo: "admin-asistente.webp", ancho: 1340, alto: 890, pie: "El recorrido del asistente comienza en «Verificar equipo». Los pasos superiores muestran cuándo vas a elegir el modo y crear la cuenta del propietario." },
  ],
  "admin-propietario": [
    { archivo: "admin-propietario.webp", ancho: 1340, alto: 890, pie: "En una instalación desde fuentes, completá este formulario una sola vez con el token que te entregó el responsable del servidor." },
  ],
  "admin-soporte": [
    { archivo: "admin-soporte.webp", ancho: 802, alto: 760, pie: "Si tu edición incluye Asistencia de YG Soluciones, podés crear la cuenta de soporte desde este formulario. En las instalaciones que ya tienen cuenta aparecen las autorizaciones." },
  ],
  "orientacion-ayuda": [
    { archivo: "orientacion-ayuda.webp", ancho: 1380, alto: 900, pie: "El recuadro de ayuda señala dónde agregar un PDF. Usá «Siguiente» para recorrer la pantalla u «Omitir» para volver a tu trabajo." },
  ],
  "orientacion-espacios": [
    { archivo: "orientacion-espacios.webp", ancho: 1180, alto: 95, pie: "La página pública presenta la imprenta. Desde sus enlaces de acceso podés entrar a tu cuenta o empezar el registro de cliente." },
  ],
  "admin-mantenimiento": [
    { archivo: "admin-mantenimiento.webp", ancho: 898, alto: 200, pie: "Usá «Buscar instalaciones presentes en el sistema» para ubicar una copia existente. Si la búsqueda es parcial, podés ampliarla a otra carpeta o disco." },
  ],
};
