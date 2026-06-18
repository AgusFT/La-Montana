export interface CreateOrderForm {
  file: File | null;

  copies: number;

  paperSize: string;

  printType: "byn" | "color";

  hasCover: boolean;

  hasBinding: boolean;
}