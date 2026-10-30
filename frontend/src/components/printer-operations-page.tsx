import {redirect} from "next/navigation";
import {getSession} from "@/lib/identity-server";
import {getOperationContext} from "@/lib/organization-server";
import {AdminShell} from "./admin-shell";
import {PrinterOperations} from "./printer-operations";
export async function PrinterOperationsPage({administration}:{administration:boolean}){
 const session=await getSession();if(session.state==="anonymous")redirect("/acceso");if(session.state!=="authenticated")return <main><h1>Impresoras y papel</h1><p role="alert">No pudimos verificar tu sesión. Volvé a cargar la página.</p></main>;
 const profile=session.profile;if(profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(profile.rol==="CLIENTE")redirect("/cliente");if(administration&&profile.rol!=="ADMIN_ADMIN")redirect("/operacion/impresoras");
 const context=await getOperationContext();const allowed=context?.permisos.some(p=>p==="GESTIONAR_IMPRESORAS"||p==="GESTIONAR_PRODUCCION");
 return <AdminShell title="Impresoras y papel" description="Estado de los equipos, recargas e historial de la imprenta." active="impresoras" name={`${profile.nombre} ${profile.apellido}`} administration={profile.rol==="ADMIN_ADMIN"} userRole={profile.rol==="EMPLEADO"?"Empleado":"Propietario"}>{allowed?<PrinterOperations/>:<p role="alert">{context?"Necesitás permiso de producción o de gestión de impresoras para consultar esta pantalla.":"No pudimos consultar tus permisos. Volvé a cargar la página."}</p>}</AdminShell>;
}
