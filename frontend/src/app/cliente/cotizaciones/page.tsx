//#region ENCABEZADO · src/app/cliente/cotizaciones/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/cliente/cotizaciones/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta el listado de cotizaciones dentro de la navegación y verificación de sesión del
 * cliente.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default] QuotesPage()
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
import {QuoteListView} from "@/components/quote-list";
export const dynamic="force-dynamic";
export default function QuotesPage(){return <ClientPageFrame title="Mis cotizaciones" description="Consultá las ofertas, su vigencia y tus decisiones guardadas." active="quotes"><QuoteListView/></ClientPageFrame>;}
