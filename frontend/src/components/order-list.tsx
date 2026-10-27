//#region ENCABEZADO · src/components/order-list.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/order-list.tsx
 * ========================================================================
 * FUNCIÓN
 * Consulta y presenta listas paginadas de pedidos con sus estados y enlaces al detalle, para
 * cliente, sucursal autorizada o administración global.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] OrderListView({branch,initialState=""}: {branch?:string;initialState?:string})
 *   Componente de interfaz.
 * - [async] OrderListView :: load(page = 0)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useRef,useState} from "react";
import {quoteRead,money,date,modeLabels} from "@/lib/quote-types";
import {isOrderList,orderStates,orderState,logisticsState,type OrderList} from "@/lib/order-types";
import "@/app/client.css";
export function OrderListView({branch,globalAdmin=false,initialState=""}:{branch?:string;globalAdmin?:boolean;initialState?:string}){
 const requestId=useRef(0);
 const[state,setState]=useState(initialState);
 useEffect(()=>setState(initialState),[initialState]);
 const[data,setData]=useState<OrderList|null>(null),[error,setError]=useState(""),[busy,setBusy]=useState(false);
 async function load(page=0){const current=++requestId.current;setBusy(true);setError("");setData(null);try{const value=await quoteRead(`${globalAdmin?"/api/admin/pedidos":branch?`/api/operacion/sucursales/${branch}/pedidos`:"/api/cliente/pedidos"}?pagina=${page}${state?`&estado=${state}`:""}`,isOrderList);if(current===requestId.current)setData(value);}catch(e){if(current===requestId.current)setError(e instanceof Error?e.message:"No pudimos consultar los pedidos.");}finally{if(current===requestId.current)setBusy(false);}}
 useEffect(()=>{void load();return()=>{requestId.current++;};},[branch,globalAdmin,state]);
 return <section className="client-card"><h2>{globalAdmin?"Todos los pedidos de la imprenta":branch?"Pedidos de esta sucursal":"Mis pedidos"}</h2><p>Pedidos confirmados con sus archivos y reserva. La aprobación, la producción y la entrega son pasos separados.</p>{busy&&<p role="status">Consultando pedidos…</p>}{error&&<p role="alert" className="client-warning">{error}</p>}<div className="client-actions"><label>Filtrar por estado <select aria-label="Filtrar por estado" value={state} disabled={busy} onChange={e=>setState(e.target.value)}><option value="">Todos los pedidos</option>{orderStates.map(s=><option key={s} value={s}>{orderState(s)}</option>)}</select></label><button className="client-button secondary" disabled={busy} onClick={()=>void load(data?.pagina??0)}>Actualizar pedidos</button></div>{data?.total===0&&<p className="client-empty">No hay pedidos con este filtro.</p>}{!!data?.total&&<><div className="client-table-wrap"><table className="client-table"><thead><tr><th>Pedido</th><th>Estado</th><th>{globalAdmin||branch?"Destino de entrega":"Sucursal"}</th><th>Entrega</th><th>Confirmado</th><th>Total</th><th>Acción</th></tr></thead><tbody>{data.elementos.map(p=><tr key={p.codigoPublico}><td>PED-{p.numero}</td><td><span className="client-state">{p.estado==="LISTO_PARA_ENTREGA"?logisticsState(p.estadoLogistico):orderState(p.estado,p.modalidad)}</span></td><td>{globalAdmin||branch?p.destino??"Consultar en el pedido":p.sucursal}</td><td>{p.retiroConTraslado?"Retiro con traslado":modeLabels[p.modalidad]}</td><td>{date(p.confirmadaEn)}</td><td>{money(p.total)}</td><td><a className="client-button secondary" href={`/${branch||globalAdmin?"operacion":"cliente"}/pedidos/${p.codigoPublico}`}>Ver PED-{p.numero}</a></td></tr>)}</tbody></table></div><div className="client-actions"><button className="client-button secondary" disabled={busy||data.pagina===0} onClick={()=>void load(data.pagina-1)}>Anteriores</button><span>Página {data.pagina+1} · {data.total} pedidos</span><button className="client-button secondary" disabled={busy||(data.pagina+1)*25>=data.total} onClick={()=>void load(data.pagina+1)}>Siguientes</button></div></>}</section>;
}
