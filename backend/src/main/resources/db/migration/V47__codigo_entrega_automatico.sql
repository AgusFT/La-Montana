ALTER TABLE lamontana.codigo_entrega ADD COLUMN automatico boolean NOT NULL DEFAULT false,ADD COLUMN codigo_cifrado text;
ALTER TABLE lamontana.codigo_entrega ADD CONSTRAINT codigo_automatico_cifrado CHECK(NOT automatico OR codigo_cifrado IS NOT NULL);
CREATE TABLE lamontana.aviso_codigo_entrega (
 id_codigo_entrega bigint PRIMARY KEY REFERENCES lamontana.codigo_entrega,
 correo varchar(254) NOT NULL,estado varchar(12) NOT NULL DEFAULT 'PENDIENTE' CHECK(estado IN ('PENDIENTE','ENVIADO','CANCELADO')),
 intentos integer NOT NULL DEFAULT 0,proximo_intento timestamptz NOT NULL DEFAULT clock_timestamp(),enviado_en timestamptz
);
CREATE OR REPLACE FUNCTION lamontana.validar_codigo_entrega() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'El código conserva su auditoría' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' THEN
  IF NOT EXISTS(SELECT 1 FROM lamontana.pedido p JOIN lamontana.reserva_entrega r USING(id_pedido)
   WHERE p.id_pedido=NEW.id_pedido AND p.estado='LISTO_PARA_ENTREGA' AND r.id_reserva_entrega=NEW.id_reserva_entrega AND r.estado='ACTIVA'
   AND ((NOT NEW.automatico AND p.id_usuario_creador=NEW.id_emisor) OR (NEW.automatico AND NEW.codigo_cifrado IS NOT NULL
    AND lamontana.estado_logistico(p.id_pedido) IN ('LISTO_RETIRO','EN_VIAJE','DISPONIBLE_PUNTO')
    AND EXISTS(SELECT 1 FROM lamontana.historial_estado_pedido h WHERE h.id_pedido=p.id_pedido AND h.id_actor=NEW.id_emisor AND h.accion IN ('SALIR_REPARTO','CONTROLAR_CALIDAD')))))
  THEN RAISE EXCEPTION 'El código automático requiere calidad o despacho y la reserva vigente' USING ERRCODE='23514'; END IF;
 ELSE
  IF (to_jsonb(NEW)-ARRAY['revocado_en','utilizado_en','intentos']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['revocado_en','utilizado_en','intentos'])
  OR OLD.revocado_en IS NOT NULL OR OLD.utilizado_en IS NOT NULL
  OR NOT ((NEW.intentos=OLD.intentos+1 AND NEW.revocado_en IS NULL AND NEW.utilizado_en IS NULL)
   OR (NEW.intentos=OLD.intentos AND ((NEW.revocado_en IS NOT NULL AND NEW.utilizado_en IS NULL) OR (NEW.utilizado_en IS NOT NULL AND NEW.revocado_en IS NULL))))
  THEN RAISE EXCEPTION 'Transición de código inválida' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
