-- Preparación privada del cliente, independiente de entrega, pago y pedidos confirmados.
CREATE TABLE lamontana.preparacion_cliente (
 id_preparacion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_usuario bigint NOT NULL REFERENCES lamontana.usuario,
 estado varchar(12) NOT NULL DEFAULT 'ABIERTA' CHECK(estado IN ('ABIERTA','CONFIRMADA')),
 version bigint NOT NULL DEFAULT 1 CHECK(version>0),
 items jsonb NOT NULL DEFAULT '[]',
 presupuesto jsonb,
 preferencias jsonb,
 id_ultima_cotizacion bigint REFERENCES lamontana.cotizacion,
 id_pedido bigint UNIQUE REFERENCES lamontana.pedido,
 creada_en timestamptz NOT NULL DEFAULT clock_timestamp(),
 actualizada_en timestamptz NOT NULL DEFAULT clock_timestamp(),
 CHECK((estado='CONFIRMADA')=(id_pedido IS NOT NULL))
);
CREATE UNIQUE INDEX una_preparacion_cliente ON lamontana.preparacion_cliente(id_usuario) WHERE estado='ABIERTA';
CREATE TABLE lamontana.archivo_preparado (
 id_archivo_almacenado bigint PRIMARY KEY REFERENCES lamontana.archivo_almacenado,
 id_preparacion bigint NOT NULL REFERENCES lamontana.preparacion_cliente,
 retirado_en timestamptz
);
CREATE INDEX preparados_por_cliente ON lamontana.archivo_preparado(id_preparacion);
CREATE TABLE lamontana.evento_preparacion (
 id_evento bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_preparacion bigint NOT NULL REFERENCES lamontana.preparacion_cliente,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 id_operacion uuid NOT NULL UNIQUE,
 tipo varchar(32) NOT NULL,
 huella char(64) NOT NULL,
 resultado jsonb NOT NULL,
 fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TRIGGER preparacion_historia_inmutable BEFORE UPDATE OR DELETE ON lamontana.evento_preparacion
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();

-- El mismo original puede acompañar sucesoras del MISMO trabajo, nunca dos pedidos.
-- La aplicación comprueba propiedad y cadena; pedido_item mantiene su unicidad global.
ALTER TABLE lamontana.archivo_trabajo DROP CONSTRAINT archivo_trabajo_id_archivo_almacenado_key;
CREATE UNIQUE INDEX archivo_en_item_unico ON lamontana.archivo_trabajo(id_archivo_almacenado,id_cotizacion_item);

CREATE OR REPLACE FUNCTION lamontana.exigir_preview_validada() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM lamontana.archivo_almacenado a
 WHERE a.id_archivo_almacenado=NEW.id_archivo_almacenado AND a.estado='VALIDO' AND a.finalidad='TRABAJO'
 AND a.sha256=NEW.sha256 AND a.id_usuario_cargador=NEW.id_usuario
 AND ((SELECT count(*) FROM lamontana.validacion_archivo v WHERE v.id_archivo_almacenado=a.id_archivo_almacenado AND v.resultado='ACEPTADO')=2)
 AND (EXISTS(SELECT 1 FROM lamontana.archivo_trabajo t WHERE t.id_archivo_almacenado=a.id_archivo_almacenado AND t.activo)
 OR EXISTS(SELECT 1 FROM lamontana.archivo_preparado ap JOIN lamontana.preparacion_cliente p USING(id_preparacion)
 WHERE ap.id_archivo_almacenado=a.id_archivo_almacenado AND ap.retirado_en IS NULL AND p.estado='ABIERTA' AND p.id_usuario=NEW.id_usuario)))
 THEN RAISE EXCEPTION 'Vista previa no aceptable' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

