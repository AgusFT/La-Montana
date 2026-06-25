import { Order } from "../../types/order";


interface OrderJobCardProps
{
  price: Order["price"];
  createdAt:Order["createdAt"]
}

export function OrderJobCard({
  price,
  createdAt
}: OrderJobCardProps){
  const formattedPrice = typeof price === "number"
    ? `$ ${price.toLocaleString("es-AR")}`
    : "A confirmar";

    return(
<section className="order-card">
  <div className="order-card-title">
        <h2>Información del pedido</h2>
            <p> Fecha de creación: 
            <strong>
              {new Date(
                createdAt
              ).toLocaleString("es-AR", { dateStyle: "short", timeStyle: "short"})}
            </strong>
            </p>

            <p>
              Precio: 
            <strong>
              {formattedPrice}
            </strong>
            </p>
  </div>

</section>
    )
}
