//#region ENCABEZADO · src/lib/quote-types.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/quote-types.ts
 * ========================================================================
 * FUNCIÓN
 * Define y valida opciones, ítems, documentos y cotizaciones, con utilidades de consulta y
 * presentación de importes y fechas.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - record(v: unknown): v is Record<string,unknown>
 * - [export] isQuote(v: unknown): v is Quote
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isOptions(v: unknown): v is QuoteOptions
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isQuoteList(v: unknown): v is QuoteList
 *   Validador de datos recibidos en tiempo de ejecución.
 * - money(value: string)
 * - date(value: string|null)
 * - [export, async] quoteRead<T>(path: string, guard: (v:unknown)=>v is T): Promise<T>
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - WorkItem (tipo).
 * - DocumentInfo (tipo).
 * - Mode (tipo).
 * - Address (tipo).
 * - QuoteOptions (tipo).
 * - Named (tipo).
 * - QuoteInput (tipo).
 * - Quote (tipo).
 * - QuoteList (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - modeLabels [const, exportado].
 * ========================================================================
 */
//#endregion


export type WorkItem={servicio:string;formato:string;papel:string;color:"BLANCO_NEGRO"|"COLOR";paginas:number;copias:number;dobleFaz:boolean;terminaciones:string[]};
export type DocumentInfo={nombre:string;bytes:number;sha256:string};
export type Mode="RETIRO_SUCURSAL"|"RETIRO_PUNTO_ENTREGA"|"ENVIO_DOMICILIO";
export const modeLabels:Record<Mode,string>={RETIRO_SUCURSAL:"Retiro en sucursal",RETIRO_PUNTO_ENTREGA:"Retiro en punto de entrega",ENVIO_DOMICILIO:"Envío a domicilio"};
export type Address={calle:string;numero:string;referencias:string;territorio:{codigoPostal:string;localidad:string;provincia:string}};
export type QuoteOptions={disponible:boolean;mensaje:string;configuracion:string|null;revisionComercial:string|null;sucursales:{codigoPublico:string;nombre:string;direccion:string}[];combinaciones:{sucursal:string;servicio:string;formato:string;papel:string;color:WorkItem["color"];dobleFaz:boolean;terminaciones:string[]}[];formatos:Named[];papeles:Named[];servicios:Named[];modalidades:Mode[];puntos:{codigoPublico:string;sucursal:string;nombre:string;direccion:string;costo:string}[];medios:string[]};
type Named={codigoPublico:string;nombre:string};
export type QuoteInput={operacion:string;configuracion:string;revisionComercial:string;sucursal:string;items:{documento:DocumentInfo;trabajo:WorkItem}[];modalidad:Mode;punto:string|null;direccion:Address|null;medioPago:string;reemplaza:string|null};
export type Quote={correccion:{pedido:string;solicitud:string;pendiente:boolean}|null;codigoPublico:string;numero:number;version:number;estado:string;generadaEn:string;vigenteHasta:string;aceptadaEn:string|null;canceladaEn:string|null;motivoCancelacion:string|null;reemplaza:string|null;reemplazadaPor:string|null;oferta:{configuracion:string;numeroConfiguracion:number;revisionComercial:string;numeroRevision:number;sucursal:{codigoPublico:string;nombre:string;direccion:string};items:{codigoPublico:string;documento:DocumentInfo;trabajo:WorkItem;formato:string;papel:string;precio:{paginas:number;copias:number;carillas:number;hojas:number;subtotal:string;lineas:{servicio:string;nombre:string;base:string;unidades:number;precioUnitario:string;importe:string;detalle?:string}[]}}[];modalidad:Mode;punto:string|null;direccion:Address|null;destino:string;medioPago:string;subtotal:string;costoEntrega:string;total:string;condiciones:{revisionHumana:boolean;cargaRequiereAcreditacion:boolean;pagoPrevioRequerido:string;senaRequerida:string;saldo:string;momento:string;mediosAcreditacion:string[];instrucciones:string[]};entrega:{disponibleDesde:string|null;zonaHoraria:string;advertencias:string[]}}};
export type QuoteList={elementos:{codigoPublico:string;numero:number;estado:string;generadaEn:string;vigenteHasta:string;aceptadaEn:string|null;total:string;sucursal:string;items:number}[];total:number;pagina:number;tamano:number};
const record=(v:unknown):v is Record<string,unknown>=>typeof v==="object"&&v!==null&&!Array.isArray(v);
export function isQuote(v:unknown):v is Quote{return record(v)&&typeof v.codigoPublico==="string"&&Number.isSafeInteger(v.numero)&&Number.isSafeInteger(v.version)&&typeof v.estado==="string"&&typeof v.vigenteHasta==="string"&&record(v.oferta)&&Array.isArray(v.oferta.items)&&v.oferta.items.every(i=>record(i)&&record(i.documento)&&record(i.trabajo)&&record(i.precio)&&Array.isArray(i.precio.lineas))&&record(v.oferta.condiciones)&&Array.isArray(v.oferta.condiciones.instrucciones)&&record(v.oferta.entrega)&&typeof v.oferta.total==="string";}
export function isOptions(v:unknown):v is QuoteOptions{return record(v)&&typeof v.disponible==="boolean"&&typeof v.mensaje==="string"&&["sucursales","combinaciones","formatos","papeles","servicios","modalidades","puntos","medios"].every(k=>Array.isArray(v[k]));}
export function isQuoteList(v:unknown):v is QuoteList{return record(v)&&Number.isSafeInteger(v.total)&&Number.isSafeInteger(v.pagina)&&Array.isArray(v.elementos)&&v.elementos.every(e=>record(e)&&typeof e.codigoPublico==="string"&&typeof e.total==="string"&&typeof e.estado==="string");}
export const money=(value:string)=>{const [whole,fraction="00"]=value.split(".");return new Intl.NumberFormat("es-AR",{style:"currency",currency:"ARS"}).formatToParts(BigInt(whole)).map(p=>p.type==="fraction"?fraction.padEnd(2,"0"):p.value).join("");};
export const date=(value:string|null)=>value?new Intl.DateTimeFormat("es-AR",{dateStyle:"short",timeStyle:"short"}).format(new Date(value)):"Pendiente";
export async function quoteRead<T>(path:string,guard:(v:unknown)=>v is T):Promise<T>{const r=await fetch(path,{cache:"no-store"});const d:unknown=await r.json();if(!r.ok||!guard(d))throw new Error(record(d)&&typeof d.mensaje==="string"?d.mensaje:"No pudimos consultar las cotizaciones. Volvé a intentarlo.");return d;}
