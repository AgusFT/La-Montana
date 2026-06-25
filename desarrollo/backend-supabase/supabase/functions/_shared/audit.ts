export type AccionAuditoria =
  | "usuario_registrado"
  | "perfil_creado"
  | "pedido_creado"
  | "archivo_pedido_cargado"
  | "pedido_confirmado"
  | "pedido_cancelado"
  | "revision_pedido_iniciada"
  | "pedido_aprobado"
  | "pedido_rechazado"
  | "estado_pedido_actualizado"
  | "validacion_financiera_realizada"
  | "error_operacion";

export type NivelAuditoria = "info" | "warning" | "error" | "critico";

export interface EventoAuditoria {
  accion: AccionAuditoria;
  mensaje: string;
  nivel?: NivelAuditoria;
  codigo?: string | null;
  idPedido?: number | null;
  tablaAfectada?: string | null;
  idRegistroAfectado?: string | null;
  requestId?: string | null;
  metadata?: Record<string, unknown>;
  idUsuarioActor?: number | null;
  idUsuarioAuthActor?: string | null;
}

export interface ClienteRpcAuditoria {
  rpc<TData>(
    nombreFuncion: string,
    parametros: Record<string, unknown>,
  ): Promise<{ data: TData | null; error: unknown }>;
}

export const ACCIONES_AUDITORIA = {
  usuarioRegistrado: "usuario_registrado",
  perfilCreado: "perfil_creado",
  pedidoCreado: "pedido_creado",
  archivoPedidoCargado: "archivo_pedido_cargado",
  pedidoConfirmado: "pedido_confirmado",
  pedidoCancelado: "pedido_cancelado",
  revisionPedidoIniciada: "revision_pedido_iniciada",
  pedidoAprobado: "pedido_aprobado",
  pedidoRechazado: "pedido_rechazado",
  estadoPedidoActualizado: "estado_pedido_actualizado",
  validacionFinancieraRealizada: "validacion_financiera_realizada",
  errorOperacion: "error_operacion",
} as const satisfies Record<string, AccionAuditoria>;

export async function registrarEventoAuditoria(
  cliente: ClienteRpcAuditoria,
  evento: EventoAuditoria,
): Promise<number> {
  const { data, error } = await cliente.rpc<number>("registrar_evento_auditoria", {
    p_accion: evento.accion,
    p_mensaje: evento.mensaje,
    p_nivel: evento.nivel ?? "info",
    p_codigo: evento.codigo ?? null,
    p_id_pedido: evento.idPedido ?? null,
    p_tabla_afectada: evento.tablaAfectada ?? null,
    p_id_registro_afectado: evento.idRegistroAfectado ?? null,
    p_request_id: evento.requestId ?? null,
    p_metadata: evento.metadata ?? {},
    p_id_usuario_actor: evento.idUsuarioActor ?? null,
    p_id_usuario_auth_actor: evento.idUsuarioAuthActor ?? null,
  });

  if (error) {
    throw error;
  }

  if (typeof data !== "number") {
    throw new Error("La auditoria no devolvio un identificador valido.");
  }

  return data;
}
