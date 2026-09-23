import type {PriceBase} from "@/lib/catalog-types";
export const amountPattern="[0-9]{1,12}([.,][0-9]{1,2})?";
export function validAmount(value:string){return new RegExp(`^${amountPattern}$`).test(value.trim());}
export function parseAmount(value:string,label:string){if(!validAmount(value))throw new Error(`${label}: ingresá un importe en ARS, sin separadores de miles ni símbolos y con hasta 2 decimales. Ejemplo: 1500,50.`);return Number(value.trim().replace(",","."));}
export function amountDraft(value:number){return String(value).replace(".",",");}
export const ars=(value:number)=>`ARS ${new Intl.NumberFormat("es-AR",{minimumFractionDigits:2,maximumFractionDigits:2}).format(value)}`;
export const priceExplanations:Record<PriceBase,string>={
  POR_COPIA:"Por cada ejemplar completo del documento. Ejemplo: 3 copias generan 3 cargos, sin importar cuántas páginas tenga cada una.",
  POR_HOJA:"Por cada hoja física utilizada. En doble faz, una hoja puede llevar dos carillas impresas.",
  POR_CARILLA:"Por cada cara impresa: páginas del documento × cantidad de copias. También se cuentan ambas caras en doble faz.",
  FIJO_POR_ITEM:"Un solo cargo por ítem (documento) de la cotización, sin multiplicarlo por páginas ni copias. No es un cargo único por todo el pedido.",
};
