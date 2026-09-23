import type {CatalogState,Compatibility} from "@/lib/catalog-types";
export function paperKey(pair:Compatibility){return `${pair.formato}/${pair.papel}`;}
export function paperLabel(catalog:CatalogState,pair:Compatibility){
  const f=catalog.formatos.find(x=>x.codigoPublico===pair.formato),p=catalog.papeles.find(x=>x.codigoPublico===pair.papel);
  return `${f?.nombre??"Tamaño anterior"} · ${p?.nombre??"Papel anterior"} · ${p?.gramaje??"—"} g/m² · ${f?.anchoMm??"—"} × ${f?.altoMm??"—"} mm · ${p?.terminacion??"—"}`;
}
export function CatalogPaperSelect({catalog,value,label,onChange}:{catalog:CatalogState;value:Compatibility;label:string;onChange:(pair:Compatibility)=>void}){
  const enabled=catalog.papelesHabilitados.filter(p=>p.habilitado),key=value.formato&&value.papel?paperKey(value):"";
  const removed=key&&!enabled.some(p=>paperKey(p)===key);
  return <><select aria-label={label} required value={key} onChange={e=>{const [formato,papel]=e.target.value.split("/");onChange({formato,papel});}}>
    <option value="" disabled>Seleccionar papel con tamaño</option>
    {removed&&<option value={key}>{paperLabel(catalog,value)} (retirado)</option>}
    {enabled.map(p=><option key={paperKey(p)} value={paperKey(p)}>{paperLabel(catalog,p)}</option>)}
  </select>{removed&&<small className="error-message">Papel retirado del catálogo base: volvé a habilitarlo o quitá su tarifa/compatibilidad de esta revisión.</small>}</>;
}
