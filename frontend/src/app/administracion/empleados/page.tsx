import { DemoModulePreview } from "@/components/demo-module-preview";
import { permissionLabels } from "@/lib/organization-types";

function EmployeeFields({ edit = false }: { edit?: boolean }) {
  return <section className="admin-card">
    <h2>{edit ? "Editar empleado" : "Nuevo empleado"}</h2>
    <div className="admin-form-grid">
      <label>Nombre<input defaultValue={edit ? "Mateo" : ""} /></label>
      <label>Apellido<input defaultValue={edit ? "Tintas" : ""} /></label>
      <label>Correo electrónico<input type="email" defaultValue={edit ? "produccion@demo.invalid" : ""} /></label>
      {edit ? <label>Estado<select><option>Activo</option><option>Desactivado</option></select></label>
        : <><label>Contraseña inicial<input type="password" /></label><label>Repetir contraseña<input type="password" /></label></>}
    </div>
    <h3>Sucursales asignadas</h3>
    <label className="admin-check"><input type="checkbox" defaultChecked={edit}/>Imprenta Senderos · Centro</label>
    <h3>Permisos</h3>
    <div className="admin-form-grid">{Object.entries(permissionLabels).map(([code, label]) =>
      <label key={code} className="admin-check"><input type="checkbox"
        defaultChecked={edit && ["GESTIONAR_PEDIDOS","GESTIONAR_PRODUCCION","CONTROLAR_CALIDAD","GESTIONAR_ENTREGAS","CERRAR_PEDIDOS","REGISTRAR_COBRO"].includes(code)}/>{label}</label>)}</div>
    <div className="configuration-actions"><button type="button" className="admin-button">{edit ? "Guardar empleado" : "Crear empleado"}</button></div>
  </section>;
}

export default function EmployeesPage() {
  return <DemoModulePreview area="empleados" title="Administración de usuarios" description="Empleados, sucursales asignadas, permisos y estado de acceso.">
    <section className="admin-card"><h2>Empleados registrados</h2><p>Ficha de muestra de Mateo Tintas · Empleado de Imprenta Senderos.</p></section>
    <EmployeeFields edit/><EmployeeFields/>
  </DemoModulePreview>;
}
