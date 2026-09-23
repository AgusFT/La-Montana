"use client";
import {catalogConfigurationText} from "@/lib/catalog-wording";
import { useRef, useState, type FormEvent } from "react";
import { MutationError, secureMutation } from "@/lib/secure-mutation";
import { readCatalog } from "@/lib/catalog-client";
import { CatalogInfo, CatalogAmount } from "@/components/catalog-pricing-fields";
import { CatalogSearchSelect } from "@/components/catalog-search-select";
import { amountDraft, parseAmount, validAmount, ars, priceExplanations } from "@/lib/catalog-pricing";
import { CatalogPaperSelect } from "@/components/catalog-paper-select";
import { RevisionView } from "@/components/catalog-view";
import { colorLabels, priceLabels, isCatalogRevision, type CatalogState, type CatalogRevision, type ColorMode, type PriceBase, type NewRevision } from "@/lib/catalog-types";

type RateDraft={id:string;formato:string;papel:string;color:ColorMode|"";precio:string;recargoDobleFaz:string;habilitada:boolean};
type PairDraft={id:string;formato:string;papel:string};
type OfferDraft={id:string;servicio:string;nombreVisible:string;basePrecio:PriceBase|"";precio:string;preparacionMinutos:string;habilitado:boolean;compatibilidades:PairDraft[]};
function seedRates(revision:CatalogRevision|null):RateDraft[]{return revision?.tarifas.map((rate,i)=>({...rate,id:`rate-${i}`,precio:amountDraft(rate.precio),recargoDobleFaz:amountDraft(rate.recargoDobleFaz)}))??[];}
function seedOffers(revision:CatalogRevision|null):OfferDraft[]{return revision?.servicios.map((offer,i)=>({...offer,id:`offer-${i}`,precio:amountDraft(offer.precio),preparacionMinutos:String(offer.preparacionMinutos),compatibilidades:offer.compatibilidades.map((pair,j)=>({...pair,id:`pair-${i}-${j}`}))}))??[];}

