//#region ENCABEZADO · src/components/catalog-pricing-fields.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-pricing-fields.tsx
 * ========================================================================
 * FUNCIÓN
 * Proporciona ayudas accesibles y campos de importes del catálogo, con moneda ARS explícita y
 * validación visual de los formatos permitidos.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] CatalogInfo({title,htmlFor,children}:
 *   {title:string;htmlFor?:string;children:ReactNode})
 *   Componente de interfaz.
 * - CatalogInfo :: outside(e: PointerEvent)
 * - CatalogInfo :: key(e: KeyboardEvent)
 * - [export] CatalogAmount({label,value,onChange,help}:
 *   {label:string;value:string;onChange:(value:string)=>void;help:ReactNode})
 *   Componente de interfaz.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useId,useRef,useState,type ReactNode} from "react";
import {amountPattern,validAmount} from "@/lib/catalog-pricing";

export function CatalogInfo({title,htmlFor,children}:{title:string;htmlFor?:string;children:ReactNode}){
  const id=useId(),root=useRef<HTMLDivElement>(null),[open,setOpen]=useState(false);
  useEffect(()=>{if(!open)return;function outside(e:PointerEvent){if(e.target instanceof Node&&!root.current?.contains(e.target))setOpen(false);}function key(e:KeyboardEvent){if(e.key==="Escape")setOpen(false);}document.addEventListener("pointerdown",outside);document.addEventListener("keydown",key);return()=>{document.removeEventListener("pointerdown",outside);document.removeEventListener("keydown",key);};},[open]);
  return <div ref={root} className="pricing-info" onPointerLeave={e=>{if(e.pointerType==="mouse"&&!root.current?.contains(document.activeElement))setOpen(false);}} onBlur={e=>{if(!e.currentTarget.contains(e.relatedTarget))setOpen(false);}}>
    {htmlFor?<label htmlFor={htmlFor}>{title}</label>:<strong>{title}</strong>}
    <button type="button" aria-label={`Información: ${title}`} aria-expanded={open} aria-describedby={id} className="pricing-info-button" onPointerEnter={e=>{if(e.pointerType==="mouse")setOpen(true);}} onFocus={e=>{if(e.currentTarget.matches(":focus-visible"))setOpen(true);}} onClick={()=>setOpen(v=>!v)}>
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 10.5V17"/><circle cx="12" cy="7.2" r=".8" fill="currentColor" stroke="none"/></svg>
    </button>
    <div id={id} role="tooltip" className="pricing-info-panel" hidden={!open}>{children}</div>
  </div>;
}
export function CatalogAmount({label,value,onChange,help}:{label:string;value:string;onChange:(value:string)=>void;help:ReactNode}){
  const id=useId(),[touched,setTouched]=useState(false),invalid=touched&&!validAmount(value);
  return <div className="pricing-field"><CatalogInfo title={label} htmlFor={id}>{help}<p>Importe fijo en pesos argentinos (ARS), sin símbolo $ ni %. Escribí 1500,50 o 1500.50; no uses separadores de miles. Hasta 2 decimales, desde 0.</p></CatalogInfo>
    <div className="pricing-money"><span aria-hidden="true">ARS</span><input id={id} type="text" inputMode="decimal" spellCheck={false} required pattern={amountPattern} maxLength={15} title="Importe en ARS, sin miles ni símbolos; hasta 2 decimales. Ej.: 1500,50" value={value} placeholder="0,00" aria-invalid={invalid||undefined} aria-describedby={`${id}-hint`} onChange={e=>onChange(e.target.value)} onBlur={()=>setTouched(true)}/></div>
    <small id={`${id}-hint`} className={invalid?"error-message":""}>{invalid?"Usá un número desde 0 con hasta 2 decimales, sin miles, $ ni %. Ej.: 1500,50.":"Importe fijo · ej.: 1500,50 · sin separadores de miles"}</small>
  </div>;
}
