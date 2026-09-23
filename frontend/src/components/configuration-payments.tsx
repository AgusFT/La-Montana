//#region ENCABEZADO · src/components/configuration-payments.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/configuration-payments.tsx
 * ========================================================================
 * FUNCIÓN
 * Edita las reglas financieras del borrador, valida sus entradas y permite simular pago previo,
 * seña y saldo antes de continuar el configurador.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - isSimulation(value: unknown): value is Simulation
 *   Validador de datos recibidos en tiempo de ejecución.
 * - initialValues(draft: ConfigurationDraft): FormValues
 * - [export] ConfigurationPayments({draft,onSaved,onBack,onLockChange,onNext}:
 *   {draft:ConfigurationDraft;onSaved:(draft:ConfigurationDraft)=>void;onBack:()=>void;onNext:()=>void;onLockChange:(locked:boolean)=>void})
 *   Componente de interfaz.
 * - ConfigurationPayments :: change<K extends keyof FormValues>(key: K, value: FormValues[K])
 * - ConfigurationPayments :: payload(): PaymentConfiguration
 * - [async] ConfigurationPayments :: save(event?: FormEvent<HTMLFormElement>)
 * - [async] ConfigurationPayments :: consult()
 * - ConfigurationPayments :: adopt()
 * - [async] ConfigurationPayments :: simulate(event: FormEvent<HTMLFormElement>)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - FormValues (tipo).
 * - Simulation (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - conditions [const].
 * - moments [const].
 * ========================================================================
 */
//#endregion

"use client";
import {ConfigurationMoney} from "@/components/configuration-money";
import {parsePesos,pesosDraft} from "@/lib/argentine-money";
import {ars} from "@/lib/catalog-pricing";
import {useNavigationGuard} from "@/components/navigation-boundary";

import {useEffect,useRef,useState,type FormEvent} from "react";
import {MutationError,secureMutation} from "@/lib/secure-mutation";
import {isConfigurationDraft,isConfigurationState,modelLabels,criterionLabels,type ConfigurationDraft,type DepositCondition,type PaymentConfiguration} from "@/lib/configuration-types";

type FormValues={transfer:boolean;cash:boolean;instructions:string;minutes:string;deposit:""|"SI"|"NO";condition:DepositCondition|"";threshold:string;kind:""|"PORCENTAJE"|"FIJA";value:string;approval:string};
type Simulation={version:number;total:string;revisionHumana:boolean;cargaRequiereAcreditacion:boolean;pagoPrevioRequerido:string;senaRequerida:string;saldo:string;momento:string;mediosGenerales:string[];mediosAcreditacion:string[];instrucciones:string[]};
function isSimulation(value:unknown):value is Simulation{
  if(!value||typeof value!=="object")return false;const r=value as Record<string,unknown>;
  return Number.isSafeInteger(r.version)&&["total","pagoPrevioRequerido","senaRequerida","saldo","momento"].every(key=>typeof r[key]==="string")&&typeof r.revisionHumana==="boolean"&&typeof r.cargaRequiereAcreditacion==="boolean"&&["mediosGenerales","mediosAcreditacion","instrucciones"].every(key=>Array.isArray(r[key])&&(r[key] as unknown[]).every(item=>typeof item==="string"));
}
const conditions:Record<string,string>={SIEMPRE:"Siempre",DESDE_CARILLAS:"Desde una cantidad de carillas",DESDE_MONTO:"Desde un importe del pedido",SUPERAR_UMBRAL_APROBACION:"Al superar el umbral de aprobación"};
const moments:Record<string,string>={ANTES_CARGA:"Antes de cargar el PDF",DESPUES_APROBACION_ANTES_PRODUCCION:"Después de aprobar y antes de producir",ANTES_PRODUCCION:"Antes de producir",ANTES_ENTREGA:"Antes de entregar"};
function initialValues(draft:ConfigurationDraft):FormValues{
  const p=draft.pagos;return{transfer:p?.medios.includes("TRANSFERENCIA")??false,cash:p?.medios.includes("EFECTIVO")??false,instructions:p?.instruccionesTransferencia??"",minutes:p?String(p.vigenciaCotizacionMinutos):"",deposit:p?(p.exigirSena?"SI":"NO"):"",condition:p?.condicionSena??"",threshold:p?.condicionSena==="DESDE_MONTO"?pesosDraft(p.umbralSena):p?.umbralSena??"",kind:p?.tipoSena??"",value:p?.tipoSena==="FIJA"?pesosDraft(p.valorSena):p?.valorSena??"",approval:pesosDraft(p?.umbralAprobacion)};
}

export function ConfigurationPayments({draft,onSaved,onBack,onLockChange,onNext}:{draft:ConfigurationDraft;onSaved:(draft:ConfigurationDraft)=>void;onBack:()=>void;onNext:()=>void;onLockChange:(locked:boolean)=>void}){
  const [form,setForm]=useState(()=>initialValues(draft)),[baseVersion,setBaseVersion]=useState(draft.version);
  const [busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState(""),[conflict,setConflict]=useState(false),[latest,setLatest]=useState<ConfigurationDraft|null>(null);
  const [total,setTotal]=useState(""),[carillas,setCarillas]=useState(""),[simulation,setSimulation]=useState<Simulation|null>(null),[simulating,setSimulating]=useState(false),[simulationError,setSimulationError]=useState("");
  const command=useRef<string|null>(null);const locked=busy||uncertain||simulating;
  useEffect(()=>{onLockChange(locked);return()=>onLockChange(false);},[locked,onLockChange]);
  const manual=draft.modelo==="MANUAL",prepaid=draft.criterio==="PAGO_PREVIO",byAmount=draft.criterio==="MONTO_TOTAL";
  const needsDeposit=!prepaid&&form.deposit==="SI";
  const switchingToManual=draft.criterio==="SENA"&&form.deposit==="NO";
  const condition:DepositCondition|""=manual?form.condition:byAmount?"SUPERAR_UMBRAL_APROBACION":"SIEMPRE";
  const dirty=JSON.stringify(form)!==JSON.stringify(initialValues(draft))||baseVersion!==draft.version;
  useNavigationGuard({dirty,blocked:locked});
  function change<K extends keyof FormValues>(key:K,value:FormValues[K]){setForm(current=>({...current,[key]:value}));setSimulation(null);setSimulationError("");}
  function payload():PaymentConfiguration{
    return{medios:[...(form.transfer?["TRANSFERENCIA" as const]:[]),...(form.cash?["EFECTIVO" as const]:[])],instruccionesTransferencia:form.transfer?form.instructions.trim():null,vigenciaCotizacionMinutos:Number(form.minutes),exigirSena:needsDeposit,condicionSena:needsDeposit?condition as DepositCondition:null,umbralSena:needsDeposit&&manual&&["DESDE_CARILLAS","DESDE_MONTO"].includes(condition)?condition==="DESDE_MONTO"?parsePesos(form.threshold,"Importe mínimo del pedido"):form.threshold:null,tipoSena:needsDeposit?form.kind as "PORCENTAJE"|"FIJA":null,valorSena:needsDeposit?(form.kind==="FIJA"?parsePesos(form.value,"Seña fija"):form.value):null,umbralAprobacion:byAmount?parsePesos(form.approval,"Umbral de aprobación"):null};
  }
  async function save(event?:FormEvent<HTMLFormElement>){
    event?.preventDefault();if(busy)return;setError("");setBusy(true);
    try{
      if(!command.current){if(!prepaid&&!form.deposit)throw new MutationError("Elegí si querés trabajar con seña o sin seña.",400);if(!form.transfer&&!form.cash)throw new MutationError("Habilitá al menos un medio de pago.",400);command.current=JSON.stringify({operacion:crypto.randomUUID(),version:baseVersion,pagos:payload()});}
      const response=await secureMutation(`/api/admin/configuracion/borradores/${draft.codigoPublico}/pagos`,command.current,"application/json","PUT");const data:unknown=await response.json();
      if(!isConfigurationDraft(data))throw new Error("No pudimos confirmar los datos financieros recibidos.");command.current=null;setUncertain(false);setConflict(false);onSaved(data);
    }catch(cause){setError(cause instanceof Error?cause.message:"No pudimos guardar las reglas.");if(cause instanceof MutationError&&!cause.uncertain){command.current=null;setUncertain(false);setConflict(cause.status===409);}else if(command.current)setUncertain(true);}
    finally{setBusy(false);}
  }
  async function consult(){setBusy(true);setError("");try{const response=await fetch("/api/admin/configuracion",{cache:"no-store"});const data:unknown=await response.json();if(!response.ok||!isConfigurationState(data))throw new Error("No pudimos consultar la configuración.");if(!data.borrador)throw new Error("El borrador ya no está en preparación. Recargá la página para consultar su historial.");setLatest(data.borrador);}catch(cause){setError(cause instanceof Error?cause.message:"No pudimos consultar el borrador.");}finally{setBusy(false);}}
  const sameModel=latest?.codigoPublico===draft.codigoPublico&&latest?.modelo===draft.modelo&&latest?.criterio===draft.criterio;
  function adopt(){if(!latest)return;if(sameModel){setBaseVersion(latest.version);setLatest(null);setConflict(false);setError("");setSimulation(null);}else onSaved(latest);}
  async function simulate(event:FormEvent<HTMLFormElement>){
    event.preventDefault();if(locked||dirty||!draft.pagos)return;setSimulating(true);setSimulationError("");setSimulation(null);
    try{const response=await secureMutation(`/api/admin/configuracion/borradores/${draft.codigoPublico}/pagos/simular`,JSON.stringify({version:baseVersion,total:parsePesos(total,"Total del pedido"),carillas:Number(carillas)}),"application/json");const data:unknown=await response.json();if(!isSimulation(data)||data.version!==baseVersion)throw new Error("No pudimos confirmar el cálculo recibido.");setSimulation(data);}catch(cause){setSimulationError(cause instanceof Error?cause.message:"No pudimos calcular el ejemplo.");if(cause instanceof MutationError&&cause.status===409)setConflict(true);}finally{setSimulating(false);}
  }
  return <>
    <h2>Fase 3 · Pagos y reglas de seña</h2><p className="admin-note">Borrador {draft.numero} · Editando versión {baseVersion}. Elegí medios y condiciones; los importes comienzan vacíos.</p>
    <p className="admin-info">Modelo heredado de Fase 2: <strong>{draft.modelo&&modelLabels[draft.modelo]}{draft.criterio&&` · ${criterionLabels[draft.criterio]}`}</strong></p>
    <form className="admin-form payment-configuration" onSubmit={save}>
      <fieldset className="admin-fieldset" disabled={locked}>
        <div className="payment-columns">
          <section className="admin-card payment-panel"><h3>Medios generales habilitados</h3>
            <label className="admin-check"><input type="checkbox" checked={form.transfer} onChange={event=>change("transfer",event.target.checked)}/>Transferencia</label>
            <label className="admin-check"><input type="checkbox" checked={form.cash} disabled={prepaid} onChange={event=>change("cash",event.target.checked)}/>Efectivo{draft.criterio==="SENA"&&needsDeposit&&" · sólo para el saldo"}</label>
            {prepaid&&<p className="admin-note">Efectivo no disponible: este modelo exige el total acreditado antes de cargar el archivo.</p>}
            <div className="payment-future"><label className="admin-check"><input type="checkbox" disabled/>Pago digital · Mercado Pago / MODO</label><span className="configuration-badge">En construcción</span></div>
            {!manual&&(prepaid||needsDeposit)&&<p className="admin-info">En esta demo, la condición previa se acredita mediante transferencia verificada por un usuario interno autorizado. Informar un comprobante no acredita dinero.</p>}
            {form.transfer&&<div className="catalog-field"><label htmlFor="payment-instructions">Instrucciones para transferir</label><textarea id="payment-instructions" required maxLength={2000} value={form.instructions} onChange={event=>change("instructions",event.target.value)}/><small>Ingresá los datos de cobro que deberá ver el cliente al pagar. Se guardan con esta configuración.</small></div>}
            <div className="catalog-field"><label htmlFor="quote-duration">Vigencia de la cotización (minutos)</label><input id="quote-duration" type="number" required min="1" max="2147483647" step="1" value={form.minutes} onChange={event=>change("minutes",event.target.value)}/><small>Define cuánto tiempo se mantiene vigente una nueva oferta. No establece un plazo bancario de acreditación.</small></div>
          </section>
          <section className="admin-card payment-panel"><h3>Regla de seña <span className="payment-condition-tag">{prepaid?"No aplica":"Opcional"}</span></h3>
            {!prepaid&&<div className="catalog-field"><label htmlFor="require-deposit">¿Querés trabajar con seña?</label><select id="require-deposit" required value={form.deposit} onChange={event=>change("deposit",event.target.value as FormValues["deposit"])}><option value="">Elegir una opción</option><option value="NO">Sin seña</option><option value="SI">Configurar una seña</option></select></div>}
            {prepaid?<><p className="admin-note">El pago previo exige el total. No se agrega una seña a este modelo.</p><fieldset disabled className="payment-disabled"><label>Tipo de seña<select value="" onChange={()=>{}}><option value="">No aplica</option></select></label><label>Valor de la seña<input value="" readOnly/></label></fieldset></>:<>
              {byAmount&&<ConfigurationMoney id="approval-threshold" label="Umbral de aprobación automática" value={form.approval} onChange={value=>change("approval",value)} help="Hasta este importe inclusive puede intentarse la aprobación automática, sujeta a validaciones técnicas. Por encima se exige revisión humana, uses o no seña."/>}{switchingToManual&&<p className="admin-info">Al guardar «Sin seña», el modelo «Seña previa» cambiará a «Control manual»: todos los pedidos requerirán revisión y aprobación humana. Podés elegir otro modelo en la fase 2.</p>}
              {needsDeposit&&<>
                {manual?<div className="catalog-field"><label htmlFor="deposit-condition">Condición de la seña</label><select id="deposit-condition" required value={form.condition} onChange={event=>{setForm(current=>({...current,condition:event.target.value as DepositCondition,threshold:""}));setSimulation(null);setSimulationError("");}}><option value="">Elegir condición</option>{["SIEMPRE","DESDE_CARILLAS","DESDE_MONTO"].map(item=><option key={item} value={item}>{conditions[item]}</option>)}</select></div>:<p className="payment-fixed-condition"><strong>Condición definida por el modelo</strong><br/>{byAmount?"Para pedidos que superan el umbral de aprobación":"Siempre, antes de cargar el PDF"}</p>}
                {manual&&condition==="DESDE_MONTO"&&<ConfigurationMoney id="deposit-threshold" label="Importe mínimo del pedido" value={form.threshold} onChange={value=>change("threshold",value)} help="La seña corresponde desde este importe inclusive."/>}
                {manual&&condition==="DESDE_CARILLAS"&&<div className="catalog-field"><label htmlFor="deposit-threshold">Cantidad mínima de carillas</label><input id="deposit-threshold" type="number" required min="1" step="1" value={form.threshold} onChange={event=>change("threshold",event.target.value)}/><small>Carillas impresas del pedido, incluyendo todas sus copias.</small></div>}
                <div className="catalog-field"><label htmlFor="deposit-kind">Tipo de seña</label><select id="deposit-kind" required value={form.kind} onChange={event=>{setForm(current=>({...current,kind:event.target.value as FormValues["kind"],value:""}));setSimulation(null);setSimulationError("");}}><option value="">Elegir tipo</option><option value="PORCENTAJE">Porcentaje del total</option><option value="FIJA">Importe fijo</option></select></div>
                {form.kind==="FIJA"?<ConfigurationMoney id="deposit-value" label="Importe de la seña" value={form.value} onChange={value=>change("value",value)} help="Si supera el total del pedido, se exige como máximo ese total."/>:<div className="catalog-field"><label htmlFor="deposit-value">Porcentaje de la seña (%)</label><input id="deposit-value" type="number" required min="0.0001" max="100" step="0.0001" value={form.value} onChange={event=>change("value",event.target.value)}/><small>Ingresá 25 para exigir el 25 % del total. El importe calculado se redondea a dos decimales.</small></div>}
              </>}
            </>}
            <p className="admin-info">{manual||switchingToManual?"La revisión y aprobación son humanas. Si corresponde una seña, debe acreditarse antes de producir; pagar no aprueba el pedido.":byAmount?"Por encima del umbral se recibe y revisa el PDF, se pueden pedir correcciones y luego aprobar. Tras aprobar, sólo se exige seña por transferencia si la habilitaste. Sin seña, el total se paga antes de entregar.":prepaid?"El PDF permanece del lado del cliente hasta acreditar el total. Las validaciones técnicas posteriores siguen siendo obligatorias.":"La seña se acredita por transferencia antes de habilitar la carga del PDF. El saldo se abona según los medios generales antes de entregar."}</p>
          </section>
        </div>
      </fieldset>
      {error&&<p role="alert" className="admin-warning">{error}</p>}
      {uncertain?<div className="admin-warning"><p>La respuesta no pudo confirmarse. Conservamos la misma operación y sus datos para evitar duplicados.</p><button type="button" className="admin-button" disabled={busy} onClick={()=>save()}>Reintentar el mismo guardado</button></div>:<div className="configuration-actions"><button type="button" className="admin-button secondary" disabled={locked} onClick={onBack}>Volver al modelo operativo</button><button className="admin-button" disabled={locked||conflict}>{busy?"Guardando…":"Guardar pagos y reglas"}</button><button type="button" className="admin-button secondary" disabled={locked||dirty||!draft.pagos||conflict} onClick={onNext}>Continuar a recursos</button></div>}
    </form>
    {conflict&&<div className="admin-warning"><p>El borrador cambió. Conservamos tus datos para que puedas revisar la edición actual.</p><button type="button" className="admin-button secondary" disabled={locked} onClick={consult}>Consultar borrador guardado</button></div>}
    {latest&&<section className="admin-card"><h3>Edición actual · {latest.version}</h3><p>Modelo: {latest.modelo&&modelLabels[latest.modelo]}{latest.criterio&&` · ${criterionLabels[latest.criterio]}`}</p><button type="button" className="admin-button secondary" disabled={locked} onClick={adopt}>{sameModel?"Usar edición actual conservando mis datos":"Cargar el modelo actualizado"}</button></section>}
    <section className="admin-card payment-simulation"><h3>Ejemplo operativo calculado</h3><p className="admin-note">Evalúa las reglas guardadas de este borrador. No crea un pedido ni registra pagos.</p>
      <form className="admin-form" onSubmit={simulate}><fieldset disabled={locked||dirty||!draft.pagos||conflict} className="admin-fieldset"><div className="admin-form-grid"><ConfigurationMoney id="simulation-total" label="Total del pedido para simular" value={total} onChange={value=>{setTotal(value);setSimulation(null);}} help="Importe final del pedido del ejemplo."/><label>Carillas totales, incluidas copias<input aria-label="Carillas para simular" type="number" required min="1" max="2147483647" step="1" value={carillas} onChange={event=>{setCarillas(event.target.value);setSimulation(null);}}/></label></div><button className="admin-button secondary">{simulating?"Calculando…":"Calcular ejemplo"}</button></fieldset></form>
      {(dirty||!draft.pagos)&&<p className="admin-note">Guardá los pagos y reglas antes de calcular un ejemplo.</p>}{simulationError&&<p role="alert" className="admin-warning">{simulationError}</p>}
      {simulation&&<div className="payment-result"><p><strong>Resultado de la edición {simulation.version}</strong></p><dl className="payment-totals"><div><dt>Total</dt><dd>{ars(Number(simulation.total))}</dd></div><div><dt>Pago total previo</dt><dd>{ars(Number(simulation.pagoPrevioRequerido))}</dd></div><div><dt>Seña requerida</dt><dd>{ars(Number(simulation.senaRequerida))}</dd></div><div><dt>Saldo posterior</dt><dd>{ars(Number(simulation.saldo))}</dd></div></dl><ol className="payment-flow"><li><strong>Borrador preparado</strong><span>Configuración sin activar</span></li><li><strong>{simulation.revisionHumana?"Revisión humana":"Aprobación condicional"}</strong><span>Las validaciones técnicas siguen siendo necesarias</span></li><li><strong>{moments[simulation.momento]??simulation.momento}</strong><span>{simulation.mediosAcreditacion.length?`Medios del requisito: ${simulation.mediosAcreditacion.join(", ")}`:"Sin anticipo requerido"}</span></li></ol><ul>{simulation.instrucciones.map((text,index)=><li key={index}>{text}</li>)}</ul><p>Medios generales: {simulation.mediosGenerales.join(", ")}. {simulation.cargaRequiereAcreditacion?"La carga del PDF requiere acreditación previa.":"La carga del PDF no exige un pago previo."}</p></div>}
    </section>
  </>;
}
