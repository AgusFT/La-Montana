ALTER TABLE lamontana.origen_configuracion ADD COLUMN tipo varchar(20) NOT NULL DEFAULT 'USO_COMO_BASE' CHECK(tipo IN ('USO_COMO_BASE','ROLLBACK'));
ALTER TABLE lamontana.intento_activacion_configuracion
 ADD COLUMN operacion varchar(16) NOT NULL DEFAULT 'ACTIVACION' CHECK(operacion IN ('ACTIVACION','ROLLBACK')),
 ADD COLUMN id_programacion_anterior bigint REFERENCES lamontana.programacion_configuracion(id_configuracion_version),
 DROP CONSTRAINT intento_activacion_configuracion_check1,
 ADD CONSTRAINT intento_resultante_operacion CHECK(
   (estado='EXITOSO' AND id_version_resultante IS NOT NULL AND
     ((operacion='ACTIVACION' AND id_version_resultante=id_version_objetivo) OR
      (operacion='ROLLBACK' AND id_version_resultante<>id_version_objetivo AND id_version_resultante<>id_version_anterior)))
   OR (estado<>'EXITOSO' AND id_version_resultante IS NULL)),
 ADD CONSTRAINT intento_rollback_manual CHECK(operacion<>'ROLLBACK' OR (origen='MANUAL' AND id_version_anterior IS NOT NULL)),
 ADD CONSTRAINT intento_programacion_rollback CHECK(id_programacion_anterior IS NULL OR operacion='ROLLBACK');
CREATE FUNCTION lamontana.comprobar_rollback() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.operacion<>'ROLLBACK' THEN RETURN NEW; END IF;
 IF TG_OP='INSERT' THEN
  IF NOT EXISTS(SELECT 1 FROM lamontana.configuracion_version c JOIN lamontana.activacion_configuracion a USING(id_configuracion_version)
    JOIN lamontana.configuracion_version h ON h.id_configuracion_version=a.id_predecesora
    WHERE c.estado='ACTIVA' AND c.id_configuracion_version=NEW.id_version_anterior AND h.estado='HISTORICA' AND h.id_configuracion_version=NEW.id_version_objetivo)
    OR EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE estado='EN_PREPARACION')
    OR NEW.id_programacion_anterior IS DISTINCT FROM (SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE estado='PROGRAMADA') THEN
     RAISE EXCEPTION 'La reversión requiere la predecesora efectiva y el estado pendiente revisado' USING ERRCODE='23514';
  END IF;
 ELSIF NEW.estado='EXITOSO' AND OLD.estado<>'EXITOSO' THEN
  IF NOT EXISTS(SELECT 1 FROM lamontana.origen_configuracion o JOIN lamontana.activacion_configuracion a USING(id_configuracion_version)
    JOIN lamontana.configuracion_version c USING(id_configuracion_version) WHERE o.id_configuracion_version=NEW.id_version_resultante
    AND o.id_version_origen=NEW.id_version_objetivo AND o.tipo='ROLLBACK' AND a.id_predecesora=NEW.id_version_anterior AND c.estado='ACTIVA')
    OR (NEW.id_programacion_anterior IS NOT NULL AND NOT EXISTS(SELECT 1 FROM lamontana.configuracion_version WHERE id_configuracion_version=NEW.id_programacion_anterior AND estado='CANCELADA')) THEN
     RAISE EXCEPTION 'La reversión no publicó su copia y cancelación completas' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER rollback_coherente BEFORE INSERT OR UPDATE ON lamontana.intento_activacion_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.comprobar_rollback();
ALTER TABLE lamontana.autorizacion_configuracion DROP CONSTRAINT autorizacion_configuracion_proposito_check, ADD CONSTRAINT autorizacion_configuracion_proposito_check CHECK(proposito IN ('CANCELAR_BORRADOR','ACTIVAR_CONFIGURACION','PROGRAMAR_CONFIGURACION','CANCELAR_PROGRAMACION','ADELANTAR_CONFIGURACION','ROLLBACK_CONFIGURACION'));
ALTER TABLE lamontana.comprobante_configuracion DROP CONSTRAINT comprobante_configuracion_tipo_check, ADD CONSTRAINT comprobante_configuracion_tipo_check CHECK(tipo IN ('CREAR_BORRADOR','GUARDAR_MODELO','CANCELAR_BORRADOR','GUARDAR_PAGOS','GUARDAR_RECURSOS','ALTA_IMPRESORA','EDITAR_IMPRESORA','ESTADO_IMPRESORA','RETIRAR_IMPRESORA','GUARDAR_ENTREGA','ALTA_PUNTO','EDITAR_PUNTO','ALTA_ZONA','EDITAR_ZONA','ACTIVAR_CONFIGURACION','PROGRAMAR_CONFIGURACION','CANCELAR_PROGRAMACION','ADELANTAR_CONFIGURACION','USAR_COMO_BASE','RECONFIRMAR_REVISION','ROLLBACK_CONFIGURACION'));
ALTER TABLE lamontana.evento_configuracion DROP CONSTRAINT evento_configuracion_tipo_check, ADD CONSTRAINT evento_configuracion_tipo_check CHECK(tipo IN ('BORRADOR_CREADO','MODELO_SELECCIONADO','CANCELACION_SOLICITADA','BORRADOR_CANCELADO','PAGOS_CONFIGURADOS','RECURSOS_CONFIGURADOS','IMPRESORA_CREADA','IMPRESORA_EDITADA','IMPRESORA_ESTADO_CAMBIADO','IMPRESORA_RETIRADA','ENTREGA_CONFIGURADA','PUNTO_CREADO','PUNTO_EDITADO','ZONA_CREADA','ZONA_EDITADA','ACTIVACION_SOLICITADA','CONFIGURACION_ACTIVADA','CONFIGURACION_HISTORICA','AUTORIZACION_RECHAZADA','PROGRAMACION_SOLICITADA','CONFIGURACION_PROGRAMADA','PROGRAMACION_CANCELADA','INTENTO_MANUAL_INICIADO','BORRADOR_DESDE_BASE','REVISION_RECONFIRMADA','ROLLBACK_SOLICITADO','ROLLBACK_INICIADO','ROLLBACK_PUBLICADO'));
