-- Logística, entrega y cierre conservan hechos independientes de pagos y calidad.
INSERT INTO lamontana.permiso(codigo,nombre,descripcion,alcance) VALUES
 ('GESTIONAR_ENTREGAS','Gestionar entregas','Preparar, trasladar, validar código y registrar entrega física en sucursales asignadas.','GLOBAL'),
 ('CERRAR_PEDIDOS','Cerrar pedidos','Cerrar pedidos entregados y conciliados en sucursales asignadas.','GLOBAL');
CREATE TABLE lamontana.codigo_entrega (
 id_codigo_entrega bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_pedido bigint NOT NULL REFERENCES lamontana.pedido,
 id_reserva_entrega bigint NOT NULL REFERENCES lamontana.reserva_entrega,
 id_emisor bigint NOT NULL REFERENCES lamontana.usuario,
 id_operacion uuid NOT NULL UNIQUE,huella char(64) NOT NULL,
 codigo_hash varchar(255) NOT NULL,
 emitido_en timestamptz NOT NULL,vence_en timestamptz NOT NULL CHECK(vence_en>emitido_en),
 revocado_en timestamptz,utilizado_en timestamptz,
 intentos integer NOT NULL DEFAULT 0 CHECK(intentos BETWEEN 0 AND 5),
 CHECK(revocado_en IS NULL OR revocado_en>=emitido_en),CHECK(utilizado_en IS NULL OR utilizado_en>=emitido_en)
);
CREATE UNIQUE INDEX codigo_entrega_actual ON lamontana.codigo_entrega(id_pedido) WHERE revocado_en IS NULL AND utilizado_en IS NULL;
CREATE TABLE lamontana.validacion_codigo_entrega (
 id_validacion_codigo bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_codigo_entrega bigint NOT NULL REFERENCES lamontana.codigo_entrega,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 actor_nombre varchar(300) NOT NULL,
 id_operacion uuid NOT NULL UNIQUE,huella char(64) NOT NULL,
 version_pedido bigint NOT NULL,
 resultado varchar(16) NOT NULL CHECK(resultado IN ('VALIDO','INVALIDO','BLOQUEADO','VENCIDO')),
 fecha timestamptz NOT NULL,valido_hasta timestamptz,
 CHECK((resultado='VALIDO')=(valido_hasta IS NOT NULL)),CHECK(valido_hasta IS NULL OR valido_hasta>fecha)
);
CREATE TABLE lamontana.movimiento_entrega (
 id_movimiento_entrega bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pedido bigint NOT NULL REFERENCES lamontana.pedido,
 id_reserva_entrega bigint NOT NULL REFERENCES lamontana.reserva_entrega,
 id_evento_pedido bigint NOT NULL UNIQUE REFERENCES lamontana.historial_estado_pedido,
 estado_origen varchar(24) NOT NULL,estado_destino varchar(24) NOT NULL CHECK(estado_destino IN ('PREPARADO','EN_VIAJE','DISPONIBLE_PUNTO'))
);
CREATE TABLE lamontana.entrega (
 id_entrega bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_pedido bigint NOT NULL UNIQUE REFERENCES lamontana.pedido,
 id_reserva_entrega bigint NOT NULL UNIQUE REFERENCES lamontana.reserva_entrega,
 id_evento_pedido bigint NOT NULL UNIQUE REFERENCES lamontana.historial_estado_pedido,
 id_validacion_codigo bigint NOT NULL UNIQUE REFERENCES lamontana.validacion_codigo_entrega,
 receptor_nombre varchar(140) NOT NULL CHECK(length(trim(receptor_nombre))>0),
 nota varchar(500) NOT NULL CHECK(length(trim(nota))>0),
 fecha_entrega timestamptz NOT NULL
);
CREATE TABLE lamontana.cierre_pedido (
 id_pedido bigint PRIMARY KEY REFERENCES lamontana.pedido,
 id_evento_pedido bigint NOT NULL UNIQUE REFERENCES lamontana.historial_estado_pedido,
 total numeric(19,2) NOT NULL CHECK(total>=0),
 aplicado numeric(19,2) NOT NULL CHECK(aplicado>=total),
 fecha_cierre timestamptz NOT NULL
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['validacion_codigo_entrega','movimiento_entrega','entrega','cierre_pedido'] LOOP
  EXECUTE format('CREATE TRIGGER %I BEFORE UPDATE OR DELETE ON lamontana.%I FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion()',t||'_inmutable',t);
 END LOOP;
END $$;
CREATE FUNCTION lamontana.estado_logistico(pedido bigint) RETURNS text LANGUAGE sql STABLE AS $$
 SELECT CASE WHEN p.estado IN ('ENTREGADO','CERRADO') THEN 'ENTREGADO'
 WHEN p.estado<>'LISTO_PARA_ENTREGA' THEN 'NO_DISPONIBLE'
 WHEN c.oferta->>'modalidad'='RETIRO_SUCURSAL' THEN 'LISTO_RETIRO'
 ELSE coalesce((SELECT m.estado_destino FROM lamontana.movimiento_entrega m WHERE m.id_pedido=p.id_pedido ORDER BY m.id_movimiento_entrega DESC LIMIT 1),'PENDIENTE_PREPARACION') END
 FROM lamontana.pedido_actual p JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE p.id_pedido=pedido
$$;
CREATE FUNCTION lamontana.saldo_entrega_cubierto(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT p.total<=coalesce((SELECT sum(n.neto) FROM lamontana.aplicacion_pago_neta n WHERE n.id_cotizacion=p.id_cotizacion),0)
 AND lamontana.produccion_cubierta(p.id_pedido) FROM lamontana.pedido_actual p WHERE p.id_pedido=pedido
$$;
CREATE FUNCTION lamontana.validar_codigo_entrega() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'El código conserva su auditoría' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' THEN
  IF NOT EXISTS(SELECT 1 FROM lamontana.pedido p JOIN lamontana.reserva_entrega r USING(id_pedido)
   WHERE p.id_pedido=NEW.id_pedido AND p.estado='LISTO_PARA_ENTREGA' AND p.id_usuario_creador=NEW.id_emisor AND r.id_reserva_entrega=NEW.id_reserva_entrega AND r.estado='ACTIVA')
  THEN RAISE EXCEPTION 'El código pertenece al cliente y a la reserva vigente lista para entrega' USING ERRCODE='23514'; END IF;
 ELSE
  IF (to_jsonb(NEW)-ARRAY['revocado_en','utilizado_en','intentos']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['revocado_en','utilizado_en','intentos'])
  OR OLD.revocado_en IS NOT NULL OR OLD.utilizado_en IS NOT NULL
  OR NOT ((NEW.intentos=OLD.intentos+1 AND NEW.revocado_en IS NULL AND NEW.utilizado_en IS NULL)
   OR (NEW.intentos=OLD.intentos AND ((NEW.revocado_en IS NOT NULL AND NEW.utilizado_en IS NULL) OR (NEW.utilizado_en IS NOT NULL AND NEW.revocado_en IS NULL))))
  THEN RAISE EXCEPTION 'Transición de código inválida' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER codigo_entrega_control BEFORE INSERT OR UPDATE OR DELETE ON lamontana.codigo_entrega FOR EACH ROW EXECUTE FUNCTION lamontana.validar_codigo_entrega();
CREATE FUNCTION lamontana.validar_movimiento_entrega() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE h lamontana.historial_estado_pedido; r lamontana.reserva_entrega; previo text;
BEGIN
 SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_evento=NEW.id_evento_pedido;
 SELECT * INTO r FROM lamontana.reserva_entrega WHERE id_reserva_entrega=NEW.id_reserva_entrega;
 previo:=lamontana.estado_logistico(NEW.id_pedido);
 IF r.id_pedido<>NEW.id_pedido OR r.estado<>'ACTIVA' OR r.modalidad='RETIRO_SUCURSAL' OR h.id_pedido<>NEW.id_pedido
 OR h.estado_origen<>'LISTO_PARA_ENTREGA' OR h.estado_destino<>'LISTO_PARA_ENTREGA' OR h.actor_tipo NOT IN ('ADMIN_ADMIN','EMPLEADO')
 OR NEW.estado_origen<>previo OR NOT (
  (previo='PENDIENTE_PREPARACION' AND NEW.estado_destino='PREPARADO' AND h.accion='PREPARAR_ENVIO')
  OR (previo='PREPARADO' AND NEW.estado_destino='EN_VIAJE' AND h.accion='SALIR_REPARTO')
  OR (previo='EN_VIAJE' AND NEW.estado_destino='DISPONIBLE_PUNTO' AND r.modalidad='RETIRO_PUNTO_ENTREGA' AND h.accion='LLEGAR_PUNTO'))
 THEN RAISE EXCEPTION 'Movimiento incompatible con modalidad y recorrido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER movimiento_entrega_valido BEFORE INSERT ON lamontana.movimiento_entrega FOR EACH ROW EXECUTE FUNCTION lamontana.validar_movimiento_entrega();
CREATE FUNCTION lamontana.validar_entrega_registrada() RETURNS trigger LANGUAGE plpgsql AS $$
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
 OR (r.modalidad='RETIRO_SUCURSAL' AND ultimo IS NOT NULL)
 OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND ultimo IS DISTINCT FROM 'DISPONIBLE_PUNTO')
 OR (r.modalidad='ENVIO_DOMICILIO' AND ultimo IS DISTINCT FROM 'EN_VIAJE')
 THEN RAISE EXCEPTION 'Entrega requiere calidad, recorrido, código personal y saldo' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER entrega_registrada_completa AFTER INSERT ON lamontana.entrega DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_entrega_registrada();
CREATE FUNCTION lamontana.dinero_pendiente_cierre(pedido bigint) RETURNS numeric LANGUAGE sql STABLE AS $$
 SELECT coalesce(sum(greatest(pa.importe-coalesce((SELECT sum(r.importe) FROM lamontana.reembolso r WHERE r.id_pago=pa.id_pago),0)
 -coalesce((SELECT sum(n.neto) FROM lamontana.aplicacion_pago_neta n WHERE n.id_pago=pa.id_pago AND n.id_cotizacion=p.id_cotizacion),0),0)),0)
 FROM lamontana.pago pa JOIN lamontana.pedido_actual p ON p.id_pedido=pedido AND lamontana.cotizacion_descendiente(p.id_cotizacion,pa.id_cotizacion)
$$;
CREATE FUNCTION lamontana.informes_pendientes_cierre(pedido bigint) RETURNS bigint LANGUAGE sql STABLE AS $$
 SELECT count(*) FROM lamontana.intento_pago i JOIN lamontana.pedido_actual p ON p.id_pedido=pedido AND lamontana.cotizacion_descendiente(p.id_cotizacion,i.id_cotizacion)
 WHERE NOT EXISTS(SELECT 1 FROM lamontana.resolucion_intento_pago r WHERE r.id_intento_pago=i.id_intento_pago)
$$;
CREATE FUNCTION lamontana.validar_cierre_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p lamontana.pedido_actual; h lamontana.historial_estado_pedido;
BEGIN
 SELECT * INTO p FROM lamontana.pedido_actual WHERE id_pedido=NEW.id_pedido;
 SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_evento=NEW.id_evento_pedido;
 IF p.estado<>'CERRADO' OR NOT EXISTS(SELECT 1 FROM lamontana.entrega WHERE id_pedido=p.id_pedido AND fecha_entrega<=NEW.fecha_cierre)
 OR NEW.total<>p.total OR NEW.aplicado<>coalesce((SELECT sum(n.neto) FROM lamontana.aplicacion_pago_neta n WHERE n.id_cotizacion=p.id_cotizacion),0)
 OR NOT lamontana.saldo_entrega_cubierto(p.id_pedido) OR lamontana.dinero_pendiente_cierre(p.id_pedido)>0 OR lamontana.informes_pendientes_cierre(p.id_pedido)>0
 OR h.id_pedido<>p.id_pedido OR h.accion<>'CERRAR' OR h.estado_origen<>'ENTREGADO' OR h.estado_destino<>'CERRADO' OR h.fecha<>NEW.fecha_cierre
 OR h.actor_tipo NOT IN ('ADMIN_ADMIN','EMPLEADO')
 THEN RAISE EXCEPTION 'Cierre requiere entrega y conciliación completas' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER cierre_pedido_completo AFTER INSERT ON lamontana.cierre_pedido DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_cierre_pedido();

ALTER TABLE lamontana.pedido DROP CONSTRAINT pedido_estado_check;
ALTER TABLE lamontana.pedido ADD CONSTRAINT pedido_estado_check CHECK(estado IN ('PENDIENTE_REVISION','CORRECCION_SOLICITADA','APROBADO','EN_PRODUCCION','LISTO_PARA_ENTREGA','ENTREGADO','CERRADO','RECHAZADO','CANCELADO'));
ALTER TABLE lamontana.pedido DROP CONSTRAINT pedido_finalizacion_check;
ALTER TABLE lamontana.pedido ADD CONSTRAINT pedido_finalizacion_check CHECK((estado IN ('CANCELADO','RECHAZADO','CERRADO'))=(finalizada_en IS NOT NULL) AND (finalizada_en IS NULL OR finalizada_en>=confirmada_en));
ALTER TABLE lamontana.reserva_entrega DROP CONSTRAINT reserva_entrega_estado_check;
ALTER TABLE lamontana.reserva_entrega ADD CONSTRAINT reserva_entrega_estado_check CHECK(estado IN ('ACTIVA','LIBERADA','REEMPLAZADA','CUMPLIDA'));
ALTER TABLE lamontana.historial_estado_pedido DROP CONSTRAINT gestion_pedido_evento_check;
ALTER TABLE lamontana.historial_estado_pedido ADD CONSTRAINT gestion_pedido_evento_check CHECK(
 (version_pedido IS NULL AND accion IS NULL AND estado_origen IS NULL AND mensaje_cliente IS NULL AND actor_nombre IS NULL AND actor_tipo IS NULL)
 OR (version_pedido IS NOT NULL AND version_pedido>1 AND accion IS NOT NULL AND accion IN ('REVISAR','APROBAR','RECHAZAR','CANCELAR','PEDIR_CORRECCION','RESPONDER_CORRECCION','INICIAR_TRABAJO','COMPLETAR_TRABAJO','ERROR_TRABAJO','CANCELAR_TRABAJO','CONTROLAR_CALIDAD','PREPARAR_ENVIO','SALIR_REPARTO','LLEGAR_PUNTO','ENTREGAR','CERRAR') AND estado_origen IS NOT NULL
  AND mensaje_cliente IS NOT NULL AND length(trim(mensaje_cliente))>0 AND actor_nombre IS NOT NULL AND actor_tipo IS NOT NULL AND actor_tipo IN ('CLIENTE','ADMIN_ADMIN','EMPLEADO')));
CREATE OR REPLACE FUNCTION lamontana.controlar_transicion_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND ((OLD.estado='LISTO_PARA_ENTREGA' AND NEW.estado IN ('LISTO_PARA_ENTREGA','ENTREGADO')) OR (OLD.estado='ENTREGADO' AND NEW.estado='CERRADO')) THEN
  IF (to_jsonb(NEW)-ARRAY['estado','version','finalizada_en']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['estado','version','finalizada_en']) OR NEW.version<>OLD.version+1
  OR (NEW.estado<>'CERRADO' AND NEW.finalizada_en IS NOT NULL) OR (NEW.estado='CERRADO' AND NEW.finalizada_en IS NULL)
  THEN RAISE EXCEPTION 'Entrega y cierre conservan el pedido' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
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
 IF OLD.estado IN ('LISTO_PARA_ENTREGA','ENTREGADO') THEN
  IF h.id_evento IS NULL OR h.estado_origen<>OLD.estado OR h.estado_destino<>NEW.estado OR h.actor_tipo NOT IN ('ADMIN_ADMIN','EMPLEADO')
   OR activas<>(CASE WHEN NEW.estado='LISTO_PARA_ENTREGA' THEN 1 ELSE 0 END)
   OR (NEW.estado='LISTO_PARA_ENTREGA' AND NOT EXISTS(SELECT 1 FROM lamontana.movimiento_entrega m WHERE m.id_pedido=NEW.id_pedido AND m.id_evento_pedido=h.id_evento))
   OR (NEW.estado='ENTREGADO' AND (h.accion<>'ENTREGAR' OR NOT EXISTS(SELECT 1 FROM lamontana.entrega e WHERE e.id_pedido=NEW.id_pedido AND e.id_evento_pedido=h.id_evento)))
   OR (NEW.estado='CERRADO' AND (h.accion<>'CERRAR' OR h.fecha<>NEW.finalizada_en OR NOT EXISTS(SELECT 1 FROM lamontana.cierre_pedido c WHERE c.id_pedido=NEW.id_pedido AND c.id_evento_pedido=h.id_evento)))
  THEN RAISE EXCEPTION 'Logística, entrega, reserva y cierre necesitan su historia atómica' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
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

CREATE OR REPLACE FUNCTION lamontana.controlar_liberacion_reserva() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'La reserva se conserva como historia' USING ERRCODE='23514'; END IF;
 IF (to_jsonb(NEW)-'estado') IS DISTINCT FROM (to_jsonb(OLD)-'estado') OR OLD.estado<>'ACTIVA'
 OR NOT ((NEW.estado='CUMPLIDA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado='ENTREGADO')) OR (NEW.estado='LIBERADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado IN ('RECHAZADO','CANCELADO')))
  OR (NEW.estado='REEMPLAZADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado='CORRECCION_SOLICITADA')))
 THEN RAISE EXCEPTION 'Liberación de reserva inválida' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

-- El cierre captura la conciliación: el circuito ordinario no deshace su saldo.
-- Un cobro tardío puede registrarse y devolverse sin tocar aplicaciones cerradas.
CREATE FUNCTION lamontana.proteger_aplicacion_cerrada() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM lamontana.aplicacion_pago_cotizacion a JOIN lamontana.pedido_actual p USING(id_cotizacion)
  WHERE a.id_aplicacion=NEW.id_aplicacion AND p.estado='CERRADO')
 THEN RAISE EXCEPTION 'Una aplicación cerrada requiere un circuito de corrección administrativa' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER reembolso_cierre_guard BEFORE INSERT ON lamontana.reembolso_aplicacion FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_aplicacion_cerrada();
CREATE TRIGGER liberacion_cierre_guard BEFORE INSERT ON lamontana.liberacion_aplicacion_pago FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_aplicacion_cerrada();

CREATE OR REPLACE FUNCTION lamontana.validar_reserva_actual_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF (SELECT count(*) FROM lamontana.reserva_entrega WHERE id_pedido=NEW.id_pedido AND estado='ACTIVA')<>
  (CASE WHEN (SELECT estado FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido) IN ('RECHAZADO','CANCELADO','ENTREGADO','CERRADO') THEN 0 ELSE 1 END)
 THEN RAISE EXCEPTION 'El pedido conserva su reserva vigente hasta cancelar o completar la entrega' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
