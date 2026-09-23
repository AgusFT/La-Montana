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
 const next=data?.pasos.find(s=>!s.completo&&!s.complementario);
 return <section className="preparation-panel" aria-labelledby="preparation-title"><header><div><p className="admin-note">TU RECORRIDO DE CONFIGURACIÓN</p><h2 id="preparation-title">Prepará tu imprenta, paso a paso</h2></div><button className="admin-button secondary" disabled={loading} onClick={()=>setEpoch(v=>v+1)}>Actualizar preparación</button></header><p>Verde: configurado y válido. Rojo: pendiente o requiere corrección. Cada paso indica si está guardado en un borrador o ya activo.</p>
 {loading?<p role="status">Comprobando la preparación con los datos guardados…</p>:error?<p role="alert" className="admin-error">No pudimos comprobarlo. Usá Actualizar preparación para volver a intentar; no se asignaron estados a los pasos.</p>:data&&<>
 {data.activa&&<p className="admin-info">Este mapa comprueba la configuración activa V{data.numero}.{data.borradorPendiente!==null&&` También hay cambios en preparación en el borrador V${data.borradorPendiente}; todavía no reemplazan la versión vigente.`}</p>}
 {!data.activa&&data.numero!==null&&<p className="admin-info">Estás preparando V{data.numero}. Los pasos guardados todavía no habilitan pedidos; falta activar la configuración.</p>}
 {next&&<p className="preparation-next"><strong>Próximo paso:</strong> <a href={next.enlace}>{next.titulo} →</a></p>}
 <ol className="preparation-timeline">{data.pasos.filter(s=>!s.complementario).map((s,i)=><li key={s.codigo} className={s.completo?"ready":"pending"}><span className="preparation-marker" aria-hidden="true">{s.completo?"✓":i+1}</span><div><h3>{s.titulo}</h3><strong className="preparation-state">{s.completo?"✓":"!"} {s.estado}</strong><p>{s.detalle}</p><a href={s.enlace}>{s.completo?"Revisar":"Configurar"} →</a></div></li>)}</ol>
 <h3>También podés preparar</h3><p className="admin-note">Estos pasos son complementarios: no impiden que operes como propietario.</p><div className="preparation-optional">{data.pasos.filter(s=>s.complementario).map(s=><article key={s.codigo} className={s.completo?"ready":"optional"}><h3>{s.titulo}</h3><strong>{s.completo?"✓ ":"○ "}{s.estado}</strong><p>{s.detalle}</p><a href={s.enlace}>Abrir configuración →</a></article>)}</div>
 </>}
 </section>;
}
