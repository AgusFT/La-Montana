//#region ENCABEZADO · src/components/client-page-frame.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/client-page-frame.tsx
 * ========================================================================
 * FUNCIÓN
 * Comprueba sesión, rol y cambio obligatorio de contraseña antes de renderizar el contenido dentro
 * del marco de navegación del cliente.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, async] ClientPageFrame({title,description,active,children}:
 *   {title:string;description:string;active:"home"|"quotes"|"new"|"orders";children:React.ReactNode})
 *   Componente de interfaz.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import {redirect} from "next/navigation";
import {getSession} from "@/lib/identity-server";
import {roleHome} from "@/lib/roles";
import {IdentityShell} from "./identity-shell";
import {ClientShell} from "./client-shell";
export async function ClientPageFrame({title,description,active,children}:{title:string;description:string;active:"home"|"quotes"|"new"|"orders";children:React.ReactNode}){
 const session=await getSession();
 if(session.state==="anonymous")redirect("/acceso");
 if(session.state==="unavailable")return <IdentityShell title="Mi cuenta" description="Verificación de tu sesión."><p role="alert" className="form-message error-message">No pudimos verificar la sesión. Volvé a intentar cuando el sistema esté disponible.</p><a href="/cliente">Volver a comprobar</a></IdentityShell>;
 if(session.state!=="authenticated")return null;
 if(session.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");
 if(session.profile.rol!=="CLIENTE")redirect(roleHome(session.profile.rol));
 return <ClientShell name={`${session.profile.nombre} ${session.profile.apellido}`} title={active==="home"?`Hola, ${session.profile.nombre}`:title} description={description} active={active}>{children}</ClientShell>;
}
