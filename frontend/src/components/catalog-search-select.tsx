"use client";
import {useEffect,useId,useRef,useState,type KeyboardEvent} from "react";

export type CatalogChoice={value:string;label:string;detail?:string;search?:string};
const normalize=(text:string)=>text.normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase().trim();

/** El texto busca opciones; sólo una selección explícita cambia el identificador. */
export function CatalogSearchSelect({label,options,value,onChange,disabled=false,placeholder="Buscar o elegir",help="Escribí 2 o más caracteres para filtrar. Elegí un resultado de la lista."}:{label:string;options:CatalogChoice[];value:string;onChange:(value:string)=>void;disabled?:boolean;placeholder?:string;help?:string}){
  const id=useId(),root=useRef<HTMLDivElement>(null),input=useRef<HTMLInputElement>(null);
  const [open,setOpen]=useState(false),[query,setQuery]=useState(""),[active,setActive]=useState(-1);
  const chosen=options.find(option=>option.value===value),shown=open&&!disabled;
  const words=normalize(query).split(/\s+/),filtering=normalize(query).length>=2;
  const matches=filtering?options.filter(o=>{const text=normalize(`${o.label} ${o.detail??""} ${o.search??""}`);return words.every(word=>/^a[0-9]+$/.test(word)||/^[0-9]+$/.test(word)?text.split(/[^a-z0-9]+/).includes(word):text.includes(word));}):options;
  function show(){setQuery("");setActive(-1);setOpen(true);}
  function choose(option:CatalogChoice){onChange(option.value);setOpen(false);setActive(-1);}
  function keyboard(event:KeyboardEvent<HTMLInputElement>){
    if(event.key==="ArrowDown"||event.key==="ArrowUp"){
      event.preventDefault();if(!shown){show();setActive(event.key==="ArrowDown"?0:options.length-1);}
      else if(matches.length)setActive(i=>event.key==="ArrowDown"?(i+1)%matches.length:(i<=0?matches.length-1:i-1));
    }else if(event.key==="Enter"&&shown){event.preventDefault();if(active>=0&&matches[active])choose(matches[active]);}
    else if(event.key==="Escape"&&shown){event.preventDefault();event.stopPropagation();setOpen(false);}
    else if(event.key==="Tab")setOpen(false);
  }
  useEffect(()=>{if(!shown)return;function outside(event:PointerEvent){if(event.target instanceof Node&&!root.current?.contains(event.target))setOpen(false);}document.addEventListener("pointerdown",outside);return()=>document.removeEventListener("pointerdown",outside);},[shown]);
  useEffect(()=>{if(shown&&active>=0)document.getElementById(`${id}-option-${active}`)?.scrollIntoView({block:"nearest"});},[shown,active,id]);
  return <div className="catalog-search-field">
    <label htmlFor={id}>{label}</label>
    <div className="catalog-search-control" ref={root} onBlur={e=>{if(!e.currentTarget.contains(e.relatedTarget))setOpen(false);}}>
      <input ref={input} id={id} role="combobox" required disabled={disabled} autoComplete="off" spellCheck={false} placeholder={placeholder} value={shown?query:chosen?.label??""}
        aria-expanded={shown} aria-autocomplete="list" aria-controls={`${id}-list`} aria-describedby={`${id}-help`} aria-activedescendant={shown&&matches[active]?`${id}-option-${active}`:undefined}
        onFocus={show} onChange={e=>{setQuery(e.target.value);setActive(-1);setOpen(true);}} onKeyDown={keyboard}/>
      <button type="button" tabIndex={-1} className="catalog-search-toggle" disabled={disabled} aria-label={`Mostrar opciones: ${label}`} aria-expanded={shown} onClick={()=>{if(shown)setOpen(false);else{input.current?.focus();show();}}}>⌄</button>
      {shown&&<div className="catalog-search-popup" tabIndex={-1}>
        <p className="catalog-search-count" role="status">{filtering?`${matches.length} coincidencias`:query?"Escribí un carácter más para filtrar":`${matches.length} opciones · buscá por nombre o código`}</p>
        <ul id={`${id}-list`} role="listbox" aria-label={`Opciones: ${label}`}>{matches.map((option,i)=><li key={option.value} id={`${id}-option-${i}`} role="option" aria-selected={value===option.value} data-active={active===i} onPointerDown={e=>e.preventDefault()} onClick={()=>choose(option)}>
          <strong>{option.label}{value===option.value&&<span aria-hidden="true"> ✓</span>}</strong>{option.detail&&<span>{option.detail}</span>}
        </li>)}</ul>
        {!matches.length&&<p className="catalog-search-empty">No hay coincidencias. Probá otro nombre, tamaño, gramaje o código. Las opciones se crean en Catálogo base.</p>}
      </div>}
    </div>
    <small id={`${id}-help`}>{shown&&chosen?`Selección actual: ${chosen.label}. `:""}{help}</small>
    {chosen?.detail&&!shown&&<small className="catalog-choice-detail">{chosen.detail}</small>}
  </div>;
}
