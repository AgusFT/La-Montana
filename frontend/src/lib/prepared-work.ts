import {isFile,type PrivateFile} from './file-types';
import {isQuote,type Quote,type WorkItem,type Mode,type Address} from './quote-types';
import {type OrderReview} from './order-types';

export type PreparedEntry={archivo:string;trabajo:WorkItem};
export type Budget={configuracion:string;revisionComercial:string;items:Quote['oferta']['items'];subtotal:string};
export type Preferences={modalidad:Mode;destinoRetiro:string|null;punto:string|null;direccion:Address|null;medioPago:string};
export type Preparation={codigoPublico:string;version:number;estado:'ABIERTA'|'CONFIRMADA';archivos:PrivateFile[];items:PreparedEntry[];presupuesto:Budget|null;preferencias:Preferences|null;cotizacion:string|null;pedido:string|null};
export type Checkout={preparacion:Preparation;cotizacion:Quote|null;revision:OrderReview|null;huella:string|null};
const record=(v:unknown):v is Record<string,unknown>=>typeof v==='object'&&v!==null&&!Array.isArray(v);
export function isPreparation(v:unknown):v is Preparation{return record(v)&&typeof v.codigoPublico==='string'&&Number.isSafeInteger(v.version)&&['ABIERTA','CONFIRMADA'].includes(String(v.estado))&&Array.isArray(v.archivos)&&v.archivos.every(isFile)&&Array.isArray(v.items)&&v.items.every(i=>record(i)&&typeof i.archivo==='string'&&record(i.trabajo))&&(v.presupuesto===null||record(v.presupuesto)&&typeof v.presupuesto.subtotal==='string'&&Array.isArray(v.presupuesto.items));}
export function isCheckout(v:unknown):v is Checkout{return record(v)&&isPreparation(v.preparacion)&&(v.cotizacion===null||isQuote(v.cotizacion))&&(v.revision===null||record(v.revision)&&typeof v.revision.habilitada==='boolean'&&Array.isArray(v.revision.bloqueos)&&Array.isArray(v.revision.archivos))&&(v.huella===null||typeof v.huella==='string');}
