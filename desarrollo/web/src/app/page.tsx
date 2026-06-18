"use client";

import Image from "next/image";
import { FormEvent, useState } from "react";

const orders = [
  {
    id: "PED-2026-003",
    status: "Cotizado",
    badge: "badge-blue",
    date: "01/06/2026",
    total: "$18.450,00",
  },
  {
    id: "PED-2026-002",
    status: "Entregado",
    badge: "badge-green",
    date: "20/05/2026",
    total: "$9.200,00",
  },
  {
    id: "PED-2026-001",
    status: "Cerrado",
    badge: "badge-gray",
    date: "18/05/2026",
    total: "$7.800,00",
  },
];

const timeline = [
  "Pedido recibido",
  "Archivos cargados",
  "Revisión administrativa",
  "Cotización",
  "Producción",
  "Control de calidad",
  "Listo para entrega",
  "Entregado",
];

function BrandLockup({ compact = false }: { compact?: boolean }) {
  return (
    <div className="brand-lockup">
      <Image
        className="brand-logo"
        src="/logo.jpg"
        alt="La Montaña Impresiones"
        width={64}
        height={64}
        priority
      />
      {!compact && (
        <div>
          <div className="brand-title">La Montaña</div>
          <div className="brand-subtitle">impresiones</div>
        </div>
      )}
    </div>
  );
}

function LoginView({ onLogin }: { onLogin: () => void }) {
  const [email, setEmail] = useState("alejandro@email.com");
  const [password, setPassword] = useState("demo1234");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onLogin();
  }

  return (
    <main className="login-shell">
      <header className="login-header">
        <div className="login-header-inner">
          <BrandLockup />
          <nav className="login-nav" aria-label="Navegación pública">
            <a href="#contacto">Contacto</a>
            <a href="#cuenta">Cuenta</a>
          </nav>
        </div>
      </header>

      <section className="login-main" aria-labelledby="login-title">
        <div className="mountain-mark" aria-hidden="true" />
        <form className="login-card panel-card" onSubmit={handleSubmit}>
          <Image
            className="login-card-logo"
            src="/logo.jpg"
            alt="La Montaña Impresiones"
            width={120}
            height={120}
          />
          <p className="eyebrow">Portal cliente</p>
          <h1 id="login-title">Ingresá a tus pedidos</h1>
          <p className="muted">
            Consultá cotizaciones, subí archivos y seguí el avance de cada
            trabajo desde un solo lugar.
          </p>

          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="tu@email.com"
            />
          </div>

          <div className="field">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Tu contraseña"
            />
          </div>

          <div className="login-actions">
            <button className="primary-button" type="submit">
              Iniciar sesión
            </button>
            <button className="secondary-button" type="button">
              Crear cuenta
            </button>
          </div>
        </form>
      </section>

      <footer className="login-footer" id="contacto">
        <div className="login-footer-inner">
          <div className="footer-feature">
            <span className="metric-dot" style={{ background: "#3498db" }}>
              S
            </span>
            <p>
              <strong>Seguridad</strong>
              <span>Protegemos tus archivos y datos de contacto.</span>
            </p>
          </div>
          <div className="footer-feature">
            <span className="metric-dot" style={{ background: "#27ae60" }}>
              A
            </span>
            <p>
              <strong>Archivos</strong>
              <span>Historial y pedidos siempre disponibles.</span>
            </p>
          </div>
          <div className="footer-feature">
            <span className="metric-dot" style={{ background: "#e67e22" }}>
              T
            </span>
            <p>
              <strong>Trazabilidad</strong>
              <span>Seguimiento claro desde revisión hasta entrega.</span>
            </p>
          </div>
        </div>
      </footer>
    </main>
  );
}

function Sidebar({ onLogout }: { onLogout: () => void }) {
  return (
    <aside className="sidebar">
      <BrandLockup />
      <nav className="side-nav" aria-label="Navegación del panel">
        <button className="is-active" type="button">
          Inicio
        </button>
        <button type="button">Mis pedidos</button>
        <button type="button">Crear pedido</button>
        <button type="button">Puntos de entrega</button>
        <button type="button">Contacto</button>
      </nav>

      <div className="sidebar-help">
        <strong>¿Necesitás ayuda?</strong>
        <span>Escribinos por WhatsApp para revisar tu pedido.</span>
      </div>

      <button className="secondary-button" type="button" onClick={onLogout}>
        Cerrar sesión
      </button>
    </aside>
  );
}

function DashboardView({ onLogout }: { onLogout: () => void }) {
  return (
    <main className="dashboard-shell app-screen">
      <Sidebar onLogout={onLogout} />

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
                    const state =
                      index < 2
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

export default function Home() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  if (isLoggedIn) {
    return <DashboardView onLogout={() => setIsLoggedIn(false)} />;
  }

  return <LoginView onLogin={() => setIsLoggedIn(true)} />;
}
