//#region ENCABEZADO · src/app/administracion/caja/page.tsx
/*
 * FUNCIÓN: Expone al administrador la caja completa de la imprenta y permite cambiar de
 * sucursal sin abandonar el espacio administrativo.
 */
//#endregion

import {AdminShell} from "@/components/admin-shell";
import {PaymentQueue} from "@/components/payment-queue";
import {IdentityShell} from "@/components/identity-shell";
import {getSession} from "@/lib/identity-server";
import {getOperationContext} from "@/lib/organization-server";
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
 const context=await getOperationContext(),requested=(await searchParams).sucursal;
 const branches=context?.sucursales??[],selected=branches.find(branch=>branch.codigoPublico===requested)??branches[0];
 return <AdminShell title="Caja" description="Cobros, transferencias, aplicaciones y devoluciones de toda la imprenta." active="caja" name={`${session.profile.nombre} ${session.profile.apellido}`}>
   <p className="admin-info">Como administrador tenés acceso financiero completo. Elegí una sucursal para consultar sus operaciones y abrí cada cotización para registrar, verificar, aplicar o devolver dinero.</p>
   {!context?<p className="admin-warning" role="alert">No pudimos consultar las sucursales y los permisos financieros. Volvé a cargar la página.</p>:branches.length===0?<p className="admin-empty">No hay sucursales activas para consultar.</p>:<>
    <nav className="cash-branch-tabs" aria-label="Caja por sucursal">{branches.map(branch=><a key={branch.codigoPublico} href={`/administracion/caja?sucursal=${branch.codigoPublico}`} aria-current={selected?.codigoPublico===branch.codigoPublico?"page":undefined}><strong>{branch.nombre}</strong><small>{branch.casaMatrizVigente?"Casa matriz · ":""}{branch.localidad}</small></a>)}</nav>
    {selected&&<div className="cash-selected-branch"><header><div><span>CAJA SELECCIONADA</span><h2>{selected.nombre}</h2><p>{selected.calle} {selected.numero}, {selected.localidad}</p></div><a className="admin-button secondary" href={`/operacion/sucursales/${selected.codigoPublico}`}>Ver operación completa</a></header><PaymentQueue branch={selected.codigoPublico}/></div>}
   </>}
  </AdminShell>;
}
