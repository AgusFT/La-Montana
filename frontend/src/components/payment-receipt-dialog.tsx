"use client";
import {useEffect,useId,useRef,type FormEvent} from "react";
import {money} from "@/lib/quote-types";
import type {PaymentView} from "@/lib/payment-types";
import {normalizeReceiptAmount,receiptSuggestion} from "@/lib/payment-receipt";

export function PaymentReceiptDialog({data,medio,intento,locked,busy,uncertain,error,onConfirm,onClose,onRecover}:{data:PaymentView;medio:string;intento?:string;locked:boolean;busy:boolean;uncertain:boolean;error:string;onConfirm:(body:Record<string,unknown>)=>void;onClose:()=>void;onRecover:()=>void}){
 const dialog=useRef<HTMLDialogElement>(null),title=useId(),amount=useRef<HTMLInputElement>(null);
 const suggestion=receiptSuggestion(data,medio,intento);
 useEffect(()=>{const opener=document.activeElement;dialog.current?.showModal();return()=>{dialog.current?.close();if(opener instanceof HTMLElement&&opener.isConnected)opener.focus();};},[]);
 function submit(e:FormEvent<HTMLFormElement>){
  e.preventDefault();if(locked)return;
  const form=new FormData(e.currentTarget),importe=normalizeReceiptAmount(String(form.get("importe")??""));
  amount.current?.setCustomValidity(importe?"":"Ingresá un importe positivo, con hasta dos decimales. Podés usar coma o punto.");
  if(!importe){amount.current?.reportValidity();return;}
  const raw=String(form.get("fecha")??"");const fecha=raw?new Date(raw):null;
  if(fecha&&(!Number.isFinite(fecha.getTime())||fecha.getTime()>Date.now())){(e.currentTarget.elements.namedItem("fecha") as HTMLInputElement).setCustomValidity("Indicá una fecha pasada válida.");e.currentTarget.reportValidity();return;}
  onConfirm({importe,medio,intento:intento??null,referencia:String(form.get("referencia")??"").trim(),motivo:String(form.get("motivo")??"").trim(),recibidoEn:fecha?.toISOString()??null,verificado:true,aplicarAlPedido:true});
 }
 return <dialog ref={dialog} className="payment-receipt-dialog" aria-labelledby={title} onCancel={e=>{e.preventDefault();if(!locked)onClose();}}>
  <form className="client-form" onSubmit={submit}><h2 id={title}>{medio==="EFECTIVO"?"Confirmar efectivo recibido":"Confirmar transferencia recibida"}</h2>
   <p>COT-{data.numero} · {data.cliente}</p>
   {error&&<p role="alert" className="client-warning">{error}</p>}
   <fieldset disabled={locked}><label>Importe recibido (ARS)<input ref={amount} name="importe" required autoFocus inputMode="decimal" maxLength={20} defaultValue={suggestion.importe.replace(".",",")} onChange={e=>e.currentTarget.setCustomValidity("")}/></label>
    <p className="client-muted">{suggestion.label}. Podés cambiarlo si recibiste otra cantidad.</p>
    <p className="payment-receipt-context">Saldo del trabajo: <strong>{money(data.pendiente)}</strong>. El pago se descontará hasta el importe permitido; cualquier excedente quedará registrado como dinero disponible.</p>
    <details><summary>Referencia, fecha u observaciones (opcional)</summary><label>{medio==="EFECTIVO"?"Número de recibo":"Referencia de la operación bancaria"}<input name="referencia" maxLength={160}/></label><label>Fecha y hora del movimiento<input name="fecha" type="datetime-local" onChange={e=>e.currentTarget.setCustomValidity("")}/></label><p className="client-muted">Si la dejás vacía, se registra la fecha y hora de la confirmación.</p><label>Observaciones<textarea name="motivo" maxLength={300}/></label></details>
    <p className="client-muted">Confirmá después de comprobar el dinero recibido. Se guardarán tu usuario, el importe, el medio y la fecha y hora.</p>
    <div className="client-actions"><button className="client-button payment-confirm">{busy?"Registrando…":"Confirmar recepción"}</button><button type="button" className="client-button secondary" onClick={onClose}>Volver</button></div>
   </fieldset>
   {uncertain&&<div role="alert" className="client-warning"><p>La respuesta quedó pendiente. Recuperá esta misma operación para evitar registrarla dos veces.</p><button type="button" className="client-button" disabled={busy} onClick={onRecover}>Recuperar registro</button></div>}
  </form>
 </dialog>;
}
