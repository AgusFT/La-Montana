import {ClientPageFrame} from "@/components/client-page-frame";
import {QuoteForm} from "@/components/quote-form";
import {uuidPattern} from "@/lib/organization-types";
import {notFound} from "next/navigation";
export const dynamic="force-dynamic";
export default async function NewQuotePage({searchParams}:{searchParams:Promise<{reemplaza?:string;archivo?:string}>}){const {reemplaza,archivo}=await searchParams;if(reemplaza&&!uuidPattern.test(reemplaza)||archivo&&(!reemplaza||!uuidPattern.test(archivo)))notFound();return <ClientPageFrame title="Preparar un nuevo pedido" description="Seleccioná los PDF y completá las opciones para obtener una cotización." active="new"><QuoteForm replaces={reemplaza} inspectedFile={archivo}/></ClientPageFrame>;}
