import {notFound,redirect} from "next/navigation";
import {getSession} from "@/lib/identity-server";
import {uuidPattern} from "@/lib/organization-types";
import {IdentityShell} from "@/components/identity-shell";
import {AdminShell} from "@/components/admin-shell";
import {PaymentPanel} from "@/components/payment-panel";
import "@/app/client.css";
export const dynamic="force-dynamic";
export default async function QuotePaymentsPage({params}:{params:Promise<{id:string}>}){
 const session=await getSession();if(session.state==="anonymous")redirect("/acceso");
 if(session.state!=="authenticated")return <IdentityShell title="Pagos" description="Verificación de acceso."><p role="alert">No pudimos verificar la sesión. Volvé a intentar.</p></IdentityShell>;
 if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(session.profile.rol==="CLIENTE")redirect("/cliente");
 const{id}=await params;if(!uuidPattern.test(id))notFound();
 const content=<><PaymentPanel quote={id} internal/><a href="/operacion">Volver a mis sucursales</a></>;
 if(session.profile.rol==="ADMIN_ADMIN")return <AdminShell title="Pagos de la cotización" description="Recepción, aplicación y devolución de dinero por personal autorizado." name={`${session.profile.nombre} ${session.profile.apellido}`} active="operacion">{content}</AdminShell>;
 return <IdentityShell title="Pagos de la cotización" description="Recepción, aplicación y devolución de dinero por personal autorizado.">{content}</IdentityShell>;
}
