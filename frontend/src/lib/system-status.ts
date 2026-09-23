import "server-only";

export type SystemStatus = {
  producto: "La Montaña";
  version: "0.1.0";
  etapa: "IDENTIDAD_INICIAL" | "CONFIGURACION_PENDIENTE" | "OPERATIVA";
  accesoDisponible: boolean;
  configuracionDisponible: boolean;
  operacionDisponible: boolean;
};

type StatusResult =
  | { ok: true; data: SystemStatus }
  | { ok: false; reason: "not-configured" | "unavailable" | "invalid-response" };

function isSystemStatus(value: unknown): value is SystemStatus {
  if (typeof value !== "object" || value === null) return false;
  const data = value as Record<string, unknown>;
  return (
    data.producto === "La Montaña" &&
    data.version === "0.1.0" &&
    ["IDENTIDAD_INICIAL","CONFIGURACION_PENDIENTE","OPERATIVA"].includes(String(data.etapa)) &&
    typeof data.accesoDisponible === "boolean" &&
    typeof data.configuracionDisponible === "boolean" &&
    typeof data.operacionDisponible === "boolean"
  );
}

export async function getSystemStatus(): Promise<StatusResult> {
  const backendUrl = process.env.BACKEND_INTERNAL_URL;
  if (!backendUrl) return { ok: false, reason: "not-configured" };

  try {
    const response = await fetch(
      `${backendUrl.replace(/\/+$/, "")}/api/sistema/estado`,
      { cache: "no-store", signal: AbortSignal.timeout(3000), redirect: "error" },
    );
    if (!response.ok) return { ok: false, reason: "unavailable" };
    const data: unknown = await response.json();
    return isSystemStatus(data)
      ? { ok: true, data }
      : { ok: false, reason: "invalid-response" };
  } catch {
    return { ok: false, reason: "unavailable" };
  }
}
