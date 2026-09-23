//#region ENCABEZADO · src/components/quote-detail.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/quote-detail.tsx
 * ========================================================================
 * FUNCIÓN
 * Consulta el detalle y vigencia de una cotización y presenta su aceptación, cancelación y
 * continuación hacia archivos, pagos y confirmación del pedido.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] QuoteDetail({id}: {id:string})
 *   Componente de interfaz.
 * - [async] QuoteDetail :: load()
 * - [async] QuoteDetail :: mutate(action?: "aceptar"|"cancelar")
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useRef,useState} from "react";
import {quoteRead,isQuote,date,money,modeLabels,type Quote} from "@/lib/quote-types";
import {secureMutation,MutationError} from "@/lib/secure-mutation";
import {PaymentPanel} from "./payment-panel";
import {OrderConfirmation} from "./order-confirmation";
import {PrivateFiles} from "./private-files";
export function QuoteDetail({id}:{id:string}){
 const[q,setQuote]=useState<Quote|null>(null),[loading,setLoading]=useState(true),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState(""),[ack,setAck]=useState(false),[reason,setReason]=useState(""),[now,setNow]=useState(()=>Date.now());
 const [financialRefresh,setFinancialRefresh]=useState(0);
 const sending=useRef(false);const command=useRef<{path:string;body:string}|null>(null);const locked=loading||busy||uncertain;
 async function load(){setLoading(true);setError("");try{setQuote(await quoteRead(`/api/cliente/cotizaciones/${id}`,isQuote));setAck(false);}catch(e){setError(e instanceof Error?e.message:"No pudimos consultar.");}finally{setLoading(false);}}
 useEffect(()=>{void load();const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer);},[id]);
 async function mutate(action?:"aceptar"|"cancelar"){
  if(sending.current||busy||action&&locked)return;if(action&&q){if(action==="aceptar"&&!ack||action==="cancelar"&&!reason.trim())return;command.current={path:`/api/cliente/cotizaciones/${id}/${action}`,body:JSON.stringify({operacion:crypto.randomUUID(),version:q.version,motivo:action==="cancelar"?reason.trim():null})};}
  const c=command.current;if(!c)return;sending.current=true;setBusy(true);setError("");try{const r=await secureMutation(c.path,c.body,"application/json"),value:unknown=await r.json();if(!isQuote(value)||value.codigoPublico!==id)throw new Error("No pudimos verificar el resultado guardado.");setQuote(value);setAck(false);setUncertain(false);command.current=null;}
  catch(e){setError(e instanceof Error?e.message:"No pudimos completar la operación.");if(e instanceof MutationError&&!e.uncertain){command.current=null;setUncertain(false);}else setUncertain(true);}finally{sending.current=false;setBusy(false);}
 }
 const expired=!!q&&now>=Date.parse(q.vigenteHasta),live=q?.estado==="VIGENTE"&&!expired,o=q?.oferta;
 return <>{loading&&<p role="status">Consultando cotización…</p>}{q&&o&&<><div className="client-actions"><a className="client-button secondary" href={locked?undefined:"/cliente/cotizaciones"}>Volver a mis cotizaciones</a><button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar estado</button></div><p className="client-note">COT-{q.numero} · {q.estado==="VIGENTE"&&expired?"EXPIRADA":q.estado} · Vigente hasta {date(q.vigenteHasta)}. El precio permanece guardado durante su vigencia; la disponibilidad se vuelve a comprobar al confirmar el pedido.</p>
 {q.correccion&&<p className="client-warning">Esta cotización pertenece a una corrección. <a href={`/cliente/pedidos/${q.correccion.pedido}`}>Volvé al pedido para revisar y responder la solicitud</a>. No crea un segundo pedido.</p>}
 <section className="client-summary"><h2>Resumen de tu cotización</h2>{o.items.map((item,i)=><section className="client-card" key={item.codigoPublico}><h3>Archivo {i+1}: {item.documento.nombre}</h3><dl className="client-key-values"><div><dt>Páginas / copias</dt><dd>{item.trabajo.paginas} páginas · {item.trabajo.copias} copias</dd></div><div><dt>Formato y papel</dt><dd>{item.formato} · {item.papel}</dd></div><div><dt>Impresión</dt><dd>{item.trabajo.color==="COLOR"?"Color":"Blanco y negro"} · {item.trabajo.dobleFaz?"Doble faz":"Simple faz"}</dd></div><div><dt>Carillas / hojas físicas</dt><dd>{item.precio.carillas} carillas · {item.precio.hojas} hojas</dd></div></dl><div className="client-table-wrap"><table className="client-table"><thead><tr><th>Servicio</th><th>Unidades</th><th>Precio unitario</th><th>Subtotal</th></tr></thead><tbody>{item.precio.lineas.map(l=><tr key={l.servicio}><td>{l.nombre}</td><td>{l.unidades}</td><td>{money(l.precioUnitario)}</td><td>{money(l.importe)}</td></tr>)}</tbody></table></div><p><strong>Subtotal: {money(item.precio.subtotal)}</strong></p><p className="client-muted">Datos declarados al cotizar. Consultá abajo los PDF recibidos, sus resultados de inspección y su aceptación.</p></section>)}
 <div className="client-grid"><section className="client-card"><h3>Entrega</h3><p>{modeLabels[o.modalidad]} · {o.destino}</p><p>Sucursal responsable: {o.sucursal.nombre}</p><p>Disponibilidad estimada: {date(o.entrega.disponibleDesde)}</p><p className="client-muted">{q.estado==="CONFIRMADA"?"Estimación capturada al cotizar. Consultá la reserva definitiva en el pedido confirmado.":"Sujeta a revisión, requisitos de pago y disponibilidad al confirmar. No hay un cupo reservado."}</p></section><section className="client-card"><h3>Importe cotizado</h3><dl className="client-key-values"><div><dt>Trabajos</dt><dd>{money(o.subtotal)}</dd></div><div><dt>Entrega</dt><dd>{money(o.costoEntrega)}</dd></div></dl><p className="client-total">{money(o.total)}</p><p>Medio preferido: {o.medioPago==="EFECTIVO"?"Efectivo":"Transferencia"}</p></section></div></section>
 <section className="client-card"><h2>Condiciones para continuar</h2><p>{o.condiciones.revisionHumana?"Requiere revisión de la imprenta.":"Las reglas de esta cotización no exigen revisión humana; los controles técnicos siguen siendo obligatorios."}</p><dl className="client-key-values"><div><dt>Pago previo requerido</dt><dd>{money(o.condiciones.pagoPrevioRequerido)}</dd></div><div><dt>Seña requerida</dt><dd>{money(o.condiciones.senaRequerida)}</dd></div><div><dt>Saldo posterior al anticipo</dt><dd>{money(o.condiciones.saldo)}</dd></div></dl>{o.condiciones.instrucciones.map((s,i)=><p key={i}>{s}</p>)}{o.condiciones.cargaRequiereAcreditacion&&<p className="client-warning">El PDF debe permanecer en tu dispositivo hasta que se acredite el importe requerido.</p>}<p className="client-note">Aceptar estas condiciones registra tu decisión sobre la oferta. Todavía no confirma un pedido, acepta una vista previa ni registra dinero.</p>{q.aceptadaEn&&<p className="client-success">Oferta aceptada el {date(q.aceptadaEn)}.</p>}{live&&!q.aceptadaEn&&<div className="client-form"><label className="client-check"><input type="checkbox" checked={ack} disabled={locked} onChange={e=>setAck(e.target.checked)}/>Revisé los importes, la entrega y las condiciones de esta cotización.</label><button className="client-button" disabled={locked||!ack} onClick={()=>void mutate("aceptar")}>Aceptar cotización</button></div>}</section>
 <PaymentPanel quote={id} onChange={()=>setFinancialRefresh(n=>n+1)}/><PrivateFiles quote={id} version={q.version} refresh={financialRefresh} confirmed={q.estado==="CONFIRMADA"} correction={q.correccion??undefined} onChange={()=>setFinancialRefresh(n=>n+1)}/><OrderConfirmation quote={q} refresh={financialRefresh}/><section className="client-card"><h2>Revisar o cancelar la oferta</h2>{q.reemplaza&&<p>Reemplaza una <a href={`/cliente/cotizaciones/${q.reemplaza}`}>cotización anterior</a>.</p>}{q.reemplazadaPor?<p>Esta cotización tiene una <a href={`/cliente/cotizaciones/${q.reemplazadaPor}`}>oferta nueva que requiere su propia aceptación</a>.</p>:q.estado!=="CONFIRMADA"&&(!q.correccion||q.correccion.pendiente)&&<a className="client-button secondary" href={locked?undefined:`/cliente/cotizaciones/nueva?reemplaza=${id}${q.correccion?`&pedido=${q.correccion.pedido}&solicitud=${q.correccion.solicitud}`:""}`}>Revisar y volver a cotizar</a>}{live&&<form className="client-form" onSubmit={e=>{e.preventDefault();void mutate("cancelar");}}><fieldset disabled={locked}><label>Motivo de cancelación<textarea required maxLength={300} value={reason} onChange={e=>setReason(e.target.value)}/></label><div className="client-actions"><button className="client-button secondary">Cancelar cotización</button></div></fieldset></form>}{q.motivoCancelacion&&<p>{q.motivoCancelacion}</p>}{expired&&q.estado==="VIGENTE"&&<p className="client-warning">La oferta venció. Solicitá y aceptá una nueva cotización para continuar.</p>}</section></>}
 {error&&<p role="alert" className="client-warning">{error}</p>}{!q&&!loading&&<button className="client-button secondary" onClick={()=>void load()}>Volver a consultar</button>}{uncertain&&<div className="client-warning"><p>No se pudo confirmar la respuesta. Recuperá la misma operación antes de continuar.</p><button className="client-button" disabled={busy} onClick={()=>void mutate()}>Recuperar resultado</button></div>}
 </>;
}
