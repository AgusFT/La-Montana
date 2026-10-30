//#region ENCABEZADO · src/components/order-progress.tsx
/*
 * FUNCIÓN: Presenta el seguimiento público del pedido en inicio y detalle.
 * FUNCIONES: OrderProgress.
 * TIPOS: propiedades inline con Order y variante visual.
 * VALORES: stepStates.
 */
//#endregion

"use client";
import {useEffect,useRef} from "react";
import type {Order} from "@/lib/order-types";
import {orderTracking,orderTrackingSummary} from "@/lib/order-tracking";
import {date} from "@/lib/quote-types";

const stepStates={complete:"Etapa superada",current:"Estado actual",pending:"Pendiente",recorded:"Registrado",stopped:"Recorrido detenido"};

export function OrderProgress({order,compact=false,featured=false}:{order:Order;compact?:boolean;featured?:boolean}){
  const tracking=orderTracking(order),summary=orderTrackingSummary(order,tracking.steps),list=useRef<HTMLOListElement>(null);
  useEffect(()=>{const container=list.current,current=container?.querySelector<HTMLElement>('[aria-current="step"]');if(container&&current&&container.scrollWidth>container.clientWidth&&(featured||window.matchMedia("(min-width:601px)").matches))container.scrollLeft=Math.max(0,current.offsetLeft-container.offsetLeft-container.clientWidth/2+current.clientWidth/2);},[order.estado,order.estadoLogistico,featured]);
  return <section className={`order-progress${compact?" order-progress-compact":""}${featured?" order-progress-featured":""}`} aria-label={`Seguimiento del pedido PED-${order.numero}`}>
    <div className="order-progress-heading"><h3>Seguimiento de tu pedido</h3><p>Estado actual: <strong>{tracking.label}</strong></p></div>
    {tracking.notice&&<p className={tracking.unavailable?"client-warning":"order-progress-note"}>{tracking.notice}</p>}
    {summary.length>0&&<ol ref={list} className="order-progress-steps order-progress-summary" aria-label="Etapas principales del pedido">{summary.map((step,index)=><li key={step.id} data-state={step.state} aria-current={step.state==="current"||step.state==="stopped"?"step":undefined}>
      <span className="order-progress-marker" aria-hidden="true">{step.state==="complete"?"✓":step.state==="stopped"?"!":index+1}</span>
      <div><strong>{step.label}</strong>{(!featured||step.state==="current"||step.state==="stopped")&&<span className="order-progress-status">{stepStates[step.state]}</span>}{step.date&&<time dateTime={step.date}>{date(step.date)}</time>}</div>
    </li>)}</ol>}
    {tracking.steps.length>summary.length&&<details className="order-progress-details"><summary>Ver recorrido completo ({tracking.steps.length} etapas)</summary><ol className="order-progress-detail-list">{tracking.steps.map(step=><li key={step.id} data-state={step.state}><span aria-hidden="true">{step.state==="complete"?"✓":step.state==="current"?"●":"○"}</span><div><strong>{step.label}</strong><small>{stepStates[step.state]}{step.date?` · ${date(step.date)}`:""}</small></div></li>)}</ol></details>}
    <p className="order-progress-footnote">{featured?"Las etapas se actualizan con el avance registrado por la imprenta.":<>Recorrido según las condiciones de este pedido · configuración v{order.numeroConfiguracion}. Las próximas etapas no tienen una fecha confirmada.</>}</p>
  </section>;
}
