//#region ENCABEZADO · src/components/catalog-editor.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-editor.tsx
 * ========================================================================
 * FUNCIÓN
 * Edita tarifas agrupadas y servicios de una configuración comercial. Valida importes, selecciones
 * y compatibilidades; publica o programa de forma explícita y conserva el borrador ante conflictos
 * o respuestas inciertas.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * - rateSignature(rate: RateDraft)
 * - [export] CatalogEditor({catalog,onSaved}:
 *   {catalog:CatalogState;onSaved:(revision:CatalogRevision)=>void})
 * - CatalogEditor :: changeRate(id: string, change: Partial<RateDraft>)
 * - CatalogEditor :: changeOffer(id: string, change: Partial<OfferDraft>)
 * - CatalogEditor :: chooseService(row: OfferDraft, id: string)
 * - CatalogEditor :: focusCard(id: string)
 * - CatalogEditor :: addRate()
 * - CatalogEditor :: editRate(rate: RateDraft)
 * - CatalogEditor :: applyRate()
 * - CatalogEditor :: cancelRate()
 * - CatalogEditor :: removeRate(id: string)
 * - CatalogEditor :: addOffer()
 * - CatalogEditor :: editOffer(offer: OfferDraft)
 * - CatalogEditor :: applyOffer()
 * - CatalogEditor :: cancelOffer()
 * - CatalogEditor :: removeOffer(id: string)
 * - CatalogEditor :: pairChange(row: OfferDraft, id: string, change: Partial<PairDraft>)
 * - CatalogEditor :: addAllCompatiblePapers(offer: OfferDraft)
 * - CatalogEditor :: payload(): NewRevision
 * - [async] CatalogEditor :: save(event?: FormEvent<HTMLFormElement>)
 * - [async] CatalogEditor :: checkLatest()
 * - CatalogEditor :: adoptBase()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos propios; importa los borradores de tarifas y servicios.
 * ========================================================================
 */
//#endregion

"use client";
import {useInstallation} from "./installation-guide";
import {SaveNotice} from "./save-notice";
import {catalogConfigurationText} from "@/lib/catalog-wording";
import { useRef, useState, type FormEvent } from "react";
import { MutationError, secureMutation } from "@/lib/secure-mutation";
import { readCatalog } from "@/lib/catalog-client";
import { CatalogInfo, CatalogAmount } from "@/components/catalog-pricing-fields";
import { CatalogSearchSelect } from "@/components/catalog-search-select";
import { priceExplanations } from "@/lib/catalog-pricing";
import { CatalogPaperSelect, paperKey } from "@/components/catalog-paper-select";
import { RevisionView } from "@/components/catalog-view";
import { priceLabels, isCatalogRevision, type CatalogState, type CatalogRevision, type PriceBase, type NewRevision } from "@/lib/catalog-types";

import {seedRates,expandRates,rateName,type RateDraft} from "@/lib/catalog-rate-draft";
import {useNavigationGuard} from "@/components/navigation-boundary";
import {CatalogRateSummary} from "@/components/catalog-rate-summary";
import {CatalogRateEditor} from "@/components/catalog-rate-editor";
import {CatalogOfferSummary} from "@/components/catalog-offer-summary";
import {seedOffers,offerSignature,validateOffer,type OfferDraft,type PairDraft} from "@/lib/catalog-offer-draft";
function rateSignature(rate:RateDraft){const {id,nombrePersonalizado,papeles,...fields}=rate;return JSON.stringify({...fields,papeles:papeles.map(paperKey).sort()});}


