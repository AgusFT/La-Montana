//#region ENCABEZADO · src/components/payment-evidence.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/payment-evidence.tsx
 * ========================================================================
 * FUNCIÓN
 * Gestiona carga y consulta de comprobantes PDF privados asociados a intentos o pagos, incluyendo
 * preparación, envío y apertura del contenido.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - isEvidence(v: unknown): v is Evidence
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] PaymentEvidence({quote,parent,kind,internal=false,onClose}:
 *   {quote:string;parent:string;kind:"intento"|"pago";internal?:boolean;onClose:()=>void})
 *   Componente de interfaz.
 * - [async] PaymentEvidence :: load()
 * - PaymentEvidence :: prepare()
 * - [async] PaymentEvidence :: execute()
 * - PaymentEvidence :: open(f: Evidence)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Evidence (tipo).
 * - List (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - states [const].
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useRef,useState} from "react";
import {secureMutation,MutationError} from "@/lib/secure-mutation";
import {date} from "@/lib/quote-types";
type Evidence={codigoPublico:string;nombre:string;estado:string;creadoEn:string;cargarHasta:string;bytes:number|null;sha256:string|null;paginas:number|null;codigoResultado:string|null;mensaje:string|null;tipo:string;detalle:string;actor:string;rol:string;origen:string;predecesor:string|null;puedeEnviar:boolean};
type List={elementos:Evidence[];total:number;pagina:number;puedeCargar:boolean;motivo:string};
const isEvidence=(v:unknown):v is Evidence=>!!v&&typeof v==="object"&&typeof(v as Evidence).codigoPublico==="string"&&typeof(v as Evidence).estado==="string";
const states:Record<string,string>={PENDIENTE:"Pendiente de carga",VALIDANDO:"Analizando",VALIDO:"Documento inspeccionado",FALLIDO:"Carga interrumpida",RECHAZADO:"Documento rechazado"};
export function PaymentEvidence({quote,parent,kind,internal=false,onClose}:{quote:string;parent:string;kind:"intento"|"pago";internal?:boolean;onClose:()=>void}){
 const base=`/api/${internal?"operacion":"cliente"}/cotizaciones/${quote}/comprobantes`;
 const[view,setView]=useState<List|null>(null),[page,setPage]=useState(0),[loading,setLoading]=useState(true),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState(""),[selected,setSelected]=useState<File|null>(null),[detail,setDetail]=useState(""),[inputKey,setInputKey]=useState(0),[preview,setPreview]=useState<Evidence|null>(null),[previewPage,setPreviewPage]=useState(1),[imageError,setImageError]=useState(false);
 const command=useRef<{file:File;body?:string;id?:string}|null>(null),running=useRef(false),panel=useRef<HTMLElement>(null);const locked=busy||uncertain||loading;
 async function load(){setLoading(true);try{const response=await fetch(`${base}?${kind}=${parent}&pagina=${page}`,{cache:"no-store"}),data=await response.json();if(!response.ok||!Array.isArray(data.elementos)||!data.elementos.every(isEvidence))throw new Error(data.mensaje??"No pudimos consultar los comprobantes.");setView(data);}catch(e){setError(e instanceof Error?e.message:"No pudimos consultar.");}finally{setLoading(false);}}
 useEffect(()=>{void load();panel.current?.scrollIntoView({behavior:"smooth",block:"start"});},[quote,parent,kind,page]);
 useEffect(()=>{if(!view?.elementos.some(f=>f.estado==="VALIDANDO"))return;const timer=setTimeout(()=>void load(),3000);return()=>clearTimeout(timer);},[view]);
 const pending=view?.elementos.find(f=>f.estado==="PENDIENTE"&&f.puedeEnviar),otherPending=view?.elementos.some(f=>f.estado==="VALIDANDO"||f.estado==="PENDIENTE"&&Date.parse(f.cargarHasta)>Date.now()&&!f.puedeEnviar);
 function prepare(){if(locked||!selected||!view?.puedeCargar)return;setError("");if(!selected.name.toLowerCase().endsWith(".pdf")||selected.size===0||selected.size>10485760){setError("Seleccioná un PDF estático de hasta 10 MiB.");return;}
  if(pending&&selected.name!==pending.nombre){setError(`La carga pendiente corresponde a ${pending.nombre}. Seleccioná ese archivo para completarla.`);return;}
  if(!pending&&!detail.trim()){setError("Indicá qué documento estás adjuntando.");return;}
  command.current={file:selected,...(pending?{id:pending.codigoPublico}:{body:JSON.stringify({operacion:crypto.randomUUID(),[kind]:parent,nombre:selected.name,detalle:detail.trim()})})};void execute();
 }
 async function execute(){if(running.current||!command.current)return;running.current=true;setBusy(true);setError("");const c=command.current;
  try{if(!c.id){const response=await secureMutation(base,c.body,"application/json"),data:unknown=await response.json();if(!isEvidence(data))throw new Error("No pudimos verificar el intento de carga.");c.id=data.codigoPublico;}
   const response=await secureMutation(`${base}/${c.id}/contenido`,c.file,"application/pdf","PUT"),data:unknown=await response.json();if(!isEvidence(data))throw new Error("No pudimos verificar el resultado del comprobante.");
   setSelected(null);setInputKey(k=>k+1);setUncertain(false);command.current=null;await load();if(data.estado==="VALIDO")open(data);
  }catch(e){setError(e instanceof Error?e.message:"No pudimos completar la carga.");if(e instanceof MutationError&&!e.uncertain){command.current=null;setUncertain(false);await load();}else setUncertain(true);}finally{running.current=false;setBusy(false);}
 }
 function open(f:Evidence){setPreview(f);setPreviewPage(1);setImageError(false);}
 return <section className="client-card payment-evidence" ref={panel} aria-label="Comprobantes privados"><div className="client-actions"><h3>Comprobantes privados · {kind==="intento"?"transferencia informada":"pago recibido"}</h3><button className="client-button secondary" disabled={locked} onClick={onClose}>Cerrar comprobantes</button></div>
 <p className="client-note">Un documento técnicamente seguro no prueba que el dinero haya ingresado. La acreditación sigue a cargo de un interno que verifica el movimiento real.</p>{loading&&<p role="status">Consultando documentos…</p>}{error&&<p className="client-warning" role="alert">{error}</p>}{view&&<><p>{view.motivo}</p>
 {view.puedeCargar&&page===0&&<form className="client-form" onSubmit={e=>{e.preventDefault();prepare();}}><fieldset disabled={locked||otherPending} style={{border:0,padding:0,minWidth:0}}>{pending&&<p className="client-warning">Carga pendiente: {pending.nombre}. {pending.detalle} · Podés completarla hasta {date(pending.cargarHasta)}.</p>}<label>Seleccionar comprobante PDF<input key={inputKey} type="file" accept="application/pdf,.pdf" required onChange={e=>setSelected(e.target.files?.[0]??null)}/></label>{!pending&&<label>Detalle del documento<textarea required maxLength={300} value={detail} onChange={e=>setDetail(e.target.value)}/></label>}<button className="client-button" disabled={!selected}>{busy?"Analizando…":pending?"Completar carga del comprobante":"Adjuntar comprobante"}</button></fieldset></form>}
 {otherPending&&<p className="client-note">Hay una carga en proceso o pendiente de otra persona. Consultá su resultado antes de iniciar otra.</p>}
 {!view.elementos.length?<p>No se adjuntaron comprobantes.</p>:view.elementos.map(f=><article className="evidence-record" key={f.codigoPublico}><h4>{f.nombre}</h4><p><strong>{states[f.estado]??f.estado}</strong> · {date(f.creadoEn)}</p><p>{f.detalle}</p><p className="client-muted">Aportado por {f.actor} · {f.rol==="CLIENTE"?"Cliente":"Personal interno"} · {f.origen==="TRANSFERENCIA_INFORMADA"?"Adjuntado al informe de transferencia":"Adjuntado al pago registrado"}.</p>{f.predecesor&&<p className="client-muted">Conserva una versión anterior en el historial.</p>}{f.mensaje&&<p className={f.estado==="VALIDO"?"client-success":"client-note"}>{f.mensaje}</p>}{f.estado==="VALIDO"&&<div className="client-actions"><button className="client-button secondary" disabled={locked} onClick={()=>open(f)}>Ver comprobante</button><a className="client-button secondary" href={`${base}/${f.codigoPublico}/original`}>Descargar PDF privado</a></div>}</article>)}
 <div className="client-actions"><button className="client-button secondary" disabled={locked} onClick={()=>{setError("");void load();}}>Actualizar comprobantes</button><button className="client-button secondary" disabled={locked||page===0} onClick={()=>setPage(p=>p-1)}>Anteriores</button><button className="client-button secondary" disabled={locked||(page+1)*25>=view.total} onClick={()=>setPage(p=>p+1)}>Más documentos</button></div></>}
 {preview&&<section className="evidence-preview" aria-label="Vista del comprobante"><h4>{preview.nombre}</h4><div className="client-actions"><button className="client-button secondary" disabled={previewPage<=1} onClick={()=>{setPreviewPage(p=>p-1);setImageError(false);}}>Página anterior</button><span>Página {previewPage} de {preview.paginas}</span><button className="client-button secondary" disabled={previewPage>=(preview.paginas??1)} onClick={()=>{setPreviewPage(p=>p+1);setImageError(false);}}>Página siguiente</button><button className="client-button secondary" onClick={()=>setPreview(null)}>Cerrar vista del comprobante</button></div>{imageError?<p className="client-warning" role="alert">No pudimos abrir esta página privada. Verificá tu acceso y volvé a consultarla.</p>:<img key={`${preview.codigoPublico}-${previewPage}`} src={`${base}/${preview.codigoPublico}/paginas/${previewPage}`} alt={`Página ${previewPage} del comprobante ${preview.nombre}`} onError={()=>setImageError(true)}/>}<p className="client-muted">Documento para consulta. No es una aprobación de pago ni una aceptación del PDF de trabajo.</p></section>}
 {uncertain&&<div className="client-warning" role="alert"><p>No pudimos confirmar la respuesta. Recuperá el mismo intento para evitar cargas duplicadas.</p><button className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar comprobante</button></div>}
 </section>;
}
