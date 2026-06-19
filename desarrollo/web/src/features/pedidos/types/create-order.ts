
// interfaz, tipado el formulario para crear orden
export interface CreateOrderForm {
  file: File | null;
  pages: number | null;
  copies: number;
  printType: "byn" | "color";
  paperSize: "A4" | "A3" | "OFICIO";
  hasCover: boolean;
  hasBinding: boolean;
}