export function CatalogEditor({catalog,onSaved,tourActive=false,activeBlock="impresion"}:{catalog:CatalogState;onSaved:(revision:CatalogRevision)=>void;tourActive?:boolean;activeBlock?:"impresion"|"servicios"}){
  const [rates,setRates]=useState(()=>seedRates(catalog.actual)),[offers,setOffers]=useState(()=>seedOffers(catalog.actual));
  const [editingRate,setEditingRate]=useState<{id:string;previous:RateDraft|null}|null>(null),[rateError,setRateError]=useState("");
  const [editingOffer,setEditingOffer]=useState<{id:string;previous:OfferDraft|null}|null>(null),[offerError,setOfferError]=useState("");
  const [baseline,setBaseline]=useState(catalog.actual);
  const [base,setBase]=useState(catalog.actual?.codigoPublico??null),[baseNumber,setBaseNumber]=useState(catalog.actual?.numero??null),[reason,setReason]=useState("");
  const [busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[conflict,setConflict]=useState(false),[message,setMessage]=useState(""),[saved,setSaved]=useState<CatalogRevision|null>(null);
  const [scheduled,setScheduled]=useState(false),[scheduledFor,setScheduledFor]=useState("");
  const [comparison,setComparison]=useState<CatalogState|null>(null),[checking,setChecking]=useState(false);
  const pending=useRef<NewRevision|null>(null);
  const guide=useInstallation(),firstSetup=guide.enabled&&!guide.data?.activa;
  const [prepared,setPrepared]=useState("");
  const printingOffered=offers.some(o=>o.habilitado&&catalog.servicios.some(s=>s.codigoPublico===o.servicio&&s.tipo==="IMPRESION"));
  const ratesReady=!editingRate&&(printingOffered?rates.some(r=>r.habilitada):!rates.some(r=>r.habilitada));
  const offersReady=!editingOffer&&offers.some(o=>o.habilitado);
  const locked=busy||uncertain||catalog.programada!==null;
  const baselineRates=seedRates(baseline),baselineOffers=seedOffers(baseline);
  const dirty=!!editingRate||!!editingOffer||JSON.stringify(rates.map(rateSignature))!==JSON.stringify(baselineRates.map(rateSignature))||JSON.stringify(offers.map(offerSignature))!==JSON.stringify(baselineOffers.map(offerSignature));
  useNavigationGuard({dirty,blocked:busy||uncertain});
  function changeRate(id:string,change:Partial<RateDraft>){setPrepared("");setRateError("");setSaved(null);setRates(current=>current.map(row=>row.id===id?{...row,...change}:row));}
  function changeOffer(id:string,change:Partial<OfferDraft>){setPrepared("");setOfferError("");setSaved(null);setOffers(current=>current.map(row=>row.id===id?{...row,...change}:row));}
  function chooseService(row:OfferDraft,id:string){
    const selected=catalog.servicios.find(service=>service.codigoPublico===id),printing=selected?.tipo==="IMPRESION";
    const oldName=catalog.servicios.find(service=>service.codigoPublico===row.servicio)?.nombre;
    changeOffer(row.id,{servicio:id,...(!row.nombreVisible.trim()||row.nombreVisible===oldName?{nombreVisible:selected?.nombre??""}:{}),...(printing?{basePrecio:"POR_CARILLA",precio:"0",compatibilidades:[]}:row.basePrecio==="POR_CARILLA"&&catalog.servicios.find(service=>service.codigoPublico===row.servicio)?.tipo==="IMPRESION"?{basePrecio:"",precio:""}:{})});
  }
  function focusCard(id:string){requestAnimationFrame(()=>{const card=document.getElementById(id);card?.scrollIntoView({block:"start"});card?.querySelector<HTMLElement>("[data-rate-color], input")?.focus({preventScroll:true});});}
  function addRate(){setPrepared("");if(editingRate||locked)return;const id=crypto.randomUUID();setEditingRate({id,previous:null});setRateError("");setSaved(null);setRates(current=>[...current,{id,grupo:id,nombre:rateName("",current.length),nombrePersonalizado:false,color:"",papeles:[],precio:"",modoDobleFaz:"FIJO",valorDobleFaz:"",habilitada:true}]);focusCard(id);}
  function editRate(rate:RateDraft){setPrepared("");if(editingRate||locked)return;setEditingRate({id:rate.id,previous:rate});setRateError("");setSaved(null);focusCard(rate.id);}
  function applyRate(){try{expandRates(rates);setEditingRate(null);setRateError("");setPrepared("Tarifa confirmada en este formulario. Continuá con los servicios; al final guardá la configuración completa para conservar los precios.");}catch(cause){setRateError(cause instanceof Error?cause.message:"Revisá los datos de la tarifa.");}}
  function cancelRate(){setPrepared("");if(!editingRate)return;const {id,previous}=editingRate;setRates(current=>previous?current.map(rate=>rate.id===id?previous:rate):current.filter(rate=>rate.id!==id));setEditingRate(null);setRateError("");}
  function removeRate(id:string){setPrepared("");setSaved(null);setRates(current=>current.filter(rate=>rate.id!==id));if(editingRate?.id===id){setEditingRate(null);setRateError("");}}
  function addOffer(){setPrepared("");if(editingOffer||locked)return;const id=crypto.randomUUID();setEditingOffer({id,previous:null});setOfferError("");setSaved(null);setOffers(current=>[...current,{id,servicio:"",nombreVisible:"",basePrecio:"",precio:"",preparacionMinutos:"0",habilitado:false,compatibilidades:[]}]);focusCard(id);}
  function editOffer(offer:OfferDraft){setPrepared("");if(editingOffer||locked)return;setEditingOffer({id:offer.id,previous:offer});setOfferError("");setSaved(null);focusCard(offer.id);}
  function applyOffer(){if(!editingOffer||locked)return;try{const index=offers.findIndex(offer=>offer.id===editingOffer.id);if(index<0)return;validateOffer(offers[index],index,offers,catalog);setEditingOffer(null);setOfferError("");setPrepared("Servicio confirmado en este formulario. Podés agregar otro o guardar la configuración completa para conservar todos los precios.");}catch(cause){setOfferError(cause instanceof Error?cause.message:"Revisá los datos del servicio.");}}
  function cancelOffer(){setPrepared("");if(!editingOffer)return;const {id,previous}=editingOffer;setOffers(current=>previous?current.map(offer=>offer.id===id?previous:offer):current.filter(offer=>offer.id!==id));setEditingOffer(null);setOfferError("");}
  function removeOffer(id:string){setPrepared("");setSaved(null);setOffers(current=>current.filter(offer=>offer.id!==id));if(editingOffer?.id===id){setEditingOffer(null);setOfferError("");}}
  function pairChange(row:OfferDraft,id:string,change:Partial<PairDraft>){changeOffer(row.id,{compatibilidades:row.compatibilidades.map(pair=>pair.id===id?{...pair,...change}:pair)});}
  const enabledPapers=catalog.papelesHabilitados.filter(p=>p.habilitado);
  const exampleRate:RateDraft={id:"tariff-tour-example",nombre:"Tarifa de ejemplo · Blanco y negro",nombrePersonalizado:false,color:"BLANCO_NEGRO",papeles:enabledPapers.slice(0,1).map(({formato,papel})=>({formato,papel})),precio:"1000",modoDobleFaz:"FIJO",valorDobleFaz:"1800",habilitada:true};
  const editedRate=rates.find(rate=>rate.id===editingRate?.id);
  const tourUsesExample=tourActive&&!editedRate?.color;
  function addAllCompatiblePapers(offer:OfferDraft){
    if(locked)return;
    const pairs=new Map(offer.compatibilidades.filter(p=>p.formato&&p.papel).map(p=>[paperKey(p),p]));
    for(const paper of enabledPapers)if(!pairs.has(paperKey(paper)))pairs.set(paperKey(paper),{id:crypto.randomUUID(),formato:paper.formato,papel:paper.papel});
    if(pairs.size>300)return;
    changeOffer(offer.id,{compatibilidades:[...pairs.values()]});
  }
  function payload():NewRevision{
    if(editingOffer)throw new Error("Guardá el servicio al borrador o cancelá su edición antes de guardar la configuración.");
    if(editingRate)throw new Error("Aplicá la tarifa al borrador o cancelá su edición antes de guardar la configuración.");
    let date:string|null=null;
    if(scheduled){const chosen=new Date(scheduledFor+":00Z");if(!scheduledFor||Number.isNaN(chosen.valueOf())||chosen.valueOf()<=Date.now())throw new Error("Elegí una fecha y hora UTC futura.");date=chosen.toISOString();}
    const expanded=expandRates(rates);
    const services=offers.map((offer,index)=>validateOffer(offer,index,offers,catalog));
    if(!offers.some(offer=>offer.habilitado))throw new Error("Habilitá al menos un servicio con su precio o configuración comercial.");
    if(printingOffered&&!rates.some(rate=>rate.habilitada))throw new Error("Impresión necesita al menos una tarifa habilitada.");
    if(!printingOffered&&rates.some(rate=>rate.habilitada))throw new Error("Deshabilitá las tarifas de impresión si no ofrecés Impresión.");
    return {versionBase:base,operacion:crypto.randomUUID(),motivo:reason.trim(),programadaPara:date,tarifas:expanded,servicios:services};
  }
  async function save(event?:FormEvent<HTMLFormElement>){
    event?.preventDefault();if(busy)return;
    setMessage("");setSaved(null);setPrepared("");setBusy(true);
    try{
      if(!pending.current)pending.current=payload();
      const response=await secureMutation("/api/admin/catalogo/revisiones",JSON.stringify(pending.current),"application/json");
      const data:unknown=await response.json();if(!isCatalogRevision(data))throw new Error("No pudimos confirmar la configuración recibida.");
      pending.current=null;setUncertain(false);setConflict(data.estado!=="VIGENTE"&&data.estado!=="PROGRAMADA");setComparison(null);if(data.estado==="VIGENTE"){setBase(data.codigoPublico);setBaseNumber(data.numero);}setReason("");setRates(seedRates(data));setOffers(seedOffers(data));setBaseline(data);setEditingOffer(null);setOfferError("");setEditingRate(null);setRateError("");setSaved(data);onSaved(data);
    }catch(error){
      setMessage(error instanceof Error?catalogConfigurationText(error.message):"No pudimos guardar la configuración.");
      if(error instanceof MutationError&&!error.uncertain){pending.current=null;setUncertain(false);setConflict(error.status===409);}
      else if(pending.current){setUncertain(true);}
    }finally{setBusy(false);}
  }
  async function checkLatest(){setChecking(true);try{setComparison(await readCatalog());}catch(error){setMessage(error instanceof Error?catalogConfigurationText(error.message):"No pudimos consultar la configuración vigente.");}finally{setChecking(false);}}
  function adoptBase(){if(!comparison)return;setBaseline(comparison.actual);setBase(comparison.actual?.codigoPublico??null);setBaseNumber(comparison.actual?.numero??null);setConflict(false);setComparison(null);setMessage("");pending.current=null;}
  const covered=new Set(rates.filter(r=>r.habilitada&&r.color&&r.id!==editingRate?.id).flatMap(r=>r.papeles.map(p=>`${r.color}/${paperKey(p)}`)));
  const catalogServices=catalog.servicios.filter(s=>s.activo);
  const offeredCount=catalogServices.filter(s=>offers.some(o=>o.id!==editingOffer?.id&&o.servicio===s.codigoPublico&&o.habilitado)).length;
  function paperLabel(formato:string,papel:string){const f=catalog.formatos.find(f=>f.codigoPublico===formato),p=catalog.papeles.find(p=>p.codigoPublico===papel);return `${f?.nombre??"Formato"} · ${p?.gramaje??"?"} g · ${p?.terminacion??"Papel"}`;}
  return <section id="revision-comercial" className="pricing-editor"><form onSubmit={save} className="admin-form">
    <div className="catalog-editor-heading"><div><h2>Tarifas y servicios</h2><p>{baseNumber===null?"Todavía no hay precios vigentes. Prepará las tarifas y los servicios y guardá la primera configuración.":dirty?`Tenés cambios pendientes sobre la configuración ${baseNumber}. Los precios vigentes se conservan hasta guardar.`:baseline?.estado==="PROGRAMADA"?"Estos precios están guardados y programados; se aplicarán en la fecha elegida.":`Configuración vigente ${baseNumber}. Las tarifas y servicios ya están aplicados. Usá Editar sólo si querés cambiarlos.`}</p></div><span className="pricing-currency">ARS · Pesos argentinos</span></div>
    <div className="pricing-guide"><strong>Configurá en este orden</strong><ol><li><b>1. Tarifas de impresión</b><span>Si ofrecés Impresión, elegí B/N o Color y los papeles cotizados.</span></li><li><b>2. Tarifas de servicios</b><span>Elegí qué otros trabajos ofrecer y configurá sus precios.</span></li><li><b>3. Publicación</b><span>Guardá la configuración completa para aplicar los cambios.</span></li></ol><p>Los servicios creados en Catálogo base pueden permanecer sin ofrecer. Los precios se muestran en ARS.</p></div>
    {dirty&&!locked&&<div className="admin-warning catalog-pending-publication" role="status"><div><strong>Tenés cambios pendientes de publicar</strong><p>Tus tarifas y servicios están preparados en el borrador. Guardá una nueva configuración para aplicarlos.</p></div><button type="button" className="admin-button" onClick={()=>document.getElementById("tariff-tour-publish")?.scrollIntoView({behavior:"smooth",block:"center"})}>Ir a Guardar nueva configuración ↓</button></div>}
    <fieldset disabled={locked} className="admin-fieldset">
      <section className="admin-card pricing-section" aria-labelledby="pricing-rates-title" hidden={activeBlock!=="impresion"}>
        <div className="admin-section-title"><div><h2 id="pricing-rates-title">1. Tarifas de impresión <span className="admin-count">{rates.length}</span></h2><p>Una tarifa aplica los mismos precios a las hojas y variantes que selecciones para un modo de color.</p></div><button type="button" className="admin-button" disabled={!!editingRate||rates.length>=300||!catalog.papelesHabilitados.some(p=>p.habilitado)||!catalog.servicios.some(s=>s.activo&&s.tipo==="IMPRESION")} onClick={addRate}>+ Agregar tarifa</button></div>
        {!catalog.servicios.some(s=>s.activo&&s.tipo==="IMPRESION")&&<p className="admin-info">Impresión no está activa en el catálogo base. Podés configurar y publicar solo otros servicios desde «Tarifas de servicios».</p>}
        <CatalogInfo title="¿Qué es una tarifa?"><p>Es una regla de precios para imprimir en color o blanco y negro sobre una o varias hojas y variantes. Primero elegí el color, después seleccioná los papeles y completá sus precios.</p><p>Simple faz es el precio de una hoja con una cara impresa. Doble faz es el precio final de una hoja con sus dos caras impresas. Si sobra una página impar, esa última hoja se cobra a simple faz, por cada copia del documento.</p><p>Los servicios de impresión usan estas tarifas sin sumar un segundo cargo. Una variante sólo puede estar en una tarifa por color. El nombre es una referencia interna editable con el lápiz.</p></CatalogInfo>
        <div id="tariff-tour-coverage" className="catalog-coverage"><h3>Cobertura de impresión</h3><p>Solo las combinaciones con tarifa habilitada se ofrecerán. Los papeles sin precio permanecen en el catálogo base, sin cotizar.</p>{(["BLANCO_NEGRO","COLOR"] as const).map(color=><div key={color}><strong>{color==="BLANCO_NEGRO"?"B/N":"Color"}: {enabledPapers.filter(p=>covered.has(`${color}/${paperKey(p)}`)).length}/{enabledPapers.length} variantes cotizadas</strong>{enabledPapers.filter(p=>!covered.has(`${color}/${paperKey(p)}`)).length>0&&<p className="admin-note">Sin precio (no ofrecidas): {enabledPapers.filter(p=>!covered.has(`${color}/${paperKey(p)}`)).map(p=>paperLabel(p.formato,p.papel)).join("; ")}</p>}</div>)}</div>
        {rates.length===0&&<p className="admin-empty">Agregá tu primera tarifa. Si no hay papeles disponibles, habilitalos primero en Catálogo base.</p>}
        {editingRate&&<p className="admin-info">Terminá esta tarifa con «Aplicar tarifa al borrador» o cancelá su edición. Después podrás editar otra o guardar la configuración completa.</p>}
        {tourUsesExample&&<div className="tariff-tour-example" inert><p><strong>Vista de ejemplo para la guía.</strong> Sus importes son ilustrativos y no se agregan al borrador.</p><CatalogRateEditor rate={exampleRate} index={0} rates={[exampleRate]} catalog={catalog} error="" onApply={()=>{}} onCancel={()=>{}} onChange={()=>{}} onRemove={()=>{}} tutorialTargets/></div>}
        <div className="pricing-card-list">{rates.map((rate,index)=>editingRate?.id===rate.id?<CatalogRateEditor key={rate.id} rate={rate} index={index} rates={rates} catalog={catalog} error={rateError} onApply={applyRate} onCancel={cancelRate} onChange={change=>changeRate(rate.id,change)} onRemove={()=>removeRate(rate.id)} tutorialTargets={tourActive&&!tourUsesExample}/>:<CatalogRateSummary key={rate.id} rate={rate} index={index} catalog={catalog} state={baselineRates.some(savedRate=>rateSignature(savedRate)===rateSignature(rate))?(baseline?.estado==="PROGRAMADA"?"PROGRAMADA":"VIGENTE"):"PENDIENTE"} disabled={!!editingRate} onEdit={()=>editRate(rate)} onRemove={()=>removeRate(rate.id)}/>)}</div>
      </section>
      <section className="admin-card pricing-section" aria-labelledby="pricing-offers-title" hidden={activeBlock!=="servicios"}>
        <div className="admin-section-title"><div><h2 id="pricing-offers-title">2. Tarifas de servicios <span className="admin-count">{offers.length}</span></h2><p>Configurá el precio de las prestaciones que vayas a habilitar. Impresión hereda las tarifas del paso 1.</p></div><button type="button" className="admin-button" disabled={!!editingOffer||offers.length>=100||!catalogServices.length} onClick={addOffer}>+ Configurar servicio</button></div>
        <div id="service-tour-coverage" className="catalog-coverage"><h3>Servicios del catálogo: {catalogServices.length} · {offeredCount}/{catalogServices.length} ofrecidos</h3><p>Existir en el catálogo no obliga a publicar el servicio. Solo se ofrecen los que habilites en esta configuración.</p><ul>{catalogServices.map(s=>{const offer=offers.find(o=>o.servicio===s.codigoPublico),editing=offer?.id===editingOffer?.id;return <li key={s.codigoPublico}>{offer?.habilitado&&!editing?"✓":"○"} {s.nombre} — {editing?"en edición · completar":s.tipo==="IMPRESION"?offer?.habilitado?rates.some(r=>r.habilitada&&r.id!==editingRate?.id)?"precio tomado de Tarifas de impresión":"falta tarifa de impresión":"no ofrecido":offer?.habilitado?"configurado":offer?"no ofrecido":"falta configurar si querés ofrecerlo"}</li>;})}</ul></div>
        <div id="service-tour-prices" className="admin-info compact"><strong>Precio del servicio</strong><p>Para Impresión, el precio viene de Tarifas de impresión. Para Anillado, Plastificado y otros adicionales, elegí la forma de cobro y el importe al configurar cada servicio.</p></div>
        <div id="service-tour-compat" className="admin-info compact"><strong>Preparación y compatibilidades</strong><p>Cada servicio puede tener un tiempo de preparación. Elegí papeles compatibles cuando el trabajo los requiera; prestaciones independientes pueden no tenerlos.</p></div>
        <div id="service-tour-apply" className="admin-info compact"><strong>Habilitar y preparar</strong><p>Marcá «Habilitar servicio» y luego «Guardar servicio al borrador» en su formulario. La publicación ocurre al guardar la configuración completa.</p></div>
        <CatalogInfo title="¿Qué es un servicio y cómo se configura?"><p>El catálogo define qué trabajos existen; acá elegís los ofrecidos. Para Impresión, B/N y Color usan las tarifas del paso 1 y no se cobra un segundo precio.</p><p>Otros servicios tienen su forma de cobro y precio aquí. Guardar servicio al borrador no publica. Al terminar, Guardar nueva configuración activa o programa el conjunto.</p></CatalogInfo>
        {offers.length===0&&<p className="admin-empty">Configurá el primer servicio que quieras ofrecer. Impresión es opcional; podés comenzar con una terminación u otro trabajo.</p>}
        {editingOffer&&<p className="admin-info">Completá este servicio y pulsá «Guardar servicio al borrador». Luego podés agregar otro sin publicar todavía la configuración.</p>}
        <div className="pricing-card-list">{offers.map((offer,index)=>{if(editingOffer?.id!==offer.id)return <CatalogOfferSummary key={offer.id} offer={offer} index={index} catalog={catalog} state={baselineOffers.some(savedOffer=>offerSignature(savedOffer)===offerSignature(offer))?(baseline?.estado==="PROGRAMADA"?"PROGRAMADA":"VIGENTE"):"PENDIENTE"} disabled={!!editingOffer} onEdit={()=>editOffer(offer)} onRemove={()=>removeOffer(offer.id)}/>;const service=catalog.servicios.find(s=>s.codigoPublico===offer.servicio),printing=service?.tipo==="IMPRESION";const selectedPapers=new Set(offer.compatibilidades.filter(p=>p.formato&&p.papel).map(paperKey)),missingPapers=enabledPapers.filter(p=>!selectedPapers.has(paperKey(p))),totalPapers=selectedPapers.size+missingPapers.length,allSelected=missingPapers.length===0&&offer.compatibilidades.length===selectedPapers.size;return <article className="pricing-entry pricing-offer" key={offer.id} id={offer.id} aria-label={`Servicio ofrecido ${index+1}`}>
          <header><h3>Servicio {index+1}</h3><span className="pricing-state is-editing">En edición · sin confirmar</span><button type="button" className="admin-link-button" onClick={()=>removeOffer(offer.id)}>Quitar servicio {index+1}</button></header>
          <div className="pricing-fields pricing-service-fields">
            <CatalogSearchSelect label={`Servicio base ${index+1}`} options={catalog.servicios.filter(s=>s.codigoPublico===offer.servicio||s.activo&&!offers.some(other=>other.id!==offer.id&&other.servicio===s.codigoPublico)).map(s=>({value:s.codigoPublico,label:s.nombre,detail:`${s.codigo} · ${s.tipo==="IMPRESION"?"Impresión":"Otro servicio"}`}))} value={offer.servicio} disabled={locked} onChange={id=>chooseService(offer,id)} placeholder="Ej.: impresión, anillado o código"/>
            <label><span className="pricing-label-text">Nombre visible de servicio {index+1}</span><input required maxLength={140} value={offer.nombreVisible} onChange={e=>changeOffer(offer.id,{nombreVisible:e.target.value})}/><small>Así lo verá el cliente. Podés editar el nombre sugerido.</small></label>
            <div className="pricing-field"><CatalogInfo title={`Preparación de servicio ${index+1} (minutos)`} htmlFor={`prep-${offer.id}`}><p>Tiempo mínimo de preparación del servicio, entre 0 y 10080 minutos. El cálculo del ítem usa el mayor tiempo entre impresión y terminaciones; no suma todos los tiempos. La entrega también depende de los horarios y reglas operativas.</p></CatalogInfo><input id={`prep-${offer.id}`} type="number" required min="0" max="10080" step="1" value={offer.preparacionMinutos} onChange={e=>changeOffer(offer.id,{preparacionMinutos:e.target.value})}/><small>Minutos enteros. Ej.: 30 = media hora; 0 = sin mínimo adicional.</small></div>
            {printing?<div className="pricing-wide admin-info compact"><strong>Impresión · precio tomado de las tarifas</strong><p>Se cobra según las hojas, caras y color del paso 1. Este servicio no agrega un segundo cargo (ARS 0,00). Sus papeles disponibles son los de las tarifas habilitadas.</p></div>:service&&<>
              <div className="pricing-field"><CatalogInfo title={`Forma de cobro de servicio ${index+1}`} htmlFor={`basis-${offer.id}`}><p>Si acompaña una impresión, este importe se suma al trabajo. Para una prestación independiente se aplica su propio precio. Elegí qué cantidad multiplica el importe unitario.</p>{Object.entries(priceExplanations).map(([key,text])=><p key={key}><strong>{priceLabels[key as PriceBase]}:</strong> {text}</p>)}</CatalogInfo><select id={`basis-${offer.id}`} required value={offer.basePrecio} onChange={e=>changeOffer(offer.id,{basePrecio:e.target.value as PriceBase})}><option value="" disabled>Elegir unidad de cobro</option>{Object.entries(priceLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select>{offer.basePrecio&&<small>{priceExplanations[offer.basePrecio]}</small>}</div>
              <CatalogAmount label={`Precio unitario de servicio ${index+1}`} value={offer.precio} onChange={precio=>changeOffer(offer.id,{precio})} help={<p>Importe en ARS por la unidad elegida. Por ejemplo, ARS 500 por copia se convierte en ARS 1500 para 3 copias. Un precio fijo por ítem se cobra una sola vez para ese documento.</p>}/>
            </>}
          </div>
          {service&&!printing&&<fieldset className="pricing-compatibilities"><legend>Papeles compatibles con servicio {index+1} (si corresponde)</legend><CatalogInfo title={`Compatibilidades de servicio ${index+1}`}><p>Elegí los papeles si esta prestación se aplica a hojas concretas. Una prestación independiente puede no tener papeles. Estas compatibilidades no agregan un segundo precio ni requieren Impresión.</p></CatalogInfo>
            <div className="configuration-actions"><button type="button" className="admin-button secondary" disabled={!enabledPapers.length||allSelected||totalPapers>300} onClick={()=>addAllCompatiblePapers(offer)}>Agregar todos los papeles</button></div>
            <p className="admin-note">Agrega todos los papeles habilitados en Catálogo base, incluidas sus variantes, sin repetir los ya elegidos. Después podés quitar o cambiar cada compatibilidad. Se aplica sólo a este servicio y se publica al guardar la configuración.</p>
            <p className="admin-note" role="status">{selectedPapers.size} papeles seleccionados.{enabledPapers.length===0?" Habilitá papeles en Catálogo base para agregarlos aquí.":allSelected?" Ya están incluidos todos los papeles habilitados.":""}{totalPapers>300&&" El conjunto supera el máximo de 300 compatibilidades por servicio; elegí los papeles individualmente."}</p>
            {offer.compatibilidades.length===0&&<p className="admin-note">Sin restricciones de papel para este servicio.</p>}
            {offer.compatibilidades.map((pair,pairIndex)=><div className="pricing-pair" key={pair.id}><CatalogPaperSelect catalog={catalog} value={pair} disabled={locked} label={`Papel compatible ${pairIndex+1} de servicio ${index+1}`} onChange={p=>pairChange(offer,pair.id,p)}/><button type="button" className="admin-link-button" onClick={()=>changeOffer(offer.id,{compatibilidades:offer.compatibilidades.filter(p=>p.id!==pair.id)})}>Quitar compatibilidad {pairIndex+1} de servicio {index+1}</button></div>)}
            <button type="button" className="admin-button secondary" disabled={offer.compatibilidades.length>=300||!catalog.papelesHabilitados.some(p=>p.habilitado)} onClick={()=>changeOffer(offer.id,{compatibilidades:[...offer.compatibilidades,{id:crypto.randomUUID(),formato:"",papel:""}]})}>+ Agregar papel compatible a servicio {index+1}</button>
          </fieldset>}
          <label className="admin-check pricing-enable"><input type="checkbox" checked={offer.habilitado} onChange={e=>changeOffer(offer.id,{habilitado:e.target.checked})}/>Habilitar servicio {index+1} en esta configuración</label>
          {offerError&&<p className="error-message" role="alert">{offerError}</p>}
          <div className="rate-editor-actions"><button type="button" className="admin-button" onClick={applyOffer}>Guardar servicio al borrador</button><button type="button" className="admin-button secondary" onClick={cancelOffer}>Cancelar edición de servicio</button><p className="admin-note">Confirma este servicio y cierra el formulario para que puedas agregar más. Al terminar, pulsá Guardar nueva configuración para publicarlos. El borrador se pierde si salís o recargás sin guardar.</p></div>
        </article>;})}</div>
      </section>
      <section className="admin-card pricing-section" aria-labelledby="offered-review-title">
        <h2 id="offered-review-title">Servicios ofrecidos · vista de publicación</h2>
        <p className="admin-note">Acá se resume qué nombres se mostrarán y cuáles quedarán habilitados. Los precios se configuran una sola vez en cada tipo de tarifa.</p>
        {offers.length===0?<p className="admin-empty">Todavía no hay servicios preparados.</p>:<ul className="catalog-offered-review">{offers.map(o=><li key={o.id}><strong>{o.nombreVisible||catalog.servicios.find(s=>s.codigoPublico===o.servicio)?.nombre||"Servicio sin elegir"}</strong><span>{o.habilitado?"Se ofrecerá":"No ofrecido"} · {catalog.servicios.find(s=>s.codigoPublico===o.servicio)?.tipo==="IMPRESION"?"Precio tomado de Tarifas de impresión":"Precio tomado de Tarifas de servicios"}</span></li>)}</ul>}
      </section>
      {firstSetup&&(!ratesReady||!offersReady)&&<p className="admin-info">Para publicar, configurá al menos un servicio habilitado. Si ofrecés Impresión, agregá una tarifa habilitada; si no ofrecés Impresión, dejá sus tarifas deshabilitadas.</p>}
      <fieldset className="admin-fieldset" disabled={firstSetup&&(!ratesReady||!offersReady)}>      <section className="admin-card catalog-save pricing-section">
        <h2>3. Revisá y aplicá los precios</h2><CatalogInfo title="¿Qué se guarda en una configuración?"><p>Una configuración reúne las tarifas de impresión y los servicios ofrecidos del formulario. El catálogo base puede tener más servicios sin que sea obligatorio publicarlos. Hasta confirmar el guardado, nada cambia en los precios vigentes.</p><p>Los cambios afectan nuevas cotizaciones. Las ofertas aceptadas y los pedidos conservan sus importes; el historial permite consultar las versiones anteriores.</p></CatalogInfo>
        {baseNumber===null&&<div className="admin-info compact"><strong>Primera configuración comercial</strong><p>Tarifas de impresión: {printingOffered?ratesReady?"completas":"faltan":"no aplican"} · Servicios ofrecidos: {offersReady?"configurados":"faltan"} · Configuración anterior: ninguna · Aplicación: {scheduled?"programada":"inmediata"}.</p></div>}
        <div className="pricing-publish-grid"><label>Cuándo aplicar<select value={scheduled?"scheduled":"now"} onChange={e=>{setScheduled(e.target.value==="scheduled");setSaved(null);}}><option value="now">Al guardar esta configuración</option><option value="scheduled">Programar para más adelante</option></select></label>
        {scheduled&&<label>Fecha y hora de vigencia (UTC)<input type="datetime-local" required value={scheduledFor} onChange={event=>setScheduledFor(event.target.value)}/><small>Hora UTC, no hora argentina. En Argentina (UTC−3), 15:00 UTC equivale a 12:00 local.</small></label>}</div>
        <div className="catalog-field"><label htmlFor="catalog-reason">Motivo de la nueva configuración</label><textarea id="catalog-reason" required maxLength={500} placeholder="Ej.: Actualización de precios de impresión A4 y anillado" value={reason} onChange={e=>{setReason(e.target.value);setSaved(null);}}/></div>
        <p className="admin-note">{scheduled?"La configuración vigente se conserva hasta la fecha elegida. Sólo puede haber una programación comercial pendiente; cancelala para reemplazarla.":"Al pulsar Guardar nueva configuración, los cambios se aplican inmediatamente. El historial anterior se conserva."}</p>
      </section></fieldset>
    </fieldset>
    {message&&<p className={`form-message ${conflict||uncertain?"admin-warning":"error-message"}`} role="alert">{message}</p>}
    {uncertain&&<div className="admin-warning"><p>No pudimos confirmar el resultado del envío. Conservamos la misma operación y sus datos para reintentar sin crear una configuración duplicada.</p><button type="button" className="admin-button" disabled={busy} onClick={()=>save()}>{busy?"Confirmando…":"Reintentar el mismo envío"}</button></div>}
    {conflict&&<div className="admin-warning"><p>Tus valores siguen en el formulario. Consultá la configuración vigente antes de decidir si querés usarlos sobre la nueva base.</p><button type="button" className="admin-button secondary" disabled={checking} onClick={checkLatest}>{checking?"Consultando…":"Consultar configuración vigente"}</button></div>}
    {comparison&&<div className="admin-card"><h3>Comparar con el catálogo vigente</h3>{comparison.actual?<RevisionView catalog={comparison} revision={comparison.actual}/>:<p>No hay una configuración vigente.</p>}<button type="button" className="admin-button secondary" onClick={adoptBase}>Usar esta base conservando mis valores</button></div>}
    {prepared&&<SaveNotice title="Aplicado al borrador · sin publicar" pending>{prepared}</SaveNotice>}
    {saved!==null&&<SaveNotice>{saved.estado==="PROGRAMADA"?`Configuración ${saved.numero} programada.`:saved.estado==="VIGENTE"?`Configuración ${saved.numero} guardada y vigente.`:`La configuración ${saved.numero} ya fue procesada; su estado actual es ${saved.estado.toLowerCase()}. Consultá la vigente antes de guardar nuevos cambios.`}</SaveNotice>}
    <div id="tariff-tour-publish" className="catalog-save-actions"><span>{rates.filter(r=>r.habilitada).length} tarifas ({rates.filter(r=>r.habilitada).reduce((total,r)=>total+r.papeles.length,0)} combinaciones de papel y color) y {offers.filter(o=>o.habilitado).length} servicios habilitados en el formulario</span><button className="admin-button" disabled={locked||conflict||!!editingRate||!!editingOffer||firstSetup&&(!ratesReady||!offersReady)}>{busy?"Guardando…":scheduled?"Confirmar programación":"Guardar nueva configuración"}</button></div>
  </form></section>;
}
