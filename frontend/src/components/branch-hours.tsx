import { weekDays, type BranchDay } from "@/lib/organization-types";

export function BranchHours({ days }: { days: BranchDay[] }) {
  return <div className="branch-hours-summary"><h4>Horario habitual de atención</h4>
    {days.length === 0 ? <p>Sin cargar en la ficha. Editá la sucursal para definir sus días y horarios.</p> : <ul>{days.map(d => <li key={d.dia}><strong>{weekDays[d.dia - 1]}</strong><span>{d.habilitado ? `${d.apertura}–${d.cierre}` : "Cerrado"}</span></li>)}</ul>}
    <p className="empty-note">El calendario operativo vigente se consulta en el configurador.</p>
  </div>;
}
