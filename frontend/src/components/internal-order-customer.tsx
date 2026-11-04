'use client';
import {useState,type FormEvent} from 'react';
import {useRouter} from 'next/navigation';
import {secureMutation} from '@/lib/secure-mutation';
import {isPreparation} from '@/lib/prepared-work';

export function InternalOrderCustomer(){
 const router=useRouter();
 const [email,setEmail]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState('');
 async function submit(event:FormEvent){
  event.preventDefault();
  if(busy)return;
  setBusy(true);setError('');
  try{
   const response=await secureMutation('/api/operacion/preparaciones',JSON.stringify({correoCliente:email.trim()}),'application/json');
   const prep:unknown=await response.json();
   if(!isPreparation(prep)||!prep.cliente)throw new Error('No pudimos comprobar el cliente del pedido.');
   router.push(`/operacion/preparaciones/${prep.codigoPublico}/trabajo`);
  }catch(e){setError(e instanceof Error?e.message:'No pudimos preparar el pedido.');setBusy(false);}
 }
 return <section className="client-card">
  <h2>¿Para quién es el pedido?</h2>
  <p>Usá el correo de una cuenta de cliente activa. Una vez confirmado, el pedido aparecerá en sus pedidos.</p>
  <form className="client-form" onSubmit={submit}>
   <label>Correo del cliente<input type="email" required maxLength={254} autoComplete="off" disabled={busy} value={email} onChange={e=>setEmail(e.target.value)}/></label>
   {error&&<p className="client-warning" role="alert">{error}</p>}
   <div className="client-actions"><button className="client-button" disabled={busy}>{busy?'Buscando cliente…':'Continuar con este cliente'}</button></div>
  </form>
 </section>;
}
