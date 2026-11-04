-- Titular técnico para conservar las relaciones de pedidos y PDF. No admite sesión.
INSERT INTO lamontana.rol(codigo,nombre,es_interno,activo)
VALUES ('INVITADO_MOSTRADOR','Invitado de mostrador',false,false);
ALTER TABLE lamontana.preparacion_cliente
 ADD COLUMN nombre_invitado varchar(100),
 ADD COLUMN sucursal_mostrador uuid REFERENCES lamontana.sucursal(codigo_publico),
 ADD CONSTRAINT preparacion_invitado_datos CHECK (
  (nombre_invitado IS NULL AND sucursal_mostrador IS NULL)
  OR (length(trim(nombre_invitado))>0 AND sucursal_mostrador IS NOT NULL AND id_operador IS NOT NULL AND nombre_invitado IS NOT NULL)
 );

DROP INDEX lamontana.una_preparacion_por_operador_cliente;
CREATE UNIQUE INDEX una_preparacion_por_operador_cliente ON lamontana.preparacion_cliente(id_operador,id_usuario)
 WHERE estado='ABIERTA' AND id_operador IS NOT NULL AND nombre_invitado IS NULL;

CREATE VIEW lamontana.destinatario_cotizacion AS
 SELECT c.id_cotizacion, coalesce(pc.nombre_invitado,u.nombre||' '||u.apellido) AS nombre,
        CASE WHEN pc.nombre_invitado IS NULL THEN u.correo END AS correo,
        pc.nombre_invitado IS NOT NULL AS invitado
 FROM lamontana.cotizacion c JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador
 LEFT JOIN lamontana.preparacion_cliente pc ON pc.id_preparacion=c.id_preparacion_interna;

CREATE OR REPLACE FUNCTION lamontana.retiro_con_traslado(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT coalesce(pc.nombre_invitado IS NULL AND c.oferta->>'modalidad'='RETIRO_SUCURSAL' AND e.id_sucursal_matriz IS NOT NULL
 AND CASE WHEN coalesce((c.oferta->>'origenGlobal')::boolean,false)
 THEN (SELECT s.codigo_publico FROM lamontana.sucursal s WHERE s.id_sucursal=e.id_sucursal_matriz)
  IS DISTINCT FROM (SELECT r.destino_publico FROM lamontana.reserva_entrega r WHERE r.id_pedido=p.id_pedido ORDER BY r.id_reserva_entrega DESC LIMIT 1)
 ELSE e.id_sucursal_matriz<>p.id_sucursal END,false)
 FROM lamontana.pedido_actual p JOIN lamontana.cotizacion c USING(id_cotizacion)
 LEFT JOIN lamontana.preparacion_cliente pc ON pc.id_preparacion=c.id_preparacion_interna
 LEFT JOIN lamontana.configuracion_entrega e ON e.id_configuracion_version=p.id_configuracion_version WHERE p.id_pedido=pedido
$$;

CREATE OR REPLACE FUNCTION lamontana.validar_pedido_completo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p lamontana.pedido; c lamontana.cotizacion; r lamontana.reserva_entrega;
BEGIN
 SELECT * INTO p FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido;
 SELECT * INTO c FROM lamontana.cotizacion WHERE id_cotizacion=p.id_cotizacion;
 SELECT * INTO r FROM lamontana.reserva_entrega WHERE id_pedido=p.id_pedido;
 IF c.estado<>'CONFIRMADA' OR c.aceptada_en IS NULL OR c.confirmada_en>=c.vigente_hasta
 OR p.confirmada_en<>c.confirmada_en OR p.id_sucursal<>c.id_sucursal OR p.id_usuario_creador<>c.id_usuario_creador
 OR p.id_configuracion_version<>c.id_configuracion_confirmacion OR p.total<>c.total
 OR ((coalesce((c.oferta->>'origenGlobal')::boolean,false) OR c.id_preparacion_interna IS NOT NULL) AND NOT EXISTS(
 SELECT 1 FROM lamontana.preparacion_cliente pc WHERE pc.estado='CONFIRMADA' AND pc.id_pedido=p.id_pedido
 AND pc.id_ultima_cotizacion=c.id_cotizacion AND pc.id_usuario=c.id_usuario_creador
 AND (c.id_preparacion_interna IS NULL OR pc.id_preparacion=c.id_preparacion_interna)
 AND (pc.nombre_invitado IS NULL OR (r.modalidad='RETIRO_SUCURSAL' AND r.origen_publico=pc.sucursal_mostrador AND r.destino_publico=pc.sucursal_mostrador))))
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

CREATE OR REPLACE FUNCTION lamontana.validar_archivo_preparado_vinculado() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE ap lamontana.archivo_preparado; pc lamontana.preparacion_cliente; c lamontana.cotizacion;
BEGIN
 SELECT * INTO ap FROM lamontana.archivo_preparado WHERE id_archivo_almacenado=NEW.id_archivo_almacenado;
 IF NOT FOUND THEN RETURN NEW; END IF;
 SELECT * INTO pc FROM lamontana.preparacion_cliente WHERE id_preparacion=ap.id_preparacion;
 SELECT q.* INTO c FROM lamontana.cotizacion_item i JOIN lamontana.cotizacion q USING(id_cotizacion) WHERE i.id_cotizacion_item=NEW.id_cotizacion_item;
 IF pc.estado<>'ABIERTA' OR ap.retirado_en IS NOT NULL OR c.id_usuario_creador<>pc.id_usuario
 OR (pc.nombre_invitado IS NULL AND NOT coalesce((c.oferta->>'origenGlobal')::boolean,false))
 OR (pc.nombre_invitado IS NOT NULL AND c.id_preparacion_interna IS DISTINCT FROM pc.id_preparacion)
 OR c.id_cotizacion_reemplazada IS DISTINCT FROM pc.id_ultima_cotizacion
 OR EXISTS(SELECT 1 FROM lamontana.pedido_item WHERE id_archivo_almacenado=NEW.id_archivo_almacenado)
 THEN RAISE EXCEPTION 'El PDF no pertenece a esta preparación abierta y cadena de cotizaciones' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
