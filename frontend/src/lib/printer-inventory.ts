import type {ConfiguredPrinter} from './configuration-types';
import type {Branch} from './organization-types';

export function printerInventory(draft:ConfiguredPrinter[],active:ConfiguredPrinter[],branches:Pick<Branch,'codigoPublico'|'nombre'>[]){
 const ids=new Set(draft.map(p=>p.codigoPublico));
 const missing=active.filter(p=>p.estado!=='RETIRADA'&&!ids.has(p.codigoPublico));
 const rows=[...draft.map(printer=>({printer,pending:false})),...missing.map(printer=>({printer,pending:true}))];
 const groups=branches.map(b=>({id:b.codigoPublico,name:b.nombre,
  active:active.filter(p=>p.sucursal===b.codigoPublico&&p.estado!=='RETIRADA').length,
  draft:draft.filter(p=>p.sucursal===b.codigoPublico&&p.estado!=='RETIRADA').length,
  missing:missing.filter(p=>p.sucursal===b.codigoPublico).length}));
 if(rows.some(r=>!r.printer.sucursal))groups.push({id:'',name:'Sin sucursal asignada',active:active.filter(p=>!p.sucursal&&p.estado!=='RETIRADA').length,draft:draft.filter(p=>!p.sucursal&&p.estado!=='RETIRADA').length,missing:missing.filter(p=>!p.sucursal).length});
 return {rows,missing,groups};
}
