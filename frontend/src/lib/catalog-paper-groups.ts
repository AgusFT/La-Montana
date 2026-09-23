//#region ENCABEZADO · src/lib/catalog-paper-groups.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/catalog-paper-groups.ts
 * ========================================================================
 * FUNCIÓN
 * Agrupa papeles por dimensiones para presentar sus gramajes como variantes, conservando las
 * identidades y la selección independiente de cada combinación.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - groupBySize<T>(items: SizedPaper<T>[]): PaperGroup<T>[]
 * - [export] presetPaperGroups(presets: PaperPreset[])
 * - [export] registeredPaperGroups(catalog: CatalogState, presets: PaperGroup<PaperPreset>[])
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - SizedPaper (tipo).
 * - PaperGroup (tipo).
 * ========================================================================
 */
//#endregion

import type {CatalogState,PaperPreset,PaperSelection} from "@/lib/catalog-types";

type SizedPaper<T>={nombre:string;anchoMm:number;altoMm:number;value:T};
export type PaperGroup<T>={key:string;nombre:string;anchoMm:number;altoMm:number;variantes:T[]};
function groupBySize<T>(items:SizedPaper<T>[]):PaperGroup<T>[]{
  const groups=new Map<string,PaperGroup<T>>();
  for(const {nombre,anchoMm,altoMm,value} of items){
    const key=`${anchoMm}/${altoMm}`;
    let group=groups.get(key);
    if(!group){group={key,nombre,anchoMm,altoMm,variantes:[]};groups.set(key,group);}
    group.variantes.push(value);
  }
  return [...groups.values()];
}
export function presetPaperGroups(presets:PaperPreset[]){
  return groupBySize(presets.map(p=>({nombre:p.nombre.split(" · ")[0],anchoMm:p.anchoMm,altoMm:p.altoMm,value:p})))
    .map(group=>({...group,variantes:group.variantes.sort((a,b)=>a.gramaje-b.gramaje)}));
}
export function registeredPaperGroups(catalog:CatalogState,presets:PaperGroup<PaperPreset>[]){
  const items:SizedPaper<PaperSelection>[]=[];
  for(const selection of catalog.papelesHabilitados){
    const format=catalog.formatos.find(f=>f.codigoPublico===selection.formato);
    if(!format)continue;
    const preset=presets.find(p=>p.anchoMm===format.anchoMm&&p.altoMm===format.altoMm);
    items.push({nombre:preset?.nombre??format.nombre,anchoMm:format.anchoMm,altoMm:format.altoMm,value:selection});
  }
  return groupBySize(items).map(group=>({...group,variantes:group.variantes.sort((a,b)=>{
    const first=catalog.papeles.find(p=>p.codigoPublico===a.papel),second=catalog.papeles.find(p=>p.codigoPublico===b.papel);
    return (first?.gramaje??0)-(second?.gramaje??0)||(first?.nombre??"").localeCompare(second?.nombre??"","es");
  })}));
}
