import { crearClienteSupabaseBrowser } from "@/lib/supabase/client";
import { obtenerConfigSupabase } from "@/lib/supabase/config";

import { CreateOrderForm } from "../types/create-order";
import { Order } from "../types/order";
import {
  ArchivoCifradoPedido,
  cifrarArchivoPedido,
  ContratoCifradoArchivo,
} from "./cifrado-archivo";

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

interface PedidoCreado {
  id_pedido: number;
  codigo: string;
  estado_visible_cliente: Order["status"];
  cantidad_carillas: number;
  cantidad_copias: number;
  cantidad_estimada: number;
  tamano_hoja: CreateOrderForm["paperSize"];
  tipo_impresion: CreateOrderForm["printType"];
  doble_faz: boolean;
  encuadernado: boolean;
  anillado: boolean;
  id_punto_entrega: number | null;
  metodo_pago_preferido: string | null;
  requiere_senia: boolean;
  porcentaje_senia: number;
  total_estimado: number;
  tipo_moneda: "ARS" | "USD";
  cotizacion: CotizacionPedido;
}

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

export interface CotizacionPedido {
  cantidad_estimada: number;
  requiere_senia: boolean;
  porcentaje_senia: number;
  total_estimado: number;
  tipo_moneda: "ARS" | "USD";
  lineas: LineaCotizacionPedido[];
}

interface ArchivoRegistrado {
  id_archivo: number;
  id_pedido: number;
  estado_archivo: "cargado";
  tamano_bytes: number;
  tamano_original_bytes: number;
  fecha_creacion: string;
}

interface PedidoConfirmado {
  id_pedido: number;
  codigo: string;
  estado_visible_cliente: Order["status"];
  fecha_confirmacion_cliente: string | null;
  confirmado: boolean;
  ya_confirmado: boolean;
}

interface PedidoClienteBackend {
  id_pedido: number;
  id_punto_entrega: number | null;
  codigo: string;
  descripcion: string | null;
  cantidad_carillas: number | null;
  cantidad_copias: number;
  tamano_hoja: CreateOrderForm["paperSize"];
  tipo_impresion: CreateOrderForm["printType"];
  doble_faz: boolean;
  encuadernado: boolean;
  anillado: boolean;
  metodo_pago_preferido: string | null;
  estado_visible_cliente: Order["status"];
  total_estimado: number | null;
  fecha_confirmacion_cliente: string | null;
  fecha_creacion: string;
}

interface ArchivoClienteBackend {
  id_archivo: number;
  id_pedido: number;
  nombre_original: string;
  tamano_bytes: number;
  tamano_original_bytes: number | null;
  estado_archivo: string;
  fecha_creacion: string;
}

export interface PuntoEntregaPedido {
  id_punto_entrega: number;
  nombre: string;
  direccion_texto: string;
  descripcion: string | null;
}

type PuntoEntregaBackend = PuntoEntregaPedido;

type MetodoPagoBackend = "efectivo" | "transferencia" | "tarjeta";

const ESTADOS_PEDIDO_ACTIVO: Order["status"][] = [
  "pendiente_revision",
  "corregir",
  "aprobado",
  "produccion",
  "control_de_calidad",
  "listo_para_entregar",
  "en_viaje",
];

export class PedidoApiError extends Error {
  readonly code?: string;
  readonly requestId?: string;

  constructor(message: string, code?: string, requestId?: string) {
    super(message);
    this.name = "PedidoApiError";
    this.code = code;
    this.requestId = requestId;
  }
}

export async function crearPedidoCompleto(
  form: CreateOrderForm,
): Promise<{
  pedido: PedidoCreado;
  archivo: ArchivoRegistrado;
  confirmacion: PedidoConfirmado;
}> {
  if (!form.file || !form.pages) {
    throw new PedidoApiError(
      "Completá el archivo PDF antes de crear el pedido.",
    );
  }

  const pedido = await crearPedido(form);
  const contrato = await obtenerContratoCifrado();
  const archivoCifrado = await cifrarArchivoPedido(form.file, contrato);
  const archivo = await cargarArchivoCifrado(pedido.id_pedido, archivoCifrado);
  const confirmacion = await confirmarPedido(pedido.id_pedido);

  return {
    pedido,
    archivo,
    confirmacion,
  };
}

export async function cotizarPedido(
  form: CreateOrderForm,
  signal?: AbortSignal,
): Promise<CotizacionPedido> {
  if (!form.file || !form.pages) {
    throw new PedidoApiError("Cargá un archivo PDF para cotizar el pedido.");
  }

  return llamarEdgeFunction<CotizacionPedido>("cotizar-pedido", {
    method: "POST",
    body: obtenerCuerpoCotizacion(form),
    signal,
  });
}

