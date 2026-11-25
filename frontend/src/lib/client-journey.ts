//#region ENCABEZADO · src/lib/client-journey.ts
/* FUNCIÓN: Decide el siguiente subpaso del resumen a partir de hechos guardados y acceso del servidor.
 * FUNCIONES: quoteStage, positiveAmount. TIPOS: QuoteStage. No acredita pagos ni acepta archivos.
 */
//#endregion
import type {Quote} from "./quote-types";
import type {FileView} from "./file-types";
export type QuoteStage="summary"|"payment"|"files"|"confirm"|"blocked"|"confirmed";
export function positiveAmount(value:string){return /^\d+(\.\d+)?$/.test(value)&&BigInt(value.replace(".",""))>BigInt(0);}
export function quoteStage(quote:Quote,files:FileView|null,now:number):QuoteStage{
  if(quote.estado==="CONFIRMADA")return "confirmed";
  if(quote.estado!=="VIGENTE"||quote.reemplazadaPor||!Number.isFinite(Date.parse(quote.vigenteHasta))||now>=Date.parse(quote.vigenteHasta)||quote.correccion&&!quote.correccion.pendiente)return "blocked";
  if(!quote.aceptadaEn)return "summary";
  if(!files||files.cotizacion!==quote.codigoPublico)return "blocked";
  if(!files.carga.habilitada)return "blocked";
  const ready=quote.oferta.items.length>0&&quote.oferta.items.every(item=>files.items.some(i=>i.codigoPublico===item.codigoPublico&&i.archivos.some(f=>f.activo&&f.estado==="VALIDO"&&!!f.aceptadaEn)));
  return ready?"confirm":"files";
}
