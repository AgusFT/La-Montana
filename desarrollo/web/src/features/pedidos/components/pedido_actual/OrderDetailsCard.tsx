import { OrderActualForm } from "../../types/order-actual";

interface OrderDetailsCardProps
{
  form: OrderActualForm;
}

export function OrderDetailsCard({
form
}: OrderDetailsCardProps){
    return(
<section className="order-card">
        <h2>Detalles del trabajo</h2>
            
            <span className="metric-label">
              
                    Número de páginas: {form.pages}
            </span>
            <span className="metric-label">
                Cantidad de Copias: {form.copies}
            </span>
             <span className="metric-label">
                Tamaño de hoja: {form.paperSize}
            </span>
            <span className="metric-label">
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
                
                {form.doubleSided ? "✅" : "-"}
                </li>

                <li>
                Encuadernado:
                {" "}
                {form.bound ? "✅" : "-"}
                </li>

                <li>
                Anillado:
                {" "}
                {form.spiralBound ? "✅" : "-"}
                </li>
            </ul>

            </span>
</section>
    )
}