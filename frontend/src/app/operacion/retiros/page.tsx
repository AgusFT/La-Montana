import {redirect} from 'next/navigation';
import {AdminShell} from '@/components/admin-shell';
import {IdentityShell} from '@/components/identity-shell';
import {BranchPickupDesk} from '@/components/branch-pickup-desk';
import {getSession} from '@/lib/identity-server';
import {getOperationContext} from '@/lib/organization-server';
import {sessionHome} from '@/lib/roles';

export default async function BranchPickupPage({searchParams}:{searchParams:Promise<{sucursal?:string}>}) {
  const session=await getSession();
  if(session.state==='anonymous')redirect('/acceso');
  if(session.state!=='authenticated')return <IdentityShell title="Retiros en sucursal" description="Verificación de acceso">
    <p role="alert">No pudimos comprobar tu sesión.</p><a href="/operacion/retiros">Volver a intentar</a>
  </IdentityShell>;
  const profile=session.profile;
  if(profile.debeCambiarContrasena)redirect('/cuenta/seguridad');
  if(profile.rol==='CLIENTE'||profile.puntoRetiro)redirect(sessionHome(profile));
  const admin=profile.rol==='ADMIN_ADMIN';
  const context=await getOperationContext();
  const {sucursal}=await searchParams;
  const branches=context?.sucursales??[];
  const selected=branches.find(branch=>branch.codigoPublico===sucursal)??branches.find(branch=>branch.casaMatrizVigente)??branches[0];
  return <AdminShell name={profile.nombre+' '+profile.apellido} administration={admin}
    userRole={admin?'Propietario':'Empleado'} active="retiros" title="Retiros en sucursal"
    description="Buscá el pedido, registrá el cobro en efectivo si corresponde y entregalo desde el mostrador.">
    {!context?<p className="admin-warning" role="alert">No pudimos consultar tus sucursales.</p>
      :!context.permisos.includes('GESTIONAR_ENTREGAS')?<p className="admin-warning" role="alert">Necesitás el permiso Gestionar entregas.</p>
      :selected?<BranchPickupDesk branches={branches.map(branch=>({id:branch.codigoPublico,name:branch.nombre}))} initialBranch={selected.codigoPublico}/>
      :<p className="admin-note">Tu cuenta no tiene sucursales activas asignadas.</p>}
  </AdminShell>;
}
