import { isCatalogState, type CatalogState } from "@/lib/catalog-types";
export async function readCatalog():Promise<CatalogState>{
  const response=await fetch("/api/admin/catalogo",{cache:"no-store",credentials:"same-origin"});
  if(!response.ok)throw new Error("No pudimos consultar el catálogo vigente.");
  const data:unknown=await response.json();
  if(!isCatalogState(data))throw new Error("La respuesta del catálogo no es válida.");
  return data;
}
