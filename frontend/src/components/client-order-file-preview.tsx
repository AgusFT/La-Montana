"use client";
import {useEffect,useId,useRef,useState} from "react";
import {PrivateFiles} from "./private-files";

/** Read-only PDF consultation; closing the window never changes the order. */
export function ClientOrderFilePreview({quote,version,file}:{quote:string;version:number;file:string}){
 const[open,setOpen]=useState(false),[locked,setLocked]=useState(false);
 const dialog=useRef<HTMLDialogElement>(null),trigger=useRef<HTMLButtonElement>(null),title=useId();
 useEffect(()=>{if(!open||!dialog.current)return;const element=dialog.current,overflow=document.body.style.overflow;document.body.style.overflow="hidden";element.showModal();return()=>{if(element.open)element.close();document.body.style.overflow=overflow;};},[open]);
 function close(){if(locked)return;setOpen(false);requestAnimationFrame(()=>trigger.current?.focus());}
 return <><button ref={trigger} type="button" className="client-text-action" onClick={()=>setOpen(true)}>Ver páginas del PDF</button>{open&&<dialog ref={dialog} className="workspace-review-dialog" aria-labelledby={title} onCancel={e=>{e.preventDefault();close();}}><header><h2 id={title}>Archivo de tu pedido</h2><button autoFocus type="button" className="client-button secondary" disabled={locked} onClick={close}>Volver al pedido ×</button></header><div className="workspace-review-body"><PrivateFiles compact confirmed quote={quote} version={version} snapshot={[file]} initialPreview={file} onLockChange={setLocked}/></div></dialog>}</>;
}
