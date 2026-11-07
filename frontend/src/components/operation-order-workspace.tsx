"use client";
import {useEffect,useMemo,useRef,useState,type MouseEvent} from 'react';
import {date,money,modeLabels} from '@/lib/quote-types';
import {orderState,orderEventLabel,type Order} from '@/lib/order-types';
import type {Permission} from '@/lib/organization-types';
import {operationTask,type OperationPanel} from '@/lib/operation-task';
import {BackToOrders} from './back-to-orders';
import {OperationStageProgress} from './operation-stage-progress';
import {orderTrackingLabel} from '@/lib/order-tracking';
import {productionSituation} from '@/lib/order-operational-status';
import {OrderOperationalBadge} from './order-operational-badge';
import {OrderWorkOverview} from './order-work-overview';
import {productionWorkProgress} from '@/lib/production-work-progress';
import {useNavigationGuard} from './navigation-boundary';
import {OrderManagement,type Management} from './order-management';
import {OrderProduction,type Production} from './order-production';
import {OrderDelivery,type Delivery} from './order-delivery';
import {OrderCorrections} from './order-corrections';
import {OrderReschedule} from './order-reschedule';
import {PaymentPanel} from './payment-panel';
import {OrderReceiptAction} from './order-receipt-action';
import {PrivateFiles} from './private-files';
import {ClientOrderFilePreview} from './client-order-file-preview';
import '@/app/operation-order.css';

const panels:Record<OperationPanel,string>={'pedido-resumen':'Historia del pedido','pedido-revision':'Revisión y decisiones','pedido-correcciones':'Corrección con el cliente','pedido-produccion':'Producción y calidad','pedido-entrega':'Logística y entrega','pedido-reprogramacion':'Nueva fecha de entrega','pedido-pdf':'PDF confirmados','pedido-pagos':'Caja y comprobantes'};

