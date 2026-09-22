import { uuidPattern } from "@/lib/organization-types";

const allowed: Record<string, readonly string[]> = {
  "setup/estado": ["GET"], "auth/csrf": ["GET"], "auth/me": ["GET"],
  "setup/propietario": ["POST"], "auth/login": ["POST"], "auth/logout": ["POST"], "auth/registro": ["POST"],
  "admin/sucursales": ["GET", "POST"], "admin/empleados": ["GET", "POST"],
  "admin/catalogo": ["GET"], "admin/catalogo/formatos": ["POST"], "admin/catalogo/papeles": ["POST"], "admin/catalogo/servicios": ["POST"], "admin/catalogo/revisiones": ["POST"],
  "admin/puntos-entrega/disponibilidad": ["GET"],
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
  const pointMethods = configDraft && path[4] === "entrega" && ["puntos", "zonas"].includes(path[5]) ? path.length === 6 ? ["POST"] : path.length === 7 && uuidPattern.test(path[6]) ? ["PUT"] : null : null;
  const activationMethods = configDraft && path.length === 6 && ["activacion","programacion"].includes(path[4]) && ["solicitar","confirmar","revocar"].includes(path[5]) ? ["POST"] : null;
  const reviewMethods = configDraft && path[4] === "revision" ? path.length === 5 ? ["GET"] : path.length === 6 && ["simular","confirmar"].includes(path[5]) ? ["POST"] : null : null;
  const availabilityMethods = path.length === 4 && path[0] === "admin" && path[1] === "puntos-entrega" && path[2] === "disponibilidad" && uuidPattern.test(path[3]) ? ["PUT"] : null;
  const scheduleCancellation = configDraft && path.length === 7 && path[4] === "programacion" && path[5] === "cancelacion" && ["solicitar","confirmar","revocar"].includes(path[6]) ? ["POST"] : null;
  const scheduledReview = configDraft && path.length === 6 && path[4] === "programacion" && path[5] === "revision" ? ["GET"] : null;
  const scheduledActivation = configDraft && path.length === 7 && path[4] === "programacion" && path[5] === "activacion" && ["solicitar","confirmar","revocar"].includes(path[6]) ? ["POST"] : null;
  const historyMethods = path[0]==="admin" && path[1]==="configuracion" && path[2]==="historial" && (path.length===3 || uuidPattern.test(path[3]??"") && (path.length===4 || path.length===5 && path[4]==="auditoria" || path.length===6 && path[4]==="comparacion" && uuidPattern.test(path[5]))) ? ["GET"] : null;
  const copyMethods = path.length===5 && path[0]==="admin" && path[1]==="configuracion" && path[2]==="historial" && uuidPattern.test(path[3]) && path[4]==="base" ? ["POST"] : null;
  const rollbackMethods = path.length===4 && path[0]==="admin" && path[1]==="configuracion" && path[2]==="rollback" ? path[3]==="revision" ? ["GET"] : ["solicitar","confirmar","revocar"].includes(path[3]) ? ["POST"] : null : null;
  const quoteMethods = path[0]==="cliente" && path[1]==="cotizaciones" ? path.length===2 ? ["GET","POST"] : path.length===3 && (path[2]==="opciones"||uuidPattern.test(path[2])) ? ["GET"] : path.length===4 && uuidPattern.test(path[2]) && ["aceptar","cancelar"].includes(path[3]) ? ["POST"] : null : null;
  const paymentRoute=["cliente","operacion"].includes(path[0])&&path[1]==="cotizaciones"&&uuidPattern.test(path[2]??"")&&path[3]==="pagos";
  const paymentMethods=paymentRoute?(path.length===4?["GET"]:path.length===5&&(["descartar"].includes(path[4])||path[0]==="cliente"&&path[4]==="informar"||path[0]==="operacion"&&["recibir","aplicar","devolver"].includes(path[4]))?["POST"]:null):null;
  const paymentQueue=path.length===4&&path[0]==="operacion"&&path[1]==="sucursales"&&uuidPattern.test(path[2])&&path[3]==="pagos"?["GET"]:null;
  const fileRoute = ["cliente","operacion"].includes(path[0]) && path[1]==="cotizaciones" && uuidPattern.test(path[2]??"") && path[3]==="archivos";
  const fileMethods = fileRoute ? path.length===4 ? (path[0]==="cliente"?["GET","POST"]:["GET"]) : uuidPattern.test(path[4]??"") ? path.length===5 ? ["GET"] : path.length===6 && path[5]==="original" ? ["GET"] : path.length===7 && path[5]==="paginas" && /^[1-9][0-9]{0,4}$/.test(path[6]) ? ["GET"] : path.length===6 && path[0]==="cliente" ? path[5]==="contenido" ? ["PUT"] : path[5]==="aceptar" ? ["POST"] : path[5]==="carga" ? ["GET"] : null : null : null : null;
  const binaryUpload=!!fileMethods&&request.method==="PUT";
  const binaryDownload=!!fileMethods&&request.method==="GET"&&(path[5]==="original"||path[5]==="paginas");
  const receivedMethods=path.length===4&&path[0]==="operacion"&&path[1]==="sucursales"&&uuidPattern.test(path[2])&&path[3]==="archivos"?["GET"]:null;
  const methods = paymentMethods ?? paymentQueue ?? receivedMethods ?? fileMethods ?? quoteMethods ?? rollbackMethods ?? copyMethods ?? historyMethods ?? scheduledReview ?? scheduledActivation ?? scheduleCancellation ?? activationMethods ?? reviewMethods ?? availabilityMethods ?? pointMethods ?? deliveryMethods ?? resourceMethods ?? ( (path.length === 2 || path.length === 3) && Object.hasOwn(allowed, route) ? allowed[route]
    : path.length === 3 && uuidPattern.test(path[2]) && path[0] === "admin" && ["empleados", "sucursales"].includes(path[1]) ? ["PUT"]
      : path.length === 3 && uuidPattern.test(path[2]) && path[0] === "operacion" && path[1] === "sucursales" ? ["GET"]
        : path.length === 4 && path[0] === "admin" && path[1] === "catalogo" && path[2] === "revisiones" && uuidPattern.test(path[3]) ? ["GET"] : path.length === 5 && path[0] === "admin" && path[1] === "catalogo" && path[2] === "programaciones" && uuidPattern.test(path[3]) && path[4] === "cancelar" ? ["POST"] : path.length === 5 && path[0] === "admin" && path[1] === "configuracion" && path[2] === "borradores" && uuidPattern.test(path[3]) && ["modelo", "pagos"].includes(path[4]) ? ["PUT"] : path.length === 6 && path[0] === "admin" && path[1] === "configuracion" && path[2] === "borradores" && uuidPattern.test(path[3]) && ((path[4] === "cancelacion" && ["solicitar", "confirmar", "revocar"].includes(path[5])) || (path[4] === "pagos" && path[5] === "simular")) ? ["POST"] : null);
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
    let body:BodyInit|undefined;
    if(binaryUpload){
      if(request.headers.get("Content-Type")?.split(";")[0]!=="application/pdf")return error(415,"Sólo se recibe contenido PDF.");
      if(Number(request.headers.get("Content-Length")??0)>10485760)return error(413,"El PDF supera el máximo de 10 MiB.");
      const check=await fetch(`${base.replace(/\/+$/, "")}/api/${path.slice(0,5).join("/")}/carga`,{headers,cache:"no-store",redirect:"manual",signal:AbortSignal.timeout(8000)});
      if(!check.ok){const data=await check.json().catch(()=>null);return error(check.status,typeof data?.mensaje==="string"?data.mensaje:"No se autorizó la carga.");}
      const gate=await check.json();
      if(gate.habilitada!==true){
        const current=await fetch(`${base.replace(/\/+$/, "")}/api/${path.slice(0,5).join("/")}`,{headers,cache:"no-store",redirect:"manual",signal:AbortSignal.timeout(8000)});
        if(current.ok){const saved=await current.json();if(typeof saved.estado==="string"&&saved.estado!=="PENDIENTE")return Response.json(saved,{headers:{"Cache-Control":"no-store"}});}
        return error(409,typeof gate.motivo==="string"?gate.motivo:"La carga está bloqueada.");
      }
      const reader=request.body?.getReader();if(!reader)return error(400,"Falta el PDF.");const chunks:Uint8Array[]=[];let total=0;
      try{for(;;){const next=await reader.read();if(next.done)break;total+=next.value.length;if(total>10485760){await reader.cancel();return error(413,"El PDF supera el máximo de 10 MiB.");}chunks.push(next.value);}}finally{reader.releaseLock();}
      const bytes=new Uint8Array(total);let offset=0;for(const chunk of chunks){bytes.set(chunk,offset);offset+=chunk.length;}body=bytes;
    }else body=["POST", "PUT"].includes(request.method)?await request.text():undefined;
    const maxBytes = (route === "admin/catalogo/revisiones" || pointMethods || quoteMethods) ? 131072 : 16384;
    if (typeof body==="string" && new TextEncoder().encode(body).length > maxBytes) return error(413, "Los datos enviados son demasiado extensos.");
    const query = historyMethods || quoteMethods || receivedMethods || paymentQueue ? new URL(request.url).search : "";
    const response = await fetch(`${base.replace(/\/+$/, "")}/api/${route}${query}`, {
      method: request.method, headers, body, redirect: "manual", cache: "no-store", signal: AbortSignal.timeout(binaryUpload?90000:binaryDownload?30000:8000),
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
    if(binaryDownload&&response.ok&&["application/pdf","image/png"].includes(contentType??"")){
      resultHeaders.set("X-Content-Type-Options","nosniff");resultHeaders.set("Content-Security-Policy","default-src 'none'; sandbox");
      const disposition=response.headers.get("Content-Disposition");if(disposition)resultHeaders.set("Content-Disposition",disposition);
      return new Response(await response.arrayBuffer(),{status:response.status,headers:resultHeaders});
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
