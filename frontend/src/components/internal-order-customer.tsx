'use client';
import {useRef,useState,type FormEvent} from 'react';
import {useRouter} from 'next/navigation';
import {secureMutation} from '@/lib/secure-mutation';
import {isPreparation} from '@/lib/prepared-work';
import type {Branch} from '@/lib/organization-types';

export function InternalOrderCustomer({branches,initialBranch}:{branches:Pick<Branch,'codigoPublico'|'nombre'>[];initialBranch:string}){
 const router=useRouter();
 const [kind,setKind]=useState<'guest'|'registered'>('guest');
 const [email,setEmail]=useState(''),[name,setName]=useState(''),[branch,setBranch]=useState(initialBranch);
 const [busy,setBusy]=useState(false),[error,setError]=useState('');
 const attempt=useRef<{name:string;branch:string;operation:string}|null>(null);
 async function submit(event:FormEvent){
  event.preventDefault();
  if(busy)return;
  setBusy(true);setError('');
  try{
   let body;
   if(kind==='guest'){
    const clean=name.trim();
    if(!clean||!branch)throw new Error('Completá el nombre del invitado y la sucursal.');
    if(attempt.current?.name!==clean||attempt.current.branch!==branch)
     attempt.current={name:clean,branch,operation:crypto.randomUUID()};
    body={nombreInvitado:clean,sucursal:branch,operacion:attempt.current.operation};
   }else body={correoCliente:email.trim()};
   const response=await secureMutation('/api/operacion/preparaciones',JSON.stringify(body),'application/json');
   const prep:unknown=await response.json();
   if(!isPreparation(prep)||!prep.cliente)throw new Error('No pudimos comprobar el cliente del pedido.');
   router.push(prep.pedido?`/operacion/pedidos/${prep.pedido}`:`/operacion/preparaciones/${prep.codigoPublico}/trabajo`);
  }catch(e){setError(e instanceof Error?e.message:'No pudimos preparar el pedido.');setBusy(false);}
 }
 return <section className="client-card">
  <h2>¿Para quién es el pedido?</h2>
  <form className="client-form" onSubmit={submit}>
   <fieldset disabled={busy}>
    <legend>Tipo de cliente</legend>
    <label className="client-check"><input type="radio" name="tipo" checked={kind==='guest'} onChange={()=>{setKind('guest');setError('');}}/> Invitado de mostrador</label>
    <label className="client-check"><input type="radio" name="tipo" checked={kind==='registered'} onChange={()=>{setKind('registered');setError('');}}/> Cliente registrado</label>
   </fieldset>
   {kind==='guest'?<>
    <label>Nombre del invitado<input required maxLength={100} autoComplete="off" disabled={busy} value={name} onChange={e=>setName(e.target.value)}/></label>
    <label>Sucursal que atiende<select required value={branch} disabled={busy} onChange={e=>setBranch(e.target.value)}>
     <option value="">Elegí una sucursal</option>
     {branches.map(b=><option key={b.codigoPublico} value={b.codigoPublico}>{b.nombre}</option>)}
    </select></label>
    <p>Alcanza con el nombre. El trabajo se entrega en esta sucursal.</p>
   </>:<>
    <label>Correo del cliente<input type="email" required maxLength={254} autoComplete="off" disabled={busy} value={email} onChange={e=>setEmail(e.target.value)}/></label>
    <p>Al confirmar, el pedido aparecerá en la cuenta del cliente.</p>
   </>}
   {error&&<p className="client-warning" role="alert">{error}</p>}
   <div className="client-actions"><button className="client-button" disabled={busy||kind==='guest'&&!branches.length}>{busy?'Preparando pedido…':'Continuar'}</button></div>
  </form>
 </section>;
}
