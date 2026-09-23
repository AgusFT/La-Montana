import { redirect } from "next/navigation";
import { sessionHome } from "@/lib/roles";
import { IdentityShell } from "@/components/identity-shell";
import { RegistrationForm } from "@/components/identity-forms";
import { getSession } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function RegistrationPage() {
  const session = await getSession();
  if (session.state === "authenticated") redirect(sessionHome(session.profile));
  return <IdentityShell access title="Creá tu cuenta" description="Registrate como cliente particular de La Montaña."><RegistrationForm /></IdentityShell>;
}
