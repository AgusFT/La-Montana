//#region ENCABEZADO · src/components/navigation-boundary.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/navigation-boundary.tsx
 * ========================================================================
 * FUNCIÓN
 * Registra formularios con cambios o acciones pendientes y controla enlaces, cambios de paso,
 * cierre de sesión y salida del navegador para evitar abandonar operaciones sin advertencia.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - register()
 * - confirm()
 * - [export] NavigationBoundary({children}: {children:ReactNode})
 *   Componente de interfaz.
 * - NavigationBoundary :: register(id: string, guard: Guard)
 * - NavigationBoundary :: confirm()
 * - NavigationBoundary :: beforeUnload(event: BeforeUnloadEvent)
 * - NavigationBoundary :: followLink(event: MouseEvent)
 * - [export] useNavigationGuard({dirty=false,blocked=false}: Guard)
 *   Hook reutilizable de React.
 * - [export] useConfirmNavigation()
 *   Hook reutilizable de React.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - Guard (tipo).
 * - Navigation (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - NavigationContext [const].
 * ========================================================================
 */
//#endregion

"use client";
import {createContext,useCallback,useContext,useEffect,useId,useRef,type ReactNode} from "react";

type Guard={dirty?:boolean;blocked?:boolean};
type Navigation={register:(id:string,guard:Guard)=>()=>void;confirm:()=>boolean};
const NavigationContext=createContext<Navigation>({register:()=>()=>{},confirm:()=>true});

/** Keeps ordinary links, logout, phase changes and browser departures aware of active forms. */
export function NavigationBoundary({children}:{children:ReactNode}){
  const guards=useRef(new Map<string,Guard>()),acceptedDeparture=useRef(false);
  const register=useCallback((id:string,guard:Guard)=>{guards.current.set(id,guard);acceptedDeparture.current=false;return()=>{guards.current.delete(id);};},[]);
  const confirm=useCallback(()=>{
    const values=[...guards.current.values()];
    if(values.some(g=>g.blocked)){window.alert("Hay una solicitud o autorización pendiente. Esperá su resultado o cancelá la autorización desde este formulario antes de navegar. Si la respuesta es incierta, usá el reintento indicado para conocer el resultado.");return false;}
    return !values.some(g=>g.dirty)||window.confirm("Tenés cambios sin guardar. Aceptar: salir sin guardar esos cambios. Cancelar: seguir editando. Lo que ya guardaste en el borrador se conserva.");
  },[]);
  useEffect(()=>{
    function beforeUnload(event:BeforeUnloadEvent){if(!acceptedDeparture.current&&[...guards.current.values()].some(g=>g.dirty||g.blocked)){event.preventDefault();event.returnValue="";}}
    function followLink(event:MouseEvent){
      if(event.defaultPrevented||event.button!==0||event.ctrlKey||event.metaKey||event.shiftKey||event.altKey)return;
      const link=event.target instanceof Element?event.target.closest<HTMLAnchorElement>("a[href]"):null;
      if(!link||link.target&&link.target!=="_self"||link.hasAttribute("download"))return;
      const destination=new URL(link.href,location.href);
      if(destination.pathname===location.pathname&&destination.search===location.search&&destination.hash)return;
      if(!confirm()){event.preventDefault();event.stopPropagation();}else acceptedDeparture.current=true;
    }
    window.addEventListener("beforeunload",beforeUnload);document.addEventListener("click",followLink,true);
    return()=>{window.removeEventListener("beforeunload",beforeUnload);document.removeEventListener("click",followLink,true);};
  },[confirm]);
  return <NavigationContext.Provider value={{register,confirm}}>{children}</NavigationContext.Provider>;
}

export function useNavigationGuard({dirty=false,blocked=false}:Guard){
  const id=useId(),{register}=useContext(NavigationContext);
  useEffect(()=>register(id,{dirty,blocked}),[id,register,dirty,blocked]);
}
export function useConfirmNavigation(){return useContext(NavigationContext).confirm;}
