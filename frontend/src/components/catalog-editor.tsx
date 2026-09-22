"use client";
import { Fragment, useRef, useState, type FormEvent } from "react";
import { MutationError, secureMutation } from "@/lib/secure-mutation";
import { readCatalog } from "@/lib/catalog-client";
import { RevisionView } from "@/components/catalog-view";
import { colorLabels, priceLabels, isCatalogRevision, type CatalogState, type CatalogRevision, type ColorMode, type PriceBase, type NewRevision } from "@/lib/catalog-types";

type RateDraft={id:string;formato:string;papel:string;color:ColorMode|"";precio:string;recargoDobleFaz:string;habilitada:boolean};
type PairDraft={id:string;formato:string;papel:string};
type OfferDraft={id:string;servicio:string;nombreVisible:string;basePrecio:PriceBase|"";precio:string;preparacionMinutos:string;habilitado:boolean;compatibilidades:PairDraft[]};
function seedRates(revision:CatalogRevision|null):RateDraft[]{return revision?.tarifas.map((rate,i)=>({...rate,id:`rate-${i}`,precio:String(rate.precio),recargoDobleFaz:String(rate.recargoDobleFaz)}))??[];}
function seedOffers(revision:CatalogRevision|null):OfferDraft[]{return revision?.servicios.map((offer,i)=>({...offer,id:`offer-${i}`,precio:String(offer.precio),preparacionMinutos:String(offer.preparacionMinutos),compatibilidades:offer.compatibilidades.map((pair,j)=>({...pair,id:`pair-${i}-${j}`}))}))??[];}
function Options({items}:{items:Array<{codigoPublico:string;nombre:string;codigo:string}>}){return <><option value="" disabled>Seleccionar</option>{items.map(item=><option key={item.codigoPublico} value={item.codigoPublico}>{item.nombre} ({item.codigo})</option>)}</>;}

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
  function chooseService(row:OfferDraft,id:string){const printing=catalog.servicios.find(service=>service.codigoPublico===id)?.tipo==="IMPRESION";changeOffer(row.id,{servicio:id,...(printing?{basePrecio:"POR_CARILLA",precio:"0",compatibilidades:[]}:row.basePrecio==="POR_CARILLA"&&catalog.servicios.find(service=>service.codigoPublico===row.servicio)?.tipo==="IMPRESION"?{basePrecio:"",precio:""}:{})});}
  function pairChange(row:OfferDraft,id:string,change:Partial<PairDraft>){changeOffer(row.id,{compatibilidades:row.compatibilidades.map(pair=>pair.id===id?{...pair,...change}:pair)});}
  function payload():NewRevision{
    let date:string|null=null;
    if(scheduled){const chosen=new Date(scheduledFor+":00Z");if(!scheduledFor||Number.isNaN(chosen.valueOf())||chosen.valueOf()<=Date.now())throw new Error("Elegí una fecha y hora UTC futura.");date=chosen.toISOString();}
    if(!rates.some(rate=>rate.habilitada))throw new Error("Habilitá al menos una tarifa de impresión.");
    if(offers.filter(offer=>offer.habilitado&&catalog.servicios.find(service=>service.codigoPublico===offer.servicio)?.tipo==="IMPRESION").length<1)throw new Error("Debe haber al menos un servicio de impresión habilitado.");
    const rateKeys=rates.map(rate=>`${rate.formato}/${rate.papel}/${rate.color}`);
    if(new Set(rateKeys).size!==rateKeys.length)throw new Error("Hay tarifas repetidas para la misma combinación de formato, papel y color.");
    if(new Set(offers.map(offer=>offer.servicio)).size!==offers.length)throw new Error("Cada servicio puede aparecer una sola vez en la revisión.");
    for(const offer of offers){const printing=catalog.servicios.find(service=>service.codigoPublico===offer.servicio)?.tipo==="IMPRESION";
      if(!printing&&offer.habilitado&&offer.compatibilidades.length===0)throw new Error(`Agregá al menos una compatibilidad para ${offer.nombreVisible||"el servicio de terminación"}.`);
      const pairs=offer.compatibilidades.map(pair=>`${pair.formato}/${pair.papel}`);if(new Set(pairs).size!==pairs.length)throw new Error("Hay compatibilidades repetidas en un servicio.");
    }
    return {versionBase:base,operacion:crypto.randomUUID(),motivo:reason.trim(),programadaPara:date,tarifas:rates.map(({formato,papel,color,precio,recargoDobleFaz,habilitada})=>({formato,papel,color:color as ColorMode,precio:Number(precio),recargoDobleFaz:Number(recargoDobleFaz),habilitada})),servicios:offers.map(({servicio,nombreVisible,basePrecio,precio,preparacionMinutos,habilitado,compatibilidades})=>({servicio,nombreVisible:nombreVisible.trim(),basePrecio:basePrecio as PriceBase,precio:Number(precio),preparacionMinutos:Number(preparacionMinutos),habilitado,compatibilidades:compatibilidades.map(({formato,papel})=>({formato,papel}))}))};
  }
  async function save(event?:FormEvent<HTMLFormElement>){
    event?.preventDefault();if(busy)return;
    setMessage("");setSaved(null);setBusy(true);
    try{
      if(!pending.current)pending.current=payload();
      const response=await secureMutation("/api/admin/catalogo/revisiones",JSON.stringify(pending.current),"application/json");
      const data:unknown=await response.json();if(!isCatalogRevision(data))throw new Error("No pudimos confirmar la revisión recibida.");
      pending.current=null;setUncertain(false);setConflict(data.estado!=="VIGENTE"&&data.estado!=="PROGRAMADA");setComparison(null);if(data.estado==="VIGENTE"){setBase(data.codigoPublico);setBaseNumber(data.numero);}setReason("");setRates(seedRates(data));setOffers(seedOffers(data));setSaved(data);onSaved(data);
    }catch(error){
      setMessage(error instanceof Error?error.message:"No pudimos guardar la revisión.");
      if(error instanceof MutationError&&!error.uncertain){pending.current=null;setUncertain(false);setConflict(error.status===409);}
      else if(pending.current){setUncertain(true);}
    }finally{setBusy(false);}
  }
  async function checkLatest(){setChecking(true);try{setComparison(await readCatalog());}catch(error){setMessage(error instanceof Error?error.message:"No pudimos consultar la revisión vigente.");}finally{setChecking(false);}}
  function adoptBase(){if(!comparison)return;setBase(comparison.actual?.codigoPublico??null);setBaseNumber(comparison.actual?.numero??null);setConflict(false);setComparison(null);setMessage("");pending.current=null;}
  return <section id="revision-comercial"><form onSubmit={save} className="admin-form">
    <div className="catalog-editor-heading"><div><h2>Nueva revisión comercial</h2><p>{baseNumber===null?"Primera revisión: todavía no hay precios vigentes.":`Editando a partir de la revisión ${baseNumber}. Al guardar se creará una nueva revisión.`}</p></div><button type="button" className="admin-button secondary" disabled={locked} onClick={()=>setScheduled(value=>!value)}>{scheduled?"Usar vigencia inmediata":"Programar cambios"}</button></div>
    <fieldset disabled={locked} className="admin-fieldset">
      <div className="catalog-two-columns">
        <section className="admin-card"><div className="admin-section-title"><div><h2>Tarifas de impresión</h2><p>Precio por carilla según formato, papel y color.</p></div><button type="button" className="admin-button" disabled={rates.length>=300||!catalog.formatos.length||!catalog.papeles.length} onClick={()=>setRates(current=>[...current,{id:crypto.randomUUID(),formato:"",papel:"",color:"",precio:"",recargoDobleFaz:"",habilitada:false}])}>+ Agregar tarifa</button></div>
          {rates.length===0?<p className="admin-empty">No hay tarifas en esta revisión. Creá formatos y papeles en el catálogo base para comenzar.</p>:<div className="admin-table-wrap" tabIndex={0} aria-label="Edición de tarifas"><table className="admin-table catalog-edit-table"><thead><tr><th>Formato / papel</th><th>Color</th><th>Precio / recargo doble faz</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{rates.map((rate,index)=><tr key={rate.id}>
            <td><select aria-label={`Formato de tarifa ${index+1}`} required value={rate.formato} onChange={e=>changeRate(rate.id,{formato:e.target.value})}><Options items={catalog.formatos}/></select><select aria-label={`Papel de tarifa ${index+1}`} required value={rate.papel} onChange={e=>changeRate(rate.id,{papel:e.target.value})}><Options items={catalog.papeles}/></select></td>
            <td><select aria-label={`Color de tarifa ${index+1}`} required value={rate.color} onChange={e=>changeRate(rate.id,{color:e.target.value as ColorMode})}><option value="" disabled>Seleccionar</option>{Object.entries(colorLabels).map(([key,label])=><option value={key} key={key}>{label}</option>)}</select></td>
            <td><input aria-label={`Precio por carilla de tarifa ${index+1}`} type="number" required min="0" max="999999999999.99" step="0.01" value={rate.precio} onChange={e=>changeRate(rate.id,{precio:e.target.value})}/><input aria-label={`Recargo doble faz por carilla impresa de tarifa ${index+1}`} type="number" required min="0" max="999999999999.99" step="0.01" value={rate.recargoDobleFaz} onChange={e=>changeRate(rate.id,{recargoDobleFaz:e.target.value})}/></td>
            <td><label className="admin-check"><input type="checkbox" checked={rate.habilitada} onChange={e=>changeRate(rate.id,{habilitada:e.target.checked})}/>Habilitada</label></td><td><button type="button" className="admin-link-button" onClick={()=>setRates(current=>current.filter(row=>row.id!==rate.id))}>Quitar tarifa {index+1}</button></td>
          </tr>)}</tbody></table></div>}
          <p className="admin-info compact">El precio de impresión se define sólo en estas tarifas. El recargo por doble faz se cobra por carilla impresa; ingresá 0 cuando no corresponda.</p>
        </section>
        <section className="admin-card"><div className="admin-section-title"><div><h2>Servicios ofrecidos</h2><p>Impresión y terminaciones, con sus condiciones.</p></div><button type="button" className="admin-button" disabled={offers.length>=100||!catalog.servicios.length} onClick={()=>setOffers(current=>[...current,{id:crypto.randomUUID(),servicio:"",nombreVisible:"",basePrecio:"",precio:"",preparacionMinutos:"",habilitado:false,compatibilidades:[]}])}>+ Agregar servicio</button></div>
          {offers.length===0?<p className="admin-empty">No hay servicios en esta revisión. Crealos en el catálogo base y agregalos aquí.</p>:<div className="admin-table-wrap" tabIndex={0} aria-label="Edición de servicios"><table className="admin-table catalog-edit-table"><thead><tr><th>Servicio / nombre visible</th><th>Forma de cobro / precio</th><th>Preparación (min)</th><th>Estado</th></tr></thead><tbody>{offers.map((offer,index)=>{const printing=catalog.servicios.find(s=>s.codigoPublico===offer.servicio)?.tipo==="IMPRESION";return <Fragment key={offer.id}><tr>
            <td><select aria-label={`Servicio ${index+1}`} required value={offer.servicio} onChange={e=>chooseService(offer,e.target.value)}><Options items={catalog.servicios}/></select><input aria-label={`Nombre visible de servicio ${index+1}`} required maxLength={140} value={offer.nombreVisible} onChange={e=>changeOffer(offer.id,{nombreVisible:e.target.value})}/></td>
            <td>{printing?<><span>Por carilla</span><small>Precio en las tarifas (cargo del servicio: 0)</small></>:<><select aria-label={`Base de precio de servicio ${index+1}`} required value={offer.basePrecio} onChange={e=>changeOffer(offer.id,{basePrecio:e.target.value as PriceBase})}><option value="" disabled>Seleccionar</option>{Object.entries(priceLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select><input aria-label={`Precio de servicio ${index+1}`} type="number" required min="0" max="999999999999.99" step="0.01" value={offer.precio} onChange={e=>changeOffer(offer.id,{precio:e.target.value})}/></>}</td>
            <td><input aria-label={`Preparación en minutos de servicio ${index+1}`} type="number" required min="0" max="10080" step="1" value={offer.preparacionMinutos} onChange={e=>changeOffer(offer.id,{preparacionMinutos:e.target.value})}/></td>
            <td><label className="admin-check"><input type="checkbox" checked={offer.habilitado} onChange={e=>changeOffer(offer.id,{habilitado:e.target.checked})}/>Habilitado</label><button type="button" className="admin-link-button" onClick={()=>setOffers(current=>current.filter(row=>row.id!==offer.id))}>Quitar servicio {index+1}</button></td>
          </tr>{!printing&&<tr><td colSpan={4}><fieldset className="catalog-compatibility"><legend>Compatibilidades de {offer.nombreVisible||`servicio ${index+1}`}</legend>{offer.compatibilidades.length===0&&<p className="admin-note">Agregá al menos un formato y papel si la terminación estará habilitada.</p>}{offer.compatibilidades.map((pair,pairIndex)=><div className="catalog-pair" key={pair.id}><select aria-label={`Formato compatible ${pairIndex+1} de servicio ${index+1}`} required value={pair.formato} onChange={e=>pairChange(offer,pair.id,{formato:e.target.value})}><Options items={catalog.formatos}/></select><select aria-label={`Papel compatible ${pairIndex+1} de servicio ${index+1}`} required value={pair.papel} onChange={e=>pairChange(offer,pair.id,{papel:e.target.value})}><Options items={catalog.papeles}/></select><button type="button" className="admin-link-button" onClick={()=>changeOffer(offer.id,{compatibilidades:offer.compatibilidades.filter(p=>p.id!==pair.id)})}>Quitar</button></div>)}<button type="button" className="admin-link-button" disabled={offer.compatibilidades.length>=300||!catalog.formatos.length||!catalog.papeles.length} onClick={()=>changeOffer(offer.id,{compatibilidades:[...offer.compatibilidades,{id:crypto.randomUUID(),formato:"",papel:""}]})}>+ Agregar compatibilidad</button></fieldset></td></tr>}</Fragment>;})}</tbody></table></div>}
          <p className="admin-info compact">Habilitá al menos un servicio de impresión. Elegí los precios y compatibilidades de cada terminación.</p>
        </section>
      </div>
      <div className="admin-card catalog-save">{scheduled&&<label>Fecha y hora de vigencia (UTC)<input type="datetime-local" required value={scheduledFor} onChange={event=>setScheduledFor(event.target.value)}/></label>}<div className="catalog-field"><label htmlFor="catalog-reason">Motivo de la nueva revisión</label><textarea id="catalog-reason" required maxLength={500} value={reason} onChange={e=>{setReason(e.target.value);setSaved(null);}}/></div><p className="admin-note">{scheduled?"La revisión vigente se conserva hasta la fecha elegida. Sólo puede haber una programación comercial pendiente; cancelala para reemplazarla.":"Los cambios se aplican inmediatamente al catálogo vigente. El historial anterior se conserva."}</p></div>
    </fieldset>
    {message&&<p className={`form-message ${conflict||uncertain?"admin-warning":"error-message"}`} role="alert">{message}</p>}
    {uncertain&&<div className="admin-warning"><p>No pudimos confirmar el resultado del envío. Conservamos la misma operación y sus datos para reintentar sin crear una revisión duplicada.</p><button type="button" className="admin-button" disabled={busy} onClick={()=>save()}>{busy?"Confirmando…":"Reintentar el mismo envío"}</button></div>}
    {conflict&&<div className="admin-warning"><p>Tus valores siguen en el formulario. Consultá la revisión vigente antes de decidir si querés usarlos sobre la nueva base.</p><button type="button" className="admin-button secondary" disabled={checking} onClick={checkLatest}>{checking?"Consultando…":"Consultar revisión vigente"}</button></div>}
    {comparison&&<div className="admin-card"><h3>Comparar con el catálogo vigente</h3>{comparison.actual?<RevisionView catalog={comparison} revision={comparison.actual}/>:<p>No hay una revisión vigente.</p>}<button type="button" className="admin-button secondary" onClick={adoptBase}>Usar esta base conservando mis valores</button></div>}
    {saved!==null&&<p className="admin-success" role="status">{saved.estado==="PROGRAMADA"?`Revisión ${saved.numero} programada.`:saved.estado==="VIGENTE"?`Revisión ${saved.numero} guardada y vigente.`:`La revisión ${saved.numero} ya fue procesada; su estado actual es ${saved.estado.toLowerCase()}. Consultá la vigente antes de guardar nuevos cambios.`}</p>}
    <div className="catalog-save-actions"><span>{rates.filter(r=>r.habilitada).length} tarifas y {offers.filter(o=>o.habilitado).length} servicios habilitados en el formulario</span><button className="admin-button" disabled={locked||conflict}>{busy?"Guardando…":scheduled?"Confirmar programación":"Guardar nueva revisión"}</button></div>
  </form></section>;
}
