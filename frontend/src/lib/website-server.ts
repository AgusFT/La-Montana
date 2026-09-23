import "server-only";
import {cookies} from "next/headers";
import {isSiteState,isSitePublic} from "./website-types";
async function read<T>(path:string,guard:(value:unknown)=>value is T,privateResource=false):Promise<T|null>{
 const base=process.env.BACKEND_INTERNAL_URL;if(!base)return null;
 try{const r=await fetch(`${base.replace(/\/+$/,"")}/api/${path}`,{headers:privateResource?{Cookie:(await cookies()).toString()}:undefined,cache:"no-store",redirect:"error",signal:AbortSignal.timeout(8000)});if(!r.ok)return null;const value:unknown=await r.json();return guard(value)?value:null;}catch{return null;}
}
export const getWebsiteState=()=>read("admin/pagina-web",isSiteState,true);
export const getPublicWebsite=()=>read("publico/pagina-web",isSitePublic);
