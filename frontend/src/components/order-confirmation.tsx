//#region ENCABEZADO · src/components/order-confirmation.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/order-confirmation.tsx
 * ========================================================================
 * FUNCIÓN
 * Consulta los requisitos para confirmar una cotización como pedido y envía la confirmación
 * conservando la misma operación cuando corresponde reintentar.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] OrderConfirmation({quote,refresh,compact=false,onLockChange}: {quote:Quote;refresh:number;compact?:boolean;onLockChange?:(locked:boolean)=>void})
 *   Componente de interfaz.
 * - [async] OrderConfirmation :: load()
 * - [async] OrderConfirmation :: confirm(recover = false)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";
import {useRouter} from "next/navigation";
import {useEffect,useRef,useState} from "react";
import {positiveAmount} from "@/lib/client-journey";
import {clearPreparedFiles} from "@/lib/prepared-files";
import {quoteRead,money,type Quote} from "@/lib/quote-types";
import {isOrderReview,isOrder,type OrderReview} from "@/lib/order-types";
import {secureMutation,MutationError} from "@/lib/secure-mutation";
export function OrderConfirmation({quote,refresh,compact=false,onLockChange}:{quote:Quote;refresh:number;compact?:boolean;onLockChange?:(locked:boolean)=>void}){
 const router=useRouter();
 const[review,setReview]=useState<OrderReview|null>(null),[error,setError]=useState(""),[busy,setBusy]=useState(false),[loading,setLoading]=useState(false),[uncertain,setUncertain]=useState(false),[ack,setAck]=useState(false),[receiver,setReceiver]=useState(""),[phone,setPhone]=useState("");
 const running=useRef(false),command=useRef<string|null>(null);const locked=busy||loading||uncertain,path=`/api/cliente/cotizaciones/${quote.codigoPublico}/confirmacion`;
 useEffect(()=>{onLockChange?.(locked);return()=>onLockChange?.(false);},[locked,onLockChange]);
 async function load(){if(running.current)return;setLoading(true);setError("");setReview(null);setAck(false);try{setReview(await quoteRead(path,isOrderReview));}catch(e){setError(e instanceof Error?e.message:"No pudimos revisar las condiciones finales.");}finally{setLoading(false);}}
 useEffect(()=>{void load();},[quote.codigoPublico,quote.version,refresh]);
 async function confirm(recover=false){if(running.current||!recover&&locked)return;if(!recover){if(!review?.habilitada||!ack)return;command.current=JSON.stringify({operacion:crypto.randomUUID(),version:review.version,huella:review.huella,condicionesAceptadas:ack,contacto:quote.oferta.modalidad==="ENVIO_DOMICILIO"?{receptor:receiver.trim(),telefono:phone.trim()}:null});}if(!command.current)return;running.current=true;setBusy(true);setError("");try{const res=await secureMutation(path,command.current,"application/json"),order:unknown=await res.json();if(!isOrder(order)||order.cotizacion!==quote.codigoPublico)throw new Error("No se pudo verificar el pedido guardado.");clearPreparedFiles();router.push(`/cliente/pedidos/${order.codigoPublico}`);}catch(e){setError(e instanceof Error?e.message:"No se pudo confirmar el pedido.");if(e instanceof MutationError&&!e.uncertain){command.current=null;setUncertain(false);setReview(null);setAck(false);}else setUncertain(true);}finally{running.current=false;setBusy(false);}}
 const f=review?.franja,c=review?.condiciones;
 return <section className="client-card order-confirmation"><h2>{review?.pedido?"Tu pedido ya está creado":compact?"Todo listo para crear tu pedido":"Confirmar tu pedido"}</h2><p>{review?.pedido?"Consultá el avance y la entrega desde el pedido.":"Revisá el precio y la entrega definitiva. El pedido y su reserva se crean únicamente al pulsar Crear pedido."}</p>{loading&&<p role="status">Comprobando archivos, condiciones y disponibilidad…</p>}{review?.pedido?<p className="client-success">Esta cotización está vinculada a un pedido. Las correcciones continúan desde allí. <a href={`/cliente/pedidos/${review.pedido}`}>Abrir pedido vinculado</a></p>:<>{review&&f&&c&&<><div className="client-grid"><div><h3>Franja disponible</h3><p>{f.nombre}</p><p><strong>{f.fecha} · {f.apertura}–{f.cierre}</strong><br/>{f.zonaHoraria}</p><p className="client-muted">Plazas libres en esta consulta: {f.plazasLibres}. La ventana es estimada y depende del recorrido de revisión y producción.</p></div><div><h3>Condiciones finales</h3><p>Precio aceptado: <strong>{money(quote.oferta.total)}</strong></p><dl className="client-key-values">{(!compact||positiveAmount(c.actuales.pagoPrevioRequerido))&&<div><dt>Pago previo requerido</dt><dd>{money(c.actuales.pagoPrevioRequerido)}</dd></div>}{(!compact||positiveAmount(c.actuales.senaRequerida))&&<div><dt>Seña requerida</dt><dd>{money(c.actuales.senaRequerida)}</dd></div>}{(!compact||positiveAmount(c.actuales.saldo))&&<div><dt>Saldo antes de la entrega</dt><dd>{money(c.actuales.saldo)}</dd></div>}</dl>{!compact&&<p className="client-muted">Se conservan las condiciones aceptadas y se comprueban los requisitos actuales antes de crear el pedido.</p>}<p>{c.revisionHumana?"El pedido quedará pendiente de revisión humana.":"Las reglas permiten aprobar el pedido al confirmar. Producirlo es un paso posterior."}</p><details><summary>Ver condiciones finales completas</summary>{c.actuales.instrucciones.filter(s=>!s.startsWith("La simulación evalúa")).map((s,i)=><p key={i}>{s.replaceAll("Este ejemplo","Este pedido").replaceAll("este ejemplo","este pedido")}</p>)}</details></div></div>{review.bloqueos.length>0&&<div className="client-warning"><ul>{review.bloqueos.map(s=><li key={s}>{s}</li>)}</ul></div>}<form className="client-form" onSubmit={e=>{e.preventDefault();void confirm();}}><fieldset disabled={locked||!review.habilitada}>{quote.oferta.modalidad==="ENVIO_DOMICILIO"&&<><label>Nombre de quien recibe<input required maxLength={140} autoComplete="name" value={receiver} onChange={e=>setReceiver(e.target.value)}/></label><label>Teléfono de contacto<input required type="tel" minLength={6} maxLength={40} pattern={"(?=(?:[^0-9]*[0-9]){6})[+0-9 \\(\\)\\-]{6,40}"} autoComplete="tel" value={phone} onChange={e=>setPhone(e.target.value)}/></label></>}<label className="client-check"><input type="checkbox" checked={ack} onChange={e=>setAck(e.target.checked)}/>Acepto los PDF seleccionados, el precio guardado, la franja y las condiciones finales del pedido.</label><div className="client-actions"><button className="client-button" disabled={!ack||!review.habilitada||locked}>Crear pedido</button></div></fieldset></form></>}{!uncertain&&<button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar revisión final</button>}</>}{error&&<p role="alert" className="client-warning">{error}</p>}{uncertain&&<div className="client-warning"><p>La respuesta se perdió. Recuperá la misma confirmación para consultar su resultado sin duplicar el pedido.</p><button className="client-button" disabled={busy} onClick={()=>void confirm(true)}>Recuperar confirmación</button></div>}</section>;
}
