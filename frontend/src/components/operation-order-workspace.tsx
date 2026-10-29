"use client";
import {useEffect,useMemo,useRef,useState,type MouseEvent} from 'react';
import {date,money,modeLabels} from '@/lib/quote-types';
import {orderState,type Order} from '@/lib/order-types';
import type {Permission} from '@/lib/organization-types';
import {operationTask,type OperationPanel} from '@/lib/operation-task';
import {orderTrackingLabel} from '@/lib/order-tracking';
import {useNavigationGuard} from './navigation-boundary';
import {OrderManagement,type Management} from './order-management';
import {OrderProduction,type Production} from './order-production';
import {OrderDelivery,type Delivery} from './order-delivery';
import {OrderCorrections} from './order-corrections';
import {OrderReschedule} from './order-reschedule';
import {PaymentPanel} from './payment-panel';
import {PrivateFiles} from './private-files';
import {ClientOrderFilePreview} from './client-order-file-preview';
import '@/app/operation-order.css';

const panels:Record<OperationPanel,string>={'pedido-resumen':'Historia del pedido','pedido-revision':'Revisión y decisiones','pedido-correcciones':'Corrección con el cliente','pedido-produccion':'Producción y calidad','pedido-entrega':'Logística y entrega','pedido-reprogramacion':'Nueva fecha de entrega','pedido-pdf':'PDF confirmados','pedido-pagos':'Caja y comprobantes'};

