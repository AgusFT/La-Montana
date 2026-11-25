-- Revalidar la procedencia bajo el bloqueo de Flyway antes de crear la identidad permanente.
DO $$
BEGIN
    IF EXISTS (SELECT FROM lamontana.flyway_schema_history
               WHERE type <> 'SCHEMA' AND installed_by <> 'LaMontana-Demo') THEN
        RAISE EXCEPTION 'La Demo requiere una base inicializada por La Montaña Demo.';
    END IF;
END $$;

ALTER TABLE lamontana.entorno_instalacion DROP CONSTRAINT entorno_instalacion_modo_check;
ALTER TABLE lamontana.entorno_instalacion ADD CONSTRAINT entorno_instalacion_modo_check CHECK (modo = 'DEMO');
INSERT INTO lamontana.entorno_instalacion (unica, id_instalacion, modo)
VALUES (true, gen_random_uuid(), 'DEMO');
