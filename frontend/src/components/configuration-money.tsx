//#region ENCABEZADO · src/components/configuration-money.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/configuration-money.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta entradas financieras en pesos argentinos, con símbolo y moneda, teclado decimal,
 * ejemplos y formato de miles al terminar de editar.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * - ConfigurationMoney({id,label,value,onChange,help}:
 * {id:string;label:string;value:string;onChange:(value:string)=>void;help:string})
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos propios.
 * VALORES DE MÓDULO
 * ========================================================================
 */
//#endregion

"use client";
import {parsePesos,pesosDraft,pesosPattern} from "@/lib/argentine-money";
export function ConfigurationMoney({id,label,value,onChange,help}:{id:string;label:string;value:string;onChange:(value:string)=>void;help:string}){
  return <div className="catalog-field"><label htmlFor={id}>{label} (ARS)</label><div className="configuration-money"><span aria-hidden="true">$ ARS</span><input id={id} type="text" inputMode="decimal" required pattern={pesosPattern} value={value} placeholder="1.500,50" aria-describedby={`${id}-help`} title="Pesos argentinos. Ejemplo: 1.500,50 o 1500,50; coma para los centavos." onChange={event=>onChange(event.target.value)} onBlur={()=>{try{onChange(pesosDraft(parsePesos(value,label)));}catch{/* Keep invalid text so it can be corrected. */}}}/></div><small id={`${id}-help`}>{help} Pesos argentinos: punto para miles y coma para centavos. Ejemplo: $ 1.500,50.</small></div>;
}
