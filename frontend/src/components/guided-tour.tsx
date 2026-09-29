"use client";

import {useCallback,useEffect,useRef,useState} from "react";

export type TourStep={target:string;title:string;description:string};

/** A small reusable spotlight tour. It never changes the underlying form or navigation. */
export function GuidedTour({steps,storageKey,autoStart,completion,helpLabel="Guía del catálogo comercial",onStepChange}:{steps:TourStep[];storageKey:string;autoStart:boolean;completion?:{title:string;description:string};helpLabel?:string;onStepChange?:(step:number)=>void}){
  const [step,setStep]=useState<number|null>(null);
  const [rect,setRect]=useState<DOMRect|null>(null);
  const button=useRef<HTMLButtonElement>(null);
  const help=useRef<HTMLButtonElement>(null);
  const onStepChangeRef=useRef(onStepChange);
  onStepChangeRef.current=onStepChange;
  const active=step!==null;
  const finish=useCallback(()=>{
    setStep(null);
    try{localStorage.setItem(storageKey,"seen");}catch{/* La guía puede reabrirse igualmente. */}
    requestAnimationFrame(()=>help.current?.focus());
  },[storageKey]);

  useEffect(()=>{
    if(!autoStart)return;
    try{if(localStorage.getItem(storageKey)==="seen")return;}catch{/* Sin almacenamiento, se ofrece la guía en esta visita. */}
    setStep(0);
  },[autoStart,storageKey]);
  useEffect(()=>{
    if(!active)return;
    if(step===steps.length){setRect(null);return;}
    onStepChangeRef.current?.(step!);
    const target=document.getElementById(steps[step!].target);
    if(!target)return;
    target.scrollIntoView({block:"center",behavior:"instant"});
    const update=()=>setRect(target.getBoundingClientRect());
    update();
    window.addEventListener("resize",update);
    window.addEventListener("scroll",update,true);
    return()=>{window.removeEventListener("resize",update);window.removeEventListener("scroll",update,true);};
  },[active,step,steps]);
  useEffect(()=>{if(active)button.current?.focus();},[active,step]);
  useEffect(()=>{
    if(!active)return;
    const escape=(event:KeyboardEvent)=>{if(event.key==="Escape")finish();};
    document.addEventListener("keydown",escape);
    return()=>document.removeEventListener("keydown",escape);
  },[active,finish]);

  const padded=rect&&{left:Math.max(0,rect.left-6),top:Math.max(0,rect.top-6),right:Math.min(innerWidth,rect.right+6),bottom:Math.min(innerHeight,rect.bottom+6)};
  const final=step===steps.length;
  const content=final?completion:step!==null?steps[step]:null;
  const nearBottom=padded?innerHeight-padded.bottom<300:false;
  const popupStyle=padded&&!final?{
    left:Math.max(12,Math.min(padded.left,innerWidth-380)),
    top:nearBottom?Math.max(12,padded.top-235):Math.min(innerHeight-245,padded.bottom+14),
  }:undefined;
  return <>
    {!active&&<button ref={help} type="button" className="guided-tour-help" aria-label={`Abrir ${helpLabel.toLowerCase()}`} title={helpLabel} onClick={()=>setStep(0)}>?</button>}
    {active&&<div className="guided-tour" role="dialog" aria-modal="true" aria-labelledby="guided-tour-title" aria-describedby="guided-tour-description" onKeyDown={event=>{if(event.key!=="Tab")return;const controls=Array.from(event.currentTarget.querySelectorAll<HTMLButtonElement>(".guided-tour-card button"));if(event.shiftKey&&document.activeElement===controls[0]){event.preventDefault();controls.at(-1)?.focus();}else if(!event.shiftKey&&document.activeElement===controls.at(-1)){event.preventDefault();controls[0]?.focus();}}}>
      {final&&<div className="guided-tour-shade" style={{inset:0}}/>}
      {padded&&<>
        <div className="guided-tour-shade" style={{top:0,left:0,right:0,height:padded.top}}/>
        <div className="guided-tour-shade" style={{top:padded.top,left:0,width:padded.left,height:padded.bottom-padded.top}}/>
        <div className="guided-tour-shade" style={{top:padded.top,left:padded.right,right:0,height:padded.bottom-padded.top}}/>
        <div className="guided-tour-shade" style={{top:padded.bottom,left:0,right:0,bottom:0}}/>
        <div className="guided-tour-focus" style={{left:padded.left,top:padded.top,width:padded.right-padded.left,height:padded.bottom-padded.top}}/>
      </>}
      <div className={`guided-tour-card${final?" is-final":""}`} style={popupStyle}>
        <div className="guided-tour-top"><span>{final?"Recorrido completo":`Paso ${step!+1} de ${steps.length}`}</span><button ref={button} type="button" className="guided-tour-close" aria-label="Cerrar guía" onClick={finish}>×</button></div>
        <h2 id="guided-tour-title">{content?.title}</h2><p id="guided-tour-description">{content?.description}</p>
        <div className="guided-tour-actions"><button type="button" className="admin-button secondary" onClick={finish}>Omitir</button><button type="button" className="admin-button" onClick={()=>final||step===steps.length-1&&!completion?finish():setStep(step!+1)}>{final?"Aceptar":step===steps.length-1?"Finalizar":"Siguiente →"}</button></div>
      </div>
    </div>}
  </>;
}
