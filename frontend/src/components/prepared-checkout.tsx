"use client";
import {useEffect,useRef,useState,type FormEvent} from 'react';
import {useRouter} from 'next/navigation';
import {ClientJourney} from './client-journey';
import {PrivateFiles} from './private-files';
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
 const[contact,setContact]=useState({receptor:'',telefono:''}),[ack,setAck]=useState(false),[busy,setBusy]=useState(false),[fileLocked,setFileLocked]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState(''),[calculated,setCalculated]=useState(''),[failedKey,setFailedKey]=useState('');
 const pending=useRef<null|{kind:'quote'|'confirm';body:string}>(null),running=useRef(false),ready=useRef(false);
 const current=useRef<Preparation|null>(null);current.current=prep;
 const pref:Preferences|null=mode&&payment?{modalidad:mode,destinoRetiro:mode==='RETIRO_SUCURSAL'?destination||null:null,punto:mode==='RETIRO_PUNTO_ENTREGA'?point||null:null,direccion:mode==='ENVIO_DOMICILIO'?address:null,medioPago:payment}:null;
 const valid=!!pref&&(mode!=='RETIRO_SUCURSAL'||!!destination)&&(mode!=='RETIRO_PUNTO_ENTREGA'||!!point)&&(mode!=='ENVIO_DOMICILIO'||[address.calle,address.numero,...Object.values(address.territorio)].every(s=>s.trim().length>0));
 const prefKey=JSON.stringify(pref),locked=busy||uncertain||fileLocked;
 useNavigationGuard({blocked:locked});
 function received(data:Checkout){setView(data);setPrep(data.preparacion);setAck(false);}
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
  setAck(false);
  const timer=setTimeout(()=>{const p=current.current;if(p)void execute({kind:'quote',body:JSON.stringify({operacion:crypto.randomUUID(),version:p.version,preferencias:JSON.parse(prefKey)})});},500);
  return()=>clearTimeout(timer);
 },[prefKey,valid,locked,calculated,failedKey]);
 // Explicit retries only after a rejected calculation; avoid automatic write loops.
 const quote=view?.cotizacion,revision=view?.revision,summary=quote&&calculated===prefKey?quote.oferta:null;
 const allAccepted=!!prep?.archivos.length&&prep.archivos.every(f=>f.estado==='VALIDO'&&!!f.aceptadaEn);
 const fresh=calculated===prefKey&&!!view?.huella&&!!summary&&!!revision?.franja&&!!quote&&Date.parse(quote.vigenteHasta)>Date.now();
 async function confirm(e:FormEvent){e.preventDefault();if(locked||!prep||!quote||!fresh||!ack||!allAccepted)return;
  await execute({kind:'confirm',body:JSON.stringify({operacion:crypto.randomUUID(),version:prep.version,cotizacion:quote.codigoPublico,huella:view?.huella,condicionesAceptadas:true,contacto:mode==='ENVIO_DOMICILIO'?contact:null})});
 }
 const budget=prep?.presupuesto;
 const destinations=options?.destinosRetiro??[];
 const points=Array.from(new Map((options?.puntos??[]).filter(p=>p.sucursal===options?.casaMatriz?.codigoPublico).map(p=>[p.codigoPublico,p])).values());
 return <div className="client-experience"><ClientJourney part={2}/>
 {!prep&&!error&&<p role="status">Recuperando el resumen de tu trabajo…</p>}
 {prep&&!budget&&<section className="client-card client-empty"><h2>Primero, prepará tu trabajo</h2><p>Agregá los PDF y revisá sus opciones para calcular el presupuesto.</p><a className="client-button" href="/cliente/cotizaciones/nueva">Volver a preparar el trabajo</a></section>}
 {budget&&<><section id="client-budget" className="client-quote-panel"><header><span aria-hidden="true">▤</span><h2>Tu pedido, listo para revisar</h2><p>El precio de tu trabajo ya está calculado. Elegí entrega y pago para conocer el total final.</p></header><div className="client-budget-items">{budget.items.map((item,n)=><article className="client-card" key={item.codigoPublico}><h3>Archivo {n+1} · {item.documento.nombre}</h3><p>{item.trabajo.paginas} páginas · {item.trabajo.copias} copia(s)</p><p>{item.formato} · {item.papel}<br/>{item.trabajo.color==='COLOR'?'Color':'Blanco y negro'} · {item.trabajo.dobleFaz?'Doble faz':'Simple faz'}</p><p>{item.precio.lineas.filter(l=>l.servicio!==item.trabajo.servicio).map(l=>l.nombre).join(' · ')||'Sin servicios adicionales'}</p><strong>{money(item.precio.subtotal)}</strong></article>)}</div><div className="client-total-strip"><span>Subtotal del trabajo</span><strong>{money(budget.subtotal)}</strong><small>Entrega a calcular · todavía sin reserva de cupo</small></div></section>
 <section id="client-delivery" className="client-card"><form onSubmit={e=>{e.preventDefault();if(pref&&prep)void execute({kind:'quote',body:JSON.stringify({operacion:crypto.randomUUID(),version:prep.version,preferencias:pref})});}}><fieldset disabled={locked||!options?.disponible}><div className="client-grid"><section><h2>¿Cómo querés recibirlo?</h2><p>Elegís el destino de tu pedido. La imprenta organiza dónde producirlo.</p><label>Modalidad<select aria-label="Modalidad" required value={mode} onChange={e=>{setAck(false);setMode(e.target.value as Mode);setDestination('');setPoint('');}}><option value="">Elegí una modalidad</option>{options?.modalidades.filter(m=>m!=='RETIRO_PUNTO_ENTREGA'||points.length>0).map(m=><option key={m} value={m}>{modeLabels[m]}</option>)}</select></label>
 {mode==='RETIRO_SUCURSAL'&&<label>Destino de retiro<select aria-label="Destino de retiro" required value={destination} onChange={e=>{setAck(false);setDestination(e.target.value);}}><option value="">Elegí dónde retirar</option>{destinations.map(s=><option key={s.codigoPublico} value={s.codigoPublico}>{s.nombre}{s.codigoPublico===options?.casaMatriz?.codigoPublico?' · Casa matriz':''} · {s.direccion}</option>)}</select></label>}
 {mode==='RETIRO_PUNTO_ENTREGA'&&<label>Punto de entrega<select aria-label="Punto de entrega" required value={point} onChange={e=>{setAck(false);setPoint(e.target.value);}}><option value="">Elegí un punto</option>{points.map(p=><option key={p.codigoPublico} value={p.codigoPublico}>{p.nombre} · {p.direccion}</option>)}</select></label>}
 {mode==='ENVIO_DOMICILIO'&&<div className="client-grid">{([['calle','Calle'],['numero','Número'],['referencias','Referencias (opcional)']] as const).map(([k,label])=><label key={k}>{label}<input required={k!=='referencias'} maxLength={k==='numero'?30:k==='calle'?140:300} value={address[k]} onChange={e=>{setAck(false);setAddress(a=>({...a,[k]:e.target.value}));}}/></label>)}{([['codigoPostal','Código postal'],['localidad','Localidad'],['provincia','Provincia']] as const).map(([k,label])=><label key={k}>{label}<input required maxLength={k==='codigoPostal'?24:120} value={address.territorio[k]} onChange={e=>{setAck(false);setAddress(a=>({...a,territorio:{...a.territorio,[k]:e.target.value}}));}}/></label>)}</div>}
 <p className="client-muted">{mode==='RETIRO_SUCURSAL'&&destination===options?.casaMatriz?.codigoPublico?'Casa matriz: se aplican la preparación y la franja de retiro configuradas.':'Un destino fuera de casa matriz incorpora el traslado o envío configurado. Las fechas son estimadas; el seguimiento mostrará cuándo esté realmente disponible.'}</p></section>
 <section><h2>¿Cómo preferís pagar?</h2><label>Medio de pago<select aria-label="Medio de pago" required value={payment} onChange={e=>{setAck(false);setPayment(e.target.value);}}><option value="">Elegí un medio</option>{options?.medios.map(m=><option key={m} value={m}>{m==='EFECTIVO'?'Efectivo':'Transferencia'}</option>)}</select></label><p>No tenés que acreditar un pago para crear el pedido. Su aprobación y producción sí dependen de las condiciones indicadas.</p><p className="client-muted">Una transferencia informada no es un pago acreditado. Podrás informarla desde tu pedido.</p></section></div>
 <button className="client-button secondary" disabled={!valid}>Actualizar entrega y total</button></fieldset></form></section>
 <section className="client-card client-final-price" aria-live="polite"><h2>Total de tu pedido</h2>{summary?<><dl><div><dt>Impresión y servicios</dt><dd>{money(summary.subtotal)}</dd></div><div><dt>Entrega</dt><dd>{money(summary.costoEntrega)}</dd></div><div className="client-total"><dt>Total final</dt><dd>{money(summary.total)}</dd></div></dl><p><strong>Destino:</strong> {summary.destino}</p><p><strong>Estimación:</strong> {revision?.franja?date(revision.franja.desde)+' – '+date(revision.franja.hasta):date(summary.entrega.disponibleDesde)} · {summary.entrega.zonaHoraria}</p><p className="client-muted">El cupo se reserva al crear el pedido. La disponibilidad real se actualiza en su seguimiento.</p>{summary.entrega.advertencias.map((s,i)=><p key={i} className="client-note">{s}</p>)}<h3>Condiciones de tu pedido</h3><p className="client-muted">Se respetan las condiciones cotizadas y se verifican también los requisitos operativos vigentes antes de crear el pedido.</p><p>{(revision?.condiciones?.revisionHumana??summary.condiciones.revisionHumana)?'Requiere revisión y aprobación humana.':'La aprobación se evalúa con las reglas configuradas.'}</p>{Array.from(new Set([...summary.condiciones.instrucciones,...(revision?.condiciones?.actuales.instrucciones??[])])).map((s,i)=><p key={i}>{s}</p>)}{summary.condiciones.pagoPrevioRequerido!=='0.00'&&<p>Pago previo para habilitar aprobación/producción: {money(summary.condiciones.pagoPrevioRequerido)}</p>}{summary.condiciones.senaRequerida!=='0.00'&&<p>Seña requerida: {money(summary.condiciones.senaRequerida)}</p>}</>:<p>{busy?'Recalculando en el servidor…':'Elegí entrega y pago para calcular el total definitivo.'}</p>}</section>
 <div id="client-review-files"><PrivateFiles prepared quote={id} version={prep.version} onChange={()=>void load().catch(e=>setError(e.message))} onLockChange={setFileLocked}/></div>
 <form id="client-create-order" onSubmit={confirm} className="client-card"><h2>Confirmá tu pedido</h2><p>Una vez creado, el trabajo no se puede editar libremente. Las correcciones y la cancelación siguen sus reglas específicas.</p>{mode==='ENVIO_DOMICILIO'&&<div className="client-grid"><label>Quién recibe<input required maxLength={140} disabled={locked} value={contact.receptor} onChange={e=>setContact({...contact,receptor:e.target.value})}/></label><label>Teléfono<input required maxLength={40} disabled={locked} value={contact.telefono} onChange={e=>setContact({...contact,telefono:e.target.value})}/></label></div>}
 <label className="client-check"><input type="checkbox" checked={ack} disabled={locked||!fresh||!allAccepted} onChange={e=>setAck(e.target.checked)}/>Revisé y acepto los archivos, el total final, el destino, la estimación y las condiciones vigentes.</label>{!allAccepted&&<p className="client-note">Revisá y aceptá todas las páginas de cada PDF antes de confirmar.</p>}
 <div className="client-actions"><a aria-disabled={locked} className="client-button secondary" href={locked?undefined:'/cliente/cotizaciones/nueva'}>← Editar el trabajo</a><button className="client-button" disabled={locked||!fresh||!allAccepted||!ack}>Confirmar / Crear pedido →</button></div></form>
 <ClientContextTour stage={2} ready={!busy&&!!options?.disponible}/></>}
 {busy&&<p role="status">Verificando la operación…</p>}{error&&<p className="client-warning" role="alert">{error}</p>}{uncertain?<button className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar la misma operación</button>:error&&<button className="client-button secondary" disabled={locked} onClick={()=>void load().catch(e=>setError(e.message))}>Actualizar resumen</button>}</div>;
}
