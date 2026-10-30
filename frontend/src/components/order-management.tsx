"use client";
import {useEffect,useId,useRef,useState,type ReactNode} from "react";
import {quoteRead,date} from "@/lib/quote-types";
import {isOrder,orderState,type Order} from "@/lib/order-types";
import {secureMutation,MutationError} from "@/lib/secure-mutation";

type Action="REVISAR"|"APROBAR"|"RECHAZAR"|"CANCELAR"|"OBSERVAR";
export type Management={version:number;cliente:string;correo:string;revisadaEn:string|null;revisor:string|null;aprobador:string|null;puedeObservar:boolean;acciones:{accion:Action;habilitada:boolean;motivo:string|null}[];observaciones:{codigoPublico:string;autor:string;importancia:string;texto:string;fecha:string}[];historial:{accion:Action|"PEDIR_CORRECCION"|"RESPONDER_CORRECCION";estadoOrigen:string;estadoDestino:string;actor:string;motivo:string;mensajeCliente:string;fecha:string}[]};
const labels:Record<Action|"PEDIR_CORRECCION"|"RESPONDER_CORRECCION",string>={PEDIR_CORRECCION:"Solicitar corrección",RESPONDER_CORRECCION:"Responder corrección",REVISAR:"Registrar revisión",APROBAR:"Aprobar pedido",RECHAZAR:"Rechazar pedido",CANCELAR:"Cancelar pedido",OBSERVAR:"Guardar observación interna"};
function isManagement(v:unknown):v is Management {if(!v||typeof v!=="object")return false;const x=v as Record<string,unknown>;return Number.isSafeInteger(x.version)&&typeof x.cliente==="string"&&typeof x.puedeObservar==="boolean"&&Array.isArray(x.acciones)&&Array.isArray(x.observaciones)&&Array.isArray(x.historial);}

