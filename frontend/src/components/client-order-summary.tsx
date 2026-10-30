"use client";
import {useCallback,useEffect,useId,useState,type ReactNode} from "react";
import {isPaymentView,type PaymentView} from "@/lib/payment-types";
import {isFileView,type FileView} from "@/lib/file-types";
import {date,money,modeLabels,quoteRead} from "@/lib/quote-types";
import {orderState,type Order} from "@/lib/order-types";
import {ClientTransferDialog} from "./client-transfer-dialog";
import {PaymentPanel} from "./payment-panel";
import {OrderDelivery} from "./order-delivery";
import {OrderReschedule} from "./order-reschedule";
import {OrderCorrections} from "./order-corrections";
import {ClientOrderFilePreview} from "./client-order-file-preview";

function CardTitle({kind,children}:{kind:"work"|"file"|"info"|"payment"|"destination";children:ReactNode}){
 const paths={work:"M9 5H5v16h14V5h-4M9 3h6v4H9zM8 12h8M8 16h6",file:"M14 2H5v20h14V7zM14 2v6h5M8 13h8M8 17h6",info:"M12 8v5l3 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0",payment:"M2 5h20v14H2zM2 10h20M6 15h4",destination:"M19 10c0 5-7 12-7 12S5 15 5 10a7 7 0 1 1 14 0M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0"};
 return <h3 className="client-order-card-title"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[kind]}/></svg>{children}</h3>;
}

