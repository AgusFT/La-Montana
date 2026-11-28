//#region ENCABEZADO · src/lib/secure-mutation.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/secure-mutation.ts
 * ========================================================================
 * FUNCIÓN
 * Prepara el token CSRF y envía escrituras autenticadas a la API, representando los errores HTTP y
 * las respuestas inciertas con MutationError. Notifica escrituras confirmadas para actualizar
 * los requisitos de la instalación; los errores no producen confirmación visual.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - MutationError :: constructor(message: string, status: number, uncertain = false)
 *   Representa un error HTTP y si el resultado del envío es incierto.
 * - [export, async] secureMutation(path: string, body?: BodyInit, contentType?: string, method:
 *   "POST" | "PUT" = "POST")
 *   Obtiene CSRF, envía la escritura y comprueba la respuesta.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - MutationError (clase).
 * ========================================================================
 */
//#endregion

import {installationChanged} from "./installation-types";

export class MutationError extends Error {
  constructor(message: string, readonly status: number, readonly uncertain = false, readonly codigo?: string) { super(message); this.name = "MutationError"; }
}

function actualizarVigenciaSiBloqueada(status: number) {
  if ((status === 423 || status === 503) && typeof window !== "undefined") window.dispatchEvent(new Event("demo:verificar-vigencia"));
}

export async function secureMutation(path: string, body?: BodyInit, contentType?: string, method: "POST" | "PUT" | "DELETE" = "POST") {
  const csrfResponse = await fetch("/api/auth/csrf", { cache: "no-store", credentials: "same-origin" }).catch(() => {
    throw new Error("No pudimos conectar con la imprenta. Comprobá que esté en funcionamiento y volvé a intentar.");
  });
  if (!csrfResponse.ok) {
    actualizarVigenciaSiBloqueada(csrfResponse.status);
    throw new MutationError("No pudimos preparar una operación segura. Intentá nuevamente.", csrfResponse.status);
  }
  const csrf = await csrfResponse.json().catch(() => { throw new Error("No pudimos leer la respuesta de la imprenta. Volvé a intentar."); });
  if (typeof csrf?.token !== "string" || typeof csrf.headerName !== "string" || csrf.headerName.toUpperCase() !== "X-CSRF-TOKEN") throw new MutationError("No pudimos preparar la sesión. Volvé a intentar.", 503);
  const response = await fetch(path, {
    method, credentials: "same-origin", cache: "no-store",
    headers: { "X-CSRF-TOKEN": csrf.token, ...(contentType ? { "Content-Type": contentType } : {}) }, body,
  }).catch(() => {
    throw new MutationError("Se interrumpió la conexión con la imprenta y no pudimos confirmar el resultado. Cuando vuelva la conexión, reintentá la misma operación.", 0, true);
  });
  if (!response.ok) {
    actualizarVigenciaSiBloqueada(response.status);
    const data = await response.json().catch(() => null);
    throw new MutationError(typeof data?.mensaje === "string" ? data.mensaje : "No fue posible completar la operación.", response.status, response.status >= 500, data?.codigo);
  }
  if(typeof window!=="undefined"&&path.startsWith("/api/admin/")&&!path.endsWith("/simular"))window.dispatchEvent(new Event(installationChanged));
  if(typeof window!=="undefined" && /^\/api\/(cliente|operacion)\/(preparaciones|pedidos|cotizaciones)(\/|$)/.test(path))window.dispatchEvent(new CustomEvent("demo:tutorial:operacion",{detail:{path}}));
  return response;
}
