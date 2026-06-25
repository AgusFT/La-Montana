"use client";

import { MapPin } from "lucide-react";
import { useEffect, useState } from "react";

import { CreateOrderForm } from "../../types/create-order";
import {
  obtenerMensajeErrorPedido,
  obtenerPuntosEntregaPedido,
  PuntoEntregaPedido,
} from "../../services/pedido-api";

interface DeliveryPointCardProps {
  form: CreateOrderForm;
  onChange: (
    field: keyof CreateOrderForm,
    value: unknown,
  ) => void;
}

export function DeliveryPointCard({
  form,
  onChange,
}: DeliveryPointCardProps) {
  const [puntosEntrega, setPuntosEntrega] = useState<PuntoEntregaPedido[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;

    async function cargarPuntosEntrega() {
      await Promise.resolve();

      try {
        const puntos = await obtenerPuntosEntregaPedido();

        if (isMounted) {
          setPuntosEntrega(puntos);
          setError("");
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

    void cargarPuntosEntrega();

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    if (isLoading || puntosEntrega.length === 0) {
      return;
    }

    const existePuntoSeleccionado = puntosEntrega.some((point) =>
      point.id_punto_entrega === form.deliveryPointId
    );

    if (!existePuntoSeleccionado) {
      onChange("deliveryPointId", puntosEntrega[0].id_punto_entrega);
    }
  }, [form.deliveryPointId, isLoading, onChange, puntosEntrega]);

  const selectedPoint = puntosEntrega.find((point) =>
    point.id_punto_entrega === form.deliveryPointId
  );

  return (
    <section className="summary-card panel-card">
      <label className="form-label" htmlFor="deliveryPoint">
        <div className="summary-card-title">
          <MapPin size={20} />
          <h3>Punto de entrega</h3>
        </div>
      </label>

      <div className="delivery-point-content">
        <strong className="delivery-point-name">
          {selectedPoint?.nombre ?? "Seleccioná un punto"}
        </strong>

        <span className="delivery-point-address">
          {selectedPoint?.direccion_texto ??
            (isLoading
              ? "Cargando puntos de entrega..."
              : "Sin puntos activos")}
        </span>

        {error ? <p className="form-error">{error}</p> : null}

        <div className="form-group">
          <label
            htmlFor="deliveryPoint"
            className="form-label"
          >
            Punto seleccionado
          </label>

          <select
            id="deliveryPoint"
            className="form-select"
            value={form.deliveryPointId ?? ""}
            disabled={isLoading || puntosEntrega.length === 0}
            onChange={(e) => {
              const value = e.target.value ? Number(e.target.value) : null;

              onChange(
                "deliveryPointId",
                value !== null && Number.isFinite(value) ? value : null,
              );
            }}
          >
            <option value="">
              {isLoading ? "Cargando..." : "Elegí un punto"}
            </option>

            {puntosEntrega.map((point) => (
              <option
                key={point.id_punto_entrega}
                value={point.id_punto_entrega}
              >
                {point.nombre}
              </option>
            ))}
          </select>
        </div>
      </div>
    </section>
  );
}
