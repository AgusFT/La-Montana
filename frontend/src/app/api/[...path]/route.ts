const allowed: Record<string, "GET" | "POST"> = {
  "setup/estado": "GET", "auth/csrf": "GET", "auth/me": "GET",
  "setup/propietario": "POST", "auth/login": "POST", "auth/logout": "POST",
};
export const dynamic = "force-dynamic";

function error(status: number, mensaje: string, headers?: Headers) {
  const resultHeaders = headers ?? new Headers();
  resultHeaders.set("Cache-Control", "no-store");
  resultHeaders.set("Content-Type", "application/json");
  return Response.json({ mensaje }, { status, headers: resultHeaders });
}

async function proxy(request: Request, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  const route = path.join("/");
  if (path.length !== 2 || !Object.hasOwn(allowed, route)) return error(404, "Ruta no disponible.");
  if (allowed[route] !== request.method) return error(405, "Método no permitido.");
  const base = process.env.BACKEND_INTERNAL_URL;
  if (!base) return error(503, "La conexión con el sistema no está configurada.");
  let responseHeaders: Headers | undefined;
  try {
    const headers = new Headers();
    for (const name of ["Cookie", "Content-Type", "X-CSRF-TOKEN"]) {
      const value = request.headers.get(name);
      if (value) headers.set(name, value);
    }
    const body = request.method === "POST" ? await request.text() : undefined;
    if (body && new TextEncoder().encode(body).length > 16384) return error(413, "Los datos enviados son demasiado extensos.");
    const response = await fetch(`${base.replace(/\/+$/, "")}/api/${route}`, {
      method: request.method, headers, body, redirect: "manual", cache: "no-store", signal: AbortSignal.timeout(8000),
    });
    const resultHeaders = new Headers({ "Cache-Control": "no-store" });
    responseHeaders = resultHeaders;
    for (const cookie of response.headers.getSetCookie()) resultHeaders.append("Set-Cookie", cookie);
    const contentType = response.headers.get("Content-Type");
    if (contentType) resultHeaders.set("Content-Type", contentType);
    if (response.status >= 300 && response.status < 400) {
      return error(502, "El sistema respondió con una redirección inesperada.", resultHeaders);
    }
    if (response.status >= 500) return error(response.status === 503 ? 503 : 502, "El sistema no está disponible. Intentá nuevamente.", resultHeaders);
    if (response.status === 204) return new Response(null, { status: 204, headers: resultHeaders });
    if (route === "setup/propietario" && response.status === 201) {
      resultHeaders.set("Content-Type", "application/json");
      return Response.json({ mensaje: "Propietario creado." }, { status: 201, headers: resultHeaders });
    }
    if (!contentType?.includes("application/json")) return error(502, "El sistema respondió con un formato inesperado.", resultHeaders);
    const data = await response.json();
    if (!response.ok) {
      const mensaje = typeof data?.mensaje === "string" ? data.mensaje.slice(0, 300) : "No fue posible completar la operación.";
      // Preserve separate Set-Cookie values, including CSRF/session changes on errors.
      return Response.json({ mensaje }, { status: response.status, headers: resultHeaders });
    }
    return Response.json(data, { status: response.status, headers: resultHeaders });
  } catch { return error(502, "No pudimos conectar con el sistema. Intentá nuevamente.", responseHeaders); }
}

export const GET = proxy;
export const POST = proxy;
