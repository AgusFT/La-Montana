import type {Order} from './order-types';
import type {Permission} from './organization-types';
import type {Management} from '@/components/order-management';
import type {Production} from '@/components/order-production';
import type {Delivery} from '@/components/order-delivery';

export type OperationPanel='pedido-resumen'|'pedido-revision'|'pedido-correcciones'|'pedido-produccion'|'pedido-entrega'|'pedido-reprogramacion'|'pedido-pdf'|'pedido-pagos';
export type OperationTask={title:string;description:string;panel:OperationPanel;waiting:boolean};
export type OperationFacts={review:Management|null;production:Production|null;delivery:Delivery|null};

export const operationStages=['Revisión','Preparación','Impresión y calidad','Entrega','Cierre'];
export function operationStage(order:Order,production:Production|null){
 if(['CERRADO','ENTREGADO'].includes(order.estado))return 4;
 if(order.estado==='LISTO_PARA_ENTREGA')return 3;
 if(order.estado==='EN_PRODUCCION'){
  // Do not jump backwards while the matching snapshot is loading.
  if(production?.version!==order.version)return 2;
  const pendingPreparation=production.items.some(i=>{const j=i.trabajos.at(-1);return !j||['ERROR','CANCELADO'].includes(j.estado)||j.calidad&&j.calidad.resultado!=='APROBADO';});
  const printing=production.items.some(i=>{const j=i.trabajos.at(-1);return j?.estado==='IMPRIMIENDO'||j?.estado==='COMPLETADO'&&!j.calidad;});
  return pendingPreparation&&!printing?1:2;
 }
 return order.estado==='APROBADO'?1:0;
}

