"use client";
import {useRef,useState,type FormEvent} from "react";
import {MutationError,secureMutation} from "@/lib/secure-mutation";
import {catalogDate} from "@/lib/catalog-types";
import {isConfigurationDraft,isConfigurationState,modelLabels,criterionLabels,type ConfigurationState,type ConfigurationDraft,type OperatingModel,type ApprovalCriterion} from "@/lib/configuration-types";
const phases=["Configuración actual","Modelo operativo y aprobación","Pagos y reglas de seña","Recursos y asignación","Horarios y entrega","Resumen y simulación","Aplicación y seguridad","Historial de versiones"];
type Command={path:string;method:"POST"|"PUT";body:string};
async function readState(){const r=await fetch("/api/admin/configuracion",{cache:"no-store",credentials:"same-origin"});if(!r.ok)throw new Error("No pudimos consultar el borrador.");const data:unknown=await r.json();if(!isConfigurationState(data))throw new Error("La configuración tiene un formato inesperado.");return data;}

export function ConfigurationWorkspace({initial}:{initial:ConfigurationState}){
  const[draft,setDraft]=useState(initial.borrador),[phase,setPhase]=useState(1),[model,setModel]=useState<OperatingModel|"">(initial.borrador?.modelo??""),[criterion,setCriterion]=useState<ApprovalCriterion|"">(initial.borrador?.criterio??"");
  const[baseVersion,setBaseVersion]=useState(initial.borrador?.version??0),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[message,setMessage]=useState(""),[success,setSuccess]=useState(""),[conflict,setConflict]=useState(false),[latest,setLatest]=useState<ConfigurationDraft|null>(null);
  const command=useRef<Command|null>(null);const locked=busy||uncertain;
  function loadDraft(data:ConfigurationDraft){setDraft(data);setBaseVersion(data.version);setModel(data.modelo??"");setCriterion(data.criterio??"");}
  async function send(next?:Command){
    if(busy)return;setBusy(true);setMessage("");setSuccess("");if(next)command.current=next;
    try{if(!command.current)throw new Error("No hay una solicitud pendiente.");const c=command.current;const response=await secureMutation(c.path,c.body,"application/json",c.method);const data:unknown=await response.json();if(!isConfigurationDraft(data))throw new Error("No pudimos confirmar el borrador recibido.");command.current=null;setUncertain(false);setConflict(false);setLatest(null);loadDraft(data);setPhase(2);setSuccess("Solicitud procesada. El formulario muestra el borrador guardado en el sistema.");}
    catch(error){setMessage(error instanceof Error?error.message:"No pudimos guardar el borrador.");if(error instanceof MutationError&&!error.uncertain){command.current=null;setUncertain(false);setConflict(error.status===409);}else if(command.current)setUncertain(true);}
    finally{setBusy(false);}
  }
  function create(){void send({path:"/api/admin/configuracion/borradores",method:"POST",body:JSON.stringify({operacion:crypto.randomUUID()})});}
  function save(event:FormEvent<HTMLFormElement>){event.preventDefault();if(!draft||locked||conflict)return;if(!model||(model==="CONDICIONAL"&&!criterion)){setMessage("Elegí el modelo y su condición antes de guardar.");return;}void send({path:`/api/admin/configuracion/borradores/${draft.codigoPublico}/modelo`,method:"PUT",body:JSON.stringify({operacion:crypto.randomUUID(),version:baseVersion,modelo:model,criterio:model==="CONDICIONAL"?criterion:null})});}
  async function consult(){setBusy(true);setMessage("");try{const state=await readState();if(!state.borrador)throw new Error("No hay un borrador guardado. Recargá la página para consultar el estado actual.");setLatest(state.borrador);}catch(error){setMessage(error instanceof Error?error.message:"No pudimos consultar el borrador.");}finally{setBusy(false);}}
  function adopt(){if(!latest)return;if(!draft){loadDraft(latest);setPhase(2);}else if(draft.codigoPublico!==latest.codigoPublico){setMessage("Cambió el borrador en preparación. Recargá la página para continuar.");return;}else{setDraft(latest);setBaseVersion(latest.version);}setLatest(null);setConflict(false);setMessage("");setSuccess("");}
  function choose(value:OperatingModel){setModel(value);setCriterion("");setSuccess("");}
  return <>
    <p className="admin-info">Fases 1 y 2 disponibles. El resto del configurador está <strong>En construcción</strong>. Guardar un borrador todavía no activa reglas ni habilita pedidos.</p>
    <div className="configuration-layout"><aside className="configuration-steps"><h2>Flujo de configuración</h2><p>Fase {phase} de 8</p><nav aria-label="Fases del configurador">{phases.map((label,index)=><button key={label} type="button" aria-current={phase===index+1?"step":undefined} disabled={locked||index>1||(index===1&&!draft)} onClick={()=>setPhase(index+1)}><span>{String(index+1).padStart(2,"0")}</span><span>{label}{index>1&&<small>En construcción</small>}</span></button>)}</nav></aside>
      <div className="configuration-body">
        {phase===1?<>
          <h2>Configuración actual</h2><p className="admin-note">Prepará un único borrador. Todas las decisiones comienzan sin seleccionar.</p>
          <div className="configuration-summary"><section className="admin-card"><small>CONFIGURACIÓN VIGENTE</small><h3>Sin configuración activa</h3><p>La activación segura está En construcción.</p></section><section className="admin-card"><small>PRÓXIMA CONFIGURACIÓN</small><h3>No programada</h3><p>La programación operativa está En construcción.</p></section><section className="admin-card"><small>ESTADO DE EDICIÓN</small><h3>{draft?"Borrador en curso":"Sin borrador"}</h3><p>{draft?`Borrador ${draft.numero} · Edición ${draft.version}`:"Creá el borrador para elegir cómo operará la imprenta."}</p></section></div>
          {draft?<section className="admin-card configuration-resume"><h3>Continuar el borrador guardado</h3><p>Responsable: {draft.actor} · Último guardado: {catalogDate(draft.actualizadaEn)}</p><p>Modelo: {draft.modelo?modelLabels[draft.modelo]:"Sin seleccionar"}{draft.criterio&&` · ${criterionLabels[draft.criterio]}`}</p><button type="button" className="admin-button" disabled={locked} onClick={()=>setPhase(2)}>Editar borrador</button></section>:<section className="admin-card"><h3>Comenzar la configuración</h3><p className="admin-note">Se crea un borrador vacío. No se copian reglas, importes ni datos de ejemplo.</p><button type="button" className="admin-button" disabled={locked||conflict} onClick={create}>Crear borrador</button></section>}
          {draft&&<section className="admin-card"><h3>Administrar el cambio pendiente</h3><p className="admin-note">La cancelación requiere contraseña y código por correo. Su confirmación de seguridad está En construcción.</p><button className="admin-button secondary" type="button" disabled>Cancelar borrador · En construcción</button></section>}
        </>:draft&&<form className="admin-form" onSubmit={save}>
          <div><h2>Fase 2 · Modelo operativo y aprobación</h2><p className="admin-note">Borrador {draft.numero} · Editando versión {baseVersion}. Esta selección se completa con las reglas financieras de la fase 3.</p></div>
          <fieldset disabled={locked} className="admin-fieldset"><legend className="configuration-legend">Elegir modo de operación</legend><div className="configuration-models">
            <label className={`configuration-option ${model==="MANUAL"?"is-selected":""}`}><input type="radio" name="modelo" aria-label="Control manual" value="MANUAL" required checked={model==="MANUAL"} onChange={()=>choose("MANUAL")}/><strong>Control manual</strong><p>Todos los pedidos requieren revisión y decisión humana. Los medios de pago y las reglas financieras se definen después.</p></label>
            <label className={`configuration-option ${model==="CONDICIONAL"?"is-selected":""}`}><input type="radio" name="modelo" aria-label="Control condicional" value="CONDICIONAL" required checked={model==="CONDICIONAL"} onChange={()=>choose("CONDICIONAL")}/><strong>Control condicional</strong><p>Una condición admitida define el recorrido de aprobación, junto con las validaciones técnicas y los parámetros financieros.</p></label>
            <div className="configuration-option is-future"><strong>Automatización certificada</strong><span className="configuration-badge">En construcción</span><p>Evolución futura. No se puede seleccionar en esta versión.</p></div>
          </div>
          {model==="CONDICIONAL"&&<section className="admin-card configuration-conditions"><h3>Condición de aprobación</h3><p>Elegí una condición. Los importes, porcentajes y medios se configurarán en la fase 3.</p>{Object.entries(criterionLabels).map(([value,label])=><label key={value} className="admin-check"><input type="radio" name="criterio" required value={value} checked={criterion===value} onChange={()=>{setCriterion(value as ApprovalCriterion);setSuccess("");}}/>{label}</label>)}
            {criterion&&<p className="admin-info">{criterion==="PAGO_PREVIO"?"El total se acredita antes de habilitar la carga del PDF. Las validaciones técnicas siguen siendo obligatorias.":criterion==="SENA"?"La seña configurada se acredita antes de habilitar la carga del PDF. No se asigna un importe ni porcentaje predeterminado.":"El umbral será configurable. La variante con revisión humana permite revisar y corregir el PDF; después de aprobar, exige la seña configurada antes de habilitar producción."}</p>}
          </section>}
          </fieldset>
          <div className="configuration-actions"><button type="button" className="admin-button secondary" disabled={locked} onClick={()=>setPhase(1)}>Volver al estado actual</button><button className="admin-button" disabled={locked||conflict}>{busy?"Guardando…":"Guardar selección"}</button><button type="button" className="admin-button secondary" disabled>Fase 3 · En construcción</button></div>
        </form>}
        {message&&<p className="admin-warning" role="alert">{message}</p>}{success&&<p className="admin-success" role="status">{success}</p>}
        {uncertain&&<div className="admin-warning"><p>No pudimos confirmar la respuesta. Conservamos los mismos datos para evitar duplicados.</p><button type="button" className="admin-button" disabled={busy} onClick={()=>send()}>Reintentar la misma solicitud</button></div>}
        {conflict&&<div className="admin-warning"><p>El borrador cambió. Tu selección sigue en el formulario.</p><button type="button" className="admin-button secondary" disabled={busy} onClick={consult}>Consultar borrador guardado</button></div>}
        {latest&&<section className="admin-card"><h3>Borrador guardado · Edición {latest.version}</h3><p>Modelo: {latest.modelo?modelLabels[latest.modelo]:"Sin seleccionar"}{latest.criterio&&` · ${criterionLabels[latest.criterio]}`}</p><button type="button" className="admin-button secondary" disabled={locked} onClick={adopt}>{draft?"Usar versión actual conservando mi selección":"Continuar este borrador"}</button></section>}
      </div>
    </div>
    <div className="admin-page-footer"><a href="/administracion">← Volver a administración</a><span>Borrador sin activar</span></div>
  </>;
}
