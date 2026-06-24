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

type CuerpoCrearPedido = Record<string, unknown>;

interface PedidoCreado {
  id_pedido: number;
  codigo: string;
  estado_visible_cliente: "pendiente_revision";
  estado_interno: "pendiente_revision";
  estado_financiero: "pendiente_evaluacion";
  cantidad_carillas: number;
  cantidad_copias: number;
  tamano_hoja: "A4" | "A3" | "OFICIO";
  tipo_impresion: "byn" | "color";
  doble_faz: boolean;
  encuadernado: boolean;
  anillado: boolean;
  id_punto_entrega: number | null;
  metodo_pago_preferido: string | null;
}

const TAMANOS_HOJA_PERMITIDOS = new Set(["A4", "A3", "OFICIO"]);
const TIPOS_IMPRESION_PERMITIDOS = new Set(["byn", "color"]);
const MAX_TEXTO_CORTO = 500;
const MAX_OBSERVACION = 1000;

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

    const { data: usuarioAuth, error: errorUsuarioAuth } = await supabase.auth.getUser(token);

    if (errorUsuarioAuth || !usuarioAuth.user) {
      throw new ApiError({ code: "UNAUTHENTICATED" });
    }

    const cuerpo = await parseJsonBody<CuerpoCrearPedido>(request);
    const pedido = normalizarCuerpoCrearPedido(cuerpo, requestId);

    const { data: pedidoCreadoRpc, error } = await supabase.rpc("crear_pedido_cliente", {
      p_cantidad_carillas: pedido.cantidadCarillas,
      p_cantidad_copias: pedido.cantidadCopias,
      p_tamano_hoja: pedido.tamanoHoja,
      p_tipo_impresion: pedido.tipoImpresion,
      p_doble_faz: pedido.dobleFaz,
      p_encuadernado: pedido.encuadernado,
      p_anillado: pedido.anillado,
      p_observacion_cliente: pedido.observacionCliente,
      p_descripcion: pedido.descripcion,
      p_id_punto_entrega: pedido.idPuntoEntrega,
      p_metodo_pago_preferido: pedido.metodoPagoPreferido,
      p_request_id: requestId,
    });
    const data = pedidoCreadoRpc as PedidoCreado | null;

    if (error) {
      throw mapearErrorSupabase(error);
    }

    if (!data) {
      throw new ApiError({ code: "INTERNAL_ERROR" });
    }

    return successResponse(data, requestId);
  } catch (error) {
    return errorResponse(error, requestId);
  }
});

function crearClienteSupabaseAutenticado(token: string) {
  const supabaseUrl = obtenerVariableEntorno("SUPABASE_URL");
  const supabaseAnonKey = obtenerVariableEntorno("SUPABASE_ANON_KEY");

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

function obtenerVariableEntorno(nombre: string): string {
  const valor = Deno.env.get(nombre);

  if (!valor) {
    throw new ApiError({ code: "INTERNAL_ERROR" });
  }

  return valor;
}

function obtenerBearerToken(request: Request): string {
  const authorization = request.headers.get("authorization")?.trim() ?? "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);

  if (!match?.[1]) {
    throw new ApiError({ code: "UNAUTHENTICATED" });
  }

  return match[1];
}

function normalizarCuerpoCrearPedido(cuerpo: CuerpoCrearPedido, requestId: string) {
  if (!cuerpo || Array.isArray(cuerpo) || typeof cuerpo !== "object") {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { cuerpo: "El cuerpo debe ser un objeto JSON.", request_id: requestId },
      exposeDetails: true,
    });
  }

  const cantidadCarillas = leerEnteroPositivo(cuerpo, ["cantidad_carillas", "pages"], "cantidad_carillas");
  const cantidadCopias = leerEnteroPositivo(cuerpo, ["cantidad_copias", "copies"], "cantidad_copias");
  const tamanoHoja = leerTextoRequerido(cuerpo, ["tamano_hoja", "paperSize"], "tamano_hoja").toUpperCase();
  const tipoImpresion = leerTextoRequerido(cuerpo, ["tipo_impresion", "printType"], "tipo_impresion").toLowerCase();

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
    observacionCliente: leerTextoOpcional(cuerpo, ["observacion_cliente", "customerNote"], MAX_OBSERVACION),
    descripcion: leerTextoOpcional(cuerpo, ["descripcion", "description"], MAX_TEXTO_CORTO),
    idPuntoEntrega: leerEnteroPositivoOpcional(cuerpo, ["id_punto_entrega", "deliveryPointId"], "id_punto_entrega"),
    metodoPagoPreferido: leerTextoOpcional(
      cuerpo,
      ["metodo_pago_preferido", "paymentMethod"],
      MAX_TEXTO_CORTO,
    ),
  };
}

function leerEnteroPositivo(cuerpo: CuerpoCrearPedido, claves: string[], campo: string): number {
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

function leerEnteroPositivoOpcional(
  cuerpo: CuerpoCrearPedido,
  claves: string[],
  campo: string,
): number | null {
  const valor = leerValor(cuerpo, claves);

  if (valor === undefined || valor === null || valor === "") {
    return null;
  }

  if (!Number.isInteger(valor) || (valor as number) <= 0) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { [campo]: "Debe ser un entero mayor a cero." },
      exposeDetails: true,
    });
  }

  return valor as number;
}

function leerTextoRequerido(cuerpo: CuerpoCrearPedido, claves: string[], campo: string): string {
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

function leerTextoOpcional(cuerpo: CuerpoCrearPedido, claves: string[], maximo: number): string | null {
  const valor = leerValor(cuerpo, claves);

  if (valor === undefined || valor === null || valor === "") {
    return null;
  }

  if (typeof valor !== "string") {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { [claves[0]]: "Debe ser texto." },
      exposeDetails: true,
    });
  }

  const texto = valor.trim();

  if (!texto) {
    return null;
  }

  if (texto.length > maximo) {
    throw new ApiError({
      code: "VALIDATION_ERROR",
      details: { [claves[0]]: `No debe superar ${maximo} caracteres.` },
      exposeDetails: true,
    });
  }

  return texto;
}

function leerBooleano(cuerpo: CuerpoCrearPedido, claves: string[], valorDefault: boolean): boolean {
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

function leerValor(cuerpo: CuerpoCrearPedido, claves: string[]): unknown {
  for (const clave of claves) {
    if (Object.prototype.hasOwnProperty.call(cuerpo, clave)) {
      return cuerpo[clave];
    }
  }

  return undefined;
}

function mapearErrorSupabase(error: { code?: string; message?: string; details?: unknown }): ApiError {
  if (error.code === "42501") {
    return new ApiError({ code: "FORBIDDEN" });
  }

  if (error.code === "23503") {
    return new ApiError({ code: "VALIDATION_ERROR" });
  }

  if (error.code === "23514" || error.code === "22P02") {
    return new ApiError({ code: "VALIDATION_ERROR" });
  }

  return new ApiError({ code: "INTERNAL_ERROR" });
}
