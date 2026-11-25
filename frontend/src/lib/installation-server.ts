//#region ENCABEZADO · installation-server.ts
/*
 * ========================================================================
 * FUNCIÓN: Carga el avance persistido antes de presentar la navegación.
 * FUNCIONES: getPreparation(), consulta autenticada sin caché y con timeout.
 * ========================================================================
 */
//#endregion
import "server-only";
import {cookies} from "next/headers";
import {isPreparation,type Preparation} from "./installation-types";
export async function getPreparation():Promise<Preparation|null>{
  const base=process.env.BACKEND_INTERNAL_URL;if(!base)return null;
  try{const r=await fetch(`${base.replace(/\/+$/,"")}/api/admin/preparacion`,{headers:{Cookie:(await cookies()).toString()},cache:"no-store",redirect:"error",signal:AbortSignal.timeout(8000)});
    const data:unknown=await r.json();return r.ok&&isPreparation(data)?data:null;
  }catch{return null;}
}
