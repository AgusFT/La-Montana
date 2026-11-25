//#region ENCABEZADO · src/lib/branches-server.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/branches-server.ts
 * ========================================================================
 * FUNCIÓN
 * Reexporta la consulta de sucursales y su tipo para mantener un punto de importación compartido;
 * no declara funciones propias.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - No declara funciones o métodos propios.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - reexportación: export { getBranches } from "@/lib/organization-server";
 * - reexportación: export type { Branch } from "@/lib/organization-types";
 * ========================================================================
 */
//#endregion

export { getBranches } from "@/lib/organization-server";
export type { Branch } from "@/lib/organization-types";
