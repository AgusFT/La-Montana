//#region ENCABEZADO · src/lib/order-tracking.ts
/*
 * FUNCIÓN: Traduce el estado general y logístico del pedido a un recorrido público.
 * Usa las condiciones guardadas del pedido; no consulta el motor vigente ni calcula porcentajes.
 * FUNCIONES: lastEvent, orderTrackingLabel, orderTracking.
 * TIPOS: TrackingOrder, TrackingStep, OrderTracking.
 * VALORES: labels, knownOrders, shipmentStates.
 */
//#endregion

import {orderState,logisticsState,type Order} from "./order-types";

export type TrackingOrder=Pick<Order,"estado"|"estadoLogistico"|"confirmadaEn"|"aprobadaEn"|"finalizadaEn"|"condiciones"|"reserva"|"historial">;
export type TrackingStep={id:string;label:string;state:"complete"|"current"|"pending"|"recorded"|"stopped";date:string|null};
export type OrderTracking={label:string;steps:TrackingStep[];notice:string|null;unavailable:boolean};
const labels:Record<string,string>={received:"Pedido recibido",review:"Revisión humana",approved:"Aprobado",production:"Producción y calidad",ready:"Listo para retirar",preparing:"Preparación del envío",prepared:"Preparado para salir",travel:"En viaje",point:"Disponible en el punto",delivered:"Entregado",closed:"Cerrado"};
const knownOrders=["PENDIENTE_REVISION","CORRECCION_SOLICITADA","APROBADO","EN_PRODUCCION","LISTO_PARA_ENTREGA","ENTREGADO","CERRADO","CANCELADO","RECHAZADO"];
const shipmentStates:Record<string,string>={PENDIENTE_PREPARACION:"preparing",PREPARADO:"prepared",EN_VIAJE:"travel",DISPONIBLE_PUNTO:"point"};

function lastEvent(order:TrackingOrder,state:string){return order.historial.filter(e=>e.estado===state).slice(-1)[0]?.fecha??null;}

export function orderTrackingLabel(order:Pick<Order,"estado"|"estadoLogistico"|"reserva">){
  const mode=order.reserva.modalidad;
  const compatible=mode==="RETIRO_SUCURSAL"?order.estadoLogistico==="LISTO_RETIRO":
    ["RETIRO_PUNTO_ENTREGA","ENVIO_DOMICILIO"].includes(mode)&&Object.hasOwn(shipmentStates,order.estadoLogistico)&&(order.estadoLogistico!=="DISPONIBLE_PUNTO"||mode==="RETIRO_PUNTO_ENTREGA");
  if(order.estado==="LISTO_PARA_ENTREGA"&&compatible)return logisticsState(order.estadoLogistico);
  return orderState(order.estado,order.reserva.modalidad);
}

export function orderTracking(order:TrackingOrder):OrderTracking{
  const label=orderTrackingLabel(order);
  const mode=order.reserva.modalidad;
  if(!knownOrders.includes(order.estado)||!["RETIRO_SUCURSAL","RETIRO_PUNTO_ENTREGA","ENVIO_DOMICILIO"].includes(mode))return {label,steps:[],notice:"El seguimiento no está disponible para este estado. Actualizá el pedido para consultar su situación.",unavailable:true};
  const review=order.condiciones.revisionHumana||order.historial.some(e=>["PENDIENTE_REVISION","CORRECCION_SOLICITADA"].includes(e.estado))||["PENDIENTE_REVISION","CORRECCION_SOLICITADA"].includes(order.estado);
  const correction=order.estado==="CORRECCION_SOLICITADA";
  const ids=["received",...(review?["review"]:[]),"approved","production",...(mode==="RETIRO_SUCURSAL"?["ready"]:["preparing","prepared","travel",...(mode==="RETIRO_PUNTO_ENTREGA"?["point"]:[])]),"delivered","closed"];
  const dates:Record<string,string|null>={received:order.confirmadaEn,review:lastEvent(order,"PENDIENTE_REVISION"),approved:lastEvent(order,"APROBADO")??order.aprobadaEn,production:lastEvent(order,"EN_PRODUCCION"),ready:lastEvent(order,"LISTO_PARA_ENTREGA"),preparing:lastEvent(order,"LISTO_PARA_ENTREGA"),delivered:lastEvent(order,"ENTREGADO"),closed:lastEvent(order,"CERRADO")??(order.estado==="CERRADO"?order.finalizadaEn:null)};
  if(correction){dates.review=lastEvent(order,"CORRECCION_SOLICITADA");dates.approved=null;}
  if(["CANCELADO","RECHAZADO"].includes(order.estado)){
    // El historial prueba qué etapas se alcanzaron; una interrupción no las da por terminadas.
    const steps:TrackingStep[]=ids.filter(id=>id==="received"||dates[id]!==null&&dates[id]!==undefined).map(id=>({id,label:labels[id],state:"recorded",date:dates[id]??null}));
    steps.push({id:"stopped",label,state:"stopped",date:lastEvent(order,order.estado)??order.finalizadaEn});
    return {label,steps,notice:"El recorrido se detuvo. Se conservan los eventos registrados y las condiciones del pedido.",unavailable:false};
  }
  let current: string;
  switch(order.estado){
    case "PENDIENTE_REVISION":case "CORRECCION_SOLICITADA":current="review";break;
    case "APROBADO":current="approved";break;
    case "EN_PRODUCCION":current="production";break;
    case "ENTREGADO":current="delivered";break;
    case "CERRADO":current="closed";break;
    default:
      current=mode==="RETIRO_SUCURSAL"&&order.estadoLogistico==="LISTO_RETIRO"?"ready":mode!=="RETIRO_SUCURSAL"&&Object.hasOwn(shipmentStates,order.estadoLogistico)?shipmentStates[order.estadoLogistico]:"";
      if(!current||current==="point"&&mode!=="RETIRO_PUNTO_ENTREGA")return {label,steps:[],notice:"Falta un estado logístico compatible para mostrar el recorrido. Actualizá el pedido; no se infiere un viaje ni una entrega.",unavailable:true};
  }
  const index=ids.indexOf(current);
  const steps:TrackingStep[]=ids.map((id,i)=>({id,label:id==="review"&&correction?"Corrección solicitada":labels[id],state:i<index?"complete":i===index?"current":"pending",date:i<=index?dates[id]??null:null}));
  return {label,steps,notice:correction?"La imprenta solicitó una corrección. Revisá lo solicitado para poder continuar.":current==="production"?"Producción y control de calidad comparten esta etapa. El pedido estará listo cuando el personal registre los controles requeridos.":null,unavailable:false};
}
