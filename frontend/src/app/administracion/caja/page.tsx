//#region ENCABEZADO · src/app/administracion/caja/page.tsx
/*
 * FUNCIÓN: Expone al administrador la caja completa de la imprenta y permite cambiar de
 * sucursal sin abandonar el espacio administrativo.
 */
//#endregion

import {AdminShell} from "@/components/admin-shell";
import {PaymentQueue} from "@/components/payment-queue";
import {BranchCash} from "@/components/branch-cash";
import {IdentityShell} from "@/components/identity-shell";
import {getSession} from "@/lib/identity-server";
import {getBranches} from "@/lib/organization-server";
import {roleHome} from "@/lib/roles";
import {redirect} from "next/navigation";

export const dynamic="force-dynamic";

export default async function AdminCashPage({searchParams}:{searchParams:Promise<{sucursal?:string}>}){
 const session=await getSession();
 if(session.state==="anonymous")redirect("/acceso");
 if(session.state==="unavailable")return <IdentityShell title="Caja" description="Verificación de tu sesión."><p role="alert">No pudimos verificar la sesión. <a href="/administracion/caja">Volver a intentar</a></p></IdentityShell>;
 if(session.state!=="authenticated")return null;
 if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");
 if(session.profile.rol!=="ADMIN_ADMIN")redirect(roleHome(session.profile.rol));
 const branches=await getBranches(),requested=(await searchParams).sucursal;
 const selected=branches?.find(branch=>branch.codigoPublico===requested)??branches?.find(branch=>branch.estado==="ACTIVA")??branches?.[0];
 return <AdminShell title="Caja" description="Movimientos, pagos, rendiciones e historial de arqueos por sucursal." active="caja" name={`${session.profile.nombre} ${session.profile.apellido}`}>
   {!branches?<p className="admin-warning" role="alert">No pudimos consultar las sucursales. Volvé a cargar la página.</p>:branches.length===0?<p className="admin-empty">No hay sucursales para consultar.</p>:<>
    <nav className="cash-branch-tabs" aria-label="Caja por sucursal">{branches.map(branch=><a key={branch.codigoPublico} href={`/administracion/caja?sucursal=${branch.codigoPublico}`} aria-current={selected?.codigoPublico===branch.codigoPublico?"page":undefined}><strong>{branch.nombre}</strong><small>{branch.estado==="DESACTIVADA"?"Desactivada · ":branch.casaMatrizVigente?"Casa matriz · ":""}{branch.localidad}</small></a>)}</nav>
    {selected&&<div className="cash-selected-branch"><header><div><span>CAJA SELECCIONADA</span><h2>{selected.nombre}</h2><p>{selected.calle} {selected.numero}, {selected.localidad}</p></div>{selected.estado==="ACTIVA"&&<a className="admin-button secondary" href={`/operacion/sucursales/${selected.codigoPublico}`}>Ver operación completa</a>}</header>
     <BranchCash key={selected.codigoPublico} branch={selected.codigoPublico} admin/>
     {selected.estado==="ACTIVA"&&<details className="workspace-panel"><summary>Gestionar cobros, transferencias y devoluciones de pedidos</summary><PaymentQueue branch={selected.codigoPublico}/></details>}
    </div>}
   </>}
  </AdminShell>;
}
