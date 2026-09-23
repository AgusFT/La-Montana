//#region ENCABEZADO · src/lib/point-availability-server.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/point-availability-server.ts
 * ========================================================================
 * FUNCIÓN
 * Consulta desde el servidor Next.js el panel de disponibilidad de puntos de entrega con la sesión
 * actual y valida su estructura.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, async] getPointAvailability(): Promise<PointAvailabilityPanel|null>
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import "server-only";
import {cookies} from "next/headers";
import {isPointAvailabilityPanel,type PointAvailabilityPanel} from "@/lib/point-availability-types";
export async function getPointAvailability():Promise<PointAvailabilityPanel|null>{
  const base=process.env.BACKEND_INTERNAL_URL;if(!base)return null;
  try{const r=await fetch(`${base.replace(/\/+$/, "")}/api/admin/puntos-entrega/disponibilidad`,{headers:{Cookie:(await cookies()).toString()},cache:"no-store",redirect:"error",signal:AbortSignal.timeout(8000)});if(!r.ok)return null;const value:unknown=await r.json();return isPointAvailabilityPanel(value)?value:null;}catch{return null;}
}
