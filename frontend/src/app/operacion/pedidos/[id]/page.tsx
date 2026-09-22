import {notFound,redirect} from "next/navigation";
import {IdentityShell} from "@/components/identity-shell";
import {AdminShell} from "@/components/admin-shell";
import {OrderDetail} from "@/components/order-detail";
import {getSession} from "@/lib/identity-server";
import {getOperationContext} from "@/lib/organization-server";
import {uuidPattern} from "@/lib/organization-types";
export const dynamic="force-dynamic";
export default async function OrderPage({params}:{params:Promise<{id:string}>}){
 const session=await getSession();if(session.state==="anonymous")redirect("/acceso");if(session.state==="unavailable")return <IdentityShell title="Pedido" description="Verificación de tu sesión."><p role="alert">No pudimos verificar la sesión.</p><a href="/operacion">Volver a operación</a></IdentityShell>;if(session.state!=="authenticated")return null;if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(session.profile.rol==="CLIENTE")redirect("/cliente");const{id}=await params;if(!uuidPattern.test(id))notFound();const context=await getOperationContext();const financial=!!context?.permisos.some(p=>["ACREDITAR_PAGO","REGISTRAR_COBRO","REGISTRAR_DEVOLUCION"].includes(p));if(session.profile.rol==="ADMIN_ADMIN")return <AdminShell title="Detalle del pedido" description="Revisión, historial y reserva del trabajo." name={`${session.profile.nombre} ${session.profile.apellido}`} active="operacion"><OrderDetail id={id} internal financial={financial}/></AdminShell>;return <IdentityShell title="Detalle del pedido" description="Archivos y reserva del trabajo asignado a la sucursal."><OrderDetail id={id} internal financial={financial}/></IdentityShell>;
}
