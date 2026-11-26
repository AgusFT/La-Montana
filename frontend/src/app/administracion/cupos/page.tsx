import { redirect } from "next/navigation";
import { AdminShell } from "@/components/admin-shell";
import { DemoQuotas } from "@/components/demo-quotas";
import { IdentityShell } from "@/components/identity-shell";
import { getSession } from "@/lib/identity-server";
import { roleHome } from "@/lib/roles";

export const dynamic = "force-dynamic";

export default async function DemoQuotaPage({ searchParams }: { searchParams: Promise<{ recurso?: string }> }) {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state !== "authenticated") return <IdentityShell title="Cupos de la Demo" description="Verificación de tu sesión.">
    <p role="alert">No pudimos verificar la sesión. <a href="/administracion/cupos">Volver a intentar</a></p>
  </IdentityShell>;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol !== "ADMIN_ADMIN") redirect(roleHome(session.profile.rol));

  const { recurso } = await searchParams;
  return <AdminShell name={`${session.profile.nombre} ${session.profile.apellido}`} active="cupos"
    title="Cupos de la Demo" description="Consultá los lugares en uso y las bajas de la imprenta.">
    <DemoQuotas initialGroup={recurso === "CLIENTES" ? "CLIENTES" : "PEDIDOS"} />
  </AdminShell>;
}
