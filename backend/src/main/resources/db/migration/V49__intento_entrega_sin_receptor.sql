-- Ausencia a domicilio: conserva reserva, dinero y código; nunca presume una entrega ni un retorno.
ALTER TABLE lamontana.movimiento_entrega DROP CONSTRAINT movimiento_entrega_estado_destino_check;
ALTER TABLE lamontana.movimiento_entrega ADD CONSTRAINT movimiento_entrega_estado_destino_check
 CHECK(estado_destino IN ('PREPARADO','EN_VIAJE','DISPONIBLE_PUNTO','CLIENTE_AUSENTE'));

ALTER TABLE lamontana.historial_estado_pedido DROP CONSTRAINT gestion_pedido_evento_check;
ALTER TABLE lamontana.historial_estado_pedido ADD CONSTRAINT gestion_pedido_evento_check CHECK(
 (version_pedido IS NULL AND accion IS NULL AND estado_origen IS NULL AND mensaje_cliente IS NULL AND actor_nombre IS NULL AND actor_tipo IS NULL AND id_actor IS NOT NULL)
 OR (version_pedido>1 AND accion IS NOT NULL AND estado_origen IS NOT NULL AND mensaje_cliente IS NOT NULL AND length(trim(mensaje_cliente))>0 AND actor_nombre IS NOT NULL
 AND ((actor_tipo IN ('CLIENTE','ADMIN_ADMIN','EMPLEADO') AND id_actor IS NOT NULL
 AND accion IN ('REVISAR','APROBAR','RECHAZAR','CANCELAR','PEDIR_CORRECCION','RESPONDER_CORRECCION','INICIAR_TRABAJO','COMPLETAR_TRABAJO','ERROR_TRABAJO','CANCELAR_TRABAJO','CONTROLAR_CALIDAD','PREPARAR_ENVIO','SALIR_REPARTO','LLEGAR_PUNTO','CLIENTE_AUSENTE','REINTENTAR_ENTREGA','ENTREGAR','CERRAR'))
 OR (actor_tipo='SISTEMA' AND id_actor IS NULL AND accion='CANCELAR'))));

CREATE OR REPLACE FUNCTION lamontana.validar_movimiento_entrega() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE h lamontana.historial_estado_pedido; r lamontana.reserva_entrega; previo text;
BEGIN
 SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_evento=NEW.id_evento_pedido;
 SELECT * INTO r FROM lamontana.reserva_entrega WHERE id_reserva_entrega=NEW.id_reserva_entrega;
 previo:=lamontana.estado_logistico(NEW.id_pedido);
 IF r.id_pedido<>NEW.id_pedido OR r.estado<>'ACTIVA' OR (r.modalidad='RETIRO_SUCURSAL' AND NOT lamontana.retiro_con_traslado(NEW.id_pedido)) OR h.id_pedido<>NEW.id_pedido
 OR h.estado_origen<>'LISTO_PARA_ENTREGA' OR h.estado_destino<>'LISTO_PARA_ENTREGA' OR h.actor_tipo NOT IN ('ADMIN_ADMIN','EMPLEADO')
 OR NEW.estado_origen<>previo OR NOT (
  (previo='PENDIENTE_PREPARACION' AND NEW.estado_destino='PREPARADO' AND h.accion='PREPARAR_ENVIO')
  OR (previo='PREPARADO' AND NEW.estado_destino='EN_VIAJE' AND h.accion='SALIR_REPARTO')
  OR (previo='EN_VIAJE' AND NEW.estado_destino='DISPONIBLE_PUNTO' AND (r.modalidad='RETIRO_PUNTO_ENTREGA' OR lamontana.retiro_con_traslado(NEW.id_pedido)) AND h.accion='LLEGAR_PUNTO')
  OR (previo='EN_VIAJE' AND NEW.estado_destino='CLIENTE_AUSENTE' AND r.modalidad='ENVIO_DOMICILIO' AND h.accion='CLIENTE_AUSENTE')
  OR (previo='CLIENTE_AUSENTE' AND NEW.estado_destino='EN_VIAJE' AND r.modalidad='ENVIO_DOMICILIO' AND h.accion='REINTENTAR_ENTREGA'))
 THEN RAISE EXCEPTION 'Movimiento incompatible con modalidad y recorrido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

