//#region ENCABEZADO · src/app/administracion/pagina-web/vista-previa/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/administracion/pagina-web/vista-previa/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Muestra al propietario una vista previa privada del borrador web y comprueba la versión
 * solicitada sin publicar el contenido.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] PreviewPage({searchParams}:
 *   {searchParams:Promise<{version?:string}>})
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

import {getSession} from "@/lib/identity-server";
import {getWebsiteState} from "@/lib/website-server";
import {WebsiteView} from "@/components/website-view";
import "@/app/web/web.css";
export const dynamic="force-dynamic";
export default async function PreviewPage({searchParams}:{searchParams:Promise<{version?:string}>}){
 const session=await getSession();if(session.state!=="authenticated"||session.profile.rol!=="ADMIN_ADMIN"||session.profile.debeCambiarContrasena)return <main className="shell"><h1>Vista previa privada</h1><p role="alert">Sólo el propietario con sesión activa puede ver este borrador.</p></main>;
 const state=await getWebsiteState(),query=await searchParams;
 if(!state)return <main className="shell"><p role="alert">No pudimos consultar el borrador. Actualizá la vista previa desde el configurador.</p></main>;
 if(query.version&&query.version!==String(state.borrador.version))return <main className="shell"><p role="alert">El borrador cambió. Volvé al configurador y consultá la versión guardada antes de revisar.</p></main>;
 return <WebsiteView content={state.borrador.contenido} preview/>;
}
