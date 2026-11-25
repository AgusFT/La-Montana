//#region ENCABEZADO · src/components/order-reschedule.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/order-reschedule.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta propuestas de reprogramación y franjas disponibles, con acciones de propuesta o retiro
 * para operación y aceptación o rechazo para el cliente.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - validSlot(v: unknown): v is OrderSlot
 * - isReschedule(v: unknown): v is Reschedule
 *   Validador de datos recibidos en tiempo de ejecución.
 * - isSlots(v: unknown): v is Slots
 *   Validador de datos recibidos en tiempo de ejecución.
 * - Window({slot,title}: {slot:OrderSlot;title:string})
 *   Componente de interfaz.
 * - [export] OrderReschedule({order,internal,onSaved}:
 *   {order:Order;internal:boolean;onSaved:()=>void})
 *   Componente de interfaz.
 * - [async] OrderReschedule :: load()
 * - OrderReschedule :: choose(a: Action)
 * - [async] OrderReschedule :: lookup()
 * - [async] OrderReschedule :: save(recover = false)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Proposal (tipo).
 * - Reschedule (tipo).
 * - Slots (tipo).
 * - Action (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - labels [const].
 * - states [const].
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useRef,useState} from "react";
import {type Order,type OrderSlot} from "@/lib/order-types";
import {quoteRead,date} from "@/lib/quote-types";
import {secureMutation,MutationError} from "@/lib/secure-mutation";
type Proposal={codigoPublico:string;estado:string;vigente:boolean;bloqueo:string|null;anterior:OrderSlot;nueva:OrderSlot;configuracion:string;responsable:string;motivoInterno:string|null;mensajeCliente:string;fecha:string;decision:{resultado:string;responsable:string;motivo:string;fecha:string}|null};
type Reschedule={version:number;estadoPedido:string;puedeProponer:boolean;puedeRetirar:boolean;bloqueo:string|null;pendiente:Proposal|null;historial:Proposal[]};
type Slots={configuracion:string;version:number;zonaHoraria:string;fecha:string;franjas:OrderSlot[];mensaje:string};
type Action="PROPONER"|"ACEPTAR"|"RECHAZAR"|"RETIRAR";
const labels:Record<Action,string>={PROPONER:"Proponer nueva fecha",ACEPTAR:"Aceptar nueva fecha",RECHAZAR:"Rechazar propuesta",RETIRAR:"Retirar propuesta"};
const states:Record<string,string>={PENDIENTE:"Pendiente de respuesta",ACEPTADA:"Aceptada por el cliente",RECHAZADA:"Rechazada por el cliente",RETIRADA:"Retirada por la imprenta"};
const validSlot=(v:unknown):v is OrderSlot=>!!v&&typeof v==="object"&&["desde","hasta","fecha","apertura","cierre","zonaHoraria"].every(k=>typeof(v as Record<string,unknown>)[k]==="string");
function isReschedule(v:unknown):v is Reschedule{if(!v||typeof v!=="object")return false;const r=v as Reschedule;return Number.isSafeInteger(r.version)&&typeof r.estadoPedido==="string"&&typeof r.puedeProponer==="boolean"&&typeof r.puedeRetirar==="boolean"&&Array.isArray(r.historial)&&r.historial.every(p=>typeof p.codigoPublico==="string"&&typeof p.estado==="string"&&validSlot(p.anterior)&&validSlot(p.nueva));}
function isSlots(v:unknown):v is Slots{if(!v||typeof v!=="object")return false;const s=v as Slots;return typeof s.configuracion==="string"&&Number.isSafeInteger(s.version)&&typeof s.fecha==="string"&&Array.isArray(s.franjas)&&s.franjas.every(validSlot);}
function Window({slot,title}:{slot:OrderSlot;title:string}){return <section className="reschedule-window"><h4>{title}</h4><p><strong>{slot.fecha}</strong><br/>{slot.apertura}–{slot.cierre}<br/>{slot.zonaHoraria}</p><p>{slot.nombre}</p></section>;}
export function OrderReschedule({order,internal,onSaved}:{order:Order;internal:boolean;onSaved:()=>void}){
 const[data,setData]=useState<Reschedule|null>(null),[available,setAvailable]=useState<Slots|null>(null),[selected,setSelected]=useState(""),[day,setDay]=useState(""),[action,setAction]=useState<Action|null>(null),[loading,setLoading]=useState(false),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState("");
 const dialog=useRef<HTMLDialogElement>(null),form=useRef<HTMLFormElement>(null),running=useRef(false),pending=useRef<{path:string;body:string;action:Action}|null>(null);
 const base=`/api/${internal?"operacion":"cliente"}/pedidos/${order.codigoPublico}/reprogramacion`,locked=busy||loading||uncertain;
 const today=new Intl.DateTimeFormat("sv-SE",{timeZone:order.reserva.zonaHoraria,year:"numeric",month:"2-digit",day:"2-digit"}).format(new Date());
 async function load(){setLoading(true);setError("");try{setData(await quoteRead(base,isReschedule));}catch(e){setData(null);setError(e instanceof Error?e.message:"No pudimos consultar la reprogramación.");}finally{setLoading(false);}}
 useEffect(()=>{void load();setAvailable(null);setSelected("");},[order.codigoPublico,order.version,internal]);
 useEffect(()=>{if(action)dialog.current?.showModal();else dialog.current?.close();},[action]);
 function choose(a:Action){setAction(a);setError("");setMessage("");setAvailable(null);setSelected("");setDay("");}
 async function lookup(){if(!day||locked)return;setLoading(true);setError("");setSelected("");setAvailable(null);try{setAvailable(await quoteRead(`${base}/franjas?fecha=${encodeURIComponent(day)}`,isSlots));}catch(e){setError(e instanceof Error?e.message:"No pudimos consultar las franjas.");}finally{setLoading(false);}}
 async function save(recover=false){
  if(running.current||!recover&&locked)return;
  if(!recover){if(!data||!action||!form.current?.reportValidity())return;const f=new FormData(form.current),body:Record<string,unknown>={operacion:crypto.randomUUID(),version:data.version,motivo:f.get("motivo"),confirmado:f.get("confirmado")==="on"};let path=base;
   if(action==="PROPONER"){const slot=available?.franjas.find(s=>s.desde===selected);if(!slot||!available){setError("Consultá y seleccioná una franja disponible.");return;}Object.assign(body,{configuracion:available.configuracion,fecha:available.fecha,desde:slot.desde,hasta:slot.hasta,mensajeCliente:f.get("mensaje")});}
   else{if(!data.pendiente)return;body.propuesta=data.pendiente.codigoPublico;if(action==="RETIRAR")path+="/retirar";else{path+="/responder";body.aceptada=action==="ACEPTAR";}}
   pending.current={path,body:JSON.stringify(body),action};
  }
  const command=pending.current;if(!command)return;running.current=true;setBusy(true);setError("");
  try{const response=await secureMutation(command.path,command.body,"application/json"),result:unknown=await response.json();if(!isReschedule(result))throw new Error("No pudimos comprobar el resultado de la operación.");setData(result);pending.current=null;setUncertain(false);setAction(null);setMessage(command.action==="PROPONER"?"Propuesta enviada al cliente. La reserva anterior sigue vigente.":command.action==="ACEPTAR"?"Nueva reserva confirmada. Se conserva el precio y debe generarse otro código para recibir el pedido.":"Propuesta resuelta. La reserva anterior se conserva.");onSaved();}
  catch(e){setError(e instanceof Error?e.message:"No pudimos guardar la decisión.");if(e instanceof MutationError&&!e.uncertain){pending.current=null;setUncertain(false);}else setUncertain(true);}
  finally{running.current=false;setBusy(false);}
 }
 const feedback=<>{error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<div className="client-warning"><p>No pudimos confirmar la respuesta. Recuperá la operación para consultar el resultado sin repetirla.</p><button type="button" className="client-button" disabled={busy} onClick={()=>void save(true)}>Recuperar reprogramación</button></div>}</>;
 return <section id="pedido-reprogramacion" className="client-card reschedule-panel" aria-label="Reprogramación de entrega"><header className="order-management-heading"><h2>Reprogramar entrega</h2><span>{data?.pendiente?"Requiere respuesta":"Reserva actual"}</span></header>
  <p>La nueva fecha requiere aceptación del cliente. Hasta entonces se conserva la reserva actual. Se mantienen el destino, el precio, los PDF y los pagos del pedido.</p><button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar reprogramación</button>
  {loading&&<p role="status">Consultando disponibilidad…</p>}{!action&&feedback}{message&&<p role="status" className="client-success">{message}</p>}
  {data&&<>{data.pendiente?<article className="reschedule-proposal"><h3>Propuesta de la imprenta</h3><p className="order-text">{data.pendiente.mensajeCliente}</p><div className="client-grid"><Window title="Reserva anterior" slot={data.pendiente.anterior}/><Window title="Fecha propuesta" slot={data.pendiente.nueva}/></div><p>Propuesta el {date(data.pendiente.fecha)} · {data.pendiente.responsable}</p>{internal&&<p className="order-text"><strong>Motivo interno:</strong> {data.pendiente.motivoInterno}</p>}
    {!data.pendiente.vigente&&<p className="client-warning">{data.pendiente.bloqueo}</p>}<p className="client-note">La propuesta no ocupa cupo. Se verifica nuevamente al aceptar; si otra reserva tomó el último lugar, se conservará la fecha anterior.</p>
    <div className="client-actions">{internal?<button className="client-button order-danger" disabled={locked||!data.puedeRetirar} onClick={()=>choose("RETIRAR")}>Retirar propuesta</button>:<><button className="client-button order-danger" disabled={locked} onClick={()=>choose("RECHAZAR")}>Rechazar propuesta</button><button className="client-button order-approve" disabled={locked||!data.pendiente.vigente} onClick={()=>choose("ACEPTAR")}>Aceptar nueva fecha</button></>}</div>
   </article>:<><p className="client-muted">No hay propuestas pendientes.</p>{internal&&<button className="client-button order-danger" disabled={locked||!data.puedeProponer} onClick={()=>choose("PROPONER")}>Proponer nueva fecha</button>}{data.bloqueo&&<p className="client-muted">{data.bloqueo}</p>}</>}
   {data.historial.some(p=>p.estado!=="PENDIENTE")&&<details><summary>Historial de reprogramaciones</summary>{data.historial.filter(p=>p.estado!=="PENDIENTE").map(p=><section className="order-private-note" key={p.codigoPublico}><h3>{states[p.estado]??p.estado}</h3><p>{p.anterior.fecha} {p.anterior.apertura}–{p.anterior.cierre} → {p.nueva.fecha} {p.nueva.apertura}–{p.nueva.cierre}</p><p className="order-text">{p.mensajeCliente}</p>{p.decision&&<p className="order-text">{date(p.decision.fecha)} · {p.decision.responsable}<br/>{p.decision.motivo}</p>}{internal&&<p className="order-text">Motivo interno: {p.motivoInterno}</p>}</section>)}</details>}
  </>}
  <dialog ref={dialog} className="delivery-dialog reschedule-dialog" aria-labelledby="reschedule-dialog-title" onCancel={e=>{e.preventDefault();if(!locked)setAction(null);}}>{action&&data&&<form ref={form} className="client-form" key={action} onSubmit={e=>{e.preventDefault();void save();}}><header className="order-management-heading"><h3 id="reschedule-dialog-title">{labels[action]}</h3><span>PED-{order.numero}</span></header>{feedback}<fieldset disabled={locked}>
   {action==="PROPONER"?<><label>Nueva fecha de entrega<input name="fecha" type="date" required min={today} value={day} onChange={e=>{setDay(e.target.value);setAvailable(null);setSelected("");}}/></label><button type="button" className="client-button secondary" disabled={!day||day<today} onClick={()=>void lookup()}>Consultar franjas disponibles</button>{available&&<><p>{available.mensaje}</p>{available.franjas.length===0?<p className="client-warning">No hay franjas configuradas y vigentes para esa fecha. Elegí otra fecha.</p>:<label>Franja de entrega<select required value={selected} onChange={e=>setSelected(e.target.value)}><option value="" disabled>Seleccioná una franja</option>{available.franjas.map(s=>{const same=s.desde===order.reserva.desde&&s.hasta===order.reserva.hasta;return <option key={s.desde} value={s.desde} disabled={s.plazasLibres<1||same}>{s.apertura}–{s.cierre} · {s.zonaHoraria} · {same?"Reserva actual":`${s.plazasLibres} lugares disponibles`}</option>;})}</select></label>}</>}<label>Mensaje visible para el cliente<textarea name="mensaje" required maxLength={500}/></label></>:data.pendiente&&<><div className="client-grid"><Window title="Reserva anterior" slot={data.pendiente.anterior}/><Window title="Fecha propuesta" slot={data.pendiente.nueva}/></div><p>{action==="ACEPTAR"?"Al aceptar se verificará el cupo y se cambiará la reserva. El código anterior dejará de funcionar; los movimientos físicos ya registrados se conservan.":"Esta decisión resuelve la propuesta y conserva la reserva anterior."}</p></>}
   <label>{internal?"Motivo interno":"Motivo de tu decisión"}<textarea name="motivo" required maxLength={500}/></label><label className="client-check"><input name="confirmado" type="checkbox" required/>{action==="PROPONER"?"Revisé la fecha y confirmo que quiero proponerla al cliente.":action==="ACEPTAR"?"Acepto cambiar la reserva a la nueva fecha y franja indicadas.":"Confirmo esta decisión sobre la propuesta."}</label>
   <div className="client-actions"><button type="button" className="client-button order-danger" onClick={()=>setAction(null)}>Volver sin guardar</button><button className="client-button order-approve" disabled={action==="PROPONER"&&(!available||!selected)}>{busy?"Guardando…":"Confirmar decisión"}</button></div>
  </fieldset></form>}</dialog>
 </section>;
}
