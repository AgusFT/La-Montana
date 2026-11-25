//#region ENCABEZADO · installation-types.ts
/*
 * ========================================================================
 * FUNCIÓN: Contrato del progreso real de primera instalación y accesos por área.
 * FUNCIONES: isPreparation(value), installationAllowed(data, area), nextStep(data).
 * TIPOS: InstallationStep, Preparation; valores: installationChanged.
 * ========================================================================
 */
//#endregion
export const installationChanged="lamontana:installation-changed";
export type InstallationStep={codigo:string;titulo:string;completa:boolean;habilitada:boolean;enlace:string};
export type Preparation={activa:boolean;numero:number|null;origen:string;borradorPendiente:number|null;
  pasos:{codigo:string;titulo:string;completo:boolean;estado:string;detalle:string;enlace:string;complementario:boolean}[];
  guia:{primeraInstalacion:boolean;faseDisponible:number;versionBorrador:number|null;etapas:InstallationStep[];pendientesFase:string[]}};
export function isPreparation(value:unknown):value is Preparation{
  if(!value||typeof value!=="object")return false;const v=value as Preparation,g=v.guia;
  return typeof v.activa==="boolean"&&!!g&&g.primeraInstalacion===!v.activa&&Number.isInteger(g.faseDisponible)&&g.faseDisponible>=1&&g.faseDisponible<=8&&
    (g.versionBorrador===null||Number.isInteger(g.versionBorrador))&&Array.isArray(g.pendientesFase)&&g.pendientesFase.every(x=>typeof x==="string")&&
    Array.isArray(g.etapas)&&g.etapas.length===9&&g.etapas.every(s=>s&&typeof s.codigo==="string"&&typeof s.titulo==="string"&&typeof s.completa==="boolean"&&typeof s.habilitada==="boolean"&&typeof s.enlace==="string"&&s.enlace.startsWith("/administracion"))&&
    Array.isArray(v.pasos)&&v.pasos.every(s=>s&&typeof s.codigo==="string"&&typeof s.titulo==="string"&&typeof s.detalle==="string"&&typeof s.estado==="string"&&typeof s.completo==="boolean"&&typeof s.complementario==="boolean"&&typeof s.enlace==="string"&&(s.enlace.startsWith("/administracion")||s.enlace==="/cuenta/seguridad"));
}
export function nextStep(data:Preparation){return data.guia.etapas.find(s=>!s.completa);}
export function installationAllowed(data:Preparation|null,area:string){
  if(data?.activa||["dashboard","sucursales","empleados","web"].includes(area))return true;
  if(!data)return false;
  if(area==="catalogo")return !!data.guia.etapas.find(s=>s.codigo==="papeles")?.habilitada;
  if(area==="configuracion")return !!data.guia.etapas.find(s=>s.codigo==="modelo")?.habilitada;
  if(area==="puntos")return data.guia.faseDisponible>=5;
  return false;
}
