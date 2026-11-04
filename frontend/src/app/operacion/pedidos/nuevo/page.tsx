import {InternalOrderFrame} from '@/components/internal-order-frame';
import {InternalOrderCustomer} from '@/components/internal-order-customer';
import {getOperationContext} from '@/lib/organization-server';

export default async function InternalOrderPage({searchParams}:{searchParams:Promise<{sucursal?:string}>}){
 const {sucursal}=await searchParams;
 const branches=(await getOperationContext())?.sucursales??[];
 const initial=branches.find(b=>b.codigoPublico===sucursal)??branches.find(b=>b.casaMatrizVigente)??(branches.length===1?branches[0]:undefined);
 return <InternalOrderFrame title="Crear pedido">
  <InternalOrderCustomer branches={branches} initialBranch={initial?.codigoPublico??''}/>
 </InternalOrderFrame>;
}
