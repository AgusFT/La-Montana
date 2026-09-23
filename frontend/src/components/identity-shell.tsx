import {MountainMark, MountainSignature} from "./mountain-brand";
import "@/app/access.css";

export function IdentityShell({ title, description, children, access=false }: { title: string; description: string; children: React.ReactNode; access?:boolean }) {
  if(access)return <div className="access-layout">
    <header className="access-header"><a className="brand brand-link" href="/"><MountainMark/><span>La Montaña<small>impresiones</small></span></a><nav aria-label="Acceso a la cuenta"><a href="/acceso">Cuenta</a></nav></header>
    <main className="access-main"><section className="access-card"><MountainSignature/><h1>{title}</h1><p className="access-description">{description}</p>{children}</section></main>
    <footer className="access-footer"><p><strong>Seguridad</strong>Acceso a tu cuenta</p><p><strong>Tus archivos</strong>PDF privados</p><p><strong>Historial</strong>Seguimiento de pedidos</p><p><a href="/">Volver al inicio</a><br/>Piloto local v0.1</p></footer>
  </div>;
  return <main className="shell identity-shell">
    <header className="brand-row"><a className="brand brand-link" href="/"><MountainMark/><span>La Montaña<small>IMPRESIONES</small></span></a><span className="version">Versión 0.1.0</span></header>
    <section className="intro"><span className="eyebrow">PILOTO LOCAL · v0.1</span><h1>{title}</h1><p>{description}</p></section>
    <section className="status-panel identity-panel">{children}</section>
    <footer><a href="/">Volver al inicio</a></footer>
  </main>;
}
