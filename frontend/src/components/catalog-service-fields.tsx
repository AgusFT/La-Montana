"use client";
import {useEffect,useId,useRef,useState,type KeyboardEvent} from "react";
import {CodeField} from "@/components/code-field";
import type {CatalogService} from "@/lib/catalog-types";

type Suggestion={nombre:string;codigo:string;tipo:CatalogService["tipo"]};
const suggestions:Suggestion[]=[
  {nombre:"Impresión blanco y negro",codigo:"IMP-BN",tipo:"IMPRESION"},
  {nombre:"Impresión a color",codigo:"IMP-COLOR",tipo:"IMPRESION"},
  {nombre:"Anillado",codigo:"ANILLADO",tipo:"TERMINACION"},
  {nombre:"Plastificado",codigo:"PLASTIFICADO",tipo:"TERMINACION"},
  {nombre:"Encuadernación",codigo:"ENCUADERNACION",tipo:"TERMINACION"},
  {nombre:"Corte y refilado",codigo:"CORTE-REFILADO",tipo:"TERMINACION"},
  {nombre:"Plegado",codigo:"PLEGADO",tipo:"TERMINACION"},
  {nombre:"Abrochado",codigo:"ABROCHADO",tipo:"TERMINACION"},
];
const normalize=(value:string)=>value.normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase().trim();

export function CatalogServiceFields({existingCodes,disabled}:{existingCodes:string[];disabled:boolean}){
  const id=useId(),input=useRef<HTMLInputElement>(null),control=useRef<HTMLDivElement>(null);
  const [name,setName]=useState(""),[code,setCode]=useState(""),[type,setType]=useState<CatalogService["tipo"]|"">("");
  const [open,setOpen]=useState(false),[query,setQuery]=useState(""),[active,setActive]=useState(-1),[selectionNote,setSelectionNote]=useState("");
  const options=suggestions.filter(s=>normalize(`${s.nombre} ${s.codigo}`).includes(normalize(query))),shown=open&&!disabled;
  const listId=`${id}-options`,helpId=`${id}-help`;
  function suggestedCode(base:string){const used=new Set(existingCodes.map(c=>c.trim().toUpperCase()));let result=base,n=2;while(used.has(result))result=`${base}-${n++}`;return result;}
  function choose(s:Suggestion){const nextCode=suggestedCode(s.codigo);setName(s.nombre);setCode(nextCode);setType(s.tipo);setOpen(false);setActive(-1);setSelectionNote(`Completamos el código ${nextCode} y el tipo ${s.tipo==="IMPRESION"?"Impresión":"Terminación"}. Podés editar todos los campos antes de crear el servicio.${nextCode!==s.codigo?" El código habitual ya existe; agregamos un número para distinguirlo.":""}`);}
  function keyboard(event:KeyboardEvent<HTMLInputElement>){
    if(event.key==="ArrowDown"||event.key==="ArrowUp"){
      event.preventDefault();
      if(!shown){setQuery("");setOpen(true);setActive(event.key==="ArrowDown"?0:suggestions.length-1);}
      else if(options.length)setActive(i=>event.key==="ArrowDown"?(i+1)%options.length:(i<0?options.length-1:(i-1+options.length)%options.length));
    }else if(event.key==="Enter"&&shown){event.preventDefault();if(active>=0&&options[active])choose(options[active]);else setOpen(false);}
  }
  useEffect(()=>{
    if(!shown)return;
    function outside(event:PointerEvent){if(event.target instanceof Node&&!control.current?.contains(event.target))setOpen(false);}
    document.addEventListener("pointerdown",outside);return()=>document.removeEventListener("pointerdown",outside);
  },[shown]);
  useEffect(()=>{if(shown&&active>=0)document.getElementById(`${listId}-${active}`)?.scrollIntoView({block:"nearest"});},[active,shown,listId]);
  return <>
    <div className="service-name-field">
      <label htmlFor={id}>Nombre</label>
      <div ref={control} className="service-name-control" onBlur={e=>{if(!e.currentTarget.contains(e.relatedTarget))setOpen(false);}} onKeyDown={e=>{if(e.key==="Escape"&&shown){e.preventDefault();e.stopPropagation();setOpen(false);setActive(-1);}}}>
        <input ref={input} id={id} name="nombre" required maxLength={140} placeholder="Elegí un servicio básico o escribí el tuyo" autoComplete="off"
          role="combobox" aria-autocomplete="list" aria-expanded={shown} aria-controls={listId} aria-describedby={helpId} aria-activedescendant={shown&&active>=0&&options[active]?`${listId}-${active}`:undefined}
          value={name} onFocus={()=>{setQuery("");setActive(-1);setOpen(true);}} onChange={e=>{setName(e.target.value);setQuery(e.target.value);setActive(-1);setOpen(true);setSelectionNote("");}} onKeyDown={keyboard}/>
        <button type="button" tabIndex={-1} className="service-name-toggle" aria-label="Mostrar servicios básicos" aria-expanded={shown} aria-controls={listId} disabled={disabled} onClick={()=>{if(shown)setOpen(false);else{input.current?.focus();setQuery("");setActive(-1);setOpen(true);}}}>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>
        </button>
        {shown&&<div className="service-suggestions" tabIndex={-1}>
          <ul id={listId} role="listbox" aria-label="Servicios básicos">{options.map((s,i)=><li id={`${listId}-${i}`} key={s.codigo} role="option" aria-selected={active===i}
            onPointerDown={e=>e.preventDefault()} onPointerMove={()=>setActive(i)} onClick={()=>choose(s)}>
            <strong>{s.nombre}</strong><span>{suggestedCode(s.codigo)} · {s.tipo==="IMPRESION"?"Impresión":"Terminación"}</span>
          </li>)}</ul>
          {options.length===0&&<p role="status">Sin coincidencias. Podés usar el nombre que escribiste y completar el código y el tipo.</p>}
        </div>}
      </div>
      <small id={helpId}>Elegí una sugerencia para completar el código y el tipo, o escribí un nombre propio. Después podés editar todo. Los precios y modos de color se configuran en Tarifas y servicios.</small>
    </div>
    {selectionNote&&<p className="service-selection-note admin-info" role="status">{selectionNote}</p>}
    <CodeField subject="servicio" maxLength={50} value={code} onValueChange={setCode}>
      <p>Es el identificador único del servicio. Se usa para reconocerlo en el catálogo, al preparar tarifas y servicios ofrecidos.</p>
      <p><strong>Formato recomendado:</strong> una abreviatura clara, como <code>IMP-BN</code>, <code>IMP-COLOR</code> o <code>ANILLADO</code>. Si ya existe, sugerimos un número al final, por ejemplo <code>IMP-BN-2</code>.</p>
      <p>Podés cambiar la sugerencia antes de guardar. Hasta 50 caracteres: letras sin tildes, números, guion o guion bajo, sin espacios. Se guarda en mayúsculas y queda fijo después del alta.</p>
    </CodeField>
    <label>Tipo<select name="tipo" required value={type} onChange={e=>setType(e.target.value as CatalogService["tipo"]|"")}><option value="" disabled>Seleccionar tipo</option><option value="IMPRESION">Impresión</option><option value="TERMINACION">Terminación</option></select></label>
    <label className="service-description">Descripción (opcional)<textarea name="descripcion" maxLength={1000}/></label>
  </>;
}
