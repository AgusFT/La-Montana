"use client";
import {useState,type FormEvent} from "react";
import {secureMutation} from "@/lib/secure-mutation";
import {CodeField} from "@/components/code-field";
import {presetPaperGroups,registeredPaperGroups} from "@/lib/catalog-paper-groups";
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
  const [busy,setBusy]=useState(false),[error,setError]=useState(""),[notice,setNotice]=useState("");
  const [addingAll,setAddingAll]=useState(false);
  const presetGroups=presetPaperGroups(catalog.papelesPredefinidos),registeredGroups=registeredPaperGroups(catalog,presetGroups);
  const allEnabled=catalog.papelesPredefinidos.length>0&&catalog.papelesPredefinidos.every(p=>catalog.papelesHabilitados.some(s=>s.predefinido===p.codigo&&s.habilitado));
  async function addAll(){
    if(busy||allEnabled)return;setBusy(true);setAddingAll(true);setError("");setNotice("");
    try{
      await secureMutation("/api/admin/catalogo/papeles-predefinidos/habilitar-todos");
      await onCreated();setNotice(`Las ${catalog.papelesPredefinidos.length} variantes precargadas están habilitadas. Ya podés elegirlas al preparar tarifas.`);
    }catch(e){setError((e instanceof TypeError?"No pudimos comunicarnos con el servidor.":e instanceof Error?e.message:"No pudimos habilitar los papeles.")+" Podés reintentar sin duplicar registros.");}finally{setBusy(false);setAddingAll(false);}
  }
  async function change(selection:PaperSelection|null,preset?:string){
    if(busy)return;setBusy(true);setError("");setNotice("");
    try{
      const enabled=!selection?.habilitado;
      await secureMutation(`/api/admin/catalogo/${selection?"papeles-habilitados":"papeles-predefinidos"}`,JSON.stringify(selection?{formato:selection.formato,papel:selection.papel,habilitado:enabled}:{codigo:preset}),"application/json",selection?"PUT":"POST");
      await onCreated();setNotice(enabled?"Variante habilitada. Los otros gramajes conservan su estado.":"Variante deshabilitada para nuevas tarifas. Los otros gramajes conservan su estado y los precios publicados no cambian.");
    }catch(e){setError((e instanceof TypeError?"No pudimos comunicarnos con el servidor.":e instanceof Error?e.message:"No pudimos actualizar el catálogo.")+" Podés reintentar la misma acción para confirmar el resultado.");}finally{setBusy(false);}
  }
  return <section id="catalogo-base" className="admin-card catalog-master-section">
    <div className="admin-section-title"><div><h2>Primero, elegí los papeles de tu imprenta</h2><p>Habilitá las opciones que usás y que admite tu equipo. Luego creá los servicios y pasá a sus tarifas.</p></div><span className="admin-count">{catalog.papelesHabilitados.filter(p=>p.habilitado).length} variantes habilitadas</span></div>
    <div className="admin-info"><strong>El tamaño ya viene incluido</strong><p>A4 o A3 indican las medidas de la hoja. El gramaje y la terminación describen el material: un mismo tamaño puede tener distintos papeles. Aquí los elegís juntos, sin crear un formato por separado.</p></div>
    <p className="admin-note">Habilitar un papel lo deja disponible para preparar precios. Para ofrecerlo a tus clientes, guardá una revisión en «Tarifas y servicios». Deshabilitarlo aquí no modifica revisiones vigentes, programadas ni pedidos.</p>
    <div className="catalog-paper-heading"><h3>Papeles habituales precargados</h3><span>{presetGroups.length} tamaños · {catalog.papelesPredefinidos.length} variantes disponibles</span></div>
    <p className="admin-note">Papel común blanco, sin estucar. Cada tarjeta reúne un tamaño de hoja. Marcá o desmarcá sus gramajes por separado, según la resma que uses. Oficio/Folio (8½ × 13) y Legal (8½ × 14) son tamaños diferentes; verificá las medidas de tu papel.</p>
    <div className="catalog-add-all"><button type="button" className="admin-button" disabled={busy||allEnabled||catalog.papelesPredefinidos.length===0} aria-describedby="catalog-add-all-help" onClick={addAll}>{addingAll?"Habilitando variantes…":allEnabled?"Todas las variantes están habilitadas":"Agregar todos los papeles"}</button><p id="catalog-add-all-help" className="admin-note">Incluye los {presetGroups.length} tamaños con sus {catalog.papelesPredefinidos.length} variantes de gramaje. Los que ya agregaste se conservan sin duplicarse. Después podés desmarcar cualquier gramaje sin afectar a los demás.</p></div>
    {error&&<p className="form-message error-message" role="alert">{error}</p>}{notice&&<p className="admin-success" role="status">{notice}</p>}
    <div className="catalog-paper-grid" aria-busy={busy}>{presetGroups.map(group=>{
      const enabled=group.variantes.filter(p=>catalog.papelesHabilitados.some(s=>s.predefinido===p.codigo&&s.habilitado)).length;
      return <article className={`catalog-paper-card ${enabled>0?"selected":""}`} key={group.key}>
        <header><h4>{group.nombre}</h4><p>{group.anchoMm} × {group.altoMm} mm</p></header>
        <span className="catalog-variant-count" data-state={enabled===0?"none":enabled===group.variantes.length?"all":"some"}>{enabled} de {group.variantes.length} variantes habilitadas</span>
        <p>Papel común blanco · sin estucar</p>
        <fieldset className="catalog-paper-variants" disabled={busy}><legend>Gramajes disponibles</legend>{group.variantes.map(p=>{
          const selection=catalog.papelesHabilitados.find(s=>s.predefinido===p.codigo)??null,checked=!!selection?.habilitado;
          return <label className={`catalog-paper-variant ${checked?"enabled":""}`} key={p.codigo}>
            <input type="checkbox" aria-label={`${group.nombre}, ${p.gramaje} g/m²`} checked={checked} onChange={()=>change(selection,p.codigo)}/>
            <strong>{p.gramaje} g/m²</strong><span>{checked?"Habilitado":"Deshabilitado"}</span>
          </label>;
        })}</fieldset>
      </article>;
    })}</div>
    <details className="catalog-custom"><summary>Agregar papel personalizado</summary><p>No está en la lista o tiene otro material: copiá los datos de su paquete o ficha técnica. El tamaño y el papel se guardan juntos. El código y sus características quedan fijos para conservar el historial.</p><MasterForm kind="papel" onCreated={onCreated}/></details>
    <details className="catalog-custom catalog-registered"><summary>Mis papeles registrados ({registeredGroups.length} tamaños)</summary><p>Se agrupan por tamaño. Abrí un papel para ver sus variantes, incluidos los materiales personalizados. Cada casilla cambia únicamente esa variante.</p>
      {registeredGroups.length===0?<p className="admin-empty">Todavía no habilitaste papeles.</p>:<div className="catalog-registered-groups">{registeredGroups.map(group=><details className="catalog-registered-group" key={group.key}>
        <summary><strong>{group.nombre}</strong><span>{group.anchoMm} × {group.altoMm} mm · {group.variantes.filter(p=>p.habilitado).length} de {group.variantes.length} variantes registradas habilitadas</span></summary>
        <fieldset className="catalog-registered-variants" disabled={busy}><legend>Variantes de {group.nombre}</legend>{group.variantes.map(selection=>{
          const paper=catalog.papeles.find(p=>p.codigoPublico===selection.papel);
          return <label className={`catalog-registered-variant ${selection.habilitado?"enabled":""}`} key={`${selection.formato}/${selection.papel}`}>
            <input type="checkbox" checked={selection.habilitado} aria-label={`${group.nombre}, ${paper?.nombre}, ${paper?.gramaje} g/m², ${paper?.codigo}`} onChange={()=>change(selection)}/>
            <span><strong>{paper?.gramaje} g/m² · {paper?.nombre}</strong><small>{paper?.terminacion} · Código: {paper?.codigo}</small></span>
            <span className="catalog-variant-state">{selection.habilitado?"Habilitada":"Deshabilitada"}</span>
          </label>;
        })}</fieldset>
      </details>)}</div>}
    </details>
    <section className="catalog-services"><h3>Después, agregá tus servicios <span className="admin-count">{catalog.servicios.length}</span></h3><p className="admin-note">Impresión es pasar el documento al papel. Terminación es un trabajo adicional, como anillado o plastificado. Sus precios se definen en el paso siguiente.</p>
      {catalog.servicios.length===0?<p className="admin-empty">Todavía no creaste servicios. Necesitarás al menos uno de impresión.</p>:<ul className="catalog-master-list">{catalog.servicios.map(s=><li key={s.codigoPublico}><strong>{s.nombre}</strong><small>{s.codigo} · {s.tipo==="IMPRESION"?"Impresión":"Terminación"}</small>{s.descripcion&&<small>{s.descripcion}</small>}</li>)}</ul>}
      <details className="catalog-custom"><summary>Agregar servicio</summary><MasterForm kind="servicio" onCreated={onCreated}/></details>
    </section>
  </section>;
}
