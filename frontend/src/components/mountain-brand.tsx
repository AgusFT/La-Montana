//#region ENCABEZADO · src/components/mountain-brand.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/mountain-brand.tsx
 * ========================================================================
 * FUNCIÓN
 * Renderiza las marcas gráficas reutilizables de La Montaña mediante SVG y texto para cabeceras y
 * paneles.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] MountainMark({sun=false}: {sun?:boolean})
 *   Componente de interfaz.
 * - [export] MountainSignature()
 *   Componente de interfaz.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import {useId} from "react";

// Vector reconstruido desde la silueta de los mocks; no depende de imágenes remotas.
export function MountainMark({sun=false}:{sun?:boolean}) {
  return <svg className="mountain-mark" viewBox="0 0 80 72" aria-hidden="true" focusable="false">
    {sun?<circle cx="40" cy="29" r="26" fill="#ff850b"/>:<path d="M7 42C3 16 26 2 47 10C23 9 13 24 7 42Z" fill="#f48328"/>}
    <path d="m4 65 13-17 6-3 9-19 9-17 10 21 6 5 6 17 14 13-20-2-8 6-10-4-12 3-11-3Z" fill="currentColor"/>
    <path className="mountain-snow" d="m41 13-6 18-7 11-4 3-5 13 10-8 4-2 5-14 2 11 9 8-1-11-5-12 4 3-4-11Zm-5 36-8 10 9-4 7 6 5-1-7-9-1 6Z" fill="white"/>
  </svg>;
}

export function MountainSignature() {
  const arc=useId();
  return <svg className="mountain-signature" viewBox="0 0 240 194" role="img" aria-label="La Montaña impresiones">
    <defs><path id={arc} d="M29 124A91 91 0 0 1 211 124"/></defs>
    <text fill="currentColor" fontFamily="Georgia, serif" fontSize="29" letterSpacing="2"><textPath href={`#${arc}`} startOffset="50%" textAnchor="middle">La Montaña</textPath></text>
    <g transform="translate(67 59) scale(1.38)"><path d="M7 42C3 16 26 2 47 10C23 9 13 24 7 42Z" fill="#f48328"/><path d="m4 65 13-17 6-3 9-19 9-17 10 21 6 5 6 17 14 13-20-2-8 6-10-4-12 3-11-3Z" fill="currentColor"/><path d="m41 13-6 18-7 11-4 3-5 13 10-8 4-2 5-14 2 11 9 8-1-11-5-12 4 3-4-11Zm-5 36-8 10 9-4 7 6 5-1-7-9-1 6Z" fill="white"/></g>
    <text x="120" y="183" textAnchor="middle" fill="currentColor" fontFamily="Georgia, serif" fontSize="15" letterSpacing="6">impresiones</text>
  </svg>;
}
