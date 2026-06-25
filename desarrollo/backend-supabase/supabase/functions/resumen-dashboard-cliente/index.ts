import { createClient } from "npm:@supabase/supabase-js@2";

import {
  ApiError,
  errorResponse,
  methodNotAllowedResponse,
  optionsResponse,
  resolveRequestId,
  successResponse,
} from "../_shared/api-response.ts";

type EstadoPedidoCliente =
  | "pendiente_revision"
  | "corregir"
  | "aprobado"
  | "produccion"
  | "control_de_calidad"
  | "listo_para_entregar"
  | "en_viaje"
  | "entregado"
  | "cancelado"
  | "rechazado";

interface PedidoClienteBackend {
  id_pedido: number;
  id_punto_entrega: number | null;
  codigo: string;
  descripcion: string | null;
  estado_visible_cliente: EstadoPedidoCliente;
  total_estimado: number | null;
  fecha_creacion: string;
  fecha_confirmacion_cliente: string | null;
}

interface ArchivoClienteBackend {
  id_pedido: number;
  nombre_original: string;
  tamano_bytes: number;
  tamano_original_bytes: number | null;
  estado_archivo: string;
  fecha_creacion: string;
}

interface PuntoEntregaBackend {
  id_punto_entrega: number;
  nombre: string;
  direccion_texto: string;
  descripcion: string | null;
}

interface PedidoDashboardCliente {
  id_pedido: number;
  codigo: string;
  descripcion: string | null;
  estado_visible_cliente: EstadoPedidoCliente;
  estado_visible_label: string;
  total_estimado: number | null;
  fecha_creacion: string;
  fecha_confirmacion_cliente: string | null;
  archivo: {
    nombre_original: string;
    tamano_bytes: number;
    tamano_original_bytes: number | null;
    estado_archivo: string;
  } | null;
  punto_entrega: PuntoEntregaDashboard | null;
}

interface PuntoEntregaDashboard {
  id_punto_entrega: number;
  nombre: string;
  direccion_texto: string;
  descripcion: string | null;
}

interface SupabaseErrorDebug {
  code?: string;
  message?: string;
  details?: unknown;
  hint?: string;
}

const ESTADOS_PEDIDO_ACTIVO: EstadoPedidoCliente[] = [
  "pendiente_revision",
  "corregir",
  "aprobado",
  "produccion",
  "control_de_calidad",
  "listo_para_entregar",
  "en_viaje",
];

const ESTADO_LABELS: Record<EstadoPedidoCliente, string> = {
  pendiente_revision: "En revision",
  corregir: "A corregir",
  aprobado: "Aprobado",
  produccion: "En produccion",
  control_de_calidad: "Control de calidad",
  listo_para_entregar: "Listo para entregar",
  en_viaje: "En viaje",
  entregado: "Entregado",
  cancelado: "Cancelado",
  rechazado: "Rechazado",
};

