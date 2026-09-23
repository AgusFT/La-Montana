//#region ENCABEZADO · src/components/catalog-offer-summary.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-offer-summary.tsx
 * FUNCIÓN
 * Muestra un servicio confirmado como tarjeta cerrada, con precio, preparación,
 * papeles y estado de publicación. Permite editarlo o quitarlo del borrador.
 * ------------------------------------------------------------------------
 * COMPONENTES Y FUNCIONES DECLARADOS
 * - CatalogOfferSummary({offer,index,catalog,state,disabled,onEdit,onRemove})
 * TIPOS DECLARADOS: no declara tipos propios.
 * ========================================================================
 */
//#endregion
import {ars,parseAmount} from "@/lib/catalog-pricing";
import {priceLabels,type CatalogState} from "@/lib/catalog-types";
import type {OfferDraft} from "@/lib/catalog-offer-draft";

export function CatalogOfferSummary({offer,index,catalog,state,disabled,onEdit,onRemove}:{offer:OfferDraft;index:number;catalog:CatalogState;state:"VIGENTE"|"PROGRAMADA"|"PENDIENTE";disabled:boolean;onEdit:()=>void;onRemove:()=>void}){
  const service=catalog.servicios.find(s=>s.codigoPublico===offer.servicio),printing=service?.tipo==="IMPRESION";
  const groups=new Map<string,{name:string;count:number}>();
  for(const pair of offer.compatibilidades){const group=groups.get(pair.formato);if(group)group.count++;else groups.set(pair.formato,{name:catalog.formatos.find(f=>f.codigoPublico===pair.formato)?.nombre??"Tamaño anterior",count:1});}
  const status=state==="PENDIENTE"?"Pendiente de guardar":state==="PROGRAMADA"?"Guardado · programado":"Guardado · vigente";
  return <article id={offer.id} className={`pricing-entry pricing-offer rate-summary ${state==="PENDIENTE"?"is-pending":state==="PROGRAMADA"?"is-scheduled":""}`} aria-label={`Resumen de servicio ${index+1}`}>
    <header><div className="rate-name"><h3>{offer.nombreVisible}</h3></div><span className={`pricing-state ${state==="PENDIENTE"?"is-pending":state==="PROGRAMADA"?"is-scheduled":"enabled"}`}>{status}</span></header>
    <p className="admin-note"><strong>{printing?"Impresión":"Terminación"}</strong> · {service?.nombre} · {offer.habilitado?"Habilitado":"Deshabilitado para nuevas cotizaciones"}</p>
    <div className="rate-final-prices"><div><span>{printing?"Precio de impresión":"Precio unitario · "+(offer.basePrecio?priceLabels[offer.basePrecio]:"")}</span><strong>{printing?"Según tarifas":ars(parseAmount(offer.precio,"Precio del servicio"))}</strong></div><div><span>Preparación mínima</span><strong>{offer.preparacionMinutos} min</strong></div></div>
    {printing?<p className="admin-note">Usa los precios y papeles de las tarifas habilitadas, sin sumar otro cargo.</p>:<><p className="admin-note">{offer.compatibilidades.length} variantes de papel compatibles.</p><ul className="rate-summary-papers" aria-label="Papeles compatibles">{[...groups].map(([id,group])=><li key={id}>{group.name} · {group.count} {group.count===1?"variante":"variantes"}</li>)}</ul></>}
    <p className="admin-note">{state==="PENDIENTE"?"Servicio preparado en el borrador. Podés agregar otro; al terminar, guardá la configuración completa para publicar todos los cambios.":state==="PROGRAMADA"?"Se aplicará en la fecha programada.":"Este servicio ya forma parte de la configuración vigente."}</p>
    <div className="rate-summary-actions"><button type="button" className="admin-button secondary" disabled={disabled} onClick={onEdit} aria-label={`Editar servicio ${index+1}`}><span aria-hidden="true">✎</span> Editar servicio</button><button type="button" className="admin-link-button" disabled={disabled} onClick={onRemove}>Quitar servicio {index+1}</button></div>
  </article>;
}
