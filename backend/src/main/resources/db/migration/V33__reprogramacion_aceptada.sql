-- Una propuesta no reserva capacidad. Sólo la aceptación del dueño cambia la reserva.
ALTER TABLE lamontana.reserva_entrega ADD COLUMN id_configuracion_reprogramacion bigint REFERENCES lamontana.configuracion_version;
ALTER TABLE lamontana.reserva_entrega DROP CONSTRAINT reserva_entrega_estado_check;
ALTER TABLE lamontana.reserva_entrega ADD CONSTRAINT reserva_entrega_estado_check CHECK(estado IN ('ACTIVA','LIBERADA','REEMPLAZADA','REPROGRAMADA','CUMPLIDA'));
UPDATE lamontana.permiso SET descripcion='Proponer y retirar reprogramaciones, preparar, trasladar, validar código y registrar entrega física en sucursales asignadas.' WHERE codigo='GESTIONAR_ENTREGAS';
CREATE TABLE lamontana.propuesta_reprogramacion (
 id_propuesta_reprogramacion bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo_publico uuid NOT NULL UNIQUE,
 id_pedido bigint NOT NULL REFERENCES lamontana.pedido,
 id_reserva_anterior bigint NOT NULL REFERENCES lamontana.reserva_entrega,
 id_configuracion_version bigint NOT NULL REFERENCES lamontana.configuracion_version,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,actor_nombre varchar(300) NOT NULL,
 id_operacion uuid NOT NULL UNIQUE,huella char(64) NOT NULL,
 version_pedido bigint NOT NULL CHECK(version_pedido>0),
 franja_nueva jsonb NOT NULL CHECK(jsonb_typeof(franja_nueva)='object'),
 motivo_interno varchar(500) NOT NULL CHECK(length(trim(motivo_interno))>0),
 mensaje_cliente varchar(500) NOT NULL CHECK(length(trim(mensaje_cliente))>0),
 fecha timestamptz NOT NULL,
 estado varchar(12) NOT NULL DEFAULT 'PENDIENTE' CHECK(estado IN ('PENDIENTE','ACEPTADA','RECHAZADA','RETIRADA'))
);
CREATE UNIQUE INDEX una_reprogramacion_pendiente ON lamontana.propuesta_reprogramacion(id_pedido) WHERE estado='PENDIENTE';
CREATE TABLE lamontana.resolucion_reprogramacion (
 id_propuesta_reprogramacion bigint PRIMARY KEY REFERENCES lamontana.propuesta_reprogramacion,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,actor_nombre varchar(300) NOT NULL,
 id_operacion uuid NOT NULL UNIQUE,huella char(64) NOT NULL,
 resultado varchar(12) NOT NULL CHECK(resultado IN ('ACEPTADA','RECHAZADA','RETIRADA')),
 motivo varchar(500) NOT NULL CHECK(length(trim(motivo))>0),fecha timestamptz NOT NULL,
 version_pedido bigint NOT NULL CHECK(version_pedido>0),
 id_reserva_nueva bigint UNIQUE REFERENCES lamontana.reserva_entrega,
 id_evento_pedido bigint UNIQUE REFERENCES lamontana.historial_estado_pedido,
 CHECK((resultado='ACEPTADA')=(id_reserva_nueva IS NOT NULL)),CHECK((resultado='ACEPTADA')=(id_evento_pedido IS NOT NULL))
);
CREATE TRIGGER resolucion_reprogramacion_inmutable BEFORE UPDATE OR DELETE ON lamontana.resolucion_reprogramacion FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE FUNCTION lamontana.proteger_propuesta_reprogramacion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'La propuesta conserva su historia' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' THEN
  IF NOT EXISTS(SELECT 1 FROM lamontana.pedido p JOIN lamontana.reserva_entrega r USING(id_pedido)
   WHERE p.id_pedido=NEW.id_pedido AND p.version=NEW.version_pedido AND p.estado IN ('PENDIENTE_REVISION','APROBADO','EN_PRODUCCION','LISTO_PARA_ENTREGA')
   AND r.id_reserva_entrega=NEW.id_reserva_anterior AND r.estado='ACTIVA')
   OR NOT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE id_configuracion_version=NEW.id_configuracion_version AND estado='ACTIVA')
   OR NEW.estado<>'PENDIENTE'
  THEN RAISE EXCEPTION 'La propuesta requiere pedido, configuración y reserva vigentes' USING ERRCODE='23514'; END IF;
 ELSE
  IF OLD.estado<>'PENDIENTE' OR NEW.estado='PENDIENTE' OR (to_jsonb(NEW)-'estado') IS DISTINCT FROM (to_jsonb(OLD)-'estado')
  THEN RAISE EXCEPTION 'La propuesta sólo admite una decisión y conserva su contenido' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER propuesta_reprogramacion_control BEFORE INSERT OR UPDATE OR DELETE ON lamontana.propuesta_reprogramacion FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_propuesta_reprogramacion();