/** MC-CLI-006: information stays in context; only exceptional tasks expand their forms. */
export function ClientOrderSummary({order,actions,onSaved}:{order:Order;actions?:(paymentAction:(disabled:boolean)=>ReactNode)=>ReactNode;onSaved:()=>void}){
 const o=order.oferta,slot=order.reserva,selector=useId();
 const [selected,setSelected]=useState(""),[revision,setRevision]=useState(0);
 const [payment,setPayment]=useState<PaymentView|null>(null),[files,setFiles]=useState<FileView|null>(null),[paymentError,setPaymentError]=useState(false),[fileError,setFileError]=useState(false);
 const [transferData,setTransferData]=useState<PaymentView|null>(null),[paymentMessage,setPaymentMessage]=useState(""),[reportRevision,setReportRevision]=useState(0);
 const paymentChanged=useCallback(()=>setRevision(v=>v+1),[]);
 useEffect(()=>{
  let active=true;setPayment(null);setFiles(null);setPaymentError(false);setFileError(false);
  void Promise.allSettled([quoteRead(`/api/cliente/cotizaciones/${order.cotizacion}/pagos`,isPaymentView),quoteRead(`/api/cliente/cotizaciones/${order.cotizacion}/archivos`,isFileView)]).then(([p,f])=>{
   if(!active)return;
   if(p.status==="fulfilled")setPayment(p.value);else setPaymentError(true);
   if(f.status==="fulfilled")setFiles(f.value);else setFileError(true);
  });
  return()=>{active=false;};
 },[order.cotizacion,order.version,revision]);
 useEffect(()=>{
  function reveal(){
   const hash=window.location.hash.slice(1);if(!hash)return;
   const target=document.getElementById(hash);if(!target)return;
   let parent:HTMLElement|null=target;
   while(parent){if(parent instanceof HTMLDetailsElement)parent.open=true;parent=parent.parentElement;}
   requestAnimationFrame(()=>target.scrollIntoView({block:"start"}));
  }
  reveal();window.addEventListener("hashchange",reveal);return()=>window.removeEventListener("hashchange",reveal);
 },[order.codigoPublico,order.version]);
 const item=o.items.find(i=>i.codigoPublico===selected)??o.items[0];
 const file=item?order.archivos.find(f=>f.item===item.codigoPublico):undefined;
 const metadata=file?files?.items.flatMap(i=>i.archivos).find(f=>f.codigoPublico===file.archivo):undefined;
 const available=!!metadata&&!metadata.revocado&&["VALIDO","REQUIERE_COTIZACION"].includes(metadata.estado);
 const dateLabel=new Intl.DateTimeFormat("es-AR",{dateStyle:"medium",timeZone:slot.zonaHoraria}).format(new Date(slot.desde));
 const pendingPayment=order.cobro&&!["PAGO_ACREDITADO","SIN_ANTICIPO"].includes(order.cobro.estado);
 return <div id="client-order-details" className="client-order-content">
  <OrderCorrections order={order} internal={false} financial onSaved={onSaved} contextual/>
  <div className="client-order-summary-grid" aria-label="Detalle de tu pedido">
   <section className="workspace-panel client-order-card client-order-work">
    <CardTitle kind="work">Detalles del trabajo</CardTitle>
    {o.items.length>1&&<label className="client-order-file-selector" htmlFor={selector}>Archivo del trabajo<select id={selector} value={item?.codigoPublico??""} onChange={e=>setSelected(e.target.value)}>{o.items.map((i,index)=><option key={i.codigoPublico} value={i.codigoPublico}>{index+1}. {i.documento.nombre}</option>)}</select><small>{o.items.length} archivos en este pedido</small></label>}
    {item&&<><dl className="client-order-specs"><div><dt>Número de páginas</dt><dd>{item.trabajo.paginas}</dd></div><div><dt>Cantidad de copias</dt><dd>{item.trabajo.copias}</dd></div><div><dt>Tamaño de hoja</dt><dd>{item.formato}</dd></div><div><dt>Papel</dt><dd>{item.papel}</dd></div></dl><ul className="client-order-finishes"><li>{item.trabajo.color==="COLOR"?"Color":"Blanco y negro"}</li><li>{item.trabajo.dobleFaz?"Doble faz":"Simple faz"}</li>{item.precio.lineas.filter(l=>item.trabajo.terminaciones.includes(l.servicio)).map(l=><li key={l.servicio}>{l.nombre}</li>)}</ul>{o.items.length>1&&<p className="client-muted">Subtotal de este archivo: {money(item.precio.subtotal)}</p>}</>}
   </section>
   <section id="pedido-pdf" className="workspace-panel client-order-card client-order-file">
    <CardTitle kind="file">{order.archivos.length===1?"Archivo":"Archivo seleccionado"}</CardTitle>
    {file?<><p>Nombre del archivo:<strong className="client-order-file-name">{file.nombre}</strong></p><p>Tamaño del archivo:<strong className="client-order-file-size">{(file.bytes/(1024*1024)).toLocaleString("es-AR",{maximumFractionDigits:2})} MB</strong></p>
     {available?<div className="client-order-file-actions"><a className="client-button secondary" href={`/api/cliente/cotizaciones/${order.cotizacion}/archivos/${file.archivo}/original`} download={file.nombre}>Descargar archivo</a><ClientOrderFilePreview quote={order.cotizacion} version={order.version} file={file.archivo}/></div>:<p role={fileError?"alert":"status"} className="client-muted">{fileError?"No pudimos consultar la disponibilidad del archivo.":!files?"Consultando el archivo…":"El contenido ya no está disponible para consultar o descargar."}</p>}
     {fileError&&<button type="button" className="client-text-action" onClick={()=>setRevision(v=>v+1)}>Volver a consultar el archivo</button>}
    </>:<p className="client-muted">No encontramos el PDF vinculado a este trabajo.</p>}
   </section>
   <section className="workspace-panel client-order-card client-order-information">
    <CardTitle kind="info">Información del pedido</CardTitle>
    <div className="client-order-information-row"><span>{slot&&order.estadoReserva==="LIBERADA"?"Última fecha reservada":order.estadoReserva==="CUMPLIDA"?"Franja de la entrega realizada":"Entrega estimada"}</span><strong>{dateLabel}</strong><span>{slot.apertura}–{slot.cierre}</span><small>{["LIBERADA","CUMPLIDA"].includes(order.estadoReserva)?"Esta fecha queda en el historial del pedido.":"La franja es estimada. El seguimiento indica cuándo está listo."}</small></div>
    <div className="client-order-information-row"><span>Precio total</span><strong className="client-order-confirmed-total">{money(o.total)}</strong><small>Incluye impresión, servicios y entrega.</small></div>
    <OrderDelivery order={order} internal={false} financial onSaved={onSaved} compact/>
   </section>
   <section className="workspace-panel client-order-card client-order-pay">
    <CardTitle kind="payment">Método de pago</CardTitle>
    <p className="client-order-method">{o.medioPago==="EFECTIVO"?"Efectivo":"Transferencia"}</p>
    {payment?<><dl className="client-order-balances"><div><dt>Acreditado y aplicado</dt><dd>{money(payment.aplicado)}</dd></div><div><dt>Saldo pendiente</dt><dd>{money(payment.pendiente)}</dd></div></dl>{payment.intentos.some(i=>i.estado==="PENDIENTE")&&<p className="client-note">Transferencia informada. La imprenta está verificando el pago.</p>}</>:<p role={paymentError?"alert":"status"}>{paymentError?"No pudimos consultar el saldo. Podés volver a consultarlo en los pagos.":"Consultando el saldo…"}</p>}
    {pendingPayment&&!payment?.intentos.some(i=>i.estado==="PENDIENTE")&&<p className="client-order-payment-notice">{order.cobro!.mensaje}</p>}
    <details id="pedido-pagos" className="client-order-context-details"><summary>Ver pagos y comprobantes</summary><PaymentPanel quote={order.cotizacion} compact title="Pagos de tu pedido" allowReport={false} refresh={reportRevision} onChange={paymentChanged}/></details>
   </section>
   <section id="pedido-destino" className="workspace-panel client-order-card client-order-destination">
    <CardTitle kind="destination">{slot.modalidad==="ENVIO_DOMICILIO"?"Dirección de entrega":slot.modalidad==="RETIRO_PUNTO_ENTREGA"?"Punto de entrega":"Lugar de retiro"}</CardTitle>
    <p className="client-order-destination-name">{slot.nombre}</p><p className="client-muted">{modeLabels[slot.modalidad]}</p>
    {order.contacto&&<p>Recibe: {order.contacto.receptor}<br/>Teléfono: {order.contacto.telefono}</p>}
    <OrderReschedule order={order} internal={false} onSaved={onSaved} contextual/>
   </section>
  </div>
  {actions?actions(disabled=>payment?.puedeInformar?<button type="button" disabled={disabled} className="client-button payment-confirm" onClick={()=>{setPaymentMessage("");setTransferData(payment);}}>Informar transferencia</button>:null):payment?.puedeInformar&&<div className="client-order-action-buttons"><button type="button" className="client-button payment-confirm" onClick={()=>setTransferData(payment)}>Informar transferencia</button></div>}
  {paymentMessage&&<p role="status" className="client-success">{paymentMessage}</p>}
  {transferData&&<ClientTransferDialog data={transferData} onClose={()=>setTransferData(null)} onSaved={(view,message)=>{setPayment(view);setTransferData(null);setPaymentMessage(message);setReportRevision(v=>v+1);onSaved();}}/>}
  <details className="workspace-panel client-order-history"><summary>Historial y condiciones del pedido</summary><a href={`/cliente/cotizaciones/${order.cotizacion}`}>Ver cotización y condiciones</a><ol className="order-timeline">{order.historial.map((event,index)=><li key={index}><strong>{orderState(event.estado,slot.modalidad)}</strong><span>{date(event.fecha)}</span><p>{event.motivo}</p></li>)}</ol></details>
 </div>;
}
