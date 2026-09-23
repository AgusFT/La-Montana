//#region ENCABEZADO · src/lib/secure-mutation.ts
/*
 * ========================================================================
 * ARCHIVO: src/lib/secure-mutation.ts
 * ========================================================================
 * FUNCIÓN
 * Prepara el token CSRF y envía escrituras autenticadas a la API, representando los errores HTTP y
 * las respuestas inciertas con MutationError.
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

export class MutationError extends Error {
  constructor(message: string, readonly status: number, readonly uncertain = false) { super(message); this.name = "MutationError"; }
}

export async function secureMutation(path: string, body?: BodyInit, contentType?: string, method: "POST" | "PUT" = "POST") {
  const csrfResponse = await fetch("/api/auth/csrf", { cache: "no-store", credentials: "same-origin" });
  if (!csrfResponse.ok) throw new MutationError("No pudimos preparar una operación segura. Intentá nuevamente.", csrfResponse.status);
  const csrf = await csrfResponse.json();
  if (typeof csrf.token !== "string" || csrf.headerName?.toUpperCase() !== "X-CSRF-TOKEN") throw new MutationError("La protección de sesión no está disponible.", 503);
  const response = await fetch(path, {
    method, credentials: "same-origin", cache: "no-store",
    headers: { "X-CSRF-TOKEN": csrf.token, ...(contentType ? { "Content-Type": contentType } : {}) }, body,
  });
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new MutationError(typeof data?.mensaje === "string" ? data.mensaje : "No fue posible completar la operación.", response.status, response.status >= 500);
  }
  return response;
}
