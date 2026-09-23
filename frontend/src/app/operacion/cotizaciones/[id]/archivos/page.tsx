import {notFound,redirect} from "next/navigation";
import {getSession} from "@/lib/identity-server";
import {uuidPattern} from "@/lib/organization-types";
import {IdentityShell} from "@/components/identity-shell";
import {PrivateFiles} from "@/components/private-files";
import "@/app/client.css";
export const dynamic="force-dynamic";
export default async function QuoteFilesPage({params}:{params:Promise<{id:string}>}){
 const session=await getSession();if(session.state==="anonymous")redirect("/acceso");
 if(session.state!=="authenticated")return <IdentityShell title="Archivos recibidos" description="Verificación de acceso."><p role="alert">No pudimos verificar la sesión. Volvé a intentar.</p></IdentityShell>;
 if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(session.profile.rol==="CLIENTE")redirect("/cliente");
 const{id}=await params;if(!uuidPattern.test(id))notFound();
 return <IdentityShell title="Archivos de la cotización" description="Consulta autorizada por sucursal. Revisá el trabajo y sus correcciones desde el pedido confirmado."><PrivateFiles quote={id} version={0} internal/><a href="/operacion">Volver a mis sucursales</a></IdentityShell>;
}
