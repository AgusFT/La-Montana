//#region ENCABEZADO · src/components/order-delivery.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/order-delivery.tsx
 * ========================================================================
 * FUNCIÓN
 * Gestiona la interfaz de logística, código de entrega, validación, entrega física y cierre del
 * pedido, con acciones disponibles según permisos y estado.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - isDelivery(v: unknown): v is Delivery
 *   Validador de datos recibidos en tiempo de ejecución.
 * - [export] OrderDelivery({order,internal,financial,onSaved}:
 *   {order:Order;internal:boolean;financial:boolean;onSaved:()=>void})
 *   Componente de interfaz.
 * - [async] OrderDelivery :: load()
 * - OrderDelivery :: choose(a: Action)
 * - [async] OrderDelivery :: save(kind: Action|"CODIGO", recover = false)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Movement (tipo).
 * - Action (tipo).
 * - Delivery (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - labels [const].
 * ========================================================================
 */
//#endregion

"use client";
import {useEffect,useRef,useState} from "react";
import {quoteRead,date,money,modeLabels} from "@/lib/quote-types";
import {type Order,logisticsState} from "@/lib/order-types";
import {secureMutation,MutationError} from "@/lib/secure-mutation";

type Movement="PREPARAR_ENVIO"|"SALIR_REPARTO"|"LLEGAR_PUNTO";
type Action=Movement|"VALIDAR"|"ENTREGAR"|"CERRAR";
export type Delivery={version:number;estadoPedido:string;estadoLogistico:string;cliente:string;correo:string|null;puedeGestionar:boolean;puedeCerrar:boolean;puedeEmitirCodigo:boolean;acciones:{accion:Movement;habilitada:boolean;motivo:string|null}[];codigo:{codigoPublico:string;estado:string;emitidoEn:string;venceEn:string;intentosRestantes:number;puedeRenovarEn:string;texto?:string|null}|null;validacion:{codigoPublico:string;fecha:string;validoHasta:string}|null;entrega:{codigoPublico:string;receptor:string;responsable:string;fecha:string;motivo:string|null}|null;cierre:{fecha:string;responsable:string;motivo:string|null;total:string;aplicado:string}|null;aplicado:string;saldo:string;bloqueosEntrega:string[];bloqueosCierre:string[];historial:{accion:Movement;estadoOrigen:string;estadoDestino:string;actor:string;motivo:string;fecha:string}[]};
const labels:Record<Action,string>={PREPARAR_ENVIO:"Preparar para despacho",SALIR_REPARTO:"Despachar · poner en viaje",LLEGAR_PUNTO:"Ya llegó al destino",VALIDAR:"Validar código de entrega",ENTREGAR:"Entregar con código",CERRAR:"Cerrar pedido"};
function isDelivery(v:unknown):v is Delivery{if(!v||typeof v!=="object")return false;const d=v as Delivery;return Number.isSafeInteger(d.version)&&typeof d.estadoPedido==="string"&&typeof d.estadoLogistico==="string"&&typeof d.cliente==="string"&&typeof d.saldo==="string"&&typeof d.aplicado==="string"&&typeof d.puedeGestionar==="boolean"&&typeof d.puedeCerrar==="boolean"&&typeof d.puedeEmitirCodigo==="boolean"&&Array.isArray(d.acciones)&&Array.isArray(d.historial)&&Array.isArray(d.bloqueosEntrega)&&Array.isArray(d.bloqueosCierre);}
export function OrderDelivery({order,internal,financial,onSaved,compact=false,workspace=false,onSnapshot,onLockChange,refresh=0}:{order:Order;internal:boolean;financial:boolean;onSaved:()=>void;compact?:boolean;workspace?:boolean;refresh?:number;onSnapshot?:(value:Delivery|null)=>void;onLockChange?:(locked:boolean)=>void}){
 const[data,setData]=useState<Delivery|null>(null),[loading,setLoading]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState(""),[uncertain,setUncertain]=useState(false),[action,setAction]=useState<Action|null>(null),[secret,setSecret]=useState<{code:string;id:string}|null>(null),[now,setNow]=useState(Date.now());
 const running=useRef(false),pending=useRef<{path:string;body:string;kind:Action|"CODIGO"}|null>(null),form=useRef<HTMLFormElement>(null),dialog=useRef<HTMLDialogElement>(null);
 const base=`/api/${internal?"operacion":"cliente"}/pedidos/${order.codigoPublico}/entrega`,locked=busy||loading||uncertain;
 async function load(){setLoading(true);setError("");try{setData(await quoteRead(base,isDelivery));}catch(e){setData(null);setError(e instanceof Error?e.message:"No pudimos consultar la entrega.");}finally{setLoading(false);}}
 useEffect(()=>{void load();},[order.codigoPublico,order.version,internal,refresh]);
 useEffect(()=>{const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer);},[]);
 useEffect(()=>{if(action)dialog.current?.showModal();else dialog.current?.close();},[action]);
 useEffect(()=>{onSnapshot?.(data);},[data,onSnapshot]);
 useEffect(()=>{onLockChange?.(busy||uncertain);return()=>onLockChange?.(false);},[busy,uncertain,onLockChange]);
 const ready=data?.estadoPedido==="LISTO_PARA_ENTREGA",finished=!!data?.entrega,valid=!!data?.validacion&&Date.parse(data.validacion.validoHasta)>now;
 const localPickup=order.reserva.modalidad==="RETIRO_SUCURSAL"&&!order.retiroConTraslado;
 const canValidate=ready&&["LISTO_RETIRO","DISPONIBLE_PUNTO",...(order.reserva.modalidad==="ENVIO_DOMICILIO"?["EN_VIAJE"]:[])].includes(data?.estadoLogistico??"");
 const canGenerate=ready&&(!data.codigo||Date.parse(data.codigo.puedeRenovarEn)<=now);
 function choose(a:Action){setError("");setMessage("");setAction(a);}
 async function save(kind:Action|"CODIGO",recover=false){
  if(running.current||!recover&&locked)return;
  if(!recover){if(!data)return;const input:Record<string,unknown>={operacion:crypto.randomUUID(),version:data.version};let path="codigo";
   if(kind!=="CODIGO"){
    if(!form.current?.reportValidity())return;const f=new FormData(form.current);
    if(kind==="VALIDAR"){input.codigo=String(f.get("codigo")).trim().toUpperCase();path="validar";}
    else{input.motivo=f.get("motivo");input.confirmado=true;
     if(kind==="ENTREGAR"){input.codigo=String(f.get('codigo')).trim().toUpperCase();input.receptor=f.get("receptor");path="recibir";}
     else if(kind==="CERRAR")path="cerrar";else{input.accion=kind;path="movimientos";}
    }
   }
   pending.current={path:`${base}/${path}`,body:JSON.stringify(input),kind};
  }
  const command=pending.current;if(!command)return;running.current=true;setBusy(true);setError("");
  try{
   const r=await secureMutation(command.path,command.body,"application/json"),result:unknown=await r.json();
   const envelope=result as {vista?:unknown;codigo?:unknown;aviso?:unknown;aceptado?:unknown;mensaje?:unknown};
   const v=command.kind==="CODIGO"||command.kind==="VALIDAR"||command.kind==='ENTREGAR'?envelope.vista:result;
   if(!isDelivery(v)||(command.kind==="VALIDAR"||command.kind==='ENTREGAR')&&typeof envelope.aceptado!=="boolean")throw new Error("No pudimos comprobar el resultado guardado.");
   setData(v);pending.current=null;setUncertain(false);if(envelope.aceptado!==false)setAction(null);
   if(command.kind==="CODIGO"){
    setSecret(typeof envelope.codigo==="string"&&v.codigo?{code:envelope.codigo,id:v.codigo.codigoPublico}:null);
    setMessage(typeof envelope.aviso==="string"?envelope.aviso:"Consultá el estado del código.");
   }else if(command.kind==="VALIDAR"||command.kind==='ENTREGAR'){
    if(envelope.aceptado){setMessage(typeof envelope.mensaje==="string"?envelope.mensaje:"Entrega registrada.");if(command.kind==='ENTREGAR')onSaved();}else setError(typeof envelope.mensaje==="string"?envelope.mensaje:"El código no coincide. Comprobalo antes de entregar.");
   }else{setMessage(command.kind==="CERRAR"?"Pedido cerrado. La entrega y la conciliación quedaron registradas.":"Movimiento de entrega registrado.");onSaved();}
  }catch(e){setError(e instanceof Error?e.message:"No pudimos completar la operación.");if(e instanceof MutationError&&!e.uncertain){pending.current=null;setUncertain(false);}else setUncertain(true);}
  finally{running.current=false;setBusy(false);}
 }
 const feedback=<>{error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<div className="client-warning"><p>No pudimos confirmar la respuesta. Recuperá la misma operación antes de continuar.</p><button type="button" className="client-button" disabled={busy} onClick={()=>void save(pending.current?.kind??"CODIGO",true)}>Recuperar operación de entrega</button></div>}</>;
 const clientCode=data&&(!internal&&ready&&<section className="delivery-code"><h3>Tu código para recibir el pedido</h3>
    {data.codigo?.estado==='ACTIVO'&&data.codigo?.texto?<><div className="delivery-secret"><strong aria-label="Código de entrega">{data.codigo.texto}</strong></div><p>Se generó automáticamente. Podés anotarlo y retirarlo sin llevar el celular cuando el seguimiento indique que está disponible. Se conserva hasta la entrega.</p></>:<p>{data.codigo?'El código necesita asistencia de la imprenta.':'Se genera automáticamente cuando el pedido sale al destino. Para retiro en la imprenta, cuando está listo.'}</p>}
    {Number(data.saldo)>0&&<p className="client-warning">Queda un saldo de {money(data.saldo)} que debe estar acreditado y aplicado antes de recibir el trabajo.</p>}
   </section>);
 if(compact&&!internal)return <div id="pedido-entrega" className="client-order-receipt">
  {loading&&<p role="status">Consultando entrega…</p>}{feedback}{message&&<p role="status" className="client-success">{message}</p>}
  {data&&<>{clientCode}{!ready&&!finished&&<p className="client-muted">{["CANCELADO","RECHAZADO"].includes(order.estado)?"Este pedido no tiene una entrega pendiente.":"El código de retiro se habilitará cuando el trabajo esté listo."}</p>}{data.entrega&&<p className="client-success">Entrega registrada el {date(data.entrega.fecha)}. Recibió {data.entrega.receptor}.</p>}</>}
  {error&&<button type="button" className="client-text-action" disabled={locked} onClick={()=>void load()}>Volver a consultar la entrega</button>}
 </div>;
 return <section id="pedido-entrega" className={`client-card delivery-panel${workspace?" operation-task-component":""}`} aria-label="Entrega y cierre del pedido">
  <header className="order-management-heading"><h2>{internal?"Logística, entrega y cierre":"Entrega de tu pedido"}</h2><span>{data?logisticsState(data.estadoLogistico):"Consultando"}</span></header>
  {(!workspace||!!error)&&<button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar entrega y saldo</button>}
  {loading&&<p role="status">Consultando entrega…</p>}{!action&&feedback}{message&&<p role="status" className="client-success">{message}</p>}
  {data&&<>
   {workspace?<div className={`operation-delivery-status${localPickup&&ready?" local-pickup-ready":""}`}><p>{localPickup&&ready?<><strong>Disponible para retirar en {order.oferta.sucursal.nombre}</strong><br/><span>No requiere despacho ni viaje.</span></>:<>Estado del recorrido: <strong>{logisticsState(data.estadoLogistico)}</strong></>}</p><p>Saldo pendiente: <strong>{money(data.saldo)}</strong></p></div>:<div className="client-grid three delivery-summary"><section><h3>{internal?"Cliente y trabajo":"Trabajo y sucursal"}</h3><p><strong>{data.cliente}</strong>{internal&&data.correo&&<><br/>{data.correo}</>}</p><p>{order.oferta.sucursal.nombre}<br/>{order.archivos.length} PDF · {order.oferta.items.reduce((s,i)=>s+i.trabajo.copias,0)} copias</p><a href="#pedido-pdf">Ver PDF confirmados</a></section>
    <section><h3>Datos de entrega</h3><p><strong>{modeLabels[order.reserva.modalidad]}</strong><br/>{order.reserva.nombre}</p>{!order.reserva.nombre.includes(order.oferta.destino)&&<p>{order.oferta.destino}</p>}<p>{order.reserva.fecha} · {order.reserva.apertura}–{order.reserva.cierre}<br/>{order.reserva.zonaHoraria}</p>{order.contacto&&<p>Recibe: {order.contacto.receptor}<br/>Teléfono: {order.contacto.telefono}</p>}<p className="client-state">{logisticsState(data.estadoLogistico)}</p></section>
    <section><h3>Situación financiera</h3><dl className="delivery-money"><dt>Total</dt><dd>{money(order.oferta.total)}</dd><dt>Acreditado y aplicado</dt><dd>{money(data.aplicado)}</dd><dt>Saldo pendiente</dt><dd><strong>{money(data.saldo)}</strong></dd></dl>{financial&&<a href="#pedido-pagos">Consultar pagos y comprobantes</a>}<p className="client-muted">El código valida la recepción. Los cobros se registran en pagos.</p></section>
   </div>}
   {!ready&&!finished&&<p className="client-note">El pedido debe superar producción y calidad antes de habilitar la entrega.</p>}
   {internal&&ready&&<>{!localPickup&&<p className="delivery-next-step">{data.estadoLogistico==="PENDIENTE_PREPARACION"?"Primero prepará el pedido. Después vas a poder ponerlo en viaje.":data.estadoLogistico==="PREPARADO"?"El pedido ya está preparado: el próximo paso es despacharlo y ponerlo en viaje.":data.estadoLogistico==="EN_VIAJE"?"El pedido está en viaje. Registrá su llegada cuando alcance el destino acordado.":"El pedido llegó y está disponible para retirar."}</p>}<div className="client-actions delivery-primary-actions">{data.acciones.filter(a=>(!workspace||a.habilitada)&&!localPickup&&(a.accion!=="LLEGAR_PUNTO"||order.reserva.modalidad==="RETIRO_PUNTO_ENTREGA"||order.retiroConTraslado)).map(a=><button key={a.accion} className="client-button order-approve" disabled={locked||!a.habilitada} title={a.motivo??undefined} onClick={()=>choose(a.accion)}>{labels[a.accion]}</button>)}</div>
    {!data.puedeGestionar&&<p className="client-warning">Tu cuenta no tiene permiso para gestionar entregas.</p>}
    {order.reserva.modalidad==="RETIRO_PUNTO_ENTREGA"&&<p className="client-note">La llegada al punto habilita el retiro. La entrega al cliente se confirma por separado.</p>}
    <section className={`delivery-code${localPickup&&canValidate?" local-pickup-ready":""}`}><h3>{localPickup?`Retiro en ${order.oferta.sucursal.nombre}`:"Retiro del pedido"}</h3><p>{canValidate?(localPickup?'El trabajo ya está disponible en esta sucursal. Ingresá el código que trae la persona y confirmá la entrega en una sola acción.':'El pedido está disponible. Ingresá el código que trae la persona y confirmá la entrega en una sola acción.'):data.codigo?'Esperando que llegue al destino de retiro. El código ya acompaña al pedido desde el despacho.':'El código se genera automáticamente al confirmar la salida al destino.'}</p>
     <div className="client-actions">{data.puedeGestionar&&canValidate&&<button className="client-button order-approve" disabled={locked||data.bloqueosEntrega.length>0} onClick={()=>choose("ENTREGAR")}>Entregar con código</button>}</div>
     {data.bloqueosEntrega.length>0&&<ul className="client-muted">{data.bloqueosEntrega.map(b=><li key={b}>{b}</li>)}</ul>}
    </section></>}
   {clientCode}
   {data.entrega&&<section className="delivery-receipt client-success"><h3>Entrega registrada</h3><p>Recibió <strong>{data.entrega.receptor}</strong> · {date(data.entrega.fecha)}<br/>Registró: {data.entrega.responsable}</p>{internal&&<p className="order-text">{data.entrega.motivo}</p>}</section>}
   {internal&&finished&&!data.cierre&&<section className="delivery-close"><h3>Cierre administrativo</h3><p>Comprobá entrega completa, dinero aplicado, excedentes devueltos e informes de transferencia resueltos.</p>{data.bloqueosCierre.length>0&&<ul className="client-warning">{data.bloqueosCierre.map(b=><li key={b}>{b}</li>)}</ul>}{(!workspace||data.puedeCerrar&&data.bloqueosCierre.length===0)&&<button className="client-button order-approve" disabled={locked||!data.puedeCerrar||data.bloqueosCierre.length>0} onClick={()=>choose("CERRAR")}>Cerrar pedido</button>}</section>}
   {data.cierre&&<section className="client-success"><h3>Pedido cerrado</h3><p>{date(data.cierre.fecha)} · {data.cierre.responsable}<br/>Importe conciliado al cierre: {money(data.cierre.aplicado)}</p>{internal&&<p className="order-text">{data.cierre.motivo}</p>}</section>}
   {data.historial.length>0&&<details><summary>Historial del recorrido de entrega</summary>{data.historial.map((h,i)=><p className="order-text" key={i}><strong>{logisticsState(h.estadoDestino)} · {date(h.fecha)}</strong><br/>{h.actor} · {h.motivo}</p>)}</details>}
  </>}
  <dialog ref={dialog} className="delivery-dialog" aria-labelledby="delivery-dialog-title" onCancel={e=>{e.preventDefault();if(!locked)setAction(null);}}>
   {action&&data&&<form ref={form} className="client-form" key={action} onSubmit={e=>{e.preventDefault();void save(action);}}><header className="order-management-heading"><h3 id="delivery-dialog-title">{labels[action]}</h3><span>PED-{order.numero}</span></header>{feedback}<fieldset disabled={locked}>
    {action==="VALIDAR"?<><p>Ingresá el código que presenta el cliente. Validarlo no registra la entrega ni modifica los pagos.</p><label>Código de entrega<input name="codigo" autoComplete="off" autoCapitalize="characters" spellCheck={false} required maxLength={8} minLength={8} pattern="[A-HJ-NP-Za-hj-np-z2-9]{8}" placeholder="8 letras y números"/></label></>:<>
     {action==="ENTREGAR"&&<><p>Entregá el pedido completo a quien presente el código. No necesita mostrar el celular.</p><label>Código de entrega<input name="codigo" autoComplete="off" autoCapitalize="characters" spellCheck={false} required maxLength={8} minLength={8} pattern="[A-HJ-NP-Za-hj-np-z2-9]{8}" placeholder="8 letras y números"/></label><details><summary>Nombre de quien retira (opcional)</summary><label>Nombre<input name="receptor" maxLength={140}/></label></details></>}
     {action==="CERRAR"&&<p>La entrega está registrada y el total conciliado es {money(data.aplicado)}. El cierre conserva estos hechos y no registra nuevos cobros.</p>}
     {action==="LLEGAR_PUNTO"&&<p>Confirmá la llegada física al punto. El cliente todavía debe retirar su trabajo.</p>}
     {action==="SALIR_REPARTO"&&<p>Confirmá que el pedido salió hacia el destino acordado. Su estado logístico pasará a En viaje.</p>}
     {action==="PREPARAR_ENVIO"&&<p>Confirmá que el pedido completo está preparado para salir al destino acordado.</p>}
     <p><strong>Destino:</strong> {order.reserva.nombre}</p><details><summary>Agregar observación (opcional)</summary><label>Observación<textarea name="motivo" maxLength={500}/></label></details><p className="client-muted">Al confirmar se guardan tu usuario, la fecha y la hora.</p>
    </>}
    <div className="client-actions"><button type="button" className="client-button secondary" onClick={()=>setAction(null)}>Volver</button><button className={`client-button ${action==="VALIDAR"?"":"order-approve"}`}>{busy?"Guardando…":action==='SALIR_REPARTO'?'Confirmar salida':action==='PREPARAR_ENVIO'?'Confirmar preparado':action==='LLEGAR_PUNTO'?'Confirmar llegada':action==='ENTREGAR'?'Confirmar entrega':action==='CERRAR'?'Cerrar pedido':'Validar código'}</button></div>
   </fieldset></form>}
  </dialog>
 </section>;
}
