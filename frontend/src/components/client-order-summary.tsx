"use client";
import {useEffect,useState} from "react";
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

export function ClientOrderSummary({order}:{order:Order}){
 const o=order.oferta,slot=order.reserva;
 const [payment,setPayment]=useState<PaymentView|null>(null),[paymentError,setPaymentError]=useState(false);
 useEffect(()=>{setPayment(null);setPaymentError(false);let active=true;void quoteRead(`/api/cliente/cotizaciones/${order.cotizacion}/pagos`,isPaymentView).then(p=>{if(active)setPayment(p);}).catch(()=>{if(active)setPaymentError(true);});return()=>{active=false;};},[order.cotizacion,order.version]);
 return <><section className="client-card client-order-summary" aria-label="Detalle de tu pedido">
  <div className="quote-original-tiles"><section><h3>Detalles del trabajo</h3>{o.items.map((item,index)=><article key={item.codigoPublico}><h4>Trabajo {index+1}</h4><p>{item.trabajo.paginas} páginas · {item.trabajo.copias} {item.trabajo.copias===1?"copia":"copias"}<br/>{item.formato} · {item.papel}<br/>{item.trabajo.color==="COLOR"?"Color":"Blanco y negro"} · {item.trabajo.dobleFaz?"Doble faz":"Simple faz"}</p>{item.precio.lineas.filter(l=>item.trabajo.terminaciones.includes(l.servicio)).map(l=><p key={l.servicio}>{l.nombre}</p>)}</article>)}</section>
   <section><h3>Archivos del pedido</h3>{order.archivos.map(file=><article key={file.archivo}><p className="quote-file-name">{file.nombre}</p><p>{file.paginas} páginas · {(file.bytes/1024).toLocaleString("es-AR",{maximumFractionDigits:1})} KiB · Vista previa aceptada</p><p>Acceso privado sujeto a disponibilidad y conservación.</p><a href="#pedido-pdf">Consultar disponibilidad del archivo</a></article>)}<p><a href="#pedido-pdf">Ver las páginas de tus archivos</a></p></section>
   <section><h3>Información del pedido</h3><p>Precio confirmado</p><p className="client-total">{money(o.total)}</p><p>{order.estadoReserva==="LIBERADA"?"Última fecha reservada":order.estadoReserva==="CUMPLIDA"?"Franja de la entrega realizada":"Entrega reservada"}<br/><strong>{slot.fecha}</strong><br/>{slot.apertura}–{slot.cierre} · {slot.zonaHoraria}</p><p className="client-muted">La fecha reservada no indica que el trabajo ya esté listo. Consultá el estado en el seguimiento.</p><a href={`/cliente/cotizaciones/${order.cotizacion}`}>Ver las condiciones aceptadas</a></section></div>
  <div className="quote-original-tiles quote-original-bottom"><section><h3>Pago del pedido</h3><p>Medio elegido: <strong>{o.medioPago==="EFECTIVO"?"Efectivo":"Transferencia"}</strong></p>{payment?<><p>Acreditado y aplicado: <strong>{money(payment.aplicado)}</strong><br/>Saldo pendiente: <strong>{money(payment.pendiente)}</strong></p>{payment.intentos.some(i=>i.estado==="INFORMADO")&&<p>Transferencia informada, pendiente de verificación. Todavía no acredita dinero.</p>}</>:<p>{paymentError?"No pudimos consultar el saldo. Revisá el panel de pagos.":"Consultando importes acreditados y saldo…"}</p>}{order.cobro&&<p>{order.cobro.mensaje}</p>}<a href="#pedido-pagos">Ver pagos y saldo</a></section><section><h3>Destino de entrega</h3><p><strong>{modeLabels[slot.modalidad]}</strong></p><p>{slot.nombre}</p>{order.contacto&&<p>Recibe: {order.contacto.receptor}<br/>Contacto: {order.contacto.telefono}</p>}<a href="#pedido-entrega">Consultar entrega</a></section></div>
 </section><details className="client-card client-order-history"><summary>Ver el historial del pedido</summary><ol className="order-timeline">{order.historial.map((event,index)=><li key={index}><strong>{orderState(event.estado,slot.modalidad)}</strong><span>{date(event.fecha)}</span><p>{event.motivo}</p></li>)}</ol></details></>;
}
