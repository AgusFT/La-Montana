'use client';
import {useEffect,useRef,useState} from 'react';
import {createPortal} from 'react-dom';
import {quoteRead,money} from '@/lib/quote-types';
import {isPaymentView,type PaymentView} from '@/lib/payment-types';
import {secureMutation,MutationError} from '@/lib/secure-mutation';
import {PaymentReceiptDialog} from './payment-receipt-dialog';
import '@/app/payments.css';

export function OrderReceiptAction({base,medio,saldo,disabled,onSaved,onLockChange}:{
 base:string;medio:string;saldo?:string;disabled?:boolean;
 onSaved:()=>void;onLockChange?:(locked:boolean)=>void;
}){
 const [data,setData]=useState<PaymentView|null>(null),[method,setMethod]=useState(medio);
 const [loading,setLoading]=useState(false),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false);
 const [error,setError]=useState(''),[message,setMessage]=useState('');
 const command=useRef<string|null>(null),running=useRef(false);
 const locked=loading||busy||uncertain;
 useEffect(()=>{onLockChange?.(locked);return()=>onLockChange?.(false);},[locked,onLockChange]);
 async function open(){
  if(disabled||locked)return;
  setLoading(true);setError('');setMessage('');
  try{
   const value=await quoteRead(base,isPaymentView);
   if(Number(value.pendiente)<=0){setMessage('El pedido ya no tiene saldo pendiente.');onSaved();return;}
   const methods=value.mediosRecepcion.filter(m=>value.permisos.includes(m==='EFECTIVO'?'REGISTRAR_COBRO':'ACREDITAR_PAGO'));
   if(!methods.length)throw Error('Tu cuenta no tiene habilitado el registro de cobros para este pedido.');
   setMethod(methods.includes(medio)?medio:methods[0]);setData(value);
  }catch(e){setError(e instanceof Error?e.message:'No pudimos consultar el saldo.');}
  finally{setLoading(false);}
 }
 async function execute(){
  if(running.current||!command.current)return;
  running.current=true;setBusy(true);setError('');
  try{
   const response=await secureMutation(base+'/recibir',command.current,'application/json');
   const value:unknown=await response.json();
   if(!isPaymentView(value)||value.cotizacion!==data?.cotizacion)throw Error('No pudimos comprobar el cobro registrado.');
   command.current=null;setUncertain(false);setData(null);
   setMessage(`Cobro registrado. Saldo pendiente ${money(value.pendiente)}.`);onSaved();
  }catch(e){
   setError(e instanceof Error?e.message:'No pudimos registrar el cobro.');
   const retry=!(e instanceof MutationError)||e.uncertain;
   setUncertain(retry);if(!retry)command.current=null;
  }finally{running.current=false;setBusy(false);}
 }
 function receive(body:Record<string,unknown>){
  if(locked||running.current)return;
  command.current=JSON.stringify({operacion:crypto.randomUUID(),...body});void execute();
 }
 if(saldo!==undefined&&Number(saldo)<=0&&!locked&&!data)return null;
 return <div className="order-receipt-action">
  <div><h3>Registrar cobro</h3>{saldo!==undefined&&<small>Saldo pendiente {money(saldo)}</small>}</div>
  <button type="button" className="client-button order-receipt-button" disabled={disabled||locked} onClick={()=>void open()}>{loading?'Consultando…':'Registrar cobro'}</button>
  {error&&!data&&<p className="client-warning" role="alert">{error}</p>}
  {message&&<p className="client-success" role="status">{message}</p>}
  {data&&createPortal(<PaymentReceiptDialog key={method} data={data} medio={method} onMedioChange={setMethod}
   locked={busy||uncertain} busy={busy} uncertain={uncertain} error={error} onConfirm={receive}
   onClose={()=>{if(!locked){setData(null);setError('');}}} onRecover={()=>void execute()}/>,document.body)}
 </div>;
}
