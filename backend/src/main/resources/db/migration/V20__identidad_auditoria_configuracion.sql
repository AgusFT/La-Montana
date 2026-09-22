-- No se reconstruye retrospectivamente un nombre o rol que no fue capturado.
ALTER TABLE lamontana.configuracion_version ADD COLUMN nombre_actor text, ADD COLUMN rol_actor text;
ALTER TABLE lamontana.evento_configuracion ADD COLUMN nombre_actor text, ADD COLUMN rol_actor text;
ALTER TABLE lamontana.programacion_configuracion ADD COLUMN nombre_actor text, ADD COLUMN rol_actor text,
 ADD COLUMN id_catalogo_revision bigint REFERENCES lamontana.catalogo_revision;
ALTER TABLE lamontana.activacion_configuracion ADD COLUMN nombre_actor text, ADD COLUMN rol_actor text;
ALTER TABLE lamontana.intento_activacion_configuracion ADD COLUMN nombre_actor text, ADD COLUMN rol_actor text;
CREATE FUNCTION lamontana.capturar_actor_configuracion() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE actor bigint;
BEGIN
 actor := (to_jsonb(NEW)->>TG_ARGV[0])::bigint;
 IF actor IS NULL THEN NEW.nombre_actor := 'Sistema automático'; NEW.rol_actor := 'SISTEMA';
 ELSE SELECT u.nombre||' '||u.apellido,r.codigo INTO NEW.nombre_actor,NEW.rol_actor
 FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE u.id_usuario=actor; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER capturar_creador BEFORE INSERT ON lamontana.configuracion_version FOR EACH ROW EXECUTE FUNCTION lamontana.capturar_actor_configuracion('id_usuario_creador');
CREATE TRIGGER capturar_evento BEFORE INSERT ON lamontana.evento_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.capturar_actor_configuracion('id_actor');
CREATE TRIGGER capturar_programador BEFORE INSERT ON lamontana.programacion_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.capturar_actor_configuracion('id_actor');
CREATE TRIGGER capturar_activador BEFORE INSERT ON lamontana.activacion_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.capturar_actor_configuracion('id_actor');
CREATE TRIGGER capturar_intento BEFORE INSERT ON lamontana.intento_activacion_configuracion FOR EACH ROW EXECUTE FUNCTION lamontana.capturar_actor_configuracion('id_usuario_actor');
CREATE INDEX configuracion_eventos_orden ON lamontana.evento_configuracion(id_configuracion_version,fecha,id_evento_configuracion);
