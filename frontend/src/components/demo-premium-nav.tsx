import "./demo-sidebar.css";

function LockIcon() {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">
    <rect x="5" y="10" width="14" height="12" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4M12 15v3"/>
  </svg>;
}

const previews = [
  {area: "puntos", href: "/administracion/puntos-entrega", label: "Puntos de entrega"},
  {area: "empleados", href: "/administracion/empleados", label: "Empleados"},
  {area: "riders", href: "/administracion/riders", label: "Riders"},
];

export function DemoLockedNav({adminArea}: {adminArea?: string}) {
  return <div className="demo-locked-nav" role="group" aria-label="Funciones bloqueadas en la demo">
    <p className="demo-locked-title">Bloqueado en la demo</p>
    {adminArea && previews.map(({area, href, label}) =>
      <a className="demo-preview-link" key={area} href={href} aria-current={area === adminArea ? "page" : undefined}>
        <LockIcon/><span>{label}<small>Vista previa</small></span>
      </a>)}
    {["Analytics", "Chat Bot"].map(label =>
      <button className="demo-premium-nav" type="button" key={label} disabled>
        <LockIcon/><span>{label}<small>Prémium futuro</small></span>
      </button>)}
  </div>;
}
