// boton para cancelar un pedido. Mientras se encuentre en Revisión quedará activo. 

import { Ban } from "lucide-react";
import { cancelLastCancelableOrder } from "../../services/order-storage";

interface CancelOrderActionProps {
  onCancelled?: () => void;
}

export function CancelOrderAction({
  onCancelled,
}: CancelOrderActionProps) {

  function handleCancelOrder() {

    const confirmed = window.confirm(
      "¿Deseás cancelar el pedido actual?"
    );

    if (!confirmed) {
      return;
    }

    const cancelled =
      cancelLastCancelableOrder();

    if (!cancelled) {
      window.alert(
        "El pedido ya no puede ser cancelado."
      );
      return;
    }

    onCancelled?.();
  }

  return (
    <button
      type="button"
      className="cancel-order-btn"
      onClick={handleCancelOrder}
    >
      <Ban size={18} />

      Cancelar pedido
    </button>
  );
}