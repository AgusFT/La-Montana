-- La identidad del entorno pertenece a la BD: una variable de arranque no convierte pruebas en producción.
CREATE TABLE lamontana.entorno_instalacion (
    unica boolean PRIMARY KEY CHECK (unica),
    id_instalacion uuid NOT NULL UNIQUE,
    modo varchar(16) NOT NULL CHECK (modo IN ('DESARROLLO','STANDARD')),
    creada_en timestamptz NOT NULL DEFAULT now()
);
-- Las bases previas con usuarios se reconocen como Desarrollo.
INSERT INTO lamontana.entorno_instalacion (unica,id_instalacion,modo)
SELECT true,gen_random_uuid(),'DESARROLLO' WHERE EXISTS (SELECT 1 FROM lamontana.usuario);
