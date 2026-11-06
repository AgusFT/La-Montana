"use client";

import {useEffect, useRef, useState, type FormEvent} from "react";
import {money, quoteRead} from "@/lib/quote-types";
import {MutationError, secureMutation} from "@/lib/secure-mutation";
import {useNavigationGuard} from "./navigation-boundary";
import "@/app/branch-cash.css";

const labels = {
  COBRO:"Cobro de pedido", DEVOLUCION:"Devolución", PAGO_EMPLEADO:"Pago a empleado",
  PAGO_PROVEEDOR:"Pago a proveedor", RENDICION:"Rendición a administración",
} as const;
const totals = [
  ["efectivoInicial","Efectivo al inicio"], ["efectivoIngresos","Entradas en efectivo"],
  ["efectivoEgresos","Salidas en efectivo"], ["efectivoFinal","Efectivo al cierre del período"],
  ["rendiciones","Rendido a administración"], ["transferenciasIngresos","Transferencias recibidas"],
  ["transferenciasEgresos","Transferencias pagadas"],
] as const;
type Movement = {codigo:string; tipo:keyof typeof labels; medio:"EFECTIVO"|"TRANSFERENCIA"; importe:string;
  fecha:string; actor:string; concepto:string; destinatario:string|null; adjunto:boolean; cotizacion:string|null};
type Count = {codigo:string; fecha:string; actor:string; esperado:string; contado:string; diferencia:string; observacion:string};
type View = {hoy:string; desde:string; hasta:string; zonaHoraria:string; activa:boolean; resumen:Record<typeof totals[number][0]|"efectivoActual",string>;
  movimientos:Movement[]; total:number; pagina:number; arqueos:Count[]; totalArqueos:number; paginaArqueos:number};
type Mode = "egreso"|"rendicion"|"arqueo";
type Range = {desde:string; hasta:string};
const modeTitles:Record<Mode,string> = {egreso:"Registrar un pago",rendicion:"Registrar rendición",arqueo:"Hacer arqueo"};
const decimal = /^-?\d+(?:\.\d{1,2})?$/;
const amount = (value:unknown):value is string => typeof value==="string" && decimal.test(value);
const object = (value:unknown):value is Record<string,unknown> => typeof value==="object"&&value!==null;
const hasText = (value:Record<string,unknown>, keys:string[]) => keys.every(key=>typeof value[key]==="string");

function isView(value:unknown):value is View {
  return object(value) && hasText(value,["hoy","desde","hasta","zonaHoraria"]) && typeof value.activa==="boolean" &&
    object(value.resumen) && ["efectivoActual",...totals.map(([key])=>key)].every(key=>amount((value.resumen as Record<string,unknown>)[key])) &&
    [value.total,value.pagina,value.totalArqueos,value.paginaArqueos].every(Number.isSafeInteger) &&
    Array.isArray(value.movimientos) && value.movimientos.every(row=>object(row)&&
      hasText(row,["codigo","fecha","actor","concepto"])&&typeof row.tipo==="string"&&Object.hasOwn(labels,row.tipo)&&
      ["EFECTIVO","TRANSFERENCIA"].includes(String(row.medio))&&amount(row.importe)&&typeof row.adjunto==="boolean") &&
    Array.isArray(value.arqueos) && value.arqueos.every(row=>object(row)&&hasText(row,["codigo","fecha","actor","observacion"])&&
      [row.esperado,row.contado,row.diferencia].every(amount));
}

function attachment(file:File):Promise<string> {
  return new Promise((resolve,reject)=>{
    const reader=new FileReader();
    reader.onerror=()=>reject(new Error("No pudimos leer el comprobante."));
    reader.onload=()=>typeof reader.result==="string"
      ? resolve(reader.result.slice(reader.result.indexOf(",")+1)) : reject(new Error("No pudimos leer el comprobante."));
    reader.readAsDataURL(file);
  });
}

function Pages({page,total,disabled,onPage}:{page:number;total:number;disabled:boolean;onPage:(page:number)=>void}) {
  return total>25&&<div className="cash-actions">
    <button type="button" className="admin-button secondary" disabled={disabled||page===0} onClick={()=>onPage(page-1)}>Anteriores</button>
    <span>Página {page+1} de {Math.ceil(total/25)}</span>
    <button type="button" className="admin-button secondary" disabled={disabled||(page+1)*25>=total} onClick={()=>onPage(page+1)}>Siguientes</button>
  </div>;
}

