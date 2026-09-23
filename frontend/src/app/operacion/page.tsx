import { redirect } from "next/navigation";
import { IdentityShell } from "@/components/identity-shell";
import { LogoutButton } from "@/components/identity-forms";
import { getSession } from "@/lib/identity-server";
import { getOperationContext } from "@/lib/organization-server";
import { permissionLabels } from "@/lib/organization-types";

export const dynamic = "force-dynamic";
export default async function OperationPage() {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state === "unavailable") return <IdentityShell title="Operación" description="Verificación de tu sesión."><p className="form-message error-message" role="alert">No pudimos verificar la sesión. <a href="/operacion">Volver a comprobar</a></p></IdentityShell>;
  if (session.state !== "authenticated") return null;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol === "CLIENTE") redirect("/cliente");
  const context = await getOperationContext();
  return <IdentityShell title={`Hola, ${session.profile.nombre}`} description="Consultá los pedidos, archivos y pagos de tus sucursales habilitadas.">
    <div className="owner-profile"><h2>{session.profile.nombre} {session.profile.apellido}</h2><p>{session.profile.correo}</p><span className="environment">{session.profile.rol === "EMPLEADO" ? "Empleado" : "Propietario"}</span></div>
    {context === null ? <p className="form-message error-message" role="alert">No pudimos consultar tu acceso operativo. <a href="/operacion">Volver a intentar</a></p> : <>
      <section className="branch-list" aria-labelledby="operation-branches"><h2 id="operation-branches">Tus sucursales activas</h2>
        {context.sucursales.length === 0 ? <p className="empty-note">No hay sucursales activas habilitadas para tu cuenta.</p> : context.sucursales.map(branch => <article className="branch-card" key={branch.codigoPublico}><h3>{branch.nombre}</h3><p>{branch.calle} {branch.numero}, {branch.localidad}</p><a className="secondary-link" href={`/operacion/sucursales/${branch.codigoPublico}`}>Ver sucursal</a></article>)}
      </section>
      <h2>Permisos asignados</h2>{context.permisos.length ? <ul className="permission-list">{context.permisos.map(permission => <li key={permission}>{permissionLabels[permission]}</li>)}</ul> : <p className="empty-note">Sin permisos de pagos y cobros asignados.</p>}
      <p className="empty-note">Revisión, correcciones, producción manual y calidad están disponibles desde los pedidos de cada sucursal. La logística y entrega efectiva siguen en construcción.</p>
    </>}
    <div className="page-actions">{session.profile.rol === "ADMIN_ADMIN" && <a className="secondary-link" href="/administracion">Volver a administración</a>}<a className="secondary-link" href="/cuenta/seguridad">Seguridad de la cuenta</a><LogoutButton /></div>
  </IdentityShell>;
}
