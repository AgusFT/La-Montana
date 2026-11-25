-- La confirmación original conserva su oferta y sus PDF. Cada respuesta del cliente
-- guarda otra versión completa del trabajo, que vuelve a revisión humana.
CREATE TABLE lamontana.solicitud_correccion (
 id_solicitud_correccion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_pedido bigint NOT NULL REFERENCES lamontana.pedido,
 id_solicitante bigint NOT NULL REFERENCES lamontana.usuario,
 solicitante_nombre varchar(300) NOT NULL,
 tipo varchar(16) NOT NULL CHECK(tipo IN ('ARCHIVO','DATOS','ENTREGA','CANTIDAD','OTRO')),
 motivo varchar(500) NOT NULL CHECK(length(trim(motivo))>0),
 mensaje_cliente varchar(2000) NOT NULL CHECK(length(trim(mensaje_cliente))>0),
 estado varchar(16) NOT NULL DEFAULT 'PENDIENTE' CHECK(estado IN ('PENDIENTE','RESPONDIDA','CERRADA','CANCELADA')),
 fecha_solicitud timestamptz NOT NULL,
 fecha_cierre timestamptz,
 version_pedido_inicio bigint NOT NULL CHECK(version_pedido_inicio>1),
 id_cotizacion_base bigint NOT NULL REFERENCES lamontana.cotizacion,
 id_configuracion_base bigint NOT NULL REFERENCES lamontana.configuracion_version,
 condiciones_base jsonb NOT NULL CHECK(jsonb_typeof(condiciones_base)='object'),
 total_base numeric(19,2) NOT NULL CHECK(total_base>=0),
 receptor_base varchar(140), telefono_base varchar(40),
 CHECK((estado IN ('CERRADA','CANCELADA'))=(fecha_cierre IS NOT NULL)),
 CHECK(fecha_cierre IS NULL OR fecha_cierre>=fecha_solicitud)
);
CREATE UNIQUE INDEX correccion_activa_pedido ON lamontana.solicitud_correccion(id_pedido) WHERE estado IN ('PENDIENTE','RESPONDIDA');
-- Captura también los ítems no observados: permite comprobar que una respuesta sin
-- recotización sólo cambia los PDF expresamente pedidos, sin perder los demás.
CREATE TABLE lamontana.solicitud_correccion_item (
 id_solicitud_correccion bigint NOT NULL REFERENCES lamontana.solicitud_correccion,
 id_cotizacion_item bigint NOT NULL REFERENCES lamontana.cotizacion_item,
 id_archivo_observado bigint NOT NULL REFERENCES lamontana.archivo_almacenado,
 requiere_reemplazo boolean NOT NULL,
 PRIMARY KEY(id_solicitud_correccion,id_cotizacion_item)
);
CREATE TABLE lamontana.cotizacion_correccion (
 id_cotizacion bigint PRIMARY KEY REFERENCES lamontana.cotizacion,
 id_solicitud_correccion bigint NOT NULL REFERENCES lamontana.solicitud_correccion
);
ALTER TABLE lamontana.archivo_trabajo ADD COLUMN id_solicitud_correccion bigint REFERENCES lamontana.solicitud_correccion;
CREATE TABLE lamontana.respuesta_correccion (
 id_respuesta_correccion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_solicitud_correccion bigint NOT NULL UNIQUE REFERENCES lamontana.solicitud_correccion,
 id_autor bigint NOT NULL REFERENCES lamontana.usuario,
 mensaje varchar(2000) NOT NULL CHECK(length(trim(mensaje))>0),
 fecha timestamptz NOT NULL,
 version_pedido bigint NOT NULL CHECK(version_pedido>1),
 id_cotizacion bigint NOT NULL REFERENCES lamontana.cotizacion,
 id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
 condiciones_finales jsonb NOT NULL CHECK(jsonb_typeof(condiciones_finales)='object'),
 total numeric(19,2) NOT NULL CHECK(total>=0),
 receptor varchar(140), telefono varchar(40),
 CHECK((receptor IS NULL)=(telefono IS NULL))
);
CREATE TABLE lamontana.respuesta_correccion_archivo (
 id_respuesta_correccion bigint NOT NULL REFERENCES lamontana.respuesta_correccion,
 id_cotizacion_item bigint NOT NULL REFERENCES lamontana.cotizacion_item,
 id_archivo_almacenado bigint NOT NULL REFERENCES lamontana.archivo_almacenado,
 id_aceptacion_vista_previa bigint NOT NULL REFERENCES lamontana.aceptacion_vista_previa,
 PRIMARY KEY(id_respuesta_correccion,id_cotizacion_item),
 UNIQUE(id_respuesta_correccion,id_archivo_almacenado)
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['solicitud_correccion_item','cotizacion_correccion','respuesta_correccion','respuesta_correccion_archivo'] LOOP
  EXECUTE format('CREATE TRIGGER %I BEFORE UPDATE OR DELETE ON lamontana.%I FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion()',t||'_inmutable',t);
 END LOOP;
END $$;
CREATE FUNCTION lamontana.proteger_solicitud_correccion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR (to_jsonb(NEW)-ARRAY['estado','fecha_cierre']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['estado','fecha_cierre'])
 OR NOT ((OLD.estado='PENDIENTE' AND NEW.estado IN ('RESPONDIDA','CANCELADA')) OR (OLD.estado='RESPONDIDA' AND NEW.estado IN ('CERRADA','CANCELADA')))
 THEN RAISE EXCEPTION 'La solicitud conserva su contenido e historia' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER solicitud_correccion_historia BEFORE UPDATE OR DELETE ON lamontana.solicitud_correccion FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_solicitud_correccion();

CREATE VIEW lamontana.pedido_actual AS
 SELECT p.id_pedido,p.codigo_publico,p.id_cotizacion AS id_cotizacion_original,
 coalesce(r.id_cotizacion,p.id_cotizacion) AS id_cotizacion,p.id_usuario_creador,p.id_sucursal,
 coalesce(r.id_configuracion_version,p.id_configuracion_version) AS id_configuracion_version,
 p.estado,p.modo_aprobacion,p.confirmada_en,p.aprobada_en,p.version,
 coalesce(r.total,p.total) AS total,coalesce(r.condiciones_finales,p.condiciones_finales) AS condiciones_finales,
 CASE WHEN r.id_respuesta_correccion IS NULL THEN p.receptor ELSE r.receptor END AS receptor,
 CASE WHEN r.id_respuesta_correccion IS NULL THEN p.telefono ELSE r.telefono END AS telefono,
 p.revisada_en,p.id_revisor,p.id_aprobador,p.finalizada_en,r.id_respuesta_correccion
 FROM lamontana.pedido p LEFT JOIN LATERAL (
  SELECT r.* FROM lamontana.respuesta_correccion r JOIN lamontana.solicitud_correccion s USING(id_solicitud_correccion)
  WHERE s.id_pedido=p.id_pedido ORDER BY r.version_pedido DESC LIMIT 1
 ) r ON true;
CREATE VIEW lamontana.pedido_archivo_actual AS
 SELECT p.id_pedido,a.id_cotizacion_item,a.id_archivo_almacenado,a.id_aceptacion_vista_previa
 FROM lamontana.pedido_actual p JOIN lamontana.respuesta_correccion_archivo a USING(id_respuesta_correccion)
 UNION ALL SELECT p.id_pedido,i.id_cotizacion_item,i.id_archivo_almacenado,i.id_aceptacion_vista_previa
 FROM lamontana.pedido_actual p JOIN lamontana.pedido_item i USING(id_pedido) WHERE p.id_respuesta_correccion IS NULL;

ALTER TABLE lamontana.pedido DROP CONSTRAINT pedido_estado_check;
ALTER TABLE lamontana.pedido ADD CONSTRAINT pedido_estado_check CHECK(estado IN ('PENDIENTE_REVISION','CORRECCION_SOLICITADA','APROBADO','RECHAZADO','CANCELADO'));
ALTER TABLE lamontana.historial_estado_pedido DROP CONSTRAINT gestion_pedido_evento_check;
ALTER TABLE lamontana.historial_estado_pedido ALTER COLUMN mensaje_cliente TYPE varchar(2000);
ALTER TABLE lamontana.historial_estado_pedido ADD CONSTRAINT gestion_pedido_evento_check CHECK(
 (version_pedido IS NULL AND accion IS NULL AND estado_origen IS NULL AND mensaje_cliente IS NULL AND actor_nombre IS NULL AND actor_tipo IS NULL)
 OR (version_pedido IS NOT NULL AND version_pedido>1 AND accion IS NOT NULL AND accion IN ('REVISAR','APROBAR','RECHAZAR','CANCELAR','PEDIR_CORRECCION','RESPONDER_CORRECCION') AND estado_origen IS NOT NULL
  AND mensaje_cliente IS NOT NULL AND length(trim(mensaje_cliente))>0 AND actor_nombre IS NOT NULL AND actor_tipo IS NOT NULL AND actor_tipo IN ('CLIENTE','ADMIN_ADMIN','EMPLEADO')));
ALTER TABLE lamontana.reserva_entrega DROP CONSTRAINT reserva_entrega_id_pedido_key;
ALTER TABLE lamontana.reserva_entrega DROP CONSTRAINT reserva_entrega_estado_check;
ALTER TABLE lamontana.reserva_entrega ADD CONSTRAINT reserva_entrega_estado_check CHECK(estado IN ('ACTIVA','LIBERADA','REEMPLAZADA'));
CREATE UNIQUE INDEX reserva_activa_pedido ON lamontana.reserva_entrega(id_pedido) WHERE estado='ACTIVA';

CREATE FUNCTION lamontana.reservas_ocupadas(modo text,destino uuid,origen uuid,fecha date,abre time,cierra time,desde timestamptz,hasta timestamptz,excluir_pedido bigint)
 RETURNS bigint LANGUAGE sql STABLE AS $$
 SELECT count(*) FROM lamontana.reserva_entrega r WHERE r.estado='ACTIVA' AND (excluir_pedido IS NULL OR r.id_pedido<>excluir_pedido)
 AND r.modalidad=modo AND r.destino_publico=destino AND (modo<>'RETIRO_PUNTO_ENTREGA' OR r.origen_publico=origen)
 AND ((r.fecha_local=fecha AND r.apertura<cierra AND r.cierre>abre) OR (r.franja_desde<hasta AND r.franja_hasta>desde))
$$;
CREATE OR REPLACE FUNCTION lamontana.reservas_ocupadas(modo text,destino uuid,origen uuid,fecha date,abre time,cierra time,desde timestamptz,hasta timestamptz)
 RETURNS bigint LANGUAGE sql STABLE AS $$ SELECT lamontana.reservas_ocupadas(modo,destino,origen,fecha,abre,cierra,desde,hasta,NULL) $$;

CREATE OR REPLACE FUNCTION lamontana.controlar_transicion_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'No se elimina la historia del pedido' USING ERRCODE='23514'; END IF;
 IF (to_jsonb(NEW)-ARRAY['estado','version','revisada_en','id_revisor','aprobada_en','id_aprobador','finalizada_en']) IS DISTINCT FROM
    (to_jsonb(OLD)-ARRAY['estado','version','revisada_en','id_revisor','aprobada_en','id_aprobador','finalizada_en'])
 OR NEW.version<>OLD.version+1 OR OLD.estado NOT IN ('PENDIENTE_REVISION','CORRECCION_SOLICITADA','APROBADO')
 THEN RAISE EXCEPTION 'La transición conserva la confirmación original' USING ERRCODE='23514'; END IF;
 IF (OLD.estado='PENDIENTE_REVISION' AND NEW.estado='CORRECCION_SOLICITADA') OR (OLD.estado='CORRECCION_SOLICITADA' AND NEW.estado='PENDIENTE_REVISION') THEN
  IF NEW.revisada_en IS NOT NULL OR NEW.id_revisor IS NOT NULL OR NEW.aprobada_en IS NOT NULL OR NEW.id_aprobador IS NOT NULL OR NEW.finalizada_en IS NOT NULL
  THEN RAISE EXCEPTION 'El contenido corregido necesita otra revisión' USING ERRCODE='23514'; END IF;
 ELSIF NEW.estado=OLD.estado THEN
  IF OLD.estado<>'PENDIENTE_REVISION' OR OLD.revisada_en IS NOT NULL OR NEW.revisada_en IS NULL
  OR NEW.id_revisor IS NULL OR NEW.aprobada_en IS DISTINCT FROM OLD.aprobada_en OR NEW.id_aprobador IS DISTINCT FROM OLD.id_aprobador OR NEW.finalizada_en IS NOT NULL
  THEN RAISE EXCEPTION 'Revisión inválida' USING ERRCODE='23514'; END IF;
 ELSE
  IF NEW.revisada_en IS DISTINCT FROM OLD.revisada_en OR NEW.id_revisor IS DISTINCT FROM OLD.id_revisor
  THEN RAISE EXCEPTION 'La decisión conserva la revisión realizada' USING ERRCODE='23514'; END IF;
  IF NEW.estado='APROBADO' THEN
   IF OLD.estado<>'PENDIENTE_REVISION' OR OLD.revisada_en IS NULL OR NEW.aprobada_en IS NULL OR NEW.id_aprobador IS NULL OR NEW.finalizada_en IS NOT NULL
   THEN RAISE EXCEPTION 'Aprobación sin revisión' USING ERRCODE='23514'; END IF;
  ELSIF NEW.estado IN ('RECHAZADO','CANCELADO') THEN
   IF (NEW.estado='RECHAZADO' AND OLD.estado NOT IN ('PENDIENTE_REVISION','CORRECCION_SOLICITADA')) OR NEW.finalizada_en IS NULL
   OR NEW.aprobada_en IS DISTINCT FROM OLD.aprobada_en OR NEW.id_aprobador IS DISTINCT FROM OLD.id_aprobador
   THEN RAISE EXCEPTION 'Finalización inválida' USING ERRCODE='23514'; END IF;
  ELSE RAISE EXCEPTION 'Transición no admitida' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION lamontana.controlar_liberacion_reserva() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'La reserva se conserva como historia' USING ERRCODE='23514'; END IF;
 IF (to_jsonb(NEW)-'estado') IS DISTINCT FROM (to_jsonb(OLD)-'estado') OR OLD.estado<>'ACTIVA'
 OR NOT ((NEW.estado='LIBERADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado IN ('RECHAZADO','CANCELADO')))
  OR (NEW.estado='REEMPLAZADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado='CORRECCION_SOLICITADA')))
 THEN RAISE EXCEPTION 'Liberación de reserva inválida' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION lamontana.validar_transicion_pedido_completa() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE h lamontana.historial_estado_pedido; activas bigint; accion_esperada text;
BEGIN
 SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_pedido=NEW.id_pedido AND version_pedido=NEW.version;
 SELECT count(*) INTO activas FROM lamontana.reserva_entrega WHERE id_pedido=NEW.id_pedido AND estado='ACTIVA';
 accion_esperada:=CASE WHEN NEW.estado='CORRECCION_SOLICITADA' THEN 'PEDIR_CORRECCION' WHEN OLD.estado='CORRECCION_SOLICITADA' AND NEW.estado='PENDIENTE_REVISION' THEN 'RESPONDER_CORRECCION'
 WHEN NEW.estado=OLD.estado THEN 'REVISAR' WHEN NEW.estado='APROBADO' THEN 'APROBAR' WHEN NEW.estado='RECHAZADO' THEN 'RECHAZAR' ELSE 'CANCELAR' END;
 IF h.id_evento IS NULL OR h.estado_origen<>OLD.estado OR h.estado_destino<>NEW.estado OR h.accion<>accion_esperada
 OR activas<>(CASE WHEN NEW.estado IN ('CANCELADO','RECHAZADO') THEN 0 ELSE 1 END)
 OR (h.accion='REVISAR' AND (h.id_actor<>NEW.id_revisor OR h.fecha<>NEW.revisada_en))
 OR (h.accion='APROBAR' AND (h.id_actor<>NEW.id_aprobador OR h.fecha<>NEW.aprobada_en))
 OR (h.accion IN ('RECHAZAR','CANCELAR') AND h.fecha<>NEW.finalizada_en)
 OR (h.actor_tipo='CLIENTE' AND (h.accion NOT IN ('CANCELAR','RESPONDER_CORRECCION') OR h.id_actor<>NEW.id_usuario_creador))
 OR (h.accion='RESPONDER_CORRECCION' AND h.actor_tipo<>'CLIENTE')
 OR (h.accion='PEDIR_CORRECCION' AND NOT EXISTS(SELECT 1 FROM lamontana.solicitud_correccion s WHERE s.id_pedido=NEW.id_pedido AND s.estado='PENDIENTE' AND s.version_pedido_inicio=NEW.version))
 OR (h.accion='RESPONDER_CORRECCION' AND NOT EXISTS(SELECT 1 FROM lamontana.respuesta_correccion r JOIN lamontana.solicitud_correccion s USING(id_solicitud_correccion) WHERE s.id_pedido=NEW.id_pedido AND s.estado='RESPONDIDA' AND r.version_pedido=NEW.version AND r.id_autor=h.id_actor))
 OR (NEW.estado IN ('APROBADO','RECHAZADO','CANCELADO') AND EXISTS(SELECT 1 FROM lamontana.solicitud_correccion WHERE id_pedido=NEW.id_pedido AND estado IN ('PENDIENTE','RESPONDIDA')))
 THEN RAISE EXCEPTION 'Estado, corrección, reserva y auditoría deben actualizarse juntos' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION lamontana.exigir_pedido_confirmado() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.estado='CONFIRMADA' AND NOT EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_cotizacion=NEW.id_cotizacion)
 AND NOT EXISTS(SELECT 1 FROM lamontana.respuesta_correccion WHERE id_cotizacion=NEW.id_cotizacion)
 THEN RAISE EXCEPTION 'La oferta confirmada debe pertenecer a un pedido o respuesta' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

CREATE FUNCTION lamontana.validar_cotizacion_de_correccion() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE s lamontana.solicitud_correccion; c lamontana.cotizacion; p lamontana.pedido;
BEGIN
 SELECT * INTO s FROM lamontana.solicitud_correccion WHERE id_solicitud_correccion=NEW.id_solicitud_correccion;
 SELECT * INTO c FROM lamontana.cotizacion WHERE id_cotizacion=NEW.id_cotizacion;
 SELECT * INTO p FROM lamontana.pedido WHERE id_pedido=s.id_pedido;
 IF s.estado<>'PENDIENTE' OR p.estado<>'CORRECCION_SOLICITADA' OR c.estado<>'VIGENTE'
 OR c.id_usuario_creador<>p.id_usuario_creador OR c.id_sucursal<>p.id_sucursal
 OR (c.id_cotizacion_reemplazada IS DISTINCT FROM s.id_cotizacion_base AND NOT EXISTS(
  SELECT 1 FROM lamontana.cotizacion_correccion WHERE id_cotizacion=c.id_cotizacion_reemplazada AND id_solicitud_correccion=s.id_solicitud_correccion))
 THEN RAISE EXCEPTION 'La nueva oferta debe continuar la corrección del mismo pedido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER cotizacion_correccion_valida BEFORE INSERT ON lamontana.cotizacion_correccion FOR EACH ROW EXECUTE FUNCTION lamontana.validar_cotizacion_de_correccion();
CREATE FUNCTION lamontana.impedir_segundo_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM lamontana.cotizacion_correccion WHERE id_cotizacion=NEW.id_cotizacion)
 THEN RAISE EXCEPTION 'Una corrección no crea otro pedido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER pedido_cotizacion_original BEFORE INSERT ON lamontana.pedido FOR EACH ROW EXECUTE FUNCTION lamontana.impedir_segundo_pedido();

CREATE FUNCTION lamontana.validar_solicitud_completa() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p lamontana.pedido_actual;
BEGIN
 SELECT * INTO p FROM lamontana.pedido_actual WHERE id_pedido=NEW.id_pedido;
 IF p.estado<>'CORRECCION_SOLICITADA' OR p.version<>NEW.version_pedido_inicio OR p.id_cotizacion<>NEW.id_cotizacion_base
 OR p.id_configuracion_version<>NEW.id_configuracion_base OR p.total<>NEW.total_base OR p.condiciones_finales<>NEW.condiciones_base
 OR p.receptor IS DISTINCT FROM NEW.receptor_base OR p.telefono IS DISTINCT FROM NEW.telefono_base
 OR (SELECT count(*) FROM lamontana.solicitud_correccion_item WHERE id_solicitud_correccion=NEW.id_solicitud_correccion)<>
    (SELECT count(*) FROM lamontana.pedido_archivo_actual WHERE id_pedido=NEW.id_pedido)
 OR EXISTS(SELECT 1 FROM lamontana.solicitud_correccion_item i WHERE i.id_solicitud_correccion=NEW.id_solicitud_correccion
  AND NOT EXISTS(SELECT 1 FROM lamontana.pedido_archivo_actual a WHERE a.id_pedido=NEW.id_pedido AND a.id_cotizacion_item=i.id_cotizacion_item AND a.id_archivo_almacenado=i.id_archivo_observado))
 OR (NEW.tipo='ARCHIVO' AND NOT EXISTS(SELECT 1 FROM lamontana.solicitud_correccion_item WHERE id_solicitud_correccion=NEW.id_solicitud_correccion AND requiere_reemplazo))
 THEN RAISE EXCEPTION 'La solicitud debe capturar el trabajo vigente' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER solicitud_correccion_completa AFTER INSERT ON lamontana.solicitud_correccion DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_solicitud_completa();

CREATE FUNCTION lamontana.validar_respuesta_completa() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE s lamontana.solicitud_correccion; p lamontana.pedido; c lamontana.cotizacion; reserva lamontana.reserva_entrega;
BEGIN
 SELECT * INTO s FROM lamontana.solicitud_correccion WHERE id_solicitud_correccion=NEW.id_solicitud_correccion;
 SELECT * INTO p FROM lamontana.pedido WHERE id_pedido=s.id_pedido;
 SELECT * INTO c FROM lamontana.cotizacion WHERE id_cotizacion=NEW.id_cotizacion;
 SELECT * INTO reserva FROM lamontana.reserva_entrega WHERE id_pedido=p.id_pedido AND estado='ACTIVA';
 IF s.estado<>'RESPONDIDA' OR p.estado<>'PENDIENTE_REVISION' OR p.version<>NEW.version_pedido OR NEW.id_autor<>p.id_usuario_creador
 OR NEW.fecha<s.fecha_solicitud OR c.estado<>'CONFIRMADA' OR c.id_usuario_creador<>p.id_usuario_creador OR c.id_sucursal<>p.id_sucursal
 OR NEW.total<>c.total OR c.aceptada_en IS NULL OR reserva.id_reserva_entrega IS NULL OR reserva.modalidad<>c.oferta->>'modalidad' OR reserva.costo<>c.costo_entrega
 OR (c.oferta->>'modalidad'='ENVIO_DOMICILIO') IS DISTINCT FROM (NEW.receptor IS NOT NULL AND NEW.telefono IS NOT NULL)
 OR (SELECT count(*) FROM lamontana.respuesta_correccion_archivo WHERE id_respuesta_correccion=NEW.id_respuesta_correccion)<>
    (SELECT count(*) FROM lamontana.cotizacion_item WHERE id_cotizacion=NEW.id_cotizacion)
 OR EXISTS(SELECT 1 FROM lamontana.respuesta_correccion_archivo f JOIN lamontana.cotizacion_item i USING(id_cotizacion_item)
  JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) JOIN lamontana.aceptacion_vista_previa v USING(id_aceptacion_vista_previa)
  WHERE f.id_respuesta_correccion=NEW.id_respuesta_correccion AND (i.id_cotizacion<>NEW.id_cotizacion OR a.estado<>'VALIDO' OR a.finalidad<>'TRABAJO'
   OR a.id_usuario_cargador<>NEW.id_autor OR a.cantidad_paginas<>i.cantidad_paginas_declaradas OR v.id_archivo_almacenado<>a.id_archivo_almacenado
   OR v.id_usuario<>NEW.id_autor OR v.sha256<>a.sha256
   OR NOT EXISTS(SELECT 1 FROM lamontana.archivo_trabajo t WHERE t.id_archivo_almacenado=a.id_archivo_almacenado AND t.id_cotizacion_item=i.id_cotizacion_item)
   OR (SELECT count(*) FROM lamontana.validacion_archivo va WHERE va.id_archivo_almacenado=a.id_archivo_almacenado AND va.resultado='ACEPTADO')<>2))
 THEN RAISE EXCEPTION 'La respuesta exige su oferta, reserva y PDF aceptados completos' USING ERRCODE='23514'; END IF;
 IF NEW.id_cotizacion=s.id_cotizacion_base THEN
  IF s.tipo IN ('ENTREGA','CANTIDAD') OR NEW.total<>s.total_base OR NEW.condiciones_finales<>s.condiciones_base OR NEW.id_configuracion_version<>s.id_configuracion_base
  OR (s.tipo NOT IN ('DATOS','OTRO') AND (NEW.receptor IS DISTINCT FROM s.receptor_base OR NEW.telefono IS DISTINCT FROM s.telefono_base))
  OR EXISTS(SELECT 1 FROM lamontana.cotizacion_correccion WHERE id_solicitud_correccion=s.id_solicitud_correccion)
  OR EXISTS(SELECT 1 FROM lamontana.respuesta_correccion_archivo f JOIN lamontana.solicitud_correccion_item i USING(id_cotizacion_item)
   JOIN lamontana.archivo_trabajo t USING(id_archivo_almacenado)
   WHERE f.id_respuesta_correccion=NEW.id_respuesta_correccion AND i.id_solicitud_correccion=s.id_solicitud_correccion
   AND ((i.requiere_reemplazo AND (f.id_archivo_almacenado=i.id_archivo_observado OR t.id_solicitud_correccion IS DISTINCT FROM s.id_solicitud_correccion))
    OR (NOT i.requiere_reemplazo AND f.id_archivo_almacenado<>i.id_archivo_observado)))
  THEN RAISE EXCEPTION 'Sin nueva oferta sólo se corrigen los PDF indicados' USING ERRCODE='23514'; END IF;
 ELSE
  IF NOT EXISTS(SELECT 1 FROM lamontana.cotizacion_correccion WHERE id_cotizacion=NEW.id_cotizacion AND id_solicitud_correccion=s.id_solicitud_correccion)
  OR c.confirmada_en<>NEW.fecha OR c.confirmada_en>=c.vigente_hasta OR c.id_configuracion_confirmacion<>NEW.id_configuracion_version
  OR NEW.condiciones_finales->'cotizadas' IS DISTINCT FROM c.oferta->'condiciones'
  THEN RAISE EXCEPTION 'La oferta nueva requiere confirmación de la respuesta y condiciones finales' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER respuesta_correccion_completa AFTER INSERT ON lamontana.respuesta_correccion DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_respuesta_completa();
CREATE FUNCTION lamontana.validar_reserva_actual_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF (SELECT count(*) FROM lamontana.reserva_entrega WHERE id_pedido=NEW.id_pedido AND estado='ACTIVA')<>
  (CASE WHEN (SELECT estado FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido) IN ('RECHAZADO','CANCELADO') THEN 0 ELSE 1 END)
 THEN RAISE EXCEPTION 'El pedido conserva exactamente su reserva vigente hasta finalizar' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER reserva_pedido_actual AFTER UPDATE ON lamontana.reserva_entrega DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_reserva_actual_pedido();

CREATE FUNCTION lamontana.validar_reserva_version_trabajo() RETURNS trigger LANGUAGE plpgsql AS $$
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
CREATE CONSTRAINT TRIGGER reserva_version_trabajo AFTER INSERT ON lamontana.reserva_entrega DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_reserva_version_trabajo();
