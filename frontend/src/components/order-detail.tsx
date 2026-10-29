"use client";
import {useEffect,useState} from "react";
import {quoteRead,date} from "@/lib/quote-types";
import {isOrder,type Order} from "@/lib/order-types";
import type {Permission} from "@/lib/organization-types";
import {ClientJourney} from "./client-journey";
import {ClientContextTour} from "./client-context-tour";
import {ClientOrderSummary} from "./client-order-summary";
import {OrderProgress} from "./order-progress";
import {orderTrackingLabel} from "@/lib/order-tracking";
import {OrderManagement} from "./order-management";
import {OperationOrderWorkspace} from "./operation-order-workspace";
import "@/app/client.css";
import "@/app/client-workspace.css";
import "@/app/client-order-compact.css";

/** Shares authenticated order loading; customer and staff each have a task-appropriate view. */
export function OrderDetail({id,internal=false,financial=true,globalAdmin=false,permissions=[]}:{id:string;internal?:boolean;financial?:boolean;globalAdmin?:boolean;permissions?:Permission[]}){
 const[refresh,setRefresh]=useState(0),[order,setOrder]=useState<Order|null>(null),[error,setError]=useState(""),[busy,setBusy]=useState(false);
 async function load(){setBusy(true);setError("");try{setOrder(await quoteRead(`/api/${internal?"operacion":"cliente"}/pedidos/${id}`,isOrder));}catch(e){setOrder(null);setError(e instanceof Error?e.message:"No pudimos consultar el pedido.");}finally{setBusy(false);}}
 useEffect(()=>{setOrder(null);void load();},[id,internal]);
 const saved=()=>{setRefresh(v=>v+1);void load();};
 if(internal&&order)return <OperationOrderWorkspace order={order} permissions={permissions} financial={financial} globalAdmin={globalAdmin} busy={busy} error={error} refresh={refresh} onRefresh={saved} onSaved={saved}/>;
 const controls=<div className={internal?"client-actions":"client-order-heading-actions"}><a className="client-button secondary" href={globalAdmin?"/administracion/pedidos":internal?"/operacion":"/cliente/pedidos"}>Volver a pedidos</a><button className="client-button secondary" disabled={busy} onClick={()=>void load()}>Actualizar pedido</button></div>;
 const content=<>{!internal&&<ClientJourney part={3} compact/>}{busy&&<p role="status">Consultando pedido…</p>}{error&&<p role="alert" className="client-warning">{error}</p>}{!order&&controls}{order&&<><header className="order-overview order-detail-heading"><div className="client-order-heading-state"><div><h2>Tu pedido PED-{order.numero}</h2><p>Confirmado el {date(order.confirmadaEn)}</p></div><span className="client-state">{orderTrackingLabel(order)}</span></div>{controls}</header><div id="client-order-progress" className="client-order-tracking"><OrderProgress order={order} featured/></div><ClientOrderSummary order={order} onSaved={saved} actions={<OrderManagement order={order} internal={false} onSaved={saved} compact/>}/><ClientContextTour stage={3} ready={!busy}/></>}</>;
 return internal?content:<div className="client-experience client-compact-experience client-order-experience">{content}</div>;
}
