//#region ENCABEZADO · src/components/order-management.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/order-management.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta la revisión administrativa del pedido, observaciones, decisiones y cancelación, con
 * control del estado y de las acciones pendientes.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - isManagement(v: unknown): v is Management
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] OrderManagement({order,internal,onSaved}:
 *   {order:Order;internal:boolean;onSaved:()=>void})
 *   Componente de interfaz.
 * - [async] OrderManagement :: load()
 * - OrderManagement :: choose(a: Action)
 * - [async] OrderManagement :: save(recover = false)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Action (tipo).
 * - Management (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - labels [const].
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useRef,useState} from "react";
import {quoteRead,date} from "@/lib/quote-types";
import {isOrder,orderState,type Order} from "@/lib/order-types";
import {secureMutation,MutationError} from "@/lib/secure-mutation";

type Action="REVISAR"|"APROBAR"|"RECHAZAR"|"CANCELAR"|"OBSERVAR";
export type Management={version:number;cliente:string;correo:string;revisadaEn:string|null;revisor:string|null;aprobador:string|null;puedeObservar:boolean;acciones:{accion:Action;habilitada:boolean;motivo:string|null}[];observaciones:{codigoPublico:string;autor:string;importancia:string;texto:string;fecha:string}[];historial:{accion:Action|"PEDIR_CORRECCION"|"RESPONDER_CORRECCION";estadoOrigen:string;estadoDestino:string;actor:string;motivo:string;mensajeCliente:string;fecha:string}[]};
const labels:Record<Action|"PEDIR_CORRECCION"|"RESPONDER_CORRECCION",string>={PEDIR_CORRECCION:"Solicitar corrección",RESPONDER_CORRECCION:"Responder corrección",REVISAR:"Registrar revisión",APROBAR:"Aprobar pedido",RECHAZAR:"Rechazar pedido",CANCELAR:"Cancelar pedido",OBSERVAR:"Guardar observación interna"};
function isManagement(v:unknown):v is Management {if(!v||typeof v!=="object")return false;const x=v as Record<string,unknown>;return Number.isSafeInteger(x.version)&&typeof x.cliente==="string"&&typeof x.puedeObservar==="boolean"&&Array.isArray(x.acciones)&&Array.isArray(x.observaciones)&&Array.isArray(x.historial);}

