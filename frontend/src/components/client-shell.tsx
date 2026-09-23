import {MountainMark} from "./mountain-brand";
import {LogoutButton} from "./identity-forms";
import "@/app/client.css";

export function ClientShell({name,title,description,active,children}:{name:string;title:string;description:string;active:"home"|"quotes"|"new"|"orders";children:React.ReactNode}){
 const brand=<span className="client-brand"><MountainMark/><span>La Montaña<small>impresiones</small></span></span>;
 return <div className="client-app"><aside className="client-sidebar"><a href="/cliente" className="client-logo">{brand}</a><nav aria-label="Navegación de cliente"><a href="/cliente" aria-current={active==="home"?"page":undefined}>⌂ Inicio</a><a href="/cliente/cotizaciones" aria-current={active==="quotes"?"page":undefined}>▤ Mis cotizaciones</a><a href="/cliente/cotizaciones/nueva" aria-current={active==="new"?"page":undefined}>⊕ Preparar pedido</a><a href="/cliente/pedidos" aria-current={active==="orders"?"page":undefined}>▤ Mis pedidos</a><span>Reclamos <small>En construcción</small></span><a href="/cuenta/seguridad">Seguridad de la cuenta</a></nav><p className="client-sidebar-note">Piloto v0.1<br/>Clientes particulares</p></aside><div className="client-main"><header className="client-top"><a href="/cliente" className="client-logo">{brand}</a><div><span>{name}</span><LogoutButton/></div></header><main className="client-content"><div className="client-title"><h1>{title}</h1><p>{description}</p></div>{children}</main><footer className="client-footer">La Montaña impresiones · Piloto local v0.1</footer></div></div>;
}
