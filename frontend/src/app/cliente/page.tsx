//#region ENCABEZADO · src/app/cliente/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/cliente/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta el dashboard del cliente con acceso a sus cotizaciones y pedidos dentro del marco de
 * sesión correspondiente.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default] ClientPage()
 *   Componente de página exportado por la ruta de Next.js.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - dynamic [const, exportado].
 * ========================================================================
 */
//#endregion

import {ClientPageFrame} from "@/components/client-page-frame";
import {ClientDashboard} from "@/components/client-dashboard";
export const dynamic="force-dynamic";
export default function ClientPage(){return <ClientPageFrame title="Mi cuenta" description="Gracias por confiar en La Montaña. Desde aquí podés preparar y seguir tus pedidos." active="home"><ClientDashboard/></ClientPageFrame>;}
