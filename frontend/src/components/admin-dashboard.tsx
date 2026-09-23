//#region ENCABEZADO · src/components/admin-dashboard.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/admin-dashboard.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta consultas y accesos operativos del dashboard administrativo por sucursal, con conteos y
 * estados obtenidos desde la API.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] AdminDashboard({branches}: {branches:Branch[]})
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
import {type Branch} from "@/lib/organization-types";
import {isOrderList,logisticsState,type OrderList} from "@/lib/order-types";
import {date,money,modeLabels} from "@/lib/quote-types";

const stages=[
 {state:"PENDIENTE_REVISION",title:"Pendientes de revisión",tone:"blue",symbol:"▤"},
 {state:"CORRECCION_SOLICITADA",title:"Correcciones solicitadas",tone:"amber",symbol:"↺"},
 {state:"APROBADO",title:"Aprobados",tone:"green",symbol:"✓"},
 {state:"EN_PRODUCCION",title:"En producción",tone:"blue",symbol:"⚙"},
 {state:"LISTO_PARA_ENTREGA",title:"Entregas pendientes",tone:"purple",symbol:"↗"},
];
type Snapshot={branch:string;lists:(OrderList|null)[]};
export function AdminDashboard({branches}:{branches:Branch[]}){
 const [branch,setBranch]=useState(branches[0]?.codigoPublico??""),[refresh,setRefresh]=useState(0),[snapshot,setSnapshot]=useState<Snapshot|null>(null),[loading,setLoading]=useState(branches.length>0);
 useEffect(()=>{
  if(!branch)return;const controller=new AbortController();setLoading(true);setSnapshot(null);
  void Promise.all(stages.map(async stage=>{try{const response=await fetch(`/api/operacion/sucursales/${branch}/pedidos?estado=${stage.state}`,{cache:"no-store",signal:controller.signal});if(!response.ok)return null;const data:unknown=await response.json();return isOrderList(data)?data:null;}catch{return null;}})).then(lists=>{if(!controller.signal.aborted){setSnapshot({branch,lists});setLoading(false);}});
  return()=>controller.abort();
 },[branch,refresh]);
 const lists=snapshot?.branch===branch?snapshot.lists:null;
 const href=(state="")=>`/operacion/sucursales/${branch}${state?`?estado=${state}`:""}`;
 return <section aria-label="Resumen operativo por sucursal">
  <div className="dashboard-toolbar"><label>Sucursal del resumen<select aria-label="Sucursal del resumen" value={branch} disabled={loading||!branches.length} onChange={e=>setBranch(e.target.value)}>{branches.length?branches.map(b=><option key={b.codigoPublico} value={b.codigoPublico}>{b.nombre}</option>):<option value="">Sin sucursales activas</option>}</select></label><p>Pedidos confirmados de la sucursal seleccionada. Los importes cobrados se consultan en su panel de pagos.</p>{branch&&<button className="admin-button secondary" disabled={loading} onClick={()=>setRefresh(v=>v+1)}>Actualizar resumen</button>}</div>
  {!branch?<div className="admin-info"><strong>Prepará tu primera sucursal</strong><p>Creá sus datos, empleados, catálogo y configuración para comenzar a recibir pedidos.</p><a href="/administracion/sucursales">Administrar sucursales</a></div>:<>
   {loading&&<p role="status">Consultando los pedidos de la sucursal…</p>}
   {lists?.some(list=>!list)&&<p role="alert" className="admin-error">No pudimos consultar todos los estados. Actualizá el resumen; los datos no disponibles no se cuentan como cero.</p>}
   <div className="dashboard-metrics">{stages.map((stage,i)=><article key={stage.state} className={`dashboard-metric ${stage.tone}`}><span className="dashboard-symbol" aria-hidden="true">{stage.symbol}</span><div><h2>{stage.title}</h2><strong aria-label={`${stage.title}: ${lists?.[i]?.total??"no disponible"}`}>{lists?.[i]?.total??"—"}</strong><a href={href(stage.state)}>Ver todos <span aria-hidden="true">→</span></a></div></article>)}</div>
   <div className="dashboard-queues">{stages.filter(s=>s.state!=="CORRECCION_SOLICITADA").map(stage=>{const list=lists?.[stages.indexOf(stage)];return <section key={stage.state} className={`dashboard-queue ${stage.tone}`} aria-label={stage.title}><header><h2>{stage.title}</h2><a href={href(stage.state)}>Ver todos</a></header>{list?list.total>0?<><div className="dashboard-table-wrap"><table><thead><tr><th>Pedido</th><th>Confirmado</th><th>{stage.state==="LISTO_PARA_ENTREGA"?"Situación":"Entrega"}</th><th>Total</th><th>Acción</th></tr></thead><tbody>{list.elementos.slice(0,5).map(p=><tr key={p.codigoPublico}><td><strong>PED-{p.numero}</strong></td><td>{date(p.confirmadaEn)}</td><td>{stage.state==="LISTO_PARA_ENTREGA"?logisticsState(p.estadoLogistico):modeLabels[p.modalidad]}</td><td>{money(p.total)}</td><td><a href={`/operacion/pedidos/${p.codigoPublico}`}>Ver pedido</a></td></tr>)}</tbody></table></div><footer>Mostrando {Math.min(5,list.elementos.length)} de {list.total} pedidos</footer></>:<p className="dashboard-empty">No hay pedidos en este estado.</p>:<p className="dashboard-empty">{loading?"Consultando…":"No disponible. Actualizá el resumen para volver a consultar."}</p>}</section>;})}</div>
   <p className="dashboard-all"><a href={href()}>Abrir todos los pedidos, archivos y cobros de esta sucursal →</a></p>
  </>}
 </section>;
}
