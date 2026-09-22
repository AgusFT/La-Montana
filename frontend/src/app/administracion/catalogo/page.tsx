import { redirect } from "next/navigation";
import { AdminShell } from "@/components/admin-shell";
import { CatalogWorkspace } from "@/components/catalog-workspace";
import { getSession } from "@/lib/identity-server";
import { roleHome } from "@/lib/roles";
import { getCatalog } from "@/lib/catalog-server";

export const dynamic = "force-dynamic";
export default async function CatalogPage() {
  const session=await getSession();
  if(session.state==="anonymous") redirect("/acceso");
  if(session.state==="unavailable") return <main className="shell"><h1>Servicios y precios</h1><p role="alert">No pudimos verificar tu sesión.</p><a href="/administracion/catalogo">Volver a intentar</a></main>;
  if(session.state!=="authenticated") return null;
  if(session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if(session.profile.rol!=="ADMIN_ADMIN") redirect(roleHome(session.profile.rol));
  const catalog=await getCatalog();
  return <AdminShell title="Servicios y precios" description="Gestión del catálogo comercial de tu imprenta" name={`${session.profile.nombre} ${session.profile.apellido}`}>
    {catalog ? <CatalogWorkspace initial={catalog}/> : <div className="admin-card"><p className="form-message error-message" role="alert">No pudimos consultar el catálogo. Tus datos no se han modificado.</p><a className="admin-button" href="/administracion/catalogo">Volver a intentar</a></div>}
  </AdminShell>;
}
