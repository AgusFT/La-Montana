//#region ENCABEZADO · src/lib/configuration-types.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/configuration-types.ts
 * ========================================================================
 * FUNCIÓN
 * Define modelos, pagos, recursos, entrega, borradores, versiones e intentos de configuración
 * operativa, con validadores de datos recibidos y etiquetas de presentación.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] isConfigurationAttempt(i: unknown): i is ConfigurationAttempt
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isActivationOutcome(v: unknown): v is ActivationOutcome
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isConfigurationSchedule(v: unknown): v is ConfigurationSchedule
 *   Validador de datos recibidos en tiempo de ejecución.
 * - record(value: unknown): value is Record<string,unknown>
 * - [export] isPaymentConfiguration(value: unknown): value is PaymentConfiguration
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isResourceConfiguration(value: unknown): value is ResourceConfiguration
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isDeliveryPoint(value: unknown): value is DeliveryPoint
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isDeliveryZone(v: unknown): v is DeliveryZone
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isDeliveryConfiguration(value: unknown): value is DeliveryConfiguration
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isConfigurationCopy(v: unknown): v is ConfigurationCopy
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isConfigurationDraft(value: unknown): value is ConfigurationDraft
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isConfigurationVersion(v: unknown): v is ConfigurationVersion
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] isConfigurationState(value: unknown): value is ConfigurationState
 *   Validador de datos recibidos en tiempo de ejecución.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - OperatingModel (tipo).
 * - ApprovalCriterion (tipo).
 * - PaymentMethod (tipo).
 * - DepositCondition (tipo).
 * - PaymentConfiguration (tipo).
 * - ConfiguredPrinter (tipo).
 * - ResourceConfiguration (tipo).
 * - DeliveryMode (tipo).
 * - OperatingDay (tipo).
 * - BranchSchedule (tipo).
 * - PointSlot (tipo).
 * - PointOrigin (tipo).
 * - DeliveryPoint (tipo).
 * - DeliveryTerritory (tipo).
 * - DeliveryZone (tipo).
 * - DeliveryConfiguration (tipo).
 * - ConfigurationCopy (tipo).
 * - ConfigurationDraft (tipo).
 * - ConfigurationVersion (tipo).
 * - ConfigurationAttempt (tipo).
 * - ConfigurationSchedule (tipo).
 * - ActivationOutcome (tipo).
 * - ConfigurationState (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - modelLabels [const, exportado].
 * - criterionLabels [const, exportado].
 * ========================================================================
 */
//#endregion

export type OperatingModel = "MANUAL" | "CONDICIONAL";
export type ApprovalCriterion = "PAGO_PREVIO" | "SENA" | "MONTO_TOTAL";
export type PaymentMethod = "TRANSFERENCIA" | "EFECTIVO";
export type DepositCondition = "SIEMPRE" | "DESDE_CARILLAS" | "DESDE_MONTO" | "SUPERAR_UMBRAL_APROBACION";
export type PaymentConfiguration = {
  medios: PaymentMethod[]; instruccionesTransferencia: string | null; vigenciaCotizacionMinutos: number;
  exigirSena: boolean; condicionSena: DepositCondition | null; umbralSena: string | null;
  tipoSena: "PORCENTAJE" | "FIJA" | null; valorSena: string | null; umbralAprobacion: string | null;
};
export type ConfiguredPrinter = {
  codigoPublico: string; sucursal: string; nombre: string; formatos: string[];
  admiteColor: boolean; admiteDobleFaz: boolean; capacidadHojas: number;
  estado: "OPERATIVA" | "DESHABILITADA" | "RETIRADA"; retiradaEn: string | null; motivoRetiro: string | null;
};
export type ResourceConfiguration = {
  metodoAsignacion: "MANUAL" | null; impresoras: ConfiguredPrinter[];
  serviciosPorSucursal: {sucursal: string; servicios: string[]}[];
};
export type DeliveryMode = "RETIRO_SUCURSAL" | "RETIRO_PUNTO_ENTREGA" | "ENVIO_DOMICILIO";
export type OperatingDay = {dia:number;habilitado:boolean|null;apertura:string|null;cierre:string|null};
export type BranchSchedule = {sucursal:string;zonaHoraria:string;dias:OperatingDay[];franjasRetiro?:PointSlot[]};
export type PointSlot = {dia:number;apertura:string;cierre:string;capacidadPedidos:number;habilitada:boolean};
export type PointOrigin = {sucursal:string;habilitado:boolean;costo:string;franjas:PointSlot[]};
export type DeliveryPoint = {codigoPublico:string;codigo:string;nombre:string;calle:string;numero:string;localidad:string;provincia:string;codigoPostal:string;referencias:string|null;zonaHoraria:string;sucursales:PointOrigin[]};
export type DeliveryTerritory = {codigoPostal:string;localidad:string;provincia:string};
export type DeliveryZone = {codigoPublico:string;codigo:string;nombre:string;descripcion:string|null;zonaHoraria:string;costo:string;habilitada:boolean;territorios:DeliveryTerritory[];franjas:PointSlot[]};
export type DeliveryConfiguration = {zonas:DeliveryZone[];puntos:DeliveryPoint[];preparacionHoras:string|null;trasladoHoras:string|null;modalidades:DeliveryMode[];horariosPorSucursal:BranchSchedule[]};
export type ConfigurationCopy = {tipo:"USO_COMO_BASE"|"ROLLBACK";origen:string;numeroOrigen:number;creadaEn:string;fasesConfirmadas:number[];barridoInicial:{nivel:string;codigo:string;area:string;fase:number;mensaje:string;sucursal:string|null}[]};
export type ConfigurationDraft = {
  copia: ConfigurationCopy | null;
  codigoPublico: string; numero: number; version: number; estado: "EN_PREPARACION" | "PROGRAMADA" | "CANCELADA" | "ACTIVA" | "HISTORICA";
  modelo: OperatingModel | null; criterio: ApprovalCriterion | null;
  creadaEn: string; actualizadaEn: string; actor: string;
  canceladaEn: string | null; motivoCancelacion: string | null; cancelador: string | null;
  pagos: PaymentConfiguration | null; recursos: ResourceConfiguration; entrega: DeliveryConfiguration;
};
export type ConfigurationVersion={configuracion:ConfigurationDraft;activadaEn:string;finVigencia:string|null;activador:string;motivo:string;predecesora:string|null;revisionComercial:string};
export type ConfigurationAttempt={operacion:"ACTIVACION"|"ROLLBACK";resultante:string|null;numeroResultante:number|null;codigo:string;configuracion:string;numero:number;origen:string;estado:string;iniciadoEn:string;terminadoEn:string|null;atrasoDetectadoEn:string|null;resultado:string|null;detalle:string|null};
export type ConfigurationSchedule={configuracion:ConfigurationDraft;confirmadaEn:string;previstaEn:string;zonaHoraria:string;programador:string;motivo:string;proximoIntentoEn:string|null;intentos:ConfigurationAttempt[]};
export type ActivationOutcome={estado:"EXITOSO"|"FALLIDO"|"INTERRUMPIDO"|"INICIADO";intento:ConfigurationAttempt|null;version:ConfigurationVersion|null};
export function isConfigurationAttempt(i:unknown):i is ConfigurationAttempt{return record(i)&&["ACTIVACION","ROLLBACK"].includes(String(i.operacion))&&(i.resultante===null||typeof i.resultante==="string")&&(i.numeroResultante===null||Number.isSafeInteger(i.numeroResultante))&&["codigo","configuracion","origen","estado","iniciadoEn"].every(k=>typeof i[k]==="string")&&Number.isSafeInteger(i.numero)&&["terminadoEn","atrasoDetectadoEn","resultado","detalle"].every(k=>i[k]===null||typeof i[k]==="string");}
export function isActivationOutcome(v:unknown):v is ActivationOutcome{return record(v)&&["EXITOSO","FALLIDO","INTERRUMPIDO","INICIADO"].includes(String(v.estado))&&(v.intento===null||isConfigurationAttempt(v.intento))&&(v.estado==="EXITOSO"?isConfigurationVersion(v.version):v.version===null&&isConfigurationAttempt(v.intento));}
export function isConfigurationSchedule(v:unknown):v is ConfigurationSchedule{return record(v)&&isConfigurationDraft(v.configuracion)&&["PROGRAMADA","ACTIVA","HISTORICA","CANCELADA"].includes(v.configuracion.estado)&&["confirmadaEn","previstaEn","zonaHoraria","programador","motivo"].every(k=>typeof v[k]==="string")&&(v.proximoIntentoEn===null||typeof v.proximoIntentoEn==="string")&&Array.isArray(v.intentos)&&v.intentos.every(i=>record(i)&&["codigo","origen","estado","iniciadoEn"].every(k=>typeof i[k]==="string")&&["terminadoEn","atrasoDetectadoEn","resultado","detalle"].every(k=>i[k]===null||typeof i[k]==="string"));}
export type ConfigurationState = { borrador: ConfigurationDraft | null; historial: ConfigurationDraft[];activa:ConfigurationVersion|null;programada:ConfigurationSchedule|null;intentos:ConfigurationAttempt[] };
export const modelLabels: Record<OperatingModel,string> = {MANUAL:"Control manual",CONDICIONAL:"Control condicional"};
export const criterionLabels: Record<ApprovalCriterion,string> = {PAGO_PREVIO:"Pago previo total",SENA:"Pago de seña",MONTO_TOTAL:"Monto total del pedido"};
function record(value:unknown):value is Record<string,unknown>{return !!value&&typeof value==="object";}
export function isPaymentConfiguration(value:unknown):value is PaymentConfiguration {
  return record(value)&&Array.isArray(value.medios)&&value.medios.length>0&&value.medios.every(item=>["TRANSFERENCIA","EFECTIVO"].includes(String(item)))&&
    (value.instruccionesTransferencia===null||typeof value.instruccionesTransferencia==="string")&&Number.isSafeInteger(value.vigenciaCotizacionMinutos)&&Number(value.vigenciaCotizacionMinutos)>0&&typeof value.exigirSena==="boolean"&&
    (value.condicionSena===null||["SIEMPRE","DESDE_CARILLAS","DESDE_MONTO","SUPERAR_UMBRAL_APROBACION"].includes(String(value.condicionSena)))&&
    (value.tipoSena===null||["PORCENTAJE","FIJA"].includes(String(value.tipoSena)))&&["umbralSena","valorSena","umbralAprobacion"].every(key=>value[key]===null||typeof value[key]==="string");
}
export function isResourceConfiguration(value: unknown): value is ResourceConfiguration {
  return record(value) && (value.metodoAsignacion === null || value.metodoAsignacion === "MANUAL") &&
    Array.isArray(value.impresoras) && value.impresoras.every(p => record(p) &&
      ["codigoPublico","sucursal","nombre"].every(k => typeof p[k] === "string") &&
      Array.isArray(p.formatos) && p.formatos.every(f => typeof f === "string") &&
      typeof p.admiteColor === "boolean" && typeof p.admiteDobleFaz === "boolean" &&
      Number.isSafeInteger(p.capacidadHojas) && Number(p.capacidadHojas) > 0 &&
      ["OPERATIVA","DESHABILITADA","RETIRADA"].includes(String(p.estado)) &&
      ["retiradaEn","motivoRetiro"].every(k => p[k] === null || typeof p[k] === "string")) &&
    Array.isArray(value.serviciosPorSucursal) && value.serviciosPorSucursal.every(s => record(s) &&
      typeof s.sucursal === "string" && Array.isArray(s.servicios) && s.servicios.every(id => typeof id === "string"));
}
export function isDeliveryPoint(value:unknown):value is DeliveryPoint {
  return record(value)&&["codigoPublico","codigo","nombre","calle","numero","localidad","provincia","codigoPostal","zonaHoraria"].every(k=>typeof value[k]==="string")&&(value.referencias===null||typeof value.referencias==="string")&&Array.isArray(value.sucursales)&&value.sucursales.every(s=>record(s)&&typeof s.sucursal==="string"&&typeof s.habilitado==="boolean"&&typeof s.costo==="string"&&Array.isArray(s.franjas)&&s.franjas.every(f=>record(f)&&Number.isInteger(f.dia)&&Number(f.dia)>=1&&Number(f.dia)<=7&&typeof f.apertura==="string"&&typeof f.cierre==="string"&&Number.isSafeInteger(f.capacidadPedidos)&&Number(f.capacidadPedidos)>=0&&typeof f.habilitada==="boolean"));
}
export function isDeliveryZone(v:unknown):v is DeliveryZone {
  return record(v)&&["codigoPublico","codigo","nombre","zonaHoraria","costo"].every(k=>typeof v[k]==="string")&&(v.descripcion===null||typeof v.descripcion==="string")&&typeof v.habilitada==="boolean"&&Array.isArray(v.territorios)&&v.territorios.every(t=>record(t)&&["codigoPostal","localidad","provincia"].every(k=>typeof t[k]==="string"))&&Array.isArray(v.franjas)&&v.franjas.every(f=>record(f)&&Number.isInteger(f.dia)&&Number(f.dia)>=1&&Number(f.dia)<=7&&typeof f.apertura==="string"&&typeof f.cierre==="string"&&Number.isSafeInteger(f.capacidadPedidos)&&Number(f.capacidadPedidos)>=0&&typeof f.habilitada==="boolean");
}
export function isDeliveryConfiguration(value:unknown):value is DeliveryConfiguration {
  return record(value) && Array.isArray(value.zonas) && value.zonas.every(isDeliveryZone) && Array.isArray(value.puntos) && value.puntos.every(isDeliveryPoint) && ["preparacionHoras","trasladoHoras"].every(k=>value[k]===null||typeof value[k]==="string") &&
    Array.isArray(value.modalidades) && value.modalidades.every(m=>["RETIRO_SUCURSAL","RETIRO_PUNTO_ENTREGA","ENVIO_DOMICILIO"].includes(String(m))) &&
    Array.isArray(value.horariosPorSucursal) && value.horariosPorSucursal.every(s=>record(s)&&typeof s.sucursal==="string"&&typeof s.zonaHoraria==="string"&&(s.franjasRetiro===undefined||Array.isArray(s.franjasRetiro)&&s.franjasRetiro.every(f=>record(f)&&Number.isInteger(f.dia)&&Number(f.dia)>=1&&Number(f.dia)<=7&&typeof f.apertura==="string"&&typeof f.cierre==="string"&&Number.isSafeInteger(f.capacidadPedidos)&&Number(f.capacidadPedidos)>=0&&typeof f.habilitada==="boolean"))&&Array.isArray(s.dias)&&s.dias.every(d=>record(d)&&Number.isInteger(d.dia)&&Number(d.dia)>=1&&Number(d.dia)<=7&&(d.habilitado===null||typeof d.habilitado==="boolean")&&["apertura","cierre"].every(k=>d[k]===null||typeof d[k]==="string")));
}
export function isConfigurationCopy(v:unknown):v is ConfigurationCopy{return record(v)&&["USO_COMO_BASE","ROLLBACK"].includes(String(v.tipo))&&typeof v.origen==="string"&&Number.isSafeInteger(v.numeroOrigen)&&typeof v.creadaEn==="string"&&Array.isArray(v.fasesConfirmadas)&&v.fasesConfirmadas.every(n=>Number.isInteger(n)&&Number(n)>=2&&Number(n)<=6)&&Array.isArray(v.barridoInicial)&&v.barridoInicial.every(h=>record(h)&&["nivel","codigo","area","mensaje"].every(k=>typeof h[k]==="string")&&Number.isInteger(h.fase)&&(h.sucursal===null||typeof h.sucursal==="string"));}
export function isConfigurationDraft(value:unknown):value is ConfigurationDraft {
  return record(value)&&(value.copia===null||isConfigurationCopy(value.copia))&&["codigoPublico","creadaEn","actualizadaEn","actor"].every(key=>typeof value[key]==="string")&&
    Number.isSafeInteger(value.numero)&&Number.isSafeInteger(value.version)&&Number(value.numero)>0&&Number(value.version)>0&&["EN_PREPARACION","PROGRAMADA","CANCELADA","ACTIVA","HISTORICA"].includes(String(value.estado))&&
    ["canceladaEn","motivoCancelacion","cancelador"].every(key=>value[key]===null||typeof value[key]==="string")&&
    (value.modelo===null||Object.hasOwn(modelLabels,String(value.modelo)))&&(value.criterio===null||Object.hasOwn(criterionLabels,String(value.criterio)))&&(value.pagos===null||isPaymentConfiguration(value.pagos))&&isResourceConfiguration(value.recursos)&&isDeliveryConfiguration(value.entrega);
}
export function isConfigurationVersion(v:unknown):v is ConfigurationVersion{return record(v)&&isConfigurationDraft(v.configuracion)&&["ACTIVA","HISTORICA"].includes(v.configuracion.estado)&&["activadaEn","activador","motivo","revisionComercial"].every(k=>typeof v[k]==="string")&&["finVigencia","predecesora"].every(k=>v[k]===null||typeof v[k]==="string");}
export function isConfigurationState(value:unknown):value is ConfigurationState{return record(value)&&Array.isArray(value.intentos)&&value.intentos.every(isConfigurationAttempt)&&(value.programada===null||(isConfigurationSchedule(value.programada)&&value.programada.configuracion.estado==="PROGRAMADA"))&&(value.activa===null||(isConfigurationVersion(value.activa)&&value.activa.configuracion.estado==="ACTIVA"))&&(value.borrador===null||(isConfigurationDraft(value.borrador)&&value.borrador.estado==="EN_PREPARACION"))&&Array.isArray(value.historial)&&value.historial.every(item=>isConfigurationDraft(item)&&item.estado==="CANCELADA");}
