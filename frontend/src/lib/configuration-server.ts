import "server-only";
import {cookies} from "next/headers";
import {isConfigurationState,type ConfigurationState} from "@/lib/configuration-types";
export async function getConfiguration():Promise<ConfigurationState|null>{
  const base=process.env.BACKEND_INTERNAL_URL;if(!base)return null;
  try{const response=await fetch(`${base.replace(/\/+$/,"")}/api/admin/configuracion`,{headers:{Cookie:(await cookies()).toString()},cache:"no-store",redirect:"error",signal:AbortSignal.timeout(8000)});
    if(!response.ok)return null;const data:unknown=await response.json();return isConfigurationState(data)?data:null;
  }catch{return null;}
}
