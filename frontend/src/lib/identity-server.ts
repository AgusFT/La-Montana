import "server-only";
import { cookies } from "next/headers";

export type SetupState = { requierePropietario: boolean; altaHabilitada: boolean };
export type Owner = { codigoPublico: string; nombre: string; apellido: string; correo: string; rol: "ADMIN_ADMIN" };

async function getIdentityResource(path: "setup/estado" | "auth/me", cookie?: string) {
  const base = process.env.BACKEND_INTERNAL_URL;
  if (!base) return null;
  try {
    return await fetch(`${base.replace(/\/+$/, "")}/api/${path}`, {
      headers: cookie ? { Cookie: cookie } : undefined,
      cache: "no-store", signal: AbortSignal.timeout(8000), redirect: "error",
    });
  } catch { return null; }
}

export async function getSetupState(): Promise<SetupState | null> {
  try {
    const response = await getIdentityResource("setup/estado");
    if (!response?.ok) return null;
    const data = await response.json();
    return typeof data?.requierePropietario === "boolean" && typeof data?.altaHabilitada === "boolean"
      ? { requierePropietario: data.requierePropietario, altaHabilitada: data.altaHabilitada }
      : null;
  } catch { return null; }
}

export async function getOwner(): Promise<{ state: "authenticated"; owner: Owner } | { state: "anonymous" | "unavailable" }> {
  try {
    const response = await getIdentityResource("auth/me", (await cookies()).toString());
    if (response?.status === 401) return { state: "anonymous" };
    if (!response?.ok) return { state: "unavailable" };
    const data = await response.json();
    if (data?.rol !== "ADMIN_ADMIN" || ![data.codigoPublico, data.nombre, data.apellido, data.correo].every(v => typeof v === "string")) {
      return { state: "unavailable" };
    }
    return { state: "authenticated", owner: data as Owner };
  } catch { return { state: "unavailable" }; }
}
