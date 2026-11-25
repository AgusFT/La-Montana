//#region ENCABEZADO · src/components/theme-switch.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/theme-switch.tsx
 * FUNCIÓN
 * Alterna la apariencia clara u oscura sin cambiar el contenido ni los datos.
 * Recuerda la preferencia local y sincroniza otras pestañas del navegador.
 * ------------------------------------------------------------------------
 * COMPONENTES Y FUNCIONES DECLARADOS
 * - ThemeSwitch()
 * - ThemeSwitch :: sync(event: StorageEvent)
 * - ThemeSwitch :: choose(next: Theme)
 * TIPOS DECLARADOS: no declara tipos propios; importa Theme.
 * ========================================================================
 */
//#endregion
"use client";
import {useEffect,useState} from "react";
import {themeStorageKey,normalizeTheme,type Theme} from "@/lib/theme";

export function ThemeSwitch(){
  const [theme,setTheme]=useState<Theme>("light");
  useEffect(()=>{
    setTheme(normalizeTheme(document.documentElement.dataset.theme));
    function sync(event:StorageEvent){if(event.key!==themeStorageKey&&event.key!==null)return;const next=normalizeTheme(event.newValue);document.documentElement.dataset.theme=next;setTheme(next);}
    window.addEventListener("storage",sync);return()=>window.removeEventListener("storage",sync);
  },[]);
  function choose(next:Theme){document.documentElement.dataset.theme=next;setTheme(next);try{localStorage.setItem(themeStorageKey,next);}catch{/* El cambio sigue funcionando si el navegador bloquea el almacenamiento. */}}
  return <div className="theme-switch" role="group" aria-label="Apariencia">
    <button type="button" onClick={()=>choose("light")} aria-label="Usar modo claro" aria-pressed={theme==="light"} title="Modo claro">
      <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.4 1.4m11.2 11.2L19 19M5 19l1.4-1.4M17.6 6.4 19 5"/></svg>
    </button>
    <button type="button" onClick={()=>choose("dark")} aria-label="Usar modo oscuro" aria-pressed={theme==="dark"} title="Modo oscuro">
      <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M20.6 14A8.7 8.7 0 0 1 10 3.4 8.8 8.8 0 1 0 20.6 14Z"/></svg>
    </button>
  </div>;
}
