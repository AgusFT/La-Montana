"use client";

import { useEffect, useState } from "react";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";

import { getLastOrder } from "../services/order-storage";
import { formatFileSize } from "@/features/utils/formatFileSize";

import { CurrentOrderHeader } from "../components/pedido_actual/CurrentOrderHeader";
import { OrderStatusTimeline } from "../components/pedido_actual/OrderStatusTimeline";

import { Order } from "../types/order";

export function PedidoActualPage() {
  const [order, setOrder] = useState<Order | null>(null);

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

        {/* <div className="order-grid">

          <section className="panel-card">
            <h2>
              Información del pedido
            </h2>

            <p>
              Fecha de creación:
            </p>

            <strong>
              {new Date(
                order.createdAt
              ).toLocaleString("es-AR")}
            </strong>

            <p>
              Precio estimado:
            </p>

            <strong>
              $
              {order.price.toLocaleString(
                "es-AR"
              )}
            </strong>
          </section>

          <section className="panel-card">
            <h2>Archivo</h2>

            <p>
              {order.fileName}
            </p>

            <p>
              {formatFileSize(
                order.fileSize
              )}
            </p>
          </section>

          <section className="panel-card">
            <h2>Detalles</h2>

            <p>
              Páginas: {order.form.pages}
            </p>

            <p>
              Copias: {order.form.copies}
            </p>

            <p>
              Tamaño: {order.form.paperSize}
            </p>

            <p>
              Impresión: {order.form.printType}
            </p>
          </section>

          <section className="panel-card">
            <h2>
              Entrega y pago
            </h2>

            <p>
              Método:
              {" "}
              {order.form.paymentMethod}
            </p>

            <p>
              Punto:
              {" "}
              {order.form.deliveryPointId}
            </p>
          </section>

        </div> */}

      </div>
    </ClienteLayout>
  );
}