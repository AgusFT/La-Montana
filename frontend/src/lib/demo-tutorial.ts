//#region ENCABEZADO · src/lib/demo-tutorial.ts
// Describe los nueve pasos, sus requisitos, los roles y las rutas del pedido.
//#endregion
import type { Role } from "./roles";
import { orderReview } from "./tutorial-revision-pedido";
import type { ReviewField } from "./tutorial-recorridos";

export const steps = [
  "tarifa",
  "archivo",
  "presupuesto",
  "confirmacion",
  "cobro",
  "revision",
  "produccion",
  "calidad",
  "entrega",
] as const;
export type Step = (typeof steps)[number];
export type Draft = Record<string, string>;
export type Tutorial = {
  intento: number;
  estado: "NUEVO" | "ACTIVO" | "PAUSADO" | "COMPLETO";
  paso: Step;
  completados: Step[];
  preparacion: string | null;
  pedido: string | null;
  cotizacion: string | null;
  borradores: Partial<Record<Step, Draft>>;
  bloqueo?: string | null;
};
export type Command = {
  accion:
    | "iniciar"
    | "retomar"
    | "pausar"
    | "ir"
    | "tarifa"
    | "vincular"
    | "borrador";
  paso?: Step;
  preparacion?: string;
  campos?: Draft;
};
type Hint = {
  selector: string;
  title: string;
  text: string;
  revision?: ReviewField[];
  role?: Role;
  mode?: "RETIRO_SUCURSAL" | "ENVIO_DOMICILIO";
};
type Lesson = {
  title: string;
  role: Role;
  intro: string;
  result: string;
  hints: Hint[];
};

const readingPdf = '[data-tutorial-revision="paginas"]';
const pdfPageAction = [
  `${readingPdf}[data-pdf-visto="true"][data-pdf-pendientes-delante="true"] [data-tutorial-pdf="siguiente-pagina"]:enabled`,
  `${readingPdf}[data-pdf-visto="true"][data-pdf-pendientes-delante="false"] [data-tutorial-pdf="pagina-anterior"]:enabled`,
  `${readingPdf}[data-pdf-visto="false"] [data-tutorial-pdf="numero-pagina"]`,
  '[data-tutorial-revision="leida"] [data-tutorial-pdf="lectura"]',
].join(", ");

