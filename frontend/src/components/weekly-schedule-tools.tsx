/* Editor por grupos: las propuestas sólo se aplican al pulsar el botón. */
"use client";
import {useId,useState} from "react";
import {validWeeklyHours} from "@/lib/schedule-presets";
import "@/app/weekly-schedule-tools.css";
const labels=["Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"];
export function WeeklyScheduleTools({kind,onApply}:{kind:"production"|"pickup";onApply:(days:number[],start:string,end:string,closeOthers:boolean,capacity:number)=>void}){
 const id=useId(),[days,setDays]=useState([1,2,3,4,5]),[start,setStart]=useState("09:00"),[end,setEnd]=useState("18:00"),[closeOthers,setCloseOthers]=useState(true),[capacity,setCapacity]=useState("");
 const pickup=kind==="pickup",valid=validWeeklyHours(days,start,end)&&(!pickup||/^[1-9]\d*$/.test(capacity)&&Number.isSafeInteger(Number(capacity))&&Number(capacity)<=2147483647);
 return <details className="weekly-schedule-tools"><summary>Configurar varios días juntos · {pickup?"retiros":"producción"}</summary><p className="admin-note">Propuesta editable: lunes a viernes, de 09:00 a 18:00. No se aplica hasta que pulses el botón. {pickup?"Los retiros tienen su propio horario y pueden atender fuera del horario de producción. Se reemplazan las franjas de los días elegidos; las de otros días se conservan.":"Después podés ajustar cada día por separado."}</p>
  <div className="configuration-actions"><button type="button" className="admin-button secondary" onClick={()=>setDays([1,2,3,4,5])}>Lunes a viernes</button><button type="button" className="admin-button secondary" onClick={()=>setDays([1,2,3,4,5,6])}>Lunes a sábado</button></div>
  <fieldset className="weekly-schedule-days"><legend>Días de {pickup?"retiro":"producción"}</legend>{labels.map((label,index)=><label className="admin-check" key={label}><input type="checkbox" checked={days.includes(index+1)} onChange={e=>setDays(values=>e.target.checked?[...values,index+1].sort((a,b)=>a-b):values.filter(d=>d!==index+1))}/>{label}</label>)}</fieldset>
  <div className="admin-form-grid"><label htmlFor={id+"-start"}>Desde<input id={id+"-start"} type="time" step="60" value={start} onChange={e=>setStart(e.target.value)}/></label><label htmlFor={id+"-end"}>Hasta<input id={id+"-end"} type="time" step="60" value={end} onChange={e=>setEnd(e.target.value)}/></label>{pickup&&<label htmlFor={id+"-capacity"}>Cupo de pedidos por franja<input id={id+"-capacity"} type="number" min="1" max="2147483647" step="1" value={capacity} onChange={e=>setCapacity(e.target.value)}/></label>}</div>
  {!pickup&&<label className="admin-check"><input type="checkbox" checked={closeOthers} onChange={e=>setCloseOthers(e.target.checked)}/>Marcar como producción cerrada los días no elegidos</label>}
  <button type="button" className="admin-button secondary" disabled={!valid} onClick={()=>onApply(days,start,end,closeOthers,Number(capacity))}>Aplicar horario a los días elegidos</button><p className="admin-note">Esta acción prepara los cambios en pantalla. Guardá la fase para conservarlos; entrarán en vigencia al activar la configuración.</p>
 </details>;
}
