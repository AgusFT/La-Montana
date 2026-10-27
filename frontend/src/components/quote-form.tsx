"use client";
import {useEffect,useRef,useState,type FormEvent} from 'react';
import {useRouter} from 'next/navigation';
import {LegacyQuoteForm} from './legacy-quote-form';
import {ClientJourney} from './client-journey';
import {ClientContextTour} from './client-context-tour';
import {PrivateFiles} from './private-files';
import {useNavigationGuard} from './navigation-boundary';
import {secureMutation,MutationError} from '@/lib/secure-mutation';
import {isOptions,quoteRead,type QuoteOptions,type WorkItem} from '@/lib/quote-types';
import {isFile} from '@/lib/file-types';
import {isPreparation,type Preparation} from '@/lib/prepared-work';

type Row={archivo:string;choice:string;copies:string;double:boolean;ends:string[]};
type Combination=QuoteOptions['combinaciones'][number];
const key=(o:Combination)=>[o.servicio,o.formato,o.papel,o.color].join(':');
export function QuoteForm(props:{replaces?:string;inspectedFile?:string;correction?:{pedido:string;solicitud:string}}){
 return props.replaces?<LegacyQuoteForm {...props}/>:<PrepareWork/>;
}
function PrepareWork(){
 const router=useRouter(),[options,setOptions]=useState<QuoteOptions|null>(null),[prep,setPrep]=useState<Preparation|null>(null),[rows,setRows]=useState<Row[]>([]);
 const [busy,setBusy]=useState(false),[error,setError]=useState(''),[uncertain,setUncertain]=useState(false),[fileLocked,setFileLocked]=useState(false);
 const running=useRef(false),pending=useRef<null|(()=>Promise<void>)>(null),input=useRef<HTMLInputElement>(null);
 const locked=busy||uncertain||fileLocked;
 useNavigationGuard({blocked:locked});
 async function load(initial=false){
  const o=await quoteRead('/api/cliente/cotizaciones/opciones',isOptions);setOptions(o);
  const response=await secureMutation('/api/cliente/preparaciones');
  const p:unknown=await response.json();if(!isPreparation(p))throw new Error('No pudimos recuperar el trabajo preparado.');setPrep(p);
  if(initial)setRows(p.archivos.map(f=>{const t=p.items.find(i=>i.archivo===f.codigoPublico)?.trabajo;return {archivo:f.codigoPublico,choice:t?[t.servicio,t.formato,t.papel,t.color].join(':'):'',copies:String(t?.copias??1),double:t?.dobleFaz??false,ends:t?.terminaciones??[]};}));
 }
 useEffect(()=>{void load(true).catch(e=>setError(e.message));},[]);
 useEffect(()=>{if(!prep?.archivos.some(f=>f.estado==='VALIDANDO'))return;const t=setTimeout(()=>void load().catch(e=>setError(e.message)),2500);return()=>clearTimeout(t);},[prep]);
 async function execute(task?:()=>Promise<void>){
  if(running.current)return;if(task)pending.current=task;if(!pending.current)return;
  running.current=true;setBusy(true);setError('');
  try{await pending.current();pending.current=null;setUncertain(false);}
  catch(e){setError(e instanceof Error?e.message:'No pudimos completar la operación.');const retry=!(e instanceof MutationError)||e.uncertain;setUncertain(retry);if(!retry){pending.current=null;await load().catch(()=>{});}}
  finally{running.current=false;setBusy(false);}
 }
 async function files(list:FileList|null){
  if(!list||locked||!prep)return;const values=Array.from(list);if(prep.archivos.length+values.length>20){setError('Podés preparar hasta 20 PDF.');return;}
  if(values.some(f=>!f.name.toLowerCase().endsWith('.pdf')||f.size===0||f.size>10*1024*1024)){setError('Seleccioná únicamente PDF de hasta 10 MiB por archivo.');return;}
  let index=0;const uploads=values.map(file=>({file,operation:crypto.randomUUID(),id:undefined as string|undefined}));
  await execute(async()=>{
   while(index<uploads.length){const u=uploads[index];
    if(!u.id){const r=await secureMutation(`/api/cliente/preparaciones/${prep.codigoPublico}/archivos`,JSON.stringify({operacion:u.operation,nombre:u.file.name}),'application/json');const a:unknown=await r.json();if(!isFile(a))throw new Error('No se pudo verificar el intento de carga.');u.id=a.codigoPublico;}
    const r=await secureMutation(`/api/cliente/preparaciones/${prep.codigoPublico}/archivos/${u.id}/contenido`,u.file,'application/pdf','PUT');const a:unknown=await r.json();if(!isFile(a))throw new Error('No se pudo verificar el análisis del PDF.');
    setRows(old=>old.some(x=>x.archivo===u.id)?old:[...old,{archivo:u.id!,choice:'',copies:'1',double:false,ends:[]}]);index++;await load();
   }
  });
 }
 function update(id:string,change:Partial<Row>){setRows(old=>old.map(r=>r.archivo===id?{...r,...change}:r));}
 const choices=Array.from(new Map((options?.combinaciones??[]).map(o=>[key(o),o])).values());
 const names=new Map([...(options?.formatos??[]),...(options?.papeles??[]),...(options?.servicios??[])].map(n=>[n.codigoPublico,n.nombre+(n.gramaje?` · ${n.gramaje} g/m²`:'')+(n.terminacion?` · ${n.terminacion}`:'')]));
 function choose(row:Row,index:number,value:string){
  const parts=Array.from({length:4},(_,i)=>row.choice.split(':')[i]??'');parts[index]=value;
  for(let i=0;i<4;i++){if(i===index)continue;const valid=choices.some(o=>{const p=key(o).split(':');return p[index]===value&&parts.every((v,j)=>j===i||!v||v===p[j])&&(!parts[i]||parts[i]===p[i]);});if(!valid)parts[i]='';}
  const selected=choices.find(o=>key(o)===parts.join(':'));
  update(row.archivo,{choice:parts.join(':'),double:!!selected?.dobleFaz&&row.double,ends:row.ends.filter(x=>selected?.terminaciones.includes(x))});
 }
 async function remove(file:string){
  if(!prep||locked)return;const body=JSON.stringify({operacion:crypto.randomUUID()});
  await execute(async()=>{const r=await secureMutation(`/api/cliente/preparaciones/${prep.codigoPublico}/archivos/${file}/quitar`,body,'application/json');const p:unknown=await r.json();if(!isPreparation(p))throw new Error('No se pudo comprobar que el PDF fue quitado.');setPrep(p);setRows(old=>old.filter(x=>x.archivo!==file));});
 }
 async function submit(e:FormEvent){
  e.preventDefault();if(locked||!prep||!options?.configuracion||!options.revisionComercial)return;
  const items=[];for(const f of prep.archivos){const row=rows.find(x=>x.archivo===f.codigoPublico),o=choices.find(x=>key(x)===row?.choice),copies=Number(row?.copies);
   if(f.estado!=='VALIDO'||!f.paginas||!row||!o||!Number.isInteger(copies)||copies<1||copies>10000){setError('Analizá los PDF y completá las opciones de cada archivo.');return;}
   const trabajo:WorkItem={servicio:o.servicio,formato:o.formato,papel:o.papel,color:o.color,paginas:f.paginas,copias:copies,dobleFaz:row.double,terminaciones:row.ends};items.push({archivo:f.codigoPublico,trabajo});
  }
  if(!items.length)return;
  const body=JSON.stringify({operacion:crypto.randomUUID(),version:prep.version,configuracion:options.configuracion,revisionComercial:options.revisionComercial,items});
  await execute(async()=>{const r=await secureMutation(`/api/cliente/preparaciones/${prep.codigoPublico}/presupuesto`,body,'application/json');const p:unknown=await r.json();if(!isPreparation(p)||!p.presupuesto)throw new Error('No se pudo verificar el presupuesto.');router.push(`/cliente/preparaciones/${p.codigoPublico}`);});
 }
 return <div className="client-experience"><ClientJourney part={1}/>
 <p className="client-note">Primero prepará tu trabajo. Después elegís entrega y pago en un único resumen.</p>
 <section id="client-upload" className="client-card client-upload-card"><h2>Subí tus archivos</h2><p>El servidor analiza cada PDF y cuenta sus páginas. No necesitás pagar para cargarlo.</p><div className="client-dropzone" onDragOver={e=>e.preventDefault()} onDrop={e=>{e.preventDefault();void files(e.dataTransfer.files);}}><span aria-hidden="true">⇧</span><strong>Arrastrá tus PDF o seleccioná tus archivos</strong><input ref={input} aria-label="Seleccionar PDF" type="file" accept=".pdf,application/pdf" multiple disabled={locked||!prep||!options?.disponible} onChange={e=>{void files(e.target.files);e.target.value='';}}/><small>Hasta 20 PDF · 10 MiB por archivo · acceso privado</small></div></section>
 {!prep&&!error&&<p role="status">Recuperando tu trabajo…</p>}
 {options&&!options.disponible&&<p className="client-warning">{options.mensaje}</p>}
 <form onSubmit={submit}><fieldset disabled={locked||!options?.disponible} id="client-work-options">
 {prep?.archivos.length===0&&<p className="client-empty">Tu trabajo todavía no tiene archivos. Agregá un PDF para configurar su impresión.</p>}
 {prep?.archivos.map((f,index)=>{const row=rows.find(r=>r.archivo===f.codigoPublico)??{archivo:f.codigoPublico,choice:'',copies:'1',double:false,ends:[]},parts=row.choice.split(':'),selected=choices.find(x=>key(x)===row.choice);
 const lists=[...Array(4)].map((_,n)=>Array.from(new Set(choices.filter(o=>{const p=key(o).split(':');return parts.every((v,j)=>j>=n||!v||v===p[j]);}).map(o=>key(o).split(':')[n]))));
 return <section className="client-card client-work-card" key={f.codigoPublico}><header className="client-item-title"><h2>PDF {index+1} · {f.nombre}</h2><button type="button" className="client-button secondary" disabled={f.estado==='VALIDANDO'} onClick={()=>void remove(f.codigoPublico)}>Quitar / reemplazar</button></header>
 <p className={f.estado==='VALIDO'?'client-success':'client-note'}>{f.estado==='VALIDO'?`✓ PDF analizado · ${f.paginas} páginas · ${((f.bytes??0)/1024).toLocaleString('es-AR',{maximumFractionDigits:1})} KiB`:f.estado==='VALIDANDO'?'Analizando seguridad y páginas…':f.mensaje??'Carga pendiente. Quitalo y volvé a seleccionarlo si no terminó.'}</p>
 <div className="client-work-columns"><section><h3>Detalles de impresión</h3><div className="client-grid">{['Servicio de impresión','Tamaño de hoja','Papel y gramaje','Impresión'].map((label,n)=><label key={label}>{label}<select aria-label={`${label} · PDF ${index+1}`} required disabled={f.estado!=='VALIDO'||n>0&&!parts[n-1]} value={parts[n]??''} onChange={e=>choose(row,n,e.target.value)}><option value="">Elegí una opción</option>{lists[n].map(v=><option key={v} value={v}>{n===3?(v==='COLOR'?'Color':'Blanco y negro'):names.get(v)??v}</option>)}</select></label>)}<label>Cantidad de copias<input aria-label={`Copias · PDF ${index+1}`} type="number" required min={1} max={10000} step={1} value={row.copies} onChange={e=>update(f.codigoPublico,{copies:e.target.value})}/></label><label>Caras<select aria-label={`Caras · PDF ${index+1}`} disabled={!selected} value={row.double?'doble':'simple'} onChange={e=>update(f.codigoPublico,{double:e.target.value==='doble'})}><option value="simple">Simple faz</option>{selected?.dobleFaz&&<option value="doble">Doble faz</option>}</select></label></div></section>
 <section className="client-extras"><h3>Servicios adicionales</h3>{!selected?<p>Completá la impresión para ver los adicionales compatibles.</p>:selected.terminaciones.length===0?<p>No hay adicionales compatibles habilitados para esta selección.</p>:selected.terminaciones.map(id=><label key={id} className="client-check"><input type="checkbox" checked={row.ends.includes(id)} onChange={e=>update(f.codigoPublico,{ends:e.target.checked?[...row.ends,id]:row.ends.filter(x=>x!==id)})}/>{names.get(id)??id}</label>)}<p className="client-muted">Los servicios pertenecen a toda la imprenta. El destino de retiro no limita tu trabajo.</p></section></div></section>;})}
 <div id="client-budget-action" className="client-actions center"><button className="client-button" disabled={!prep?.archivos.length||busy||uncertain}>Revisar presupuesto →</button></div>
 </fieldset></form>
 {prep&&prep.archivos.some(f=>f.estado==='VALIDO')&&<details className="client-prepared-preview"><summary>Ver PDF y aceptar sus páginas (también podés hacerlo en el resumen)</summary><PrivateFiles prepared quote={prep.codigoPublico} version={prep.version} onChange={()=>void load().catch(e=>setError(e.message))} onLockChange={setFileLocked}/></details>}
 {busy&&<p role="status">Guardando y verificando tu trabajo…</p>}{error&&<p className="client-warning" role="alert">{error}</p>}{uncertain?<button type="button" className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar la misma operación</button>:error&&<button className="client-button secondary" disabled={busy} onClick={()=>void load(true).catch(e=>setError(e.message))}>Actualizar datos</button>}
 {prep&&<ClientContextTour stage={1} ready={!!options?.disponible&&!busy}/>}</div>;
}
