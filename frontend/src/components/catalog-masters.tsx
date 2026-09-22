"use client";
import { useState, type FormEvent } from "react";
import { secureMutation } from "@/lib/secure-mutation";
import type { CatalogState } from "@/lib/catalog-types";

type Kind="formatos"|"papeles"|"servicios";
const headings={formatos:"Formatos",papeles:"Papeles",servicios:"Servicios"};
function MasterForm({kind,onCreated}:{kind:Kind;onCreated:()=>Promise<void>}) {
  const [busy,setBusy]=useState(false),[error,setError]=useState(""),[saved,setSaved]=useState(false);
  async function submit(event:FormEvent<HTMLFormElement>){
    event.preventDefault();if(busy)return;const form=event.currentTarget,data=new FormData(form);
    const base={codigo:String(data.get("codigo")).trim(),nombre:String(data.get("nombre")).trim()};
    const payload=kind==="formatos"?{...base,anchoMm:Number(data.get("anchoMm")),altoMm:Number(data.get("altoMm"))}:kind==="papeles"?{...base,gramaje:Number(data.get("gramaje")),terminacion:String(data.get("terminacion")).trim()}:{...base,tipo:String(data.get("tipo")),descripcion:String(data.get("descripcion")).trim()};
    setBusy(true);setError("");setSaved(false);
    try{await secureMutation(`/api/admin/catalogo/${kind}`,JSON.stringify(payload),"application/json");form.reset();setSaved(true);await onCreated();}
    catch(error){setError(error instanceof Error?error.message:"No pudimos completar el alta.");}finally{setBusy(false);}
  }
  return <form className="admin-form" onSubmit={submit}><fieldset disabled={busy} className="admin-fieldset"><div className="admin-form-grid">
    <label>Código<input name="codigo" required maxLength={kind==="servicios"?50:40} pattern={"[A-Za-z0-9_\\-]+"}/></label><label>Nombre<input name="nombre" required maxLength={kind==="formatos"?100:kind==="papeles"?120:140}/></label>
    {kind==="formatos"&&<><label>Ancho (mm)<input name="anchoMm" type="number" required min="0.01" max="999999.99" step="0.01"/></label><label>Alto (mm)<input name="altoMm" type="number" required min="0.01" max="999999.99" step="0.01"/></label></>}
    {kind==="papeles"&&<><label>Gramaje (g/m²)<input name="gramaje" type="number" required min="0.01" max="999999.99" step="0.01"/></label><label>Terminación del papel<input name="terminacion" required maxLength={100}/></label></>}
    {kind==="servicios"&&<><label>Tipo<select name="tipo" required defaultValue=""><option value="" disabled>Seleccionar tipo</option><option value="IMPRESION">Impresión</option><option value="TERMINACION">Terminación</option></select></label><label>Descripción (opcional)<textarea name="descripcion" maxLength={1000}/></label></>}
  </div></fieldset>{error&&<p className="form-message error-message" role="alert">{error}</p>}{saved&&<p role="status" className="admin-success">Alta guardada.</p>}<button className="admin-button" disabled={busy}>{busy?"Guardando…":"Crear registro"}</button></form>;
}
export function CatalogMasters({catalog,onCreated}:{catalog:CatalogState;onCreated:()=>Promise<void>}){
  return <section id="catalogo-base" className="admin-card catalog-master-section"><div className="admin-section-title"><div><h2>Catálogo base</h2><p>Creá los formatos, materiales y tipos de servicio que ofrece tu imprenta.</p></div></div><p className="admin-note">Los códigos y las características del catálogo base quedan fijos. Los nombres comerciales de servicios, precios y habilitaciones se definen en cada revisión.</p>
    <div className="catalog-master-grid">{(["formatos","papeles","servicios"] as const).map(kind=><div className="catalog-master" key={kind}><h3>{headings[kind]} <span className="admin-count">{catalog[kind].length}</span></h3>
      {catalog[kind].length===0?<p className="admin-empty">Todavía no hay registros.</p>:<ul className="catalog-master-list">{kind==="formatos"?catalog.formatos.map(f=><li key={f.codigoPublico}><strong>{f.nombre}</strong><small>{f.codigo} · {f.anchoMm} × {f.altoMm} mm</small></li>):kind==="papeles"?catalog.papeles.map(p=><li key={p.codigoPublico}><strong>{p.nombre}</strong><small>{p.codigo} · {p.gramaje} g/m² · {p.terminacion}</small></li>):catalog.servicios.map(s=><li key={s.codigoPublico}><strong>{s.nombre}</strong><small>{s.codigo} · {s.tipo==="IMPRESION"?"Impresión":"Terminación"}</small>{s.descripcion&&<small>{s.descripcion}</small>}</li>)}</ul>}
      <details><summary>Agregar {kind==="formatos"?"formato":kind==="papeles"?"papel":"servicio"}</summary><MasterForm kind={kind} onCreated={onCreated}/></details>
    </div>)}</div>
  </section>;
}
