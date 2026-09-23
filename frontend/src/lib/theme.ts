//#region ENCABEZADO · src/lib/theme.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/theme.ts
 * FUNCIÓN
 * Define la preferencia visual y la aplica antes de pintar la página para
 * evitar un destello claro al recargar una apariencia oscura guardada.
 * ------------------------------------------------------------------------
 * FUNCIONES DECLARADAS
 * - normalizeTheme(value: unknown): Theme
 * TIPOS DECLARADOS: Theme.
 * VALORES DE MÓDULO: themeStorageKey, themeBootstrap.
 * ========================================================================
 */
//#endregion
export type Theme="light"|"dark";
export const themeStorageKey="lamontana-apariencia";
export function normalizeTheme(value:unknown):Theme{return value==="dark"?"dark":"light";}
export const themeBootstrap=`try{document.documentElement.dataset.theme=localStorage.getItem("${themeStorageKey}")==="dark"?"dark":"light"}catch{document.documentElement.dataset.theme="light"}`;
