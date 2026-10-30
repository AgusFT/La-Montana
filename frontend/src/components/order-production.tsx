//#region ENCABEZADO · src/components/order-production.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/order-production.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta producción manual por ítem, impresoras, trabajos, reimpresión y controles de calidad, y
 * envía las transiciones autorizadas.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - jobState(v: string)
 * - isProduction(v: unknown): v is Production
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] OrderProduction({order,onSaved}: {order:Order;onSaved:()=>void})
 *   Componente de interfaz.
 * - [async] OrderProduction :: load()
 * - OrderProduction :: choose(a: Action)
 * - [async] OrderProduction :: save(recover = false)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Checklist (tipo).
 * - Job (tipo).
 * - Item (tipo).
 * - Production (tipo).
 * - Action (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - checks [const].
 * - actionLabels [const].
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useRef,useState} from "react";
import {quoteRead,date,money} from "@/lib/quote-types";
import {type Order,orderState} from "@/lib/order-types";
import {secureMutation,MutationError} from "@/lib/secure-mutation";
import {isPrinterCandidate,type PrinterCandidate} from "@/lib/printer-types";
import {PrinterSelection} from "./printer-selection";
import {qualityRequirements,type QualityRequirements} from '@/lib/quality-guide';
import {QualityWorkGuide} from './quality-work-guide';

