import "server-only";
import { cookies } from "next/headers";
import { isBranchLocation, type BranchLocation, isBranch, isEmployee, isPermissions, uuidPattern, type Employee, type Branch, type OperationContext } from "@/lib/organization-types";

async function getResource(path: string): Promise<unknown> {
  const base = process.env.BACKEND_INTERNAL_URL;
  if (!base) return null;
  try {
    const response = await fetch(`${base.replace(/\/+$/, "")}/api/${path}`, {
      headers: { Cookie: (await cookies()).toString() }, cache: "no-store",
      signal: AbortSignal.timeout(8000), redirect: "error",
    });
    return response.ok ? await response.json() : null;
  } catch { return null; }
}
export async function getBranchLocations(): Promise<BranchLocation[] | null> {
  const data = await getResource("admin/sucursales/ubicaciones");
  return Array.isArray(data) && data.length > 0 && data.every(isBranchLocation) ? data : null;
}
export async function getBranches(): Promise<Branch[] | null> {
  const data = await getResource("admin/sucursales");
  return Array.isArray(data) && data.every(isBranch) ? data : null;
}
export async function getEmployees(): Promise<Employee[] | null> {
  const data = await getResource("admin/empleados");
  return Array.isArray(data) && data.every(isEmployee) ? data : null;
}
export async function getOperationContext(): Promise<OperationContext | null> {
  const data = await getResource("operacion/contexto");
  if (!data || typeof data !== "object") return null;
  const context = data as Record<string, unknown>;
  return Array.isArray(context.sucursales) && context.sucursales.every(isBranch) && isPermissions(context.permisos)
    ? { sucursales: context.sucursales, permisos: context.permisos } : null;
}
export type OperationBranchResult = { state: "found"; branch: Branch } | { state: "forbidden" | "not-found" | "unavailable" };
export async function getOperationBranch(id: string): Promise<OperationBranchResult> {
  if (!uuidPattern.test(id)) return { state: "not-found" };
  const base = process.env.BACKEND_INTERNAL_URL;
  if (!base) return { state: "unavailable" };
  try {
    const response = await fetch(`${base.replace(/\/+$/, "")}/api/operacion/sucursales/${id}`, {
      headers: { Cookie: (await cookies()).toString() }, cache: "no-store",
      signal: AbortSignal.timeout(8000), redirect: "error",
    });
    if (response.status === 403) return { state: "forbidden" };
    if (response.status === 404) return { state: "not-found" };
    if (!response.ok) return { state: "unavailable" };
    const data: unknown = await response.json();
    return isBranch(data) ? { state: "found", branch: data } : { state: "unavailable" };
  } catch { return { state: "unavailable" }; }
}
