"use client";
import {useEffect,useRef,useState,type FormEvent} from "react";
import {MutationError,secureMutation} from "@/lib/secure-mutation";
import {catalogDate} from "@/lib/catalog-types";
import {isConfigurationDraft,type ConfigurationDraft} from "@/lib/configuration-types";
type Receipt={mensaje:string;operacion:string;vencimiento:string};
type Pending={kind:"solicitar"|"confirmar"|"revocar";body:string};
function isReceipt(value:unknown):value is Receipt{return !!value&&typeof value==="object"&&["mensaje","operacion","vencimiento"].every(key=>typeof(value as Record<string,unknown>)[key]==="string");}

export function DraftCancellation({draft,onCancelled,onRefresh,onLockChange}:{draft:ConfigurationDraft;onCancelled:(value:ConfigurationDraft)=>Promise<void>;onRefresh:()=>Promise<void>;onLockChange:(locked:boolean)=>void}){
  const[open,setOpen]=useState(false),[reason,setReason]=useState(""),[password,setPassword]=useState(""),[code,setCode]=useState(""),[receipt,setReceipt]=useState<Receipt|null>(null);
  const[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState(""),[conflict,setConflict]=useState(false);
  const pending=useRef<Pending|null>(null),operation=useRef<string|null>(null),closeAfterRevoke=useRef(false);
  const locked=busy||uncertain,programada=draft.estado==="PROGRAMADA",nombre=programada?"programación":"borrador";
  useEffect(()=>{onLockChange(locked||receipt!==null);return()=>onLockChange(false);},[locked,receipt,onLockChange]);
  async function send(kind?:"solicitar"|"confirmar"|"revocar",event?:FormEvent<HTMLFormElement>){
    event?.preventDefault();if(busy)return;setBusy(true);setError("");
    try{
      if(kind){if(!operation.current)operation.current=crypto.randomUUID();const data={operacion:operation.current,version:draft.version,motivo:reason.trim(),contrasena:password,...(kind==="confirmar"?{codigo:code.trim()}:{})};pending.current={kind,body:JSON.stringify(kind==="revocar"?{operacion:operation.current}:data)};}
      if(!pending.current)throw new Error("No hay una solicitud pendiente.");const command=pending.current;
      const response=await secureMutation(`/api/admin/configuracion/borradores/${draft.codigoPublico}/${programada?"programacion/":""}cancelacion/${command.kind}`,command.body,"application/json");const data:unknown=await response.json();
      if(command.kind==="solicitar"){
        if(!isReceipt(data)||data.operacion!==operation.current)throw new Error("No pudimos confirmar el envío del código.");
        setReceipt(data);pending.current=null;setUncertain(false);setConflict(false);
      }else if(command.kind==="revocar"){restart();setPassword("");setUncertain(false);if(closeAfterRevoke.current)setOpen(false);
      }else{
        if(!isConfigurationDraft(data)||data.estado!=="CANCELADA"||data.codigoPublico!==draft.codigoPublico)throw new Error("No pudimos confirmar la cancelación recibida.");
        pending.current=null;operation.current=null;setUncertain(false);setPassword("");setCode("");await onCancelled(data);
      }
    }catch(cause){
      setError(cause instanceof Error?cause.message:"No pudimos completar la cancelación.");
      if(cause instanceof MutationError&&!cause.uncertain){pending.current=null;setUncertain(false);setConflict(cause.status===409);}else if(pending.current)setUncertain(true);
    }finally{setBusy(false);}
  }
  function restart(){operation.current=null;pending.current=null;setReceipt(null);setCode("");setError("");setConflict(false);}
  async function leave(close:boolean){closeAfterRevoke.current=close;if(operation.current)await send("revocar");else{restart();setPassword("");if(close)setOpen(false);}}
  async function refresh(){setBusy(true);setError("");try{await onRefresh();}catch{setError("No pudimos actualizar el borrador. Intentá nuevamente.");}finally{setBusy(false);}}
  if(!open)return <section className="admin-card"><h3>Administrar el cambio pendiente</h3><p className="admin-note">Cancelar conserva el contenido en el historial y permite comenzar otro borrador. Una programación cancelada deja de reintentarse. Se confirma con tu contraseña y un código enviado a tu correo.</p><button type="button" className="admin-button secondary" onClick={()=>setOpen(true)}>Cancelar {nombre}</button></section>;
  return <section className="admin-card draft-cancellation" aria-labelledby="cancel-draft-title">
    <h3 id="cancel-draft-title">Cancelar {nombre} {draft.numero}</h3><p className="admin-note">Edición {draft.version}. Se conservarán su selección, responsable, fecha y motivo de cancelación. Los precios del catálogo no se modifican.</p>
    <form className="admin-form" onSubmit={event=>send(receipt?"confirmar":"solicitar",event)}>
      <div className="catalog-field"><label htmlFor="draft-cancel-reason">Motivo de cancelación</label><textarea id="draft-cancel-reason" required maxLength={500} value={reason} disabled={locked||receipt!==null} onChange={event=>setReason(event.target.value)}/></div>
      <div className="catalog-field"><label htmlFor="draft-cancel-password">Tu contraseña actual</label><input id="draft-cancel-password" type="password" required maxLength={128} autoComplete="current-password" value={password} disabled={locked} onChange={event=>setPassword(event.target.value)}/></div>
      {receipt&&<><p className="admin-info">Código enviado al correo de tu cuenta. Abrí el buzón local Mailpit para copiarlo. Vence el {catalogDate(receipt.vencimiento)}.</p><div className="catalog-field"><label htmlFor="draft-cancel-code">Código recibido por correo</label><input id="draft-cancel-code" required maxLength={128} autoComplete="one-time-code" spellCheck={false} value={code} disabled={locked} onChange={event=>setCode(event.target.value)}/></div><p className="admin-note">La confirmación se aplica únicamente a este borrador y su edición. Si cambian sus datos, deberá solicitarse una nueva autorización.</p></>}
      {error&&<p className="admin-warning" role="alert">{error}</p>}
      {uncertain?<div className="admin-warning"><p>No pudimos confirmar la respuesta. Conservamos la misma operación y sus datos para evitar duplicados.</p><button type="button" className="admin-button" disabled={busy} onClick={()=>send()}>Reintentar la misma operación</button></div>:<div className="configuration-actions"><button className="admin-button" disabled={busy||conflict}>{busy?"Procesando…":receipt?"Confirmar cancelación":"Enviar código de cancelación"}</button>{receipt&&<button type="button" className="admin-button secondary" disabled={busy} onClick={()=>leave(false)}>Solicitar otro código o cambiar motivo</button>}<button type="button" className="admin-button secondary" disabled={busy} onClick={()=>leave(true)}>Conservar {nombre}</button></div>}
      {conflict&&<div className="admin-warning"><p>Consultá el borrador actual antes de solicitar otra autorización.</p><button type="button" className="admin-button secondary" disabled={locked} onClick={refresh}>Actualizar borrador</button></div>}
    </form>
  </section>;
}
