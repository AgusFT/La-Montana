//#region ENCABEZADO · src/components/catalog-workspace.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-workspace.tsx
 * ========================================================================
 * FUNCIÓN
 * Coordina las pestañas de catálogo base, tarifas y servicios e historial. Refresca el estado y
 * permite consultar configuraciones anteriores sin descartar el trabajo al cambiar de pestaña.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] CatalogWorkspace({initial}: {initial:CatalogState})
 *   Componente de interfaz.
 * - CatalogWorkspace :: sync()
 * - CatalogWorkspace :: tabAllowed(id: string)
 *   Impide saltar la base durante la primera instalación.
 * - CatalogWorkspace :: selectTab(id: string)
 * - [async] CatalogWorkspace :: refresh()
 * - CatalogWorkspace :: saved(revision: CatalogRevision)
 * - [async] CatalogWorkspace :: openHistory(id: string)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";
import {useInstallation} from "./installation-guide";
import { useEffect, useState } from "react";
import { readCatalog } from "@/lib/catalog-client";
import { CatalogMasters } from "@/components/catalog-masters";
import { CatalogSchedule } from "@/components/catalog-schedule";
import { CatalogEditor } from "@/components/catalog-editor";
import { RevisionView } from "@/components/catalog-view";
import { catalogDate, isCatalogRevision, type CatalogState, type CatalogRevision } from "@/lib/catalog-types";


