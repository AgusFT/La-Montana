import { Clock5, TicketCheck } from "lucide-react";

interface OrderEstimateCardProps {
  estimatedPrice: number;
  responseTime: string;
}

export function OrderEstimateCard({
  estimatedPrice,
  responseTime,
}: OrderEstimateCardProps) {
  return (
    <section className="summary-estimate-card panel-card">
      <div className="estimate-item">
        <span className="estimate-label">
          <Clock5 />
          Tiempo estimado de respuesta: 
        </span>

        <strong className="estimate-value">
          {responseTime}
        </strong>
      </div>

      <div className="estimate-divider" />

      <div className="estimate-item">
        <span className="estimate-label">
          <TicketCheck />
          Precio Final:  
        </span>

        <strong className="estimate-value">
          ${estimatedPrice.toLocaleString("es-AR")}
        </strong>
      </div>
    </section>
  );
}