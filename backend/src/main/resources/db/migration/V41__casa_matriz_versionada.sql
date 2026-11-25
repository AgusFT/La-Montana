-- No se elige la primera sucursal ni se reescriben versiones existentes.
ALTER TABLE lamontana.configuracion_entrega
 ADD COLUMN id_sucursal_matriz bigint REFERENCES lamontana.sucursal,
 ADD COLUMN matriz_confirmada boolean NOT NULL DEFAULT false,
 ADD CONSTRAINT matriz_confirmada_requiere_sucursal CHECK(NOT matriz_confirmada OR id_sucursal_matriz IS NOT NULL);

-- Preferencia del alta: solamente propone una ubicación para el asistente. No es la matriz vigente.
ALTER TABLE lamontana.sucursal ADD COLUMN propuesta_matriz boolean NOT NULL DEFAULT false;
CREATE UNIQUE INDEX una_propuesta_matriz ON lamontana.sucursal((true)) WHERE propuesta_matriz;

CREATE FUNCTION lamontana.retiro_con_traslado(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 -- Se consulta la versión confirmada del pedido, nunca la actualmente vigente ni la estimación inicial de la cotización.
 SELECT coalesce(c.oferta->>'modalidad'='RETIRO_SUCURSAL'
   AND e.id_sucursal_matriz IS NOT NULL AND e.id_sucursal_matriz<>p.id_sucursal,false)
 FROM lamontana.pedido_actual p JOIN lamontana.cotizacion c USING(id_cotizacion)
 LEFT JOIN lamontana.configuracion_entrega e ON e.id_configuracion_version=p.id_configuracion_version WHERE p.id_pedido=pedido
$$;

CREATE OR REPLACE FUNCTION lamontana.estado_logistico(pedido bigint) RETURNS text LANGUAGE sql STABLE AS $$
 SELECT CASE WHEN p.estado IN ('ENTREGADO','CERRADO') THEN 'ENTREGADO'
 WHEN p.estado<>'LISTO_PARA_ENTREGA' THEN 'NO_DISPONIBLE'
 WHEN c.oferta->>'modalidad'='RETIRO_SUCURSAL' AND NOT lamontana.retiro_con_traslado(pedido) THEN 'LISTO_RETIRO'
 ELSE coalesce((SELECT m.estado_destino FROM lamontana.movimiento_entrega m WHERE m.id_pedido=p.id_pedido ORDER BY m.id_movimiento_entrega DESC LIMIT 1),'PENDIENTE_PREPARACION') END
 FROM lamontana.pedido_actual p JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE p.id_pedido=pedido
$$;

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
  OR (previo='EN_VIAJE' AND NEW.estado_destino='DISPONIBLE_PUNTO' AND (r.modalidad='RETIRO_PUNTO_ENTREGA' OR lamontana.retiro_con_traslado(NEW.id_pedido)) AND h.accion='LLEGAR_PUNTO'))
 THEN RAISE EXCEPTION 'Movimiento incompatible con modalidad y recorrido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION lamontana.validar_entrega_registrada() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p lamontana.pedido_actual; r lamontana.reserva_entrega; h lamontana.historial_estado_pedido; v lamontana.validacion_codigo_entrega; c lamontana.codigo_entrega; ultimo text;
BEGIN
 SELECT * INTO p FROM lamontana.pedido_actual WHERE id_pedido=NEW.id_pedido;
 SELECT * INTO r FROM lamontana.reserva_entrega WHERE id_reserva_entrega=NEW.id_reserva_entrega;
 SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_evento=NEW.id_evento_pedido;
 SELECT * INTO v FROM lamontana.validacion_codigo_entrega WHERE id_validacion_codigo=NEW.id_validacion_codigo;
 SELECT * INTO c FROM lamontana.codigo_entrega WHERE id_codigo_entrega=v.id_codigo_entrega;
 SELECT estado_destino INTO ultimo FROM lamontana.movimiento_entrega WHERE id_pedido=p.id_pedido ORDER BY id_movimiento_entrega DESC LIMIT 1;
 IF p.estado<>'ENTREGADO' OR NOT lamontana.calidad_pedido_aprobada(p.id_pedido) OR NOT lamontana.saldo_entrega_cubierto(p.id_pedido)
 OR r.id_pedido<>p.id_pedido OR r.estado<>'CUMPLIDA'
 OR h.id_pedido<>p.id_pedido OR h.accion<>'ENTREGAR' OR h.fecha<>NEW.fecha_entrega OR h.estado_origen<>'LISTO_PARA_ENTREGA' OR h.estado_destino<>'ENTREGADO'
 OR h.actor_tipo NOT IN ('ADMIN_ADMIN','EMPLEADO') OR v.id_actor<>h.id_actor OR v.resultado<>'VALIDO' OR v.version_pedido<>p.version-1
 OR NEW.fecha_entrega<v.fecha OR NEW.fecha_entrega>=v.valido_hasta OR NEW.fecha_entrega>=c.vence_en
 OR c.intentos>=5 OR c.revocado_en IS NOT NULL OR c.utilizado_en IS DISTINCT FROM NEW.fecha_entrega OR c.id_reserva_entrega<>r.id_reserva_entrega OR c.id_pedido<>p.id_pedido
 OR (r.modalidad='RETIRO_SUCURSAL' AND NOT lamontana.retiro_con_traslado(p.id_pedido) AND ultimo IS NOT NULL)
 OR ((r.modalidad='RETIRO_PUNTO_ENTREGA' OR lamontana.retiro_con_traslado(p.id_pedido)) AND ultimo IS DISTINCT FROM 'DISPONIBLE_PUNTO')
 OR (r.modalidad='ENVIO_DOMICILIO' AND ultimo IS DISTINCT FROM 'EN_VIAJE')
 THEN RAISE EXCEPTION 'Entrega requiere calidad, recorrido, código personal y saldo' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
