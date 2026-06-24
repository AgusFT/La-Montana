// OrderSummaryActions

import { StickyNotePlus } from "lucide-react";

interface OrderSummaryActionsProps {
   disabled: boolean;
  onConfirm: () => void;
}

export function OrderSummaryActions({
  disabled,
  onConfirm,
}: OrderSummaryActionsProps) {
  console.log("estadp", disabled);
  return (
    <section className="summary-actions">
     <button
        type="button"
        className="summary-create-order-btn"
        disabled={disabled}
        onClick={onConfirm}
      >
        <StickyNotePlus size={20} />
        Crear Pedido
      </button>   

      <p className="summary-actions-note">
        Nos pondremos en contacto para revisar y aprobar tu pedido.
      </p>
    </section>
  );
}