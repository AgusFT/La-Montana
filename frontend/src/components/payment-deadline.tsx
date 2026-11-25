"use client";

import {useEffect,useRef,useState,type FormEvent} from "react";
import {MutationError,secureMutation} from "@/lib/secure-mutation";
import {date} from "@/lib/quote-types";
export type PaymentDeadline={pedido:string;version:number;estado:string;mensaje:string;limite:string|null;retenerHasta:string|null;retencionHoras:24|48|72;puedeInformar:boolean;purgaSuspendida:boolean;recordatorio:boolean;puedeResolver:boolean;historial:{tipo:string;actor:string;fecha:string;motivo:string}[]};
const labels:Record<string,string>={SIN_ANTICIPO:"Condición sin anticipo",PENDIENTE_PAGO:"Pendiente de pago",PAGO_INFORMADO:"Pago informado · pendiente de verificación",PAGO_INFORMADO_FUERA_TERMINO:"Pago informado fuera de término · pendiente de verificación",PAGO_ACREDITADO:"Condición económica acreditada",PAGO_VENCIDO_RECUPERABLE:"Pago vencido · recuperable",RESOLUCION_ADMINISTRATIVA:"Resolución administrativa pendiente",RESOLUCION_FINANCIERA_PENDIENTE:"Pedido cancelado · resolución financiera pendiente",RETENCION_AGOTADA:"Retención técnica agotada",VENCIDO_DEFINITIVO:"Vencido definitivamente",CANCELADO:"Pedido cancelado"};
const actions={ACEPTAR_CONTINUAR:"Aceptar el caso y continuar",MANTENER_PENDIENTE:"Mantener pendiente",EXTENDER_PLAZO:"Extender plazo",SOLICITAR_COMPROBANTE:"Solicitar nuevo comprobante",FINALIZAR_CANCELACION:"Finalizar resolución financiera de cancelación"};

export function PaymentDeadlinePanel({value,internal,blocked,onChange,onLockChange}:{value:PaymentDeadline;internal:boolean;blocked:boolean;onChange:()=>void;onLockChange:(v:boolean)=>void}){
 const[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState("");
 const[action,setAction]=useState<keyof typeof actions>("MANTENER_PENDIENTE");
 const command=useRef<string|null>(null),locked=busy||uncertain;
 const canceled=["CANCELADO","RESOLUCION_FINANCIERA_PENDIENTE"].includes(value.estado);
 useEffect(()=>{if(!locked)setAction(canceled?"FINALIZAR_CANCELACION":"MANTENER_PENDIENTE");},[canceled,locked]);
 useEffect(()=>{onLockChange(locked);return()=>onLockChange(false);},[locked,onLockChange]);
 async function submit(e?:FormEvent<HTMLFormElement>){
  e?.preventDefault();if(busy)return;setError("");
  if(!command.current){if(!e||blocked)return;const f=new FormData(e.currentTarget),raw=f.get("limite");let limit:string|null=null;
   if(action==="EXTENDER_PLAZO"){const d=new Date(String(raw));if(!Number.isFinite(d.getTime())){setError("Indicá una fecha/hora futura válida.");return;}limit=d.toISOString();}
   command.current=JSON.stringify({operacion:crypto.randomUUID(),version:value.version,accion:action,nuevoLimite:limit,motivo:f.get("motivo")});
  }
  setBusy(true);try{await secureMutation(`/api/operacion/pedidos/${value.pedido}/cobro/resolver`,command.current,"application/json");command.current=null;setUncertain(false);onChange();}
  catch(cause){setError(cause instanceof Error?cause.message:"No se pudo resolver el caso.");if(cause instanceof MutationError&&!cause.uncertain){command.current=null;setUncertain(false);}else setUncertain(true);}
  finally{setBusy(false);}
 }
 return <section className="client-note" aria-label="Plazo de pago y conservación"><h3>{labels[value.estado]??value.estado}</h3><p>{value.mensaje}</p>
  {value.recordatorio&&<p role="status"><strong>Recordatorio: se aproxima el vencimiento comercial.</strong> Si ya transferiste, informá el pago para verificarlo.</p>}
  <dl className="client-key-values"><div><dt>Plazo comercial</dt><dd>{value.limite?date(value.limite):"Sin plazo automático en esta versión legada"}</dd></div><div><dt>Conservación posterior</dt><dd>{value.retencionHoras} horas{value.retencionHoras===72?" · Recomendada":""}</dd></div>{value.retenerHasta&&<div><dt>Fin de retención original</dt><dd>{date(value.retenerHasta)}</dd></div>}</dl>
  {value.purgaSuspendida&&<p className="client-success">Eliminación automática suspendida por pago informado, dinero o intervención pendiente. No se elimina el archivo al vencer el plazo.</p>}
  {value.puedeResolver&&internal&&<details><summary>Resolver excepción de pago</summary><p>Acreditar y aplicar dinero se realiza con las acciones financieras existentes. Aceptar el caso no acredita fondos ni omite la revisión humana. Revisá la franja de entrega y reprogramá si corresponde.</p>
   <form className="client-form" onSubmit={e=>void submit(e)}><fieldset disabled={blocked||locked}><label>Acción<select value={action} onChange={e=>setAction(e.target.value as keyof typeof actions)}>{Object.entries(actions).filter(([k])=>canceled?k==="FINALIZAR_CANCELACION":k!=="FINALIZAR_CANCELACION").map(([k,v])=><option key={k} value={k}>{v}</option>)}</select></label>{action==="EXTENDER_PLAZO"&&<label>Nuevo vencimiento (hora de tu dispositivo)<input type="datetime-local" name="limite" required/></label>}<label>Motivo / indicación visible al cliente<textarea name="motivo" required maxLength={2000}/></label><button className="client-button">Registrar resolución</button></fieldset></form>
   <p>Para rechazar un pago informado, usá «Rechazar informe» en el movimiento pendiente. Para cancelar definitivamente el pedido, usá la gestión del pedido con motivo; el dinero pendiente de devolución no se borra.</p>
  </details>}
  {error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<button type="button" className="client-button" disabled={busy} onClick={()=>void submit()}>Recuperar la misma resolución</button>}
  {value.historial.length>0&&<details><summary>Historial de plazos y resolución</summary><ol>{value.historial.map((e,i)=><li key={`${e.fecha}:${i}`}><strong>{e.tipo.replaceAll("_"," ")}</strong> · {date(e.fecha)} · {e.actor}<p>{e.motivo}</p></li>)}</ol></details>}
 </section>;
}
