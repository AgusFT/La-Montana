import {AdminShell} from "@/components/admin-shell";
import { redirect } from "next/navigation";
import { roleHome } from "@/lib/roles";
import { IdentityShell } from "@/components/identity-shell";
import { BranchForm } from "@/components/organization-forms";
import { getSession } from "@/lib/identity-server";
import { getBranches } from "@/lib/branches-server";

export const dynamic = "force-dynamic";
export default async function BranchesPage() {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state === "unavailable") return <IdentityShell title="Sucursales" description="Verificación de tu sesión."><p className="form-message error-message" role="alert">No pudimos verificar la sesión. Intentá nuevamente cuando el sistema esté disponible.</p><a className="refresh" href="/administracion/sucursales">Volver a comprobar</a></IdentityShell>;
  if (session.state !== "authenticated") return null;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol !== "ADMIN_ADMIN") redirect(roleHome(session.profile.rol));
  const branches = await getBranches();
  return <AdminShell name={`${session.profile.nombre} ${session.profile.apellido}`} active="sucursales" title="Sucursales" description="Alta, edición y desactivación disponibles. Los horarios y cupos se definen en el configurador; los pedidos se gestionan desde Operación.">
    <a className="secondary-link" href="/administracion">Volver a administración</a>
    <section className="branch-list" aria-labelledby="branches-heading"><h2 id="branches-heading">Sucursales registradas</h2>
      {branches === null ? <p className="form-message error-message" role="alert">No pudimos consultar las sucursales. <a href="/administracion/sucursales">Volver a intentar</a></p> : branches.length === 0 ? <p className="empty-note">Todavía no hay sucursales registradas.</p> : branches.map(branch => <article className="branch-card" key={branch.codigoPublico}>
        <h3>{branch.nombre}</h3><p>Código: {branch.codigo} · Estado: {branch.estado}</p>
        <p>{branch.calle} {branch.numero}, {branch.localidad}, {branch.provincia} · CP {branch.codigoPostal}</p>
        <p>Zona horaria: {branch.zonaHoraria}</p>
        {branch.correo && <p>Correo: {branch.correo}</p>}{branch.telefono && <p>Teléfono: {branch.telefono}</p>}
        <details className="edit-details"><summary>Editar datos y estado</summary><BranchForm key={`${branch.codigoPublico}-${branch.version}`} branch={branch} /></details>
      </article>)}
    </section>
    <BranchForm />
  </AdminShell>;
}
