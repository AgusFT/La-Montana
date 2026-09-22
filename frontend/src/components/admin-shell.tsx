import { LogoutButton } from "@/components/identity-forms";
import "@/app/admin.css";

function AdminIcon({ kind }: { kind: "home" | "catalog" | "people" | "branch" | "lock" }) {
  return <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">{kind==="home"?<path d="m3 11 9-8 9 8M5 9v12h5v-7h4v7h5V9"/>:kind==="catalog"?<><path d="m3 7 9-5 9 5v11l-9 4-9-4V7Zm0 0 9 5 9-5M12 12v10"/></>:kind==="people"?<><circle cx="9" cy="7" r="3"/><path d="M2 21v-4a7 7 0 0 1 14 0v4M17 4a3 3 0 0 1 0 6m2 4c3 1 3 4 3 7"/></>:kind==="branch"?<><path d="M4 22V3h12v19M16 9h5v13M1 22h23M8 7h4m-4 4h4m-4 4h4m-4 4h4"/></>:<><rect x="5" y="10" width="14" height="12" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4M12 15v3"/></>}</svg>;
}
export function AdminShell({ title, description, name, children }: { title:string; description:string; name:string; children:React.ReactNode }) {
  return <div className="admin-layout">
    <aside className="admin-sidebar"><a href="/administracion" className="admin-brand"><span aria-hidden="true">△</span><span>La Montaña<small>IMPRESIONES</small></span></a><p className="admin-nav-label">ADMINISTRACIÓN</p>
      <nav aria-label="Navegación administrativa"><a href="/administracion"><AdminIcon kind="home"/>Dashboard</a><a href="/administracion/catalogo" aria-current="page"><AdminIcon kind="catalog"/>Servicios y precios</a><a href="/administracion/sucursales"><AdminIcon kind="branch"/>Sucursales</a><a href="/administracion/empleados"><AdminIcon kind="people"/>Empleados</a><a href="/cuenta/seguridad"><AdminIcon kind="lock"/>Seguridad de la cuenta</a></nav>
      <div className="admin-sidebar-pending"><span>Configurador</span><small>En construcción</small><span>Pedidos y cotizaciones</span><small>En construcción</small></div><p className="admin-sidebar-version">Versión 0.1 · Entorno local</p>
    </aside>
    <div className="admin-main"><header className="admin-topbar"><span>Administración de la imprenta</span><div className="admin-user"><span className="admin-avatar" aria-hidden="true">{name.slice(0,1).toUpperCase()}</span><div><strong>{name}</strong><small>Propietario</small></div><LogoutButton/></div></header>
      <main className="admin-content"><p className="admin-breadcrumb"><a href="/administracion">Administración</a><span aria-hidden="true">›</span>{title}</p><div className="admin-title"><AdminIcon kind="catalog"/><div><h1>{title}</h1><p>{description}</p></div></div>{children}</main>
    </div>
  </div>;
}
