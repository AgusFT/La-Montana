//#region ENCABEZADO · src/app/web/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/web/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Consulta el contenido publicado y el estado de instalación para mostrar la página pública de la
 * imprenta o su estructura todavía sin configurar.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] WebsitePage()
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

import {getSetupState} from "@/lib/identity-server";
import {getPublicWebsite} from "@/lib/website-server";
import {WebsiteView} from "@/components/website-view";
import "./web.css";
export const dynamic="force-dynamic";
export default async function WebsitePage(){const[setup,site]=await Promise.all([getSetupState(),getPublicWebsite()]);return <WebsiteView content={site?.contenido??null} unavailable={!site} requiresOwner={setup?.requierePropietario}/>;}
