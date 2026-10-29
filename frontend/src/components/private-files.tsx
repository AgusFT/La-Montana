//#region ENCABEZADO · src/components/private-files.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/private-files.tsx
 * ========================================================================
 * FUNCIÓN
 * Gestiona los PDF privados de una cotización: consulta, carga, apertura, vista previa opcional, respetando los requisitos y estados devueltos por el servidor.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export]
 *   PrivateFiles({quote,version,internal=false,prepared=false,compact=false,refresh=0,onChange,confirmed=false,snapshot,correction,onLockChange}:
 *   {quote:string;version:number;internal?:boolean;prepared?:boolean;compact?:boolean;refresh?:number;onChange?:()=>void;confirmed?:boolean;snapshot?:string[];correction?:{pedido:string;solicitud:string};onLockChange?:(locked:boolean)=>void})
 *   Componente de interfaz.
 * - [async] PrivateFiles :: load()
 * - PrivateFiles :: open(file: PrivateFile)
 * - [async] PrivateFiles :: execute()
 * - PrivateFiles :: upload(item: string)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useRef,useState} from "react";
import {secureMutation,MutationError} from "@/lib/secure-mutation";
import {preparedFile} from "@/lib/prepared-files";
import {quoteRead,date} from "@/lib/quote-types";
import {isFile,isFileView,type PrivateFile,type FileView} from "@/lib/file-types";

