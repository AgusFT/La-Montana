import {installationRequired,operationPending} from "@/lib/installation-messages";
import {getSystemStatus} from "@/lib/system-status";
import { redirect } from "next/navigation";
import { sessionHome } from "@/lib/roles";
import { IdentityShell } from "@/components/identity-shell";
import { LoginForm } from "@/components/identity-forms";
import { getSession, getSetupState } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function AccessPage() {
  const [session, setup, status] = await Promise.all([getSession(), getSetupState(), getSystemStatus()]);
  if (session.state === "authenticated") redirect(sessionHome(session.profile));
  return <IdentityShell access title="Ingresá a La Montaña" description="Accedé con tu correo y contraseña.">
    {(!setup || session.state === "unavailable") && <p className="form-message error-message" role="alert">No pudimos verificar el estado del sistema. Si intentás ingresar, volveremos a comprobar la conexión.</p>}
    {setup?.requierePropietario ? <><p className="form-message error-message" role="alert">{installationRequired}</p><a className="refresh" href="/instalacion">Preparar instalación</a></> : <>{status.ok&&!status.data.operacionDisponible&&<p className="empty-note">{operationPending}</p>}<LoginForm /></>}
  </IdentityShell>;
}
