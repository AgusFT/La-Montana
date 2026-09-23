//#region ENCABEZADO · src/app/cliente/pedidos/[id]/page.tsx
/*
 * ========================================================================
 * ARCHIVO: src/app/cliente/pedidos/[id]/page.tsx
 * ========================================================================
 * FUNCIÓN
 * Valida el identificador del pedido y presenta su detalle y acciones para el cliente dentro de su
 * área protegida.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, default, async] OrderPage({params}: {params:Promise<{id:string}>})
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
import {OrderDetail} from "@/components/order-detail";
import {uuidPattern} from "@/lib/organization-types";
import {notFound} from "next/navigation";
export const dynamic="force-dynamic";
export default async function OrderPage({params}:{params:Promise<{id:string}>}){const{id}=await params;if(!uuidPattern.test(id))notFound();return <ClientPageFrame title="Detalle del pedido" description="Consultá los PDF confirmados, los pagos y la reserva de entrega." active="orders"><OrderDetail id={id}/></ClientPageFrame>;}
