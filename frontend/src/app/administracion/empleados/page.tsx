import { DemoModulePreview } from "@/components/demo-module-preview";
import { permissionLabels, type Permission } from "@/lib/organization-types";

const employee = { nombre: "Mateo", apellido: "Tintas", correo: "produccion@demo.invalid" };
const branch = "Imprenta Senderos · Centro";
const permissions: Permission[] = ["GESTIONAR_PEDIDOS", "GESTIONAR_PRODUCCION", "CONTROLAR_CALIDAD",
  "GESTIONAR_ENTREGAS", "CERRAR_PEDIDOS", "REGISTRAR_COBRO"];

function EmployeeFields({ edit = false }: { edit?: boolean }) {
  return <form className="identity-form">
    <h2>{edit ? "Editar empleado" : "Nuevo empleado"}</h2>
    <fieldset className="plain-fieldset" disabled>
      <div className="form-columns">
        <label>Nombre<input defaultValue={edit ? employee.nombre : ""}/></label>
        <label>Apellido<input defaultValue={edit ? employee.apellido : ""}/></label>
      </div>
      <label>Correo electrónico<input type="email" defaultValue={edit ? employee.correo : ""}/></label>
      {edit ? <label>Estado<select><option>Activo</option><option>Desactivado</option></select></label> : <>
        <label>Contraseña inicial<input type="password" autoComplete="new-password" aria-describedby="employee-password-help"/></label>
        <p id="employee-password-help" className="field-help">Entre 12 y 128 caracteres. El empleado deberá cambiarla en su primer ingreso.</p>
        <label>Repetir contraseña<input type="password" autoComplete="new-password"/></label>
      </>}
      <fieldset className="choice-fieldset"><legend>Sucursales asignadas</legend>
        <p className="empty-note">Un empleado activo necesita al menos una sucursal activa.</p>
        <label className="checkbox-label"><input type="checkbox" defaultChecked={edit}/><span>{branch} (DEMO-SENDEROS)</span></label>
      </fieldset>
      <fieldset className="choice-fieldset"><legend>Permisos</legend>
        <p className="empty-note">Estos permisos habilitan acciones sobre los pedidos, pagos y entregas de las sucursales asignadas.</p>
        {Object.entries(permissionLabels).map(([code, label]) =>
          <label key={code} className="checkbox-label"><input type="checkbox" defaultChecked={edit && permissions.some(permission => permission === code)}/><span>{label}</span></label>)}
      </fieldset>
    </fieldset>
    <button type="button" className="refresh" disabled>{edit ? "Guardar empleado" : "Crear empleado"}</button>
  </form>;
}

export default function EmployeesPage() {
  return <DemoModulePreview area="empleados" title="Empleados" description="Administrá sus datos, sucursales asignadas, permisos y estado. Los permisos se aplican a la operación de pedidos y pagos de cada sucursal.">
    <nav className="page-actions" aria-label="Administración">
      <a className="secondary-link" href="/administracion">Volver a administración</a>
      <a className="secondary-link" href="/administracion/sucursales">Administrar sucursales</a>
    </nav>
    <section className="branch-list" aria-labelledby="employees-heading">
      <h2 id="employees-heading">Empleados registrados</h2>
      <article className="branch-card">
        <h3>{employee.nombre} {employee.apellido}</h3><p>{employee.correo} · Activo</p>
        <p>Sucursales: {branch}</p>
        <p>Permisos: {permissions.map(permission => permissionLabels[permission]).join(", ")}</p>
        <details className="edit-details"><summary>Editar datos, asignaciones y estado</summary><EmployeeFields edit/></details>
      </article>
    </section>
    <EmployeeFields/>
    <p className="admin-note">Ficha de muestra de Mateo Tintas · Empleado de Imprenta Senderos.</p>
  </DemoModulePreview>;
}
