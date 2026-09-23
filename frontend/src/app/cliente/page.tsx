import {ClientPageFrame} from "@/components/client-page-frame";
import {ClientDashboard} from "@/components/client-dashboard";
export const dynamic="force-dynamic";
export default function ClientPage(){return <ClientPageFrame title="Mi cuenta" description="Gracias por confiar en La Montaña. Desde aquí podés preparar y seguir tus pedidos." active="home"><ClientDashboard/></ClientPageFrame>;}
