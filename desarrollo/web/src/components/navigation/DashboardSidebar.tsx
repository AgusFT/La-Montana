"use client"

import { BrandLockup } from "../brand/BrandLockup";
import { useRouter } from "next/navigation";
// utilizado para obtener la ruta actual
import { usePathname } from "next/navigation";

export function Sidebar() {

  const router = useRouter(); 
  // guardo la ruta actual
  const pathname = usePathname();

  // sirve para verificar si la ruta actual esta activa o no
  function isActive(path: string) {
    return pathname.startsWith(path);
  }

  // a futuro supabase haria aca el   await supabase.auth.signOut();
  function handleLogout() {
    router.push("/login");
  }

  function navigateToDashboard() {
    router.push("/dashboard");
  }

  
  function navigateToCrearPedido() {
    router.push("/pedidos/nuevo");
  }

  function navigateToPedidoActual() {
    router.push("/pedidos/actual");
  }

  return (
    <aside className="sidebar">
      <BrandLockup />
      <nav className="side-nav" aria-label="Navegación del panel">
        <button className={isActive("/dashboard") ? "is-active" : ""} type="button" onClick={navigateToDashboard}>
          Inicio
        </button>
        <button className={isActive("/mis-pedidos") ? "is-active" : ""} type="button" onClick={navigateToPedidoActual}>Mis pedidos</button>
        <button className={isActive("/pedidos/nuevo") ? "is-active" : ""} type="button" onClick={navigateToCrearPedido}>Crear pedido</button>
        <button className={isActive("/puntos-de-entrega") ? "is-active" : ""} type="button">Puntos de entrega</button>
        <button className={isActive("/contacto") ? "is-active" : ""} type="button">Contacto</button>
      </nav>

      <div className="sidebar-help">
        <strong>¿Necesitás ayuda?</strong>
        <span>Escribinos por WhatsApp para revisar tu pedido.</span>
      </div>

      <button className="secondary-button" type="button" onClick={handleLogout}>
        Cerrar sesión
      </button>
    </aside>
  );
}
