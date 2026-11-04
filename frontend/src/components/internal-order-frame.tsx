import {redirect} from 'next/navigation';
import {getSession} from '@/lib/identity-server';
import {getOperationContext} from '@/lib/organization-server';
import {AdminShell} from './admin-shell';
import {IdentityShell} from './identity-shell';
import '@/app/client.css';

export async function InternalOrderFrame({title,children}:{title:string;children:React.ReactNode}){
 const session=await getSession();
 if(session.state==='anonymous')redirect('/acceso');
 if(session.state!=='authenticated')return <IdentityShell title={title} description="Verificación de tu sesión."><p role="alert">No pudimos verificar la sesión.</p></IdentityShell>;
 const profile=session.profile;
 if(profile.debeCambiarContrasena)redirect('/cuenta/seguridad');
 if(profile.rol==='CLIENTE')redirect('/cliente');
 if(profile.puntoRetiro)redirect('/punto-retiro');
 const admin=profile.rol==='ADMIN_ADMIN';
 const context=admin?null:await getOperationContext();
 const allowed=admin||context?.permisos.includes('CREAR_PEDIDOS');
 return <AdminShell active="operacion" administration={admin} userRole={admin?'Propietario':'Empleado'} name={profile.nombre+' '+profile.apellido}
  title={title} description="Prepará un trabajo y confirmalo en nombre de un cliente de la imprenta.">
  {allowed?children:<p className="client-warning" role="alert">{context?'Necesitás el permiso Crear pedidos para clientes.':'No pudimos verificar tu acceso operativo.'}</p>}
 </AdminShell>;
}
