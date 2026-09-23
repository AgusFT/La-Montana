//#region ENCABEZADO · src/components/branch-hours.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/branch-hours.tsx
 * ========================================================================
 * FUNCIÓN
 * Muestra el horario de atención declarado por una sucursal, distinguiendo días abiertos, cerrados
 * y ausencia de configuración.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] BranchHours({ days }: { days: BranchDay[] })
 *   Componente de interfaz.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import { weekDays, type BranchDay } from "@/lib/organization-types";

export function BranchHours({ days }: { days: BranchDay[] }) {
  return <div className="branch-hours-summary"><h4>Horario habitual de atención</h4>
    {days.length === 0 ? <p>Sin cargar en la ficha. Editá la sucursal para definir sus días y horarios.</p> : <ul>{days.map(d => <li key={d.dia}><strong>{weekDays[d.dia - 1]}</strong><span>{d.habilitado ? `${d.apertura}–${d.cierre}` : "Cerrado"}</span></li>)}</ul>}
    <p className="empty-note">El calendario operativo vigente se consulta en el configurador.</p>
  </div>;
}
