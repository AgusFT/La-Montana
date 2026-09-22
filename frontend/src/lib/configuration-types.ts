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
export type BranchSchedule = {sucursal:string;zonaHoraria:string;dias:OperatingDay[]};
export type PointSlot = {dia:number;apertura:string;cierre:string;capacidadPedidos:number;habilitada:boolean};
export type PointOrigin = {sucursal:string;habilitado:boolean;costo:string;franjas:PointSlot[]};
export type DeliveryPoint = {codigoPublico:string;codigo:string;nombre:string;calle:string;numero:string;localidad:string;provincia:string;codigoPostal:string;referencias:string|null;zonaHoraria:string;sucursales:PointOrigin[];disponibilidadOperativa:"PENDIENTE"};
export type DeliveryConfiguration = {puntos:DeliveryPoint[];preparacionHoras:string|null;trasladoHoras:string|null;modalidades:DeliveryMode[];horariosPorSucursal:BranchSchedule[]};
export type ConfigurationDraft = {
  codigoPublico: string; numero: number; version: number; estado: "EN_PREPARACION" | "CANCELADA";
  modelo: OperatingModel | null; criterio: ApprovalCriterion | null;
  creadaEn: string; actualizadaEn: string; actor: string;
  canceladaEn: string | null; motivoCancelacion: string | null; cancelador: string | null;
  pagos: PaymentConfiguration | null; recursos: ResourceConfiguration; entrega: DeliveryConfiguration;
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
  return record(value)&&["codigoPublico","codigo","nombre","calle","numero","localidad","provincia","codigoPostal","zonaHoraria"].every(k=>typeof value[k]==="string")&&(value.referencias===null||typeof value.referencias==="string")&&value.disponibilidadOperativa==="PENDIENTE"&&Array.isArray(value.sucursales)&&value.sucursales.every(s=>record(s)&&typeof s.sucursal==="string"&&typeof s.habilitado==="boolean"&&typeof s.costo==="string"&&Array.isArray(s.franjas)&&s.franjas.every(f=>record(f)&&Number.isInteger(f.dia)&&Number(f.dia)>=1&&Number(f.dia)<=7&&typeof f.apertura==="string"&&typeof f.cierre==="string"&&Number.isSafeInteger(f.capacidadPedidos)&&Number(f.capacidadPedidos)>=0&&typeof f.habilitada==="boolean"));
}
export function isDeliveryConfiguration(value:unknown):value is DeliveryConfiguration {
  return record(value) && Array.isArray(value.puntos) && value.puntos.every(isDeliveryPoint) && ["preparacionHoras","trasladoHoras"].every(k=>value[k]===null||typeof value[k]==="string") &&
    Array.isArray(value.modalidades) && value.modalidades.every(m=>["RETIRO_SUCURSAL","RETIRO_PUNTO_ENTREGA","ENVIO_DOMICILIO"].includes(String(m))) &&
    Array.isArray(value.horariosPorSucursal) && value.horariosPorSucursal.every(s=>record(s)&&typeof s.sucursal==="string"&&typeof s.zonaHoraria==="string"&&Array.isArray(s.dias)&&s.dias.every(d=>record(d)&&Number.isInteger(d.dia)&&Number(d.dia)>=1&&Number(d.dia)<=7&&(d.habilitado===null||typeof d.habilitado==="boolean")&&["apertura","cierre"].every(k=>d[k]===null||typeof d[k]==="string")));
}
export function isConfigurationDraft(value:unknown):value is ConfigurationDraft {
  return record(value)&&["codigoPublico","creadaEn","actualizadaEn","actor"].every(key=>typeof value[key]==="string")&&
    Number.isSafeInteger(value.numero)&&Number.isSafeInteger(value.version)&&Number(value.numero)>0&&Number(value.version)>0&&["EN_PREPARACION","CANCELADA"].includes(String(value.estado))&&
    ["canceladaEn","motivoCancelacion","cancelador"].every(key=>value[key]===null||typeof value[key]==="string")&&
    (value.modelo===null||Object.hasOwn(modelLabels,String(value.modelo)))&&(value.criterio===null||Object.hasOwn(criterionLabels,String(value.criterio)))&&(value.pagos===null||isPaymentConfiguration(value.pagos))&&isResourceConfiguration(value.recursos)&&isDeliveryConfiguration(value.entrega);
}
export function isConfigurationState(value:unknown):value is ConfigurationState{return record(value)&&(value.borrador===null||(isConfigurationDraft(value.borrador)&&value.borrador.estado==="EN_PREPARACION"))&&Array.isArray(value.historial)&&value.historial.every(item=>isConfigurationDraft(item)&&item.estado==="CANCELADA");}
