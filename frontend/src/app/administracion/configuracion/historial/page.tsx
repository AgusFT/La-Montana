//#region ENCABEZADO · src/app/administracion/configuracion/historial/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/administracion/configuracion/historial/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Protege y presenta la consulta del historial de configuraciones operativas dentro del panel
 * administrativo.
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
import {ConfigurationHistoryPage} from "@/components/configuration-history-page";
import {getSession} from "@/lib/identity-server";
import {roleHome} from "@/lib/roles";
export const dynamic="force-dynamic";
export default async function Page(){const s=await getSession();if(s.state==="anonymous")redirect("/acceso");if(s.state!=="authenticated")return <main className="shell"><h1>Historial de configuración</h1><p role="alert">No pudimos verificar tu sesión.</p><a href="/administracion/configuracion/historial">Volver a intentar</a></main>;if(s.profile.debeCambiarContrasena)redirect("/cuenta/seguridad");if(s.profile.rol!=="ADMIN_ADMIN")redirect(roleHome(s.profile.rol));return <AdminShell title="Motor de configuración" description="Historial de versiones de la imprenta" name={`${s.profile.nombre} ${s.profile.apellido}`} userRole="Administración" active="configuracion"><ConfigurationHistoryPage/></AdminShell>;}
