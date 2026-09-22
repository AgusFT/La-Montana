"use client";
import { useState } from "react";
import { readCatalog } from "@/lib/catalog-client";
import { CatalogMasters } from "@/components/catalog-masters";
import { CatalogEditor } from "@/components/catalog-editor";
import { RevisionView } from "@/components/catalog-view";
import { catalogDate, isCatalogRevision, type CatalogState, type CatalogRevision } from "@/lib/catalog-types";


export function CatalogWorkspace({initial}:{initial:CatalogState}){
  const [catalog,setCatalog]=useState(initial),[history,setHistory]=useState<CatalogRevision|null>(null),[historyError,setHistoryError]=useState(""),[loadingHistory,setLoadingHistory]=useState(false);
  async function refresh(){setCatalog(await readCatalog());}
  function saved(revision:CatalogRevision){setCatalog(current=>({...current,actual:revision,historial:[revision,...current.historial.filter(r=>r.codigoPublico!==revision.codigoPublico)]}));}
  async function openHistory(id:string){setLoadingHistory(true);setHistoryError("");try{const response=await fetch(`/api/admin/catalogo/revisiones/${id}`,{cache:"no-store",credentials:"same-origin"});if(!response.ok)throw new Error("No pudimos abrir la revisión.");const data:unknown=await response.json();if(!isCatalogRevision(data))throw new Error("La revisión tiene un formato inesperado.");setHistory(data);}catch(error){setHistoryError(error instanceof Error?error.message:"No pudimos abrir la revisión.");}finally{setLoadingHistory(false);}}
  return <>
    <div className="catalog-overview"><div className="admin-info"><strong>Catálogo comercial global</strong><p>Las revisiones conservan sus precios y condiciones. El configurador y la creación de pedidos están En construcción.</p></div><div className={`catalog-current ${catalog.actual?"is-active":""}`}><strong>{catalog.actual?`Revisión vigente ${catalog.actual.numero}`:"Sin revisión vigente"}</strong><small>{catalog.actual?`${catalogDate(catalog.actual.creadaEn)} · ${catalog.actual.actor}`:"Comenzá creando el catálogo base y sus precios."}</small></div></div>
    <nav className="admin-tabs" aria-label="Secciones del catálogo"><a href="#revision-comercial">Tarifas y servicios</a><a href="#catalogo-base">Catálogo base</a><a href="#historial-comercial">Historial de revisiones</a></nav>
    <CatalogEditor catalog={catalog} onSaved={saved}/>
    <CatalogMasters catalog={catalog} onCreated={refresh}/>
    <section id="historial-comercial" className="admin-card"><div className="admin-section-title"><div><h2>Historial de revisiones</h2><p>Consultá los cambios guardados. Las revisiones anteriores son de solo lectura.</p></div></div>
      {catalog.historial.length===0?<p className="admin-empty">Todavía no se guardaron revisiones comerciales.</p>:<div className="admin-table-wrap" tabIndex={0} aria-label="Historial comercial"><table className="admin-table"><thead><tr><th>Revisión</th><th>Fecha</th><th>Responsable</th><th>Motivo</th><th>Detalle</th></tr></thead><tbody>{catalog.historial.map(revision=><tr key={revision.codigoPublico}><td>{revision.numero}</td><td>{catalogDate(revision.creadaEn)}</td><td>{revision.actor}</td><td>{revision.motivo}</td><td><button type="button" className="admin-link-button" disabled={loadingHistory} onClick={()=>openHistory(revision.codigoPublico)}>Ver revisión {revision.numero}</button></td></tr>)}</tbody></table></div>}
      {historyError&&<p role="alert" className="form-message error-message">{historyError}</p>}{history&&<div className="catalog-history-detail"><div className="admin-section-title"><h3>Detalle de solo lectura</h3><button type="button" className="admin-link-button" onClick={()=>setHistory(null)}>Cerrar detalle</button></div><RevisionView catalog={catalog} revision={history}/></div>}
    </section>
    <div className="admin-page-footer"><a href="/administracion">← Volver al dashboard</a><span>Programar cambios: En construcción</span></div>
  </>;
}
