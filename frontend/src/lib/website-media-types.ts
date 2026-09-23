//#region ENCABEZADO · src/lib/website-media-types.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/website-media-types.ts
 * ========================================================================
 * FUNCIÓN
 * Define y valida imágenes de biblioteca, archivos candidatos y respuestas de exploración de la
 * carpeta de origen.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - record(v: unknown): v is Record<string,unknown>
 * - [export] isWebImage(v: unknown): v is WebImage
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isImageSource(v: unknown): v is ImageSource
 *   Validador de datos recibidos en tiempo de ejecución.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - WebImage (tipo).
 * - SourceFile (tipo).
 * - ImageSource (tipo).
 * ========================================================================
 */
//#endregion

export type WebImage={codigo:string;nombre:string;sha256:string;tipoOrigen:string;bytesOrigen:number;ancho:number;alto:number;creadaEn:string};
export type SourceFile={ruta:string;nombre:string;bytes:number;sha256:string|null;estado:"CANDIDATA"|"INVALIDA";mensaje:string};
export type ImageSource={rutaHost:string;carpeta:string;legible:boolean;mensaje:string;subcarpetas:string[];archivos:SourceFile[];limitada:boolean};
const record=(v:unknown):v is Record<string,unknown>=>!!v&&typeof v==="object"&&!Array.isArray(v);
export function isWebImage(v:unknown):v is WebImage{return record(v)&&["codigo","nombre","sha256","tipoOrigen","creadaEn"].every(k=>typeof v[k]==="string")&&["bytesOrigen","ancho","alto"].every(k=>typeof v[k]==="number"&&Number.isSafeInteger(v[k])&&Number(v[k])>0);}
export function isImageSource(v:unknown):v is ImageSource{return record(v)&&typeof v.rutaHost==="string"&&typeof v.carpeta==="string"&&typeof v.legible==="boolean"&&typeof v.mensaje==="string"&&typeof v.limitada==="boolean"&&Array.isArray(v.subcarpetas)&&v.subcarpetas.every(s=>typeof s==="string")&&Array.isArray(v.archivos)&&v.archivos.every(f=>record(f)&&typeof f.ruta==="string"&&typeof f.nombre==="string"&&typeof f.bytes==="number"&&(f.sha256===null||typeof f.sha256==="string")&&["CANDIDATA","INVALIDA"].includes(String(f.estado))&&typeof f.mensaje==="string");}
