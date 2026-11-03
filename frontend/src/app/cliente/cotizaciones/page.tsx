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

import {redirect} from "next/navigation";

export default function QuotesPage(){redirect("/cliente/pedidos?vista=cotizaciones");}
