import type {Order} from './order-types';
import type {Production} from '@/components/order-production';

export type WorkStatus={code:'loading'|'prepare'|'printing'|'quality'|'reprint'|'incident'|'approved';label:string;tone:'neutral'|'warning'|'danger'|'success';date?:string};
export function productionWorkStatus(item:Production['items'][number]|undefined,expectedFile?:string):WorkStatus{
 if(!item)return {code:'loading',label:'Consultando trabajo…',tone:'neutral'};
 const job=item.trabajos.at(-1);
 if(!job||expectedFile&&job.archivo!==expectedFile)return {code:'prepare',label:'Falta imprimir',tone:'warning'};
 if(job.calidad?.resultado==='REIMPRESION_REQUERIDA')return {code:'reprint',label:'Requiere reimpresión',tone:'danger',date:job.calidad.fecha};
 if(job.calidad?.resultado==='INCIDENCIA'||['ERROR','CANCELADO'].includes(job.estado))return {code:'incident',label:'Corregir este trabajo',tone:'danger',date:job.calidad?.fecha??job.fin??job.inicio};
 if(job.estado==='COMPLETADO'&&job.calidad?.resultado==='APROBADO')return {code:'approved',label:'Calidad aprobada',tone:'success',date:job.calidad.fecha};
 if(job.estado==='IMPRIMIENDO')return {code:'printing',label:'Impresión iniciada · falta calidad',tone:'warning',date:job.inicio};
 if(job.estado==='COMPLETADO')return {code:'quality',label:'Falta aprobar calidad',tone:'warning',date:job.fin??undefined};
 return {code:'prepare',label:'Falta imprimir',tone:'warning'};
}
export function productionWorkProgress(order:Pick<Order,'version'|'oferta'|'archivos'>,production:Production|null){
 const known=!!production&&production.version===order.version;
 const rows=order.oferta.items.map((item,index)=>({id:item.codigoPublico,index,name:item.documento.nombre,work:item,status:productionWorkStatus(known?production.items.find(i=>i.codigoPublico===item.codigoPublico):undefined,order.archivos.find(f=>f.item===item.codigoPublico)?.archivo)}));
 const approved=rows.filter(r=>r.status.code==='approved').length;
 return {known,rows,total:rows.length,approved,pending:rows.length-approved,complete:known&&rows.length>0&&approved===rows.length};
}
