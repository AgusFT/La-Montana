-- Una identidad de empleado restringida por punto; no altera los roles ni los pedidos históricos.
ALTER TABLE lamontana.usuario ADD COLUMN alias_acceso varchar(80);
ALTER TABLE lamontana.usuario ADD CONSTRAINT alias_acceso_formato CHECK(alias_acceso IS NULL OR alias_acceso ~ '^[a-z0-9][a-z0-9._-]{2,79}$');
CREATE UNIQUE INDEX usuario_alias_acceso_unico ON lamontana.usuario(alias_acceso) WHERE alias_acceso IS NOT NULL;
CREATE TABLE lamontana.cuenta_punto (
 id_punto_entrega bigint PRIMARY KEY REFERENCES lamontana.punto_entrega,
 id_usuario bigint NOT NULL UNIQUE REFERENCES lamontana.usuario,
 responsable varchar(100) NOT NULL CHECK(length(trim(responsable))>0),
 activa boolean NOT NULL DEFAULT true,cobros boolean NOT NULL DEFAULT true,
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),creada_en timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE lamontana.evento_cuenta_punto (
 id_operacion uuid PRIMARY KEY,id_punto_entrega bigint NOT NULL REFERENCES lamontana.cuenta_punto,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,accion varchar(16) NOT NULL CHECK(accion IN ('CREAR','ACTUALIZAR')),
 huella char(64) NOT NULL,fecha timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TRIGGER evento_cuenta_punto_inmutable BEFORE UPDATE OR DELETE ON lamontana.evento_cuenta_punto FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE FUNCTION lamontana.validar_cuenta_punto() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR (TG_OP='UPDATE' AND (NEW.id_punto_entrega<>OLD.id_punto_entrega OR NEW.id_usuario<>OLD.id_usuario))
 THEN RAISE EXCEPTION 'El vínculo entre punto y cuenta se conserva' USING ERRCODE='23514'; END IF;
 IF NOT EXISTS(SELECT 1 FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.id_usuario=NEW.id_usuario AND r.codigo='EMPLEADO' AND u.alias_acceso IS NOT NULL)
 THEN RAISE EXCEPTION 'La cuenta del punto necesita un empleado con usuario de acceso' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER cuenta_punto_control BEFORE INSERT OR UPDATE OR DELETE ON lamontana.cuenta_punto FOR EACH ROW EXECUTE FUNCTION lamontana.validar_cuenta_punto();
CREATE TABLE lamontana.recepcion_lote_punto (
 id_operacion uuid PRIMARY KEY,id_punto_entrega bigint NOT NULL REFERENCES lamontana.cuenta_punto,
 id_usuario bigint NOT NULL REFERENCES lamontana.usuario,huella char(64) NOT NULL,cantidad integer NOT NULL CHECK(cantidad BETWEEN 1 AND 25),fecha timestamptz NOT NULL
);
CREATE TRIGGER recepcion_lote_punto_inmutable BEFORE UPDATE OR DELETE ON lamontana.recepcion_lote_punto FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
