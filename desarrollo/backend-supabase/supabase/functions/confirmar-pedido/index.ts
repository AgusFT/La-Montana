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

type CuerpoConfirmarPedido = Record<string, unknown>;

interface PedidoConfirmado {
  id_pedido: number;
  codigo: string;
  estado_visible_cliente: "pendiente_revision";
  estado_interno: "pendiente_revision";
  estado_financiero: string;
  fecha_confirmacion_cliente: string | null;
  confirmado: boolean;
  ya_confirmado: boolean;
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

    const { data: usuarioAuth, error: errorUsuarioAuth } =
      await supabase.auth.getUser(token);

    if (errorUsuarioAuth || !usuarioAuth.user) {
      throw new ApiError({ code: "UNAUTHENTICATED" });
    }

    const cuerpo = await parseJsonBody<CuerpoConfirmarPedido>(request);
    const pedido = normalizarCuerpoConfirmarPedido(cuerpo);

    const { data: pedidoConfirmadoRpc, error } = await supabase.rpc(
      "confirmar_pedido_cliente",
      {
        p_id_pedido: pedido.idPedido,
        p_request_id: requestId,
      },
    );
    const data = pedidoConfirmadoRpc as PedidoConfirmado | null;

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

function normalizarCuerpoConfirmarPedido(cuerpo: CuerpoConfirmarPedido) {
  if (!cuerpo || Array.isArray(cuerpo) || typeof cuerpo !== "object") {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { cuerpo: "El cuerpo debe ser un objeto JSON." },
      exposeDetails: true,
    });
  }

  return {
    idPedido: leerEnteroPositivo(cuerpo, ["id_pedido", "orderId"], "id_pedido"),
  };
}

function leerEnteroPositivo(
  cuerpo: CuerpoConfirmarPedido,
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

function leerValor(cuerpo: CuerpoConfirmarPedido, claves: string[]): unknown {
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

  if (error.code === "23505") {
    return new ApiError({
      code: "ORDER_NOT_EDITABLE",
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
