//#region ENCABEZADO · preparation-map.tsx
/*
 * ========================================================================
 * FUNCIÓN: Muestra la instalación inicial en orden con pasos guardados,
 * siguiente paso y bloqueos; se oculta tras la primera activación exitosa.
 * FUNCIONES: PreparationMap(). Comparte el progreso del marco administrativo.
 * ========================================================================
 */
//#endregion
"use client";
import {useInstallation} from "./installation-guide";
import {nextStep} from "@/lib/installation-types";
export function PreparationMap(){
 const {data,loading,error,refresh}=useInstallation();
 if(data?.activa)return null;
 if(error)return <p className="admin-error" role="alert">No pudimos consultar el estado de la instalación. <button type="button" className="admin-button secondary" disabled={loading} onClick={()=>void refresh()}>Volver a consultar instalación</button></p>;
 if(!data)return null;
 const next=nextStep(data);
 return <section className="preparation-panel" aria-labelledby="preparation-title"><header><div><p className="admin-note">TU PRIMERA INSTALACIÓN</p><h2 id="preparation-title">Prepará tu imprenta, paso a paso</h2></div><button className="admin-button secondary" disabled={loading} onClick={()=>void refresh()}>Actualizar preparación</button></header>
 <p>Completá los pasos en orden. Verde: guardado y válido. El siguiente paso se habilita al completar sus requisitos. Podés volver a los anteriores para corregirlos. Las simulaciones son opcionales.</p>
 {next&&<p className="preparation-next"><strong>Continuá por acá:</strong> <a href={next.enlace}>{next.titulo} →</a></p>}
 <ol className="preparation-timeline">{data.guia.etapas.map((s,i)=><li key={s.codigo} className={s.completa?"ready":s.habilitada?"pending":"preparation-locked"}><span className="preparation-marker" aria-hidden="true">{s.completa?"✓":i+1}</span><div><h3>{s.titulo}</h3><strong className="preparation-state">{s.completa?"✓ Guardado":s.habilitada?"Pendiente · continuar ahora":"Bloqueado · completá los pasos anteriores"}</strong>{s.habilitada?<a href={s.enlace}>{s.completa?"Revisar":"Continuar"} →</a>:<span className="preparation-lock-text">Primero: {next?.titulo}.</span>}</div></li>)}</ol>
 <h3>Complementos opcionales</h3><p className="admin-note">La página web y las cuentas de empleados pueden completarse después. No bloquean tu primera activación.</p><div className="preparation-optional">{data.pasos.filter(s=>s.complementario).map(s=><article key={s.codigo} className={s.completo?"ready":"optional"}><h3>{s.titulo}</h3><strong>{s.completo?"✓ ":"○ "}{s.estado}</strong><p>{s.detalle}</p><a href={s.enlace}>Abrir configuración →</a></article>)}</div>
 </section>;
}
