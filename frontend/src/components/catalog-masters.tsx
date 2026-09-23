"use client";
import {useState,type FormEvent} from "react";
import {secureMutation} from "@/lib/secure-mutation";
import {CodeField} from "@/components/code-field";
import {paperLabel} from "@/components/catalog-paper-select";
import type {CatalogState,PaperSelection} from "@/lib/catalog-types";

function MasterForm({kind,onCreated}:{kind:"papel"|"servicio";onCreated:()=>Promise<void>}) {
  const [busy,setBusy]=useState(false),[error,setError]=useState(""),[saved,setSaved]=useState(false);
  const paper=kind==="papel";
  async function submit(event:FormEvent<HTMLFormElement>){
    event.preventDefault();if(busy)return;const form=event.currentTarget,data=new FormData(form);
    const base={codigo:String(data.get("codigo")).trim(),nombre:String(data.get("nombre")).trim()};
    const payload=paper?{...base,anchoMm:Number(data.get("anchoMm")),altoMm:Number(data.get("altoMm")),gramaje:Number(data.get("gramaje")),terminacion:String(data.get("terminacion")).trim()}:{...base,tipo:String(data.get("tipo")),descripcion:String(data.get("descripcion")).trim()};
    setBusy(true);setError("");setSaved(false);
    try{await secureMutation(`/api/admin/catalogo/${paper?"papeles-personalizados":"servicios"}`,JSON.stringify(payload),"application/json");await onCreated();form.reset();setSaved(true);}
    catch(error){setError(error instanceof TypeError?"No pudimos comunicarnos con el servidor. Reintentá para confirmar el resultado.":error instanceof Error?error.message:"No pudimos completar el alta.");}finally{setBusy(false);}
  }
  return <form className="admin-form catalog-custom-form" onSubmit={submit}><fieldset disabled={busy} className="admin-fieldset"><div className="admin-form-grid">
    <CodeField subject={kind} maxLength={paper?40:50}>
      <p>Es el identificador único de este {kind}. Se usa para reconocerlo en el catálogo, al preparar tarifas{paper?" y compatibilidades":" y servicios ofrecidos"}.</p>
      <p><strong>Formato recomendado:</strong> {paper?"tipo + tamaño + gramaje":"abreviatura del servicio"}. Por ejemplo, <code>{paper?"ILUST-A4-150":"IMP-BN"}</code> o <code>{paper?"CART-300":"ANILLADO"}</code>.</p>
      <p>Hasta {paper?40:50} caracteres: letras sin tildes (A–Z), números, guion (-) o guion bajo (_), sin espacios. Se guarda en mayúsculas y queda fijo después del alta.</p>
    </CodeField>
    <label>Nombre<input name="nombre" required maxLength={paper?120:140} placeholder={paper?"Ej.: Ilustración A4 150 g/m²":"Ej.: Anillado"}/></label>
    {paper?<>
      <label>Ancho de la hoja (mm)<input name="anchoMm" type="number" required min="0.01" max="999999.99" step="0.01"/><small>La medida de un lado de la hoja, en milímetros.</small></label>
      <label>Alto de la hoja (mm)<input name="altoMm" type="number" required min="0.01" max="999999.99" step="0.01"/><small>La medida del otro lado. Ejemplo A4: 210 × 297 mm.</small></label>
      <label>Gramaje (g/m²)<input name="gramaje" type="number" required min="0.01" max="999999.99" step="0.01"/><small>Buscalo en el paquete: expresa el peso del papel por metro cuadrado.</small></label>
      <label>Terminación del papel<input name="terminacion" required maxLength={100} placeholder="Ej.: Mate, brillante o sin estucar"/><small>Indica cómo es su superficie, según el fabricante.</small></label>
    </>:<>
      <label>Tipo<select name="tipo" required defaultValue=""><option value="" disabled>Seleccionar tipo</option><option value="IMPRESION">Impresión</option><option value="TERMINACION">Terminación</option></select></label>
      <label>Descripción (opcional)<textarea name="descripcion" maxLength={1000}/></label>
    </>}
  </div></fieldset>{error&&<p className="form-message error-message" role="alert">{error}</p>}{saved&&<p role="status" className="admin-success">{paper?"Papel guardado y habilitado en el catálogo base.":"Servicio guardado. Agregalo a una revisión para definir su precio y habilitarlo."}</p>}<button className="admin-button" disabled={busy}>{busy?"Guardando…":paper?"Guardar papel personalizado":"Crear servicio"}</button></form>;
}
export function CatalogMasters({catalog,onCreated}:{catalog:CatalogState;onCreated:()=>Promise<void>}){
  const [busy,setBusy]=useState(false),[error,setError]=useState(""),[notice,setNotice]=useState(""),[weight,setWeight]=useState("80");
  const [addingAll,setAddingAll]=useState(false);
  const allEnabled=catalog.papelesPredefinidos.length>0&&catalog.papelesPredefinidos.every(p=>catalog.papelesHabilitados.some(s=>s.predefinido===p.codigo&&s.habilitado));
  async function addAll(){
    if(busy||allEnabled)return;setBusy(true);setAddingAll(true);setError("");setNotice("");
    try{
      await secureMutation("/api/admin/catalogo/papeles-predefinidos/habilitar-todos");
      await onCreated();setNotice(`Los ${catalog.papelesPredefinidos.length} papeles precargados están habilitados. Ya podés elegirlos al preparar tarifas.`);
    }catch(e){setError((e instanceof TypeError?"No pudimos comunicarnos con el servidor.":e instanceof Error?e.message:"No pudimos habilitar los papeles.")+" Podés reintentar sin duplicar registros.");}finally{setBusy(false);setAddingAll(false);}
  }
  async function change(selection:PaperSelection|null,preset?:string){
    if(busy)return;setBusy(true);setError("");setNotice("");
    try{
      const enabled=!selection?.habilitado;
      await secureMutation(`/api/admin/catalogo/${selection?"papeles-habilitados":"papeles-predefinidos"}`,JSON.stringify(selection?{formato:selection.formato,papel:selection.papel,habilitado:enabled}:{codigo:preset}),"application/json",selection?"PUT":"POST");
      await onCreated();setNotice(enabled?"Papel habilitado. Ya podés elegirlo al preparar tarifas.":"Papel deshabilitado para nuevas tarifas. Los precios vigentes o programados conservan su estado hasta que guardes otra revisión.");
    }catch(e){setError((e instanceof TypeError?"No pudimos comunicarnos con el servidor.":e instanceof Error?e.message:"No pudimos actualizar el catálogo.")+" Podés reintentar la misma acción para confirmar el resultado.");}finally{setBusy(false);}
  }
  return <section id="catalogo-base" className="admin-card catalog-master-section">
    <div className="admin-section-title"><div><h2>Primero, elegí los papeles de tu imprenta</h2><p>Habilitá las opciones que usás y que admite tu equipo. Luego creá los servicios y pasá a sus tarifas.</p></div><span className="admin-count">{catalog.papelesHabilitados.filter(p=>p.habilitado).length} habilitados</span></div>
    <div className="admin-info"><strong>El tamaño ya viene incluido</strong><p>A4 o A3 indican las medidas de la hoja. El gramaje y la terminación describen el material: un mismo tamaño puede tener distintos papeles. Aquí los elegís juntos, sin crear un formato por separado.</p></div>
    <p className="admin-note">Habilitar un papel lo deja disponible para preparar precios. Para ofrecerlo a tus clientes, guardá una revisión en «Tarifas y servicios». Deshabilitarlo aquí no modifica revisiones vigentes, programadas ni pedidos.</p>
    <div className="catalog-paper-heading"><h3>Papeles habituales precargados</h3><label>Mostrar gramaje<select value={weight} onChange={e=>setWeight(e.target.value)}><option value="80">80 g/m²</option><option value="75">75 g/m²</option><option value="90">90 g/m²</option><option value="">Todos</option></select></label></div>
    <p className="admin-note">Papel común blanco, sin estucar. Elegí el gramaje indicado en tu resma. Oficio/Folio (8½ × 13) y Legal (8½ × 14) son tamaños diferentes; verificá las medidas de tu papel.</p>
    <div className="catalog-add-all"><button type="button" className="admin-button" disabled={busy||allEnabled||catalog.papelesPredefinidos.length===0} aria-describedby="catalog-add-all-help" onClick={addAll}>{addingAll?"Habilitando todos…":allEnabled?"Todos los papeles están habilitados":`Agregar todos los papeles (${catalog.papelesPredefinidos.length})`}</button><p id="catalog-add-all-help" className="admin-note">Incluye todos los tamaños y gramajes precargados, aunque estés filtrando la lista. Los que ya agregaste se conservan sin duplicarse. Después podés deshabilitar cualquiera por separado.</p></div>
    {error&&<p className="form-message error-message" role="alert">{error}</p>}{notice&&<p className="admin-success" role="status">{notice}</p>}
    <div className="catalog-paper-grid" aria-busy={busy}>{catalog.papelesPredefinidos.filter(p=>!weight||p.gramaje===Number(weight)).map(p=>{
      const selection=catalog.papelesHabilitados.find(s=>s.predefinido===p.codigo)??null,enabled=!!selection?.habilitado;
      return <article className={`catalog-paper-card ${enabled?"selected":""}`} key={p.codigo}>
        <h4>{p.nombre}</h4><p>{p.anchoMm} × {p.altoMm} mm · {p.terminacion}</p><span>{enabled?"✓ Habilitado":"Sin habilitar"}</span>
        <button className="admin-button secondary" type="button" aria-pressed={enabled} aria-label={`${enabled?"Deshabilitar":"Habilitar"} ${p.nombre}`} disabled={busy} onClick={()=>change(selection,p.codigo)}>{enabled?"Deshabilitar":"Habilitar papel"}</button>
      </article>;
    })}</div>
    <details className="catalog-custom"><summary>Agregar papel personalizado</summary><p>No está en la lista o tiene otro material: copiá los datos de su paquete o ficha técnica. El tamaño y el papel se guardan juntos. El código y sus características quedan fijos para conservar el historial.</p><MasterForm kind="papel" onCreated={onCreated}/></details>
    <details className="catalog-custom"><summary>Mis papeles registrados ({catalog.papelesHabilitados.length})</summary><p>Incluye todos los gramajes seleccionados, personalizados y combinaciones anteriores a esta mejora.</p>
      {catalog.papelesHabilitados.length===0?<p className="admin-empty">Todavía no habilitaste papeles.</p>:<ul className="catalog-selected-papers">{catalog.papelesHabilitados.map(p=><li key={`${p.formato}/${p.papel}`}><div><strong>{paperLabel(catalog,p)}</strong><small>Código de papel: {catalog.papeles.find(x=>x.codigoPublico===p.papel)?.codigo} · {p.habilitado?"Habilitado":"Deshabilitado"}</small></div><button className="admin-button secondary" type="button" disabled={busy} onClick={()=>change(p)}>{p.habilitado?"Deshabilitar":"Habilitar"}</button></li>)}</ul>}
    </details>
    <section className="catalog-services"><h3>Después, agregá tus servicios <span className="admin-count">{catalog.servicios.length}</span></h3><p className="admin-note">Impresión es pasar el documento al papel. Terminación es un trabajo adicional, como anillado o plastificado. Sus precios se definen en el paso siguiente.</p>
      {catalog.servicios.length===0?<p className="admin-empty">Todavía no creaste servicios. Necesitarás al menos uno de impresión.</p>:<ul className="catalog-master-list">{catalog.servicios.map(s=><li key={s.codigoPublico}><strong>{s.nombre}</strong><small>{s.codigo} · {s.tipo==="IMPRESION"?"Impresión":"Terminación"}</small>{s.descripcion&&<small>{s.descripcion}</small>}</li>)}</ul>}
      <details className="catalog-custom"><summary>Agregar servicio</summary><MasterForm kind="servicio" onCreated={onCreated}/></details>
    </section>
  </section>;
}
