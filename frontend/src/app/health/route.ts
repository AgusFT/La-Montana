//#region ENCABEZADO · src/app/health/route.ts
/*
 * ========================================================================
 * ARCHIVO: src/app/health/route.ts
 * ========================================================================
 * FUNCIÓN
 * Expone la respuesta HTTP de salud del proceso frontend sin caché. Esta ruta no comprueba por sí
 * misma el estado del backend ni de la base de datos.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] GET()
 *   Respuesta de salud del proceso frontend.
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

export const dynamic = "force-dynamic";

export function GET() {
  return Response.json({ status: "ok" }, {
    headers: { "Cache-Control": "no-store" },
  });
}