CREATE FUNCTION lamontana.validar_resolucion_reprogramacion() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE prop lamontana.propuesta_reprogramacion; decision lamontana.resolucion_reprogramacion;
 p lamontana.pedido_actual; anterior lamontana.reserva_entrega; nueva lamontana.reserva_entrega; h lamontana.historial_estado_pedido; rol_actor text;
BEGIN
 SELECT * INTO prop FROM lamontana.propuesta_reprogramacion WHERE id_propuesta_reprogramacion=NEW.id_propuesta_reprogramacion;
 SELECT * INTO decision FROM lamontana.resolucion_reprogramacion WHERE id_propuesta_reprogramacion=prop.id_propuesta_reprogramacion;
 SELECT * INTO p FROM lamontana.pedido_actual WHERE id_pedido=prop.id_pedido;
 SELECT r.codigo INTO rol_actor FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.id_usuario=decision.id_actor;
 IF decision.id_actor IS NULL OR prop.estado<>decision.resultado OR decision.fecha<prop.fecha
 OR (decision.resultado IN ('ACEPTADA','RECHAZADA') AND (decision.id_actor<>p.id_usuario_creador OR rol_actor<>'CLIENTE'))
 OR (decision.resultado='RETIRADA' AND rol_actor NOT IN ('ADMIN_ADMIN','EMPLEADO'))
 THEN RAISE EXCEPTION 'La decisión requiere al cliente dueño o al interno que retira' USING ERRCODE='23514'; END IF;
 IF decision.resultado='ACEPTADA' THEN
  SELECT * INTO anterior FROM lamontana.reserva_entrega WHERE id_reserva_entrega=prop.id_reserva_anterior;
  SELECT * INTO nueva FROM lamontana.reserva_entrega WHERE id_reserva_entrega=decision.id_reserva_nueva;
  SELECT * INTO h FROM lamontana.historial_estado_pedido WHERE id_evento=decision.id_evento_pedido;
  IF anterior.estado<>'REPROGRAMADA' OR nueva.estado<>'ACTIVA' OR nueva.id_pedido<>p.id_pedido OR anterior.id_pedido<>p.id_pedido
  OR nueva.id_configuracion_reprogramacion IS DISTINCT FROM prop.id_configuracion_version
  OR p.estado NOT IN ('PENDIENTE_REVISION','APROBADO','EN_PRODUCCION','LISTO_PARA_ENTREGA')
  OR h.accion<>'REPROGRAMAR' OR h.id_pedido<>p.id_pedido OR h.id_actor<>p.id_usuario_creador OR h.actor_tipo<>'CLIENTE'
  OR h.estado_origen<>p.estado OR h.estado_destino<>p.estado OR h.version_pedido<>p.version OR decision.version_pedido<>p.version OR h.fecha<>decision.fecha
  OR nueva.modalidad<>anterior.modalidad OR nueva.destino_publico<>anterior.destino_publico OR nueva.origen_publico<>anterior.origen_publico
  OR nueva.nombre_destino<>anterior.nombre_destino OR nueva.costo<>anterior.costo
  OR nueva.franja_desde<>(prop.franja_nueva->>'desde')::timestamptz OR nueva.franja_hasta<>(prop.franja_nueva->>'hasta')::timestamptz
  OR nueva.zona_horaria<>prop.franja_nueva->>'zonaHoraria' OR nueva.cupo_confirmado<>(prop.franja_nueva->>'cupoConfigurado')::integer
  OR nueva.franja_hasta<=decision.fecha
  OR EXISTS(SELECT 1 FROM lamontana.codigo_entrega WHERE id_pedido=p.id_pedido AND revocado_en IS NULL AND utilizado_en IS NULL)
  OR NOT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE id_configuracion_version=prop.id_configuracion_version AND estado='ACTIVA')
  THEN RAISE EXCEPTION 'La aceptación reprograma la reserva, revoca el código y conserva trabajo, precio y logística' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER reprogramacion_resuelta AFTER UPDATE ON lamontana.propuesta_reprogramacion DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_resolucion_reprogramacion();
CREATE CONSTRAINT TRIGGER reprogramacion_aceptada AFTER INSERT ON lamontana.resolucion_reprogramacion DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_resolucion_reprogramacion();
CREATE FUNCTION lamontana.validar_reserva_reprogramada() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.estado='REPROGRAMADA' AND NOT EXISTS(SELECT 1 FROM lamontana.propuesta_reprogramacion p JOIN lamontana.resolucion_reprogramacion d USING(id_propuesta_reprogramacion)
  WHERE p.id_reserva_anterior=NEW.id_reserva_entrega AND d.resultado='ACEPTADA')
 THEN RAISE EXCEPTION 'La reserva anterior sólo se reemplaza con aceptación registrada' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER reserva_reprogramada_aceptada AFTER UPDATE ON lamontana.reserva_entrega DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_reserva_reprogramada();

