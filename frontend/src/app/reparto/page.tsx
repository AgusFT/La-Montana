import {redirect} from "next/navigation";
import {AdminShell} from "@/components/admin-shell";
import {LogoutButton} from "@/components/identity-forms";
import {RiderDesk} from "@/components/rider-desk";
import {getSession} from "@/lib/identity-server";
import {getOperationContext} from "@/lib/organization-server";
import {sessionHome} from "@/lib/roles";
import "@/app/admin.css";

export const dynamic="force-dynamic";

export default async function RepartoPage(){
  const session=await getSession();
  if(session.state==="anonymous")redirect("/acceso");
  if(session.state!=="authenticated")return <main className="shell"><p role="alert">No pudimos verificar tu sesión. <a href="/reparto">Reintentar</a></p></main>;
  const profile=session.profile;
  const admin=profile.rol==="ADMIN_ADMIN";
  if(profile.debeCambiarContrasena||profile.puntoRetiro||(!admin&&!(profile.rol==="EMPLEADO"&&profile.repartidor)))
    redirect(sessionHome(profile));
  const name=`${profile.nombre} ${profile.apellido}`;
  const desk=<RiderDesk enabled={!!profile.repartidor} admin={admin}/>;
  if(admin)return <AdminShell title="Mis repartos" description="Pedidos asignados a tu cuenta y próximos destinos." name={name} active="reparto">{desk}</AdminShell>;
  const context=await getOperationContext();
  return <div className="standalone-account-layout">
  <aside className="standalone-account-art account-sidebar-art" aria-hidden="true"/>
  <div className="admin-layout"><div className="admin-main">
    <header className="admin-topbar"><strong>La Montaña · Reparto</strong><div className="admin-user"><strong>{name}</strong><LogoutButton/></div></header>
    <main className="admin-content">
      <h1>Mis repartos</h1>
      <nav className="client-actions" aria-label="Mi cuenta">
        {!!context?.permisos.length&&<a className="admin-button secondary" href="/operacion">Mi operación</a>}
        <a className="admin-button secondary" href="/cuenta/seguridad">Seguridad de la cuenta</a>
      </nav>
      {desk}
    </main>
  </div></div></div>;
}
