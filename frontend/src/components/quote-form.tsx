"use client";
import {useEffect,useRef,useState,type FormEvent} from 'react';
import {useRouter} from 'next/navigation';
import {useTutorial} from "./tutorial-context";
import {useRoleTutorial} from "./role-tutorial-context";
import {TutorialPreparationNotice} from "./tutorial-preparation-notice";
import {LegacyQuoteForm} from './legacy-quote-form';
import {ClientJourney} from './client-journey';
import {ClientContextTour} from './client-context-tour';
import {PreparedFileReview} from './prepared-file-review';
import '@/app/client-workspace.css';
import {useNavigationGuard} from './navigation-boundary';
import {secureMutation,MutationError} from '@/lib/secure-mutation';
import {isOptions,quoteRead,type QuoteOptions,type WorkItem} from '@/lib/quote-types';
import {isFile} from '@/lib/file-types';
import {isOpenTutorialPreparation,isPreparation,type Preparation} from '@/lib/prepared-work';

type Row={archivo:string;choice:string;copies:string;double:boolean;ends:string[]};
type Combination=QuoteOptions['combinaciones'][number];
const key=(o:Combination)=>[o.servicio,o.formato,o.papel,o.color].join(':');
export function QuoteForm(props:{replaces?:string;inspectedFile?:string;correction?:{pedido:string;solicitud:string}}){
 return props.replaces?<LegacyQuoteForm {...props}/>:<PrepareWork/>;
}
function PrepareWork(){
 const tutorial=useTutorial(), restored=useRef('');
 const clientGuide=useRoleTutorial('cliente');
 const router=useRouter(),[options,setOptions]=useState<QuoteOptions|null>(null),[prep,setPrep]=useState<Preparation|null>(null),[rows,setRows]=useState<Row[]>([]);
 const [activeFile,setActiveFile]=useState('');
 const [busy,setBusy]=useState(false),[error,setError]=useState(''),[uncertain,setUncertain]=useState(false),[fileLocked,setFileLocked]=useState(false);
 const running=useRef(false),pending=useRef<null|(()=>Promise<void>)>(null),input=useRef<HTMLInputElement>(null);
 const [refreshing,setRefreshing]=useState(false);
 const readSequence=useRef(0);
 const progress=tutorial?.state;
 const practicing=tutorial?.open && progress?.estado==='ACTIVO' && ['archivo','presupuesto'].includes(progress.paso);
 const attached=isOpenTutorialPreparation(prep,progress);
 const clientPractice=clientGuide?.lesson==='archivo'||clientGuide?.lesson==='presupuesto'||clientGuide?.lesson==='decision';
 const clientAttached=!!prep&&clientGuide?.draft.preparacion===prep.codigoPublico;
 const needsPracticeConsent=!!prep&&(
   (practicing&&!attached&&!!prep.archivos.length)||
   (clientPractice&&!clientAttached&&(attached||!!prep.archivos.length||!!prep.presupuesto))
 );
 const operationLocked=busy||refreshing||uncertain||fileLocked;
 const locked=operationLocked||needsPracticeConsent;
 useNavigationGuard({blocked:operationLocked});
 async function load(){
  const sequence=++readSequence.current;
  const o=await quoteRead('/api/cliente/cotizaciones/opciones',isOptions);
  const response=await secureMutation('/api/cliente/preparaciones');
  const p:unknown=await response.json();if(!isPreparation(p))throw new Error('No pudimos recuperar el trabajo preparado.');
  if(sequence!==readSequence.current)return;
  setOptions(o);setPrep(p);
  setRows(previous=>p.archivos.map(file=>{
   const archivo=file.codigoPublico;
   const draft=previous.find(row=>row.archivo===archivo);
   if(draft)return draft;
   const work=p.items.find(item=>item.archivo===archivo)?.trabajo;
   return {archivo,choice:work?[work.servicio,work.formato,work.papel,work.color].join(':'):'',
    copies:String(work?.copias??1),double:work?.dobleFaz??false,ends:work?.terminaciones??[]};
  }));
 }
 const draftOwner=clientPractice&&clientAttached?'cliente':practicing&&attached?'completo':'';
 const savedFileDraft=draftOwner==='cliente'?clientGuide?.draft:progress?.borradores.archivo;
 useEffect(()=>{
   if(!draftOwner||!prep){restored.current='';return;}
   const key=draftOwner+':'+prep.codigoPublico;
   if(restored.current===key)return;
   const saved=savedFileDraft;
   restored.current=key;
   if(saved?.archivoActivo && prep.archivos.some(f=>f.codigoPublico===saved.archivoActivo))setActiveFile(saved.archivoActivo);
   try{
     const work:unknown=JSON.parse(saved?.opciones??'[]');
     if(Array.isArray(work))setRows(old=>old.map(row=>{
       const value=work.find(item=>item?.archivo===row.archivo);
       return value && typeof value.choice==='string' && typeof value.copies==='string' && typeof value.double==='boolean' && Array.isArray(value.ends) && value.ends.every((x:unknown)=>typeof x==='string') ? {...row,choice:value.choice,copies:value.copies,double:value.double,ends:value.ends} : row;
     }));
   }catch{/* El documento y las opciones confirmadas se cargan desde la preparación. */}
 },[draftOwner,prep,savedFileDraft]);
 async function linkPractice(){
   if(clientPractice&&clientGuide&&prep&&!clientAttached){
     const preferences=prep.preferencias, address=preferences?.direccion;
     await clientGuide.saveDraft({preparacion:prep.codigoPublico,pedido:'',archivoActivo:'',opciones:'[]',
       modalidad:preferences?.modalidad??'',destino:preferences?.destinoRetiro??'',medio:preferences?.medioPago??'',
       calle:address?.calle??'',numero:address?.numero??'',referencias:address?.referencias??'',
       codigoPostal:address?.territorio.codigoPostal??'',localidad:address?.territorio.localidad??'',provincia:address?.territorio.provincia??'',
       receptor:'',telefono:''});
   }else if(tutorial && prep && practicing && !attached){
     const linked=await tutorial.command({accion:"vincular",preparacion:prep.codigoPublico});
     if(linked.preparacion!==prep.codigoPublico)throw Error("No se pudo asociar este trabajo al tutorial.");
   }
 }
 function rememberFile(fields:Record<string,string>){
   if(clientPractice&&clientAttached)void clientGuide?.saveDraft(fields).catch(e=>setError(e instanceof Error?e.message:'No pudimos guardar el borrador de práctica.'));
   if(practicing&&attached)tutorial?.draft('archivo',fields);
 }
 async function refresh(){
  if(running.current)return;
  running.current=true;setRefreshing(true);
  try{await load();setError('');}
  catch(e){setError(e instanceof Error?e.message:'No pudimos actualizar los datos. Volvé a intentar.');}
  finally{running.current=false;setRefreshing(false);}
 }
 useEffect(()=>{void load().catch(e=>setError(e.message));},[]);
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
   await linkPractice();
   while(index<uploads.length){const u=uploads[index];
    if(!u.id){const r=await secureMutation(`/api/cliente/preparaciones/${prep.codigoPublico}/archivos`,JSON.stringify({operacion:u.operation,nombre:u.file.name}),'application/json');const a:unknown=await r.json();if(!isFile(a))throw new Error('No se pudo verificar el intento de carga.');u.id=a.codigoPublico;}
    const r=await secureMutation(`/api/cliente/preparaciones/${prep.codigoPublico}/archivos/${u.id}/contenido`,u.file,'application/pdf','PUT');const a:unknown=await r.json();if(!isFile(a))throw new Error('No se pudo verificar el análisis del PDF.');
    setActiveFile(u.id);index++;
   }
   await load();
  });
 }
 function update(id:string,change:Partial<Row>){
   const next=rows.map(row=>row.archivo===id?{...row,...change}:row);setRows(next);
   rememberFile({archivoActivo:id,opciones:JSON.stringify(next)});
 }
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
  await execute(async()=>{const r=await secureMutation(`/api/cliente/preparaciones/${prep.codigoPublico}/archivos/${file}/quitar`,body,'application/json');const p:unknown=await r.json();if(!isPreparation(p))throw new Error('No se pudo comprobar que el PDF fue quitado.');readSequence.current++;setPrep(p);setRows(old=>old.filter(x=>x.archivo!==file));});
 }
 async function submit(e:FormEvent){
  e.preventDefault();if(locked||!prep||!options?.configuracion||!options.revisionComercial)return;
  if(practicing&&!attached){try{await linkPractice();}catch(e){setError(e instanceof Error?e.message:'No pudimos vincular el trabajo.');return;}}
  if(practicing){try{await tutorial?.flush();}catch{setError('No se guardó el borrador del tutorial. Reintentá antes de continuar.');return;}}
  const items=[];for(const f of prep.archivos){const row=rows.find(x=>x.archivo===f.codigoPublico),o=choices.find(x=>key(x)===row?.choice),copies=Number(row?.copies);
   if(f.estado!=='VALIDO'||!f.paginas||!row||!o||!Number.isInteger(copies)||copies<1||copies>10000){setActiveFile(f.codigoPublico);setError(`Completá las opciones de ${f.nombre} y verificá su análisis.`);return;}
   const trabajo:WorkItem={servicio:o.servicio,formato:o.formato,papel:o.papel,color:o.color,paginas:f.paginas,copias:copies,dobleFaz:row.double,terminaciones:row.ends};items.push({archivo:f.codigoPublico,trabajo});
  }
  if(!items.length)return;
  const body=JSON.stringify({operacion:crypto.randomUUID(),version:prep.version,configuracion:options.configuracion,revisionComercial:options.revisionComercial,items});
  await execute(async()=>{const r=await secureMutation(`/api/cliente/preparaciones/${prep.codigoPublico}/presupuesto`,body,'application/json');const p:unknown=await r.json();if(!isPreparation(p)||!p.presupuesto)throw new Error('No se pudo verificar el presupuesto.');
   if(clientPractice&&clientGuide){await clientGuide.goTo('presupuesto');return;}
   router.push(`/cliente/preparaciones/${p.codigoPublico}`);
  });
 }
 const selectedFile=prep?.archivos.find(f=>f.codigoPublico===activeFile)??prep?.archivos[0];
 const row=rows.find(r=>r.archivo===selectedFile?.codigoPublico)??{archivo:selectedFile?.codigoPublico??'',choice:'',copies:'1',double:false,ends:[]};
 const parts=row.choice.split(':'),selected=choices.find(x=>key(x)===row.choice);
 const lists=[...Array(4)].map((_,n)=>Array.from(new Set(choices.filter(o=>{const p=key(o).split(':');return parts.every((v,j)=>j>=n||!v||v===p[j]);}).map(o=>key(o).split(':')[n]))));
 function configured(id:string){const r=rows.find(x=>x.archivo===id),f=prep?.archivos.find(x=>x.codigoPublico===id),copies=Number(r?.copies);return f?.estado==='VALIDO'&&!!f.paginas&&choices.some(x=>key(x)===r?.choice)&&Number.isInteger(copies)&&copies>=1&&copies<=10000;}
 const configuredCount=prep?.archivos.filter(f=>configured(f.codigoPublico)).length??0;
 return <div className="client-experience client-compact-experience"><ClientJourney part={1}/>
  {prep && !tutorial?.open && !needsPracticeConsent && attached && <TutorialPreparationNotice preparation={prep}/>}
  {prep && needsPracticeConsent && <TutorialPreparationNotice preparation={prep} busy={operationLocked} onLink={()=>void execute(linkPractice)}/>}
  <p className="client-compact-intro">Agregá tus PDF y configurá cada uno. En el siguiente paso elegís entrega y pago.</p>
  {!prep&&!error&&<p role="status">Recuperando tu trabajo…</p>}
  {options&&!options.disponible&&<p className="client-warning">{options.mensaje}</p>}
  {busy&&<p role="status" className="client-note">Guardando y verificando tu trabajo…</p>}
  {error&&<p className="client-warning" role="alert">{error}</p>}
  {uncertain?<button type="button" className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar la misma operación</button>:error&&<button type="button" className="client-button secondary" disabled={locked} onClick={()=>void refresh()}>{refreshing?'Actualizando…':'Actualizar datos'}</button>}
  <form onSubmit={submit} className="client-workspace">
   <section id="client-upload" className="workspace-panel workspace-files" data-has-files={!!prep?.archivos.length} data-practica-pdf={clientAttached&&prep?.archivos.some(file=>file.estado==='VALIDO')?'listo':undefined}>
    <header className="workspace-heading"><span className="workspace-step" aria-hidden="true">1</span><h2>Tus archivos</h2><small>{prep?.archivos.length??0} / 20</small></header>
    <div className="client-dropzone" onDragOver={e=>e.preventDefault()} onDrop={e=>{e.preventDefault();void files(e.dataTransfer.files);}}>
     <span aria-hidden="true">⇧</span><strong>Arrastrá tus PDF acá</strong>
     <button type="button" className="client-button secondary" disabled={locked||!prep||!options?.disponible} onClick={()=>input.current?.click()}>{prep?.archivos.length?'Agregar PDF':'Seleccionar PDF'}</button>
     <input ref={input} className="workspace-file-input" tabIndex={-1} aria-label="Seleccionar PDF" type="file" accept=".pdf,application/pdf" multiple disabled={locked||!prep||!options?.disponible} onChange={e=>{void files(e.target.files);e.target.value='';}}/>
     <small>Hasta 10 MiB por archivo · acceso privado</small>
    </div>
    {!!prep?.archivos.length&&<ul className="workspace-file-list" aria-label="Archivos del trabajo">{prep.archivos.map((f,index)=><li key={f.codigoPublico}><button type="button" className="workspace-file-choice" aria-pressed={selectedFile?.codigoPublico===f.codigoPublico} aria-label={`Configurar PDF ${index+1}: ${f.nombre}`} disabled={locked} data-complete={configured(f.codigoPublico)} onClick={()=>{setActiveFile(f.codigoPublico);rememberFile({archivoActivo:f.codigoPublico});}}><span className="workspace-file-icon" aria-hidden="true">PDF</span><span><strong title={f.nombre}>{f.nombre}</strong><small>{f.estado==='VALIDO'?`${f.paginas} páginas · ${configured(f.codigoPublico)?'Configurado':'Falta configurar'}`:f.estado==='VALIDANDO'?'Analizando seguridad…':'Revisar archivo'}</small></span><span className="workspace-file-status" aria-hidden="true">{configured(f.codigoPublico)?'✓':index+1}</span></button></li>)}</ul>}
    <p className="workspace-files-footer">{prep?.archivos.length?'Seleccioná un archivo para ajustar sus opciones. Podés agregar varios y cotizarlos juntos.':'El servidor analiza la seguridad y cuenta las páginas de cada PDF. No necesitás pagar para cargarlo.'}</p>
   </section>
   <fieldset disabled={locked||!options?.disponible} id="client-work-options" className="workspace-editor"><legend>Opciones del archivo seleccionado</legend>
    {selectedFile?<><section className="workspace-panel">
     <header className="workspace-heading"><span className="workspace-step" aria-hidden="true">2</span><h2>Detalles de impresión</h2></header>
     <div className="workspace-selected-file"><div><strong>{selectedFile.nombre}</strong><small>{selectedFile.estado==='VALIDO'?`✓ PDF analizado · ${selectedFile.paginas} páginas · ${((selectedFile.bytes??0)/1024).toLocaleString('es-AR',{maximumFractionDigits:1})} KiB`:selectedFile.estado==='VALIDANDO'?'Analizando seguridad y páginas…':selectedFile.mensaje??'Carga pendiente. Quitalo y volvé a seleccionarlo si no terminó.'}</small></div><button type="button" disabled={selectedFile.estado==='VALIDANDO'} onClick={()=>void remove(selectedFile.codigoPublico)}>Quitar</button></div>
     <div className="workspace-print-fields">{['Servicio de impresión','Tamaño de hoja','Papel y gramaje','Impresión'].map((label,n)=><label key={label}>{label}<select aria-label={label} required disabled={selectedFile.estado!=='VALIDO'||n>0&&!parts[n-1]} value={parts[n]??''} onChange={e=>choose(row,n,e.target.value)}><option value="">Elegí una opción</option>{lists[n].map(v=><option key={v} value={v}>{n===3?(v==='COLOR'?'Color':'Blanco y negro'):names.get(v)??v}</option>)}</select></label>)}
      <label>Cantidad de copias<input aria-label="Copias" type="number" required min={1} max={10000} step={1} value={row.copies} onChange={e=>update(selectedFile.codigoPublico,{copies:e.target.value})}/></label>
      <label>Caras<select aria-label="Caras" disabled={!selected} value={row.double?'doble':'simple'} onChange={e=>update(selectedFile.codigoPublico,{double:e.target.value==='doble'})}><option value="simple">Simple faz</option>{selected?.dobleFaz&&<option value="doble">Doble faz</option>}</select></label>
     </div>
    </section><section className="workspace-panel workspace-additions">
     <header className="workspace-heading"><span className="workspace-step" aria-hidden="true">3</span><h2>Adicionales</h2></header>
     {!selected?<p>Completá la impresión para ver los servicios compatibles.</p>:selected.terminaciones.length===0?<p>No hay adicionales compatibles para esta selección.</p>:<div className="workspace-additions-list">{selected.terminaciones.map(id=><label key={id} className="client-check"><input type="checkbox" checked={row.ends.includes(id)} onChange={e=>update(selectedFile.codigoPublico,{ends:e.target.checked?[...row.ends,id]:row.ends.filter(x=>x!==id)})}/>{names.get(id)??id}</label>)}</div>}
     <p>Opcionales para este archivo. El destino de retiro no limita los servicios de la imprenta.</p>
    </section></>:<section className="workspace-panel workspace-placeholder"><span aria-hidden="true">▤</span><h2>Tu trabajo empieza con un PDF</h2><p>Subí un archivo para elegir impresión, papel, copias y adicionales.</p></section>}
   </fieldset>
   <div id="client-budget-action" className="workspace-next"><div><strong>{prep?.archivos.length?`${configuredCount} de ${prep.archivos.length} archivos configurados`:'Agregá tu primer archivo'}</strong><small>Las opciones se guardan al revisar el presupuesto.</small></div><button className="client-button" disabled={!prep?.archivos.length||locked}>Revisar presupuesto →</button></div>
  </form>
  {prep&&prep.archivos.some(f=>f.estado==='VALIDO')&&<section className="workspace-panel workspace-optional-preview"><PreparedFileReview optional quote={prep.codigoPublico} version={prep.version} total={prep.archivos.length} accepted={prep.archivos.filter(f=>!!f.aceptadaEn).length} disabled={locked} onChange={()=>void load().catch(e=>setError(e.message))} onLockChange={setFileLocked}/></section>}
  {prep&&<ClientContextTour stage={1} ready={!!options?.disponible&&!busy}/>}
 </div>;
}
