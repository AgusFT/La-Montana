"use client";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { SummaryHeader } from "../components/resumen_pedido/SummaryHeader";
import { OrderSummaryCard } from "../components/resumen_pedido/OrderSummaryCard";

import { useCreateOrder } from "../context/CreateOrderContext";
import { OrderEstimateCard } from "../components/resumen_pedido/OrderEstimateCard";
import { PaymentMethodCard } from "../components/resumen_pedido/PaymentMethodCard";
import { DeliveryPointCard } from "../components/resumen_pedido/DeliveryPointCard";
import { OrderSummaryActions } from "../components/resumen_pedido/OrderSummaryActions";
import type { CotizacionPedido } from "../services/pedido-api";
import {
  cotizarPedido,
  crearPedidoCompleto,
  obtenerMensajeErrorPedido,
} from "../services/pedido-api";

export function OrderSummaryPage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [cotizacion, setCotizacion] = useState<CotizacionPedido | null>(null);
  const [isQuoteLoading, setIsQuoteLoading] = useState(false);
  const [quoteError, setQuoteError] = useState("");

  const { form, setForm } = useCreateOrder();

  useEffect(() => {
    if (!form.file || !form.pages) {
      return;
    }

    const controller = new AbortController();

    async function cargarCotizacion() {
      await Promise.resolve();

      if (controller.signal.aborted) {
        return;
      }

      setCotizacion(null);
      setIsQuoteLoading(true);
      setQuoteError("");

      try {
        const cotizacionBackend = await cotizarPedido(form, controller.signal);

        setCotizacion(cotizacionBackend);
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }

        setCotizacion(null);
        setQuoteError(obtenerMensajeErrorPedido(error));
      } finally {
        if (!controller.signal.aborted) {
          setIsQuoteLoading(false);
        }
      }
    }

    void cargarCotizacion();

    return () => {
      controller.abort();
    };
  }, [form]);

  const puedeCotizar = Boolean(form.file && form.pages);
  const cotizacionVisible = puedeCotizar ? cotizacion : null;
  const quoteErrorVisible = puedeCotizar ? quoteError : "";
  const isQuoteLoadingVisible = puedeCotizar ? isQuoteLoading : false;

  const handleBack = () => {
    router.push("/pedidos/nuevo");
  };

  const handleConfirmOrder = async () => {
    if (!form.file) {
      setSubmitError("Debe seleccionar un archivo antes de crear el pedido.");
      return;
    }

    if (!cotizacionVisible) {
      setSubmitError(
        quoteErrorVisible || "No pudimos obtener la cotización del pedido.",
      );
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
        <SummaryHeader onBack={handleBack} />

        {/* Detalles del Trabajo (cantidad de paginas calculadas, cantidad de copias solicitadas) */}
        <OrderSummaryCard form={form} />

        <OrderEstimateCard
          disabled={!form.file}
          estimatedPrice={cotizacionVisible?.total_estimado ?? null}
          isLoading={isQuoteLoadingVisible}
          error={quoteErrorVisible}
          requiereSenia={cotizacionVisible?.requiere_senia ?? false}
          porcentajeSenia={cotizacionVisible?.porcentaje_senia ?? 0}
          cantidadEstimada={cotizacionVisible?.cantidad_estimada ?? null}
          responseTime=" 24 hs hábiles"
        />

        <div className="summary-bottom-row">
          <PaymentMethodCard
            form={form}
            onChange={(field, value) =>
              setForm((prev) => ({
                ...prev,
                [field]: value,
              }))}
          />

          <DeliveryPointCard
            form={form}
            onChange={(field, value) =>
              setForm((prev) => ({
                ...prev,
                [field]: value,
              }))}
          />
        </div>

        <OrderSummaryActions
          disabled={!form.file || isQuoteLoadingVisible || !cotizacionVisible}
          isLoading={isSubmitting}
          error={submitError}
          onConfirm={handleConfirmOrder}
        />
      </div>
    </ClienteLayout>
  );
}
