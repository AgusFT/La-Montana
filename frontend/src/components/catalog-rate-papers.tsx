//#region ENCABEZADO · src/components/catalog-rate-papers.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/catalog-rate-papers.tsx
 * ========================================================================
 * FUNCIÓN
 * Selecciona hojas por tarjetas de tamaño y variantes independientes, con búsqueda, selección
 * total, controles por teclado y advertencias de variantes ocupadas o retiradas.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * - [export] pairKey(pair: Compatibility)
 * - normalize(text: string)
 * - [export] CatalogRatePapers({catalog,value,occupied,onChange,label}:
 *   {catalog:CatalogState;value:Compatibility[];occupied:Map<string,string>;onChange:(value:Compatibility[])=>void;label:string})
 * - CatalogRatePapers :: selected(pair: Compatibility)
 * - CatalogRatePapers :: available(pair: PaperSelection)
 * - CatalogRatePapers :: toggle(pairs: PaperSelection[])
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos propios.
 * ========================================================================
 */
//#endregion

"use client";
import {useState} from "react";
import {presetPaperGroups,registeredPaperGroups} from "@/lib/catalog-paper-groups";
import type {CatalogState,Compatibility,PaperSelection} from "@/lib/catalog-types";

export function pairKey(pair:Compatibility){return `${pair.formato}/${pair.papel}`;}
function normalize(text:string){return text.normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase().trim();}

export function CatalogRatePapers({catalog,value,occupied,onChange,label}:{catalog:CatalogState;value:Compatibility[];occupied:Map<string,string>;onChange:(value:Compatibility[])=>void;label:string}){
  const [query,setQuery]=useState("");
  const selectedKeys=new Set(value.map(pairKey));
  const presets=presetPaperGroups(catalog.papelesPredefinidos);
  const order=new Map(presets.map((p,i)=>[p.key,i]));
  const groups=registeredPaperGroups(catalog,presets).sort((a,b)=>(order.get(a.key)??999)-(order.get(b.key)??999)||a.nombre.localeCompare(b.nombre,"es"));
  function selected(pair:Compatibility){return selectedKeys.has(pairKey(pair));}
  function available(pair:PaperSelection){return pair.habilitado&&!occupied.has(pairKey(pair));}
  const eligible=catalog.papelesHabilitados.filter(available);
  function toggle(pairs:PaperSelection[]){
    const usable=pairs.filter(available),all=usable.length>0&&usable.every(selected);
    const keys=new Set(usable.map(pairKey));
    onChange(all?value.filter(p=>!keys.has(pairKey(p))):[...value,...usable.filter(p=>!selected(p)).map(({formato,papel})=>({formato,papel}))]);
  }
  const terms=normalize(query).length>=2?normalize(query).split(/\s+/):[];
  const visible=groups.map(group=>({...group,variantes:group.variantes.filter(pair=>pair.habilitado||selected(pair))})).filter(group=>group.variantes.length&&group.variantes.some(pair=>{
    const paper=catalog.papeles.find(p=>p.codigoPublico===pair.papel),format=catalog.formatos.find(f=>f.codigoPublico===pair.formato);
    const text=normalize(`${group.nombre} ${group.anchoMm} ${group.altoMm} ${format?.codigo} ${paper?.codigo} ${paper?.nombre} ${paper?.gramaje} ${paper?.terminacion}`);
    return terms.every(term=>/^a[0-9]+$/.test(term)||/^[0-9]+$/.test(term)?text.split(/[^a-z0-9]+/).includes(term):text.includes(term));
  }));
  return <div className="rate-paper-selector" role="group" aria-label={label}>
    <div className="rate-paper-toolbar">
      <label>Buscar hojas o variantes<input type="search" value={query} onChange={e=>setQuery(e.target.value)} placeholder="Desde 2 caracteres: A4, 80, mate…"/></label>
      <button type="button" className="admin-button secondary" disabled={!eligible.length||eligible.every(selected)} onClick={()=>onChange([...value,...eligible.filter(p=>!selected(p)).map(({formato,papel})=>({formato,papel}))])}>Seleccionar todas</button>
      <button type="button" className="admin-link-button" disabled={!value.length} onClick={()=>onChange([])}>Quitar selección</button>
    </div>
    <p className="admin-note" aria-live="polite">{value.length} variantes seleccionadas. Seleccionar todas incluye las {eligible.length} disponibles, aunque uses el buscador. Tocá una tarjeta para seleccionar su tamaño completo o una variante para elegirla individualmente.</p>
    <div className="rate-paper-grid">{visible.map(group=>{
      const usable=group.variantes.filter(available),count=group.variantes.filter(selected).length,all=usable.length>0&&usable.every(selected);
      return <article key={group.key} className={`rate-paper-card ${count?"is-selected":""}`}>
        <button type="button" className="rate-paper-heading" aria-label={`Seleccionar ${group.nombre} y sus variantes`} aria-pressed={all?true:count?"mixed":false} disabled={!usable.length} onClick={()=>toggle(group.variantes)}>
          <span className="rate-paper-icon" aria-hidden="true">▤</span><span><strong>{group.nombre}</strong><small>{group.anchoMm} × {group.altoMm} mm</small><span>{count} de {group.variantes.length} variantes seleccionadas</span></span><span aria-hidden="true">{all?"✓":count?"−":"+"}</span>
        </button>
        <div className="rate-paper-variants">{group.variantes.map(pair=>{
          const paper=catalog.papeles.find(p=>p.codigoPublico===pair.papel),key=pairKey(pair),owner=occupied.get(key);
          return <label key={key} className={selected(pair)?"is-selected":""}>
            <input type="checkbox" checked={selected(pair)} disabled={!selected(pair)&&!available(pair)} onChange={e=>onChange(e.target.checked?[...value,{formato:pair.formato,papel:pair.papel}]:value.filter(p=>pairKey(p)!==key))}/>
            <span><strong>{paper?.gramaje} g/m² · {paper?.terminacion}</strong><small>{paper?.nombre} · {paper?.codigo}</small>{owner&&<small className={selected(pair)?"error-message":"rate-paper-assigned"}>Ya incluida en «{owner}» para este color.</small>}{!pair.habilitado&&<small className="error-message">Retirada del catálogo. Quitala de la selección o volvé a habilitarla en Catálogo base.</small>}</span>
          </label>;
        })}</div>
      </article>;
    })}</div>
    {visible.length===0&&<p className="admin-empty">{query?"No hay hojas que coincidan con la búsqueda.":"Habilitá papeles en Catálogo base para crear esta tarifa."}</p>}
  </div>;
}
