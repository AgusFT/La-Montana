import {ClientPageFrame} from '@/components/client-page-frame';
import {PreparedCheckout} from '@/components/prepared-checkout';
import {uuidPattern} from '@/lib/organization-types';
import {notFound} from 'next/navigation';
export const dynamic='force-dynamic';
export default async function PreparedWorkPage({params}:{params:Promise<{id:string}>}){const {id}=await params;if(!uuidPattern.test(id))notFound();return <ClientPageFrame title="Revisá y confirmá" description="Tu trabajo, entrega y total final en un mismo resumen." active="new"><PreparedCheckout id={id}/></ClientPageFrame>;}
