"use client";
import {useEffect,useRef,useState,type FormEvent} from 'react';
import {useRouter} from 'next/navigation';
import {LegacyQuoteForm} from './legacy-quote-form';
import {ClientJourney} from './client-journey';
import {ClientContextTour} from './client-context-tour';
import {PreparedFileReview} from './prepared-file-review';
import '@/app/client-workspace.css';
import {useNavigationGuard} from './navigation-boundary';
import {secureMutation,MutationError} from '@/lib/secure-mutation';
import {isOptions,quoteRead,type QuoteOptions,type WorkItem} from '@/lib/quote-types';
import {isFile} from '@/lib/file-types';
import {globalPrintChoices,defaultServiceChoice,printChoiceKey as key} from '@/lib/print-options';
import {isPreparation,type Preparation} from '@/lib/prepared-work';

type Row={archivo:string;choice:string;copies:string;double:boolean;ends:string[]};
export function QuoteForm(props:{replaces?:string;inspectedFile?:string;correction?:{pedido:string;solicitud:string}}){
 return props.replaces?<LegacyQuoteForm {...props}/>:<PrepareWork/>;
}
function PrepareWork(){
 const router=useRouter(),[options,setOptions]=useState<QuoteOptions|null>(null),[prep,setPrep]=useState<Preparation|null>(null),[rows,setRows]=useState<Row[]>([]);
 const [activeFile,setActiveFile]=useState('');
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
    setRows(old=>old.some(x=>x.archivo===u.id)?old:[...old,{archivo:u.id!,choice:'',copies:'1',double:false,ends:[]}]);setActiveFile(u.id!);index++;await load();
   }
  });
 }
 function update(id:string,change:Partial<Row>){setRows(old=>old.map(r=>r.archivo===id?{...r,...change}:r));}
 const choices=globalPrintChoices(options?.combinaciones??[]);
 const singleService=new Set(choices.map(o=>o.servicio)).size===1;
 function effectiveRow(row:Row):Row{return {...row,choice:defaultServiceChoice(row.choice,choices)};}
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
  const items=[];for(const f of prep.archivos){const stored=rows.find(x=>x.archivo===f.codigoPublico),row=stored?effectiveRow(stored):undefined,o=choices.find(x=>key(x)===row?.choice),copies=Number(row?.copies);
   if(f.estado!=='VALIDO'||!f.paginas||!row||!o||!Number.isInteger(copies)||copies<1||copies>10000){setActiveFile(f.codigoPublico);setError(`Completá las opciones de ${f.nombre} y verificá su análisis.`);return;}
   const trabajo:WorkItem={servicio:o.servicio,formato:o.formato,papel:o.papel,color:o.color,paginas:f.paginas,copias:copies,dobleFaz:row.double,terminaciones:row.ends};items.push({archivo:f.codigoPublico,trabajo});
  }
  if(!items.length)return;
  const body=JSON.stringify({operacion:crypto.randomUUID(),version:prep.version,configuracion:options.configuracion,revisionComercial:options.revisionComercial,items});
  await execute(async()=>{const r=await secureMutation(`/api/cliente/preparaciones/${prep.codigoPublico}/presupuesto`,body,'application/json');const p:unknown=await r.json();if(!isPreparation(p)||!p.presupuesto)throw new Error('No se pudo verificar el presupuesto.');router.push(`/cliente/preparaciones/${p.codigoPublico}`);});
 }
 const selectedFile=prep?.archivos.find(f=>f.codigoPublico===activeFile)??prep?.archivos[0];
 const row=effectiveRow(rows.find(r=>r.archivo===selectedFile?.codigoPublico)??{archivo:selectedFile?.codigoPublico??'',choice:'',copies:'1',double:false,ends:[]});
 const parts=row.choice.split(':'),selected=choices.find(x=>key(x)===row.choice);
 const lists=[...Array(4)].map((_,n)=>Array.from(new Set(choices.filter(o=>{const p=key(o).split(':');return parts.every((v,j)=>j>=n||!v||v===p[j]);}).map(o=>key(o).split(':')[n]))));
 function configured(id:string){const stored=rows.find(x=>x.archivo===id),r=stored?effectiveRow(stored):undefined,f=prep?.archivos.find(x=>x.codigoPublico===id),copies=Number(r?.copies);return f?.estado==='VALIDO'&&!!f.paginas&&choices.some(x=>key(x)===r?.choice)&&Number.isInteger(copies)&&copies>=1&&copies<=10000;}
 const configuredCount=prep?.archivos.filter(f=>configured(f.codigoPublico)).length??0;
 return <div className="client-experience client-compact-experience"><ClientJourney part={1}/>
  <p className="client-compact-intro">Agregá tus PDF y configurá cada uno. En el siguiente paso elegís entrega y pago.</p>
  {!prep&&!error&&<p role="status">Recuperando tu trabajo…</p>}
  {options&&!options.disponible&&<p className="client-warning">{options.mensaje}</p>}
  {busy&&<p role="status" className="client-note">Guardando y verificando tu trabajo…</p>}
  {error&&<p className="client-warning" role="alert">{error}</p>}
  {uncertain?<button type="button" className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar la misma operación</button>:error&&<button type="button" className="client-button secondary" disabled={locked} onClick={()=>void load(true).catch(e=>setError(e.message))}>Actualizar datos</button>}
  <form onSubmit={submit} className="client-workspace">
   <section id="client-upload" className="workspace-panel workspace-files" data-has-files={!!prep?.archivos.length}>
    <header className="workspace-heading"><span className="workspace-step" aria-hidden="true">1</span><h2>Tus archivos</h2><small>{prep?.archivos.length??0} / 20</small></header>
    <div className="client-dropzone" onDragOver={e=>e.preventDefault()} onDrop={e=>{e.preventDefault();void files(e.dataTransfer.files);}}>
     <span aria-hidden="true">⇧</span><strong>Arrastrá tus PDF acá</strong>
     <button type="button" className="client-button secondary" disabled={locked||!prep||!options?.disponible} onClick={()=>input.current?.click()}>{prep?.archivos.length?'Agregar PDF':'Seleccionar PDF'}</button>
     <input ref={input} className="workspace-file-input" tabIndex={-1} aria-label="Seleccionar PDF" type="file" accept=".pdf,application/pdf" multiple disabled={locked||!prep||!options?.disponible} onChange={e=>{void files(e.target.files);e.target.value='';}}/>
     <small>Hasta 10 MiB por archivo · acceso privado</small>
    </div>
    {!!prep?.archivos.length&&<ul className="workspace-file-list" aria-label="Archivos del trabajo">{prep.archivos.map((f,index)=><li key={f.codigoPublico}><button type="button" className="workspace-file-choice" aria-pressed={selectedFile?.codigoPublico===f.codigoPublico} aria-label={`Configurar PDF ${index+1}: ${f.nombre}`} disabled={locked} data-complete={configured(f.codigoPublico)} onClick={()=>setActiveFile(f.codigoPublico)}><span className="workspace-file-icon" aria-hidden="true">PDF</span><span><strong title={f.nombre}>{f.nombre}</strong><small>{f.estado==='VALIDO'?`${f.paginas} páginas · ${configured(f.codigoPublico)?'Configurado':'Falta configurar'}`:f.estado==='VALIDANDO'?'Analizando seguridad…':'Revisar archivo'}</small></span><span className="workspace-file-status" aria-hidden="true">{configured(f.codigoPublico)?'✓':index+1}</span></button></li>)}</ul>}
    <p className="workspace-files-footer">{prep?.archivos.length?'Seleccioná un archivo para ajustar sus opciones. Podés agregar varios y cotizarlos juntos.':'El servidor analiza la seguridad y cuenta las páginas de cada PDF. No necesitás pagar para cargarlo.'}</p>
   </section>
   <fieldset disabled={locked||!options?.disponible} id="client-work-options" className="workspace-editor"><legend>Opciones del archivo seleccionado</legend>
    {selectedFile?<><section className="workspace-panel">
     <header className="workspace-heading"><span className="workspace-step" aria-hidden="true">2</span><h2>Detalles de impresión</h2></header>
     <div className="workspace-selected-file"><div><strong>{selectedFile.nombre}</strong><small>{selectedFile.estado==='VALIDO'?`✓ PDF analizado · ${selectedFile.paginas} páginas · ${((selectedFile.bytes??0)/1024).toLocaleString('es-AR',{maximumFractionDigits:1})} KiB`:selectedFile.estado==='VALIDANDO'?'Analizando seguridad y páginas…':selectedFile.mensaje??'Carga pendiente. Quitalo y volvé a seleccionarlo si no terminó.'}</small></div><button type="button" disabled={selectedFile.estado==='VALIDANDO'} onClick={()=>void remove(selectedFile.codigoPublico)}>Quitar</button></div>
     <div className="workspace-print-fields">{['Servicio de impresión','Tamaño de hoja','Papel y gramaje','Impresión'].map((label,n)=>n===0&&singleService?null:<label key={label} data-field={n===0?'service':n===2?'paper':undefined}>{label}<select aria-label={label} required disabled={selectedFile.estado!=='VALIDO'||n>0&&!parts[n-1]} value={parts[n]??''} onChange={e=>choose(row,n,e.target.value)}><option value="">Elegí una opción</option>{lists[n].map(v=><option key={v} value={v}>{n===3?(v==='COLOR'?'Color':'Blanco y negro'):names.get(v)??v}</option>)}</select></label>)}
      <label>Cantidad de copias<input aria-label="Copias" type="number" required min={1} max={10000} step={1} value={row.copies} onChange={e=>update(selectedFile.codigoPublico,{copies:e.target.value})}/></label>
      <label>Caras<select aria-label="Caras" disabled={!selected} value={row.double?'doble':'simple'} onChange={e=>update(selectedFile.codigoPublico,{double:e.target.value==='doble'})}><option value="simple">Simple faz</option>{selected?.dobleFaz&&<option value="doble">Doble faz</option>}</select></label>
     </div>
    </section><section className="workspace-panel workspace-additions">
     <header className="workspace-heading"><span className="workspace-step" aria-hidden="true">3</span><h2>Adicionales</h2>{selected&&<small>{selected.terminaciones.length} disponibles</small>}</header>
     {!selected?<p>Completá la impresión para ver los servicios compatibles.</p>:selected.terminaciones.length===0?<p>No hay adicionales habilitados para este tamaño y papel.</p>:<div className="workspace-additions-list">{selected.terminaciones.map(id=><label key={id} className="client-check"><input type="checkbox" checked={row.ends.includes(id)} onChange={e=>update(selectedFile.codigoPublico,{ends:e.target.checked?[...row.ends,id]:row.ends.filter(x=>x!==id)})}/>{names.get(id)??id}</label>)}</div>}
     <p>Elegí los adicionales que quieras agregar a este archivo.</p>
    </section></>:<section className="workspace-panel workspace-placeholder"><span aria-hidden="true">▤</span><h2>Tu trabajo empieza con un PDF</h2><p>Subí un archivo para elegir impresión, papel, copias y adicionales.</p></section>}
   </fieldset>
   <div id="client-budget-action" className="workspace-next"><div><strong>{prep?.archivos.length?`${configuredCount} de ${prep.archivos.length} archivos configurados`:'Agregá tu primer archivo'}</strong><small>Las opciones se guardan al revisar el presupuesto.</small></div><button className="client-button" disabled={!prep?.archivos.length||locked}>Revisar presupuesto →</button></div>
  </form>
  {prep&&prep.archivos.some(f=>f.estado==='VALIDO')&&<section className="workspace-panel workspace-optional-preview"><PreparedFileReview optional quote={prep.codigoPublico} version={prep.version} total={prep.archivos.length} disabled={locked} onChange={()=>void load().catch(e=>setError(e.message))} onLockChange={setFileLocked}/></section>}
  {prep&&<ClientContextTour stage={1} ready={!!options?.disponible&&!busy}/>}
 </div>;
}
