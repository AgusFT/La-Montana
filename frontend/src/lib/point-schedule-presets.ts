import type {Branch} from './organization-types';
import type {PointSource,PointWindow} from './point-network';

/** Only an unambiguous enabled priority 1 is chosen automatically. */
export function priorityOneBranch(sources:PointSource[],branches:Branch[]):Branch|undefined{
 const first=sources.filter(s=>s.habilitado&&s.prioridad===1);
 return first.length===1?branches.find(b=>b.codigoPublico===first[0].sucursal):undefined;
}

/** A one-time editable copy; source schedules, route assignments and costs stay intact. */
export function pointWindowsFromBranch(branch:Branch,capacity:number):PointWindow[]{
 if(!Number.isSafeInteger(capacity)||capacity<1||capacity>1000000)throw new Error('Indicá un cupo de 1 a 1.000.000 de pedidos por franja.');
 const days=branch.horarioAtencion.filter(d=>d.habilitado);
 if(!days.length)throw new Error('Esta sucursal todavía no tiene días de atención habilitados. Completá sus horarios en Sucursales o agregá las franjas manualmente.');
 const time=/^([01]\d|2[0-3]):[0-5]\d$/;
 if(new Set(days.map(d=>d.dia)).size!==days.length||days.some(d=>!Number.isInteger(d.dia)||d.dia<1||d.dia>7||!d.apertura||!d.cierre||!time.test(d.apertura)||!time.test(d.cierre)||d.apertura>=d.cierre))throw new Error('Revisá los horarios de atención de la sucursal antes de copiarlos.');
 return days.map(d=>({dia:d.dia,apertura:d.apertura!,cierre:d.cierre!,capacidadPedidos:capacity,habilitada:true})).sort((a,b)=>a.dia-b.dia);
}
