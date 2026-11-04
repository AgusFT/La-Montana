import {notFound} from 'next/navigation';
import {InternalOrderFrame} from '@/components/internal-order-frame';
import {PrepareWork} from '@/components/quote-form';
import {PreparedCheckout} from '@/components/prepared-checkout';
import {uuidPattern} from '@/lib/organization-types';

export default async function InternalPreparationPage({params}:{params:Promise<{id:string;etapa:string}>}){
 const {id,etapa}=await params;
 if(!uuidPattern.test(id)||!['trabajo','resumen'].includes(etapa))notFound();
 return <InternalOrderFrame title={etapa==='trabajo'?'Prepará el pedido':'Revisá y confirmá el pedido'}>
  {etapa==='trabajo'?<PrepareWork key={id} internalId={id}/>:<PreparedCheckout key={id} id={id} internal/>}
 </InternalOrderFrame>;
}
