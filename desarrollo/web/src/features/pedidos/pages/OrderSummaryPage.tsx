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
  if (!form.file) {
    alert(
      "Debe seleccionar un archivo antes de crear el pedido."
    );
    return;
  }

  const order = createOrderFromForm(
    form,
    estimatedPrice
  );

  saveOrder(order);

  alert("Pedido creado correctamente.");

  router.push("/dashboard");
};

  console.log("file", form.file);
  console.log("disabled", !form.file);

  const estimatedPrice =
  calculateEstimatedPrice(form);

  return (
    <ClienteLayout>
      <div className="dashboard-main">

      {/* componente que contiene titulo, subtitulo y botón Volver */}
        <SummaryHeader
          onBack={handleBack}
        />

        {/* Detalles del Trabajo (cantidad de paginas calculadas, cantidad de copias solicitadas) */}
        <OrderSummaryCard
          form={form}
        />

      {/* se debera modificar el responseTime acorde a reglas de negocio */}
        <OrderEstimateCard
          disabled={!form.file}
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
          disabled={!form.file}
          onConfirm={handleConfirmOrder}
        /> 

      </div>
    </ClienteLayout>
  );
}