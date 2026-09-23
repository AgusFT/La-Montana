//#region ENCABEZADO · src/app/cliente/cotizaciones/nueva/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/cliente/cotizaciones/nueva/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta el formulario de nueva cotización y permite partir de una solicitud de corrección
 * cuando la navegación proporciona ese contexto.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] NewQuotePage({searchParams}:
 *   {searchParams:Promise<{reemplaza?:string;archivo?:string;pedido?:string;solicitud?:string}>})
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
import {QuoteForm} from "@/components/quote-form";
import {uuidPattern} from "@/lib/organization-types";
import {notFound} from "next/navigation";
export const dynamic="force-dynamic";
export default async function NewQuotePage({searchParams}:{searchParams:Promise<{reemplaza?:string;archivo?:string;pedido?:string;solicitud?:string}>}){const {reemplaza,archivo,pedido,solicitud}=await searchParams;if((pedido||solicitud)&&(!pedido||!solicitud||!reemplaza||!uuidPattern.test(pedido)||!uuidPattern.test(solicitud)))notFound();if(reemplaza&&!uuidPattern.test(reemplaza)||archivo&&(!reemplaza||!uuidPattern.test(archivo)))notFound();return <ClientPageFrame title={pedido?"Cotizar una corrección":"Preparar un nuevo pedido"} description="Seleccioná los PDF y completá las opciones para obtener una cotización." active="new"><QuoteForm replaces={reemplaza} inspectedFile={archivo} correction={pedido&&solicitud?{pedido,solicitud}:undefined}/></ClientPageFrame>;}
