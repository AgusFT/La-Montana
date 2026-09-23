//#region ENCABEZADO · src/app/operacion/pedidos/[id]/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/operacion/pedidos/[id]/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Verifica sesión, rol e identificador y presenta el detalle operativo del pedido con los permisos
 * disponibles para el personal.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] OrderPage({params}: {params:Promise<{id:string}>})
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
import {IdentityShell} from "@/components/identity-shell";
import {AdminShell} from "@/components/admin-shell";
import {OrderDetail} from "@/components/order-detail";
import {getSession} from "@/lib/identity-server";
import {getOperationContext} from "@/lib/organization-server";
import {uuidPattern} from "@/lib/organization-types";
export const dynamic="force-dynamic";
export default async function OrderPage({params}:{params:Promise<{id:string}>}){
 const session=await getSession();if(session.state==="anonymous")redirect("/acceso");if(session.state==="unavailable")return <IdentityShell title="Pedido" description="Verificación de tu sesión."><p role="alert">No pudimos verificar la sesión.</p><a href="/operacion">Volver a operación</a></IdentityShell>;if(session.state!=="authenticated")return null;if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(session.profile.rol==="CLIENTE")redirect("/cliente");const{id}=await params;if(!uuidPattern.test(id))notFound();const context=await getOperationContext();const financial=!!context?.permisos.some(p=>["ACREDITAR_PAGO","REGISTRAR_COBRO","REGISTRAR_DEVOLUCION"].includes(p));return <AdminShell title="Detalle del pedido" description="Revisión, producción, entrega e historia del trabajo." name={`${session.profile.nombre} ${session.profile.apellido}`} active="operacion" administration={session.profile.rol === "ADMIN_ADMIN"} userRole={session.profile.rol === "EMPLEADO" ? "Empleado" : "Propietario"}><OrderDetail id={id} internal financial={financial}/></AdminShell>;
}
