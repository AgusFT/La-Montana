import { createClient } from "npm:@supabase/supabase-js@2";

import {
  ApiError,
  errorResponse,
  methodNotAllowedResponse,
  optionsResponse,
  parseJsonBody,
  resolveRequestId,
  successResponse,
} from "../_shared/api-response.ts";

type CuerpoCotizarPedido = Record<string, unknown>;

interface LineaCotizacionPedido {
  id_servicio: number;
  codigo_servicio: string;
  nombre: string;
  tipo: "impresion" | "adicional";
  cantidad: number;
  precio_unitario: number;
  subtotal: number;
  tipo_moneda: "ARS" | "USD";
}

interface CotizacionPedido {
  cantidad_estimada: number;
  requiere_senia: boolean;
  porcentaje_senia: number;
  total_estimado: number;
  tipo_moneda: "ARS" | "USD";
  lineas: LineaCotizacionPedido[];
}

interface SupabaseErrorDebug {
  code?: string;
  message?: string;
  details?: unknown;
  hint?: string;
}

const TAMANOS_HOJA_PERMITIDOS = new Set(["A4", "A3", "OFICIO"]);
const TIPOS_IMPRESION_PERMITIDOS = new Set(["byn", "color"]);

Deno.serve(async (request) => {
  const requestId = resolveRequestId(request);

  try {
    if (request.method === "OPTIONS") {
      return optionsResponse(requestId);
    }

    if (request.method !== "POST") {
      return methodNotAllowedResponse(requestId);
    }

    const token = obtenerBearerToken(request);
    const supabase = crearClienteSupabaseAutenticado(token);

    const { data: usuarioAuth, error: errorUsuarioAuth } =
      await supabase.auth.getUser(token);

    if (errorUsuarioAuth || !usuarioAuth.user) {
      throw new ApiError({ code: "UNAUTHENTICATED" });
    }

    const cuerpo = await parseJsonBody<CuerpoCotizarPedido>(request);
    const pedido = normalizarCuerpoCotizarPedido(cuerpo, requestId);

    const { data: cotizacionRpc, error } = await supabase.rpc(
      "calcular_cotizacion_pedido_cliente",
      {
        p_cantidad_carillas: pedido.cantidadCarillas,
        p_cantidad_copias: pedido.cantidadCopias,
        p_tamano_hoja: pedido.tamanoHoja,
        p_tipo_impresion: pedido.tipoImpresion,
        p_doble_faz: pedido.dobleFaz,
        p_encuadernado: pedido.encuadernado,
        p_anillado: pedido.anillado,
      },
    );
    const data = cotizacionRpc as CotizacionPedido | null;

    if (error) {
      throw mapearErrorSupabase(error);
    }

    if (!data) {
      throw new ApiError({ code: "INTERNAL_ERROR" });
    }

    return successResponse(data, requestId);
  } catch (error) {
    return errorResponse(error, requestId, { includeDetails: true });
  }
});

function crearClienteSupabaseAutenticado(token: string) {
  const supabaseUrl = obtenerVariableEntorno("SUPABASE_URL");
  const supabaseAnonKey = obtenerVariableEntorno("SUPABASE_ANON_KEY", [
    "SUPABASE_PUBLISHABLE_KEY",
  ]);

  return createClient(supabaseUrl, supabaseAnonKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
    global: {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  });
}

function obtenerVariableEntorno(
  nombre: string,
  alternativas: string[] = [],
): string {
  for (const variable of [nombre, ...alternativas]) {
    const valor = Deno.env.get(variable);

    if (valor) {
      return valor;
    }
  }

  throw new ApiError({ code: "INTERNAL_ERROR" });
}

function obtenerBearerToken(request: Request): string {
  const authorization = request.headers.get("authorization")?.trim() ?? "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);

  if (!match?.[1]) {
    throw new ApiError({ code: "UNAUTHENTICATED" });
  }

  return match[1];
}

