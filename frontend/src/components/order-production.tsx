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

const checks={impresionCompleta:"Impresión completa",calidadCorrecta:"Calidad de impresión correcta",alineacionCorrecta:"Alineación de páginas",ordenCorrecto:"Orden de páginas correcto",terminacionesCorrectas:"Encuadernación / opcionales OK"};
type Checklist=Record<keyof typeof checks,boolean>;
type Job={codigoPublico:string;numero:number;origen:string|null;tipo:string;estado:string;archivo:string;impresora:string|null;motivoExcepcion:string|null;operador:string;inicio:string;fin:string|null;calidad:{resultado:string;checklist:Checklist;observaciones:string;inspector:string;fecha:string}|null;historial:{accion:string;actor:string;motivo:string;fecha:string}[]};
type Item={codigoPublico:string;archivo:string;nombre:string;impresoras:{codigoPublico:string;nombre:string}[];puedeIniciar:boolean;bloqueo:string|null;trabajos:Job[]};
type Production={version:number;estado:string;puedeProducir:boolean;puedeControlarCalidad:boolean;cobertura:{condiciones:string;requerido:string;aplicado:string;pendiente:string}[];bloqueos:string[];items:Item[]};
const jobState=(v:string)=>({IMPRIMIENDO:"En producción manual",COMPLETADO:"Trabajo terminado",CANCELADO:"Trabajo cancelado",ERROR:"Error registrado",APROBADO:"Calidad aprobada",REIMPRESION_REQUERIDA:"Reimpresión requerida",INCIDENCIA:"Incidencia de calidad"}[v]??v);
const actionLabels={INICIAR_TRABAJO:"Inicio manual",COMPLETAR_TRABAJO:"Fin manual",ERROR_TRABAJO:"Error",CANCELAR_TRABAJO:"Cancelación de trabajo",CONTROLAR_CALIDAD:"Control de calidad"};
function isProduction(v:unknown):v is Production{if(!v||typeof v!=="object")return false;const x=v as Production;return Number.isSafeInteger(x.version)&&typeof x.estado==="string"&&typeof x.puedeProducir==="boolean"&&typeof x.puedeControlarCalidad==="boolean"&&Array.isArray(x.cobertura)&&Array.isArray(x.bloqueos)&&Array.isArray(x.items)&&x.items.every(i=>typeof i.codigoPublico==="string"&&typeof i.nombre==="string"&&Array.isArray(i.impresoras)&&Array.isArray(i.trabajos)&&i.trabajos.every(t=>typeof t.codigoPublico==="string"&&typeof t.estado==="string"&&Array.isArray(t.historial)));}
type Action={tipo:"INICIAR"|"COMPLETAR"|"ERROR"|"CANCELAR"|"CALIDAD";item:Item;job?:Job};
export function OrderProduction({order,onSaved}:{order:Order;onSaved:()=>void}){
 const[data,setData]=useState<Production|null>(null),[error,setError]=useState(""),[message,setMessage]=useState(""),[busy,setBusy]=useState(false),[loading,setLoading]=useState(false),[uncertain,setUncertain]=useState(false),[action,setAction]=useState<Action|null>(null),[result,setResult]=useState(""),[printer,setPrinter]=useState("");
 const pending=useRef<{path:string;body:string}|null>(null),running=useRef(false),form=useRef<HTMLFormElement>(null);
 const selectedWork=action?order.oferta.items.find(i=>i.codigoPublico===action.item.codigoPublico):null;
 const base=`/api/operacion/pedidos/${order.codigoPublico}/produccion`,locked=busy||loading||uncertain;
 async function load(){setLoading(true);setError("");try{setData(await quoteRead(base,isProduction));}catch(e){setData(null);setError(e instanceof Error?e.message:"No pudimos consultar producción.");}finally{setLoading(false);}}
 useEffect(()=>{void load();},[order.codigoPublico,order.version]);
 function choose(a:Action){setAction(a);setResult("");setPrinter("");setError("");setMessage("");setTimeout(()=>form.current?.scrollIntoView({behavior:"smooth",block:"center"}),0);}
 async function save(recover=false){
  if(running.current||!recover&&locked)return;
  if(!recover){if(!action||!data||!form.current?.reportValidity())return;const f=new FormData(form.current);const common={operacion:crypto.randomUUID(),version:data.version,confirmacionManual:f.get("confirmacion")==="on"};
   const body=action.tipo==="INICIAR"?{...common,item:action.item.codigoPublico,impresora:printer==="MANUAL"?null:printer,motivo:f.get("motivo")}:action.tipo==="CALIDAD"?{...common,resultado:result,checklist:Object.fromEntries(Object.keys(checks).map(k=>[k,f.get(k)==="on"])),observaciones:f.get("motivo")}: {...common,accion:action.tipo,motivo:f.get("motivo")};
   pending.current={path:action.tipo==="INICIAR"?`${base}/iniciar`:`${base}/trabajos/${action.job?.codigoPublico}/${action.tipo==="CALIDAD"?"calidad":"estado"}`,body:JSON.stringify(body)};
  }
  if(!pending.current)return;running.current=true;setBusy(true);setError("");
  try{const response=await secureMutation(pending.current.path,pending.current.body,"application/json"),v:unknown=await response.json();if(!isProduction(v))throw new Error("No pudimos verificar el hecho guardado.");setData(v);pending.current=null;setUncertain(false);setAction(null);setMessage("Hecho manual registrado. El historial conserva sus responsables y el resultado.");onSaved();}
  catch(e){setError(e instanceof Error?e.message:"No se pudo guardar.");if(e instanceof MutationError&&!e.uncertain){pending.current=null;setUncertain(false);}else setUncertain(true);}
  finally{running.current=false;setBusy(false);}
 }
 return <section id="pedido-produccion" className="client-card order-management production-panel" aria-label="Producción manual y calidad"><header className="order-management-heading"><h2>Producción y control de calidad</h2><span>{orderState(data?.estado??order.estado,order.reserva.modalidad)}</span></header>
  <p>Registrá lo que realiza el personal con cada PDF confirmado. Completar un trabajo no lo entrega al cliente: debe superar el control de calidad.</p>
  <p className="client-note">Impresión automática y conectividad CUPS <span className="client-construction">En construcción</span>. Las impresoras de esta vista son capacidades declaradas en la configuración del pedido; no indican conexión ni disponibilidad física en tiempo real.</p>
  <button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar producción y cobertura</button>
  {loading&&<p role="status">Consultando trabajos…</p>}{error&&<p role="alert" className="client-warning">{error}</p>}{message&&<p role="status" className="client-success">{message}</p>}
  {uncertain&&<div className="client-warning"><p>La respuesta no pudo confirmarse. Recuperá la misma operación para evitar registrar dos veces el hecho.</p><button className="client-button" disabled={busy} onClick={()=>void save(true)}>Recuperar operación de producción</button></div>}
  {data&&<><details className="production-coverage" open={data.cobertura.some(c=>Number(c.pendiente)>0)}><summary>Anticipo necesario para iniciar trabajos</summary><div className="client-grid">{data.cobertura.map(c=><div key={c.condiciones}><h3>{c.condiciones}</h3><p>Requerido: {money(c.requerido)}<br/>Acreditado y aplicado por medios admitidos: {money(c.aplicado)}<br/><strong>Falta: {money(c.pendiente)}</strong></p></div>)}</div></details>
  {data.bloqueos.length>0&&<div className={data.estado==="LISTO_PARA_ENTREGA"?"client-note":"client-warning"}>{data.bloqueos.map(b=><p key={b}>{b}</p>)}</div>}
  {data.items.map(i=>{const last=i.trabajos.at(-1),work=order.oferta.items.find(o=>o.codigoPublico===i.codigoPublico);return <article className="production-item" key={i.codigoPublico}><div className="client-grid production-work-grid"><section><h3>{i.nombre}</h3><p>{work?.formato} · {work?.papel}<br/>{work?.trabajo.paginas} páginas × {work?.trabajo.copias} copias · {work?.trabajo.dobleFaz?"Doble faz":"Simple faz"}<br/>{work?.trabajo.color==="COLOR"?"Color":"Blanco y negro"}</p><p>Terminaciones: {work?.precio.lineas.slice(1).map(l=>l.nombre).join(", ")||"Sin terminaciones"}</p><a href="#pedido-pdf">Consultar PDF confirmado y vista previa</a></section><section className="production-current"><h3>Trabajo actual</h3>{last?<><strong>IMP-{last.numero} · {jobState(last.estado)}</strong><p>{last.impresora??"Recurso manual fuera del inventario"}<br/>Registrado por {last.operador} · {date(last.inicio)}</p>{last.fin&&<p>Finalizado: {date(last.fin)}</p>}{last.calidad&&<p className={last.calidad.resultado==="APROBADO"?"client-success":"client-warning"}>{jobState(last.calidad.resultado)} · {last.calidad.inspector}</p>}</>:<p>Aún no se inició este ítem.</p>}</section></div>
   <div className="client-actions">{last?.calidad?.resultado!=="APROBADO"&&<button className="client-button order-approve" disabled={locked||!!action||!i.puedeIniciar} onClick={()=>choose({tipo:"INICIAR",item:i})}>{!last?"Iniciar trabajo manual":last.calidad?"Iniciar reimpresión manual":"Reintentar trabajo manual"}</button>}
   {last?.estado==="IMPRIMIENDO"&&<>{(["COMPLETAR","ERROR","CANCELAR"] as const).map(t=><button key={t} className={`client-button ${t==="COMPLETAR"?"order-approve":"order-danger"}`} disabled={locked||!!action||!data.puedeProducir} onClick={()=>choose({tipo:t,item:i,job:last})}>{t==="COMPLETAR"?"Registrar trabajo terminado":t==="ERROR"?"Registrar error":"Cancelar trabajo"}</button>)}</>}
   {last?.estado==="COMPLETADO"&&!last.calidad&&<button className="client-button order-approve" disabled={locked||!!action||!data.puedeControlarCalidad} onClick={()=>choose({tipo:"CALIDAD",item:i,job:last})}>Controlar calidad</button>}</div>
   {i.bloqueo&&<p className="client-muted">{i.bloqueo}</p>}{!data.puedeControlarCalidad&&last?.estado==="COMPLETADO"&&!last.calidad&&<p className="client-warning">Se requiere el permiso de control de calidad.</p>}
   {i.trabajos.length>0&&<details><summary>Historial de trabajos e inspecciones ({i.trabajos.length})</summary>{i.trabajos.map(t=><section className="order-private-note" key={t.codigoPublico}><h4>IMP-{t.numero} · {jobState(t.estado)}{t.origen?" · Continuación de un intento anterior":""}</h4><p>{t.impresora??"Recurso manual fuera del inventario"}{t.motivoExcepcion&&` · ${t.motivoExcepcion}`}</p>{t.historial.map((e,n)=><p className="order-text" key={n}><strong>{actionLabels[e.accion as keyof typeof actionLabels]} · {e.actor} · {date(e.fecha)}</strong><br/>{e.motivo}</p>)}{t.calidad&&<><h4>{jobState(t.calidad.resultado)}</h4><ul className="production-checklist">{Object.entries(checks).map(([k,label])=><li key={k}>{t.calidad!.checklist[k as keyof Checklist]?"✓":"Pendiente / observado"} · {label}</li>)}</ul></>}</section>)}</details>}
  </article>;})}
  {action&&<form ref={form} className="client-form production-form" key={`${action.tipo}-${action.item.codigoPublico}`} onSubmit={e=>{e.preventDefault();void save();}}><header><h3>{action.tipo==="CALIDAD"?"Control de calidad":action.tipo==="INICIAR"?"Iniciar producción manual":action.tipo==="COMPLETAR"?"Registrar trabajo terminado":action.tipo==="ERROR"?"Registrar error de producción":"Cancelar trabajo manual"}</h3><p>{action.item.nombre}{action.job&&` · IMP-${action.job.numero}`}</p></header><fieldset disabled={locked}><div className={`client-grid ${action.tipo==="CALIDAD"?"three production-quality-grid":""}`}>
   {action.tipo==="CALIDAD"&&<section className="production-quality-details"><h4>Detalles del trabajo</h4><p>{selectedWork?.formato} · {selectedWork?.papel}<br/>{selectedWork?.trabajo.color==="COLOR"?"Color":"Blanco y negro"}<br/>{selectedWork?.trabajo.paginas} páginas × {selectedWork?.trabajo.copias} copias<br/>{selectedWork?.trabajo.dobleFaz?"Doble faz":"Simple faz"}</p><p>Opcionales: {selectedWork?.precio.lineas.slice(1).map(l=>l.nombre).join(", ")||"Sin terminaciones"}</p><p>Recurso: {action.job?.impresora??"Manual fuera del inventario"}</p><a href={`/operacion/pedidos/${order.codigoPublico}#pedido-pdf`} target="_blank" rel="noopener">Abrir PDF confirmado en otra pestaña</a></section>}
   <section className={action.tipo==="CALIDAD"?"production-quality-checks":undefined}>{action.tipo==="CALIDAD"?<><h4>Checklist de calidad</h4>{Object.entries(checks).map(([k,label])=><label className="client-check" key={k}><input type="checkbox" name={k} required={result==="APROBADO"}/>{label}</label>)}<p className="client-muted">Si no hay terminaciones, comprobá que el trabajo corresponde a lo solicitado y marcá el control como correcto.</p></>:<><h4>PDF y recurso</h4><p>El trabajo incluye todas las copias de este ítem. No modifica el PDF, el precio ni los pagos.</p>{action.tipo==="INICIAR"?<label>Impresora o recurso manual<select aria-label="Impresora o recurso manual" required value={printer} onChange={e=>setPrinter(e.target.value)}><option value="" disabled>Seleccioná el recurso</option><option value="MANUAL">Recurso manual fuera del inventario</option>{action.item.impresoras.map(p=><option key={p.codigoPublico} value={p.codigoPublico}>{p.nombre} · declarada</option>)}</select></label>:<p>{action.job?.impresora??"Recurso manual fuera del inventario"}</p>}{printer==="MANUAL"&&<p className="client-warning">Indicá el recurso utilizado y el motivo de la excepción en las observaciones.</p>}{["CANCELAR","ERROR"].includes(action.tipo)&&<p className="client-warning">Este registro afecta al trabajo físico, conserva el pedido y permite un intento posterior. No cancela el pedido ni registra una devolución.</p>}</>}</section>
   <section className={action.tipo==="CALIDAD"?"production-quality-result":undefined}>{action.tipo==="CALIDAD"&&<label>Resultado de calidad<select aria-label="Resultado de calidad" required value={result} onChange={e=>setResult(e.target.value)}><option value="" disabled>Seleccioná el resultado</option><option value="APROBADO">Aprobar calidad</option><option value="REIMPRESION_REQUERIDA">Solicitar reimpresión</option><option value="INCIDENCIA">Registrar incidencia para repetir el trabajo</option></select></label>}<label>Observaciones internas / motivo<textarea name="motivo" required maxLength={500}/></label><label className="client-check"><input type="checkbox" name="confirmacion" required/>{action.tipo==="CALIDAD"?"Inspeccioné físicamente el trabajo y confirmo este resultado.":action.tipo==="INICIAR"?"Confirmo que inicio este trabajo manualmente con el PDF y las opciones indicadas.":"Confirmo el hecho físico que estoy registrando."}</label></section>
  </div><p className="client-note">{action.tipo==="CALIDAD"?"Aprobar todos los ítems dejará el pedido listo para entrega o despacho. No lo marca entregado ni cancela el saldo.":"El sistema guarda este registro; no envía instrucciones a impresoras."}</p><div className="client-actions"><button type="button" className="client-button order-danger" onClick={()=>setAction(null)}>Volver sin guardar</button><button className="client-button order-approve">{busy?"Guardando…":"Confirmar registro manual"}</button></div></fieldset></form>}
  {data.estado==="LISTO_PARA_ENTREGA"&&<p className="client-success">{order.reserva.modalidad==="RETIRO_SUCURSAL"?"Listo para entregar en la sucursal.":"La producción y calidad están completas. Consultá el recorrido actual en logística y entrega."} La entrega efectiva, su código y el cierre se registran en logística y entrega.</p>}
  </>}
 </section>;
}
