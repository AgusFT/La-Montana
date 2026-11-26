import "./demo-sidebar.css";

export function DemoSidebarMore({children}: {children?: React.ReactNode}) {
  return <>
    <button type="button" className="demo-menu-trigger" popoverTarget="demo-sidebar-options" aria-label="Más de la Demo" title="Más de la Demo">Demo</button>
    <div id="demo-sidebar-options" className="demo-sidebar-options" popover="auto" role="group" aria-label="Más de la Demo">
      {children}
      {["Analytics", "Chat Bot"].map(label =>
        <button className="demo-premium-nav" type="button" key={label} disabled>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">
            <rect x="5" y="10" width="14" height="12" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4M12 15v3"/>
          </svg>
          <span>{label}{" "}<small>Prémium futuro</small></span>
        </button>)}
    </div>
  </>;
}
