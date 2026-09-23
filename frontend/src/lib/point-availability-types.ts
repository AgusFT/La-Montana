//#region ENCABEZADO · src/lib/point-availability-types.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/point-availability-types.ts
 * ========================================================================
 * FUNCIÓN
 * Define y valida los estados de disponibilidad temporal, puntos operativos y panel de
 * disponibilidad.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - record(v: unknown): v is Record<string,unknown>
 * - [export] isPointAvailability(v: unknown): v is PointAvailability
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isPointAvailabilityPanel(v: unknown): v is PointAvailabilityPanel
 *   Validador de datos recibidos en tiempo de ejecución.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PointAvailability (tipo).
 * - OperationalPoint (tipo).
 * - PointAvailabilityPanel (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - pointStateLabels [const, exportado].
 * ========================================================================
 */
//#endregion

import {uuidPattern} from "@/lib/organization-types";
export type PointAvailability={punto:string;estado:"SIN_DEFINIR"|"HABILITADO"|"DESHABILITADO";version:number;actualizadaEn:string|null;actor:string|null};
export type OperationalPoint={codigoPublico:string;codigo:string;nombre:string;direccion:string;zonaHoraria:string;disponibilidad:PointAvailability};
export type PointAvailabilityPanel={contexto:"SIN_CONFIGURACION_ACTIVA"|"CONFIGURACION_ACTIVA";borrador:string|null;puntos:OperationalPoint[];deshabilitados:number};
const record=(v:unknown):v is Record<string,unknown>=>!!v&&typeof v==="object";
export function isPointAvailability(v:unknown):v is PointAvailability{return record(v)&&typeof v.punto==="string"&&uuidPattern.test(v.punto)&&["SIN_DEFINIR","HABILITADO","DESHABILITADO"].includes(String(v.estado))&&Number.isSafeInteger(v.version)&&Number(v.version)>=0&&(v.actualizadaEn===null||typeof v.actualizadaEn==="string")&&(v.actor===null||typeof v.actor==="string");}
export function isPointAvailabilityPanel(v:unknown):v is PointAvailabilityPanel{return record(v)&&["SIN_CONFIGURACION_ACTIVA","CONFIGURACION_ACTIVA"].includes(String(v.contexto))&&(v.borrador===null||typeof v.borrador==="string"&&uuidPattern.test(v.borrador))&&Number.isSafeInteger(v.deshabilitados)&&Number(v.deshabilitados)>=0&&Array.isArray(v.puntos)&&v.puntos.every(p=>record(p)&&["codigoPublico","codigo","nombre","direccion","zonaHoraria"].every(k=>typeof p[k]==="string")&&isPointAvailability(p.disponibilidad)&&p.disponibilidad.punto===p.codigoPublico);}
export const pointStateLabels={SIN_DEFINIR:"Sin definir",HABILITADO:"Habilitado",DESHABILITADO:"Deshabilitado"};
