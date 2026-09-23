"use client";

import { useId, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { secureMutation } from "@/lib/secure-mutation";
import { weekDays, type Branch, type BranchDay, type BranchLocation } from "@/lib/organization-types";

const fields = [["codigo", "Código", 40], ["nombre", "Nombre", 140], ["calle", "Calle", 160], ["numero", "Número", 20],
  ["codigoPostal", "Código postal", 12], ["correo", "Correo (opcional)", 254], ["telefono", "Teléfono (opcional)", 40]] as const;
const normalize = (s: string) => s.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().replace(/[^a-z0-9]/g, "");
const week = (branch?: Branch): BranchDay[] => weekDays.map((_, i) => branch?.horarioAtencion.find(d => d.dia === i + 1)
  ?? { dia: i + 1, habilitado: false, apertura: null, cierre: null });

export function BranchForm({ branch, locations }: { branch?: Branch; locations: BranchLocation[] | null }) {
  const [busy, setBusy] = useState(false), [message, setMessage] = useState(""), [saved, setSaved] = useState(false);
  const [province, setProvince] = useState(() => locations?.find(p => [p.provincia, ...p.alias].some(a => normalize(a) === normalize(branch?.provincia ?? "")))?.provincia ?? "");
  const [locality, setLocality] = useState(branch?.localidad ?? ""), [hours, setHours] = useState(() => week(branch));
  const id = useId(), router = useRouter();
  const location = locations?.find(p => p.provincia === province);
  function dayChange(day: number, changes: Partial<BranchDay>) {
    setHours(rows => rows.map(row => row.dia === day ? { ...row, ...changes } : row));
    setSaved(false); setMessage("");
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setSaved(false); setMessage("");
    if (!location) { setMessage("Seleccioná la provincia o Ciudad Autónoma de Buenos Aires."); return; }
    if (!hours.some(d => d.habilitado)) { setMessage("Seleccioná al menos un día de atención."); return; }
    const invalid = hours.find(d => d.habilitado && (!d.apertura || !d.cierre || d.apertura >= d.cierre));
    if (invalid) { setMessage(`${weekDays[invalid.dia - 1]}: completá Desde y Hasta; la apertura debe ser anterior al cierre, dentro del mismo día.`); return; }
    const form = event.currentTarget, data = new FormData(form);
    const values = { ...Object.fromEntries(fields.map(([key]) => [key, String(data.get(key) ?? "").trim()])),
      provincia: province, localidad: locality.trim(), horarioAtencion: hours };
    const payload = branch ? { ...values, codigo: branch.codigo, estado: data.get("estado"), version: branch.version } : values;
    setBusy(true);
    try {
      await secureMutation(`/api/admin/sucursales${branch ? `/${branch.codigoPublico}` : ""}`, JSON.stringify(payload), "application/json", branch ? "PUT" : "POST");
      if (!branch) { form.reset(); setProvince(""); setLocality(""); setHours(week()); }
      setSaved(true); router.refresh();
    } catch (error) { setMessage(error instanceof Error ? error.message : "No pudimos guardar la sucursal."); }
    finally { setBusy(false); }
  }
  function field([key, label, maxLength]: typeof fields[number]) {
    return <label key={key}>{label}<input name={key} required={key !== "correo" && key !== "telefono"} maxLength={maxLength}
      type={key === "correo" ? "email" : key === "telefono" ? "tel" : "text"} defaultValue={branch?.[key] ?? ""}
      readOnly={key === "codigo" && !!branch} pattern={key === "codigo" ? "[A-Za-z0-9_\\-]+" : undefined} /></label>;
  }
  return <form className="identity-form branch-form" onSubmit={submit}>
    <h2>{branch ? "Editar sucursal" : "Nueva sucursal"}</h2>
    <p className="empty-note">Completá la dirección y después elegí qué días abre y en qué horario atiende.</p>
    {!locations && <p className="form-message error-message" role="alert">No pudimos cargar las provincias. <a href="/administracion/sucursales">Volver a intentar</a>.</p>}
    <fieldset className="plain-fieldset" disabled={busy || !locations}>
      <fieldset className="choice-fieldset branch-form-section"><legend>1. Ubicación y contacto</legend>
        <p className="empty-note">País: Argentina. La zona horaria se calcula sin conexión a Internet.</p>
        <div className="form-columns">{fields.slice(0, 4).map(field)}
          <label>Provincia / Ciudad Autónoma<select name="provincia" required value={province} onChange={e => {
            setProvince(e.target.value); setSaved(false);
            if (!locality && e.target.value === "Ciudad Autónoma de Buenos Aires") setLocality("Ciudad Autónoma de Buenos Aires");
          }}><option value="">Seleccionar provincia o CABA</option>{locations?.map(p => <option key={p.provincia} value={p.provincia}>{p.provincia === "Ciudad Autónoma de Buenos Aires" ? "CABA · Ciudad Autónoma de Buenos Aires" : p.provincia}</option>)}</select></label>
          <label>Localidad<input name="localidad" required maxLength={120} value={locality} onChange={e => { setLocality(e.target.value); setSaved(false); }} /></label>
          {fields.slice(4).map(field)}
        </div>
        {branch && !province && <p className="empty-note">Provincia guardada: {branch.provincia}. Seleccionala en el listado para actualizar la ubicación.</p>}
        <p className="branch-timezone" role="status">{location ? <>Zona horaria automática: <strong>{location.desfase}</strong>. Los horarios se interpretan en la hora local de la sucursal.</> : "Seleccioná la provincia para detectar la zona horaria."}</p>
      </fieldset>
      <fieldset className="choice-fieldset branch-form-section"><legend>2. Días y horario de atención</legend>
        <p className="empty-note">Marcá los días que abre la sucursal y elegí Desde y Hasta para cada uno. Los días sin marcar quedan cerrados. Se admite un horario continuo por día, sin cruzar la medianoche.</p>
        <div className="branch-week">{hours.map(d => <div className={`branch-day${d.habilitado ? " is-open" : ""}`} key={d.dia}>
          <label className="checkbox-label branch-day-toggle"><input type="checkbox" checked={d.habilitado} aria-label={`Abierto ${weekDays[d.dia - 1]}`} onChange={e => dayChange(d.dia, { habilitado: e.target.checked, ...(e.target.checked ? {} : { apertura: null, cierre: null }) })} /><span>{weekDays[d.dia - 1]}<small>{d.habilitado ? "Abierto" : "Cerrado"}</small></span></label>
          <label htmlFor={`${id}-${d.dia}-from`}>Desde<input id={`${id}-${d.dia}-from`} aria-label={`Desde ${weekDays[d.dia - 1]}`} type="time" step="60" required={d.habilitado} disabled={!d.habilitado} value={d.apertura ?? ""} onChange={e => dayChange(d.dia, { apertura: e.target.value || null })} /></label>
          <label htmlFor={`${id}-${d.dia}-to`}>Hasta<input id={`${id}-${d.dia}-to`} aria-label={`Hasta ${weekDays[d.dia - 1]}`} type="time" step="60" required={d.habilitado} disabled={!d.habilitado} value={d.cierre ?? ""} onChange={e => dayChange(d.dia, { cierre: e.target.value || null })} /></label>
        </div>)}</div>
        <p className="empty-note">Este horario queda guardado en la ficha. En Configurador → Horarios y entregas podés copiarlo al calendario, completar los cupos y activar la configuración. Guardar esta ficha no cambia los pedidos ni los calendarios ya activos.</p>
      </fieldset>
      {branch && <label>Estado<select name="estado" defaultValue={branch.estado}><option value="ACTIVA">Activa</option><option value="DESACTIVADA">Desactivada</option></select></label>}
    </fieldset>
    {branch && <p className="empty-note">El código permanece fijo. No se puede desactivar la última sucursal activa asignada a un empleado activo.</p>}
    {message && <p className="form-message error-message" role="alert">{message}</p>}
    {saved && <p className="form-message" role="status">Sucursal guardada con su horario de atención. Podés reutilizarlo en el configurador.</p>}
    <button className="refresh" disabled={busy || !locations}>{busy ? "Guardando…" : branch ? "Guardar sucursal" : "Crear sucursal"}</button>
  </form>;
}
