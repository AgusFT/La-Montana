export type Vigencia = {
  estado: "VIGENTE" | "RELOJ_ATRASADO" | "VENCIDA" | "NO_DISPONIBLE";
  diasRestantes: number;
  restanteMillis: number;
  mensaje: string;
};

export const sinVigencia: Vigencia = {
  estado: "NO_DISPONIBLE", diasRestantes: 0, restanteMillis: 0,
  mensaje: "No pudimos verificar la vigencia de la Demo. Comprobá que la aplicación local esté en ejecución.",
};

export async function consultarVigencia(url: string, signal: AbortSignal): Promise<Vigencia> {
  try {
    const response = await fetch(url, { cache: "no-store", redirect: "error", signal });
    if (!response.ok) return sinVigencia;
    const data = await response.json();
    if (!["VIGENTE", "RELOJ_ATRASADO", "VENCIDA", "NO_DISPONIBLE"].includes(data?.estado)
        || !Number.isSafeInteger(data.diasRestantes) || data.diasRestantes < 0
        || !Number.isSafeInteger(data.restanteMillis) || data.restanteMillis < 0
        || typeof data.mensaje !== "string"
        || (data.estado === "VIGENTE" && (data.diasRestantes < 1 || data.restanteMillis < 1))) return sinVigencia;
    return data;
  } catch { return sinVigencia; }
}
