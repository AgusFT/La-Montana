"use client";

import { useEffect, useState } from "react";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";

import { getLastOrder } from "../services/order-storage";



import { useCreateOrder } from "../context/CreateOrderContext";

import { CurrentOrderHeader } from "../components/pedido_actual/CurrentOrderHeader";
import { OrderStatusTimeline } from "../components/pedido_actual/OrderStatusTimeline";

import { Order } from "../types/order";
import { OrderDetailsCard } from "../components/pedido_actual/OrderDetailsCard";
import { OrderJobCard } from "../components/pedido_actual/OrderInfoCard";
import { OrderFileCard } from "../components/pedido_actual/OrderFileCard";

export function PedidoActualPage() {
  const [order, setOrder] = useState<Order | null>(null);
  
   const { form, setForm } =  useCreateOrder();

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const lastOrder = getLastOrder();
    setOrder(lastOrder ?? null);

    setLoading(false);
  }, []);

  if (loading) {
    return (
      <ClienteLayout>
        <div className="dashboard-main">
          <h1>Pedido Actual</h1>

          <p>Cargando pedido...</p>
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
            {/* <section className="order-card">
              <h2>Detalles del trabajo</h2>
                
            </section> */}

          <OrderFileCard 
            fileName={order.fileName}
            fileSize={order.fileSize}
          />

            <OrderJobCard 
              price={order.price}
              createdAt={order.createdAt}
            />

          </div>

          <div className="order-secondary-grid">
              <section className="order-card">
                <h2>Método de pago</h2>
              </section>

              <section className="order-card">
                <h2>Punto de entrega</h2>
              </section>
            </div>
          </div>
    </ClienteLayout>
  );
}