'use client';

import {useEffect, useRef, useState, type FormEvent} from 'react';
import {date, money, quoteRead} from '@/lib/quote-types';
import {MutationError, secureMutation} from '@/lib/secure-mutation';
import {useNavigationGuard} from './navigation-boundary';
import {isDelivery, type Delivery} from './order-delivery';
import '@/app/client.css';
import '@/app/point-desk.css';
import styles from './branch-pickup-desk.module.css';

const tabs = {PARA_RETIRAR:'Para retirar', EN_CAMINO:'En camino', ENTREGADOS:'Entregados'} as const;
type Tab = keyof typeof tabs;
type Branch = {id:string; name:string};
type Row = {pedido:string; numero:number; cliente:string; invitado:boolean; estado:string; saldo:string; medio:string};
type Page = {pedidos:Row[]; total:number; pagina:number};

function isPage(value:unknown):value is Page {
  if (!value || typeof value !== 'object') return false;
  const page = value as Page;
  return Number.isSafeInteger(page.total) && Number.isSafeInteger(page.pagina) && Array.isArray(page.pedidos)
    && page.pedidos.every(row => row && typeof row.pedido === 'string' && Number.isSafeInteger(row.numero)
      && typeof row.cliente === 'string' && typeof row.saldo === 'string' && typeof row.invitado === 'boolean'
      && typeof row.estado === 'string' && typeof row.medio === 'string');
}

export function BranchPickupDesk({branches, initialBranch}:{branches:Branch[]; initialBranch:string}) {
  const [branch, setBranch] = useState(initialBranch);
  return <div className={styles.desk}>
    <label className={styles.branch}>Sucursal de retiro
      <select value={branch} onChange={event => setBranch(event.target.value)}>
        {branches.map(option => <option key={option.id} value={option.id}>{option.name}</option>)}
      </select>
    </label>
    <PickupQueue key={branch} branch={branch}/>
  </div>;
}

