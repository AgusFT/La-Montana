import { redirect } from "next/navigation";
import { roleHome } from "@/lib/roles";
import { IdentityShell } from "@/components/identity-shell";
import { LogoutButton } from "@/components/identity-forms";
import { getSession } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function ClientPage() {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state === "unavailable") return <IdentityShell title="Mi cuenta" description="Verificación de tu sesión."><p className="form-message error-message" role="alert">No pudimos verificar la sesión. Intentá nuevamente cuando el sistema esté disponible.</p><a className="refresh" href="/cliente">Volver a comprobar</a></IdentityShell>;
  if (session.state !== "authenticated") return null;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol !== "CLIENTE") redirect(roleHome(session.profile.rol));
  const { profile } = session;
  return <IdentityShell title={`Hola, ${profile.nombre}`} description="Tu sesión como cliente está activa.">
    <div className="owner-profile"><h2>{profile.nombre} {profile.apellido}</h2><p>{profile.correo}</p><span className="environment">Cliente particular</span></div>
    <dl className="capabilities"><div><dt>Mis pedidos y cotizaciones</dt><dd>En construcción</dd></div><div><dt>Configuración de trabajos</dt><dd>En construcción</dd></div><div><dt>Correo electrónico</dt><dd>{profile.correoVerificado ? "Verificado" : "Pendiente de verificación"}</dd></div></dl>
    <div className="page-actions"><a className="secondary-link" href="/cuenta/seguridad">Seguridad de la cuenta</a><LogoutButton /></div>
  </IdentityShell>;
}
