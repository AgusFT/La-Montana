import { MapPin } from "lucide-react";
import { Order } from "../../types/order";

interface OrderDeliveryPointCardProps {
  deliveryPoint: Order["deliveryPoint"];
}

export function OrderDeliveryPointCard({
  deliveryPoint,
}: OrderDeliveryPointCardProps) {
  return (
    <section className="order-card">
      <h2>Punto de entrega</h2>

      <div className="form-group">
        <div className="summary-card-title">
          <MapPin />
          <strong>
            {deliveryPoint?.name ?? "Punto de entrega no informado"}
          </strong>
        </div>

        {deliveryPoint ? (
          <p className="muted">
            {deliveryPoint.address}
          </p>
        ) : (
          <p className="muted">
            No encontramos un punto de entrega asociado al pedido.
          </p>
        )}
      </div>
    </section>
  );
}
