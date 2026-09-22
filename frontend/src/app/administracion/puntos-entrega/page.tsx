import {redirect} from "next/navigation";
import {AdminShell} from "@/components/admin-shell";
import {PointAvailabilityWorkspace} from "@/components/point-availability-workspace";
import {getSession} from "@/lib/identity-server";
import {getPointAvailability} from "@/lib/point-availability-server";
import {roleHome} from "@/lib/roles";
export const dynamic="force-dynamic";
export default async function PointsPage(){
  const session=await getSession();if(session.state==="anonymous")redirect("/acceso");
  if(session.state==="unavailable")return <main className="shell"><h1>Disponibilidad de puntos</h1><p role="alert">No pudimos verificar tu sesión.</p><a href="/administracion/puntos-entrega">Volver a intentar</a></main>;
  if(session.state!=="authenticated")return null;
  if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(session.profile.rol!=="ADMIN_ADMIN")redirect(roleHome(session.profile.rol));
  const initial=await getPointAvailability();return <AdminShell title="Disponibilidad de puntos" description="Habilitación temporal de los puntos de entrega" active="puntos" name={`${session.profile.nombre} ${session.profile.apellido}`}>
    {initial?<PointAvailabilityWorkspace initial={initial}/>:<section className="admin-card"><p role="alert">No pudimos consultar la disponibilidad. No se modificó ningún punto.</p><a className="admin-button" href="/administracion/puntos-entrega">Volver a intentar</a></section>}
  </AdminShell>;
}
