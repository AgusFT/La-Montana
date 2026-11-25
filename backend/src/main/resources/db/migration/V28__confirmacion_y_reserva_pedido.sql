-- La oferta y sus originales conservan su historia; el pedido referencia los PDF exactos,
-- sin duplicar bytes ni cambiar retroactivamente la propiedad del archivo cotizado.
CREATE TABLE lamontana.pedido (
 id_pedido bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_cotizacion bigint NOT NULL UNIQUE REFERENCES lamontana.cotizacion,
 id_usuario_creador bigint NOT NULL REFERENCES lamontana.usuario,
 id_sucursal bigint NOT NULL REFERENCES lamontana.sucursal,
 id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
 estado varchar(32) NOT NULL CHECK(estado IN ('PENDIENTE_REVISION','APROBADO')),
 modo_aprobacion varchar(16) NOT NULL CHECK(modo_aprobacion IN ('HUMANA','AUTOMATICA')),
 confirmada_en timestamptz NOT NULL,
 aprobada_en timestamptz,
 version bigint NOT NULL DEFAULT 1 CHECK(version>0),
 total numeric(19,2) NOT NULL CHECK(total>=0),
 condiciones_finales jsonb NOT NULL CHECK(jsonb_typeof(condiciones_finales)='object'),
 receptor varchar(140), telefono varchar(40),
 CHECK((modo_aprobacion='AUTOMATICA')=(aprobada_en IS NOT NULL)),
 CHECK((estado='APROBADO')=(aprobada_en IS NOT NULL)),
 CHECK((receptor IS NULL)=(telefono IS NULL))
);
CREATE INDEX pedido_cliente ON lamontana.pedido(id_usuario_creador,id_pedido DESC);
CREATE INDEX pedido_sucursal ON lamontana.pedido(id_sucursal,id_pedido DESC);
CREATE TABLE lamontana.pedido_item (
 id_pedido_item bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pedido bigint NOT NULL REFERENCES lamontana.pedido,
 id_cotizacion_item bigint NOT NULL UNIQUE REFERENCES lamontana.cotizacion_item,
 id_archivo_almacenado bigint NOT NULL UNIQUE REFERENCES lamontana.archivo_almacenado,
 id_aceptacion_vista_previa bigint NOT NULL UNIQUE REFERENCES lamontana.aceptacion_vista_previa
);
CREATE TABLE lamontana.reserva_entrega (
 id_reserva_entrega bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pedido bigint NOT NULL UNIQUE REFERENCES lamontana.pedido,
 modalidad varchar(24) NOT NULL CHECK(modalidad IN ('RETIRO_SUCURSAL','RETIRO_PUNTO_ENTREGA','ENVIO_DOMICILIO')),
 destino_publico uuid NOT NULL,
 origen_publico uuid NOT NULL,
 nombre_destino varchar(600) NOT NULL,
 zona_horaria varchar(80) NOT NULL,
 fecha_local date NOT NULL,
 apertura time NOT NULL, cierre time NOT NULL CHECK(cierre>apertura),
 franja_desde timestamptz NOT NULL, franja_hasta timestamptz NOT NULL CHECK(franja_hasta>franja_desde),
 cupo_confirmado integer NOT NULL CHECK(cupo_confirmado>0),
 costo numeric(19,2) NOT NULL CHECK(costo>=0),
 estado varchar(16) NOT NULL DEFAULT 'ACTIVA' CHECK(estado='ACTIVA')
);
CREATE INDEX reserva_destino ON lamontana.reserva_entrega(modalidad,destino_publico,fecha_local);
CREATE TABLE lamontana.historial_estado_pedido (
 id_evento bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pedido bigint NOT NULL REFERENCES lamontana.pedido,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 id_operacion uuid NOT NULL UNIQUE,
 huella char(64) NOT NULL,
 estado_destino varchar(32) NOT NULL,
 motivo varchar(500) NOT NULL,
 fecha timestamptz NOT NULL
);
-- El mismo recurso no vuelve a tener cupo por cambiar versión. En el solapamiento DST
-- se cuenta la franja civil completa; un cambio de zona horaria tampoco duplica plazas.
CREATE FUNCTION lamontana.reservas_ocupadas(modo text,destino uuid,origen uuid,fecha date,abre time,cierra time,desde timestamptz,hasta timestamptz)
 RETURNS bigint LANGUAGE sql STABLE AS $$
 SELECT count(*) FROM lamontana.reserva_entrega r
 WHERE r.estado='ACTIVA' AND r.modalidad=modo AND r.destino_publico=destino
 AND (modo<>'RETIRO_PUNTO_ENTREGA' OR r.origen_publico=origen)
 AND ((r.fecha_local=fecha AND r.apertura<cierra AND r.cierre>abre) OR (r.franja_desde<hasta AND r.franja_hasta>desde))
