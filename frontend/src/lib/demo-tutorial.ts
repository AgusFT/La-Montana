//#region ENCABEZADO · src/lib/demo-tutorial.ts
// Describe los nueve pasos, sus requisitos, los roles y las rutas del pedido.
//#endregion
import type { Role } from "./roles";

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
type Hint = readonly [selector: string, text: string, role?: Role];
type Lesson = {
  title: string;
  role: Role;
  intro: string;
  result: string;
  hints: Hint[];
};

export const lessons: Record<Step, Lesson> = {
  tarifa: {
    title: "Consultar la tarifa",
    role: "ADMIN_ADMIN",
    intro:
      "Antes de presupuestar un pedido, la imprenta define cuánto cobra por imprimir. En Servicios y precios, cada tarifa vigente combina un papel, un modo de color y un precio.",
    result:
      "Tarifa consultada. Ahora podés pasar al rol Cliente y cargar el PDF que vas a cotizar.",
    hints: [
      [
        '[data-tutorial-tarifa-leida="true"]',
        "Leé el importe de «Simple faz» en la tarjeta resaltada. Es el precio de imprimir una cara. Fijate en el papel y el color de esa tarjeta para elegirlos al presupuestar. Pulsá «Confirmar que revisé la tarifa» en esta guía.",
      ],
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
      [
        "#client-upload .client-dropzone button",
        "Si vas a usar el afiche, pulsá «Guardar PDF de práctica» en la guía. Después elegí «Seleccionar PDF» en Tus archivos y abrí el archivo que guardaste.",
      ],
      [
        ".workspace-file-choice",
        "Esperá a que termine el análisis. En Tus archivos, elegí el PDF que vas a imprimir y comprobá su nombre y sus páginas en Detalles de impresión.",
      ],
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
      [
        '#client-work-options select[aria-label="Servicio de impresión"]',
        "En «Servicio de impresión», elegí Impresión para que aparezcan los tamaños que ofrece la imprenta.",
      ],
      [
        '#client-work-options select[aria-label="Tamaño de hoja"]',
        "Elegí el tamaño de la tarifa que consultaste. En el catálogo de práctica es A4.",
      ],
      [
        '#client-work-options select[aria-label="Papel y gramaje"]',
        "Seleccioná el papel de esa tarifa. En el catálogo de práctica es Obra de 80 g/m².",
      ],
      [
        '#client-work-options select[aria-label="Impresión"]',
        "Elegí Blanco y negro o Color según la tarjeta de precios que leíste. Ese valor se usará para calcular tus copias.",
      ],
      [
        '#client-work-options input[aria-label="Copias"]',
        "En «Cantidad de copias», escribí 3 para probar cómo cambia el importe al pedir tres ejemplares del PDF.",
      ],
      [
        '#client-work-options select[aria-label="Caras"]',
        "Dejá «Simple faz» para imprimir una cara por hoja. Para comparar el precio con la tarifa del primer paso, dejá los adicionales sin marcar.",
      ],
      [
        "#client-budget-action button",
        "Pulsá «Revisar presupuesto». Con el afiche de una página y tres copias a simple faz, el subtotal de impresión corresponde a tres veces la tarifa por hoja.",
      ],
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
      [
        '#client-delivery select[aria-label="Modalidad"]',
        "Para practicar un retiro en mostrador, elegí retiro en sucursal y seleccioná Senderos en «Destino de retiro». Si elegís envío, completá la dirección y los datos de quien recibe.",
      ],
      [
        '#client-delivery select[aria-label="Medio de pago"]',
        "Elegí «Efectivo» para practicar un cobro en mostrador. Cuando aparezca «Total de tu pedido», revisá el destino y el importe final.",
      ],
      [
        "#client-review-files button",
        "Abrí «Revisar archivos» y elegí «Ver páginas» en tu PDF. Recorré todas las páginas, marcá la casilla de lectura, pulsá «Aceptar vista previa» y luego «Volver al resumen».",
      ],
      [
        '#client-create-order input[type="checkbox"]',
        "En «Confirmá tu pedido», comprobá el total, el destino y la estimación. Marcá la casilla de aceptación cuando coincidan con lo que querés pedir.",
      ],
      [
        "#client-create-order button",
        "Pulsá «Confirmar / Crear pedido» una vez. El pedido conserva el PDF, sus opciones y el precio aceptado.",
      ],
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
      [
        "[data-tutorial-cobrar]",
        "Buscá el saldo pendiente. Si el cliente eligió efectivo, pulsá «Registrar efectivo». Para una transferencia, elegí «Registrar transferencia recibida».",
      ],
      [
        '[data-tutorial-pago="recibir"] [name="importe"]',
        "Escribí el saldo pendiente completo en pesos. Ingresá un número de recibo de práctica único (o referencia de la transferencia), la fecha y la hora del movimiento.",
      ],
      [
        '[data-tutorial-pago="recibir"] [name="motivo"]',
        "En «Motivo / detalle» podés escribir «Cobro del pedido de práctica». Marcá la verificación del dinero y pulsá «Registrar dinero recibido».",
      ],
      [
        "[data-tutorial-aplicar]",
        "Buscá tu recibo en «Dinero recibido y conservado». Si la sección está plegada, abrí «Transferencias, comprobantes e historial de pagos». Pulsá «Aplicar este pago».",
      ],
      [
        '[data-tutorial-pago="aplicar"] [name="importe"]',
        "Indicá el importe que acabás de recibir y un motivo, por ejemplo «Pago completo del pedido». Pulsá «Aplicar dinero a esta cotización» y revisá el nuevo saldo.",
      ],
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
      [
        '[data-tutorial-gestion="REVISAR"]',
        "En «Revisión administrativa», comprobá el pedido y el PDF confirmado. Pulsá «Registrar revisión» para dejar constancia de lo que verificaste.",
      ],
      [
        '[data-tutorial-decision="REVISAR"] [name="motivo"]',
        "En «Motivo interno», anotá qué revisaste, por ejemplo «Coinciden PDF, copias y destino». Marcá la casilla y confirmá la revisión.",
      ],
      [
        '[data-tutorial-gestion="APROBAR"]',
        "Elegí «Aprobar pedido». Si la acción está deshabilitada, revisá el motivo que figura junto al botón y comprobá el dinero aplicado en Pagos.",
      ],
      [
        '[data-tutorial-decision="APROBAR"] [name="motivo"]',
        "Anotá por qué autorizás el trabajo, por ejemplo «Archivo y anticipo verificados». Marcá la casilla y confirmá la aprobación.",
      ],
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
      [
        '[data-tutorial-movimiento="INICIAR"]:enabled',
        "Buscá el nombre de tu PDF y pulsá «Iniciar impresión simulada» en esa fila.",
      ],
      [
        '[data-tutorial-produccion="INICIAR"] select[aria-label="Impresora o recurso simulado"]',
        "Seleccioná la impresora simulada que ofrece la lista. Anotá el motivo, marcá la confirmación del inicio y pulsá «Confirmar simulación».",
      ],
      [
        '[data-tutorial-movimiento="COMPLETAR"]',
        "El trabajo ya figura en impresión. Pulsá «Completar impresión simulada» en el mismo PDF cuando quieras registrar el fin de la tirada.",
      ],
      [
        '[data-tutorial-produccion="COMPLETAR"] [name="motivo"]',
        "Escribí el resultado de la tirada, marcá la casilla y pulsá «Confirmar simulación». Si cargaste más de un PDF, repetí el inicio y el fin para cada uno.",
      ],
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
      [
        '[data-tutorial-movimiento="CALIDAD"]',
        "En el trabajo que acabás de completar, pulsá «Controlar calidad». Leé los detalles del trabajo y abrí el PDF confirmado si necesitás cotejarlo.",
      ],
      [
        ".production-quality-checks",
        "Revisá y marcá los cinco controles de la lista. Si no pediste terminaciones, comprobá que el trabajo coincida con esa elección y marcá el último control.",
      ],
      [
        '.production-quality-result select[aria-label="Resultado de calidad"]',
        "Para seguir con la entrega, elegí «Aprobar calidad» si todo coincide. Escribí qué comprobaste, marcá la confirmación del resultado y pulsá «Confirmar simulación». Repetilo para cada PDF.",
      ],
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
      [
        "[data-tutorial-codigo]",
        "Pulsá «Generar código de entrega». El código aparece en tu pantalla y vence en 30 minutos. Copialo o anotalo antes de volver al rol Empleado.",
        "CLIENTE",
      ],
      [
        ".delivery-secret",
        "Conservá los ocho caracteres tal como aparecen. Elegí «Volver a Empleado para registrar la entrega» en la guía y confirmá el cambio de rol.",
        "CLIENTE",
      ],
      [
        '[data-tutorial-entrega="VALIDAR"]',
        "Si todavía no tenés el código, elegí «Pasar a Cliente para obtener el código» en la guía. Cuando lo tengas, pulsá «Validar código de entrega», ingresá sus ocho caracteres y elegí «Validar código».",
        "EMPLEADO",
      ],
      [
        '[data-tutorial-entrega="ENTREGAR"]',
        "Con el código validado, pulsá «Confirmar entrega física». Revisá el nombre de quien recibe, anotá el motivo, marcá la confirmación y elegí «Confirmar registro». Si elegiste envío, registrá antes los movimientos de traslado que indica la ficha.",
        "EMPLEADO",
      ],
      [
        '[data-tutorial-entrega="CERRAR"]',
        "En «Cierre administrativo», verificá la entrega y el total conciliado. Abrí el cierre, anotá el motivo, marcá la casilla y elegí «Confirmar registro».",
        "EMPLEADO",
      ],
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