export function OrderManagement({order,internal,onSaved,compact=false,workspace=false,leadingAction,onSnapshot,onLockChange,refresh=0}:{order:Order;internal:boolean;onSaved:()=>void;compact?:boolean;workspace?:boolean;leadingAction?:(disabled:boolean)=>ReactNode;refresh?:number;onSnapshot?:(value:Management|null)=>void;onLockChange?:(locked:boolean)=>void}){
 const[data,setData]=useState<Management|null>(null),[error,setError]=useState(""),[message,setMessage]=useState(""),[action,setAction]=useState<Action|null>(null),[loading,setLoading]=useState(false),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false);
 const pending=useRef<{path:string;body:string}|null>(null),running=useRef(false),form=useRef<HTMLFormElement>(null),dialog=useRef<HTMLDialogElement>(null),trigger=useRef<HTMLButtonElement|null>(null),title=useId();
 const base=`/api/operacion/pedidos/${order.codigoPublico}`,locked=busy||loading||uncertain;
 async function load(){if(!internal)return;setLoading(true);setError("");try{setData(await quoteRead(`${base}/gestion`,isManagement));}catch(e){setError(e instanceof Error?e.message:"No pudimos consultar la revisión.");setData(null);}finally{setLoading(false);}}
 useEffect(()=>{void load();},[order.codigoPublico,order.version,internal,refresh]);
 useEffect(()=>{onSnapshot?.(data);},[data,onSnapshot]);
 useEffect(()=>{onLockChange?.(busy||uncertain);return()=>onLockChange?.(false);},[busy,uncertain,onLockChange]);
 useEffect(()=>{if(!workspace)return;if(action)dialog.current?.showModal();else if(dialog.current?.open){dialog.current.close();trigger.current?.focus();}},[action,workspace]);
 function choose(a:Action,button?:HTMLButtonElement){trigger.current=button??null;setAction(a);setError("");setMessage("");if(!workspace)setTimeout(()=>form.current?.scrollIntoView({behavior:"smooth",block:"center"}),0);}
 async function save(recover=false){
  if(running.current||!recover&&locked)return;
  if(!recover){
   if(!action||!form.current?.reportValidity())return;
   const f=new FormData(form.current),op={operacion:crypto.randomUUID(),version:order.version};
   const body=action==="OBSERVAR"?{...op,importancia:f.get("importancia"),texto:f.get("motivo")}:internal?{...op,accion:action,motivo:f.get("motivo")??"",mensajeCliente:f.get("mensajeCliente"),archivosYDatosRevisados:f.get("revisado")==="on"}:{...op,motivo:String(f.get("motivo")??"").trim()||(compact?"Cancelación solicitada por el cliente.":"")};
   pending.current={path:internal?`${base}/${action==="OBSERVAR"?"observaciones":"gestion"}`:`/api/cliente/pedidos/${order.codigoPublico}/cancelar`,body:JSON.stringify(body)};
  }
  if(!pending.current)return;running.current=true;setBusy(true);setError("");
  try{
   const res=await secureMutation(pending.current.path,pending.current.body,"application/json"),v:unknown=await res.json();
   if(pending.current.path.endsWith("/observaciones")){if(!isManagement(v))throw new Error("No se pudo verificar la observación guardada.");setData(v);}
   else if(!isOrder(v)||v.codigoPublico!==order.codigoPublico)throw new Error("No se pudo verificar el estado guardado.");
   pending.current=null;setUncertain(false);setAction(null);setMessage("Operación guardada. El historial conserva el resultado.");onSaved();
  }catch(e){setError(e instanceof Error?e.message:"No se pudo registrar la operación.");if(e instanceof MutationError&&!e.uncertain){pending.current=null;setUncertain(false);}else setUncertain(true);}
  finally{running.current=false;setBusy(false);}
 }
 const active=["PENDIENTE_REVISION","CORRECCION_SOLICITADA","APROBADO"].includes(order.estado),normal=action==="APROBAR"||action==="REVISAR";
 const approval=data?.version===order.version?data.acciones.find(a=>a.accion==="APROBAR"):undefined;
 const financialNotice="La cancelación o el rechazo libera la reserva. El dinero recibido y los PDF se conservan; una devolución exige su registro por personal autorizado.";
 const feedback=<>{error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<div className="client-warning"><p>No se pudo confirmar la respuesta. Recuperá la misma operación para consultar su resultado sin duplicarla.</p><button type="button" className="client-button" disabled={busy} onClick={()=>void save(true)}>Recuperar operación del pedido</button></div>}</>;
 const decision=action&&<form ref={form} key={action} className="client-form order-decision-form" onSubmit={e=>{e.preventDefault();void save();}}><h3 id={title}>{action==="APROBAR"?`Aprobar pedido PED-${order.numero}`:labels[action]}</h3>{workspace&&feedback}<fieldset disabled={locked}>
  {action==="OBSERVAR"&&<label>Importancia<select name="importancia" required defaultValue=""><option value="" disabled>Seleccioná la importancia</option><option value="INFORMATIVA">Informativa</option><option value="RELEVANTE">Relevante</option><option value="CRITICA">Crítica</option></select></label>}
  {workspace&&normal?<><p>Al aprobar se guardan la revisión y la decisión con tu usuario y fecha.</p><label className="client-check"><input name="revisado" type="checkbox" required/>Revisé los archivos, las opciones de impresión y los datos de entrega.</label><details><summary>Agregar una observación (opcional)</summary><label>Observaciones internas<textarea name="motivo" maxLength={500}/></label></details></>:<><label>{action==="OBSERVAR"?"Observación interna":internal?normal?"Observaciones internas (opcional)":"Motivo interno (no se muestra al cliente)":compact?"Motivo de cancelación (opcional)":"Motivo de cancelación"}<textarea name="motivo" required={internal?!normal:!compact} maxLength={action==="OBSERVAR"?2000:500}/></label>{normal&&<label className="client-check"><input name="revisado" type="checkbox" required/>Revisé los PDF confirmados, las opciones del trabajo y los datos de entrega.</label>}</>}
  {internal&&["CANCELAR","RECHAZAR"].includes(action)&&<label>Mensaje visible al cliente<textarea name="mensajeCliente" required maxLength={500}/></label>}
  {["CANCELAR","RECHAZAR"].includes(action)&&<><p className="client-warning">{compact&&!internal?"Vas a cancelar este pedido. Si ya pagaste, la devolución debe coordinarse con la imprenta; no se realiza automáticamente.":financialNotice}</p>{(!compact||internal)&&<label className="client-check"><input type="checkbox" required/>Confirmo {action==="RECHAZAR"?"el rechazo":"la cancelación"} y la liberación de la reserva.</label>}</>}
  <div className="client-actions"><button className={`client-button ${["CANCELAR","RECHAZAR"].includes(action)?"order-danger":"order-approve"}`}>{busy?"Guardando…":action==="APROBAR"?"Confirmar aprobación":compact&&!internal?"Sí, cancelar pedido":`Confirmar: ${labels[action].toLowerCase()}`}</button><button type="button" className="client-button secondary" onClick={()=>setAction(null)}>{compact&&!internal?"Conservar pedido":"Volver"}</button></div>
 </fieldset></form>;
 return <section className={compact&&!internal?"client-order-cancel":`client-card order-management${workspace?" operation-task-component operation-review-actions":""}`} aria-label={internal?"Decisión sobre el pedido":"Gestión de mi pedido"}>
  {!workspace&&(!compact||internal)&&<header className="order-management-heading"><h2>{internal?"Revisión administrativa":"Gestión de tu pedido"}</h2><span>{orderState(order.estado,order.reserva.modalidad)}</span></header>}
  {loading&&<p role="status">Consultando acciones…</p>}
  {internal&&data&&(workspace?<><div className="operation-decision-actions">{order.estado==="PENDIENTE_REVISION"&&<button className="client-button order-approve" disabled={locked||!approval?.habilitada||!!action} onClick={e=>choose("APROBAR",e.currentTarget)}>Aprobar pedido</button>}{data.puedeObservar&&["PENDIENTE_REVISION","APROBADO"].includes(order.estado)&&<a className="client-button secondary" href="#pedido-correcciones">Solicitar corrección</a>}
   {data.acciones.some(a=>a.habilitada&&["RECHAZAR","CANCELAR"].includes(a.accion))&&<details className="operation-more-decisions"><summary>Otras acciones</summary>{data.acciones.filter(a=>a.habilitada&&["RECHAZAR","CANCELAR"].includes(a.accion)).map(a=><button key={a.accion} className="client-button order-danger" disabled={locked||!!action} onClick={e=>choose(a.accion,e.currentTarget)}>{labels[a.accion]}</button>)}</details>}</div></>:<><p>{data.cliente} · {data.correo}</p><div className="order-decision-grid">{data.acciones.map(a=><div key={a.accion}><button className="client-button secondary" disabled={locked||!a.habilitada||!!action} onClick={()=>choose(a.accion)}>{labels[a.accion]}</button>{a.motivo&&<p>{a.motivo}</p>}</div>)}</div></>)}
  {!internal&&<>{!compact&&<p>{active?"Podés cancelar este pedido antes de que comience la producción.":"La cancelación directa no está disponible en este estado."}</p>}{!compact&&<p>{financialNotice}</p>}<div className="client-order-action-buttons">{leadingAction?.(locked||!!action)}{active&&<button className="client-button order-danger" disabled={locked||!!action} onClick={()=>choose("CANCELAR")}>Cancelar pedido</button>}</div>{compact&&active&&<p className="client-muted">Podés cancelarlo antes de que comience la producción.</p>}</>}
  {workspace?<dialog ref={dialog} className="operation-decision-dialog" aria-labelledby={title} onCancel={e=>{e.preventDefault();if(!locked)setAction(null);}}>{decision}</dialog>:decision}
  {(!workspace||!action)&&feedback}{message&&<p role="status" className="client-success">{message}</p>}
  {internal&&data&&<details className="order-private-notes"><summary>Notas y decisiones anteriores</summary>{data.puedeObservar&&<button className="client-button secondary" disabled={locked||!!action} onClick={e=>choose("OBSERVAR",e.currentTarget)}>Agregar nota interna</button>}{data.observaciones.map(n=><article key={n.codigoPublico}><strong>{n.importancia} · {n.autor}</strong><p>{date(n.fecha)} · {n.texto}</p></article>)}{data.historial.map((e,i)=><p key={i}>{date(e.fecha)} · {labels[e.accion]} · {e.actor}<br/>{e.motivo}</p>)}</details>}
 </section>;
}
