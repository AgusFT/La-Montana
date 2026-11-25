//#region ENCABEZADO · src/lib/website-server.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/website-server.ts
 * ========================================================================
 * FUNCIÓN
 * Consulta el borrador privado y la publicación pública del sitio desde Next.js, reenviando
 * cookies únicamente para recursos privados y validando las respuestas.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [async] read<T>(path: string, guard: (value:unknown)=>value is T, privateResource = false):
 *   Promise<T|null>
 * - getWebsiteState()
 * - getPublicWebsite()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 * ========================================================================
 */
//#endregion

import "server-only";
import {cookies} from "next/headers";
import {isSiteState,isSitePublic} from "./website-types";
async function read<T>(path:string,guard:(value:unknown)=>value is T,privateResource=false):Promise<T|null>{
 const base=process.env.BACKEND_INTERNAL_URL;if(!base)return null;
 try{const r=await fetch(`${base.replace(/\/+$/,"")}/api/${path}`,{headers:privateResource?{Cookie:(await cookies()).toString()}:undefined,cache:"no-store",redirect:"error",signal:AbortSignal.timeout(8000)});if(!r.ok)return null;const value:unknown=await r.json();return guard(value)?value:null;}catch{return null;}
}
export const getWebsiteState=()=>read("admin/pagina-web",isSiteState,true);
export const getPublicWebsite=()=>read("publico/pagina-web",isSitePublic);
