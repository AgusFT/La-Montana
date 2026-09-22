import {ClientPageFrame} from "@/components/client-page-frame";
import {OrderListView} from "@/components/order-list";
export const dynamic="force-dynamic";
export default function OrdersPage(){return <ClientPageFrame title="Mis pedidos" description="Tus trabajos confirmados, su estado y la entrega reservada." active="orders"><OrderListView/></ClientPageFrame>;}
