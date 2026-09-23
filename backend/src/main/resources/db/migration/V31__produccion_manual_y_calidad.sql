-- Producción manual por ítem efectivo; nunca representa envíos ni eventos de CUPS.
INSERT INTO lamontana.permiso(codigo,nombre,descripcion,alcance) VALUES
 ('GESTIONAR_PRODUCCION','Registrar producción manual','Iniciar, completar, cancelar y registrar errores de trabajos en las sucursales asignadas.','GLOBAL'),
 ('CONTROLAR_CALIDAD','Controlar calidad','Registrar checklist e inspecciones para aprobar o repetir trabajos en las sucursales asignadas.','GLOBAL');
CREATE TABLE lamontana.trabajo_impresion (
 id_trabajo_impresion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_pedido bigint NOT NULL REFERENCES lamontana.pedido,
 id_cotizacion_item bigint NOT NULL REFERENCES lamontana.cotizacion_item,
 id_archivo_almacenado bigint NOT NULL REFERENCES lamontana.archivo_almacenado,
 id_aceptacion_vista_previa bigint NOT NULL REFERENCES lamontana.aceptacion_vista_previa,
 id_trabajo_origen bigint UNIQUE REFERENCES lamontana.trabajo_impresion,
 id_impresora bigint REFERENCES lamontana.impresora,
 id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
 impresora_nombre varchar(180),
 id_operador bigint NOT NULL REFERENCES lamontana.usuario,
 operador_nombre varchar(300) NOT NULL,
 modo varchar(16) NOT NULL DEFAULT 'MANUAL' CHECK(modo='MANUAL'),
 tipo varchar(24) NOT NULL CHECK(tipo IN ('NORMAL','REIMPRESION_CALIDAD')),
 estado varchar(16) NOT NULL CHECK(estado IN ('IMPRIMIENDO','COMPLETADO','ERROR','CANCELADO')),
 motivo_excepcion varchar(500),
 fecha_inicio timestamptz NOT NULL,
 fecha_fin timestamptz,
 CHECK((estado='IMPRIMIENDO')=(fecha_fin IS NULL)),
 CHECK(fecha_fin IS NULL OR fecha_fin>=fecha_inicio),
 CHECK((id_impresora IS NULL)=(impresora_nombre IS NULL)),
 CHECK(id_impresora IS NOT NULL OR length(trim(motivo_excepcion))>0 AND motivo_excepcion IS NOT NULL)
);
CREATE UNIQUE INDEX trabajo_activo_item ON lamontana.trabajo_impresion(id_pedido,id_cotizacion_item) WHERE estado='IMPRIMIENDO';
CREATE INDEX trabajo_pedido ON lamontana.trabajo_impresion(id_pedido,id_trabajo_impresion);
CREATE TABLE lamontana.historial_trabajo_impresion (
 id_historial_trabajo bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_trabajo_impresion bigint NOT NULL REFERENCES lamontana.trabajo_impresion,
 id_evento_pedido bigint NOT NULL UNIQUE REFERENCES lamontana.historial_estado_pedido,
 accion varchar(32) NOT NULL CHECK(accion IN ('INICIAR_TRABAJO','COMPLETAR_TRABAJO','ERROR_TRABAJO','CANCELAR_TRABAJO','CONTROLAR_CALIDAD')),
 estado_origen varchar(16),estado_destino varchar(16) NOT NULL,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 actor_nombre varchar(300) NOT NULL,
 motivo varchar(500) NOT NULL CHECK(length(trim(motivo))>0),
 fecha timestamptz NOT NULL
);
CREATE TABLE lamontana.control_calidad (
 id_control_calidad bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_trabajo_impresion bigint NOT NULL UNIQUE REFERENCES lamontana.trabajo_impresion,
 id_inspector bigint NOT NULL REFERENCES lamontana.usuario,
 inspector_nombre varchar(300) NOT NULL,
 resultado varchar(24) NOT NULL CHECK(resultado IN ('APROBADO','REIMPRESION_REQUERIDA','INCIDENCIA')),
 impresion_completa boolean NOT NULL,calidad_correcta boolean NOT NULL,alineacion_correcta boolean NOT NULL,
 orden_correcto boolean NOT NULL,terminaciones_correctas boolean NOT NULL,
 observaciones varchar(500) NOT NULL CHECK(length(trim(observaciones))>0),
 fecha timestamptz NOT NULL,
 CHECK(resultado<>'APROBADO' OR impresion_completa AND calidad_correcta AND alineacion_correcta AND orden_correcto AND terminaciones_correctas)
);
CREATE TRIGGER trabajo_historial_inmutable BEFORE UPDATE OR DELETE ON lamontana.historial_trabajo_impresion FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER control_calidad_inmutable BEFORE UPDATE OR DELETE ON lamontana.control_calidad FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE FUNCTION lamontana.produccion_cubierta(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT NOT EXISTS(
  SELECT 1 FROM lamontana.pedido_actual p CROSS JOIN LATERAL (VALUES(p.condiciones_finales->'cotizadas'),(p.condiciones_finales->'actuales')) c(terminos)
  WHERE p.id_pedido=pedido AND coalesce((SELECT sum(a.neto) FROM lamontana.aplicacion_pago_neta a JOIN lamontana.pago pago USING(id_pago)
   WHERE a.id_cotizacion=p.id_cotizacion AND c.terminos->'mediosAcreditacion' ? pago.medio),0)
   < (c.terminos->>'pagoPrevioRequerido')::numeric+(c.terminos->>'senaRequerida')::numeric
 ) AND EXISTS(SELECT 1 FROM lamontana.pedido_actual WHERE id_pedido=pedido)
$$;
CREATE FUNCTION lamontana.calidad_pedido_aprobada(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM lamontana.pedido_archivo_actual WHERE id_pedido=pedido) AND NOT EXISTS(
  SELECT 1 FROM lamontana.pedido_archivo_actual i
  LEFT JOIN LATERAL (SELECT t.* FROM lamontana.trabajo_impresion t WHERE t.id_pedido=i.id_pedido AND t.id_cotizacion_item=i.id_cotizacion_item ORDER BY t.id_trabajo_impresion DESC LIMIT 1) t ON true
  LEFT JOIN lamontana.control_calidad c USING(id_trabajo_impresion)
  WHERE i.id_pedido=pedido AND (t.estado IS DISTINCT FROM 'COMPLETADO' OR c.resultado IS DISTINCT FROM 'APROBADO')
 )
$$;
CREATE FUNCTION lamontana.validar_trabajo_manual() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p lamontana.pedido_actual; anterior lamontana.trabajo_impresion; resultado text;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'No se elimina la historia de producción' USING ERRCODE='23514'; END IF;
 SELECT * INTO p FROM lamontana.pedido_actual WHERE id_pedido=NEW.id_pedido;
 IF TG_OP='UPDATE' THEN
  IF (to_jsonb(NEW)-ARRAY['estado','fecha_fin']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['estado','fecha_fin'])
  OR OLD.estado<>'IMPRIMIENDO' OR NEW.estado NOT IN ('COMPLETADO','ERROR','CANCELADO') OR p.estado<>'EN_PRODUCCION'
  THEN RAISE EXCEPTION 'Transición de trabajo manual inválida' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
 IF p.estado NOT IN ('APROBADO','EN_PRODUCCION') OR p.aprobada_en IS NULL OR NEW.estado<>'IMPRIMIENDO'
 OR NEW.id_configuracion_version<>p.id_configuracion_version OR NEW.fecha_inicio<p.aprobada_en
 OR NOT EXISTS(SELECT 1 FROM lamontana.sucursal WHERE id_sucursal=p.id_sucursal AND estado='ACTIVA')
 OR NOT lamontana.produccion_cubierta(p.id_pedido)
 OR NOT EXISTS(SELECT 1 FROM lamontana.pedido_archivo_actual i WHERE i.id_pedido=p.id_pedido AND i.id_cotizacion_item=NEW.id_cotizacion_item
  AND i.id_archivo_almacenado=NEW.id_archivo_almacenado AND i.id_aceptacion_vista_previa=NEW.id_aceptacion_vista_previa)
 THEN RAISE EXCEPTION 'Producción exige aprobación, cobertura y PDF confirmado vigente' USING ERRCODE='23514'; END IF;
 SELECT * INTO anterior FROM lamontana.trabajo_impresion WHERE id_pedido=p.id_pedido AND id_cotizacion_item=NEW.id_cotizacion_item ORDER BY id_trabajo_impresion DESC LIMIT 1;
 SELECT c.resultado INTO resultado FROM lamontana.control_calidad c WHERE id_trabajo_impresion=anterior.id_trabajo_impresion;
 IF NEW.id_trabajo_origen IS DISTINCT FROM anterior.id_trabajo_impresion
 OR (anterior.id_trabajo_impresion IS NOT NULL AND NOT (anterior.estado IN ('ERROR','CANCELADO') OR anterior.estado='COMPLETADO' AND coalesce(resultado IN ('REIMPRESION_REQUERIDA','INCIDENCIA'),false)))
 OR NEW.tipo<>(CASE WHEN resultado IN ('REIMPRESION_REQUERIDA','INCIDENCIA') THEN 'REIMPRESION_CALIDAD' ELSE 'NORMAL' END)
 THEN RAISE EXCEPTION 'El nuevo trabajo debe continuar el último intento pendiente' USING ERRCODE='23514'; END IF;
 IF NEW.id_impresora IS NOT NULL AND NOT EXISTS(SELECT 1 FROM lamontana.configuracion_impresora c JOIN lamontana.impresora i USING(id_impresora)
  WHERE c.id_configuracion_version=NEW.id_configuracion_version AND c.id_impresora=NEW.id_impresora AND i.id_sucursal=p.id_sucursal
  AND c.nombre=NEW.impresora_nombre AND c.estado='OPERATIVA' AND c.retirada_en IS NULL)
 THEN RAISE EXCEPTION 'La impresora debe estar declarada en la versión y sucursal del pedido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trabajo_manual_valido BEFORE INSERT OR UPDATE OR DELETE ON lamontana.trabajo_impresion FOR EACH ROW EXECUTE FUNCTION lamontana.validar_trabajo_manual();
CREATE FUNCTION lamontana.validar_calidad_manual() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM lamontana.trabajo_impresion t JOIN lamontana.pedido p USING(id_pedido)
  WHERE t.id_trabajo_impresion=NEW.id_trabajo_impresion AND t.estado='COMPLETADO' AND p.estado='EN_PRODUCCION' AND NEW.fecha>=t.fecha_fin
  AND NOT EXISTS(SELECT 1 FROM lamontana.trabajo_impresion otro WHERE otro.id_trabajo_origen=t.id_trabajo_impresion))
 THEN RAISE EXCEPTION 'Calidad requiere un trabajo completo sin inspección ni sucesor' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER calidad_manual_valida BEFORE INSERT ON lamontana.control_calidad FOR EACH ROW EXECUTE FUNCTION lamontana.validar_calidad_manual();

