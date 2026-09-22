import type {PointSlot} from "@/lib/configuration-types";
export type PickupSlotDraft=Omit<PointSlot,"dia"|"capacidadPedidos"|"habilitada">&{dia:number|null;capacidadPedidos:number|null;habilitada:boolean|null};
const days=["Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"];
export function ConfigurationPickupSlots({branch,slots,onChange}:{branch:string;slots:PickupSlotDraft[];onChange:(slots:PickupSlotDraft[])=>void}){
 function change(index:number,field:Partial<PickupSlotDraft>){onChange(slots.map((s,i)=>i===index?{...s,...field}:s));}
 return <section className="pickup-slots" aria-label={`Franjas de retiro de ${branch}`}><h4>Franjas y cupos de retiro</h4><p className="admin-note">Cada cupo corresponde a un pedido completo. Definí las franjas dentro del horario abierto; cero o deshabilitada no ofrecen plazas. No hay una capacidad predeterminada.</p>
 {!slots.length&&<p className="admin-empty">Sin franjas de retiro configuradas.</p>}
 {slots.map((s,i)=><fieldset className="pickup-slot" key={i}><legend>Franja {i+1} · {branch}</legend>
 <label>Día<select aria-label={`Día de retiro ${i+1} de ${branch}`} required value={s.dia??""} onChange={e=>change(i,{dia:e.target.value?Number(e.target.value):null})}><option value="">Elegir día</option>{days.map((d,n)=><option value={n+1} key={d}>{d}</option>)}</select></label>
 <label>Desde<input aria-label={`Inicio de retiro ${i+1} de ${branch}`} type="time" step="60" required value={s.apertura} onChange={e=>change(i,{apertura:e.target.value})}/></label>
 <label>Hasta<input aria-label={`Fin de retiro ${i+1} de ${branch}`} type="time" step="60" required value={s.cierre} onChange={e=>change(i,{cierre:e.target.value})}/></label>
 <label>Cupo de pedidos<input aria-label={`Cupo de retiro ${i+1} de ${branch}`} type="number" min="0" max="2147483647" step="1" required value={s.capacidadPedidos??""} onChange={e=>change(i,{capacidadPedidos:e.target.value===""?null:Number(e.target.value)})}/></label>
 <label>Estado<select aria-label={`Estado de retiro ${i+1} de ${branch}`} required value={s.habilitada===null?"":s.habilitada?"HABILITADA":"DESHABILITADA"} onChange={e=>change(i,{habilitada:e.target.value===""?null:e.target.value==="HABILITADA"})}><option value="">Elegir estado</option><option value="HABILITADA">Habilitada</option><option value="DESHABILITADA">Deshabilitada</option></select></label>
 <button type="button" className="admin-button secondary" onClick={()=>onChange(slots.filter((_,n)=>n!==i))}>Quitar franja {i+1}</button></fieldset>)}
 <button type="button" className="admin-button secondary" disabled={slots.length>=100} onClick={()=>onChange([...slots,{dia:null,apertura:"",cierre:"",capacidadPedidos:null,habilitada:null}])}>Agregar franja de retiro de {branch}</button></section>;
}
