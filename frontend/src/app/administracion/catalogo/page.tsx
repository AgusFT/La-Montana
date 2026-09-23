//#region ENCABEZADO · src/app/administracion/catalogo/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/administracion/catalogo/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Protege la pantalla de servicios y precios para el propietario, obtiene el catálogo inicial y
 * monta su editor dentro de la navegación administrativa.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] CatalogPage()
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
