import {uuidPattern} from "./organization-types";
export type SiteImage={codigo:string;alternativo:string};
export type SiteCard={codigo:string;tipo:"PRODUCTO"|"SERVICIO";nombre:string;descripcion:string;servicio:string|null;imagenes:SiteImage[];orden:number;visible:boolean};
export type SiteSection="PORTADA"|"CATALOGO"|"CONTACTO";
export type SiteContent={nombre:string;titulo:string;descripcion:string;logo:string|null;logoAlt:string;portada:string|null;portadaAlt:string;correo:string;telefono:string;direccion:string;secciones:SiteSection[];fichas:SiteCard[]};
export type SiteDraft={version:number;contenido:SiteContent;carpeta:string;actualizadaEn:string};
export type SitePublication={codigo:string;numero:number;versionBorrador:number;autor:string;publicadaEn:string};
export type SiteState={borrador:SiteDraft;publicada:SitePublication|null};
export type SitePublic={configurada:boolean;version:number|null;publicadaEn:string|null;contenido:SiteContent|null};
export type SiteReview={version:number;publicable:boolean;pendientes:string[];fichasVisibles:number;imagenes:number};
const record=(v:unknown):v is Record<string,unknown>=>!!v&&typeof v==="object"&&!Array.isArray(v);
const id=(v:unknown)=>typeof v==="string"&&uuidPattern.test(v);
const optionalId=(v:unknown)=>v===null||id(v);
const integer=(v:unknown)=>typeof v==="number"&&Number.isSafeInteger(v)&&v>=0;
export function isSiteContent(v:unknown):v is SiteContent{return record(v)&&["nombre","titulo","descripcion","logoAlt","portadaAlt","correo","telefono","direccion"].every(k=>typeof v[k]==="string")&&optionalId(v.logo)&&optionalId(v.portada)&&Array.isArray(v.secciones)&&v.secciones.every(s=>["PORTADA","CATALOGO","CONTACTO"].includes(s))&&Array.isArray(v.fichas)&&v.fichas.every(f=>record(f)&&id(f.codigo)&&["PRODUCTO","SERVICIO"].includes(String(f.tipo))&&typeof f.nombre==="string"&&typeof f.descripcion==="string"&&optionalId(f.servicio)&&integer(f.orden)&&typeof f.visible==="boolean"&&Array.isArray(f.imagenes)&&f.imagenes.every(i=>record(i)&&id(i.codigo)&&typeof i.alternativo==="string"));}
export function isSitePublication(v:unknown):v is SitePublication{return record(v)&&id(v.codigo)&&integer(v.numero)&&integer(v.versionBorrador)&&typeof v.autor==="string"&&typeof v.publicadaEn==="string";}
export function isSiteState(v:unknown):v is SiteState{return record(v)&&record(v.borrador)&&integer(v.borrador.version)&&isSiteContent(v.borrador.contenido)&&typeof v.borrador.carpeta==="string"&&typeof v.borrador.actualizadaEn==="string"&&(v.publicada===null||isSitePublication(v.publicada));}
export function isSitePublic(v:unknown):v is SitePublic{return record(v)&&typeof v.configurada==="boolean"&&(v.configurada?integer(v.version)&&typeof v.publicadaEn==="string"&&isSiteContent(v.contenido):v.version===null&&v.publicadaEn===null&&v.contenido===null);}
export function isSiteReview(v:unknown):v is SiteReview{return record(v)&&integer(v.version)&&typeof v.publicable==="boolean"&&integer(v.fichasVisibles)&&integer(v.imagenes)&&Array.isArray(v.pendientes)&&v.pendientes.every(p=>typeof p==="string");}
export const sectionLabels:Record<SiteSection,string>={PORTADA:"Portada",CATALOGO:"Productos y servicios",CONTACTO:"Contacto"};
