import type {Order,OrderSummary} from "@/lib/order-types";
import {clientOrderTone,orderTrackingLabel} from "@/lib/order-tracking";
import "@/app/client-order-status.css";

/** Shared customer status for the dashboard, history and order detail. */
export function ClientOrderBadge({order}:{order:Order|OrderSummary}){
  const state="reserva" in order?order:{...order,reserva:{modalidad:order.modalidad}};
  return <span className="client-state client-order-state" data-tone={clientOrderTone(state)}>{orderTrackingLabel(state)}</span>;
}