export async function obtenerPedidoActualCliente(): Promise<Order | null> {
  const supabase = crearClienteSupabaseBrowser();
  const { data: pedidos, error } = await supabase
    .from("pedido_cliente")
    .select(
      "id_pedido,id_punto_entrega,codigo,descripcion,cantidad_carillas,cantidad_copias,tamano_hoja,tipo_impresion,doble_faz,encuadernado,anillado,metodo_pago_preferido,estado_visible_cliente,total_estimado,fecha_confirmacion_cliente,fecha_creacion",
    )
    .in("estado_visible_cliente", ESTADOS_PEDIDO_ACTIVO)
    .order("fecha_creacion", { ascending: false })
    .limit(1)
    .returns<PedidoClienteBackend[]>();

  if (error) {
    throw new PedidoApiError("No pudimos consultar tus pedidos.");
  }

  const pedido = pedidos?.[0];

  if (!pedido) {
    return null;
  }

  const [archivo, puntoEntrega] = await Promise.all([
    obtenerArchivoPrincipal(pedido.id_pedido),
    obtenerPuntoEntrega(pedido.id_punto_entrega),
  ]);
  const form = mapearPedidoAFormulario(pedido);

  return {
    id: String(pedido.id_pedido),
    code: pedido.codigo,
    createdAt: pedido.fecha_creacion,
    status: pedido.estado_visible_cliente,
    statusLabel: obtenerEtiquetaEstadoPedido(pedido.estado_visible_cliente),
    price: pedido.total_estimado,
    fileName: archivo?.nombre_original ?? pedido.descripcion ?? pedido.codigo,
    fileSize: archivo?.tamano_original_bytes ?? archivo?.tamano_bytes ?? 0,
    deliveryPoint: puntoEntrega
      ? {
        id: puntoEntrega.id_punto_entrega,
        name: puntoEntrega.nombre,
        address: puntoEntrega.direccion_texto,
        reference: puntoEntrega.descripcion,
      }
      : null,
    form,
  };
}

export async function obtenerPuntosEntregaPedido(): Promise<
  PuntoEntregaPedido[]
> {
  const supabase = crearClienteSupabaseBrowser();
  const { data, error } = await supabase
    .from("punto_entrega")
    .select("id_punto_entrega,nombre,direccion_texto,descripcion")
    .eq("activo", true)
    .eq("eliminado", false)
    .order("nombre", { ascending: true })
    .returns<PuntoEntregaPedido[]>();

  if (error) {
    throw new PedidoApiError("No pudimos cargar los puntos de entrega.");
  }

  return data ?? [];
}

