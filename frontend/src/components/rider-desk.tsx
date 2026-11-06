"use client";

import {useEffect,useRef,useState} from "react";
import {secureMutation} from "@/lib/secure-mutation";
import "@/app/client.css";

type Pedido={
  codigoPublico:string; numero:number; modalidad:string; destino:string;
  receptor:string; telefono:string|null; version:number; estadoLogistico:string;
  presentadoEn:string|null; destinoPublico:string|null;
};
type Recorrido={codigoPublico:string; asignadoEn:string; pedidos:Pedido[]};
type Accion="LLEGAR_PUNTO"|"CLIENTE_AUSENTE"|"REINTENTAR_ENTREGA"|"ENTREGAR";
const labels:Record<Accion,string>={
  LLEGAR_PUNTO:"Confirmar llegada al destino",
  CLIENTE_AUSENTE:"Registrar visita sin receptor",
  REINTENTAR_ENTREGA:"Iniciar nuevo intento",
  ENTREGAR:"Entregar con código",
};
type Seleccion={pedido:Pedido;accion:Accion}|{recorrido:string;pedidos:Pedido[]};

export function RiderDesk({enabled,admin}:{enabled:boolean;admin:boolean}){
  const [active,setActive]=useState(enabled);
  const [routes,setRoutes]=useState<Recorrido[]|null>(null);
  const [selection,setSelection]=useState<Seleccion|null>(null);
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState("");
  const [error,setError]=useState("");
  const dialog=useRef<HTMLDialogElement>(null);
  const operation=useRef<{key:string;id:string}|null>(null);

  async function refresh(){
    setBusy(true);
    setError("");
    try{
      const response=await fetch("/api/rider/recorridos",{cache:"no-store"});
      if(!response.ok)throw new Error("No pudimos consultar tus recorridos.");
      setRoutes(await response.json());
    }catch(e){setError(e instanceof Error?e.message:"No pudimos actualizar.");}
    finally{setBusy(false);}
  }
  useEffect(()=>{if(active)void refresh();},[active]);
  useEffect(()=>{if(selection&&!dialog.current?.open)dialog.current?.showModal();},[selection]);

  async function enable(){
    setBusy(true);
    setError("");
    try{
      await secureMutation("/api/admin/reparto/propio");
      setActive(true);
    }catch(e){setError(e instanceof Error?e.message:"No pudimos habilitar el reparto.");}
    finally{setBusy(false);}
  }
  function close(){dialog.current?.close();setSelection(null);operation.current=null;setError("");}

  async function confirm(event:React.FormEvent<HTMLFormElement>){
    event.preventDefault();
    if(!selection||busy)return;
    let path:string;
    let body:object;
    if("recorrido" in selection){
      path=`/api/rider/recorridos/${selection.recorrido}/presentar`;
      body={pedidos:selection.pedidos.map(p=>({pedido:p.codigoPublico,version:p.version}))};
    }else{
      const {pedido,accion}=selection;
      const code=String(new FormData(event.currentTarget).get("codigo")??"").trim().toUpperCase();
      const entrega=accion==="ENTREGAR";
      if(entrega&&!/^[A-HJ-NP-Z2-9]{8}$/.test(code)){setError("Ingresá los 8 caracteres del código.");return;}
      const key=JSON.stringify([pedido.codigoPublico,pedido.version,accion,code]);
      if(operation.current?.key!==key)operation.current={key,id:crypto.randomUUID()};
      path=`/api/rider/pedidos/${pedido.codigoPublico}/${entrega?"entregar":"parada"}`;
      body={operacion:operation.current.id,version:pedido.version,confirmado:true,...(entrega?{codigo:code}:{accion})};
    }
    setBusy(true);
    setError("");
    setMessage("");
    try{
      const response=await secureMutation(path,JSON.stringify(body),"application/json");
      const result=await response.json();
      if(!result.aceptado){operation.current=null;setError(result.mensaje);return;}
      close();
      setMessage(result.mensaje);
      await refresh();
    }catch(e){setError(e instanceof Error?e.message:"No pudimos registrar la acción.");}
    finally{setBusy(false);}
  }

  return <>
    {!selection&&error&&<p role="alert" className="admin-warning">{error}</p>}
    {message&&<p role="status" className="admin-success">{message}</p>}
    {!active&&admin?<section className="admin-card">
      <h2>Repartí con tu cuenta de administrador</h2>
      <p>Al habilitarte vas a aparecer en el selector de riders de Entregas pendientes.</p>
      <button className="admin-button" disabled={busy} onClick={()=>void enable()}>Habilitarme para repartir</button>
    </section>:<>
      <div className="client-actions">
        <button className="admin-button secondary" disabled={busy} onClick={()=>void refresh()}>Actualizar recorridos</button>
        {admin&&<a className="admin-button" href="/administracion/pedidos?estado=LISTO_PARA_ENTREGA">Asignar pedidos para repartir</a>}
      </div>
      {busy&&!selection&&<p role="status">Consultando recorridos…</p>}
      {routes?.length===0&&<p className="admin-empty">Todavía no tenés recorridos asignados.</p>}
      {routes?.map(route=>{
        const stops=new Map<string,Pedido[]>();
        for(const pedido of route.pedidos){
          const key=`${pedido.modalidad}:${pedido.modalidad==="RETIRO_PUNTO_ENTREGA"?pedido.destinoPublico??pedido.destino:pedido.destino}`;
          const stop=stops.get(key);
          if(stop)stop.push(pedido);
          else stops.set(key,[pedido]);
        }
        return <section key={route.codigoPublico} className="admin-card">
          <h2>Recorrido del {new Date(route.asignadoEn).toLocaleString("es-AR")}</h2>
          <p>{stops.size} destinos · {route.pedidos.length} pedidos</p>
          {[...stops.values()].map(orders=><section className="branch-card" key={orders[0].codigoPublico}>
            <h3>{orders[0].destino}</h3>
            <p>{orders[0].modalidad==="ENVIO_DOMICILIO"?"Entrega a domicilio":"Dejar en destino de retiro"}</p>
            {orders[0].modalidad==="RETIRO_PUNTO_ENTREGA"&&<PointSelection orders={orders} busy={busy}
              onConfirm={pedidos=>{setError("");setSelection({recorrido:route.codigoPublico,pedidos});}}/>}
            {orders.map(p=>{
              const home=p.modalidad==="ENVIO_DOMICILIO";
              const point=p.modalidad==="RETIRO_PUNTO_ENTREGA";
              const actions:Accion[]=p.estadoLogistico==="CLIENTE_AUSENTE"?["REINTENTAR_ENTREGA"]:
                p.estadoLogistico!=="EN_VIAJE"?[]:home?["ENTREGAR","CLIENTE_AUSENTE"]:
                point?[]:["LLEGAR_PUNTO"];
              const status=p.estadoLogistico==="EN_VIAJE"?(point&&p.presentadoEn?"Pendiente de recepción en el punto":"En viaje"):
                ({CLIENTE_AUSENTE:"Visita sin receptor",DISPONIBLE_PUNTO:"Recibido en destino",ENTREGADO:"Entregado",CERRADO:"Entregado",CANCELADO:"Cancelado"} as Record<string,string>)[p.estadoLogistico]??"Sin acción pendiente";
              return <div key={p.codigoPublico}>
                <p><strong>PED-{p.numero}</strong> · {status}{home&&<> · {p.receptor}{p.telefono&&<> · <a href={`tel:${p.telefono}`}>{p.telefono}</a></>}</>}</p>
                {!!actions.length&&<div className="client-actions">{actions.map(accion=><button key={accion} className={`admin-button${accion==="CLIENTE_AUSENTE"?" secondary":""}`} disabled={busy} onClick={()=>{setError("");setSelection({pedido:p,accion});}}>{labels[accion]}</button>)}</div>}
              </div>;
            })}
          </section>)}
        </section>;
      })}
    </>}
    <dialog ref={dialog} closedby="any" className="payment-receipt-dialog" aria-labelledby="rider-action-title" onCancel={e=>{e.preventDefault();if(!busy)close();}}>
      {selection&&<form className="admin-form" onSubmit={confirm}>
        {"recorrido" in selection?<><h2 id="rider-action-title">Confirmá los pedidos que dejaste</h2>
          <p>{selection.pedidos[0].destino} · {selection.pedidos.map(p=>`PED-${p.numero}`).join(", ")}</p>
          <p>El punto confirmará la recepción para habilitar el retiro.</p>
        </>:<><h2 id="rider-action-title">{labels[selection.accion]}</h2>
          <p>PED-{selection.pedido.numero} · {selection.pedido.destino}</p>
          {selection.accion==="ENTREGAR"&&<label>Código de entrega<input name="codigo" autoComplete="off" autoCapitalize="characters" maxLength={8} required autoFocus/></label>}
        </>}
        {error&&<p role="alert" className="admin-warning">{error}</p>}
        <div className="client-actions">
          <button type="button" className="admin-button secondary" disabled={busy} onClick={close}>Volver</button>
          <button className="admin-button" disabled={busy}>{busy?"Registrando…":"Confirmar"}</button>
        </div>
      </form>}
    </dialog>
  </>;
}

function PointSelection({orders,busy,onConfirm}:{orders:Pedido[];busy:boolean;onConfirm:(orders:Pedido[])=>void}){
  const [selected,setSelected]=useState<string[]>([]);
  const eligible=orders.filter(p=>p.estadoLogistico==="EN_VIAJE"&&!p.presentadoEn);
  const picked=eligible.filter(p=>selected.includes(p.codigoPublico));
  if(!eligible.length)return null;
  return <fieldset>
    <legend>Pedidos que dejás en este punto</legend>
    {eligible.map(p=><label key={p.codigoPublico}>
      <input type="checkbox" disabled={busy} checked={selected.includes(p.codigoPublico)}
        onChange={e=>setSelected(old=>e.target.checked?[...old,p.codigoPublico]:old.filter(id=>id!==p.codigoPublico))}/>
      PED-{p.numero} · {p.receptor}
    </label>)}
    <button className="admin-button" disabled={busy||!picked.length} onClick={()=>onConfirm(picked)}>Confirmar que dejé los pedidos ({picked.length})</button>
  </fieldset>;
}
