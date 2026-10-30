//#region ENCABEZADO · src/components/point-availability-workspace.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/point-availability-workspace.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta y modifica la disponibilidad temporal de puntos de entrega, diferenciándola de su
 * configuración estructural y conservando reintentos seguros.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] PointAvailabilityWorkspace({initial}: {initial:PointAvailabilityPanel})
 *   Componente de interfaz.
 * - [async] PointAvailabilityWorkspace :: refresh()
 * - [async] PointAvailabilityWorkspace :: send(next?: Command)
 * - PointAvailabilityWorkspace :: change(point: OperationalPoint, estado:
 *   "HABILITADO"|"DESHABILITADO")
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Command (tipo).
 * ========================================================================
 */
//#endregion

"use client";
import {useRef,useState} from "react";
import {MutationError,secureMutation} from "@/lib/secure-mutation";
import {catalogDate} from "@/lib/catalog-types";
import {isPointAvailability,isPointAvailabilityPanel,pointStateLabels,type PointAvailabilityPanel,type OperationalPoint} from "@/lib/point-availability-types";
import {ResourceHistory} from "./resource-history";
import {useNavigationGuard} from "./navigation-boundary";
type Command={point:string;body:string};
export function PointAvailabilityWorkspace({initial,internal=false}:{initial:PointAvailabilityPanel;internal?:boolean}){
  const [data,setData]=useState(initial),[query,setQuery]=useState(""),[filter,setFilter]=useState(""),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[conflict,setConflict]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState("");
  const command=useRef<Command|null>(null),sending=useRef(false),locked=busy||uncertain;
  const base=`/api/${internal?"operacion":"admin"}/puntos-entrega/disponibilidad`;
  useNavigationGuard({blocked:locked});
  async function refresh(){if(locked)return;setBusy(true);setError("");setMessage("");try{const r=await fetch(base,{cache:'no-store'});const value:unknown=await r.json();if(!r.ok||!isPointAvailabilityPanel(value))throw new Error("No pudimos consultar el estado actual. Volvé a intentar.");setData(value);setConflict(false);}catch(cause){setError(cause instanceof Error?cause.message:"No pudimos actualizar los puntos.");}finally{setBusy(false);}}
  async function send(next?:Command){if(sending.current)return;if(next)command.current=next;if(!command.current)return;sending.current=true;setBusy(true);setError("");setMessage("");try{
    const c=command.current,r=await secureMutation(`${base}/${c.point}`,c.body,"application/json","PUT"),value:unknown=await r.json();
    if(!isPointAvailability(value)||value.punto!==c.point)throw new Error("No pudimos confirmar la disponibilidad recibida.");
    setData(old=>{const puntos=old.puntos.map(p=>p.codigoPublico===value.punto?{...p,disponibilidad:value}:p);return {...old,puntos,deshabilitados:puntos.filter(p=>p.disponibilidad.estado==="DESHABILITADO").length};});
    command.current=null;setUncertain(false);setConflict(false);setMessage(`Operación confirmada. Estado actual: ${pointStateLabels[value.estado]}. La configuración no fue modificada.`);
  }catch(cause){setError(cause instanceof Error?cause.message:"No pudimos cambiar el estado.");if(cause instanceof MutationError&&!cause.uncertain){command.current=null;setUncertain(false);setConflict(cause.status===409);}else setUncertain(true);}finally{sending.current=false;setBusy(false);}}
  function change(point:OperationalPoint,estado:"HABILITADO"|"DESHABILITADO"){void send({point:point.codigoPublico,body:JSON.stringify({operacion:crypto.randomUUID(),version:point.disponibilidad.version,estado})});}
  const points=data.puntos.filter(p=>(!filter||p.disponibilidad.estado===filter)&&`${p.codigo} ${p.nombre} ${p.direccion}`.toLocaleLowerCase().includes(query.toLocaleLowerCase()));
  return <>
    <p className="admin-info">{data.contexto==="CONFIGURACION_ACTIVA"?"Habilitá o deshabilitá los puntos para nuevas entregas. Cada cambio guarda responsable y fecha, sin editar el motor.":"Sin configuración activa. Podés preparar la disponibilidad de los puntos del borrador; todavía no se ofrecen pedidos ni reservas."}</p>
    <details><summary>Qué cambia al deshabilitar un punto</summary><p className="admin-note">Los cambios son inmediatos y se comprueban al cotizar, confirmar y aceptar una reprogramación. No cancelan ni cambian automáticamente los pedidos ya confirmados.</p></details>
    {data.deshabilitados>0&&<p className="admin-warning" role="status">Recordatorio: {data.deshabilitados} {data.deshabilitados===1?"punto deshabilitado":"puntos deshabilitados"}. Revisá su disponibilidad antes de ofrecer nuevas entregas.</p>}
    <div className="point-list-layout"><section><div className="resource-toolbar availability-toolbar"><div className="catalog-field point-search"><label htmlFor="availability-search">Buscar punto o dirección</label><input id="availability-search" value={query} disabled={locked} onChange={e=>setQuery(e.target.value)}/></div><div className="catalog-field"><label htmlFor="availability-filter">Estado temporal</label><select id="availability-filter" value={filter} disabled={locked} onChange={e=>setFilter(e.target.value)}><option value="">Todos los estados</option>{Object.entries(pointStateLabels).map(([code,label])=><option key={code} value={code}>{label}</option>)}</select></div></div>
    {points.length===0?<p className="admin-empty">{data.puntos.length?"No hay coincidencias.":"No hay puntos en la preparación actual. Agregalos desde la fase 5 del configurador."}</p>:points.map(p=><article className="admin-card point-row availability-row" key={p.codigoPublico}><div className="point-location"><span className="point-location-icon" aria-hidden="true">⌖</span><div><h2>{p.nombre}</h2><p>{p.direccion}</p><small>{p.codigo} · {p.zonaHoraria}</small></div></div><div><span className={`availability-state state-${p.disponibilidad.estado.toLowerCase()}`}>{pointStateLabels[p.disponibilidad.estado]}</span>{p.disponibilidad.actualizadaEn?<p className="admin-note">{catalogDate(p.disponibilidad.actualizadaEn)}<br/>{p.disponibilidad.actor}</p>:<p className="admin-note">El administrador aún no eligió un estado.</p>}</div><div className="availability-actions">{p.disponibilidad.estado!=="HABILITADO"&&<button type="button" className="admin-button" disabled={locked||conflict} onClick={()=>change(p,"HABILITADO")}>Habilitar {p.nombre}</button>}{p.disponibilidad.estado!=="DESHABILITADO"&&<button type="button" className="admin-button secondary" disabled={locked||conflict} onClick={()=>change(p,"DESHABILITADO")}>Deshabilitar {p.nombre}</button>}</div><div className="point-history"><ResourceHistory url={`${base}/${p.codigoPublico}/historial`} revision={p.disponibilidad.version}/></div></article>)}
    </section><aside className="admin-card point-summary"><h2>Resumen de disponibilidad</h2><dl>{Object.entries(pointStateLabels).map(([code,label])=><div key={code}><dt>{label}</dt><dd>{data.puntos.filter(p=>p.disponibilidad.estado===code).length}</dd></div>)}</dl><p className="admin-note">Habilitado indica una decisión temporal. También se necesitan configuración activa, origen, franjas y cupos válidos para ofrecer el punto.</p></aside></div>
    {error&&<p className="admin-warning" role="alert">{error}</p>}{message&&<p className="admin-success" role="status">{message}</p>}
    {uncertain&&<div className="admin-warning"><p>La respuesta no se pudo confirmar. Reintentá el mismo cambio para recuperar su resultado sin duplicarlo.</p><button className="admin-button" disabled={busy} onClick={()=>send()}>Reintentar el mismo cambio</button></div>}
    {conflict&&<p className="admin-warning">Otro cambio impidió aplicar la acción. Actualizá la disponibilidad y volvé a elegir qué hacer.</p>}
    <div className="configuration-actions"><button type="button" className="admin-button secondary" disabled={locked} onClick={refresh}>Actualizar disponibilidad</button>{!internal&&<a className="admin-button secondary" href={locked?undefined:"/administracion/configuracion"} aria-disabled={locked} tabIndex={locked?-1:undefined}>Editar definiciones en el configurador</a>}<a className="admin-button secondary" href={locked?undefined:internal?"/operacion":"/administracion"} aria-disabled={locked} tabIndex={locked?-1:undefined}>Volver al inicio</a></div>
  </>;
}
