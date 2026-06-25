"use client";

import { Ban } from "lucide-react";
import { useEffect, useState } from "react";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";

import { CurrentOrderHeader } from "../components/pedido_actual/CurrentOrderHeader";
import { OrderDeliveryPointCard } from "../components/pedido_actual/OrderDeliveryPointCard";
import { OrderDetailsCard } from "../components/pedido_actual/OrderDetailsCard";
import { OrderFileCard } from "../components/pedido_actual/OrderFileCard";
import { OrderJobCard } from "../components/pedido_actual/OrderInfoCard";
import { OrderPaymentCard } from "../components/pedido_actual/OrderPaymentCard";
import { OrderStatusTimeline } from "../components/pedido_actual/OrderStatusTimeline";
import {
  cancelarPedidoCliente,
  obtenerMensajeErrorPedido,
  obtenerPedidoActualCliente,
} from "../services/pedido-api";
import type { Order } from "../types/order";

export function PedidoActualPage() {
  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [isCancelling, setIsCancelling] = useState(false);
  const [isConfirmingCancellation, setIsConfirmingCancellation] = useState(false);
  const [cancelError, setCancelError] = useState("");
  const [cancelSuccess, setCancelSuccess] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function cargarPedidoActual() {
      try {
        const pedidoActual = await obtenerPedidoActualCliente();

        if (isMounted) {
          setOrder(pedidoActual);
        }
      } catch (loadError) {
        if (isMounted) {
          setError(obtenerMensajeErrorPedido(loadError));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    cargarPedidoActual();

    return () => {
      isMounted = false;
    };
  }, []);

  async function handleCancelarPedido() {
    if (!order || isCancelling) {
      return;
    }

    const idPedido = Number(order.id);

    setIsCancelling(true);
    setCancelError("");
    setCancelSuccess("");

    try {
      const pedidoCancelado = await cancelarPedidoCliente(
        idPedido,
        "Cancelado por el cliente desde el portal.",
      );

      setOrder({
        ...order,
        status: pedidoCancelado.estado_visible_cliente,
        statusLabel: "Cancelado",
      });
      setIsConfirmingCancellation(false);
      setCancelSuccess("Pedido cancelado correctamente.");
    } catch (cancelError) {
      setCancelError(obtenerMensajeErrorPedido(cancelError));
    } finally {
      setIsCancelling(false);
    }
  }

  function mostrarConfirmacionCancelacion() {
    setCancelError("");
    setCancelSuccess("");
    setIsConfirmingCancellation(true);
  }

  function ocultarConfirmacionCancelacion() {
    if (isCancelling) {
      return;
    }

    setCancelError("");
    setIsConfirmingCancellation(false);
  }

  if (isLoading) {
    return (
      <ClienteLayout>
        <div className="dashboard-main">
          <h1>Pedido Actual</h1>

          <p>Cargando pedido...</p>
        </div>
      </ClienteLayout>
    );
  }

  if (error) {
    return (
      <ClienteLayout>
        <div className="dashboard-main">
          <h1>Pedido Actual</h1>

          <p className="form-error">{error}</p>
        </div>
      </ClienteLayout>
    );
  }

  if (!order) {
    return (
      <ClienteLayout>
        <div className="dashboard-main">
          <h1>Pedido Actual</h1>

          <p>No existen pedidos registrados.</p>
        </div>
      </ClienteLayout>
    );
  }

  const puedeCancelarPedido =
    order.status === "pendiente_revision" && !order.fechaConfirmacionCliente;
  const mostrarAccionCancelacion =
    puedeCancelarPedido || Boolean(cancelError || cancelSuccess);

  return (
    <ClienteLayout>
      <div className="dashboard-main">
        <CurrentOrderHeader
          code={order.code}
          statusLabel={order.statusLabel}
        />

        <OrderStatusTimeline status={order.status} />

        <div className="order-main-grid">
          <OrderDetailsCard form={order.form} />

          <OrderFileCard
            fileName={order.fileName}
            fileSize={order.fileSize}
          />

          <OrderJobCard
            price={order.price}
            createdAt={order.createdAt}
          />
        </div>

        <div className="btn-cancelar--div">
          <div className="order-secondary-grid">
            <OrderPaymentCard paymentMethod={order.form.paymentMethod} />

            <OrderDeliveryPointCard deliveryPoint={order.deliveryPoint} />
          </div>

          {mostrarAccionCancelacion ? (
            <div className="cancel-order-action">
              {cancelError ? (
                <p className="form-error" role="alert">
                  {cancelError}
                </p>
              ) : null}

              {cancelSuccess ? (
                <p className="cancel-order-feedback" role="status">
                  {cancelSuccess}
                </p>
              ) : null}

              {puedeCancelarPedido ? (
                isConfirmingCancellation ? (
                  <div
                    className="cancel-order-confirm"
                    role="group"
                    aria-label="Confirmar cancelación del pedido"
                  >
                    <p>¿Cancelar este pedido?</p>

                    <div className="cancel-order-confirm-actions">
                      <button
                        className="secondary-button"
                        type="button"
                        onClick={ocultarConfirmacionCancelacion}
                        disabled={isCancelling}
                      >
                        Mantener pedido
                      </button>

                      <button
                        className="cancel-order-btn cancel-order-btn--compact"
                        type="button"
                        onClick={handleCancelarPedido}
                        disabled={isCancelling}
                      >
                        <Ban size={18} aria-hidden="true" />
                        {isCancelling
                          ? "Cancelando..."
                          : "Confirmar cancelación"}
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    className="cancel-order-btn"
                    type="button"
                    onClick={mostrarConfirmacionCancelacion}
                  >
                    <Ban size={20} aria-hidden="true" />
                    Cancelar pedido
                  </button>
                )
              ) : null}
            </div>
          ) : null}
        </div>
      </div>
    </ClienteLayout>
  );
}
