//#region ENCABEZADO · save-notice.tsx
/*
 * ========================================================================
 * FUNCIÓN: Confirma un resultado con texto, icono y contraste. Acerca el aviso
 * cuando queda fuera de pantalla, sin mover el foco ni desaparecer por tiempo.
 * FUNCIONES: SaveNotice({children,title,pending}). Respeta movimiento reducido.
 * ========================================================================
 */
//#endregion
"use client";
import {useEffect,useRef,type ReactNode} from "react";
export function SaveNotice({children,title="Guardado con éxito",pending=false}:{children:ReactNode;title?:string;pending?:boolean}){
  const ref=useRef<HTMLDivElement>(null);
  useEffect(()=>{const el=ref.current;if(!el||!el.getClientRects().length)return;const r=el.getBoundingClientRect();if(r.top<0||r.bottom>innerHeight)el.scrollIntoView({block:"nearest",behavior:matchMedia("(prefers-reduced-motion: reduce)").matches?"instant":"smooth"});},[children,title]);
  return <div className={`save-notice${pending?" is-pending":""}`} role="status" aria-live="polite" aria-atomic="true" ref={ref}><span className="save-notice-icon" aria-hidden="true">✓</span><div><strong>{title}</strong><p>{children}</p></div></div>;
}
