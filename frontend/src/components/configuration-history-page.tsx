"use client";
import {useStepNavigation} from "./use-step-navigation";
import {useNavigationGuard} from "@/components/navigation-boundary";
import {useState} from "react";
import {ConfigurationHistory} from "@/components/configuration-history";
const phases=["Configuración actual","Modelo operativo y aprobación","Pagos y reglas de seña","Recursos y asignación","Horarios y entrega","Resumen y simulación","Aplicación y seguridad","Historial de versiones"];
export function ConfigurationHistoryPage(){const stepNavigation=useStepNavigation(8);const[locked,setLocked]=useState(false);useNavigationGuard({blocked:locked});return <div className="configuration-layout"><aside className="configuration-steps"><h2>Flujo de configuración</h2><p>Fase 8 de 8</p><nav ref={stepNavigation} aria-label="Fases del configurador">{phases.map((label,i)=><button key={i} aria-current={i===7?"step":undefined} disabled={locked||i>0&&i<7} onClick={()=>{if(i===0)location.assign("/administracion/configuracion");}}><span>{String(i+1).padStart(2,"0")}</span><span>{label}</span></button>)}</nav></aside><div className="configuration-body"><ConfigurationHistory onLockChange={setLocked} onCurrent={()=>location.assign("/administracion/configuracion")}/></div></div>;}
