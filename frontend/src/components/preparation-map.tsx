//#region ENCABEZADO · src/components/preparation-map.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/preparation-map.tsx
 * ========================================================================
 * FUNCIÓN
 * Consulta el estado real de preparación de la imprenta y muestra una secuencia visual de pasos,
 * pendientes y enlaces sólo hasta la primera activación exitosa. Actualiza el estado al volver
 * a la pestaña o restaurar la página, sin mostrar el mapa mientras se desconoce la instalación.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - valid(value: unknown): value is Preparation
 * - [export] PreparationMap()
 *   Componente de interfaz.
 * - PreparationMap :: refresh()
 *   Vuelve a consultar el estado persistido de la instalación.
 * - PreparationMap :: restore(event: PageTransitionEvent)
 *   Actualiza al volver mediante la caché de navegación del navegador.
 * - PreparationMap :: visible()
 *   Actualiza al regresar a la pestaña, por si se activó en otra.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Step (tipo).
 * - Preparation (tipo).
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useState} from "react";
type Step={codigo:string;titulo:string;completo:boolean;estado:string;detalle:string;enlace:string;complementario:boolean};
type Preparation={origen:string;numero:number|null;activa:boolean;borradorPendiente:number|null;pasos:Step[]};
function valid(value:unknown):value is Preparation{
 if(!value||typeof value!=="object")return false;const x=value as Preparation;
 return typeof x.origen==="string"&&typeof x.activa==="boolean"&&[x.numero,x.borradorPendiente].every(v=>v===null||Number.isInteger(v))&&Array.isArray(x.pasos)&&x.pasos.length>=7&&x.pasos.every(s=>s&&typeof s.completo==="boolean"&&typeof s.complementario==="boolean"&&[s.codigo,s.titulo,s.estado,s.detalle,s.enlace].every(v=>typeof v==="string")&&(s.enlace.startsWith("/administracion")||s.codigo==="propietario"&&s.enlace==="/cuenta/seguridad"));
}
export function PreparationMap(){
 const[data,setData]=useState<Preparation|null>(null),[loading,setLoading]=useState(true),[error,setError]=useState(false),[epoch,setEpoch]=useState(0);
 useEffect(()=>{const abort=new AbortController();setLoading(true);setError(false);void(async()=>{try{const r=await fetch("/api/admin/preparacion",{cache:"no-store",signal:abort.signal}),value:unknown=await r.json();if(!r.ok||!valid(value))throw new Error();if(!abort.signal.aborted)setData(value);}catch{if(!abort.signal.aborted){setData(null);setError(true);}}finally{if(!abort.signal.aborted)setLoading(false);}})();return()=>abort.abort();},[epoch]);
 useEffect(()=>{
  function refresh(){setLoading(true);setData(null);setEpoch(v=>v+1);}
  function restore(event:PageTransitionEvent){if(event.persisted)refresh();}
  function visible(){if(document.visibilityState==="visible")refresh();}
  window.addEventListener("pageshow",restore);document.addEventListener("visibilitychange",visible);
  return()=>{window.removeEventListener("pageshow",restore);document.removeEventListener("visibilitychange",visible);};
 },[]);
 if(loading&&!data||data?.activa)return null;
 if(error)return <p className="admin-error" role="alert">No pudimos consultar el estado de la instalación. <button type="button" className="admin-button secondary" onClick={()=>setEpoch(v=>v+1)}>Volver a consultar instalación</button></p>;
 if(!data)return null;
 const next=data.pasos.find(s=>!s.completo&&!s.complementario);
 return <section className="preparation-panel" aria-labelledby="preparation-title"><header><div><p className="admin-note">TU RECORRIDO DE CONFIGURACIÓN</p><h2 id="preparation-title">Prepará tu imprenta, paso a paso</h2></div><button className="admin-button secondary" disabled={loading} onClick={()=>setEpoch(v=>v+1)}>Actualizar preparación</button></header><p>Verde: configurado y válido. Rojo: pendiente o requiere corrección. Este recorrido acompaña la primera instalación y desaparece cuando activás la primera configuración.</p>
 {loading?<p role="status">Comprobando la preparación con los datos guardados…</p>:<>
 {data.numero!==null&&<p className="admin-info">Estás preparando V{data.numero}. Los pasos guardados todavía no habilitan pedidos; falta activar la configuración.</p>}
 {next&&<p className="preparation-next"><strong>Próximo paso:</strong> <a href={next.enlace}>{next.titulo} →</a></p>}
 <ol className="preparation-timeline">{data.pasos.filter(s=>!s.complementario).map((s,i)=><li key={s.codigo} className={s.completo?"ready":"pending"}><span className="preparation-marker" aria-hidden="true">{s.completo?"✓":i+1}</span><div><h3>{s.titulo}</h3><strong className="preparation-state">{s.completo?"✓":"!"} {s.estado}</strong><p>{s.detalle}</p><a href={s.enlace}>{s.completo?"Revisar":"Configurar"} →</a></div></li>)}</ol>
 <h3>También podés preparar</h3><p className="admin-note">Estos pasos son complementarios: no impiden que operes como propietario.</p><div className="preparation-optional">{data.pasos.filter(s=>s.complementario).map(s=><article key={s.codigo} className={s.completo?"ready":"optional"}><h3>{s.titulo}</h3><strong>{s.completo?"✓ ":"○ "}{s.estado}</strong><p>{s.detalle}</p><a href={s.enlace}>Abrir configuración →</a></article>)}</div>
 </>}
 </section>;
}
