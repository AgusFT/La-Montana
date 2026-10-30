-- Estado cotidiano por identidad física; no reescribe las capacidades versionadas.
INSERT INTO lamontana.permiso(codigo,nombre,descripcion,alcance) VALUES
 ('GESTIONAR_IMPRESORAS','Gestionar impresoras','Habilitar equipos y registrar recargas y conteos de papel de la imprenta.','GLOBAL'),
 ('GESTIONAR_PUNTOS_ENTREGA','Gestionar puntos de entrega','Cambiar la disponibilidad cotidiana de los puntos de entrega.','GLOBAL');
CREATE TABLE lamontana.operacion_impresora (
 id_impresora bigint PRIMARY KEY REFERENCES lamontana.impresora,
 habilitada boolean NOT NULL,
 hojas_registradas integer CHECK(hojas_registradas>=0),
 version bigint NOT NULL CHECK(version>0),
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 actualizada_en timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE lamontana.evento_operacion_impresora (
 id_operacion uuid PRIMARY KEY,
 id_impresora bigint NOT NULL REFERENCES lamontana.impresora,
 id_actor bigint NOT NULL REFERENCES lamontana.usuario,
 actor_nombre varchar(300) NOT NULL,actor_rol varchar(40) NOT NULL,
 accion varchar(24) NOT NULL CHECK(accion IN ('HABILITAR','DESHABILITAR','RECARGAR','CONTAR','ASIGNAR_TRABAJO')),
 huella char(64) NOT NULL,
 habilitada_anterior boolean NOT NULL,habilitada_nueva boolean NOT NULL,
 hojas_anteriores integer CHECK(hojas_anteriores>=0),hojas_nuevas integer CHECK(hojas_nuevas>=0),
 cantidad integer CHECK(cantidad>=0),
 version_anterior bigint NOT NULL CHECK(version_anterior>=0),version_nueva bigint NOT NULL CHECK(version_nueva=version_anterior+1),
 id_trabajo_impresion bigint REFERENCES lamontana.trabajo_impresion,
 motivo varchar(500) NOT NULL,
 fecha timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(id_impresora,version_nueva)
);
CREATE TRIGGER evento_operacion_impresora_inmutable BEFORE UPDATE OR DELETE ON lamontana.evento_operacion_impresora
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE TRIGGER evento_disponibilidad_punto_inmutable BEFORE UPDATE OR DELETE ON lamontana.evento_disponibilidad_punto
 FOR EACH ROW EXECUTE FUNCTION lamontana.proteger_detalle_cotizacion();
CREATE INDEX evento_operacion_impresora_orden ON lamontana.evento_operacion_impresora(id_impresora,version_nueva DESC);
CREATE FUNCTION lamontana.controlar_disponibilidad_impresora() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id_impresora IS NOT NULL AND EXISTS(SELECT 1 FROM lamontana.operacion_impresora o WHERE o.id_impresora=NEW.id_impresora AND NOT o.habilitada)
 THEN RAISE EXCEPTION 'La impresora está deshabilitada para nuevos trabajos' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trabajo_disponibilidad_impresora BEFORE INSERT ON lamontana.trabajo_impresion
 FOR EACH ROW EXECUTE FUNCTION lamontana.controlar_disponibilidad_impresora();
