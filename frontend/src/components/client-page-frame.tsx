import {redirect} from "next/navigation";
import {getSession} from "@/lib/identity-server";
import {roleHome} from "@/lib/roles";
import {IdentityShell} from "./identity-shell";
import {ClientShell} from "./client-shell";
export async function ClientPageFrame({title,description,active,children}:{title:string;description:string;active:"home"|"quotes"|"new"|"orders";children:React.ReactNode}){
 const session=await getSession();
 if(session.state==="anonymous")redirect("/acceso");
 if(session.state==="unavailable")return <IdentityShell title="Mi cuenta" description="Verificación de tu sesión."><p role="alert" className="form-message error-message">No pudimos verificar la sesión. Volvé a intentar cuando el sistema esté disponible.</p><a href="/cliente">Volver a comprobar</a></IdentityShell>;
 if(session.state!=="authenticated")return null;
 if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");
 if(session.profile.rol!=="CLIENTE")redirect(roleHome(session.profile.rol));
 return <ClientShell name={`${session.profile.nombre} ${session.profile.apellido}`} title={active==="home"?`Hola, ${session.profile.nombre}`:title} description={description} active={active}>{children}</ClientShell>;
}
