"use client";

import {useEffect,useRef,useState} from "react";
import {GuidedTour,type TourStep} from "./guided-tour";
import {useInstallation} from "./installation-guide";
import type {Branch} from "@/lib/organization-types";

const steps:TourStep[]=[
  {target:"branch-create-location",title:"Creá la primera sucursal",description:"Completá el código, el nombre y la dirección de tu imprenta. La provincia determina automáticamente la zona horaria. Después podés agregar otras sucursales."},
  {target:"branch-create-hours",title:"Definí días y horarios de atención",description:"Marcá los días en que abre esta sucursal e indicá la hora de apertura y cierre. Necesitás al menos un día válido antes de crearla; luego podrás reutilizar estos horarios en el configurador."},
];
const completion={title:"¡Listo! Ya creaste tu primera sucursal",description:"Ya podés continuar con la instalación o agregar otras sucursales ahora o más adelante. Los horarios guardados se podrán usar en el configurador."};

export function BranchOnboarding({branches,owner}:{branches:Branch[]|null;owner:string}){
  const guide=useInstallation();
  const initialWithoutActive=useRef(!branches?.some(branch=>branch.estado==="ACTIVA"));
  const [completed,setCompleted]=useState(false);
  const hasActive=!!branches?.some(branch=>branch.estado==="ACTIVA");
  const firstSetup=guide.enabled&&!guide.data?.activa;
  useEffect(()=>{
    const created=()=>{if(initialWithoutActive.current)setCompleted(true);};
    window.addEventListener("lamontana:branch-created",created);
    return()=>window.removeEventListener("lamontana:branch-created",created);
  },[]);
  return <>
    {firstSetup&&<section className="installation-guide catalog-installation" aria-label="Avance de sucursales de la primera instalación">
      <div><strong>Primera instalación · Sucursales</strong><p>{hasActive?"Ya hay una sucursal activa. Podés continuar con el catálogo base o agregar otra sucursal.":"Creá al menos una sucursal activa con ubicación y días y horarios de atención para continuar."}</p></div>
      <div className="catalog-installation-action"><button type="button" className="admin-button" disabled={!hasActive||guide.loading||guide.error} onClick={()=>{window.location.href="/administracion/catalogo";}}>Continuar instalación →</button>{!hasActive&&<small>Guardá una sucursal activa para habilitar el siguiente paso.</small>}</div>
    </section>}
    {!completed&&<GuidedTour steps={steps} completion={hasActive?completion:undefined} helpLabel="Guía de Sucursales" storageKey={`lamontana:branches-tour:v1:${owner}`} autoStart={firstSetup&&!!guide.data&&!guide.error}/>}
    {completed&&<div className="guided-tour branch-completion" role="dialog" aria-modal="true" aria-labelledby="branch-completion-title" aria-describedby="branch-completion-description" onKeyDown={event=>{if(event.key==="Escape")setCompleted(false);}}>
      <div className="guided-tour-shade" style={{inset:0}}/>
      <div className="guided-tour-card is-final"><div className="guided-tour-top"><span>Sucursal creada</span><button type="button" className="guided-tour-close" aria-label="Cerrar guía" onClick={()=>setCompleted(false)}>×</button></div>
        <h2 id="branch-completion-title">{completion.title}</h2><p id="branch-completion-description">{completion.description}</p>
        <div className="guided-tour-actions"><button type="button" className="admin-button secondary" onClick={()=>setCompleted(false)}>Omitir</button><button type="button" className="admin-button" autoFocus onClick={()=>setCompleted(false)}>Aceptar</button></div>
      </div>
    </div>}
  </>;
}
