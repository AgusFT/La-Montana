"use client";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { SummaryHeader } from "../components/resumen_pedido/SummaryHeader";
import { OrderSummaryCard } from "../components/resumen_pedido/OrderSummaryCard";

import { useCreateOrder } from "../context/CreateOrderContext";
import { OrderEstimateCard } from "../components/resumen_pedido/OrderEstimateCard";
import { PaymentMethodCard } from "../components/resumen_pedido/PaymentMethodCard";
import { DeliveryPointCard } from "../components/resumen_pedido/DeliveryPointCard"
import { calculateEstimatedPrice } from "@/features/utils/calculateEstimatedPrice";
import { OrderSummaryActions } from "../components/resumen_pedido/OrderSummaryActions"
import {
  crearPedidoCompleto,
  obtenerMensajeErrorPedido,
} from "../services/pedido-api";



export function OrderSummaryPage()
  {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");

  // importo del context el form 
  const { form, setForm } =  useCreateOrder();
  const estimatedPrice =
  calculateEstimatedPrice(form);

  const handleBack = () => {
    router.push("/pedidos/nuevo");
  };

  // funcion que confirma el pedido contra Supabase Edge Functions
const handleConfirmOrder = async () => {
  if (!form.file) {
    setSubmitError("Debe seleccionar un archivo antes de crear el pedido.");
    return;
  }

  setIsSubmitting(true);
  setSubmitError("");

  try {
    await crearPedidoCompleto(form);

    router.push("/pedidos/actual");
    router.refresh();
  } catch (error) {
    setSubmitError(obtenerMensajeErrorPedido(error));
  } finally {
    setIsSubmitting(false);
  }
};


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
          isLoading={isSubmitting}
          error={submitError}
          onConfirm={handleConfirmOrder}
        /> 

      </div>
    </ClienteLayout>
  );
}
