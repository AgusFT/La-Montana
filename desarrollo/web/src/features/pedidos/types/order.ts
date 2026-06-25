import { CreateOrderForm } from "@/features/pedidos/types/create-order";

export interface OrderDeliveryPoint {
  id: number;
  name: string;
  address: string;
  reference: string | null;
}

export interface Order {
  id: string;

  code: string;

  createdAt: string;

  fechaConfirmacionCliente: string | null;

  status:
     | "pendiente_revision"
     | "aprobado"
     | "corregir"
     | "rechazado"
     | "produccion"
     | "control_de_calidad"
     | "listo_para_entregar"
     | "en_viaje"
     | "entregado"
     | "cancelado";

  statusLabel: string;

  price: number | null;

  // guardamos el nombre del archivo y el tamaño. Ya que el LocalStorage no soporta almacenar un archivo. 
  fileName: string;
  fileSize: number;

  deliveryPoint: OrderDeliveryPoint | null;

  // Omitimos el file,. ya que el LocalStorage no soporta. 
  form: Omit<CreateOrderForm, "file">;
}