CREATE OR REPLACE FUNCTION lamontana.controlar_transicion_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND OLD.estado IN ('PENDIENTE_REVISION','APROBADO','EN_PRODUCCION','LISTO_PARA_ENTREGA')
 AND NEW.version=OLD.version+1 AND (to_jsonb(NEW)-'version')=(to_jsonb(OLD)-'version')
 AND EXISTS(SELECT 1 FROM lamontana.propuesta_reprogramacion WHERE id_pedido=NEW.id_pedido AND estado='PENDIENTE')
 THEN RETURN NEW; END IF;
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
 IF h.accion='REPROGRAMAR' THEN
  IF NEW.estado<>OLD.estado OR NEW.version<>OLD.version+1 OR activas<>1 OR h.actor_tipo<>'CLIENTE' OR h.id_actor<>NEW.id_usuario_creador
  OR NOT EXISTS(SELECT 1 FROM lamontana.resolucion_reprogramacion d JOIN lamontana.propuesta_reprogramacion p USING(id_propuesta_reprogramacion)
   WHERE p.id_pedido=NEW.id_pedido AND d.id_evento_pedido=h.id_evento AND d.resultado='ACEPTADA')
  THEN RAISE EXCEPTION 'La reprogramación conserva el estado y requiere aceptación e historia' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
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
 OR NOT ((NEW.estado='REPROGRAMADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado IN ('PENDIENTE_REVISION','APROBADO','EN_PRODUCCION','LISTO_PARA_ENTREGA'))) OR (NEW.estado='CUMPLIDA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado='ENTREGADO')) OR (NEW.estado='LIBERADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado IN ('RECHAZADO','CANCELADO')))
  OR (NEW.estado='REEMPLAZADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado='CORRECCION_SOLICITADA')))
 THEN RAISE EXCEPTION 'Liberación de reserva inválida' USING ERRCODE='23514'; END IF;
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
 IF NOT EXISTS(
   SELECT 1 FROM lamontana.franja_entrega f
   LEFT JOIN lamontana.configuracion_punto_entrega cp USING(id_configuracion_punto_entrega)
   LEFT JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
   LEFT JOIN lamontana.configuracion_zona_entrega cz USING(id_configuracion_zona_entrega)
   LEFT JOIN lamontana.zona_entrega z USING(id_zona_entrega)
   WHERE f.habilitada AND f.dia_semana=extract(isodow FROM r.fecha_local)
   AND f.hora_desde=r.apertura AND f.hora_hasta=r.cierre AND f.capacidad_pedidos=r.cupo_confirmado
   AND ((r.modalidad='RETIRO_SUCURSAL' AND f.id_configuracion_retiro=coalesce(r.id_configuracion_reprogramacion,p.id_configuracion_version) AND f.id_sucursal_retiro=p.id_sucursal)
    OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND cp.habilitado AND cp.id_configuracion_version=coalesce(r.id_configuracion_reprogramacion,p.id_configuracion_version) AND cp.id_sucursal=p.id_sucursal AND pt.codigo_publico=r.destino_publico)
    OR (r.modalidad='ENVIO_DOMICILIO' AND cz.habilitada AND cz.id_configuracion_version=coalesce(r.id_configuracion_reprogramacion,p.id_configuracion_version) AND z.codigo_publico=r.destino_publico))
 ) THEN RAISE EXCEPTION 'La reserva debe corresponder a una franja configurada' USING ERRCODE='23514'; END IF;
 IF r.origen_publico<>(SELECT codigo_publico FROM lamontana.sucursal WHERE id_sucursal=p.id_sucursal)
 OR (r.modalidad='RETIRO_SUCURSAL' AND r.destino_publico<>r.origen_publico)
 OR (r.modalidad='RETIRO_PUNTO_ENTREGA' AND r.destino_publico<>(c.oferta->>'punto')::uuid)
 OR (r.modalidad='ENVIO_DOMICILIO' AND (p.receptor IS NULL OR p.telefono IS NULL))
 THEN RAISE EXCEPTION 'Destino del pedido inválido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

ALTER TABLE lamontana.historial_estado_pedido DROP CONSTRAINT gestion_pedido_evento_check;
ALTER TABLE lamontana.historial_estado_pedido ADD CONSTRAINT gestion_pedido_evento_check CHECK(
 (version_pedido IS NULL AND accion IS NULL AND estado_origen IS NULL AND mensaje_cliente IS NULL AND actor_nombre IS NULL AND actor_tipo IS NULL)
 OR (version_pedido IS NOT NULL AND version_pedido>1 AND accion IS NOT NULL AND accion IN ('REVISAR','APROBAR','RECHAZAR','CANCELAR','PEDIR_CORRECCION','RESPONDER_CORRECCION','INICIAR_TRABAJO','COMPLETAR_TRABAJO','ERROR_TRABAJO','CANCELAR_TRABAJO','CONTROLAR_CALIDAD','PREPARAR_ENVIO','SALIR_REPARTO','LLEGAR_PUNTO','ENTREGAR','CERRAR','REPROGRAMAR') AND estado_origen IS NOT NULL
  AND mensaje_cliente IS NOT NULL AND length(trim(mensaje_cliente))>0 AND actor_nombre IS NOT NULL AND actor_tipo IS NOT NULL AND actor_tipo IN ('CLIENTE','ADMIN_ADMIN','EMPLEADO')));
