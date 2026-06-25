"use client";

import { useEffect, useState } from "react";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";

import { CurrentOrderHeader } from "../components/pedido_actual/CurrentOrderHeader";
import { OrderStatusTimeline } from "../components/pedido_actual/OrderStatusTimeline";

import { Order } from "../types/order";
import { OrderDetailsCard } from "../components/pedido_actual/OrderDetailsCard";
import { OrderJobCard } from "../components/pedido_actual/OrderInfoCard";
import { OrderFileCard } from "../components/pedido_actual/OrderFileCard";
import { OrderPaymentCard } from "../components/pedido_actual/OrderPaymentCard";
import { OrderDeliveryPointCard } from "../components/pedido_actual/OrderDeliveryPointCard";
import {
  obtenerMensajeErrorPedido,
  obtenerPedidoActualCliente,
} from "../services/pedido-api";

export function PedidoActualPage() {
  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

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

  if (isLoading) {
    return (
      <ClienteLayout>
        <div className="dashboard-main">
          <h1>Pedido Actual</h1>

          <p>
            Cargando pedido...
          </p>
        </div>
      </ClienteLayout>
    );
  }

  if (error) {
    return (
      <ClienteLayout>
        <div className="dashboard-main">
          <h1>Pedido Actual</h1>

          <p className="form-error">
            {error}
          </p>
        </div>
      </ClienteLayout>
    );
  }

  if (!order) {
    return (
      <ClienteLayout>
        <div className="dashboard-main">
          <h1>Pedido Actual</h1>

          <p>
            No existen pedidos registrados.
          </p>
        </div>
      </ClienteLayout>
    );
  }

  return (
    <ClienteLayout>
      
      <div className="dashboard-main">

        <CurrentOrderHeader />

        <OrderStatusTimeline
          status={order.status}
        />

         <div className="order-main-grid">

          
          <OrderDetailsCard
            form={order.form}
          />

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
          
              <OrderPaymentCard
                paymentMethod={order.form.paymentMethod}
                />

            <OrderDeliveryPointCard
                deliveryPointId={order.form.deliveryPointId}
            />

            </div>
             </div>
          </div>
    </ClienteLayout>
  );
}
