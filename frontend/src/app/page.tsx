import {MountainMark} from "@/components/mountain-brand";
import { getSystemStatus } from "@/lib/system-status";
import {installationRequired,operationPending} from "@/lib/installation-messages";
import { sessionHome } from "@/lib/roles";
import { getSession, getSetupState } from "@/lib/identity-server";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  const [status, setup, session] = await Promise.all([getSystemStatus(), getSetupState(), getSession()]);
  const errorMessage = !status.ok && status.reason === "not-configured"
    ? "La conexión con el backend todavía no está configurada."
    : !status.ok && status.reason === "invalid-response"
      ? "El sistema respondió con un estado que no pudimos comprobar."
      : "No pudimos consultar el backend. Puede estar iniciando o no estar disponible.";

  return (
    <main className="shell">
      <header className="brand-row">
        <div className="brand"><MountainMark/><span>La Montaña<small>IMPRESIONES</small></span></div>
        <span className="version">Versión 0.1.0</span>
      </header>

      <section className="intro" aria-labelledby="page-title">
        <span className="eyebrow">PILOTO LOCAL · v0.1</span>
        <h1 id="page-title">Tu demo local.<br />Prepará la imprenta.</h1>
        <p>Este es el panel de inicio del software La Montaña: reúne la instalación y los accesos de propietario, empleados y clientes. La página pública de tu imprenta tiene su propio espacio. Este piloto funciona con producción y pagos registrados manualmente.</p>
      </section>

      <section className="status-panel" aria-labelledby="setup-order"><h2 id="setup-order">Por dónde empezar</h2><ol className="installation-order"><li><strong>Crear al propietario.</strong> El responsable prepara la instalación una sola vez.</li><li><strong>Configurar la imprenta.</strong> Ingresá como propietario y seguí el mapa del dashboard: sucursales, servicios, precios y reglas.</li><li><strong>Revisar y activar.</strong> La activación habilita la operación de pedidos con la configuración guardada.</li><li><strong>Preparar y publicar la página web.</strong> Es opcional e independiente de la activación operativa.</li><li><strong>Ingresar como cliente.</strong> El registro está habilitado desde que se crea al propietario, aunque todavía no se puedan pedir trabajos. El propietario crea las cuentas de empleados.</li></ol><a className="refresh" href="/web">Ver página de la imprenta</a>{session.state==="authenticated"&&<a className="secondary-link" href={sessionHome(session.profile)}>Ir a mi panel</a>}</section>

      <section className="status-panel" aria-labelledby="status-title">
        <div className="panel-heading"><h2 id="status-title">Estado del sistema</h2><span className="environment">Entorno local</span></div>
        <div className={`connection ${status.ok ? "connected" : "disconnected"}`} role="status">
          <span className="status-dot" aria-hidden="true" />
          <div><h3>{status.ok ? "Conexión con el backend verificada" : "Conexión pendiente"}</h3><p>{status.ok ? "El sistema respondió y pudo consultar la base de datos." : errorMessage}</p></div>
        </div>
        <dl className="capabilities">
          <div><dt>Acceso del propietario</dt><dd>{setup ? setup.requierePropietario ? "Alta pendiente" : "Habilitado" : "Sin verificar"}</dd></div>
          <div><dt>Registro y acceso de clientes</dt><dd>{setup ? !setup.requierePropietario ? "Habilitado" : "Pendiente de instalación" : "Sin verificar"}</dd></div>
          <div><dt>Gestión de empleados y sucursales</dt><dd>Disponible para el propietario</dd></div>
          <div><dt>Configuración de la imprenta</dt><dd>Disponible para el propietario</dd></div>
          <div><dt>Pedidos y operación</dt><dd>{status.ok?status.data.operacionDisponible?"Configuración operativa activa":"Pendientes de configuración y activación":"Sin verificar"}</dd></div>
        </dl>
        {setup?.requierePropietario?<p className="form-message error-message">{installationRequired}</p>:status.ok&&!status.data.operacionDisponible&&<p className="empty-note">{operationPending}</p>}
        {!setup && <p className="form-message error-message" role="alert">No pudimos verificar si la instalación necesita un propietario.</p>}
        {setup?.requierePropietario && !setup.altaHabilitada && <p className="empty-note">El operador debe habilitar el token de instalación en el servidor.</p>}
        <div className="page-actions"><a className="refresh" href="/acceso">Iniciar sesión</a><a className="secondary-link" href="/registro">Crear cuenta de cliente</a>{setup?.requierePropietario && <a className="secondary-link" href="/instalacion">Preparar instalación</a>}<a className="secondary-link" href="/">Volver a comprobar <span aria-hidden="true">↗</span></a></div>
      </section>

      <footer>La Montaña <span aria-hidden="true">·</span> Panel de inicio de la demo</footer>
    </main>
  );
}
