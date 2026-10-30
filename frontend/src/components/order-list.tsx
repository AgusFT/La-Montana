"use client";
import {useEffect,useRef,useState} from 'react';
import {quoteRead,money,date,modeLabels} from '@/lib/quote-types';
import {isOrderList,orderStates,orderState,logisticsState,type OrderList} from '@/lib/order-types';
import {recentMovementLabel} from '@/lib/order-operational-status';
import {secureMutation,MutationError} from '@/lib/secure-mutation';
import {useNavigationGuard} from './navigation-boundary';
import {OrderListFilters} from './order-list-filters';
import {OrderOperationalBadge} from './order-operational-badge';
import '@/app/client.css';
import '@/app/order-operations-list.css';

type GroupAction='PREPARAR_ENVIO'|'SALIR_REPARTO';
export function OrderListView({branch,globalAdmin=false,initialState=''}:{branch?:string;globalAdmin?:boolean;initialState?:string}){
 const internal=!!branch||globalAdmin,requestId=useRef(0),running=useRef(false),dialog=useRef<HTMLDialogElement>(null);
 const[state,setState]=useState(initialState),[sort,setSort]=useState(internal?'MOVIMIENTO_DESC':'CONFIRMADO_DESC');
 const[data,setData]=useState<OrderList|null>(null),[error,setError]=useState(''),[busy,setBusy]=useState(false),[message,setMessage]=useState('');
 const[groupAction,setGroupAction]=useState<GroupAction>('PREPARAR_ENVIO'),[selected,setSelected]=useState<string[]>([]),[open,setOpen]=useState(false),[saving,setSaving]=useState(false),[uncertain,setUncertain]=useState(false),[groupError,setGroupError]=useState('');
 const command=useRef<string|null>(null),locked=busy||saving||uncertain;
 useNavigationGuard({blocked:saving||uncertain});
 useEffect(()=>setState(initialState),[initialState]);
 async function load(page=0){
  const current=++requestId.current;setBusy(true);setError('');setData(null);setSelected([]);
  try{const value=await quoteRead(`${globalAdmin?'/api/admin/pedidos':branch?`/api/operacion/sucursales/${branch}/pedidos`:'/api/cliente/pedidos'}?pagina=${page}${state?`&estado=${state}`:''}${internal?`&orden=${sort}`:''}`,isOrderList);if(current===requestId.current)setData(value);}
  catch(e){if(current===requestId.current)setError(e instanceof Error?e.message:'No pudimos consultar los pedidos.');}
  finally{if(current===requestId.current)setBusy(false);}
 }
 useEffect(()=>{void load();return()=>{requestId.current++;};},[branch,globalAdmin,state,sort]);
 useEffect(()=>{if(open)dialog.current?.showModal();else dialog.current?.close();},[open]);
 const batching=internal&&state==='LISTO_PARA_ENTREGA'&&data?.puedeGestionarEntregas;
 const eligible=data?.elementos.filter(p=>p.accionEntrega===groupAction&&Number.isSafeInteger(p.version))??[];
 const picked=data?.elementos.filter(p=>selected.includes(p.codigoPublico))??[];
 function chooseAction(action:GroupAction){setGroupAction(action);setSelected([]);setMessage('');}
 function openGroup(){command.current=null;setGroupError('');setOpen(true);}
 async function saveGroup(){
  if(running.current)return;
  if(!command.current)command.current=JSON.stringify({operacion:crypto.randomUUID(),accion:groupAction,pedidos:picked.map(p=>({pedido:p.codigoPublico,version:p.version}))});
  running.current=true;setSaving(true);setGroupError('');
  try{
   const response=await secureMutation('/api/operacion/entregas/lotes',command.current,'application/json');const result:unknown=await response.json();
   if(!result||typeof result!=='object'||!('cantidad' in result)||typeof result.cantidad!=='number')throw new Error('No pudimos comprobar el resultado.');
   setMessage(`${result.cantidad} pedidos ${groupAction==='SALIR_REPARTO'?'despachados y en viaje':'preparados para despachar'}. Se registraron tu usuario, fecha y hora en cada pedido.`);
   command.current=null;setUncertain(false);setOpen(false);await load(data?.pagina??0);
  }catch(e){const unknown=!(e instanceof MutationError)||e.uncertain;setUncertain(unknown);setGroupError(unknown?'No pudimos confirmar la respuesta. Recuperá el mismo intento para comprobar el grupo sin duplicarlo.':e.message);if(!unknown)command.current=null;}
  finally{running.current=false;setSaving(false);}
 }
 return <section className="client-card order-operations-list">
  <h2>{globalAdmin?'Todos los pedidos de la imprenta':branch?'Pedidos de esta sucursal':'Mis pedidos'}</h2>
  <p>{internal?'Consultá qué necesita cada pedido y su último movimiento. Los despachos conjuntos están en Entregas pendientes.':'Pedidos confirmados con sus archivos y reserva.'}</p>
  {busy&&<p role="status">Consultando pedidos…</p>}{error&&<p role="alert" className="client-warning">{error}</p>}{message&&<p role="status" className="client-success">{message}</p>}
  {internal?<OrderListFilters state={state} busy={locked} onChange={setState} onRefresh={()=>void load(data?.pagina??0)}/>:<div className="client-actions"><label>Filtrar por estado <select aria-label="Filtrar por estado" value={state} disabled={locked} onChange={e=>setState(e.target.value)}><option value="">Todos los pedidos</option>{orderStates.map(s=><option key={s} value={s}>{orderState(s)}</option>)}</select></label><button className="client-button secondary" disabled={locked} onClick={()=>void load(data?.pagina??0)}>Actualizar pedidos</button></div>}
  {internal&&<label className="order-list-sort">Ordenar pedidos<select value={sort} disabled={locked} onChange={e=>setSort(e.target.value)}><option value="MOVIMIENTO_DESC">Último movimiento · más recientes primero</option><option value="MOVIMIENTO_ASC">Último movimiento · más antiguos primero</option><option value="CONFIRMADO_ASC">Confirmación · más antiguos primero</option><option value="CONFIRMADO_DESC">Confirmación · más recientes primero</option></select></label>}
  {batching&&<section className="order-batch-bar" aria-label="Gestión conjunta de entregas"><div><h3>Preparar y despachar varios pedidos</h3><p>Elegí la tarea y seleccioná los pedidos del mismo reparto. Sólo se habilitan los que aprobaron calidad y están en el paso correspondiente.</p></div><div className="client-actions" role="group" aria-label="Tarea del grupo"><button type="button" className={`client-button ${groupAction==='PREPARAR_ENVIO'?'':'secondary'}`} aria-pressed={groupAction==='PREPARAR_ENVIO'} disabled={locked} onClick={()=>chooseAction('PREPARAR_ENVIO')}>1. Preparar para despacho</button><button type="button" className={`client-button ${groupAction==='SALIR_REPARTO'?'':'secondary'}`} aria-pressed={groupAction==='SALIR_REPARTO'} disabled={locked} onClick={()=>chooseAction('SALIR_REPARTO')}>2. Poner en viaje</button></div><div className="order-batch-selection"><span>{selected.length} seleccionados · {eligible.length} disponibles en esta página</span><button type="button" className="client-button order-batch-confirm" disabled={locked||!selected.length} onClick={openGroup}>{groupAction==='SALIR_REPARTO'?'Despachar':'Preparar'} seleccionados ({selected.length})</button></div></section>}
  {data?.total===0&&<p className="client-empty">No hay pedidos con este filtro.</p>}
  {!!data?.total&&<><div className="client-table-wrap"><table className="client-table"><thead><tr>
   {batching&&<th><input type="checkbox" aria-label="Seleccionar todos los pedidos disponibles de esta página" checked={eligible.length>0&&selected.length===eligible.length} disabled={locked||!eligible.length} onChange={e=>setSelected(e.target.checked?eligible.map(p=>p.codigoPublico):[])}/></th>}
   <th>Pedido</th><th>{internal?'Situación actual':'Estado'}</th><th>{internal?'Destino de entrega':'Sucursal'}</th><th>Entrega</th><th>Confirmado</th>{internal&&<th>Último movimiento</th>}<th>Total</th><th>Acción</th>
  </tr></thead><tbody>{data.elementos.map(p=><tr key={p.codigoPublico} className={selected.includes(p.codigoPublico)?'order-row-selected':undefined}>
   {batching&&<td><input type="checkbox" aria-label={`Seleccionar PED-${p.numero}`} title={p.accionEntrega!==groupAction?'Este pedido no está disponible para la tarea seleccionada.':undefined} disabled={locked||!eligible.some(x=>x.codigoPublico===p.codigoPublico)} checked={selected.includes(p.codigoPublico)} onChange={e=>setSelected(old=>e.target.checked?[...old,p.codigoPublico]:old.filter(id=>id!==p.codigoPublico))}/></td>}
   <td className="order-list-id">PED-{p.numero}</td><td>{internal?<OrderOperationalBadge order={p}/>:<span className="client-state">{p.estado==='LISTO_PARA_ENTREGA'?logisticsState(p.estadoLogistico):orderState(p.estado,p.modalidad)}</span>}</td><td>{internal?p.destino??'Consultar en el pedido':p.sucursal}</td><td>{p.retiroConTraslado?'Retiro con traslado':modeLabels[p.modalidad]}</td><td>{date(p.confirmadaEn)}</td>
   {internal&&<td className="order-last-movement"><strong>{recentMovementLabel(p.ultimoMovimiento?.accion,p.modalidad)}</strong><time dateTime={p.ultimoMovimiento?.fecha??p.confirmadaEn}>{date(p.ultimoMovimiento?.fecha??p.confirmadaEn)}</time>{p.ultimoMovimiento?.actor&&<small>{p.ultimoMovimiento.actor}</small>}</td>}
   <td>{money(p.total)}</td><td><a className="client-button secondary" href={`/${internal?'operacion':'cliente'}/pedidos/${p.codigoPublico}`}>Ver PED-{p.numero}</a></td>
  </tr>)}</tbody></table></div><div className="client-actions"><button className="client-button secondary" disabled={locked||data.pagina===0} onClick={()=>void load(data.pagina-1)}>Anteriores</button><span>Página {data.pagina+1} · {data.total} pedidos</span><button className="client-button secondary" disabled={locked||(data.pagina+1)*25>=data.total} onClick={()=>void load(data.pagina+1)}>Siguientes</button></div></>}
  <dialog ref={dialog} className="order-batch-dialog" aria-labelledby="order-batch-title" onCancel={e=>{e.preventDefault();if(!saving&&!uncertain)setOpen(false);}}>
   {open&&<form onSubmit={e=>{e.preventDefault();void saveGroup();}}><h2 id="order-batch-title">{groupAction==='SALIR_REPARTO'?'Despachar el grupo · poner en viaje':'Preparar el grupo para despacho'}</h2><p>{groupAction==='SALIR_REPARTO'?'Confirmá cuando estos pedidos hayan salido juntos de la imprenta. Se genera el código de cada pedido y queda disponible para su cliente.':'Confirmá cuando estos pedidos estén completos y preparados para salir.'}</p><ul className="order-batch-manifest">{picked.map(p=><li key={p.codigoPublico}><strong>PED-{p.numero}</strong><span>{p.destino}</span></li>)}</ul><p className="client-muted">Se registra tu usuario, fecha y hora en cada pedido. Si alguno cambió o perdió las condiciones necesarias, el grupo completo queda sin modificar.</p>{groupError&&<p role="alert" className="client-warning">{groupError}</p>}<div className="client-actions"><button type="button" className="client-button secondary" disabled={saving||uncertain} onClick={()=>setOpen(false)}>Volver</button><button type="submit" className="client-button order-batch-confirm" disabled={saving}>{saving?'Registrando…':uncertain?'Recuperar el mismo intento':groupAction==='SALIR_REPARTO'?`Confirmar salida de ${picked.length} pedidos`:`Confirmar preparación de ${picked.length} pedidos`}</button></div></form>}
  </dialog>
 </section>;
}
