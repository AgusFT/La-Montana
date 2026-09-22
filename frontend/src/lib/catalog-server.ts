import "server-only";
import { cookies } from "next/headers";
import { isCatalogState, type CatalogState } from "@/lib/catalog-types";
export async function getCatalog(): Promise<CatalogState | null> {
  const base=process.env.BACKEND_INTERNAL_URL;
  if(!base) return null;
  try {
    const response=await fetch(`${base.replace(/\/+$/, "")}/api/admin/catalogo`, {headers:{Cookie:(await cookies()).toString()},cache:"no-store",redirect:"error",signal:AbortSignal.timeout(8000)});
    if(!response.ok) return null;
    const data:unknown=await response.json();
    return isCatalogState(data)?data:null;
  }catch{return null;}
}
