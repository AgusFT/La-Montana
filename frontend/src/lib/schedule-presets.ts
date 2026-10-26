/* Operaciones de edición explícitas; no guardan ni activan horarios. */
import type {OperatingDay,PointSlot} from "./configuration-types";
export type EditablePickupSlot=Omit<PointSlot,"dia"|"capacidadPedidos"|"habilitada">&{dia:number|null;capacidadPedidos:number|null;habilitada:boolean|null};
export function validWeeklyHours(days:number[],start:string,end:string){return days.length>0&&new Set(days).size===days.length&&days.every(d=>Number.isInteger(d)&&d>=1&&d<=7)&&/^([01]\d|2[0-3]):[0-5]\d$/.test(start)&&/^([01]\d|2[0-3]):[0-5]\d$/.test(end)&&start<end;}
export function applyProductionWeek(current:OperatingDay[],days:number[],start:string,end:string,closeOthers:boolean):OperatingDay[]{
 if(!validWeeklyHours(days,start,end))throw new Error("Elegí días y un horario válido.");
 return Array.from({length:7},(_,i)=>{const dia=i+1;return days.includes(dia)?{dia,habilitado:true,apertura:start,cierre:end}:closeOthers?{dia,habilitado:false,apertura:null,cierre:null}:{...(current.find(d=>d.dia===dia)??{dia,habilitado:null,apertura:null,cierre:null})};});
}
export function applyPickupWeek(current:EditablePickupSlot[],days:number[],start:string,end:string,capacity:number):EditablePickupSlot[]{
 if(!validWeeklyHours(days,start,end)||!Number.isSafeInteger(capacity)||capacity<=0||capacity>2147483647)throw new Error("Indicá un horario y un cupo positivo válidos.");
 return [...current.filter(s=>s.dia===null||!days.includes(s.dia)).map(s=>({...s})),...days.map(dia=>({dia,apertura:start,cierre:end,capacidadPedidos:capacity,habilitada:true}))];
}
