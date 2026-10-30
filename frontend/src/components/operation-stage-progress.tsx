import type {Order} from '@/lib/order-types';
import type {Production} from './order-production';
import {operationStage,operationStages} from '@/lib/operation-task';

export function OperationStageProgress({order,production}:{order:Order;production:Production|null}){
 const stage=operationStage(order,production),ended=['CANCELADO','RECHAZADO'].includes(order.estado);
 return <nav className="operation-stages" aria-label="Etapas del trabajo">{operationStages.map((label,index)=>{const done=!ended&&(index<stage||order.estado==='CERRADO');return <span key={label} className={done?'completed':undefined} aria-current={!ended&&order.estado!=='CERRADO'&&stage===index?'step':undefined}><b aria-hidden="true">{done?'✓':index+1}</b><span>{label}{done&&<span className="operation-stage-status"> · Completada</span>}</span></span>;})}</nav>;
}
