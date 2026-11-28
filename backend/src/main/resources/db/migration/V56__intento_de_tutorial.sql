-- Los avances existentes pertenecen al intento inicial. Cada reinicio invalida sus guardados pendientes.
ALTER TABLE lamontana.tutorial_demo ADD COLUMN intento bigint NOT NULL DEFAULT 0 CHECK (intento >= 0);
ALTER TABLE lamontana.tutorial_recorrido ADD COLUMN intento bigint NOT NULL DEFAULT 0 CHECK (intento >= 0);
