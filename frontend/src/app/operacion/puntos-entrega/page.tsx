import {redirect} from "next/navigation";
import {getSession} from "@/lib/identity-server";
import {getOperationContext} from "@/lib/organization-server";
import {getPointAvailability} from "@/lib/point-availability-server";
import {AdminShell} from "@/components/admin-shell";
import {PointAvailabilityWorkspace} from "@/components/point-availability-workspace";
export const dynamic="force-dynamic";
export default async function Page(){
 const session=await getSession();if(session.state==="anonymous")redirect("/acceso");if(session.state!=="authenticated")return <main><p role="alert">No pudimos verificar tu sesión.</p></main>;
 const p=session.profile;if(p.debeCambiarContrasena)redirect("/cuenta/seguridad");if(p.rol==="CLIENTE")redirect("/cliente");if(p.rol==="ADMIN_ADMIN")redirect("/administracion/puntos-entrega");
 const context=await getOperationContext();const allowed=context?.permisos.includes("GESTIONAR_PUNTOS_ENTREGA");const initial=allowed?await getPointAvailability(true):null;
 return <AdminShell title="Puntos de entrega" description="Disponibilidad de hoy e historial de cambios." active="puntos" administration={false} userRole="Empleado" name={`${p.nombre} ${p.apellido}`}>{initial?<PointAvailabilityWorkspace initial={initial} internal/>:<p role="alert">{allowed?"No pudimos consultar los puntos. Volvé a cargar la página.":"Necesitás el permiso Habilitar puntos de entrega para acceder."}</p>}</AdminShell>;
}
