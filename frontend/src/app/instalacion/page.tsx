//#region ENCABEZADO · src/app/instalacion/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/instalacion/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Consulta el estado de instalación y presenta el formulario de creación del propietario cuando el
 * alta inicial está habilitada.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] InstallationPage()
 *   Componente de página exportado por la ruta de Next.js.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - dynamic [const, exportado].
 * ========================================================================
 */
//#endregion

import { redirect } from "next/navigation";
import { IdentityShell } from "@/components/identity-shell";
import { InstallationForm } from "@/components/identity-forms";
import { sessionHome } from "@/lib/roles";
import { getSession, getSetupState } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function InstallationPage() {
  const [setup, session] = await Promise.all([getSetupState(), getSession()]);
  if (session.state === "authenticated") redirect(sessionHome(session.profile));
  if (setup && !setup.requierePropietario) redirect("/acceso");
  return <IdentityShell title="Preparar la instalación" description="Creá la cuenta del propietario para comenzar.">
    {!setup ? <p className="form-message error-message" role="alert">No pudimos consultar el estado de instalación. Intentá nuevamente cuando el backend esté disponible.</p>
      : !setup.altaHabilitada ? <p className="form-message error-message" role="alert">El alta no está habilitada. El operador debe configurar el token de instalación en el servidor.</p>
        : <InstallationForm />}
  </IdentityShell>;
}
