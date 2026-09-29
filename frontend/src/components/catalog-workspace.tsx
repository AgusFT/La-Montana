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
import { GuidedTour, type TourStep } from "@/components/guided-tour";
import { RevisionView } from "@/components/catalog-view";
import { catalogDate, isCatalogRevision, type CatalogState, type CatalogRevision } from "@/lib/catalog-types";

const catalogTour:TourStep[]=[
  {target:"tab-catalogo-base",title:"Configurá tu catálogo base",description:"Acá definís la base comercial de tu imprenta. Primero elegís los papeles que usás y después creás los servicios que ofrecés. Esta base será la referencia para las futuras cotizaciones."},
  {target:"catalog-papers-tab",title:"Primero, elegí los papeles de tu imprenta",description:"Habilitá los tamaños y gramajes que realmente usás. Necesitás al menos un papel activo para poder armar tu catálogo comercial."},
  {target:"catalog-services-tab",title:"Después, agregá tus servicios",description:"Creá al menos un servicio base, como impresión blanco y negro, color o terminaciones. Luego vas a poder asignarles precios en la sección Tarifas."},
];
const catalogTourCompletion={title:"¡Listo! Ya entendés cómo armar el catálogo base",description:"Primero definí papeles y servicios. Después continuá en Tarifas para completar los precios. Más adelante vas a poder volver a esta pantalla y seguir ajustando tu catálogo cuando lo necesites."};
const tariffTour:TourStep[]=[
  {target:"tariff-tour-service",title:"Primero, elegí el servicio",description:"Los servicios de impresión de tu catálogo usan estas tarifas. Primero elegís el modo de impresión, blanco y negro o color; después configurás a qué papeles y variantes se aplica. Los servicios ofrecidos se completan más abajo."},
  {target:"tariff-tour-papers",title:"Después, seleccioná las hojas y sus variantes",description:"Elegí el tamaño de hoja y sus variantes, como gramaje o tipo de papel. La tarifa que estás armando se aplicará solamente a las variantes que selecciones acá. Si necesitás precios diferentes para otros papeles o gramajes, podés crear otra tarifa."},
  {target:"tariff-tour-prices",title:"Ahora definí los precios",description:"Primero cargás el valor de impresión simple faz. Después configurás cómo se calcula la doble faz, según la regla comercial que use tu imprenta. El sistema va a utilizar estos importes como base para calcular nuevas cotizaciones."},
  {target:"tariff-tour-apply",secondaryTarget:"tariff-tour-publish",secondaryLabel:"Ver dónde se guarda la configuración →",title:"Aplicá la tarifa y guardá la configuración",description:"Cuando terminás este formulario, aplicás la tarifa al borrador. Después, al guardar la configuración completa, estos precios quedan listos para utilizarse en nuevas cotizaciones. Los cambios nuevos afectan cotizaciones futuras. Los pedidos ya aceptados conservan sus condiciones anteriores."},
];
const tariffTourCompletion={title:"¡Listo! Ya configuraste una tarifa base",description:"Primero elegiste el servicio, después seleccionaste las hojas y sus variantes, y finalmente definiste el precio simple faz y la regla de doble faz. Si necesitás más combinaciones, podés crear nuevas tarifas."};

