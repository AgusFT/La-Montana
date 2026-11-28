import type { RoleLesson } from "./tutorial-recorridos";

const prepare = "/cliente/cotizaciones/nueva";
const checkout = "/cliente/preparaciones/:preparacion";
const order = "/cliente/pedidos/:pedido";
const printField = (name: string) => `#client-work-options select[aria-label="${name}"]`;

function point(selector: string, text: string) { return { when: selector, selector, text }; }

const uploadButton = "#client-upload .client-dropzone button";
const acceptedFile = '#client-upload[data-practica-pdf="listo"] .workspace-file-choice[aria-pressed="true"]';
const readyForDecision = 'body:not(:has(dialog[open])) #client-review-files[data-aceptados="true"]';
const address = (field: string) => `#client-delivery .workspace-address input[name="${field}"]`;
const pdf = (action: string) => `.workspace-review-dialog [data-tutorial-pdf="${action}"]`;
const unreadPdf = '.workspace-review-dialog .private-preview[data-pdf-aceptado="false"][data-tutorial-revision="paginas"][data-pdf-visto="true"]';
const firstUnreadAction = (action:string) => `${unreadPdf}[data-pdf-pendientes-delante="${action==="siguiente-pagina"}"] [data-tutorial-pdf="${action}"]:enabled`;