export function OrderManagement({order,internal,onSaved,compact=false,workspace=false,onSnapshot,onLockChange,refresh=0}:{order:Order;internal:boolean;onSaved:()=>void;compact?:boolean;workspace?:boolean;refresh?:number;onSnapshot?:(value:Management|null)=>void;onLockChange?:(locked:boolean)=>void}){
 const[data,setData]=useState<Management|null>(null),[error,setError]=useState(""),[message,setMessage]=useState(""),[action,setAction]=useState<Action|null>(null),[loading,setLoading]=useState(false),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false);
 const pending=useRef<{path:string;body:string}|null>(null),running=useRef(false),form=useRef<HTMLFormElement>(null);
 const base=`/api/operacion/pedidos/${order.codigoPublico}`,locked=busy||loading||uncertain;
 async function load(){if(!internal)return;setLoading(true);try{setData(await quoteRead(`${base}/gestion`,isManagement));}catch(e){setError(e instanceof Error?e.message:"No pudimos consultar la revisión.");setData(null);}finally{setLoading(false);}}
 useEffect(()=>{void load();},[order.codigoPublico,order.version,internal,refresh]);
 useEffect(()=>{onSnapshot?.(data);},[data,onSnapshot]);
 useEffect(()=>{onLockChange?.(busy||uncertain);return()=>onLockChange?.(false);},[busy,uncertain,onLockChange]);
 function choose(a:Action){setAction(a);setError("");setMessage("");setTimeout(()=>form.current?.scrollIntoView({behavior:"smooth",block:"center"}),0);}
 async function save(recover=false){
  if(running.current||!recover&&locked)return;
  if(!recover){
   if(!action||!form.current?.reportValidity())return;
   const f=new FormData(form.current),op={operacion:crypto.randomUUID(),version:order.version};
   const body=action==="OBSERVAR"?{...op,importancia:f.get("importancia"),texto:f.get("motivo")}:internal?{...op,accion:action,motivo:f.get("motivo"),mensajeCliente:f.get("mensajeCliente"),archivosYDatosRevisados:f.get("revisado")==="on"}:{...op,motivo:String(f.get("motivo")??"").trim()||(compact?"Cancelación solicitada por el cliente.":"")};
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
 const primary=data?.acciones.find(a=>a.accion==="REVISAR"&&a.habilitada)??data?.acciones.find(a=>a.accion==="APROBAR"&&a.habilitada);
 const active=["PENDIENTE_REVISION","CORRECCION_SOLICITADA","APROBADO"].includes(order.estado),financialNotice="La cancelación o el rechazo libera la reserva. El dinero recibido y los PDF se conservan; una devolución exige su registro por personal autorizado.";
 return <section className={compact&&!internal?"client-order-cancel":`client-card order-management${workspace?" operation-task-component":""}`} aria-label={internal?"Revisión administrativa":"Gestión de mi pedido"}>
  {(!compact||internal)&&<header className="order-management-heading"><h2>{internal?"Revisión administrativa":"Gestión de tu pedido"}</h2><span>{orderState(order.estado,order.reserva.modalidad)}</span></header>}
  {loading&&<p role="status">Consultando acciones y observaciones…</p>}
  {internal&&data&&<>{!workspace&&<div className="client-grid"><section><h3>Cliente</h3><p>{data.cliente}<br/>{data.correo}</p></section><section><h3>Revisión y decisión</h3><p>{data.revisadaEn?`Revisado por ${data.revisor} · ${date(data.revisadaEn)}`:"Sin revisión humana registrada."}</p>{order.aprobadaEn&&<p>Aprobado {data.aprobador?`por ${data.aprobador}`:"según las reglas configuradas"} · {date(order.aprobadaEn)}</p>}</section></div>}{workspace&&<p className="client-muted">{data.revisadaEn?`Revisión registrada por ${data.revisor} · ${date(data.revisadaEn)}`:"Todavía no se registró la revisión de este trabajo."}</p>}
   <p><a href="#pedido-pdf">Ver los PDF confirmados</a>. Revisá los PDF privados, las opciones de impresión y los datos de entrega. Aprobar no inicia producción ni registra pagos.</p>
   <div className="order-decision-grid">{(workspace?data.acciones.filter(a=>a.habilitada&&a===primary):data.acciones).map(a=><div key={a.accion}><button className={`client-button ${a.accion==="APROBAR"?"order-approve":["RECHAZAR","CANCELAR"].includes(a.accion)?"order-danger":workspace?"":"secondary"}`} disabled={locked||!a.habilitada||!!action} onClick={()=>choose(a.accion)}>{labels[a.accion]}</button>{a.motivo&&<p className="client-muted">{a.motivo}</p>}</div>)}</div>
   {workspace&&data.acciones.some(a=>a.habilitada&&a!==primary)&&<details className="operation-secondary-decisions"><summary>Otras decisiones sobre este pedido</summary><div className="client-actions">{data.acciones.filter(a=>a.habilitada&&a!==primary).map(a=><button key={a.accion} className="client-button order-danger" disabled={locked||!!action} onClick={()=>choose(a.accion)}>{labels[a.accion]}</button>)}</div></details>}
   {workspace&&!primary&&<p className="client-muted">{data.acciones.find(a=>a.accion==="APROBAR")?.motivo??"No hay decisiones disponibles para tu perfil en este estado."}</p>}
   <p><a href="#pedido-correcciones">Consultar o solicitar una corrección al cliente</a>.</p>
  </>}
  {!internal&&<>{!compact&&<p>{active?"Podés cancelar este pedido antes de que comience la producción.":["EN_PRODUCCION","LISTO_PARA_ENTREGA"].includes(order.estado)?"La producción ya comenzó. La cancelación directa no está disponible.":"El pedido finalizó su recorrido operativo."}</p>}{!compact&&<p>{financialNotice}</p>}{active&&<button className="client-button order-danger" disabled={locked||!!action} onClick={()=>choose("CANCELAR")}>Cancelar pedido</button>}{compact&&active&&<p className="client-muted">Podés cancelarlo antes de que comience la producción.</p>}</>}
  {action&&<form ref={form} key={action} className="client-form order-decision-form" onSubmit={e=>{e.preventDefault();void save();}}><h3>{labels[action]}</h3><fieldset disabled={locked}>
   {action==="OBSERVAR"&&<><p>Esta nota sólo será visible para el personal autorizado de la sucursal.</p><label>Importancia<select name="importancia" aria-label="Importancia" required defaultValue=""><option value="" disabled>Seleccioná la importancia</option><option value="INFORMATIVA">Informativa</option><option value="RELEVANTE">Relevante</option><option value="CRITICA">Crítica</option></select></label></>}
   <label>{action==="OBSERVAR"?"Observación interna":internal?"Motivo interno (no se muestra al cliente)":compact?"Motivo de cancelación (opcional)":"Motivo de cancelación"}<textarea name="motivo" required={internal||!compact} maxLength={action==="OBSERVAR"?2000:500}/></label>
   {internal&&["CANCELAR","RECHAZAR"].includes(action)&&<label>Mensaje visible al cliente<textarea name="mensajeCliente" required maxLength={500}/></label>}
   {["REVISAR","APROBAR"].includes(action)&&<label className="client-check"><input name="revisado" type="checkbox" required/>Revisé los PDF confirmados, las opciones del trabajo y los datos de entrega.</label>}
   {["CANCELAR","RECHAZAR"].includes(action)&&<><p className="client-warning">{compact&&!internal?"Vas a cancelar este pedido. Si ya pagaste, la devolución debe coordinarse con la imprenta; no se realiza automáticamente.":financialNotice}</p>{(!compact||internal)&&<label className="client-check"><input type="checkbox" required/>Confirmo {action==="RECHAZAR"?"el rechazo":"la cancelación"} y la liberación de la reserva.</label>}</>}
   <div className="client-actions"><button className={`client-button ${["CANCELAR","RECHAZAR"].includes(action)?"order-danger":"order-approve"}`}>{busy?"Guardando…":compact&&!internal?"Sí, cancelar pedido":`Confirmar: ${labels[action].toLowerCase()}`}</button><button type="button" className="client-button secondary" onClick={()=>setAction(null)}>{compact&&!internal?"Conservar pedido":"Volver sin guardar"}</button></div>
  </fieldset></form>}
  {error&&<p role="alert" className="client-warning">{error}</p>}{message&&<p role="status" className="client-success">{message}</p>}
  {uncertain&&<div className="client-warning"><p>No se pudo confirmar la respuesta. Recuperá la misma operación para consultar su resultado sin duplicarla.</p><button className="client-button" disabled={busy} onClick={()=>void save(true)}>Recuperar operación del pedido</button></div>}
  {internal&&data&&<><details className="order-private-notes"><summary>Notas internas e historial de decisiones</summary><h3>Observaciones internas</h3><p>Estas notas no se envían al cliente ni cambian el estado o los pagos.</p>{(!workspace||data.puedeObservar)&&<button className="client-button secondary" disabled={locked||!!action||!data.puedeObservar} onClick={()=>choose("OBSERVAR")}>Agregar observación interna</button>}{data.observaciones.length===0&&<p className="client-empty">Sin observaciones internas.</p>}{data.observaciones.map(n=><article key={n.codigoPublico} className="order-private-note"><strong>{n.importancia} · {n.autor}</strong><p>{date(n.fecha)}</p><p className="order-text">{n.texto}</p></article>)}</details>
   {data.historial.length>0&&<details className="order-internal-history"><summary>Historial de decisiones internas</summary>{data.historial.map((e,i)=><article className="order-private-note" key={i}><strong>{labels[e.accion]??e.accion.replaceAll("_"," ")} · {e.actor}</strong><p>{date(e.fecha)} · {orderState(e.estadoOrigen,order.reserva.modalidad)} → {orderState(e.estadoDestino,order.reserva.modalidad)}</p><p className="order-text">Motivo interno: {e.motivo}</p><p className="order-text">Mensaje al cliente: {e.mensajeCliente}</p></article>)}</details>}
  </>}
 </section>;
}
