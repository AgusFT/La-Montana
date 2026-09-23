//#region ENCABEZADO · src/app/cliente/cotizaciones/[id]/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/cliente/cotizaciones/[id]/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Valida el identificador de la cotización y presenta su detalle dentro del marco protegido del
 * cliente.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] QuotePage({params}: {params:Promise<{id:string}>})
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
import {QuoteDetail} from "@/components/quote-detail";
import {uuidPattern} from "@/lib/organization-types";
import {notFound} from "next/navigation";
export const dynamic="force-dynamic";
export default async function QuotePage({params}:{params:Promise<{id:string}>}){const {id}=await params;if(!uuidPattern.test(id))notFound();return <ClientPageFrame title="Detalle de cotización" description="Revisá los trabajos, los importes y las condiciones antes de continuar." active="quotes"><QuoteDetail id={id}/></ClientPageFrame>;}
