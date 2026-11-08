'use client';
import {useState} from 'react';
import type {Branch} from '@/lib/organization-types';
import type {PointSource,PointWindow} from '@/lib/point-network';
import {pointWindowsFromBranch,priorityOneBranch} from '@/lib/point-schedule-presets';
import styles from './point-network-workspace.module.css';

export function PointScheduleTools({branches,sources,route,onChange}:{branches:Branch[];sources:PointSource[];route:PointSource;onChange:(windows:PointWindow[])=>void}){
 const [chosen,setChosen]=useState(''),[capacity,setCapacity]=useState(''),[error,setError]=useState(''),[message,setMessage]=useState('');
 const available=branches.filter(b=>sources.some(s=>s.sucursal===b.codigoPublico&&s.habilitado));
 const priority=priorityOneBranch(sources,branches),branch=available.find(b=>b.codigoPublico===chosen)||available.find(b=>b.codigoPublico===priority?.codigoPublico)||available.find(b=>b.codigoPublico===route.sucursal);
 function apply(){
  setError('');setMessage('');
  try{
   if(!branch)throw new Error('Elegí la sucursal de la que querés copiar los horarios.');
   const windows=pointWindowsFromBranch(branch,Number(capacity));onChange(windows);
   setMessage(`Se copiaron ${windows.length} franjas de ${branch.nombre}. Podés ajustar cada una antes de guardar el punto.`);
  }catch(e){setError(e instanceof Error?e.message:'No pudimos copiar los horarios.');}
 }
 return <section className={styles.scheduleTools} aria-label="Copiar horarios de una sucursal">
  <h4>Tomar horarios de una sucursal</h4><p className={styles.muted}>Usá su horario de atención como base y después editá las franjas. Se propone la sucursal con prioridad 1.</p>
  <div className={styles.scheduleFields}>
   <label>Sucursal para copiar<select value={branch?.codigoPublico??''} onChange={e=>{setChosen(e.target.value);setMessage('');setError('');}}><option value="">Elegí una sucursal</option>{available.map(b=><option key={b.codigoPublico} value={b.codigoPublico}>{b.nombre}{b.codigoPublico===priority?.codigoPublico?' · prioridad 1':''}</option>)}</select></label>
   <label>Cupo por franja<input aria-label="Cupo para las franjas copiadas" type="number" min={1} max={1000000} step={1} placeholder="Ej. 10" value={capacity} onChange={e=>{setCapacity(e.target.value);setMessage('');setError('');}}/></label>
   <button type="button" className="admin-button secondary" disabled={!branch} onClick={apply}>{route.franjas.length?'Copiar y reemplazar franjas':'Copiar horarios'}</button>
  </div>
  {!!route.franjas.length&&<p className={styles.muted}>Reemplaza las franjas de esta ruta. Revisá los cambios antes de guardar.</p>}
  {error&&<p className="admin-warning" role="alert">{error}</p>}{message&&<p className="admin-success" role="status">{message}</p>}
 </section>;
}