-- Los nuevos pedidos globales usan la reserva persistida como destino, no la ubicación productiva.
CREATE OR REPLACE FUNCTION lamontana.retiro_con_traslado(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT coalesce(c.oferta->>'modalidad'='RETIRO_SUCURSAL' AND e.id_sucursal_matriz IS NOT NULL
 AND CASE WHEN coalesce((c.oferta->>'origenGlobal')::boolean,false)
 THEN (SELECT s.codigo_publico FROM lamontana.sucursal s WHERE s.id_sucursal=e.id_sucursal_matriz)
  IS DISTINCT FROM (SELECT r.destino_publico FROM lamontana.reserva_entrega r WHERE r.id_pedido=p.id_pedido ORDER BY r.id_reserva_entrega DESC LIMIT 1)
 ELSE e.id_sucursal_matriz<>p.id_sucursal END,false)
 FROM lamontana.pedido_actual p JOIN lamontana.cotizacion c USING(id_cotizacion)
 LEFT JOIN lamontana.configuracion_entrega e ON e.id_configuracion_version=p.id_configuracion_version WHERE p.id_pedido=pedido
$$;

-- Conserva las invariantes de confirmación; el retiro puede tener un destino distinto al origen.
CREATE OR REPLACE FUNCTION lamontana.validar_pedido_completo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p lamontana.pedido; c lamontana.cotizacion; r lamontana.reserva_entrega;
BEGIN
 SELECT * INTO p FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido;
 SELECT * INTO c FROM lamontana.cotizacion WHERE id_cotizacion=p.id_cotizacion;
 SELECT * INTO r FROM lamontana.reserva_entrega WHERE id_pedido=p.id_pedido;
 IF c.estado<>'CONFIRMADA' OR c.aceptada_en IS NULL OR c.confirmada_en>=c.vigente_hasta
 OR p.confirmada_en<>c.confirmada_en OR p.id_sucursal<>c.id_sucursal OR p.id_usuario_creador<>c.id_usuario_creador
 OR p.id_configuracion_version<>c.id_configuracion_confirmacion OR p.total<>c.total
 OR (coalesce((c.oferta->>'origenGlobal')::boolean,false) AND NOT EXISTS(
 SELECT 1 FROM lamontana.preparacion_cliente pc WHERE pc.estado='CONFIRMADA' AND pc.id_pedido=p.id_pedido
 AND pc.id_ultima_cotizacion=c.id_cotizacion AND pc.id_usuario=c.id_usuario_creador))
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
   AND ((r.modalidad='RETIRO_SUCURSAL' AND f.id_configuracion_retiro=p.id_configuracion_version AND f.id_sucursal_retiro=(SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=r.destino_publico))
    OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND cp.habilitado AND cp.id_configuracion_version=p.id_configuracion_version AND cp.id_sucursal=p.id_sucursal AND pt.codigo_publico=r.destino_publico)
    OR (r.modalidad='ENVIO_DOMICILIO' AND cz.habilitada AND cz.id_configuracion_version=p.id_configuracion_version AND z.codigo_publico=r.destino_publico))
 ) THEN RAISE EXCEPTION 'La reserva debe corresponder a una franja configurada' USING ERRCODE='23514'; END IF;
 IF r.origen_publico<>(SELECT codigo_publico FROM lamontana.sucursal WHERE id_sucursal=p.id_sucursal)
 OR (r.modalidad='RETIRO_SUCURSAL' AND r.destino_publico<>CASE WHEN coalesce((c.oferta->>'origenGlobal')::boolean,false) THEN (c.oferta->'entrega'->'destinoSucursal'->>'sucursal')::uuid ELSE r.origen_publico END)
 OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND r.destino_publico<>(c.oferta->>'punto')::uuid)
 OR (r.modalidad='ENVIO_DOMICILIO' AND (p.receptor IS NULL OR p.telefono IS NULL))
 THEN RAISE EXCEPTION 'Destino del pedido inválido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

-- Impide reutilizar originales preparados en cotizaciones ajenas o trabajos ya confirmados.
CREATE FUNCTION lamontana.validar_archivo_preparado_vinculado() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE ap lamontana.archivo_preparado; pc lamontana.preparacion_cliente; c lamontana.cotizacion;
BEGIN
 SELECT * INTO ap FROM lamontana.archivo_preparado WHERE id_archivo_almacenado=NEW.id_archivo_almacenado;
 IF NOT FOUND THEN RETURN NEW; END IF;
 SELECT * INTO pc FROM lamontana.preparacion_cliente WHERE id_preparacion=ap.id_preparacion;
 SELECT q.* INTO c FROM lamontana.cotizacion_item i JOIN lamontana.cotizacion q USING(id_cotizacion) WHERE i.id_cotizacion_item=NEW.id_cotizacion_item;
 IF pc.estado<>'ABIERTA' OR ap.retirado_en IS NOT NULL OR c.id_usuario_creador<>pc.id_usuario
 OR NOT coalesce((c.oferta->>'origenGlobal')::boolean,false)
 OR c.id_cotizacion_reemplazada IS DISTINCT FROM pc.id_ultima_cotizacion
 OR EXISTS(SELECT 1 FROM lamontana.pedido_item WHERE id_archivo_almacenado=NEW.id_archivo_almacenado)
 THEN RAISE EXCEPTION 'El PDF no pertenece a esta preparación abierta y cadena de cotizaciones' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER archivo_preparado_vinculado BEFORE INSERT ON lamontana.archivo_trabajo
 FOR EACH ROW EXECUTE FUNCTION lamontana.validar_archivo_preparado_vinculado();

