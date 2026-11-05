"use client";
import {useEffect,useRef,useState} from 'react';
import {quoteRead,money,date,modeLabels} from '@/lib/quote-types';
import {isOrderList,type OrderList} from '@/lib/order-types';
import {recentMovementLabel} from '@/lib/order-operational-status';
import {secureMutation,MutationError} from '@/lib/secure-mutation';
import {useNavigationGuard} from './navigation-boundary';
import {OrderListFilters} from './order-list-filters';
import {OrderOperationalBadge} from './order-operational-badge';
import {ClientOrderBadge} from './client-order-badge';
import '@/app/client.css';
import '@/app/order-operations-list.css';
import '@/app/rider-assignment.css';

const customerViews=[
 {value:'CURSO',label:'En curso',empty:'No tenés pedidos en preparación.'},
 {value:'VIAJE',label:'En viaje',empty:'No tenés pedidos en viaje.'},
 {value:'RETIRO',label:'Listos para retirar',empty:'No tenés pedidos listos para retirar.'},
 {value:'HISTORIAL',label:'Historial',empty:'Todavía no hay pedidos en tu historial.'},
];

type GroupAction='PREPARAR_ENVIO'|'SALIR_REPARTO';
type DispatchFilter=GroupAction|'SEGUIMIENTO'|'TODOS';
const dispatchFilters:{value:DispatchFilter;label:string}[]=[
 {value:'PREPARAR_ENVIO',label:'1. Preparar para despacho'},
 {value:'SALIR_REPARTO',label:'2. Poner en viaje'},
 {value:'SEGUIMIENTO',label:'En viaje o en destino'},
 {value:'TODOS',label:'Todos'},
];
type Rider={codigoPublico:string;nombre:string;correo:string};
function isRiders(value:unknown):value is Rider[]{return Array.isArray(value)&&value.every(r=>!!r&&typeof r==='object'&&typeof r.codigoPublico==='string'&&typeof r.nombre==='string'&&typeof r.correo==='string');}
export function OrderListView({branch,globalAdmin=false,initialState=''}:{branch?:string;globalAdmin?:boolean;initialState?:string}){
 const internal=!!branch||globalAdmin,requestId=useRef(0),running=useRef(false),dialog=useRef<HTMLDialogElement>(null);
 const[customerView,setCustomerView]=useState('CURSO');
 const[state,setState]=useState(initialState),[sort,setSort]=useState(internal?'MOVIMIENTO_DESC':'CONFIRMADO_DESC');
 const[data,setData]=useState<OrderList|null>(null),[error,setError]=useState(''),[busy,setBusy]=useState(false),[message,setMessage]=useState('');
 const[dispatchFilter,setDispatchFilter]=useState<DispatchFilter>('PREPARAR_ENVIO');
 const[selected,setSelected]=useState<string[]>([]),[open,setOpen]=useState(false),[saving,setSaving]=useState(false),[uncertain,setUncertain]=useState(false),[groupError,setGroupError]=useState('');
 const[riders,setRiders]=useState<Rider[]>([]),[ridersBusy,setRidersBusy]=useState(false),[selectedRider,setSelectedRider]=useState('');
 const command=useRef<string|null>(null),locked=busy||saving||uncertain;
 const deliveryQueue=internal&&state==='LISTO_PARA_ENTREGA';
 const dispatch=deliveryQueue&&dispatchFilter!=='TODOS'?dispatchFilter:null;
 const groupAction:GroupAction=dispatchFilter==='SALIR_REPARTO'?'SALIR_REPARTO':'PREPARAR_ENVIO';
 useNavigationGuard({blocked:saving||uncertain});
 useEffect(()=>setState(initialState),[initialState]);
 async function load(page=0){
  const current=++requestId.current;setBusy(true);setError('');setData(null);setSelected([]);
  try{const value=await quoteRead(`${globalAdmin?'/api/admin/pedidos':branch?`/api/operacion/sucursales/${branch}/pedidos`:'/api/cliente/pedidos'}?pagina=${page}${state?`&estado=${state}`:''}${internal?`&orden=${sort}`:''}${dispatch?`&despacho=${dispatch}`:''}${!internal?`&vista=${customerView}`:''}`,isOrderList);if(current===requestId.current)setData(value);}
  catch(e){if(current===requestId.current)setError(e instanceof Error?e.message:'No pudimos consultar los pedidos.');}
  finally{if(current===requestId.current)setBusy(false);}
 }
 useEffect(()=>{void load();return()=>{requestId.current++;};},[branch,globalAdmin,state,sort,dispatch,customerView]);
 useEffect(()=>{if(open)dialog.current?.showModal();else dialog.current?.close();},[open]);
 const batching=deliveryQueue&&dispatch===groupAction&&data?.puedeGestionarEntregas;
 const eligible=data?.elementos.filter(p=>p.accionEntrega===groupAction&&Number.isSafeInteger(p.version))??[];
 const picked=data?.elementos.filter(p=>selected.includes(p.codigoPublico))??[];
 function chooseDispatchFilter(filter:DispatchFilter){
  if(filter===dispatchFilter)return;
  setDispatchFilter(filter);setData(null);setSelected([]);setSelectedRider('');setMessage('');
 }
 async function openGroup(){command.current=null;setGroupError('');
  if(groupAction==='SALIR_REPARTO'){setRidersBusy(true);try{const available=await quoteRead('/api/operacion/repartidores',isRiders);setRiders(available);if(available.length===1)setSelectedRider(available[0].codigoPublico);}catch(e){setRiders([]);setGroupError(e instanceof Error?e.message:'No pudimos consultar los riders activos.');}finally{setRidersBusy(false);}}
  setOpen(true);
 }
 async function saveGroup(){
  if(running.current)return;
  if(groupAction==='SALIR_REPARTO'&&!selectedRider){setGroupError('Elegí quién recibe estos pedidos e inicia el recorrido.');return;}
  if(!command.current)command.current=JSON.stringify({operacion:crypto.randomUUID(),accion:groupAction,pedidos:picked.map(p=>({pedido:p.codigoPublico,version:p.version})),repartidor:groupAction==='SALIR_REPARTO'?selectedRider:null});
  running.current=true;setSaving(true);setGroupError('');
  try{
   const response=await secureMutation('/api/operacion/entregas/lotes',command.current,'application/json');const result:unknown=await response.json();
   if(!result||typeof result!=='object'||!('cantidad' in result)||typeof result.cantidad!=='number')throw new Error('No pudimos comprobar el resultado.');
   const assigned=result&&typeof result==='object'&&'repartidor' in result&&result.repartidor&&typeof result.repartidor==='object'&&'nombre' in result.repartidor?String(result.repartidor.nombre):'';
   setMessage(`${result.cantidad} pedidos ${groupAction==='SALIR_REPARTO'?`asignados a ${assigned||'el rider'} y puestos en viaje`:'preparados para despachar'}. Se registraron tu usuario, fecha y hora en cada pedido.`);
   command.current=null;setUncertain(false);setOpen(false);await load();
  }catch(e){const unknown=!(e instanceof MutationError)||e.uncertain;setUncertain(unknown);setGroupError(unknown?'No pudimos confirmar la respuesta. Recuperá el mismo intento para comprobar el grupo sin duplicarlo.':e.message);if(!unknown)command.current=null;}
  finally{running.current=false;setSaving(false);}
 }
 return <section id={internal?"despachos":undefined} className="client-card order-operations-list">
  <h2>{globalAdmin?'Todos los pedidos de la imprenta':branch?'Pedidos de esta sucursal':'Mis pedidos'}</h2>
  <p>{internal?'Consultá qué necesita cada pedido y su último movimiento. Los despachos conjuntos están en Entregas pendientes.':'Pedidos confirmados con sus archivos y reserva.'}</p>
  {busy&&<p role="status">Consultando pedidos…</p>}{error&&<p role="alert" className="client-warning">{error}</p>}{message&&<p role="status" className="client-success">{message}</p>}
  {internal?<OrderListFilters state={state} busy={locked} onChange={setState} onRefresh={()=>void load(data?.pagina??0)}/>:<div className="client-order-views">
   <div className="client-actions" role="group" aria-label="Vistas de mis pedidos">
    {customerViews.map(({value,label})=><button key={value} type="button"
     className={`client-button ${customerView===value?'':'secondary'}`} aria-pressed={customerView===value}
     onClick={()=>{if(customerView!==value){setData(null);setCustomerView(value);}}}>{label}</button>)}
   </div>
   <button type="button" className="client-text-action" disabled={busy} onClick={()=>void load(data?.pagina??0)}>Actualizar pedidos</button>
  </div>}
  {internal&&<label className="order-list-sort">Ordenar pedidos<select value={sort} disabled={locked} onChange={e=>setSort(e.target.value)}><option value="MOVIMIENTO_DESC">Último movimiento · más recientes primero</option><option value="MOVIMIENTO_ASC">Último movimiento · más antiguos primero</option><option value="CONFIRMADO_ASC">Confirmación · más antiguos primero</option><option value="CONFIRMADO_DESC">Confirmación · más recientes primero</option></select></label>}
  {deliveryQueue&&<section className="order-batch-bar" aria-label="Bandejas de despacho">
   <div><h3>Despacho y seguimiento</h3><p>Elegí el paso del reparto que querés consultar.</p></div>
   <div className="client-actions" role="group" aria-label="Filtrar entregas pendientes">
    {dispatchFilters.map(({value,label})=><button key={value} type="button"
     className={`client-button ${dispatchFilter===value?'':'secondary'}`} aria-pressed={dispatchFilter===value}
     disabled={saving||uncertain} onClick={()=>chooseDispatchFilter(value)}>{label}</button>)}
   </div>
   {batching&&<div className="order-batch-selection"><span>{selected.length} seleccionados · {data.total} disponibles en total</span><button type="button" className="client-button order-batch-confirm" disabled={locked||!selected.length} onClick={openGroup}>{groupAction==='SALIR_REPARTO'?'Despachar':'Preparar'} seleccionados ({selected.length})</button></div>}
  </section>}
  {data?.total===0&&<p className="client-empty">{internal?"No hay pedidos con este filtro.":customerViews.find(view=>view.value===customerView)?.empty}</p>}
  {!!data?.total&&<><div className="client-table-wrap"><table className="client-table"><thead><tr>
   {batching&&<th><input type="checkbox" aria-label="Seleccionar todos los pedidos disponibles de esta página" checked={eligible.length>0&&selected.length===eligible.length} disabled={locked||!eligible.length} onChange={e=>setSelected(e.target.checked?eligible.map(p=>p.codigoPublico):[])}/></th>}
   <th>Pedido</th><th>{internal?'Situación actual':'Estado'}</th><th>{internal?'Destino de entrega':'Sucursal'}</th><th>Entrega</th><th>Confirmado</th>{internal&&<th>Último movimiento</th>}<th>Total</th><th>Acción</th>
  </tr></thead><tbody>{data.elementos.map(p=><tr key={p.codigoPublico} className={selected.includes(p.codigoPublico)?'order-row-selected':undefined}>
   {batching&&<td><input type="checkbox" aria-label={`Seleccionar PED-${p.numero}`} title={p.accionEntrega!==groupAction?'Este pedido no está disponible para la tarea seleccionada.':undefined} disabled={locked||!eligible.some(x=>x.codigoPublico===p.codigoPublico)} checked={selected.includes(p.codigoPublico)} onChange={e=>setSelected(old=>e.target.checked?[...old,p.codigoPublico]:old.filter(id=>id!==p.codigoPublico))}/></td>}
   <td className="order-list-id">PED-{p.numero}{internal&&p.nombreInvitado&&<><br/><small>{p.nombreInvitado} · Invitado</small></>}</td><td>{internal?<OrderOperationalBadge order={p}/>:<ClientOrderBadge order={p}/>}</td><td>{internal?p.destino??'Consultar en el pedido':p.sucursal}</td><td>{p.retiroConTraslado?'Retiro en otra sucursal':modeLabels[p.modalidad]}</td><td>{date(p.confirmadaEn)}</td>
   {internal&&<td className="order-last-movement"><strong>{recentMovementLabel(p.ultimoMovimiento?.accion,p.modalidad)}</strong><time dateTime={p.ultimoMovimiento?.fecha??p.confirmadaEn}>{date(p.ultimoMovimiento?.fecha??p.confirmadaEn)}</time>{p.ultimoMovimiento?.actor&&<small>{p.ultimoMovimiento.actor}</small>}</td>}
   <td>{money(p.total)}</td><td><a className="client-button secondary" href={`/${internal?'operacion':'cliente'}/pedidos/${p.codigoPublico}`}>Ver PED-{p.numero}</a></td>
  </tr>)}</tbody></table></div><div className="client-actions"><button className="client-button secondary" disabled={locked||data.pagina===0} onClick={()=>void load(data.pagina-1)}>Anteriores</button><span>Página {data.pagina+1} · {data.total} pedidos</span><button className="client-button secondary" disabled={locked||(data.pagina+1)*25>=data.total} onClick={()=>void load(data.pagina+1)}>Siguientes</button></div></>}
  <dialog ref={dialog} closedby="any" className="order-batch-dialog" aria-labelledby="order-batch-title" onCancel={e=>{e.preventDefault();if(!saving&&!uncertain)setOpen(false);}}>
   {open&&<form onSubmit={e=>{e.preventDefault();void saveGroup();}}><h2 id="order-batch-title">{groupAction==='SALIR_REPARTO'?'Asignar rider e iniciar reparto':'Preparar el grupo para despacho'}</h2><p>{groupAction==='SALIR_REPARTO'?'Elegí al rider que está recibiendo físicamente estos pedidos. Al confirmar, el recorrido aparecerá en su cuenta y todos los pedidos pasarán a En viaje.':'Confirmá cuando estos pedidos estén completos y preparados para salir.'}</p>{groupAction==='SALIR_REPARTO'&&<label className="order-batch-rider">Rider responsable<select required aria-label="Rider responsable" value={selectedRider} disabled={saving||uncertain||ridersBusy} onChange={e=>{setSelectedRider(e.target.value);command.current=null;}}><option value="">{ridersBusy?'Consultando riders…':'Seleccioná un rider activo'}</option>{riders.map(r=><option key={r.codigoPublico} value={r.codigoPublico}>{r.nombre} · {r.correo}</option>)}</select>{!ridersBusy&&!riders.length&&<small>No hay riders activos. Habilitá un empleado como rider desde Administración → Empleados.</small>}</label>}<ul className="order-batch-manifest">{picked.map(p=><li key={p.codigoPublico}><strong>PED-{p.numero}</strong><span>{p.destino}</span></li>)}</ul><p className="client-muted">Se registra tu usuario, el rider, la fecha y la hora. Si algún pedido cambió o perdió las condiciones necesarias, el grupo completo queda sin modificar.</p>{groupError&&<p role="alert" className="client-warning">{groupError}</p>}<div className="client-actions"><button type="button" className="client-button secondary" disabled={saving||uncertain} onClick={()=>setOpen(false)}>Volver</button><button type="submit" className="client-button order-batch-confirm" disabled={saving||ridersBusy||(groupAction==='SALIR_REPARTO'&&(!selectedRider||!riders.length))}>{saving?'Registrando…':uncertain?'Recuperar el mismo intento':groupAction==='SALIR_REPARTO'?`Asignar e iniciar reparto · ${picked.length} pedidos`:`Confirmar preparación de ${picked.length} pedidos`}</button></div></form>}
  </dialog>
 </section>;
}
