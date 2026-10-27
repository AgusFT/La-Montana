-- Nuevas políticas versionadas. NULL identifica configuraciones legadas, sin inventar un plazo comercial.
ALTER TABLE lamontana.configuracion_financiera ADD COLUMN politica_pago_archivos jsonb;
ALTER TABLE lamontana.configuracion_financiera ADD CONSTRAINT politica_pago_archivos_certificada CHECK(
 politica_pago_archivos IS NULL OR (
  jsonb_typeof(politica_pago_archivos)='object'
  AND politica_pago_archivos ?& ARRAY['plazoMinutos','retencionHoras']
  AND jsonb_typeof(politica_pago_archivos->'plazoMinutos')='number'
  AND (politica_pago_archivos->>'plazoMinutos')::numeric BETWEEN 1 AND 43200
  AND (politica_pago_archivos->>'plazoMinutos')::numeric=trunc((politica_pago_archivos->>'plazoMinutos')::numeric)
  AND jsonb_typeof(politica_pago_archivos->'retencionHoras')='number'
  AND (politica_pago_archivos->>'retencionHoras') IN ('24','48','72')
  AND (politica_pago_archivos->>'recordatorioMinutos' IS NULL OR
   (jsonb_typeof(politica_pago_archivos->'recordatorioMinutos')='number'
    AND (politica_pago_archivos->>'recordatorioMinutos')::numeric>0
    AND (politica_pago_archivos->>'recordatorioMinutos')::numeric<(politica_pago_archivos->>'plazoMinutos')::numeric
    AND (politica_pago_archivos->>'recordatorioMinutos')::numeric=trunc((politica_pago_archivos->>'recordatorioMinutos')::numeric)))
 ));

-- Sólo pedidos creados con el nuevo flujo. Las condiciones e historias anteriores no se reescriben.
CREATE TABLE lamontana.cobro_pedido (
 id_pedido bigint PRIMARY KEY REFERENCES lamontana.pedido,
 politica_original jsonb,
 fecha_limite timestamptz,
 recordatorio_minutos integer,
 retencion_horas integer NOT NULL CHECK(retencion_horas IN (24,48,72)),
 retener_hasta timestamptz,
 cobertura_confirmada_en timestamptz,
 intervencion_pendiente boolean NOT NULL DEFAULT false,
 excepcion_aceptada boolean NOT NULL DEFAULT false,
 recordatorio_emitido_en timestamptz,
 definitivo_en timestamptz,
 version bigint NOT NULL DEFAULT 1 CHECK(version>0),
 CHECK((fecha_limite IS NULL)=(retener_hasta IS NULL)),
 CHECK(retener_hasta=fecha_limite+retencion_horas*interval '1 hour'),
 CHECK(recordatorio_minutos IS NULL OR recordatorio_minutos>0)
);
CREATE TABLE lamontana.evento_cobro_pedido (
 id_evento bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_pedido bigint NOT NULL REFERENCES lamontana.cobro_pedido,
 id_operacion uuid NOT NULL UNIQUE,
 id_actor bigint REFERENCES lamontana.usuario,
 tipo varchar(48) NOT NULL,
 motivo varchar(2000) NOT NULL CHECK(length(trim(motivo))>0),
 resultado jsonb NOT NULL,
 fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TRIGGER evento_cobro_inmutable BEFORE UPDATE OR DELETE ON lamontana.evento_cobro_pedido
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TABLE lamontana.eliminacion_archivo (
 id_archivo_almacenado bigint PRIMARY KEY REFERENCES lamontana.archivo_almacenado,
 id_pedido bigint NOT NULL REFERENCES lamontana.cobro_pedido,
 revocado_en timestamptz NOT NULL DEFAULT clock_timestamp(),
 eliminado_en timestamptz,
 intentos integer NOT NULL DEFAULT 0,
 ultimo_error varchar(200),
 CHECK(eliminado_en IS NULL OR eliminado_en>=revocado_en)
);
CREATE TABLE lamontana.aviso_cobro_pedido (
 id_pedido bigint NOT NULL REFERENCES lamontana.cobro_pedido,
 fecha_limite timestamptz NOT NULL,
 creado_en timestamptz NOT NULL DEFAULT clock_timestamp(),
 correo_enviado_en timestamptz,
 PRIMARY KEY(id_pedido,fecha_limite)
);

CREATE FUNCTION lamontana.cobro_informado_pendiente(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM lamontana.pedido_actual p JOIN lamontana.intento_pago i
 ON lamontana.cotizacion_descendiente(p.id_cotizacion,i.id_cotizacion)
 LEFT JOIN lamontana.resolucion_intento_pago r USING(id_intento_pago)
 WHERE p.id_pedido=pedido AND r.id_intento_pago IS NULL)
$$;
CREATE FUNCTION lamontana.cobro_comprobante_validando(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM lamontana.pedido_actual p
 JOIN lamontana.cotizacion c ON lamontana.cotizacion_descendiente(p.id_cotizacion,c.id_cotizacion)
 JOIN lamontana.intento_pago i ON i.id_cotizacion=c.id_cotizacion
 JOIN lamontana.comprobante_pago cp USING(id_intento_pago)
 JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado)
 WHERE p.id_pedido=pedido AND a.estado='VALIDANDO')
