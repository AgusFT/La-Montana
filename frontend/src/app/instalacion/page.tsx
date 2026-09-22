import { redirect } from "next/navigation";
import { IdentityShell } from "@/components/identity-shell";
import { InstallationForm } from "@/components/identity-forms";
import { getSetupState } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function InstallationPage() {
  const setup = await getSetupState();
  if (setup && !setup.requierePropietario) redirect("/acceso");
  return <IdentityShell title="Preparar la instalación" description="Creá la cuenta del propietario para comenzar.">
    {!setup ? <p className="form-message error-message" role="alert">No pudimos consultar el estado de instalación. Intentá nuevamente cuando el backend esté disponible.</p>
      : !setup.altaHabilitada ? <p className="form-message error-message" role="alert">El alta no está habilitada. El operador debe configurar el token de instalación en el servidor.</p>
        : <InstallationForm />}
  </IdentityShell>;
}
