//#region ENCABEZADO · src/app/administracion/puntos-entrega/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/administracion/puntos-entrega/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Comprueba el acceso del propietario y presenta el panel de disponibilidad temporal de los puntos
 * de entrega.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] PointsPage()
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

import {redirect} from "next/navigation";
import {AdminShell} from "@/components/admin-shell";
import {PointAccounts} from "@/components/point-accounts";
import {PointNetworkWorkspace} from "@/components/point-network-workspace";
import {getPointNetwork} from "@/lib/point-network-server";
import {getSession} from "@/lib/identity-server";
import {getPointAvailability} from "@/lib/point-availability-server";
import {roleHome} from "@/lib/roles";
export const dynamic="force-dynamic";
export default async function PointsPage({searchParams}:{searchParams:Promise<{sucursal?:string}>}){
  const session=await getSession();if(session.state==="anonymous")redirect("/acceso");
  if(session.state==="unavailable")return <main className="shell"><h1>Disponibilidad de puntos</h1><p role="alert">No pudimos verificar tu sesión.</p><a href="/administracion/puntos-entrega">Volver a intentar</a></main>;
  if(session.state!=="authenticated")return null;
  if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(session.profile.rol!=="ADMIN_ADMIN")redirect(roleHome(session.profile.rol));
  const [initial,network,params]=await Promise.all([getPointAvailability(),getPointNetwork(),searchParams]);return <AdminShell title="Puntos de entrega" description="Alta de puntos, sucursales de origen, horarios, disponibilidad y cuentas responsables" active="puntos" name={`${session.profile.nombre} ${session.profile.apellido}`}>
    <PointNetworkWorkspace initial={network} initialAvailability={initial} initialBranch={params.sucursal??''}/>
    <PointAccounts/>
  </AdminShell>;
}
