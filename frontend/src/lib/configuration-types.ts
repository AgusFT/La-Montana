export type OperatingModel = "MANUAL" | "CONDICIONAL";
export type ApprovalCriterion = "PAGO_PREVIO" | "SENA" | "MONTO_TOTAL";
export type ConfigurationDraft = {
  codigoPublico: string; numero: number; version: number; estado: "EN_PREPARACION";
  modelo: OperatingModel | null; criterio: ApprovalCriterion | null;
  creadaEn: string; actualizadaEn: string; actor: string;
};
export type ConfigurationState = { borrador: ConfigurationDraft | null };
export const modelLabels: Record<OperatingModel,string> = {MANUAL:"Control manual",CONDICIONAL:"Control condicional"};
export const criterionLabels: Record<ApprovalCriterion,string> = {PAGO_PREVIO:"Pago previo total",SENA:"Pago de seña",MONTO_TOTAL:"Monto total del pedido"};
function record(value:unknown):value is Record<string,unknown>{return !!value&&typeof value==="object";}
export function isConfigurationDraft(value:unknown):value is ConfigurationDraft {
  return record(value)&&["codigoPublico","creadaEn","actualizadaEn","actor"].every(key=>typeof value[key]==="string")&&
    Number.isSafeInteger(value.numero)&&Number.isSafeInteger(value.version)&&Number(value.numero)>0&&Number(value.version)>0&&value.estado==="EN_PREPARACION"&&
    (value.modelo===null||Object.hasOwn(modelLabels,String(value.modelo)))&&(value.criterio===null||Object.hasOwn(criterionLabels,String(value.criterio)));
}
export function isConfigurationState(value:unknown):value is ConfigurationState{return record(value)&&(value.borrador===null||isConfigurationDraft(value.borrador));}
