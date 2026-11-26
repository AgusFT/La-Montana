'use client';
import {useEffect,useState,type FormEvent} from 'react';
import {money,date} from '@/lib/quote-types';
import {financeRead,proofData,useFinanceCommand,type Remittance} from './point-finance';

type Branch={codigoPublico:string;nombre:string};
type Point={punto:string;nombre:string;recibido:string;directa:string;confirmado:string;reservado:string;pendiente:string;servicios:string};
type Instruction={cuenta:string;punto:string;sucursal:string|null;uso:string;medio:string;habilitada:boolean;instrucciones:string;verifica_punto:boolean;verifica_admin:boolean};
type Entry=Remittance&{tipo:string;pedido:number|null;responsable:string;verificador:string|null;cotizacion:string|null;destino:string};
const amountColumns={recibido:'Fondos recibidos',directa:'Directo a sucursal',confirmado:'Rendido',reservado:'En confirmación',pendiente:'Pendiente acumulado',servicios:'Pagos por servicios'} as const;
function QueryFilters({points,branches,search}:{points:Point[];branches:Branch[];search:(query:string)=>void}){
 const [range,setRange]=useState('historial');
 function submit(e:FormEvent<HTMLFormElement>){e.preventDefault();const params=new URLSearchParams(),f=new FormData(e.currentTarget);
  for(const [k,v] of f)if(typeof v==='string'&&v)params.set(k,v);
  if(range!=='personalizado'&&range!=='historial'){
   const hoy=new Intl.DateTimeFormat('sv-SE',{timeZone:'America/Argentina/Buenos_Aires'}).format(new Date());const from=new Date(hoy+'T12:00:00Z');
   if(range==='semana')from.setUTCDate(from.getUTCDate()-(from.getUTCDay()+6)%7);
   if(range==='mes')from.setUTCDate(1);
   if(range==='año')from.setUTCMonth(0,1);
   params.set('desde',from.toISOString().slice(0,10));params.set('hasta',hoy);
  }
  search(params.toString());
 }
 return <form className="cash-fields" onSubmit={submit}><label>Período<select value={range} onChange={e=>setRange(e.target.value)}>{['historial','hoy','semana','mes','año','personalizado'].map(v=><option key={v}>{v}</option>)}</select></label>
 {range==='personalizado'&&<><label>Desde<input type="date" name="desde"/></label><label>Hasta<input type="date" name="hasta"/></label></>}
 <label>Punto<select name="punto"><option value="">Todos</option>{points.map(p=><option key={p.punto} value={p.punto}>{p.nombre}</option>)}</select></label>
 <label>Sucursal<select name="sucursal"><option value="">Todas</option>{branches.map(b=><option key={b.codigoPublico} value={b.codigoPublico}>{b.nombre}</option>)}</select></label>
 {Object.entries({medio:['EFECTIVO','TRANSFERENCIA'],destino:['PUNTO_ENTREGA','SUCURSAL_ORIGEN','SUCURSAL'],estado:['ACREDITADO','PENDIENTE','CONFIRMADA','RECHAZADA','DEVUELTO','PAGADO','DESCARTADO']}).map(([key,values])=><label key={key}>{key}<select name={key}><option value="">Todos</option>{values.map(v=><option key={v}>{v}</option>)}</select></label>)}
 <label>Responsable<input name="responsable"/></label><button>Consultar</button></form>;
}
export function RemittanceInbox({branch}:{branch:string}){
 const [rows,S]=useState<Remittance[]>([]),[revision,R]=useState(0),[error,E]=useState('');
 const cmd=useFinanceCommand(()=>R(v=>v+1));
 useEffect(()=>{let live=true;E('');financeRead<Remittance[]>(`/api/operacion/sucursales/${branch}/rendiciones-puntos`).then(v=>live&&S(v)).catch(e=>live&&E(e.message));return()=>{live=false;};},[branch,revision]);
 return <section className="workspace-panel"><h3>Rendiciones pendientes de recepción</h3><button onClick={()=>R(v=>v+1)}>Actualizar bandeja</button>{cmd.feedback}{error&&<p role="alert">{error}</p>}{!error&&!rows.length&&<p>No hay rendiciones en espera.</p>}
 {rows.map(r=><form key={r.codigo} onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);const decision=(e.nativeEvent as SubmitEvent).submitter as HTMLButtonElement;
 if(decision.value==='RECHAZADA'&&!String(f.get('motivo')).trim()){E('Ingresá el motivo del rechazo.');return;}
 cmd.send(`/api/operacion/sucursales/${branch}/rendiciones-puntos/${r.codigo}`,{estado:decision.value,motivo:f.get('motivo')});}}>
 <strong>{r.nombre} · {money(r.importe)} · {r.medio}</strong><p>{date(r.fecha)} · {r.destinatario} · {r.referencia||r.receptor} {r.observacion}</p>
 {r.adjunto&&<a href={`/api/operacion/sucursales/${branch}/rendiciones-puntos/${r.codigo}/comprobante`}>Descargar comprobante</a>}
 <label>Motivo del rechazo<input name="motivo" maxLength={300}/></label><button disabled={cmd.locked} value="CONFIRMADA">Confirmar dinero recibido</button><button disabled={cmd.locked} value="RECHAZADA">Rechazar</button></form>)}</section>;
}
function Accounts({points,branches}:{points:Point[];branches:Branch[]}){
 const [rows,S]=useState<Instruction[]>([]),[revision,R]=useState(0),[error,E]=useState(''),[ready,loaded]=useState(false);
 const [use,U]=useState('COBRO_PUNTO'),[point,P]=useState(points[0]?.punto??''),[branch,B]=useState(branches[0]?.codigoPublico??''),[method,M]=useState('TRANSFERENCIA');
 const own=use==='COBRO_PUNTO',medium=use==='RENDICION'?method:'TRANSFERENCIA';
 const active=rows.find(i=>i.uso===use&&i.punto===point&&i.sucursal===(own?null:branch)&&i.medio===medium);
 const cmd=useFinanceCommand(()=>R(v=>v+1));
 useEffect(()=>{let live=true;loaded(false);financeRead<Instruction[]>('/api/admin/finanzas-puntos/configuracion').then(v=>{if(live){S(v);loaded(true);E('');}}).catch(e=>live&&E(e.message));return()=>{live=false;};},[revision]);
 return <details><summary>Autorizar cuentas y destinatarios</summary><form className="cash-fields" onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);cmd.send('/api/admin/finanzas-puntos/configuracion',{anterior:active?.cuenta??null,punto:point,sucursal:own?null:branch,uso:use,medio:medium,instrucciones:f.get('instrucciones'),habilitada:f.has('habilitada'),verificaPunto:own&&f.has('verifica'),verificaAdmin:use!=='RENDICION'&&f.has('verificaAdmin')});}}>
 <label>Punto<select value={point} onChange={e=>P(e.target.value)}>{points.map(p=><option key={p.punto} value={p.punto}>{p.nombre}</option>)}</select></label>
 <label>Operación<select value={use} onChange={e=>U(e.target.value)}><option value="COBRO_PUNTO">Transferencia al punto</option><option value="COBRO_ORIGEN">Transferencia a sucursal de origen</option><option value="RENDICION">Rendición a la imprenta</option></select></label>
 {!own&&<label>Sucursal<select value={branch} onChange={e=>B(e.target.value)}>{branches.map(b=><option key={b.codigoPublico} value={b.codigoPublico}>{b.nombre}</option>)}</select></label>}
 {use==='RENDICION'&&<label>Medio<select value={method} onChange={e=>M(e.target.value)}><option>TRANSFERENCIA</option><option>EFECTIVO</option></select></label>}
 <fieldset key={[point,branch,use,medium,active?.cuenta].join('/')} disabled={cmd.locked||!ready}><label>Instrucciones de recepción<input name="instrucciones" defaultValue={active?.instrucciones} maxLength={500}/></label>
 <label><input name="habilitada" type="checkbox" defaultChecked={active?.habilitada}/>Habilitar</label>{own&&<label><input name="verifica" type="checkbox" defaultChecked={active?.verifica_punto}/>El responsable puede verificar esta cuenta</label>}{use!=='RENDICION'&&<label><input name="verificaAdmin" type="checkbox" defaultChecked={active?.verifica_admin}/>Administración tiene acceso para verificar esta cuenta</label>}<button>Guardar autorización</button></fieldset></form>{error}{cmd.feedback}</details>;
}
export function PointFinanceAdmin({branches}:{branches:Branch[]}){
 const [points,P]=useState<Point[]>([]),[rows,S]=useState<Entry[]>([]),[page,G]=useState(0),[query,Q]=useState(''),[revision,R]=useState(0),[error,E]=useState('');
 const cmd=useFinanceCommand(()=>R(v=>v+1));
 useEffect(()=>{let live=true;E('');
 Promise.all([financeRead<Point[]>('/api/admin/finanzas-puntos'),financeRead<Entry[]>(`/api/admin/finanzas-puntos/historial?${query}&pagina=${page}`)]).then(([p,r])=>{if(live){P(p);S(r);}}).catch(e=>live&&E(e.message));return()=>{live=false;};},[query,page,revision]);
 return <section className="workspace-panel"><h2>Finanzas de puntos de entrega</h2>{cmd.feedback}{error&&<p role="alert">{error}</p>}<button onClick={()=>R(v=>v+1)}>Actualizar finanzas</button>
 <div className="client-table-wrap"><table><caption>Saldos acumulados de todos los puntos</caption><thead><tr><th>Punto</th>{Object.values(amountColumns).map(t=><th key={t}>{t}</th>)}</tr></thead><tbody>{points.map(p=><tr key={p.punto}><td>{p.nombre}</td>{(Object.keys(amountColumns) as (keyof typeof amountColumns)[]).map(k=><td key={k}>{money(p[k])}</td>)}</tr>)}</tbody></table></div>
 {points.length>0&&<Accounts points={points} branches={branches}/>}
 <h3>Historial financiero</h3><QueryFilters points={points} branches={branches} search={q=>{Q(q);G(0);}}/>
 {rows.map(m=><details key={m.codigo}><summary>{date(m.fecha)} · {m.nombre} · {m.tipo} · {money(m.importe)} · {m.estado}</summary><p>{m.pedido&&'PED-'+m.pedido+' · '}{m.medio} · {m.destinatario??'Destinatario sin determinar'} · {m.referencia} · {m.responsable} {m.verificador&&' · Verificó '+m.verificador} {m.motivo}</p>
 {m.tipo==='COBRO'&&!m.destino&&<form onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);cmd.send('/api/admin/finanzas-puntos/revisiones',{pago:m.codigo,destino:f.get('destino'),motivo:f.get('motivo')});}}>
 <label>Destinatario comprobado<select name="destino"><option value="PUNTO_ENTREGA">Punto</option>{m.medio==='TRANSFERENCIA'&&<option value="SUCURSAL_ORIGEN">Sucursal de origen</option>}</select></label><label>Evidencia de la revisión<input name="motivo" required maxLength={300}/></label><button disabled={cmd.locked}>Registrar revisión histórica</button></form>}
 {m.cotizacion&&<a href={`/operacion/cotizaciones/${m.cotizacion}/pagos`}>Pagos y comprobantes del pedido</a>}{m.adjunto&&<a href={m.tipo==='RENDICION'?`/api/operacion/sucursales/${m.sucursal}/rendiciones-puntos/${m.codigo}/comprobante`:`/api/operacion/sucursales/${m.sucursal}/caja/egresos/${m.codigo}/comprobante`}>Descargar comprobante</a>}</details>)}
 {!rows.length&&!error&&<p>No hay movimientos para estos filtros.</p>}<button disabled={!page} onClick={()=>G(page-1)}>Anteriores</button><button disabled={rows.length<50} onClick={()=>G(page+1)}>Siguientes</button></section>;
}

