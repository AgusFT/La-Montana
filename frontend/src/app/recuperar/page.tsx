import { IdentityShell } from "@/components/identity-shell";
import { RecoveryForms } from "@/components/security-forms";

export default function RecoveryPage() {
  return <IdentityShell access title="Recuperar acceso" description="Recibí un código por correo para elegir una nueva contraseña.">
    <a className="secondary-link" href="/acceso">Volver a iniciar sesión</a>
    <RecoveryForms />
  </IdentityShell>;
}
