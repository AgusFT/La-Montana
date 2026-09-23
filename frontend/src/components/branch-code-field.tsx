"use client";

import { useEffect, useId, useRef, useState } from "react";

export function BranchCodeField({ code }: { code?: string }) {
  const id = useId(), [open, setOpen] = useState(false);
  const control = useRef<HTMLDivElement>(null), button = useRef<HTMLButtonElement>(null);
  const helpId = `${id}-help`;
  useEffect(() => {
    if (!open) return;
    function outside(event: PointerEvent) {
      if (event.target instanceof Node && !control.current?.contains(event.target)) setOpen(false);
    }
    function escape(event: KeyboardEvent) { if (event.key === "Escape") setOpen(false); }
    document.addEventListener("pointerdown", outside);
    document.addEventListener("keydown", escape);
    return () => {
      document.removeEventListener("pointerdown", outside);
      document.removeEventListener("keydown", escape);
    };
  }, [open]);

  return <div className="branch-code-field">
    <label htmlFor={id}>Código</label>
    <div ref={control} className="branch-code-control"
      onPointerLeave={event => { if (event.pointerType === "mouse" && document.activeElement !== button.current) setOpen(false); }}
      onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false); }}
      onKeyDown={event => { if (event.key === "Escape" && open) { event.stopPropagation(); setOpen(false); } }}>
      <input id={id} name="codigo" required maxLength={40} pattern="[A-Za-z0-9_\-]+" defaultValue={code ?? ""} readOnly={!!code} aria-describedby={helpId} />
      <button ref={button} type="button" className="branch-code-info" aria-label="Información sobre el código de sucursal"
        aria-expanded={open} aria-controls={helpId} aria-describedby={helpId}
        onPointerEnter={event => { if (event.pointerType === "mouse") setOpen(true); }}
        onFocus={event => { if (event.currentTarget.matches(":focus-visible")) setOpen(true); }}
        onClick={() => setOpen(value => !value)}>
        <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
          <circle cx="12" cy="12" r="9" /><path d="M12 10.5V17" /><circle cx="12" cy="7.2" r=".8" fill="currentColor" stroke="none" />
        </svg>
      </button>
      <div id={helpId} className="branch-code-tooltip" role="tooltip" hidden={!open} tabIndex={-1}>
        <strong>¿Para qué sirve el código?</strong>
        <p>Es un identificador único que elegís para distinguir esta sucursal. Se muestra en los listados de sucursales, la asignación de empleados y la operación interna.</p>
        <p><strong>Formato recomendado:</strong> localidad o barrio + número. Por ejemplo, <code>CABA-01</code> o <code>PALERMO-02</code>.</p>
        <p>Hasta 40 caracteres: letras sin tildes (A–Z), números, guion (-) o guion bajo (_), sin espacios. Se guarda en mayúsculas y no se puede cambiar después de crear la sucursal.</p>
      </div>
    </div>
  </div>;
}
