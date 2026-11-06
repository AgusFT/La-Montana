import {redirect} from "next/navigation";
import {AdminShell} from "@/components/admin-shell";
import {BranchCash} from "@/components/branch-cash";
import {IdentityShell} from "@/components/identity-shell";
import {getSession} from "@/lib/identity-server";
import {getOperationContext} from "@/lib/organization-server";
import {sessionHome} from "@/lib/roles";

export const dynamic="force-dynamic";

export default async function OperationCashPage() {
  const session=await getSession();
  if(session.state==="anonymous")redirect("/acceso");
  if(session.state!=="authenticated")return <IdentityShell title="Caja" description="Verificación de acceso">
    <p role="alert">No pudimos verificar tu sesión. <a href="/operacion/caja">Volver a intentar</a></p>
  </IdentityShell>;
  const {profile}=session;
  if(profile.debeCambiarContrasena)redirect("/cuenta/seguridad");
  if(profile.rol==="ADMIN_ADMIN")redirect("/administracion/caja");
  if(profile.rol!=="EMPLEADO"||profile.puntoRetiro)redirect(sessionHome(profile));
  const context=await getOperationContext();
  const branch=context?.sucursales.find(s=>s.codigoPublico===context.cajaSucursal);
  return <AdminShell title={branch?`Caja · ${branch.nombre}`:"Caja"} description="Efectivo acumulado, movimientos del día y arqueos de tu sucursal."
    administration={false} userRole={branch?"Encargado de sucursal":"Empleado"} active="caja" name={`${profile.nombre} ${profile.apellido}`}>
    {branch?<BranchCash branch={branch.codigoPublico}/>:<p className="admin-warning" role="alert">
      {context?"Caja está disponible para el encargado de la sucursal.":"No pudimos consultar tu sucursal. Volvé a cargar la página."}
    </p>}
  </AdminShell>;
}
