//#region ENCABEZADO · installation-guide.tsx
/*
 * ========================================================================
 * FUNCIÓN: Comparte el avance inicial, bloquea pantallas/pasos prematuros y guía
 * al siguiente guardado; vuelve a consultar tras escrituras y al retomar la vista.
 * FUNCIONES: InstallationProvider (refresh, visible, restore), useInstallation,
 * GuidedNavLink, InstallationGate. TIPO: InstallationContext.
 * ========================================================================
 */
//#endregion
"use client";
import {createContext,useCallback,useContext,useEffect,useRef,useState,type ReactNode} from "react";
import {installationAllowed,installationChanged,isPreparation,nextStep,type Preparation} from "@/lib/installation-types";
type InstallationContext={data:Preparation|null;loading:boolean;error:boolean;enabled:boolean;refresh:()=>Promise<void>};
const Context=createContext<InstallationContext>({data:null,loading:false,error:false,enabled:false,refresh:async()=>{}});
export function useInstallation(){return useContext(Context);}
export function InstallationProvider({initial,enabled,children}:{initial:Preparation|null;enabled:boolean;children:ReactNode}){
  const[data,setData]=useState(initial),[loading,setLoading]=useState(false),[error,setError]=useState(enabled&&!initial),request=useRef<AbortController|null>(null);
  const refresh=useCallback(async()=>{
    if(!enabled)return;request.current?.abort();const controller=new AbortController();request.current=controller;setLoading(true);setError(false);
    try{const r=await fetch("/api/admin/preparacion",{cache:"no-store",signal:controller.signal});const value:unknown=await r.json();if(!r.ok||!isPreparation(value))throw Error();if(!controller.signal.aborted)setData(value);}
    catch{if(!controller.signal.aborted)setError(true);}finally{if(!controller.signal.aborted)setLoading(false);}
  },[enabled]);
  useEffect(()=>{setData(initial);setError(enabled&&!initial);},[initial,enabled]);
  useEffect(()=>{
    function visible(){if(document.visibilityState==="visible")void refresh();}
    function restore(event:PageTransitionEvent){if(event.persisted)void refresh();}
    window.addEventListener(installationChanged,refresh);window.addEventListener("pageshow",restore);document.addEventListener("visibilitychange",visible);
    return()=>{request.current?.abort();window.removeEventListener(installationChanged,refresh);window.removeEventListener("pageshow",restore);document.removeEventListener("visibilitychange",visible);};
  },[refresh]);
  return <Context.Provider value={{data,loading,error,enabled,refresh}}>{children}</Context.Provider>;
}
export function GuidedNavLink({area,href,current,children}:{area:string;href:string;current?:boolean;children:ReactNode}){
  const {data,enabled,error}=useInstallation(),allowed=!enabled||!error&&installationAllowed(data,area),next=data&&nextStep(data);
  return allowed?<a href={href} aria-current={current?"page":undefined}>{children}</a>:<span role="link" aria-disabled="true" tabIndex={0} className="guided-link-disabled" title={next?`Primero: ${next.titulo}.`:"Esperá a comprobar el estado de la instalación."}>{children}<span aria-hidden="true">🔒</span></span>;
}
export function InstallationGate({area,children}:{area:string;children:ReactNode}){
  const {data,enabled,loading,error,refresh}=useInstallation();
  const guided=enabled&&!data?.activa;
  const next=data&&nextStep(data),allowed=installationAllowed(data,area),protectedArea=!["dashboard","sucursales","empleados","web"].includes(area),blocked=!allowed||!data&&error&&protectedArea;
  return <>
    {guided&&area!=="dashboard"&&<section className="installation-guide" aria-label="Guía de primera instalación">
      <div><strong>Primera instalación · pasos en orden</strong><p>{next?`Próximo paso: ${next.titulo}.`:"Comprobando qué falta para completar tu instalación."} Guardá cada paso antes de continuar. Las simulaciones, los empleados y la página web son opcionales.</p></div>
      {next&&<a className="admin-button" href={next.enlace}>Continuar instalación →</a>}
    </section>}
    {guided&&error&&<div className="admin-warning" role="alert">No pudimos comprobar el avance guardado. <button type="button" className="admin-button secondary" disabled={loading} onClick={()=>void refresh()}>Volver a comprobar instalación</button></div>}
    {guided&&blocked&&<section className="admin-card installation-blocked"><h2>Este paso todavía no está habilitado</h2><p>{next?`Primero completá «${next.titulo}». Al guardar los datos requeridos se habilitará el paso siguiente.`:"Necesitamos consultar el estado de la instalación para habilitar esta pantalla."}</p>{next&&<a className="admin-button" href={next.enlace}>Ir al paso pendiente →</a>}</section>}
    <div hidden={guided&&blocked}>{children}</div>
  </>;
}
