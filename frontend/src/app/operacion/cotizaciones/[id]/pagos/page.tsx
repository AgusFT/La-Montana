//#region ENCABEZADO · src/app/operacion/cotizaciones/[id]/pagos/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/operacion/cotizaciones/[id]/pagos/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Verifica la sesión operativa y el identificador de cotización para mostrar su gestión de pagos
 * con los permisos del usuario.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] QuotePaymentsPage({params}: {params:Promise<{id:string}>})
 *   Componente de página exportado por la ruta de Next.js.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - dynamic [const, exportado].
 * ========================================================================
 */
//#endregion

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
 return <AdminShell title="Pagos de la cotización" description="Recepción, aplicación y devolución de dinero por personal autorizado." name={`${session.profile.nombre} ${session.profile.apellido}`} active="operacion" administration={session.profile.rol === "ADMIN_ADMIN"} userRole={session.profile.rol === "EMPLEADO" ? "Empleado" : "Propietario"}>{content}</AdminShell>;
}
