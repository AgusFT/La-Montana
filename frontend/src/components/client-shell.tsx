//#region ENCABEZADO · src/components/client-shell.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/client-shell.tsx
 * ========================================================================
 * FUNCIÓN
 * Proporciona la estructura visual compartida del área del cliente, su navegación y las acciones
 * de cuenta.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] ClientShell({name,title,description,active,children}:
 *   {name:string;title:string;description:string;active:"home"|"quotes"|"new"|"orders";children:React.ReactNode})
 *   Componente de interfaz.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import "@/app/client-experience.css";
import {TutorialProvider} from "./tutorial-context";
import {TutorialButton,TutorialPanel} from "./demo-tutorial";
import {NavigationBoundary} from "./navigation-boundary";
import {DemoSidebarMore} from "./demo-premium-nav";
import {MountainMark} from "./mountain-brand";
import {LogoutButton} from "./identity-forms";
import "@/app/client.css";

export function ClientShell({name,title,description,active,children}:{name:string;title:string;description:string;active:"home"|"quotes"|"new"|"orders";children:React.ReactNode}){
 const brand=<span className="client-brand"><MountainMark/><span>La Montaña<small>impresiones</small></span></span>;
 return <TutorialProvider role="CLIENTE"><NavigationBoundary><div className="client-app"><aside className="client-sidebar"><a href="/cliente" className="client-logo">{brand}</a><TutorialButton/><nav aria-label="Navegación de cliente"><a href="/cliente" aria-current={active==="home"?"page":undefined}>⌂ Inicio</a><a href="/cliente/cotizaciones" aria-current={active==="quotes"?"page":undefined}>▤ Mis cotizaciones</a><a href="/cliente/cotizaciones/nueva" aria-current={active==="new"?"page":undefined}>⊕ Crear pedido</a><a href="/cliente/pedidos" aria-current={active==="orders"?"page":undefined}>▤ Mis pedidos</a><span>Reclamos <small>En construcción</small></span><a href="/manual-de-usuario">Manual de usuario</a><a href="/cuenta/seguridad">Seguridad de la cuenta</a><DemoSidebarMore/></nav><p className="client-sidebar-note">Piloto v0.1<br/>Clientes particulares</p></aside><TutorialPanel/><div className="client-main"><header className="client-top"><a href="/cliente" className="client-logo">{brand}</a><div><span>{name}</span><LogoutButton/></div></header><main className="client-content"><div className="client-title"><h1>{title}</h1><p>{description}</p></div>{children}</main><footer className="client-footer">La Montaña impresiones · Piloto local v0.1</footer></div></div></NavigationBoundary></TutorialProvider>;
}
