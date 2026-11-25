//#region ENCABEZADO · src/components/quote-detail.tsx
/*
 * FUNCIÓN: Coordina la segunda parte del recorrido: resumen compacto y sólo un subpaso a la vez.
 * FUNCIONES: QuoteDetail; internas load, refresh, mutate, show, editWork, continueJourney.
 * Usa el acceso a archivos decidido por el backend; no infiere pagos por textos ni omite controles.
 * Los subformularios visitados se conservan montados y las operaciones inciertas bloquean navegación.
 */
//#endregion
"use client";
import {useRouter} from "next/navigation";
import {useCallback,useEffect,useRef,useState} from "react";
import {isQuote,date,type Quote} from "@/lib/quote-types";
import {isFileView,type FileView} from "@/lib/file-types";
import {quoteStage,type QuoteStage} from "@/lib/client-journey";
import {secureMutation,MutationError} from "@/lib/secure-mutation";
import {useNavigationGuard} from "./navigation-boundary";
import {ClientJourney} from "./client-journey";
import {QuoteSummary} from "./quote-summary";
import {PaymentPanel} from "./payment-panel";
import {OrderConfirmation} from "./order-confirmation";
import {PrivateFiles} from "./private-files";
export function QuoteDetail({id}:{id:string}){
 const router=useRouter();
 const[q,setQuote]=useState<Quote|null>(null),[files,setFiles]=useState<FileView|null>(null),[loading,setLoading]=useState(true),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState(""),[ack,setAck]=useState(false),[reason,setReason]=useState(""),[now,setNow]=useState(()=>Date.now()),[revision,setRevision]=useState(0);
 const[stage,setStage]=useState<QuoteStage>("summary"),[visited,setVisited]=useState<QuoteStage[]>([]),[paymentLocked,setPaymentLocked]=useState(false),[filesLocked,setFilesLocked]=useState(false),[confirmationLocked,setConfirmationLocked]=useState(false);
 const sending=useRef(false),manual=useRef(false),request=useRef<AbortController|null>(null),command=useRef<{path:string;body:string}|null>(null);
 const locked=busy||uncertain||paymentLocked||filesLocked||confirmationLocked;
 useNavigationGuard({blocked:locked});
 const refresh=useCallback(()=>{manual.current=false;setRevision(n=>n+1);},[]);
 useEffect(()=>{setQuote(null);setFiles(null);setStage("summary");setVisited([]);setAck(false);manual.current=false;},[id]);
 useEffect(()=>{
  const abort=new AbortController();request.current?.abort();request.current=abort;
  async function load(){setLoading(true);setError("");try{
   const response=await fetch(`/api/cliente/cotizaciones/${id}`,{cache:"no-store",signal:abort.signal});const value:unknown=await response.json();if(!response.ok||!isQuote(value)||value.codigoPublico!==id)throw new Error("No pudimos consultar el resumen.");
   if(!abort.signal.aborted){setQuote(value);setAck(false);setFiles(null);}
   if(value.aceptadaEn&&value.estado==="VIGENTE"&&!value.reemplazadaPor&&Date.now()<Date.parse(value.vigenteHasta)){
    const r=await fetch(`/api/cliente/cotizaciones/${id}/archivos`,{cache:"no-store",signal:abort.signal});const v:unknown=await r.json();if(!r.ok||!isFileView(v)||v.cotizacion!==id)throw new Error("No pudimos comprobar los requisitos del archivo. Volvé a consultar antes de continuar.");if(!abort.signal.aborted)setFiles(v);
   }
  }catch(e){if(!abort.signal.aborted)setError(e instanceof Error?e.message:"No pudimos consultar el resumen.");}finally{if(!abort.signal.aborted)setLoading(false);}}
  void load();return()=>abort.abort();
 },[id,revision]);
 useEffect(()=>{const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer);},[id]);
 const next=q?quoteStage(q,files,now):"summary",live=q?.estado==="VIGENTE"&&!q.reemplazadaPor&&now<Date.parse(q.vigenteHasta);
 useEffect(()=>{if(!loading&&!locked&&!manual.current&&q&&(q.aceptadaEn||next==="blocked"||next==="confirmed"))setStage(next);},[next,loading,locked,q]);
 useEffect(()=>{if(["payment","files","confirm","confirmed"].includes(stage))setVisited(old=>old.includes(stage)?old:[...old,stage]);},[stage]);
 async function mutate(action?:"aceptar"|"cancelar"){
  if(sending.current||action&&(locked||loading)||!q)return;
  if(action){if(action==="aceptar"&&!ack||action==="cancelar"&&!reason.trim())return;command.current={path:`/api/cliente/cotizaciones/${id}/${action}`,body:JSON.stringify({operacion:crypto.randomUUID(),version:q.version,motivo:action==="cancelar"?reason.trim():null})};}
  const c=command.current;if(!c)return;request.current?.abort();sending.current=true;setBusy(true);setError("");
  try{const r=await secureMutation(c.path,c.body,"application/json");const value:unknown=await r.json();if(!isQuote(value)||value.codigoPublico!==id)throw new Error("No pudimos verificar la respuesta guardada.");setQuote(value);setFiles(null);setAck(false);setUncertain(false);command.current=null;refresh();}
  catch(e){setError(e instanceof Error?e.message:"No pudimos completar la operación.");if(e instanceof MutationError&&!e.uncertain){command.current=null;setUncertain(false);}else setUncertain(true);}
  finally{sending.current=false;setBusy(false);}
 }
 function show(value:QuoteStage){if(locked||loading)return;manual.current=true;setStage(value);}
 function editWork(){if(!q||locked||loading)return;router.push(`/cliente/cotizaciones/nueva?reemplaza=${id}${q.correccion?`&pedido=${q.correccion.pedido}&solicitud=${q.correccion.solicitud}`:""}`);}
 function continueJourney(){if(!q||locked||loading)return;manual.current=false;setStage(next);}
 if(q?.preparacion&&!q.correccion&&q.estado!=="CONFIRMADA")return <><ClientJourney part={2}/><section className="client-card"><h2>COT-{q.numero} · {q.estado}</h2><p>Esta oferta pertenece a tu trabajo preparado. La entrega, el pago, los archivos y la confirmación se revisan juntos en su resumen actual.</p><a className="client-button" href={`/cliente/preparaciones/${q.preparacion}`}>Continuar / consultar el trabajo</a><a className="client-button secondary" href="/cliente/cotizaciones">Mis cotizaciones</a></section><QuoteSummary quote={q}/></>;
 return <><ClientJourney part={q?.estado==="CONFIRMADA"?3:2}/>
  {loading&&<p role="status">Comprobando el resumen y sus requisitos…</p>}
  {q&&<><div className="client-actions"><a className="client-button secondary" href={locked?undefined:"/cliente/cotizaciones"}>Mis cotizaciones</a>{q.estado!=="CONFIRMADA"&&!q.reemplazadaPor&&(!q.correccion||q.correccion.pendiente)&&<button className="client-button secondary" disabled={locked||loading} onClick={editWork}>← Modificar trabajo</button>}<button className="client-button secondary" disabled={locked||loading} onClick={refresh}>Actualizar estado</button>{stage!=="summary"&&<button className="client-button secondary" disabled={locked||loading} onClick={()=>show("summary")}>Volver al resumen</button>}</div>
   <p className="client-muted">COT-{q.numero} · {q.estado==="VIGENTE"&&now>=Date.parse(q.vigenteHasta)?"Vencida":q.estado==="VIGENTE"?"Presupuesto guardado":q.estado} · Vigencia: {date(q.vigenteHasta)}</p>
   {q.correccion&&<p className="client-note">Este resumen corresponde a una corrección del mismo pedido. Al completar los requisitos, vas a responder desde ese pedido. <a href={`/cliente/pedidos/${q.correccion.pedido}`}>Ver solicitud</a>.</p>}
   <div hidden={stage!=="summary"}><QuoteSummary quote={q}/>
    {live&&<section className="quote-primary-action">{!q.aceptadaEn?<><label className="client-check"><input type="checkbox" checked={ack} disabled={locked||loading} onChange={e=>setAck(e.target.checked)}/>Revisé el trabajo, el precio, la entrega y las condiciones.</label><button className="client-button" disabled={!ack||locked||loading} onClick={()=>void mutate("aceptar")}>Aceptar resumen y continuar →</button></>:<><p className="client-success">Resumen aceptado el {date(q.aceptadaEn)}.</p><button className="client-button" disabled={locked||loading||!!error} onClick={continueJourney}>Continuar con mi pedido →</button></>}</section>}
   </div>
   {stage==="payment"&&<div className="quote-substep"><h2>{next==="payment"?"Completá el pago previo":"Pagos registrados"}</h2><p>{next==="payment"?"El PDF puede cargarse y el pedido completarse antes de pagar. La acreditación requerida condiciona su aprobación y producción, no la carga. Informar una transferencia no acredita el pago.":"Consultá las transferencias y los importes registrados para este trabajo."}</p><button className="client-button secondary" disabled={locked||loading} onClick={refresh}>{next==="payment"?"Ya pagué · comprobar acreditación":"Actualizar requisitos del pedido"}</button></div>}
   {visited.includes("payment")&&<div hidden={stage!=="payment"}><PaymentPanel quote={id} compact advanceRequired={next==="payment"} title={next==="payment"?"Pago previo requerido":"Pagos de este trabajo"} onChange={refresh} onLockChange={setPaymentLocked}/></div>}
   {stage==="files"&&<div className="quote-substep"><h2>Revisá lo que vamos a imprimir</h2><p>Enviá el archivo preparado, comprobá todas sus páginas y aceptá la vista previa. Después vas a crear el pedido.</p></div>}
   {visited.includes("files")&&<div hidden={stage!=="files"}><PrivateFiles quote={id} version={q.version} refresh={revision} confirmed={q.estado==="CONFIRMADA"} correction={q.correccion??undefined} onChange={refresh} onLockChange={setFilesLocked}/></div>}
   {visited.includes("confirm")&&<div hidden={stage!=="confirm"}>{q.correccion?<section className="client-card"><h2>Tu corrección está preparada</h2><p>Volvé al pedido para revisar y enviar la respuesta. Se conserva el pedido original.</p><a className="client-button" href={`/cliente/pedidos/${q.correccion.pedido}`}>Responder corrección</a></section>:<OrderConfirmation quote={q} refresh={revision} compact onLockChange={setConfirmationLocked}/>}</div>}
   {stage==="confirmed"&&<section><OrderConfirmation quote={q} refresh={revision} compact onLockChange={setConfirmationLocked}/></section>}
   {stage==="blocked"&&<section className="client-card"><h2>{!live?"Este presupuesto ya no permite crear un pedido":"Necesitamos revisar un requisito"}</h2><p className="client-warning">{!live?q.reemplazadaPor?"Hay una cotización posterior de este trabajo.":q.estado==="CANCELADA"?"La cotización fue cancelada.":"La cotización dejó de estar vigente. Revisá y aceptá un presupuesto actualizado.":files?.carga.motivo??"No pudimos comprobar los requisitos actuales. Actualizá el estado para continuar."}</p>{q.reemplazadaPor&&<a className="client-button" href={`/cliente/cotizaciones/${q.reemplazadaPor}`}>Abrir resumen actualizado</a>}</section>}
   <details className="quote-other-actions"><summary>Revisar presupuesto y otras acciones</summary><button className="client-button secondary" disabled={locked||loading} onClick={()=>show("payment")}>Consultar pagos registrados</button>{live&&<form className="client-form" onSubmit={e=>{e.preventDefault();void mutate("cancelar");}}><fieldset disabled={locked||loading}><label>Motivo de cancelación<textarea required maxLength={300} value={reason} onChange={e=>setReason(e.target.value)}/></label><button className="client-button secondary">Cancelar presupuesto</button></fieldset></form>}{q.motivoCancelacion&&<p>{q.motivoCancelacion}</p>}</details>
  </>}
  {error&&<p role="alert" className="client-warning">{error}</p>}{!q&&!loading&&<button className="client-button" onClick={refresh}>Volver a consultar</button>}
  {uncertain&&<div className="client-warning" role="alert"><p>No pudimos confirmar la respuesta. Recuperá la misma operación antes de continuar.</p><button className="client-button" disabled={busy} onClick={()=>void mutate()}>Recuperar operación</button></div>}
 </>;
}
