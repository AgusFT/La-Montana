import "server-only";

export type SystemStatus = {
  producto: "La Montaña";
  version: "0.1.0";
  etapa: "IDENTIDAD_INICIAL";
  accesoDisponible: true;
  configuracionDisponible: false;
  operacionDisponible: false;
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
    data.etapa === "IDENTIDAD_INICIAL" &&
    data.accesoDisponible === true &&
    data.configuracionDisponible === false &&
    data.operacionDisponible === false
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
