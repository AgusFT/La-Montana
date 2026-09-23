import { getPointAvailability } from "@/lib/point-availability-server";
import { redirect } from "next/navigation";
import { roleHome } from "@/lib/roles";
import { IdentityShell } from "@/components/identity-shell";
import {AdminShell} from "@/components/admin-shell";
import {AdminDashboard} from "@/components/admin-dashboard";
import {getOperationContext} from "@/lib/organization-server";
import { getSession } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function AdministrationPage() {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state === "unavailable") return <IdentityShell title="Administración" description="Verificación de tu sesión."><p className="form-message error-message" role="alert">No pudimos verificar la sesión. Intentá nuevamente cuando el sistema esté disponible.</p><a className="refresh" href="/administracion">Volver a comprobar</a></IdentityShell>;
  if (session.state !== "authenticated") return null;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol !== "ADMIN_ADMIN") redirect(roleHome(session.profile.rol));
  const owner = session.profile;
  const [points,context]=await Promise.all([getPointAvailability(),getOperationContext()]);
  return <AdminShell title="Dashboard administrativo" description={`Bienvenido, ${owner.nombre}. Consultá el trabajo de tus sucursales.`} name={`${owner.nombre} ${owner.apellido}`} active="dashboard">
    <div className="dashboard-layout"><div>{context?<AdminDashboard branches={context.sucursales}/>:<p className="admin-error" role="alert">No pudimos consultar las sucursales. <a href="/administracion">Volver a intentar</a></p>}</div>
    <aside className="dashboard-aside"><section className="dashboard-side-card"><h2>Disponibilidad de puntos</h2>{!points?<p role="alert">No pudimos consultar la disponibilidad.</p>:points.deshabilitados>0?<p className="dashboard-notice" role="status">{points.deshabilitados} {points.deshabilitados===1?"punto de entrega deshabilitado":"puntos de entrega deshabilitados"}.</p>:<p>Sin puntos deshabilitados en la configuración consultada.</p>}<a href="/administracion/puntos-entrega">Revisar disponibilidad →</a></section>
    <section className="dashboard-side-card"><h2>Acciones rápidas</h2><nav aria-label="Acciones rápidas"><a href="/operacion">Pedidos, producción y entregas</a><a href="/administracion/catalogo">Servicios y precios</a><a href="/administracion/configuracion">Configurar la imprenta</a><a href="/administracion/configuracion/historial">Historial de configuraciones</a><a href="/administracion/sucursales">Administrar sucursales</a><a href="/administracion/empleados">Administrar empleados</a></nav></section>
    <section className="dashboard-side-card"><h2>Funciones del piloto</h2><p>La producción se registra manualmente y los pagos se verifican por personal autorizado.</p><p><span className="configuration-badge">En construcción</span></p><p>CUPS, pagos en línea, reclamos, gestión de clientes y reportes de facturación.</p></section></aside></div>
  </AdminShell>;
}
