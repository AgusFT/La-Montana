import { Clock5, TicketCheck } from "lucide-react";

interface OrderEstimateCardProps {
  disabled: boolean;
  estimatedPrice: number | null;
  isLoading?: boolean;
  error?: string;
  requiereSenia?: boolean;
  porcentajeSenia?: number;
  cantidadEstimada?: number | null;
  responseTime: string;
}

export function OrderEstimateCard({
  disabled,
  estimatedPrice,
  isLoading = false,
  error = "",
  requiereSenia = false,
  porcentajeSenia = 0,
  cantidadEstimada = null,
  responseTime,
}: OrderEstimateCardProps) {
  const priceLabel = obtenerEtiquetaPrecio({
    disabled,
    isLoading,
    estimatedPrice,
  });
  const quoteReady = !disabled && !isLoading && !error &&
    estimatedPrice !== null;

  return (
    <section className="summary-estimate-card panel-card">
      <div className="estimate-item">
        <span className="estimate-label">
          <Clock5 />
          {disabled ? " " : "Tiempo estimado de respuesta:"}
        </span>

        <strong className="estimate-value">
          {disabled
            ? "Cargá un archivo para obtener la cotización"
            : responseTime}
        </strong>
      </div>

      <div className="estimate-divider" />

      <div className="estimate-item">
        <span className="estimate-label">
          <TicketCheck />
          {disabled ? " " : "Precio Final:"}
        </span>

        <div className="estimate-content">
          <strong className="estimate-value">
            {priceLabel}
          </strong>

          {quoteReady
            ? (
              <span className="estimate-helper">
                {requiereSenia
                  ? `Requiere seña del ${porcentajeSenia}%`
                  : "No requiere seña"}
                {cantidadEstimada ? ` · ${cantidadEstimada} impresiones` : ""}
              </span>
            )
            : null}

          {error ? <p className="form-error estimate-error">{error}</p> : null}
        </div>
      </div>
    </section>
  );
}

function obtenerEtiquetaPrecio({
  disabled,
  isLoading,
  estimatedPrice,
}: {
  disabled: boolean;
  isLoading: boolean;
  estimatedPrice: number | null;
}) {
  if (disabled) {
    return "Pendiente";
  }

  if (isLoading) {
    return "Calculando...";
  }

  if (estimatedPrice === null) {
    return "Sin cotización";
  }

  return `$${estimatedPrice.toLocaleString("es-AR")}`;
}
