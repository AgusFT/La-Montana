//#region ENCABEZADO · src/app/cuenta/seguridad/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/cuenta/seguridad/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta las acciones de seguridad de la cuenta autenticada, incluyendo verificación de correo y
 * cambio de contraseña.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] AccountSecurityPage()
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
import { LogoutButton } from "@/components/identity-forms";
import { EmailVerificationForm, PasswordChangeForm } from "@/components/security-forms";
import { getSession } from "@/lib/identity-server";
import { roleHome } from "@/lib/roles";

export const dynamic = "force-dynamic";
export default async function AccountSecurityPage() {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state === "unavailable") return <IdentityShell title="Seguridad de la cuenta" description="Verificación de tu sesión."><p className="form-message error-message" role="alert">No pudimos verificar la sesión. <a href="/cuenta/seguridad">Volver a comprobar</a></p></IdentityShell>;
  if (session.state !== "authenticated") return null;
  const { profile } = session;
  return <IdentityShell title="Seguridad de la cuenta" description="Verificá tu correo y administrá tu contraseña.">
    <div className="owner-profile"><h2>{profile.nombre} {profile.apellido}</h2><p>{profile.correo}</p></div>
    {profile.debeCambiarContrasena && <p className="form-message security-notice" role="status">Antes de continuar, cambiá tu contraseña inicial. Luego podrás volver a ingresar y acceder a tus sucursales.</p>}
    {profile.debeCambiarContrasena && <PasswordChangeForm />}
    <EmailVerificationForm correo={profile.correo} verified={profile.correoVerificado} />
    {!profile.debeCambiarContrasena && <PasswordChangeForm />}
    <div className="page-actions">{!profile.debeCambiarContrasena && <a className="secondary-link" href={roleHome(profile.rol)}>Volver a mi espacio</a>}<LogoutButton /></div>
  </IdentityShell>;
}
