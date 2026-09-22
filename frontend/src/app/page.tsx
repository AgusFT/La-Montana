import { getSystemStatus } from "@/lib/system-status";
import { redirect } from "next/navigation";
import { sessionHome } from "@/lib/roles";
import { getSession, getSetupState } from "@/lib/identity-server";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  const [status, setup, session] = await Promise.all([getSystemStatus(), getSetupState(), getSession()]);
  if (session.state === "authenticated") redirect(sessionHome(session.profile));
  const errorMessage = !status.ok && status.reason === "not-configured"
    ? "La conexión con el backend todavía no está configurada."
    : !status.ok && status.reason === "invalid-response"
      ? "El backend respondió, pero su estado no corresponde a esta etapa."
      : "No pudimos consultar el backend. Puede estar iniciando o no estar disponible.";

  return (
    <main className="shell">
      <header className="brand-row">
        <div className="brand"><span className="brand-mark" aria-hidden="true">△</span><span>La Montaña<small>IMPRESIONES</small></span></div>
        <span className="version">Versión 0.1.0</span>
      </header>

      <section className="intro" aria-labelledby="page-title">
        <span className="eyebrow">IDENTIDAD INICIAL</span>
        <h1 id="page-title">Estamos construyendo<br />La Montaña.</h1>
        <p>Ya podés crear tu cuenta de cliente e ingresar. La configuración de la imprenta y la operación de pedidos todavía están en construcción.</p>
      </section>

      <section className="status-panel" aria-labelledby="status-title">
        <div className="panel-heading"><h2 id="status-title">Estado del sistema</h2><span className="environment">Entorno local</span></div>
        <div className={`connection ${status.ok ? "connected" : "disconnected"}`} role="status">
          <span className="status-dot" aria-hidden="true" />
          <div><h3>{status.ok ? "Conexión con el backend verificada" : "Conexión pendiente"}</h3><p>{status.ok ? "El backend respondió correctamente. El acceso inicial está disponible." : errorMessage}</p></div>
        </div>
        <dl className="capabilities">
          <div><dt>Acceso del propietario</dt><dd>{setup ? setup.requierePropietario ? "Alta pendiente" : "Habilitado" : "Sin verificar"}</dd></div>
          <div><dt>Registro y acceso de clientes</dt><dd>{setup && !setup.requierePropietario ? "Habilitado" : "Pendiente de instalación"}</dd></div>
          <div><dt>Gestión de empleados y sucursales</dt><dd>Disponible para el propietario</dd></div>
          <div><dt>Configuración de la imprenta</dt><dd>En construcción</dd></div>
          <div><dt>Pedidos y operación</dt><dd>En construcción</dd></div>
        </dl>
        <p className="empty-note">Esta instalación comienza sin sucursales ni configuración comercial precargadas.</p>
        {!setup && <p className="form-message error-message" role="alert">No pudimos verificar si la instalación necesita un propietario.</p>}
        {setup?.requierePropietario && !setup.altaHabilitada && <p className="empty-note">El operador debe habilitar el token de instalación en el servidor.</p>}
        <div className="page-actions"><a className="refresh" href="/acceso">Iniciar sesión</a><a className="secondary-link" href="/registro">Crear cuenta de cliente</a>{setup?.requierePropietario && <a className="secondary-link" href="/instalacion">Preparar instalación</a>}<a className="secondary-link" href="/">Volver a comprobar <span aria-hidden="true">↗</span></a></div>
      </section>

      <footer>La Montaña <span aria-hidden="true">·</span> Identidad inicial</footer>
    </main>
  );
}
