//#region ENCABEZADO · src/components/order-progress.tsx
/*
 * FUNCIÓN: Presenta el seguimiento público del pedido en inicio y detalle.
 * FUNCIONES: OrderProgress.
 * TIPOS: propiedades inline con Order y variante visual.
 * VALORES: stepStates.
 */
//#endregion

import type {Order} from "@/lib/order-types";
import {orderTracking} from "@/lib/order-tracking";
import {date} from "@/lib/quote-types";

const stepStates={complete:"Etapa superada",current:"Estado actual",pending:"Pendiente",recorded:"Registrado",stopped:"Recorrido detenido"};

export function OrderProgress({order,compact=false}:{order:Order;compact?:boolean}){
  const tracking=orderTracking(order);
  return <section className={`order-progress${compact?" order-progress-compact":""}`} aria-label={`Seguimiento del pedido PED-${order.numero}`}>
    <div className="order-progress-heading"><h3>Seguimiento de tu pedido</h3><p>Estado actual: <strong>{tracking.label}</strong></p></div>
    {tracking.notice&&<p className={tracking.unavailable?"client-warning":"order-progress-note"}>{tracking.notice}</p>}
    {tracking.steps.length>0&&<ol className="order-progress-steps">{tracking.steps.map((step,index)=><li key={step.id} data-state={step.state} aria-current={step.state==="current"||step.state==="stopped"?"step":undefined}>
      <span className="order-progress-marker" aria-hidden="true">{step.state==="complete"?"✓":step.state==="stopped"?"!":index+1}</span>
      <div><strong>{step.label}</strong><span className="order-progress-status">{stepStates[step.state]}</span>{step.date&&<time dateTime={step.date}>{date(step.date)}</time>}</div>
    </li>)}</ol>}
    <p className="order-progress-footnote">Recorrido según las condiciones de este pedido · configuración v{order.numeroConfiguracion}. Las próximas etapas no tienen una fecha confirmada.</p>
  </section>;
}