/** Prioritize this person's work; actual buttons still use the server's action flags. */
export function operationTask(order:Order,permissions:Permission[],facts:OperationFacts):OperationTask{
 const has=(p:Permission)=>permissions.includes(p),finance=has('ACREDITAR_PAGO')||has('REGISTRAR_COBRO')||has('REGISTRAR_DEVOLUCION');
 const review=facts.review?.version===order.version?facts.review:null;
 const production=facts.production?.version===order.version?facts.production:null;
 const delivery=facts.delivery?.version===order.version?facts.delivery:null;
 const task=(title:string,description:string,panel:OperationPanel,waiting=false)=>({title,description,panel,waiting});
 const advance=!!order.cobro&&!['PAGO_ACREDITADO','SIN_ANTICIPO'].includes(order.cobro.estado);
 if(['CANCELADO','RECHAZADO','CERRADO'].includes(order.estado))return task(order.estado==='CERRADO'?'Pedido cerrado':'Pedido finalizado','Consultá el historial, los archivos y los movimientos conservados.','pedido-resumen',true);
 if(order.estado==='CORRECCION_SOLICITADA')return task('Esperando la respuesta del cliente','La imprenta solicitó una corrección. Consultá el mensaje y los archivos; el trabajo continúa cuando el cliente responda.','pedido-correcciones',true);
 if(order.estado==='PENDIENTE_REVISION'){
  if(has('GESTIONAR_PEDIDOS')){
   const approval=review?.acciones.find(a=>a.accion==='APROBAR');
   if(approval?.habilitada)return task('Revisar y aprobar el pedido','Comprobá el trabajo y confirmá la aprobación para habilitar la preparación de producción.','pedido-revision');
   return task(advance?'Falta resolver el pago requerido':'Revisar el pedido',approval?.motivo??'Comprobando las condiciones del motor para aprobar.','pedido-revision',!!review);
  }
  if(advance){
   if(finance)return task('Resolver el pago pendiente','Comprobá el anticipo y los movimientos informados. Registrar o informar un pago no equivale a aprobar el trabajo.','pedido-pagos');
   return task('Esperando la condición de pago','Caja debe verificar el pago necesario antes de que el pedido pueda avanzar. Podés consultar el trabajo mientras tanto.','pedido-resumen',true);
  }
  return task('Esperando revisión y aprobación','El responsable de pedidos debe revisar el trabajo. Tu perfil podrá intervenir cuando llegue su etapa.','pedido-resumen',true);
 }
 if(['APROBADO','EN_PRODUCCION'].includes(order.estado)){
  if(has('CONTROLAR_CALIDAD')&&production?.puedeControlarCalidad&&production.items.some(i=>{const j=i.trabajos.at(-1);return j?.estado==='COMPLETADO'&&!j.calidad;}))return task('Controlar la calidad del trabajo','Hay impresiones terminadas que necesitan inspección. Registrá el resultado de cada PDF.','pedido-produccion');
  if(has('GESTIONAR_PRODUCCION')&&production?.puedeProducir){
   if(production.items.some(i=>i.puedeIniciar))return task('Preparar e iniciar la impresión','Usá el PDF confirmado y sus opciones. Elegí el recurso y registrá el inicio manual.','pedido-produccion');
   if(production.items.some(i=>i.trabajos.at(-1)?.estado==='IMPRIMIENDO'))return task(production.puedeControlarCalidad&&has('CONTROLAR_CALIDAD')?'Impresión y control de calidad':'Impresión en curso',production.puedeControlarCalidad&&has('CONTROLAR_CALIDAD')?'Cuando termine la impresión, inspeccioná el trabajo y registrá su resultado en una sola ventana.':'Cuando termine la impresión, registrá su finalización. El personal de calidad hará la inspección.','pedido-produccion');
  }
  if(finance&&production?.cobertura.some(c=>Number(c.pendiente)>0))return task('Completar el anticipo para producir','Falta dinero acreditado y aplicado por los medios admitidos. Consultá los pagos antes de continuar.','pedido-pagos');
  if(has('GESTIONAR_PRODUCCION')||has('CONTROLAR_CALIDAD'))return task('Estado de producción',production?.bloqueos[0]??'Comprobando los trabajos y los controles disponibles.','pedido-produccion',!!production);
  return task('Esperando producción y calidad','El equipo de producción debe completar las impresiones y sus controles antes de preparar la entrega.','pedido-resumen',true);
 }
 if(order.estado==='LISTO_PARA_ENTREGA'){
  if(has('GESTIONAR_ENTREGAS')&&delivery?.puedeGestionar){
   const state=delivery.estadoLogistico;
   const local=order.reserva?.modalidad==='RETIRO_SUCURSAL'&&!order.retiroConTraslado;
   if(local)return task(`Disponible para retirar en ${order.oferta?.sucursal?.nombre??'la sucursal'}`,'No requiere despacho ni viaje. Cuando llegue la persona, ingresá su código y confirmá la entrega; el sistema comprobará el saldo.','pedido-entrega');
   if(state==='PREPARADO')return task('Despachar · poner en viaje','Confirmá la salida hacia el destino acordado. Se generará el código de entrega y el cliente podrá verlo desde su pedido.','pedido-entrega');
   if(state==='EN_VIAJE')return task('Registrar llegada al destino','El pedido ya está en viaje. Cuando llegue a la sucursal o punto acordado, registrá la llegada para habilitar el retiro.','pedido-entrega');
   if(state==='DISPONIBLE_PUNTO'||state==='LISTO_RETIRO')return task('Disponible para retirar','Ingresá el código presentado y confirmá la entrega; el sistema comprobará el saldo.','pedido-entrega');
   return task('Preparar para el despacho','Confirmá que el pedido completo está preparado. Después aparecerá la acción “Despachar · poner en viaje”.','pedido-entrega');
  }
  if(finance&&delivery&&Number(delivery.saldo)>0)return task('Cobrar el saldo antes de entregar','El trabajo está listo y tiene un saldo pendiente. Registrá y aplicá el dinero realmente recibido.','pedido-pagos');
  if(has('GESTIONAR_ENTREGAS'))return task('Entrega del pedido','Comprobando el recorrido y las acciones de entrega disponibles.','pedido-entrega',!!delivery);
  return task('Esperando la entrega','El trabajo está listo. El personal autorizado de entregas continúa con el recorrido y la recepción.','pedido-resumen',true);
 }
 if(order.estado==='ENTREGADO'){
  if(has('CERRAR_PEDIDOS'))return task('Comprobar y cerrar el pedido','La entrega está registrada. Verificá la conciliación y resolvé los requisitos pendientes antes del cierre.','pedido-entrega',!delivery?.puedeCerrar);
  if(finance&&delivery&&Number(delivery.saldo)>0)return task('Revisar la conciliación del pedido','La entrega figura registrada y queda un saldo por resolver. Comprobá los movimientos reales.','pedido-pagos');
  return task('Esperando cierre administrativo','La entrega está registrada. El responsable debe completar la conciliación y el cierre.','pedido-resumen',true);
 }
 return task('Comprobar el estado del pedido','Este estado necesita una consulta actualizada antes de indicar una acción.','pedido-resumen',true);
}
