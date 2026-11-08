'use client';
import {useRef,useState,type FormEvent} from 'react';
import {useRouter} from 'next/navigation';
import {secureMutation} from '@/lib/secure-mutation';
import {isPreparation} from '@/lib/prepared-work';
import type {Branch} from '@/lib/organization-types';
import styles from './internal-order-customer.module.css';

const customerTypes = [
 {value:'guest', label:'Invitado sin cuenta', description:'Cargá un pedido de mostrador o recibido por WhatsApp.'},
 {value:'registered', label:'Cliente registrado', description:'Buscá su cuenta por correo.'},
] as const;

export function InternalOrderCustomer({branches,initialBranch}:{branches:Pick<Branch,'codigoPublico'|'nombre'|'pausada'>[];initialBranch:string}){
 const router=useRouter();
 const [kind,setKind]=useState<'guest'|'registered'>('guest');
 const [email,setEmail]=useState(''),[name,setName]=useState(''),[branch,setBranch]=useState(branches.some(b=>b.codigoPublico===initialBranch&&!b.pausada)?initialBranch:'');
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
    if(!clean||!branch||!branches.some(b=>b.codigoPublico===branch&&!b.pausada))throw new Error('Completá el nombre del invitado y la sucursal.');
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
 return <section className={`admin-card ${styles.card}`}>
  <h2 id="internal-order-customer-heading">¿Para quién es el pedido?</h2>
  <form onSubmit={submit}>
   <fieldset className={styles.choices} aria-labelledby="internal-order-customer-heading" disabled={busy}>
    {customerTypes.map(({value,label,description})=><label key={value} className={styles.choice}>
     <input type="radio" name="tipo" value={value} checked={kind===value} onChange={()=>{setKind(value);setError('');}}/>
     <span><strong>{label}</strong><small>{description}</small></span>
    </label>)}
   </fieldset>
   <div className={`admin-form ${styles.fields}`}>
    {kind==='guest'?<>
     <label>Nombre del invitado<input required maxLength={100} autoComplete="off" disabled={busy} value={name} onChange={e=>setName(e.target.value)}/></label>
     <label>Sucursal que atiende<select required value={branch} disabled={busy} onChange={e=>setBranch(e.target.value)}>
      <option value="">Elegí una sucursal</option>
      {branches.map(b=><option key={b.codigoPublico} value={b.codigoPublico} disabled={b.pausada}>{b.nombre}{b.pausada?' · Momentáneamente fuera de servicio':''}</option>)}
     </select></label>
    </>:<label>Correo del cliente<input type="email" required maxLength={254} autoComplete="off" disabled={busy} value={email} onChange={e=>setEmail(e.target.value)}/></label>}
   </div>
   <p className={styles.hint}>{kind==='guest'?'Después de cargar los PDF, elegí retiro en esta sucursal o en uno de sus puntos de entrega.':'Al confirmar, el pedido aparecerá en la cuenta del cliente.'}</p>
   {error&&<p className="client-warning" role="alert">{error}</p>}
   <div className={styles.actions}>
    <button type="submit" className="admin-button" disabled={busy||kind==='guest'&&!branches.some(b=>!b.pausada)}>{busy?'Preparando pedido…':'Continuar'}</button>
   </div>
  </form>
 </section>;
}

