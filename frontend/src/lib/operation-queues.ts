/** Shared by the dashboard's four work panels and the internal order list. */
export const operationQueues=[
 {state:'PENDIENTE_REVISION',title:'Pendientes de revisión',tone:'blue',symbol:'▤'},
 {state:'APROBADO',title:'Aprobados',tone:'green',symbol:'✓'},
 {state:'EN_PRODUCCION',title:'En producción',tone:'blue',symbol:'⚙'},
 {state:'LISTO_PARA_ENTREGA',title:'Entregas pendientes',tone:'purple',symbol:'↗'},
] as const;
