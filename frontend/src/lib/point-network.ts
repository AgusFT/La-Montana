import {isBranch,type Branch} from './organization-types';
export type PointWindow={dia:number;apertura:string;cierre:string;capacidadPedidos:number;habilitada:boolean};
export type PointSource={sucursal:string;prioridad:number;habilitado:boolean;costo:string;franjas:PointWindow[]};
export type PointDefinition={codigoPublico:string;codigo:string;nombre:string;calle:string;numero:string;localidad:string;provincia:string;codigoPostal:string;referencias:string|null;zonaHoraria:string;sucursales:PointSource[]};
export type NetworkPoint={definicion:PointDefinition;version:number;revision:string|null;actor:string|null;fecha:string|null};
export type PointNetwork={puntos:NetworkPoint[];sucursales:Branch[];configuracionActiva:boolean};
const record=(v:unknown):v is Record<string,unknown>=>!!v&&typeof v==='object'&&!Array.isArray(v);
export function isNetworkPoint(v:unknown):v is NetworkPoint{
 if(!record(v)||!record(v.definicion)||!Number.isSafeInteger(v.version)||Number(v.version)<0||!['revision','actor','fecha'].every(k=>v[k]===null||typeof v[k]==='string'))return false;
 const p=v.definicion;
 return ['codigoPublico','codigo','nombre','calle','numero','localidad','provincia','codigoPostal','zonaHoraria'].every(k=>typeof p[k]==='string')&&
  (p.referencias===null||typeof p.referencias==='string')&&Array.isArray(p.sucursales)&&p.sucursales.every(s=>record(s)&&typeof s.sucursal==='string'&&Number.isSafeInteger(s.prioridad)&&Number(s.prioridad)>=1&&Number(s.prioridad)<=999&&typeof s.habilitado==='boolean'&&typeof s.costo==='string'&&Array.isArray(s.franjas)&&s.franjas.every(f=>record(f)&&Number.isInteger(f.dia)&&Number(f.dia)>=1&&Number(f.dia)<=7&&typeof f.apertura==='string'&&typeof f.cierre==='string'&&Number.isSafeInteger(f.capacidadPedidos)&&Number(f.capacidadPedidos)>=0&&typeof f.habilitada==='boolean'));
}
export function isPointNetwork(v:unknown):v is PointNetwork{return record(v)&&Array.isArray(v.puntos)&&v.puntos.every(isNetworkPoint)&&Array.isArray(v.sucursales)&&v.sucursales.every(isBranch)&&typeof v.configuracionActiva==='boolean';}
export const pointAddress=(p:PointDefinition)=>`${p.calle} ${p.numero}, ${p.localidad}, ${p.provincia}`;
export function sourcesForBranch(network:PointNetwork|null,branch:string){return network?.puntos.filter(p=>p.definicion.sucursales.some(s=>s.sucursal===branch&&s.habilitado))??[];}
