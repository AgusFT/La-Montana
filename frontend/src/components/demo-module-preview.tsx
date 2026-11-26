import { redirect } from "next/navigation";
import { AdminShell } from "./admin-shell";
import { IdentityShell } from "./identity-shell";
import { getSession } from "@/lib/identity-server";
import { roleHome } from "@/lib/roles";
import "./demo-preview.css";

export async function DemoModulePreview({ area, title, description, children }: {
  area: "empleados" | "puntos" | "riders"; title: string; description: string; children: React.ReactNode;
}) {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state !== "authenticated") return <IdentityShell title={title} description="Verificación de tu sesión.">
    <p role="alert">No pudimos verificar tu sesión.</p>
  </IdentityShell>;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol !== "ADMIN_ADMIN") redirect(roleHome(session.profile.rol));
  return <AdminShell title={title} active={area} name={`${session.profile.nombre} ${session.profile.apellido}`} description={description}>
    <p className="demo-preview-notice">Vista previa de la versión completa · Acciones deshabilitadas en la Demo</p>
    <fieldset className={`demo-module-preview demo-preview-${area}`} aria-label={title} disabled>{children}</fieldset>
  </AdminShell>;
}
