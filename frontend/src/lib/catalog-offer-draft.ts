//#region ENCABEZADO · src/lib/catalog-offer-draft.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/catalog-offer-draft.ts
 * FUNCIÓN
 * Prepara y compara servicios del borrador comercial y valida sus campos antes
 * de confirmarlos individualmente o publicar la configuración completa.
 * ------------------------------------------------------------------------
 * FUNCIONES DECLARADAS
 * - seedOffers(revision: CatalogRevision|null): OfferDraft[]
 * - offerSignature(offer: OfferDraft): string
 * - validateOffer(offer: OfferDraft, index: number, offers: OfferDraft[],
 *   catalog: CatalogState): ServiceOffer
 * TIPOS DECLARADOS: PairDraft, OfferDraft.
 * ========================================================================
 */
//#endregion
import {amountDraft,parseAmount} from "@/lib/catalog-pricing";
import {priceLabels,type CatalogRevision,type CatalogState,type PriceBase,type ServiceOffer} from "@/lib/catalog-types";

export type PairDraft={id:string;formato:string;papel:string};
export type OfferDraft={id:string;servicio:string;nombreVisible:string;basePrecio:PriceBase|"";precio:string;preparacionMinutos:string;habilitado:boolean;compatibilidades:PairDraft[]};

export function seedOffers(revision:CatalogRevision|null):OfferDraft[]{return revision?.servicios.map((offer,i)=>({...offer,id:`offer-${i}`,precio:amountDraft(offer.precio),preparacionMinutos:String(offer.preparacionMinutos),compatibilidades:offer.compatibilidades.map((pair,j)=>({...pair,id:`pair-${i}-${j}`}))}))??[];}

export function offerSignature(offer:OfferDraft):string{
  const {id,compatibilidades,...fields}=offer;
  return JSON.stringify({...fields,compatibilidades:compatibilidades.map(pair=>`${pair.formato}/${pair.papel}`).sort()});
}

export function validateOffer(offer:OfferDraft,index:number,offers:OfferDraft[],catalog:CatalogState):ServiceOffer{
  const label=`Servicio ${index+1}`,service=catalog.servicios.find(s=>s.codigoPublico===offer.servicio);
  if(!service||!offer.basePrecio||!Object.hasOwn(priceLabels,offer.basePrecio)||!offer.nombreVisible.trim())throw new Error(`${label}: elegí un servicio de la lista, su nombre visible y la forma de cobro.`);
  if(offer.nombreVisible.trim().length>140)throw new Error(`${label}: el nombre admite hasta 140 caracteres.`);
  if(offers.some(other=>other.id!==offer.id&&other.servicio===offer.servicio))throw new Error(`${label}: ese servicio ya está agregado. Editá el existente o elegí otro del Catálogo base.`);
  const precio=parseAmount(offer.precio,`${label}, precio unitario`),preparacionMinutos=Number(offer.preparacionMinutos);
  if(!offer.preparacionMinutos.trim()||!Number.isInteger(preparacionMinutos)||preparacionMinutos<0||preparacionMinutos>10080)throw new Error(`${label}: la preparación debe ser un número entero entre 0 y 10080 minutos.`);
  if(offer.compatibilidades.length>300)throw new Error(`${label}: se admiten hasta 300 compatibilidades.`);
  if(offer.compatibilidades.some(pair=>!pair.formato||!pair.papel))throw new Error(`${label}: elegí un papel de la lista para cada compatibilidad.`);
  const pairs=offer.compatibilidades.map(pair=>`${pair.formato}/${pair.papel}`);
  if(new Set(pairs).size!==pairs.length)throw new Error(`${label}: hay papeles compatibles repetidos.`);
  if(service.tipo!=="IMPRESION"&&offer.habilitado&&!pairs.length)throw new Error(`${label}: agregá al menos un papel compatible para habilitar esta terminación.`);
  return {servicio:offer.servicio,nombreVisible:offer.nombreVisible.trim(),basePrecio:offer.basePrecio,precio,preparacionMinutos,habilitado:offer.habilitado,compatibilidades:offer.compatibilidades.map(({formato,papel})=>({formato,papel}))};
}