export function PrivateFiles({quote,version,internal=false,prepared=false,compact=false,refresh=0,onChange,confirmed=false,snapshot,correction,onLockChange}:{quote:string;version:number;internal?:boolean;prepared?:boolean;compact?:boolean;refresh?:number;onChange?:()=>void;confirmed?:boolean;snapshot?:string[];correction?:{pedido:string;solicitud:string};onLockChange?:(locked:boolean)=>void}){
 const stateNames:Record<string,string>={PENDIENTE:"Pendiente de carga",VALIDANDO:"Analizando",VALIDO:"PDF válido",REQUIERE_COTIZACION:"Necesita una nueva cotización",RECHAZADO:"Archivo rechazado",FALLIDO:"Carga interrumpida"};
 const base=prepared?`/api/cliente/preparaciones/${quote}/archivos`:`/api/${internal?"operacion":"cliente"}/cotizaciones/${quote}/archivos`;
 const[view,setView]=useState<FileView|null>(null),[error,setError]=useState(""),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[selected,setSelected]=useState<Record<string,File>>({}),[preview,setPreview]=useState<PrivateFile|null>(null),[page,setPage]=useState(1),[imageError,setImageError]=useState(false);
 const running=useRef(false),command=useRef<{kind:"upload";item:string;file:File;body:string;id?:string}|null>(null);
 const locked=busy||uncertain;
 useEffect(()=>{onLockChange?.(locked);return()=>onLockChange?.(false);},[locked,onLockChange]);
 async function load(){try{const data=await quoteRead(base,isFileView);setSelected(old=>{const next={...old};for(const i of data.items){const file=preparedFile(quote,i.codigoPublico);if(!next[i.codigoPublico]&&file&&!i.archivos.some(f=>f.activo&&["VALIDANDO","VALIDO","REQUIERE_COTIZACION"].includes(f.estado)))next[i.codigoPublico]=file;}return next;});setView(snapshot?{...data,items:data.items.map(i=>({...i,archivos:i.archivos.filter(f=>snapshot.includes(f.codigoPublico))}))}:data);setPreview(current=>current?data.items.flatMap(i=>i.archivos).find(f=>f.codigoPublico===current.codigoPublico)??current:null);}catch(e){setError(e instanceof Error?e.message:"No pudimos consultar los archivos.");}}
 useEffect(()=>{void load();},[quote,version,internal,refresh,snapshot?.join(":")]);
 useEffect(()=>{if(!view?.items.some(i=>i.archivos.some(f=>f.estado==="VALIDANDO")))return;const timer=setTimeout(()=>void load(),3000);return()=>clearTimeout(timer);},[view]);
 function open(file:PrivateFile){setPreview(file);setPage(1);setImageError(false);}
 async function execute(){
  if(running.current||!command.current)return;running.current=true;setBusy(true);setError("");const c=command.current;
  try{
   let result:unknown;
    if(!c.id){const started=await secureMutation(base,c.body,"application/json");const data:unknown=await started.json();if(!isFile(data))throw new Error("No pudimos verificar el intento de carga.");c.id=data.codigoPublico;}
    const response=await secureMutation(`${base}/${c.id}/contenido`,c.file,"application/pdf","PUT");result=await response.json();
   if(!isFile(result))throw new Error("No pudimos verificar el resultado guardado.");
   if(c.kind==="upload")setSelected(old=>{const copy={...old};delete copy[c.item];return copy;});
   setUncertain(false);command.current=null;
   if(result.estado==="VALIDO"||result.estado==="REQUIERE_COTIZACION")open(result);
   await load();onChange?.();
  }catch(e){setError(e instanceof Error?e.message:"No pudimos completar la carga.");if(e instanceof MutationError&&!e.uncertain){command.current=null;setUncertain(false);await load();}else setUncertain(true);}
  finally{running.current=false;setBusy(false);}
 }
 function upload(item:string){if(locked||!view?.carga.habilitada||!view.items.find(i=>i.codigoPublico===item)?.admiteCarga)return;const file=selected[item];if(!file)return;if(file.size===0||file.size>10*1024*1024||!file.name.toLowerCase().endsWith(".pdf")){setError("Seleccioná un PDF de hasta 10 MiB.");return;}
  const pending=view.items.find(i=>i.codigoPublico===item)?.archivos.find(f=>f.estado==="PENDIENTE"&&Date.now()<Date.parse(f.cargarHasta));
  command.current={kind:"upload",item,file,body:JSON.stringify({operacion:crypto.randomUUID(),item,version:view.version,correccion:view.correccion}),id:pending?.codigoPublico};void execute();
 }

 const ready=preview&&(preview.estado==="VALIDO"||preview.estado==="REQUIERE_COTIZACION"),pages=preview?.paginas??0;
 const fileList=<>{view?<><p className={view.carga.habilitada?"client-note":"client-warning"}>{confirmed?"Consulta de versiones guardadas. Las correcciones se responden explícitamente desde el pedido.":view.carga.motivo}</p>{view.items.map((item,index)=><section className="private-file-item" key={item.codigoPublico}><h3>Archivo {index+1}: {item.nombre}</h3>
 {!prepared&&!internal&&!confirmed&&item.admiteCarga&&view.carga.habilitada&&<div className="client-form">{selected[item.codigoPublico]&&<p className="client-success">Archivo preparado: {selected[item.codigoPublico].name}. No necesitás seleccionarlo otra vez.</p>}<label>{selected[item.codigoPublico]?"Cambiar PDF":"Seleccionar PDF"} para el archivo {index+1}<input key={item.archivos.map(f=>f.codigoPublico+f.estado).join(":")} type="file" accept="application/pdf,.pdf" disabled={locked} onChange={e=>{const f=e.target.files?.[0];if(f)setSelected(old=>({...old,[item.codigoPublico]:f}));}}/></label><button type="button" className="client-button" disabled={locked||!selected[item.codigoPublico]||item.archivos.some(f=>f.estado==="VALIDANDO")} onClick={()=>upload(item.codigoPublico)}>{busy?"Procesando…":"Enviar PDF para analizar"}</button></div>}
 {item.archivos.length===0?<p className="client-muted">Todavía no hay un PDF recibido para este ítem.</p>:<ul className="private-file-history">{item.archivos.map(f=><li key={f.codigoPublico}><strong>{stateNames[f.estado]??f.estado}{snapshot?" · versión del pedido":f.activo?" · última carga":""}</strong><span>{date(f.creadoEn)}{f.paginas?` · ${f.paginas} páginas`:""}</span><p>{f.revocado?"Contenido revocado; se conservan identificación, fechas y auditoría.":f.aceptadaEn?"PDF inspeccionado; esta versión tiene su vista previa aceptada.":f.mensaje??(f.estado==="VALIDANDO"?"Analizando el archivo; podés recuperar el estado al recargar.":"Intento preparado. Seleccioná el PDF y envialo antes del vencimiento.")}</p>{f.aceptadaEn&&<p className="client-success">Vista previa aceptada el {date(f.aceptadaEn)}.</p>}{!f.revocado&&["VALIDO","REQUIERE_COTIZACION"].includes(f.estado)&&<div className="client-actions"><button className="client-button secondary" disabled={locked} onClick={()=>open(f)}>Ver páginas{snapshot?" del pedido":f.activo?" de esta carga":""}</button><a href={locked?undefined:`${base}/${f.codigoPublico}/original`} className="client-button secondary">Descargar PDF inspeccionado</a>{f.estado==="REQUIERE_COTIZACION"&&!internal&&<a className="client-button" href={locked?undefined:`/cliente/cotizaciones/nueva?reemplaza=${quote}&archivo=${f.codigoPublico}${correction?`&pedido=${correction.pedido}&solicitud=${correction.solicitud}`:""}`}>Recotizar con datos inspeccionados</a>}</div>}</li>)}</ul>}
 </section>)}</>:!error&&<p role="status">Consultando archivos…</p>}</>;
 return <section className={`client-card private-files${compact?' private-files-compact':''}`}>{!compact&&<><h2>{internal?"PDF privados y vista previa":confirmed?"Archivos de tu pedido":"Revisá tus archivos"}</h2><p>{confirmed&&snapshot?"Estos son los PDF analizados y vinculados al pedido. Podés consultar las páginas o descargar los originales.":"Tus PDF se almacenan en privado y se analizan automáticamente. La vista previa es opcional; podés consultarlos antes de enviar el pedido."}</p></>}
 {compact?<div className="private-file-browser">{fileList}</div>:fileList}
 {compact&&!ready&&<section className="workspace-pdf-placeholder"><span aria-hidden="true">▤</span><h3>{confirmed?"Elegí un PDF para consultar":"Elegí un PDF para revisar"}</h3><p>{confirmed?"Consultá la versión vinculada al pedido. El acceso al contenido depende de su disponibilidad y conservación.":"Elegí un archivo de la lista para consultar su contenido. Podés volver al resumen en cualquier momento."}</p></section>}
 {ready&&preview&&!preview.revocado&&<section className="private-preview" aria-label="Vista previa del PDF"><h3>Vista previa · {preview.nombre}</h3><p>{pages} páginas inspeccionadas · {snapshot?"Versión del pedido":preview.activo?"Última carga":"Versión histórica o pendiente de recotizar"}</p><div className="client-actions"><button className="client-button secondary" disabled={locked||page<=1} onClick={()=>{setPage(p=>p-1);setImageError(false);}}>Página anterior</button><label>Página<input type="number" min={1} max={pages} value={page} disabled={locked} onChange={e=>{const n=Number(e.target.value);if(Number.isInteger(n)&&n>=1&&n<=pages){setPage(n);setImageError(false);}}}/></label><span>de {pages}</span><button className="client-button secondary" disabled={locked||page>=pages} onClick={()=>{setPage(p=>p+1);setImageError(false);}}>Página siguiente</button></div>
 <div className="private-preview-sheet"><img key={`${preview.codigoPublico}-${page}`} src={`${base}/${preview.codigoPublico}/paginas/${page}`} alt={`Página ${page} del PDF inspeccionado`} onError={()=>setImageError(true)}/></div>{imageError&&<p role="alert">No se pudo cargar esta página. Actualizá el estado y volvé a abrir la vista previa.</p>}
 {preview.aceptadaEn&&<p className="client-success">Aceptación de vista previa registrada anteriormente el {date(preview.aceptadaEn)}.</p>}
 </section>}
 {error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<div className="client-warning"><p>No se pudo confirmar la respuesta. Recuperá el mismo intento antes de enviar otro archivo.</p><button className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar carga</button></div>}<button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar archivos</button>
 </section>;
}
