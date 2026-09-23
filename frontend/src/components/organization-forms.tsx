//#region ENCABEZADO · src/components/organization-forms.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/organization-forms.tsx
 * ========================================================================
 * FUNCIÓN
 * Gestiona alta y edición de empleados, sus sucursales, permisos y estado, con validación,
 * protección CSRF y tratamiento de conflictos.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] EmployeeForm({ employee, branches }: { employee?: Employee; branches: Branch[] })
 *   Componente de interfaz.
 * - [async] EmployeeForm :: submit(event: FormEvent<HTMLFormElement>)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - reexportación: export { BranchForm } from "./branch-form";
 * ========================================================================
 */
//#endregion

"use client";

import { useId, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { secureMutation } from "@/lib/secure-mutation";
import { permissionLabels, type Branch, type Employee } from "@/lib/organization-types";

export { BranchForm } from "./branch-form";

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
      <fieldset className="choice-fieldset"><legend>Permisos</legend><p className="empty-note">Estos permisos habilitan acciones sobre los pedidos, pagos y entregas de las sucursales asignadas.</p>
        {Object.entries(permissionLabels).map(([code, label]) => <label className="checkbox-label" key={code}><input type="checkbox" name="permisos" value={code} defaultChecked={employee?.permisos.some(permission => permission === code) ?? false} /><span>{label}</span></label>)}
      </fieldset>
    </fieldset>
    {message && <p className="form-message error-message" role="alert">{message}</p>}
    {saved && <p className="form-message" role="status">Empleado guardado.</p>}
    <button className="refresh" disabled={busy || (!employee && !branches.some(branch => branch.estado === "ACTIVA"))}>{busy ? "Guardando…" : employee ? "Guardar empleado" : "Crear empleado"}</button>
  </form>;
}