function PickupQueue({branch}:{branch:string}) {
  const [tab, setTab] = useState<Tab>('PARA_RETIRAR');
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [revision, setRevision] = useState(0);
  const [list, setList] = useState<Page|null>(null);
  const [row, setRow] = useState<Row|null>(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const base = '/api/operacion/sucursales/'+branch+'/retiros';
  useEffect(() => {
    let active = true;
    const params = new URLSearchParams({bandeja:tab,texto:query,pagina:String(page)});
    setLoading(true);
    setError('');
    setList(null);
    void quoteRead(base+'?'+params,isPage).then(value => {
      if(active) setList(value);
    }).catch((failure:Error) => {
      if(active) setError(failure.message);
    }).finally(() => {
      if(active) setLoading(false);
    });
    return () => {active=false;};
  }, [base, tab, query, page, revision]);

  function searchFor(text:string) {
    setQuery(text.trim());
    setPage(0);
    setRevision(value => value+1);
  }

  return <section className="admin-card" aria-label="Pedidos para retiro">
    <div className={`point-tabs ${styles.tabs}`} role="group" aria-label="Situación del retiro">
      {(Object.keys(tabs) as Tab[]).map(value =>
        <button type="button" key={value} aria-pressed={tab===value} onClick={() => {
          if (tab!==value) {setTab(value);setPage(0);setList(null);}
        }} disabled={loading}>{tabs[value]}</button>
      )}
    </div>
    <form className="point-search" onSubmit={event => {event.preventDefault();searchFor(search);}}>
      <label>Buscar por nombre o número de pedido
        <input placeholder="Nombre del cliente o PED-138" maxLength={160} value={search} onChange={event => setSearch(event.target.value)}/>
      </label>
      <button className="admin-button" disabled={loading}>Buscar</button>
      {query&&<button type="button" className="admin-button secondary" disabled={loading} onClick={() => {setSearch('');searchFor('');}}>Limpiar</button>}
      <button type="button" className="admin-button secondary" disabled={loading} onClick={() => setRevision(value => value+1)}>Actualizar</button>
    </form>
    {message&&<p className="client-success" role="status">{message}</p>}
    {error&&<p className="client-warning" role="alert">{error}</p>}
    {loading&&<p className="point-empty" role="status">Consultando pedidos…</p>}
    {list&&(!list.pedidos.length?<p className="point-empty">{query?'No encontramos pedidos con esa búsqueda.':`No hay pedidos en «${tabs[tab]}».`}</p>:
      <div className="client-table-wrap"><table className="point-table">
        <thead><tr><th>Pedido</th><th>Cliente</th><th>Saldo</th><th>Acción</th></tr></thead>
        <tbody>{list.pedidos.map(item=><tr key={item.pedido}>
          <td><strong>PED-{item.numero}</strong></td>
          <td>{item.cliente}{item.invitado&&<small>Invitado de mostrador</small>}</td>
          <td><strong>{money(item.saldo)}</strong><small>{item.medio==='EFECTIVO'?'Efectivo':'Transferencia'}</small></td>
          <td><button type="button" className={`admin-button ${tab==='PARA_RETIRAR'?'':'secondary'}`} onClick={() => {setMessage('');setRow(item);}}>
            {tab==='PARA_RETIRAR'?'Entregar / cobrar':tab==='EN_CAMINO'?'Registrar llegada':item.estado==='CERRADO'?'Ver cierre':'Ver entrega'}
          </button></td>
        </tr>)}</tbody>
      </table></div>)}
    {list&&list.total>25&&<footer className="point-pagination">
      <button type="button" className="admin-button secondary" disabled={page===0} onClick={() => setPage(value => value-1)}>Anteriores</button>
      <span>{list.total} pedidos · página {page+1}</span>
      <button type="button" className="admin-button secondary" disabled={(page+1)*25>=list.total} onClick={() => setPage(value => value+1)}>Siguientes</button>
    </footer>}
    {row&&<PickupDialog path={base+'/'+row.pedido} row={row} onClose={() => setRow(null)} onSaved={notice => {
      setRow(null);setMessage(notice);setPage(0);setRevision(value => value+1);
    }}/>}
  </section>;
}

function PickupDialog({path,row,onClose,onSaved}:{path:string;row:Row;onClose:()=>void;onSaved:(message:string)=>void}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const command = useRef<{url:string;body:string}|null>(null);
  const running = useRef(false);
  const [detail,setDetail] = useState<Delivery|null>(null);
  const [code,setCode] = useState('');
  const [error,setError] = useState('');
  const [loading,setLoading] = useState(true);
  const [busy,setBusy] = useState(false);
  const [uncertain,setUncertain] = useState(false);
  const [refresh,setRefresh] = useState(0);
  const locked = busy || uncertain;
  useNavigationGuard({blocked:locked});
  useEffect(() => {dialog.current?.showModal();}, []);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    setDetail(null);
    void quoteRead(path,isDelivery).then(value => {if(active) setDetail(value);})
      .catch((failure:Error) => {if(active) setError(failure.message);})
      .finally(() => {if(active) setLoading(false);});
    return () => {active=false;};
  }, [path,refresh]);

  const arrival = detail?.acciones.some(action => action.accion==='LLEGAR_PUNTO' && action.habilitada);
  const ready = detail?.estadoPedido==='LISTO_PARA_ENTREGA' && ['LISTO_RETIRO','DISPONIBLE_PUNTO'].includes(detail.estadoLogistico);
  const cash = ready && detail.cobroEfectivo?.habilitado ? detail.cobroEfectivo : null;
  const canDeliver = ready && (cash || !detail.bloqueosEntrega.length);
  const action = arrival?'llegada':cash?'cobrar-efectivo':'entregar';
  const title = arrival?'Registrar llegada':cash?'Cobrar efectivo y entregar':detail?.entrega?'Entrega registrada':'Entregar pedido';

  async function execute() {
    const current = command.current;
    if (running.current || !current) return;
    running.current = true;
    setBusy(true);
    setError('');
    try {
      const response = await secureMutation(current.url,current.body,'application/json');
      const result = await response.json();
      if (current.url.endsWith('/llegada')) {
        if (!isDelivery(result)) throw Error('No pudimos comprobar la llegada.');
        onSaved(`PED-${row.numero} recibido en sucursal. Ya aparece en Para retirar.`);
      } else {
        if (typeof result?.aceptado !== 'boolean' || !isDelivery(result.vista)) throw Error('No pudimos comprobar la entrega.');
        if (!result.aceptado) {
          setDetail(result.vista);
          setError(result.mensaje ?? 'Revisá el código de entrega.');
        } else onSaved(`PED-${row.numero}. ${result.mensaje ?? 'Entrega registrada.'}`);
      }
      command.current=null;
      setUncertain(false);
    } catch(failure) {
      setError(failure instanceof Error ? failure.message : 'No pudimos completar el registro.');
      const retry = !(failure instanceof MutationError) || failure.uncertain;
      setUncertain(retry);
      if (!retry) command.current=null;
    } finally {
      running.current=false;
      setBusy(false);
    }
  }

  function confirm(event:FormEvent) {
    event.preventDefault();
    if (locked || !detail || (!arrival && !canDeliver)) return;
    command.current={
      url:path+'/'+action,
      body:JSON.stringify({
        operacion:crypto.randomUUID(), version:detail.version, confirmado:true,
        ...(arrival?{}:{codigo:row.invitado?null:code}),
        ...(cash?{importe:cash.importe,cerrar:cash.puedeCerrar}:{}),
      }),
    };
    void execute();
  }

  return <dialog ref={dialog} className="delivery-dialog point-dialog" aria-labelledby="pickup-title"
    onCancel={event => {event.preventDefault();if(!locked)onClose();}}>
    <form className={`client-form point-order ${styles.dialog}`} onSubmit={confirm}>
      <h2 id="pickup-title">{title}</h2>
      <p><strong>PED-{row.numero} · {row.cliente}</strong>{row.invitado&&' · Invitado de mostrador'}</p>
      {loading&&<p role="status">Consultando el pedido…</p>}
      {error&&<p className="client-warning" role="alert">{error}</p>}
      {detail?.entrega&&<p>{date(detail.entrega.fecha)} · {detail.entrega.responsable}{detail.cierre&&' · Pedido cerrado'}</p>}
      {arrival&&<p>Confirmá que el pedido está físicamente en esta sucursal. Quedará listo para que el cliente lo retire.</p>}
      {ready&&<>
        <p className={cash?'client-success':undefined}>Saldo pendiente <strong>{money(detail.saldo)}</strong>{cash&&' · Confirmá después de recibir el efectivo.'}</p>
        {canDeliver?row.invitado?<p>Verificá el nombre del invitado y el número de pedido.</p>:
          <label>Código de entrega
            <input className="point-code" autoFocus required autoComplete="off" minLength={8} pattern="[A-HJ-NP-Z2-9]{8}"
              disabled={locked} placeholder="ABCD2345" value={code}
              onChange={event => setCode(event.target.value.toUpperCase().replace(/[\s-]/g,'').slice(0,8))}/>
          </label>:<p className="client-note">{detail.cobroEfectivo?.bloqueo ?? detail.bloqueosEntrega.join(' ')}</p>}
      </>}
      {detail&&!detail.entrega&&!arrival&&!ready&&<p className="client-note">El pedido todavía no está disponible para retirar.</p>}
      <div className="client-actions">
        <button type="button" className="admin-button secondary" disabled={locked} onClick={onClose}>Cerrar</button>
        {error&&!locked&&<button type="button" className="admin-button secondary" onClick={() => setRefresh(value => value+1)}>Actualizar pedido</button>}
        {uncertain?<button type="button" className="admin-button" disabled={busy} onClick={() => void execute()}>Recuperar registro</button>:
          (arrival||canDeliver)&&<button type="submit" className="client-button order-approve" disabled={locked}>
            {busy?'Registrando…':arrival?'Confirmar llegada':cash?`Cobrar ${money(cash.importe)} y entregar`:'Confirmar entrega'}
          </button>}
      </div>
    </form>
  </dialog>;
}
