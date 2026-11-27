import type { Capitulo } from "./tipos";

export const orientacion: Capitulo = {
  id: "orientacion",
  titulo: "Orientarse en La Montaña",
  temas: [
    {
      id: "orientacion-espacios",
      titulo: "Elegir el espacio de trabajo",
      paraQue: "Pensá en un afiche para retirar en una sucursal. El cliente pide el presupuesto. La imprenta revisa el PDF y el pago, produce las copias y las deja listas. El mostrador registra el retiro. Cada persona trabaja sobre el mismo pedido desde su propia cuenta.",
      pasos: [
        "Si vas a comprar, abrí la página que te compartió la imprenta, iniciá sesión o registrate como cliente. Elegí la opción para crear un pedido cuando tengas el PDF.",
        "Si atendés en la imprenta, ingresá con tu cuenta de empleado. Mirá la sucursal y el estado antes de abrir un trabajo. Usá el número de pedido para coordinar con caja, producción o entrega.",
        "Si recibís pedidos en un punto o los transportás, ingresá con la cuenta del punto o del repartidor. Consultá la lista asignada antes de recibir un paquete o registrar una visita.",
        "Si sos el propietario y la imprenta es nueva, empezá por la puesta en marcha. Para cotizar se necesitan papel, tarifas y una configuración operativa activa.",
      ],
      nota: "Los accesos dependen del rol, los permisos, la sucursal y la versión instalada. Algunos pasos del manual describen dos variantes para que puedas seguir la que coincida con los controles que ves.",
    },
    {
      id: "orientacion-ayuda",
      titulo: "Consultar la ayuda de una pantalla y cambiar la apariencia",
      paraQue: "La guía dentro de la aplicación señala el control que corresponde a cada paso. Podés usarla para ubicarte antes de modificar un pedido o una configuración.",
      pasos: [
        "En una pantalla con el botón «?», pulsalo para abrir la guía. Leé el texto del cuadro junto al elemento señalado y usá «Siguiente» para avanzar.",
        "Elegí «Finalizar» al llegar al último paso. Si ya encontraste lo que necesitás, usá «Omitir» o «Cerrar guía». Después, realizá la acción en el formulario correspondiente.",
        "En la barra lateral, el sol y la luna de «Apariencia» cambian entre modo claro y oscuro. El navegador recuerda tu elección. Las capturas de este manual usan el modo claro.",
      ],
      nota: "Los elementos que la pantalla marca «En construcción», como la conexión automática con impresoras mediante CUPS, indican funciones pendientes. La producción se registra desde el pedido.",
    },
  ],
};

export const continuaciones: Record<string, string[]> = {
  "orientacion-espacios": ["cli-ingresar", "admin-primera-configuracion"],
  "cli-confirmar": ["cli-seguimiento", "op-revision"],
  "op-mostrador": ["op-revision", "op-cobros"],
  "op-correcciones": ["cli-corregir"],
  "cli-transferencia": ["op-comprobantes"],
  "op-calidad": ["op-despacho", "op-retiro-sucursal"],
  "op-despacho": ["op-reparto", "op-punto"],
  "op-punto": ["op-finanzas-punto", "op-cierre"],
  "admin-primera-configuracion": ["admin-papeles", "admin-borrador"],
  "admin-publicar-precios": ["admin-borrador", "cli-impresion"],
};
