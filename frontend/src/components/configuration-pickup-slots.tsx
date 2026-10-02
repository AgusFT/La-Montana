//#region ENCABEZADO · src/components/configuration-pickup-slots.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/configuration-pickup-slots.tsx
 * ========================================================================
 * FUNCIÓN
 * Edita las franjas de retiro por sucursal con días, horas, capacidad y habilitación explícita.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] ConfigurationPickupSlots({branch,slots,operatingDays,onChange}:
 *   {branch:string;slots:PickupSlotDraft[];operatingDays:OperatingDay[];onChange:(slots:PickupSlotDraft[])=>void})
 *   Componente de interfaz.
 * - ConfigurationPickupSlots :: change(index: number, field: Partial<PickupSlotDraft>)
 * - ConfigurationPickupSlots :: outsideProduction(slot: PickupSlotDraft)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PickupSlotDraft (tipo).
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - days [const].
 * ========================================================================
 */
//#endregion

import type {OperatingDay,PointSlot} from "@/lib/configuration-types";
export type PickupSlotDraft=Omit<PointSlot,"dia"|"capacidadPedidos"|"habilitada">&{dia:number|null;capacidadPedidos:number|null;habilitada:boolean|null};
const days=["Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"];
export function ConfigurationPickupSlots({branch,slots,operatingDays,onChange}:{branch:string;slots:PickupSlotDraft[];operatingDays:OperatingDay[];onChange:(slots:PickupSlotDraft[])=>void}){
 function change(index:number,field:Partial<PickupSlotDraft>){onChange(slots.map((s,i)=>i===index?{...s,...field}:s));}
 function outsideProduction(slot:PickupSlotDraft){
  if(!slot.habilitada||!(slot.capacidadPedidos!==null&&slot.capacidadPedidos>0)||slot.dia===null||!slot.apertura||!slot.cierre||slot.apertura>=slot.cierre)return false;
  const day=operatingDays.find(d=>d.dia===slot.dia);
  return day?.habilitado===false||day?.habilitado===true&&!!day.apertura&&!!day.cierre&&(slot.apertura<day.apertura||slot.cierre>day.cierre);
 }
 return <section className="pickup-slots" aria-label={`Franjas de retiro de ${branch}`}><h4>Franjas y cupos de retiro</h4><p className="admin-note">Estas franjas son para retirar pedidos listos. Pueden estar fuera del horario de producción, incluso en días sin producción, si hay personal para entregar. No suman horas de preparación. Cada cupo corresponde a un pedido completo; cero o deshabilitada no ofrecen plazas. La disponibilidad se verifica al confirmar el pedido.</p>
 {!slots.length&&<p className="admin-empty">Sin franjas de retiro configuradas.</p>}
 {slots.map((s,i)=><fieldset className="pickup-slot" key={i}><legend>Franja {i+1} · {branch}</legend>
 <label>Día<select aria-label={`Día de retiro ${i+1} de ${branch}`} required value={s.dia??""} onChange={e=>change(i,{dia:e.target.value?Number(e.target.value):null})}><option value="">Elegir día</option>{days.map((d,n)=><option value={n+1} key={d}>{d}</option>)}</select></label>
 <label>Desde<input aria-label={`Inicio de retiro ${i+1} de ${branch}`} type="time" step="60" required value={s.apertura} onChange={e=>change(i,{apertura:e.target.value})}/></label>
 <label>Hasta<input aria-label={`Fin de retiro ${i+1} de ${branch}`} type="time" step="60" required value={s.cierre} onChange={e=>change(i,{cierre:e.target.value})}/></label>
 <label>Cupo de pedidos<input aria-label={`Cupo de retiro ${i+1} de ${branch}`} type="number" min="0" max="2147483647" step="1" required value={s.capacidadPedidos??""} onChange={e=>change(i,{capacidadPedidos:e.target.value===""?null:Number(e.target.value)})}/></label>
 <label>Estado<select aria-label={`Estado de retiro ${i+1} de ${branch}`} required value={s.habilitada===null?"":s.habilitada?"HABILITADA":"DESHABILITADA"} onChange={e=>change(i,{habilitada:e.target.value===""?null:e.target.value==="HABILITADA"})}><option value="">Elegir estado</option><option value="HABILITADA">Habilitada</option><option value="DESHABILITADA">Deshabilitada</option></select></label>
 <button type="button" className="admin-button secondary" onClick={()=>onChange(slots.filter((_,n)=>n!==i))}>Quitar franja {i+1}</button>
 {outsideProduction(s)&&<p className="admin-info" role="status" style={{gridColumn:"1 / -1"}}><strong>Producción cerrada · retiros habilitados</strong> en parte o toda esta franja. Confirmá que haya personal para entregar pedidos listos. Este horario no se cuenta para preparación.</p>}
 </fieldset>)}
 <button type="button" className="admin-button secondary" disabled={slots.length>=100} onClick={()=>onChange([...slots,{dia:null,apertura:"",cierre:"",capacidadPedidos:null,habilitada:null}])}>Agregar franja de retiro de {branch}</button></section>;
}
