// tarjeta que muestra los datos del archivo cargado

import { Order } from "../../types/order";
import { formatFileSize } from "@/features/utils/formatFileSize";

interface OrderFileCardProps
{
  fileName: Order["fileName"];
  fileSize:Order["fileSize"]
}

export function OrderFileCard({
  fileName,
  fileSize
}: OrderFileCardProps){
    return(
<section className="order-card">
      <div className="order-card-title">
        <h2>Archivo</h2>
               
                <div className="summary-file">
                    <p>{fileName}</p>
                    
                    <small>
                    {formatFileSize(fileSize)}
                    </small>
                </div>
      </div>
</section>
    )
}