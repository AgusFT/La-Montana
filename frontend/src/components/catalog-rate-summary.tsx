//#region ENCABEZADO · src/components/catalog-rate-summary.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-rate-summary.tsx
 * FUNCIÓN
 * Presenta una tarifa cerrada con sus papeles, precios y estado real: vigente,
 * programada o pendiente de publicación. Permite abrir su edición explícita.
 * ------------------------------------------------------------------------
 * COMPONENTES Y FUNCIONES DECLARADOS
 * - CatalogRateSummary({rate,index,catalog,state,disabled,onEdit,onRemove})
 * TIPOS DECLARADOS: no declara tipos propios.
 * ========================================================================
 */
//#endregion
import {ars,parseAmount} from "@/lib/catalog-pricing";
import {duplexPrice,type RateDraft} from "@/lib/catalog-rate-draft";
import {colorLabels,type CatalogState} from "@/lib/catalog-types";
export function CatalogRateSummary({rate,index,catalog,state,disabled,onEdit,onRemove}:{rate:RateDraft;index:number;catalog:CatalogState;state:"VIGENTE"|"PROGRAMADA"|"PENDIENTE";disabled:boolean;onEdit:()=>void;onRemove:()=>void}){
  const simple=parseAmount(rate.precio,"Simple faz"),legacy=rate.recargoAnterior!==undefined,duplex=legacy?simple+(rate.recargoAnterior??0):duplexPrice(rate.precio,rate.modoDobleFaz,rate.valorDobleFaz);
  const groups=new Map<string,{name:string;count:number}>();
  for(const paper of rate.papeles){const group=groups.get(paper.formato);if(group)group.count++;else groups.set(paper.formato,{name:catalog.formatos.find(format=>format.codigoPublico===paper.formato)?.nombre??"Tamaño anterior",count:1});}
  const status=state==="PENDIENTE"?"Pendiente de guardar":state==="PROGRAMADA"?"Guardada · programada":"Guardada · vigente";
  return <article id={rate.id} className={`pricing-entry rate-summary ${state==="PENDIENTE"?"is-pending":state==="PROGRAMADA"?"is-scheduled":""}`} aria-label={`Resumen de tarifa ${index+1}`}>
    <header><div className="rate-name"><h3>{rate.nombre}</h3></div><span className={`pricing-state ${state==="PENDIENTE"?"is-pending":state==="PROGRAMADA"?"is-scheduled":"enabled"}`}>{status}</span></header>
    <p className="admin-note"><strong>{rate.color?colorLabels[rate.color]:"Sin modo de impresión"}</strong> · {rate.papeles.length} variantes · {rate.habilitada?"Habilitada":"Deshabilitada para nuevas cotizaciones"}</p>
    <ul className="rate-summary-papers" aria-label="Hojas incluidas">{[...groups].map(([id,group])=><li key={id}>{group.name} · {group.count} {group.count===1?"variante":"variantes"}</li>)}</ul>
    <div className="rate-final-prices"><div><span>Simple faz · {legacy?"por cara":"por hoja"}</span><strong>{ars(simple)}</strong></div><div><span>Doble faz · {legacy?"por cara":"por hoja con dos caras"}</span><strong>{duplex===null?"—":ars(duplex)}</strong></div></div>
    <p className="admin-note">{legacy?"Conserva el cálculo anterior por cara impresa.":rate.modoDobleFaz==="FIJO"?"Doble faz tiene un precio final independiente.":rate.modoDobleFaz==="ADICIONAL"?`Doble faz: simple faz + ${ars(parseAmount(rate.valorDobleFaz,"Adicional"))}.`:`Doble faz: simple faz + ${rate.valorDobleFaz} %.`} {state==="PENDIENTE"?"La tarifa está preparada. Guardá la configuración completa para publicar estos cambios.":state==="PROGRAMADA"?"Se aplicará en la fecha programada.":"Esta tarifa ya forma parte de la configuración vigente."}</p>
    <div className="rate-summary-actions"><button type="button" className="admin-button secondary" disabled={disabled} onClick={onEdit} aria-label={`Editar tarifa ${index+1}`}><span aria-hidden="true">✎</span> Editar tarifa</button><button type="button" className="admin-link-button" disabled={disabled} onClick={onRemove}>Quitar tarifa {index+1}</button></div>
  </article>;
}
