import { redirect } from "next/navigation";
import { IdentityShell } from "@/components/identity-shell";
import { EmployeeForm } from "@/components/organization-forms";
import { getSession } from "@/lib/identity-server";
import { roleHome } from "@/lib/roles";
import { getBranches, getEmployees } from "@/lib/organization-server";
import { permissionLabels } from "@/lib/organization-types";

export const dynamic = "force-dynamic";
export default async function EmployeesPage() {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state === "unavailable") return <IdentityShell title="Empleados" description="Verificación de tu sesión."><p className="form-message error-message" role="alert">No pudimos verificar la sesión. <a href="/administracion/empleados">Volver a comprobar</a></p></IdentityShell>;
  if (session.state !== "authenticated") return null;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol !== "ADMIN_ADMIN") redirect(roleHome(session.profile.rol));
  const [employees, branches] = await Promise.all([getEmployees(), getBranches()]);
  return <IdentityShell title="Empleados" description="Administrá sus datos, sucursales asignadas, permisos y estado. La operación de pagos y pedidos está En construcción.">
    <nav className="page-actions" aria-label="Administración"><a className="secondary-link" href="/administracion">Volver a administración</a><a className="secondary-link" href="/administracion/sucursales">Administrar sucursales</a></nav>
    <section className="branch-list" aria-labelledby="employees-heading"><h2 id="employees-heading">Empleados registrados</h2>
      {employees === null || branches === null ? <p className="form-message error-message" role="alert">No pudimos consultar empleados o sucursales. <a href="/administracion/empleados">Volver a intentar</a></p>
        : employees.length === 0 ? <p className="empty-note">Todavía no hay empleados registrados.</p>
          : employees.map(employee => <article className="branch-card" key={employee.codigoPublico}>
            <h3>{employee.nombre} {employee.apellido}</h3><p>{employee.correo} · {employee.estado === "ACTIVO" ? "Activo" : "Desactivado"}</p>
            <p>Sucursales: {employee.sucursales.map(id => branches.find(branch => branch.codigoPublico === id)?.nombre ?? "Sucursal no disponible").join(", ") || "Sin asignaciones"}</p>
            <p>Permisos: {employee.permisos.map(permission => permissionLabels[permission]).join(", ") || "Sin permisos de pagos"}</p>
            <details className="edit-details"><summary>Editar datos, asignaciones y estado</summary><EmployeeForm key={`${employee.codigoPublico}-${employee.version}`} employee={employee} branches={branches} /></details>
          </article>)}
    </section>
    {branches !== null && <EmployeeForm branches={branches} />}
  </IdentityShell>;
}
