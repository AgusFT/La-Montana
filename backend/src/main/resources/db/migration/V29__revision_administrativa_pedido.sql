INSERT INTO lamontana.permiso(codigo,nombre,descripcion,alcance) VALUES
 ('GESTIONAR_PEDIDOS','Revisar y decidir pedidos','Registrar revisiones, observaciones, aprobaciones, rechazos y cancelaciones en las sucursales asignadas.','GLOBAL');

ALTER TABLE lamontana.pedido DROP CONSTRAINT pedido_estado_check;
DO $$ DECLARE c record; BEGIN
 FOR c IN SELECT conname FROM pg_constraint WHERE conrelid='lamontana.pedido'::regclass AND contype='c'
 AND pg_get_constraintdef(oid) LIKE '%aprobada_en%' LOOP
  EXECUTE format('ALTER TABLE lamontana.pedido DROP CONSTRAINT %I',c.conname);
 END LOOP;
END $$;
ALTER TABLE lamontana.pedido
 ADD COLUMN revisada_en timestamptz,
 ADD COLUMN id_revisor bigint REFERENCES lamontana.usuario,
 ADD COLUMN id_aprobador bigint REFERENCES lamontana.usuario,
 ADD COLUMN finalizada_en timestamptz,
 ADD CONSTRAINT pedido_estado_check CHECK(estado IN ('PENDIENTE_REVISION','APROBADO','RECHAZADO','CANCELADO')),
 ADD CONSTRAINT pedido_revision_check CHECK((revisada_en IS NULL)=(id_revisor IS NULL) AND (revisada_en IS NULL OR revisada_en>=confirmada_en)),
 ADD CONSTRAINT pedido_aprobacion_check CHECK(
  (estado<>'APROBADO' OR aprobada_en IS NOT NULL)
  AND (estado NOT IN ('PENDIENTE_REVISION','RECHAZADO') OR aprobada_en IS NULL)
  AND (aprobada_en IS NULL OR aprobada_en>=confirmada_en)
  AND ((modo_aprobacion='AUTOMATICA' AND aprobada_en=confirmada_en AND id_aprobador IS NULL)
    OR (modo_aprobacion='HUMANA' AND ((aprobada_en IS NULL AND id_aprobador IS NULL)
      OR (aprobada_en IS NOT NULL AND id_aprobador IS NOT NULL AND revisada_en IS NOT NULL AND aprobada_en>=revisada_en))))),
 ADD CONSTRAINT pedido_finalizacion_check CHECK((estado IN ('CANCELADO','RECHAZADO'))=(finalizada_en IS NOT NULL) AND (finalizada_en IS NULL OR finalizada_en>=confirmada_en));
ALTER TABLE lamontana.reserva_entrega DROP CONSTRAINT reserva_entrega_estado_check;
ALTER TABLE lamontana.reserva_entrega ADD CONSTRAINT reserva_entrega_estado_check CHECK(estado IN ('ACTIVA','LIBERADA'));
ALTER TABLE lamontana.historial_estado_pedido
 ADD COLUMN estado_origen varchar(32),
 ADD COLUMN version_pedido bigint,
 ADD COLUMN accion varchar(32),
 ADD COLUMN mensaje_cliente varchar(500),
 ADD COLUMN actor_nombre varchar(300),
 ADD COLUMN actor_tipo varchar(16),
 ADD CONSTRAINT gestion_pedido_evento_check CHECK(
  (version_pedido IS NULL AND accion IS NULL AND estado_origen IS NULL AND mensaje_cliente IS NULL AND actor_nombre IS NULL AND actor_tipo IS NULL)
  OR (version_pedido IS NOT NULL AND version_pedido>1 AND accion IS NOT NULL AND accion IN ('REVISAR','APROBAR','RECHAZAR','CANCELAR') AND estado_origen IS NOT NULL
   AND mensaje_cliente IS NOT NULL AND length(trim(mensaje_cliente))>0 AND actor_nombre IS NOT NULL AND actor_tipo IS NOT NULL AND actor_tipo IN ('CLIENTE','ADMIN_ADMIN','EMPLEADO')));
CREATE UNIQUE INDEX pedido_evento_version ON lamontana.historial_estado_pedido(id_pedido,version_pedido) WHERE version_pedido IS NOT NULL;
CREATE TABLE lamontana.observacion_interna (
 id_observacion_interna bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_pedido bigint NOT NULL REFERENCES lamontana.pedido,
 id_autor bigint NOT NULL REFERENCES lamontana.usuario,
 autor_nombre varchar(300) NOT NULL,
 id_operacion uuid NOT NULL UNIQUE,
 huella char(64) NOT NULL,
 importancia varchar(16) NOT NULL CHECK(importancia IN ('INFORMATIVA','RELEVANTE','CRITICA')),
 texto varchar(2000) NOT NULL CHECK(length(trim(texto))>0),
 fecha timestamptz NOT NULL
);
CREATE INDEX observacion_pedido ON lamontana.observacion_interna(id_pedido,id_observacion_interna);
CREATE TRIGGER observacion_inmutable BEFORE UPDATE OR DELETE ON lamontana.observacion_interna FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();

