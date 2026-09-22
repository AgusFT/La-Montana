import {ClientPageFrame} from "@/components/client-page-frame";
import {QuoteListView} from "@/components/quote-list";
export const dynamic="force-dynamic";
export default function QuotesPage(){return <ClientPageFrame title="Mis cotizaciones" description="Consultá las ofertas, su vigencia y tus decisiones guardadas." active="quotes"><QuoteListView/></ClientPageFrame>;}