export const lessons: Record<Step, Lesson> = {
  tarifa: {
    title: "Consultar la tarifa",
    role: "ADMIN_ADMIN",
    intro:
      "La imprenta usa estas tarifas para calcular los presupuestos. Cada tarjeta muestra el precio según el papel y el modo de impresión.",
    result:
      "Tarifa consultada. Ahora podés pasar al rol Cliente y cargar el PDF que vas a cotizar.",
    hints: [
      {
        selector: '[data-tutorial-tarifa-leida="true"]',
        title: "Leer el precio por hoja",
        text: "Leé el importe de «Simple faz»: es lo que cuesta imprimir una cara. Recordá el papel y el color para el pedido y confirmá la lectura.",
      },
    ],
  },
  archivo: {
    title: "Cargar el PDF",
    role: "CLIENTE",
    intro:
      "Ahora sos quien encarga las copias. La imprenta necesita el PDF para contar sus páginas y calcular el trabajo. Podés usar el afiche de práctica o un PDF propio.",
    result:
      "Tu PDF ya está analizado. Junto al nombre figura la cantidad de páginas que se usará en el presupuesto.",
    hints: [
      {
        selector: `[data-tutorial-vincular], #client-upload .client-dropzone button:not(:disabled)`,
        title: "Seleccionar el PDF",
        text: "Si ya tenías una preparación, elegí si querés usarla en el tutorial. Para usar el afiche, pulsá «Guardar PDF de práctica» en la guía. Después elegí «Seleccionar PDF» en Tus archivos y abrí el archivo que guardaste.",
      },
      {
        selector: ".workspace-file-choice",
        title: "Comprobar las páginas",
        text: "Esperá a que termine el análisis. En Tus archivos, elegí el PDF que vas a imprimir y comprobá su nombre y sus páginas en Detalles de impresión.",
      },
    ],
  },
  presupuesto: {
    title: "Configurar y presupuestar",
    role: "CLIENTE",
    intro:
      "Vas a ponerle precio al PDF. Cada elección habilita las opciones compatibles de la siguiente casilla. Para reconocer la tarifa del primer paso, usá el mismo papel y modo de impresión.",
    result:
      "Ya tenés el subtotal de impresión. Ahora podés elegir dónde retirar el pedido y cómo pagarlo.",
    hints: [
      {
        selector:
          '#client-work-options select[aria-label="Servicio de impresión"]',
        title: "Elegir el servicio",
        text: "En «Servicio de impresión», elegí Impresión para que aparezcan los tamaños que ofrece la imprenta.",
      },
      {
        selector: '#client-work-options select[aria-label="Tamaño de hoja"]',
        title: "Elegir el tamaño",
        text: "Elegí el tamaño de la tarifa que consultaste. En el catálogo de práctica es A4.",
      },
      {
        selector: '#client-work-options select[aria-label="Papel y gramaje"]',
        title: "Elegir el papel",
        text: "Seleccioná el papel de esa tarifa. En el catálogo de práctica es Obra de 80 g/m².",
      },
      {
        selector: '#client-work-options select[aria-label="Impresión"]',
        title: "Elegir el modo de impresión",
        text: "Elegí Blanco y negro o Color según la tarjeta de precios que leíste. Ese valor se usará para calcular tus copias.",
      },
      {
        selector: '#client-work-options input[aria-label="Copias"]',
        title: "Indicar las copias",
        text: "En «Cantidad de copias», escribí 3 para probar cómo cambia el importe al pedir tres ejemplares del PDF.",
      },
      {
        selector: '#client-work-options select[aria-label="Caras"]',
        title: "Elegir las caras",
        text: "Dejá «Simple faz» para imprimir una cara por hoja. Para comparar el precio con la tarifa del primer paso, dejá los adicionales sin marcar.",
      },
      {
        selector: "#client-budget-action button",
        title: "Calcular el subtotal",
        text: "Pulsá «Revisar presupuesto». Con el afiche de una página y tres copias a simple faz, el subtotal de impresión corresponde a tres veces la tarifa por hoja.",
      },
    ],
  },
  confirmacion: {
    title: "Confirmar el pedido",
    role: "CLIENTE",
    intro:
      "Ya conocés el subtotal. Para crear el pedido, la imprenta necesita saber dónde entregarlo, cómo vas a pagar y qué versión del PDF aprobás para imprimir.",
    result:
      "Al confirmar aparece el número de pedido. Como empleado vas a reconocer ese mismo número cuando registres el pago.",
    hints: [
      {
        selector: '#client-delivery select[aria-label="Modalidad"]',
        title: "Indicar la entrega",
        text: "Elegí «Retiro en sucursal» para practicar la entrega en mostrador. Si elegís envío a domicilio, la guía te pedirá la dirección de entrega.",
      },
      {
        selector: '#client-delivery select[aria-label="Destino de retiro"]',
        mode: "RETIRO_SUCURSAL",
        title: "Elegir dónde retirar",
        text: "Seleccioná la sucursal de Senderos donde el cliente retirará las copias. El destino permite calcular el traslado.",
      },
      ...([
        ["calle", "Indicar la calle", "Escribí la calle de entrega. Usá datos ficticios para la práctica."],
        ["numero", "Indicar la altura", "Escribí el número de la calle donde entregar el pedido."],
        ["codigoPostal", "Indicar el código postal", "Escribí el código postal del domicilio de entrega."],
        ["localidad", "Indicar la localidad", "Escribí la localidad del domicilio."],
        ["provincia", "Indicar la provincia", "Completá la provincia para que la imprenta pueda calcular el envío."],
        ["referencias", "Añadir una referencia", "Podés indicar el piso o una referencia del lugar. Si no hace falta, continuá con el medio de pago."],
      ] as const).map(([name, title, text]) => ({
        selector: `#client-delivery input[name="${name}"]`,
        title,
        text,
        mode: "ENVIO_DOMICILIO" as const,
      })),
      {
        selector: '#client-delivery select[aria-label="Medio de pago"]',
        title: "Elegir el medio de pago",
        text: "Elegí «Efectivo» para practicar un cobro en mostrador. Cuando aparezca «Total de tu pedido», revisá el destino y el importe final.",
      },
      {
        selector: '#client-review-files[data-aceptados="false"] button',
        title: "Abrir la revisión del PDF",
        text: "Pulsá «Revisar archivos» para ver qué PDF se enviará a impresión.",
      },
      {
        selector: '[data-tutorial-pdf="abrir"]',
        title: "Ver las páginas",
        text: "Elegí «Ver páginas» en el PDF que vas a aceptar. Así podés cotejar el contenido exacto que recibirá la imprenta.",
      },
      {
        selector: pdfPageAction,
        title: "Revisar las páginas",
        text: "Recorré las hojas con «Página siguiente». Si saltaste alguna, usá «Página anterior» para verla. Al terminar, marcá la casilla si el contenido coincide con lo que querés imprimir.",
      },
      {
        selector: '[data-tutorial-pdf="aceptar"]',
        title: "Aceptar la vista previa",
        text: "Pulsá «Aceptar vista previa» para autorizar esta versión del PDF. Si cargaste varios, repetí la revisión con cada uno.",
      },
      {
        selector: '[data-tutorial-pdf="volver"]',
        title: "Volver al resumen",
        text: "Ya aceptaste todos los PDF. Pulsá «Volver al resumen» para comprobar los datos finales.",
      },
      {
        selector: '#client-create-order input[name="receptor"]',
        mode: "ENVIO_DOMICILIO",
        title: "Identificar al destinatario",
        text: "Escribí el nombre de quien recibirá las copias en el domicilio.",
      },
      {
        selector: '#client-create-order input[name="telefono"]',
        mode: "ENVIO_DOMICILIO",
        title: "Indicar un teléfono",
        text: "Ingresá un teléfono de práctica para el contacto de entrega.",
      },
      {
        selector: '#client-create-order input[type="checkbox"]',
        title: "Aceptar las condiciones",
        revision: orderReview,
        text: "Si los datos que revisaste coinciden con el pedido que querés hacer, marcá esta casilla para aceptar la oferta.",
      },
      {
        selector: '#client-create-order button[type="submit"]',
        title: "Crear el pedido",
        revision: orderReview,
        text: "Pulsá «Confirmar / Crear pedido» una vez. El pedido conserva el PDF, sus opciones y el precio aceptado.",
      },
    ],
  },
  cobro: {
    title: "Registrar y aplicar el cobro",
    role: "EMPLEADO",
    intro:
      "En Pagos vas a registrar dos hechos. Primero dejás constancia del dinero que recibió la imprenta. Después indicás a qué pedido se destina ese dinero.",
    result:
      "El dinero quedó aplicado y el saldo del pedido está cubierto. Ahora podés revisar el trabajo antes de autorizar la impresión.",
    hints: [
      {
        selector: "[data-tutorial-cobrar]",
        title: "Abrir el registro del cobro",
        text: "Abrí el formulario del medio que recibiste. Para practicar en efectivo, pulsá «Registrar efectivo». Para una acreditación bancaria, elegí «Registrar transferencia recibida».",
        revision: [
          {selector: ".order-detail-heading h2", text: "Buscá el número PED del pedido que atendés. Debe coincidir con el del trabajo que el cliente viene a pagar."},
          {selector: "#pedido-pagos [data-tutorial-cotizacion-cobro]", text: "En Pagos, esta línea identifica la cotización COT de ese pedido, el cliente y la sucursal. Comprobá los tres datos antes de tomar el cobro."},
          {selector: "#pedido-pagos .payment-pending", text: "Leé el importe que todavía falta cubrir. En este ejercicio vas a recibir ese importe y después vas a aplicarlo al pedido."},
        ],
      },
      {
        selector: '[data-tutorial-pago="recibir"] [name="importe"]',
        title: "Indicar el importe recibido",
        text: "Escribí el saldo pendiente completo en pesos para que el pedido quede cubierto cuando le apliques este pago.",
      },
      {
        selector: '[data-tutorial-pago="recibir"] [name="referencia"]',
        title: "Identificar el recibo",
        text: "Escribí una referencia única para que el recibo se pueda encontrar después.",
      },
      {
        selector: '[data-tutorial-pago="recibir"] [name="fecha"]',
        title: "Indicar cuándo se recibió",
        text: "Ingresá la fecha y la hora reales del cobro de práctica.",
      },
      {
        selector: '[data-tutorial-pago="recibir"] [name="motivo"]',
        title: "Confirmar la recepción",
        text: "Describí por qué recibiste el dinero. En «Motivo / detalle» podés escribir «Cobro del pedido de práctica».",
      },
      {
        title: "Verificar el dinero",
        selector: '[data-tutorial-pago="recibir"] [name="verificado"]',
        text: "Marcá esta casilla cuando el importe coincida con el dinero que recibiste.",
      },
      {
        selector: '[data-tutorial-pago="recibir"] button:not([type="button"])',
        title: "Guardar el recibo",
        text: "Si los datos coinciden con el movimiento, pulsá «Registrar dinero recibido». Vas a obtener un recibo. En el paso siguiente vas a aplicar ese dinero al pedido.",
        revision: [
          {selector: '[data-tutorial-pago="recibir"] [data-tutorial-medio-cobro]', text: "Este es el medio que elegiste. Si corresponde a una transferencia, comprobá la acreditación en el banco. Si fue en efectivo, comprobá el dinero recibido."},
          {selector: '[data-tutorial-pago="recibir"] [name="importe"]', text: "Cotejá el importe en pesos con el dinero que efectivamente recibiste. Podés corregir el campo."},
          {selector: '[data-tutorial-pago="recibir"] [name="referencia"]', text: "Leé la referencia que va a identificar este recibo. Comprobá que corresponda al movimiento de este cliente."},
          {selector: '[data-tutorial-pago="recibir"] [name="fecha"]', text: "Verificá el día y la hora en que recibiste el dinero. La fecha se muestra en la hora local de este dispositivo."},
        ],
      },
      {
        selector: ".payment-history-details:not([open]) > summary, [data-tutorial-aplicar]",
        title: "Elegir el pago recibido",
        text: "Buscá tu recibo en «Dinero recibido y conservado». Si la sección está plegada, abrí «Transferencias, comprobantes e historial de pagos». Pulsá «Aplicar este pago».",
      },
      {
        selector: '[data-tutorial-pago="aplicar"] [name="importe"]',
        title: "Aplicar el importe",
        text: "Indicá cuánto del dinero recibido vas a asignar a este pedido. Para cubrir el saldo, usá el importe pendiente.",
      },
      {
        title: "Indicar a qué se aplica",
        selector: '[data-tutorial-pago="aplicar"] [name="motivo"]',
        text: "Escribí por qué asignás el importe a este trabajo. Por ejemplo, «Pago completo del pedido».",
      },
      {
        selector: '[data-tutorial-pago="aplicar"] button:not([type="button"])',
        title: "Guardar la aplicación",
        text: "Si el recibo, el destino y el importe son los de este pedido, pulsá «Aplicar dinero a esta cotización». Después vas a ver el saldo actualizado.",
        revision: [
          {selector: '[data-tutorial-pago="aplicar"] [data-tutorial-recibo]', text: "Localizá la referencia del recibo que elegiste. Esta línea distingue el dinero recibido, el que ya se aplicó o devolvió y el que queda disponible."},
          {selector: '[data-tutorial-pago="aplicar"] [data-tutorial-destino-cobro]', text: "Leé el número COT al que vas a asignar el dinero. Comprobá el saldo pendiente y el máximo aplicable que indica el formulario."},
          {selector: '[data-tutorial-pago="aplicar"] [name="importe"]', text: "Revisá cuántos pesos de ese recibo vas a aplicar. Para cubrir el pedido de práctica, el importe debe coincidir con su saldo pendiente."},
          {selector: '[data-tutorial-pago="aplicar"] [name="motivo"]', text: "Leé el detalle que quedará junto a la aplicación. Tiene que explicar por qué asignás ese recibo a este trabajo."},
        ],
      },
    ],
  },
  revision: {
    title: "Revisar y aprobar",
    role: "EMPLEADO",
    intro:
      "Antes de producir, quien atiende el pedido coteja el PDF, las opciones de impresión y el destino. Después autoriza que el trabajo pase a impresión.",
    result:
      "El pedido está aprobado. El empleado ya puede abrir la impresión simulada para este PDF.",
    hints: [
      {
        selector: '[data-tutorial-gestion="REVISAR"]',
        title: "Abrir la revisión",
        text: "En «Revisión administrativa», comprobá el pedido y el PDF confirmado. Pulsá «Registrar revisión» para dejar constancia de lo que verificaste.",
      },
      {
        selector: '[data-tutorial-decision="REVISAR"] [name="motivo"]',
        title: "Registrar lo revisado",
        text: "En «Motivo interno», anotá qué revisaste, por ejemplo «Coinciden PDF, copias y destino».",
      },
      {
        title: "Confirmar la revisión",
        selector: '[data-tutorial-decision="REVISAR"] [name="revisado"]',
        text: "Marcá que cotejaste los PDF, las opciones y los datos de entrega del pedido.",
      },
      {
        selector:
          '[data-tutorial-decision="REVISAR"] button:not([type="button"])',
        title: "Guardar la revisión",
        text: "Guardá la revisión con el botón señalado para habilitar la aprobación del pedido.",
      },
      {
        selector: '[data-tutorial-gestion="APROBAR"]',
        title: "Abrir la aprobación",
        text: "Elegí «Aprobar pedido». Si la acción está deshabilitada, revisá el motivo que figura junto al botón y comprobá el dinero aplicado en Pagos.",
      },
      {
        selector: '[data-tutorial-decision="APROBAR"] [name="motivo"]',
        title: "Autorizar la impresión",
        text: "Anotá por qué autorizás el trabajo, por ejemplo «Archivo y anticipo verificados».",
      },
      {
        title: "Verificar antes de aprobar",
        selector: '[data-tutorial-decision="APROBAR"] [name="revisado"]',
        text: "Confirmá que verificaste los datos de este trabajo antes de autorizarlo.",
      },
      {
        selector:
          '[data-tutorial-decision="APROBAR"] button:not([type="button"])',
        title: "Guardar la aprobación",
        text: "Confirmá la aprobación para que el pedido quede disponible en producción.",
      },
    ],
  },
  produccion: {
    title: "Simular la impresión",
    role: "EMPLEADO",
    intro:
      "En producción, cada PDF tiene su propio trabajo. La demo simula el uso de una impresora y registra el inicio y el fin de cada trabajo.",
    result:
      "Todas las impresiones están completas. Ahora podés elegir «Controlar calidad» en cada trabajo.",
    hints: [
      {
        selector: '[data-tutorial-movimiento="INICIAR"]:enabled',
        title: "Iniciar la impresión",
        text: "Buscá el nombre de tu PDF y pulsá «Iniciar impresión simulada» en esa fila.",
      },
      {
        selector:
          '[data-tutorial-produccion="INICIAR"] select[aria-label="Impresora o recurso simulado"]',
        title: "Elegir la impresora",
        text: "Seleccioná el recurso simulado que imprimirá este PDF. Su nombre quedará registrado en el historial.",
      },
      {
        selector: '[data-tutorial-produccion="INICIAR"] [name="motivo"]',
        title: "Anotar el motivo del inicio",
        text: "Escribí el motivo del inicio de la tirada, por ejemplo «PDF aprobado para imprimir».",
      },
      {
        selector: '[data-tutorial-produccion="INICIAR"] [name="confirmacion"]',
        title: "Confirmar el inicio",
        text: "Marcá la casilla cuando hayas verificado el archivo y el recurso que seleccionaste.",
      },
      {
        selector:
          '[data-tutorial-produccion="INICIAR"] button:not([type="button"])',
        title: "Guardar el inicio",
        text: "Pulsá «Confirmar simulación» para registrar el inicio. A partir de ese momento el trabajo figura en impresión.",
      },
      {
        selector: '[data-tutorial-movimiento="COMPLETAR"]',
        title: "Completar la tirada",
        text: "El trabajo ya figura en impresión. Pulsá «Completar impresión simulada» en el mismo PDF cuando quieras registrar el fin de la tirada.",
      },
      {
        selector: '[data-tutorial-produccion="COMPLETAR"] [name="motivo"]',
        title: "Confirmar el resultado",
        text: "Anotá el resultado de esta tirada. Por ejemplo, «Se imprimieron todas las copias del PDF».",
      },
      {
        selector:
          '[data-tutorial-produccion="COMPLETAR"] [name="confirmacion"]',
        title: "Registrar el fin de la tirada",
        text: "Marcá la casilla si finalizó la impresión de este archivo.",
      },
      {
        selector:
          '[data-tutorial-produccion="COMPLETAR"] button:not([type="button"])',
        title: "Guardar la tirada",
        text: "Pulsá «Confirmar simulación». El historial registrará la impresión terminada.",
      },
    ],
  },
  calidad: {
    title: "Controlar la calidad",
    role: "EMPLEADO",
    intro:
      "Antes de entregar, compará el resultado simulado con lo que pidió el cliente. La ficha reúne el formato, las caras, la cantidad de copias y los adicionales de cada PDF.",
    result:
      "Todos los trabajos tienen la calidad aprobada. El pedido está listo para entregar o despachar según su destino.",
    hints: [
      {
        selector: '[data-tutorial-movimiento="CALIDAD"]',
        title: "Abrir el control",
        text: "En el trabajo que acabás de completar, pulsá «Controlar calidad». Leé los detalles del trabajo y abrí el PDF confirmado si necesitás cotejarlo.",
      },
      ...([
        ["impresionCompleta","Comprobar la impresión completa","Cotejá el número de páginas y las copias con la ficha del pedido. Marcá la primera casilla si la tirada está completa."],
        ["calidadCorrecta","Revisar la calidad de impresión","Verificá que el texto y las imágenes salieron legibles y marcá esta casilla."],
        ["alineacionCorrecta","Revisar la alineación","Comprobá que las páginas estén bien orientadas y alineadas antes de marcar la casilla."],
        ["ordenCorrecto","Comprobar el orden","Cotejá el orden de las páginas con el PDF aprobado y confirmalo."],
        ["terminacionesCorrectas","Revisar los opcionales","Cotejá las terminaciones con las opciones del pedido. Si el cliente no pidió ninguna, verificá que se respetó esa elección y marcá la casilla."],
      ] as const).map(([name, title, text]) => ({
        selector: `.production-quality-checks input[name="${name}"]`,
        title,
        text,
      })),
      {
        selector:
          '.production-quality-result select[aria-label="Resultado de calidad"]',
        title: "Aprobar la calidad",
        text: "Elegí «Aprobar calidad» si las copias coinciden con el pedido y pueden pasar a entrega.",
      },
      {
        selector: '[data-tutorial-produccion="CALIDAD"] [name="motivo"]',
        title: "Anotar el control",
        text: "Describí lo que comprobaste al comparar la impresión con el pedido.",
      },
      {
        selector: '[data-tutorial-produccion="CALIDAD"] [name="confirmacion"]',
        title: "Guardar el control",
        text: "Marcá la casilla si el resultado refleja el control que acabás de hacer.",
      },
      {
        selector:
          '[data-tutorial-produccion="CALIDAD"] button:not([type="button"])',
        title: "Guardar la calidad",
        text: "Pulsá «Confirmar simulación» para registrar el control terminado.",
      },
    ],
  },
  entrega: {
    title: "Entregar y cerrar",
    role: "EMPLEADO",
    intro:
      "En el mostrador participan dos personas. El cliente presenta un código para identificar el pedido y el empleado lo valida antes de registrar quién lo recibió.",
    result:
      "El pedido figura como cerrado y conserva el registro del cobro y la entrega. Podés volver a su ficha para consultar todo el recorrido.",
    hints: [
      {
        selector: "[data-tutorial-codigo]",
        title: "Generar el código",
        text: "Pulsá «Generar código de entrega». El código aparece en tu pantalla y vence en 30 minutos. Copialo o anotalo antes de volver al rol Empleado.",
        role: "CLIENTE",
      },
      {
        selector: ".delivery-secret",
        title: "Conservar el código",
        text: "Conservá los ocho caracteres tal como aparecen. Elegí «Volver a Empleado para registrar la entrega» en la guía y confirmá el cambio de rol.",
        role: "CLIENTE",
      },
      {
        selector: '[data-tutorial-entrega="VALIDAR"]',
        title: "Abrir la validación",
        text: "Pulsá «Validar código de entrega» para abrir la comprobación del código que presenta el cliente.",
        role: "EMPLEADO",
      },
      {
        selector: '.delivery-dialog [name="codigo"]',
        title: "Ingresar el código",
        text: "Escribí los ocho caracteres que te mostró el cliente. El código identifica este pedido al entregarlo.",
        role: "EMPLEADO",
      },
      {
        selector:
          '[data-tutorial-dialogo="VALIDAR"] button:not([type="button"])',
        title: "Validar el código",
        text: "Pulsá «Validar código». Si se acepta, podrás registrar a quién entregaste el pedido.",
        role: "EMPLEADO",
      },
      {
        selector: '[data-tutorial-entrega="ENTREGAR"]',
        title: "Registrar la entrega",
        text: "Pulsá «Confirmar entrega física» para abrir el formulario. Ahí vas a identificar a quien recibió las copias y dejar constancia de la entrega.",
        revision: [
          {selector: "#pedido-entrega [data-tutorial-destino-entrega]", text: "Comprobá la modalidad, el destino y la franja acordada. Si es un envío, cotejá también el domicilio y la persona de contacto que figuran en el pedido."},
          {selector: "#pedido-entrega .delivery-summary .client-state", text: "Leé en qué etapa logística está el trabajo. Si es un envío, registrá los movimientos de traslado que correspondan antes de marcar la entrega física."},
          {selector: "#pedido-entrega .delivery-code .client-success", text: "Esta confirmación indica hasta cuándo tu sesión está autorizada a registrar la entrega de este pedido. Si vence, volvé a validar el código que presentó el cliente."},
        ],
        role: "EMPLEADO",
      },
      {
        selector: '.delivery-dialog [name="receptor"]',
        title: "Identificar a quien recibe",
        text: "Comprobá el nombre de quien recibe el pedido. Corregilo si la persona que lo retira es otra.",
        role: "EMPLEADO",
      },
      {
        selector: '[data-tutorial-dialogo="ENTREGAR"] [name="motivo"]',
        title: "Anotar la entrega",
        text: "Escribí dónde y a quién entregaste el pedido.",
        role: "EMPLEADO",
      },
      {
        selector: '[data-tutorial-dialogo="ENTREGAR"] [name="confirmado"]',
        title: "Confirmar la recepción física",
        text: "Marcá esta casilla cuando hayas entregado las copias a la persona que figura en el registro.",
        role: "EMPLEADO",
      },
      {
        selector:
          '[data-tutorial-dialogo="ENTREGAR"] button:not([type="button"])',
        title: "Guardar la entrega",
        text: "Pulsá «Confirmar registro» para dejar asentado quién recibió el pedido.",
        revision: [
          {selector: '[data-tutorial-dialogo="ENTREGAR"] .order-management-heading', text: "Comprobá el número PED del formulario. Tiene que ser el mismo que figura en el trabajo que entregaste."},
          {selector: '[data-tutorial-dialogo="ENTREGAR"] [name="receptor"]', text: "Este nombre quedará como receptor. Confirmá que sea la persona que recibió las copias, aun si el formulario ya trajo un nombre."},
          {selector: '[data-tutorial-dialogo="ENTREGAR"] [name="motivo"]', text: "Releé la observación antes de registrar la entrega. Tiene que describir lo que pasó al entregar el pedido completo."},
        ],
        role: "EMPLEADO",
      },
      {
        selector: '[data-tutorial-entrega="CERRAR"]',
        title: "Abrir el cierre",
        text: "En «Cierre administrativo», abrí el cierre del pedido que ya entregaste y cobraste.",
        role: "EMPLEADO",
      },
      {
        selector: '[data-tutorial-dialogo="CERRAR"] [name="motivo"]',
        title: "Confirmar el cierre",
        text: "Anotá por qué podés cerrar el pedido. Por ejemplo, «Copias entregadas y pago conciliado».",
        role: "EMPLEADO",
      },
      {
        selector: '[data-tutorial-dialogo="CERRAR"] [name="confirmado"]',
        title: "Guardar el cierre",
        text: "Marcá que verificaste la entrega y la conciliación del pago.",
        role: "EMPLEADO",
      },
      {
        selector:
          '[data-tutorial-dialogo="CERRAR"] button:not([type="button"])',
        title: "Registrar el cierre",
        text: "Pulsá «Confirmar registro» para cerrar el pedido conciliado.",
        role: "EMPLEADO",
      },
    ],
  },
};
export const roleNames: Record<Role, string> = {
  ADMIN_ADMIN: "Administrador",
  CLIENTE: "Cliente",
  EMPLEADO: "Empleado",
};
export function firstRequired(requested: Step, completed: Step[]): Step {
  return (
    steps
      .slice(0, steps.indexOf(requested))
      .find((step) => !completed.includes(step)) ?? requested
  );
}
export function tutorialPath(step: Step, state: Tutorial, role: Role): string {
  if (step === "tarifa") return "/administracion/catalogo#revision-comercial";
  if (
    !state.bloqueo &&
    state.pedido &&
    ["archivo", "presupuesto", "confirmacion"].includes(step)
  )
    return `/cliente/pedidos/${state.pedido}#pedido-${step === "archivo" ? "pdf" : "resumen"}`;
  if (step === "archivo" || step === "presupuesto")
    return "/cliente/cotizaciones/nueva";
  if (step === "confirmacion")
    return state.preparacion
      ? `/cliente/preparaciones/${state.preparacion}`
      : "/cliente/cotizaciones/nueva";
  const hash = {
    cobro: "pagos",
    revision: "revision",
    produccion: "produccion",
    calidad: "produccion",
    entrega: "entrega",
  }[step];
  const area = role === "CLIENTE" ? "cliente" : "operacion";
  return state.pedido
    ? `/${area}/pedidos/${state.pedido}#pedido-${hash}`
    : role === "CLIENTE"
      ? "/cliente"
      : "/operacion";
}
