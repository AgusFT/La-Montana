export function IdentityShell({ title, description, children }: { title: string; description: string; children: React.ReactNode }) {
  return <main className="shell identity-shell">
    <header className="brand-row"><a className="brand brand-link" href="/"><span className="brand-mark" aria-hidden="true">△</span><span>La Montaña<small>IMPRESIONES</small></span></a><span className="version">Versión 0.1.0</span></header>
    <section className="intro"><span className="eyebrow">PILOTO LOCAL · v0.1</span><h1>{title}</h1><p>{description}</p></section>
    <section className="status-panel identity-panel">{children}</section>
    <footer><a href="/">Volver al inicio</a></footer>
  </main>;
}
