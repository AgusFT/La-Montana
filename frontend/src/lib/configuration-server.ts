//#region ENCABEZADO · src/lib/configuration-server.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/configuration-server.ts
 * ========================================================================
 * FUNCIÓN
 * Obtiene desde el servidor Next.js el estado del configurador operativo, reenviando la sesión y
 * comprobando la respuesta del backend.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export, async] getConfiguration(): Promise<ConfigurationState|null>
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import "server-only";
import {cookies} from "next/headers";
import {isConfigurationState,type ConfigurationState} from "@/lib/configuration-types";
export async function getConfiguration():Promise<ConfigurationState|null>{
  const base=process.env.BACKEND_INTERNAL_URL;if(!base)return null;
  try{const response=await fetch(`${base.replace(/\/+$/,"")}/api/admin/configuracion`,{headers:{Cookie:(await cookies()).toString()},cache:"no-store",redirect:"error",signal:AbortSignal.timeout(8000)});
    if(!response.ok)return null;const data:unknown=await response.json();return isConfigurationState(data)?data:null;
  }catch{return null;}
}
