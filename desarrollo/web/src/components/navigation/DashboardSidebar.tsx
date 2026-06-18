import { BrandLockup } from "../brand/BrandLockup";


export function Sidebar({ onLogout }: { onLogout: () => void; }) {
  return (
    <aside className="sidebar">
      <BrandLockup />
      <nav className="side-nav" aria-label="Navegación del panel">
        <button className="is-active" type="button">
          Inicio
        </button>
        <button type="button">Mis pedidos</button>
        <button type="button">Crear pedido</button>
        <button type="button">Puntos de entrega</button>
        <button type="button">Contacto</button>
      </nav>

      <div className="sidebar-help">
        <strong>¿Necesitás ayuda?</strong>
        <span>Escribinos por WhatsApp para revisar tu pedido.</span>
      </div>

      <button className="secondary-button" type="button" onClick={onLogout}>
        Cerrar sesión
      </button>
    </aside>
  );
}
