-- La ubicación física pasa a ser el alcance productivo real de cada impresora.
COMMENT ON COLUMN lamontana.configuracion_impresora.id_sucursal_ubicacion IS
    'Sucursal productiva del equipo. Limita su visualización, operación y asignación a pedidos.';

ALTER TABLE lamontana.configuracion_punto_entrega
    ADD COLUMN prioridad smallint NOT NULL DEFAULT 100 CHECK(prioridad BETWEEN 1 AND 999);

WITH ordenadas AS (
    SELECT id_configuracion_punto_entrega,
           row_number() OVER(PARTITION BY id_configuracion_version,id_punto_entrega ORDER BY id_sucursal)::smallint AS prioridad
    FROM lamontana.configuracion_punto_entrega
)
UPDATE lamontana.configuracion_punto_entrega c SET prioridad=o.prioridad
FROM ordenadas o WHERE o.id_configuracion_punto_entrega=c.id_configuracion_punto_entrega;

CREATE UNIQUE INDEX configuracion_punto_prioridad_unica
    ON lamontana.configuracion_punto_entrega(id_configuracion_version,id_punto_entrega,prioridad);

-- Una cuenta responsable por sucursal y una sola sucursal por cuenta responsable.
CREATE TABLE lamontana.sucursal_responsable (
    id_sucursal bigint PRIMARY KEY REFERENCES lamontana.sucursal,
    id_usuario bigint NOT NULL UNIQUE REFERENCES lamontana.usuario,
    asignado_por bigint NOT NULL REFERENCES lamontana.usuario,
    asignado_en timestamptz NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE lamontana.evento_asignacion_produccion (
    id_evento bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_operacion uuid NOT NULL UNIQUE,
    id_pedido bigint NOT NULL REFERENCES lamontana.pedido,
    id_sucursal_anterior bigint NOT NULL REFERENCES lamontana.sucursal,
    id_sucursal_nueva bigint NOT NULL REFERENCES lamontana.sucursal,
    id_actor bigint NOT NULL REFERENCES lamontana.usuario,
    motivo varchar(500) NOT NULL CHECK(length(trim(motivo))>0),
    fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);

-- Puede derivarse antes de comenzar o después de dejar el último intento de cada ítem
-- expresamente fallido/cancelado u observado para reimpresión. Nunca durante una impresión,
-- con un trabajo pendiente de calidad ni después de aprobarlo.
CREATE FUNCTION lamontana.pedido_admite_reasignacion(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT NOT EXISTS(
  SELECT 1 FROM lamontana.pedido_archivo_actual i
  JOIN LATERAL (
   SELECT t.id_trabajo_impresion,t.estado FROM lamontana.trabajo_impresion t
   WHERE t.id_pedido=i.id_pedido AND t.id_cotizacion_item=i.id_cotizacion_item
   ORDER BY t.id_trabajo_impresion DESC LIMIT 1
  ) t ON true
  LEFT JOIN lamontana.control_calidad c ON c.id_trabajo_impresion=t.id_trabajo_impresion
  WHERE i.id_pedido=pedido AND NOT (
   t.estado IN ('ERROR','CANCELADO')
   OR t.estado='COMPLETADO' AND c.resultado IN ('REIMPRESION_REQUERIDA','INCIDENCIA')
  )
 )
$$;

-- El traslado depende del origen operativo vigente, no de la casa matriz histórica.
CREATE OR REPLACE FUNCTION lamontana.retiro_con_traslado(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT coalesce(r.modalidad='RETIRO_SUCURSAL' AND r.origen_publico IS DISTINCT FROM r.destino_publico,false)
 FROM lamontana.reserva_entrega r WHERE r.id_pedido=pedido AND r.estado='ACTIVA'
$$;

-- Excepción auditada a la inmutabilidad comercial. Sólo cambia el origen operativo antes del
-- inicio o después de un intento fallido. El destino, precio, archivos y condiciones aceptadas
-- permanecen intactos.
CREATE OR REPLACE FUNCTION lamontana.controlar_liberacion_reserva() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'La reserva se conserva como historia' USING ERRCODE='23514'; END IF;
 IF OLD.estado='ACTIVA' AND NEW.estado='ACTIVA'
 AND (to_jsonb(NEW)-'origen_publico')=(to_jsonb(OLD)-'origen_publico')
 AND EXISTS(SELECT 1 FROM lamontana.evento_asignacion_produccion e
   JOIN lamontana.sucursal anterior ON anterior.id_sucursal=e.id_sucursal_anterior
   JOIN lamontana.sucursal nueva ON nueva.id_sucursal=e.id_sucursal_nueva
   WHERE e.id_pedido=NEW.id_pedido AND anterior.codigo_publico=OLD.origen_publico AND nueva.codigo_publico=NEW.origen_publico
   AND e.id_evento=(SELECT max(ultima.id_evento) FROM lamontana.evento_asignacion_produccion ultima WHERE ultima.id_pedido=NEW.id_pedido))
 AND lamontana.pedido_admite_reasignacion(NEW.id_pedido)
 THEN RETURN NEW; END IF;
 IF (to_jsonb(NEW)-'estado') IS DISTINCT FROM (to_jsonb(OLD)-'estado') OR OLD.estado<>'ACTIVA'
 OR NOT ((NEW.estado='REPROGRAMADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado IN ('PENDIENTE_REVISION','APROBADO','EN_PRODUCCION','LISTO_PARA_ENTREGA'))) OR (NEW.estado='CUMPLIDA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado='ENTREGADO')) OR (NEW.estado='LIBERADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado IN ('RECHAZADO','CANCELADO')))
  OR (NEW.estado='REEMPLAZADA' AND EXISTS(SELECT 1 FROM lamontana.pedido WHERE id_pedido=NEW.id_pedido AND estado='CORRECCION_SOLICITADA')))
 THEN RAISE EXCEPTION 'Liberación de reserva inválida' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION lamontana.controlar_transicion_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND NEW.version=OLD.version+1
 AND (to_jsonb(NEW)-ARRAY['id_sucursal','version'])=(to_jsonb(OLD)-ARRAY['id_sucursal','version'])
 AND NEW.id_sucursal<>OLD.id_sucursal
 AND EXISTS(SELECT 1 FROM lamontana.evento_asignacion_produccion e WHERE e.id_pedido=NEW.id_pedido AND e.id_sucursal_anterior=OLD.id_sucursal AND e.id_sucursal_nueva=NEW.id_sucursal AND e.id_evento=(SELECT max(ultima.id_evento) FROM lamontana.evento_asignacion_produccion ultima WHERE ultima.id_pedido=NEW.id_pedido))
 AND lamontana.pedido_admite_reasignacion(NEW.id_pedido)
 THEN RETURN NEW; END IF;
 IF TG_OP='UPDATE' AND OLD.estado='PENDIENTE_REVISION' AND NEW.estado='APROBADO' AND OLD.modo_aprobacion='AUTOMATICA'
 AND EXISTS(SELECT 1 FROM lamontana.cobro_pedido WHERE id_pedido=NEW.id_pedido) THEN
  IF (to_jsonb(NEW)-ARRAY['estado','version','aprobada_en','id_aprobador']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['estado','version','aprobada_en','id_aprobador'])
   OR NEW.version<>OLD.version+1 OR NEW.aprobada_en IS NULL OR NEW.id_aprobador IS NULL
   OR NOT lamontana.produccion_cubierta(NEW.id_pedido) OR lamontana.cobro_bloquea_produccion(NEW.id_pedido)
  THEN RAISE EXCEPTION 'Aprobación condicional requiere cobertura y condiciones inmutables' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
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
 IF NEW.estado=OLD.estado AND NEW.version=OLD.version+1 AND NEW.id_sucursal<>OLD.id_sucursal
 AND EXISTS(SELECT 1 FROM lamontana.evento_asignacion_produccion e WHERE e.id_pedido=NEW.id_pedido AND e.id_sucursal_anterior=OLD.id_sucursal AND e.id_sucursal_nueva=NEW.id_sucursal AND e.id_evento=(SELECT max(ultima.id_evento) FROM lamontana.evento_asignacion_produccion ultima WHERE ultima.id_pedido=NEW.id_pedido))
 AND lamontana.pedido_admite_reasignacion(NEW.id_pedido)
 THEN RETURN NEW; END IF;
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

-- La base también impide iniciar un trabajo con un equipo situado en otra sucursal.
CREATE OR REPLACE FUNCTION lamontana.validar_trabajo_manual() RETURNS trigger LANGUAGE plpgsql AS $$
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
 IF NEW.id_impresora IS NOT NULL AND NOT EXISTS(SELECT 1 FROM lamontana.configuracion_impresora c
  WHERE c.id_configuracion_version=NEW.id_configuracion_version AND c.id_impresora=NEW.id_impresora
  AND c.id_sucursal_ubicacion=p.id_sucursal AND c.nombre=NEW.impresora_nombre
  AND c.estado='OPERATIVA' AND c.retirada_en IS NULL)
 THEN RAISE EXCEPTION 'La impresora debe pertenecer a la sucursal productiva del pedido' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
