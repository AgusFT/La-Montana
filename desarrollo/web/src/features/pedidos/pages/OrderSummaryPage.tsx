"use client";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";
import { useRouter } from "next/navigation";

import { SummaryHeader } from "../components/resumen_pedido/SummaryHeader";

export function OrderSummaryPage() {
  const router = useRouter();

  const handleBack = () => {
    router.push("/pedidos/nuevo");
  };

  const handleConfirmOrder = () => {
    console.log("Pedido confirmado");
  };

  return (
    <ClienteLayout>
      <div className="dashboard-main">

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