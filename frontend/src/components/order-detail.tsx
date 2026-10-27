//#region ENCABEZADO · src/components/order-detail.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/order-detail.tsx
 * ========================================================================
 * FUNCIÓN
 * Coordina la vista del pedido: resumen de trabajo, gestión, correcciones, producción y entrega,
 * según el contexto de cliente u operación. El cliente ve el seguimiento global sobre las secciones.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - defaultSection(order: Order, internal: boolean)
 * - WorkSummary({order,internal,onSaved}: {order:Order;internal:boolean;onSaved:()=>void})
 *   Componente de interfaz.
 * - Summary({order,internal}: {order:Order;internal:boolean})
 *   Componente de interfaz.
 * - [export] OrderDetail({id,internal=false,financial=true}:
 *   {id:string;internal?:boolean;financial?:boolean})
 *   Componente de interfaz.
 * - [async] OrderDetail :: load()
 * - OrderDetail :: read()
 * - OrderDetail :: saved()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - sections [const].
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useState} from "react";
import {quoteRead,money,date,modeLabels} from "@/lib/quote-types";
import {isOrder,orderState,type Order} from "@/lib/order-types";
import {ClientJourney} from "./client-journey";
import {ClientContextTour} from "./client-context-tour";
import {ClientOrderSummary} from "./client-order-summary";
import {OrderProgress} from "./order-progress";
import {orderTracking,orderTrackingLabel} from "@/lib/order-tracking";
import {PaymentPanel} from "./payment-panel";
import {OrderReschedule} from "./order-reschedule";
import {OrderDelivery} from "./order-delivery";
import {OrderProduction} from "./order-production";
import {OrderCorrections} from "./order-corrections";
import {OrderManagement} from "./order-management";
import {PrivateFiles} from "./private-files";
import "@/app/client.css";
import "@/app/client-workspace.css";
import "@/app/client-order-compact.css";

