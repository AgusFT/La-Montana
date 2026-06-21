// OrderSummaryCard
// Responsabilidad:
// Resumen del pedido
// Número de páginas
// Copias
// Tamaño hoja
// Tipo impresión
// Opcionales
// Nombre archivo
// Tamaño archivo
// Tiempo estimado
// Precio estimado

// Props:

// type OrderSummaryCardProps = {
//   form: CreateOrderForm;
// };

import { CreateOrderForm } from "../../types/create-order";

interface OrderSummaryCardProps {
  form: CreateOrderForm;
}

export function OrderSummaryCard({
  form,
}: OrderSummaryCardProps) {
  return (
    <section className="summary-card">
      <h2>Resumen de tu pedido</h2>

      {/* Métricas */}
      <div className="summary-metrics">

        <div className="summary-metric-card">
            <h3> Trabajo </h3>
            <div className="resumen-texto">
            <span className="metric-label">
                Número de Páginas:
            </span>

            <strong className="metric-value">
                {form.pages ?? "-"}
            </strong>
            </div>

            <div className="resumen-texto">
            <span className="metric-label">
                Cantidad de Copias:
            </span>
            <strong className="metric-value step-badge--form">
                {form.copies}
            </strong>
            </div>

        </div>

        <div className="summary-metric-card">
            <h3> Configuración </h3>
            <div className="resumen-texto">
                <span className="metric-label">
                    Tamaño de Hoja
                </span>

                <strong className="metric-value step-badge--form">
                    {form.paperSize}
                </strong>
            </div>

            <ul className="summary-list">
                <li>
                Tipo de impresión:
                {" "}
                {form.printType === "color"
                    ? "Color"
                    : "Blanco y negro"}
                </li>

                <li>
                Doble faz:
                
                {" "}
                
                {form.doubleSided ? "✅" : "✖️"}
                </li>

                <li>
                Encuadernado:
                {" "}
                {form.bound ? "✅" : "✖️"}
                </li>

                <li>
                Anillado:
                {" "}
                {form.spiralBound ? "✅" : "✖️"}
                </li>
            </ul>
        </div>
        
        {/* Archivo */}
        <div className="summary-metric-card">
            <div className="summary-section">
                <h3>Archivo</h3>

                {form.file ? (
                <div className="summary-file">
                    <p>{form.file.name}</p>

                    <small>
                    {(form.file.size / 1024 / 1024).toFixed(2)}
                    {" "}MB
                    </small>
                </div>
                ) : (
                <p>No se cargó ningún archivo.</p>
                )}
            </div>
        </div>

      </div>

      
    </section>
  );
}