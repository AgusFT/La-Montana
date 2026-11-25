ALTER TABLE lamontana.archivo_almacenado ADD COLUMN finalidad varchar(16) NOT NULL DEFAULT 'TRABAJO' CHECK(finalidad IN ('TRABAJO','COMPROBANTE'));
CREATE TABLE lamontana.comprobante_pago (
 id_comprobante_pago bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_archivo_almacenado bigint NOT NULL UNIQUE REFERENCES lamontana.archivo_almacenado,
 id_intento_pago bigint REFERENCES lamontana.intento_pago,
 id_pago bigint REFERENCES lamontana.pago,
 tipo varchar(28) NOT NULL CHECK(tipo IN ('EVIDENCIA_TRANSFERENCIA','RECIBO_INTERNO')),
 id_comprobante_reemplazado bigint REFERENCES lamontana.comprobante_pago,
 actor_nombre varchar(300) NOT NULL, actor_rol varchar(40) NOT NULL,
 detalle varchar(300) NOT NULL,
 CHECK((id_intento_pago IS NULL)<>(id_pago IS NULL)),
 CHECK(id_intento_pago IS NULL OR tipo='EVIDENCIA_TRANSFERENCIA')
);
CREATE INDEX comprobante_intento ON lamontana.comprobante_pago(id_intento_pago,id_comprobante_pago DESC);
CREATE INDEX comprobante_por_pago ON lamontana.comprobante_pago(id_pago,id_comprobante_pago DESC);
CREATE TRIGGER comprobante_inmutable BEFORE UPDATE OR DELETE ON lamontana.comprobante_pago FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE FUNCTION lamontana.exigir_finalidad_archivo() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM lamontana.archivo_almacenado a WHERE a.id_archivo_almacenado=NEW.id_archivo_almacenado
    AND a.finalidad=CASE WHEN TG_TABLE_NAME='archivo_trabajo' THEN 'TRABAJO' ELSE 'COMPROBANTE' END)
 THEN RAISE EXCEPTION 'La finalidad del archivo no corresponde al vínculo' USING ERRCODE='23514'; END IF;
 IF TG_TABLE_NAME='comprobante_pago' THEN
  IF NEW.id_pago IS NOT NULL AND NOT EXISTS(SELECT 1 FROM lamontana.pago p WHERE p.id_pago=NEW.id_pago
    AND NEW.tipo=CASE WHEN p.medio='EFECTIVO' THEN 'RECIBO_INTERNO' ELSE 'EVIDENCIA_TRANSFERENCIA' END)
  THEN RAISE EXCEPTION 'El comprobante no corresponde al medio recibido' USING ERRCODE='23514'; END IF;
  IF NEW.id_comprobante_reemplazado IS NOT NULL AND NOT EXISTS(SELECT 1 FROM lamontana.comprobante_pago p
    WHERE p.id_comprobante_pago=NEW.id_comprobante_reemplazado AND p.id_pago IS NOT DISTINCT FROM NEW.id_pago
    AND p.id_intento_pago IS NOT DISTINCT FROM NEW.id_intento_pago)
  THEN RAISE EXCEPTION 'La versión anterior pertenece a otro movimiento' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trabajo_finalidad BEFORE INSERT ON lamontana.archivo_trabajo FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_finalidad_archivo();
CREATE TRIGGER comprobante_finalidad BEFORE INSERT ON lamontana.comprobante_pago FOR EACH ROW EXECUTE FUNCTION lamontana.exigir_finalidad_archivo();
