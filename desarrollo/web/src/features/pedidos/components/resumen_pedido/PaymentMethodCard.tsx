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

interface PaymentMethodCardProps {
  form: CreateOrderForm;

  onChange: (
    field: keyof CreateOrderForm,
    value: unknown
  ) => void;
}

export function PaymentMethodCard({
  form,
  onChange
}: PaymentMethodCardProps) {
  return (
    <section className="summary-card panel-card">

      <div className="summary-metrics">
            
            <div className="form-group">
                    <label className="form-label" htmlFor="paymentMethod">
                    <div className="summary-card-title" >
                      <CreditCard size={20} />
                      <h3>Método de pago</h3>
                    </div>
          
                    </label>

                    <select
                    id="paymentMethod"
                    className="form-select"
                    value={form.paymentMethod}
                    onChange={(e) =>
                        onChange("paymentMethod", e.target.value)
                    }
                    >
                    <option value="Efectivo">Efectivo</option>
                    <option value="Transferencia">Transferencia</option>
                    <option value="Debito">Debito</option>
                    </select>
            </div>

      </div>
    </section>
  );
}
