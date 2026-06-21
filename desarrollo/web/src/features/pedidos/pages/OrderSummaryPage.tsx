"use client";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";
import { useRouter } from "next/navigation";

import { SummaryHeader } from "../components/resumen_pedido/SummaryHeader";
import { OrderSummaryCard } from "../components/resumen_pedido/OrderSummaryCard";

import { useCreateOrder } from "../context/CreateOrderContext";
import { OrderEstimateCard } from "../components/resumen_pedido/OrderEstimateCard";
import { PaymentMethodCard } from "../components/resumen_pedido/PaymentMethodCard";
import { DeliveryPointCard } from "../components/resumen_pedido/DeliveryPointCard"
import { calculateEstimatedPrice } from "@/features/utils/calculateEstimatedPrice";
import { OrderSummaryActions } from "../components/resumen_pedido/OrderSummaryActions"
import { createOrderFromForm } from "@/features/utils/createOrderFromForm";
import { saveOrder } from "../services/order-storage";



export function OrderSummaryPage()
  {
  const router = useRouter();

  // importo del context el form 
  const { form, setForm } =  useCreateOrder();

  

  console.log("form",form)
  const handleBack = () => {
    router.push("/pedidos/nuevo");
  };

  // funcion que confirma la orden y la guarda en el localStorage 
  const handleConfirmOrder = () => {  

    const order = createOrderFromForm(
        form,
        estimatedPrice
    );

    // guardo en el localStorage el pedido
    saveOrder(order);
    console.log(order);


    alert("Pedido creado correctamente.");
    // redirección al dashboard
    
    router.push("/dashboard");
  };

  const estimatedPrice =
  calculateEstimatedPrice(form);

  return (
    <ClienteLayout>
      <div className="dashboard-main">

      {/* componente que contiene titulo, subtitulo y botón Volver */}
        <SummaryHeader
          onBack={handleBack}
        />

        <OrderSummaryCard
          form={form}
        />

      {/* se debera modificar el responseTime acorde a reglas de negocio */}
        <OrderEstimateCard
          estimatedPrice={estimatedPrice}
          responseTime=" 24 hs hábiles"
        />
        
         <div className="summary-bottom-row">

           <PaymentMethodCard
            form={form}
            onChange={(field, value) =>
              setForm((prev) => ({
              ...prev,
            [field]: value,
              }))
            }
          />
        
         <DeliveryPointCard
            form={form}
            onChange={(field, value) =>
              setForm((prev) => ({
                ...prev,
                [field]: value,
              }))
            }
          />

        </div> 

        <OrderSummaryActions
          onConfirm={handleConfirmOrder}
        /> 

      </div>
    </ClienteLayout>
  );
}