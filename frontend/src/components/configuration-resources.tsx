//#region ENCABEZADO · src/components/configuration-resources.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/configuration-resources.tsx
 * ========================================================================
 * FUNCIÓN
 * Gestiona las impresoras de cada sucursal dentro del borrador: capacidades, formatos, planta
 * productiva, estados y retiros, con validación y conservación del trabajo local.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - printerForm(printer: ConfiguredPrinter): PrinterForm
 * - serviceMap(draft: ConfigurationDraft)
 * - [export] ConfigurationResources({draft,onSaved,onBack,onLockChange,onNext}:
 *   {draft:ConfigurationDraft;onSaved:(value:ConfigurationDraft,notice?:string)=>void;onBack:()=>void;onNext:()=>void;onLockChange:(locked:boolean)=>void})
 *   Componente de interfaz.
 * - [async] ConfigurationResources :: loadOptions()
 * - ConfigurationResources :: branchName(id: string)
 * - ConfigurationResources :: change<K extends keyof PrinterForm>(key: K, value: PrinterForm[K])
 * - ConfigurationResources :: openEditor(printer?: ConfiguredPrinter)
 * - [async] ConfigurationResources :: send(next?: Command)
 * - ConfigurationResources :: mutate(path: string, method: "POST"|"PUT", fields:
 *   Record<string,unknown>)
 * - ConfigurationResources :: saveResources(event: FormEvent)
 * - ConfigurationResources :: savePrinter(event: FormEvent)
 * - ConfigurationResources :: saveAction(event: FormEvent)
 * - [async] ConfigurationResources :: consult()
 * - ConfigurationResources :: adopt()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PrinterForm (tipo).
 * - Command (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - emptyPrinter [const].
 * - printerLabels [const].
 * ========================================================================
 */
//#endregion

"use client";
import {useInstallation} from "./installation-guide";
import {useNavigationGuard,useConfirmNavigation} from "@/components/navigation-boundary";
import {useEffect,useRef,useState,type FormEvent} from "react";
import {MutationError,secureMutation} from "@/lib/secure-mutation";
import {isConfigurationDraft,isConfigurationState,type ConfigurationDraft,type ConfiguredPrinter} from "@/lib/configuration-types";
import {isCatalogState,type CatalogState} from "@/lib/catalog-types";
import {isBranch,type Branch} from "@/lib/organization-types";
import {isPrinterPanel,type PrinterPanel} from "@/lib/printer-types";
import {printerInventory} from "@/lib/printer-inventory";

type PrinterForm={sucursal:string;nombre:string;formatos:string[];color:string;duplex:string;capacity:string;state:string};
type Command={path:string;method:"POST"|"PUT";body:string};
const emptyPrinter:PrinterForm={sucursal:"",nombre:"",formatos:[],color:"",duplex:"",capacity:"",state:""};
const printerLabels={OPERATIVA:"Operativa",DESHABILITADA:"Deshabilitada",RETIRADA:"Retirada"};
function printerForm(printer:ConfiguredPrinter):PrinterForm{return {sucursal:printer.sucursal??"",nombre:printer.nombre,formatos:printer.formatos,color:printer.admiteColor?"COLOR":"BN",duplex:printer.admiteDobleFaz?"SI":"NO",capacity:String(printer.capacidadHojas),state:printer.estado};}

export function ConfigurationResources({draft,onSaved,onBack,onLockChange,onNext,onPrinterEditorChange}:{draft:ConfigurationDraft;onSaved:(value:ConfigurationDraft,notice?:string)=>void;onBack:()=>void;onNext:()=>void;onLockChange:(locked:boolean)=>void;onPrinterEditorChange:(editing:boolean)=>void}){
  const guide=useInstallation(),firstSetup=guide.enabled&&!guide.data?.activa;
  const [current,setCurrent]=useState(draft),[branches,setBranches]=useState<Branch[]>([]),[catalog,setCatalog]=useState<CatalogState|null>(null),[loading,setLoading]=useState(true),[loadError,setLoadError]=useState("");
  const [activePrinters,setActivePrinters]=useState<ConfiguredPrinter[]>([]),[operations,setOperations]=useState<PrinterPanel|null>(null),[branchFilter,setBranchFilter]=useState("ALL");
  const [method,setMethod]=useState(draft.recursos.metodoAsignacion??"");
  const [editor,setEditor]=useState<"NEW"|ConfiguredPrinter|null>(null),[form,setForm]=useState<PrinterForm>(emptyPrinter),[action,setAction]=useState<{printer:ConfiguredPrinter;kind:"STATE"|"RETIRE"}|null>(null),[reason,setReason]=useState("");
  const [busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState(""),[conflict,setConflict]=useState(false),[latest,setLatest]=useState<ConfigurationDraft|null>(null),[showRetired,setShowRetired]=useState(false);
  const command=useRef<Command|null>(null),locked=busy||uncertain;const base=`/api/admin/configuracion/borradores/${draft.codigoPublico}`;
  useEffect(()=>{onLockChange(locked);return()=>onLockChange(false);},[locked,onLockChange]);
  useEffect(()=>{onPrinterEditorChange(!!editor);return()=>onPrinterEditorChange(false);},[editor,onPrinterEditorChange]);
  async function loadOptions(){setLoading(true);setLoadError("");try{
    const responses=await Promise.all([fetch("/api/admin/sucursales",{cache:"no-store"}),fetch("/api/admin/catalogo",{cache:"no-store"}),fetch("/api/admin/configuracion",{cache:"no-store"}),fetch("/api/operacion/impresoras",{cache:"no-store"})]);
    const [b,c,state,ops]:unknown[]=await Promise.all(responses.map(r=>r.json()));
    if(!responses.every(r=>r.ok)||!Array.isArray(b)||!b.every(isBranch)||!isCatalogState(c))throw new Error("No pudimos consultar las sucursales y el catálogo.");
    if(!isConfigurationState(state))throw new Error("No pudimos consultar las impresoras vigentes.");
    if(!isPrinterPanel(ops))throw new Error("No pudimos consultar el estado operativo de las impresoras.");setBranches(b);setCatalog(c);setActivePrinters(state.activa?.configuracion.recursos.impresoras??[]);setOperations(ops);
  }catch(cause){setLoadError(cause instanceof Error?cause.message:"No pudimos cargar las opciones.");}finally{setLoading(false);}}
  useEffect(()=>{void loadOptions();},[]);
  const resourcesDirty=method!==(current.recursos.metodoAsignacion??"");
  const confirmNavigation=useConfirmNavigation();
  useNavigationGuard({dirty:resourcesDirty||!!editor&&JSON.stringify(form)!==JSON.stringify(editor==="NEW"?emptyPrinter:printerForm(editor))||!!action&&!!reason,blocked:locked});
  function branchName(id:string|null){if(!id)return "Sin ubicación asignada";const branch=branches.find(b=>b.codigoPublico===id);return branch?`${branch.nombre}${branch.estado!=="ACTIVA"?" · Desactivada":""}`:id;}
  function change<K extends keyof PrinterForm>(key:K,value:PrinterForm[K]){setForm(f=>({...f,[key]:value}));}
  function openEditor(printer?:ConfiguredPrinter){setError("");setAction(null);setReason("");setEditor(printer??"NEW");setForm(printer?printerForm(printer):{...emptyPrinter});}
  async function send(next?:Command){if(busy)return;setBusy(true);setError("");if(next)command.current=next;
    try{if(!command.current)throw new Error("No hay una solicitud pendiente.");const c=command.current;const response=await secureMutation(c.path,c.body,"application/json",c.method);const data:unknown=await response.json();if(!isConfigurationDraft(data))throw new Error("No pudimos confirmar los recursos recibidos.");command.current=null;setUncertain(false);onSaved(data,c.path.endsWith("/recuperar-vigentes")?"Impresoras vigentes recuperadas sin duplicarlas. Revisá sus capacidades y sucursales productivas; el resto del borrador se conserva.":c.path.endsWith("/recursos")?"Asignación manual guardada para las plantas de cada sucursal.":c.path.endsWith("/impresoras")?"Impresora guardada en el borrador y asignada a su sucursal productiva.":"Cambio de impresora guardado en el borrador. Sus condiciones todavía no están activas.");}
    catch(cause){setError(cause instanceof Error?cause.message:"No pudimos guardar los recursos.");if(cause instanceof MutationError&&!cause.uncertain){command.current=null;setUncertain(false);setConflict(cause.status===409);}else if(command.current)setUncertain(true);}finally{setBusy(false);}}
  function mutate(path:string,method:"POST"|"PUT",fields:Record<string,unknown>){if(locked||conflict)return;void send({path:base+path,method,body:JSON.stringify({operacion:crypto.randomUUID(),version:current.version,...fields})});}
  function saveResources(event:FormEvent){event.preventDefault();mutate("/recursos","PUT",{metodoAsignacion:method});}
  function savePrinter(event:FormEvent){event.preventDefault();if(!form.formatos.length){setError("Seleccioná al menos un formato compatible.");return;}const fields={sucursal:form.sucursal||null,nombre:form.nombre.trim(),formatos:form.formatos,admiteColor:form.color==="COLOR",admiteDobleFaz:form.duplex==="SI",capacidadHojas:Number(form.capacity)};
    if(editor==="NEW")mutate("/impresoras","POST",{...fields,estado:form.state});else if(editor)mutate(`/impresoras/${editor.codigoPublico}`,"PUT",fields);}
  function saveAction(event:FormEvent){event.preventDefault();if(!action)return;mutate(`/impresoras/${action.printer.codigoPublico}/${action.kind==="RETIRE"?"retirar":"estado"}`,"POST",{motivo:reason.trim(),...(action.kind==="STATE"?{estado:action.printer.estado==="OPERATIVA"?"DESHABILITADA":"OPERATIVA"}:{})});}
  async function consult(){setBusy(true);setError("");try{const r=await fetch("/api/admin/configuracion",{cache:"no-store"});const data:unknown=await r.json();if(!r.ok||!isConfigurationState(data))throw new Error("No pudimos consultar el borrador.");if(!data.borrador)throw new Error("El borrador ya no está en preparación. Recargá la página para consultar su historial.");setLatest(data.borrador);}catch(cause){setError(cause instanceof Error?cause.message:"No pudimos consultar el borrador.");}finally{setBusy(false);}}
  function adopt(){if(!latest)return;if(latest.codigoPublico!==draft.codigoPublico){onSaved(latest);return;}
    if(editor&&editor!=="NEW"){const found=latest.recursos.impresoras.find(p=>p.codigoPublico===editor.codigoPublico);if(!found||found.estado!=="DESHABILITADA"){setError("Esta impresora ya no se puede editar. Cargá el estado guardado para continuar.");return;}setEditor(found);}
    if(action){const found=latest.recursos.impresoras.find(p=>p.codigoPublico===action.printer.codigoPublico);if(!found||found.estado!==action.printer.estado){setError("Cambió el estado de esta impresora. Cargá el estado guardado para continuar.");return;}setAction({...action,printer:found});}
    setCurrent(latest);setConflict(false);setLatest(null);setError("");}
  const printerReady=current.recursos.impresoras.some(p=>p.estado==="OPERATIVA");
  const inventory=printerInventory(current.recursos.impresoras,activePrinters,branches);
  const printerList=inventory.rows.filter(r=>(showRetired||r.printer.estado!=="RETIRADA")&&(branchFilter==="ALL"||(r.printer.sucursal??"")===branchFilter));
  return <>
    <h2>Fase 4 · Impresoras y capacidades</h2><p className="admin-note">Borrador {draft.numero} · Editando versión {current.version}. Revisá los equipos recuperados y agregá sólo los nuevos. Los cambios se aplicarán al activar la configuración.</p>
    <div className="resource-toolbar"><p>Cada sucursal produce con las impresoras que tiene asignadas. Compará los equipos vigentes con los incluidos en este borrador.</p><button type="button" className="admin-button secondary" disabled={locked||loading} onClick={loadOptions}>Actualizar ubicaciones y formatos</button></div>
    {loading&&<p role="status">Cargando opciones…</p>}{loadError&&<p className="admin-warning" role="alert">{loadError}</p>}
    <p className="admin-note">Los formatos se administran en <a href={locked?undefined:"/administracion/catalogo"} target="_blank" rel="noopener noreferrer">Servicios y precios (otra pestaña)</a>. Las <a href={locked?undefined:"/administracion/sucursales"} target="_blank" rel="noopener noreferrer">sucursales (otra pestaña)</a> definen las plantas productivas, la atención y los destinos de retiro.</p>
    {!editor?<>
      <section id="config-tour-printers" className="admin-card"><div className="resource-toolbar"><h3>Planta de impresoras por sucursal</h3><button type="button" className="admin-button" disabled={locked||loading||!!loadError||resourcesDirty||conflict||!!action||!catalog?.formatos.length} onClick={()=>openEditor()}>Agregar impresora</button></div>
        <div className="admin-info"><strong>{activePrinters.filter(p=>p.estado!=="RETIRADA").length} impresoras vigentes · {current.recursos.impresoras.filter(p=>p.estado!=="RETIRADA").length} en este borrador</strong>{inventory.missing.length>0&&<><p>Faltan incorporar {inventory.missing.length} equipos vigentes. Se muestran en la tabla; al incorporarlos se conservan las impresoras y decisiones que ya editaste.</p><button type="button" className="admin-button" disabled={locked||loading||!!loadError||resourcesDirty||conflict||!!action} onClick={()=>mutate("/impresoras/recuperar-vigentes","POST",{})}>Incorporar {inventory.missing.length} impresoras faltantes</button></>}</div>
        <div className="configuration-summary">{inventory.groups.map(g=><section className="admin-info" key={g.id}><strong>{g.name}</strong><p>{g.active} vigentes · {g.draft} en borrador{g.missing?` · ${g.missing} por incorporar`:''}</p></section>)}</div>
        <div className="admin-form"><label>Sucursal productiva<select value={branchFilter} disabled={locked} onChange={e=>setBranchFilter(e.target.value)}><option value="ALL">Todas las sucursales</option>{inventory.groups.map(g=><option key={g.id} value={g.id}>{g.name}</option>)}</select></label></div>
        {!catalog?.formatos.length&&<p className="admin-info">Para dar de alta una impresora, creá al menos un formato en el catálogo y elegí su sucursal productiva.</p>}
        {resourcesDirty&&<p className="admin-info">Guardá el método de asignación antes de modificar impresoras.</p>}
        {printerList.length===0?<p className="admin-empty">No hay impresoras declaradas en este borrador.</p>:<div className="admin-table-wrap" tabIndex={0} aria-label="Impresoras por sucursal"><table className="admin-table resource-table"><thead><tr><th>Impresora</th><th>Estado previsto</th><th>Sucursal productiva</th><th>Formatos</th><th>Color</th><th>Doble faz</th><th>Capacidad</th><th>Papel / histórico</th><th>Acciones</th></tr></thead><tbody>{printerList.map(({printer:p,pending})=><tr key={p.codigoPublico}><td><strong>{p.nombre}</strong>{pending&&<p className="admin-warning">Vigente · pendiente de incorporar</p>}<small className="resource-identity">{p.codigoPublico}</small></td><td><span className={`resource-state ${p.estado.toLowerCase()}`}>{printerLabels[p.estado]}</span></td><td>{branchName(p.sucursal)}<small>Disponible para trabajos asignados a esta sucursal</small></td><td>{p.formatos.map(id=>catalog?.formatos.find(f=>f.codigoPublico===id)?.nombre??id).join(", ")}</td><td>{p.admiteColor?"B/N y color":"B/N"}</td><td>{p.admiteDobleFaz?"Sí":"No"}</td><td>{p.capacidadHojas} hojas</td><td>{operations?.impresoras.find(e=>e.codigoPublico===p.codigoPublico)?.operacion.hojas==null?"Sin conteo registrado":`${operations.impresoras.find(e=>e.codigoPublico===p.codigoPublico)?.operacion.hojas} hojas estimadas`}<small><a href="/administracion/impresoras" target="_blank" rel="noopener noreferrer">Ver papel y movimientos</a></small></td><td><div className="resource-row-actions">{p.estado==="DESHABILITADA"&&<button type="button" className="admin-button secondary" disabled={pending||locked||resourcesDirty||conflict||!!action} onClick={()=>openEditor(p)}>Editar {p.nombre}</button>}{p.estado!=="RETIRADA"&&<button type="button" className="admin-button secondary" disabled={pending||locked||resourcesDirty||conflict||!!action} onClick={()=>{setAction({printer:p,kind:"STATE"});setReason("");setError("");}}>{p.estado==="OPERATIVA"?"Deshabilitar":"Habilitar"} {p.nombre}</button>}{p.estado==="RETIRADA"&&<small>{p.motivoRetiro}</small>}</div></td></tr>)}</tbody></table></div>}
        {current.recursos.impresoras.some(p=>p.estado==="RETIRADA")&&<label className="admin-check"><input type="checkbox" checked={showRetired} disabled={locked} onChange={e=>setShowRetired(e.target.checked)}/>Mostrar impresoras retiradas</label>}
      </section>
      {firstSetup&&!printerReady&&<p className="admin-info">Primero guardá al menos una impresora en estado Operativa. Debe tener una sucursal productiva asignada.</p>}
      <form id="config-tour-assignment" className="admin-form admin-card resource-services" onSubmit={saveResources}><fieldset className="admin-fieldset" disabled={locked||loading||!!loadError||!!action||firstSetup&&!printerReady}>
        <h3>Método de asignación</h3><label className="admin-check"><input type="radio" name="assignment" required checked={method==="MANUAL"} onChange={()=>setMethod("MANUAL")}/>Asignación manual</label><p className="admin-note">El personal elegirá la impresora compatible al organizar el trabajo.</p><label className="admin-check"><input type="radio" disabled/>Asignación automática <span className="configuration-badge">En construcción</span></label>
        <div className="admin-info"><strong>Red productiva</strong><p>Los servicios, papeles y tarifas siguen siendo comunes. Cada sucursal sólo recibe combinaciones que pueda fabricar con sus propias impresoras.</p></div>
      </fieldset><button className="admin-button" disabled={locked||loading||!!loadError||!!action||conflict||firstSetup&&!printerReady}>Guardar método de asignación</button></form>
    </>:<section className="admin-card resource-editor"><h3>{editor==="NEW"?"Agregar impresora":`Editar impresora · ${editor.nombre}`}</h3>
      {editor!=="NEW"&&<><p className="admin-info">Deshabilitada en este borrador. Podés revisar capacidades y cambiar su sucursal productiva sin alterar su identidad técnica.</p></>}
      <div className="resource-edit-grid"><form className="admin-form" onSubmit={savePrinter}><fieldset className="admin-fieldset" disabled={locked||!!action||conflict}><div id="config-tour-printer-details" className="admin-form-grid">
        <label>Nombre de la impresora<input required maxLength={120} value={form.nombre} onChange={e=>change("nombre",e.target.value)}/></label>
        <div className="catalog-field"><label htmlFor="resource-branch">Sucursal productiva</label><select id="resource-branch" required value={form.sucursal} onChange={e=>change("sucursal",e.target.value)}><option value="">Elegir sucursal</option>{branches.filter(b=>b.estado==="ACTIVA"||b.codigoPublico===form.sucursal).map(b=><option key={b.codigoPublico} value={b.codigoPublico}>{b.nombre}{b.estado!=="ACTIVA"?" · Desactivada":""}</option>)}</select><small>El equipo sólo estará disponible para los pedidos asignados a esta sucursal.</small></div>
        <div className="catalog-field"><label htmlFor="resource-color">Modo de color</label><select id="resource-color" required value={form.color} onChange={e=>change("color",e.target.value)}><option value="">Elegir capacidad</option><option value="BN">Blanco y negro</option><option value="COLOR">Blanco y negro y color</option></select></div>
        <div className="catalog-field"><label htmlFor="resource-duplex">Admite doble faz</label><select id="resource-duplex" required value={form.duplex} onChange={e=>change("duplex",e.target.value)}><option value="">Elegir capacidad</option><option value="SI">Sí</option><option value="NO">No</option></select></div>
        <label>Capacidad máxima de hojas<input type="number" min="1" max="2147483647" step="1" required value={form.capacity} onChange={e=>change("capacity",e.target.value)}/></label>
        {editor==="NEW"&&<div className="catalog-field"><label htmlFor="resource-state">Estado previsto inicial</label><select id="resource-state" required value={form.state} onChange={e=>change("state",e.target.value)}><option value="">Elegir estado</option><option value="DESHABILITADA">Deshabilitada</option><option value="OPERATIVA">Operativa</option></select></div>}
      </div><fieldset id="config-tour-printer-formats" className="resource-formats"><legend>Formatos compatibles</legend><div className="configuration-actions"><button type="button" className="admin-button secondary" disabled={!catalog?.formatos.length||catalog.formatos.every(f=>form.formatos.includes(f.codigoPublico))} onClick={()=>change("formatos",catalog?.formatos.map(f=>f.codigoPublico)??[])}>Seleccionar todos los formatos</button><button type="button" className="admin-button secondary" disabled={!form.formatos.length} onClick={()=>change("formatos",[])}>Quitar selección</button></div><p className="admin-note" role="status">{form.formatos.length} de {catalog?.formatos.length??0} formatos seleccionados. Marcá sólo los tamaños que acepta esta impresora.</p>{catalog?.formatos.map(f=><label className="admin-check" key={f.codigoPublico}><input type="checkbox" checked={form.formatos.includes(f.codigoPublico)} onChange={e=>change("formatos",e.target.checked?[...form.formatos,f.codigoPublico]:form.formatos.filter(id=>id!==f.codigoPublico))}/>{f.nombre}</label>)}</fieldset></fieldset>
        <div id="config-tour-printer-actions" className="configuration-actions"><button type="button" className="admin-button secondary" disabled={locked||!!action} onClick={()=>{if(!confirmNavigation())return;setEditor(null);setError("");}}>Volver a la lista</button><button className="admin-button" disabled={locked||!!action||conflict}>Guardar impresora</button></div>
      </form><aside className="resource-details"><h4>Información de la impresora</h4><dl><dt>Identidad técnica</dt><dd className="resource-identity">{editor==="NEW"?"Se asignará al guardar":editor.codigoPublico}</dd><dt>Sucursal productiva</dt><dd>{branchName(form.sucursal||null)}</dd><dt>Alcance productivo</dt><dd>{branchName(form.sucursal||null)}</dd><dt>Historial operativo</dt><dd><a href="/administracion/impresoras" target="_blank" rel="noopener noreferrer">Consultar movimientos</a></dd><dt>Papel registrado</dt><dd>{editor!=="NEW"&&operations?.impresoras.find(p=>p.codigoPublico===editor.codigoPublico)?.operacion.hojas!=null?`${operations.impresoras.find(p=>p.codigoPublico===editor.codigoPublico)?.operacion.hojas} hojas estimadas`:"Sin conteo registrado"}</dd></dl><p className="admin-info">La capacidad se declara manualmente. Los encargados sólo ven y operan los equipos de su sucursal. El papel y las recargas se consultan en Impresoras. Los valores son estimaciones registradas por el personal.</p></aside></div>
      {editor!=="NEW"&&<div className="resource-danger"><h4>Retirar impresora del borrador</h4><p>La impresora deshabilitada dejará de estar disponible en esta configuración. Su identidad e historial se conservan.</p><button type="button" className="admin-button secondary" disabled={locked||!!action||conflict} onClick={()=>{setAction({printer:editor,kind:"RETIRE"});setReason("");}}>Retirar impresora</button></div>}
    </section>}
    {action&&<form className="admin-form admin-card resource-action" onSubmit={saveAction}><h3>{action.kind==="RETIRE"?"Confirmar retiro":action.printer.estado==="OPERATIVA"?"Deshabilitar impresora":"Habilitar impresora"} · {action.printer.nombre}</h3><p>Este cambio afecta el estado previsto del borrador.</p><fieldset className="admin-fieldset" disabled={locked||conflict}><label>Motivo del cambio<input required maxLength={500} value={reason} onChange={e=>setReason(e.target.value)}/></label></fieldset><div className="configuration-actions"><button type="button" className="admin-button secondary" disabled={locked} onClick={()=>setAction(null)}>Volver sin cambiar estado</button><button className="admin-button" disabled={locked||conflict}>Confirmar cambio de estado</button></div></form>}
    {error&&<p className="admin-warning" role="alert">{error}</p>}
    {uncertain&&<div className="admin-warning"><p>No pudimos confirmar la respuesta. Conservamos el comando y sus datos para evitar duplicados.</p><button type="button" className="admin-button" disabled={busy} onClick={()=>send()}>Reintentar la misma operación</button></div>}
    {conflict&&<div className="admin-warning"><p>El borrador cambió. Los datos de tu formulario se conservan.</p><button type="button" className="admin-button secondary" disabled={locked} onClick={consult}>Consultar recursos guardados</button></div>}
    {latest&&<section className="admin-card"><h3>Edición guardada {latest.version}</h3><p>{latest.recursos.impresoras.filter(p=>p.estado!=="RETIRADA").length} impresoras declaradas. Revisá los datos antes de volver a guardar.</p><div className="configuration-actions"><button type="button" className="admin-button secondary" disabled={locked} onClick={adopt}>Usar edición actual conservando mis datos</button><button type="button" className="admin-button secondary" disabled={locked} onClick={()=>{if(confirmNavigation())onSaved(latest);}}>Cargar el estado guardado</button></div></section>}
    <aside className="admin-card resource-notes"><h3>Consideraciones</h3><ul><li>Deshabilitá una impresora antes de editarla o retirarla. Retirar conserva el registro.</li><li>La capacidad máxima declarada no indica cuántas hojas quedan. Consultá los trabajos e inspecciones en el pedido correspondiente.</li><li>El conteo y las recargas se gestionan desde Impresoras; no son lecturas de sensores.</li><li>Conexión CUPS y lecturas de dispositivos: <strong>En construcción</strong>.</li></ul></aside>
    <div id="config-tour-resources-actions" className="configuration-actions"><button type="button" className="admin-button secondary" disabled={locked} onClick={onBack}>Volver a pagos y reglas</button><button type="button" className="admin-button secondary" disabled={locked||inventory.missing.length>0||resourcesDirty||!current.recursos.metodoAsignacion||!!editor||!!action||conflict||firstSetup&&(guide.loading||guide.error||(guide.data?.guia.faseDisponible??0)<5||guide.data?.guia.versionBorrador!==current.version)} onClick={onNext}>Continuar a horarios y entrega</button></div>
  </>;
}

