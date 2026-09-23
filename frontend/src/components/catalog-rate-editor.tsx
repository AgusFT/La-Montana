//#region ENCABEZADO · src/components/catalog-rate-editor.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-rate-editor.tsx
 * ========================================================================
 * FUNCIÓN
 * Edita el nombre, color, papeles y precios de una tarifa mediante pasos. Permite precios finales
 * o relativos, presenta el importe por hoja y convierte explícitamente tarifas anteriores.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * - [export] CatalogRateEditor({rate,index,rates,catalog,onChange,onRemove}:
 *   {rate:RateDraft;index:number;rates:RateDraft[];catalog:CatalogState;onChange:(change:Partial<RateDraft>)=>void;onRemove:()=>void})
 * - CatalogRateEditor :: changeColor(color: ColorMode)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos propios.
 * ========================================================================
 */
//#endregion

"use client";
import {useState} from "react";
import {CatalogRatePapers,pairKey} from "@/components/catalog-rate-papers";
import {CatalogInfo,CatalogAmount} from "@/components/catalog-pricing-fields";
import {amountDraft,amountPattern,ars,parseAmount,validAmount} from "@/lib/catalog-pricing";
import {duplexPrice,rateName,type RateDraft} from "@/lib/catalog-rate-draft";
import {colorLabels,type CatalogState,type ColorMode,type DuplexMode} from "@/lib/catalog-types";

