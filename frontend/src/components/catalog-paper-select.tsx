//#region ENCABEZADO · src/components/catalog-paper-select.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-paper-select.tsx
 * ========================================================================
 * FUNCIÓN
 * Construye opciones de papel completo con tamaño, gramaje y material y las presenta en un
 * selector con búsqueda que conserva referencias retiradas.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] paperKey(pair: Compatibility)
 * - [export] paperLabel(catalog: CatalogState, pair: Compatibility)
 * - [export] CatalogPaperSelect({catalog,value,label,onChange,disabled=false}:
 *   {catalog:CatalogState;value:Compatibility;label:string;onChange:(pair:Compatibility)=>void;disabled?:boolean})
 *   Componente de interfaz.
 * - CatalogPaperSelect :: choice(pair: Compatibility): CatalogChoice
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import {CatalogSearchSelect,type CatalogChoice} from "@/components/catalog-search-select";
import type {CatalogState,Compatibility} from "@/lib/catalog-types";
export function paperKey(pair:Compatibility){return `${pair.formato}/${pair.papel}`;}
export function paperLabel(catalog:CatalogState,pair:Compatibility){
  const f=catalog.formatos.find(x=>x.codigoPublico===pair.formato),p=catalog.papeles.find(x=>x.codigoPublico===pair.papel);
  return `${f?.nombre??"Tamaño anterior"} · ${p?.nombre??"Papel anterior"} · ${p?.gramaje??"—"} g/m² · ${f?.anchoMm??"—"} × ${f?.altoMm??"—"} mm · ${p?.terminacion??"—"}`;
}
export function CatalogPaperSelect({catalog,value,label,onChange,disabled=false}:{catalog:CatalogState;value:Compatibility;label:string;onChange:(pair:Compatibility)=>void;disabled?:boolean}){
  const enabled=catalog.papelesHabilitados.filter(p=>p.habilitado),key=value.formato&&value.papel?paperKey(value):"";
  const removed=!!key&&!enabled.some(p=>paperKey(p)===key);
  function choice(pair:Compatibility):CatalogChoice{
    const f=catalog.formatos.find(x=>x.codigoPublico===pair.formato),p=catalog.papeles.find(x=>x.codigoPublico===pair.papel);
    return {value:paperKey(pair),label:`${f?.nombre??"Tamaño anterior"} · ${p?.gramaje??"—"} g/m² · ${p?.nombre??"Papel anterior"}`,
      detail:`${f?.anchoMm??"—"} × ${f?.altoMm??"—"} mm · ${p?.terminacion??"—"} · ${p?.codigo??""}${removed&&paperKey(pair)===key?" · Retirado":""}`};
  }
  return <div className="catalog-paper-picker"><CatalogSearchSelect label={label} options={[...(removed?[choice(value)]:[]),...enabled.map(choice)]} value={key} disabled={disabled}
    placeholder="Ej.: A4 80, ilustración o código" help="Escribí al menos 2 caracteres. Podés combinar tamaño y gramaje: A4 80. Elegí el resultado."
    onChange={next=>{const [formato,papel]=next.split("/");onChange({formato,papel});}}/>
    {removed&&<small className="error-message">Papel retirado: habilitalo en Catálogo base o deshabilitá la tarifa. Si es una compatibilidad de terminación, quitá esa compatibilidad.</small>}
  </div>;
}
