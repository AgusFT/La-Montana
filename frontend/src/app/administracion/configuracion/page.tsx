import {redirect} from "next/navigation";
import {AdminShell} from "@/components/admin-shell";
import {ConfigurationWorkspace} from "@/components/configuration-workspace";
import {getSession} from "@/lib/identity-server";
import {roleHome} from "@/lib/roles";
import {getConfiguration} from "@/lib/configuration-server";
export const dynamic="force-dynamic";
export default async function ConfigurationPage(){
  const session=await getSession();
  if(session.state==="anonymous")redirect("/acceso");
  if(session.state==="unavailable")return <main className="shell"><h1>Motor de configuración</h1><p role="alert">No pudimos verificar tu sesión.</p><a href="/administracion/configuracion">Volver a intentar</a></main>;
  if(session.state!=="authenticated")return null;
  if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");
  if(session.profile.rol!=="ADMIN_ADMIN")redirect(roleHome(session.profile.rol));
  const state=await getConfiguration();
  return <AdminShell title="Motor de configuración" description="Configuración operativa de la imprenta" name={`${session.profile.nombre} ${session.profile.apellido}`} active="configuracion">
    {state?<ConfigurationWorkspace initial={state}/>:<div className="admin-card"><p role="alert">No pudimos consultar la configuración.</p><a className="admin-button" href="/administracion/configuracion">Volver a intentar</a></div>}
  </AdminShell>;
}
