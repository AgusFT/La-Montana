'use client';
import {useEffect,useRef,useState,type FormEvent} from 'react';
import {quoteRead,money,date} from '@/lib/quote-types';
import {logisticsState} from '@/lib/order-types';
import {secureMutation,MutationError} from '@/lib/secure-mutation';
import {useNavigationGuard} from './navigation-boundary';
import {LogoutButton} from './identity-forms';
import {MountainMark} from './mountain-brand';
import {deskStates,type DeskContext,type DeskList,type DeskOrder,type DeskRow,type DeskFinance} from '@/lib/point-desk-types';
import '@/app/client.css';import '@/app/point-desk.css';
const object=(v:unknown):v is Record<string,unknown>=>!!v&&typeof v==='object';
const isDeskOrder=(v:unknown):v is DeskOrder=>object(v)&&object(v.pedido)&&object(v.entrega)&&Array.isArray(v.trabajos)&&Array.isArray(v.medios);
export function PointDesk(){
 const[context,setContext]=useState<DeskContext|null>(null),[list,setList]=useState<DeskList|null>(null),[tab,setTab]=useState<keyof typeof deskStates>('DISPONIBLES'),[query,setQuery]=useState(''),[selected,setSelected]=useState<string[]>([]),[finance,setFinance]=useState<DeskFinance|null>(null),[showFinance,setShowFinance]=useState(false),[order,setOrder]=useState<DeskOrder|null>(null),[mode,setMode]=useState<'entregar'|'cobro-entrega'|'cobrar'|null>(null),[code,setCode]=useState(''),[error,setError]=useState(''),[message,setMessage]=useState(''),[notice,setNotice]=useState<{title:string;message:string}|null>(null),[loading,setLoading]=useState(false),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false);
 const [medio,setMedio]=useState(''),[destino,setDestino]=useState('SUCURSAL_ORIGEN');
 const command=useRef<{path:string;body:string}|null>(null),running=useRef(false),dialog=useRef<HTMLDialogElement>(null);const locked=loading||busy||uncertain;useNavigationGuard({blocked:busy||uncertain});
 async function readList(state=tab,text=query,page=0){const response=await secureMutation('/api/punto/pedidos/buscar',JSON.stringify({estado:state,texto:text,pagina:page}),'application/json');const v=await response.json();if(!object(v)||!Array.isArray(v.pedidos)||!Number.isSafeInteger(v.total))throw Error('No pudimos comprobar la lista.');setList(v as DeskList);}
 async function load(state=tab,text=query,page=0){setLoading(true);setError('');try{const c=await quoteRead('/api/punto/contexto',(v):v is DeskContext=>object(v)&&typeof v.nombre==='string'&&typeof v.punto==='string'&&typeof v.cobros==='boolean');setContext(c);await readList(state,text,page);}catch(e){setError(e instanceof Error?e.message:'No pudimos consultar el punto.');}finally{setLoading(false);}}
 useEffect(()=>{void load();},[]);
 const dialogOpen=!!order||!!notice;
 useEffect(()=>{if(dialogOpen)dialog.current?.showModal();else dialog.current?.close();},[dialogOpen]);
 function closeDetail(){
  if(locked&&!notice)return;
  setError('');
  setCode('');
  setMode(null);
  setOrder(null);
  setNotice(null);
 }
 function backToDetail(){setError('');setCode('');setMode(null);}
 async function open(row:DeskRow){if(locked)return;setLoading(true);setError('');try{const v=await quoteRead('/api/punto/pedidos/'+row.pedido,isDeskOrder);setOrder(v);}catch(e){setError(e instanceof Error?e.message:'No pudimos consultar el pedido.');}finally{setLoading(false);}}
 async function financial(page=0,desde='',hasta=''){setLoading(true);setError('');try{setFinance(await quoteRead(`/api/punto/finanzas?pagina=${page}&desde=${desde}&hasta=${hasta}`,(v):v is DeskFinance=>object(v)&&Array.isArray(v.movimientos)&&typeof v.pendiente==='string'));setShowFinance(true);}catch(e){setError(e instanceof Error?e.message:'No pudimos consultar los movimientos.');}finally{setLoading(false);}}
 async function execute(){
  if(running.current||!command.current)return;
  running.current=true;setBusy(true);setError('');
  let saved=false;
  try{
   const response=await secureMutation(command.current.path,command.current.body,'application/json');
   const result=await response.json();
   if(mode==='cobrar'&&!isDeskOrder(result)||mode&&mode!=='cobrar'&&typeof result?.aceptado!=='boolean'){
    throw Error('No pudimos comprobar el resultado del registro.');
   }
   command.current=null;setUncertain(false);
   if(result.aceptado===false){setError(result.mensaje??'El código no coincide.');return;}
   saved=true;
   backToDetail();
   setSelected([]);
   if(mode&&order){
    const title=mode==='cobrar'?'Cobro registrado':mode==='cobro-entrega'?'Cobro y entrega confirmados':'Entrega confirmada';
    setNotice({title,message:result.mensaje??`Se registró el cobro del pedido PED-${order.pedido.numero}.`});
    setOrder(mode==='cobrar'?result:null);
   }else{setOrder(null);setMessage(result.mensaje??'Recepción registrada con tu usuario, fecha y hora.');}
   await load();
  }catch(e){
   setError(e instanceof Error?e.message:'No se pudo completar el registro.');
   if(!saved&&(!(e instanceof MutationError)||e.uncertain))setUncertain(true);
   else if(!saved){command.current=null;setUncertain(false);}
  }finally{running.current=false;setBusy(false);}
 }
 function send(path:string,body:Record<string,unknown>){if(locked)return;command.current={path,body:JSON.stringify({operacion:crypto.randomUUID(),...body})};void execute();}
 const receivable=list?.pedidos.filter(row=>row.puedeRecibir)??[];
 const selectedReceivable=receivable.filter(row=>selected.includes(row.pedido));
 function receive(){if(!selectedReceivable.length)return;send('/api/punto/recibir',{pedidos:selectedReceivable.map(r=>({pedido:r.pedido,version:r.version}))});}
 function choose(value:NonNullable<typeof mode>){
  const options=(value==='cobro-entrega'?order?.mediosSaldo:order?.medios)??[];
  setMedio(order&&options.includes(order.pedido.medio)?order.pedido.medio:options[0]??'');
  setDestino('SUCURSAL_ORIGEN');setCode('');setError('');setMode(value);
 }
 function confirm(e:FormEvent){
  e.preventDefault();if(!order||!mode||locked)return;
  const f=new FormData(e.currentTarget as HTMLFormElement),body:Record<string,unknown>={version:order.entrega.version,confirmado:true};
  if(paymentMode){
   body.medio=medio;
   body.importe=mode==='cobrar'?String(f.get('importe')).replace(',','.'):order.entrega.saldo;
   body.destino=medio==='TRANSFERENCIA'?destino:'PUNTO_ENTREGA';
   body.referencia=String(f.get('referencia')??'').trim()||null;
  }
  if(mode!=='cobrar'){body.codigo=code;body.receptor=f.get('receptor')??null;}
  send(`/api/punto/pedidos/${order.pedido.pedido}/${mode}`,body);
 }
 const title=mode==='cobrar'?'Registrar dinero recibido':mode==='cobro-entrega'?'Cobrar saldo y entregar':'Entregar con código';
 const paymentMode=mode==='cobrar'||mode==='cobro-entrega';
 const medios=(mode==='cobro-entrega'?order?.mediosSaldo:order?.medios)??[];
 return <div className="point-desk"><header className="point-top"><div className="point-brand"><MountainMark/><span>La Montaña<small>RETIRO DE PEDIDOS</small></span></div><div><a href="/cuenta/seguridad">Mi cuenta</a><LogoutButton/></div></header><main><header className="point-heading"><div><span>Tu punto de entrega</span><h1>{context?.nombre??'Consultando punto…'}</h1><p>Responsable: {context?.responsable??'…'}</p></div><button className="client-button secondary" disabled={locked} onClick={()=>void load()}>Actualizar</button></header>
 {error&&!mode&&<p className="client-warning" role="alert">{error}</p>}{message&&<p className="client-success" role="status">{message}</p>}{uncertain&&!mode&&<div className="client-warning"><p>Recuperá la misma operación para evitar duplicar el registro.</p><button className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar registro</button></div>}
 <nav className="point-tabs" aria-label="Pedidos de este punto">{(Object.keys(deskStates) as (keyof typeof deskStates)[]).map(state=><button key={state} aria-pressed={!showFinance&&tab===state} disabled={locked} onClick={()=>{setShowFinance(false);setOrder(null);setSelected([]);setTab(state);void load(state,query);}}><span>{deskStates[state]}</span><strong>{context?.[state==='POR_LLEGAR'?'porLlegar':state==='DISPONIBLES'?'disponibles':'entregados']??'—'}</strong></button>)}<button aria-pressed={showFinance} disabled={locked} onClick={()=>{setOrder(null);void financial();}}><span>Finanzas del punto</span><strong>$</strong></button></nav>
 {!showFinance?<><form className="point-search" onSubmit={e=>{e.preventDefault();setSelected([]);void load(tab,query);}}><label>Buscar por nombre, pedido o código<input value={query} onChange={e=>setQuery(e.target.value)} maxLength={160} placeholder="Nombre del cliente, PED-138 o código"/></label><button className="client-button secondary" disabled={locked}>Buscar</button>{query&&<button type="button" className="client-button secondary" disabled={locked} onClick={()=>{setQuery('');void load(tab,'');}}>Limpiar</button>}</form>
 {tab==='POR_LLEGAR'&&<div className="point-bulk"><p>Seleccioná los pedidos que el rider ya dejó y confirmá la recepción física.</p><button className="client-button order-approve" disabled={locked||!selectedReceivable.length} onClick={receive}>Recibir {selectedReceivable.length||''} {selectedReceivable.length===1?'pedido':'pedidos'}</button></div>}
 <div className="client-table-wrap"><table className="point-table"><thead><tr>{tab==='POR_LLEGAR'&&<th><input type="checkbox" aria-label="Seleccionar todos los pedidos listos para recibir de esta página" disabled={locked||!receivable.length} checked={!!receivable.length&&receivable.every(r=>selected.includes(r.pedido))} onChange={e=>setSelected(e.target.checked?receivable.map(r=>r.pedido):[])}/></th>}<th>Pedido y cliente</th><th>Situación</th><th>Último movimiento</th><th>Saldo</th><th>Acción</th></tr></thead><tbody>{list?.pedidos.map(row=><tr key={row.pedido}>{tab==='POR_LLEGAR'&&<td><input type="checkbox" aria-label={`Recibir PED-${row.numero}`} disabled={locked||!row.puedeRecibir} checked={selected.includes(row.pedido)} onChange={e=>setSelected(old=>e.target.checked?[...old,row.pedido]:old.filter(id=>id!==row.pedido))}/></td>}<td><strong>PED-{row.numero}</strong><span>{row.cliente}</span><small>{row.correo}</small></td><td><span className={'point-status '+(row.estado==='LISTO_PARA_ENTREGA'?'pending':'done')}>{row.estado==='CERRADO'?'Entregado y cerrado':row.puedeRecibir?'Listo para confirmar recepción':logisticsState(row.recorrido)}</span></td><td>{date(row.movimiento)}<small>{row.franja}</small></td><td><strong>{money(row.saldo)}</strong><small>{row.medio==='EFECTIVO'?'Efectivo':'Transferencia'}</small></td><td><button className="client-button secondary" disabled={locked} onClick={()=>void open(row)}>{tab==='DISPONIBLES'?'Entregar / cobrar':'Ver pedido'}</button></td></tr>)}</tbody></table>{!list?.pedidos.length&&<p className="point-empty">{loading?'Consultando…':'No hay pedidos para esta búsqueda.'}</p>}</div><footer className="point-pagination"><button className="client-button secondary" disabled={locked||!list||list.pagina===0} onClick={()=>void load(tab,query,(list?.pagina??0)-1)}>Anteriores</button><span>{list?.total??0} pedidos · página {(list?.pagina??0)+1}</span><button className="client-button secondary" disabled={locked||!list||(list.pagina+1)*25>=list.total} onClick={()=>void load(tab,query,(list?.pagina??0)+1)}>Siguientes</button></footer>
</>:finance&&<section className="point-finance"><header><h2>Finanzas de {context?.nombre}</h2><p>Los importes recibidos en este punto se muestran por separado de los pagos recibidos en la sucursal de origen.</p></header><form className="point-search" onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);void financial(0,String(f.get('desde')),String(f.get('hasta')));}}><label>Desde<input name="desde" type="date" defaultValue={finance.desde}/></label><label>Hasta<input name="hasta" type="date" defaultValue={finance.hasta}/></label><button className="client-button secondary" disabled={locked}>Ver período</button></form><div className="point-money"><article><span>Pendiente de cobrar al retirar</span><strong>{money(finance.pendiente)}</strong><small>Pedidos disponibles ahora en este punto.</small></article><article><span>Efectivo recibido por esta cuenta</span><strong>{money(finance.efectivo)}</strong><small>En el período elegido.</small></article><article><span>Transferencias recibidas en el punto</span><strong>{money(finance.transferencia)}</strong><small>En el período elegido.</small></article></div><h3>Movimientos de pedidos de este punto</h3><div className="client-table-wrap"><table className="point-table"><thead><tr><th>Pedido</th><th>Movimiento</th><th>Importe</th><th>Registró</th><th>Fecha y hora</th></tr></thead><tbody>{finance.movimientos.map((m,i)=><tr key={i}><td>PED-{m.pedido}</td><td>{({RECIBIR:'Dinero recibido',APLICAR:'Aplicación al pedido',DEVOLVER:'Devolución'} as Record<string,string>)[m.tipo]??m.tipo}<small>{m.medio==='EFECTIVO'?'Efectivo':'Transferencia'} · {m.referencia}</small>{m.destinoCobro&&<small>Destino del cobro · {m.destinoCobro}</small>}</td><td>{money(m.importe)}</td><td>{m.responsable}<small>{m.enPunto?'Cuenta del punto':'Administración / caja'}</small></td><td>{date(m.fecha)}</td></tr>)}</tbody></table></div>{!finance.movimientos.length&&<p className="point-empty">No hay movimientos en este período.</p>}<footer className="point-pagination"><button className="client-button secondary" disabled={locked||finance.pagina===0} onClick={()=>void financial(finance.pagina-1,finance.desde,finance.hasta)}>Anteriores</button><span>{finance.total} movimientos</span><button className="client-button secondary" disabled={locked||(finance.pagina+1)*25>=finance.total} onClick={()=>void financial(finance.pagina+1,finance.desde,finance.hasta)}>Siguientes</button></footer></section>}
 <dialog ref={dialog} closedby="any" className="delivery-dialog point-dialog" aria-labelledby="point-dialog-title" onCancel={e=>{e.preventDefault();closeDetail();}}>
  {notice?<section className="point-order">
   <h2 id="point-dialog-title">{notice.title}</h2>
   <p role="status" className="client-success">{notice.message}</p>
   <button type="button" className="client-button" autoFocus onClick={()=>order?setNotice(null):closeDetail()}>{order?'Volver al pedido':'Aceptar'}</button>
  </section>:order&&(mode?
   <form className="client-form" onSubmit={confirm}><h2 id="point-dialog-title">{title}</h2><p>PED-{order.pedido.numero} · {order.pedido.cliente}</p>{error&&<p className="client-warning" role="alert">{error}</p>}<fieldset disabled={locked}>
    {paymentMode&&<>
     {mode==='cobrar'?<label>Importe recibido<input name="importe" required inputMode="decimal" pattern="[0-9]+([.,][0-9]{1,2})?" defaultValue={order.entrega.saldo.replace('.',',')}/></label>:<p>Saldo a cobrar <strong>{money(order.entrega.saldo)}</strong></p>}
     <label>Medio de pago recibido<select name="medio" autoFocus required value={medio} onChange={e=>setMedio(e.target.value)}>{medios.map(m=><option key={m} value={m}>{m==='EFECTIVO'?'Efectivo':'Transferencia'}</option>)}</select></label>
     {medio==='TRANSFERENCIA'?<>
      <label>Cuenta que recibió la transferencia<select value={destino} onChange={e=>setDestino(e.target.value)}><option value="SUCURSAL_ORIGEN">Sucursal de origen · {order.sucursalOrigen}</option><option value="PUNTO_ENTREGA">Punto de entrega · {context?.nombre}</option></select></label>
      <label>Número de operación<input name="referencia" maxLength={160} required/></label>
      <p>{destino==='SUCURSAL_ORIGEN'?'Confirmá cuando la sucursal de origen verifique la acreditación en su cuenta.':'Confirmá después de verificar la acreditación en la cuenta del punto.'} Usá el número que figura en el comprobante.</p>
     </>:<p>Confirmá después de recibir el efectivo en el punto.</p>}
    </>}
    {mode!=='cobrar'&&<><label>Código de entrega<input className="point-code" autoFocus={!paymentMode} name="codigo" value={code} onChange={e=>setCode(e.target.value.toUpperCase().replace(/[\s-]/g,'').slice(0,8))} required pattern="[A-HJ-NP-Z2-9]{8}" minLength={8} maxLength={32} autoComplete="off" placeholder="8 letras y números"/></label><p>Puede traerlo anotado. No hace falta mostrar el celular.</p><details><summary>Nombre de quien recibe (opcional)</summary><label>Nombre<input name="receptor" maxLength={140}/></label></details></>}<p className="client-muted">Se registra tu usuario, fecha y hora. El pedido se cierra si la conciliación está completa.</p><div className="client-actions"><button className="client-button order-approve">{busy?'Registrando…':mode==='cobrar'?'Confirmar dinero recibido':mode==='cobro-entrega'?'Confirmar cobro y entrega':'Confirmar entrega'}</button><button type="button" className="client-button secondary" onClick={backToDetail}>Volver</button></div></fieldset>{uncertain&&<div className="client-warning"><p>Recuperá la misma operación antes de continuar.</p><button type="button" className="client-button" disabled={busy} onClick={()=>void execute()}>Recuperar registro</button></div>}</form>
   :<section className="point-order">
    <header>
     <div><h2 id="point-dialog-title">PED-{order.pedido.numero} · {order.pedido.cliente}</h2><p>{logisticsState(order.entrega.estadoLogistico)}</p></div>
     <button type="button" className="client-button secondary" autoFocus disabled={locked} onClick={closeDetail}>Cerrar detalle</button>
    </header>
    <ul>{order.trabajos.map((t,i)=><li key={i}>{t.nombre} · {t.paginas} páginas × {t.copias} copias</li>)}</ul>
    {order.pedido.puedeRecibir&&<div className="client-actions"><p>El rider ya presentó el pedido. Confirmá después de verificar la recepción física.</p><button type="button" className="client-button order-approve" disabled={locked} onClick={()=>send('/api/punto/recibir',{pedidos:[{pedido:order.pedido.pedido,version:order.pedido.version}]})}>Confirmar recepción</button></div>}
    {order.pedido.recorrido==='EN_VIAJE'&&!order.pedido.puedeRecibir&&<p className="client-note">El rider debe presentar el pedido antes de que puedas confirmar la recepción.</p>}
    {Number(order.entrega.saldo)>0?<p>Saldo pendiente: <strong>{money(order.entrega.saldo)}</strong></p>:<p className="client-success">Sin deuda pendiente.</p>}
    {order.entrega.estadoLogistico==='DISPONIBLE_PUNTO'&&!order.entrega.entrega&&<div className="client-actions">
     {!!order.mediosSaldo?.length?<button className="client-button order-approve" disabled={locked} onClick={()=>choose('cobro-entrega')}>Cobrar {money(order.entrega.saldo)} y entregar</button>:<button className="client-button order-approve" disabled={locked||!!order.entrega.bloqueosEntrega.length} onClick={()=>choose('entregar')}>Ingresar código y entregar</button>}
     {context?.cobros&&Number(order.entrega.saldo)>0&&!!order.medios.length&&<button className="client-button secondary" disabled={locked} onClick={()=>choose('cobrar')}>Registrar un pago sin entregar</button>}
    </div>}
    {order.entrega.cobroEfectivo?.bloqueo&&<p className="client-warning">{order.entrega.cobroEfectivo.bloqueo}</p>}
    {!order.mediosSaldo?.length&&!!order.entrega.bloqueosEntrega.length&&order.entrega.estadoPedido==='LISTO_PARA_ENTREGA'&&<p className="client-muted">{order.entrega.bloqueosEntrega.join(' ')}</p>}
    {!!order.pagos?.length&&<section><h3>Cobros registrados</h3><ul>{order.pagos.map(p=><li key={p.referencia}>{p.medio==='EFECTIVO'?'Efectivo':'Transferencia'} · {money(p.importe)} · {p.destinoCobro.nombre}{p.medio==='TRANSFERENCIA'&&<small> · Operación {p.referencia}</small>}</li>)}</ul></section>}
    {order.entrega.reciboEfectivo&&!order.pagos?.some(p=>p.referencia===order.entrega.reciboEfectivo?.referencia)&&<p className="client-success">Comprobante interno: efectivo {money(order.entrega.reciboEfectivo.importe)} · {order.entrega.reciboEfectivo.responsable} · {date(order.entrega.reciboEfectivo.fecha)}</p>}
    {order.entrega.entrega&&<p className="client-success">Entregado · {date(order.entrega.entrega.fecha)} · {order.entrega.entrega.responsable}</p>}
    {order.entrega.cierre&&<p className="client-success">Pedido cerrado · {date(order.entrega.cierre.fecha)}</p>}
    <details><summary>Historial de recepción y entrega</summary>{order.entrega.historial.map((h,i)=><p key={i}>{logisticsState(h.estadoDestino)} · {date(h.fecha)} · {h.actor}</p>)}</details>
   </section>)}
 </dialog>
 </main></div>;
}
