"use client";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";
import { useRouter } from "next/navigation";

import { SummaryHeader } from "../components/resumen_pedido/SummaryHeader";

import { useCreateOrder } from "../context/CreateOrderContext";



export function OrderSummaryPage()
  {
  const router = useRouter();

  // importo del context el form 
  const { form } =  useCreateOrder();

  console.log("form",form)
  const handleBack = () => {
    router.push("/pedidos/nuevo");
  };

  const handleConfirmOrder = () => {
    console.log("Pedido confirmado");
  };

  return (
    <ClienteLayout>
      <div className="dashboard-main">

      {/* componente que contiene titulo, subtitulo y botón Volver */}
        <SummaryHeader
          onBack={handleBack}
        />

        {/* <OrderSummaryCard
          form={mockForm}
        />

        <div className="summary-bottom-row">

          <PaymentMethodCard
            paymentMethod="cash"
            onChange={() => {}}
          />

          <DeliveryPointCard
            deliveryPoint="Lope de Vega 2150, CABA"
          />

        </div>

        <OrderSummaryActions
          onConfirm={handleConfirmOrder}
        /> */}

      </div>
    </ClienteLayout>
  );
}