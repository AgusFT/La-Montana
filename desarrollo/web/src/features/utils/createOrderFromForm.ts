import { CreateOrderForm } from "../pedidos/types/create-order";
import { Order } from "../pedidos/types/order";

// formulario de lo que tiene una orden
export function createOrderFromForm(
  form: CreateOrderForm,
  estimatedPrice: number
): Order {
  return {
    id: crypto.randomUUID(),

    code: "Pedido local",

    createdAt: new Date().toISOString(),

    status: "pendiente_revision",

    statusLabel: "En revisión",

    price: estimatedPrice,

    fileName: form.file?.name ?? "",

    fileSize: form.file?.size ?? 0,

    deliveryPoint: null,

    form: {
      pages: form.pages,
      copies: form.copies,
      printType: form.printType,
      paperSize: form.paperSize,
      doubleSided: form.doubleSided,
      bound: form.bound,
      spiralBound: form.spiralBound,
      paymentMethod: form.paymentMethod,
      deliveryPointId: form.deliveryPointId,
    },
  };
}
