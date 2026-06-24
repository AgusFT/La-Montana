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
         <h3>Trabajo</h3>
        <div className="summary-group">
          <div className="summary-row">
            <span className="summary-row-label">
              Número de páginas
            </span>
          
            <span className="summary-row-value">
              {form.pages ?? "-"}
            </span>
          </div>
          
          <div className="summary-row">
            <span className="summary-row-label">
              Cantidad de copias
            </span>
          
            <span className="summary-row-value">
              {form.copies}
            </span>
          </div>
        </div>
      </div>

{/* configuracion */}
        <div className="summary-metric-card">
            <h3> Configuración </h3>
            <div className="resumen-texto">
                <div className="summary-row">
                  <span className="summary-row-label">
                    Tamaño de hoja
                  </span>

                  <span className="summary-row-value">
                    {form.paperSize}
                  </span>
                </div>
            </div>
            <div className="summary-details">
                <div className="summary-row">
                  <span>Tipo de impresión</span>

                  <span>
                    {form.printType === "color"
                      ? "Color"
                      : "Blanco y negro"}
                  </span>
                </div>
                    
                <div className="summary-row">
                  <span>Doble faz</span>
                    
                  <span>
                    {form.doubleSided ? "Sí" : "No"}
                  </span>

                  <span>Encuadernado</span>

                  <span>
                    {form.bound ? "Sí" : "No"}
                  </span>

                   <span>Anillado</span>
                    
                  <span>
                    {form.spiralBound ? "Sí" : "No"}
                  </span>
                </div>
            </div>
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