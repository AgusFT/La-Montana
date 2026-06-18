"use client";

import { Sidebar } from "@/components/navigation/DashboardSidebar";
import { BrandLockup } from "@/components/brand/BrandLockup";
import { orders } from "@/mocks/orders";
import { timeline } from "@/mocks/timeline";


export function DashboardView() {
  return (
    <main className="dashboard-shell app-screen">

      {/* Sidebar de navegacion */}
      <Sidebar />

      <section className="dashboard-content">
        <header className="topbar">
          <BrandLockup compact />
          <div className="topbar-actions">
            <a>Contacto</a>
            <span className="avatar" aria-label="Usuario Alejandro">
              A
            </span>
            <span>Alejandro</span>
          </div>
        </header>

        <div className="dashboard-main">
          <section className="welcome-grid">
            <div className="welcome-panel panel-card">
              <div className="mountain-mark" aria-hidden="true" />
              <div className="welcome-copy">
                <p className="eyebrow">Panel cliente</p>
                <h1>Hola Alejandro</h1>
                <p className="muted">
                  Gestioná tus trabajos de impresión, revisá cotizaciones y
                  seguí el estado visible de cada pedido.
                </p>
              </div>
            </div>

            <aside className="summary-panel panel-card">
              <h2>Resumen general</h2>
              <div className="summary-list">
                <div className="summary-row">
                  <span
                    className="metric-dot"
                    style={{ background: "#3498db" }}
                  >
                    3
                  </span>
                  <span>Pedidos realizados</span>
                </div>
                <div className="summary-row">
                  <span
                    className="metric-dot"
                    style={{ background: "#e67e22" }}
                  >
                    1
                  </span>
                  <span>Pedido en curso</span>
                </div>
                <div className="summary-row">
                  <span
                    className="metric-dot"
                    style={{ background: "#27ae60" }}
                  >
                    L
                  </span>
                  <span>Sucursal Centro como punto de entrega</span>
                </div>
              </div>
            </aside>
          </section>

          <section className="dashboard-grid">
            <div className="stack">
              <article className="current-order panel-card">
                <div className="current-order-header">
                  <div>
                    <p className="eyebrow">Pedido actual</p>
                    <h2>PED-2026-004</h2>
                    <p>Tu pedido está siendo revisado por nuestro equipo.</p>
                  </div>
                  <span className="status-badge">En revisión</span>
                </div>

                <div className="timeline">
                  {timeline.map((step, index) => {
                    const state = index < 2
                      ? "is-done"
                      : index === 2
                        ? "is-active"
                        : "";

                    return (
                      <div className={`timeline-item ${state}`} key={step}>
                        <span className="timeline-index">
                          {index < 2 ? "✓" : index + 1}
                        </span>
                        <span>{step}</span>
                      </div>
                    );
                  })}
                </div>
              </article>

              <article className="table-card panel-card">
                <div className="table-heading">
                  <h2>Mis pedidos</h2>
                  <button className="secondary-button" type="button">
                    Ver todos
                  </button>
                </div>

                <table className="orders-table">
                  <thead>
                    <tr>
                      <th>Pedido</th>
                      <th>Estado visible</th>
                      <th>Fecha</th>
                      <th>Total</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orders.map((order) => (
                      <tr key={order.id}>
                        <td>{order.id}</td>
                        <td>
                          <span className={`badge ${order.badge}`}>
                            {order.status}
                          </span>
                        </td>
                        <td>{order.date}</td>
                        <td className="price">{order.total}</td>
                        <td>
                          <button className="secondary-button" type="button">
                            Ver detalle
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </article>
            </div>

            <aside className="stack">
              <article className="delivery-panel panel-card">
                <h2>Punto de entrega</h2>
                <div className="delivery-box">
                  <h3>Sucursal Centro</h3>
                  <p className="muted">
                    Av. Corrientes 1234
                    <br />
                    CABA, Argentina
                  </p>
                </div>
                <button className="secondary-button" type="button">
                  Cambiar punto
                </button>
              </article>

              <article className="cta-panel panel-card">
                <span className="cta-icon">+</span>
                <div>
                  <h2>Crear nuevo pedido</h2>
                  <p className="muted">
                    Comenzá un nuevo trabajo subiendo tus archivos.
                  </p>
                </div>
                <button className="primary-button" type="button">
                  Crear pedido
                </button>
              </article>
            </aside>
          </section>
        </div>
      </section>
    </main>
  );
}
