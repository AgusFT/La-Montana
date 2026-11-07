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
import {createPortal} from "react-dom";
import {useEffect,useRef,useState} from "react";
import {quoteRead,date,money,modeLabels} from "@/lib/quote-types";
import {type Order,logisticsState} from "@/lib/order-types";
import {deliveryExperience} from "@/lib/delivery-experience";
import "@/app/delivery-experience.css";
import {secureMutation,MutationError} from "@/lib/secure-mutation";

type Movement="PREPARAR_ENVIO"|"SALIR_REPARTO"|"LLEGAR_PUNTO"|"CLIENTE_AUSENTE"|"REINTENTAR_ENTREGA";
type Action=Movement|"VALIDAR"|"ENTREGAR"|"COBRAR_ENTREGAR"|"CERRAR";
type Rider={codigoPublico:string;nombre:string};
function isRiders(value:unknown):value is Rider[]{
 return Array.isArray(value)&&value.every(r=>r&&typeof r.codigoPublico==="string"&&typeof r.nombre==="string");
}
export type Delivery={version:number;estadoPedido:string;estadoLogistico:string;cliente:string;correo:string|null;puedeGestionar:boolean;puedeCerrar:boolean;puedeEmitirCodigo:boolean;acciones:{accion:Movement;habilitada:boolean;motivo:string|null}[];codigo:{codigoPublico:string;estado:string;emitidoEn:string;venceEn:string;intentosRestantes:number;puedeRenovarEn:string;texto?:string|null}|null;validacion:{codigoPublico:string;fecha:string;validoHasta:string}|null;entrega:{codigoPublico:string;receptor:string;responsable:string;fecha:string;motivo:string|null}|null;cierre:{fecha:string;responsable:string;motivo:string|null;total:string;aplicado:string}|null;aplicado:string;saldo:string;bloqueosEntrega:string[];bloqueosCierre:string[];cobroEfectivo?:{importe:string;habilitado:boolean;puedeCerrar:boolean;bloqueo:string|null}|null;reciboEfectivo?:{pago:string;importe:string;referencia:string;responsable:string;fecha:string}|null;historial:{accion:Movement;estadoOrigen:string;estadoDestino:string;actor:string;motivo:string;fecha:string}[]};
const labels:Record<Action,string>={PREPARAR_ENVIO:"Preparar para despacho",SALIR_REPARTO:"Despachar · poner en viaje",LLEGAR_PUNTO:"Ya llegó al destino",VALIDAR:"Validar código de entrega",ENTREGAR:"Ingresar código y entregar",COBRAR_ENTREGAR:"Cobrar efectivo y entregar",CLIENTE_AUSENTE:"El cliente no está",REINTENTAR_ENTREGA:"Iniciar nuevo intento",CERRAR:"Cerrar pedido"};
export function isDelivery(v:unknown):v is Delivery{if(!v||typeof v!=="object")return false;const d=v as Delivery;return Number.isSafeInteger(d.version)&&typeof d.estadoPedido==="string"&&typeof d.estadoLogistico==="string"&&typeof d.cliente==="string"&&typeof d.saldo==="string"&&typeof d.aplicado==="string"&&typeof d.puedeGestionar==="boolean"&&typeof d.puedeCerrar==="boolean"&&typeof d.puedeEmitirCodigo==="boolean"&&Array.isArray(d.acciones)&&Array.isArray(d.historial)&&Array.isArray(d.bloqueosEntrega)&&Array.isArray(d.bloqueosCierre);}
export function OrderDelivery({order,actionTarget,internal,financial,onSaved,compact=false,workspace=false,dispatchHref,onSnapshot,onLockChange,refresh=0}:{order:Order;actionTarget?:HTMLElement|null;internal:boolean;financial:boolean;onSaved:(view?:Delivery)=>void;compact?:boolean;workspace?:boolean;dispatchHref?:string;refresh?:number;onSnapshot?:(value:Delivery|null)=>void;onLockChange?:(locked:boolean)=>void}){
 const[data,setData]=useState<Delivery|null>(null),[loading,setLoading]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState(""),[uncertain,setUncertain]=useState(false),[action,setAction]=useState<Action|null>(null),[deliveryCode,setDeliveryCode]=useState(""),[secret,setSecret]=useState<{code:string;id:string}|null>(null),[now,setNow]=useState(Date.now());
 const[riders,setRiders]=useState<Rider[]>([]),[loadingRiders,setLoadingRiders]=useState(false);
 const running=useRef(false),pending=useRef<{path:string;body:string;kind:Action|"CODIGO"}|null>(null),form=useRef<HTMLFormElement>(null),dialog=useRef<HTMLDialogElement>(null);
 const base=`/api/${internal?"operacion":"cliente"}/pedidos/${order.codigoPublico}/entrega`,locked=busy||loading||uncertain;
 async function load(){setLoading(true);setError("");try{setData(await quoteRead(base,isDelivery));}catch(e){setData(null);setError(e instanceof Error?e.message:"No pudimos consultar la entrega.");}finally{setLoading(false);}}
 useEffect(()=>{void load();},[order.codigoPublico,order.version,internal,refresh]);
 useEffect(()=>{const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer);},[]);
 useEffect(()=>{if(action)dialog.current?.showModal();else dialog.current?.close();},[action]);
 useEffect(()=>{onSnapshot?.(data);},[data,onSnapshot]);
 useEffect(()=>{onLockChange?.(busy||uncertain);return()=>onLockChange?.(false);},[busy,uncertain,onLockChange]);
 const ready=data?.estadoPedido==="LISTO_PARA_ENTREGA",finished=!!data?.entrega,valid=!!data?.validacion&&Date.parse(data.validacion.validoHasta)>now;
 const experience=deliveryExperience(order,data?.estadoLogistico??"");
 const localPickup=experience.local;
 const guest=internal&&!!order.nombreInvitado&&experience.local;
 const canValidate=ready&&experience.canReceive;
 const canGenerate=ready&&(!data.codigo||Date.parse(data.codigo.puedeRenovarEn)<=now);
 async function choose(a:Action){
  setError("");setMessage("");setDeliveryCode("");setAction(a);
  if(a!=="SALIR_REPARTO")return;
  setRiders([]);setLoadingRiders(true);
  try{setRiders(await quoteRead("/api/operacion/repartidores",isRiders));}
  catch(e){setError(e instanceof Error?e.message:"No pudimos consultar los riders.");}
  finally{setLoadingRiders(false);}
 }
 async function save(kind:Action|"CODIGO",recover=false){
  if(running.current||!recover&&locked)return;
  if(!recover){if(!data)return;let input:Record<string,unknown>={operacion:crypto.randomUUID(),version:data.version};let path=`${base}/codigo`;
   if(kind!=="CODIGO"){
    if(!form.current?.reportValidity())return;const f=new FormData(form.current);
    if(kind==="SALIR_REPARTO"){
     const repartidor=f.get("repartidor");
     if(typeof repartidor!=="string"||!repartidor)return;
     path="/api/operacion/entregas/lotes";
     input={operacion:input.operacion,accion:kind,pedidos:[{pedido:order.codigoPublico,version:data.version}],repartidor};
    }
    else if(kind==="VALIDAR"){input.codigo=String(f.get("codigo")).trim().toUpperCase();path=`${base}/validar`;}
    else{input.motivo=f.get("motivo");input.confirmado=true;
     if(kind==="ENTREGAR"||kind==="COBRAR_ENTREGAR"){input.codigo=guest?null:String(f.get('codigo')).trim().toUpperCase();input.receptor=f.get("receptor");path=`${base}/${kind==="COBRAR_ENTREGAR"?"cobrar-efectivo":"recibir"}`;if(kind==="COBRAR_ENTREGAR"){input.importe=data.cobroEfectivo?.importe;input.cerrar=data.cobroEfectivo?.puedeCerrar??false;}}
     else if(kind==="CERRAR")path=`${base}/cerrar`;else{input.accion=kind;path=`${base}/movimientos`;}
    }
   }
   pending.current={path,body:JSON.stringify(input),kind};
  }
  const command=pending.current;if(!command)return;running.current=true;setBusy(true);setError("");
  try{
   const r=await secureMutation(command.path,command.body,"application/json"),result:unknown=await r.json();
   if(command.kind==="SALIR_REPARTO"){
    if(!result||typeof result!=="object"||!("recorrido" in result)||typeof result.recorrido!=="string")
     throw new Error("No pudimos comprobar el despacho guardado.");
    const vista=await quoteRead(base,isDelivery);
    setData(vista);pending.current=null;setUncertain(false);setAction(null);
    setMessage("El pedido quedó en viaje y asignado al rider.");onSaved(vista);
    return;
   }
   const envelope=result as {vista?:unknown;codigo?:unknown;aviso?:unknown;aceptado?:unknown;mensaje?:unknown};
   const v=command.kind==="CODIGO"||command.kind==="VALIDAR"||(command.kind==='ENTREGAR'||command.kind==='COBRAR_ENTREGAR')?envelope.vista:result;
   if(!isDelivery(v)||(command.kind==="VALIDAR"||command.kind==='ENTREGAR'||command.kind==='COBRAR_ENTREGAR')&&typeof envelope.aceptado!=="boolean")throw new Error("No pudimos comprobar el resultado guardado.");
   setData(v);pending.current=null;setUncertain(false);if(envelope.aceptado!==false)setAction(null);
   if(command.kind==="CODIGO"){
    setSecret(typeof envelope.codigo==="string"&&v.codigo?{code:envelope.codigo,id:v.codigo.codigoPublico}:null);
    setMessage(typeof envelope.aviso==="string"?envelope.aviso:"Consultá el estado del código.");
   }else if(command.kind==="VALIDAR"||command.kind==='ENTREGAR'||command.kind==='COBRAR_ENTREGAR'){
    if(envelope.aceptado){setMessage(typeof envelope.mensaje==="string"?envelope.mensaje:"Entrega registrada.");if(command.kind==='ENTREGAR'||command.kind==='COBRAR_ENTREGAR')onSaved(v);}else setError(typeof envelope.mensaje==="string"?envelope.mensaje:"El código no coincide. Comprobalo antes de entregar.");
   }else{setMessage(command.kind==="CERRAR"?"Pedido cerrado. La entrega y la conciliación quedaron registradas.":command.kind==="CLIENTE_AUSENTE"?"Intento sin receptor registrado. La entrega sigue pendiente y conserva el código.":command.kind==="REINTENTAR_ENTREGA"?"Nuevo intento en viaje. Se conserva el mismo código de entrega.":"Movimiento de entrega registrado.");onSaved();}
  }catch(e){setError(e instanceof Error?e.message:"No pudimos completar la operación.");if(e instanceof MutationError&&!e.uncertain){pending.current=null;setUncertain(false);}else setUncertain(true);}
  finally{running.current=false;setBusy(false);}
 }
 const feedback=<>{error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<div className="client-warning"><p>No pudimos confirmar la respuesta. Recuperá la misma operación antes de continuar.</p><button type="button" className="client-button" disabled={busy} onClick={()=>void save(pending.current?.kind??"CODIGO",true)}>Recuperar operación de entrega</button></div>}</>;
 const movementButtons=data&&<div className="client-actions delivery-primary-actions">{data.acciones.filter(a=>a.habilitada&&!localPickup&&a.accion!=="CLIENTE_AUSENTE"&&(a.accion!=="LLEGAR_PUNTO"||order.reserva.modalidad==="RETIRO_PUNTO_ENTREGA"||order.retiroConTraslado)).map(a=><button key={a.accion} className="client-button order-approve" disabled={locked||!a.habilitada} title={a.motivo??undefined} onClick={()=>choose(a.accion)}>{labels[a.accion]}</button>)}</div>;
 const clientCode=data&&(!internal&&ready&&<section className="delivery-code"><h3>Tu código para recibir el pedido</h3>
    {data.codigo?.estado==='ACTIVO'&&data.codigo?.texto?<><div className="delivery-secret"><strong aria-label="Código de entrega">{data.codigo.texto}</strong></div><p>{experience.home?'Se generó automáticamente. Podés anotarlo y dárselo a quien reciba el pedido en tu domicilio. Se conserva hasta la entrega, incluso si hace falta otro intento.':'Se generó automáticamente. Podés anotarlo y retirarlo sin llevar el celular cuando el seguimiento indique que está disponible. Se conserva hasta la entrega.'}</p></>:<p>{data.codigo?'El código necesita asistencia de la imprenta.':'Se genera automáticamente cuando el pedido sale al destino. Para retiro en la imprenta, cuando está listo.'}</p>}
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
   {workspace?<div className={`operation-delivery-status${localPickup&&ready?" local-pickup-ready":""}`}><p>{localPickup&&ready?<><strong>Disponible para retirar en {order.oferta.sucursal.nombre}</strong><br/><span>No requiere despacho ni viaje.</span></>:<>Estado del recorrido: <strong>{logisticsState(data.estadoLogistico)}</strong></>}</p></div>:<div className="client-grid three delivery-summary"><section><h3>{internal?"Cliente y trabajo":"Trabajo y sucursal"}</h3><p><strong>{data.cliente}</strong>{internal&&data.correo&&<><br/>{data.correo}</>}</p><p>{order.oferta.sucursal.nombre}<br/>{order.archivos.length} PDF · {order.oferta.items.reduce((s,i)=>s+i.trabajo.copias,0)} copias</p><a href="#pedido-pdf">Ver PDF confirmados</a></section>
    <section><h3>Datos de entrega</h3><p><strong>{modeLabels[order.reserva.modalidad]}</strong><br/>{order.reserva.nombre}</p>{!order.reserva.nombre.includes(order.oferta.destino)&&<p>{order.oferta.destino}</p>}<p>{order.reserva.fecha} · {order.reserva.apertura}–{order.reserva.cierre}<br/>{order.reserva.zonaHoraria}</p>{order.contacto&&<p>Recibe: {order.contacto.receptor}<br/>Teléfono: {order.contacto.telefono}</p>}<p className="client-state">{logisticsState(data.estadoLogistico)}</p></section>
    <section><h3>Situación financiera</h3><dl className="delivery-money"><dt>Total</dt><dd>{money(order.oferta.total)}</dd><dt>Acreditado y aplicado</dt><dd>{money(data.aplicado)}</dd><dt>Saldo pendiente</dt><dd><strong>{money(data.saldo)}</strong></dd></dl>{financial&&<a href="#pedido-pagos">Consultar pagos y comprobantes</a>}<p className="client-muted">El código valida la recepción. Los cobros se registran en pagos.</p></section>
   </div>}
   {!ready&&!finished&&<p className="client-note">El pedido debe superar producción y calidad antes de habilitar la entrega.</p>}
   {internal&&ready&&<>
    {internal&&order.nombreInvitado&&(experience.point||order.retiroConTraslado)&&data.codigo?.texto&&<section className="client-note"><h3>Código para el invitado</h3><p>Compartilo con {order.nombreInvitado} por el canal acordado para que pueda retirar en {order.reserva.nombre}.</p><div className="delivery-secret"><strong aria-label="Código de entrega del invitado">{data.codigo.texto}</strong></div></section>}
    <section className="delivery-destination"><span>{modeLabels[order.reserva.modalidad]}</span><h3>{order.reserva.nombre}</h3>{experience.home&&order.contacto&&<p>Recibe: <strong>{order.contacto.receptor}</strong> · <a href={`tel:${order.contacto.telefono}`}>{order.contacto.telefono}</a></p>}</section>
    {!data.puedeGestionar&&<p className="client-warning">Tu cuenta no tiene permiso para gestionar entregas.</p>}
    {experience.absent&&<div className="client-warning"><strong>La entrega sigue pendiente</strong><p>No se entregó el pedido ni se consumió el código. Coordiná el próximo intento con el cliente. Este registro no cambia la dirección ni la franja acordada.</p></div>}
    {dispatchHref&&data.acciones.some(a=>a.accion==="SALIR_REPARTO"&&a.habilitada)&&<p><a href={dispatchHref}>Despachar junto con otros pedidos</a></p>}
    {data.puedeGestionar&&!localPickup&&["PENDIENTE_PREPARACION","PREPARADO"].includes(data.estadoLogistico)&&data.acciones.filter(a=>a.accion===(data.estadoLogistico==="PREPARADO"?"SALIR_REPARTO":"PREPARAR_ENVIO")&&!a.habilitada&&a.motivo).map(a=><p key={a.accion} className="client-warning">{a.motivo}</p>)}
    {actionTarget?createPortal(movementButtons,actionTarget):movementButtons}
    {(canValidate||!experience.home)&&<section className={`delivery-code delivery-receive-card${localPickup&&canValidate?" local-pickup-ready":""}`}><div><h3>{experience.receiveTitle}</h3><p>{guest?"Verificá el nombre del invitado y el número de pedido antes de entregar.":experience.receiveDescription}</p></div>
     {!experience.point&&data.cobroEfectivo?<div className="delivery-cash-action"><p><strong>Falta cobrar {money(data.cobroEfectivo.importe)} en efectivo</strong></p><p>{guest?"Registrá en una sola confirmación el efectivo recibido y la entrega en mostrador.":"Registrá el efectivo recibido y la entrega con el código en una sola confirmación."}</p>{actionTarget?createPortal(<button className="client-button order-approve delivery-enter-code" disabled={locked||!data.cobroEfectivo.habilitado} onClick={()=>choose("COBRAR_ENTREGAR")}>Cobrar {money(data.cobroEfectivo.importe)} y entregar</button>,actionTarget):<button className="client-button order-approve delivery-enter-code" disabled={locked||!data.cobroEfectivo.habilitado} onClick={()=>choose("COBRAR_ENTREGAR")}>Cobrar {money(data.cobroEfectivo.importe)} y entregar</button>}{data.cobroEfectivo.bloqueo&&<p className="client-warning">{data.cobroEfectivo.bloqueo}</p>}{data.cobroEfectivo.puedeCerrar&&<p className="client-muted">También quedará cerrado, con su comprobante interno.</p>}</div>:data.puedeGestionar&&canValidate&&(actionTarget?createPortal(<button className="client-button order-approve delivery-enter-code" disabled={locked||data.bloqueosEntrega.length>0} onClick={()=>choose("ENTREGAR")}><span aria-hidden="true">✓</span> {guest?"Confirmar entrega en mostrador":"Ingresar código y entregar"}</button>,actionTarget):<button className="client-button order-approve delivery-enter-code" disabled={locked||data.bloqueosEntrega.length>0} onClick={()=>choose("ENTREGAR")}><span aria-hidden="true">✓</span> {guest?"Confirmar entrega en mostrador":"Ingresar código y entregar"}</button>)}
     {!experience.point&&!data.cobroEfectivo&&data.bloqueosEntrega.length>0&&<ul className="client-muted">{data.bloqueosEntrega.map(b=><li key={b}>{b}</li>)}</ul>}
    </section>}
    {experience.home&&data.acciones.some(a=>a.accion==='CLIENTE_AUSENTE'&&a.habilitada)&&<section className="delivery-absence-action"><div><h3>¿No hay nadie para recibirlo?</h3><p>Registrá el intento. El pedido quedará pendiente de un nuevo intento.</p></div><button type="button" className="client-button secondary" disabled={locked} onClick={()=>choose('CLIENTE_AUSENTE')}>El cliente no está</button></section>}
   </>}
   {clientCode}
   {data.reciboEfectivo&&<section className="delivery-cash-receipt" aria-label="Comprobante interno de cobro"><h3>Comprobante interno de cobro</h3><strong>Efectivo · {money(data.reciboEfectivo.importe)}</strong><dl><div><dt>Recibió</dt><dd>{data.reciboEfectivo.responsable}</dd></div><div><dt>Fecha y hora</dt><dd>{date(data.reciboEfectivo.fecha)}</dd></div><div><dt>Referencia</dt><dd>{data.reciboEfectivo.referencia}</dd></div></dl><p>El dinero quedó registrado y aplicado a este pedido.</p>{financial&&<a href="#pedido-pagos">Ver movimiento de caja y comprobantes</a>}</section>}
   {data.entrega&&<section className="delivery-receipt client-success"><h3>Entrega registrada</h3><p>Recibió <strong>{data.entrega.receptor}</strong> · {date(data.entrega.fecha)}<br/>Registró: {data.entrega.responsable}</p>{internal&&<p className="order-text">{data.entrega.motivo}</p>}</section>}
   {internal&&finished&&!data.cierre&&<section className="delivery-close"><h3>Cierre administrativo</h3><p>Comprobá entrega completa, dinero aplicado, excedentes devueltos e informes de transferencia resueltos.</p>{data.bloqueosCierre.length>0&&<ul className="client-warning">{data.bloqueosCierre.map(b=><li key={b}>{b}</li>)}</ul>}{(!workspace||data.puedeCerrar&&data.bloqueosCierre.length===0)&&(actionTarget?createPortal(<button className="client-button order-approve" disabled={locked||!data.puedeCerrar||data.bloqueosCierre.length>0} onClick={()=>choose("CERRAR")}>Cerrar pedido</button>,actionTarget):<button className="client-button order-approve" disabled={locked||!data.puedeCerrar||data.bloqueosCierre.length>0} onClick={()=>choose("CERRAR")}>Cerrar pedido</button>)}</section>}
   {data.cierre&&<section className="client-success"><h3>Pedido cerrado</h3><p>{date(data.cierre.fecha)} · {data.cierre.responsable}<br/>Importe conciliado al cierre: {money(data.cierre.aplicado)}</p>{internal&&<p className="order-text">{data.cierre.motivo}</p>}</section>}
   {data.historial.length>0&&<details><summary>Historial del recorrido de entrega</summary>{data.historial.map((h,i)=><p className="order-text" key={i}><strong>{logisticsState(h.estadoDestino)} · {date(h.fecha)}</strong><br/>{h.actor} · {h.motivo}</p>)}</details>}
  </>}
  <dialog ref={dialog} closedby="any" className={`delivery-dialog${(action==='ENTREGAR'||action==='COBRAR_ENTREGAR')?" delivery-code-dialog":""}`} aria-labelledby="delivery-dialog-title" onCancel={e=>{e.preventDefault();if(!locked)setAction(null);}}>
   {action&&data&&<form ref={form} className="client-form" key={action} onSubmit={e=>{e.preventDefault();void save(action);}}><header className="order-management-heading"><h3 id="delivery-dialog-title">{action==='COBRAR_ENTREGAR'&&data.cobroEfectivo?.puedeCerrar?'Cobrar, entregar y cerrar':guest&&action==='ENTREGAR'?'Confirmar entrega en mostrador':labels[action]}</h3><span>PED-{order.numero}</span></header>{feedback}<fieldset disabled={locked}>
    {action==="VALIDAR"?<><p>Ingresá el código que presenta el cliente. Validarlo no registra la entrega ni modifica los pagos.</p><label>Código de entrega<input name="codigo" autoComplete="off" autoCapitalize="characters" spellCheck={false} required maxLength={8} minLength={8} pattern="[A-HJ-NP-Za-hj-np-z2-9]{8}" placeholder="8 letras y números"/></label></>:<>
     {(action==="ENTREGAR"||action==="COBRAR_ENTREGAR")&&<><div className="delivery-code-intro"><strong>{experience.home?'Entrega a domicilio':'Entrega del pedido completo'}</strong><p>{order.reserva.nombre}</p><p>{guest?`Confirmá la entrega a ${order.nombreInvitado} (invitado).`:"Pedile el código a la persona que recibe. Puede traerlo anotado; no necesita el celular."}</p></div>{action==='COBRAR_ENTREGAR'&&data.cobroEfectivo&&<div className="delivery-cash-amount"><span>Efectivo recibido</span><strong>{money(data.cobroEfectivo.importe)}</strong><p>Es el saldo completo. Confirmá después de recibir este importe. Se guardan el cobro y su aplicación al pedido.</p></div>}{!guest&&<><label className="delivery-code-entry">Código de entrega<input name="codigo" autoFocus autoComplete="off" autoCapitalize="characters" spellCheck={false} required maxLength={32} minLength={8} pattern="[A-HJ-NP-Za-hj-np-z2-9]{8}" placeholder="ABCD2345" value={deliveryCode} onChange={e=>setDeliveryCode(e.target.value.toUpperCase().replace(/[\s-]/g,'').slice(0,8))} aria-describedby="delivery-code-help"/></label><p id="delivery-code-help" className="client-muted">8 letras y números · {action==='COBRAR_ENTREGAR'?data.cobroEfectivo?.puedeCerrar?'Al confirmar se registran el efectivo, la entrega y el cierre.':'Al confirmar se registran el efectivo y la entrega.':'Al confirmar se comprueba el código y se registra la entrega.'}</p></>}</>}
     {action==="CERRAR"&&<p>La entrega está registrada y el total conciliado es {money(data.aplicado)}. El cierre conserva estos hechos y no registra nuevos cobros.</p>}
     {action==="LLEGAR_PUNTO"&&<p>Confirmá la llegada física al punto. El cliente todavía debe retirar su trabajo.</p>}
     {action==="SALIR_REPARTO"&&<><p>Elegí el rider que recibe el pedido en la sucursal. Al confirmar, pasará a En viaje.</p><label>Rider responsable<select name="repartidor" required disabled={loadingRiders||riders.length===0} defaultValue=""><option value="">{loadingRiders?"Consultando riders…":"Elegí un rider activo"}</option>{riders.map(r=><option key={r.codigoPublico} value={r.codigoPublico}>{r.nombre}</option>)}</select></label>{!loadingRiders&&riders.length===0&&!error&&<p className="client-muted">No hay riders activos disponibles.</p>}</>}
     {action==="PREPARAR_ENVIO"&&<p>Confirmá que el pedido completo está preparado para salir al destino acordado.</p>}
     {action==="CLIENTE_AUSENTE"&&<><p><strong>No había nadie para recibir el pedido en el domicilio.</strong></p><p>Se registra el intento sin marcar el pedido como entregado ni consumir su código. Después de coordinar con el cliente, podrás iniciar un nuevo intento.</p></>}
     {action==="REINTENTAR_ENTREGA"&&<p>Confirmá que comenzó el nuevo intento hacia el mismo domicilio. Se conserva el código y todavía no se registra una entrega.</p>}
     {action!=="ENTREGAR"&&action!=="COBRAR_ENTREGAR"&&<p><strong>Destino:</strong> {order.reserva.nombre}</p>}{action!=="SALIR_REPARTO"&&<details><summary>Agregar datos (opcional)</summary>{(action==="ENTREGAR"||action==="COBRAR_ENTREGAR")&&<label>Nombre de quien recibe<input name="receptor" maxLength={140}/></label>}<label>Observación<textarea name="motivo" maxLength={action==='COBRAR_ENTREGAR'?300:500}/></label></details>}<p className="client-muted">Al confirmar se guardan tu usuario, la fecha y la hora.</p>
    </>}
    <div className="client-actions"><button type="button" className="client-button secondary" onClick={()=>setAction(null)}>Volver</button><button disabled={action==="SALIR_REPARTO"&&(loadingRiders||!riders.length)} className={`client-button ${action==="VALIDAR"?"":"order-approve"}`}>{busy?"Guardando…":action==='SALIR_REPARTO'?'Confirmar salida':action==='PREPARAR_ENVIO'?'Confirmar preparado':action==='LLEGAR_PUNTO'?'Confirmar llegada':action==='CLIENTE_AUSENTE'?'Registrar intento sin entrega':action==='REINTENTAR_ENTREGA'?'Confirmar nuevo intento':action==='COBRAR_ENTREGAR'?data.cobroEfectivo?.puedeCerrar?'Confirmar cobro, entrega y cierre':'Confirmar cobro y entrega':action==='ENTREGAR'?'Confirmar entrega':action==='CERRAR'?'Cerrar pedido':'Validar código'}</button></div>
   </fieldset></form>}
  </dialog>
 </section>;
}