$$;
CREATE FUNCTION lamontana.cobro_dinero_por_resolver(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM lamontana.pedido_actual p JOIN lamontana.pago pa
 ON lamontana.cotizacion_descendiente(p.id_cotizacion,pa.id_cotizacion)
 WHERE p.id_pedido=pedido AND pa.importe>coalesce((SELECT sum(r.importe) FROM lamontana.reembolso r WHERE r.id_pago=pa.id_pago),0))
$$;
CREATE FUNCTION lamontana.cobro_bloquea_produccion(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM lamontana.cobro_pedido cp WHERE cp.id_pedido=pedido AND (
  cp.definitivo_en IS NOT NULL OR cp.intervencion_pendiente
  OR (cp.fecha_limite IS NOT NULL AND clock_timestamp()>=cp.fecha_limite AND NOT cp.excepcion_aceptada
   AND (cp.cobertura_confirmada_en IS NULL OR cp.cobertura_confirmada_en>=cp.fecha_limite))
 )) OR EXISTS(SELECT 1 FROM lamontana.eliminacion_archivo WHERE id_pedido=pedido)
$$;
CREATE FUNCTION lamontana.cobro_puede_purgar(pedido bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM lamontana.cobro_pedido cp JOIN lamontana.pedido_actual p USING(id_pedido)
 WHERE cp.id_pedido=pedido AND NOT cp.intervencion_pendiente
 AND NOT lamontana.cobro_informado_pendiente(pedido) AND NOT lamontana.cobro_dinero_por_resolver(pedido)
 AND NOT lamontana.cobro_comprobante_validando(pedido)
 AND NOT EXISTS(SELECT 1 FROM lamontana.solicitud_correccion s WHERE s.id_pedido=pedido AND s.estado IN ('PENDIENTE','RESPONDIDA'))
 AND NOT EXISTS(SELECT 1 FROM lamontana.propuesta_reprogramacion s WHERE s.id_pedido=pedido AND s.estado='PENDIENTE')
 AND NOT EXISTS(SELECT 1 FROM lamontana.trabajo_impresion t WHERE t.id_pedido=pedido AND t.estado='IMPRIMIENDO')
 AND NOT EXISTS(SELECT 1 FROM lamontana.pedido_archivo_actual pa JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) WHERE pa.id_pedido=pedido AND a.estado='VALIDANDO')
 AND ((p.estado IN ('CANCELADO','RECHAZADO')) OR (p.estado IN ('PENDIENTE_REVISION','APROBADO')
  AND cp.retener_hasta IS NOT NULL AND clock_timestamp()>=cp.retener_hasta AND NOT cp.excepcion_aceptada AND NOT lamontana.produccion_cubierta(pedido))))
$$;

-- El modo automático puede esperar acreditación: no se simula una aprobación humana.
ALTER TABLE lamontana.pedido DROP CONSTRAINT pedido_aprobacion_check;
ALTER TABLE lamontana.pedido ADD CONSTRAINT pedido_aprobacion_check CHECK(
 (estado<>'APROBADO' OR aprobada_en IS NOT NULL)
 AND (estado NOT IN ('PENDIENTE_REVISION','RECHAZADO') OR aprobada_en IS NULL)
 AND (aprobada_en IS NULL OR aprobada_en>=confirmada_en)
 AND ((modo_aprobacion='AUTOMATICA' AND (aprobada_en IS NULL OR aprobada_en>=confirmada_en))
 OR (modo_aprobacion='HUMANA' AND ((aprobada_en IS NULL AND id_aprobador IS NULL)
 OR (aprobada_en IS NOT NULL AND id_aprobador IS NOT NULL AND revisada_en IS NOT NULL AND aprobada_en>=revisada_en)))));

