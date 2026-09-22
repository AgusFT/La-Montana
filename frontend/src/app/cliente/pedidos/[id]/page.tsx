import {ClientPageFrame} from "@/components/client-page-frame";
import {OrderDetail} from "@/components/order-detail";
import {uuidPattern} from "@/lib/organization-types";
import {notFound} from "next/navigation";
export const dynamic="force-dynamic";
export default async function OrderPage({params}:{params:Promise<{id:string}>}){const{id}=await params;if(!uuidPattern.test(id))notFound();return <ClientPageFrame title="Detalle del pedido" description="Consultá los PDF confirmados, los pagos y la reserva de entrega." active="orders"><OrderDetail id={id}/></ClientPageFrame>;}
