import {redirect} from 'next/navigation';
import {getSession} from '@/lib/identity-server';
import {sessionHome} from '@/lib/roles';
import {IdentityShell} from '@/components/identity-shell';
import {NavigationBoundary} from '@/components/navigation-boundary';
import {PointDesk} from '@/components/point-desk';
export const dynamic='force-dynamic';
export default async function PointDeskPage(){
 const s=await getSession();if(s.state==='anonymous')redirect('/acceso');if(s.state!=='authenticated')return <IdentityShell title="Punto de retiro" description="Verificación de acceso"><p>No pudimos comprobar tu sesión. Volvé a intentar.</p><a href="/punto-retiro">Actualizar</a></IdentityShell>;
 if(!s.profile.puntoRetiro)redirect(sessionHome(s.profile));if(s.profile.debeCambiarContrasena)redirect('/cuenta/seguridad');
 return <NavigationBoundary><PointDesk/></NavigationBoundary>;
}
