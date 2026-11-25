//#region ENCABEZADO · src/components/client-journey.tsx
/* FUNCIÓN: Mantiene visibles las tres partes del recorrido original. FUNCIÓN: ClientJourney. */
//#endregion
export function ClientJourney({part}:{part:1|2|3}){
  return <nav className="client-journey" aria-label="Pasos para crear y seguir tu pedido"><ol>{["Prepará tu trabajo","Revisá y confirmá","Seguí tu pedido"].map((label,i)=><li key={label} aria-current={part===i+1?"step":undefined} data-complete={part>i+1}><span aria-hidden="true">{part>i+1?"✓":i+1}</span><strong>{label}</strong></li>)}</ol></nav>;
}
