import { MapPin } from "lucide-react";
import { MOCK_DELIVERY_POINTS } from "@/mocks/delivery-points";
import { CreateOrderForm } from "../../types/create-order";

interface DeliveryPointCardProps {
  form: CreateOrderForm;
  onChange: (
    field: keyof CreateOrderForm,
    value: unknown
  ) => void;
}

export function DeliveryPointCard({
  form,
  onChange,
}: DeliveryPointCardProps) {

  const selectedPoint =
    MOCK_DELIVERY_POINTS.find(
      point => point.id === form.deliveryPointId
    );

  return (
    <section className="summary-card panel-card">

        <label className="form-label" htmlFor="paymentMethod">

      <div className="summary-card-title">
        <MapPin size={20} />
        <h3>Punto de entrega</h3>
      </div>
        </label>

      <div className="delivery-point-content">

        <strong className="delivery-point-name">
          {selectedPoint?.name}
        </strong>

        <span className="delivery-point-address">
          {selectedPoint?.address}
        </span>

        <div className="form-group">
          <label
            htmlFor="deliveryPoint"
            className="form-label"
          >
            Punto seleccionado
          </label>

          <select
            id="deliveryPoint"
            className="form-select"
            value={form.deliveryPointId}
            onChange={(e) =>
              onChange(
                "deliveryPointId",
                e.target.value
              )
            }
          >
            {MOCK_DELIVERY_POINTS.map((point) => (
              <option
                key={point.id}
                value={point.id}
              >
                {point.name}
              </option>
            ))}
          </select>

        </div>

      </div>

    </section>
  );
}