//#region ENCABEZADO · src/lib/catalog-client.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/catalog-client.ts
 * ========================================================================
 * FUNCIÓN
 * Consulta el catálogo desde el navegador usando la API de la misma aplicación y valida la
 * estructura de la respuesta antes de usarla.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, async] readCatalog(): Promise<CatalogState>
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import { isCatalogState, type CatalogState } from "@/lib/catalog-types";
export async function readCatalog():Promise<CatalogState>{
  const response=await fetch("/api/admin/catalogo",{cache:"no-store",credentials:"same-origin"});
  if(!response.ok)throw new Error("No pudimos consultar el catálogo vigente.");
  const data:unknown=await response.json();
  if(!isCatalogState(data))throw new Error("La respuesta del catálogo no es válida.");
  return data;
}
