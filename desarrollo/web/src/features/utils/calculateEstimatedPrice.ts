import { CreateOrderForm } from "../pedidos/types/create-order";
import { MOCK_PRICING  } from "./../../mocks/pricing";

export function calculateEstimatedPrice(
  form: CreateOrderForm
) {
  if (!form.pages) return 0;

  const pagePrice =
    form.printType === "color"
      ? MOCK_PRICING.colorPage
      : MOCK_PRICING.blackAndWhitePage;

  let total =
    form.pages *
    form.copies *
    pagePrice;

    // falta definir si se cobra o no el doble faz 
    if (form.doubleSided) {
     total *= MOCK_PRICING.doubleSidedMultiplier;
    }

  if (form.bound) {
    total += MOCK_PRICING.bound;
  }

  if (form.spiralBound) {
    total += MOCK_PRICING.spiralBound;
  }

  return total;
}