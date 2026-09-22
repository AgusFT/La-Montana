import {isConfigurationCopy,type ConfigurationCopy} from "@/lib/configuration-types";
export type Value=null|boolean|number|string|Value[]|{[key:string]:Value};
export type Actor={nombre:string;rol:string|null;capturado:boolean};
export type Reference={codigo:string;numero:number};
export type HistoryRow={codigo:string;numero:number;estado:string;modelo:string|null;criterio:string|null;creadaEn:string;activadaEn:string|null;finVigencia:string|null;confirmadaEn:string|null;previstaEn:string|null;zonaHoraria:string|null;canceladaEn:string|null;autor:Actor;motivo:string};
export type HistoryPage<T>={elementos:T[];total:number;pagina:number;tamano:number};
export type HistoryList={versiones:HistoryPage<HistoryRow>;cantidades:Record<string,number>;activa:Reference|null};
export type HistoryModule={codigo:string;titulo:string;valores:Value};
export type BaseAvailability={permitida:boolean;motivo:string};
export type HistoryDetail={copia:ConfigurationCopy|null;base:BaseAvailability;version:HistoryRow;modulos:HistoryModule[];predecesora:Reference|null;reemplazadaPor:Reference|null;revisionComercial:Reference|null;activador:Actor|null;programador:Actor|null;motivoProgramacion:string|null;referencias:Record<string,string>};
export type HistoryEvent={clave:string;fecha:string;tipo:string;descripcion:string|null;actor:Actor;edicion:number|null;recurso:string|null;estadoAnterior:string|null;estadoNuevo:string|null;origen:string|null;inicio:string|null;fin:string|null;atraso:string|null};
export type HistoryComparison={base:BaseAvailability;origen:HistoryRow;destino:HistoryRow;secciones:{codigo:string;titulo:string;modificada:boolean;origen:Value;destino:Value}[];modificadas:number;comercialOrigen:Reference|null;comercialDestino:Reference|null;referencias:Record<string,string>};
const record=(v:unknown):v is Record<string,unknown>=>v!==null&&typeof v==="object"&&!Array.isArray(v);
const nullableString=(v:unknown)=>v===null||typeof v==="string";
function actor(v:unknown):v is Actor{return record(v)&&typeof v.nombre==="string"&&nullableString(v.rol)&&typeof v.capturado==="boolean";}
function ref(v:unknown):boolean{return v===null||record(v)&&typeof v.codigo==="string"&&Number.isSafeInteger(v.numero);}
function row(v:unknown):v is HistoryRow{return record(v)&&typeof v.codigo==="string"&&Number.isSafeInteger(v.numero)&&["ACTIVA","HISTORICA","PROGRAMADA","CANCELADA"].includes(String(v.estado))&&actor(v.autor)&&typeof v.motivo==="string"&&typeof v.creadaEn==="string"&&["modelo","criterio","activadaEn","finVigencia","confirmadaEn","previstaEn","zonaHoraria","canceladaEn"].every(k=>nullableString(v[k]));}
function value(v:unknown):v is Value{return v===null||["boolean","number","string"].includes(typeof v)||Array.isArray(v)&&v.every(value)||record(v)&&Object.values(v).every(value);}
function refs(v:unknown):v is Record<string,string>{return record(v)&&Object.values(v).every(x=>typeof x==="string");}
function page<T>(v:unknown,valid:(x:unknown)=>x is T):v is HistoryPage<T>{return record(v)&&Array.isArray(v.elementos)&&v.elementos.every(valid)&&["total","pagina","tamano"].every(k=>Number.isSafeInteger(v[k])&&Number(v[k])>=0);}
export function isHistoryList(v:unknown):v is HistoryList{return record(v)&&page(v.versiones,row)&&ref(v.activa)&&record(v.cantidades)&&Object.values(v.cantidades).every(x=>Number.isSafeInteger(x));}
export function isHistoryDetail(v:unknown):v is HistoryDetail{return record(v)&&base(v.base)&&(v.copia===null||isConfigurationCopy(v.copia))&&row(v.version)&&Array.isArray(v.modulos)&&v.modulos.every(m=>record(m)&&typeof m.codigo==="string"&&typeof m.titulo==="string"&&value(m.valores))&&["predecesora","reemplazadaPor","revisionComercial"].every(k=>ref(v[k]))&&["activador","programador"].every(k=>v[k]===null||actor(v[k]))&&nullableString(v.motivoProgramacion)&&refs(v.referencias);}
export function isHistoryEvents(v:unknown):v is HistoryPage<HistoryEvent>{return page(v,(e:unknown):e is HistoryEvent=>record(e)&&["clave","fecha","tipo"].every(k=>typeof e[k]==="string")&&actor(e.actor)&&(e.edicion===null||Number.isSafeInteger(e.edicion))&&["descripcion","recurso","estadoAnterior","estadoNuevo","origen","inicio","fin","atraso"].every(k=>nullableString(e[k])));}
export function isHistoryComparison(v:unknown):v is HistoryComparison{return record(v)&&base(v.base)&&row(v.origen)&&row(v.destino)&&Array.isArray(v.secciones)&&v.secciones.every(s=>record(s)&&typeof s.codigo==="string"&&typeof s.titulo==="string"&&typeof s.modificada==="boolean"&&value(s.origen)&&value(s.destino))&&Number.isSafeInteger(v.modificadas)&&ref(v.comercialOrigen)&&ref(v.comercialDestino)&&refs(v.referencias);}

function base(v:unknown):v is BaseAvailability{return record(v)&&typeof v.permitida==="boolean"&&typeof v.motivo==="string";}
