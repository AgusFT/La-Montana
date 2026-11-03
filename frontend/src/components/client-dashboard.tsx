//#region ENCABEZADO · src/components/client-dashboard.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/client-dashboard.tsx
 * ========================================================================
 * FUNCIÓN
 * Consulta y presenta el resumen de cotizaciones y pedidos del cliente y los accesos para
 * continuar su trabajo. El seguimiento combina los estados generales y logísticos del pedido.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] ClientDashboard()
 *   Componente de interfaz.
 * - [async] ClientDashboard :: load()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Dashboard (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - activeStates [const].
 * ========================================================================
 */
//#endregion

"use client";

import {useEffect,useState} from "react";
import {quoteRead,money,date,modeLabels} from "@/lib/quote-types";
import {isOrder,isOrderList,type Order,type OrderList} from "@/lib/order-types";
import {OrderProgress} from "./order-progress";
import {ClientOrderBadge} from "./client-order-badge";
import {orderTrackingLabel} from "@/lib/order-tracking";

const activeStates=["PENDIENTE_REVISION","CORRECCION_SOLICITADA","APROBADO","EN_PRODUCCION","LISTO_PARA_ENTREGA","ENTREGADO"];
type Dashboard={orders:OrderList;activeOrders:OrderList['elementos'];active:number;current:Order|null};
export function ClientDashboard(){
  const[data,setData]=useState<Dashboard|null>(null),[busy,setBusy]=useState(true),[error,setError]=useState("");
  async function load(){
    setBusy(true);setError("");
    try{
      const [orders,...groups]=await Promise.all([quoteRead("/api/cliente/pedidos",isOrderList),...activeStates.map(state=>quoteRead(`/api/cliente/pedidos?estado=${state}`,isOrderList))]);
      const activeOrders=groups.flatMap(g=>g.elementos).sort((a,b)=>Date.parse(b.confirmadaEn)-Date.parse(a.confirmadaEn)||b.numero-a.numero);
      const latest=activeOrders[0];
      const current=latest?await quoteRead(`/api/cliente/pedidos/${latest.codigoPublico}`,isOrder):null;
      setData({orders,activeOrders,active:groups.reduce((sum,g)=>sum+g.total,0),current});
    }catch(e){setData(null);setError(e instanceof Error?e.message:"No pudimos consultar tus pedidos.");}
    finally{setBusy(false);}
  }
  useEffect(()=>{void load();},[]);
  const current=data?.current;
  return <>
    <div className="client-dashboard-shortcut">
      <a className="client-button secondary" href="/cliente/pedidos">Ver mis pedidos <span aria-hidden="true">→</span></a>
    </div>
    {busy&&<p className="client-note" role="status">Consultando tus pedidos…</p>}
    {error&&<section className="client-card"><h2>Pedidos no disponibles</h2><p className="client-warning" role="alert">{error}</p><button className="client-button secondary" onClick={()=>void load()}>Volver a consultar</button></section>}
    {data&&<div className="client-dashboard">
      <div className="client-dashboard-main">
        <section className="client-current-order" aria-label="Pedido actual">
          <header><h2>Pedido actual</h2>{current&&<ClientOrderBadge order={current}/>}</header>
          {current?<><div className="client-current-info"><h3>PED-{current.numero}</h3><p>{orderTrackingLabel(current)}.</p>{current.cobro&&!["SIN_ANTICIPO","PAGO_ACREDITADO"].includes(current.cobro.estado)&&<p className="client-current-next"><strong>Próximo paso:</strong> {current.cobro.mensaje}</p>}{current.estado==="CORRECCION_SOLICITADA"&&<p>Revisá la corrección solicitada por la imprenta.</p>}<p className="client-current-date">Confirmado el {date(current.confirmadaEn)}</p><div className="client-actions"><a className="client-button" href={`/cliente/pedidos/${current.codigoPublico}`}>Ver detalle del pedido <span aria-hidden="true">›</span></a><button className="client-button" disabled={busy} onClick={()=>void load()}>Actualizar seguimiento</button></div></div><details className="client-dashboard-tracking"><summary>Ver seguimiento del pedido</summary><OrderProgress order={current} compact/></details><p className="client-current-criterion">Destacamos tu pedido en curso más reciente.{data.active>1&&<> Tenés {data.active} pedidos en curso. <a href="/cliente/pedidos">Consultar los demás</a>.</>}</p></>:<div className="client-current-empty"><h3>No tenés pedidos en curso</h3><p>{data.orders.total?"Tus pedidos anteriores siguen disponibles en el historial.":"Prepará tu primer trabajo y confirmá la cotización para comenzar su seguimiento."}</p><a className="client-button" href="/cliente/cotizaciones/nueva">Preparar un pedido</a></div>}
        </section>
        <section className="client-card client-dashboard-orders"><header><h2>Pedidos activos recientes</h2><a href="/cliente/pedidos">Ver todos <span aria-hidden="true">→</span></a></header>
          {data.active===0?<p className="client-empty">No tenés pedidos activos.</p>:<><div className="client-table-wrap"><table className="client-table"><thead><tr><th>Pedido</th><th>Estado</th><th>Fecha</th><th>Total</th><th>Acción</th></tr></thead><tbody>{data.activeOrders.slice(0,3).map(p=><tr key={p.codigoPublico}><td>PED-{p.numero}</td><td><ClientOrderBadge order={p}/></td><td>{date(p.confirmadaEn)}</td><td>{money(p.total)}</td><td><a className="client-button secondary" href={`/cliente/pedidos/${p.codigoPublico}`}>Ver PED-{p.numero}</a></td></tr>)}</tbody></table></div><p className="client-muted">Mostrando {Math.min(data.active,3)} de {data.active} pedidos activos.</p></>}
        </section>
      </div>
      <aside className="client-dashboard-aside" aria-label="Resumen de la cuenta">
        <section className="client-card"><h2>Resumen general</h2><dl className="client-dashboard-counts"><div><dt>Pedidos realizados</dt><dd data-testid="customer-order-count">{data.orders.total}</dd></div><div><dt>Pedidos en curso</dt><dd data-testid="customer-active-count">{data.active}</dd></div></dl></section>
        <section className="client-card"><h2>Entrega del pedido actual</h2>{current?<><h3>{modeLabels[current.reserva.modalidad]}</h3><p>{current.reserva.nombre}</p><p>{current.reserva.fecha} · {current.reserva.apertura}–{current.reserva.cierre}<br/><span className="client-muted">{current.reserva.zonaHoraria}</span></p><a href={`/cliente/pedidos/${current.codigoPublico}#pedido-entrega`}>Consultar entrega <span aria-hidden="true">›</span></a></>:<p className="client-muted">Al confirmar un pedido, verás aquí su lugar y franja de entrega.</p>}</section>
      </aside>
    </div>}
  </>;
}