export function CatalogWorkspace({initial,tourOwner}:{initial:CatalogState;tourOwner:string}){
  const sections=["catalogo-base","revision-comercial","historial-comercial"] as const;
  const [tab,setTab]=useState<string>("catalogo-base");
  const [tariffTourActive,setTariffTourActive]=useState(false);
  const [block,setBlock]=useState<"papeles"|"servicios">("papeles");

  const guide=useInstallation(),firstSetup=guide.enabled&&!guide.data?.activa;
  function selectTab(id:string){if(!tabAllowed(id))return;setTab(id);window.history.replaceState(null,"",`#${id}`);}
  const [editorEpoch,setEditorEpoch]=useState(0);
  const [catalog,setCatalog]=useState(initial),[history,setHistory]=useState<CatalogRevision|null>(null),[historyError,setHistoryError]=useState(""),[loadingHistory,setLoadingHistory]=useState(false);
  const papersReady=catalog.papelesHabilitados.some(p=>p.habilitado),servicesReady=catalog.servicios.length>0,baseReady=papersReady&&servicesReady;
  const printingReady=catalog.servicios.some(s=>s.tipo==="IMPRESION");
  const commercialReady=baseReady&&!!catalog.actual?.tarifas.some(t=>t.habilitada)&&!!catalog.actual?.servicios.some(o=>o.habilitado&&catalog.servicios.some(s=>s.codigoPublico===o.servicio&&s.tipo==="IMPRESION"));
  function tabAllowed(id:string){return !firstSetup||id!=="revision-comercial"||baseReady;}
  useEffect(()=>{const sync=()=>{const hash=location.hash.slice(1),next=sections.includes(hash as typeof sections[number])?hash:"catalogo-base";const allowed=tabAllowed(next)?next:"catalogo-base";setTab(allowed);if(allowed!==next)window.history.replaceState(null,"",`#${allowed}`);};sync();window.addEventListener("hashchange",sync);return()=>window.removeEventListener("hashchange",sync);},[firstSetup,baseReady,!!catalog.actual]);
  async function refresh(){const next=await readCatalog();if(catalog.programada&&!next.programada&&next.actual?.codigoPublico!==catalog.actual?.codigoPublico)setEditorEpoch(value=>value+1);setCatalog(next);}
  function saved(revision:CatalogRevision){setCatalog(current=>({...current,...(revision.estado==="PROGRAMADA"?{programada:revision}:revision.estado==="VIGENTE"?{actual:revision,programada:null}:{}),historial:[revision,...current.historial.filter(r=>r.codigoPublico!==revision.codigoPublico).map(r=>revision.estado==="VIGENTE"&&r.estado==="VIGENTE"?{...r,estado:"HISTORICA" as const}:r)]}));void refresh().catch(()=>setHistoryError("El cambio fue guardado, pero no pudimos actualizar el estado. Recargá la página para consultar la vigencia actual."));}
  async function openHistory(id:string){setLoadingHistory(true);setHistoryError("");try{const response=await fetch(`/api/admin/catalogo/revisiones/${id}`,{cache:"no-store",credentials:"same-origin"});if(!response.ok)throw new Error("No pudimos abrir la configuración.");const data:unknown=await response.json();if(!isCatalogRevision(data))throw new Error("La configuración tiene un formato inesperado.");setHistory(data);}catch(error){setHistoryError(error instanceof Error?error.message:"No pudimos abrir la configuración.");}finally{setLoadingHistory(false);}}
  return <>
    {firstSetup&&<section className="installation-guide catalog-installation" aria-label="Avance comercial de la primera instalación"><div><strong>Primera instalación · pasos en orden</strong><p>{!papersReady?"Primero, habilitá un papel.":!servicesReady?"Ahora creá un servicio base.":!commercialReady?"Catálogo base completo. Continuá en Tarifas y guardá una configuración comercial válida.":"Catálogo comercial vigente. Ya podés continuar con el modelo operativo."} Guardá cada paso antes de continuar.</p></div><div className="catalog-installation-action"><button type="button" className="admin-button" disabled={!commercialReady||guide.loading||guide.error} onClick={()=>{window.location.href="/administracion/configuracion";}}>Continuar instalación →</button>{(!commercialReady||guide.error)&&<small>{guide.error?"No pudimos comprobar la instalación. Actualizá la página para reintentar.":"Completá el catálogo base y una tarifa válida para continuar."}</small>}</div></section>}
    <div className="catalog-overview"><div className="admin-info"><strong>Catálogo comercial global</strong><p>Las configuraciones conservan sus precios y condiciones. Las nuevas cotizaciones usan los precios vigentes; las ofertas aceptadas y los pedidos conservan sus importes.</p></div><div className={`catalog-current ${catalog.actual?"is-active":""}`}><strong>{catalog.actual?`Configuración vigente ${catalog.actual.numero}`:"Sin configuración vigente"}</strong><small>{catalog.actual?`${catalogDate(catalog.actual.activadaEn??catalog.actual.creadaEn)} · ${catalog.actual.actor}`:"Comenzá creando el catálogo base y sus precios."}</small></div></div>
    <nav className="admin-tabs catalog-tabs" role="tablist" aria-label="Secciones del catálogo">{sections.map((id,i)=><button key={id} id={`tab-${id}`} type="button" role="tab" disabled={!tabAllowed(id)} title={!tabAllowed(id)?"Primero habilitá un papel y creá un servicio base.":undefined} aria-selected={tab===id} aria-controls={`panel-${id}`} tabIndex={tab===id?0:-1} onClick={()=>selectTab(id)} onKeyDown={event=>{const next=event.key==="ArrowRight"?(i+1)%3:event.key==="ArrowLeft"?(i+2)%3:event.key==="Home"?0:event.key==="End"?2:null;if(next!==null){event.preventDefault();const available=sections.filter(tabAllowed);const target=event.key==="Home"?available[0]:event.key==="End"?available[available.length-1]:available[(available.indexOf(id as typeof sections[number])+(event.key==="ArrowLeft"?-1:1)+available.length)%available.length];selectTab(target);document.getElementById(`tab-${target}`)?.focus();}}}>{i+1}. {i===0?"Catálogo base":i===1?"Tarifas":"Historial de configuraciones"}</button>)}</nav>
    <div id="panel-catalogo-base" role="tabpanel" aria-labelledby="tab-catalogo-base" hidden={tab!=="catalogo-base"}>
      <div className="catalog-base-intro"><div><h2>Catálogo base</h2><p>Primero configurá los papeles de tu imprenta y luego creá los servicios. Ambos forman la base de tus futuras cotizaciones.</p></div><div className="catalog-base-progress"><strong>Progreso del catálogo base · {[papersReady,servicesReady].filter(Boolean).length} de 2</strong><progress max="2" value={Number(papersReady)+Number(servicesReady)} aria-label="Progreso del catálogo base"/><ol><li className={papersReady?"complete":""}>{papersReady?"✓":"○"} 1. Habilitar al menos un papel</li><li className={servicesReady?"complete":""}>{servicesReady?"✓":"○"} 2. Crear al menos un servicio</li><li className={commercialReady?"complete":""}>{commercialReady?"✓":"○"} 3. Guardar una tarifa y configuración comercial válida</li></ol></div></div>
      <nav className="catalog-subnav" aria-label="Pasos del catálogo base"><button id="catalog-papers-tab" type="button" className={block==="papeles"?"active":""} aria-current={block==="papeles"?"step":undefined} onClick={()=>setBlock("papeles")}>1. Papeles {papersReady&&<span aria-label="completo">✓</span>}</button><button id="catalog-services-tab" type="button" className={block==="servicios"?"active":""} aria-current={block==="servicios"?"step":undefined} onClick={()=>setBlock("servicios")}>2. Servicios {servicesReady&&<span aria-label="completo">✓</span>}</button></nav>
      <CatalogMasters catalog={catalog} onCreated={refresh} activeBlock={block}/>
      <div className="catalog-next"><p>{!papersReady?"Habilitá al menos un papel para continuar.":!servicesReady?"Ahora creá un servicio base.":!printingReady?"Ya podés abrir Tarifas. Para publicar precios necesitás además un servicio de impresión.":"Catálogo base completo. Ahora definí cuánto cuestan tus servicios."}</p>{block==="papeles"?<button type="button" className="admin-button" disabled={firstSetup&&!papersReady} onClick={()=>setBlock("servicios")}>Continuar a Servicios →</button>:<button type="button" className="admin-button" disabled={firstSetup&&!baseReady} onClick={()=>selectTab("revision-comercial")}>Continuar a Tarifas →</button>}</div>
    </div>
    <div id="panel-revision-comercial" role="tabpanel" aria-labelledby="tab-revision-comercial" hidden={tab!=="revision-comercial"}>
    <p className="catalog-context-note">Definí cuánto cuestan los servicios creados. Para publicar necesitás una tarifa habilitada y ofrecer al menos un servicio de impresión. Los cambios se aplican a nuevas cotizaciones al guardar una configuración.</p>
    {catalog.programada&&<CatalogSchedule key={catalog.programada.codigoPublico} revision={catalog.programada} onChanged={refresh}/>}
    <CatalogEditor key={editorEpoch} catalog={catalog} onSaved={saved} tourActive={tab==="revision-comercial"&&tariffTourActive}/>
    </div>
    <div id="panel-historial-comercial" role="tabpanel" aria-labelledby="tab-historial-comercial" hidden={tab!=="historial-comercial"}>
    <section id="historial-comercial" className="admin-card"><div className="admin-section-title"><div><h2>Historial de configuraciones</h2><p>Consultá los cambios guardados. Las configuraciones anteriores son de solo lectura.</p></div></div>
      {catalog.historial.length===0?<p className="admin-empty">Todavía no se guardaron configuraciones comerciales.</p>:<div className="admin-table-wrap" tabIndex={0} aria-label="Historial comercial"><table className="admin-table"><thead><tr><th>Configuración</th><th>Fecha</th><th>Estado</th><th>Responsable</th><th>Motivo</th><th>Detalle</th></tr></thead><tbody>{catalog.historial.map(revision=><tr key={revision.codigoPublico}><td>{revision.numero}</td><td>{catalogDate(revision.creadaEn)}</td><td>{revision.estado}{revision.programadaPara&&<small>{catalogDate(revision.programadaPara)}</small>}</td><td>{revision.actor}</td><td>{revision.motivo}</td><td><button type="button" className="admin-link-button" disabled={loadingHistory} onClick={()=>openHistory(revision.codigoPublico)}>Ver configuración {revision.numero}</button></td></tr>)}</tbody></table></div>}
      {historyError&&<p role="alert" className="form-message error-message">{historyError}</p>}{history&&<div className="catalog-history-detail"><div className="admin-section-title"><h3>Detalle de solo lectura</h3><button type="button" className="admin-link-button" onClick={()=>setHistory(null)}>Cerrar detalle</button></div><RevisionView catalog={catalog} revision={history}/></div>}
    </section>
    </div>
    <div className="admin-page-footer"><a href="/administracion">← Volver al dashboard</a><span>Configuraciones comerciales independientes del configurador</span></div>
    {tab==="catalogo-base"&&<GuidedTour steps={catalogTour} completion={catalogTourCompletion} storageKey={`lamontana:catalog-tour:v1:${tourOwner}`} autoStart={firstSetup&&!!guide.data&&!guide.error} helpLabel="Guía del catálogo base" onStepChange={index=>setBlock(index===2?"servicios":"papeles")}/>}
    {tab==="revision-comercial"&&<GuidedTour steps={tariffTour} completion={tariffTourCompletion} storageKey={`lamontana:tarifas-tour:v1:${tourOwner}`} autoStart={firstSetup&&!!guide.data&&!guide.error} helpLabel="Guía de Tarifas" onActiveChange={setTariffTourActive}/>}
  </>;
}
