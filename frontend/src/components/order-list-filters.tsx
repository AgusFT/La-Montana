"use client";
import {useEffect,useState} from 'react';
import {operationQueues} from '@/lib/operation-queues';
import {orderState,orderStates} from '@/lib/order-types';
import '@/app/order-list-filters.css';

const isQuick=(state:string)=>operationQueues.some(q=>q.state===state);
export function OrderListFilters({state,busy,onChange,onRefresh}:{state:string;busy:boolean;onChange:(state:string)=>void;onRefresh:()=>void}){
 const [advanced,setAdvanced]=useState(!!state&&!isQuick(state));
 useEffect(()=>{if(state&&!isQuick(state))setAdvanced(true);},[state]);
 function select(value:string){setAdvanced(false);onChange(value);}
 return <div className="order-list-filters"><div className="order-quick-filters" role="group" aria-label="Filtros rápidos de pedidos">{operationQueues.map(q=><button type="button" key={q.state} className={`order-quick-filter ${q.tone}`} aria-pressed={state===q.state} disabled={busy} onClick={()=>select(q.state)}><span aria-hidden="true">{q.symbol}</span>{q.title}</button>)}</div><div className="order-filter-tools"><p aria-live="polite">Mostrando: <strong>{operationQueues.find(q=>q.state===state)?.title??(state?orderState(state):'Todos los pedidos')}</strong></p>{state&&<button type="button" className="client-text-action" disabled={busy} onClick={()=>select('')}>Ver todos los pedidos</button>}<button type="button" className="client-button secondary" disabled={busy} onClick={onRefresh}>Actualizar pedidos</button></div><details className="order-advanced-filters" open={advanced} onToggle={e=>setAdvanced(e.currentTarget.open)}><summary>Filtros avanzados</summary><label>Filtrar por estado<select aria-label="Filtrar por estado" value={state} disabled={busy} onChange={e=>onChange(e.target.value)}><option value="">Todos los pedidos</option>{orderStates.map(s=><option key={s} value={s}>{orderState(s)}</option>)}</select></label></details></div>;
}
