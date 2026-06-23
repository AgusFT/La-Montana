import { CreateOrderForm } from "@/features/pedidos/types/create-order";

export interface Order {
  id: string;

  createdAt: string;

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

  price: number;

  // guardamos el nombre del archivo y el tamaño. Ya que el LocalStorage no soporta almacenar un archivo. 
  fileName: string;
  fileSize: number;

  // Omitimos el file,. ya que el LocalStorage no soporta. 
  form: Omit<CreateOrderForm, "file">;
}