// tarjeta que muestra los datos del archivo cargado

// PaymentMethodCard
// Responsabilidad:

// Método de pago
// ☐ Transferencia
// ☐ Débito
// ☑ Efectivo

// Props:

// type PaymentMethodCardProps = {
//   paymentMethod: string;
//   onChange: (method: string) => void;
// };

import { CreditCard } from "lucide-react";
import { CreateOrderForm } from "../../types/create-order";

interface OrderPaymentCardProps {
    paymentMethod : CreateOrderForm["paymentMethod"]
}

export function OrderPaymentCard({
paymentMethod
}: OrderPaymentCardProps) {
  return (
    <section className="order-card">
        <h2>Método de pago</h2>
      
        <div className="form-group">
                   
                    <div className="summary-card-title" >
                      <CreditCard size={20} />
                        <strong>
                            {paymentMethod}
                        </strong>
                    </div>               
      </div>
    </section>
  );
}
