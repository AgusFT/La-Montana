-- Conserva las aceptaciones históricas. Confirmar un envío no afirma que el cliente haya
-- abierto la vista previa: ambos hechos se distinguen sin debilitar la vinculación del PDF.
ALTER TABLE lamontana.aceptacion_vista_previa
 ADD COLUMN tipo_registro varchar(20) NOT NULL DEFAULT 'VISTA_PREVIA',
 ADD CONSTRAINT registro_pdf_tipo CHECK(tipo_registro IN ('VISTA_PREVIA','ENVIO_PEDIDO'));

-- El registro técnico de envío sólo puede existir junto con el pedido o la respuesta que
-- conserva ese original exacto. Los controles existentes de propietario, hash y validaciones
-- siguen activos, al igual que las FK no nulas de producción y correcciones.
CREATE FUNCTION lamontana.validar_registro_envio_pdf() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.tipo_registro='ENVIO_PEDIDO' AND NOT (
  EXISTS(SELECT 1 FROM lamontana.pedido_item i JOIN lamontana.pedido p USING(id_pedido)
   WHERE i.id_aceptacion_vista_previa=NEW.id_aceptacion_vista_previa
    AND i.id_archivo_almacenado=NEW.id_archivo_almacenado AND p.id_usuario_creador=NEW.id_usuario)
  OR EXISTS(SELECT 1 FROM lamontana.respuesta_correccion_archivo i
   JOIN lamontana.respuesta_correccion r USING(id_respuesta_correccion)
   WHERE i.id_aceptacion_vista_previa=NEW.id_aceptacion_vista_previa
    AND i.id_archivo_almacenado=NEW.id_archivo_almacenado AND r.id_autor=NEW.id_usuario)
 ) THEN RAISE EXCEPTION 'El envío de PDF requiere su pedido o respuesta en la misma transacción' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER registro_envio_pdf_vinculado
 AFTER INSERT ON lamontana.aceptacion_vista_previa DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION lamontana.validar_registro_envio_pdf();
