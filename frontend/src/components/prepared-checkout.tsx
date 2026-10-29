"use client";
import {useEffect,useRef,useState,type FormEvent} from 'react';
import {useRouter} from 'next/navigation';
import {ClientJourney} from './client-journey';
import {PreparedFileReview} from './prepared-file-review';
import '@/app/client-workspace.css';
import {ClientContextTour} from './client-context-tour';
import {useNavigationGuard} from './navigation-boundary';
import {secureMutation,MutationError} from '@/lib/secure-mutation';
import {isOptions,quoteRead,money,date,modeLabels,type QuoteOptions,type Mode,type Address} from '@/lib/quote-types';
import {isCheckout,isPreparation,type Checkout,type Preparation,type Preferences} from '@/lib/prepared-work';
import {isOrder} from '@/lib/order-types';

const emptyAddress:Address={calle:'',numero:'',referencias:'',territorio:{codigoPostal:'',localidad:'',provincia:''}};
export function PreparedCheckout({id}:{id:string}){
 const router=useRouter(),[prep,setPrep]=useState<Preparation|null>(null),[view,setView]=useState<Checkout|null>(null),[options,setOptions]=useState<QuoteOptions|null>(null);
 const[mode,setMode]=useState<Mode|''>(''),[destination,setDestination]=useState(''),[point,setPoint]=useState(''),[payment,setPayment]=useState(''),[address,setAddress]=useState(emptyAddress);
 const[contact,setContact]=useState({receptor:'',telefono:''}),[busy,setBusy]=useState(false),[fileLocked,setFileLocked]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState(''),[calculated,setCalculated]=useState(''),[failedKey,setFailedKey]=useState('');
 const pending=useRef<null|{kind:'quote'|'confirm';body:string}>(null),running=useRef(false),ready=useRef(false);
 const current=useRef<Preparation|null>(null);current.current=prep;
 const pref:Preferences|null=mode&&payment?{modalidad:mode,destinoRetiro:mode==='RETIRO_SUCURSAL'?destination||null:null,punto:mode==='RETIRO_PUNTO_ENTREGA'?point||null:null,direccion:mode==='ENVIO_DOMICILIO'?address:null,medioPago:payment}:null;
 const valid=!!pref&&(mode!=='RETIRO_SUCURSAL'||!!destination)&&(mode!=='RETIRO_PUNTO_ENTREGA'||!!point)&&(mode!=='ENVIO_DOMICILIO'||[address.calle,address.numero,...Object.values(address.territorio)].every(s=>s.trim().length>0));
 const prefKey=JSON.stringify(pref),locked=busy||uncertain||fileLocked;
 useNavigationGuard({blocked:locked});
 function received(data:Checkout){setView(data);setPrep(data.preparacion);}
 async function load(initial=false){
  const p=await quoteRead(`/api/cliente/preparaciones/${id}`,isPreparation);setPrep(p);
  if(p.pedido){router.replace(`/cliente/pedidos/${p.pedido}`);return;}
  if(!p.presupuesto)return;
  const o=await quoteRead('/api/cliente/cotizaciones/opciones',isOptions);setOptions(o);
  const data=await quoteRead(`/api/cliente/preparaciones/${id}/resumen`,isCheckout);received(data);
  if(initial&&p.preferencias){const f=p.preferencias;setMode(f.modalidad);setDestination(f.destinoRetiro??'');setPoint(f.punto??'');setPayment(f.medioPago);setAddress(f.direccion??emptyAddress);if(data.revision&&data.huella)setCalculated(JSON.stringify(f));}
  ready.current=true;
 }
 useEffect(()=>{void load(true).catch(e=>setError(e.message));},[id]);
 async function execute(command?:{kind:'quote'|'confirm';body:string}){
  if(running.current)return;if(command)pending.current=command;if(!pending.current)return;
  const c=pending.current;running.current=true;setBusy(true);setError('');
  try{const r=await secureMutation(`/api/cliente/preparaciones/${id}/${c.kind==='quote'?'cotizar':'confirmar'}`,c.body,'application/json');const result:unknown=await r.json();
   if(c.kind==='confirm'){if(!isOrder(result))throw new Error('No pudimos comprobar el pedido creado. Recuperá la misma operación.');pending.current=null;setUncertain(false);router.push(`/cliente/pedidos/${result.codigoPublico}`);return;}
   if(!isCheckout(result))throw new Error('No pudimos comprobar el total guardado.');received(result);setCalculated(JSON.stringify(JSON.parse(c.body).preferencias));
   pending.current=null;setUncertain(false);setFailedKey('');
  }catch(e){setError(e instanceof Error?e.message:'No pudimos actualizar el resumen.');const retry=!(e instanceof MutationError)||e.uncertain;setUncertain(retry);if(!retry){pending.current=null;if(c.kind==='quote')setFailedKey(JSON.stringify(JSON.parse(c.body).preferencias));setCalculated('');}}
  finally{running.current=false;setBusy(false);}
 }
 useEffect(()=>{
  if(!ready.current||!valid||!pref||locked||calculated===prefKey||failedKey===prefKey)return;
  const timer=setTimeout(()=>{const p=current.current;if(p)void execute({kind:'quote',body:JSON.stringify({operacion:crypto.randomUUID(),version:p.version,preferencias:JSON.parse(prefKey)})});},500);
  return()=>clearTimeout(timer);
 },[prefKey,valid,locked,calculated,failedKey]);
 // Explicit retries only after a rejected calculation; avoid automatic write loops.
 const quote=view?.cotizacion,revision=view?.revision,summary=quote&&calculated===prefKey?quote.oferta:null;
 const allReady=!!prep?.archivos.length&&prep.archivos.every(f=>f.activo&&!f.revocado&&f.estado==='VALIDO');
 const fresh=calculated===prefKey&&!!view?.huella&&!!summary&&!!revision?.franja&&!!quote&&Date.parse(quote.vigenteHasta)>Date.now();
 async function confirm(e:FormEvent){e.preventDefault();if(locked||!prep||!quote||!fresh||!allReady)return;
  await execute({kind:'confirm',body:JSON.stringify({operacion:crypto.randomUUID(),version:prep.version,cotizacion:quote.codigoPublico,huella:view?.huella,condicionesAceptadas:true,contacto:mode==='ENVIO_DOMICILIO'?contact:null})});
 }
 const budget=prep?.presupuesto;
 const destinations=options?.destinosRetiro??[];
 const points=Array.from(new Map((options?.puntos??[]).filter(p=>p.sucursal===options?.casaMatriz?.codigoPublico).map(p=>[p.codigoPublico,p])).values());
 return <div className="client-experience client-compact-experience"><ClientJourney part={2}/>
  {!prep&&!error&&<p role="status">Recuperando el resumen de tu trabajo…</p>}
  {busy&&<p role="status" className="client-note">Verificando y actualizando tu pedido…</p>}
  {error&&<p className="client-warning" role="alert">{error}</p>}
  {uncertain?<button className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar la misma operación</button>:error&&<button className="client-button secondary" disabled={locked} onClick={()=>void load().catch(e=>setError(e.message))}>Actualizar resumen</button>}
  {prep&&!budget&&<section className="client-card client-empty"><h2>Primero, prepará tu trabajo</h2><p>Agregá los PDF y revisá sus opciones para calcular el presupuesto.</p><a className="client-button" href="/cliente/cotizaciones/nueva">Volver a preparar el trabajo</a></section>}
  {budget&&<><p className="client-compact-intro">Revisá tu trabajo, elegí cómo recibirlo y comprobá el total antes de crear el pedido.</p>
   <div className="client-checkout-layout">
    <div className="workspace-checkout-main">
     <section id="client-budget" className="workspace-panel">
      <header className="workspace-budget-header"><div><h2>Tu trabajo</h2><p>{budget.items.length} archivo(s) · {budget.items.reduce((sum,item)=>sum+item.trabajo.paginas,0)} páginas en los PDF</p></div><a aria-disabled={locked} href={locked?undefined:'/cliente/cotizaciones/nueva'}>Editar trabajo</a></header>
      <div className="workspace-budget-items" tabIndex={budget.items.length>2?0:undefined} aria-label="Detalle de los archivos presupuestados">{budget.items.map((item,n)=><article key={item.codigoPublico}><h3>PDF {n+1} · {item.documento.nombre}</h3><p>{item.trabajo.paginas} páginas · {item.trabajo.copias} copia(s) · {item.trabajo.dobleFaz?'Doble faz':'Simple faz'}</p><p>{item.formato} · {item.papel} · {item.trabajo.color==='COLOR'?'Color':'Blanco y negro'}</p><strong>{money(item.precio.subtotal)}</strong><details><summary>Servicios incluidos</summary><p>{item.precio.lineas.filter(l=>l.servicio!==item.trabajo.servicio).map(l=>l.nombre).join(' · ')||'Sin servicios adicionales'}</p></details></article>)}</div>
      {budget.items.length>2&&<p className="workspace-files-footer">Desplazate dentro del listado para consultar los {budget.items.length} archivos.</p>}
     </section>
     <form id="client-delivery" className="workspace-preferences" onSubmit={e=>{e.preventDefault();if(pref&&prep)void execute({kind:'quote',body:JSON.stringify({operacion:crypto.randomUUID(),version:prep.version,preferencias:pref})});}}>
      <fieldset disabled={locked||!options?.disponible}>
       <section className="workspace-panel"><h2>Entrega</h2>
        <label>Modalidad<select aria-label="Modalidad" required value={mode} onChange={e=>{setMode(e.target.value as Mode);setDestination('');setPoint('');}}><option value="">Elegí una modalidad</option>{options?.modalidades.filter(m=>m!=='RETIRO_PUNTO_ENTREGA'||points.length>0).map(m=><option key={m} value={m}>{modeLabels[m]}</option>)}</select></label>
        {mode==='RETIRO_SUCURSAL'&&<label>Destino de retiro<select aria-label="Destino de retiro" required value={destination} onChange={e=>{setDestination(e.target.value);}}><option value="">Elegí dónde retirar</option>{destinations.map(s=><option key={s.codigoPublico} value={s.codigoPublico}>{s.nombre}{s.codigoPublico===options?.casaMatriz?.codigoPublico?' · Casa matriz':''} · {s.direccion}</option>)}</select></label>}
        {mode==='RETIRO_PUNTO_ENTREGA'&&<label>Punto de entrega<select aria-label="Punto de entrega" required value={point} onChange={e=>{setPoint(e.target.value);}}><option value="">Elegí un punto</option>{points.map(p=><option key={p.codigoPublico} value={p.codigoPublico}>{p.nombre} · {p.direccion}</option>)}</select></label>}
        {mode==='ENVIO_DOMICILIO'&&<div className="workspace-address">{([['calle','Calle'],['numero','Número'],['referencias','Referencias (opcional)']] as const).map(([k,label])=><label key={k}>{label}<input required={k!=='referencias'} maxLength={k==='numero'?30:k==='calle'?140:300} value={address[k]} onChange={e=>{setAddress(a=>({...a,[k]:e.target.value}));}}/></label>)}{([['codigoPostal','Código postal'],['localidad','Localidad'],['provincia','Provincia']] as const).map(([k,label])=><label key={k}>{label}<input required maxLength={k==='codigoPostal'?24:120} value={address.territorio[k]} onChange={e=>{setAddress(a=>({...a,territorio:{...a.territorio,[k]:e.target.value}}));}}/></label>)}</div>}
        <p>{mode==='RETIRO_SUCURSAL'&&destination===options?.casaMatriz?.codigoPublico?'Retiro en casa matriz, según su franja disponible.':'Fuera de casa matriz se agrega el traslado o envío configurado.'} El destino no define dónde se imprime.</p>
       </section>
       <section className="workspace-panel"><h2>Pago</h2>
        <label>Medio de pago<select aria-label="Medio de pago" required value={payment} onChange={e=>{setPayment(e.target.value);}}><option value="">Elegí un medio</option>{options?.medios.map(m=><option key={m} value={m}>{m==='EFECTIVO'?'Efectivo':'Transferencia'}</option>)}</select></label>
        <p>Podés crear el pedido antes de pagar. La aprobación y producción dependen de las condiciones indicadas en el resumen.</p><p>Una transferencia informada no es un pago acreditado. Podrás informarla desde tu pedido.</p>
       </section>
       <div className="workspace-recalculate"><p>{summary?'Entrega y total actualizados. El cupo se reserva al crear el pedido.':busy?'Actualizando entrega y total…':'Elegí entrega y pago. El total se actualizará automáticamente.'}</p><button className="client-button secondary" disabled={!valid}>Actualizar entrega y total</button></div>
      </fieldset>
     </form>
     <section id="client-review-files" className="workspace-panel"><PreparedFileReview quote={id} version={prep.version} total={prep.archivos.length} disabled={locked} onChange={()=>void load().catch(e=>setError(e.message))} onLockChange={setFileLocked}/></section>
    </div>
    <aside className="workspace-checkout-aside" aria-label="Total y confirmación">
     <section className="workspace-panel workspace-price" aria-live="polite"><h2>{summary?'Total de tu pedido':'Subtotal de tu trabajo'}</h2>
      <dl><div><dt>Impresión y servicios</dt><dd>{money(summary?.subtotal??budget.subtotal)}</dd></div><div><dt>Entrega</dt><dd>{summary?money(summary.costoEntrega):'A calcular'}</dd></div>{summary&&<div className="workspace-grand-total"><dt>Total final</dt><dd>{money(summary.total)}</dd></div>}</dl>
      {summary?<><div className="workspace-delivery-summary"><p><strong>Destino:</strong> {summary.destino}</p><p><strong>Estimación:</strong> {revision?.franja?date(revision.franja.desde)+' – '+date(revision.franja.hasta):date(summary.entrega.disponibleDesde)}</p></div>
       <details className="workspace-delivery-notes"><summary>Cómo se estima la entrega</summary>{summary.entrega.advertencias.map((s,i)=><p key={i}>{s}</p>)}<p>Zona horaria: {summary.entrega.zonaHoraria}.</p></details>
       <p>{(revision?.condiciones?.revisionHumana??summary.condiciones.revisionHumana)?'Requiere revisión y aprobación humana.':'La aprobación se evalúa con las reglas configuradas.'}</p>
       {summary.condiciones.pagoPrevioRequerido!=='0.00'&&<p><strong>Pago previo para habilitar aprobación/producción:</strong> {money(summary.condiciones.pagoPrevioRequerido)}</p>}
       {summary.condiciones.senaRequerida!=='0.00'&&<p><strong>Seña requerida:</strong> {money(summary.condiciones.senaRequerida)}</p>}
       <details><summary>Ver condiciones del pedido</summary><p>Se verifican las condiciones cotizadas y los requisitos operativos vigentes antes de crear el pedido.</p>{Array.from(new Set([...summary.condiciones.instrucciones,...(revision?.condiciones?.actuales.instrucciones??[])])).map((s,i)=><p key={i}>{s}</p>)}<p>Las fechas son estimadas. La disponibilidad real se informa en el seguimiento.</p></details>
      </>:<p>{busy?'Recalculando en el servidor…':'Falta elegir entrega y pago para conocer el total final.'}</p>}
     </section>
     <form id="client-create-order" onSubmit={confirm} className="workspace-panel workspace-confirm"><h2>Confirmá tu pedido</h2>
      {mode==='ENVIO_DOMICILIO'&&<div className="workspace-contact"><label>Quién recibe<input required maxLength={140} disabled={locked} value={contact.receptor} onChange={e=>setContact({...contact,receptor:e.target.value})}/></label><label>Teléfono<input required maxLength={40} disabled={locked} value={contact.telefono} onChange={e=>setContact({...contact,telefono:e.target.value})}/></label></div>}
      <p className="workspace-confirm-note">Se imprimirá el contenido de los PDF que cargaste. Podés consultarlos en <a href="#client-review-files">Archivos adjuntos</a>.</p>
      {!allReady&&<p className="client-note"><a href="#client-review-files">Consultá el estado de tus PDF</a>. El análisis debe terminar antes de crear el pedido.</p>}
      <button className="client-button" disabled={locked||!fresh||!allReady}>Crear pedido →</button>
      <p className="workspace-confirm-note">Después de crearlo, los cambios y la cancelación siguen las reglas del pedido.</p>
     </form>
    </aside>
   </div>
   <ClientContextTour stage={2} ready={!busy&&!!options?.disponible}/>
  </>}
 </div>;
}
