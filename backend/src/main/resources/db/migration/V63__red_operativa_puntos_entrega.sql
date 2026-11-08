-- Red de reparto independiente del motor. Cada edición conserva una revisión inmutable.
CREATE TABLE lamontana.version_red_punto (
 id_version_red_punto bigserial PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
 id_punto_entrega bigint NOT NULL REFERENCES lamontana.punto_entrega,
 version bigint NOT NULL CHECK(version>0),
 definicion jsonb NOT NULL CHECK(jsonb_typeof(definicion)='object'),
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 id_operacion uuid NOT NULL UNIQUE,
 huella text NOT NULL,
 fecha timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(id_punto_entrega,version), UNIQUE(id_punto_entrega,id_version_red_punto)
);
CREATE TABLE lamontana.red_punto_actual (
 id_punto_entrega bigint PRIMARY KEY REFERENCES lamontana.punto_entrega,
 id_version_red_punto bigint NOT NULL UNIQUE,
 FOREIGN KEY(id_punto_entrega,id_version_red_punto)
  REFERENCES lamontana.version_red_punto(id_punto_entrega,id_version_red_punto)
);
ALTER TABLE lamontana.reserva_entrega ADD COLUMN id_version_red_punto bigint REFERENCES lamontana.version_red_punto CHECK(id_version_red_punto IS NULL OR modalidad='RETIRO_PUNTO_ENTREGA');

-- Se importa la red vigente, sin modificar configuraciones, borradores ni pedidos históricos.
INSERT INTO lamontana.version_red_punto(id_punto_entrega,version,definicion,id_actor,id_operacion,huella)
SELECT p.id_punto_entrega,1,jsonb_build_object(
 'codigoPublico',p.codigo_publico,'codigo',p.codigo,'nombre',d.nombre,'calle',d.calle,
 'numero',d.numero,'localidad',d.localidad,'provincia',d.provincia,'codigoPostal',d.codigo_postal,
 'referencias',d.referencias,'zonaHoraria',d.zona_horaria,
 'sucursales',coalesce((SELECT jsonb_agg(jsonb_build_object(
   'sucursal',s.codigo_publico,'prioridad',r.prioridad,'habilitado',r.habilitado,'costo',r.costo::text,
   'franjas',coalesce((SELECT jsonb_agg(jsonb_build_object('dia',f.dia_semana,'apertura',to_char(f.hora_desde,'HH24:MI'),
     'cierre',to_char(f.hora_hasta,'HH24:MI'),'capacidadPedidos',f.capacidad_pedidos,'habilitada',f.habilitada)
     ORDER BY f.dia_semana,f.hora_desde) FROM lamontana.franja_entrega f
     WHERE f.id_configuracion_punto_entrega=r.id_configuracion_punto_entrega),'[]'::jsonb)) ORDER BY r.prioridad)
   FROM lamontana.configuracion_punto_entrega r JOIN lamontana.sucursal s USING(id_sucursal)
   WHERE r.id_configuracion_version=c.id_configuracion_version AND r.id_punto_entrega=p.id_punto_entrega),'[]'::jsonb)),
 c.id_usuario_creador,gen_random_uuid(),'IMPORTACION_RED_VIGENTE'
FROM lamontana.configuracion_version c JOIN lamontana.configuracion_definicion_punto d USING(id_configuracion_version)
JOIN lamontana.punto_entrega p USING(id_punto_entrega) WHERE c.estado='ACTIVA' AND EXISTS(SELECT 1 FROM lamontana.configuracion_modalidad_entrega m WHERE m.id_configuracion_version=c.id_configuracion_version AND m.modalidad='RETIRO_PUNTO_ENTREGA');
INSERT INTO lamontana.red_punto_actual SELECT id_punto_entrega,id_version_red_punto FROM lamontana.version_red_punto;

CREATE FUNCTION lamontana.conservar_version_red_punto() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Las versiones de la red de entrega son inmutables'; END;
$$;
CREATE TRIGGER conservar_version_red_punto BEFORE UPDATE OR DELETE ON lamontana.version_red_punto
 FOR EACH ROW EXECUTE FUNCTION lamontana.conservar_version_red_punto();