ALTER TABLE lamontana.pedido DROP CONSTRAINT pedido_estado_check;
ALTER TABLE lamontana.pedido ADD CONSTRAINT pedido_estado_check CHECK(estado IN ('PENDIENTE_REVISION','CORRECCION_SOLICITADA','APROBADO','EN_PRODUCCION','LISTO_PARA_ENTREGA','RECHAZADO','CANCELADO'));
ALTER TABLE lamontana.historial_estado_pedido DROP CONSTRAINT gestion_pedido_evento_check;
ALTER TABLE lamontana.historial_estado_pedido ADD CONSTRAINT gestion_pedido_evento_check CHECK(
 (version_pedido IS NULL AND accion IS NULL AND estado_origen IS NULL AND mensaje_cliente IS NULL AND actor_nombre IS NULL AND actor_tipo IS NULL)
 OR (version_pedido IS NOT NULL AND version_pedido>1 AND accion IS NOT NULL AND accion IN ('REVISAR','APROBAR','RECHAZAR','CANCELAR','PEDIR_CORRECCION','RESPONDER_CORRECCION','INICIAR_TRABAJO','COMPLETAR_TRABAJO','ERROR_TRABAJO','CANCELAR_TRABAJO','CONTROLAR_CALIDAD') AND estado_origen IS NOT NULL
  AND mensaje_cliente IS NOT NULL AND length(trim(mensaje_cliente))>0 AND actor_nombre IS NOT NULL AND actor_tipo IS NOT NULL AND actor_tipo IN ('CLIENTE','ADMIN_ADMIN','EMPLEADO')));
