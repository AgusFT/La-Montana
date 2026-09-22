import "server-only";
import { cookies } from "next/headers";

export type Branch = {
  codigoPublico: string; codigo: string; nombre: string; calle: string; numero: string;
  localidad: string; provincia: string; codigoPostal: string; zonaHoraria: string;
  correo: string | null; telefono: string | null; estado: string;
};

export async function getBranches(): Promise<Branch[] | null> {
  const base = process.env.BACKEND_INTERNAL_URL;
  if (!base) return null;
  try {
    const response = await fetch(`${base.replace(/\/+$/, "")}/api/admin/sucursales`, {
      headers: { Cookie: (await cookies()).toString() }, cache: "no-store",
      signal: AbortSignal.timeout(8000), redirect: "error",
    });
    if (!response.ok) return null;
    const data: unknown = await response.json();
    if (!Array.isArray(data) || !data.every(item => item &&
      ["codigoPublico", "codigo", "nombre", "calle", "numero", "localidad", "provincia", "codigoPostal", "zonaHoraria", "estado"].every(key => typeof item[key] === "string") &&
      ["correo", "telefono"].every(key => item[key] === null || typeof item[key] === "string"))) return null;
    return data as Branch[];
  } catch { return null; }
}
