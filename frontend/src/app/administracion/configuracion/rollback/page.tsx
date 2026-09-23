//#region ENCABEZADO · src/app/administracion/configuracion/rollback/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/administracion/configuracion/rollback/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Protege y presenta el flujo de reversión manual de la configuración operativa para el
 * propietario.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] Page()
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
import {ConfigurationRollback} from "@/components/configuration-rollback";
import {getSession} from "@/lib/identity-server";
import {roleHome} from "@/lib/roles";
export const dynamic="force-dynamic";
export default async function Page(){const s=await getSession();if(s.state==="anonymous")redirect("/acceso");if(s.state!=="authenticated")return <main className="shell"><h1>Reversión de configuración</h1><p role="alert">No pudimos verificar tu sesión.</p><a href="/administracion/configuracion/rollback">Volver a intentar</a></main>;if(s.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(s.profile.rol!=="ADMIN_ADMIN")redirect(roleHome(s.profile.rol));return <AdminShell title="Motor de configuración" description="Reversión manual de la configuración operativa" name={`${s.profile.nombre} ${s.profile.apellido}`} active="configuracion"><ConfigurationRollback/></AdminShell>;}
