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
