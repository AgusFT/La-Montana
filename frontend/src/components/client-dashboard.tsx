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
 * - isDashboard(value: unknown): value is Dashboard
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Dashboard (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * ========================================================================
 */
//#endregion

"use client";

import {useEffect,useState} from "react";
import {quoteRead,money,date,modeLabels} from "@/lib/quote-types";
import {isOrder,isOrderList,type Order,type OrderList} from "@/lib/order-types";
import {OrderProgress} from "./order-progress";
import {ClientOrderBadge} from "./client-order-badge";

type Dashboard={total:number;activos:OrderList;actual:Order|null};
function isDashboard(value:unknown):value is Dashboard {
  return typeof value==="object"&&value!==null&&"total" in value&&Number.isSafeInteger(value.total)
    &&"activos" in value&&isOrderList(value.activos)&&"actual" in value&&(value.actual===null||isOrder(value.actual));
}
export function ClientDashboard(){
  const[data,setData]=useState<Dashboard|null>(null),[busy,setBusy]=useState(true),[error,setError]=useState("");
  async function load(){
    setBusy(true);setError("");
    try{
      setData(await quoteRead("/api/cliente/pedidos/resumen",isDashboard));
    }catch(e){setError(e instanceof Error?e.message:"No pudimos consultar tus pedidos.");}
    finally{setBusy(false);}
  }
  useEffect(()=>{void load();},[]);
  const current=data?.actual;
  const recent=data?.activos.elementos.slice(0,3)??[];
  const detailUrl=current?`/cliente/pedidos/${current.codigoPublico}`:null;
  return <>
    {busy&&!data&&<p className="client-note" role="status">Consultando tus pedidos…</p>}
    {error&&<section className="client-card">
      <h2>{data?"No pudimos actualizar tus pedidos":"Pedidos no disponibles"}</h2>
      <p className="client-warning" role="alert">{error}</p>
      <button className="client-button secondary" onClick={()=>void load()}>Volver a consultar</button>
    </section>}
    {data&&<div className="client-dashboard">
      <div className="client-dashboard-column">
        <section className="client-current-order" aria-labelledby="client-current-title" aria-busy={busy}>
          <h2 id="client-current-title">Pedido actual</h2>
          <div className={current?"client-current-layout":undefined}>
            <div className="client-current-info">
              {current?<><h3>PED-{current.numero}</h3>
                <ClientOrderBadge order={current}/>
                <p className="client-current-date">Confirmado el {date(current.confirmadaEn)}</p>
                {current.cobro&&!["SIN_ANTICIPO","PAGO_ACREDITADO"].includes(current.cobro.estado)&&
                  <p className="client-current-next"><strong>Próximo paso:</strong> {current.cobro.mensaje}</p>}
                {current.estado==="CORRECCION_SOLICITADA"&&<p>Revisá la corrección solicitada por la imprenta.</p>}
              </>:<>
                <h3>No tenés pedidos en seguimiento</h3>
                <p>{data.total?"Tus pedidos anteriores siguen disponibles en el historial.":"Prepará tu primer trabajo y confirmá la cotización para comenzar su seguimiento."}</p>
              </>}
              <div className="client-current-actions">
                {detailUrl&&<a className="client-button" href={detailUrl}>Ver detalle del pedido <span aria-hidden="true">›</span></a>}
                <button type="button" className="client-current-refresh" disabled={busy} onClick={()=>void load()}>
                  {busy?"Actualizando…":"Actualizar seguimiento"}
                </button>
              </div>
            </div>
            {current&&<OrderProgress key={current.codigoPublico} order={current} compact/>}
          </div>
        </section>
        <section className="client-card client-dashboard-delivery">
          <h2>Entrega del pedido actual</h2>
          {current&&detailUrl?<div className="client-delivery-layout">
            <div>
              <h3>{modeLabels[current.reserva.modalidad]}</h3>
              <p>{current.reserva.nombre}</p>
              <p className="client-muted">{current.estadoReserva==="CUMPLIDA"?"Franja registrada":"Franja estimada"} · <time dateTime={current.reserva.fecha}>{current.reserva.fecha.split("-").reverse().join("/")}</time> · {current.reserva.apertura}–{current.reserva.cierre}</p>
            </div>
            <a href={`${detailUrl}#pedido-entrega`}>Consultar entrega <span aria-hidden="true">›</span></a>
          </div>:<p className="client-muted">Acá vas a ver el lugar y el horario de entrega de tu pedido.</p>}
        </section>
      </div>
      <aside className="client-dashboard-column" aria-label="Resumen de la cuenta">
        <section className="client-card client-dashboard-orders">
          <header><h2>Pedidos activos recientes</h2><a href="/cliente/pedidos">Ver todos <span aria-hidden="true">→</span></a></header>
          {recent.length?<ul className="client-recent-list">{recent.map(order=><li key={order.codigoPublico}>
            <a className="client-recent-link" href={`/cliente/pedidos/${order.codigoPublico}`}>
              <div className="client-recent-heading"><strong>PED-{order.numero}</strong><span>{money(order.total)}</span><span aria-hidden="true">›</span></div>
              <ClientOrderBadge order={order}/>
              <time dateTime={order.confirmadaEn}>{date(order.confirmadaEn)}</time>
            </a>
          </li>)}</ul>:<p className="client-muted">No tenés pedidos activos.</p>}
        </section>
        <section className="client-card client-dashboard-summary">
          <header><h2>Resumen general</h2><a href="/cliente/pedidos?vista=historial">Ver historial <span aria-hidden="true">→</span></a></header>
          <dl className="client-dashboard-counts">
            <div><dt>Pedidos realizados</dt><dd data-testid="customer-order-count">{data.total}</dd></div>
            <div><dt>Pedidos en curso</dt><dd data-testid="customer-active-count">{data.activos.total}</dd></div>
          </dl>
        </section>
      </aside>
    </div>}
  </>;
}