-- La reserva captura la revisión al insertarse; sus datos siguen siendo inmutables.
CREATE FUNCTION lamontana.franja_reserva_configurada(r lamontana.reserva_entrega,p_configuracion bigint,p_sucursal bigint)
 RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT CASE WHEN r.id_version_red_punto IS NOT NULL THEN
  r.modalidad='RETIRO_PUNTO_ENTREGA' AND EXISTS(
   SELECT 1 FROM lamontana.version_red_punto v JOIN lamontana.punto_entrega p USING(id_punto_entrega),
    jsonb_array_elements(v.definicion->'sucursales') ruta,
    jsonb_array_elements(ruta->'franjas') franja
   WHERE v.id_version_red_punto=r.id_version_red_punto AND p.codigo_publico=r.destino_publico
    AND (ruta->>'sucursal')::uuid=r.origen_publico
    AND r.origen_publico=(SELECT codigo_publico FROM lamontana.sucursal WHERE id_sucursal=p_sucursal)
    AND (ruta->>'habilitado')::boolean AND v.definicion->>'zonaHoraria'=r.zona_horaria
    AND (franja->>'habilitada')::boolean AND (franja->>'dia')::int=extract(isodow FROM r.fecha_local)
    AND (franja->>'apertura')::time=r.apertura AND (franja->>'cierre')::time=r.cierre
    AND (franja->>'capacidadPedidos')::int=r.cupo_confirmado
  )
 ELSE EXISTS(
   SELECT 1 FROM lamontana.franja_entrega f
   LEFT JOIN lamontana.configuracion_punto_entrega cp USING(id_configuracion_punto_entrega)
   LEFT JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
   LEFT JOIN lamontana.configuracion_zona_entrega cz USING(id_configuracion_zona_entrega)
   LEFT JOIN lamontana.zona_entrega z USING(id_zona_entrega)
   WHERE f.habilitada AND f.dia_semana=extract(isodow FROM r.fecha_local)
    AND f.hora_desde=r.apertura AND f.hora_hasta=r.cierre AND f.capacidad_pedidos=r.cupo_confirmado
    AND ((r.modalidad='RETIRO_SUCURSAL' AND f.id_configuracion_retiro=p_configuracion AND f.id_sucursal_retiro=(SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=r.destino_publico))
     OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND cp.habilitado AND cp.id_configuracion_version=p_configuracion AND cp.id_sucursal=p_sucursal AND pt.codigo_publico=r.destino_publico)
     OR (r.modalidad='ENVIO_DOMICILIO' AND cz.habilitada AND cz.id_configuracion_version=p_configuracion AND z.codigo_publico=r.destino_publico))
 ) END
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
 AND (pc.nombre_invitado IS NULL OR (r.origen_publico=pc.sucursal_mostrador AND
  ((r.modalidad='RETIRO_SUCURSAL' AND r.destino_publico=pc.sucursal_mostrador)
   OR r.modalidad='RETIRO_PUNTO_ENTREGA')))))
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
 IF NOT lamontana.franja_reserva_configurada(r,p.id_configuracion_version,p.id_sucursal) THEN RAISE EXCEPTION 'La reserva debe corresponder a una franja configurada' USING ERRCODE='23514'; END IF;
 IF r.origen_publico<>(SELECT codigo_publico FROM lamontana.sucursal WHERE id_sucursal=p.id_sucursal)
 OR (r.modalidad='RETIRO_SUCURSAL' AND r.destino_publico<>CASE WHEN coalesce((c.oferta->>'origenGlobal')::boolean,false) THEN (c.oferta->'entrega'->'destinoSucursal'->>'sucursal')::uuid ELSE r.origen_publico END)
 OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND r.destino_publico<>(c.oferta->>'punto')::uuid)
 OR (r.modalidad='ENVIO_DOMICILIO' AND (p.receptor IS NULL OR p.telefono IS NULL))
 THEN RAISE EXCEPTION 'Destino del pedido inválido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

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
 IF NOT lamontana.franja_reserva_configurada(r,coalesce(r.id_configuracion_reprogramacion,p.id_configuracion_version),p.id_sucursal) THEN RAISE EXCEPTION 'La reserva debe corresponder a una franja configurada' USING ERRCODE='23514'; END IF;
 IF r.origen_publico<>(SELECT codigo_publico FROM lamontana.sucursal WHERE id_sucursal=p.id_sucursal)
 OR (r.modalidad='RETIRO_SUCURSAL' AND r.destino_publico<>CASE WHEN coalesce((c.oferta->>'origenGlobal')::boolean,false) THEN (c.oferta->'entrega'->'destinoSucursal'->>'sucursal')::uuid ELSE r.origen_publico END)
 OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND r.destino_publico<>(c.oferta->>'punto')::uuid)
 OR (r.modalidad='ENVIO_DOMICILIO' AND (p.receptor IS NULL OR p.telefono IS NULL))
 THEN RAISE EXCEPTION 'Destino del pedido inválido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

-- Conserva la aceptación de una nueva franja junto con las acciones logísticas actuales.
ALTER TABLE lamontana.historial_estado_pedido DROP CONSTRAINT gestion_pedido_evento_check;
ALTER TABLE lamontana.historial_estado_pedido ADD CONSTRAINT gestion_pedido_evento_check CHECK(
 (version_pedido IS NULL AND accion IS NULL AND estado_origen IS NULL AND mensaje_cliente IS NULL AND actor_nombre IS NULL AND actor_tipo IS NULL AND id_actor IS NOT NULL)
 OR (version_pedido>1 AND accion IS NOT NULL AND estado_origen IS NOT NULL AND mensaje_cliente IS NOT NULL AND length(trim(mensaje_cliente))>0 AND actor_nombre IS NOT NULL
 AND ((actor_tipo IN ('CLIENTE','ADMIN_ADMIN','EMPLEADO') AND id_actor IS NOT NULL
 AND accion IN ('REVISAR','APROBAR','RECHAZAR','CANCELAR','PEDIR_CORRECCION','RESPONDER_CORRECCION','INICIAR_TRABAJO','COMPLETAR_TRABAJO','ERROR_TRABAJO','CANCELAR_TRABAJO','CONTROLAR_CALIDAD','PREPARAR_ENVIO','SALIR_REPARTO','LLEGAR_PUNTO','CLIENTE_AUSENTE','REINTENTAR_ENTREGA','REPROGRAMAR','ENTREGAR','CERRAR'))
 OR (actor_tipo='SISTEMA' AND id_actor IS NULL AND accion='CANCELAR'))));
