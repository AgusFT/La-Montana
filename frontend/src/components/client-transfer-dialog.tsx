"use client";
import {useEffect,useId,useRef,useState,type FormEvent} from "react";
import type {PaymentView} from "@/lib/payment-types";
import {money} from "@/lib/quote-types";
import {normalizeReceiptAmount,receiptSuggestion} from "@/lib/payment-receipt";
import {ClientTransferCommand,transferFileError} from "@/lib/client-transfer";
import {MutationError,secureMutation} from "@/lib/secure-mutation";
import {useNavigationGuard} from "./navigation-boundary";
import "@/app/payments.css";

export function ClientTransferDialog({data,onClose,onSaved,onLockChange}:{data:PaymentView;onClose:()=>void;onSaved:(view:PaymentView,message:string)=>void;onLockChange?:(locked:boolean)=>void}){
 const dialog=useRef<HTMLDialogElement>(null),amount=useRef<HTMLInputElement>(null),command=useRef<ClientTransferCommand|null>(null),running=useRef(false),title=useId();
 const [file,setFile]=useState<File|null>(null),[error,setError]=useState(""),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[reported,setReported]=useState(false);
 const locked=busy||uncertain,suggestion=receiptSuggestion(data,"TRANSFERENCIA");
 useNavigationGuard({blocked:locked,dirty:!!file&&!reported});
 useEffect(()=>{const opener=document.activeElement;dialog.current?.showModal();return()=>{dialog.current?.close();if(opener instanceof HTMLElement&&opener.isConnected)opener.focus();};},[]);
 useEffect(()=>{onLockChange?.(locked);return()=>onLockChange?.(false);},[locked,onLockChange]);
 function finish(view:PaymentView,message="Transferencia informada. La imprenta verificará el pago."){onSaved(view,message);}
 async function execute(){
  if(running.current||!command.current)return;running.current=true;setBusy(true);setError("");
  try{
   const {view,evidence}=await command.current.execute(secureMutation);setUncertain(false);
   const message=evidence?.estado==="VALIDO"?"Transferencia informada con comprobante adjunto. La imprenta verificará el pago.":evidence?.estado==="VALIDANDO"?"Transferencia informada. El comprobante está en análisis; podés consultar su estado en los pagos.":evidence?`Transferencia informada. El comprobante no pudo aceptarse: ${evidence.mensaje??"podés adjuntar otro desde los pagos."}`:undefined;
   finish(view,message);
  }catch(e){
   setError(e instanceof Error?e.message:"No pudimos completar el aviso.");setReported(!!command.current.view);
   const unknown=!(e instanceof MutationError)||e.uncertain;setUncertain(unknown);
   if(!unknown&&!command.current.view)command.current=null;
  }finally{running.current=false;setBusy(false);}
 }
 function submit(e:FormEvent<HTMLFormElement>){
  e.preventDefault();if(locked)return;
  if(command.current){void execute();return;}
  const importe=normalizeReceiptAmount(String(new FormData(e.currentTarget).get("importe")??""));
  amount.current?.setCustomValidity(importe?"":"Ingresá un importe positivo, con hasta dos decimales. Podés usar coma o punto.");
  if(!importe){amount.current?.reportValidity();return;}
  const invalid=transferFileError(file);if(invalid){setError(invalid);return;}
  command.current=new ClientTransferCommand(data.cotizacion,importe,file);void execute();
 }
 return <dialog ref={dialog} className="payment-receipt-dialog client-transfer-dialog" aria-labelledby={title} onCancel={e=>{e.preventDefault();if(!locked){if(command.current?.view)finish(command.current.view,"Transferencia informada. El comprobante quedó pendiente; podés retomarlo desde los pagos.");else onClose();}}}>
  <form className="client-form" onSubmit={submit}><h2 id={title}>Informar transferencia</h2><p>Confirmá el importe que ya transferiste para este trabajo.</p>
   {error&&<p role="alert" className="client-warning">{error}</p>}
   {reported&&<p role="status" className="client-note">El aviso ya quedó registrado. Sólo falta completar el comprobante; no se volverá a informar el importe.</p>}
   <fieldset disabled={locked}>
    <label>Importe transferido (ARS)<input ref={amount} name="importe" required autoFocus inputMode="decimal" maxLength={20} defaultValue={suggestion.importe.replace(".",",")} readOnly={reported} onChange={e=>e.currentTarget.setCustomValidity("")}/></label>
    <p className="client-muted">{suggestion.label}. Podés cambiarlo si transferiste otra cantidad.</p>
    <p className="payment-receipt-context">Total del trabajo: <strong>{money(data.total)}</strong> · Saldo pendiente: <strong>{money(data.pendiente)}</strong></p>
    {data.intentos.some(i=>i.estado==="PENDIENTE")&&<p className="client-note">Ya hay una transferencia informada pendiente de verificación. Confirmá sólo si hiciste otra transferencia.</p>}
    <label>Adjuntar comprobante (opcional)<input type="file" accept="application/pdf,.pdf" disabled={reported} onChange={e=>{setFile(e.target.files?.[0]??null);setError("");}}/></label>
    <p className="client-muted">PDF de hasta 10 MiB. También podés adjuntarlo después.</p>
    <p className="client-note">La imprenta verificará la recepción. Informar el pago o adjuntar un comprobante no lo acredita automáticamente.</p>
    <div className="client-actions"><button className="client-button payment-confirm">{busy?"Enviando…":reported?"Reintentar comprobante":"Confirmar transferencia informada"}</button><button type="button" className="client-button secondary" onClick={()=>command.current?.view?finish(command.current.view,"Transferencia informada. El comprobante quedó pendiente; podés retomarlo desde los pagos."):onClose()}>{reported?"Finalizar sin comprobante":"Volver"}</button></div>
   </fieldset>
   {uncertain&&<div role="alert" className="client-warning"><p>Falta confirmar la respuesta. Recuperá esta misma operación para evitar duplicados.</p><button type="button" className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar aviso y comprobante</button></div>}
  </form>
 </dialog>;
}
