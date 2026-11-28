import type { RoleLesson } from "./tutorial-recorridos";

const prepare = "/cliente/cotizaciones/nueva";
const checkout = "/cliente/preparaciones/:preparacion";
const order = "/cliente/pedidos/:pedido";
const printField = (name: string) => `#client-work-options select[aria-label="${name}"]`;

// Mientras está abierto el visor nativo, la indicación acompaña la operación pendiente.
const reviewTarget = [
  "#client-review-files button",
  '[data-tutorial-pdf="volver"]',
  '.workspace-review-dialog:not(:has(.private-preview input[type=checkbox])) [data-tutorial-pdf="abrir"]',
  '.private-preview[data-tutorial-revision="paginas"] .client-actions',
  '.private-preview[data-tutorial-revision="leida"] input[type=checkbox]:not(:checked)',
  '.private-preview:has(input:checked) [data-tutorial-pdf="aceptar"]',
].join(", ");

export const clientLessons: RoleLesson[] = [
  {
    id: "inicio",
    title: "Conocer mi cuenta",
    path: "/cliente",
    tasks: [
      { title: "Leer el pedido actual", selector: ".client-current-order h2", text: "Inicio reúne tus trabajos como cliente de la imprenta. Buscá el pedido actual: el número PED identifica el trabajo; el estado indica si sigue en revisión, en producción o listo para retirar. Si todavía no tenés uno, acá aparece la invitación a prepararlo." },
      { title: "Interpretar el resumen", selector: ".client-dashboard-counts", text: "«Pedidos realizados» cuenta todo tu historial. «Pedidos en curso» cuenta los que siguen abiertos. Un presupuesto aún sin confirmar se consulta en Mis cotizaciones." },
    ],
  },
  {
    id: "cotizaciones",
    title: "Consultar mis cotizaciones",
    path: "/cliente/cotizaciones",
    tasks: [
      { title: "Distinguir una cotización de un pedido", selector: "[data-tutorial-cotizaciones] h2", text: "Una cotización conserva el precio, las condiciones y su vencimiento. Podés abrir una oferta con su número COT. Cuando creás un pedido a partir de la oferta, obtenés un número PED y empieza el seguimiento." },
      { title: "Ubicar el comienzo del trabajo", selector: '.client-content a[href="/cliente/cotizaciones/nueva"]', text: "«Preparar un pedido» abre la mesa de trabajo para cargar y configurar tus archivos. En el siguiente paso vamos a presupuestar uno antes de decidir si querés confirmarlo." },
    ],
  },
  {
    id: "archivo",
    title: "Preparar y presupuestar el PDF",
    path: prepare,
    tasks: [
      { title: "Cargar o retomar el archivo", selector: '[data-tutorial-vincular], #client-upload .client-dropzone button:enabled, #client-upload .workspace-file-choice', readySelector: '#client-upload[data-practica-pdf="listo"]', download: { href: "/tutorial/afiche-senderos.pdf", label: "Guardar PDF de práctica" }, text: "Guardá el afiche de práctica o elegí un PDF de hasta 10 MiB. Usá «Seleccionar PDF» y esperá a que se lean sus páginas. Si ya venías preparando un trabajo, comprobá el nombre y elegí «Usar esta preparación en el tutorial» para seguir con ese mismo archivo." },
      { title: "Elegir el servicio", selector: printField("Servicio de impresión"), readySelector: printField("Servicio de impresión") + ":valid:enabled", text: "Elegí el servicio que hará la imprenta para este archivo. Al elegirlo se habilitan los tamaños de hoja compatibles." },
      { title: "Elegir el tamaño", selector: printField("Tamaño de hoja"), readySelector: printField("Tamaño de hoja") + ":valid:enabled", text: "Elegí el formato de hoja, por ejemplo A4 si aparece entre las opciones. El tamaño determina qué papeles podés usar." },
      { title: "Elegir el papel", selector: printField("Papel y gramaje"), readySelector: printField("Papel y gramaje") + ":valid:enabled", text: "Elegí un papel. El gramaje indica su peso por metro cuadrado y te ayuda a distinguir una hoja liviana de otra de mayor cuerpo." },
      { title: "Elegir la impresión", selector: printField("Impresión"), readySelector: printField("Impresión") + ":valid:enabled", text: "Elegí blanco y negro o color entre las opciones disponibles para tu papel. La imprenta usa esta combinación para buscar la tarifa." },
      { title: "Ajustar las copias y las caras", selector: '#client-work-options input[aria-label="Copias"]', readySelector: '#client-work-options input[aria-label="Copias"]:valid:enabled', text: "Probá con tres copias. Elegí simple faz o doble faz si está disponible para ese papel. Los adicionales compatibles aparecen debajo. Podés cambiar estos valores antes de crear el pedido." },
      { title: "Calcular el presupuesto", selector: "#client-budget-action button", external: true, text: "Comprobá que todos los archivos figuren configurados y pulsá «Revisar presupuesto». La imprenta calcula el subtotal con sus tarifas. La guía pasa al resultado cuando el cálculo queda guardado." },
    ],
  },
  {
    id: "presupuesto",
    title: "Revisar el total y decidir",
    path: checkout,
    tasks: [
      { title: "Leer el subtotal", selector: "#client-budget .workspace-budget-items", text: "Revisá nombres, páginas, copias y precio de cada PDF. Todavía falta calcular la entrega. Con «Editar trabajo» podés volver a cambiar la impresión." },
      { title: "Elegir cómo recibirlo", selector: '#client-delivery select[aria-label="Modalidad"]', readySelector: '#client-delivery[data-destino-listo="true"]', text: "Elegí retiro o envío entre las modalidades habilitadas. Para retiro, seleccioná la sucursal. Para envío, completá la dirección y los datos de localidad. Así se puede estimar fecha y costo." },
      { title: "Elegir el medio de pago", selector: '#client-delivery select[aria-label="Medio de pago"]', readySelector: '#client-delivery[data-total-vigente="true"]', text: "Elegí un medio habilitado y esperá el cálculo del total. El resumen muestra cuánto corresponde a impresión y cuánto a entrega. Esta elección todavía no registra un pago." },
      { title: "Revisar todas las páginas", selector: reviewTarget, readySelector: 'body:not(:has(dialog[open])) #client-review-files[data-aceptados="true"]', text: "Abrí «Revisar archivos» y «Ver páginas». Recorré cada página con «Página siguiente». Si el contenido es correcto, marcá la casilla y pulsá «Aceptar vista previa». Repetí para cada PDF y volvé al resumen." },
    ],
  },
  {
    id: "decision",
    title: "Decidir si crear el pedido",
    path: checkout,
    tasks: [
      { title: "Elegir cómo terminar la práctica", selector: "[data-tutorial-decision-cliente]", external: true, text: "Para conservar el trabajo y ver cómo se sigue un pedido existente, pulsá «Conservar la preparación y ver el seguimiento». Para un pedido propio, elegí «Revisar para crear este pedido», completá los datos, aceptá el total y pulsá «Confirmar / Crear pedido». Solo al confirmarlo se ocupa una de las 20 plazas de pedidos de la demo." },
    ],
  },
  {
    id: "seguimiento",
    title: "Abrir un pedido existente",
    path: "/cliente/pedidos",
    tasks: [
      { title: "Consultar la lista", selector: 'select[aria-label="Filtrar por estado"]', text: "Mis pedidos reúne los trabajos confirmados. Cada fila tiene su número PED, estado, modalidad de entrega y total. Si aplicás un filtro, volvé a «Todos los pedidos» para ver el ejemplo de la demo." },
      { title: "Abrir su seguimiento", selector: '[data-tutorial-consultar-pedido]', external: true, text: "Elegí «Ver PED» en un pedido de la lista. Vamos a leer su ficha real. Consultarlo no modifica su estado, tu preparación ni el cupo." },
    ],
  },
  {
    id: "ficha",
    title: "Seguir las etapas del pedido",
    path: order + "#pedido-resumen",
    tasks: [
      { title: "Reconocer el estado y la reserva", selector: ".client-order-destination", text: "La franja reservada indica cuándo se estima recibir el trabajo. Mirá el estado del pedido para saber si ya está listo. El resumen conserva las condiciones que aceptaste al confirmarlo." },
      { title: "Consultar los PDF", path: order + "#pedido-pdf", selector: "#pedido-pdf .private-file-item h3", text: "Archivos PDF reúne las versiones confirmadas de este pedido. Podés volver a consultar sus páginas cuando estén disponibles." },
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
