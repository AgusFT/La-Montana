export type OperatingModel = "MANUAL" | "CONDICIONAL";
export type ApprovalCriterion = "PAGO_PREVIO" | "SENA" | "MONTO_TOTAL";
export type PaymentMethod = "TRANSFERENCIA" | "EFECTIVO";
export type DepositCondition = "SIEMPRE" | "DESDE_CARILLAS" | "DESDE_MONTO" | "SUPERAR_UMBRAL_APROBACION";
export type PaymentConfiguration = {
  medios: PaymentMethod[]; instruccionesTransferencia: string | null; vigenciaCotizacionMinutos: number;
  exigirSena: boolean; condicionSena: DepositCondition | null; umbralSena: string | null;
  tipoSena: "PORCENTAJE" | "FIJA" | null; valorSena: string | null; umbralAprobacion: string | null;
};
export type ConfigurationDraft = {
  codigoPublico: string; numero: number; version: number; estado: "EN_PREPARACION" | "CANCELADA";
  modelo: OperatingModel | null; criterio: ApprovalCriterion | null;
  creadaEn: string; actualizadaEn: string; actor: string;
  canceladaEn: string | null; motivoCancelacion: string | null; cancelador: string | null;
  pagos: PaymentConfiguration | null;
};
export type ConfigurationState = { borrador: ConfigurationDraft | null; historial: ConfigurationDraft[] };
export const modelLabels: Record<OperatingModel,string> = {MANUAL:"Control manual",CONDICIONAL:"Control condicional"};
export const criterionLabels: Record<ApprovalCriterion,string> = {PAGO_PREVIO:"Pago previo total",SENA:"Pago de seña",MONTO_TOTAL:"Monto total del pedido"};
function record(value:unknown):value is Record<string,unknown>{return !!value&&typeof value==="object";}
export function isPaymentConfiguration(value:unknown):value is PaymentConfiguration {
  return record(value)&&Array.isArray(value.medios)&&value.medios.length>0&&value.medios.every(item=>["TRANSFERENCIA","EFECTIVO"].includes(String(item)))&&
    (value.instruccionesTransferencia===null||typeof value.instruccionesTransferencia==="string")&&Number.isSafeInteger(value.vigenciaCotizacionMinutos)&&Number(value.vigenciaCotizacionMinutos)>0&&typeof value.exigirSena==="boolean"&&
    (value.condicionSena===null||["SIEMPRE","DESDE_CARILLAS","DESDE_MONTO","SUPERAR_UMBRAL_APROBACION"].includes(String(value.condicionSena)))&&
    (value.tipoSena===null||["PORCENTAJE","FIJA"].includes(String(value.tipoSena)))&&["umbralSena","valorSena","umbralAprobacion"].every(key=>value[key]===null||typeof value[key]==="string");
}
export function isConfigurationDraft(value:unknown):value is ConfigurationDraft {
  return record(value)&&["codigoPublico","creadaEn","actualizadaEn","actor"].every(key=>typeof value[key]==="string")&&
    Number.isSafeInteger(value.numero)&&Number.isSafeInteger(value.version)&&Number(value.numero)>0&&Number(value.version)>0&&["EN_PREPARACION","CANCELADA"].includes(String(value.estado))&&
    ["canceladaEn","motivoCancelacion","cancelador"].every(key=>value[key]===null||typeof value[key]==="string")&&
    (value.modelo===null||Object.hasOwn(modelLabels,String(value.modelo)))&&(value.criterio===null||Object.hasOwn(criterionLabels,String(value.criterio)))&&(value.pagos===null||isPaymentConfiguration(value.pagos));
}
export function isConfigurationState(value:unknown):value is ConfigurationState{return record(value)&&(value.borrador===null||(isConfigurationDraft(value.borrador)&&value.borrador.estado==="EN_PREPARACION"))&&Array.isArray(value.historial)&&value.historial.every(item=>isConfigurationDraft(item)&&item.estado==="CANCELADA");}
