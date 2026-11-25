//#region ENCABEZADO · src/lib/catalog-wording.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/catalog-wording.ts
 * ========================================================================
 * FUNCIÓN
 * Adapta mensajes del catálogo a la terminología configuración comercial y conserva revisión
 * cuando se refiere a una comprobación, sin alterar contratos ni registros.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] catalogConfigurationText(message: string): string
 * - [export] commercialConfigurationText(message: string): string
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

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