export function OperationOrderWorkspace({order,permissions,financial,globalAdmin,busy,error,refresh,onRefresh,onSaved}:{order:Order;permissions:Permission[];financial:boolean;globalAdmin:boolean;busy:boolean;error:string;refresh:number;onRefresh:()=>void;onSaved:()=>void}){
 const[review,setReview]=useState<Management|null>(null),[production,setProduction]=useState<Production|null>(null),[delivery,setDelivery]=useState<Delivery|null>(null);
 const[requested,setRequested]=useState<OperationPanel|null>(null),[selected,setSelected]=useState(''),[locks,setLocks]=useState<Partial<Record<OperationPanel,boolean>>>({}),[notice,setNotice]=useState('');
 const lockCallbacks=useMemo(()=>Object.fromEntries(Object.keys(panels).map(key=>[key,(value:boolean)=>setLocks(old=>old[key as OperationPanel]===value?old:{...old,[key]:value})])) as Record<OperationPanel,(value:boolean)=>void>,[]);
 const [receiptLocked,setReceiptLocked]=useState(false);
 const blocked=receiptLocked||Object.values(locks).some(Boolean);
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
 const back=globalAdmin?'/administracion/pedidos':`/operacion/sucursales/${order.reserva.origen}`;
 const progress=productionWorkProgress(order,production);
 const defaultWork=progress.rows.find(r=>r.status.code!=='approved')?.id;
 const work=order.oferta.items.find(i=>i.codigoPublico===selected)??order.oferta.items.find(i=>i.codigoPublico===defaultWork)??order.oferta.items[0],file=work?order.archivos.find(f=>f.item===work.codigoPublico):undefined;
 const carillas=order.oferta.items.reduce((total,item)=>total+item.precio.carillas,0);
 const totalPrevio=Math.max(Number(order.condiciones.cotizadas.pagoPrevioRequerido),Number(order.condiciones.actuales.pagoPrevioRequerido));
 const sena=Math.max(Number(order.condiciones.cotizadas.senaRequerida),Number(order.condiciones.actuales.senaRequerida));
 const receipt=financial&&<OrderReceiptAction base={`/api/operacion/cotizaciones/${order.cotizacion}/pagos`} medio={order.oferta.medioPago} saldo={delivery?.saldo} disabled={blocked||busy} onSaved={onSaved} onLockChange={setReceiptLocked}/>;
 const currentReview=review?.version===order.version?review:null,currentDelivery=delivery?.version===order.version?delivery:null;
 return <div className="operation-order operation-order-simple" onClickCapture={protect}>
  <header className="operation-order-heading"><div><BackToOrders href={back}/><div className="operation-order-number"><h2>Pedido PED-{order.numero}</h2>{order.estado==='EN_PRODUCCION'&&production?.version===order.version?<OrderOperationalBadge order={{estado:order.estado,estadoLogistico:order.estadoLogistico,modalidad:order.reserva.modalidad,...productionSituation(production)}}/>:<span className="client-state">{orderTrackingLabel(order)}</span>}</div><p>{order.oferta.sucursal.nombre} · Recibido el {date(order.confirmadaEn)}</p></div><button type="button" className="client-button secondary" disabled={busy||blocked} onClick={onRefresh}>{busy?'Actualizando…':'Actualizar'}</button></header>
  <OperationStageProgress order={order} production={production}/>
  {error&&<p role="alert" className="client-warning">{error}</p>}{notice&&<p role="alert" className="client-warning">{notice}</p>}
  <OrderWorkOverview order={order} production={production} selected={work?.codigoPublico??''} disabled={blocked} onSelect={setSelected}/>
  <div className="operation-summary-grid" aria-label="Resumen del pedido">
   <section className="operation-context-card"><h2>Cliente</h2><p className="operation-customer"><strong>{order.nombreInvitado??currentReview?.cliente??currentDelivery?.cliente??'Consultando datos…'}</strong></p>{order.nombreInvitado?<p>Invitado sin cuenta</p>:(currentReview?.correo??currentDelivery?.correo)&&<p>{currentReview?.correo??currentDelivery?.correo}</p>}{order.contacto&&<p>Recibe: {order.contacto.receptor}<br/>{order.contacto.telefono}</p>}</section>
   <section className="operation-context-card" id="operation-selected-work"><h2>Detalles del trabajo{order.oferta.items.length>1&&<> {order.oferta.items.findIndex(i=>i.codigoPublico===work?.codigoPublico)+1} de {order.oferta.items.length}</>}</h2>{order.oferta.items.length>1&&<p><strong>{work?.documento.nombre}</strong></p>}{work&&<dl><div><dt>Páginas y copias</dt><dd>{work.trabajo.paginas} × {work.trabajo.copias}</dd></div><div><dt>Carillas impresas</dt><dd>{work.precio.carillas} en {work.precio.hojas} hojas</dd></div><div><dt>Impresión</dt><dd>{work.formato} · {work.trabajo.color==='COLOR'?'Color':'Blanco y negro'} · {work.trabajo.dobleFaz?'Doble faz':'Simple faz'}</dd></div><div><dt>Papel</dt><dd>{work.papel}</dd></div><div><dt>Adicionales</dt><dd>{work.precio.lineas.filter(l=>work.trabajo.terminaciones.includes(l.servicio)).map(l=>l.nombre).join(' · ')||'Sin adicionales'}</dd></div></dl>}</section>
   <section className="operation-context-card operation-document-card"><h2>Documentos</h2>{file?<><div className="operation-document"><span aria-hidden="true">PDF</span><div><strong>{file.nombre}</strong><small>{file.paginas} páginas · {(file.bytes/1024).toFixed(1)} KiB</small></div></div><ClientOrderFilePreview internal quote={order.cotizacion} version={order.version} file={file.archivo}/><a href={'/api/operacion/cotizaciones/'+order.cotizacion+'/archivos/'+file.archivo+'/original'}>Descargar PDF confirmado</a></>:<p>Sin PDF asociado a este ítem.</p>}{order.archivos.length>1&&<a href="#pedido-pdf">Ver todos los documentos ({order.archivos.length})</a>}</section>
   <section className="operation-context-card"><h2>{order.estadoReserva==='LIBERADA'?'Reserva liberada':order.estadoReserva==='CUMPLIDA'?'Entrega realizada':'Detalles de entrega'}</h2><p><strong>{modeLabels[order.reserva.modalidad]}</strong></p><p>{order.reserva.nombre}</p><p>{order.reserva.fecha} · {order.reserva.apertura}–{order.reserva.cierre}</p><a href="#pedido-entrega">Ver entrega →</a></section>
   <section className="operation-context-card operation-financial-card"><h2>Resumen económico</h2><dl><div><dt>Total</dt><dd>{money(order.oferta.total)}</dd></div><div><dt>Medio de pago</dt><dd>{order.oferta.medioPago==='EFECTIVO'?'Efectivo':'Transferencia'}</dd></div>
   <div><dt>Carillas del pedido</dt><dd>{carillas}</dd></div>
   {totalPrevio>0&&<div><dt>Pago total previo</dt><dd>{money(totalPrevio.toFixed(2))}</dd></div>}
   {sena>0&&<div><dt>Seña requerida</dt><dd>{money(sena.toFixed(2))}</dd></div>}
   {totalPrevio===0&&sena===0&&<div><dt>Anticipo</dt><dd>No requerido</dd></div>}
   {currentDelivery&&<><div><dt>Acreditado y aplicado</dt><dd>{money(currentDelivery.aplicado)}</dd></div><div className="operation-balance"><dt>Saldo pendiente</dt><dd>{money(currentDelivery.saldo)}</dd></div></>}</dl>{financial&&<a href="#pedido-pagos">{order.nombreInvitado?"Registrar cobro →":"Ir a cobros →"}</a>}</section>
   <section className="operation-context-card"><h2>Historial del pedido</h2><ol className="operation-short-history">{order.historial.slice(-3).map((event,index)=><li key={index}><strong>{orderEventLabel(event,order.reserva.modalidad)}</strong><span>{date(event.fecha)}</span></li>)}</ol><a href="#pedido-resumen">Ver historial completo →</a></section>
  </div>
  <section className={'operation-action-shell'+(task.waiting?' operation-waiting':'')} aria-labelledby="operation-panel-title">
   <header><div><h2 id="operation-panel-title" ref={heading} tabIndex={-1}>{current===task.panel?task.title:panels[current]}</h2><p>{task.description}</p></div>{current!=='pedido-revision'&&receipt}{current!==task.panel&&<a href={'#'+task.panel}>Volver a la tarea actual →</a>}{task.waiting&&financial&&current!=='pedido-pagos'&&['PENDIENTE_REVISION','APROBADO','EN_PRODUCCION'].includes(order.estado)&&<a className="client-button secondary" href="#pedido-pagos">Ir a cobros</a>}</header>
   <div className="operation-task-area">
    {/* Forms stay mounted to preserve drafts and recover the same uncertain operation. */}
    <div id="pedido-resumen" hidden={current!=='pedido-resumen'}><ol className="order-timeline">{order.historial.map((event,index)=><li key={index}><strong>{orderState(event.estado,order.reserva.modalidad)}</strong><span>{date(event.fecha)}</span><p>{event.motivo}</p>{event.actor&&<p>Registró: <strong>{event.actor}</strong></p>}</li>)}</ol></div>
    <div id="pedido-revision" hidden={current!=='pedido-revision'}><OrderManagement order={order} internal workspace adjacentAction={current==='pedido-revision'?receipt:null} refresh={refresh} onSaved={onSaved} onSnapshot={setReview} onLockChange={lockCallbacks['pedido-revision']}/></div>
    <div hidden={current!=='pedido-correcciones'}><OrderCorrections order={order} internal financial={financial} refresh={refresh} onSaved={onSaved} onLockChange={lockCallbacks['pedido-correcciones']}/></div>
    <div hidden={current!=='pedido-produccion'}><OrderProduction order={order} workspace refresh={refresh} onSaved={()=>{setSelected('');onSaved();}} onSelectItem={setSelected} onSnapshot={setProduction} onLockChange={lockCallbacks['pedido-produccion']}/></div>
    <div hidden={current!=='pedido-entrega'}><OrderDelivery order={order} internal financial={financial} workspace dispatchHref={`${back}?estado=LISTO_PARA_ENTREGA`} refresh={refresh} onSaved={view=>{if(view?.reciboEfectivo){setRequested('pedido-entrega');history.replaceState(null,'',location.pathname+location.search+'#pedido-entrega');}onSaved();}} onSnapshot={setDelivery} onLockChange={lockCallbacks['pedido-entrega']}/></div>
    <div hidden={current!=='pedido-reprogramacion'}><OrderReschedule order={order} internal workspace refresh={refresh} onSaved={onSaved} onLockChange={lockCallbacks['pedido-reprogramacion']}/></div>
    <div id="pedido-pdf" hidden={current!=='pedido-pdf'} className="workspace-review-body operation-pdf-panel"><PrivateFiles quote={order.cotizacion} version={order.version} refresh={refresh} internal compact confirmed snapshot={order.archivos.map(f=>f.archivo)} onLockChange={lockCallbacks['pedido-pdf']}/></div>
    {financial&&<div id="pedido-pagos" hidden={current!=='pedido-pagos'}><PaymentPanel quote={order.cotizacion} internal refresh={refresh} onSaved={onSaved} onLockChange={lockCallbacks['pedido-pagos']}/></div>}
   </div>
  </section>
  <details className="operation-other-tools"><summary>Historial, notas y otras gestiones</summary><div><a href="#pedido-resumen">Historial completo</a>{permissions.includes('GESTIONAR_PEDIDOS')&&<><a href="#pedido-revision">Decisiones y notas internas</a>{!order.nombreInvitado&&<a href="#pedido-correcciones">Correcciones</a>}</>}{(permissions.includes('GESTIONAR_PRODUCCION')||permissions.includes('CONTROLAR_CALIDAD'))&&<a href="#pedido-produccion">Trabajos y controles de calidad</a>}<a href="#pedido-entrega">Entrega y cierre</a>{!order.nombreInvitado&&permissions.includes('GESTIONAR_ENTREGAS')&&<a href="#pedido-reprogramacion">Proponer otra fecha</a>}<a href="#pedido-pdf">PDF confirmados</a>{financial&&<a href="#pedido-pagos">Cobros y comprobantes</a>}</div></details>
 </div>;
}
