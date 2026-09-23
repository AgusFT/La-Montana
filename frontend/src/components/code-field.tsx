//#region ENCABEZADO · src/components/code-field.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/code-field.tsx
 * ========================================================================
 * FUNCIÓN
 * Implementa el campo reutilizable de código con una ayuda accesible que se puede abrir por
 * puntero, teclado o toque y cerrar al salir o presionar Escape.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] CodeField({code, value, onValueChange, subject, maxLength=40, children}:
 *   {code?:string;value?:string;onValueChange?:(value:string)=>void;subject:string;maxLength?:number;children:ReactNode})
 *   Componente de interfaz.
 * - CodeField :: outside(event: PointerEvent)
 * - CodeField :: escape(event: KeyboardEvent)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

"use client";

import { useEffect, useId, useRef, useState, type ReactNode } from "react";

export function CodeField({code, value, onValueChange, subject, maxLength=40, children}: {code?:string;value?:string;onValueChange?:(value:string)=>void;subject:string;maxLength?:number;children:ReactNode}) {
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
      <input id={id} name="codigo" required maxLength={maxLength} pattern="[A-Za-z0-9_\-]+" {...(value===undefined?{defaultValue:code??""}:{value})} onChange={event=>onValueChange?.(event.target.value)} readOnly={!!code} aria-describedby={helpId} />
      <button ref={button} type="button" className="branch-code-info" aria-label={`Información sobre el código de ${subject}`}
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
        {children}
      </div>
    </div>
  </div>;
}
