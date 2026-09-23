//#region ENCABEZADO · src/app/cliente/pedidos/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/cliente/pedidos/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Presenta el listado de pedidos del cliente dentro de su navegación protegida.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default] OrdersPage()
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
import {OrderListView} from "@/components/order-list";
export const dynamic="force-dynamic";
export default function OrdersPage(){return <ClientPageFrame title="Mis pedidos" description="Tus trabajos confirmados, su estado y la entrega reservada." active="orders"><OrderListView/></ClientPageFrame>;}
