//#region ENCABEZADO · next.config.ts
/*
 * ========================================================================
 * ARCHIVO: next.config.ts
 * ========================================================================
 * FUNCIÓN
 * Configura Next.js para producir una salida standalone, desactivar la cabecera de identificación
 * del framework y conservar la comprobación de tipos con la API del compilador TypeScript.
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
 * - nextConfig [const].
 * - default: nextConfig
 * ========================================================================
 */
//#endregion

import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  // Keep the compiler API for TypeScript 5; builds still check all types.
  experimental: { useTypeScriptCli: false },
};

export default nextConfig;
