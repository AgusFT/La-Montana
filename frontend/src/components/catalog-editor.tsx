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


export function CatalogEditor({catalog,onSaved}:{catalog:CatalogState;onSaved:(revision:CatalogRevision)=>void}){
  const [rates,setRates]=useState(()=>seedRates(catalog.actual)),[offers,setOffers]=useState(()=>seedOffers(catalog.actual));
  const [editingRate,setEditingRate]=useState<{id:string;previous:RateDraft|null}|null>(null),[rateError,setRateError]=useState("");
  const [editingOffer,setEditingOffer]=useState<{id:string;previous:OfferDraft|null}|null>(null),[offerError,setOfferError]=useState("");
  const [baseline,setBaseline]=useState(catalog.actual);
  const [base,setBase]=useState(catalog.actual?.codigoPublico??null),[baseNumber,setBaseNumber]=useState(catalog.actual?.numero??null),[reason,setReason]=useState("");
  const [busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[conflict,setConflict]=useState(false),[message,setMessage]=useState(""),[saved,setSaved]=useState<CatalogRevision|null>(null);
  const [scheduled,setScheduled]=useState(false),[scheduledFor,setScheduledFor]=useState("");
  const [comparison,setComparison]=useState<CatalogState|null>(null),[checking,setChecking]=useState(false);
  const pending=useRef<NewRevision|null>(null);
  const locked=busy||uncertain||catalog.programada!==null;
  const baselineRates=seedRates(baseline),baselineOffers=seedOffers(baseline);
  const dirty=!!editingRate||!!editingOffer||JSON.stringify(rates.map(rateSignature))!==JSON.stringify(baselineRates.map(rateSignature))||JSON.stringify(offers.map(offerSignature))!==JSON.stringify(baselineOffers.map(offerSignature));
  useNavigationGuard({dirty,blocked:busy||uncertain});
  function changeRate(id:string,change:Partial<RateDraft>){setRateError("");setSaved(null);setRates(current=>current.map(row=>row.id===id?{...row,...change}:row));}
  function changeOffer(id:string,change:Partial<OfferDraft>){setOfferError("");setSaved(null);setOffers(current=>current.map(row=>row.id===id?{...row,...change}:row));}
  function chooseService(row:OfferDraft,id:string){
    const selected=catalog.servicios.find(service=>service.codigoPublico===id),printing=selected?.tipo==="IMPRESION";
    const oldName=catalog.servicios.find(service=>service.codigoPublico===row.servicio)?.nombre;
    changeOffer(row.id,{servicio:id,...(!row.nombreVisible.trim()||row.nombreVisible===oldName?{nombreVisible:selected?.nombre??""}:{}),...(printing?{basePrecio:"POR_CARILLA",precio:"0",compatibilidades:[]}:row.basePrecio==="POR_CARILLA"&&catalog.servicios.find(service=>service.codigoPublico===row.servicio)?.tipo==="IMPRESION"?{basePrecio:"",precio:""}:{})});
  }
  function focusCard(id:string){requestAnimationFrame(()=>{const card=document.getElementById(id);card?.scrollIntoView({block:"start"});card?.querySelector<HTMLElement>("[data-rate-color], input")?.focus({preventScroll:true});});}
  function addRate(){if(editingRate||locked)return;const id=crypto.randomUUID();setEditingRate({id,previous:null});setRateError("");setSaved(null);setRates(current=>[...current,{id,grupo:id,nombre:rateName("",current.length),nombrePersonalizado:false,color:"",papeles:[],precio:"",modoDobleFaz:"FIJO",valorDobleFaz:"",habilitada:true}]);focusCard(id);}
  function editRate(rate:RateDraft){if(editingRate||locked)return;setEditingRate({id:rate.id,previous:rate});setRateError("");setSaved(null);focusCard(rate.id);}
  function applyRate(){try{expandRates(rates);setEditingRate(null);setRateError("");}catch(cause){setRateError(cause instanceof Error?cause.message:"Revisá los datos de la tarifa.");}}
  function cancelRate(){if(!editingRate)return;const {id,previous}=editingRate;setRates(current=>previous?current.map(rate=>rate.id===id?previous:rate):current.filter(rate=>rate.id!==id));setEditingRate(null);setRateError("");}
  function removeRate(id:string){setSaved(null);setRates(current=>current.filter(rate=>rate.id!==id));if(editingRate?.id===id){setEditingRate(null);setRateError("");}}
  function addOffer(){if(editingOffer||locked)return;const id=crypto.randomUUID();setEditingOffer({id,previous:null});setOfferError("");setSaved(null);setOffers(current=>[...current,{id,servicio:"",nombreVisible:"",basePrecio:"",precio:"",preparacionMinutos:"0",habilitado:false,compatibilidades:[]}]);focusCard(id);}
  function editOffer(offer:OfferDraft){if(editingOffer||locked)return;setEditingOffer({id:offer.id,previous:offer});setOfferError("");setSaved(null);focusCard(offer.id);}
  function applyOffer(){if(!editingOffer||locked)return;try{const index=offers.findIndex(offer=>offer.id===editingOffer.id);if(index<0)return;validateOffer(offers[index],index,offers,catalog);setEditingOffer(null);setOfferError("");}catch(cause){setOfferError(cause instanceof Error?cause.message:"Revisá los datos del servicio.");}}
  function cancelOffer(){if(!editingOffer)return;const {id,previous}=editingOffer;setOffers(current=>previous?current.map(offer=>offer.id===id?previous:offer):current.filter(offer=>offer.id!==id));setEditingOffer(null);setOfferError("");}
  function removeOffer(id:string){setSaved(null);setOffers(current=>current.filter(offer=>offer.id!==id));if(editingOffer?.id===id){setEditingOffer(null);setOfferError("");}}
  function pairChange(row:OfferDraft,id:string,change:Partial<PairDraft>){changeOffer(row.id,{compatibilidades:row.compatibilidades.map(pair=>pair.id===id?{...pair,...change}:pair)});}
  const enabledPapers=catalog.papelesHabilitados.filter(p=>p.habilitado);
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
    if(!rates.some(rate=>rate.habilitada))throw new Error("Habilitá al menos una tarifa de impresión.");
    if(offers.filter(offer=>offer.habilitado&&catalog.servicios.find(service=>service.codigoPublico===offer.servicio)?.tipo==="IMPRESION").length<1)throw new Error("Debe haber al menos un servicio de impresión habilitado.");
    return {versionBase:base,operacion:crypto.randomUUID(),motivo:reason.trim(),programadaPara:date,tarifas:expanded,servicios:services};
  }
  async function save(event?:FormEvent<HTMLFormElement>){
    event?.preventDefault();if(busy)return;
    setMessage("");setSaved(null);setBusy(true);
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
  return <section id="revision-comercial" className="pricing-editor"><form onSubmit={save} className="admin-form">
    <div className="catalog-editor-heading"><div><h2>Tarifas y servicios</h2><p>{baseNumber===null?"Todavía no hay precios vigentes. Prepará las tarifas y los servicios y guardá la primera configuración.":dirty?`Tenés cambios pendientes sobre la configuración ${baseNumber}. Los precios vigentes se conservan hasta guardar.`:baseline?.estado==="PROGRAMADA"?"Estos precios están guardados y programados; se aplicarán en la fecha elegida.":`Configuración vigente ${baseNumber}. Las tarifas y servicios ya están aplicados. Usá Editar sólo si querés cambiarlos.`}</p></div><span className="pricing-currency">ARS · Pesos argentinos</span></div>
    <div className="pricing-guide"><strong>Configurá en este orden</strong><ol><li><b>1. Tarifas</b><span>Editá y aplicá cada tarifa al borrador.</span></li><li><b>2. Servicios</b><span>Guardá cada servicio al borrador y agregá los que necesites.</span></li><li><b>3. Publicación</b><span>Guardá la configuración completa para aplicar sus precios.</span></li></ol><p>Los precios finales se muestran en ARS. Para doble faz podés ingresar el precio final o calcularlo sumando un importe o un porcentaje sobre simple faz. Escribí, por ejemplo, <b>1500,50</b> o <b>1500.50</b>, sin separadores de miles.</p></div>
    <fieldset disabled={locked} className="admin-fieldset">
      <section className="admin-card pricing-section" aria-labelledby="pricing-rates-title">
        <div className="admin-section-title"><div><h2 id="pricing-rates-title">1. Tarifas de impresión <span className="admin-count">{rates.length}</span></h2><p>Una tarifa aplica los mismos precios a las hojas y variantes que selecciones para un modo de color.</p></div><button type="button" className="admin-button" disabled={!!editingRate||rates.length>=300||!catalog.papelesHabilitados.some(p=>p.habilitado)} onClick={addRate}>+ Agregar tarifa</button></div>
        <CatalogInfo title="¿Qué es una tarifa?"><p>Es una regla de precios para imprimir en color o blanco y negro sobre una o varias hojas y variantes. Primero elegí el color, después seleccioná los papeles y completá sus precios.</p><p>Simple faz es el precio de una hoja con una cara impresa. Doble faz es el precio final de una hoja con sus dos caras impresas. Si sobra una página impar, esa última hoja se cobra a simple faz, por cada copia del documento.</p><p>Los servicios de impresión usan estas tarifas sin sumar un segundo cargo. Una variante sólo puede estar en una tarifa por color. El nombre es una referencia interna editable con el lápiz.</p></CatalogInfo>
        {rates.length===0&&<p className="admin-empty">Agregá tu primera tarifa. Si no hay papeles disponibles, habilitalos primero en Catálogo base.</p>}
        {editingRate&&<p className="admin-info">Terminá esta tarifa con «Aplicar tarifa al borrador» o cancelá su edición. Después podrás editar otra o guardar la configuración completa.</p>}
        <div className="pricing-card-list">{rates.map((rate,index)=>editingRate?.id===rate.id?<CatalogRateEditor key={rate.id} rate={rate} index={index} rates={rates} catalog={catalog} error={rateError} onApply={applyRate} onCancel={cancelRate} onChange={change=>changeRate(rate.id,change)} onRemove={()=>removeRate(rate.id)}/>:<CatalogRateSummary key={rate.id} rate={rate} index={index} catalog={catalog} state={baselineRates.some(savedRate=>rateSignature(savedRate)===rateSignature(rate))?(baseline?.estado==="PROGRAMADA"?"PROGRAMADA":"VIGENTE"):"PENDIENTE"} disabled={!!editingRate} onEdit={()=>editRate(rate)} onRemove={()=>removeRate(rate.id)}/>)}</div>
      </section>
      <section className="admin-card pricing-section" aria-labelledby="pricing-offers-title">
        <div className="admin-section-title"><div><h2 id="pricing-offers-title">2. Servicios ofrecidos <span className="admin-count">{offers.length}</span></h2><p>Elegí servicios del Catálogo base y completá cómo se ofrecen al cliente.</p></div><button type="button" className="admin-button" disabled={!!editingOffer||offers.length>=100||!catalog.servicios.length} onClick={addOffer}>+ Agregar servicio</button></div>
        <CatalogInfo title="¿Qué es un servicio y cómo se configura?"><p>Es un trabajo que ofrece tu imprenta, como impresión o anillado. Primero se crea su identidad en Catálogo base. Aquí definís el nombre que verá el cliente, el tiempo de preparación y si está habilitado.</p><p>La impresión toma su precio de las tarifas del paso 1. Las terminaciones suman su propio cargo, según la unidad de cobro elegida, y necesitan papeles compatibles con tarifa habilitada.</p><p>Guardá cada servicio al borrador para cerrar su formulario y poder agregar otro. Al terminar, Guardar nueva configuración publica el conjunto. Para publicar necesitás al menos una tarifa y un servicio de impresión habilitados. Crear un servicio base no lo publica automáticamente.</p></CatalogInfo>
        {offers.length===0&&<p className="admin-empty">Agregá un servicio de impresión. Después incorporá las terminaciones que ofrezcas. Si falta un servicio, crealo en Catálogo base.</p>}
        {editingOffer&&<p className="admin-info">Completá este servicio y pulsá «Guardar servicio al borrador». Luego podés agregar otro sin publicar todavía la configuración.</p>}
        <div className="pricing-card-list">{offers.map((offer,index)=>{if(editingOffer?.id!==offer.id)return <CatalogOfferSummary key={offer.id} offer={offer} index={index} catalog={catalog} state={baselineOffers.some(savedOffer=>offerSignature(savedOffer)===offerSignature(offer))?(baseline?.estado==="PROGRAMADA"?"PROGRAMADA":"VIGENTE"):"PENDIENTE"} disabled={!!editingOffer} onEdit={()=>editOffer(offer)} onRemove={()=>removeOffer(offer.id)}/>;const service=catalog.servicios.find(s=>s.codigoPublico===offer.servicio),printing=service?.tipo==="IMPRESION";const selectedPapers=new Set(offer.compatibilidades.filter(p=>p.formato&&p.papel).map(paperKey)),missingPapers=enabledPapers.filter(p=>!selectedPapers.has(paperKey(p))),totalPapers=selectedPapers.size+missingPapers.length,allSelected=missingPapers.length===0&&offer.compatibilidades.length===selectedPapers.size;return <article className="pricing-entry pricing-offer" key={offer.id} id={offer.id} aria-label={`Servicio ofrecido ${index+1}`}>
          <header><h3>Servicio {index+1}</h3><span className="pricing-state is-editing">En edición · sin confirmar</span><button type="button" className="admin-link-button" onClick={()=>removeOffer(offer.id)}>Quitar servicio {index+1}</button></header>
          <div className="pricing-fields pricing-service-fields">
            <CatalogSearchSelect label={`Servicio base ${index+1}`} options={catalog.servicios.filter(s=>s.codigoPublico===offer.servicio||!offers.some(other=>other.id!==offer.id&&other.servicio===s.codigoPublico)).map(s=>({value:s.codigoPublico,label:s.nombre,detail:`${s.codigo} · ${s.tipo==="IMPRESION"?"Impresión":"Terminación"}`}))} value={offer.servicio} disabled={locked} onChange={id=>chooseService(offer,id)} placeholder="Ej.: impresión, anillado o código"/>
            <label><span className="pricing-label-text">Nombre visible de servicio {index+1}</span><input required maxLength={140} value={offer.nombreVisible} onChange={e=>changeOffer(offer.id,{nombreVisible:e.target.value})}/><small>Así lo verá el cliente. Podés editar el nombre sugerido.</small></label>
            <div className="pricing-field"><CatalogInfo title={`Preparación de servicio ${index+1} (minutos)`} htmlFor={`prep-${offer.id}`}><p>Tiempo mínimo de preparación del servicio, entre 0 y 10080 minutos. El cálculo del ítem usa el mayor tiempo entre impresión y terminaciones; no suma todos los tiempos. La entrega también depende de los horarios y reglas operativas.</p></CatalogInfo><input id={`prep-${offer.id}`} type="number" required min="0" max="10080" step="1" value={offer.preparacionMinutos} onChange={e=>changeOffer(offer.id,{preparacionMinutos:e.target.value})}/><small>Minutos enteros. Ej.: 30 = media hora; 0 = sin mínimo adicional.</small></div>
            {printing?<div className="pricing-wide admin-info compact"><strong>Impresión · precio tomado de las tarifas</strong><p>Se cobra según las hojas, caras y color del paso 1. Este servicio no agrega un segundo cargo (ARS 0,00). Sus papeles disponibles son los de las tarifas habilitadas.</p></div>:service&&<>
              <div className="pricing-field"><CatalogInfo title={`Forma de cobro de servicio ${index+1}`} htmlFor={`basis-${offer.id}`}><p>La terminación se suma al precio de impresión. Elegí qué cantidad multiplica el importe unitario.</p>{Object.entries(priceExplanations).map(([key,text])=><p key={key}><strong>{priceLabels[key as PriceBase]}:</strong> {text}</p>)}</CatalogInfo><select id={`basis-${offer.id}`} required value={offer.basePrecio} onChange={e=>changeOffer(offer.id,{basePrecio:e.target.value as PriceBase})}><option value="" disabled>Elegir unidad de cobro</option>{Object.entries(priceLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select>{offer.basePrecio&&<small>{priceExplanations[offer.basePrecio]}</small>}</div>
              <CatalogAmount label={`Precio unitario de servicio ${index+1}`} value={offer.precio} onChange={precio=>changeOffer(offer.id,{precio})} help={<p>Importe en ARS por la unidad elegida. Por ejemplo, ARS 500 por copia se convierte en ARS 1500 para 3 copias. Un precio fijo por ítem se cobra una sola vez para ese documento.</p>}/>
            </>}
          </div>
          {service&&!printing&&<fieldset className="pricing-compatibilities"><legend>Papeles compatibles con servicio {index+1}</legend><CatalogInfo title={`Compatibilidades de servicio ${index+1}`}><p>Indican en qué papeles y tamaños podés realizar esta terminación. No agregan otro precio. Para habilitar el servicio necesitás al menos una compatibilidad y una tarifa de impresión habilitada para cada papel elegido.</p></CatalogInfo>
            <div className="configuration-actions"><button type="button" className="admin-button secondary" disabled={!enabledPapers.length||allSelected||totalPapers>300} onClick={()=>addAllCompatiblePapers(offer)}>Agregar todos los papeles</button></div>
            <p className="admin-note">Agrega todos los papeles habilitados en Catálogo base, incluidas sus variantes, sin repetir los ya elegidos. Después podés quitar o cambiar cada compatibilidad. Se aplica sólo a este servicio y se publica al guardar la configuración.</p>
            <p className="admin-note" role="status">{selectedPapers.size} papeles seleccionados.{enabledPapers.length===0?" Habilitá papeles en Catálogo base para agregarlos aquí.":allSelected?" Ya están incluidos todos los papeles habilitados.":""}{totalPapers>300&&" El conjunto supera el máximo de 300 compatibilidades por servicio; elegí los papeles individualmente."}</p>
            {offer.compatibilidades.length===0&&<p className="admin-note">Agregá al menos un papel compatible si vas a habilitar esta terminación.</p>}
            {offer.compatibilidades.map((pair,pairIndex)=><div className="pricing-pair" key={pair.id}><CatalogPaperSelect catalog={catalog} value={pair} disabled={locked} label={`Papel compatible ${pairIndex+1} de servicio ${index+1}`} onChange={p=>pairChange(offer,pair.id,p)}/><button type="button" className="admin-link-button" onClick={()=>changeOffer(offer.id,{compatibilidades:offer.compatibilidades.filter(p=>p.id!==pair.id)})}>Quitar compatibilidad {pairIndex+1} de servicio {index+1}</button>{pair.papel&&!rates.some(r=>r.habilitada&&r.papeles.some(p=>p.formato===pair.formato&&p.papel===pair.papel))&&<p className="pricing-wide admin-note">Este papel necesita una tarifa habilitada en el paso 1 para ofrecer la terminación.</p>}</div>)}
            <button type="button" className="admin-button secondary" disabled={offer.compatibilidades.length>=300||!catalog.papelesHabilitados.some(p=>p.habilitado)} onClick={()=>changeOffer(offer.id,{compatibilidades:[...offer.compatibilidades,{id:crypto.randomUUID(),formato:"",papel:""}]})}>+ Agregar papel compatible a servicio {index+1}</button>
          </fieldset>}
          <label className="admin-check pricing-enable"><input type="checkbox" checked={offer.habilitado} onChange={e=>changeOffer(offer.id,{habilitado:e.target.checked})}/>Habilitar servicio {index+1} en esta configuración</label>
          {offerError&&<p className="error-message" role="alert">{offerError}</p>}
          <div className="rate-editor-actions"><button type="button" className="admin-button" onClick={applyOffer}>Guardar servicio al borrador</button><button type="button" className="admin-button secondary" onClick={cancelOffer}>Cancelar edición de servicio</button><p className="admin-note">Confirma este servicio y cierra el formulario para que puedas agregar más. Al terminar con las tarifas y los servicios, pulsá Guardar nueva configuración para publicarlos. El borrador se conserva entre pestañas; se pierde si salís o recargás sin guardar la configuración.</p></div>
        </article>;})}</div>
      </section>
      <section className="admin-card catalog-save pricing-section">
        <h2>3. Revisá y aplicá los precios</h2><CatalogInfo title="¿Qué se guarda en una configuración?"><p>Una configuración reúne todas las tarifas y servicios de este formulario. Hasta confirmar el guardado, editar, agregar o quitar no cambia los precios vigentes. Estos cambios de trabajo se conservan al cambiar de pestaña, pero se pierden al recargar o salir sin guardar.</p><p>Al aplicar la configuración cambian las nuevas cotizaciones. Las ofertas aceptadas y los pedidos conservan sus importes, y el historial permite consultar los precios anteriores.</p></CatalogInfo>
        <div className="pricing-publish-grid"><label>Cuándo aplicar<select value={scheduled?"scheduled":"now"} onChange={e=>{setScheduled(e.target.value==="scheduled");setSaved(null);}}><option value="now">Al guardar esta configuración</option><option value="scheduled">Programar para más adelante</option></select></label>
        {scheduled&&<label>Fecha y hora de vigencia (UTC)<input type="datetime-local" required value={scheduledFor} onChange={event=>setScheduledFor(event.target.value)}/><small>Hora UTC, no hora argentina. En Argentina (UTC−3), 15:00 UTC equivale a 12:00 local.</small></label>}</div>
        <div className="catalog-field"><label htmlFor="catalog-reason">Motivo de la nueva configuración</label><textarea id="catalog-reason" required maxLength={500} placeholder="Ej.: Actualización de precios de impresión A4 y anillado" value={reason} onChange={e=>{setReason(e.target.value);setSaved(null);}}/></div>
        <p className="admin-note">{scheduled?"La configuración vigente se conserva hasta la fecha elegida. Sólo puede haber una programación comercial pendiente; cancelala para reemplazarla.":"Al pulsar Guardar nueva configuración, los cambios se aplican inmediatamente. El historial anterior se conserva."}</p>
      </section>
    </fieldset>
    {message&&<p className={`form-message ${conflict||uncertain?"admin-warning":"error-message"}`} role="alert">{message}</p>}
    {uncertain&&<div className="admin-warning"><p>No pudimos confirmar el resultado del envío. Conservamos la misma operación y sus datos para reintentar sin crear una configuración duplicada.</p><button type="button" className="admin-button" disabled={busy} onClick={()=>save()}>{busy?"Confirmando…":"Reintentar el mismo envío"}</button></div>}
    {conflict&&<div className="admin-warning"><p>Tus valores siguen en el formulario. Consultá la configuración vigente antes de decidir si querés usarlos sobre la nueva base.</p><button type="button" className="admin-button secondary" disabled={checking} onClick={checkLatest}>{checking?"Consultando…":"Consultar configuración vigente"}</button></div>}
    {comparison&&<div className="admin-card"><h3>Comparar con el catálogo vigente</h3>{comparison.actual?<RevisionView catalog={comparison} revision={comparison.actual}/>:<p>No hay una configuración vigente.</p>}<button type="button" className="admin-button secondary" onClick={adoptBase}>Usar esta base conservando mis valores</button></div>}
    {saved!==null&&<p className="admin-success" role="status">{saved.estado==="PROGRAMADA"?`Configuración ${saved.numero} programada.`:saved.estado==="VIGENTE"?`Configuración ${saved.numero} guardada y vigente.`:`La configuración ${saved.numero} ya fue procesada; su estado actual es ${saved.estado.toLowerCase()}. Consultá la vigente antes de guardar nuevos cambios.`}</p>}
    <div className="catalog-save-actions"><span>{rates.filter(r=>r.habilitada).length} tarifas ({rates.filter(r=>r.habilitada).reduce((total,r)=>total+r.papeles.length,0)} combinaciones de papel y color) y {offers.filter(o=>o.habilitado).length} servicios habilitados en el formulario</span><button className="admin-button" disabled={locked||conflict||!!editingRate||!!editingOffer}>{busy?"Guardando…":scheduled?"Confirmar programación":"Guardar nueva configuración"}</button></div>
  </form></section>;
}
