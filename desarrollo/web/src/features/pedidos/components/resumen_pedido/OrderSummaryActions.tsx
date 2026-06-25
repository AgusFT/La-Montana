// OrderSummaryActions

import { StickyNotePlus } from "lucide-react";

interface OrderSummaryActionsProps {
  disabled: boolean;
  isLoading?: boolean;
  error?: string;
  onConfirm: () => void | Promise<void>;
}

export function OrderSummaryActions({
  disabled,
  isLoading = false,
  error = "",
  onConfirm,
}: OrderSummaryActionsProps) {
  return (
    <section className="summary-actions">
     <button
        type="button"
        className="summary-create-order-btn"
        disabled={disabled || isLoading}
        onClick={onConfirm}
      >
        <StickyNotePlus size={20} />
        {isLoading ? "Creando pedido..." : "Crear Pedido"}
      </button>   

      {error ? <p className="form-error">{error}</p> : null}

      <p className="summary-actions-note">
        Nos pondremos en contacto para revisar y aprobar tu pedido.
      </p>
    </section>
  );
}
