-- Flyway ya tiene el bloqueo. Otra edición pudo crear el historial tras la inspección inicial.
DO $$
BEGIN
    IF EXISTS (SELECT FROM lamontana.flyway_schema_history
               WHERE installed_by <> 'LaMontana-Demo') THEN
        RAISE EXCEPTION 'La Demo requiere una base inicializada por La Montaña Demo.';
    END IF;
END $$;
