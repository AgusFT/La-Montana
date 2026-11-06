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
import {NavigationBoundary} from "./navigation-boundary";
import {MountainMark} from "./mountain-brand";
import {LogoutButton} from "./identity-forms";
import "@/app/client.css";

function ClientNavIcon({kind}:{kind:"home"|"orders"|"new"|"security"}){
 const paths={home:"m3 11 9-8 9 8M5 10v10h5v-6h4v6h5V10",orders:"m3 7 9-4 9 4v10l-9 4-9-4V7Zm0 0 9 4 9-4m-9 4v10",new:"M12 5v14M5 12h14",security:"M7 10V8a5 5 0 0 1 10 0v2M5 10h14v11H5z"};
 return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[kind]}/></svg>;
}

export function ClientShell({name,title,description,active,children}:{name:string;title:string;description:string;active:"home"|"quotes"|"new"|"orders";children:React.ReactNode}){
 const brand=<span className="client-brand"><MountainMark/><span>La Montaña<small>impresiones</small></span></span>;
 return <NavigationBoundary><div className="client-app"><aside className="client-sidebar"><a href="/cliente" className="client-logo">{brand}</a><nav aria-label="Navegación de cliente">
   <span className="client-nav-heading">Mi cuenta</span>
   <a href="/cliente" aria-current={active==="home"?"page":undefined}><ClientNavIcon kind="home"/>Inicio</a>
   <a href="/cliente/pedidos" aria-current={active==="orders"||active==="quotes"?"page":undefined}><ClientNavIcon kind="orders"/>Mis pedidos</a>
   <a href="/cliente/cotizaciones/nueva" className="client-nav-create" aria-current={active==="new"?"page":undefined}><ClientNavIcon kind="new"/>Crear pedido</a>
   <a href="/cuenta/seguridad" className="client-nav-security"><ClientNavIcon kind="security"/>Seguridad de la cuenta</a>
  </nav><p className="client-sidebar-note">Piloto v0.1<br/>Clientes particulares</p></aside><div className="client-main"><header className="client-top"><a href="/cliente" className="client-logo">{brand}</a><div><span>{name}</span><LogoutButton/></div></header><main className={active==="home"?"client-content client-home":"client-content"}>
    <div className="client-title">
      <div><h1>{title}</h1><p>{description}</p></div>
      {active==="home"&&<a className="client-button" href="/cliente/cotizaciones/nueva"><span aria-hidden="true">＋</span> Crear pedido</a>}
    </div>
    {children}
  </main><footer className="client-footer">La Montaña impresiones · Piloto local v0.1</footer></div></div></NavigationBoundary>;
}
