'use client';
import {useEffect,useRef,useState,type FormEvent} from 'react';
import {MutationError,secureMutation} from '@/lib/secure-mutation';
import {useNavigationGuard} from './navigation-boundary';
import {isNetworkPoint,isPointNetwork,pointAddress,type PointNetwork,type NetworkPoint,type PointDefinition,type PointSource,type PointWindow} from '@/lib/point-network';
import {isPointAvailability,isPointAvailabilityPanel,pointReceivesOrders,type PointAvailabilityPanel} from '@/lib/point-availability-types';
import {ResourceHistory} from './resource-history';
import {PointScheduleTools} from './point-schedule-tools';
import {catalogDate} from '@/lib/catalog-types';
import styles from './point-network-workspace.module.css';

type Command={path:string;method:'POST'|'PUT';body:string;network:boolean};
const days=['','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'];
const empty:PointDefinition={codigoPublico:'',codigo:'',nombre:'',calle:'',numero:'',localidad:'',provincia:'',codigoPostal:'',referencias:'',zonaHoraria:'America/Argentina/Buenos_Aires',sucursales:[]};
export function PointNetworkWorkspace({initial,initialAvailability,initialBranch=''}:{initial:PointNetwork|null;initialAvailability:PointAvailabilityPanel|null;initialBranch?:string}){
 const[data,setData]=useState(initial),[availability,setAvailability]=useState(initialAvailability),[query,setQuery]=useState(''),[branch,setBranch]=useState(initialBranch);
 const[editor,setEditor]=useState<NetworkPoint|'NEW'|null>(null),[form,setForm]=useState<PointDefinition>(empty),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[conflict,setConflict]=useState(false),[error,setError]=useState(''),[message,setMessage]=useState('');
 const command=useRef<Command|null>(null),sending=useRef(false),editorHeading=useRef<HTMLHeadingElement|null>(null),opener=useRef<HTMLButtonElement|null>(null),focusPending=useRef(false),locked=busy||uncertain;
 const[noticePoint,setNoticePoint]=useState<string|null>(null);
 const editorCode=editor==='NEW'?'new':editor?.definicion.codigoPublico;
 useEffect(()=>{
  if(locked||!focusPending.current)return;
  const target=editorCode?editorHeading.current:opener.current;
  if(!target?.isConnected)return;
  focusPending.current=false;
  target.focus({preventScroll:true});
  target.scrollIntoView({block:editorCode?'start':'nearest',behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth'});
 },[editorCode,locked]);
 useNavigationGuard({blocked:locked,dirty:!!editor&&JSON.stringify(form)!==JSON.stringify(editor==='NEW'?empty:editor?.definicion)});
 async function load(){
  const responses=await Promise.all([fetch('/api/admin/red-puntos',{cache:'no-store'}),fetch('/api/admin/puntos-entrega/disponibilidad',{cache:'no-store'})]);
  const [network,states]:unknown[]=await Promise.all(responses.map(r=>r.json()));
  if(!responses.every(r=>r.ok)||!isPointNetwork(network)||!isPointAvailabilityPanel(states))throw new Error('No pudimos consultar la red de entrega.');setData(network);setAvailability(states);
 }
 async function refresh(){if(locked)return;setNoticePoint(null);setBusy(true);setError('');try{await load();}catch(e){setError(e instanceof Error?e.message:'No pudimos actualizar.');}finally{setBusy(false);}}
 useEffect(()=>{if(!initial||!initialAvailability)void refresh();},[]);
 useEffect(()=>{const focus=()=>{if(!sending.current&&!editor&&!uncertain)void load().catch(()=>{});};window.addEventListener('focus',focus);return()=>window.removeEventListener('focus',focus);},[editor,uncertain]);
 function open(point:NetworkPoint|'NEW',trigger:HTMLButtonElement){if(locked)return;focusPending.current=true;opener.current=trigger;setNoticePoint(point==='NEW'?null:point.definicion.codigoPublico);setEditor(point);setForm(point==='NEW'?{...empty}:structuredClone(point.definicion));setError('');setMessage('');setConflict(false);}
 function close(){if(locked)return;focusPending.current=true;setEditor(null);setError('');setMessage('');}
 async function send(next?:Command){
  if(sending.current)return;if(next)command.current=next;const c=command.current;if(!c)return;
  sending.current=true;setBusy(true);setError('');setMessage('');
  try{
   const response=await secureMutation(c.path,c.body,'application/json',c.method),value:unknown=await response.json();
   if(c.network?(!isNetworkPoint(value)||value.definicion.codigo!==JSON.parse(c.body).codigo||value.version<=JSON.parse(c.body).version):(!isPointAvailability(value)||value.punto!==c.path.split("/").at(-1)))throw new Error('No pudimos confirmar la respuesta.');
   command.current=null;setUncertain(false);setConflict(false);if(editor)focusPending.current=true;setEditor(null);if(c.network)window.dispatchEvent(new Event("lamontana:point-network-changed"));setMessage(c.network?'Punto guardado. La red actualizada se aplica a pedidos nuevos.':'Disponibilidad del punto registrada.');
   try{await load();}catch{setError('El cambio quedó registrado. Actualizá la lista para consultar su estado.');}
  }catch(e){setError(e instanceof Error?e.message:'No pudimos guardar el cambio.');if(e instanceof MutationError&&!e.uncertain){command.current=null;setUncertain(false);setConflict(e.status===409&&editor!=='NEW');if(e.status===409)try{await load();}catch{}}else setUncertain(true);}
  finally{sending.current=false;setBusy(false);}
 }
 function save(e:FormEvent){e.preventDefault();if(locked||conflict||!editor)return;
  const {codigoPublico:identity,...definition}=form;
  const body={...definition,operacion:crypto.randomUUID(),version:editor==='NEW'?0:editor.version,codigo:form.codigo.trim().toUpperCase()};
  void send({path:'/api/admin/red-puntos'+(editor==='NEW'?'':`/${editor.definicion.codigoPublico}`),method:editor==='NEW'?'POST':'PUT',body:JSON.stringify(body),network:true});
 }
 function source(id:string,patch:Partial<PointSource>){setForm(f=>({...f,sucursales:f.sucursales.map(s=>s.sucursal===id?{...s,...patch}:s)}));}
 function toggle(id:string,checked:boolean){setForm(f=>({...f,sucursales:checked?[...f.sucursales.filter(s=>s.sucursal!==id),{...(f.sucursales.find(s=>s.sucursal===id)??{sucursal:id,prioridad:Math.max(0,...f.sucursales.map(s=>s.prioridad))+1,costo:'',franjas:[]}),habilitado:true}]:f.sucursales.filter(s=>s.sucursal!==id)}));}
 function windowChange(id:string,index:number,patch:Partial<PointWindow>){const route=form.sucursales.find(s=>s.sucursal===id);if(route)source(id,{franjas:route.franjas.map((f,n)=>n===index?{...f,...patch}:f)});}
 const points=data?.puntos.filter(p=>(!branch||p.definicion.sucursales.some(s=>s.sucursal===branch&&s.habilitado))&&`${p.definicion.nombre} ${pointAddress(p.definicion)}`.toLocaleLowerCase().includes(query.toLocaleLowerCase()))??[];
 const feedback=<>{error&&<p className="admin-warning" role="alert">{error}</p>}{message&&<p className="admin-success" role="status">{message}</p>}{uncertain&&<button type="button" className="admin-button" disabled={busy} onClick={()=>void send()}>Recuperar el mismo cambio</button>}</>;
 const editorPanel=editor&&<form id={`point-network-editor-${editorCode}`} aria-labelledby={`point-network-heading-${editorCode}`} className={`admin-card admin-form ${styles.editor}`} onSubmit={save}><h2 ref={editorHeading} tabIndex={-1} id={`point-network-heading-${editorCode}`}>{editor==='NEW'?'Agregar punto de entrega':`Editar ${editor.definicion.nombre}`}</h2><p>Los pedidos confirmados conservan su dirección, ruta y condiciones. La pausa de una sucursal inhabilita sus puntos exclusivos; un punto compartido puede seguir por otro origen disponible.</p>
   <fieldset className="admin-fieldset" disabled={locked}><div className={styles.fields}>
    {([['codigo','Código del punto',40],['nombre','Nombre',160],['calle','Calle',160],['numero','Número',20],['localidad','Localidad',120],['provincia','Provincia',120],['codigoPostal','Código postal',12]] as const).map(([key,label,max])=><label key={key}>{label}<input required maxLength={max} readOnly={key==='codigo'&&editor!=='NEW'} value={form[key]} onChange={e=>setForm(f=>({...f,[key]:e.target.value}))}/></label>)}
    <label>Zona horaria<select required value={form.zonaHoraria} onChange={e=>setForm(f=>({...f,zonaHoraria:e.target.value}))}>{[...new Set(['America/Argentina/Buenos_Aires',...data?.sucursales.map(b=>b.zonaHoraria)??[],form.zonaHoraria])].map(z=><option key={z} value={z}>{z}</option>)}</select></label>
    <label>Referencias (opcional)<input maxLength={2000} value={form.referencias??''} onChange={e=>setForm(f=>({...f,referencias:e.target.value}))}/></label>
   </div><h3>Sucursales que pueden abastecer este punto</h3>
   <p>Elegí una o varias. La prioridad 1 se intenta primero; las otras se usan sólo si tienen una ruta habilitada y pueden producir el trabajo.</p>
   {data?.sucursales.filter(b=>b.estado==='ACTIVA'||form.sucursales.some(s=>s.sucursal===b.codigoPublico)).map(b=>{
    const route=form.sucursales.find(s=>s.sucursal===b.codigoPublico);
    return <section className={styles.route} key={b.codigoPublico}><label className="admin-check"><input type="checkbox" checked={route?.habilitado===true} disabled={b.estado!=='ACTIVA'&&!route} onChange={e=>toggle(b.codigoPublico,e.target.checked)}/> {b.nombre}{b.pausada?' · momentáneamente fuera de servicio':b.estado!=='ACTIVA'?' · desactivada':''}</label>{route?.habilitado&&<>
     <div className={styles.routeFields}><label>Prioridad<input type="number" required min={1} max={999} value={route.prioridad} onChange={e=>source(b.codigoPublico,{prioridad:Number(e.target.value)})}/></label><label>Costo del traslado (ARS)<input type="number" required min={0} step="0.01" max="999999999999" value={route.costo} onChange={e=>source(b.codigoPublico,{costo:e.target.value})}/></label></div>
     <PointScheduleTools key={`${editor==='NEW'?'new':editor.definicion.codigoPublico}:${b.codigoPublico}`} branches={data.sucursales} sources={form.sucursales} route={route} onChange={franjas=>source(b.codigoPublico,{franjas})}/>
     <details open><summary>Horarios de retiro y cupos · {route.franjas.length} franjas</summary><div className={styles.actions}><button type="button" className="admin-button secondary" onClick={()=>source(b.codigoPublico,{franjas:[...route.franjas,{dia:1,apertura:'',cierre:'',capacidadPedidos:0,habilitada:true}]})}>Agregar franja</button></div><p className={styles.muted}>Indicá el cupo de pedidos de cada franja. Cero deja esa franja sin capacidad.</p>
     {route.franjas.map((f,n)=><div className={styles.window} key={n}><label>Día<select value={f.dia} onChange={e=>windowChange(b.codigoPublico,n,{dia:Number(e.target.value)})}>{days.slice(1).map((d,i)=><option key={d} value={i+1}>{d}</option>)}</select></label><label>Desde<input type="time" required value={f.apertura} onChange={e=>windowChange(b.codigoPublico,n,{apertura:e.target.value})}/></label><label>Hasta<input type="time" required value={f.cierre} onChange={e=>windowChange(b.codigoPublico,n,{cierre:e.target.value})}/></label><label>Cupo<input type="number" required min={0} max={1000000} value={f.capacidadPedidos} onChange={e=>windowChange(b.codigoPublico,n,{capacidadPedidos:Number(e.target.value)})}/></label><label className="admin-check"><input type="checkbox" checked={f.habilitada} onChange={e=>windowChange(b.codigoPublico,n,{habilitada:e.target.checked})}/>Habilitada</label><button type="button" className="admin-button secondary" onClick={()=>source(b.codigoPublico,{franjas:route.franjas.filter((_,i)=>i!==n)})}>Quitar</button></div>)}</details>
     {!route.franjas.some(f=>f.habilitada&&f.capacidadPedidos>0)&&<p className="admin-note">Agregá al menos una franja con cupo para recibir pedidos desde esta sucursal.</p>}
    </>}</section>;
   })}</fieldset>
   {conflict&&editor!=='NEW'&&<div className="admin-warning"><p>Hay una edición más reciente. Revisá la lista y recuperá su versión antes de volver a guardar.</p><button type="button" className="admin-button secondary" disabled={locked} onClick={()=>{const latest=data?.puntos.find(p=>p.definicion.codigoPublico===editor.definicion.codigoPublico);if(latest){setEditor(latest);setConflict(false);}}}>Usar versión actual conservando mi formulario</button></div>}
   <div className={styles.save}>{editor!=='NEW'&&feedback}<div className={styles.actions}><button type="button" className="admin-button secondary" disabled={locked} onClick={close}>Cancelar edición</button><button className="admin-button" disabled={locked||conflict||!form.sucursales.some(s=>s.habilitado)}>{busy?'Guardando…':'Guardar punto y sucursales'}</button></div></div>
  </form>;
 return <>
  <div className={styles.toolbar}><div><h2>Red de puntos de entrega</h2><p>Agregá puntos y elegí qué sucursales los abastecen. Cada origen tiene prioridad, costo y horarios de retiro.</p></div><div className={styles.actions}><button className="admin-button secondary" disabled={locked} onClick={()=>void refresh()}>Actualizar lista</button><button className="admin-button" disabled={locked||!!editor||!data?.sucursales.some(b=>b.estado==='ACTIVA')} onClick={e=>open('NEW',e.currentTarget)}>Agregar punto</button></div></div>
  {!data?.sucursales.length&&<p className="admin-info">Creá primero una sucursal para asignar el punto.</p>}
  {data&&!data.configuracionActiva&&<p className="admin-info">Podés preparar la red ahora. Se ofrecerá a los clientes cuando haya una configuración operativa activa.</p>}
  {!noticePoint&&feedback}
  {editor==='NEW'&&editorPanel}
  <div className={`admin-form ${styles.filters}`}><label>Buscar punto o dirección<input value={query} disabled={locked||!!editor} onChange={e=>setQuery(e.target.value)}/></label><label>Sucursal de origen<select value={branch} disabled={locked||!!editor} onChange={e=>setBranch(e.target.value)}><option value="">Todas las sucursales</option>{data?.sucursales.map(b=><option key={b.codigoPublico} value={b.codigoPublico}>{b.nombre}</option>)}</select></label></div>
  <div className={styles.list}>{points.map(p=>{
   const editing=editorCode===p.definicion.codigoPublico;
   const d=p.definicion,state=availability?.puntos.find(s=>s.codigoPublico===d.codigoPublico),enabled=state?.disponibilidad.estado==='HABILITADO',available=!!state&&pointReceivesOrders(state);
   return <article className={`admin-card ${editing?styles.editingCard:''}`} key={d.codigoPublico}><div className={styles.row}><div><h2>{d.nombre}</h2><p>{pointAddress(d)}</p><small>{d.codigo}</small>{p.actor&&p.fecha&&<p className={styles.muted}>Red v{p.version} · {p.actor} · {catalogDate(p.fecha)}</p>}</div><div><span className={`${styles.badge} ${available?styles.available:styles.unavailable}`}>{available?'Recibe pedidos nuevos':'Momentáneamente fuera de servicio'}</span><p className={styles.muted}>Sucursales de origen</p><div className={styles.sources}>{d.sucursales.filter(s=>s.habilitado).sort((a,b)=>a.prioridad-b.prioridad).map(s=>{const b=data?.sucursales.find(b=>b.codigoPublico===s.sucursal);return <span className={styles.source} key={s.sucursal}>{s.prioridad}. {b?.nombre??'Sucursal no disponible'}{b?.pausada?' · pausada':''}</span>;})}</div></div><div className={styles.actions}><button className="admin-button" aria-expanded={editing} aria-controls={editing?`point-network-editor-${d.codigoPublico}`:undefined} disabled={locked||!!editor&&!editing} onClick={e=>editing?close():open(p,e.currentTarget)}>{editing?'Cerrar edición':'Editar datos y sucursales'}</button>{state&&<button className="admin-button secondary" disabled={locked||!!editor} onClick={()=>{setNoticePoint(d.codigoPublico);void send({path:`/api/admin/puntos-entrega/disponibilidad/${d.codigoPublico}`,method:'PUT',body:JSON.stringify({operacion:crypto.randomUUID(),version:state.disponibilidad.version,estado:enabled?'DESHABILITADO':'HABILITADO'}),network:false});}}>{enabled?'Pausar el punto':'Quitar pausa propia'}</button>}</div></div>{editing&&editorPanel}{noticePoint===d.codigoPublico&&!editing&&feedback}{state&&<ResourceHistory url={`/api/admin/puntos-entrega/disponibilidad/${d.codigoPublico}/historial`} revision={state.disponibilidad.version}/>}</article>;
  })}{data&&!points.length&&<p className="admin-info">No hay puntos para esta búsqueda. Podés agregar uno y asignar sus sucursales.</p>}</div>
 </>;
}
