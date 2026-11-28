import type {RoleLesson} from "./tutorial-recorridos";

const admin="/administracion";
const phaseButton=(n:number)=>`#config-tour-phases nav button:nth-child(${n})`;
const inPhase=(n:number)=>`.configuration-layout[data-fase="${n}"]`;
const cue=(selector:string,text:string,when=selector)=>({when,selector,text});
function openPhase(n:number,label:string){
  return cue(phaseButton(n),`Primero abrí «${label}» en las fases del Configurador.`,`.configuration-layout:not([data-fase="${n}"])`);
}
// Avanzar desde el botón de la aplicación también mueve la guía una sola subtarea.
function nativeNext(n:number, button:string) {
  return {selector:`${button}, ${phaseButton(n)}`,readySelector:inPhase(n)};
}

// Se puede seguir leyendo la guía o abrir las etapas del editor sobre su borrador.
function webCues(step:number, name:string, inside:string, explanation:string) {
  const locked=`.website-configuration:has(nav button:nth-child(${step}):disabled)`;
  return [
    cue(inside,explanation,`.website-configuration[data-fase-web="${step}"]`),
    cue('input[aria-label="Nombre público de la imprenta"]',`Para explorar ${name}, completá el nombre público. También podés seguir leyendo con Continuar en la guía.`,`${locked}[data-fase-web="1"][data-falta-nombre]`),
    cue('input[aria-label="Título de portada"]',`Escribí un título de portada para poder avanzar a ${name}.`,`${locked}[data-fase-web="1"][data-falta-titulo]`),
    cue('#tutorial-web-continuar',`${name} está en el paso ${step}. Para explorarlo, guardá la etapa actual con «Guardar y continuar». Solo cambia el borrador. Para seguir con el recorrido sin editar, usá Continuar en la guía.`,locked),
  ];
}

