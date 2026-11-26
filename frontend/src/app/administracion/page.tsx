//#region ENCABEZADO · src/app/administracion/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/administracion/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Construye el dashboard del propietario con mapa de primera instalación hasta activar la
 * configuración, consultas por sucursal, disponibilidad de puntos y accesos administrativos.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] AdministrationPage()
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

import { redirect } from "next/navigation";
import { roleHome } from "@/lib/roles";
import { IdentityShell } from "@/components/identity-shell";
import {AdminShell} from "@/components/admin-shell";
import {GuidedNavLink} from "@/components/installation-guide";
import {PreparationMap} from "@/components/preparation-map";
import {AdminDashboard} from "@/components/admin-dashboard";
import { getSession } from "@/lib/identity-server";

export const dynamic = "force-dynamic";
export default async function AdministrationPage() {
  const session = await getSession();
  if (session.state === "anonymous") redirect("/acceso");
  if (session.state === "unavailable") return <IdentityShell title="Administración" description="Verificación de tu sesión."><p className="form-message error-message" role="alert">No pudimos verificar la sesión. Intentá nuevamente cuando el sistema esté disponible.</p><a className="refresh" href="/administracion">Volver a comprobar</a></IdentityShell>;
  if (session.state !== "authenticated") return null;
  if (session.profile.debeCambiarContrasena) redirect("/cuenta/seguridad");
  if (session.profile.rol !== "ADMIN_ADMIN") redirect(roleHome(session.profile.rol));
  const owner = session.profile;
  return <AdminShell title="Dashboard administrativo" description={`Bienvenido, ${owner.nombre}. Consultá todos los pedidos de la imprenta y sus destinos de entrega.`} name={`${owner.nombre} ${owner.apellido}`} active="dashboard">
    <PreparationMap/>
    <div className="dashboard-layout"><div><AdminDashboard/></div>
    <aside className="dashboard-aside"><section className="dashboard-side-card"><h2>Disponibilidad de puntos</h2><p>Vista previa del módulo de la aplicación móvil.</p><a href="/administracion/puntos-entrega">Ver puntos de entrega →</a></section>
    <section className="dashboard-side-card"><h2>Acciones rápidas</h2><nav aria-label="Acciones rápidas"><GuidedNavLink area="operacion" href="/administracion/pedidos">Pedidos, producción y entregas</GuidedNavLink><GuidedNavLink area="catalogo" href="/administracion/catalogo">Servicios y precios</GuidedNavLink><GuidedNavLink area="configuracion" href="/administracion/configuracion">Configurar la imprenta</GuidedNavLink><GuidedNavLink area="configuracion" href="/administracion/configuracion/historial">Historial de configuraciones</GuidedNavLink><GuidedNavLink area="sucursales" href="/administracion/sucursales">Administrar sucursales</GuidedNavLink><GuidedNavLink area="empleados" href="/administracion/empleados">Ver administración de usuarios</GuidedNavLink></nav></section>
    <section className="dashboard-side-card"><h2>Funciones del piloto</h2><p>La producción se registra manualmente y los pagos se verifican por personal autorizado.</p><p><span className="configuration-badge">En construcción</span></p><p>CUPS, pagos en línea, reclamos, gestión de clientes y reportes de facturación.</p></section></aside></div>
  </AdminShell>;
}