const checks={impresionCompleta:"Impresión completa",calidadCorrecta:"Calidad de impresión correcta",alineacionCorrecta:"Alineación de páginas",ordenCorrecto:"Orden de páginas correcto",terminacionesCorrectas:"Encuadernación / opcionales OK"};
type Checklist=Record<keyof typeof checks,boolean>;
type Job={codigoPublico:string;numero:number;origen:string|null;tipo:string;estado:string;archivo:string;impresora:string|null;motivoExcepcion:string|null;operador:string;inicio:string;fin:string|null;calidad:{resultado:string;checklist:Checklist;observaciones:string;inspector:string;fecha:string;modalidadConfirmacion?:string;requisitos?:QualityRequirements|null}|null;historial:{accion:string;actor:string;motivo:string;fecha:string}[]};
type Item={codigoPublico:string;archivo:string;nombre:string;impresoras:PrinterCandidate[];puedeIniciar:boolean;bloqueo:string|null;trabajos:Job[]};
export type Production={version:number;estado:string;puedeProducir:boolean;puedeControlarCalidad:boolean;cobertura:{condiciones:string;requerido:string;aplicado:string;pendiente:string}[];bloqueos:string[];items:Item[]};
const jobState=(v:string)=>({IMPRIMIENDO:"En producción manual",COMPLETADO:"Trabajo terminado",CANCELADO:"Trabajo cancelado",ERROR:"Error registrado",APROBADO:"Calidad aprobada",REIMPRESION_REQUERIDA:"Reimpresión requerida",INCIDENCIA:"Incidencia de calidad"}[v]??v);
const actionLabels={INICIAR_TRABAJO:"Inicio manual",COMPLETAR_TRABAJO:"Fin manual",ERROR_TRABAJO:"Error",CANCELAR_TRABAJO:"Cancelación de trabajo",CONTROLAR_CALIDAD:"Control de calidad"};
function isProduction(v:unknown):v is Production{if(!v||typeof v!=="object")return false;const x=v as Production;return Number.isSafeInteger(x.version)&&typeof x.estado==="string"&&typeof x.puedeProducir==="boolean"&&typeof x.puedeControlarCalidad==="boolean"&&Array.isArray(x.cobertura)&&Array.isArray(x.bloqueos)&&Array.isArray(x.items)&&x.items.every(i=>typeof i.codigoPublico==="string"&&typeof i.nombre==="string"&&Array.isArray(i.impresoras)&&i.impresoras.every(isPrinterCandidate)&&Array.isArray(i.trabajos)&&i.trabajos.every(t=>typeof t.codigoPublico==="string"&&typeof t.estado==="string"&&Array.isArray(t.historial)));}
type Action={tipo:"INICIAR"|"COMPLETAR"|"ERROR"|"CANCELAR"|"CALIDAD";item:Item;job?:Job};
export function OrderProduction({order,onSaved,workspace=false,onSnapshot,onLockChange,refresh=0}:{order:Order;onSaved:()=>void;workspace?:boolean;refresh?:number;onSnapshot?:(value:Production|null)=>void;onLockChange?:(locked:boolean)=>void}){
 const[data,setData]=useState<Production|null>(null),[error,setError]=useState(""),[message,setMessage]=useState(""),[busy,setBusy]=useState(false),[loading,setLoading]=useState(false),[uncertain,setUncertain]=useState(false),[action,setAction]=useState<Action|null>(null),[result,setResult]=useState(""),[printer,setPrinter]=useState("");
 const pending=useRef<{path:string;body:string}|null>(null),running=useRef(false),form=useRef<HTMLFormElement>(null),dialog=useRef<HTMLDialogElement>(null);
 const selectedWork=action?order.oferta.items.find(i=>i.codigoPublico===action.item.codigoPublico):null;
 const requirements=selectedWork?qualityRequirements(selectedWork):null;
 const base=`/api/operacion/pedidos/${order.codigoPublico}/produccion`,locked=busy||loading||uncertain;
 async function load(){setLoading(true);setError("");try{setData(await quoteRead(base,isProduction));}catch(e){setData(null);setError(e instanceof Error?e.message:"No pudimos consultar producción.");}finally{setLoading(false);}}
 useEffect(()=>{if(!data||action?.tipo!=="INICIAR")return;const item=data.items.find(i=>i.codigoPublico===action.item.codigoPublico);if(!item||item===action.item)return;setAction({...action,item});setPrinter(selected=>selected==="MANUAL"||item.impresoras.some(p=>p.codigoPublico===selected&&p.compatible)?selected:item.impresoras.find(p=>p.recomendada)?.codigoPublico??"");},[data,action]);
 useEffect(()=>{void load();},[order.codigoPublico,order.version,refresh]);
 useEffect(()=>{onSnapshot?.(data);},[data,onSnapshot]);
 useEffect(()=>{onLockChange?.(busy||uncertain);return()=>onLockChange?.(false);},[busy,uncertain,onLockChange]);
 function choose(a:Action){setAction(a);setResult(a.tipo==="CALIDAD"?"APROBADO":"");setPrinter(a.tipo==="INICIAR"?a.item.impresoras.find(p=>p.recomendada)?.codigoPublico??"":"");setError("");setMessage("");if(a.tipo==="INICIAR")void load();if(!workspace)setTimeout(()=>form.current?.scrollIntoView({behavior:"smooth",block:"center"}),0);}
 async function save(recover=false){
  if(running.current||!recover&&locked)return;
  if(!recover){if(!action||!data||!form.current?.reportValidity())return;if(action.tipo==='CALIDAD'&&(!requirements||requirements.adicionales.some(a=>!a.base))){setError('No pudimos identificar todos los adicionales. Actualizá el pedido antes de aprobar.');return;}if(action.tipo==="INICIAR"&&(!printer||printer!=="MANUAL"&&!action.item.impresoras.some(p=>p.codigoPublico===printer&&p.compatible))){setError("Elegí una impresora disponible o indicá la excepción manual.");return;}const f=new FormData(form.current);const common={operacion:crypto.randomUUID(),version:data.version,confirmacionManual:true};
   const body=action.tipo==="INICIAR"?{...common,item:action.item.codigoPublico,impresora:printer==="MANUAL"?null:printer,motivo:f.get("motivo")}:action.tipo==="CALIDAD"?{...common,resultado:result,observaciones:[result!=='APROBADO'?String(f.get('problema')??'Trabajo observado'):'',String(f.get('motivo')??'')].filter(Boolean).join('. ')}: {...common,accion:action.tipo,motivo:f.get("motivo")};
   pending.current={path:action.tipo==="INICIAR"?`${base}/iniciar`:`${base}/trabajos/${action.job?.codigoPublico}/${action.tipo==="CALIDAD"?'verificar':"estado"}`,body:JSON.stringify(body)};
  }
  if(!pending.current)return;running.current=true;setBusy(true);setError("");
  try{const response=await secureMutation(pending.current.path,pending.current.body,"application/json"),v:unknown=await response.json();if(!isProduction(v))throw new Error("No pudimos verificar el hecho guardado.");setData(v);pending.current=null;setUncertain(false);setAction(null);setMessage("Hecho manual registrado. El historial conserva sus responsables y el resultado.");onSaved();}
  catch(e){setError(e instanceof Error?e.message:"No se pudo guardar.");if(e instanceof MutationError&&!e.uncertain){pending.current=null;setUncertain(false);}else setUncertain(true);}
  finally{running.current=false;setBusy(false);}
 }
 useEffect(()=>{if(!workspace)return;if(action)dialog.current?.showModal();else dialog.current?.close();},[action,workspace]);
 return <section id="pedido-produccion" className={`client-card order-management production-panel${workspace?" operation-task-component":""}`} aria-label="Producción manual y calidad"><header className="order-management-heading"><h2>Producción y control de calidad</h2><span>{orderState(data?.estado??order.estado,order.reserva.modalidad)}</span></header>
  {!workspace&&<p>Registrá lo que realiza el personal con cada PDF confirmado. Completar un trabajo no lo entrega al cliente: debe superar el control de calidad.</p>}
  {!workspace&&<p className="client-note">Impresión automática y conectividad CUPS <span className="client-construction">En construcción</span>. Las impresoras de esta vista son capacidades declaradas en la configuración del pedido; no indican conexión ni disponibilidad física en tiempo real.</p>}
  {(!workspace||!!error)&&<button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar producción y cobertura</button>}
  {loading&&<p role="status">Consultando trabajos…</p>}{error&&<p role="alert" className="client-warning">{error}</p>}{message&&<p role="status" className="client-success">{message}</p>}
  {(!workspace||!action)&&uncertain&&<div className="client-warning"><p>La respuesta no pudo confirmarse. Recuperá la misma operación para evitar registrar dos veces el hecho.</p><button className="client-button" disabled={busy} onClick={()=>void save(true)}>Recuperar operación de producción</button></div>}
  {data&&<><details className="production-coverage" open={data.cobertura.some(c=>Number(c.pendiente)>0)}><summary>Anticipo necesario para iniciar trabajos</summary><div className="client-grid">{data.cobertura.map(c=><div key={c.condiciones}><h3>{c.condiciones}</h3><p>Requerido: {money(c.requerido)}<br/>Acreditado y aplicado por medios admitidos: {money(c.aplicado)}<br/><strong>Falta: {money(c.pendiente)}</strong></p></div>)}</div></details>
  {data.bloqueos.length>0&&<div className={data.estado==="LISTO_PARA_ENTREGA"?"client-note":"client-warning"}>{data.bloqueos.map(b=><p key={b}>{b}</p>)}</div>}
  {data.items.map(i=>{const last=i.trabajos.at(-1),work=order.oferta.items.find(o=>o.codigoPublico===i.codigoPublico);return <article className="production-item" key={i.codigoPublico}>{workspace&&<h3>{i.nombre}</h3>}<div className="client-grid production-work-grid"><section><h3>{i.nombre}</h3><p>{work?.formato} · {work?.papel}<br/>{work?.trabajo.paginas} páginas × {work?.trabajo.copias} copias · {work?.trabajo.dobleFaz?"Doble faz":"Simple faz"}<br/>{work?.trabajo.color==="COLOR"?"Color":"Blanco y negro"}</p><p>Adicionales: {work?.precio.lineas.filter(l=>work.trabajo.terminaciones.includes(l.servicio)).map(l=>l.nombre).join(", ")||"Sin terminaciones"}</p><a href="#pedido-pdf">Consultar PDF confirmado y vista previa</a></section><section className="production-current"><h3>Trabajo actual</h3>{last?<><strong>IMP-{last.numero} · {jobState(last.estado)}</strong><p>{last.impresora??"Recurso manual fuera del inventario"}<br/>Registrado por {last.operador} · {date(last.inicio)}</p>{last.fin&&<p>Finalizado: {date(last.fin)}</p>}{last.calidad&&<p className={last.calidad.resultado==="APROBADO"?"client-success":"client-warning"}>{jobState(last.calidad.resultado)} · {last.calidad.inspector}</p>}</>:<p>Aún no se inició este ítem.</p>}</section></div>
   <div className="client-actions">{last?.calidad?.resultado!=="APROBADO"&&(!workspace||data.puedeProducir&&i.puedeIniciar)&&<button className="client-button order-approve" disabled={locked||!!action||!i.puedeIniciar} onClick={()=>choose({tipo:"INICIAR",item:i})}>{!last?workspace?"Preparar producción":"Iniciar trabajo manual":last.calidad?"Preparar reimpresión":"Reintentar producción"}</button>}
   {last?.estado==="IMPRIMIENDO"&&data.puedeProducir&&data.puedeControlarCalidad&&<button className="client-button order-approve" disabled={locked||!!action} onClick={()=>choose({tipo:'CALIDAD',item:i,job:last})}>Terminar impresión y controlar calidad</button>}
   {last?.estado==="IMPRIMIENDO"&&(!workspace||data.puedeProducir)&&<>{((workspace?data.puedeControlarCalidad?[]:["COMPLETAR"]:data.puedeControlarCalidad?["ERROR","CANCELAR"]:["COMPLETAR","ERROR","CANCELAR"]) as ("COMPLETAR"|"ERROR"|"CANCELAR")[]).map(t=><button key={t} className={`client-button ${t==="COMPLETAR"?"order-approve":"order-danger"}`} disabled={locked||!!action||!data.puedeProducir} onClick={()=>choose({tipo:t,item:i,job:last})}>{t==="COMPLETAR"?"Registrar fin de impresión":t==="ERROR"?"Registrar error":"Cancelar trabajo"}</button>)}</>}
   {last?.estado==="COMPLETADO"&&!last.calidad&&(!workspace||data.puedeControlarCalidad)&&<button className="client-button order-approve" disabled={locked||!!action||!data.puedeControlarCalidad} onClick={()=>choose({tipo:"CALIDAD",item:i,job:last})}>Controlar calidad</button>}</div>
   {workspace&&last?.estado==="IMPRIMIENDO"&&data.puedeProducir&&<details className="operation-secondary-decisions"><summary>Informar un problema con este trabajo</summary><div className="client-actions"><button className="client-button order-danger" disabled={locked||!!action} onClick={()=>choose({tipo:"ERROR",item:i,job:last})}>Registrar error</button><button className="client-button order-danger" disabled={locked||!!action} onClick={()=>choose({tipo:"CANCELAR",item:i,job:last})}>Cancelar este trabajo de impresión</button></div></details>}
   {i.bloqueo&&<p className="client-muted">{i.bloqueo}</p>}{!data.puedeControlarCalidad&&last?.estado==="COMPLETADO"&&!last.calidad&&<p className="client-warning">La impresión terminó. El personal con permiso de control de calidad debe inspeccionarla.</p>}{!data.puedeProducir&&data.puedeControlarCalidad&&last?.estado==='IMPRIMIENDO'&&<p className="client-note">Esperando que producción registre el fin de la impresión para inspeccionarla.</p>}
   {i.trabajos.length>0&&<details><summary>Historial de trabajos e inspecciones ({i.trabajos.length})</summary>{i.trabajos.map(t=><section className="order-private-note" key={t.codigoPublico}><h4>IMP-{t.numero} · {jobState(t.estado)}{t.origen?" · Continuación de un intento anterior":""}</h4><p>{t.impresora??"Recurso manual fuera del inventario"}{t.motivoExcepcion&&` · ${t.motivoExcepcion}`}</p>{t.historial.map((e,n)=><p className="order-text" key={n}><strong>{actionLabels[e.accion as keyof typeof actionLabels]} · {e.actor} · {date(e.fecha)}</strong><br/>{e.motivo}</p>)}{t.calidad&&<><h4>{jobState(t.calidad.resultado)} · {t.calidad.inspector} · {date(t.calidad.fecha)}</h4>{t.calidad.modalidadConfirmacion==='CONFORMIDAD_GLOBAL'?<><p>Confirmación del trabajo completo según la guía del pedido.</p>{t.calidad.requisitos&&<QualityWorkGuide requirements={t.calidad.requisitos}/>}</>:<ul className="production-checklist">{Object.entries(checks).map(([k,label])=><li key={k}>{t.calidad!.checklist[k as keyof Checklist]?"✓":"Pendiente / observado"} · {label}</li>)}</ul>}</>}</section>)}</details>}
  </article>;})}
  <dialog ref={dialog} className={workspace?"operation-decision-dialog operation-production-dialog":"production-inline-dialog"} aria-labelledby="production-action-title" onCancel={e=>{e.preventDefault();if(!locked)setAction(null);}} open={!workspace&&!!action}>{action&&<form ref={form} className="client-form production-form" key={`${action.tipo}-${action.item.codigoPublico}`} onSubmit={e=>{e.preventDefault();void save();}}><header><h3 id="production-action-title">{action.tipo==="CALIDAD"?action.job?.estado==='IMPRIMIENDO'?"Terminar impresión y controlar calidad":"Control de calidad":action.tipo==="INICIAR"?"Preparar producción":action.tipo==="COMPLETAR"?"Registrar fin de impresión":action.tipo==="ERROR"?"Registrar error de producción":"Cancelar trabajo manual"}</h3><p>{action.item.nombre}{action.job&&` · IMP-${action.job.numero}`}</p>{action.tipo==='CALIDAD'&&action.job?.estado==='IMPRIMIENDO'&&<p>Completá este control cuando la impresión haya terminado. Al guardar se registran el fin de impresión y tu inspección juntos.</p>}</header>{error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<div className="client-warning"><p>No se pudo confirmar la respuesta. Recuperá el mismo registro antes de continuar.</p><button type="button" className="client-button" disabled={busy} onClick={()=>void save(true)}>Recuperar operación de producción</button></div>}<fieldset disabled={locked}>
  {action.tipo==="INICIAR"&&<><div className="printer-work-summary">Este trabajo: <strong>{selectedWork?.precio.hojas} hojas</strong> · {selectedWork?.formato} · {selectedWork?.trabajo.color==="COLOR"?"Color":"Blanco y negro"} · {selectedWork?.trabajo.dobleFaz?"Doble faz":"Simple faz"}</div><PrinterSelection printers={action.item.impresoras} value={printer} onChange={setPrinter} locked={locked} onRefresh={()=>void load()}/><details className="printer-start-notes" open={printer==="MANUAL"}><summary>{printer==="MANUAL"?"Describir recurso y motivo de la excepción":"Agregar observaciones (opcional)"}</summary><label>Observaciones<textarea name="motivo" maxLength={500} required={printer==="MANUAL"}/></label></details><p className="client-muted">Al iniciar confirmás el uso manual del PDF y sus opciones. Se guardan tu usuario, la impresora y las hojas previstas. Todavía no se envían instrucciones al equipo.</p></>}
  {action.tipo==='CALIDAD'&&requirements&&<div className="quality-review-grid"><section><h4>El trabajo debe entregarse así</h4><QualityWorkGuide requirements={requirements}/><a href={`/operacion/pedidos/${order.codigoPublico}#pedido-pdf`} target="_blank" rel="noopener">Consultar el PDF si lo necesitás</a></section><section className="quality-expected"><h4>Calidad esperada</h4><ul><li>Impresión nítida y uniforme, sin manchas ni contenido cortado.</li><li>Hojas completas, alineadas y en el orden del archivo.</li><li>{requirements.adicionales.length?'Todos los adicionales indicados, terminados y prolijos en cada ejemplar.':'Ejemplares completos, sin terminaciones adicionales.'}</li></ul><p>Compará el trabajo físico con esta guía. No necesitás marcar casillas.</p>{result==='APROBADO'?<button type="button" className="client-button secondary" onClick={()=>setResult('INCIDENCIA')}>Hay un problema</button>:<div className="quality-problem"><label>Qué hay que corregir<select name="problema"><option>Impresión defectuosa</option><option>Faltan hojas o copias</option><option>Faltan adicionales o están mal terminados</option><option>Otro problema</option></select></label><label>Próximo paso<select value={result} onChange={e=>setResult(e.target.value)}><option value="INCIDENCIA">Devolver a producción para corregir</option><option value="REIMPRESION_REQUERIDA">Solicitar reimpresión</option></select></label><button type="button" className="client-text-action" onClick={()=>setResult('APROBADO')}>Volver a aprobar el trabajo</button></div>}<details className="quality-notes"><summary>Agregar observación (opcional)</summary><label>Observación<textarea name="motivo" maxLength={350}/></label></details></section></div>}
  {!['INICIAR','CALIDAD'].includes(action.tipo)&&<><p>{action.job?.impresora??'Recurso manual fuera del inventario'}</p>{['CANCELAR','ERROR'].includes(action.tipo)&&<p className="client-warning">Este registro afecta al trabajo físico y permite otro intento. No cancela el pedido.</p>}<details><summary>Agregar observación (opcional)</summary><label>Observación<textarea name="motivo" maxLength={500}/></label></details></>}
  {action.tipo!=='INICIAR'&&<p className="client-muted">Al confirmar se guardan tu usuario, la fecha y la hora.{action.tipo==='CALIDAD'&&' El pedido pasa a entrega cuando todos los trabajos estén aprobados.'}</p>}<div className="printer-start-footer"><button type="button" className="client-button secondary" onClick={()=>setAction(null)}>Volver</button><button className="client-button order-approve">{busy?'Guardando…':action.tipo==='INICIAR'?'Iniciar producción':action.tipo==='CALIDAD'?result==='APROBADO'?'Está correcto · Aprobar':result==='REIMPRESION_REQUERIDA'?'Solicitar reimpresión':'Devolver a producción':action.tipo==='COMPLETAR'?'Confirmar fin de impresión':action.tipo==='ERROR'?'Registrar error':'Cancelar este trabajo'}</button></div></fieldset></form>}</dialog>
  {data.estado==="LISTO_PARA_ENTREGA"&&<p className="client-success">{order.reserva.modalidad==="RETIRO_SUCURSAL"?"Listo para entregar en la sucursal.":"La producción y calidad están completas. Consultá el recorrido actual en logística y entrega."} La entrega efectiva, su código y el cierre se registran en logística y entrega.</p>}
  </>}
 </section>;
}
