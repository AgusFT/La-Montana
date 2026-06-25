import { ORDER_STATUS_STEPS } from "./../../constants/order-status";
import { OrderStatusStep } from "./OrderStatusStep";
import { Order } from "../../types/order";

interface OrderStatusTimelineProps {
  status: Order["status"];
}

export function OrderStatusTimeline({
  status,
}: OrderStatusTimelineProps) {
  const steps = status === "cancelado"
    ? [{ id: "cancelado", label: "Cancelado" }]
    : ORDER_STATUS_STEPS;

  const currentIndex =
    steps.findIndex(
      (step) => step.id === status
    );

  return (
    <section className="timeline-card">

      {steps.map(
        (step, index) => {

          let variant:
            | "completed"
            | "current"
            | "pending";

          if (index < currentIndex) {
            variant = "completed";
          } else if (
            index === currentIndex
          ) {
            variant = "current";
          } else {
            variant = "pending";
          }

          return (
            <OrderStatusStep
              key={step.id}
              label={step.label}
              variant={variant}
            />
          );
        }
      )}

    </section>
  );
}
