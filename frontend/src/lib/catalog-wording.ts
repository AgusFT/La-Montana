/** Terminología de presentación del catálogo; no altera contratos ni registros. */
export function catalogConfigurationText(message:string):string {
  return message.replace(/\b([Rr])evisión\b/g,(_,initial:string)=>initial==="R"?"Configuración":"configuración")
    .replace(/\b([Rr])evisiones\b/g,(_,initial:string)=>initial==="R"?"Configuraciones":"configuraciones");
}
/** En el configurador, conservar «revisión» cuando describe una comprobación. */
export function commercialConfigurationText(message:string):string {
  return message.replace(/\b([Rr])evisión (?=comercial|vigente|programada)/g,(_,initial:string)=>(initial==="R"?"Configuración":"configuración")+" ")
    .replace(/\b([Rr])evisiones (?=comerciales|vigentes|programadas|de precios)/g,(_,initial:string)=>(initial==="R"?"Configuraciones":"configuraciones")+" ");
}
