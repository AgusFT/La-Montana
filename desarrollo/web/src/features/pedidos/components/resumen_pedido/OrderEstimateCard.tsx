import { Clock5, TicketCheck } from "lucide-react";

interface OrderEstimateCardProps {
  disabled: boolean;
  estimatedPrice: number;
  responseTime: string;
}

export function OrderEstimateCard({
  disabled,
  estimatedPrice,
  responseTime,
}: OrderEstimateCardProps) {
  return (
    <section className="summary-estimate-card panel-card">
      <div className="estimate-item">
        <span className="estimate-label">
          <Clock5 />
         {disabled ? " " : "Tiempo estimado de respuesta:" }
        </span>
        
        <strong className="estimate-value">
         {disabled ? "Cargá un archivo para obtener la cotización" : responseTime}
        </strong>
      </div>

      <div className="estimate-divider" />

      <div className="estimate-item">
        <span className="estimate-label">
          <TicketCheck />
          {disabled ? " " : "Precio Final:" }  
        </span>

        <strong className="estimate-value">
          ${estimatedPrice.toLocaleString("es-AR")}
        </strong>
      </div>
    </section>
  );
}