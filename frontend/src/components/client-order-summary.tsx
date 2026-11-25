"use client";
import {useEffect,useState,type ReactNode} from "react";
import {isPaymentView,type PaymentView} from "@/lib/payment-types";
import {quoteRead} from "@/lib/quote-types";
//#region ENCABEZADO · src/components/client-order-summary.tsx
/* FUNCIÓN: Presenta el pedido confirmado con las tarjetas del mock MC-CLI-006.
 * FUNCIONES: ClientOrderSummary. TIPO: propiedades inline con Order.
 * Usa la oferta y los archivos del pedido; no recalcula precios ni acredita pagos.
 */
//#endregion
import {date,money,modeLabels} from "@/lib/quote-types";
import {orderState,type Order} from "@/lib/order-types";

export function ClientOrderSummary({order,actions}:{order:Order;actions?:ReactNode}){
 const o=order.oferta,slot=order.reserva;
 const [payment,setPayment]=useState<PaymentView|null>(null),[paymentError,setPaymentError]=useState(false);
 useEffect(()=>{setPayment(null);setPaymentError(false);let active=true;void quoteRead(`/api/cliente/cotizaciones/${order.cotizacion}/pagos`,isPaymentView).then(p=>{if(active)setPayment(p);}).catch(()=>{if(active)setPaymentError(true);});return()=>{active=false;};},[order.cotizacion,order.version]);
 return <div className="client-order-summary-grid" aria-label="Detalle de tu pedido">
  <div className="client-order-work-column">
   <section className="workspace-panel client-order-work">
    <header className="workspace-budget-header"><div><h3>Trabajo y archivos</h3><p>{o.items.length} {o.items.length===1?"trabajo":"trabajos"} · {order.archivos.length} {order.archivos.length===1?"PDF confirmado":"PDF confirmados"}</p></div><a href="#pedido-pdf">Ver archivos</a></header>
    <div className="client-order-work-items" aria-label="Trabajos confirmados" tabIndex={o.items.length>1?0:undefined}>{o.items.map((item,index)=>{const file=order.archivos.find(f=>f.item===item.codigoPublico);return <article key={item.codigoPublico}>
     <h4>PDF {index+1} · {item.documento.nombre}</h4>
     <p>{item.trabajo.paginas} páginas · {item.trabajo.copias} {item.trabajo.copias===1?"copia":"copias"} · {item.trabajo.dobleFaz?"Doble faz":"Simple faz"}</p>
     <p>{item.formato} · {item.papel}<br/>{item.trabajo.color==="COLOR"?"Color":"Blanco y negro"}</p>
     <strong>{money(item.precio.subtotal)}</strong>
     <details><summary>Ver adicionales y archivo</summary>{item.precio.lineas.filter(l=>item.trabajo.terminaciones.includes(l.servicio)).map(l=><p key={l.servicio}>{l.nombre}</p>)}{item.trabajo.terminaciones.length===0&&<p>Sin servicios adicionales.</p>}{file&&<p>{file.nombre}<br/>{file.paginas} páginas · {(file.bytes/1024).toLocaleString("es-AR",{maximumFractionDigits:1})} KiB · Versión aceptada</p>}<p>El acceso al contenido depende de su disponibilidad y conservación.</p><a href="#pedido-pdf">Consultar archivo confirmado</a></details>
    </article>;})}</div>
    <footer className="client-order-work-footer">{o.items.length>2&&<span>Desplazate dentro del listado para ver todos los trabajos.</span>}<a href={`/cliente/cotizaciones/${order.cotizacion}`}>Ver cotización y condiciones aceptadas</a></footer>
   </section>
  </div>
  <aside className="client-order-sidebar" aria-label="Importes y destino del pedido">
   <section className="workspace-panel client-order-money"><h3>Importe del pedido</h3><strong className="client-order-confirmed-total">{money(o.total)}</strong><p>Precio confirmado · {o.medioPago==="EFECTIVO"?"Efectivo":"Transferencia"}</p>
    {payment?<><dl><div><dt>Acreditado y aplicado</dt><dd>{money(payment.aplicado)}</dd></div><div><dt>Saldo pendiente</dt><dd>{money(payment.pendiente)}</dd></div></dl>{payment.intentos.some(i=>i.estado==="INFORMADO")&&<p className="client-note">Transferencia informada, pendiente de verificación. Todavía no acredita dinero.</p>}</>:<p role={paymentError?"alert":"status"}>{paymentError?"No pudimos consultar el saldo. Revisá el panel de pagos.":"Consultando importes acreditados y saldo…"}</p>}
    <a href="#pedido-pagos">Ver pagos y saldo →</a>
   </section>
   <section className="workspace-panel client-order-destination"><h3>Destino de entrega</h3><p><strong>{modeLabels[slot.modalidad]}</strong></p><p>{slot.nombre}</p><div className="client-order-slot"><span>{order.estadoReserva==="LIBERADA"?"Última fecha reservada":order.estadoReserva==="CUMPLIDA"?"Entrega realizada · franja reservada":"Entrega reservada"}</span><strong>{slot.fecha} · {slot.apertura}–{slot.cierre}</strong><small>{slot.zonaHoraria}</small></div>
    {order.contacto&&<details><summary>Contacto de recepción</summary><p>Recibe: {order.contacto.receptor}<br/>Contacto: {order.contacto.telefono}</p></details>}
    <p className="client-muted">La fecha reservada no indica que el trabajo ya esté listo. El seguimiento muestra su disponibilidad real.</p><a href="#pedido-entrega">Consultar entrega →</a>
   </section>
  </aside>
  <div className="client-order-secondary">
   <details className="workspace-panel client-order-history"><summary>Historial del pedido <span>{order.historial.length} {order.historial.length===1?"evento":"eventos"}</span></summary><ol className="order-timeline">{order.historial.map((event,index)=><li key={index}><strong>{orderState(event.estado,slot.modalidad)}</strong><span>{date(event.fecha)}</span><p>{event.motivo}</p></li>)}</ol></details>
   {actions&&<details className="workspace-panel client-order-actions"><summary>Opciones de tu pedido <span>Gestión y cancelación según su estado</span></summary>{actions}</details>}
  </div>
 </div>;
}
