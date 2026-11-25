"use client";
import {useEffect,useState} from "react";
/** El modo viene del servidor; no se infiere de la URL ni de una falla de Internet. */
export function EntornoAviso(){
  const [modo,setModo]=useState<string|null>(null);
  useEffect(()=>{const abort=new AbortController();fetch("/api/sistema/entorno",{cache:"no-store",signal:abort.signal}).then(r=>r.ok?r.json():null).then(v=>setModo(v?.modo??null)).catch(()=>{});return()=>abort.abort();},[]);
  if(modo!=="DESARROLLO")return null;
  return <aside className="entorno-desarrollo" role="note"><strong>Modo Desarrollo.</strong> Datos de prueba. El correo se consulta en Mailpit; las integraciones simuladas no realizan operaciones reales.</aside>;
}
