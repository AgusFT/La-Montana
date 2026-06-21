// OrderSummaryActions

import { StickyNotePlus } from "lucide-react";

interface OrderSummaryActionsProps {
  onConfirm: () => void;
}

export function OrderSummaryActions({
  onConfirm,
}: OrderSummaryActionsProps) {
  return (
    <section className="summary-actions">
     <button
        className="summary-create-order-btn"
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