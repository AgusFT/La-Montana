import {redirect} from "next/navigation";
import {getSession} from "@/lib/identity-server";
import {roleHome} from "@/lib/roles";
import {SoportePanel} from "@/components/soporte-panel";
export const dynamic="force-dynamic";
export default async function SoportePage(){
  const s=await getSession();
  if(s.state==="anonymous")redirect("/acceso");
  if(s.state!=="authenticated")return <main><p>No pudimos verificar la sesión. Volvé a intentar.</p></main>;
  if(s.profile.rol!=="ADMIN_ADMIN")redirect(roleHome(s.profile.rol));
  if(s.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");
  return <main className="support-page"><a href="/administracion">Volver a administración</a><h1>Asistencia de YG Soluciones</h1><p>La cuenta SOPORTE es opcional. Podés crearla aquí si necesitás asistencia; su acceso queda bloqueado hasta que un administrador del negocio lo autorice. El permiso abarca funciones administrativas de la app, dura como máximo 24 horas y puede revocarse en cualquier momento. No permite controlar esta computadora.</p><SoportePanel/></main>;
}