function normalizarCuerpoCotizarPedido(
  cuerpo: CuerpoCotizarPedido,
  requestId: string,
) {
  if (!cuerpo || Array.isArray(cuerpo) || typeof cuerpo !== "object") {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { cuerpo: "El cuerpo debe ser un objeto JSON.", request_id: requestId },
      exposeDetails: true,
    });
  }

  const cantidadCarillas = leerEnteroPositivo(
    cuerpo,
    ["cantidad_carillas", "pages"],
    "cantidad_carillas",
  );
  const cantidadCopias = leerEnteroPositivo(
    cuerpo,
    ["cantidad_copias", "copies"],
    "cantidad_copias",
  );
  const tamanoHoja = leerTextoRequerido(
    cuerpo,
    ["tamano_hoja", "paperSize"],
    "tamano_hoja",
  ).toUpperCase();
  const tipoImpresion = leerTextoRequerido(
    cuerpo,
    ["tipo_impresion", "printType"],
    "tipo_impresion",
  ).toLowerCase();

  if (!TAMANOS_HOJA_PERMITIDOS.has(tamanoHoja)) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { tamano_hoja: "Debe ser A4, A3 u OFICIO." },
      exposeDetails: true,
    });
  }

  if (!TIPOS_IMPRESION_PERMITIDOS.has(tipoImpresion)) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { tipo_impresion: "Debe ser byn o color." },
      exposeDetails: true,
    });
  }

  return {
    cantidadCarillas,
    cantidadCopias,
    tamanoHoja,
    tipoImpresion,
    dobleFaz: leerBooleano(cuerpo, ["doble_faz", "doubleSided"], false),
    encuadernado: leerBooleano(cuerpo, ["encuadernado", "bound"], false),
    anillado: leerBooleano(cuerpo, ["anillado", "spiralBound"], false),
  };
}

function leerEnteroPositivo(
  cuerpo: CuerpoCotizarPedido,
  claves: string[],
  campo: string,
): number {
  const valor = leerValor(cuerpo, claves);

  if (!Number.isInteger(valor) || (valor as number) <= 0) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { [campo]: "Debe ser un entero mayor a cero." },
      exposeDetails: true,
    });
  }

  return valor as number;
}

function leerTextoRequerido(
  cuerpo: CuerpoCotizarPedido,
  claves: string[],
  campo: string,
): string {
  const valor = leerValor(cuerpo, claves);

  if (typeof valor !== "string" || valor.trim() === "") {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { [campo]: "Debe ser un texto no vacio." },
      exposeDetails: true,
    });
  }

  return valor.trim();
}

function leerBooleano(
  cuerpo: CuerpoCotizarPedido,
  claves: string[],
  valorDefault: boolean,
): boolean {
  const valor = leerValor(cuerpo, claves);

  if (valor === undefined || valor === null) {
    return valorDefault;
  }

  if (typeof valor !== "boolean") {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { [claves[0]]: "Debe ser booleano." },
      exposeDetails: true,
    });
  }

  return valor;
}

function leerValor(cuerpo: CuerpoCotizarPedido, claves: string[]): unknown {
  for (const clave of claves) {
    if (Object.prototype.hasOwnProperty.call(cuerpo, clave)) {
      return cuerpo[clave];
    }
  }

  return undefined;
}

function mapearErrorSupabase(error: SupabaseErrorDebug): ApiError {
  const details = {
    codigo_db: error.code ?? null,
    mensaje_db: error.message ?? null,
    detalle_db: error.details ?? null,
    sugerencia_db: error.hint ?? null,
  };

  if (error.code === "42501") {
    return new ApiError({
      code: "FORBIDDEN",
      details,
      exposeDetails: true,
    });
  }

  if (error.code === "23514" || error.code === "22P02") {
    return new ApiError({
      code: "VALIDATION_ERROR",
      details,
      exposeDetails: true,
    });
  }

  return new ApiError({
    code: "INTERNAL_ERROR",
    details,
    exposeDetails: true,
  });
}
