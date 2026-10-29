"use client";
import {useCallback,useEffect,useId,useRef,useState} from 'react';
import {createPortal} from 'react-dom';
import {PrivateFiles} from './private-files';

/** Optional PDF consultation, collapsed by default, with an accessible dialog. */
export function PreparedFileReview({quote,version,total,optional=false,disabled=false,onChange,onLockChange}:{quote:string;version:number;total:number;optional?:boolean;disabled?:boolean;onChange:()=>void;onLockChange:(locked:boolean)=>void}){
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
 return <><details className="workspace-file-details"><summary><strong>Archivos adjuntos</strong><span>{total} PDF · Vista previa opcional</span></summary><div className="workspace-review-prompt"><p>Abrí el visor para consultar el contenido o descargar tus PDF antes de enviar el pedido.</p><button ref={trigger} type="button" className="client-button secondary" disabled={disabled} onClick={()=>setOpen(true)}>Ver mis PDF <span aria-hidden="true">↗</span></button></div></details>
 {open&&createPortal(<dialog ref={dialog} className="workspace-review-dialog" aria-labelledby={title} onCancel={e=>{e.preventDefault();close();}} onClose={()=>{if(!locked)setOpen(false);}}><header><div><h2 id={title}>Tus PDF</h2><p>Consultá el contenido cuando quieras. Se imprimirá la versión que cargaste.</p></div><button autoFocus type="button" className="client-button secondary" disabled={locked} onClick={close}>Volver al {optional?'trabajo':'resumen'} <span aria-hidden="true">×</span></button></header><div className="workspace-review-body"><PrivateFiles prepared compact quote={quote} version={version} onChange={onChange} onLockChange={changeLock}/></div></dialog>,document.body)}
 </>;
}
