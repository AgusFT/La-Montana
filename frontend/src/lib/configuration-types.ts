export type OperatingModel = "MANUAL" | "CONDICIONAL";
export type ApprovalCriterion = "PAGO_PREVIO" | "SENA" | "MONTO_TOTAL";
export type ConfigurationDraft = {
  codigoPublico: string; numero: number; version: number; estado: "EN_PREPARACION" | "CANCELADA";
  modelo: OperatingModel | null; criterio: ApprovalCriterion | null;
  creadaEn: string; actualizadaEn: string; actor: string;
  canceladaEn: string | null; motivoCancelacion: string | null; cancelador: string | null;
};
export type ConfigurationState = { borrador: ConfigurationDraft | null; historial: ConfigurationDraft[] };
export const modelLabels: Record<OperatingModel,string> = {MANUAL:"Control manual",CONDICIONAL:"Control condicional"};
export const criterionLabels: Record<ApprovalCriterion,string> = {PAGO_PREVIO:"Pago previo total",SENA:"Pago de seña",MONTO_TOTAL:"Monto total del pedido"};
function record(value:unknown):value is Record<string,unknown>{return !!value&&typeof value==="object";}
export function isConfigurationDraft(value:unknown):value is ConfigurationDraft {
  return record(value)&&["codigoPublico","creadaEn","actualizadaEn","actor"].every(key=>typeof value[key]==="string")&&
    Number.isSafeInteger(value.numero)&&Number.isSafeInteger(value.version)&&Number(value.numero)>0&&Number(value.version)>0&&["EN_PREPARACION","CANCELADA"].includes(String(value.estado))&&
    ["canceladaEn","motivoCancelacion","cancelador"].every(key=>value[key]===null||typeof value[key]==="string")&&
    (value.modelo===null||Object.hasOwn(modelLabels,String(value.modelo)))&&(value.criterio===null||Object.hasOwn(criterionLabels,String(value.criterio)));
}
export function isConfigurationState(value:unknown):value is ConfigurationState{return record(value)&&(value.borrador===null||(isConfigurationDraft(value.borrador)&&value.borrador.estado==="EN_PREPARACION"))&&Array.isArray(value.historial)&&value.historial.every(item=>isConfigurationDraft(item)&&item.estado==="CANCELADA");}