export function CatalogRateEditor({rate,index,rates,catalog,onChange,onRemove}:{rate:RateDraft;index:number;rates:RateDraft[];catalog:CatalogState;onChange:(change:Partial<RateDraft>)=>void;onRemove:()=>void}){
  const [editingName,setEditingName]=useState(false),[oldName,setOldName]=useState(rate.nombre);
  const legacy=rate.recargoAnterior!==undefined,final=duplexPrice(rate.precio,rate.modoDobleFaz,rate.valorDobleFaz);
  const simple=validAmount(rate.precio)?parseAmount(rate.precio,"Simple faz"):null;
  const occupied=new Map(rates.filter(r=>r.id!==rate.id&&r.color===rate.color).flatMap(r=>r.papeles.map(pair=>[pairKey(pair),r.nombre] as [string,string])));
  function changeColor(color:ColorMode){onChange({color,...(!rate.nombrePersonalizado?{nombre:rateName(color,index)}:{})});}
  return <article className="pricing-entry pricing-rate" id={rate.id} aria-label={`Tarifa ${index+1}`}>
    <header>
      <div className="rate-name">
        {editingName?<input aria-label={`Nombre de tarifa ${index+1}`} maxLength={140} required autoFocus value={rate.nombre} onChange={e=>onChange({nombre:e.target.value,nombrePersonalizado:true})} onBlur={()=>{if(rate.nombre.trim())setEditingName(false);}} onKeyDown={e=>{if(e.key==="Enter"){e.preventDefault();if(rate.nombre.trim())setEditingName(false);}if(e.key==="Escape"){e.preventDefault();onChange({nombre:oldName});setEditingName(false);}}}/>:<h3>{rate.nombre}</h3>}
        <button type="button" className="rate-rename" aria-label={`Editar nombre de tarifa ${index+1}`} title="Editar nombre de la tarifa" onClick={()=>{setOldName(rate.nombre);setEditingName(true);}}>
          <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="m16 3 5 5L9 20l-6 1 1-6Z M13 6l5 5"/></svg>
        </button>
      </div>
      <span className={`pricing-state ${rate.habilitada?"enabled":""}`}>{rate.habilitada?"Habilitada al guardar":"Deshabilitada"}</span>
      <button type="button" className="admin-link-button" onClick={onRemove}>Quitar tarifa {index+1}</button>
    </header>
    <fieldset className="rate-step"><legend>1. Elegí el modo de impresión</legend>
      <label className="rate-color">Color de tarifa {index+1}<select data-rate-color required value={rate.color} onChange={e=>changeColor(e.target.value as ColorMode)}><option value="" disabled>Elegir color o blanco y negro</option>{Object.entries(colorLabels).map(([key,label])=><option value={key} key={key}>{label}</option>)}</select></label>
    </fieldset>
    {rate.color&&<>
      <fieldset className="rate-step"><legend>2. Seleccioná las hojas y sus variantes</legend>
        <CatalogRatePapers catalog={catalog} value={rate.papeles} occupied={occupied} label={`Papeles de tarifa ${index+1}`} onChange={papeles=>onChange({papeles})}/>
      </fieldset>
      <fieldset className="rate-step"><legend>3. Definí los precios para la selección</legend>
        <p className="admin-note">La misma regla se aplicará a las {rate.papeles.length} variantes seleccionadas. Para otros precios, creá otra tarifa con esas variantes.</p>
        {legacy?<div className="admin-warning">
          <strong>Tarifa anterior: conserva el cobro por cada cara impresa.</strong>
          <p>Simple faz: {simple===null?"—":ars(simple)} por cara. Doble faz: {simple===null?"—":ars(simple+(rate.recargoAnterior??0))} por cara. Mantiene sus importes hasta que elijas cambiarla.</p>
          <p>Al convertirla se propone el precio equivalente de una hoja con dos caras. La última hoja impar pasará a cobrarse a simple faz. Revisá los importes antes de guardar.</p>
          <button type="button" className="admin-button secondary" onClick={()=>onChange({recargoAnterior:undefined,modoDobleFaz:"FIJO",valorDobleFaz:amountDraft(2*((simple??0)+(rate.recargoAnterior??0)))})}>Cambiar al cobro por hoja</button>
        </div>:<>
          <div className="rate-price-grid">
            <CatalogAmount label={`Precio simple faz de tarifa ${index+1}`} value={rate.precio} onChange={precio=>onChange({precio})} help={<p>Precio final de una hoja impresa en una sola cara. También se aplica a la última hoja si el documento doble faz tiene una página impar.</p>}/>
            <div className="pricing-field"><CatalogInfo title={`Cómo definir doble faz de tarifa ${index+1}`} htmlFor={`duplex-${rate.id}`}><p>Precio final: independiente del simple faz, incluso si es menor. Sumar importe: simple faz más un monto fijo en ARS. Sumar porcentaje: simple faz más el porcentaje indicado.</p><p>Los modos relativos se recalculan al cambiar simple faz. El resultado se redondea a centavos por hoja. Las cotizaciones anteriores conservan sus importes.</p></CatalogInfo>
              <select id={`duplex-${rate.id}`} value={rate.modoDobleFaz} onChange={e=>onChange({modoDobleFaz:e.target.value as DuplexMode,valorDobleFaz:""})}><option value="FIJO">Ingresar precio final</option><option value="ADICIONAL">Sumar un importe a simple faz</option><option value="PORCENTAJE">Sumar un porcentaje a simple faz</option></select>
            </div>
            {rate.modoDobleFaz==="PORCENTAJE"?<div className="pricing-field"><CatalogInfo title={`Porcentaje adicional de tarifa ${index+1}`} htmlFor={`percent-${rate.id}`}><p>Escribí 10 para sumar un 10 % sobre simple faz. Por ejemplo, simple faz ARS 30 + 10 % da ARS 33 por hoja doble faz. No escribas el símbolo %. Hasta dos decimales.</p></CatalogInfo>
              <div className="pricing-money"><span aria-hidden="true">%</span><input id={`percent-${rate.id}`} type="text" inputMode="decimal" required pattern={amountPattern} maxLength={15} value={rate.valorDobleFaz} onChange={e=>onChange({valorDobleFaz:e.target.value})} placeholder="10" title="Porcentaje desde 0, sin símbolo %, hasta dos decimales"/></div><small>Porcentaje adicional · ej.: 10 o 12,50</small>
            </div>:<CatalogAmount label={`${rate.modoDobleFaz==="FIJO"?"Precio final doble faz":"Importe adicional para doble faz"} de tarifa ${index+1}`} value={rate.valorDobleFaz} onChange={valorDobleFaz=>onChange({valorDobleFaz})} help={<p>{rate.modoDobleFaz==="FIJO"?"Precio total de una hoja impresa de ambos lados. No depende del precio simple faz.":"Monto que se suma una sola vez al precio simple faz para obtener el precio final de una hoja impresa de ambos lados."}</p>}/>}
          </div>
          <div className="rate-final-prices" aria-live="polite"><div><span>Simple faz · 1 cara por hoja</span><strong>{simple===null?"Completá el precio":ars(simple)}</strong></div><div><span>Doble faz · 2 caras por hoja</span><strong>{final===null?"Completá el cálculo":ars(final)}</strong></div></div>
          {simple!==null&&final!==null&&<p className="pricing-example"><strong>Ejemplo · 3 páginas, 1 copia:</strong> 1 hoja doble faz ({ars(final)}) + 1 hoja simple faz ({ars(simple)}) = <b>{ars(final+simple)}</b>. Con 2 copias: {ars((final+simple)*2)}. Sólo impresión, sin terminaciones.</p>}
          {validAmount(rate.precio)&&validAmount(rate.valorDobleFaz)&&final===null&&<p className="error-message" role="alert">El precio final doble faz supera ARS 999999999999,99. Reducí el importe o porcentaje.</p>}
        </>}
      </fieldset>
    </>}
    <label className="admin-check pricing-enable"><input type="checkbox" checked={rate.habilitada} onChange={e=>onChange({habilitada:e.target.checked})}/>Habilitar tarifa {index+1} al guardar esta configuración</label>
  </article>;
}
