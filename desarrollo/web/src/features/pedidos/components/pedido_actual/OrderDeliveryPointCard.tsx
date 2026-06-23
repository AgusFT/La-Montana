// tarjeta que muestra punyo de delivery

import { CreateOrderForm } from "../../types/create-order"
import { MOCK_DELIVERY_POINTS } from "@/mocks/delivery-points";
import { MapPin } from "lucide-react";

interface OrderDeliveryPointCardProps{
    deliveryPointId : CreateOrderForm["deliveryPointId"]
}

export function OrderDeliveryPointCard({
    deliveryPointId 
}: OrderDeliveryPointCardProps
){
    // matcheo el ID con la lista de puntos de delivery y obtengo los datos (Este Mock se debe migrar a supabase)
     const selectedPoint =
        MOCK_DELIVERY_POINTS.find(
          point => point.id === deliveryPointId
        );

        console.log("selec ",selectedPoint)
    return(
        <section className="order-card">
          
            <h2>Punto de entrega</h2>
        
            <div className="form-group">
                    
                        <div className="summary-card-title" >
                          <MapPin />
                            <strong>
                                {selectedPoint?.name} | {selectedPoint?.address}
                            </strong>
                        </div>               
        </div>
        </section>
    )
}