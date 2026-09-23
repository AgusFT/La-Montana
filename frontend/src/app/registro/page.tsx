import { redirect } from "next/navigation";
import { sessionHome } from "@/lib/roles";
import { IdentityShell } from "@/components/identity-shell";
import { RegistrationForm } from "@/components/identity-forms";
import {operationPending} from "@/lib/installation-messages";
import {getSystemStatus} from "@/lib/system-status";
import { getSession, getSetupState } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function RegistrationPage() {
  const [session,setup,status] = await Promise.all([getSession(),getSetupState(),getSystemStatus()]);
  if(setup?.requierePropietario)redirect("/acceso");
  if (session.state === "authenticated") redirect(sessionHome(session.profile));
  return <IdentityShell access title="Creá tu cuenta" description="Registrate como cliente particular de La Montaña.">{!setup?<p className="form-message error-message" role="alert">No pudimos comprobar si el registro está habilitado. <a href="/registro">Volver a intentar</a></p>:<>{status.ok&&!status.data.operacionDisponible&&<p className="empty-note">{operationPending}</p>}<RegistrationForm /></>}</IdentityShell>;
}
