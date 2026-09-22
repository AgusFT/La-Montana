"use client";
import {useEffect,useRef,useState} from "react";
import {secureMutation,MutationError} from "@/lib/secure-mutation";
import {quoteRead,date} from "@/lib/quote-types";
import {isFile,isFileView,type PrivateFile,type FileView} from "@/lib/file-types";

export function PrivateFiles({quote,version,internal=false,refresh=0}:{quote:string;version:number;internal?:boolean;refresh?:number}){
 const stateNames:Record<string,string>={PENDIENTE:"Pendiente de carga",VALIDANDO:"Analizando",VALIDO:"PDF válido",REQUIERE_COTIZACION:"Necesita una nueva cotización",RECHAZADO:"Archivo rechazado",FALLIDO:"Carga interrumpida"};
 const base=`/api/${internal?"operacion":"cliente"}/cotizaciones/${quote}/archivos`;
 const[view,setView]=useState<FileView|null>(null),[error,setError]=useState(""),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[selected,setSelected]=useState<Record<string,File>>({}),[preview,setPreview]=useState<PrivateFile|null>(null),[page,setPage]=useState(1),[visited,setVisited]=useState<Set<number>>(new Set()),[ack,setAck]=useState(false),[imageError,setImageError]=useState(false);
 const running=useRef(false),command=useRef<{kind:"upload";item:string;file:File;body:string;id?:string}|{kind:"accept";id:string;body:string}|null>(null);
 const locked=busy||uncertain;
 async function load(){try{const data=await quoteRead(base,isFileView);setView(data);setPreview(current=>current?data.items.flatMap(i=>i.archivos).find(f=>f.codigoPublico===current.codigoPublico)??current:null);}catch(e){setError(e instanceof Error?e.message:"No pudimos consultar los archivos.");}}
 useEffect(()=>{void load();},[quote,version,internal,refresh]);
 useEffect(()=>{if(!view?.items.some(i=>i.archivos.some(f=>f.estado==="VALIDANDO")))return;const timer=setTimeout(()=>void load(),3000);return()=>clearTimeout(timer);},[view]);
 function open(file:PrivateFile){setPreview(file);setPage(1);setVisited(new Set());setAck(false);setImageError(false);}
 async function execute(){
  if(running.current||!command.current)return;running.current=true;setBusy(true);setError("");const c=command.current;
  try{
   let result:unknown;
   if(c.kind==="upload"){
    if(!c.id){const started=await secureMutation(base,c.body,"application/json");const data:unknown=await started.json();if(!isFile(data))throw new Error("No pudimos verificar el intento de carga.");c.id=data.codigoPublico;}
    const response=await secureMutation(`${base}/${c.id}/contenido`,c.file,"application/pdf","PUT");result=await response.json();
   }else{const response=await secureMutation(`${base}/${c.id}/aceptar`,c.body,"application/json");result=await response.json();}
   if(!isFile(result))throw new Error("No pudimos verificar el resultado guardado.");
   if(c.kind==="upload")setSelected(old=>{const copy={...old};delete copy[c.item];return copy;});
   setUncertain(false);command.current=null;
   if(c.kind==="accept")setPreview(result);else if(result.estado==="VALIDO"||result.estado==="REQUIERE_COTIZACION")open(result);
   await load();
  }catch(e){setError(e instanceof Error?e.message:"No pudimos completar la carga.");if(e instanceof MutationError&&!e.uncertain){command.current=null;setUncertain(false);await load();}else setUncertain(true);}
  finally{running.current=false;setBusy(false);}
 }
 function upload(item:string){if(locked||!view?.carga.habilitada)return;const file=selected[item];if(!file)return;if(file.size===0||file.size>10*1024*1024||!file.name.toLowerCase().endsWith(".pdf")){setError("Seleccioná un PDF de hasta 10 MiB.");return;}
  const pending=view.items.find(i=>i.codigoPublico===item)?.archivos.find(f=>f.estado==="PENDIENTE"&&Date.now()<Date.parse(f.cargarHasta));
  command.current={kind:"upload",item,file,body:JSON.stringify({operacion:crypto.randomUUID(),item,version}),id:pending?.codigoPublico};void execute();
 }
 function accept(){if(locked||!preview?.sha256||!ack||visited.size!==preview.paginas)return;command.current={kind:"accept",id:preview.codigoPublico,body:JSON.stringify({operacion:crypto.randomUUID(),sha256:preview.sha256})};void execute();}
 const ready=preview&&(preview.estado==="VALIDO"||preview.estado==="REQUIERE_COTIZACION"),pages=preview?.paginas??0;
 return <section className="client-card private-files"><h2>PDF privados y vista previa</h2><p>El sistema analiza el contenido y genera las páginas de vista previa. Aceptarlas no confirma todavía el pedido ni registra un pago.</p>
 {view?<><p className={view.carga.habilitada?"client-note":"client-warning"}>{view.carga.motivo}</p>{view.items.map((item,index)=><section className="private-file-item" key={item.codigoPublico}><h3>Archivo {index+1}: {item.nombre}</h3>
 {!internal&&view.carga.habilitada&&<div className="client-form"><label>Seleccionar PDF para el archivo {index+1}<input key={item.archivos.map(f=>f.codigoPublico+f.estado).join(":")} type="file" accept="application/pdf,.pdf" disabled={locked} onChange={e=>{const f=e.target.files?.[0];if(f)setSelected(old=>({...old,[item.codigoPublico]:f}));}}/></label><button type="button" className="client-button" disabled={locked||!selected[item.codigoPublico]||item.archivos.some(f=>f.estado==="VALIDANDO")} onClick={()=>upload(item.codigoPublico)}>{busy?"Procesando…":"Enviar PDF para analizar"}</button></div>}
 {item.archivos.length===0?<p className="client-muted">Todavía no hay un PDF recibido para este ítem.</p>:<ul className="private-file-history">{item.archivos.map(f=><li key={f.codigoPublico}><strong>{stateNames[f.estado]??f.estado}{f.activo?" · versión actual":""}</strong><span>{date(f.creadoEn)}{f.paginas?` · ${f.paginas} páginas`:""}</span><p>{f.mensaje??(f.estado==="VALIDANDO"?"Analizando el archivo; podés recuperar el estado al recargar.":"Intento preparado. Seleccioná el PDF y envialo antes del vencimiento.")}</p>{f.aceptadaEn&&<p className="client-success">Vista previa aceptada el {date(f.aceptadaEn)}.</p>}{["VALIDO","REQUIERE_COTIZACION"].includes(f.estado)&&<div className="client-actions"><button className="client-button secondary" disabled={locked} onClick={()=>open(f)}>Ver páginas{f.activo?" actuales":""}</button><a href={locked?undefined:`${base}/${f.codigoPublico}/original`} className="client-button secondary">Descargar PDF inspeccionado</a>{f.estado==="REQUIERE_COTIZACION"&&!internal&&<a className="client-button" href={locked?undefined:`/cliente/cotizaciones/nueva?reemplaza=${quote}&archivo=${f.codigoPublico}`}>Recotizar con datos inspeccionados</a>}</div>}</li>)}</ul>}
 </section>)}</>:!error&&<p role="status">Consultando archivos…</p>}
 {ready&&preview&&<section className="private-preview" aria-label="Vista previa del PDF"><h3>Vista previa · {preview.nombre}</h3><p>{pages} páginas inspeccionadas · {preview.activo?"Versión actual":"Versión histórica o pendiente de recotizar"}</p><div className="client-actions"><button className="client-button secondary" disabled={locked||page<=1} onClick={()=>{setPage(p=>p-1);setImageError(false);}}>Página anterior</button><label>Página<input type="number" min={1} max={pages} value={page} disabled={locked} onChange={e=>{const n=Number(e.target.value);if(Number.isInteger(n)&&n>=1&&n<=pages){setPage(n);setImageError(false);}}}/></label><span>de {pages}</span><button className="client-button secondary" disabled={locked||page>=pages} onClick={()=>{setPage(p=>p+1);setImageError(false);}}>Página siguiente</button></div>
 <div className="private-preview-sheet"><img key={`${preview.codigoPublico}-${page}`} src={`${base}/${preview.codigoPublico}/paginas/${page}`} alt={`Página ${page} del PDF inspeccionado`} onLoad={()=>setVisited(old=>new Set([...old,page]))} onError={()=>setImageError(true)}/></div>{imageError&&<p role="alert">No se pudo cargar esta página. Actualizá el estado y volvé a abrir la vista previa.</p>}
 {preview.aceptadaEn?<p className="client-success">Esta versión ya fue aceptada el {date(preview.aceptadaEn)}.</p>:!internal&&preview.activo&&view?.carga.habilitada&&<div className="client-form"><p>{visited.size} de {pages} páginas abiertas. Revisalas todas antes de aceptar.</p><label className="client-check"><input type="checkbox" checked={ack} disabled={locked||visited.size!==pages} onChange={e=>setAck(e.target.checked)}/>Revisé todas las páginas y acepto imprimir este contenido con las opciones cotizadas.</label><button className="client-button" disabled={locked||!ack||visited.size!==pages||imageError} onClick={accept}>Aceptar vista previa</button></div>}
 </section>}
 {error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<div className="client-warning"><p>No se pudo confirmar la respuesta. Recuperá el mismo intento antes de enviar otro archivo.</p><button className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar carga o aceptación</button></div>}<button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar archivos</button>
 </section>;
}
