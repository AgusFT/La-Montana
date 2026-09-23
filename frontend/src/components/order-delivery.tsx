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
type Delivery={version:number;estadoPedido:string;estadoLogistico:string;cliente:string;correo:string|null;puedeGestionar:boolean;puedeCerrar:boolean;puedeEmitirCodigo:boolean;acciones:{accion:Movement;habilitada:boolean;motivo:string|null}[];codigo:{codigoPublico:string;estado:string;emitidoEn:string;venceEn:string;intentosRestantes:number;puedeRenovarEn:string}|null;validacion:{codigoPublico:string;fecha:string;validoHasta:string}|null;entrega:{codigoPublico:string;receptor:string;responsable:string;fecha:string;motivo:string|null}|null;cierre:{fecha:string;responsable:string;motivo:string|null;total:string;aplicado:string}|null;aplicado:string;saldo:string;bloqueosEntrega:string[];bloqueosCierre:string[];historial:{accion:Movement;estadoOrigen:string;estadoDestino:string;actor:string;motivo:string;fecha:string}[]};
const labels:Record<Action,string>={PREPARAR_ENVIO:"Preparar envío",SALIR_REPARTO:"Registrar salida a reparto",LLEGAR_PUNTO:"Registrar llegada al punto",VALIDAR:"Validar código de entrega",ENTREGAR:"Confirmar entrega física",CERRAR:"Cerrar pedido"};
function isDelivery(v:unknown):v is Delivery{if(!v||typeof v!=="object")return false;const d=v as Delivery;return Number.isSafeInteger(d.version)&&typeof d.estadoPedido==="string"&&typeof d.estadoLogistico==="string"&&typeof d.cliente==="string"&&typeof d.saldo==="string"&&typeof d.aplicado==="string"&&typeof d.puedeGestionar==="boolean"&&typeof d.puedeCerrar==="boolean"&&typeof d.puedeEmitirCodigo==="boolean"&&Array.isArray(d.acciones)&&Array.isArray(d.historial)&&Array.isArray(d.bloqueosEntrega)&&Array.isArray(d.bloqueosCierre);}
export function OrderDelivery({order,internal,financial,onSaved}:{order:Order;internal:boolean;financial:boolean;onSaved:()=>void}){
 const[data,setData]=useState<Delivery|null>(null),[loading,setLoading]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState(""),[uncertain,setUncertain]=useState(false),[action,setAction]=useState<Action|null>(null),[secret,setSecret]=useState<{code:string;id:string}|null>(null),[now,setNow]=useState(Date.now());
 const running=useRef(false),pending=useRef<{path:string;body:string;kind:Action|"CODIGO"}|null>(null),form=useRef<HTMLFormElement>(null),dialog=useRef<HTMLDialogElement>(null);
 const base=`/api/${internal?"operacion":"cliente"}/pedidos/${order.codigoPublico}/entrega`,locked=busy||loading||uncertain;
 async function load(){setLoading(true);setError("");try{setData(await quoteRead(base,isDelivery));}catch(e){setData(null);setError(e instanceof Error?e.message:"No pudimos consultar la entrega.");}finally{setLoading(false);}}
 useEffect(()=>{void load();},[order.codigoPublico,order.version,internal]);
 useEffect(()=>{const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer);},[]);
 useEffect(()=>{if(action)dialog.current?.showModal();else dialog.current?.close();},[action]);
 const ready=data?.estadoPedido==="LISTO_PARA_ENTREGA",finished=!!data?.entrega,valid=!!data?.validacion&&Date.parse(data.validacion.validoHasta)>now;
 const canValidate=ready&&["LISTO_RETIRO","DISPONIBLE_PUNTO",...(order.reserva.modalidad==="ENVIO_DOMICILIO"?["EN_VIAJE"]:[])].includes(data?.estadoLogistico??"");
 const canGenerate=ready&&(!data.codigo||Date.parse(data.codigo.puedeRenovarEn)<=now);
 function choose(a:Action){setError("");setMessage("");setAction(a);}
 async function save(kind:Action|"CODIGO",recover=false){
  if(running.current||!recover&&locked)return;
  if(!recover){if(!data)return;const input:Record<string,unknown>={operacion:crypto.randomUUID(),version:data.version};let path="codigo";
   if(kind!=="CODIGO"){
    if(!form.current?.reportValidity())return;const f=new FormData(form.current);
    if(kind==="VALIDAR"){input.codigo=String(f.get("codigo")).trim().toUpperCase();path="validar";}
    else{input.motivo=f.get("motivo");input.confirmado=f.get("confirmado")==="on";
     if(kind==="ENTREGAR"){input.validacion=data.validacion?.codigoPublico;input.receptor=f.get("receptor");path="confirmar";}
     else if(kind==="CERRAR")path="cerrar";else{input.accion=kind;path="movimientos";}
    }
   }
   pending.current={path:`${base}/${path}`,body:JSON.stringify(input),kind};
  }
  const command=pending.current;if(!command)return;running.current=true;setBusy(true);setError("");
  try{
   const r=await secureMutation(command.path,command.body,"application/json"),result:unknown=await r.json();
   const envelope=result as {vista?:unknown;codigo?:unknown;aviso?:unknown;aceptado?:unknown;mensaje?:unknown};
   const v=command.kind==="CODIGO"||command.kind==="VALIDAR"?envelope.vista:result;
   if(!isDelivery(v)||command.kind==="VALIDAR"&&typeof envelope.aceptado!=="boolean")throw new Error("No pudimos comprobar el resultado guardado.");
   setData(v);pending.current=null;setUncertain(false);setAction(null);
   if(command.kind==="CODIGO"){
    setSecret(typeof envelope.codigo==="string"&&v.codigo?{code:envelope.codigo,id:v.codigo.codigoPublico}:null);
    setMessage(typeof envelope.aviso==="string"?envelope.aviso:"Consultá el estado del código.");
   }else if(command.kind==="VALIDAR"){
    if(envelope.aceptado)setMessage(typeof envelope.mensaje==="string"?envelope.mensaje:"Código validado.");else setError(typeof envelope.mensaje==="string"?envelope.mensaje:"El código no pudo validarse.");
   }else{setMessage(command.kind==="CERRAR"?"Pedido cerrado. La entrega y la conciliación quedaron registradas.":command.kind==="ENTREGAR"?"Entrega física registrada. Falta verificar el cierre administrativo.":"Movimiento de entrega registrado.");onSaved();}
  }catch(e){setError(e instanceof Error?e.message:"No pudimos completar la operación.");if(e instanceof MutationError&&!e.uncertain){pending.current=null;setUncertain(false);}else setUncertain(true);}
  finally{running.current=false;setBusy(false);}
 }
 const feedback=<>{error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<div className="client-warning"><p>No pudimos confirmar la respuesta. Recuperá la misma operación antes de continuar.</p><button type="button" className="client-button" disabled={busy} onClick={()=>void save(pending.current?.kind??"CODIGO",true)}>Recuperar operación de entrega</button></div>}</>;
 return <section id="pedido-entrega" className="client-card delivery-panel" aria-label="Entrega y cierre del pedido">
  <header className="order-management-heading"><h2>{internal?"Logística, entrega y cierre":"Entrega de tu pedido"}</h2><span>{data?logisticsState(data.estadoLogistico):"Consultando"}</span></header>
  <button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar entrega y saldo</button>
  {loading&&<p role="status">Consultando entrega…</p>}{!action&&feedback}{message&&<p role="status" className="client-success">{message}</p>}
  {data&&<>
   <div className="client-grid three delivery-summary"><section><h3>{internal?"Cliente y trabajo":"Trabajo y sucursal"}</h3><p><strong>{data.cliente}</strong>{internal&&data.correo&&<><br/>{data.correo}</>}</p><p>{order.oferta.sucursal.nombre}<br/>{order.archivos.length} PDF · {order.oferta.items.reduce((s,i)=>s+i.trabajo.copias,0)} copias</p><a href="#pedido-pdf">Ver PDF confirmados</a></section>
    <section><h3>Datos de entrega</h3><p><strong>{modeLabels[order.reserva.modalidad]}</strong><br/>{order.reserva.nombre}</p>{!order.reserva.nombre.includes(order.oferta.destino)&&<p>{order.oferta.destino}</p>}<p>{order.reserva.fecha} · {order.reserva.apertura}–{order.reserva.cierre}<br/>{order.reserva.zonaHoraria}</p>{order.contacto&&<p>Recibe: {order.contacto.receptor}<br/>Teléfono: {order.contacto.telefono}</p>}<p className="client-state">{logisticsState(data.estadoLogistico)}</p></section>
    <section><h3>Situación financiera</h3><dl className="delivery-money"><dt>Total</dt><dd>{money(order.oferta.total)}</dd><dt>Acreditado y aplicado</dt><dd>{money(data.aplicado)}</dd><dt>Saldo pendiente</dt><dd><strong>{money(data.saldo)}</strong></dd></dl>{financial&&<a href="#pedido-pagos">Consultar pagos y comprobantes</a>}<p className="client-muted">El código valida la recepción. Los cobros se registran en pagos.</p></section>
   </div>
   {!ready&&!finished&&<p className="client-note">El pedido debe superar producción y calidad antes de habilitar la entrega.</p>}
   {internal&&ready&&<><div className="client-actions">{data.acciones.filter(a=>order.reserva.modalidad!=="RETIRO_SUCURSAL"&&(a.accion!=="LLEGAR_PUNTO"||order.reserva.modalidad==="RETIRO_PUNTO_ENTREGA")).map(a=><button key={a.accion} className="client-button" disabled={locked||!a.habilitada} title={a.motivo??undefined} onClick={()=>choose(a.accion)}>{labels[a.accion]}</button>)}</div>
    {!data.puedeGestionar&&<p className="client-warning">Tu cuenta no tiene permiso para gestionar entregas.</p>}
    {order.reserva.modalidad==="RETIRO_PUNTO_ENTREGA"&&<p className="client-note">La llegada al punto habilita el retiro. La entrega al cliente se confirma por separado.</p>}
    <section className="delivery-code"><h3>Código de recepción</h3><p>{data.codigo?`Código ${data.codigo.estado.toLowerCase()} · ${data.codigo.intentosRestantes} intentos restantes · vence ${date(data.codigo.venceEn)}.`:"El cliente todavía no generó un código desde su pedido."}</p>
     {valid&&<p className="client-success">✓ Código validado por tu sesión hasta {date(data.validacion!.validoHasta)}.</p>}{data.validacion&&!valid&&<p className="client-warning">La validación venció. Comprobá nuevamente el código antes de entregar.</p>}
     <div className="client-actions"><button className="client-button" disabled={locked||!data.puedeGestionar||!canValidate} onClick={()=>choose("VALIDAR")}>Validar código de entrega</button><button className="client-button order-approve" disabled={locked||!valid||data.bloqueosEntrega.length>0} onClick={()=>choose("ENTREGAR")}>Confirmar entrega física</button></div>
     {data.bloqueosEntrega.length>0&&<ul className="client-muted">{data.bloqueosEntrega.map(b=><li key={b}>{b}</li>)}</ul>}
    </section></>}
   {!internal&&ready&&<section className="delivery-code"><h3>Tu código para recibir el pedido</h3><p>Generalo cuando estés por recibir el trabajo y mostralo al personal. Tiene una vigencia de 30 minutos. Cada código nuevo reemplaza al anterior.</p>
    {secret&&data.codigo?.codigoPublico===secret.id&&data.codigo.estado==="ACTIVO"&&Date.parse(data.codigo.venceEn)>now?<div className="delivery-secret"><strong aria-label="Código de entrega">{secret.code}</strong><span>Vence {date(data.codigo.venceEn)}</span></div>:data.codigo&&<p>El código está {data.codigo.estado.toLowerCase()}. Su texto se muestra sólo al generarlo; si ya no lo tenés, generá otro.</p>}
    <button className="client-button" disabled={locked||!canGenerate} onClick={()=>void save("CODIGO")}>{data.codigo?"Generar nuevo código":"Generar código de entrega"}</button>{!canGenerate&&data.codigo&&<p className="client-muted">Podés generar otro desde {date(data.codigo.puedeRenovarEn)}.</p>}
    {Number(data.saldo)>0&&<p className="client-warning">Queda un saldo de {money(data.saldo)} que debe estar acreditado y aplicado antes de recibir el trabajo.</p>}
   </section>}
   {data.entrega&&<section className="delivery-receipt client-success"><h3>Entrega registrada</h3><p>Recibió <strong>{data.entrega.receptor}</strong> · {date(data.entrega.fecha)}<br/>Registró: {data.entrega.responsable}</p>{internal&&<p className="order-text">{data.entrega.motivo}</p>}</section>}
   {internal&&finished&&!data.cierre&&<section className="delivery-close"><h3>Cierre administrativo</h3><p>Comprobá entrega completa, dinero aplicado, excedentes devueltos e informes de transferencia resueltos.</p>{data.bloqueosCierre.length>0&&<ul className="client-warning">{data.bloqueosCierre.map(b=><li key={b}>{b}</li>)}</ul>}<button className="client-button order-approve" disabled={locked||!data.puedeCerrar||data.bloqueosCierre.length>0} onClick={()=>choose("CERRAR")}>Cerrar pedido</button></section>}
   {data.cierre&&<section className="client-success"><h3>Pedido cerrado</h3><p>{date(data.cierre.fecha)} · {data.cierre.responsable}<br/>Importe conciliado al cierre: {money(data.cierre.aplicado)}</p>{internal&&<p className="order-text">{data.cierre.motivo}</p>}</section>}
   {data.historial.length>0&&<details><summary>Historial del recorrido de entrega</summary>{data.historial.map((h,i)=><p className="order-text" key={i}><strong>{logisticsState(h.estadoDestino)} · {date(h.fecha)}</strong><br/>{h.actor} · {h.motivo}</p>)}</details>}
  </>}
  <dialog ref={dialog} className="delivery-dialog" aria-labelledby="delivery-dialog-title" onCancel={e=>{e.preventDefault();if(!locked)setAction(null);}}>
   {action&&data&&<form ref={form} className="client-form" key={action} onSubmit={e=>{e.preventDefault();void save(action);}}><header className="order-management-heading"><h3 id="delivery-dialog-title">{labels[action]}</h3><span>PED-{order.numero}</span></header>{feedback}<fieldset disabled={locked}>
    {action==="VALIDAR"?<><p>Ingresá el código que presenta el cliente. Validarlo no registra la entrega ni modifica los pagos.</p><label>Código de entrega<input name="codigo" autoComplete="off" autoCapitalize="characters" spellCheck={false} required maxLength={8} minLength={8} pattern="[A-HJ-NP-Za-hj-np-z2-9]{8}" placeholder="8 letras y números"/></label></>:<>
     {action==="ENTREGAR"&&<><p>Registrá la recepción física del pedido completo. Esta acción cumple la reserva y utiliza el código validado.</p><label>Nombre de quien recibe<input name="receptor" required maxLength={140} defaultValue={order.contacto?.receptor??data.cliente}/></label></>}
     {action==="CERRAR"&&<p>La entrega está registrada y el total conciliado es {money(data.aplicado)}. El cierre conserva estos hechos y no registra nuevos cobros.</p>}
     {action==="LLEGAR_PUNTO"&&<p>Confirmá la llegada física al punto. El cliente todavía debe retirar su trabajo.</p>}
     {action==="SALIR_REPARTO"&&<p>Confirmá que el pedido salió hacia el destino acordado. Su estado logístico pasará a En viaje.</p>}
     {action==="PREPARAR_ENVIO"&&<p>Confirmá que el pedido completo está preparado para salir al destino acordado.</p>}
     <label>Observaciones internas / motivo<textarea name="motivo" required maxLength={500}/></label><label className="client-check"><input type="checkbox" name="confirmado" required/>{action==="ENTREGAR"?"Confirmo que el cliente recibió físicamente el pedido completo.":action==="CERRAR"?"Verifiqué la entrega y la conciliación para cerrar este pedido.":"Confirmo que este hecho físico ya ocurrió."}</label>
    </>}
    <div className="client-actions"><button type="button" className="client-button order-danger" onClick={()=>setAction(null)}>Volver sin guardar</button><button className={`client-button ${action==="VALIDAR"?"":"order-approve"}`}>{busy?"Guardando…":action==="VALIDAR"?"Validar código":"Confirmar registro"}</button></div>
   </fieldset></form>}
  </dialog>
 </section>;
}
