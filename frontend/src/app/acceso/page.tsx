import { redirect } from "next/navigation";
import { IdentityShell } from "@/components/identity-shell";
import { LoginForm } from "@/components/identity-forms";
import { getSession, getSetupState } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function AccessPage() {
  const [session, setup] = await Promise.all([getSession(), getSetupState()]);
  if (session.state === "authenticated") redirect(session.profile.rol === "ADMIN_ADMIN" ? "/administracion" : "/cliente");
  return <IdentityShell title="Ingresá a La Montaña" description="Accedé con tu correo y contraseña.">
    {(!setup || session.state === "unavailable") && <p className="form-message error-message" role="alert">No pudimos verificar el estado del sistema. Si intentás ingresar, volveremos a comprobar la conexión.</p>}
    {setup?.requierePropietario ? <><p className="empty-note">Todavía no se creó el propietario de esta instalación.</p><a className="refresh" href="/instalacion">Preparar instalación</a></> : <LoginForm />}
  </IdentityShell>;
}
