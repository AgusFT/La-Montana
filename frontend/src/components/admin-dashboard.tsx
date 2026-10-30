//#region ENCABEZADO · src/components/admin-dashboard.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/admin-dashboard.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta consultas y accesos operativos del dashboard global de la imprenta, con conteos y
 * estados obtenidos desde la API.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] AdminDashboard()
 *   Componente de interfaz.
 * - AdminDashboard :: href(state = "")
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Snapshot (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - stages [const].
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useState} from "react";
import {isOrderList,logisticsState,type OrderList} from "@/lib/order-types";
import {date,money,modeLabels} from "@/lib/quote-types";
import {operationQueues} from '@/lib/operation-queues';

const stages=[
 operationQueues[0],
 {state:"CORRECCION_SOLICITADA",title:"Correcciones solicitadas",tone:"amber",symbol:"↺"},
 ...operationQueues.slice(1),
];
type Snapshot={lists:(OrderList|null)[]};
export function AdminDashboard(){
 const [refresh,setRefresh]=useState(0),[snapshot,setSnapshot]=useState<Snapshot|null>(null),[loading,setLoading]=useState(true);
 useEffect(()=>{
  const controller=new AbortController();setLoading(true);setSnapshot(null);
  void Promise.all(stages.map(async stage=>{try{const response=await fetch(`/api/admin/pedidos?estado=${stage.state}`,{cache:"no-store",signal:controller.signal});if(!response.ok)return null;const data:unknown=await response.json();return isOrderList(data)?data:null;}catch{return null;}})).then(lists=>{if(!controller.signal.aborted){setSnapshot({lists});setLoading(false);}});
  return()=>controller.abort();
 },[refresh]);
 const lists=snapshot?.lists;
 const href=(state="")=>`/administracion/pedidos${state?`?estado=${state}`:""}`;
 return <section aria-label="Resumen operativo de la imprenta">
  <div className="dashboard-toolbar"><div><strong>Todos los pedidos de la imprenta</strong><p>Las bandejas reúnen los pedidos de todos los destinos. El retiro elegido por el cliente no divide la revisión ni la producción. Los importes son valores de pedidos, no dinero cobrado.</p></div><button className="admin-button secondary" disabled={loading} onClick={()=>setRefresh(v=>v+1)}>Actualizar resumen</button></div>
   {loading&&<p role="status">Consultando todos los pedidos de la imprenta…</p>}
   {lists?.some(list=>!list)&&<p role="alert" className="admin-error">No pudimos consultar todos los estados. Actualizá el resumen; los datos no disponibles no se cuentan como cero.</p>}
   <div className="dashboard-metrics">{stages.map((stage,i)=><article key={stage.state} className={`dashboard-metric ${stage.tone}`}><span className="dashboard-symbol" aria-hidden="true">{stage.symbol}</span><div><h2>{stage.title}</h2><strong aria-label={`${stage.title}: ${lists?.[i]?.total??"no disponible"}`}>{lists?.[i]?.total??"—"}</strong><a href={href(stage.state)}>Ver todos <span aria-hidden="true">→</span></a></div></article>)}</div>
   <div className="dashboard-queues">{stages.filter(s=>s.state!=="CORRECCION_SOLICITADA").map(stage=>{const list=lists?.[stages.indexOf(stage)];return <section key={stage.state} className={`dashboard-queue ${stage.tone}`} aria-label={stage.title}><header><h2>{stage.title}</h2><a href={href(stage.state)}>Ver todos</a></header>{list?list.total>0?<><div className="dashboard-table-wrap"><table><thead><tr><th>Pedido</th><th>Confirmado</th><th>{stage.state==="LISTO_PARA_ENTREGA"?"Situación":"Entrega"}</th><th>Destino de entrega</th><th>Total</th><th>Acción</th></tr></thead><tbody>{list.elementos.slice(0,5).map(p=><tr key={p.codigoPublico}><td><strong>PED-{p.numero}</strong></td><td>{date(p.confirmadaEn)}</td><td>{stage.state==="LISTO_PARA_ENTREGA"?logisticsState(p.estadoLogistico):p.retiroConTraslado?"Retiro con traslado":modeLabels[p.modalidad]}</td><td>{p.destino??"Consultar en el pedido"}</td><td>{money(p.total)}</td><td><a href={`/operacion/pedidos/${p.codigoPublico}`}>Ver pedido</a></td></tr>)}</tbody></table></div><footer>Mostrando {Math.min(5,list.elementos.length)} de {list.total} pedidos</footer></>:<p className="dashboard-empty">No hay pedidos en este estado.</p>:<p className="dashboard-empty">{loading?"Consultando…":"No disponible. Actualizá el resumen para volver a consultar."}</p>}</section>;})}</div>
   <p className="dashboard-all"><a href={href()}>Abrir todos los pedidos de la imprenta →</a></p>
 </section>;
}