$$;
CREATE FUNCTION lamontana.validar_pedido_completo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p lamontana.pedido; c lamontana.cotizacion; r lamontana.reserva_entrega;
BEGIN
 SELECT * INTO p FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido;
 SELECT * INTO c FROM lamontana.cotizacion WHERE id_cotizacion=p.id_cotizacion;
 SELECT * INTO r FROM lamontana.reserva_entrega WHERE id_pedido=p.id_pedido;
 IF c.estado<>'CONFIRMADA' OR c.aceptada_en IS NULL OR c.confirmada_en>=c.vigente_hasta
 OR p.confirmada_en<>c.confirmada_en OR p.id_sucursal<>c.id_sucursal OR p.id_usuario_creador<>c.id_usuario_creador
 OR p.id_configuracion_version<>c.id_configuracion_confirmacion OR p.total<>c.total
 OR r.id_pedido IS NULL OR r.costo<>c.costo_entrega OR r.modalidad<>c.oferta->>'modalidad'
 OR NOT EXISTS(SELECT 1 FROM lamontana.historial_estado_pedido h WHERE h.id_pedido=p.id_pedido)
 OR (SELECT count(*) FROM lamontana.pedido_item WHERE id_pedido=p.id_pedido)<>
    (SELECT count(*) FROM lamontana.cotizacion_item WHERE id_cotizacion=c.id_cotizacion)
 OR EXISTS(SELECT 1 FROM lamontana.pedido_item i JOIN lamontana.cotizacion_item ci USING(id_cotizacion_item)
 JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado)
 JOIN lamontana.aceptacion_vista_previa v USING(id_aceptacion_vista_previa)
 WHERE i.id_pedido=p.id_pedido AND (ci.id_cotizacion<>c.id_cotizacion OR a.estado<>'VALIDO' OR a.finalidad<>'TRABAJO'
 OR a.id_usuario_cargador<>c.id_usuario_creador OR a.cantidad_paginas<>ci.cantidad_paginas_declaradas
 OR v.id_archivo_almacenado<>a.id_archivo_almacenado OR v.id_usuario<>c.id_usuario_creador OR v.sha256<>a.sha256
 OR NOT EXISTS(SELECT 1 FROM lamontana.archivo_trabajo t WHERE t.id_archivo_almacenado=a.id_archivo_almacenado AND t.id_cotizacion_item=ci.id_cotizacion_item AND t.activo)
 OR (SELECT count(*) FROM lamontana.validacion_archivo va WHERE va.id_archivo_almacenado=a.id_archivo_almacenado AND va.resultado='ACEPTADO')<>2))
 THEN RAISE EXCEPTION 'Pedido incompleto o distinto de su oferta y PDF aceptados' USING ERRCODE='23514'; END IF;
 IF NOT EXISTS(
   SELECT 1 FROM lamontana.franja_entrega f
   LEFT JOIN lamontana.configuracion_punto_entrega cp USING(id_configuracion_punto_entrega)
   LEFT JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
   LEFT JOIN lamontana.configuracion_zona_entrega cz USING(id_configuracion_zona_entrega)
   LEFT JOIN lamontana.zona_entrega z USING(id_zona_entrega)
   WHERE f.habilitada AND f.dia_semana=extract(isodow FROM r.fecha_local)
   AND f.hora_desde=r.apertura AND f.hora_hasta=r.cierre AND f.capacidad_pedidos=r.cupo_confirmado
   AND ((r.modalidad='RETIRO_SUCURSAL' AND f.id_configuracion_retiro=p.id_configuracion_version AND f.id_sucursal_retiro=p.id_sucursal)
    OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND cp.habilitado AND cp.id_configuracion_version=p.id_configuracion_version AND cp.id_sucursal=p.id_sucursal AND pt.codigo_publico=r.destino_publico)
    OR (r.modalidad='ENVIO_DOMICILIO' AND cz.habilitada AND cz.id_configuracion_version=p.id_configuracion_version AND z.codigo_publico=r.destino_publico))
 ) THEN RAISE EXCEPTION 'La reserva debe corresponder a una franja configurada' USING ERRCODE='23514'; END IF;
 IF r.origen_publico<>(SELECT codigo_publico FROM lamontana.sucursal WHERE id_sucursal=p.id_sucursal)
 OR (r.modalidad='RETIRO_SUCURSAL' AND r.destino_publico<>r.origen_publico)
 OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND r.destino_publico<>(c.oferta->>'punto')::uuid)
 OR (r.modalidad='ENVIO_DOMICILIO' AND (p.receptor IS NULL OR p.telefono IS NULL))
 THEN RAISE EXCEPTION 'Destino del pedido inválido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER pedido_completo AFTER INSERT ON lamontana.pedido DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_pedido_completo();
CREATE FUNCTION lamontana.validar_cupo_reserva() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(764003);
 IF lamontana.reservas_ocupadas(NEW.modalidad,NEW.destino_publico,NEW.origen_publico,NEW.fecha_local,NEW.apertura,NEW.cierre,NEW.franja_desde,NEW.franja_hasta)>NEW.cupo_confirmado
 THEN RAISE EXCEPTION 'La franja no tiene cupo disponible' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER reserva_cupo AFTER INSERT ON lamontana.reserva_entrega DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_cupo_reserva();
CREATE TRIGGER pedido_inmutable BEFORE UPDATE OR DELETE ON lamontana.pedido FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER pedido_item_inmutable BEFORE UPDATE OR DELETE ON lamontana.pedido_item FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER reserva_inmutable BEFORE UPDATE OR DELETE ON lamontana.reserva_entrega FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER pedido_historia_inmutable BEFORE UPDATE OR DELETE ON lamontana.historial_estado_pedido FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();

CREATE FUNCTION lamontana.exigir_pedido_confirmado() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.estado='CONFIRMADA' AND NOT EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_cotizacion=NEW.id_cotizacion)
 THEN RAISE EXCEPTION 'La confirmación debe guardar su pedido en la misma transacción' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER cotizacion_con_pedido AFTER UPDATE ON lamontana.cotizacion DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_pedido_confirmado();