export function ReportForPoint({branch}:{branch:string}){
 type Agreement={cuenta:string;punto:string;nombre:string;medio:string;instrucciones:string;disponible:string};
 const [rows,S]=useState<Agreement[]>([]),[account,A]=useState(''),[error,E]=useState(''),[revision,R]=useState(0);
 const cmd=useFinanceCommand(()=>R(v=>v+1)),base=`/api/operacion/sucursales/${branch}/rendiciones-puntos`;
 useEffect(()=>{let live=true;financeRead<Agreement[]>(base+'/cuentas').then(v=>{if(live){S(v);A(v[0]?.cuenta??'');}}).catch(e=>live&&E(e.message));return()=>{live=false;};},[base,revision]);
 const selected=rows.find(v=>v.cuenta===account);
 return <details className="workspace-panel"><summary>Informar una rendición por un punto</summary>{error&&<p role="alert">{error}</p>}{cmd.feedback}
 {!selected?<p>Sin acuerdos disponibles para esta sucursal.</p>:<form className="cash-fields" onSubmit={async e=>{
  e.preventDefault();const f=new FormData(e.currentTarget);const file=f.get('comprobante') as File;
  try{cmd.send(base+'/informes/'+selected.punto,{cuenta:account,importe:String(f.get('importe')).replace(',','.'),referencia:f.get('referencia'),receptor:f.get('receptor'),observacion:f.get('observacion'),comprobante:file?.size?await proofData(file):null});}catch(err){E((err as Error).message);}
 }}><fieldset disabled={cmd.locked}><label>Punto y medio<select value={account} onChange={e=>A(e.target.value)}>{rows.map(r=><option key={r.cuenta} value={r.cuenta}>{r.nombre} · {r.medio}</option>)}</select></label>
 <p>{selected.instrucciones} · Disponible {money(selected.disponible)}</p><label>Importe<input name="importe" required inputMode="decimal"/></label>
 {selected.medio==='EFECTIVO'?<label>Persona receptora<input name="receptor" required maxLength={140}/></label>:<label>Referencia<input name="referencia" required maxLength={160}/></label>}
 <label>Comprobante<input name="comprobante" type="file"/></label><label>Observación<input name="observacion" maxLength={300}/></label><button>Informar para confirmación</button></fieldset></form>}</details>;
}
