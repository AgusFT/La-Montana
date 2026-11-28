//#region ENCABEZADO · src/components/admin-shell.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/admin-shell.tsx
 * ========================================================================
 * FUNCIÓN
 * Proporciona el marco administrativo compartido: menú lateral, navegación adaptable, identidad
 * del usuario, selector claro/oscuro y cierre de sesión. Carga el progreso persistido para
 * bloquear pantallas prematuras durante la primera instalación.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - AdminIcon({ kind }: { kind: "home" | "catalog" | "people" | "branch" | "shield" })
 *   Componente de interfaz.
 * - [export, async] AdminShell({ title, description, name, children, active="catalogo",
 *   userRole="Administración", administration=true }: { title:string; description:string;
 *   name:string; userRole?:string; administration?:boolean; children:React.ReactNode;
 *   active?:"web"|"dashboard"|"catalogo"|"configuracion"|"puntos"|"operacion"|"sucursales"|"empleados"|"cupos"|"riders"
 *   })
 *   Componente de interfaz.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import {TutorialProvider} from "./tutorial-context";
import {TutorialButton,TutorialPanel} from "./demo-tutorial";
import {NavigationBoundary} from "./navigation-boundary";
import {DemoLockedNav} from "./demo-premium-nav";
import {MountainMark} from "./mountain-brand";
import { LogoutButton } from "@/components/identity-forms";
import "@/app/admin.css";
import {ThemeSwitch} from "./theme-switch";
import {getPreparation} from "@/lib/installation-server";
import {InstallationProvider,InstallationGate,GuidedNavLink} from "./installation-guide";
import "@/app/installation.css";

function AdminIcon({ kind }: { kind: "home" | "catalog" | "people" | "branch" | "shield" }) {
  return <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">{kind==="home"?<path d="m3 11 9-8 9 8M5 9v12h5v-7h4v7h5V9"/>:kind==="catalog"?<><path d="m3 7 9-5 9 5v11l-9 4-9-4V7Zm0 0 9 5 9-5M12 12v10"/></>:kind==="people"?<><circle cx="9" cy="7" r="3"/><path d="M2 21v-4a7 7 0 0 1 14 0v4M17 4a3 3 0 0 1 0 6m2 4c3 1 3 4 3 7"/></>:kind==="branch"?<><path d="M4 22V3h12v19M16 9h5v13M1 22h23M8 7h4m-4 4h4m-4 4h4m-4 4h4"/></>:<><path d="m12 2 8 3v7c0 5-4 8-8 10-4-2-8-5-8-10V5l8-3Z"/><path d="m8.5 11.5 2.5 2.5 4.5-5"/></>}</svg>;
}
export async function AdminShell({ title, description, name, children, active="catalogo", userRole="Administración", administration=true }: { title:string; description:string; name:string; userRole?:string; administration?:boolean; children:React.ReactNode; active?:"web"|"dashboard"|"catalogo"|"configuracion"|"puntos"|"operacion"|"sucursales"|"empleados"|"cupos"|"riders" }) {
  const preparation=administration?await getPreparation():null;
  return <TutorialProvider role={administration?"ADMIN_ADMIN":"EMPLEADO"}><InstallationProvider initial={preparation} enabled={administration}><NavigationBoundary><div className={`admin-layout${active==="configuracion"?" configuration-mode":""}`}>
    <aside className="admin-sidebar"><a href={administration?"/administracion":"/operacion"} className="admin-brand"><MountainMark sun/><span>La Montaña<small>IMPRESIONES</small></span></a><p className="admin-nav-label">{administration?"ADMINISTRACIÓN":"OPERACIÓN"}</p>
      <TutorialButton/>
      <nav aria-label={administration?"Navegación administrativa":"Navegación operativa"}>
        {administration && <>
          <GuidedNavLink area="dashboard" href="/administracion" current={active==="dashboard"}><AdminIcon kind="home"/>Dashboard</GuidedNavLink>
          <GuidedNavLink area="catalogo" href="/administracion/catalogo" current={active==="catalogo"}><AdminIcon kind="catalog"/>Servicios y precios</GuidedNavLink>
          <GuidedNavLink area="configuracion" href="/administracion/configuracion" current={active==="configuracion"}><AdminIcon kind="catalog"/>Configurador</GuidedNavLink>
          <GuidedNavLink area="web" href="/administracion/pagina-web" current={active==="web"}><AdminIcon kind="home"/>Configurador de página web</GuidedNavLink>
          <GuidedNavLink area="sucursales" href="/administracion/sucursales" current={active==="sucursales"}><AdminIcon kind="branch"/>Sucursales</GuidedNavLink>
        </>}
        <GuidedNavLink area="operacion" href={administration?"/administracion/pedidos":"/operacion"} current={active==="operacion"}><AdminIcon kind="branch"/>Operación y cobros</GuidedNavLink>
        {administration && <a href="/administracion/soporte"><AdminIcon kind="people"/>Asistencia de YG Soluciones</a>}
        {administration && <a href="/administracion/cupos" aria-current={active==="cupos"?"page":undefined}><AdminIcon kind="catalog"/>Cupos de la Demo</a>}
        <a href="/manual-de-usuario"><AdminIcon kind="catalog"/>Manual de usuario</a>
        <a href="/cuenta/seguridad"><AdminIcon kind="shield"/>Seguridad de la cuenta</a>
        <DemoLockedNav adminArea={administration ? active : undefined}/>
      </nav>
      <div className="admin-sidebar-pending"><span>CUPS, pagos en línea y reclamos</span><small>En construcción</small></div><ThemeSwitch/><p className="admin-sidebar-version">La Montaña</p>
    </aside>
    <TutorialPanel/>
    <div className="admin-main"><header className="admin-topbar"><span>{administration?"Administración de la imprenta":"Operación de sucursal"}</span><div className="admin-user"><span className="admin-avatar" aria-hidden="true">{name.slice(0,1).toUpperCase()}</span><div><strong>{name}</strong><small>{userRole}</small></div><LogoutButton/></div></header>
      <main className="admin-content"><p className="admin-breadcrumb"><a href={administration?"/administracion":"/operacion"}>{administration?"Administración":"Operación"}</a><span aria-hidden="true">›</span>{title}</p><div className="admin-title"><AdminIcon kind="catalog"/><div><h1>{title}</h1><p>{description}</p></div></div><InstallationGate area={active}>{children}</InstallationGate></main>
    </div>
  </div></NavigationBoundary></InstallationProvider></TutorialProvider>;
}
