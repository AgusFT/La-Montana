-- La entrega puede finalizar en CERRADO en la misma transacción.
-- La versión del código se contrasta con el evento de entrega, no con el cierre posterior.
CREATE OR REPLACE FUNCTION lamontana.validar_entrega_registrada() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p lamontana.pedido_actual; r lamontana.reserva_entrega; h lamontana.historial_estado_pedido; v lamontana.validacion_codigo_entrega; c lamontana.codigo_entrega; ultimo text;
BEGIN
 SELECT * INTO p FROM lamontana.pedido_actual WHERE id_pedido=NEW.id_pedido;
 SELECT * INTO r FROM lamontana.reserva_entrega WHERE id_reserva_entrega=NEW.id_reserva_entrega;
 SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_evento=NEW.id_evento_pedido;
 SELECT * INTO v FROM lamontana.validacion_codigo_entrega WHERE id_validacion_codigo=NEW.id_validacion_codigo;
 SELECT * INTO c FROM lamontana.codigo_entrega WHERE id_codigo_entrega=v.id_codigo_entrega;
 SELECT estado_destino INTO ultimo FROM lamontana.movimiento_entrega WHERE id_pedido=p.id_pedido ORDER BY id_movimiento_entrega DESC LIMIT 1;
 IF p.estado NOT IN ('ENTREGADO','CERRADO') OR NOT lamontana.calidad_pedido_aprobada(p.id_pedido) OR NOT lamontana.saldo_entrega_cubierto(p.id_pedido)
 OR r.id_pedido<>p.id_pedido OR r.estado<>'CUMPLIDA'
 OR h.id_pedido<>p.id_pedido OR h.accion<>'ENTREGAR' OR h.fecha<>NEW.fecha_entrega OR h.estado_origen<>'LISTO_PARA_ENTREGA' OR h.estado_destino<>'ENTREGADO'
 OR h.actor_tipo NOT IN ('ADMIN_ADMIN','EMPLEADO') OR v.id_actor<>h.id_actor OR v.resultado<>'VALIDO' OR v.version_pedido<>h.version_pedido-1
 OR p.version<>(h.version_pedido+CASE WHEN p.estado='CERRADO' THEN 1 ELSE 0 END)
 OR NEW.fecha_entrega<v.fecha OR NEW.fecha_entrega>=v.valido_hasta OR NEW.fecha_entrega>=c.vence_en
 OR c.intentos>=5 OR c.revocado_en IS NOT NULL OR c.utilizado_en IS DISTINCT FROM NEW.fecha_entrega OR c.id_reserva_entrega<>r.id_reserva_entrega OR c.id_pedido<>p.id_pedido
 OR (r.modalidad='RETIRO_SUCURSAL' AND NOT lamontana.retiro_con_traslado(p.id_pedido) AND ultimo IS NOT NULL)
 OR ((r.modalidad='RETIRO_PUNTO_ENTREGA' OR lamontana.retiro_con_traslado(p.id_pedido)) AND ultimo IS DISTINCT FROM 'DISPONIBLE_PUNTO')
 OR (r.modalidad='ENVIO_DOMICILIO' AND ultimo IS DISTINCT FROM 'EN_VIAJE')
 THEN RAISE EXCEPTION 'Entrega requiere calidad, recorrido, código personal y saldo' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