export function BranchCash({branch,admin=false}:{branch:string;admin?:boolean}) {
  const base=`/api/operacion/sucursales/${branch}/caja`;
  const [view,setView]=useState<View|null>(null),[mode,setMode]=useState<Mode|null>(null);
  const [loading,setLoading]=useState(true),[saving,setSaving]=useState(false),[uncertain,setUncertain]=useState(false);
  const [error,setError]=useState(""),[formError,setFormError]=useState(""),[message,setMessage]=useState("");
  const version=useRef(0), running=useRef(false);
  const pending=useRef<{path:string; body:string}|null>(null);
  const disabled=loading||saving||uncertain;
  useNavigationGuard({blocked:saving||uncertain});

  async function load(range?:Range,page=0,pageCounts=0) {
    const request=++version.current;
    setLoading(true);setError("");
    const params=new URLSearchParams({pagina:String(page),paginaArqueos:String(pageCounts)});
    if(range){params.set("desde",range.desde);params.set("hasta",range.hasta);}
    try {
      const result=await quoteRead(`${base}?${params}`,isView);
      if(request===version.current)setView(result);
    } catch(e) {
      if(request===version.current)setError(e instanceof Error?e.message:"No pudimos consultar Caja.");
    } finally {if(request===version.current)setLoading(false);}
  }
  useEffect(()=>{void load();return()=>{version.current++;};},[base]);

  async function send() {
    if(!pending.current||running.current)return;
    running.current=true;setSaving(true);setFormError("");setMessage("");
    try {
      await secureMutation(pending.current.path,pending.current.body,"application/json");
      pending.current=null;setUncertain(false);setMode(null);
      setMessage(mode==="arqueo"?"Arqueo guardado.":mode==="rendicion"?"Rendición registrada.":"Pago registrado en Caja.");
      await load();
    } catch(e) {
      const retry=!(e instanceof MutationError)||e.uncertain;
      setUncertain(retry);
      if(!retry)pending.current=null;
      setFormError(e instanceof Error?e.message:"No pudimos confirmar el registro.");
    } finally {running.current=false;setSaving(false);}
  }

  async function submit(event:FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if(!mode||!view||disabled||running.current)return;
    const fields=new FormData(event.currentTarget);
    const field=(name:string)=>String(fields.get(name)??"").trim();
    const importe=field("importe").replace(",",".");
    const operacion=crypto.randomUUID();
    if(mode==="arqueo") {
      pending.current={path:`${base}/arqueos`,body:JSON.stringify({
        operacion,contado:importe,esperado:view.resumen.efectivoActual,observacion:field("observacion"),
      })};
    } else {
      const file=fields.get("comprobante");
      let comprobante:string|null=null;
      if(file instanceof File&&file.size) {
        if(file.size>2_097_152){setFormError("El comprobante no puede superar 2 MiB.");return;}
        running.current=true;setSaving(true);
        try {comprobante=await attachment(file);}
        catch(e){setFormError(e instanceof Error?e.message:"No pudimos leer el comprobante.");return;}
        finally {running.current=false;setSaving(false);}
      }
      pending.current={path:`${base}/egresos`,body:JSON.stringify({
        operacion,tipo:mode==="rendicion"?"RENDICION":field("tipo"),medio:mode==="rendicion"?"EFECTIVO":field("medio"),
        importe,destinatario:field("destinatario"),concepto:field("concepto"),comprobante,
      })};
    }
    await send();
  }

  function choose(next:Mode) {setMode(next);setFormError("");setMessage("");}
  const zone=view?.zonaHoraria;
  const moment=(iso:string)=>new Intl.DateTimeFormat("es-AR",{dateStyle:"short",timeStyle:"short",timeZone:zone}).format(new Date(iso));
  return <section className="cash-ledger" aria-label="Movimientos y arqueos de caja">
    {error&&<p className="admin-warning" role="alert">{error}</p>}
    {message&&<p className="client-success" role="status">{message}</p>}
    {!view?<button type="button" className="admin-button secondary" disabled={loading} onClick={()=>void load()}>{loading?"Consultando Caja…":"Volver a consultar"}</button>:<>
      <div className="cash-balance">
        <div><h3>Efectivo acumulado en caja</h3><strong>{money(view.resumen.efectivoActual)}</strong>
          <p>Saldo esperado hoy. Las transferencias no forman parte del efectivo a rendir.</p></div>
        <div className="cash-actions">
          {view.activa&&<>{(["egreso","arqueo","rendicion"] as const).map(option=><button key={option} type="button"
            className={option==="egreso"?"admin-button":"admin-button secondary"} disabled={disabled||!!mode}
            onClick={()=>choose(option)}>{modeTitles[option]}</button>)}</>}
          <button type="button" className="admin-button secondary" disabled={disabled} onClick={()=>void load(view,view.pagina,view.paginaArqueos)}>Actualizar</button>
        </div>
      </div>
      {mode&&<form className="client-form cash-editor" onSubmit={submit}>
        <h3>{modeTitles[mode]}</h3>
        <fieldset disabled={saving||uncertain||loading} className="cash-fields">
          {mode==="egreso"&&<><label>Tipo de pago<select name="tipo"><option value="PAGO_PROVEEDOR">Pago a proveedor</option><option value="PAGO_EMPLEADO">Pago a empleado</option></select></label>
            <label>Medio<select name="medio"><option value="EFECTIVO">Efectivo</option><option value="TRANSFERENCIA">Transferencia</option></select></label></>}
          <label>{mode==="arqueo"?"Efectivo contado":"Importe en pesos"}<input name="importe" required inputMode="decimal" maxLength={20} pattern="[0-9]{1,17}([.,][0-9]{1,2})?"/></label>
          {mode==="arqueo"?<label>Observación (opcional)<input name="observacion" maxLength={300}/></label>:<>
            <label>{mode==="rendicion"?"Quién recibió el efectivo":"Persona o proveedor que recibió el pago"}<input name="destinatario" required maxLength={140}/></label>
            <label>Concepto<input name="concepto" required maxLength={300} placeholder={mode==="rendicion"?"Ej. Rendición del turno tarde":"Ej. Resmas de la semana"}/></label>
            <label>Comprobante opcional (PDF, PNG o JPEG · hasta 2 MiB)<input name="comprobante" type="file" accept=".pdf,.png,.jpg,.jpeg"/></label>
          </>}
          {mode!=="arqueo"&&<label className="client-check"><input type="checkbox" required/>Confirmo que la entrega del dinero o el pago ya se realizó.</label>}
        </fieldset>
        {mode==="arqueo"&&<p>El arqueo compara el efectivo contado con el esperado. La diferencia queda registrada y no altera el saldo.</p>}
        {formError&&<p className="admin-warning" role="alert">{formError}</p>}
        <div className="cash-actions">
          {uncertain?<button type="button" className="admin-button" disabled={saving} onClick={()=>void send()}>Recuperar el mismo registro</button>:<>
            <button type="submit" className="admin-button" disabled={disabled}>{saving?"Guardando…":"Guardar registro"}</button>
            <button type="button" className="admin-button secondary" disabled={saving} onClick={()=>setMode(null)}>Cancelar</button>
          </>}
        </div>
      </form>}
      <div className="cash-period">
        <h3>{admin?"Movimientos e historial":"Movimientos del día"}</h3>
        {admin&&<form className="client-form cash-actions" key={view.desde+view.hasta} onSubmit={e=>{
          e.preventDefault();const fields=new FormData(e.currentTarget);
          void load({desde:String(fields.get("desde")),hasta:String(fields.get("hasta"))});
        }}>
          <label>Desde<input name="desde" type="date" required max={view.hoy} defaultValue={view.desde}/></label>
          <label>Hasta<input name="hasta" type="date" required max={view.hoy} defaultValue={view.hasta}/></label>
          <button className="admin-button secondary" disabled={disabled||!!mode}>Ver período</button>
          <button type="button" className="admin-button secondary" disabled={disabled||!!mode} onClick={()=>void load({desde:view.hoy.slice(0,7)+"-01",hasta:view.hoy})}>Este mes</button>
        </form>}
        <dl className="cash-totals">{totals.map(([key,label])=><div key={key}><dt>{label}</dt><dd>{money(view.resumen[key])}</dd></div>)}</dl>
        <div className="client-table-wrap"><table className="client-table cash-table">
          <thead><tr><th>Fecha y hora</th><th>Movimiento</th><th>Importe</th><th>Concepto</th><th>Registró</th><th>Detalle</th></tr></thead>
          <tbody>{view.movimientos.map(row=><tr key={row.tipo+row.codigo}>
            <td>{moment(row.fecha)}</td><td>{labels[row.tipo]}<small>{row.medio==="EFECTIVO"?"Efectivo":"Transferencia"}</small></td>
            <td className={row.importe.startsWith("-")?"cash-negative":"cash-positive"}>{money(row.importe)}</td>
            <td>{row.concepto}{row.destinatario&&<small>{row.destinatario}</small>}</td><td>{row.actor}</td>
            <td>{row.adjunto?<a href={`${base}/egresos/${row.codigo}/comprobante`} download>Descargar comprobante</a>:
              admin&&row.cotizacion?<a href={`/operacion/cotizaciones/${row.cotizacion}/pagos`}>Ver cobros</a>:"—"}</td>
          </tr>)}</tbody>
        </table>{!view.movimientos.length&&<p className="admin-empty">No hay movimientos en este período.</p>}</div>
        <Pages page={view.pagina} total={view.total} disabled={disabled} onPage={page=>void load(view,page,view.paginaArqueos)}/>
      </div>
      <div className="cash-period">
        <h3>Arqueos registrados</h3>
        <div className="client-table-wrap"><table className="client-table cash-table">
          <thead><tr><th>Fecha y hora</th><th>Efectivo esperado</th><th>Efectivo contado</th><th>Diferencia</th><th>Registró</th><th>Observación</th></tr></thead>
          <tbody>{view.arqueos.map(row=><tr key={row.codigo}><td>{moment(row.fecha)}</td><td>{money(row.esperado)}</td>
            <td>{money(row.contado)}</td><td>{money(row.diferencia)}</td><td>{row.actor}</td><td>{row.observacion||"—"}</td></tr>)}</tbody>
        </table>{!view.arqueos.length&&<p className="admin-empty">No hay arqueos guardados en este período.</p>}</div>
        <Pages page={view.paginaArqueos} total={view.totalArqueos} disabled={disabled} onPage={page=>void load(view,view.pagina,page)}/>
      </div>
    </>}
  </section>;
}
