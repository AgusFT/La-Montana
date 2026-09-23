//#region ENCABEZADO · src/lib/catalog-rate-draft.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/catalog-rate-draft.ts
 * ========================================================================
 * FUNCIÓN
 * Agrupa y expande las variantes de tarifas conservando nombres y reglas; calcula el precio doble
 * faz en centavos enteros y mantiene compatibles los registros históricos.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * - [export] rateName(color: ColorMode|"", index: number)
 * - cents(value: string)
 * - [export] duplexPrice(simple: string, mode: DuplexMode, value: string)
 * - [export] seedRates(revision: CatalogRevision|null): RateDraft[]
 * - [export] expandRates(rates: RateDraft[]): PrintRate[]
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - RateDraft (TypeAliasDeclaration).
 * ========================================================================
 */
//#endregion

import type {CatalogRevision,ColorMode,Compatibility,DuplexMode,PrintRate} from "@/lib/catalog-types";
import {amountDraft,parseAmount,validAmount} from "@/lib/catalog-pricing";

export type RateDraft={id:string;grupo?:string;nombre:string;nombrePersonalizado:boolean;color:ColorMode|"";papeles:Compatibility[];precio:string;modoDobleFaz:DuplexMode;valorDobleFaz:string;recargoAnterior?:number;habilitada:boolean};
export function rateName(color:ColorMode|"",index:number){return `Tarifa ${index+1}${color?` · ${color==="COLOR"?"Color":"Blanco y negro"}`:""}`;}
function cents(value:string){const [whole,decimal=""]=value.trim().replace(",",".").split(".");return BigInt(whole)*BigInt(100)+BigInt(decimal.padEnd(2,"0"));}
export function duplexPrice(simple:string,mode:DuplexMode,value:string){
  if(!validAmount(value)||(mode!=="FIJO"&&!validAmount(simple)))return null;
  const amount=cents(value),base=mode==="FIJO"?BigInt(0):cents(simple);
  const total=mode==="FIJO"?amount:mode==="ADICIONAL"?base+amount:base+(base*amount+BigInt(5000))/BigInt(10000);
  return total<=BigInt(99999999999999)?Number(total)/100:null;
}
export function seedRates(revision:CatalogRevision|null):RateDraft[]{
  const grouped=new Map<string,RateDraft>();
  for(const rate of revision?.tarifas??[]){
    const key=rate.grupo??JSON.stringify([rate.color,rate.precio,rate.recargoDobleFaz,rate.habilitada]);
    let group=grouped.get(key);
    if(!group){group={id:`rate-${grouped.size}`,grupo:rate.grupo,nombre:rate.nombre??rateName(rate.color,grouped.size),nombrePersonalizado:!!rate.nombre,color:rate.color,papeles:[],precio:amountDraft(rate.precio),modoDobleFaz:rate.modoDobleFaz??"FIJO",valorDobleFaz:amountDraft(rate.valorDobleFaz??2*(rate.precio+rate.recargoDobleFaz)),...(rate.modoDobleFaz?{}:{recargoAnterior:rate.recargoDobleFaz}),habilitada:rate.habilitada};grouped.set(key,group);}
    group.papeles.push({formato:rate.formato,papel:rate.papel});
  }
  return [...grouped.values()];
}
export function expandRates(rates:RateDraft[]):PrintRate[]{
  const expanded:PrintRate[]=[],used=new Set<string>();
  for(const [index,rate] of rates.entries()){
    const name=rate.nombre.trim();
    if(!name||name.length>140)throw new Error(`Tarifa ${index+1}: ingresá un nombre de hasta 140 caracteres.`);
    if(!rate.color||!rate.papeles.length)throw new Error(`${name}: elegí un modo de color y al menos una variante de papel.`);
    const price=parseAmount(rate.precio,`${name} · Simple faz`),legacy=rate.recargoAnterior!==undefined,group=rate.grupo??crypto.randomUUID();
    if(!legacy&&duplexPrice(rate.precio,rate.modoDobleFaz,rate.valorDobleFaz)===null)throw new Error(`${name}: completá el valor doble faz con hasta 2 decimales. Su precio final no puede superar ARS 999999999999,99.`);
    for(const pair of rate.papeles){
      const key=`${pair.formato}/${pair.papel}/${rate.color}`;
      if(used.has(key))throw new Error(`${name}: una de sus variantes ya está incluida en otra tarifa para el mismo color. Quitala de una de las dos tarifas.`);
      used.add(key);
      expanded.push({...pair,color:rate.color,precio:price,recargoDobleFaz:rate.recargoAnterior??0,habilitada:rate.habilitada,grupo:group,nombre:name,...(legacy?{}:{modoDobleFaz:rate.modoDobleFaz,valorDobleFaz:Number(rate.valorDobleFaz.trim().replace(",","."))})});
    }
  }
  if(expanded.length>300)throw new Error("La configuración admite hasta 300 combinaciones de papel y color. Reducí la selección.");
  return expanded;
}
