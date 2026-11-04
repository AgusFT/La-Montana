import {InternalOrderFrame} from '@/components/internal-order-frame';
import {InternalOrderCustomer} from '@/components/internal-order-customer';

export default function InternalOrderPage(){
 return <InternalOrderFrame title="Crear pedido para un cliente"><InternalOrderCustomer/></InternalOrderFrame>;
}