export const adminLessons:RoleLesson[]=[
  {id:"tablero",title:"Interpretar el tablero",path:admin,tasks:[
    {title:"Leer un indicador",selector:'.dashboard-metric strong[aria-label^="Pendientes de revisión"]',text:"Cada cifra cuenta pedidos de toda la imprenta que están en ese estado. Ubicá «Pendientes de revisión». Ese número te ayuda a decidir qué trabajo atender primero; «Ver todos» abre la lista filtrada."},
    {title:"Comparar las bandejas",selector:'.dashboard-queue[aria-label="Pendientes de revisión"] header h2',text:"Mirá el pedido, la fecha y el destino en la bandeja. El total es el precio del trabajo. Para averiguar cuánto pagó el cliente vas a consultar la sección de cobros del pedido."},
  ]},
  {id:"tarifas",title:"Preparar una tarifa",path:admin+"/catalogo#revision-comercial",tasks:[
    {title:"Elegir el servicio",selector:"#tutorial-tarifa-restaurar, #tutorial-tarifa-servicio",readySelector:'#tutorial-tarifa[data-servicio="true"]',text:"Elegí el servicio de impresión que vas a cotizar. El ejercicio guarda su propio borrador. Si cambió la versión de precios desde tu última práctica, pulsá «empezá desde la tarifa actual» para elegir de nuevo."},
    {title:"Elegir papel y color",selector:"#tutorial-tarifa-restaurar, #tutorial-tarifa-papel",readySelector:'#tutorial-tarifa[data-variante="true"]',text:"Elegí una combinación de formato, gramaje, terminación y color. Así se identifica la tarifa que pagarían las hojas de un pedido. Al seleccionarla aparece su precio actual."},
    {title:"Cambiar el precio por hoja",selector:"#tutorial-tarifa-restaurar, #tutorial-tarifa-precio input",readySelector:'#tutorial-tarifa[data-precio="true"]',text:"Escribí un precio diferente del vigente para una hoja simple faz. Por ejemplo, si sale 100 pesos, probá con 120. Podés usar una coma para los centavos."},
    {title:"Aplicar al borrador",selector:"#tutorial-tarifa-restaurar, #tutorial-tarifa-aplicar",readySelector:'#tutorial-tarifa[data-aplicada="true"]',text:"Pulsá «Aplicar tarifa al borrador de práctica». Se calcula cuánto costarían tres hojas y se conserva el ejercicio. Las cotizaciones de la imprenta siguen usando la tarifa vigente."},
    {title:"Comparar y decidir",selector:"#tutorial-tarifa-publicar",text:"Compará el costo de tres hojas. Para conservar el ejercicio y seguir, usá Continuar en este cuadro. Si decidís que las cotizaciones futuras usen este precio, pulsá «Preparar publicación opcional».",indicaciones:[
      cue('[data-precio-publicado] .save-notice:not(.is-pending) strong',"La imprenta guardó la configuración. Revisá si quedó vigente o programada y continuá el tutorial."),
      cue('#catalog-reason',"Escribí por qué publicás esta tarifa. El motivo queda en el historial comercial.",'[data-publicacion-practica]:not([data-motivo-practica])'),
      cue('[data-publicacion-practica] input[type="datetime-local"]:invalid',"Elegiste programar el precio. Indicá la fecha y la hora UTC para su entrada en vigencia."),
      cue('#tariff-tour-publish button',"Revisá el precio y la vigencia. Pulsá el botón señalado para abrir la confirmación. Podés cancelar para conservar el borrador.",'[data-publicacion-practica]'),
      cue('#tutorial-tarifa-restaurar',"El catálogo tiene una versión nueva. Recuperá la práctica con las tarifas actuales para volver a comparar."),
      cue('#tutorial-tarifa-aplicar:enabled',"Aplicá el nuevo precio al borrador para ver la comparación de tres hojas.",'#tutorial-tarifa:not([data-aplicada])'),
      cue('#tutorial-tarifa-precio input',"Primero ingresá un importe distinto del vigente.",'#tutorial-tarifa:not([data-precio])'),
      cue('#tutorial-tarifa-servicio',"Elegí el servicio con el que vas a calcular el ejemplo.",'#tutorial-tarifa:not([data-servicio])'),
      cue('#tutorial-tarifa-papel',"Elegí una combinación de papel y color para recuperar la práctica.",'#tutorial-tarifa:not([data-variante])'),
    ]},
  ]},
  {id:"catalogo",title:"Consultar catálogo e historial",path:admin+"/catalogo#catalogo-base",tasks:[
    {title:"Consultar papeles",selector:"#catalog-papers-tab",text:"Abrí «Papeles» para ver qué combinaciones de tamaño, gramaje y terminación ofrece la imprenta. Cada tarifa solo puede cotizar las variantes que tiene asociadas."},
    {title:"Consultar servicios",selector:"#catalog-services-tab",readySelector:'#catalog-services-tab[aria-current="step"]',text:"Pulsá «Servicios». Acá podés dar de alta el tipo de trabajo. En Tarifas de servicios se define después su nombre comercial, precio, tiempo y los papeles compatibles, según corresponda."},
    {title:"Comparar versiones",path:admin+"/catalogo#historial-comercial",selector:"#historial-comercial h2",text:"Todavía no hay versiones para comparar. Las próximas publicaciones aparecerán en el historial.",indicaciones:[
      cue('.catalog-history-detail h3',"La versión se abre en modo de lectura. Revisá el precio que estaba vigente en esa fecha y continuá."),
      cue('#historial-comercial tbody tr:first-child button',"Esta fila muestra la versión, el responsable y el motivo del cambio. Pulsá «Ver configuración» para consultar sus precios."),
    ]},
  ]},
  {id:"configurador",title:"Ensayar la operación en borrador",path:admin+"/configuracion",tasks:[
    {title:"Reconocer la configuración vigente",selector:"#config-tour-current h3",indicaciones:[openPhase(1,"Configuración actual")],text:"Este es el número de versión que usa la imprenta. El borrador que vas a revisar se guarda por separado. Antes de editar, identificá si ya hay uno abierto."},
    {title:"Abrir el borrador de trabajo",selector:'#config-tour-start button, #config-tour-resume button',readySelector:'.configuration-layout:not([data-fase="1"])',advanceOnClick:{selector:'#config-tour-start button, #config-tour-resume button',readySelector:'.configuration-layout:not([data-fase="1"])'},indicaciones:[
      cue('#config-tour-resume button',"Ya hay un borrador. Pulsá el botón señalado para seguir trabajando con sus datos."),
      cue('#config-tour-phase-content h2',"Ya abriste el borrador. Continuá para revisar cómo se aprueban los pedidos.",'.configuration-layout:not([data-fase="1"])'),
    ],text:"Pulsá «Crear borrador». Vas a trabajar sobre una copia y los pedidos siguen usando la configuración vigente."},
    {title:"Revisar el modelo de aprobación",selector:"#tutorial-modelo-guardar",readySelector:'[data-modelo-guardado="true"] #tutorial-modelo-continuar:enabled',advanceOnClick:nativeNext(3,"#tutorial-modelo-continuar"),indicaciones:[
      openPhase(2,"Modelo operativo"),
      cue('#tutorial-modelos',"Elegí cómo se aprueban los pedidos. En «Control manual» decide una persona. Si preferís practicar con una regla de pago o importe, elegí «Control condicional».",'#config-tour-model:not(:has(input[name=modelo]:checked))'),
      cue('#tutorial-criterios',"Elegiste control condicional. Marcá la condición que querés ensayar. En la fase siguiente vas a definir los importes o medios de pago.",'#config-tour-criterion:not(:has(input:checked))'),
      cue('#tutorial-modelo-continuar:enabled',"El modelo ya está guardado. Pulsá «Continuar a pagos y reglas». Si preferís cambiarlo primero, elegí el otro modo y volvé a guardar.",'[data-modelo-guardado="true"]'),
    ],text:"Ya hay un modelo seleccionado. El control manual deja la revisión a una persona y el condicional aplica la regla elegida. Para conservar tu selección en este borrador, pulsá «Guardar selección»."},
    {title:"Revisar pagos y seña",selector:"#tutorial-pagos-guardar",readySelector:'[data-pagos-guardados="true"] #tutorial-pagos-continuar:enabled',advanceOnClick:nativeNext(4,"#tutorial-pagos-continuar"),indicaciones:[
      openPhase(3,"Pagos y reglas"),
      cue('#tutorial-medios-pago input:enabled',"Con el pago total previo habilitá transferencia. La acreditación se controla antes de producir.",'#tutorial-medios-pago:has(input:disabled):not(:has(input:checked))'),
      cue('#tutorial-medios-pago',"Elegí uno o ambos medios para que el cliente pueda pagar, por transferencia o en efectivo.",'#tutorial-medios-pago:not(:has(input:checked))'),
      cue('#payment-instructions:invalid',"Escribí las instrucciones que vería el cliente para transferir. Usá datos ficticios para practicar."),
      cue('#quote-duration:invalid',"Indicá cuántos minutos se mantiene el precio de una cotización. Por ejemplo, 60 equivale a una hora."),
      cue('#require-deposit:invalid',"Elegí si el pedido requiere una seña antes de producir o si se paga sin anticipo."),
      cue('.payment-configuration :is(input,textarea,select):enabled:invalid',"Completá el campo señalado según la regla de cobro elegida. Se usa para definir las condiciones del pedido."),
      cue('#tutorial-pagos-continuar:enabled',"Los medios de pago y las reglas ya están guardados. Pulsá «Continuar a recursos».",'[data-pagos-guardados="true"]'),
    ],text:"Revisá los medios de pago y las condiciones de seña. Para conservarlos en el borrador, pulsá «Guardar pagos y reglas»."},
    {title:"Comprobar recursos",selector:"#tutorial-recursos-guardar",readySelector:'.configuration-layout:has([data-asignacion-guardada="true"]) #tutorial-recursos-continuar:enabled',advanceOnClick:nativeNext(5,"#tutorial-recursos-continuar"),indicaciones:[
      openPhase(4,"Recursos"),
      cue('#config-tour-assignment input[name=assignment]',"Seleccioná «Asignación manual». El personal elegirá una impresora compatible al organizar el trabajo.",'#config-tour-assignment:not(:has(input[name=assignment]:checked))'),
      cue('#tutorial-recursos-continuar:enabled',"El método está guardado. Pulsá «Continuar a horarios y entrega».",'[data-asignacion-guardada="true"]'),
    ],text:"Revisá las impresoras y su método de asignación. En la demo la impresión se simula. Guardá el método elegido para conservarlo en el borrador."},
    {title:"Comprobar horarios y entrega",selector:"#guardar-horarios",readySelector:'#tutorial-entrega-continuar:enabled',advanceOnClick:nativeNext(6,"#tutorial-entrega-continuar"),indicaciones:[
      openPhase(5,"Horarios y entrega"),
      cue('#main-branch',"Elegí una sucursal activa como casa matriz. Su calendario permite calcular cuándo va a estar listo el trabajo.",'#main-branch:has(option[value=""]:checked), #config-tour-matrix:has(.admin-warning)'),
      cue('#config-tour-matrix input:not(:checked)',"Confirmá la casa matriz marcando esta casilla. La copia requiere tu confirmación aunque ya tenga una sucursal seleccionada."),
      cue('#tutorial-entrega-continuar:enabled',"Los horarios y las modalidades quedaron validados. Pulsá «Continuar a revisión final» para abrir la fase 6."),
      cue('#config-tour-delivery-validation .delivery-issues li',"La validación indica lo que falta en el borrador. Corregí el dato señalado y guardá otra vez.",'[data-horarios-guardados="true"]'),
    ],text:"La matriz está confirmada. Revisá los horarios y las modalidades que usarán los pedidos. Pulsá «Guardar horarios y modalidades»."},
    {title:"Abrir una simulación",selector:'.optional-simulation summary',indicaciones:[openPhase(6,"Revisión final")],readySelector:'.configuration-layout[data-fase="6"] .optional-simulation[open]',advanceOnClick:{selector:'.optional-simulation:not([open]) summary',readySelector:'.configuration-layout[data-fase="6"] .optional-simulation[open]'},text:"Desplegá «Simulación opcional del recorrido completo» para probar las reglas del borrador con un ejemplo de impresión."},
    {title:"Simular y leer el resultado",selector:"#tutorial-simulacion-calcular",readySelector:".review-result",indicaciones:[
      openPhase(6,"Revisión final"),
      cue('.optional-simulation:not([open]) summary',"Abrí de nuevo la simulación. Vas a encontrar los datos que habías completado."),
      cue('.review-result h3',"Compará el total, el pago previo y el plazo estimado. Simulaste un pedido sin consumir un cupo. Continuá cuando termines de leer."),
      cue('#review-combination:invalid',"Elegí el servicio, la sucursal y el papel del ejemplo. La combinación define los precios disponibles."),
      cue('#tutorial-simulacion-paginas:invalid',"¿Cuántas páginas tiene tu PDF? Probá con 3 para calcular un trabajo pequeño."),
      cue('#tutorial-simulacion-copias:invalid',"Indicá cuántas copias del PDF querés imprimir. Con 3 páginas y 2 copias se cotizan 6 carillas."),
      cue('#review-sides:invalid',"Elegí simple o doble faz. Esto cambia cuántas hojas consume el ejemplo."),
      cue('#review-mode:invalid',"Elegí cómo recibiría el trabajo el cliente, por ejemplo, retiro en sucursal."),
      cue('#tutorial-simulacion-recepcion:invalid',"Elegí la fecha y hora UTC en que entraría el trabajo. La simulación aplica el calendario a partir de ese momento."),
      cue('#tutorial-simulacion-codigoPostal:invalid',"Para simular el envío ingresá el código postal de una zona con cobertura."),
      cue('#tutorial-simulacion-localidad:invalid',"Ingresá la localidad del domicilio de ejemplo."),
      cue('#tutorial-simulacion-provincia:invalid',"Ingresá la provincia del domicilio de ejemplo."),
    ],text:"Los datos del ejemplo están completos. Pulsá «Simular recorrido completo» para obtener el precio, el pago previo y la entrega estimada."},
    {title:"Decidir sobre la activación",selector:"#config-tour-review-actions button:last-child",indicaciones:[openPhase(6,"Revisión final")],text:"Este botón abre la decisión de activación cuando la revisión está completa. Para terminar la práctica con tu borrador, pulsá Continuar en la guía. La activación real requiere una autorización y verificación aparte."},
    {title:"Abrir el historial de versiones",selector:phaseButton(8),readySelector:inPhase(8),indicaciones:[
      cue('.history-table tbody tr:first-child td:first-child',"Esta versión tiene su estado, vigencia y responsable en la misma fila. Continuá cuando termines de consultarla."),
      cue('#config-tour-phase-content h2',"Ya estás en el historial de versiones. Continuá cuando termines de consultarlo.",inPhase(8)),
    ],text:"Pulsá la fase 8 para ver las versiones guardadas, quién aplicó cada una y cuándo."},
  ]},
  {id:"web",title:"Conocer la página de la imprenta",path:admin+"/pagina-web",tasks:[
    {title:"Reconocer la identidad pública",selector:'.website-configuration nav button:first-child',indicaciones:[cue('.website-configuration input[aria-label="Nombre público de la imprenta"]',"Este nombre identifica la imprenta ante los clientes. La identidad guardada en el borrador se publica cuando vos lo decidís.")],text:"El asistente de la web administra la identidad, imágenes, productos y contenido que vería tu cliente. Revisá el nombre público y observá qué edición del borrador está guardada."},
    {title:"Ubicar las imágenes de identidad",selector:'.website-configuration nav button:nth-child(3)',indicaciones:webCues(3,"Biblioteca de imágenes",'.website-configuration select[aria-label="Logotipo"]',"Acá elegís un logotipo entre tus imágenes importadas. También podés elegir una portada y escribir su texto alternativo."),text:"Biblioteca de imágenes permite importar archivos desde la carpeta que elegís en esta computadora, seleccionar un logotipo o una portada y describirlos para quienes usan lector de pantalla. Si el paso está pendiente, «Guardar y continuar» desde la identidad habilita el siguiente paso."},
    {title:"Preparar una tarjeta del catálogo",selector:'.website-configuration nav button:nth-child(4)',indicaciones:webCues(4,"Productos y servicios","#tutorial-web-agregar-ficha","«Agregar ficha» abre un producto o servicio del catálogo público. Completá el nombre, descripción e imágenes si decidís probarlo."),text:"En Productos y servicios cada tarjeta reúne lo que ofrecés, su descripción y sus imágenes. Podés preparar una tarjeta y dejarla en borrador mientras revisás su texto. La tarifa comercial que cotiza la imprenta se configura en Servicios y precios."},
    {title:"Ubicar la vista previa",selector:'.website-configuration nav button:nth-child(5)',indicaciones:webCues(5,"Contenido y vista previa",".website-configuration .resource-toolbar h3","La vista previa privada muestra el diseño con los datos del borrador. Comprobá la lectura de la portada y el catálogo antes de publicar."),text:"Contenido y vista previa muestra cómo se verán la portada, el catálogo y el contacto. Revisá la lectura y las imágenes en móvil y escritorio antes de decidir si querés publicar."},
    {title:"Distinguir borrador y publicación local",selector:'.website-configuration nav button:nth-child(6)',indicaciones:webCues(6,"Revisar y publicar",".web-publish-confirm","Esta confirmación pertenece a la publicación local de la web. Para conservar el borrador, seguí desde el cuadro del tutorial."),text:"Los seis pasos terminan en una vista previa y la publicación local. Podés explorar los pasos habilitados y guardar el que edites. La dirección local abre la web de esta computadora; el alojamiento remoto está en desarrollo."},
  ]},
  {id:"sucursales",title:"Ubicar la atención al cliente",path:admin+"/sucursales",tasks:[
    {title:"Revisar dirección y horarios",selector:".branch-list .branch-card h3, .branch-list .empty-note",text:"Buscá la sucursal de ejemplo. Leé su dirección, zona horaria, días y horas de atención. En «Editar datos y estado» se mantienen esos datos. La casa matriz del circuito se confirma en la fase 5 del Configurador."},
  ]},
  {id:"operacion",title:"Consultar pedidos y cobros",path:admin+"/pedidos",tasks:[
    {title:"Filtrar la bandeja",selector:'select[aria-label="Filtrar por estado"]',text:"Elegí un estado para localizar pedidos o volvé a «Todos los pedidos». La bandeja reúne el trabajo de la imprenta con independencia del destino de retiro."},
    {title:"Interpretar el total y el saldo",selector:'select[aria-label="Filtrar por estado"]',indicaciones:[cue(".client-table tbody tr:first-child td:nth-child(6)","Este es el total del pedido, incluidos sus conceptos y entrega. En la ficha, la sección Pagos distingue el cobro recibido de su aplicación. En el tutorial de flujo completo vas a practicar ese cobro.")],text:"Si no hay pedidos, elegí otro estado para comparar sus importes. La ficha de cada pedido reúne archivos, producción, entrega y pagos. El total refleja el importe del pedido. En Pagos se distingue lo recibido de lo que se aplicó a ese pedido. En el Tutorial de flujo completo vas a registrar y aplicar un cobro."},
  ]},
  {id:"cupos",title:"Interpretar los cupos",path:admin+"/cupos",tasks:[
    {title:"Consultar el saldo de recursos",selector:'.demo-quota-cards button[data-recurso="PEDIDOS"]',text:"Elegí un recurso para ver sus registros y cuánto espacio de práctica queda. En Pedidos, confirmar uno nuevo ocupa una plaza. La baja y sus requisitos se revisan en cada registro; con consultar los cupos alcanza para este ejercicio."},
  ]},
  {id:"ayuda",title:"Encontrar ayuda y reconocer los límites",path:admin,tasks:[
    {title:"Reconocer las funciones bloqueadas",selector:".demo-locked-title",text:"Los candados de Puntos de entrega, Empleados y Riders identifican vistas restringidas en esta demo. La edición completa incorpora su gestión según la instalación. Analytics y Chat Bot aparecen como módulos prémium futuros; consultá a YG por su disponibilidad."},
    {title:"Ubicar la asistencia",selector:'.admin-sidebar a[href="/administracion/soporte"]',text:"Desde Asistencia de YG Soluciones podés identificar cuándo interviene el equipo de soporte y qué acceso se autoriza. Para practicar alcanza con ubicar este enlace."},
    {title:"Encontrar el manual",selector:'.admin-sidebar a[href="/manual-de-usuario"]',text:"El Manual de usuario está disponible dentro de la instalación. Después del tutorial podés consultar los pasos, las capturas y los capítulos de Administración aunque estés sin Internet."},
    {title:"Identificar la seguridad de la cuenta",selector:'.admin-sidebar a[href="/cuenta/seguridad"]',text:"En Seguridad de la cuenta vas a encontrar las acciones de identidad disponibles para tu usuario. Para finalizar la práctica, conservá las credenciales de la demo y terminá el recorrido desde este cuadro."},
  ]},
];
