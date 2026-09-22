import {ClientPageFrame} from "@/components/client-page-frame";
import {QuoteListView} from "@/components/quote-list";
export const dynamic="force-dynamic";
export default function ClientPage(){return <ClientPageFrame title="Mi cuenta" description="Prepará tus trabajos y consultá las cotizaciones guardadas de la imprenta." active="home"><QuoteListView/></ClientPageFrame>;}
