"use client";
import {useCallback,useEffect,useId,useRef,useState} from 'react';
import {createPortal} from 'react-dom';
import {PrivateFiles} from './private-files';

/** Keeps the existing page-by-page PDF acceptance in a focused, accessible dialog. */
export function PreparedFileReview({quote,version,accepted,total,optional=false,disabled=false,onChange,onLockChange}:{quote:string;version:number;accepted:number;total:number;optional?:boolean;disabled?:boolean;onChange:()=>void;onLockChange:(locked:boolean)=>void}){
 const [open,setOpen]=useState(false),[locked,setLocked]=useState(false);
 const dialog=useRef<HTMLDialogElement>(null),trigger=useRef<HTMLButtonElement>(null),title=useId();
 const changeLock=useCallback((value:boolean)=>{setLocked(value);onLockChange(value);},[onLockChange]);
 useEffect(()=>{
  if(!open||!dialog.current)return;
  const element=dialog.current,overflow=document.body.style.overflow;
  document.body.style.overflow='hidden';element.showModal();
  return()=>{if(element.open)element.close();document.body.style.overflow=overflow;};
 },[open]);
 function close(){if(!locked){setOpen(false);requestAnimationFrame(()=>trigger.current?.focus());}}
 return <><div className="workspace-review-prompt"><div><strong>{optional?'Vista previa de tus PDF':'Revisión de archivos'}</strong><p>{accepted===total?`${total} de ${total} archivos aceptados.`:`${accepted} de ${total} archivos aceptados. ${optional?'También podés revisarlos en el siguiente paso.':'Abrí cada PDF y revisá todas sus páginas antes de confirmar.'}`}</p></div><button ref={trigger} type="button" className="client-button secondary" disabled={disabled} onClick={()=>setOpen(true)}>{accepted===total?'Ver archivos':'Revisar archivos'} <span aria-hidden="true">↗</span></button></div>
 {open&&createPortal(<dialog ref={dialog} className="workspace-review-dialog" aria-labelledby={title} onCancel={e=>{e.preventDefault();close();}} onClose={()=>{if(!locked)setOpen(false);}}><header><div><h2 id={title}>Revisá tus PDF</h2><p>La aceptación se guarda por archivo. Al terminar, volvés al resumen.</p></div><button autoFocus type="button" className="client-button secondary" disabled={locked} onClick={close}>Volver al {optional?'trabajo':'resumen'} <span aria-hidden="true">×</span></button></header><div className="workspace-review-body"><PrivateFiles prepared compact quote={quote} version={version} onChange={onChange} onLockChange={changeLock}/></div></dialog>,document.body)}
 </>;
}