const sections=[
 {id:"pedido-resumen",label:"Resumen e historia"},
 {id:"pedido-revision",label:"Revisión",internal:true},
 {id:"pedido-correcciones",label:"Correcciones"},
 {id:"pedido-produccion",label:"Producción y calidad",internal:true},
 {id:"pedido-entrega",label:"Entrega y cierre"},
 {id:"pedido-reprogramacion",label:"Reprogramación"},
 {id:"pedido-pdf",label:"Archivos PDF"},
 {id:"pedido-pagos",label:"Pagos",financial:true},
];
function defaultSection(order:Order,internal:boolean){
 if(!internal)return "pedido-resumen";
 if(order.cobro&&!["PAGO_ACREDITADO","SIN_ANTICIPO"].includes(order.cobro.estado)&&order.estado==="PENDIENTE_REVISION")return "pedido-pagos";
 if(order.estado==="CORRECCION_SOLICITADA")return "pedido-correcciones";
 if(["LISTO_PARA_ENTREGA","ENTREGADO","CERRADO"].includes(order.estado))return "pedido-entrega";
 if(internal&&["APROBADO","EN_PRODUCCION"].includes(order.estado))return "pedido-produccion";
 return internal&&order.estado==="PENDIENTE_REVISION"?"pedido-revision":"pedido-resumen";
}
function WorkSummary({order,internal}:{order:Order;internal:boolean}){
 return <section className="client-card"><h3>Trabajo e importe</h3><p>Sucursal responsable: {order.oferta.sucursal.nombre}.</p>
  <div className="client-table-wrap"><table className="client-table"><thead><tr><th>PDF confirmado</th><th>Impresión</th><th>Cantidad</th><th>Subtotal</th></tr></thead><tbody>{order.oferta.items.map(item=><tr key={item.codigoPublico}><td>{item.documento.nombre}</td><td>{item.formato} · {item.papel}<br/>{item.trabajo.color==="COLOR"?"Color":"Blanco y negro"} · {item.trabajo.dobleFaz?"Doble faz":"Simple faz"}</td><td>{item.trabajo.paginas} páginas × {item.trabajo.copias} copias</td><td>{money(item.precio.subtotal)}</td></tr>)}</tbody></table></div>
  <p>Entrega: {money(order.oferta.costoEntrega)}</p><p className="client-total">{money(order.oferta.total)}</p>{!internal&&<a href={`/cliente/cotizaciones/${order.cotizacion}`}>Ver cotización y condiciones aceptadas</a>}
 </section>;
}
function Summary({order,internal,onSaved}:{order:Order;internal:boolean;onSaved:()=>void}){
 if(!internal)return <ClientOrderSummary order={order} actions={<OrderManagement order={order} internal={false} onSaved={onSaved}/>}/>;
 return <><div className="client-grid"><section className="client-card"><h3>Recorrido del pedido</h3><ol className="order-timeline">{order.historial.map((e,i)=><li key={i}><strong>{orderState(e.estado,order.reserva.modalidad)}</strong><span>{date(e.fecha)}</span><p>{e.motivo}</p></li>)}</ol></section>
  <section className="client-card"><h3>{order.estadoReserva==="LIBERADA"?"Reserva liberada":order.estadoReserva==="CUMPLIDA"?"Reserva cumplida":"Entrega reservada"}</h3><p><strong>{modeLabels[order.reserva.modalidad]}</strong></p><p>{order.reserva.nombre}</p><p>{order.reserva.fecha} · {order.reserva.apertura}–{order.reserva.cierre}<br/>{order.reserva.zonaHoraria}</p>{order.contacto&&<p>Recibe: {order.contacto.receptor}<br/>Contacto: {order.contacto.telefono}</p>}
   <p className="client-muted">{["LIBERADA","CUMPLIDA"].includes(order.estadoReserva)?"Esta franja quedó en el historial y ya no ocupa capacidad.":"Un cupo reservado para el pedido completo. La fecha reservada no indica que el trabajo ya esté listo."}</p><a href="#pedido-entrega">Consultar entrega y saldo</a></section></div><WorkSummary order={order} internal={internal}/></>;
}
export function OrderDetail({id,internal=false,financial=true,globalAdmin=false}:{id:string;internal?:boolean;financial?:boolean;globalAdmin?:boolean}){
 const[refresh,setRefresh]=useState(0),[requested,setRequested]=useState("");
 const[order,setOrder]=useState<Order|null>(null),[error,setError]=useState(""),[busy,setBusy]=useState(false);
 async function load(){setBusy(true);setError("");try{setOrder(await quoteRead(`/api/${internal?"operacion":"cliente"}/pedidos/${id}`,isOrder));}catch(e){setOrder(null);setError(e instanceof Error?e.message:"No pudimos consultar el pedido.");}finally{setBusy(false);}}
 useEffect(()=>{void load();},[id,internal]);
 useEffect(()=>{const read=()=>setRequested(window.location.hash.slice(1));read();window.addEventListener("hashchange",read);return()=>window.removeEventListener("hashchange",read);},[id]);
 const available=sections.filter(s=>(!s.internal||internal)&&(!s.financial||financial));
 const current=available.some(s=>s.id===requested)?requested:order?defaultSection(order,internal):"pedido-resumen";
 useEffect(()=>{if(!order||!requested)return;const frame=requestAnimationFrame(()=>document.getElementById("pedido-secciones")?.scrollIntoView({block:"start"}));return()=>cancelAnimationFrame(frame);},[current,requested,!!order]);
 const saved=()=>{setRefresh(v=>v+1);void load();};
 const controls=<div className={internal?"client-actions":"client-order-heading-actions"}><a className="client-button secondary" href={globalAdmin?"/administracion/pedidos":internal?order?`/operacion/sucursales/${order.oferta.sucursal.codigoPublico}`:"/operacion":"/cliente/pedidos"}>Volver a pedidos</a><button className="client-button secondary" disabled={busy} onClick={()=>void load()}>Actualizar pedido</button></div>;
 const tracking=order?orderTracking(order):null;
 const content=<>{!internal&&<ClientJourney part={3}/>} {busy&&<p role="status">Consultando pedido…</p>}{error&&<p role="alert" className="client-warning">{error}</p>}
  {(internal||!order)&&controls}
  {order&&<><header className="order-overview order-detail-heading">
   {internal?<><div><h2>Pedido PED-{order.numero}</h2><p>{order.oferta.sucursal.nombre} · Confirmado el {date(order.confirmadaEn)}</p></div><span className="client-state">{orderTrackingLabel(order)}</span></>:<><div className="client-order-heading-state"><div><h2>Tu pedido PED-{order.numero}</h2><p>Confirmado el {date(order.confirmadaEn)}</p></div><span className="client-state">{orderTrackingLabel(order)}</span></div>{controls}</>}
  </header>
   {!internal&&<><details id="client-order-progress" className="client-order-tracking"><summary>Ver recorrido completo <span>Etapas y fechas registradas de tu pedido</span></summary><OrderProgress order={order}/></details>
    {tracking?.notice&&order.estado!=="CORRECCION_SOLICITADA"&&<p className={tracking.unavailable?"client-warning":"client-note"}>{tracking.notice}</p>}
    {order.cobro&&<p className="client-note"><strong>Situación del pedido:</strong> {order.cobro.mensaje} <a href="#pedido-pagos">Consultar pagos y próximos pasos</a></p>}
    {order.estado==="CORRECCION_SOLICITADA"&&<p className="client-note"><strong>{tracking?.notice??"Tu pedido requiere una corrección."}</strong> <a href="#pedido-correcciones">Consultar lo solicitado y responder</a>.</p>}
   </>}
   <div id="client-order-details"><nav id="pedido-secciones" className="order-sections" aria-label="Secciones del pedido">{available.map(s=><a key={s.id} href={`#${s.id}`} aria-current={current===s.id?"page":undefined} data-attention={!internal&&s.id==="pedido-correcciones"&&order.estado==="CORRECCION_SOLICITADA"?true:undefined}>{!internal&&s.id==="pedido-resumen"?"Resumen":s.label}</a>)}</nav></div>
   {/* Keep each form mounted so switching sections preserves drafts and recoverable operations. */}
   <div id="pedido-resumen" className="order-section" hidden={current!=="pedido-resumen"}><Summary order={order} internal={internal} onSaved={saved}/></div>
   {internal&&<div id="pedido-revision" className="order-section" hidden={current!=="pedido-revision"}><WorkSummary order={order} internal/><OrderManagement order={order} internal onSaved={saved}/></div>}
   <div className="order-section" hidden={current!=="pedido-correcciones"}><OrderCorrections order={order} internal={internal} financial={financial} onSaved={saved}/></div>
   {internal&&<div className="order-section" hidden={current!=="pedido-produccion"}><OrderProduction order={order} onSaved={saved}/></div>}
   <div className="order-section" hidden={current!=="pedido-entrega"}><OrderDelivery order={order} internal={internal} financial={financial} onSaved={saved}/><p className="client-note"><a href="#pedido-reprogramacion">{internal?"Consultar o proponer una reprogramación de la entrega":"Ver propuestas de nueva fecha"}</a>. La fecha sólo cambia con aceptación del cliente.</p></div>
   <div className="order-section" hidden={current!=="pedido-reprogramacion"}><OrderReschedule order={order} internal={internal} onSaved={saved}/></div>
   <div id="pedido-pdf" className="order-section" hidden={current!=="pedido-pdf"}>{internal?<PrivateFiles quote={order.cotizacion} version={0} internal confirmed snapshot={order.archivos.map(f=>f.archivo)}/>:<div className="workspace-review-body client-order-file-panel"><PrivateFiles compact quote={order.cotizacion} version={0} confirmed snapshot={order.archivos.map(f=>f.archivo)}/></div>}</div>
   {financial&&<div id="pedido-pagos" className="order-section" hidden={current!=="pedido-pagos"}><PaymentPanel key={refresh} quote={order.cotizacion} internal={internal}/></div>}
   {!internal&&<ClientContextTour stage={3} ready={!busy}/>}
   <details className="order-pilot-limits"><summary>Funciones del piloto <span className="client-construction">En construcción</span></summary><p>CUPS, aplicación móvil, licencias, pasarelas de pago y reclamos siguen en construcción.</p></details>
  </>}
 </>;
 return internal?content:<div className="client-experience client-compact-experience client-order-experience">{content}</div>;
}