Deno.serve(async (request) => {
  const requestId = resolveRequestId(request);

  try {
    if (request.method === "OPTIONS") {
      return optionsResponse(requestId);
    }

    if (request.method !== "GET") {
      return methodNotAllowedResponse(requestId);
    }

    const token = obtenerBearerToken(request);
    const supabase = crearClienteSupabaseAutenticado(token);

    const { data: usuarioAuth, error: errorUsuarioAuth } =
      await supabase.auth.getUser(token);

    if (errorUsuarioAuth || !usuarioAuth.user) {
      throw new ApiError({ code: "UNAUTHENTICATED" });
    }

    const [
      totalPedidos,
      pedidosEnCurso,
      pedidosRecientesBackend,
    ] = await Promise.all([
      contarPedidos(supabase),
      contarPedidosEnCurso(supabase),
      obtenerPedidosRecientes(supabase),
    ]);

    const [archivosPorPedido, puntosPorEntrega] = await Promise.all([
      obtenerArchivosPorPedido(
        supabase,
        pedidosRecientesBackend.map((pedido) => pedido.id_pedido),
      ),
      obtenerPuntosEntrega(
        supabase,
        pedidosRecientesBackend
          .map((pedido) => pedido.id_punto_entrega)
          .filter((id): id is number => typeof id === "number"),
      ),
    ]);

    const pedidosRecientes = pedidosRecientesBackend.map((pedido) =>
      mapearPedidoDashboard(pedido, archivosPorPedido, puntosPorEntrega)
    );
    const pedidoActual =
      pedidosRecientes.find((pedido) =>
        ESTADOS_PEDIDO_ACTIVO.includes(pedido.estado_visible_cliente)
      ) ?? null;

    return successResponse(
      {
        total_pedidos: totalPedidos,
        pedidos_en_curso: pedidosEnCurso,
        pedido_actual: pedidoActual,
        pedidos_recientes: pedidosRecientes,
        punto_entrega_principal: pedidoActual?.punto_entrega ??
          pedidosRecientes.find((pedido) => pedido.punto_entrega)
            ?.punto_entrega ??
          null,
      },
      requestId,
    );
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

async function contarPedidos(
  supabase: ReturnType<typeof crearClienteSupabaseAutenticado>,
): Promise<number> {
  const { count, error } = await supabase
    .from("pedido_cliente")
    .select("id_pedido", { count: "exact", head: true });

  if (error) {
    throw mapearErrorSupabase(error, "No pudimos contar tus pedidos.");
  }

  return count ?? 0;
}

async function contarPedidosEnCurso(
  supabase: ReturnType<typeof crearClienteSupabaseAutenticado>,
): Promise<number> {
  const { count, error } = await supabase
    .from("pedido_cliente")
    .select("id_pedido", { count: "exact", head: true })
    .in("estado_visible_cliente", ESTADOS_PEDIDO_ACTIVO);

  if (error) {
    throw mapearErrorSupabase(error, "No pudimos contar tus pedidos en curso.");
  }

  return count ?? 0;
}

async function obtenerPedidosRecientes(
  supabase: ReturnType<typeof crearClienteSupabaseAutenticado>,
): Promise<PedidoClienteBackend[]> {
  const { data, error } = await supabase
    .from("pedido_cliente")
    .select(
      "id_pedido,id_punto_entrega,codigo,descripcion,estado_visible_cliente,total_estimado,fecha_creacion,fecha_confirmacion_cliente",
    )
    .order("fecha_creacion", { ascending: false })
    .limit(5)
    .returns<PedidoClienteBackend[]>();

  if (error) {
    throw mapearErrorSupabase(
      error,
      "No pudimos consultar tus pedidos recientes.",
    );
  }

  return data ?? [];
}

async function obtenerArchivosPorPedido(
  supabase: ReturnType<typeof crearClienteSupabaseAutenticado>,
  idsPedido: number[],
): Promise<Map<number, ArchivoClienteBackend>> {
  if (idsPedido.length === 0) {
    return new Map();
  }

  const { data, error } = await supabase
    .from("archivo_cliente")
    .select(
      "id_pedido,nombre_original,tamano_bytes,tamano_original_bytes,estado_archivo,fecha_creacion",
    )
    .in("id_pedido", idsPedido)
    .order("fecha_creacion", { ascending: false })
    .returns<ArchivoClienteBackend[]>();

  if (error) {
    throw mapearErrorSupabase(
      error,
      "No pudimos consultar los archivos de tus pedidos.",
    );
  }

  const archivosPorPedido = new Map<number, ArchivoClienteBackend>();

  for (const archivo of data ?? []) {
    if (!archivosPorPedido.has(archivo.id_pedido)) {
      archivosPorPedido.set(archivo.id_pedido, archivo);
    }
  }

  return archivosPorPedido;
}

async function obtenerPuntosEntrega(
  supabase: ReturnType<typeof crearClienteSupabaseAutenticado>,
  idsPuntoEntrega: number[],
): Promise<Map<number, PuntoEntregaDashboard>> {
  const idsUnicos = [...new Set(idsPuntoEntrega)];

  if (idsUnicos.length === 0) {
    return new Map();
  }

  const { data, error } = await supabase
    .from("punto_entrega")
    .select("id_punto_entrega,nombre,direccion_texto,descripcion")
    .in("id_punto_entrega", idsUnicos)
    .returns<PuntoEntregaBackend[]>();

  if (error) {
    throw mapearErrorSupabase(
      error,
      "No pudimos consultar los puntos de entrega.",
    );
  }

  const puntosPorEntrega = new Map<number, PuntoEntregaDashboard>();

  for (const punto of data ?? []) {
    puntosPorEntrega.set(punto.id_punto_entrega, {
      id_punto_entrega: punto.id_punto_entrega,
      nombre: punto.nombre,
      direccion_texto: punto.direccion_texto,
      descripcion: punto.descripcion,
    });
  }

  return puntosPorEntrega;
}

function mapearPedidoDashboard(
  pedido: PedidoClienteBackend,
  archivosPorPedido: Map<number, ArchivoClienteBackend>,
  puntosPorEntrega: Map<number, PuntoEntregaDashboard>,
): PedidoDashboardCliente {
  const archivo = archivosPorPedido.get(pedido.id_pedido) ?? null;
  const puntoEntrega = pedido.id_punto_entrega
    ? puntosPorEntrega.get(pedido.id_punto_entrega) ?? null
    : null;

  return {
    id_pedido: pedido.id_pedido,
    codigo: pedido.codigo,
    descripcion: pedido.descripcion,
    estado_visible_cliente: pedido.estado_visible_cliente,
    estado_visible_label: ESTADO_LABELS[pedido.estado_visible_cliente] ??
      pedido.estado_visible_cliente,
    total_estimado: pedido.total_estimado,
    fecha_creacion: pedido.fecha_creacion,
    fecha_confirmacion_cliente: pedido.fecha_confirmacion_cliente,
    archivo: archivo
      ? {
        nombre_original: archivo.nombre_original,
        tamano_bytes: archivo.tamano_bytes,
        tamano_original_bytes: archivo.tamano_original_bytes,
        estado_archivo: archivo.estado_archivo,
      }
      : null,
    punto_entrega: puntoEntrega,
  };
}

function mapearErrorSupabase(
  error: SupabaseErrorDebug,
  mensaje: string,
): ApiError {
  return new ApiError({
    code: error.code === "42501" ? "FORBIDDEN" : "INTERNAL_ERROR",
    message: mensaje,
    details: {
      codigo_db: error.code ?? null,
      mensaje_db: error.message ?? null,
      detalle_db: error.details ?? null,
      sugerencia_db: error.hint ?? null,
    },
    exposeDetails: true,
  });
}
