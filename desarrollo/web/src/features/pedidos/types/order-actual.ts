// import { CreateOrderForm } from "@/features/pedidos/types/create-order";
// interfaz, tipado el formulario para crear orden
export interface OrderActualForm {
  // Job Details Section
  pages: number | null;
  copies: number;
  printType: "byn" | "color";
  paperSize: "A4" | "A3" | "OFICIO";

  // Order Options Section
  doubleSided: boolean;
  bound: boolean;
  spiralBound: boolean;

  // Para la sección de metodo de pago
  paymentMethod: "Efectivo" | "Transferencia" | "Debito";

  // Delivery
  deliveryPointId: number | null;

  //   form: Omit<CreateOrderForm, "file">;
}
