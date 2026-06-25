"use client";

import { useEffect, useState } from "react";

import { ClienteLayout } from "@/layouts/cliente/ClienteLayout";
import {
  obtenerPerfilAutenticado,
  type PerfilUsuario,
} from "@/lib/auth/profile";
import { crearClienteSupabaseBrowser } from "@/lib/supabase/client";
import { useRouter } from "next/navigation";

import {
  EstadoPedidoDashboard,
  obtenerMensajeErrorDashboard,
  obtenerResumenDashboardCliente,
  PedidoDashboardCliente,
  ResumenDashboardCliente,
} from "../services/dashboard-api";

const TIMELINE_PEDIDO = [
  "Recibido",
  "Archivos cargados",
  "Revisión administrativa",
  "Producción",
  "Listo para entregar",
];

export function DashboardView() {
  const router = useRouter();
  const [resumen, setResumen] = useState<ResumenDashboardCliente | null>(null);
  const [perfil, setPerfil] = useState<PerfilUsuario | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function cargarDashboard() {
      try {
        const supabase = crearClienteSupabaseBrowser();
        const [resumenDashboard, perfilActual] = await Promise.all([
          obtenerResumenDashboardCliente(),
          obtenerPerfilAutenticado(supabase),
        ]);

        if (isMounted) {
          setResumen(resumenDashboard);
          setPerfil(perfilActual);
        }
      } catch (loadError) {
        if (isMounted) {
          setError(obtenerMensajeErrorDashboard(loadError));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    cargarDashboard();

    return () => {
      isMounted = false;
    };
  }, []);

  function navigateToCreateOrder() {
    router.push("/pedidos/nuevo");
  }

  function navigateToCurrentOrder() {
    router.push("/pedidos/actual");
  }

  const nombreCliente = perfil?.nombreVisible ?? "cliente";
  const pedidoActual = resumen?.pedido_actual ?? null;
  const puntoEntrega = resumen?.punto_entrega_principal ?? null;

  return (
    <ClienteLayout>
      <div className="dashboard-main">
        <section className="welcome-grid">
          <div className="welcome-panel panel-card">
            <div className="mountain-mark" aria-hidden="true" />
            <div className="welcome-copy">
              <p className="eyebrow">Panel cliente</p>
              <h1>Hola {nombreCliente}</h1>
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
                <span className="metric-dot resumen">
                  {isLoading ? "..." : resumen?.total_pedidos ?? 0}
                </span>
                <span>Pedidos realizados</span>
              </div>
              <div className="summary-row">
                <span className="metric-dot realizado">
                  {isLoading ? "..." : resumen?.pedidos_en_curso ?? 0}
                </span>
                <span>Pedidos en curso</span>
              </div>
              <div className="summary-row">
                <span className="metric-dot sucursal">
                  {puntoEntrega?.nombre[0]?.toUpperCase() ?? "-"}
                </span>
                <span>
                  {puntoEntrega
                    ? `${puntoEntrega.nombre} como punto de entrega`
                    : "Sin punto de entrega registrado"}
                </span>
              </div>
            </div>
          </aside>
        </section>

        {error ? (
          <p className="form-error">
            {error}
          </p>
        ) : null}

        <section className="dashboard-grid">
          <div className="stack">
            <article className="current-order panel-card">
              <div className="current-order-header">
                <div>
                  <p className="eyebrow">Pedido actual</p>
                  <h2>{obtenerTituloPedidoActual(pedidoActual, isLoading)}</h2>
                  <p>
                    {obtenerDescripcionPedidoActual(pedidoActual, isLoading)}
                  </p>
                </div>
                <span className="status-badge">
                  {pedidoActual?.estado_visible_label ??
                    (isLoading ? "Cargando" : "Sin pedidos")}
                </span>
              </div>

              <div className="timeline">
                {pedidoActual
                  ? TIMELINE_PEDIDO.map((step, index) => {
                    const state = obtenerEstadoTimeline(
                      pedidoActual.estado_visible_cliente,
                      index,
                    );

                    return (
                      <div className={`timeline-item ${state}`} key={step}>
                        <span className="timeline-index">
                          {state === "is-done" ? "✓" : index + 1}
                        </span>
                        <span>{step}</span>
                      </div>
                    );
                  })
                  : (
                    <p className="muted">
                      {isLoading
                        ? "Cargando seguimiento del pedido..."
                        : "Cuando confirmes un pedido, vas a ver su seguimiento acá."}
                    </p>
                  )}
              </div>
            </article>

            <article className="table-card panel-card">
              <div className="table-heading">
                <h2>Mis pedidos</h2>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={navigateToCurrentOrder}
                >
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
                  {isLoading ? (
                    <tr>
                      <td colSpan={5}>
                        <span className="muted">Cargando pedidos...</span>
                      </td>
                    </tr>
                  ) : resumen?.pedidos_recientes.length ? (
                    resumen.pedidos_recientes.map((pedido) => (
                      <tr key={pedido.id_pedido}>
                        <td>{pedido.codigo}</td>
                        <td>
                          <span className={`badge ${obtenerBadgeEstado(pedido.estado_visible_cliente)}`}>
                            {pedido.estado_visible_label}
                          </span>
                        </td>
                        <td>{formatearFecha(pedido.fecha_creacion)}</td>
                        <td className="price">
                          {formatearMoneda(pedido.total_estimado)}
                        </td>
                        <td>
                          <button
                            className="secondary-button"
                            type="button"
                            onClick={navigateToCurrentOrder}
                          >
                            Ver detalle
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={5}>
                        <span className="muted">
                          Todavía no tenés pedidos registrados.
                        </span>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </article>
          </div>

          <aside className="stack">
            <article className="delivery-panel panel-card">
              <h2>Punto de entrega</h2>
              <div className="delivery-box">
                <h3>{puntoEntrega?.nombre ?? "Sin punto seleccionado"}</h3>
                <p className="muted">
                  {puntoEntrega?.direccion_texto ??
                    "Elegí un punto de entrega al crear tu próximo pedido."}
                </p>
              </div>
              <button
                className="secondary-button"
                type="button"
                onClick={navigateToCreateOrder}
              >
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
              <button
                className="primary-button --radius-lg"
                type="button"
                onClick={navigateToCreateOrder}
              >
                Crear pedido
              </button>
            </article>
          </aside>
        </section>
      </div>
    </ClienteLayout>
  );
}

function obtenerTituloPedidoActual(
  pedido: PedidoDashboardCliente | null,
  isLoading: boolean,
): string {
  if (isLoading) {
    return "Cargando pedido...";
  }

  return pedido?.codigo ?? "Sin pedido actual";
}

function obtenerDescripcionPedidoActual(
  pedido: PedidoDashboardCliente | null,
  isLoading: boolean,
): string {
  if (isLoading) {
    return "Estamos consultando el estado visible de tus pedidos.";
  }

  if (!pedido) {
    return "Todavía no tenés pedidos activos para seguir.";
  }

  const archivo = pedido.archivo?.nombre_original ?? pedido.descripcion;

  if (archivo) {
    return `Tu pedido ${archivo} está en estado ${pedido.estado_visible_label}.`;
  }

  return `Tu pedido está en estado ${pedido.estado_visible_label}.`;
}

function obtenerEstadoTimeline(
  estado: EstadoPedidoDashboard,
  index: number,
): string {
  const indiceActivo = obtenerIndiceTimeline(estado);

  if (index < indiceActivo) {
    return "is-done";
  }

  if (index === indiceActivo) {
    return "is-active";
  }

  return "";
}

function obtenerIndiceTimeline(estado: EstadoPedidoDashboard): number {
  if (estado === "entregado") {
    return TIMELINE_PEDIDO.length;
  }

  if (estado === "produccion" || estado === "control_de_calidad") {
    return 3;
  }

  if (estado === "listo_para_entregar" || estado === "en_viaje") {
    return 4;
  }

  if (estado === "aprobado") {
    return 2;
  }

  return 2;
}

function obtenerBadgeEstado(estado: EstadoPedidoDashboard): string {
  if (estado === "entregado" || estado === "listo_para_entregar") {
    return "badge-green";
  }

  if (estado === "cancelado" || estado === "rechazado") {
    return "badge-gray";
  }

  return "badge-blue";
}

function formatearFecha(fecha: string): string {
  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(fecha));
}

function formatearMoneda(total: number | null): string {
  if (typeof total !== "number") {
    return "A confirmar";
  }

  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 0,
  }).format(total);
}
