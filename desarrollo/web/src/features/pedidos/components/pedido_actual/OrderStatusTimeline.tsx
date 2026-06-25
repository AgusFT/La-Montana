import { ORDER_STATUS_STEPS } from "./../../constants/order-status";
import { OrderStatusStep } from "./OrderStatusStep";
import { Order } from "../../types/order";

interface OrderStatusTimelineProps {
  status: Order["status"];
}

export function OrderStatusTimeline({
  status,
}: OrderStatusTimelineProps) {

  const currentIndex =
    ORDER_STATUS_STEPS.findIndex(
      (step) => step.id === status
    );

  return (
    <section className="timeline-card">

      {ORDER_STATUS_STEPS.map(
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