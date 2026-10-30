import {logisticsState,orderEventLabel,orderState,type OrderSummary} from './order-types';
import type {Production} from '@/components/order-production';

const situations:Record<string,{label:string;tone:string}>={
 REIMPRESION_REQUERIDA:{label:'Requiere reimpresión',tone:'danger'},
 INCIDENCIA_PRODUCCION:{label:'Incidencia de impresión',tone:'danger'},
 PREPARACION_PENDIENTE:{label:'Falta preparar trabajos',tone:'warning'},
 CALIDAD_PENDIENTE:{label:'Confirmar impresión y calidad',tone:'warning'},
 PENDIENTE_REVISION:{label:'Pendiente de revisión',tone:'warning'},
 CORRECCION_SOLICITADA:{label:'Esperando corrección',tone:'warning'},
 APROBADO:{label:'Aprobado · preparar impresión',tone:'success'},
 PENDIENTE_PREPARACION:{label:'Calidad aprobada · preparar despacho',tone:'warning'},
 PREPARADO:{label:'Preparado para despachar',tone:'success'},
 EN_VIAJE:{label:'En viaje',tone:'info'},
 LISTO_RETIRO:{label:'Disponible para retirar en sucursal',tone:'success'},
 DISPONIBLE_PUNTO:{label:'Disponible en destino de retiro',tone:'success'},
 ENTREGADO:{label:'Entregado · pendiente de cierre',tone:'success'},
 CERRADO:{label:'Cerrado',tone:'neutral'},CANCELADO:{label:'Cancelado',tone:'neutral'},RECHAZADO:{label:'Rechazado',tone:'danger'},
};
export function operationalStatus(p:Pick<OrderSummary,'estado'|'estadoLogistico'|'modalidad'|'situacionOperativa'>){
 const code=p.situacionOperativa??(p.estado==='LISTO_PARA_ENTREGA'?p.estadoLogistico:p.estado);
 return situations[code]??{label:p.estado==='LISTO_PARA_ENTREGA'?logisticsState(p.estadoLogistico):orderState(p.estado,p.modalidad),tone:'info'};
}
export function recentMovementLabel(action:string|null|undefined,mode:OrderSummary['modalidad']){
 return ({CONTROLAR_CALIDAD_APROBADO:'Calidad aprobada',CONTROLAR_CALIDAD_REIMPRESION_REQUERIDA:'Calidad: requiere reimpresión',CONTROLAR_CALIDAD_INCIDENCIA:'Incidencia en calidad',ERROR_TRABAJO:'Error de impresión',CANCELAR_TRABAJO:'Trabajo de impresión cancelado',REVISAR:'Revisión registrada',APROBAR:'Pedido aprobado',CONFIRMAR:'Pedido confirmado',ENTREGAR:'Entregado al cliente',CERRAR:'Pedido cerrado',CANCELAR:'Pedido cancelado',RECHAZAR:'Pedido rechazado',SOLICITAR_CORRECCION:'Corrección solicitada',RESPONDER_CORRECCION:'Respuesta del cliente recibida',ACEPTAR_REPROGRAMACION:'Nueva fecha acordada'} as Record<string,string>)[action??'']??(action?orderEventLabel({estado:'Movimiento del pedido',accion:action},mode):'Pedido confirmado');
}
export function productionSituation(production:Production){
 const jobs=production.items.map(i=>i.trabajos.at(-1));
 const rejected=jobs.filter(j=>j?.calidad?.resultado==='REIMPRESION_REQUERIDA');
 const incidents=jobs.filter(j=>j&&(['ERROR','CANCELADO'].includes(j.estado)||j.calidad?.resultado==='INCIDENCIA'));
 const inspection=jobs.filter(j=>j&&!j.calidad&&['IMPRIMIENDO','COMPLETADO'].includes(j.estado));
 const latest=(dates:(string|null|undefined)[])=>dates.filter((d):d is string=>!!d).sort().at(-1);
 if(rejected.length)return {situacionOperativa:'REIMPRESION_REQUERIDA',situacionDesde:latest(rejected.map(j=>j?.calidad?.fecha))};
 if(incidents.length)return {situacionOperativa:'INCIDENCIA_PRODUCCION',situacionDesde:latest(incidents.map(j=>j?.calidad?.fecha??j?.fin))};
 if(jobs.some(j=>!j))return {situacionOperativa:'PREPARACION_PENDIENTE'};
 if(inspection.length)return {situacionOperativa:'CALIDAD_PENDIENTE',situacionDesde:latest(inspection.map(j=>j?.fin??j?.inicio))};
 return {situacionOperativa:'EN_PRODUCCION'};
}
