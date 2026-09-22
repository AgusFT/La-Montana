import { redirect } from "next/navigation";
import { roleHome } from "@/lib/roles";
import { IdentityShell } from "@/components/identity-shell";
import { LogoutButton } from "@/components/identity-forms";
import { getSession } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function AdministrationPage() {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state === "unavailable") return <IdentityShell title="Administración" description="Verificación de tu sesión."><p className="form-message error-message" role="alert">No pudimos verificar la sesión. Intentá nuevamente cuando el sistema esté disponible.</p><a className="refresh" href="/administracion">Volver a comprobar</a></IdentityShell>;
  if (session.state !== "authenticated") return null;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol !== "ADMIN_ADMIN") redirect(roleHome(session.profile.rol));
  const owner = session.profile;
  return <IdentityShell title={`Hola, ${owner.nombre}`} description="Tu sesión como propietario está activa.">
    <div className="owner-profile"><h2>{owner.nombre} {owner.apellido}</h2><p>{owner.correo}</p><span className="environment">Propietario de la imprenta</span></div>
    <dl className="capabilities"><div><dt>Gestión de clientes</dt><dd>En construcción</dd></div><div><dt>Configuración de la imprenta</dt><dd>Borrador y modelo disponibles; fases restantes En construcción</dd></div><div><dt>Pedidos y operación</dt><dd>En construcción</dd></div></dl>
    <div className="page-actions"><a className="refresh" href="/administracion/configuracion">Configurar la imprenta</a><a className="refresh" href="/administracion/catalogo">Servicios y precios</a><a className="refresh" href="/administracion/sucursales">Administrar sucursales</a><a className="refresh" href="/administracion/empleados">Administrar empleados</a><a className="secondary-link" href="/operacion">Ver sucursales operativas</a><a className="secondary-link" href="/cuenta/seguridad">Seguridad de la cuenta</a><LogoutButton /></div>
  </IdentityShell>;
}
