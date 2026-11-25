//#region ENCABEZADO · src/components/catalog-schedule.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-schedule.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta una configuración comercial programada y permite cancelarla con motivo, control de
 * envío y tratamiento de errores.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] CatalogSchedule({revision,onChanged}:
 *   {revision:CatalogRevision;onChanged:()=>Promise<void>})
 *   Componente de interfaz.
 * - [async] CatalogSchedule :: cancel(event?: FormEvent<HTMLFormElement>)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";
import {catalogConfigurationText} from "@/lib/catalog-wording";
import { useRef, useState, type FormEvent } from "react";
import { MutationError, secureMutation } from "@/lib/secure-mutation";
import { catalogDate, isCatalogRevision, type CatalogRevision } from "@/lib/catalog-types";

export function CatalogSchedule({revision,onChanged}:{revision:CatalogRevision;onChanged:()=>Promise<void>}) {
  const [reason,setReason]=useState(""),[busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState("");
  const command=useRef<{operacion:string;motivo:string}|null>(null);
  async function cancel(event?:FormEvent<HTMLFormElement>) {
    event?.preventDefault();if(busy)return;setBusy(true);setError("");
    try {
      if(!command.current)command.current={operacion:crypto.randomUUID(),motivo:reason.trim()};
      const response=await secureMutation(`/api/admin/catalogo/programaciones/${revision.codigoPublico}/cancelar`,JSON.stringify(command.current),"application/json");
      const data:unknown=await response.json();if(!isCatalogRevision(data)||data.estado!=="CANCELADA")throw new Error("No pudimos confirmar la cancelación recibida.");
      command.current=null;setUncertain(false);await onChanged();
    } catch(error) {
      setError(error instanceof Error?catalogConfigurationText(error.message):"No pudimos cancelar la programación.");
      if(error instanceof MutationError&&!error.uncertain){command.current=null;setUncertain(false);}
      else if(command.current)setUncertain(true);
    } finally {setBusy(false);}
  }
  return <section className="admin-card catalog-pending" aria-labelledby="pending-title">
    <div className="admin-section-title"><div><h2 id="pending-title">Configuración {revision.numero} programada</h2><p>Entrará en vigencia el {revision.programadaPara?catalogDate(revision.programadaPara):"instante indicado"}.</p></div><button type="button" className="admin-button secondary" disabled={busy} onClick={()=>onChanged().catch(()=>setError("No pudimos actualizar el estado."))}>Actualizar estado</button></div>
    <p className="admin-note">La configuración actual se mantiene hasta entonces. Para guardar otro cambio comercial, primero cancelá esta programación. El historial conserva la configuración cancelada.</p>
    {error&&<p className="form-message error-message" role="alert">{error}</p>}
    <details><summary>Cancelar programación</summary><form className="admin-form" onSubmit={cancel}>
      <div className="catalog-field"><label htmlFor="catalog-cancel-reason">Motivo de cancelación</label><textarea id="catalog-cancel-reason" required maxLength={500} value={reason} disabled={busy||uncertain} onChange={event=>setReason(event.target.value)}/></div>
      {uncertain?<div className="admin-warning"><p>No pudimos confirmar la respuesta. El reintento conserva la misma solicitud.</p><button type="button" className="admin-button" disabled={busy} onClick={()=>cancel()}>Reintentar cancelación</button></div>:<button className="admin-button secondary" disabled={busy}>{busy?"Cancelando…":"Confirmar cancelación"}</button>}
    </form></details>
  </section>;
}
