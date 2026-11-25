import {redirect} from 'next/navigation';
import {getSession} from '@/lib/identity-server';
import {roleHome} from '@/lib/roles';
import {IdentityShell} from '@/components/identity-shell';
import {AdminShell} from '@/components/admin-shell';
import {OrderListView} from '@/components/order-list';
import {orderStates} from '@/lib/order-types';

export const dynamic='force-dynamic';
/** Bandeja administrativa global. El destino es información logística, no alcance productivo. */
export default async function GlobalOrdersPage({searchParams}:{searchParams:Promise<{estado?:string}>}){
 const session=await getSession();
 if(session.state==='anonymous')redirect('/acceso');
 if(session.state==='unavailable')return <IdentityShell title="Pedidos de la imprenta" description="Verificación de tu sesión."><p role="alert">No pudimos verificar la sesión. Volvé a intentar.</p><a href="/administracion/pedidos">Volver a comprobar</a></IdentityShell>;
 if(session.state!=='authenticated')return null;
 if(session.profile.debeCambiarContrasena)redirect('/cuenta/seguridad');
 if(session.profile.rol!=='ADMIN_ADMIN')redirect(roleHome(session.profile.rol));
 const {estado}=await searchParams;const initialState=typeof estado==='string'&&orderStates.includes(estado)?estado:'';
 return <AdminShell name={`${session.profile.nombre} ${session.profile.apellido}`} active="operacion" title="Pedidos de la imprenta" description="Revisión, producción y entregas de todos los pedidos, independientemente de su destino."><p className="admin-info">La imprenta es una sola. El destino indica dónde recibir el pedido, no dónde hay que producirlo. Abrí cada pedido para gestionar sus archivos, producción, pagos y entrega con los permisos vigentes.</p><OrderListView globalAdmin initialState={initialState}/><a className="secondary-link" href="/administracion">Volver al dashboard global</a></AdminShell>;
}