CREATE OR REPLACE FUNCTION lamontana.controlar_transicion_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND (OLD.estado IN ('APROBADO','EN_PRODUCCION')) AND NEW.estado IN ('EN_PRODUCCION','LISTO_PARA_ENTREGA') THEN
  IF (to_jsonb(NEW)-ARRAY['estado','version']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['estado','version']) OR NEW.version<>OLD.version+1
   OR OLD.aprobada_en IS NULL OR (NEW.estado='LISTO_PARA_ENTREGA' AND (OLD.estado<>'EN_PRODUCCION' OR NOT lamontana.calidad_pedido_aprobada(NEW.id_pedido)))
  THEN RAISE EXCEPTION 'Producción conserva el pedido aprobado y requiere calidad completa' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
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
CREATE OR REPLACE FUNCTION lamontana.validar_transicion_pedido_completa() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE h lamontana.historial_estado_pedido; activas bigint; accion_esperada text;
BEGIN
 SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_pedido=NEW.id_pedido AND version_pedido=NEW.version;
 SELECT count(*) INTO activas FROM lamontana.reserva_entrega WHERE id_pedido=NEW.id_pedido AND estado='ACTIVA';
 IF NEW.estado IN ('EN_PRODUCCION','LISTO_PARA_ENTREGA') THEN
  IF h.id_evento IS NULL OR h.estado_origen<>OLD.estado OR h.estado_destino<>NEW.estado OR h.actor_tipo NOT IN ('ADMIN_ADMIN','EMPLEADO')
   OR activas<>1 OR (OLD.estado='APROBADO' AND h.accion<>'INICIAR_TRABAJO')
   OR (NEW.estado='LISTO_PARA_ENTREGA' AND (h.accion<>'CONTROLAR_CALIDAD' OR NOT lamontana.calidad_pedido_aprobada(NEW.id_pedido)))
   OR NOT EXISTS(SELECT 1 FROM lamontana.historial_trabajo_impresion t JOIN lamontana.trabajo_impresion w USING(id_trabajo_impresion)
    WHERE t.id_evento_pedido=h.id_evento AND w.id_pedido=NEW.id_pedido AND t.accion=h.accion AND t.id_actor=h.id_actor AND t.fecha=h.fecha)
  THEN RAISE EXCEPTION 'Producción, calidad e historia del pedido deben ser atómicas' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
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

