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
type Hint = readonly [selector: string, text: string];
type Lesson = { title: string; role: Role; intro: string; hints: Hint[] };
export const lessons: Record<Step, Lesson> = {
  tarifa: {
    title: "Consultar la tarifa",
    role: "ADMIN_ADMIN",
    intro:
      "En Administración, revisá el precio vigente de Senderos que usará la cotización.",
    hints: [
      [".catalog-current", "Comprobá que haya una configuración vigente."],
      [
        "[data-tutorial-tarifa]",
        "Abrí el detalle de la revisión vigente y localizá el precio para el papel y la impresión que vas a usar.",
      ],
      [
        ".catalog-history-detail",
        "Leé las tarifas y servicios del detalle. Luego confirmá la lectura en el tutorial.",
      ],
    ],
  },
  archivo: {
    title: "Cargar el PDF",
    role: "CLIENTE",
    intro:
      "Como cliente vas a preparar el archivo de un pedido nuevo. Podés usar el afiche de práctica o un PDF propio.",
    hints: [
      [
        "#client-upload .client-dropzone button",
        "Elegí el PDF. Esperá a que el análisis termine. Si cerraste el selector antes de subirlo, volvé a elegirlo.",
      ],
      [
        ".workspace-file-choice",
        "Seleccioná el archivo analizado. Su nombre y cantidad de páginas identifican el trabajo.",
      ],
    ],
  },
  presupuesto: {
    title: "Configurar y presupuestar",
    role: "CLIENTE",
    intro:
      "Elegí cómo imprimir tu PDF. El presupuesto usa el catálogo vigente.",
    hints: [
      [
        '#client-work-options select[aria-label="Servicio de impresión"]',
        "Elegí el servicio para este PDF.",
      ],
      [
        '#client-work-options select[aria-label="Tamaño de hoja"]',
        "Seleccioná el tamaño de hoja.",
      ],
      [
        '#client-work-options select[aria-label="Papel y gramaje"]',
        "Elegí un papel disponible.",
      ],
      [
        '#client-work-options select[aria-label="Impresión"]',
        "Seleccioná blanco y negro o color.",
      ],
      [
        '#client-work-options input[aria-label="Copias"]',
        "Indicá las copias y revisá las caras y terminaciones.",
      ],
      [
        "#client-budget-action button",
        "Calculá el presupuesto y comprobá el subtotal.",
      ],
    ],
  },
  confirmacion: {
    title: "Confirmar el pedido",
    role: "CLIENTE",
    intro:
      "Revisá tu presupuesto, la entrega y el PDF. Para el recorrido más corto usá retiro en Senderos y pago en efectivo si están disponibles.",
    hints: [
      [
        '#client-delivery select[aria-label="Modalidad"]',
        "Elegí la modalidad de entrega. Si elegís retiro, seleccioná la sucursal.",
      ],
      [
        '#client-delivery select[aria-label="Medio de pago"]',
        "Elegí el medio y esperá el cálculo del total.",
      ],
      [
        "#client-review-files",
        "Revisá las páginas de cada PDF y aceptalas expresamente.",
      ],
      [
        '#client-create-order input[type="checkbox"]',
        "Verificá el total, las condiciones y el destino antes de aceptar.",
      ],
      [
        "#client-create-order button",
        "Confirmá el pedido. El número quedará asociado a este recorrido.",
      ],
    ],
  },
  cobro: {
    title: "Registrar y aplicar el cobro",
    role: "EMPLEADO",
    intro:
      "Abrí Pagos en el pedido de práctica. La recepción y la aplicación del dinero son operaciones diferentes. Para practicar el ciclo completo podés registrar el total pendiente.",
    hints: [
      [
        "[data-tutorial-cobrar]",
        "Elegí recibir el medio usado en el pedido. Consultá el saldo e ingresá importe, referencia y fecha.",
      ],
      [
        '.payment-panel form [name="importe"]',
        "Ingresá el importe y el motivo de prueba. Revisá la referencia y confirmá la recepción.",
      ],
      [
        "[data-tutorial-aplicar]",
        "En Dinero recibido, aplicá ese cobro a este trabajo y revisá cuánto queda pendiente.",
      ],
    ],
  },
  revision: {
    title: "Revisar y aprobar",
    role: "EMPLEADO",
    intro:
      "Compará el trabajo solicitado con el PDF. La aprobación respeta el anticipo y los permisos de la sucursal.",
    hints: [
      [
        '[data-tutorial-gestion="REVISAR"]',
        "Abrí la revisión e indicá qué comprobaste.",
      ],
      [
        '.order-decision-form textarea[name="motivo"]',
        "Explicá el motivo y confirmá tu decisión.",
      ],
      [
        '[data-tutorial-gestion="APROBAR"]',
        "Aprobá el trabajo una vez que estén cumplidas las condiciones vigentes.",
      ],
    ],
  },
  produccion: {
    title: "Simular la impresión",
    role: "EMPLEADO",
    intro: "En Impresión simulada, procesá cada ítem del pedido.",
    hints: [
      [
        '[data-tutorial-movimiento="INICIAR"]',
        "Iniciá la simulación de un ítem pendiente.",
      ],
      [
        'select[aria-label="Impresora o recurso simulado"]',
        "Elegí una impresora compatible, indicá el motivo y confirmá.",
      ],
      [
        '[data-tutorial-movimiento="COMPLETAR"]',
        "Registrá que terminó la simulación de impresión. Repetilo si el pedido tiene más de un ítem.",
      ],
    ],
  },
  calidad: {
    title: "Controlar la calidad",
    role: "EMPLEADO",
    intro: "Verificá el resultado de cada ítem antes de dejar listo el pedido.",
    hints: [
      [
        '[data-tutorial-movimiento="CALIDAD"]',
        "Abrí el control de calidad para la impresión terminada.",
      ],
      [
        ".production-quality-form, .production-quality-result",
        "Revisá individualmente impresión, calidad, alineación, orden y terminaciones. Elegí el resultado y confirmá.",
      ],
    ],
  },
  entrega: {
    title: "Entregar y cerrar",
    role: "EMPLEADO",
    intro:
      "Primero el cliente presenta su código. Después el empleado lo valida, registra la entrega y cierra el pedido conciliado.",
    hints: [
      [
        "[data-tutorial-codigo]",
        "Como cliente, generá el código y tenelo a mano antes de volver a Empleado.",
      ],
      [
        '[data-tutorial-entrega="VALIDAR"]',
        "Como empleado, verificá el saldo y validá el código del cliente. Si hay traslado, registrá también los movimientos indicados.",
      ],
      [
        '[data-tutorial-entrega="ENTREGAR"]',
        "Confirmá la entrega con los datos de quien recibe.",
      ],
      [
        '[data-tutorial-entrega="CERRAR"]',
        "Revisá el total conciliado y los comprobantes antes del cierre.",
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
  if (step === "tarifa") return "/administracion/catalogo#historial-comercial";
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
