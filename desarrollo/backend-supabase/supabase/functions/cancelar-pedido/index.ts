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

type CuerpoCancelarPedido = Record<string, unknown>;

interface PedidoCancelado {
  id_pedido: number;
  codigo: string;
  estado_visible_cliente: "cancelado";
  estado_interno: "cancelado";
  estado_financiero: "cancelado";
  fecha_cancelacion: string | null;
  cancelado: boolean;
}

interface SupabaseErrorDebug {
  code?: string;
  message?: string;
  details?: unknown;
  hint?: string;
}

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

    const { data: usuarioAuth, error: errorUsuarioAuth } = await supabase.auth
      .getUser(token);

    if (errorUsuarioAuth || !usuarioAuth.user) {
      throw new ApiError({ code: "UNAUTHENTICATED" });
    }

    const cuerpo = await parseJsonBody<CuerpoCancelarPedido>(request);
    const pedido = normalizarCuerpoCancelarPedido(cuerpo);

    const { data: pedidoCanceladoRpc, error } = await supabase.rpc(
      "cancelar_pedido_cliente",
      {
        p_id_pedido: pedido.idPedido,
        p_motivo: pedido.motivo,
        p_request_id: requestId,
      },
    );
    const data = pedidoCanceladoRpc as PedidoCancelado | null;

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

function normalizarCuerpoCancelarPedido(cuerpo: CuerpoCancelarPedido) {
  if (!cuerpo || Array.isArray(cuerpo) || typeof cuerpo !== "object") {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { cuerpo: "El cuerpo debe ser un objeto JSON." },
      exposeDetails: true,
    });
  }

  return {
    idPedido: leerEnteroPositivo(cuerpo, ["id_pedido", "orderId"], "id_pedido"),
    motivo: leerTextoOpcional(cuerpo, ["motivo", "reason"], 300),
  };
}

function leerEnteroPositivo(
  cuerpo: CuerpoCancelarPedido,
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

function leerTextoOpcional(
  cuerpo: CuerpoCancelarPedido,
  claves: string[],
  maximo: number,
): string | null {
  const valor = leerValor(cuerpo, claves);

  if (valor === undefined || valor === null || valor === "") {
    return null;
  }

  if (typeof valor !== "string") {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { motivo: "Debe ser texto." },
      exposeDetails: true,
    });
  }

  const normalizado = valor.trim();

  if (!normalizado) {
    return null;
  }

  return normalizado.slice(0, maximo);
}

function leerValor(cuerpo: CuerpoCancelarPedido, claves: string[]): unknown {
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
    return new ApiError({ code: "FORBIDDEN" });
  }

  if (error.code === "P0002") {
    return new ApiError({
      code: "ORDER_NOT_FOUND",
      details,
      exposeDetails: true,
    });
  }

  if (error.code === "P0001") {
    return new ApiError({
      code: "ORDER_NOT_EDITABLE",
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