export function CatalogWorkspace({initial}:{initial:CatalogState}){
  const sections=["catalogo-base","revision-comercial","historial-comercial"] as const;
  const [tab,setTab]=useState<string>("catalogo-base");

  const guide=useInstallation(),firstSetup=guide.enabled&&!guide.data?.activa;
  function selectTab(id:string){if(!tabAllowed(id))return;setTab(id);window.history.replaceState(null,"",`#${id}`);}
  const [editorEpoch,setEditorEpoch]=useState(0);
  const [catalog,setCatalog]=useState(initial),[history,setHistory]=useState<CatalogRevision|null>(null),[historyError,setHistoryError]=useState(""),[loadingHistory,setLoadingHistory]=useState(false);
  const papersReady=catalog.papelesHabilitados.some(p=>p.habilitado),servicesReady=catalog.servicios.some(s=>s.tipo==="IMPRESION"),baseReady=papersReady&&servicesReady;
  function tabAllowed(id:string){return !firstSetup||id==="catalogo-base"||baseReady&&(id==="revision-comercial"||!!catalog.actual);}
  useEffect(()=>{const sync=()=>{const hash=location.hash.slice(1),next=sections.includes(hash as typeof sections[number])?hash:"catalogo-base";const allowed=tabAllowed(next)?next:"catalogo-base";setTab(allowed);if(allowed!==next)window.history.replaceState(null,"",`#${allowed}`);};sync();window.addEventListener("hashchange",sync);return()=>window.removeEventListener("hashchange",sync);},[firstSetup,baseReady,!!catalog.actual]);
  async function refresh(){const next=await readCatalog();if(catalog.programada&&!next.programada&&next.actual?.codigoPublico!==catalog.actual?.codigoPublico)setEditorEpoch(value=>value+1);setCatalog(next);}
  function saved(revision:CatalogRevision){setCatalog(current=>({...current,...(revision.estado==="PROGRAMADA"?{programada:revision}:revision.estado==="VIGENTE"?{actual:revision,programada:null}:{}),historial:[revision,...current.historial.filter(r=>r.codigoPublico!==revision.codigoPublico).map(r=>revision.estado==="VIGENTE"&&r.estado==="VIGENTE"?{...r,estado:"HISTORICA" as const}:r)]}));void refresh().catch(()=>setHistoryError("El cambio fue guardado, pero no pudimos actualizar el estado. Recargá la página para consultar la vigencia actual."));}
  async function openHistory(id:string){setLoadingHistory(true);setHistoryError("");try{const response=await fetch(`/api/admin/catalogo/revisiones/${id}`,{cache:"no-store",credentials:"same-origin"});if(!response.ok)throw new Error("No pudimos abrir la configuración.");const data:unknown=await response.json();if(!isCatalogRevision(data))throw new Error("La configuración tiene un formato inesperado.");setHistory(data);}catch(error){setHistoryError(error instanceof Error?error.message:"No pudimos abrir la configuración.");}finally{setLoadingHistory(false);}}
  return <>
    <div className="catalog-overview"><div className="admin-info"><strong>Catálogo comercial global</strong><p>Las configuraciones conservan sus precios y condiciones. Las nuevas cotizaciones usan los precios vigentes; las ofertas aceptadas y los pedidos conservan sus importes.</p></div><div className={`catalog-current ${catalog.actual?"is-active":""}`}><strong>{catalog.actual?`Configuración vigente ${catalog.actual.numero}`:"Sin configuración vigente"}</strong><small>{catalog.actual?`${catalogDate(catalog.actual.activadaEn??catalog.actual.creadaEn)} · ${catalog.actual.actor}`:"Comenzá creando el catálogo base y sus precios."}</small></div></div>
    <nav className="admin-tabs catalog-tabs" role="tablist" aria-label="Secciones del catálogo">{sections.map((id,i)=><button key={id} id={`tab-${id}`} type="button" role="tab" disabled={!tabAllowed(id)} title={!tabAllowed(id)?"Primero habilitá un papel y creá un servicio base de impresión.":undefined} aria-selected={tab===id} aria-controls={`panel-${id}`} tabIndex={tab===id?0:-1} onClick={()=>selectTab(id)} onKeyDown={event=>{const next=event.key==="ArrowRight"?(i+1)%3:event.key==="ArrowLeft"?(i+2)%3:event.key==="Home"?0:event.key==="End"?2:null;if(next!==null){event.preventDefault();const available=sections.filter(tabAllowed);const target=event.key==="Home"?available[0]:event.key==="End"?available[available.length-1]:available[(available.indexOf(id as typeof sections[number])+(event.key==="ArrowLeft"?-1:1)+available.length)%available.length];selectTab(target);document.getElementById(`tab-${target}`)?.focus();}}}>{i+1}. {i===0?"Catálogo base":i===1?"Tarifas y servicios":"Historial de configuraciones"}</button>)}</nav>
    <div id="panel-catalogo-base" role="tabpanel" aria-labelledby="tab-catalogo-base" hidden={tab!=="catalogo-base"}>
      {firstSetup&&<ol className="installation-substeps"><li className={papersReady?"complete":""}>{papersReady?"✓ ":"1. "}Habilitar al menos un papel</li><li className={servicesReady?"complete":""}>{servicesReady?"✓ ":"2. "}Crear un servicio de impresión</li><li>3. Continuar a precios</li></ol>}
      <CatalogMasters catalog={catalog} onCreated={refresh}/>
      {firstSetup&&!baseReady&&<p className="admin-note">Para continuar, guardá al menos un papel habilitado y un servicio de tipo Impresión. Una terminación como anillado no reemplaza al servicio de impresión.</p>}
      <button type="button" className="admin-button" disabled={firstSetup&&!baseReady} onClick={()=>selectTab("revision-comercial")}>Continuar a tarifas y servicios →</button>
    </div>
    <div id="panel-revision-comercial" role="tabpanel" aria-labelledby="tab-revision-comercial" hidden={tab!=="revision-comercial"}>
    {catalog.programada&&<CatalogSchedule key={catalog.programada.codigoPublico} revision={catalog.programada} onChanged={refresh}/>}
    <CatalogEditor key={editorEpoch} catalog={catalog} onSaved={saved}/>
    {firstSetup&&catalog.actual&&<div className="configuration-actions"><a className="admin-button" href="/administracion/configuracion">Continuar al modelo operativo →</a></div>}
    </div>
    <div id="panel-historial-comercial" role="tabpanel" aria-labelledby="tab-historial-comercial" hidden={tab!=="historial-comercial"}>
    <section id="historial-comercial" className="admin-card"><div className="admin-section-title"><div><h2>Historial de configuraciones</h2><p>Consultá los cambios guardados. Las configuraciones anteriores son de solo lectura.</p></div></div>
      {catalog.historial.length===0?<p className="admin-empty">Todavía no se guardaron configuraciones comerciales.</p>:<div className="admin-table-wrap" tabIndex={0} aria-label="Historial comercial"><table className="admin-table"><thead><tr><th>Configuración</th><th>Fecha</th><th>Estado</th><th>Responsable</th><th>Motivo</th><th>Detalle</th></tr></thead><tbody>{catalog.historial.map(revision=><tr key={revision.codigoPublico}><td>{revision.numero}</td><td>{catalogDate(revision.creadaEn)}</td><td>{revision.estado}{revision.programadaPara&&<small>{catalogDate(revision.programadaPara)}</small>}</td><td>{revision.actor}</td><td>{revision.motivo}</td><td><button type="button" className="admin-link-button" disabled={loadingHistory} onClick={()=>openHistory(revision.codigoPublico)}>Ver configuración {revision.numero}</button></td></tr>)}</tbody></table></div>}
      {historyError&&<p role="alert" className="form-message error-message">{historyError}</p>}{history&&<div className="catalog-history-detail"><div className="admin-section-title"><h3>Detalle de solo lectura</h3><button type="button" className="admin-link-button" onClick={()=>setHistory(null)}>Cerrar detalle</button></div><RevisionView catalog={catalog} revision={history}/></div>}
    </section>
    </div>
    <div className="admin-page-footer"><a href="/administracion">← Volver al dashboard</a><span>Configuraciones comerciales independientes del configurador</span></div>
  </>;
}