export function OperationOrderWorkspace({order,permissions,financial,globalAdmin,busy,error,refresh,onRefresh,onSaved}:{order:Order;permissions:Permission[];financial:boolean;globalAdmin:boolean;busy:boolean;error:string;refresh:number;onRefresh:()=>void;onSaved:()=>void}){
 const[review,setReview]=useState<Management|null>(null),[production,setProduction]=useState<Production|null>(null),[delivery,setDelivery]=useState<Delivery|null>(null);
 const[requested,setRequested]=useState<OperationPanel|null>(null),[selected,setSelected]=useState(''),[locks,setLocks]=useState<Partial<Record<OperationPanel,boolean>>>({}),[notice,setNotice]=useState('');
 const lockCallbacks=useMemo(()=>Object.fromEntries(Object.keys(panels).map(key=>[key,(value:boolean)=>setLocks(old=>old[key as OperationPanel]===value?old:{...old,[key]:value})])) as Record<OperationPanel,(value:boolean)=>void>,[]);
 const blocked=Object.values(locks).some(Boolean);
 useEffect(()=>{if(!blocked)setNotice('');},[blocked]);
 useNavigationGuard({blocked});
 const task=operationTask(order,permissions,{review,production,delivery});
 const heading=useRef<HTMLHeadingElement>(null),navigation=useRef({blocked:false,current:'pedido-resumen' as OperationPanel});
 const desired=requested&&!(requested==='pedido-pagos'&&!financial)?requested:task.panel;
 const current=blocked?navigation.current.current:desired;navigation.current={blocked,current};
 useEffect(()=>{
  function read(){const key=location.hash.slice(1) as OperationPanel;if(!Object.hasOwn(panels,key)){setRequested(null);return;}if(navigation.current.blocked&&key!==navigation.current.current){history.replaceState(null,'',location.pathname+location.search+'#'+navigation.current.current);setNotice('Hay una operación pendiente. Esperá su resultado o recuperá el mismo intento antes de cambiar de tarea.');return;}setNotice('');setRequested(key);}
  read();window.addEventListener('hashchange',read);return()=>window.removeEventListener('hashchange',read);
 },[order.codigoPublico]);
 useEffect(()=>{if(!requested)return;const frame=requestAnimationFrame(()=>{heading.current?.scrollIntoView({block:'start'});heading.current?.focus({preventScroll:true});});return()=>cancelAnimationFrame(frame);},[requested]);
 function protect(event:MouseEvent){const anchor=event.target instanceof Element?event.target.closest<HTMLAnchorElement>('a[href]'):null;if(blocked&&anchor?.getAttribute('href')?.startsWith('#')){event.preventDefault();setNotice('Terminá o recuperá la operación pendiente antes de cambiar de tarea.');}}
 const back=globalAdmin?'/administracion/pedidos':`/operacion/sucursales/${order.oferta.sucursal.codigoPublico}`;
 const work=order.oferta.items.find(i=>i.codigoPublico===selected)??order.oferta.items[0],file=work?order.archivos.find(f=>f.item===work.codigoPublico):undefined;
 const currentReview=review?.version===order.version?review:null,currentDelivery=delivery?.version===order.version?delivery:null;
 return <div className="operation-order" onClickCapture={protect}>
  <header className="operation-order-heading"><div><a href={back} className="operation-back">← Volver a la bandeja de pedidos</a><h2>Pedido PED-{order.numero}</h2><p>{order.oferta.sucursal.nombre} · Recibido el {date(order.confirmadaEn)}</p></div><div><span className="client-state">{orderTrackingLabel(order)}</span><button type="button" className="client-button secondary" disabled={busy||blocked} onClick={onRefresh}>{busy?'Actualizando…':'Actualizar tareas'}</button></div></header>
  {error&&<p role="alert" className="client-warning">{error}</p>}{notice&&<p role="alert" className="client-warning">{notice}</p>}
  <div className="operation-order-grid">
   <div className="operation-order-main">
    <section className={`operation-task-heading${task.waiting?' operation-task-waiting':''}`} aria-label="Siguiente tarea del pedido"><span>{task.waiting?'SITUACIÓN ACTUAL':'PARA HACER AHORA'}</span><h2>{task.title}</h2><p>{task.description}</p>{requested&&current!==task.panel&&<a href={'#'+task.panel}>Volver a la tarea actual →</a>}</section>
    <section className="operation-task-area" aria-labelledby="operation-panel-title"><h2 id="operation-panel-title" ref={heading} tabIndex={-1}>{panels[current]}</h2>
     {/* Keep forms mounted: changing tools must not discard drafts or idempotent retries. */}
     <div id="pedido-resumen" hidden={current!=='pedido-resumen'}><p className="operation-readonly-note">{task.waiting?'Podés consultar el trabajo y su recorrido mientras el equipo responsable continúa con el pedido.':'Los hechos registrados quedan en este historial.'}</p><ol className="order-timeline">{order.historial.map((event,index)=><li key={index}><strong>{orderState(event.estado,order.reserva.modalidad)}</strong><span>{date(event.fecha)}</span><p>{event.motivo}</p></li>)}</ol>{currentReview?.observaciones.length? <details><summary>Notas internas ({currentReview.observaciones.length})</summary>{currentReview.observaciones.map(n=><article className="order-private-note" key={n.codigoPublico}><strong>{n.importancia} · {n.autor}</strong><p>{date(n.fecha)}</p><p>{n.texto}</p></article>)}</details>:null}</div>
     <div id="pedido-revision" hidden={current!=='pedido-revision'}><OrderManagement order={order} internal workspace refresh={refresh} onSaved={onSaved} onSnapshot={setReview} onLockChange={lockCallbacks['pedido-revision']}/></div>
     <div hidden={current!=='pedido-correcciones'}><OrderCorrections order={order} internal financial={financial} refresh={refresh} onSaved={onSaved} onLockChange={lockCallbacks['pedido-correcciones']}/></div>
     <div hidden={current!=='pedido-produccion'}><OrderProduction order={order} workspace refresh={refresh} onSaved={onSaved} onSnapshot={setProduction} onLockChange={lockCallbacks['pedido-produccion']}/></div>
     <div hidden={current!=='pedido-entrega'}><OrderDelivery order={order} internal financial={financial} workspace refresh={refresh} onSaved={onSaved} onSnapshot={setDelivery} onLockChange={lockCallbacks['pedido-entrega']}/></div>
     <div hidden={current!=='pedido-reprogramacion'}><OrderReschedule order={order} internal workspace refresh={refresh} onSaved={onSaved} onLockChange={lockCallbacks['pedido-reprogramacion']}/></div>
     <div id="pedido-pdf" hidden={current!=='pedido-pdf'} className="workspace-review-body operation-pdf-panel"><PrivateFiles quote={order.cotizacion} version={order.version} refresh={refresh} internal compact confirmed snapshot={order.archivos.map(f=>f.archivo)} onLockChange={lockCallbacks['pedido-pdf']}/></div>
     {financial&&<div id="pedido-pagos" hidden={current!=='pedido-pagos'}><PaymentPanel quote={order.cotizacion} internal refresh={refresh} onSaved={onSaved} onLockChange={lockCallbacks['pedido-pagos']}/></div>}
    </section>
    <details className="operation-other-tools"><summary>Otras tareas de este pedido</summary><div><a href="#pedido-resumen">Consultar historial</a>{permissions.includes('GESTIONAR_PEDIDOS')&&<><a href="#pedido-revision">Revisión, decisiones y notas</a><a href="#pedido-correcciones">Solicitar o consultar una corrección</a></>}{(permissions.includes('GESTIONAR_PRODUCCION')||permissions.includes('CONTROLAR_CALIDAD'))&&<a href="#pedido-produccion">Consultar trabajos y controles de calidad</a>}<a href="#pedido-entrega">Consultar el recorrido de entrega</a>{permissions.includes('GESTIONAR_ENTREGAS')&&<a href="#pedido-reprogramacion">Proponer otra fecha de entrega</a>}</div></details>
   </div>
   <aside className="operation-order-context" aria-label="Trabajo y datos para operar">
    <section className="operation-context-card operation-work-card"><h2>Trabajo a realizar</h2>{order.oferta.items.length>1&&<label>PDF del pedido<select aria-label="PDF del pedido" value={work?.codigoPublico??''} onChange={e=>setSelected(e.target.value)}>{order.oferta.items.map((item,index)=><option key={item.codigoPublico} value={item.codigoPublico}>{index+1}. {item.documento.nombre}</option>)}</select></label>}{work&&<><h3>{file?.nombre??work.documento.nombre}</h3><dl><div><dt>Páginas y copias</dt><dd>{work.trabajo.paginas} páginas × {work.trabajo.copias} copias</dd></div><div><dt>Impresión</dt><dd>{work.formato} · {work.trabajo.color==='COLOR'?'Color':'Blanco y negro'} · {work.trabajo.dobleFaz?'Doble faz':'Simple faz'}</dd></div><div><dt>Papel</dt><dd>{work.papel}</dd></div><div><dt>Adicionales</dt><dd>{work.precio.lineas.filter(l=>work.trabajo.terminaciones.includes(l.servicio)).map(l=>l.nombre).join(' · ')||'Sin adicionales'}</dd></div></dl>{file&&<ClientOrderFilePreview internal quote={order.cotizacion} version={order.version} file={file.archivo}/>}</>}<a href="#pedido-pdf">Abrir todos los PDF confirmados ({order.archivos.length})</a></section>
    <section className="operation-context-card operation-order-facts"><h2>Cliente y entrega</h2><p><strong>{currentReview?.cliente??currentDelivery?.cliente??'Consultando datos…'}</strong>{currentReview?.correo&&<><br/>{currentReview.correo}</>}</p><div><h3>{order.estadoReserva==='LIBERADA'?'Reserva liberada':order.estadoReserva==='CUMPLIDA'?'Entrega realizada':'Entrega acordada'}</h3><p><strong>{modeLabels[order.reserva.modalidad]}</strong><br/>{order.reserva.nombre}</p><p>{order.reserva.fecha} · {order.reserva.apertura}–{order.reserva.cierre}</p>{order.contacto&&<p>Recibe: {order.contacto.receptor}<br/>{order.contacto.telefono}</p>}<a href="#pedido-entrega">Ver logística y recepción →</a></div><div><h3>Importe del pedido</h3><strong className="operation-order-total">{money(order.oferta.total)}</strong><p>{order.oferta.medioPago==='EFECTIVO'?'Efectivo':'Transferencia'}</p>{financial&&currentDelivery&&<p>Saldo pendiente: <strong>{money(currentDelivery.saldo)}</strong></p>}{financial&&<a href="#pedido-pagos">Gestionar caja y comprobantes →</a>}</div></section>
   </aside>
  </div>
 </div>;
}
