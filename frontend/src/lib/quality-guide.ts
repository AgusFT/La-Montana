import type {Quote} from './quote-types';
export type QualityWork=Quote['oferta']['items'][number];
export type QualityRequirements={archivo:string;formato:string;papel:string;paginas:number;copias:number;hojasPorCopia:number;hojasTotales:number;impresion:string;caras:string;adicionales:{servicio:string;nombre:string;base:string;unidades:number}[]};
export function qualityRequirements(item:QualityWork):QualityRequirements{
 const t=item.trabajo;
 return {archivo:item.documento.nombre,formato:item.formato,papel:item.papel,paginas:t.paginas,copias:t.copias,hojasPorCopia:Math.ceil(t.paginas/(t.dobleFaz?2:1)),hojasTotales:item.precio.hojas,impresion:t.color==='COLOR'?'Color':'Blanco y negro',caras:t.dobleFaz?'Doble faz':'Simple faz',adicionales:t.terminaciones.map(id=>{const line=item.precio.lineas.find(l=>l.servicio===id);return {servicio:id,nombre:line?.nombre??'Adicional pendiente de identificar',base:line?.base??'',unidades:line?.unidades??0};})};
}
export function additionalUnits(base:string,units:number){return `${units.toLocaleString('es-AR')} en total · ${base==='POR_COPIA'?'Una por ejemplar':base==='POR_HOJA'?'Aplicación por hoja':base==='POR_CARILLA'?'Aplicación por carilla':'Aplicación al trabajo'}`;}