export function obtenerMensajeErrorPedido(error: unknown): string {
  if (error instanceof PedidoApiError) {
    return error.message;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "No pudimos crear el pedido. Intentá nuevamente.";
}

async function crearPedido(form: CreateOrderForm): Promise<PedidoCreado> {
  const idPuntoEntrega = obtenerIdPuntoEntregaSeleccionado(
    form.deliveryPointId,
  );

  return llamarEdgeFunction<PedidoCreado>("crear-pedido", {
    method: "POST",
    body: {
      ...obtenerCuerpoCotizacion(form),
      descripcion: form.file?.name ?? "Pedido web",
      id_punto_entrega: idPuntoEntrega,
      metodo_pago_preferido: mapearMetodoPagoBackend(form.paymentMethod),
    },
  });
}

async function obtenerContratoCifrado(): Promise<ContratoCifradoArchivo> {
  return llamarEdgeFunction<ContratoCifradoArchivo>("clave-publica-cifrado", {
    method: "GET",
  });
}

async function cargarArchivoCifrado(
  idPedido: number,
  archivoCifrado: ArchivoCifradoPedido,
): Promise<ArchivoRegistrado> {
  const formData = new FormData();

  formData.set("id_pedido", String(idPedido));
  formData.set("nombre_original", archivoCifrado.nombreOriginal);
  formData.set("mime_original", archivoCifrado.mimeOriginal);
  formData.set(
    "tamano_original_bytes",
    String(archivoCifrado.tamanoOriginalBytes),
  );
  formData.set("hash_archivo", archivoCifrado.hashArchivo);
  formData.set("clave_envuelta", archivoCifrado.claveEnvuelta);
  formData.set("iv", archivoCifrado.iv);
  formData.set("version_cifrado", archivoCifrado.versionCifrado);
  formData.set("archivo", archivoCifrado.archivo);

  return llamarEdgeFunction<ArchivoRegistrado>("cargar-archivo", {
    method: "POST",
    body: formData,
  });
}

async function confirmarPedido(idPedido: number): Promise<PedidoConfirmado> {
  return llamarEdgeFunction<PedidoConfirmado>("confirmar-pedido", {
    method: "POST",
    body: {
      id_pedido: idPedido,
    },
  });
}

async function llamarEdgeFunction<TData>(
  nombre: string,
  options: {
    method: "GET" | "POST";
    body?: Record<string, unknown> | FormData;
    signal?: AbortSignal;
  },
): Promise<TData> {
  const supabase = crearClienteSupabaseBrowser();
  const { data: sesion } = await supabase.auth.getSession();
  const token = sesion.session?.access_token;

  if (!token) {
    throw new PedidoApiError("Iniciá sesión para continuar.");
  }

  const { url, key } = obtenerConfigSupabase();
  const headers = new Headers({
    apikey: key,
    authorization: `Bearer ${token}`,
    "x-request-id": crypto.randomUUID(),
  });
  let body: BodyInit | undefined;

  if (options.body instanceof FormData) {
    body = options.body;
  } else if (options.body) {
    headers.set("content-type", "application/json");
    body = JSON.stringify(options.body);
  }

  const response = await fetch(
    `${url.replace(/\/$/, "")}/functions/v1/${nombre}`,
    {
      method: options.method,
      headers,
      body,
      signal: options.signal,
    },
  );
  const envelope = await leerEnvelope<TData>(response);

  if (!response.ok || !envelope.ok) {
    throw new PedidoApiError(
      envelope.ok ? "No pudimos completar la operación." : envelope.message,
      envelope.ok ? undefined : envelope.code,
      envelope.request_id,
    );
  }

  return envelope.data;
}

function obtenerCuerpoCotizacion(
  form: CreateOrderForm,
): Record<string, unknown> {
  return {
    cantidad_carillas: form.pages,
    cantidad_copias: form.copies,
    tamano_hoja: form.paperSize,
    tipo_impresion: form.printType,
    doble_faz: form.doubleSided,
    encuadernado: form.bound,
    anillado: form.spiralBound,
  };
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

function obtenerIdPuntoEntregaSeleccionado(
  deliveryPointId: CreateOrderForm["deliveryPointId"],
): number {
  if (!deliveryPointId) {
    throw new PedidoApiError("Seleccioná un punto de entrega.");
  }

  return deliveryPointId;
}

async function obtenerPuntoEntrega(
  idPuntoEntrega: number | null,
): Promise<PuntoEntregaBackend | null> {
  if (!idPuntoEntrega) {
    return null;
  }

  const supabase = crearClienteSupabaseBrowser();
  const { data, error } = await supabase
    .from("punto_entrega")
    .select("id_punto_entrega,nombre,direccion_texto,descripcion")
    .eq("id_punto_entrega", idPuntoEntrega)
    .maybeSingle<PuntoEntregaBackend>();

  if (error) {
    return null;
  }

  return data ?? null;
}

async function obtenerArchivoPrincipal(
  idPedido: number,
): Promise<ArchivoClienteBackend | null> {
  const supabase = crearClienteSupabaseBrowser();
  const { data, error } = await supabase
    .from("archivo_cliente")
    .select(
      "id_archivo,id_pedido,nombre_original,tamano_bytes,tamano_original_bytes,estado_archivo,fecha_creacion",
    )
    .eq("id_pedido", idPedido)
    .order("fecha_creacion", { ascending: false })
    .limit(1)
    .maybeSingle<ArchivoClienteBackend>();

  if (error) {
    return null;
  }

  return data ?? null;
}

function mapearPedidoAFormulario(
  pedido: PedidoClienteBackend,
): Omit<CreateOrderForm, "file"> {
  return {
    pages: pedido.cantidad_carillas,
    copies: pedido.cantidad_copias,
    printType: pedido.tipo_impresion,
    paperSize: pedido.tamano_hoja,
    doubleSided: pedido.doble_faz,
    bound: pedido.encuadernado,
    spiralBound: pedido.anillado,
    paymentMethod: mapearMetodoPagoFront(pedido.metodo_pago_preferido),
    deliveryPointId: pedido.id_punto_entrega,
  };
}

function mapearMetodoPagoBackend(
  paymentMethod: CreateOrderForm["paymentMethod"],
): MetodoPagoBackend {
  if (paymentMethod === "Transferencia") {
    return "transferencia";
  }

  if (paymentMethod === "Debito") {
    return "tarjeta";
  }

  return "efectivo";
}

function mapearMetodoPagoFront(
  paymentMethod: string | null,
): CreateOrderForm["paymentMethod"] {
  if (paymentMethod === "transferencia") {
    return "Transferencia";
  }

  if (paymentMethod === "tarjeta") {
    return "Debito";
  }

  return "Efectivo";
}

function obtenerEtiquetaEstadoPedido(status: Order["status"]): string {
  const labels: Record<Order["status"], string> = {
    pendiente_revision: "En revisión",
    aprobado: "Aprobado",
    corregir: "A corregir",
    rechazado: "Rechazado",
    produccion: "En producción",
    control_de_calidad: "Control de calidad",
    listo_para_entregar: "Listo para entregar",
    en_viaje: "En viaje",
    entregado: "Entregado",
    cancelado: "Cancelado",
  };

  return labels[status];
}
