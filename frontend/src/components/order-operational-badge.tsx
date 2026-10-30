import {date} from '@/lib/quote-types';
import {operationalStatus} from '@/lib/order-operational-status';
import type {OrderSummary} from '@/lib/order-types';
import '@/app/order-operations-list.css';
export function OrderOperationalBadge({order,showDate=true}:{order:Pick<OrderSummary,'estado'|'estadoLogistico'|'modalidad'|'situacionOperativa'|'situacionDesde'>;showDate?:boolean}){
 const status=operationalStatus(order);
 return <div className="order-operational-state"><span className={`order-status-badge ${status.tone}`}>{status.label}</span>{showDate&&order.situacionDesde&&<small>Desde {date(order.situacionDesde)}</small>}</div>;
}
