import { getSystemStatus } from "@/lib/system-status";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  const status = await getSystemStatus();
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
        <span className="eyebrow">ETAPA 1 · BASE TÉCNICA</span>
        <h1 id="page-title">Estamos construyendo<br />La Montaña.</h1>
        <p>Este es el punto de partida del nuevo sistema. La configuración de la imprenta y la operación de pedidos todavía están en construcción.</p>
      </section>

      <section className="status-panel" aria-labelledby="status-title">
        <div className="panel-heading"><h2 id="status-title">Estado del sistema</h2><span className="environment">Entorno local</span></div>
        <div className={`connection ${status.ok ? "connected" : "disconnected"}`} role="status">
          <span className="status-dot" aria-hidden="true" />
          <div><h3>{status.ok ? "Conexión con el backend verificada" : "Conexión pendiente"}</h3><p>{status.ok ? "El backend respondió correctamente. La base técnica está disponible." : errorMessage}</p></div>
        </div>
        <dl className="capabilities">
          <div><dt>Acceso de clientes y personal</dt><dd>En construcción</dd></div>
          <div><dt>Configuración de la imprenta</dt><dd>En construcción</dd></div>
          <div><dt>Pedidos y operación</dt><dd>En construcción</dd></div>
        </dl>
        <p className="empty-note">Esta etapa no incluye usuarios, sucursales ni configuración comercial precargados.</p>
        <a className="refresh" href="/">Volver a comprobar <span aria-hidden="true">↗</span></a>
      </section>

      <footer>La Montaña <span aria-hidden="true">·</span> Primera etapa de implementación</footer>
    </main>
  );
}