CREATE FUNCTION lamontana.validar_autorizacion_economica() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF ((TG_OP='INSERT' AND NEW.estado='APROBADO') OR (TG_OP='UPDATE' AND NEW.estado IN ('APROBADO','EN_PRODUCCION') AND NEW.estado IS DISTINCT FROM OLD.estado)) AND EXISTS(SELECT 1 FROM lamontana.cobro_pedido WHERE id_pedido=NEW.id_pedido)
 AND (NOT lamontana.produccion_cubierta(NEW.id_pedido) OR lamontana.cobro_bloquea_produccion(NEW.id_pedido))
 THEN RAISE EXCEPTION 'Aprobación/producción requiere anticipo acreditado y resolución financiera vigente' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;

-- Mantiene el parque global: la ubicación física no restringe la autorización del equipo.
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
 IF NEW.id_impresora IS NOT NULL AND NOT EXISTS(SELECT 1 FROM lamontana.configuracion_impresora c JOIN lamontana.impresora i USING(id_impresora)
  WHERE c.id_configuracion_version=NEW.id_configuracion_version AND c.id_impresora=NEW.id_impresora
  AND c.nombre=NEW.impresora_nombre AND c.estado='OPERATIVA' AND c.retirada_en IS NULL)
 THEN RAISE EXCEPTION 'La impresora debe estar declarada en la versión global de la imprenta' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER pedido_autorizacion_economica AFTER INSERT OR UPDATE ON lamontana.pedido
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION lamontana.validar_autorizacion_economica();
CREATE FUNCTION lamontana.archivo_no_revocado() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM lamontana.eliminacion_archivo WHERE id_archivo_almacenado=NEW.id_archivo_almacenado)
 OR lamontana.cobro_bloquea_produccion(NEW.id_pedido)
 THEN RAISE EXCEPTION 'El archivo no tiene autorización vigente para producir' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trabajo_archivo_autorizado BEFORE INSERT ON lamontana.trabajo_impresion FOR EACH ROW EXECUTE FUNCTION lamontana.archivo_no_revocado();

-- El sistema tiene identidad explícita de auditoría, no se atribuye su acción al cliente/administrador.
ALTER TABLE lamontana.historial_estado_pedido ALTER COLUMN id_actor DROP NOT NULL;
ALTER TABLE lamontana.historial_estado_pedido DROP CONSTRAINT gestion_pedido_evento_check;
ALTER TABLE lamontana.historial_estado_pedido ADD CONSTRAINT gestion_pedido_evento_check CHECK(
 (version_pedido IS NULL AND accion IS NULL AND estado_origen IS NULL AND mensaje_cliente IS NULL AND actor_nombre IS NULL AND actor_tipo IS NULL AND id_actor IS NOT NULL)
 OR (version_pedido>1 AND accion IS NOT NULL AND estado_origen IS NOT NULL AND mensaje_cliente IS NOT NULL AND length(trim(mensaje_cliente))>0 AND actor_nombre IS NOT NULL
 AND ((actor_tipo IN ('CLIENTE','ADMIN_ADMIN','EMPLEADO') AND id_actor IS NOT NULL
 AND accion IN ('REVISAR','APROBAR','RECHAZAR','CANCELAR','PEDIR_CORRECCION','RESPONDER_CORRECCION','INICIAR_TRABAJO','COMPLETAR_TRABAJO','ERROR_TRABAJO','CANCELAR_TRABAJO','CONTROLAR_CALIDAD','PREPARAR_ENVIO','SALIR_REPARTO','LLEGAR_PUNTO','ENTREGAR','CERRAR'))
 OR (actor_tipo='SISTEMA' AND id_actor IS NULL AND accion='CANCELAR'))));

-- Conserva todas las transiciones previas y añade únicamente la aprobación condicional diferida.
CREATE OR REPLACE FUNCTION lamontana.controlar_transicion_pedido() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
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