CREATE FUNCTION lamontana.validar_evento_trabajo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE h lamontana.historial_estado_pedido; t lamontana.trabajo_impresion; c lamontana.control_calidad;
BEGIN
 SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_evento=NEW.id_evento_pedido;
 SELECT * INTO t FROM lamontana.trabajo_impresion WHERE id_trabajo_impresion=NEW.id_trabajo_impresion;
 SELECT * INTO c FROM lamontana.control_calidad WHERE id_trabajo_impresion=t.id_trabajo_impresion;
 IF h.id_pedido<>t.id_pedido OR h.accion<>NEW.accion OR h.id_actor<>NEW.id_actor OR h.actor_nombre<>NEW.actor_nombre OR h.motivo<>NEW.motivo OR h.fecha<>NEW.fecha
 OR h.actor_tipo NOT IN ('ADMIN_ADMIN','EMPLEADO') OR NEW.estado_destino<>t.estado
 OR (NEW.accion='INICIAR_TRABAJO' AND (NEW.estado_origen IS NOT NULL OR t.estado<>'IMPRIMIENDO' OR NEW.fecha<>t.fecha_inicio OR NEW.id_actor<>t.id_operador))
 OR (NEW.accion IN ('COMPLETAR_TRABAJO','CANCELAR_TRABAJO','ERROR_TRABAJO') AND (NEW.estado_origen IS DISTINCT FROM 'IMPRIMIENDO' OR NEW.fecha<>t.fecha_fin
  OR t.estado<>(CASE NEW.accion WHEN 'COMPLETAR_TRABAJO' THEN 'COMPLETADO' WHEN 'ERROR_TRABAJO' THEN 'ERROR' ELSE 'CANCELADO' END)))
 OR (NEW.accion='CONTROLAR_CALIDAD' AND (NEW.estado_origen IS DISTINCT FROM 'COMPLETADO' OR c.id_control_calidad IS NULL OR c.fecha<>NEW.fecha OR c.id_inspector<>NEW.id_actor OR c.observaciones<>NEW.motivo))
 THEN RAISE EXCEPTION 'Evento de trabajo sin su hecho físico manual' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER evento_trabajo_completo AFTER INSERT ON lamontana.historial_trabajo_impresion DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_evento_trabajo();
CREATE FUNCTION lamontana.exigir_historia_trabajo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_trabajo bigint; v_accion text; v_fecha timestamptz;
BEGIN
 IF TG_TABLE_NAME='control_calidad' THEN v_trabajo:=NEW.id_trabajo_impresion;v_accion:='CONTROLAR_CALIDAD';v_fecha:=NEW.fecha;
 ELSE v_trabajo:=NEW.id_trabajo_impresion;v_accion:=CASE WHEN TG_OP='INSERT' THEN 'INICIAR_TRABAJO' WHEN NEW.estado='COMPLETADO' THEN 'COMPLETAR_TRABAJO' WHEN NEW.estado='ERROR' THEN 'ERROR_TRABAJO' ELSE 'CANCELAR_TRABAJO' END;v_fecha:=CASE WHEN TG_OP='INSERT' THEN NEW.fecha_inicio ELSE NEW.fecha_fin END; END IF;
 IF NOT EXISTS(SELECT 1 FROM lamontana.historial_trabajo_impresion h WHERE h.id_trabajo_impresion=v_trabajo AND h.accion=v_accion AND h.fecha=v_fecha)
 THEN RAISE EXCEPTION 'Cada hecho de producción necesita historia' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER trabajo_con_historia AFTER INSERT OR UPDATE ON lamontana.trabajo_impresion DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_historia_trabajo();
CREATE CONSTRAINT TRIGGER calidad_con_historia AFTER INSERT ON lamontana.control_calidad DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_historia_trabajo();
