import { crearClienteSupabaseBrowser } from "@/lib/supabase/client";
import { obtenerConfigSupabase } from "@/lib/supabase/config";

interface ApiSuccessEnvelope<TData> {
  ok: true;
  request_id: string;
  data: TData;
}

interface ApiErrorEnvelope {
  ok: false;
  request_id: string;
  code: string;
  message: string;
  details: unknown;
}

type ApiEnvelope<TData> = ApiSuccessEnvelope<TData> | ApiErrorEnvelope;

export type EstadoPedidoDashboard =
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

export interface PuntoEntregaDashboard {
  id_punto_entrega: number;
  nombre: string;
  direccion_texto: string;
  descripcion: string | null;
}

export interface PedidoDashboardCliente {
  id_pedido: number;
  codigo: string;
  descripcion: string | null;
  estado_visible_cliente: EstadoPedidoDashboard;
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

export interface ResumenDashboardCliente {
  total_pedidos: number;
  pedidos_en_curso: number;
  pedido_actual: PedidoDashboardCliente | null;
  pedidos_recientes: PedidoDashboardCliente[];
  punto_entrega_principal: PuntoEntregaDashboard | null;
}

export class DashboardApiError extends Error {
  readonly code?: string;
  readonly requestId?: string;

  constructor(message: string, code?: string, requestId?: string) {
    super(message);
    this.name = "DashboardApiError";
    this.code = code;
    this.requestId = requestId;
  }
}

export async function obtenerResumenDashboardCliente(): Promise<ResumenDashboardCliente> {
  return llamarEdgeFunction<ResumenDashboardCliente>("resumen-dashboard-cliente");
}

export function obtenerMensajeErrorDashboard(error: unknown): string {
  if (error instanceof DashboardApiError) {
    return error.message;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "No pudimos cargar el dashboard. Intentá nuevamente.";
}

async function llamarEdgeFunction<TData>(nombre: string): Promise<TData> {
  const supabase = crearClienteSupabaseBrowser();
  const { data: sesion } = await supabase.auth.getSession();
  const token = sesion.session?.access_token;

  if (!token) {
    throw new DashboardApiError("Iniciá sesión para ver tu dashboard.");
  }

  const { url, key } = obtenerConfigSupabase();
  const response = await fetch(
    `${url.replace(/\/$/, "")}/functions/v1/${nombre}`,
    {
      method: "GET",
      headers: {
        apikey: key,
        authorization: `Bearer ${token}`,
        "x-request-id": crypto.randomUUID(),
      },
    },
  );
  const envelope = await leerEnvelope<TData>(response);

  if (!response.ok || !envelope.ok) {
    throw new DashboardApiError(
      envelope.ok ? "No pudimos completar la operación." : envelope.message,
      envelope.ok ? undefined : envelope.code,
      envelope.request_id,
    );
  }

  return envelope.data;
}

async function leerEnvelope<TData>(
  response: Response,
): Promise<ApiEnvelope<TData>> {
  try {
    return await response.json() as ApiEnvelope<TData>;
  } catch {
    return {
      ok: false,
      request_id: response.headers.get("x-request-id") ?? crypto.randomUUID(),
      code: "INVALID_RESPONSE",
      message: "El backend respondió con un formato inesperado.",
      details: null,
    };
  }
}
