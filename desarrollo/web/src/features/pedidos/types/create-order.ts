
// interfaz, tipado el formulario para crear orden
export interface CreateOrderForm {
  // file upload section
  file: File | null;

  // Job Details Section
  pages: number | null;
  copies: number;
  printType: "byn" | "color";
  paperSize: "A4" | "A3" | "OFICIO";

  // Order Options Section
  doubleSided: boolean;
  bound: boolean;
  spiralBound: boolean;

  hasCover: boolean;
  hasBinding: boolean;
}