-- Conserva también el guard de correcciones y reprogramación (versión V33).
CREATE OR REPLACE FUNCTION lamontana.validar_reserva_version_trabajo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p lamontana.pedido_actual; c lamontana.cotizacion; r lamontana.reserva_entrega;
BEGIN
 SELECT * INTO p FROM lamontana.pedido_actual WHERE id_pedido=NEW.id_pedido;
 SELECT * INTO c FROM lamontana.cotizacion WHERE id_cotizacion=p.id_cotizacion;
 r:=NEW;
 IF r.estado<>'ACTIVA' OR r.modalidad<>c.oferta->>'modalidad' OR r.costo<>c.costo_entrega
 OR (r.franja_desde AT TIME ZONE r.zona_horaria)::date<>r.fecha_local
 OR (r.franja_desde AT TIME ZONE r.zona_horaria)::time<>r.apertura
 OR (r.franja_hasta AT TIME ZONE r.zona_horaria)::date<>r.fecha_local
 OR (r.franja_hasta AT TIME ZONE r.zona_horaria)::time<>r.cierre
 THEN RAISE EXCEPTION 'La reserva debe corresponder al trabajo y horario locales' USING ERRCODE='23514'; END IF;
 IF NOT EXISTS(
   SELECT 1 FROM lamontana.franja_entrega f
   LEFT JOIN lamontana.configuracion_punto_entrega cp USING(id_configuracion_punto_entrega)
   LEFT JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
   LEFT JOIN lamontana.configuracion_zona_entrega cz USING(id_configuracion_zona_entrega)
   LEFT JOIN lamontana.zona_entrega z USING(id_zona_entrega)
   WHERE f.habilitada AND f.dia_semana=extract(isodow FROM r.fecha_local)
   AND f.hora_desde=r.apertura AND f.hora_hasta=r.cierre AND f.capacidad_pedidos=r.cupo_confirmado
   AND ((r.modalidad='RETIRO_SUCURSAL' AND f.id_configuracion_retiro=coalesce(r.id_configuracion_reprogramacion,p.id_configuracion_version) AND f.id_sucursal_retiro=(SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=r.destino_publico))
    OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND cp.habilitado AND cp.id_configuracion_version=coalesce(r.id_configuracion_reprogramacion,p.id_configuracion_version) AND cp.id_sucursal=p.id_sucursal AND pt.codigo_publico=r.destino_publico)
    OR (r.modalidad='ENVIO_DOMICILIO' AND cz.habilitada AND cz.id_configuracion_version=coalesce(r.id_configuracion_reprogramacion,p.id_configuracion_version) AND z.codigo_publico=r.destino_publico))
 ) THEN RAISE EXCEPTION 'La reserva debe corresponder a una franja configurada' USING ERRCODE='23514'; END IF;
 IF r.origen_publico<>(SELECT codigo_publico FROM lamontana.sucursal WHERE id_sucursal=p.id_sucursal)
 OR (r.modalidad='RETIRO_SUCURSAL' AND r.destino_publico<>CASE WHEN coalesce((c.oferta->>'origenGlobal')::boolean,false) THEN (c.oferta->'entrega'->'destinoSucursal'->>'sucursal')::uuid ELSE r.origen_publico END)
 OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND r.destino_publico<>(c.oferta->>'punto')::uuid)
 OR (r.modalidad='ENVIO_DOMICILIO' AND (p.receptor IS NULL OR p.telefono IS NULL))
 THEN RAISE EXCEPTION 'Destino del pedido inválido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
