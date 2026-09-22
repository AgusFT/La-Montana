"use client";

import { useId, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { secureMutation } from "@/lib/secure-mutation";
import { permissionLabels, type Branch, type Employee } from "@/lib/organization-types";

const branchFields = [
  ["codigo", "Código", 40], ["nombre", "Nombre", 140], ["calle", "Calle", 160], ["numero", "Número", 20],
  ["localidad", "Localidad", 120], ["provincia", "Provincia", 120], ["codigoPostal", "Código postal", 12],
  ["zonaHoraria", "Zona horaria IANA", 64], ["correo", "Correo (opcional)", 254], ["telefono", "Teléfono (opcional)", 40],
] as const;

export function BranchForm({ branch }: { branch?: Branch }) {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [saved, setSaved] = useState(false);
  const router = useRouter();
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget, data = new FormData(form);
    const fields = Object.fromEntries(branchFields.map(([key]) => [key, String(data.get(key) ?? "").trim()]));
    const payload = branch ? { ...fields, codigo: branch.codigo, estado: data.get("estado"), version: branch.version } : fields;
    setBusy(true); setMessage(""); setSaved(false);
    try {
      await secureMutation(`/api/admin/sucursales${branch ? `/${branch.codigoPublico}` : ""}`, JSON.stringify(payload), "application/json", branch ? "PUT" : "POST");
      if (!branch) form.reset();
      setSaved(true); router.refresh();
    } catch (error) { setMessage(error instanceof Error ? error.message : "No pudimos guardar la sucursal."); }
    finally { setBusy(false); }
  }
  return <form className="identity-form" onSubmit={submit}>
    <h2>{branch ? "Editar sucursal" : "Nueva sucursal"}</h2>
    <fieldset className="plain-fieldset" disabled={busy}><div className="form-columns">
      {branchFields.map(([key, label, maxLength]) => <label key={key}>{label}<input name={key} required={key !== "correo" && key !== "telefono"} maxLength={maxLength}
        type={key === "correo" ? "email" : key === "telefono" ? "tel" : "text"} defaultValue={branch?.[key] ?? ""}
        readOnly={key === "codigo" && !!branch} pattern={key === "codigo" ? "[A-Za-z0-9_\\-]+" : undefined}
        placeholder={key === "zonaHoraria" ? "America/Argentina/Buenos_Aires" : undefined} /></label>)}
    </div>
    {branch && <label>Estado<select name="estado" defaultValue={branch.estado}><option value="ACTIVA">Activa</option><option value="DESACTIVADA">Desactivada</option></select></label>}
    </fieldset>
    {branch && <p className="empty-note">El código permanece fijo. No se puede desactivar la última sucursal activa asignada a un empleado activo.</p>}
    {message && <p className="form-message error-message" role="alert">{message}</p>}
    {saved && <p className="form-message" role="status">Sucursal guardada.</p>}
    <button className="refresh" disabled={busy}>{busy ? "Guardando…" : branch ? "Guardar sucursal" : "Crear sucursal"}</button>
  </form>;
}

export function EmployeeForm({ employee, branches }: { employee?: Employee; branches: Branch[] }) {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [saved, setSaved] = useState(false);
  const helpId = useId();
  const router = useRouter();
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget, data = new FormData(form);
    const sucursales = data.getAll("sucursales").map(String);
    const estado = employee ? String(data.get("estado")) : "ACTIVO";
    if (estado === "ACTIVO" && !branches.some(branch => branch.estado === "ACTIVA" && sucursales.includes(branch.codigoPublico))) {
      setMessage("Asigná al menos una sucursal activa al empleado activo."); return;
    }
    if (!employee && data.get("contrasena") !== data.get("confirmacion")) { setMessage("Las contraseñas no coinciden."); return; }
    const common = { nombre: String(data.get("nombre")).trim(), apellido: String(data.get("apellido")).trim(), correo: String(data.get("correo")).trim(), sucursales, permisos: data.getAll("permisos").map(String) };
    const payload = employee ? { ...common, estado, version: employee.version } : { ...common, contrasena: String(data.get("contrasena")) };
    setBusy(true); setMessage(""); setSaved(false);
    try {
      await secureMutation(`/api/admin/empleados${employee ? `/${employee.codigoPublico}` : ""}`, JSON.stringify(payload), "application/json", employee ? "PUT" : "POST");
      if (!employee) form.reset();
      setSaved(true); router.refresh();
    } catch (error) { setMessage(error instanceof Error ? error.message : "No pudimos guardar el empleado."); }
    finally { setBusy(false); }
  }
  return <form className="identity-form" onSubmit={submit}>
    <h2>{employee ? "Editar empleado" : "Nuevo empleado"}</h2>
    <fieldset className="plain-fieldset" disabled={busy}>
      <div className="form-columns"><label>Nombre<input name="nombre" required maxLength={100} defaultValue={employee?.nombre ?? ""} /></label><label>Apellido<input name="apellido" required maxLength={100} defaultValue={employee?.apellido ?? ""} /></label></div>
      <label>Correo electrónico<input name="correo" type="email" required maxLength={254} defaultValue={employee?.correo ?? ""} /></label>
      {!employee && <><label>Contraseña inicial<input name="contrasena" type="password" autoComplete="new-password" required minLength={12} maxLength={128} aria-describedby={helpId} /></label><p id={helpId} className="field-help">Entre 12 y 128 caracteres. El empleado deberá cambiarla en su primer ingreso.</p><label>Repetir contraseña<input name="confirmacion" type="password" autoComplete="new-password" required minLength={12} maxLength={128} /></label></>}
      {employee && <label>Estado<select name="estado" defaultValue={employee.estado}><option value="ACTIVO">Activo</option><option value="DESACTIVADO">Desactivado</option></select></label>}
      <fieldset className="choice-fieldset"><legend>Sucursales asignadas</legend><p className="empty-note">Un empleado activo necesita al menos una sucursal activa.</p>
        {branches.length === 0 ? <p className="empty-note">Primero creá una sucursal.</p> : branches.map(branch => <label className="checkbox-label" key={branch.codigoPublico}><input type="checkbox" name="sucursales" value={branch.codigoPublico} defaultChecked={employee?.sucursales.includes(branch.codigoPublico) ?? false} disabled={branch.estado !== "ACTIVA" && !employee?.sucursales.includes(branch.codigoPublico)} /><span>{branch.nombre} ({branch.codigo}){branch.estado !== "ACTIVA" ? " · Desactivada" : ""}</span></label>)}
      </fieldset>
      <fieldset className="choice-fieldset"><legend>Permisos</legend><p className="empty-note">Se guardan las autorizaciones. La operación de pagos y cobros está En construcción.</p>
        {Object.entries(permissionLabels).map(([code, label]) => <label className="checkbox-label" key={code}><input type="checkbox" name="permisos" value={code} defaultChecked={employee?.permisos.some(permission => permission === code) ?? false} /><span>{label}</span></label>)}
      </fieldset>
    </fieldset>
    {message && <p className="form-message error-message" role="alert">{message}</p>}
    {saved && <p className="form-message" role="status">Empleado guardado.</p>}
    <button className="refresh" disabled={busy || (!employee && !branches.some(branch => branch.estado === "ACTIVA"))}>{busy ? "Guardando…" : employee ? "Guardar empleado" : "Crear empleado"}</button>
  </form>;
}
