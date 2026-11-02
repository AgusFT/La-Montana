import type {Order} from '@/lib/order-types';
import type {Production} from './order-production';
import {productionWorkProgress} from '@/lib/production-work-progress';
import '@/app/operation-order.css';
import '@/app/order-operations-list.css';
import '@/app/production-work-progress.css';

export function OrderWorkOverview({order,production,selected,onSelect,disabled=false}:{order:Order;production:Production|null;selected:string;onSelect:(id:string)=>void;disabled?:boolean}){
 const progress=productionWorkProgress(order,production);
 if(progress.total<2)return null;
 return <section className="order-work-overview" aria-label="Todos los trabajos del pedido">
  <header><div><h2>{progress.complete?`Los ${progress.total} trabajos tienen calidad aprobada`:`Este pedido incluye ${progress.total} PDF para imprimir`}</h2><p>Cada PDF se imprime y se controla por separado. El pedido pasa a entrega cuando todos tienen la calidad aprobada.</p></div><div className="order-work-count" role="status">{progress.known?<><strong>{progress.approved} de {progress.total}</strong><span>{progress.complete?'Todos los trabajos aprobados':<>con calidad aprobada · {progress.pending} {progress.pending===1?'pendiente':'pendientes'}</>}</span></>:<span>Consultando el avance de los trabajos…</span>}{!disabled&&progress.known&&progress.pending>0&&['APROBADO','EN_PRODUCCION'].includes(order.estado)&&<a href="#pedido-produccion">Ir a los trabajos pendientes ↓</a>}</div></header>
  <ol className="order-work-cards">{progress.rows.map(row=><li key={row.id}><button type="button" className={`order-work-card ${row.status.tone}`} aria-pressed={selected===row.id} aria-controls="operation-selected-work" disabled={disabled} onClick={()=>onSelect(row.id)}><span className="order-work-card-number">Trabajo {row.index+1} de {progress.total}</span><strong className="order-work-card-name">{row.name}</strong><span>{row.work.trabajo.paginas} páginas × {row.work.trabajo.copias} copias · {row.work.formato} · {row.work.trabajo.color==='COLOR'?'Color':'Blanco y negro'}</span><span className={`order-status-badge ${row.status.tone}`}>{row.status.label}</span><small>{selected===row.id?'Mostrando sus detalles y documento':'Ver detalles y documento'}</small></button></li>)}</ol>
 </section>;
}
