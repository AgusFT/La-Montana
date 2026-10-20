"use client";
//#region ENCABEZADO · src/lib/prepared-files.ts
/* FUNCIÓN: Conserva sólo en memoria los PDF del último resumen abierto en esta pestaña.
 * FUNCIONES: rememberPreparedFiles, preparedFile, clearPreparedFiles. VALOR: handoff.
 * Se pierde al recargar y se borra al cerrar sesión. Nunca acredita, carga ni confirma un archivo.
 */
//#endregion
import type {Quote} from "./quote-types";
let handoff:{quote:string;files:Map<string,{sha256:string;file:File}>}|null=null;
export function rememberPreparedFiles(quote:Quote,files:{sha256:string;file?:File}[]){
  handoff={quote:quote.codigoPublico,files:new Map()};
  for(const item of quote.oferta.items){const original=files.find(f=>f.sha256===item.documento.sha256);if(original?.file)handoff.files.set(item.codigoPublico,{sha256:item.documento.sha256,file:original.file});}
}
export function preparedFile(quote:string,item:string,sha256?:string){const entry=handoff?.quote===quote?handoff.files.get(item):undefined;return entry&&(!sha256||entry.sha256===sha256)?entry.file:undefined;}
export function clearPreparedFiles(){handoff=null;}