export function CatalogEditor({catalog,onSaved}:{catalog:CatalogState;onSaved:(revision:CatalogRevision)=>void}){
  const [rates,setRates]=useState(()=>seedRates(catalog.actual)),[offers,setOffers]=useState(()=>seedOffers(catalog.actual));
  const [base,setBase]=useState(catalog.actual?.codigoPublico??null),[baseNumber,setBaseNumber]=useState(catalog.actual?.numero??null),[reason,setReason]=useState("");
  const [busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[conflict,setConflict]=useState(false),[message,setMessage]=useState(""),[saved,setSaved]=useState<CatalogRevision|null>(null);
  const [scheduled,setScheduled]=useState(false),[scheduledFor,setScheduledFor]=useState("");
  const [comparison,setComparison]=useState<CatalogState|null>(null),[checking,setChecking]=useState(false);
  const pending=useRef<NewRevision|null>(null);
  const locked=busy||uncertain||catalog.programada!==null;
  function changeRate(id:string,change:Partial<RateDraft>){setSaved(null);setRates(current=>current.map(row=>row.id===id?{...row,...change}:row));}
  function changeOffer(id:string,change:Partial<OfferDraft>){setSaved(null);setOffers(current=>current.map(row=>row.id===id?{...row,...change}:row));}
  function chooseService(row:OfferDraft,id:string){
    const selected=catalog.servicios.find(service=>service.codigoPublico===id),printing=selected?.tipo==="IMPRESION";
    const oldName=catalog.servicios.find(service=>service.codigoPublico===row.servicio)?.nombre;
    changeOffer(row.id,{servicio:id,...(!row.nombreVisible.trim()||row.nombreVisible===oldName?{nombreVisible:selected?.nombre??""}:{}),...(printing?{basePrecio:"POR_CARILLA",precio:"0",compatibilidades:[]}:row.basePrecio==="POR_CARILLA"&&catalog.servicios.find(service=>service.codigoPublico===row.servicio)?.tipo==="IMPRESION"?{basePrecio:"",precio:""}:{})});
  }
  function focusCard(id:string){requestAnimationFrame(()=>{const card=document.getElementById(id);card?.scrollIntoView({block:"start"});card?.querySelector<HTMLInputElement>("input")?.focus({preventScroll:true});});}
  function addRate(){const id=crypto.randomUUID();setSaved(null);setRates(current=>[...current,{id,formato:"",papel:"",color:"",precio:"",recargoDobleFaz:"0",habilitada:false}]);focusCard(id);}
  function addOffer(){const id=crypto.randomUUID();setSaved(null);setOffers(current=>[...current,{id,servicio:"",nombreVisible:"",basePrecio:"",precio:"",preparacionMinutos:"0",habilitado:false,compatibilidades:[]}]);focusCard(id);}
  function pairChange(row:OfferDraft,id:string,change:Partial<PairDraft>){changeOffer(row.id,{compatibilidades:row.compatibilidades.map(pair=>pair.id===id?{...pair,...change}:pair)});}
  function payload():NewRevision{
    let date:string|null=null;
    if(scheduled){const chosen=new Date(scheduledFor+":00Z");if(!scheduledFor||Number.isNaN(chosen.valueOf())||chosen.valueOf()<=Date.now())throw new Error("Elegí una fecha y hora UTC futura.");date=chosen.toISOString();}
    for(const [index,rate] of rates.entries())if(!rate.formato||!rate.papel||!rate.color)throw new Error(`Tarifa ${index+1}: elegí un papel de la lista y su modo de color.`);
    for(const [index,offer] of offers.entries()){
      if(!offer.servicio||!offer.basePrecio||!offer.nombreVisible.trim())throw new Error(`Servicio ${index+1}: elegí un servicio de la lista, su nombre visible y la forma de cobro.`);
      if(offer.compatibilidades.some(pair=>!pair.formato||!pair.papel))throw new Error(`Servicio ${index+1}: elegí un papel de la lista para cada compatibilidad.`);
    }
    if(!rates.some(rate=>rate.habilitada))throw new Error("Habilitá al menos una tarifa de impresión.");
    if(offers.filter(offer=>offer.habilitado&&catalog.servicios.find(service=>service.codigoPublico===offer.servicio)?.tipo==="IMPRESION").length<1)throw new Error("Debe haber al menos un servicio de impresión habilitado.");
    const rateKeys=rates.map(rate=>`${rate.formato}/${rate.papel}/${rate.color}`);
    if(new Set(rateKeys).size!==rateKeys.length)throw new Error("Hay tarifas repetidas para la misma combinación de formato, papel y color.");
    if(new Set(offers.map(offer=>offer.servicio)).size!==offers.length)throw new Error("Cada servicio puede aparecer una sola vez en la configuración.");
    for(const offer of offers){const printing=catalog.servicios.find(service=>service.codigoPublico===offer.servicio)?.tipo==="IMPRESION";
      if(!printing&&offer.habilitado&&offer.compatibilidades.length===0)throw new Error(`Agregá al menos una compatibilidad para ${offer.nombreVisible||"el servicio de terminación"}.`);
      const pairs=offer.compatibilidades.map(pair=>`${pair.formato}/${pair.papel}`);if(new Set(pairs).size!==pairs.length)throw new Error("Hay compatibilidades repetidas en un servicio.");
    }
    return {versionBase:base,operacion:crypto.randomUUID(),motivo:reason.trim(),programadaPara:date,tarifas:rates.map(({formato,papel,color,precio,recargoDobleFaz,habilitada})=>({formato,papel,color:color as ColorMode,precio:parseAmount(precio,"Precio por carilla"),recargoDobleFaz:parseAmount(recargoDobleFaz,"Recargo doble faz"),habilitada})),servicios:offers.map(({servicio,nombreVisible,basePrecio,precio,preparacionMinutos,habilitado,compatibilidades})=>({servicio,nombreVisible:nombreVisible.trim(),basePrecio:basePrecio as PriceBase,precio:parseAmount(precio,"Precio del servicio"),preparacionMinutos:Number(preparacionMinutos),habilitado,compatibilidades:compatibilidades.map(({formato,papel})=>({formato,papel}))}))};
  }
  async function save(event?:FormEvent<HTMLFormElement>){
    event?.preventDefault();if(busy)return;
    setMessage("");setSaved(null);setBusy(true);
    try{
      if(!pending.current)pending.current=payload();
      const response=await secureMutation("/api/admin/catalogo/revisiones",JSON.stringify(pending.current),"application/json");
      const data:unknown=await response.json();if(!isCatalogRevision(data))throw new Error("No pudimos confirmar la configuración recibida.");
      pending.current=null;setUncertain(false);setConflict(data.estado!=="VIGENTE"&&data.estado!=="PROGRAMADA");setComparison(null);if(data.estado==="VIGENTE"){setBase(data.codigoPublico);setBaseNumber(data.numero);}setReason("");setRates(seedRates(data));setOffers(seedOffers(data));setSaved(data);onSaved(data);
    }catch(error){
      setMessage(error instanceof Error?catalogConfigurationText(error.message):"No pudimos guardar la configuración.");
      if(error instanceof MutationError&&!error.uncertain){pending.current=null;setUncertain(false);setConflict(error.status===409);}
      else if(pending.current){setUncertain(true);}
    }finally{setBusy(false);}
  }
  async function checkLatest(){setChecking(true);try{setComparison(await readCatalog());}catch(error){setMessage(error instanceof Error?catalogConfigurationText(error.message):"No pudimos consultar la configuración vigente.");}finally{setChecking(false);}}
  function adoptBase(){if(!comparison)return;setBase(comparison.actual?.codigoPublico??null);setBaseNumber(comparison.actual?.numero??null);setConflict(false);setComparison(null);setMessage("");pending.current=null;}
  return <section id="revision-comercial" className="pricing-editor"><form onSubmit={save} className="admin-form">
    <div className="catalog-editor-heading"><div><h2>Nueva configuración comercial</h2><p>{baseNumber===null?"Primera configuración: todavía no hay precios vigentes.":`Editando a partir de la configuración ${baseNumber}. Al guardar se creará una nueva configuración.`}</p></div><span className="pricing-currency">ARS · Pesos argentinos</span></div>
    <div className="pricing-guide"><strong>Configurá en este orden</strong><ol><li><b>1. Tarifas</b><span>Cuánto vale imprimir en cada papel y color.</span></li><li><b>2. Servicios</b><span>Qué trabajos ofrecés y cómo cobrás sus terminaciones.</span></li><li><b>3. Aplicación</b><span>Revisá el conjunto y elegí cuándo usar estos precios.</span></li></ol><p>Todos los precios y recargos son importes fijos en ARS. Esta vista no aplica descuentos ni porcentajes. Escribí, por ejemplo, <b>1500,50</b> o <b>1500.50</b>, sin separadores de miles.</p></div>
    <fieldset disabled={locked} className="admin-fieldset">
      <section className="admin-card pricing-section" aria-labelledby="pricing-rates-title">
        <div className="admin-section-title"><div><h2 id="pricing-rates-title">1. Tarifas de impresión <span className="admin-count">{rates.length}</span></h2><p>Una tarifa combina papel, tamaño y color con un precio por carilla.</p></div><button type="button" className="admin-button" disabled={rates.length>=300||!catalog.papelesHabilitados.some(p=>p.habilitado)} onClick={addRate}>+ Agregar tarifa</button></div>
        <CatalogInfo title="¿Qué es una tarifa?"><p>Es la regla que calcula el costo de imprimir una carilla (una cara de la hoja) en un papel y modo de color concretos. A4 de 80 g/m² en blanco y negro puede tener un precio distinto de A4 de 90 g/m² a color.</p><p>El total usa las páginas del documento × cantidad de copias. En doble faz se suma el recargo a cada carilla impresa; no se divide el precio por dos.</p><p>Los servicios de impresión comparten estas tarifas. El nombre del servicio no limita el color: el modo disponible se determina por las tarifas habilitadas.</p></CatalogInfo>
        {rates.length===0&&<p className="admin-empty">Agregá tu primera tarifa. Si no hay papeles disponibles, habilitalos primero en Catálogo base.</p>}
        <div className="pricing-card-list">{rates.map((rate,index)=><article className="pricing-entry pricing-rate" id={rate.id} key={rate.id} aria-label={`Tarifa ${index+1}`}>
          <header><h3>Tarifa {index+1}</h3><span className={`pricing-state ${rate.habilitada?"enabled":""}`}>{rate.habilitada?"Habilitada al guardar":"Deshabilitada"}</span><button type="button" className="admin-link-button" onClick={()=>{setSaved(null);setRates(current=>current.filter(row=>row.id!==rate.id));}}>Quitar tarifa {index+1}</button></header>
          <div className="pricing-fields">
            <div className="pricing-wide"><CatalogPaperSelect catalog={catalog} value={rate} disabled={locked} label={`Papel de tarifa ${index+1}`} onChange={pair=>changeRate(rate.id,pair)}/></div>
            <label><span className="pricing-label-text">Color de tarifa {index+1}</span><select required value={rate.color} onChange={e=>changeRate(rate.id,{color:e.target.value as ColorMode})}><option value="" disabled>Elegir modo de color</option>{Object.entries(colorLabels).map(([key,label])=><option value={key} key={key}>{label}</option>)}</select><small>Una tarifa por cada combinación de papel y color.</small></label>
            <CatalogAmount label={`Precio por carilla de tarifa ${index+1}`} value={rate.precio} onChange={precio=>changeRate(rate.id,{precio})} help={<p>Lo que cobrás por cada cara impresa. Se multiplica por las páginas y las copias del documento.</p>}/>
            <CatalogAmount label={`Recargo doble faz de tarifa ${index+1}`} value={rate.recargoDobleFaz} onChange={recargoDobleFaz=>changeRate(rate.id,{recargoDobleFaz})} help={<p>Adicional fijo en ARS por cada carilla impresa cuando el cliente elige doble faz. No es un porcentaje ni un descuento. Dejá 0 si no cobrás este adicional.</p>}/>
          </div>
          {validAmount(rate.precio)&&validAmount(rate.recargoDobleFaz)&&<p className="pricing-example"><strong>Ejemplo · 4 páginas, 1 copia:</strong> simple faz {ars(parseAmount(rate.precio,"Precio")*4)}; doble faz {ars((parseAmount(rate.precio,"Precio")+parseAmount(rate.recargoDobleFaz,"Recargo"))*4)}. Sólo impresión, sin terminaciones.</p>}
          <label className="admin-check pricing-enable"><input type="checkbox" checked={rate.habilitada} onChange={e=>changeRate(rate.id,{habilitada:e.target.checked})}/>Habilitar tarifa {index+1} en esta configuración</label>
        </article>)}</div>
      </section>
      <section className="admin-card pricing-section" aria-labelledby="pricing-offers-title">
        <div className="admin-section-title"><div><h2 id="pricing-offers-title">2. Servicios ofrecidos <span className="admin-count">{offers.length}</span></h2><p>Elegí servicios del Catálogo base y completá cómo se ofrecen al cliente.</p></div><button type="button" className="admin-button" disabled={offers.length>=100||!catalog.servicios.length} onClick={addOffer}>+ Agregar servicio</button></div>
        <CatalogInfo title="¿Qué es un servicio y cómo se configura?"><p>Es un trabajo que ofrece tu imprenta, como impresión o anillado. Primero se crea su identidad en Catálogo base. Aquí definís el nombre que verá el cliente, el tiempo de preparación y si está habilitado.</p><p>La impresión toma su precio de las tarifas del paso 1. Las terminaciones suman su propio cargo, según la unidad de cobro elegida, y necesitan papeles compatibles con tarifa habilitada.</p><p>Para guardar necesitás al menos una tarifa y un servicio de impresión habilitados. Crear un servicio base no lo publica automáticamente.</p></CatalogInfo>
        {offers.length===0&&<p className="admin-empty">Agregá un servicio de impresión. Después incorporá las terminaciones que ofrezcas. Si falta un servicio, crealo en Catálogo base.</p>}
        <div className="pricing-card-list">{offers.map((offer,index)=>{const service=catalog.servicios.find(s=>s.codigoPublico===offer.servicio),printing=service?.tipo==="IMPRESION";return <article className="pricing-entry pricing-offer" key={offer.id} id={offer.id} aria-label={`Servicio ofrecido ${index+1}`}>
          <header><h3>Servicio {index+1}</h3><span className={`pricing-state ${offer.habilitado?"enabled":""}`}>{offer.habilitado?"Habilitado al guardar":"Deshabilitado"}</span><button type="button" className="admin-link-button" onClick={()=>{setSaved(null);setOffers(current=>current.filter(row=>row.id!==offer.id));}}>Quitar servicio {index+1}</button></header>
          <div className="pricing-fields pricing-service-fields">
            <CatalogSearchSelect label={`Servicio base ${index+1}`} options={catalog.servicios.map(s=>({value:s.codigoPublico,label:s.nombre,detail:`${s.codigo} · ${s.tipo==="IMPRESION"?"Impresión":"Terminación"}`}))} value={offer.servicio} disabled={locked} onChange={id=>chooseService(offer,id)} placeholder="Ej.: impresión, anillado o código"/>
            <label><span className="pricing-label-text">Nombre visible de servicio {index+1}</span><input required maxLength={140} value={offer.nombreVisible} onChange={e=>changeOffer(offer.id,{nombreVisible:e.target.value})}/><small>Así lo verá el cliente. Podés editar el nombre sugerido.</small></label>
            <div className="pricing-field"><CatalogInfo title={`Preparación de servicio ${index+1} (minutos)`} htmlFor={`prep-${offer.id}`}><p>Tiempo mínimo de preparación del servicio, entre 0 y 10080 minutos. El cálculo del ítem usa el mayor tiempo entre impresión y terminaciones; no suma todos los tiempos. La entrega también depende de los horarios y reglas operativas.</p></CatalogInfo><input id={`prep-${offer.id}`} type="number" required min="0" max="10080" step="1" value={offer.preparacionMinutos} onChange={e=>changeOffer(offer.id,{preparacionMinutos:e.target.value})}/><small>Minutos enteros. Ej.: 30 = media hora; 0 = sin mínimo adicional.</small></div>
            {printing?<div className="pricing-wide admin-info compact"><strong>Impresión · precio tomado de las tarifas</strong><p>Se cobra por carilla según el papel y color del paso 1. Este servicio no agrega un segundo cargo (ARS 0,00). Sus papeles disponibles son los de las tarifas habilitadas.</p></div>:service&&<>
              <div className="pricing-field"><CatalogInfo title={`Forma de cobro de servicio ${index+1}`} htmlFor={`basis-${offer.id}`}><p>La terminación se suma al precio de impresión. Elegí qué cantidad multiplica el importe unitario.</p>{Object.entries(priceExplanations).map(([key,text])=><p key={key}><strong>{priceLabels[key as PriceBase]}:</strong> {text}</p>)}</CatalogInfo><select id={`basis-${offer.id}`} required value={offer.basePrecio} onChange={e=>changeOffer(offer.id,{basePrecio:e.target.value as PriceBase})}><option value="" disabled>Elegir unidad de cobro</option>{Object.entries(priceLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select>{offer.basePrecio&&<small>{priceExplanations[offer.basePrecio]}</small>}</div>
              <CatalogAmount label={`Precio unitario de servicio ${index+1}`} value={offer.precio} onChange={precio=>changeOffer(offer.id,{precio})} help={<p>Importe en ARS por la unidad elegida. Por ejemplo, ARS 500 por copia se convierte en ARS 1500 para 3 copias. Un precio fijo por ítem se cobra una sola vez para ese documento.</p>}/>
            </>}
          </div>
          {service&&!printing&&<fieldset className="pricing-compatibilities"><legend>Papeles compatibles con servicio {index+1}</legend><CatalogInfo title={`Compatibilidades de servicio ${index+1}`}><p>Indican en qué papeles y tamaños podés realizar esta terminación. No agregan otro precio. Para habilitar el servicio necesitás al menos una compatibilidad y una tarifa de impresión habilitada para cada papel elegido.</p></CatalogInfo>
            {offer.compatibilidades.length===0&&<p className="admin-note">Agregá al menos un papel compatible si vas a habilitar esta terminación.</p>}
            {offer.compatibilidades.map((pair,pairIndex)=><div className="pricing-pair" key={pair.id}><CatalogPaperSelect catalog={catalog} value={pair} disabled={locked} label={`Papel compatible ${pairIndex+1} de servicio ${index+1}`} onChange={p=>pairChange(offer,pair.id,p)}/><button type="button" className="admin-link-button" onClick={()=>changeOffer(offer.id,{compatibilidades:offer.compatibilidades.filter(p=>p.id!==pair.id)})}>Quitar compatibilidad {pairIndex+1} de servicio {index+1}</button>{pair.papel&&!rates.some(r=>r.habilitada&&r.formato===pair.formato&&r.papel===pair.papel)&&<p className="pricing-wide admin-note">Este papel necesita una tarifa habilitada en el paso 1 para ofrecer la terminación.</p>}</div>)}
            <button type="button" className="admin-button secondary" disabled={offer.compatibilidades.length>=300||!catalog.papelesHabilitados.some(p=>p.habilitado)} onClick={()=>changeOffer(offer.id,{compatibilidades:[...offer.compatibilidades,{id:crypto.randomUUID(),formato:"",papel:""}]})}>+ Agregar papel compatible a servicio {index+1}</button>
          </fieldset>}
          <label className="admin-check pricing-enable"><input type="checkbox" checked={offer.habilitado} onChange={e=>changeOffer(offer.id,{habilitado:e.target.checked})}/>Habilitar servicio {index+1} en esta configuración</label>
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
    <div className="catalog-save-actions"><span>{rates.filter(r=>r.habilitada).length} tarifas y {offers.filter(o=>o.habilitado).length} servicios habilitados en el formulario</span><button className="admin-button" disabled={locked||conflict}>{busy?"Guardando…":scheduled?"Confirmar programación":"Guardar nueva configuración"}</button></div>
  </form></section>;
}
