"use client";

import {useState} from "react";
import {useRoleTutorial} from "./role-tutorial-context";
import {CatalogAmount} from "./catalog-pricing-fields";
import {ars, parseAmount, validAmount} from "@/lib/catalog-pricing";
import {seedRates, type RateDraft} from "@/lib/catalog-rate-draft";
import {colorLabels, type CatalogState} from "@/lib/catalog-types";

type Practice = {version:string; servicio:string; variante:string; precio:string; aplicada:boolean};
function readPractice(value:string|undefined, version:string):Practice {
  if(value){
    try {
      const data:unknown=JSON.parse(value);
      if(data && typeof data==="object" && "version" in data && typeof data.version==="string" &&
          "servicio" in data && typeof data.servicio==="string" && "variante" in data && typeof data.variante==="string" &&
          "precio" in data && typeof data.precio==="string" && "aplicada" in data && typeof data.aplicada==="boolean") return data as Practice;
    } catch { /* Un borrador de un formato anterior se puede rehacer. */ }
  }
  return {version,servicio:"",variante:"",precio:"",aplicada:false};
}

export function TutorialTarifa({catalog, onTransfer, canTransfer}: {
  catalog:CatalogState; canTransfer:boolean; onTransfer:(id:string,price:string)=>void;
}){
  const guide=useRoleTutorial("administrador");
  if(!guide || guide.lesson!=="tarifas") return null;
  return <PriceExercise key={catalog.actual?.codigoPublico??"inicial"} catalog={catalog} onTransfer={onTransfer}
    canTransfer={canTransfer} initial={guide.draft.tarifa} save={value=>guide.saveDraft({tarifa:JSON.stringify(value)})}
    resumeFirst={()=>guide.goTo("tarifas",0)}/>;
}

