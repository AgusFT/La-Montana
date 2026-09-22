import {ClientPageFrame} from "@/components/client-page-frame";
import {QuoteDetail} from "@/components/quote-detail";
import {uuidPattern} from "@/lib/organization-types";
import {notFound} from "next/navigation";
export const dynamic="force-dynamic";
export default async function QuotePage({params}:{params:Promise<{id:string}>}){const {id}=await params;if(!uuidPattern.test(id))notFound();return <ClientPageFrame title="Detalle de cotización" description="Revisá los trabajos, los importes y las condiciones antes de continuar." active="quotes"><QuoteDetail id={id}/></ClientPageFrame>;}
