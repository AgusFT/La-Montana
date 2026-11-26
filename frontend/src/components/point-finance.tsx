'use client';
import {useEffect,useRef,useState,type FormEvent} from 'react';
import {money,date} from '@/lib/quote-types';
import {secureMutation,MutationError} from '@/lib/secure-mutation';
import {useNavigationGuard} from './navigation-boundary';
import type {DeskFinance,FinanceAccount} from '@/lib/point-desk-types';

export type Remittance={codigo:string;punto:string;nombre:string;sucursal:string;destinatario:string;medio:string;importe:string;estado:string;fecha:string;motivo:string|null;referencia:string|null;receptor:string|null;observacion:string|null;adjunto:boolean};
type Settlement={saldo:{pendiente:string;reservado:string;disponible:string};rendiciones:Remittance[];cuentas:FinanceAccount[]};
export async function financeRead<T>(url:string):Promise<T>{
 const response=await fetch(url,{cache:'no-store'});
 if(!response.ok)throw Error((await response.json().catch(()=>null))?.mensaje??'No se pudo consultar Finanzas.');
 return response.json();
}
export function useFinanceCommand(onSaved:()=>void){
 const command=useRef<{path:string;body:string}|null>(null),running=useRef(false);
 const [error,setError]=useState(''),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false);
 useNavigationGuard({blocked:busy||uncertain});
 async function execute(){
  if(!command.current||running.current)return;
  running.current=true;setBusy(true);setError('');
  try{await secureMutation(command.current.path,command.current.body,'application/json');command.current=null;setUncertain(false);onSaved();}
  catch(e){const retry=!(e instanceof MutationError)||e.uncertain;setUncertain(retry);if(!retry)command.current=null;setError(e instanceof Error?e.message:'No se pudo confirmar el resultado.');}
  finally{running.current=false;setBusy(false);}
 }
 function send(path:string,data:object){if(command.current)return;command.current={path,body:JSON.stringify({operacion:crypto.randomUUID(),...data})};void execute();}
 return {send,locked:busy||uncertain,feedback:<>{error&&<p role="alert">{error}</p>}{uncertain&&<button disabled={busy} onClick={execute}>Recuperar registro</button>}</>};
}
export async function proofData(file:File){
 if(file.size>2097152)throw Error('El comprobante supera 2 MiB.');
 return new Promise<string>((resolve,reject)=>{const r=new FileReader();r.onload=()=>resolve(String(r.result).split(',')[1]);r.onerror=()=>reject(Error('No se pudo leer el archivo.'));r.readAsDataURL(file);});
}
export function PointFinance({setLocked}:{setLocked:(locked:boolean)=>void}){
 const [tab,T]=useState('Cobros'),[period,P]=useState('HOY'),[page,G]=useState(0),[revision,R]=useState(0);
 const [fees,F]=useState<DeskFinance|null>(null),[returns,S]=useState<Settlement|null>(null),[account,A]=useState(''),[form,B]=useState(false),[error,E]=useState('');
 const cmd=useFinanceCommand(()=>{B(false);R(x=>x+1);});
 useEffect(()=>{setLocked(cmd.locked);return()=>setLocked(false);},[cmd.locked,setLocked]);
 useEffect(()=>{let live=true;F(null);S(null);E('');
 async function load(){
  if(tab==='Cobros'){const v=await financeRead<DeskFinance>(`/api/punto/finanzas?periodo=${period}&pagina=${page}`);if(live)F(v);}
  else{const v=await financeRead<Settlement>('/api/punto/rendiciones');if(live){S(v);A(v.cuentas[0]?.cuenta??'');}}
 }
 void load().catch(e=>live&&E(e.message));return()=>{live=false;};},[tab,period,page,revision]);
 const selected=returns?.cuentas.find(c=>c.cuenta===account);
 async function submit(e:FormEvent<HTMLFormElement>){e.preventDefault();const f=new FormData(e.currentTarget);const file=f.get('comprobante') as File;
 try{cmd.send('/api/punto/rendiciones',{cuenta:account,importe:String(f.get('importe')).replace(',','.'),referencia:f.get('referencia'),receptor:f.get('receptor'),observacion:f.get('observacion'),comprobante:file?.size?await proofData(file):null});}catch(e){E((e as Error).message);}
 }
 return <section className="point-finance"><h2>Finanzas del punto</h2><nav className="cash-actions">{['Cobros','Rendiciones'].map(t=><button className="client-button secondary" disabled={cmd.locked} aria-pressed={tab===t} key={t} onClick={()=>{T(t);B(false);}}>{t}</button>)}<button disabled={cmd.locked} onClick={()=>R(x=>x+1)}>Actualizar</button></nav>
 {error&&<p role="alert">{error}</p>}{cmd.feedback}
 {tab==='Cobros'?<><label>Período <select value={period} onChange={e=>{P(e.target.value);G(0);}}><option value="HOY">Hoy</option><option value="AYER">Ayer</option><option value="SEMANA">Últimos 7 días</option></select></label>
 {fees&&<><div className="point-money">{Object.entries({'Recibido por el punto':fees.recibido,'Efectivo':fees.efectivo,'Transferencias al punto':fees.transferencia,'Directo a sucursal':fees.directa,'Total gestionado':fees.gestionado,'Pendiente de cobro al retirar':fees.pendiente}).map(([k,v])=><article key={k}>{k}<strong>{money(v)}</strong></article>)}</div>
 {fees.movimientos.map(m=><details key={m.codigo}><summary>PED-{m.pedido} · {money(m.importe)} · {m.estado}</summary><p>{date(m.fecha)} · {m.medio} · {m.destinoCobro??'Destinatario sin determinar'}</p><p>{m.referencia} · Gestionó {m.responsable} {m.verificador&&' · Verificó '+m.verificador}</p>
 {m.estado==='PENDIENTE'&&<label>Adjuntar comprobante PDF<input type="file" accept="application/pdf" disabled={cmd.locked} onChange={async e=>{const file=e.target.files?.[0];if(!file)return;try{cmd.send(`/api/punto/avisos/${m.codigo}/comprobante`,{pdf:await proofData(file)});}catch(e){E((e as Error).message);}}}/></label>}
 {m.puedeVerificar&&<button disabled={cmd.locked} onClick={()=>cmd.send('/api/punto/finanzas/verificar',{cotizacion:m.cotizacion,aviso:m.codigo})}>Confirmar que verifiqué la acreditación</button>}</details>)}
 {!fees.total&&<p>No hay movimientos en este período.</p>}<button disabled={!page} onClick={()=>G(page-1)}>Anteriores</button><button disabled={(page+1)*25>=fees.total} onClick={()=>G(page+1)}>Siguientes</button></>}</>:returns&&<>
 <div className="point-money">{Object.entries({'Pendiente acumulado':returns.saldo.pendiente,'Informado en espera de confirmación':returns.saldo.reservado,'Disponible para informar':returns.saldo.disponible}).map(([k,v])=><article key={k}>{k}<strong>{money(v)}</strong></article>)}</div>
 <button className="client-button" disabled={cmd.locked||!selected||Number(returns.saldo.disponible)<=0} onClick={()=>B(!form)}>Informar rendición</button>
 {form&&selected&&<form className="client-form" onSubmit={submit}><fieldset disabled={cmd.locked}><label>Importe<input name="importe" required inputMode="decimal" pattern="[0-9]+([.,][0-9]{1,2})?"/></label>
 <label>Destinatario y medio<select value={account} onChange={e=>A(e.target.value)}>{returns.cuentas.map(c=><option key={c.cuenta} value={c.cuenta}>{c.nombre} · {c.medio}</option>)}</select></label><p>{selected.instrucciones}</p>
 {selected.medio==='EFECTIVO'?<label>Persona receptora<input name="receptor" required maxLength={140}/></label>:<label>Referencia<input name="referencia" required maxLength={160}/></label>}
 <label>Comprobante opcional<input name="comprobante" type="file" accept=".pdf,.jpg,.png"/></label><label>Observación<input name="observacion" maxLength={300}/></label><button>Informar dinero entregado</button></fieldset></form>}
 {returns.rendiciones.map(r=><details key={r.codigo}><summary>{money(r.importe)} · {r.estado}</summary><p>{date(r.fecha)} · {r.destinatario} · {r.medio} · {r.referencia||r.receptor} {r.motivo}</p></details>)}{!returns.rendiciones.length&&<p>Todavía no informaste rendiciones.</p>}</>}</section>;
}
