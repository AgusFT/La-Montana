import type {DeliveryLocation,Mode,QuoteOptions} from './quote-types';

export const temporarilyUnavailable='Momentáneamente fuera de servicio';

export function deliveryLocations(options:QuoteOptions|null,mode:Mode,source?:string):DeliveryLocation[]{
 if(!options)return [];
 if(options.destinosEntrega){
  return options.destinosEntrega.filter(d=>d.modalidad===mode).map(d=>{
   if(mode!=='RETIRO_PUNTO_ENTREGA'||!source)return d;
   const sucursales=d.sucursales.filter(s=>s.codigoPublico===source);
   return {...d,sucursales,disponible:d.disponible&&sucursales.some(s=>s.disponible)};
  }).filter(d=>mode!=='RETIRO_PUNTO_ENTREGA'||!source||d.sucursales.length>0);
 }
 if(mode==='RETIRO_SUCURSAL')return (options.destinosRetiro??options.sucursales).map(s=>({...s,modalidad:mode,disponible:true,motivo:null,sucursales:[]}));
 if(mode!=='RETIRO_PUNTO_ENTREGA')return [];
 const points=new Map<string,DeliveryLocation>();
 for(const p of [...options.puntos].filter(p=>!source||p.sucursal===source).sort((a,b)=>a.prioridad-b.prioridad)){
  if(!points.has(p.codigoPublico))points.set(p.codigoPublico,{codigoPublico:p.codigoPublico,nombre:p.nombre,direccion:p.direccion,modalidad:mode,disponible:true,motivo:null,sucursales:[]});
 }
 return [...points.values()];
}

export function deliveryModes(options:QuoteOptions|null):Mode[]{
 return [...new Set([...(options?.modalidades??[]),...(options?.destinosEntrega??[]).map(d=>d.modalidad)])];
}

export function deliverySelectionAvailable(options:QuoteOptions|null,mode:Mode|'',destination:string,point:string):boolean{
 if(!options?.disponible||!mode)return false;
 if(mode==='ENVIO_DOMICILIO')return options.modalidades.includes(mode);
 return deliveryLocations(options,mode).some(d=>d.codigoPublico===(mode==='RETIRO_SUCURSAL'?destination:point)&&d.disponible);
}

export function deliveryLocationLabel(location:DeliveryLocation):string{
 return `${location.nombre}${location.disponible?'':` · ${temporarilyUnavailable}`} · ${location.direccion}`;
}
