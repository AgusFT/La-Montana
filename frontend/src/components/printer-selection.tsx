"use client";
import type {PrinterCandidate} from "@/lib/printer-types";
import {PrinterIllustration} from "./printer-illustration";
import "@/app/printers.css";
export function PrinterSelection({printers,value,onChange,locked,onRefresh}:{printers:PrinterCandidate[];value:string;onChange:(value:string)=>void;locked:boolean;onRefresh:()=>void}){
 return <section className="printer-selection"><header><div><h4>Elegí una impresora</h4><p>Capacidades del pedido y disponibilidad de hoy.</p></div><button type="button" className="client-button secondary" disabled={locked} onClick={onRefresh}>Actualizar disponibilidad</button></header>
  {!printers.length&&<p className="client-warning">No hay impresoras declaradas en la configuración de este pedido.</p>}
  <div className="printer-card-grid" role="radiogroup" aria-label="Impresoras para este trabajo">{[...printers].sort((a,b)=>Number(b.recomendada)-Number(a.recomendada)||Number(b.compatible)-Number(a.compatible)||a.nombre.localeCompare(b.nombre)).map(p=><label key={p.codigoPublico} className={`printer-choice${value===p.codigoPublico?" selected":""}${!p.compatible?" incompatible":""}`}>
   <div className="printer-choice-top"><input type="radio" name="impresora" value={p.codigoPublico} required disabled={locked||!p.compatible} checked={value===p.codigoPublico} onChange={()=>onChange(p.codigoPublico)}/><span className={`printer-badge ${p.recomendada?"recommended":p.compatible?"compatible":"unavailable"}`}>{p.recomendada?"RECOMENDADA":p.compatible?"COMPATIBLE":"NO DISPONIBLE"}</span></div>
   <PrinterIllustration color={p.color}/><strong>{p.nombre}</strong><p>{p.formatos.map(f=>f.nombre).join(" / ")||"Formatos sin informar"}</p><p>{p.color?"Color y blanco y negro":"Blanco y negro"} · {p.dobleFaz?"Admite doble faz":"Simple faz"}</p>
   <dl><div><dt>Capacidad declarada</dt><dd>{p.capacidadHojas.toLocaleString("es-AR")} hojas</dd></div><div><dt>Papel registrado · estimado</dt><dd>{p.operacion.hojas===null?"Sin informar":`${p.operacion.hojas.toLocaleString("es-AR")} hojas`}</dd></div></dl>
   {p.compatible?<p className="printer-suitability">✓ Puede realizar este trabajo<small>{p.operacion.hojas===null?"Cumple capacidades; comprobá físicamente el papel.":p.recomendada?"Papel suficiente registrado y capacidad ajustada al trabajo.":"Cumple las capacidades y tiene papel registrado suficiente."}</small></p>:<ul className="printer-reasons">{p.motivos.map(m=><li key={m}>{m}</li>)}</ul>}
  </label>)}</div><p className="client-muted printer-selection-note">Imagen orientativa. El papel es un registro manual estimado; las recargas y asignaciones conservan su historial.</p>
  <details className="printer-exception" open={value==="MANUAL"}><summary>Usar otro recurso (excepción)</summary><label className="client-check"><input type="radio" name="impresora" required value="MANUAL" checked={value==="MANUAL"} disabled={locked} onChange={()=>onChange("MANUAL")}/>Recurso manual fuera del inventario</label><p className="client-muted">Describí el equipo y el motivo. Esta excepción queda registrada y no modifica las capacidades declaradas.</p></details>
 </section>;
}
