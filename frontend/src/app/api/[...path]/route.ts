import { uuidPattern } from "@/lib/organization-types";

const allowed: Record<string, readonly string[]> = {
  "setup/estado": ["GET"], "auth/csrf": ["GET"], "auth/me": ["GET"],
  "setup/propietario": ["POST"], "auth/login": ["POST"], "auth/logout": ["POST"], "auth/registro": ["POST"],
  "admin/sucursales": ["GET", "POST"], "admin/empleados": ["GET", "POST"],
  "admin/catalogo": ["GET"], "admin/catalogo/formatos": ["POST"], "admin/catalogo/papeles": ["POST"], "admin/catalogo/servicios": ["POST"], "admin/catalogo/revisiones": ["POST"],
  "admin/configuracion": ["GET"], "admin/configuracion/borradores": ["POST"],
  "operacion/contexto": ["GET"], "auth/contrasena": ["POST"],
  "auth/correo/solicitar": ["POST"], "auth/correo/confirmar": ["POST"],
  "auth/recuperacion/solicitar": ["POST"], "auth/recuperacion/confirmar": ["POST"],
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
  const configDraft = path[0] === "admin" && path[1] === "configuracion" && path[2] === "borradores" && uuidPattern.test(path[3] ?? "");
  const resourceMethods = configDraft && path.length === 5 && ["recursos", "entrega"].includes(path[4]) ? ["PUT"]
    : configDraft && path.length === 5 && path[4] === "impresoras" ? ["POST"]
    : configDraft && path[4] === "impresoras" && uuidPattern.test(path[5] ?? "") && path.length === 6 ? ["PUT"]
    : configDraft && path[4] === "impresoras" && uuidPattern.test(path[5] ?? "") && path.length === 7 && ["estado", "retirar"].includes(path[6]) ? ["POST"] : null;
  const deliveryMethods = configDraft && path.length === 6 && path[4] === "entrega" ? path[5] === "validacion" ? ["GET"] : path[5] === "simular" ? ["POST"] : null : null;
  const pointMethods = configDraft && path[4] === "entrega" && path[5] === "puntos" ? path.length === 6 ? ["POST"] : path.length === 7 && uuidPattern.test(path[6]) ? ["PUT"] : null : null;
  const methods = pointMethods ?? deliveryMethods ?? resourceMethods ?? ( (path.length === 2 || path.length === 3) && Object.hasOwn(allowed, route) ? allowed[route]
    : path.length === 3 && uuidPattern.test(path[2]) && path[0] === "admin" && ["empleados", "sucursales"].includes(path[1]) ? ["PUT"]
      : path.length === 3 && uuidPattern.test(path[2]) && path[0] === "operacion" && path[1] === "sucursales" ? ["GET"]
        : path.length === 4 && path[0] === "admin" && path[1] === "catalogo" && path[2] === "revisiones" && uuidPattern.test(path[3]) ? ["GET"] : path.length === 5 && path[0] === "admin" && path[1] === "catalogo" && path[2] === "programaciones" && uuidPattern.test(path[3]) && path[4] === "cancelar" ? ["POST"] : path.length === 5 && path[0] === "admin" && path[1] === "configuracion" && path[2] === "borradores" && uuidPattern.test(path[3]) && ["modelo", "pagos"].includes(path[4]) ? ["PUT"] : path.length === 6 && path[0] === "admin" && path[1] === "configuracion" && path[2] === "borradores" && uuidPattern.test(path[3]) && ((path[4] === "cancelacion" && ["solicitar", "confirmar"].includes(path[5])) || (path[4] === "pagos" && path[5] === "simular")) ? ["POST"] : null);
  if (!methods) return error(404, "Ruta no disponible.");
  if (!methods.includes(request.method)) return error(405, "Método no permitido.");
  const base = process.env.BACKEND_INTERNAL_URL;
  if (!base) return error(503, "La conexión con el sistema no está configurada.");
  let responseHeaders: Headers | undefined;
  try {
    const headers = new Headers();
    for (const name of ["Cookie", "Content-Type", "X-CSRF-TOKEN"]) {
      const value = request.headers.get(name);
      if (value) headers.set(name, value);
    }
    const body = ["POST", "PUT"].includes(request.method) ? await request.text() : undefined;
    const maxBytes = (route === "admin/catalogo/revisiones" || pointMethods) ? 131072 : 16384;
    if (body && new TextEncoder().encode(body).length > maxBytes) return error(413, "Los datos enviados son demasiado extensos.");
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
    if (["setup/propietario", "auth/registro"].includes(route) && response.status === 201) {
      resultHeaders.set("Content-Type", "application/json");
      return Response.json({ mensaje: route === "auth/registro" ? "Cuenta de cliente creada." : "Propietario creado." }, { status: 201, headers: resultHeaders });
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

export const PUT = proxy;