function PriceExercise({catalog, initial, save, canTransfer, onTransfer, resumeFirst}:{
  catalog:CatalogState;initial?:string;save:(value:Practice)=>Promise<void>;canTransfer:boolean;
  onTransfer:(id:string,price:string)=>void;resumeFirst:()=>Promise<void>;
}){
  const version=catalog.actual?.codigoPublico??"";
  const [form,setForm]=useState(()=>readPractice(initial,version)), [failure,setFailure]=useState("");
  const rates=seedRates(catalog.actual).filter(rate=>rate.habilitada);
  const services=catalog.servicios.filter(s=>s.activo && s.tipo==="IMPRESION" && catalog.actual?.servicios.some(o=>o.servicio===s.codigoPublico && o.habilitado));
  const variants=rates.flatMap(rate=>rate.papeles.map(pair=>{
    const paper=catalog.papeles.find(p=>p.codigoPublico===pair.papel), format=catalog.formatos.find(f=>f.codigoPublico===pair.formato);
    return {id:`${rate.id}/${pair.formato}/${pair.papel}`, rate, label:`${format?.nombre??"Formato"} · ${paper?.gramaje??"?"} g · ${paper?.terminacion??"Papel"} · ${rate.color && colorLabels[rate.color]}`};
  }));
  const selected=variants.find(variant=>variant.id===form.variante);
  const current=version===form.version, serviceChosen=services.some(s=>s.codigoPublico===form.servicio);
  const valid=!!selected && current && serviceChosen && validAmount(form.precio);
  const edited=valid && parseAmount(form.precio,"Precio")!==parseAmount(selected.rate.precio,"Precio vigente");
  const applied=edited && form.aplicada;
  function persist(next:Practice){setForm(next);setFailure("");void save(next).catch(()=>setFailure("No se pudo guardar el ejercicio. Reintentá antes de salir."));}
  function change(fields:Partial<Practice>){persist({...form,...fields,aplicada:false});}
  async function useCurrentVersion(){
    const next=readPractice(undefined,version);
    try {await save(next);setForm(next);setFailure("");await resumeFirst();}
    catch {setFailure("No se pudo abrir la práctica con los precios vigentes.");}
  }
  const total=(rate:RateDraft)=>ars(3*parseAmount(rate.precio,"Precio vigente"));
  return <section className="admin-card" id="tutorial-tarifa" aria-labelledby="tutorial-tarifa-titulo"
    data-servicio={current&&serviceChosen||undefined} data-variante={current&&!!selected||undefined} data-precio={edited||undefined} data-aplicada={applied||undefined}>
    <h2 id="tutorial-tarifa-titulo">Práctica de tarifa por hoja</h2>
    <p>Elegí una tarifa vigente para calcular un presupuesto de tres páginas a simple faz. El ejercicio conserva sus propios valores cuando pausás el tutorial.</p>
    {!current && <p className="admin-warning" role="status">El ejercicio corresponde a otra versión de precios. Su precio de práctica era {form.precio||"sin completar"}. Para trabajar con los precios vigentes, <button id="tutorial-tarifa-restaurar" type="button" className="admin-button secondary" onClick={()=>void useCurrentVersion()}>empezá desde la tarifa actual</button>.</p>}
    {(!services.length||!variants.length)&&<p className="admin-warning">Para practicar se necesita un servicio de impresión ofrecido y al menos una tarifa vigente. Revisá el catálogo base y sus tarifas.</p>}
    <fieldset disabled={!current} className="admin-fieldset admin-form"><legend>Datos de la práctica</legend>
      <label>Servicio de impresión del ejercicio<select id="tutorial-tarifa-servicio" value={form.servicio} onChange={e=>change({servicio:e.target.value,variante:"",precio:""})}>
        <option value="">Elegí un servicio</option>{services.map(s=><option key={s.codigoPublico} value={s.codigoPublico}>{s.nombre}</option>)}
      </select></label>
      <label>Papel y modo de impresión<select id="tutorial-tarifa-papel" value={form.variante} disabled={!serviceChosen} onChange={e=>change({variante:e.target.value,precio:variants.find(v=>v.id===e.target.value)?.rate.precio??""})}>
        <option value="">Elegí la combinación de papel y color</option>{variants.map(v=><option key={v.id} value={v.id}>{v.label}</option>)}
      </select></label>
      {selected&&<><p>La tarifa vigente «{selected.rate.nombre}» cobra <strong>{ars(parseAmount(selected.rate.precio,"Vigente"))}</strong> a simple faz.</p>
        <div id="tutorial-tarifa-precio"><CatalogAmount label="Precio simple faz del ejercicio" value={form.precio} onChange={precio=>change({precio})} help={<p>Probá con otro importe por hoja y compará el total del ejemplo.</p>}/></div>
        <button id="tutorial-tarifa-aplicar" type="button" className="admin-button" disabled={!edited} onClick={()=>persist({...form,aplicada:true})}>Aplicar tarifa al borrador de práctica</button>
      </>}
    </fieldset>
    {applied&&selected&&<div id="tutorial-tarifa-resultado" className="admin-info"><strong>Presupuesto de prueba · tres hojas simple faz</strong>
      <p>Con la tarifa vigente: {total(selected.rate)}. Con tu borrador: 3 × {ars(parseAmount(form.precio,"Nuevo precio"))} = <strong>{ars(3*parseAmount(form.precio,"Nuevo precio"))}</strong>.</p>
      <p>Podés seguir con el tutorial. Si querés publicar, llevá este precio al formulario comercial y revisá las {selected.rate.papeles.length} variantes de «{selected.rate.nombre}». Al publicar cambia el precio de esa tarifa para futuras cotizaciones.</p>
      <button type="button" className="admin-button secondary" disabled={!canTransfer} onClick={()=>onTransfer(selected.rate.id,form.precio)}>Preparar publicación opcional</button>
      {!canTransfer&&<p>Revisá los cambios pendientes o la configuración programada en el formulario comercial antes de preparar otra publicación.</p>}
    </div>}
    {failure&&<p role="alert" className="admin-warning">{failure} <button type="button" onClick={()=>persist(form)}>Reintentar</button></p>}
  </section>;
}
