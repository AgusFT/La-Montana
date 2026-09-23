//#region ENCABEZADO · src/lib/configuration-rollback-types.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/configuration-rollback-types.ts
 * ========================================================================
 * FUNCIÓN
 * Define y valida las respuestas de revisión y resultado de la reversión manual de configuración.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - record(v: unknown): v is Record<string,unknown>
 * - [export] isRollbackReview(v: unknown): v is RollbackReview
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isRollbackResult(v: unknown): v is RollbackResult
 *   Validador de datos recibidos en tiempo de ejecución.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - RollbackReview (tipo).
 * - RollbackResult (tipo).
 * ========================================================================
 */
//#endregion

import {isConfigurationVersion,isConfigurationSchedule,isConfigurationDraft,isConfigurationAttempt,type ConfigurationVersion,type ConfigurationSchedule,type ConfigurationDraft,type ConfigurationAttempt} from "@/lib/configuration-types";
import {isReview,type Review} from "@/lib/configuration-review-types";
export type RollbackReview={activa:ConfigurationVersion|null;objetivo:ConfigurationVersion|null;programada:ConfigurationSchedule|null;condiciones:Review|null;impedimentos:string[];permitida:boolean;huella:string};
export type RollbackResult={estado:string;intento:ConfigurationAttempt;version:ConfigurationVersion|null;programacionCancelada:ConfigurationDraft|null};
const record=(v:unknown):v is Record<string,unknown>=>v!==null&&typeof v==="object"&&!Array.isArray(v);
export function isRollbackReview(v:unknown):v is RollbackReview{return record(v)&&(v.activa===null||isConfigurationVersion(v.activa))&&(v.objetivo===null||isConfigurationVersion(v.objetivo))&&(v.programada===null||isConfigurationSchedule(v.programada))&&(v.condiciones===null||isReview(v.condiciones))&&Array.isArray(v.impedimentos)&&v.impedimentos.every(x=>typeof x==="string")&&typeof v.permitida==="boolean"&&typeof v.huella==="string";}
export function isRollbackResult(v:unknown):v is RollbackResult{return record(v)&&["EXITOSO","FALLIDO","INTERRUMPIDO","INICIADO"].includes(String(v.estado))&&isConfigurationAttempt(v.intento)&&v.intento.operacion==="ROLLBACK"&&v.estado===v.intento.estado&&(v.estado==="EXITOSO"?isConfigurationVersion(v.version)&&v.version.configuracion.copia?.tipo==="ROLLBACK"&&v.version.configuracion.copia.origen===v.intento.configuracion&&v.version.configuracion.codigoPublico===v.intento.resultante:v.version===null)&&(v.programacionCancelada===null||v.estado==="EXITOSO"&&isConfigurationDraft(v.programacionCancelada)&&v.programacionCancelada.estado==="CANCELADA");}
