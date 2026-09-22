"use client";
import {useEffect,useState} from "react";
import {money} from "@/lib/quote-types";
import "@/app/client.css";
type Page={elementos:{cotizacion:string;numero:number;cliente:string;estado:string;total:string;aplicado:string;pendientes:number}[];total:number;pagina:number};
export function PaymentQueue({branch}:{branch:string}){
 const[data,setData]=useState<Page|null>(null),[page,setPage]=useState(0),[busy,setBusy]=useState(false),[error,setError]=useState("");
 async function load(){setBusy(true);setError("");try{const r=await fetch(`/api/operacion/sucursales/${branch}/pagos?pagina=${page}`,{cache:"no-store"}),v=await r.json();if(!r.ok||!Array.isArray(v.elementos))throw new Error(v.mensaje??"No pudimos consultar los pagos.");setData(v);}catch(e){setError(e instanceof Error?e.message:"No pudimos consultar.");}finally{setBusy(false);}}
 useEffect(()=>{void load();},[branch,page]);
 return <section className="client-card"><h2>Cobros y transferencias</h2><p>Ofertas aceptadas de esta sucursal, incluidas las vencidas con dinero por recuperar.</p>{busy&&<p role="status">Consultando…</p>}{error&&<p role="alert" className="client-warning">{error}</p>}{data&&!busy&&(data.elementos.length?<div className="client-table-wrap"><table className="client-table"><thead><tr><th>Cotización</th><th>Cliente</th><th>Estado</th><th>Total / aplicado</th><th>Informes pendientes</th><th>Acción</th></tr></thead><tbody>{data.elementos.map(q=><tr key={q.cotizacion}><td>COT-{q.numero}</td><td>{q.cliente}</td><td>{q.estado}</td><td>{money(q.total)} / {money(q.aplicado)}</td><td>{q.pendientes}</td><td><a href={`/operacion/cotizaciones/${q.cotizacion}/pagos`}>Consultar pagos</a></td></tr>)}</tbody></table></div>:<p>No hay cotizaciones aceptadas en esta sucursal.</p>)}<div className="client-actions"><button className="client-button secondary" disabled={busy} onClick={()=>void load()}>Actualizar cobros</button><button className="client-button secondary" disabled={busy||!page} onClick={()=>setPage(p=>p-1)}>Anterior</button><button className="client-button secondary" disabled={busy||!data||(page+1)*25>=data.total} onClick={()=>setPage(p=>p+1)}>Siguiente</button></div></section>;
}