CREATE FUNCTION lamontana.controlar_transicion_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'No se elimina la historia del pedido' USING ERRCODE='23514'; END IF;
 IF (to_jsonb(NEW)-ARRAY['estado','version','revisada_en','id_revisor','aprobada_en','id_aprobador','finalizada_en']) IS DISTINCT FROM
    (to_jsonb(OLD)-ARRAY['estado','version','revisada_en','id_revisor','aprobada_en','id_aprobador','finalizada_en'])
 OR NEW.version<>OLD.version+1 OR OLD.estado NOT IN ('PENDIENTE_REVISION','APROBADO')
 THEN RAISE EXCEPTION 'La transición no puede reescribir el pedido confirmado' USING ERRCODE='23514'; END IF;
 IF NEW.estado=OLD.estado THEN
  IF OLD.estado<>'PENDIENTE_REVISION' OR OLD.revisada_en IS NOT NULL OR NEW.revisada_en IS NULL
  OR NEW.id_revisor IS NULL OR NEW.aprobada_en IS DISTINCT FROM OLD.aprobada_en OR NEW.id_aprobador IS DISTINCT FROM OLD.id_aprobador
  OR NEW.finalizada_en IS NOT NULL THEN RAISE EXCEPTION 'Revisión inválida' USING ERRCODE='23514'; END IF;
 ELSE
  IF NEW.revisada_en IS DISTINCT FROM OLD.revisada_en OR NEW.id_revisor IS DISTINCT FROM OLD.id_revisor
  THEN RAISE EXCEPTION 'La decisión conserva la revisión realizada' USING ERRCODE='23514'; END IF;
  IF NEW.estado='APROBADO' THEN
   IF OLD.estado<>'PENDIENTE_REVISION' OR OLD.revisada_en IS NULL OR NEW.aprobada_en IS NULL OR NEW.id_aprobador IS NULL OR NEW.finalizada_en IS NOT NULL
   THEN RAISE EXCEPTION 'Aprobación sin revisión' USING ERRCODE='23514'; END IF;
  ELSIF NEW.estado IN ('RECHAZADO','CANCELADO') THEN
   IF (NEW.estado='RECHAZADO' AND OLD.estado<>'PENDIENTE_REVISION') OR NEW.finalizada_en IS NULL
   OR NEW.aprobada_en IS DISTINCT FROM OLD.aprobada_en OR NEW.id_aprobador IS DISTINCT FROM OLD.id_aprobador
   THEN RAISE EXCEPTION 'Finalización inválida' USING ERRCODE='23514'; END IF;
  ELSE RAISE EXCEPTION 'Transición no admitida' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER pedido_inmutable ON lamontana.pedido;
CREATE TRIGGER pedido_transicion BEFORE UPDATE OR DELETE ON lamontana.pedido FOR EACH ROW EXECUTE FUNCTION lamontana.controlar_transicion_pedido();

CREATE FUNCTION lamontana.controlar_liberacion_reserva() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'La reserva se conserva como historia' USING ERRCODE='23514'; END IF;
 IF (to_jsonb(NEW)-'estado') IS DISTINCT FROM (to_jsonb(OLD)-'estado') OR OLD.estado<>'ACTIVA' OR NEW.estado<>'LIBERADA'
 OR NOT EXISTS(SELECT 1 FROM lamontana.pedido p WHERE p.id_pedido=NEW.id_pedido AND p.estado IN ('RECHAZADO','CANCELADO'))
 THEN RAISE EXCEPTION 'Liberación de reserva inválida' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
DROP TRIGGER reserva_inmutable ON lamontana.reserva_entrega;
CREATE TRIGGER reserva_transicion BEFORE UPDATE OR DELETE ON lamontana.reserva_entrega FOR EACH ROW EXECUTE FUNCTION lamontana.controlar_liberacion_reserva();

CREATE FUNCTION lamontana.validar_transicion_pedido_completa() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE h lamontana.historial_estado_pedido; r text; accion_esperada text;
BEGIN
 SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_pedido=NEW.id_pedido AND version_pedido=NEW.version;
 SELECT estado INTO r FROM lamontana.reserva_entrega WHERE id_pedido=NEW.id_pedido;
 accion_esperada:=CASE WHEN NEW.estado=OLD.estado THEN 'REVISAR' WHEN NEW.estado='APROBADO' THEN 'APROBAR' WHEN NEW.estado='RECHAZADO' THEN 'RECHAZAR' ELSE 'CANCELAR' END;
 IF h.id_evento IS NULL OR h.estado_origen<>OLD.estado OR h.estado_destino<>NEW.estado OR h.accion<>accion_esperada
 OR (NEW.estado IN ('CANCELADO','RECHAZADO') AND r IS DISTINCT FROM 'LIBERADA')
 OR (NEW.estado NOT IN ('CANCELADO','RECHAZADO') AND r IS DISTINCT FROM 'ACTIVA')
 OR (h.accion='REVISAR' AND (h.id_actor<>NEW.id_revisor OR h.fecha<>NEW.revisada_en))
 OR (h.accion='APROBAR' AND (h.id_actor<>NEW.id_aprobador OR h.fecha<>NEW.aprobada_en))
 OR (h.accion IN ('RECHAZAR','CANCELAR') AND h.fecha<>NEW.finalizada_en)
 OR (h.actor_tipo='CLIENTE' AND (h.accion<>'CANCELAR' OR h.id_actor<>NEW.id_usuario_creador))
 THEN RAISE EXCEPTION 'Estado, reserva y auditoría deben actualizarse juntos' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER pedido_transicion_completa AFTER UPDATE ON lamontana.pedido DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_transicion_pedido_completa();
