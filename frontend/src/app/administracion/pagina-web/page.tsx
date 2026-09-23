//#region ENCABEZADO · src/app/administracion/pagina-web/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/administracion/pagina-web/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Comprueba el acceso del propietario, obtiene el borrador del sitio y presenta el configurador de
 * la página web pública.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] WebsiteConfigurationPage()
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

import {redirect} from "next/navigation";
import {getSession} from "@/lib/identity-server";
import {roleHome} from "@/lib/roles";
import {getWebsiteState} from "@/lib/website-server";
import {AdminShell} from "@/components/admin-shell";
import {WebsiteWorkspace} from "@/components/website-workspace";
export const dynamic="force-dynamic";
export default async function WebsiteConfigurationPage(){
 const session=await getSession();if(session.state==="anonymous")redirect("/acceso");
 if(session.state!=="authenticated")return <main className="shell"><h1>Configurador de página web</h1><p role="alert">No pudimos verificar la sesión. <a href="/administracion/pagina-web">Volver a intentar</a></p></main>;
 if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(session.profile.rol!=="ADMIN_ADMIN")redirect(roleHome(session.profile.rol));
 const state=await getWebsiteState();return <AdminShell title="Configurador de página web" description="Prepará la página pública de tu imprenta y publicala cuando esté lista." name={`${session.profile.nombre} ${session.profile.apellido}`} active="web">{state?<WebsiteWorkspace initial={state}/>:<p className="admin-error" role="alert">No pudimos consultar el borrador web. <a href="/administracion/pagina-web">Volver a intentar</a></p>}</AdminShell>;
}
