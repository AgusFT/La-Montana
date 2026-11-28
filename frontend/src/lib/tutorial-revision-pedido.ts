// Ambos recorridos cotejan la misma oferta que el formulario enviará a la imprenta.
export const orderReview = [
  {
    selector: "#client-budget .workspace-budget-items",
    text: "Comprobá el nombre de cada PDF y la cantidad de páginas y copias. Mirá también el papel, el color y si va a simple o doble faz. La imprenta usará estas opciones para imprimir los archivos que aceptaste.",
  },
  {
    selector: ".workspace-price .workspace-delivery-summary",
    text: "Leé cómo vas a recibir el pedido, a qué dirección y en qué franja se estima que estará disponible. Si el destino es incorrecto, podés corregirlo en Entrega y esperar el nuevo cálculo.",
  },
  {
    selector: "#client-create-order .workspace-contact",
    text: "Para el envío a domicilio, comprobá quién recibe y su teléfono. Podés completar o corregir ambos campos acá con datos de práctica.",
  },
  {
    selector: '#client-delivery select[aria-label="Medio de pago"]',
    text: "Revisá si elegiste efectivo o transferencia. Este es el medio de pago que se indica en el pedido. La imprenta registrará el cobro cuando reciba el dinero.",
  },
  {
    selector: ".workspace-price dl",
    text: "Compará el importe de impresión y servicios con el costo de entrega. El total final suma los dos y es el importe del pedido que vas a confirmar.",
  },
  {
    selector: "#client-order-conditions",
    text: "Leé cómo se aprueba el pedido y fijate si aparece una seña o un pago previo. Abrí «Ver condiciones del pedido» para consultar las instrucciones de esta cotización.",
  },
].map((field) => ({ ...field, when: "#client-create-order[data-revision-pedido]" }));