export const clientLessons: RoleLesson[] = [
  {
    id: "inicio",
    title: "Conocer mi cuenta",
    path: "/cliente",
    tasks: [
      { title: "Leer el pedido actual", selector: ".client-current-order header", text: "Inicio reúne tus trabajos como cliente de la imprenta. Buscá el pedido actual: el número PED identifica el trabajo; el estado indica si sigue en revisión, en producción o listo para retirar. Si todavía no tenés uno, acá aparece la invitación a prepararlo." },
      { title: "Interpretar el resumen", selector: ".client-dashboard-counts", text: "«Pedidos realizados» cuenta todo tu historial. «Pedidos en curso» cuenta los que siguen abiertos. Un presupuesto aún sin confirmar se consulta en Mis cotizaciones." },
    ],
  },
  {
    id: "cotizaciones",
    title: "Consultar mis cotizaciones",
    path: "/cliente/cotizaciones",
    tasks: [
      { title: "Distinguir una cotización de un pedido", selector: "[data-tutorial-cotizaciones] .client-muted", text: "Una cotización conserva el precio, las condiciones y su vencimiento. El número COT identifica esa oferta en la lista. Cuando creás un pedido a partir de la oferta, obtenés un número PED y empieza el seguimiento." },
      { title: "Ubicar el comienzo del trabajo", selector: '.client-content a[href="/cliente/cotizaciones/nueva"]', external: true, text: "Pulsá «Preparar un pedido» para abrir la mesa de trabajo. Ahí vas a cargar y configurar un PDF antes de decidir si querés confirmar el pedido." },
    ],
  },
  {
    id: "archivo",
    title: "Preparar y presupuestar el PDF",
    path: prepare,
    tasks: [
      { title: "Cargar o retomar el archivo", selector: uploadButton, indicaciones: [
        point("[data-tutorial-vincular]", "Ya hay una preparación abierta. Revisá el nombre del PDF y elegí «Usar esta preparación en el tutorial» si querés practicar con ese trabajo."),
        point(acceptedFile, "El PDF seleccionado ya fue validado. En esta fila podés leer el nombre y la cantidad de páginas. Pulsá «Continuar» para configurar su impresión."),
      ], readySelector: '#client-upload[data-practica-pdf="listo"]', download: { href: "/tutorial/afiche-senderos.pdf", label: "Guardar PDF de práctica" }, text: "Pulsá «Seleccionar PDF» para abrir el explorador de archivos y elegí un PDF de hasta 10 MiB. Si necesitás un ejemplo, guardá el afiche desde esta guía. La imprenta verificará el archivo y contará las páginas." },
      { title: "Elegir el servicio", selector: printField("Servicio de impresión"), readySelector: printField("Servicio de impresión") + ":valid:enabled", text: "Elegí el servicio que hará la imprenta para este archivo. Al elegirlo se habilitan los tamaños de hoja compatibles." },
      { title: "Elegir el tamaño", selector: printField("Tamaño de hoja"), readySelector: printField("Tamaño de hoja") + ":valid:enabled", text: "Elegí el formato de hoja, por ejemplo A4 si aparece entre las opciones. El tamaño determina qué papeles podés usar." },
      { title: "Elegir el papel", selector: printField("Papel y gramaje"), readySelector: printField("Papel y gramaje") + ":valid:enabled", text: "Elegí un papel. El gramaje indica su peso por metro cuadrado y te ayuda a distinguir una hoja liviana de otra de mayor cuerpo." },
      { title: "Elegir la impresión", selector: printField("Impresión"), readySelector: printField("Impresión") + ":valid:enabled", text: "Elegí blanco y negro o color entre las opciones disponibles para tu papel. La imprenta usa esta combinación para buscar la tarifa." },
      { title: "Ajustar las copias y las caras", selector: '#client-work-options input[aria-label="Copias"]', readySelector: '#client-work-options input[aria-label="Copias"]:valid:enabled', indicaciones: [
        point('#client-work-options input[aria-label="Copias"]:focus', "Escribí cuántas veces querés imprimir el documento completo. Al terminar, pulsá Tab para elegir las caras."),
        point('#client-work-options[data-caras-editadas="true"] .workspace-additions-list', "Estas son las terminaciones disponibles para la combinación elegida. Marcá solo las que quieras aplicar al archivo, o continuá sin adicionales."),
        point('#client-work-options[data-copias-editadas="true"]:has(input[aria-label="Copias"]:valid) select[aria-label="Caras"]', "Elegí simple faz para imprimir un solo lado de cada hoja. Si el papel admite doble faz, podés seleccionarla para usar ambos lados. Conservá la opción que prefieras. Con Tab podés pasar a los adicionales disponibles."),
      ], text: "Cada copia repite tu documento completo. Probá escribir 3 en «Cantidad de copias». Después revisaremos si conviene imprimir una o dos caras." },
      { title: "Calcular el presupuesto", selector: "#client-budget-action button", external: true, text: "Comprobá que todos los archivos figuren configurados y pulsá «Revisar presupuesto». La imprenta calcula el subtotal con sus tarifas. La guía pasa al resultado cuando el cálculo queda guardado." },
    ],
  },
  {
    id: "presupuesto",
    title: "Revisar el total y decidir",
    path: checkout,
    tasks: [
      { title: "Leer el subtotal", selector: "#client-budget .workspace-budget-items", text: "Revisá nombres, páginas, copias y precio de cada PDF. El subtotal cubre la impresión y los adicionales. En el paso siguiente vas a elegir la entrega para calcular el total final." },
      { title: "Elegir cómo recibirlo", selector: '#client-delivery select[aria-label="Modalidad"]', readySelector: '#client-delivery[data-destino-listo="true"]', indicaciones: [
        point('#client-delivery select[aria-label="Destino de retiro"]', "Elegí en qué sucursal lo vas a retirar. La dirección distingue los puntos y permite que la imprenta calcule el traslado."),
        point(address("calle")+":is(:invalid,:focus)", "Escribí el nombre de la calle de entrega. Usá una dirección ficticia y pulsá Tab para escribir el número."),
        point(address("numero")+":is(:invalid,:focus)", "Indicá la altura o número del domicilio donde se recibe el pedido."),
        point(address("codigoPostal")+":is(:invalid,:focus)", "Ingresá el código postal del destino. Es uno de los datos con los que se comprueba la cobertura."),
        point(address("localidad")+":is(:invalid,:focus)", "Escribí la localidad que corresponde al domicilio."),
        point(address("provincia")+":is(:invalid,:focus)", "Completá la provincia. Con Tab podés agregar referencias de entrega; también podés continuar directamente al medio de pago."),
        point(address("referencias"), "La dirección ya tiene los datos obligatorios. Podés agregar piso u otras referencias de entrega si hacen falta. Después pulsá «Continuar»."),
      ], text: "Elegí la modalidad habilitada que querés practicar. Si retirás, se te pedirá una sucursal; si elegís envío, vas a completar la dirección." },
      { title: "Elegir el medio de pago", selector: '#client-delivery select[aria-label="Medio de pago"]', readySelector: '#client-delivery[data-total-vigente="true"]', text: "Elegí un medio habilitado y esperá el cálculo del total. El resumen muestra cuánto corresponde a impresión y cuánto a entrega. Esta elección todavía no registra un pago." },
      { title: "Revisar todas las páginas", selector: "#client-review-files button", readySelector: readyForDecision, indicaciones: [
        point(pdf("volver"), "Ya aceptaste todos los archivos. Pulsá «Volver al resumen» para continuar con la decisión final."),
        point(pdf("aceptar")+":enabled", "Pulsá «Aceptar vista previa» para guardar tu aceptación de esta versión del archivo."),
        point(pdf("lectura")+":enabled:not(:checked)", "Llegaste al final de este PDF. Si lo que viste coincide con lo que querés imprimir, marcá la casilla de revisión."),
        point(firstUnreadAction("siguiente-pagina"), "Pulsá «Página siguiente» para ver la próxima hoja. Repetí hasta recorrer todas las páginas del PDF."),
        point(firstUnreadAction("pagina-anterior"), "Quedan páginas anteriores sin revisar. Volvé con «Página anterior» para leerlas antes de aceptar."),
        point('.workspace-review-dialog .private-preview[data-pdf-visto="false"] h3', "Esperá mientras se carga esta página. Cuando esté visible podrás seguir revisando el PDF."),
        point(pdf("abrir"), "Pulsá «Ver páginas» en el PDF pendiente. Comprobá que el archivo y el contenido correspondan al trabajo."),
        point(readyForDecision+" .workspace-review-prompt strong", "Todos los PDF están aceptados y la ventana de revisión está cerrada. Pulsá «Continuar» para decidir si querés crear un pedido."),
      ], text: "Pulsá «Revisar archivos» para comprobar el contenido de cada PDF antes de decidir si lo enviás a la imprenta." },
    ],
  },
  {
    id: "decision",
    title: "Decidir si crear el pedido",
    path: checkout,
    tasks: [
      { title: "Elegir cómo terminar la práctica", selector: "[data-tutorial-decision-cliente]", external: true, indicaciones: [
        point('#client-create-order[data-decision-confirmar] input[name="receptor"]:is([data-tutorial-decision-cliente],:focus)', "Escribí el nombre de la persona que recibirá el envío. Para esta práctica podés usar un nombre ficticio."),
        point('#client-create-order[data-decision-confirmar] input[name="telefono"]:is([data-tutorial-decision-cliente],:focus)', "Indicá un teléfono ficticio para la recepción de esta entrega de práctica."),
        point('input[type="checkbox"][data-tutorial-decision-cliente]', "Leé el total final, los datos de entrega y las condiciones. Si coinciden con tu pedido, marcá esta casilla."),
        point('button[data-tutorial-decision-cliente]', "Pulsá «Confirmar / Crear pedido» para registrarlo una sola vez, reservar una de las veinte plazas y abrir el seguimiento de tu pedido."),
      ], text: "Elegí uno de los dos botones. «Conservar la preparación» guarda tu trabajo y permite consultar un pedido existente sin ocupar cupo. «Revisar para crear este pedido» te lleva a la confirmación de un pedido propio." },
    ],
  },
  {
    id: "seguimiento",
    title: "Abrir un pedido existente",
    path: "/cliente/pedidos",
    tasks: [
      { title: "Consultar la lista", selector: 'select[aria-label="Filtrar por estado"]', text: "Mis pedidos reúne los trabajos confirmados. Cada fila tiene su número PED, estado, modalidad de entrega y total. Si aplicás un filtro, volvé a «Todos los pedidos» para ver el ejemplo de la demo." },
      { title: "Abrir su seguimiento", selector: '[data-tutorial-consultar-pedido]', external: true, text: "Pulsá el «Ver PED» señalado para abrir la ficha de ese pedido. Consultarlo no modifica su estado, tu preparación ni el cupo." },
    ],
  },
  {
    id: "ficha",
    title: "Seguir las etapas del pedido",
    path: order + "#pedido-resumen",
    tasks: [
      { title: "Reconocer el estado y la reserva", selector: ".client-order-destination", text: "La franja reservada indica cuándo se estima recibir el trabajo. Mirá el estado del pedido para saber si ya está listo. El resumen conserva las condiciones que aceptaste al confirmarlo." },
      { title: "Consultar los PDF", path: order + "#pedido-pdf", selector: "#pedido-pdf .private-file-item h3", indicaciones: [
        point('#pedido-pdf .private-preview [data-tutorial-pdf="siguiente-pagina"]:enabled', "Usá «Página siguiente» si querés revisar el resto del documento. Cuando termines, podés continuar para ver los pagos."),
        point('#pedido-pdf .private-preview h3', "Esta es la versión que quedó vinculada al pedido. La consulta no cambia el archivo confirmado."),
        point('#pedido-pdf [data-tutorial-pdf="consultar"]', "Acá podés abrir las páginas del archivo confirmado. Elegí «Ver páginas del pedido» si querés consultarlas, o continuá hacia los pagos."),
      ], text: "En Archivos PDF figuran las versiones confirmadas. Un archivo se puede volver a consultar mientras su contenido siga disponible." },
      { title: "Leer los pagos", path: order + "#pedido-pagos", selector: ".payment-heading", text: "Acá distinguís un importe pendiente, una transferencia informada y un pago que la imprenta ya acreditó y aplicó. Revisá el saldo de este pedido." },
      { title: "Consultar correcciones", path: order + "#pedido-correcciones", selector: "#pedido-correcciones h2", text: "Si la imprenta pide un cambio, acá aparece la solicitud y cómo responderla. Si ves que no hay correcciones, el pedido puede seguir su recorrido habitual." },
      { title: "Revisar la fecha", path: order + "#pedido-reprogramacion", selector: "#pedido-reprogramacion h2", text: "Esta sección muestra las propuestas de cambio de fecha. La reserva vigente solo cambia cuando corresponde y se acepta la nueva franja." },
      { title: "Leer la entrega", path: order + "#pedido-entrega", selector: ".delivery-panel h2", text: "La imprenta controla producción y calidad. Cuando el pedido esté listo, esta sección te permitirá generar un código de recepción para mostrar al personal. Un pedido que aún está aprobado puede seguir pendiente de impresión." },
    ],
  },
  {
    id: "ayuda",
    title: "Encontrar ayuda y volver al trabajo",
    path: "/cliente",
    tasks: [
      { title: "Ubicar el manual", selector: '.client-sidebar a[href="/manual-de-usuario"]', text: "En «Manual de usuario» tenés instrucciones con capturas disponibles sin conexión. Desde Inicio podés volver a crear o seguir trabajos. Si conservaste la preparación, entrá a «Crear pedido» para recuperar sus archivos." },
      { title: "Reconocer los límites de la demo", selector: '.client-sidebar .demo-locked-nav', text: "El candado identifica las opciones que no podés usar en esta demo. Analytics y Chat Bot figuran como módulos prémium futuros. Reclamos figura en construcción. Las tareas de imprimir, verificar pagos y entregar corresponden al personal de la imprenta." },
    ],
  },
];
